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
import org.jcae.mesh.*;
import java.util.ArrayList;
import org.jcae.mesh.util.HashSet;
import java.util.Set;
import java.util.Iterator;
import org.apache.log4j.Logger;
/**
 * This class allows edge tessellation.
 * The length of edge discretisation is provided by the constraint hypothesis.s
 * \n
 */
public class RefineEdge
{
	private static Logger logger=Logger.getLogger(RefineEdge.class);
	/** Number of points on edge. */
	private int nbPoints;
	/** An array to stock curvilinear coordinates */
	private double[] paramOnEdge;
	/** Boolean flag to identify degenerated edge. */
	private boolean isDegenerated;
	
	/**
	 * Default constructor.
	 */
	public 	RefineEdge()
	{
	}
	
	/**
	 * Explores each edge of the mesh and calls the discretisation method.
	 * @param mesh : a MeshOfCAD instance, the current mesh.
	 * @param constraint : a MeshConstraint instance, the constraint to apply.
	 */
	public void compute(MeshOfCAD mesh, MeshConstraint constraint)
	{
		/* Explore the shape for each edge and build a map of edges */		
		Iterator ite = mesh.getGeometryIterator();
		int nbedges=0;
		while (ite.hasNext())
		{
			TopoDS_Shape s = (TopoDS_Shape) ite.next();
			if (s.shapeType() == TopAbs_ShapeEnum.EDGE )
			{
				nbedges++;
				MeshOfCAD m = mesh.getMeshFromMapOfSubMesh(s);
				computeEdge(m, constraint);
				m = null;
			}
		}
		logger.info("TopoEdges discretisees "+nbedges);
		logger.info("Edges   "+mesh.numberOfEdges());
		logger.info("Nodes   "+mesh.numberOfNodes());
	}

	/**
	 * Calls the discretisation method for a given edge.
	 * @param edge  the edge to discretize.
	 * @param constraint  the constraint to apply.
	 */
	private void computeEdge(MeshOfCAD edge, MeshConstraint constraint)
	{
		if (edge.getGeometry().shapeType() != TopAbs_ShapeEnum.EDGE)
		{
			logger.error("Not a TopoDS_Edge, cant refine");
			return;
		}
		TopoDS_Edge e = (TopoDS_Edge)edge.getGeometry();
		buildPointsOnEdge(e, constraint.getValue(), edge.isCircular());
		updateMesh(edge);
	}
	
	/**
	 * Fills vertices sub-meshes and HashSets related to the current edge.
	 * As an edge is defined by its two bounded vertices, these vertices
	 * correspond to the first and last node of the discretisation and are
	 * related to existing sub-meshes. Method getMeshFromMapOfSubMesh is
	 * used to retrieve the corresponding sub-mesh.
	 * Other points are added to the nodelist of the current mesh (eg
	 * corresponding to the current edge).
	 * - Particular case of circular edges: first and last vertex are
	 *   identical, so there is only one sub-mesh for both vertices.
	 * @param E : a TopoDS_Edge instance, the current edge.
	 */
	private void updateMesh(MeshOfCAD edge)
	{
		TopoDS_Edge E = (TopoDS_Edge)edge.getGeometry();
		double fl[] = new double[2];
		MeshOfCAD father = edge.getFather();
		TopoDS_Vertex[] v = TopExp.vertices(E);
		TopoDS_Face face = null;
		MeshNode firstNode, n1, n2;
		PST_Position pst;
		double param;

		if (isDegenerated)
		{
			ArrayList faces=new MiscAlgos(father).getTopoFaces(E);
			face = (TopoDS_Face)faces.get(0);
		}

		//  First vertex
		MeshMesh meshPt = father.getMeshFromMapOfSubMesh(v[0]);
		firstNode = new MeshNode(new PST_VertexPosition(v[0]));
		firstNode = meshPt.addNode(firstNode);
		n1 = firstNode;

		//  Intermediate points
		for (int i = 0; i < nbPoints - 2; i++)
		{
			param = paramOnEdge[i+1];
			if (isDegenerated)
				pst = new PST_DegeneratedLinePosition(E,param,null,face);
			else
				pst = new PST_LinePosition(E, param);
			
			n2 = new MeshNode(pst);
			n2 = edge.addNode(n2);
			
			MeshEdge me=new MeshEdge(n1, n2);
			me.setWire(true);
			edge.addEdge(me);
			n1 = n2;
		}

		//  Last vertex
		if (edge.isCircular())
			n2 = firstNode;
		else
		{
			meshPt = father.getMeshFromMapOfSubMesh(v[1]);
			n2 = new MeshNode(new PST_VertexPosition(v[1]));
			n2 = meshPt.addNode(n2);
		}
			
		MeshEdge me=new MeshEdge(n1, n2);
		me.setWire(true);
		edge.addEdge(me);
	}
	
	/** Edge tesselation.
	 * Creates points along the current edge according to the constraint value.
	 * An edge is defined by its two bounded nodes, so whatever the
	 * constraint value, there are at least 2 points of discretisation.
	 * For degenerated edges, we force the discretization by an arbitrary
	 * number of points set to 5.
	 * @param E : a TopoDS_Edge instance, the current edge to tesselate.
	 */
	private void buildPointsOnEdge(TopoDS_Edge E, double discr, boolean isCircular)
	{
		int i;
		double range[] = new double[2];
		
		Geom_Curve curve = BRep_Tool.curve(E, range);
		isDegenerated = false;
		
		if (curve != null)
		{
			GeomAdaptor_Curve C3d = new GeomAdaptor_Curve(curve);
			GCPnts_UniformAbscissa discret = new GCPnts_UniformAbscissa();
			discret.initialize((Adaptor3d_Curve)C3d, discr, range[0], range[1]);
			nbPoints = discret.nbPoints();
			if (nbPoints <= 2 && !isCircular)
				nbPoints=3;
			else if (nbPoints <= 3 && isCircular)
				nbPoints=4; 

			discret.initialize((Adaptor3d_Curve)C3d, nbPoints, range[0], range[1]);
			paramOnEdge = new double[nbPoints];
			for (i = 0; i < nbPoints; i++)
				paramOnEdge[i] = discret.parameter(i+1);
		}
		else
		{
			if (BRep_Tool.degenerated(E))
			{
				// arbitrary number of points on a degenerated edge
				nbPoints = 5;
				paramOnEdge = new double[nbPoints];
				range = BRep_Tool.range(E);
				isDegenerated = true;
				for (i = 0; i < nbPoints; i++)
					paramOnEdge[i] = range[0] + i * (range[1] - range[0]) / (nbPoints - 1);
			}
			else
				logger.info("Problem in edge tesselation");
		}
	}

}
