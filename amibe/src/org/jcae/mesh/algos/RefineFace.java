/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2003 Jerome Robert <jeromerobert@users.sourceforge.net>

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.algos;

import org.jcae.opencascade.jni.*;
import org.jcae.mesh.sd.*;
import org.jcae.mesh.util.*;
import java.util.Iterator;
import java.util.Set;
import java.util.Collection;
import java.util.ArrayList;
import org.jcae.mesh.*;
import org.apache.log4j.Logger;

//provisoire (pour test)
import java.io.*;
import org.jcae.mesh.drivers.UNVWriter;

/**
 * This class allows face discretization.
 * The value of discretisation is provided by the constraint hypothesis.
 * \n
 */

public class RefineFace
{
	private static Logger logger=Logger.getLogger(RefineFace.class);
	
	public MeshOfCAD compute(MeshOfCAD mesh, MeshConstraint constraint)
	{		
		Iterator itf = mesh.getGeometryIterator();
		int nbfaces = 0;
		
		/* Explore the shape for each face */
		while (itf.hasNext())
		{
			TopoDS_Shape s = (TopoDS_Shape) itf.next();
			if (s.shapeType() == TopAbs_ShapeEnum.FACE)
			{
				MeshOfCAD m = mesh.getMeshFromMapOfSubMesh(s);
				Mesh2D m2d = computeFace(m, constraint);
				if (m2d!=null)
				{
		 			innerRefine(m2d, constraint.getValue());
					innerRefine(m2d, constraint.getValue());
					nbfaces++;
					m2d.addMesh(mesh);
					logger.info(" Fin add , face: "+nbfaces);
					m2d=null;
				}
			}
		}
		return mesh;
	}

