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

package org.jcae.mesh.mesher.algos2d;

import org.jcae.opencascade.jni.TopoDS_Face;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.jcae.mesh.mesher.ds.*;
import org.jcae.mesh.sd.*;
import org.jcae.mesh.algos.*;
import org.jcae.mesh.cad.occ.OCCFace;
import java.util.Iterator;
import java.util.HashSet;
import org.jcae.mesh.util.Pair;
import java.util.HashMap;
import java.util.Collection;
import org.apache.log4j.Logger;

/**
 * Performs an initial surface triangulation (deprecated).
 * The value of discretisation is provided by the constraint hypothesis.
 */

public class RefineFaceOld
{
	private static Logger logger=Logger.getLogger(RefineFaceOld.class);
	
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
	 * @param submesh2d  the mesh data structure which is ipdated.
	 */
	public static void compute(SubMesh2D submesh2d)
	{
		TopoDS_Face F = (TopoDS_Face) ((OCCFace)submesh2d.getGeometry()).getShape();
		MeshOfCAD mesh = new MeshOfCAD((TopoDS_Shape) F);
		Mesh2D m2d = new Mesh2D(F);
		
		HashSet edgeslust = new HashSet();
		HashMap mapEdges = new HashMap();
		HashMap mapNodes = new HashMap();
		//  Loop on boundary edges
		Iterator it = submesh2d.getEdges().iterator();
		while (it.hasNext())
		{
			MEdge2D e = (MEdge2D) it.next();
			assert null != e : submesh2d;
			MNode2D node1 = e.getNodes1();
			assert null != node1 : e;
			MNode2D node2 = e.getNodes2();
			assert null != node2 : e;
			MeshNode n1 = new MeshNode(new PST_SurfacePosition(F, node1.getU(), node1.getV()));
			MeshNode n2 = new MeshNode(new PST_SurfacePosition(F, node2.getU(), node2.getV()));
			MeshNode2D pt1 = new MeshNode2D(node1.getU(), node1.getV());
			MeshNode2D pt2 = new MeshNode2D(node2.getU(), node2.getV());
			
			pt1 = (MeshNode2D)m2d.addNode(pt1);
			pt1.addNodeMaj(n1);
			pt2 = (MeshNode2D)m2d.addNode(pt2);
			pt2.addNodeMaj(n2);
			MeshEdge et = new MeshEdge(pt1,pt2);
			et.setWire(true);
			//  et must not be added to m2d, otherwise computations
			//  below will fail.  So it is stored into an array,
			//  and will be inserted later into edgeset.
			mapEdges.put(et, e);
			edgeslust.add(et);

			//  Keep track of relations between Mesh2D and MMesh2D
			mapNodes.put(pt1, node1);
			mapNodes.put(pt2, node2);
		}
		
		Triangle2D triangle0 =(Triangle2D)m2d.getTriangleMax(F);
		m2d.insertPt(triangle0);
		Pair encore = new Pair();
		encore.second = new Boolean(true);
		while (((Boolean) (encore.second)).booleanValue()) {
			encore = m2d.getTriangle();
			if (((Boolean) (encore.second)).booleanValue())
				m2d.insertPt((Triangle2D) encore.first);
		}
		logger.info(" Fin mesh ");

		/* Swap */

		Object[] edges = edgeslust.toArray();
		int nb_edges = edges.length;
		int j = 0;
		int nb_swap = 0, edge_ok = 0;
		int nbsousswap = 0;

		while (nb_swap + edge_ok < nb_edges) {
			MeshEdge e = (MeshEdge) edges[j];
			MeshNode2D node1 = (MeshNode2D)e.getNodes1();
			MeshNode2D node2 = (MeshNode2D)e.getNodes2();
			
			// if it exists an edge to swap
			if (m2d.getEdgeDefinedByNodes(node1, node2) == null) {
				Collection set = m2d.findEdgesCuttingSegment(node1,node2);
				nbsousswap = 0;
				Iterator ite = set.iterator();
				while (ite.hasNext()) {
					MeshEdge elem = (MeshEdge) ite.next();
					if (m2d.swapEdge(elem))
					{
						nbsousswap++;
					}
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
			MeshNode2D node1 = (MeshNode2D)e.getNodes1();
			MeshNode2D node2 = (MeshNode2D)e.getNodes2();
			
			MeshEdge e1=m2d.getEdgeDefinedByNodes(node1, node2);
			if (e1!=null){
				e1.setWire(true);
				e1.setFrozen(true);
			}
		}

		logger.info(" Fin Swap ");
		m2d.tague();
		logger.info(" Fin Tag ");
		m2d.finalize2D();
		logger.info(" Fin finalize2D ");
		
		// Import m2d into mesh2d
		HashSet nodeset = submesh2d.getNodes();
		HashSet edgeset = submesh2d.getEdges();
		HashSet faceset = submesh2d.getFaces();
		it = m2d.getFacesIterator();
		while (it.hasNext())
		{
			MeshFace t = (MeshFace) it.next();
			Iterator ite = t.getEdgesIterator();
			MEdge2D[] e = new MEdge2D[3];
			int i = 0;
			while (ite.hasNext())
			{
				MeshEdge edge = (MeshEdge) ite.next();
				MeshNode2D node1 = (MeshNode2D) edge.getNodes1();
				MeshNode2D node2 = (MeshNode2D) edge.getNodes2();
				assert (mapNodes.containsKey(node1));
				assert (mapNodes.containsKey(node2));
				e[i] = (MEdge2D) mapEdges.get(edge);
				if (edge.isWire())
				{
					assert(null != e[i]);
				}
				else if (null == e[i])
				{
					//  This edge is not a wire and is thus new
					MNode2D n1 = (MNode2D) mapNodes.get(node1);
					MNode2D n2 = (MNode2D) mapNodes.get(node2);
					e[i] = new MEdge2D(n1, n2);
					mapEdges.put(edge, e[i]);
					edgeset.add(e[i]);
				}
				i++;
			}
			MFace2D f = new MFace2D(e[0], e[1], e[2]);
			faceset.add(f);
		}
		assert (submesh2d.isValid());
	}
}