	/**
	 * Launch method to mesh a surface.
	 * - first step : create a Mesh2D instance, then retrives all nodes from the mesh structure, converts them in 2D space,
	 * and adds these MeshNode2D instances to this Mesh2D instance. Nodes position are added to the nodeMaj array in order to be able
	 * to retrieve the correct vertex in the case where node is on a circular edge (degenerated or not).
	 * - second step : create a Triangle2D instance ( mesh.Mesh2D.getTriangleMax() ) from the mesh2d just created, and
	 * runs the meshing algorithm
	 * 	-# insertPt()
	 * 	-# swapEdge()
	 * 	-# tague()
	 * 	-# finalize2D()
	 * 	-# innerRefine() for TRIA algorithm only.
	 * \n
	 * @param mesh : a MeshMesh instance, the main mesh
	 * @param constraint : a MeshConstraint instance, the hypothesis to apply.
	 * @return MeshMesh : the mesh2d that describes the meshing surface.
	 * @see org.jcae.mesh.Algo#compute(MeshMesh, MeshConstraint)
	 */
	/**
	 * Launch method to mesh a surface.
	 * This method calls the older mesher to perform the initial Delaunay
	 * triangulation.  Thus a <code>Mesh2D</code> instance is created first,
	 * boundary nodes are converted to <code>MeshNode</code>,
	 * <code>MeshNode2D</code> and <code>MeshEdge</code>, and transported to
	 * this instance,  The older meshing algorithm can then take place:
	 * <ul>
	 *   <li>Creation of a <code>Triangle2D</code> instance which contains
	 *       all boundary nodes.</li>
	 *   <li>Insertion of all boundary nodes and triangulation.</li>
	 *   <li>Edge swap to restore boundary edges.</li>
	 *   <li>Removal of outer triangles.</li>
	 * </ul>
	 *
	 * @see Mesh2D#getTriangleMax
	 * @see Mesh2D#insertPt
	 * @see Mesh2D#tague
	 * @see Mesh2D#finalize2D
	 *
	 * @param mesh  the mesh data structure which is ipdated.
	 * @param constraint  the constraint to apply.
	 */
	protected static Mesh2D computeFace(MeshOfCAD mesh, MeshConstraint constraint)
	{
		logger.debug("Enter Mesh2D.computeFace");
		
		if (mesh.getGeometry().shapeType() != TopAbs_ShapeEnum.FACE)
		{
			logger.error("Not a TopoDS_Face, cant refine");
			return null;
		}

		TopoDS_Face face = (TopoDS_Face) mesh.getGeometry();
		Mesh2D mesh2d = new Mesh2D(mesh.getGeometry());
		HashSet edgeslust=new HashSet();
		TopoDS_Edge E;
		TopExp_Explorer expW = new TopExp_Explorer(face, TopAbs_ShapeEnum.WIRE);
		while (expW.more()) {
			TopoDS_Wire W = (TopoDS_Wire) expW.current();
			BRepTools_WireExplorer wexp = new BRepTools_WireExplorer();
			for (wexp.init(W, face); wexp.more(); wexp.next()) {
				E = wexp.current();
				MeshOfCAD edge = mesh.getFather().getMeshFromMapOfSubMesh(E);
				if (edge == null){
					TopoDS_Edge EE = (TopoDS_Edge) E.reversed();
					edge=mesh.getFather().getMeshFromMapOfSubMesh(EE);
				}
				
				Iterator it=edge.getEdgesIterator();
				while(it.hasNext()){
					MeshEdge e=(MeshEdge)it.next();
					MeshNode node1=e.getNodes1();
					MeshNode node2=e.getNodes2();
					
					MeshNode2D pt1=MeshNode2D.getCoord2D(face,E,node1.getPosition());
					MeshNode2D pt2=MeshNode2D.getCoord2D(face,E,node2.getPosition());
					
					pt1=(MeshNode2D)mesh2d.addNode(pt1);
					pt1.addNodeMaj(node1);
					pt2=(MeshNode2D)mesh2d.addNode(pt2);
					pt2.addNodeMaj(node2);
					MeshEdge et=new MeshEdge(pt1,pt2);

					edgeslust.add(et);
				}
			}
			expW.next();
		}
		
		Triangle2D triangle0 =(Triangle2D)mesh2d.getTriangleMax(
			(TopoDS_Face) mesh.getGeometry());
		mesh2d.insertPt(triangle0);
		Pair encore = new Pair();
		encore.second = new Boolean(true);
		int tt = 0;
		while (((Boolean) (encore.second)).booleanValue()) {
			encore = mesh2d.getTriangle();
			if (((Boolean) (encore.second)).booleanValue())
				mesh2d.insertPt((Triangle2D) encore.first);
		}
		logger.info(" Fin mesh ");

		

		/* Swap */

		int j = 0;
		int nb_swap = 0, edge_ok = 0;
		Object[] edges = edgeslust.toArray();
		int nb_edges = edges.length;
		int nbsousswap = 0;

		while (nb_swap + edge_ok < nb_edges) {
			MeshEdge e = (MeshEdge) edges[j];
			MeshNode2D n1 = (MeshNode2D)e.getNodes1();
			MeshNode2D n2 = (MeshNode2D)e.getNodes2();
			
			// if it exists an edge to swap
			if (mesh2d.getEdgeDefinedByNodes(n1, n2) == null) {
				
				Collection set = mesh2d.findEdgesCuttingSegment(n1,n2);
				nbsousswap = 0;
				Iterator ite = set.iterator();
				while (ite.hasNext()) {
					MeshEdge elem = (MeshEdge) ite.next();
					if (mesh2d.swapEdge(elem))
						nbsousswap++;
				}
				if (nbsousswap == set.size())
					nb_swap++;

			} else // the edge is OK
				edge_ok++;

			j++;
			if ((j == nb_edges) && (nb_swap + edge_ok != nb_edges)) {
				j = 0;
				edge_ok = 0;
				nb_swap = 0;
			}
		}

		for (int i = 0; i < nb_edges; i++) {
			
			//set wire edge
			MeshEdge e = (MeshEdge) edges[i];
			MeshNode2D n1 = (MeshNode2D)e.getNodes1();
			MeshNode2D n2 = (MeshNode2D)e.getNodes2();
			
			MeshEdge e1=mesh2d.getEdgeDefinedByNodes(n1, n2);
			if (e1!=null){
				e1.setWire(true);
				e1.setFrozen(true);
			}
		}

		logger.info(" Fin Swap ");
		mesh2d.tague();
		logger.info(" Fin Tag ");
		mesh2d.finalize2D();
		logger.info(" Fin finalize2D ");
		
/*		// sauvegarde pour voir
		try
		{
			UNVWriter sav = new UNVWriter(new FileOutputStream("/Users/garat/pieces/refinePlaque2d.unv"),mesh2d);
			sav.writeMesh();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
*/
		return mesh2d;
	}

	/**
	 * Method used for TRIA algorithm only. Allows to refine all edges of the surface once.
	 * @param mesh2d : a Mesh2D instance, the current 2D mesh.
	 * @param area : a double value, the TRIA constraint value.
	 */
	protected static void innerRefine(Mesh2D mesh2d, double area)
	{
		TopoDS_Face face = (TopoDS_Face) mesh2d.getGeometry();
		// refine all edges
		//HashSet edgesToRefine = mesh2d.getEdgesOfSubMesh(new HashSet());
		// refine edges longer than area 
		HashSet edgesToRefine = mesh2d.getEdgesSupTo(area);
		logger.info("Nb d'edges a refiner2d: " + edgesToRefine.size());
		
		Iterator it = edgesToRefine.iterator();
		while (it.hasNext()) {
			MeshEdge e = (MeshEdge) it.next();
			if (!(e.isWire())) {
				MeshNode2D n1 = (MeshNode2D)e.getNodes1();
				MeshNode2D n2 = (MeshNode2D)e.getNodes2();
				MeshNode2D mid = (MeshNode2D)n1.middle(n2);
				PST_SurfacePosition posS = new PST_SurfacePosition();
				posS.setSurfacePosition(face,mid.getX(),mid.getY());
				mid.setPosition(posS);
				if (mid != null) {
					mid=(MeshNode2D)mesh2d.addNode(mid);
					e.setMidNode(mid);
				} else
					logger.error("MeshMesh - refine2D : Mid pt is null");
			}
		}
						
		mesh2d.refine2D();
	}
}
