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
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */


package org.jcae.mesh.algos;
import org.jcae.mesh.sd.*;
import org.jcae.opencascade.jni.*;
import org.jcae.mesh.util.*;
import org.jcae.mesh.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.HashMap;
import java.util.Stack;
import org.apache.log4j.Logger;

/**
 *
 * @author  Jerome Robert
 */
public class MiscAlgos extends TopoHelper
{
	private static Logger logger=Logger.getLogger(MiscAlgos.class);
	private MeshOfCAD myMesh;
	//private MeshOfCAD father;
	private MeshHypothesis hypo;
	/** Creates a new instance of MiscAlgos */
	public MiscAlgos(MeshOfCAD mesh)
	{
		super(mesh.getGeometry());
		myMesh=mesh;
		/*if(mesh instanceof MeshOfCADEntity)
			father=((MeshOfCADEntity)mesh).getFather();
		else father=mesh;*/
	}

	public void removeMidPoints()
	{
		logger.info("Removing mid points");
		Iterator it=myMesh.getEdgesIterator();
		while(it.hasNext()) ((MeshEdge)it.next()).setMidNode(null);
	}
	
	/**
	 * Computes the middle point of two given nodes.
	 * Various cases were taken into account, middle point is compute between:
	 * -# two vertex positions
	 * 	- both vertices are edge bounding nodes, thus the middle point is classified on a line
	 * 	- both vertices are not along the same edge, thus the middle point is classified on a surface
	 * -# two line positions (one can be a degenerated line position)
	 * 	- both vertices are along the same edge and are nearby, thus the middle is classified on a line
	 * 	- both vertices are along the same edge but not nearby, thus the middle is classified on a surface
	 * 	- both vertices are not along the same edge, thus the middle is classified on a surface
	 * -# two degenerated line position
	 * 	- both vertices are along the same degenerated edge and are neraby, thus the middle point is classified on a degenerated line
	 * 	- both vertices are along the same degenerated edge but not neraby, thus the middle point is classified on a surface
	 * 	- both vertices are on different edge, thus the middle point is classified on a surface
	 * -# two surface position
	 * 	- both vertices are on the same surface, thus the middle point is classified on a surface
	 * 	- both vertices are not on the same surface, thus impossible
	 * -# a vertex position and a line position (or degenerated line position)
	 * 	- the vertex is one of the two vertex bounding edge, thus the middle point is classified on a line (or degenerated
	 * line)
	 * 	- the vertex is not on the edge, thus the middle point is classified on a surface
	 * -# a vertex position and a surface position
	 * 	- the vertex is on the surface, thus the middle point is classified on a surface
	 * 	- the vertex is not on the surface, thus impossible
	 * -# a line (or degenerated line) position and a surface position
	 * 	- the line is bounding the surface, thus the middle point is classified on a surface
	 * 	- the line is not bounding the surface, impossible
	 *
	 * @param n1 : a MeshNode instance, one of the two nodes.
	 * @param n2 : a MeshNode instance, the second node
	 * @return MeshNode : the middle node of the two input nodes.
	 */
	public MeshNode createMidPt(MeshNode n1, MeshNode n2)
	{
		TopoDS_Face topoface = null;
		double[] P0, P1;
		MeshNode mid = null;
		
		// Case: VERTEX-VERTEX
		if ((n1.getPosition().getType() == PST_Position.VERTEX) && (n2.getPosition().getType() == PST_Position.VERTEX))
		{
			TopoDS_Edge edge = getTopoEdge(n1, n2);
			//Case: Vertices on edge
			if (edge != null)
			{
				double f, l;
				double[] res = new double[2];
				Geom_Curve curve = BRep_Tool.curve(edge, res);
				if (curve == null) res = BRep_Tool.range(edge);
				f = res[0];
				l = res[1];
				TopoDS_Vertex[] vertices = new TopoDS_Vertex[2];
				TopoDS_Vertex V1, V2;
				vertices = TopExp.vertices(edge);
				V1 = vertices[0];
				V2 = vertices[1];
				if (((((PST_VertexPosition) n1.getPosition()).getVertex()).equals(V1)) && ((((PST_VertexPosition) n2.getPosition()).getVertex()).equals(V2))
				|| ((((PST_VertexPosition) n1.getPosition()).getVertex()).equals(V2)) && ((((PST_VertexPosition) n2.getPosition()).getVertex()).equals(V1)))
				{
					if (V1.equals(V2))
						logger.info("MeshNode - midPt : V1==V2");
					PST_LinePosition posP = new PST_LinePosition(edge, 0.5 * (f + l));
					mid = new MeshNode(posP);
					return mid;
				}
			}
			// Case Vertices on surface
			PST_VertexPosition pos0 = (PST_VertexPosition) n1.getPosition();
			PST_VertexPosition pos1 = (PST_VertexPosition) n2.getPosition();
			P0 = BRep_Tool.pnt((pos0.getVertex()));
			P1 = BRep_Tool.pnt((pos1.getVertex()));
			// Retrieve the face common to both nodes
			topoface = getTopoFace((pos0.getVertex()), (pos1.getVertex()));
			if (topoface == null)
			{
				System.out.println(	"MeshNode - midPt : VERTEX VERTEX pas de surface commune");
				return null;
			}		

			double[] p0 = BRep_Tool.parameters((pos0.getVertex()), topoface);
			double[] p1 = BRep_Tool.parameters((pos1.getVertex()), topoface);
			PST_SurfacePosition posP = new PST_SurfacePosition(topoface,0.5 * (p0[0] + p1[0]),0.5 * (p0[1] + p1[1]));
			mid = new MeshNode(posP);
			return mid;
		}
		// Case: EDGE-EDGE or DEGENERATEDEDGE-EDGE
		else if ( (n1.getPosition().getType() == PST_Position.EDGE)	&& (n2.getPosition().getType() == PST_Position.EDGE)
		||(n1.getPosition().getType() == PST_Position.DEGENERATEDLINE)	&& (n2.getPosition().getType() == PST_Position.EDGE)
		||(n1.getPosition().getType() == PST_Position.EDGE)	&& (n2.getPosition().getType() == PST_Position.DEGENERATEDLINE)	)
		{
			PST_LinePosition pos0 = (PST_LinePosition) n1.getPosition();
			PST_LinePosition pos1 = (PST_LinePosition) n2.getPosition();
			Pair Et0 = pos0.getCurvePosition();
			Pair Et1 = pos1.getCurvePosition();
			// Case nodes on same edge and edge is wire : mid node on edge
			if  ( (((TopoDS_Edge)(Et0.first)).equals((TopoDS_Edge) (Et1.first)))
				&& (myMesh.getEdgeDefinedByNodes(n1, n2).isWire()) )
			{
				PST_LinePosition posP = new PST_LinePosition((TopoDS_Edge) Et0.first,0.5 * (((Double) Et0.second).doubleValue()
				+ ((Double) Et1.second).doubleValue()));
				mid = new MeshNode(posP);
				return mid;
			}
			// case mid node between one node on edge and one node on degeneraed edge connected to edge and edge is wire
			else if ( !(((TopoDS_Edge)(Et0.first)).equals((TopoDS_Edge) (Et1.first)))
				&& (myMesh.getEdgeDefinedByNodes(n1, n2).isWire()))
			{
					TopoDS_Edge myedge = null;
					TopoDS_Edge mydege = null;
					double mydouble = 0.0;
					double[] fl = new double[2];
					TopoDS_Vertex V1, V2, Vd;
					TopoDS_Vertex[] res, resdege;

				if ((n1.getPosition().getType() == PST_Position.EDGE) && (n2.getPosition().getType() == PST_Position.DEGENERATEDLINE))
				{
					myedge = (TopoDS_Edge)Et0.first;
					mydouble = ((Double) Et0.second).doubleValue();
					mydege = (TopoDS_Edge)Et1.first;
				}
				else 
				{
					myedge = (TopoDS_Edge)Et1.first;
					mydouble = ((Double) Et1.second).doubleValue();
					mydege = (TopoDS_Edge)Et0.first;
				}
				res = TopExp.vertices(myedge);
				V1 = res[0];
				V2 = res[1];
				resdege = TopExp.vertices(mydege);
				Vd = resdege[0];
				double f, l;
				Geom_Curve Curve = BRep_Tool.curve(myedge, fl);
				f = fl[0];
				l = fl[1];
					
				if (Vd.equals(V1))
				{
					if (V1.equals(V2))
					{
						System.out.println("MeshNode - modPt : V1==V2");
						if ((mydouble - f) > (l - mydouble))
						f = l;
					}
					PST_LinePosition posP = new PST_LinePosition(myedge, 0.5 * (f + mydouble));
					mid = new MeshNode(posP);
					return mid;
				}
				else if (Vd.equals(V2))
				{
					PST_LinePosition posP = new PST_LinePosition(myedge, 0.5 * (mydouble + l));
					mid = new MeshNode(posP);
					return mid;
				}
			}
			// case mid node on surface
			else
			{// il faut trouver le point milieu sur la surface (et non sur l'edge)
				ArrayList Tfaces = getTopoFaces((TopoDS_Edge) Et0.first,(TopoDS_Edge) Et1.first);
				int index = 0;
				double dist = 0;
				if (Tfaces.size()>1) // choix de la surface
				{
					for (int i = 0; i<Tfaces.size(); i++)
					{
						topoface = (TopoDS_Face)Tfaces.get(i);
						PST_SurfacePosition posP = getMidSurfacePosition(topoface, Et0, Et1);
						MeshNode n = new MeshNode(posP);
						n.setCoord3D();
						if (i==0) dist = n.distance(n1);
						else 
						{
							double li = n.distance(n1);
							if (li<dist)
							{
								index = i;
								dist = li;
							}
						}
					}
				}
				if (Tfaces.isEmpty()) 
				{
					logger.debug("MeshNode - midPt : Edge-Edge, face commune nulle");
					return null;
				}
//					topoface=getTopoFace((TopoDS_Edge) Et0.first,(TopoDS_Edge) Et1.first);
//				else 
				topoface =(TopoDS_Face)Tfaces.get(index); 
			
				PST_SurfacePosition posP = getMidSurfacePosition(topoface, Et0, Et1);
				mid = new MeshNode(posP);
				return mid;
			} 
		}
		// Case: DEGENERATEDEDGE-DEGENERATEDEDGE
		else if ( (n1.getPosition().getType() == PST_Position.DEGENERATEDLINE)	&& (n2.getPosition().getType() == PST_Position.DEGENERATEDLINE))
		{
			PST_DegeneratedLinePosition pos0 = (PST_DegeneratedLinePosition) n1.getPosition();
			PST_DegeneratedLinePosition pos1 = (PST_DegeneratedLinePosition) n2.getPosition();
			Pair Et0 = pos0.getCurvePosition();
			Pair Et1 = pos1.getCurvePosition();
			// case: nodes on same DegeneratedEdge and edge wire : mid node on edge
			if ( (((TopoDS_Edge)(Et0.first)).equals((TopoDS_Edge) (Et1.first)))
				&& (myMesh.getEdgeDefinedByNodes(n1, n2).isWire()) )
			{
				topoface=(TopoDS_Face)pos0.getGeometry();
				PST_DegeneratedLinePosition posP = new PST_DegeneratedLinePosition((TopoDS_Edge) Et0.first,0.5 * (((Double) Et0.second).doubleValue()
				+ ((Double) Et1.second).doubleValue()),null, topoface);
				mid = new MeshNode(posP);
				return mid;
			} 
			else
			{ // mid node on surfae
				// Retrieve the commun face to both edges
				ArrayList Tfaces = getTopoFaces((TopoDS_Edge) Et0.first,(TopoDS_Edge) Et1.first);
				int index = 0;
				double dist = 0;
				if (Tfaces.size()>1) // choix de la surface
				{
					for (int i = 0; i<Tfaces.size(); i++)
					{
						topoface = (TopoDS_Face)Tfaces.get(i);
						PST_SurfacePosition posP = getMidSurfacePosition(topoface, Et0, Et1);
						MeshNode n = new MeshNode(posP);
						n.setCoord3D();
						if (i==0) dist = n.distance(n1);
						else 
						{
							double li = n.distance(n1);
							if (li<dist)
							{
								index = i;
								dist = li;
							}
						}
					}
				}
				if (Tfaces.isEmpty()) 
				{
					logger.debug("MeshNode - midPt : DegeneratedEdge-DegeneratedEdge, face commune nulle");
					return null;
				}
//					topoface=getTopoFace((TopoDS_Edge) Et0.first,(TopoDS_Edge) Et1.first);
//				else 
				topoface =(TopoDS_Face)Tfaces.get(index); 		
				PST_SurfacePosition posP = getMidSurfacePosition(topoface, Et0, Et1);
				mid = new MeshNode(posP);
				return mid;
			}
		}
		// Case: SURFACE-SURFACE
		else if ((n1.getPosition().getType() == PST_Position.SURFACE) && (n2.getPosition().getType() == PST_Position.SURFACE))
		{
			PST_SurfacePosition pos0 = (PST_SurfacePosition) n1.getPosition();
			PST_SurfacePosition pos1 = (PST_SurfacePosition) n2.getPosition();
			if (!((TopoDS_Face)(pos0.getSurface())).equals((TopoDS_Face)(pos1.getSurface())))
			{
				System.out.println("MeshNode - modPt : SURFACE SURFACE S0!=S1");
				return null;
			}
			else
			{
				Pair uv0 = pos0.getSurfacePosition();
				Pair uv1 = pos1.getSurfacePosition();
				float u = (float)(((Double) uv0.first).doubleValue()+ ((Double) uv1.first).doubleValue())* 0.5f;
				float v = (float)(((Double) uv0.second).doubleValue()+ ((Double) uv1.second).doubleValue())* 0.5f;
				PST_SurfacePosition posP = new PST_SurfacePosition((pos1.getSurface()), u, v);
				mid = new MeshNode(posP);
				return mid;
			}
		}
		// Case: VERTEX-EDGE or VERTEX-DEGENERATEDEDGE
		else if (((n1.getPosition().getType() == PST_Position.VERTEX) && (((n2.getPosition()).getType() == PST_Position.EDGE)||(n2.getPosition()).getType() == PST_Position.DEGENERATEDLINE))
		|| (((n1.getPosition().getType() == PST_Position.EDGE)||(n1.getPosition().getType() == PST_Position.DEGENERATEDLINE)) && ((n2.getPosition()).getType() == PST_Position.VERTEX)))
		{
			Pair Et = new Pair();
			double[] resd = new double[2];
			PST_VertexPosition pos = new PST_VertexPosition();
			if ( ( n1.getPosition().getType() == PST_Position.VERTEX) && ( ((n2.getPosition()).getType() == PST_Position.EDGE)||(n2.getPosition()).getType() == PST_Position.DEGENERATEDLINE))
			{
				Et = ((PST_LinePosition) n2.getPosition()).getCurvePosition();
				pos = (PST_VertexPosition) n1.getPosition();
			}
			if (  (((n1.getPosition()).getType() == PST_Position.EDGE)||(n1.getPosition()).getType() == PST_Position.DEGENERATEDLINE)&& ((n2.getPosition()).getType() == PST_Position.VERTEX))
			{
				Et = ((PST_LinePosition) n1.getPosition()).getCurvePosition();
				pos = (PST_VertexPosition) n2.getPosition();

			}
			TopoDS_Edge myedge = (TopoDS_Edge) Et.first;
			double mydouble = ((Double) Et.second).doubleValue();
			if(myMesh.getEdgeDefinedByNodes(n1, n2).isWire())	//MH pas sur ca
			{
				// Test if the node is a vertex of the edge
				TopoDS_Vertex V1, V2;
				TopoDS_Vertex[] res;
				res = TopExp.vertices(myedge);
				V1 = res[0];
				V2 = res[1];
				if ((pos.getVertex()).equals(V1))
				{
					double f, l;
					Geom_Curve Curve = BRep_Tool.curve(myedge, resd);
					if (BRep_Tool.degenerated(myedge))
						resd = BRep_Tool.range(myedge);
					f = resd[0];
					l = resd[1];
					if (V1.equals(V2))
					{
						System.out.println("MeshNode - modPt : V1==V2");
						if ((mydouble - f) > (l - mydouble))
							f = l;
					}
					PST_Position posP=null;
					posP = new PST_LinePosition(myedge, 0.5 * (f + mydouble));
					mid = new MeshNode(posP);
					return mid;
				}
				else if (pos.getVertex().equals(V2))
				{
					double f, l;
					Geom_Curve Curve = BRep_Tool.curve(myedge, resd);
					if (BRep_Tool.degenerated(myedge))
						resd = BRep_Tool.range(myedge);
					f = resd[0];
					l = resd[1];
					PST_LinePosition posP = new PST_LinePosition(myedge, 0.5 * (mydouble + l));
					mid = new MeshNode(posP);
					return mid;
				}
			}
			else
			{ // the vertex is not on the same edge or the edge does not lie the TopoDS_Edge: mid node on surface
				double f, l;
				ArrayList Tfaces = getTopoFaces(pos.getVertex(), myedge);
				int index = 0;
				double dist = 0;
				if (Tfaces.size()>1) // choix de la surface
				{
					for (int i = 0; i<Tfaces.size(); i++)
					{
						topoface = (TopoDS_Face)Tfaces.get(i);
						PST_SurfacePosition posP = getMidSurfacePosition(topoface, pos.getVertex(), Et);
						MeshNode n = new MeshNode(posP);
						n.setCoord3D();
						if (i==0) dist = n.distance(n1);
						else 
						{
							double li = n.distance(n1);
							if (li<dist)
							{
								index = i;
								dist = li;
							}
						}
					}
				}
//				if (Tfaces.isEmpty()) 	
//					topoface=getTopoFace(pos.getVertex(), myedge);
//				else 
				
				if (Tfaces.isEmpty())
				{
					System.out.println("MeshNode - midPt : VERTEX EDGE, not on the same edge, face commune nulle");
					return null;
				}

				topoface =(TopoDS_Face)Tfaces.get(index); 
				PST_SurfacePosition posP = getMidSurfacePosition(topoface, pos.getVertex(), Et);
				mid = new MeshNode(posP);
				return mid;
			}
		}
		// Case : VERTEX-SURFACE
		else if (((n1.getPosition().getType() == PST_Position.VERTEX) && ((n2.getPosition()).getType() == PST_Position.SURFACE))
		|| ((n1.getPosition().getType() == PST_Position.SURFACE) && ((n2.getPosition()).getType() == PST_Position.VERTEX)))
		{
			//return null;
			PST_SurfacePosition posS;
			PST_VertexPosition posV;
			Pair uv = null;
			double resd[];
			if ((n1.getPosition().getType() == PST_Position.VERTEX) && ((n2.getPosition()).getType() == PST_Position.SURFACE))
			{
				posS = (PST_SurfacePosition) (n2.getPosition());
				posV = (PST_VertexPosition) (n1.getPosition());
			} else
			{
				posS = (PST_SurfacePosition) n1.getPosition();
				posV = (PST_VertexPosition) (n2.getPosition());
			}
			topoface = posS.getSurface();
			// le vertex est il sur cette surface ?( oui, normalement toujours: test inutile)
			boolean trouve = false;
			HashMap ancestors = TopExp.mapShapesAndAncestors
				(myMesh.getGeometry(),TopAbs_ShapeEnum.VERTEX,TopAbs_ShapeEnum.FACE);
			ArrayList tfaces = (ArrayList) ancestors.get((posV.getVertex()));
			Iterator it = tfaces.iterator();
			while (it.hasNext())
			{
				if (topoface.equals((TopoDS_Face) it.next()))
					trouve = true;
			}
			if (trouve == false)
			{
				System.out.println("MeshNode - midPt : VERTEX SURFACE pas de surface commune");
				return null;
			}
			double[] p = BRep_Tool.parameters((posV.getVertex()), topoface);
			uv = posS.getSurfacePosition();
			double u = (((Double) uv.first).doubleValue() + p[0]) * 0.5;
			double v = (((Double) uv.second).doubleValue() + p[1]) * 0.5;
			PST_SurfacePosition posP = new PST_SurfacePosition(topoface, u, v);
			mid = new MeshNode(posP);
			return mid;
		}
		// Case EDGE-SURFACE or DEGENERATED-SURFACE
		else if ((    ((n1.getPosition().getType() == PST_Position.EDGE) ||(n1.getPosition().getType() == PST_Position.DEGENERATEDLINE))    && ((n2.getPosition()).getType() == PST_Position.SURFACE))
		|| ((n1.getPosition().getType() == PST_Position.SURFACE)&&  (((n2.getPosition()).getType() == PST_Position.EDGE)||((n2.getPosition()).getType() == PST_Position.DEGENERATEDLINE))   ))
		{
			Pair Et = null;
			PST_SurfacePosition pos;
			double[] resd = new double[2];
			Pair uv = null;
			double f, l;
			if (((n1.getPosition().getType() == PST_Position.EDGE)||(n1.getPosition().getType() == PST_Position.DEGENERATEDLINE))	&& ((n2.getPosition()).getType() == PST_Position.SURFACE))
			{
				pos = (PST_SurfacePosition) n2.getPosition();
				Et = ((PST_LinePosition) n1.getPosition()).getCurvePosition();
			} 
			else
			{
				pos = (PST_SurfacePosition) n1.getPosition();
				Et = ((PST_LinePosition) (n2.getPosition())).getCurvePosition();
			}
			topoface = pos.getSurface();
			TopoDS_Edge E=(TopoDS_Edge) Et.first;
			
			Geom2d_Curve Curve = BRep_Tool.curveOnSurface((TopoDS_Edge) Et.first,topoface,resd);
			if (Curve != null)
			{
				Geom2dAdaptor_Curve C2d = new Geom2dAdaptor_Curve(Curve);
				double[] p = C2d.value(((Double) Et.second).floatValue());
				uv = pos.getSurfacePosition();
				double u = (((Double) uv.first).doubleValue() + p[0]) * 0.5;
				double v = (((Double) uv.second).doubleValue() + p[1]) * 0.5;
				PST_SurfacePosition posP = new PST_SurfacePosition((pos.getSurface()), u, v);
				mid = new MeshNode(posP);
				return mid;
			}
		}
		// case PST_3DPOSITION
		else if ((n1.getPosition().getType() == PST_Position.UNDEFINED) || (n2.getPosition().getType() == PST_Position.UNDEFINED))
		{
			mid = n1.middle(n2);
			PST_3DPosition pos = new PST_3DPosition(mid.getX(),mid.getY(),mid.getZ());
			mid.setPosition(pos);
			return mid;
		}
		logger.info("Cas mal traite..........!");
		// Cas restants:  ??
		Geom_Surface S = BRep_Tool.surface(topoface);
		mid = n1.middle(n2);
		//		mid.set2DFlag((n1.is2D && n2.is2D));
		// n1.getUV !!!!!!
		PST_SurfacePosition posi = new PST_SurfacePosition(topoface,0.5 * (n1.getX() + n2.getX()),0.5 * (n1.getY() + n2.getY()));
		mid.setPosition(posi);
		return mid;
	}
	
	/**
	 * private method to compute the PST_SurfacePosition of a node located at the middle of an edge defined by its two ends.
	 * Those ends are located on PST_LinePosition. 
	 * @param topoface : the TopoDS_Face where the mid node should be located on.
	 * @param Et0, Et1 : two Pairs (TopoDS_Edge,param) defining the PST_LinePosition of edge ends.
	 * @return PST_SurfacePosition : the mid node position on the topoface.
	 */
	private PST_SurfacePosition getMidSurfacePosition(TopoDS_Face topoface, Pair Et0, Pair Et1 )
	{
		double[] res1 = new double[2];
		double[] res2 = new double[2];
		double f, l;
		double r, t;
		Geom2d_Curve Curve0 = BRep_Tool.curveOnSurface((TopoDS_Edge) Et0.first,topoface,res1);
		f = res1[0];
		l = res1[1];
		Geom2d_Curve Curve1 = BRep_Tool.curveOnSurface((TopoDS_Edge) Et1.first,topoface,res2);
		r = res2[0];
		t = res2[1];
		Geom2dAdaptor_Curve C2d0 = new Geom2dAdaptor_Curve(Curve0);
		Geom2dAdaptor_Curve C2d1 = new Geom2dAdaptor_Curve(Curve1);
		double[] p0 = C2d0.value(((Double) Et0.second).floatValue());
		double[] p1 = C2d1.value(((Double) Et1.second).floatValue());
		double u = 0.5 * (p0[0] + p1[0]);
		double v = 0.5 * (p0[1] + p1[1]);
		PST_SurfacePosition posP = new PST_SurfacePosition(topoface, u, v);
		return posP;	
	}

	private PST_SurfacePosition getMidSurfacePosition(TopoDS_Face topoface, TopoDS_Vertex vertex, Pair Et )
	{
		TopoDS_Edge myedge = (TopoDS_Edge) Et.first;
		double mydouble = ((Double) Et.second).doubleValue();

		double[] resd = new double[2];
		Geom2d_Curve Curve2d = BRep_Tool.curveOnSurface(myedge, topoface, resd);
		if (Curve2d == null)
			System.out.println("MeshNode - midPt : curve null");
		Geom2dAdaptor_Curve C2d = new Geom2dAdaptor_Curve(Curve2d);
		double[] pE = C2d.value((float) mydouble);
		double[] pV = BRep_Tool.parameters(vertex, topoface);
		double u = 0.5 * (pE[0] + pV[0]);
		double v = 0.5 * (pE[1] + pV[1]);
		PST_SurfacePosition posP = new PST_SurfacePosition(topoface, u, v);
		return posP;
	}
	
	/**
	 * The collapsing operator.
	 * Method used to merge two nodes of a same edge.
	 * Topological condition: both nodes, defining the edge are not classified on a vertex.
	 * @param na - a MeshNode entity defining one of both edge ends
	 * @param nb - a MeshNode entity defining the other edge end.
	 * @return boolean - set to @c true if the method is succesfull, @c false if not.
	 */
	public boolean collapse(MeshNode na, MeshNode nb)
	{
		// test if edge defined by both node exists
		MeshEdge edge = myMesh.getEdgeDefinedByNodes(na,nb);
		if (edge==null)
		{
			logger.warn("collapse: edge does not exist");
			return false;
		}
		return collapse(edge);
	}
	
	public boolean collapse(MeshEdge edge)
	{
		MeshNode na = edge.getNodes1();
		MeshNode nb = edge.getNodes2();

		/* TODO:
		 * This test should be a AND clause, which means that
		 * an edge can be contracted even if one of its vertices
		 * is a boundary node.  But edges are destaoryed and
		 * reconstructed by the algorithm below, and boundary
		 * is not preserved.  Until this problem is fixed,
		 * such edges are not contracted.
		 */
		if ( (na.getPosition().getType()!=PST_Position.SURFACE) ||
		     (nb.getPosition().getType()!=PST_Position.SURFACE))
			return false;

		if (!(edge.canContract()))
			return false;

		// retrieve faces linked to the edge
		HashSet faces = edge.getFaces();
		if (faces.size()!=2)
		{
			logger.warn("edge non connectee a deux faces");
			return false;
		}
		
		// Save faces connected to na or nb in a HashSet
		HashSet facesConnected = new HashSet();
		facesConnected.addAll(na.getElements());
		facesConnected.addAll(nb.getElements());

		// Retrieve the edges connected to na or nb
		HashSet edgesConnected = new HashSet();
		// Retrieve the edges not connected to na or nb
		HashSet edgesNotConnected = new HashSet();
		Iterator itfc = facesConnected.iterator();
		while(itfc.hasNext())
		{
			MeshElement e = (MeshElement)itfc.next();
			if (e.getType() == MeshElement.FACE)
			{
				Iterator ittemp = ((MeshFace)e).getEdgesIterator();
				while (ittemp.hasNext())
				{
					MeshEdge ed = (MeshEdge)ittemp.next();
					if (!(ed.getNodes1().equals(na))
						&& !(ed.getNodes1().equals(nb))
						&& !(ed.getNodes2().equals(na))
						&& !(ed.getNodes2().equals(nb)))
					{
						edgesNotConnected.add(ed);
					}
					else edgesConnected.add(ed);
				}
			}
		}
		
		// Computes new point
		MeshNode np;
		
		// if a node is not interior, it must be kept
		if (na.getPosition().getType()!=PST_Position.SURFACE)
			np = na;
		else if (nb.getPosition().getType()!=PST_Position.SURFACE)
			np = nb;
		// Other case
		else
		{
			np = createMidPt(na, nb);
			np.setCoord3D();
		}

		// Destroy connected faces
		myMesh.rmFaces(facesConnected);
		
		// Destroy connected edges
		myMesh.rmEdges(edgesConnected);
		
		np = myMesh.addNode(np);
		
		// Construct new faces
		Iterator ite = edgesNotConnected.iterator();
		while (ite.hasNext())
		{
			MeshEdge e = (MeshEdge)ite.next();
			myMesh.addNode(e.getNodes1());
			myMesh.addNode(e.getNodes2());
			myMesh.addTriangle(e.getNodes1(),np,e.getNodes2());
		}
		return true;
		
	}
	
	public void reduceNodeConnection()
	{
		int nbEdges = 0;		
		Iterator it = myMesh.getNodesIterator();
		while (it.hasNext())
		{
			MeshNode n = (MeshNode)it.next();
			while (n.getPosition().getType() != PST_Position.SURFACE)
			{
				if (it.hasNext()) n = (MeshNode)it.next();
				else break;
			}
			if (n.getPosition().getType() != PST_Position.SURFACE)
			{
				break;
			}
			nbEdges = n.numberOfEdges();
			if (nbEdges > 6)
			{
				Collection voisins = n.getNeighboursNodes();				
				// recherche du point ayant le moins d'edges connectes
				int min = 100;
				MeshNode bestv = null;
				Iterator itv = voisins.iterator();
				while (itv.hasNext())
				{
					MeshNode node = (MeshNode)itv.next();
					if ( (node.numberOfEdges()<min) &&
						(node.getPosition().getType() == PST_Position.SURFACE) )
					{
						bestv = node;
						min = bestv.numberOfEdges();
					}
				}
				//Classement des voisins
				ArrayList nodeslist = new ArrayList(nbEdges);
				HashSet facestraitees = new HashSet();
				MeshFace face0 = null;
				nodeslist.add(0,bestv);
				logger.info("best: "+bestv);
				for (int i = 1; i < nbEdges; i++)
				{
					//nodeslist.add(i,bestv);
					MeshEdge e = myMesh.getEdgeDefinedByNodes(n,(MeshNode)nodeslist.get(i-1));
					Iterator itf = e.getFaces().iterator();
					if ( itf.hasNext() )
					{
						face0 = (MeshFace)itf.next();
						if (facestraitees.contains(face0)) face0 = (MeshFace)itf.next();
						facestraitees.add(face0);
					}
					MeshNode node0 = face0.apex(e);
					nodeslist.add(i,node0);
				}
				
				// destruction des faces
				HashSet faces = n.getFaces();
				Iterator itf = faces.iterator();
				while (itf.hasNext() )
				{
					MeshFace face = (MeshFace)itf.next();
					myMesh.rmFace(face);
					faces = n.getFaces();
					itf = faces.iterator();
				}
				//rmNode(n);
				// Constructions des nouvelles faces
				for (int i = 1; i < nodeslist.size()-1; i++)
				{
					myMesh.addTriangle(
						(MeshNode)nodeslist.get(0),
						(MeshNode)nodeslist.get(i),
						(MeshNode)nodeslist.get(i+1));
				}
				it = myMesh.getNodesIterator();
			}
		}
	}
	
	/**
	 * Method improveConnect.
	 * Every edge is scanned for a potential swapping.
	 * The ideal connectivity is : 6 edges around a node.
	 * Method: let alpha_i the material angle around a node n_i, e_i the number on edges connected to n_i,
	 * we introduce the angular connectivity : connect_i = e_i * (alpha_i/(2*PI)) should be equal to 6
	 * If swapping improve the connection for every nodes around an edge it can be automatically applied.
	 */
	public void improveConnect()
	{
		// check et swap sur toutes les edges		
		Iterator itc = myMesh.getEdgesIterator();
		while (itc.hasNext())
		{
			// edge to be cheked
			MeshEdge e = (MeshEdge) itc.next();
			
			// swap forbidden if the edge is qualified wire or frozen
			if (e.isWire() || e.isFrozen()) return;
			
			// retrive the 2 faces bounding e
			HashSet trianglelist = e.getFaces();
			if (trianglelist.size() != 2){
				logger.error("Swap impossible: the edge is not bounding two faces ");
				return;
			}
			
			MeshFace triangle = null, adj_triangle = null;
			Iterator it = trianglelist.iterator();
			triangle = (MeshFace) it.next();
			adj_triangle = (MeshFace) it.next();
			MeshNode nc = triangle.apex(e);
			MeshNode nd = adj_triangle.apex(e);
			if ( (nc==null)|| (nd == null) ) {
				logger.warn("Apex null");
				return;
			}
			// check if the new edge in case of swap already exists
			MeshEdge eSwapped = myMesh.getEdgeDefinedByNodes(nc, nd);
			if (eSwapped != null) return;
			

			// compute the angular connectivity
			double I = angularConnectivity(e);
			logger.info("I= "+I);
			if ( I > 6 ) {
				// construction provisoire de l'edge swappee + faces
/*				eSwapped = new MeshEdge(nc,nd);
				eSwapped = addEdge(eSwapped);
				MeshEdge e1 = getEdgeDefinedByNodes(nc,	e.getNodes1());
				MeshEdge e2 = getEdgeDefinedByNodes(nc,	e.getNodes2());
				MeshEdge e3 = getEdgeDefinedByNodes(e.getNodes1(), nd);
				MeshEdge e4 = getEdgeDefinedByNodes(e.getNodes2(), nd);
				HashSet list = triangle.getElements();
				HashSet adj_list = adj_triangle.getElements();
				 rmFace(triangle); rmFace(adj_triangle);
				triangle.clearList();
				adj_triangle.clearList();
				// construction des nouvelles faces
				e1 = triangle.addEdge(e1);
				e3 = triangle.addEdge(e3);
				eSwapped = triangle.addEdge(eSwapped);
				e2 = adj_triangle.addEdge(e2);
				e4 = adj_triangle.addEdge(e4);
				eSwapped = adj_triangle.addEdge(eSwapped);
				//link
				triangle = (MeshFace)addFace(triangle);
				adj_triangle = (MeshFace)addFace(adj_triangle);
				addElement(triangle);
				addElement(adj_triangle);
*/
				// On swap afin de calculer l'angular connectivity
				double Iswapped = 0.0;
				boolean swap = swapEdge(e);
				if (swap) {
					eSwapped = myMesh.getEdgeDefinedByNodes(nc, nd);
					Iswapped = angularConnectivity(eSwapped);
					logger.info("I(original) = "+I+" I(siSwapped) = "+Iswapped);
					if (I <= Iswapped) swapEdge(eSwapped); //on remet comme avant
					else logger.info("Edge Swapped pour ImproveConnect");
				}

				// on remet tout en place
/*				rmEdge(	eSwapped );
				e1 = triangle.addEdge(e1);
				e3 = triangle.addEdge(e3);
				e = triangle.addEdge(e);
				e2 = adj_triangle.addEdge(e2);
				e4 = adj_triangle.addEdge(e4);
				e = adj_triangle.addEdge(e);
				//link
				triangle = (MeshFace)addFace(triangle);
				adj_triangle = (MeshFace)addFace(adj_triangle);
				addElement(triangle);
				addElement(adj_triangle);
				
				if (I > toto) swapEdge(e);
*/			}
				
			// Condition for check further
			//else  return;
			
		}
	}
	
	/**
	 * Method angularConnectivity.
	 * let alpha_i the material angle around a node n_i, e_i the number on edges connected to n_i,
	 * we introduce the angular connectivity : connect_i = e_i * (alpha_i/(2*PI))
	 * @return double - the maximum value between the angular connectivity of both endpoints.
	 *
	 */
	private double angularConnectivity(MeshEdge edge0)
	{
		double connect_a=0., connect_b=0., I=0.;
		double alpha;
		double connect1 = 0.0, connect2 = 0.0;
		MeshNode node = null;
		MeshNode n1 = null, n2 = null;
		MeshEdge ed=null;
		MeshFace face = null;
		int test = 0;
		
		for (int i = 1; i<=2; i++) {
			// initialisation
			MeshEdge edge = null;
			alpha = 0.0;
			test = 0;
			
			if (i==1)
			{
				n1 = edge0.getNodes1();
				n2 = edge0.getNodes2();
			}
			else
			{
				n1 = edge0.getNodes2();
				n2 = edge0.getNodes1();
				face = null;
				test = 0;
			}
	
			// iteration sur toutes les faces
			HashSet faces;
			while ( (edge==null) || (!edge.equals(edge0) && !edge.isWire()) )
			{
				// cas premiere face: on prend la premiere trouvee
				if (face == null) {
					faces = edge0.getFaces();
					if (faces.size() !=0 ) face = (MeshFace) faces.iterator().next();
					else return 0.;
					edge = edge0;
				}
				// autre cas: recherche de la face suivante
				else {
					faces = edge.getFaces();
					Iterator it = faces.iterator();
					if (it.hasNext()) {
						MeshFace f = (MeshFace)it.next();
						if (f.equals(face)) f = (MeshFace)it.next();
						face = f;
					}
				}
				node = face.apex(edge);
				if (edge.getNodes1().equals(n1)) n2 = edge.getNodes2();
				else n2 = edge.getNodes1();
				alpha += n1.angle(node, n2);
				test++;
	/**
				HashSet edges = face.getEdges();
				Iterator ite = edges.iterator();
				while (ite.hasNext()) {
					ed = (MeshEdge) ite.next();
					while ( ( (ed.equals(edge)) || (!(ed.getNodes()).contains(n1)) ) && ite.hasNext() )
						ed = (MeshEdge) ite.next();
				}
				edge = ed;
*/
  				edge = myMesh.getEdgeDefinedByNodes(n1, node);
			}

			// cas ou l'on n'a pas parcouru toutes les edges
			face = null;
			if (edge.isWire()) {
				//edge = null;
				if (face == null) {
					faces = edge0.getFaces();
					Iterator it = faces.iterator();
					if (faces.size() == 2) {
						face = (MeshFace) it.next();
						face = (MeshFace) it.next();
					}
					else return 0; // non manifold
					edge = edge0;
				}
/*				else {
					faces = edge.getFaces();
					Iterator it = faces.iterator();
					if (it.hasNext()) {
						MeshFace f = (MeshFace)it.next();
						if (f.equals(face)) f = (MeshFace)it.next();
						face = f;
					}
				}
*/
				while (!edge.isWire()) {
					node = face.apex(edge);
					if (edge.getNodes1().equals(n1)) n2 = edge.getNodes2();
					else n2 = edge.getNodes1();
					alpha += n1.angle(node, n2);
					test ++;
  					edge = myMesh.getEdgeDefinedByNodes(n1, node);
  					// face suivante
  					faces = edge.getFaces();
					Iterator it = faces.iterator();
					if (it.hasNext()) {
						MeshFace f = (MeshFace)it.next();
						if (f.equals(face)) f = (MeshFace)it.next();
						face = f;
					}
				}
			}

			if (i==1) {
				connect_a = ( alpha/(2*Math.PI) ) * n1.numberOfEdges();
				// verification
				logger.info("nb d'angles calcules: "+test+" sur "+n1.numberOfEdges());
			}
			else if (i==2) {
				connect_b = ( alpha/(2*Math.PI) ) * n1.numberOfEdges();
				// verification
				logger.info("nb d'angles calcules: "+test+" sur "+n2.numberOfEdges());
			}
		}
		I = Math.max(connect_a, connect_b);
		return I;
	}

	public void targetSizeConvergence(double minlen, double maxlen)
	{
		boolean redo = false;
		int count;

		do
		{
			HashSet edges = getEdgesSupTo(maxlen);
			redo = refine(edges);
			edges = getEdgesInfTo(minlen);
			count = 0;
			Iterator itc = edges.iterator();
			while (itc.hasNext())
			{
				MeshEdge e = (MeshEdge)itc.next();
				// The collapse method removes edges, so we must ensure that e is still valid
				if (!e.isWire() && collapse(e.getNodes1(), e.getNodes2()))
				{
					redo = true;
					count++;
				}
			}
			logger.debug("Nb collapsed edges: "+count);
		}
		while (redo);
	}
	
	/**
	 * Method used to identify from all edges entities of the under focus mesh which of them bound more
	 * than two face entities (edges which defined non-manifold area), or only one face(edges used to
	 * bound open surfaces).
	 * @return HashSet - the list of edges not connected twice
	 */
	public HashSet edgesNotConnectedTwice()
	{
		HashSet edgesNCT = new HashSet();		
		Iterator it = myMesh.getEdgesIterator();
		while(it.hasNext())
		{
			MeshEdge e = (MeshEdge) it.next();
			if (e.getFaces().size() != 2) edgesNCT.add(e);
		}
		return edgesNCT;
	}

    /**
	 * Method detect a closed contour giving a selected edge
	 * In case of multiple contours, just one is retreive.
	 * @param freeEdge - a free edge belonging to the contour to retrieve
	 * @return a closed contour containing the edge passed in parameter
	 */
	public static HashSet getFreeContour(MeshEdge freeEdge)
	{
		HashSet freeContour = new HashSet();
		
		// The selected edge cannot have ends nodes belonging to more tham one contour.
		if ( (freeEdge.getNodes1().getFreeEdges().size() > 2) && (freeEdge.getNodes2().getFreeEdges().size() > 2) )
		{
			logger.info("Contour detection confused, select another edge");
			return freeContour;
		}
			
		// Choice of first node
		MeshNode n = freeEdge.getNodes1();
		MeshNode n0 = freeEdge.getNodes2();
		if (n.getFreeEdges().size() > 2)
		{
			n0 = n;
			n = freeEdge.getNodes2();
		}
		 
		Stack contour = new Stack();
		Stack tocheck = new Stack();
		
		if (!freeEdge.isWire()) freeEdge.setWire(true);
		contour.push(freeEdge);
	
		MeshEdge edge = freeEdge;
		HashSet freeEdgesOfn = null;
		
		while ((edge==freeEdge) || (!edge.getNodes().contains(n0)))
		{
			freeEdgesOfn = n.getFreeEdges();
			freeEdgesOfn.remove(edge);
			// a single contour
			if (freeEdgesOfn.size() == 1)
			{
				edge = (MeshEdge)freeEdgesOfn.iterator().next();
			}
			// detection of multi contours attached to n
			if (freeEdgesOfn.size() > 1)
			{
				tocheck.addAll(freeEdgesOfn);
				MeshNode node0 = n;
				while (!tocheck.isEmpty())
				{
					MeshEdge tempedge = (MeshEdge)tocheck.peek();
					HashSet tempcontour = getFreeContour(tempedge);
					if ( tempcontour.contains(freeEdge) )
					{
						logger.debug("c'est la bonne");
						edge = tempedge;
						tocheck.clear();
					}
					else
					{
						tocheck.pop();
						HashSet topop = new HashSet(tocheck);
						topop.retainAll(tempcontour);
						tocheck.removeAll(topop);
						// pour accelerer
						if (tocheck.size()==1)
							edge = (MeshEdge)tocheck.pop();
					}
				}
			}
			
			if (!edge.isWire()) edge.setWire(true);
			contour.push(edge);
			
			// find next node to continue from
			if (edge.getNodes1().equals(n))
				n = edge.getNodes2();
			else
				n = edge.getNodes1();
		}
		freeContour.addAll(contour);
		return freeContour;
	}

	/**
	 * Fills a close contour, and create a sub-mesh.
	 * Topological algorithm: The contour is closed and defined by an oriented list of nodes (LN).
	 * The node connected to the fewer number of edges is selected, and its next and previous nodes.
	 * A face is built to connect these three nodes.
	 * The contour is updated without the initial selected node, and the process is repeated until
	 * three nodes remain in the contour.
	 * @param edges - an ArrayList of edges describing the topologic closed contour of the submesh
	 * @return a sub-mesh - a MeshMesh entity.
	 */
	public void fillContour(ArrayList edges)
	{		
		//MeshMesh sm = getMeshFromMapOfSubMesh(shape);
		switch(edges.size())
		{
			case 0 :
			case 1 :
			case 2 :
				logger.info("et on fait quoi la ?");
				break;
			case 3 :
				myMesh.addTriangle(
					(MeshEdge)edges.get(0),
					(MeshEdge)edges.get(1),
					(MeshEdge)edges.get(2));				
				break;
			default :
				// creation d'une liste ordonnee de nodes
				ArrayList LN = new ArrayList(edges.size());
				LN.add(0,((MeshEdge)edges.get(0)).getNodes1());
				LN.add(1,((MeshEdge)edges.get(0)).getNodes2());
				int k = 1;
				while (k < edges.size()-1) 
				{
					for (int j = 1; j< edges.size(); j++) 
					{
						MeshEdge e = (MeshEdge)edges.get(j);
						if (e.getNodes().contains(LN.get(k)) ) 
						{
							if ( (e.getNodes1().equals(LN.get(k))) && (!LN.contains(e.getNodes2())) )
								LN.add(k+1,e.getNodes2());
							else if ( (e.getNodes2().equals(LN.get(k))) && (!LN.contains(e.getNodes1())) )
								LN.add(k+1,e.getNodes1());
							else k--;
							k++;
							if (k == edges.size()-1) 
							{
								//j = edges.size();
								break;
							}
						}
						else if (j == (edges.size()-1)) j=0;
					}
				}
				// Contour fulfilment (iterative process)
				while (LN.size()>=3)
				{
					MeshNode nt1 = null;
					MeshNode nt2 = null;
					MeshNode nt3 = null;
					//recherche du node de connectivite max
					HashSet badindices = new HashSet();
					boolean trouve=false;
					while (!trouve)
					{
						int connec = 0;
						int indice = 0;
						for (int i = 0; i<LN.size(); i++)
						{
							if (!badindices.contains(new Integer(i)))
							{
								if ( ((MeshNode)LN.get(i)).getFaces().size() > connec)
								{
									connec = ((MeshNode)LN.get(i)).getFaces().size();
									indice = i;
								}
							}
						}
						nt1 = (MeshNode)LN.get(indice);
						if (indice == 0)
							nt2 = (MeshNode)LN.get(LN.size()-1);
						else
							nt2 = (MeshNode)LN.get(indice-1);
						if (indice == LN.size()-1 )
							nt3 = (MeshNode)LN.get(0);
						else
							nt3 = (MeshNode)LN.get(indice+1);

						// Condition to create a face:
						double angle = Calculs.angleVect(nt1, nt2, nt3);
						if ( angle == 0. || angle >= Math.PI)
						{
							badindices.add(new Integer(indice));
						}
						else
						{
							trouve = true;
							badindices.clear();
						}
					}
					
					// Creation de la face
					MeshEdge e0 = null;
					MeshEdge e1 = null;
					MeshEdge e2 = null;
					nt1 = myMesh.addNode(nt1);
					nt2 = myMesh.addNode(nt2);
					nt3 = myMesh.addNode(nt3);
					MeshFace f=myMesh.addTriangle(nt1, nt2, nt3);

					// swap edge if possible
					Iterator itEdges=f.getEdgesIterator();
					while(itEdges.hasNext())
						check((MeshEdge)itEdges.next(), false);
					
					// remove nt1 from the list LN
					LN.remove(nt1);
				} //while (LN.size()>=3)
				break ;
		}
	}
	
	/**
	 * return a list of edges oriented if the set of edges represent a single contour
	 * This contour can be open or closed.
	 * If the edges cannot form a single contourm this method return null.
	 */
	public static ArrayList isSingleContour(HashSet edges)
	{
		ArrayList contour = null;
		if (edges.size() <= 2)
			return contour;
		
		// detection of contour ends and contour jonction
		HashSet extrems = new HashSet();
		HashSet multi = new HashSet();
		HashSet nodes = new HashSet();
		Iterator it = edges.iterator();
		while (it.hasNext())
			nodes.addAll(((MeshEdge)it.next()).getNodes());

		it = nodes.iterator();
		while (it.hasNext())
		{
			MeshNode node = (MeshNode)it.next();
			Collection nedges = node.getEdges();
			nedges.retainAll(edges);
			if (nedges.size() < 2)
				extrems.addFast(node);
			else if (nedges.size() > 2)
				multi.addFast(node);
		}

		// First edge
		MeshNode n0 = null;
		MeshEdge e0 = null;
		MeshNode n = null;
		if (extrems.isEmpty() && nodes.size()==edges.size() ) // closed contour
		{
			e0 = (MeshEdge)edges.iterator().next();
			n0 = e0.getNodes2();
			n = e0.getNodes1();
		}
		if (extrems.size()==2 && nodes.size()==edges.size()+1 ) // open contour
		{
			n0 = (MeshNode)extrems.iterator().next();
			Collection nedges = n0.getEdges();
			nedges.retainAll(edges);
			e0 = (MeshEdge)nedges.iterator().next();
			if (e0.getNodes1().equals(n0))
				n = e0.getNodes2();
			else
				n = e0.getNodes1();

		}
		// all other cases are not single contour:
		if (n0==null) return contour;
		
		// orientation du contour
		contour = new ArrayList(edges.size());
		// first element
		contour.add(0,e0);
		int i = 1;
		// recherche de l'edge suivante contenant n
		while (contour.size() != edges.size() )
		{
			Iterator itc = edges.iterator();
			while (itc.hasNext() )
			{
				MeshEdge e = (MeshEdge)itc.next();
				if (!contour.contains(e))
				{
					if (e.getNodes1().equals(n))
					{
						contour.add(i,e);
						n = e.getNodes2();
						i++;
					}
					else if (e.getNodes2().equals(n))
					{
						contour.add(i,e);
						n = e.getNodes1();
						i++;
					}
				}
			}
		}		
		return contour;
	}
	
		
	/**
	 * Method to test if a contour is closed
	 * @param contour - an oriented arrayList computed with #isSingleContour.
	 * @return boolean - set to true if the contour is closed, false if not.
	 * @see #isSingleContour
	 */
	public static boolean contourIsClosed(ArrayList contour)
	{
		if (contour.isEmpty()) return false;
		if (contour.size() <= 2) return false;
	
		// the contour is closed if the first and last edges have one node in commun
		HashSet nodes1 = ((MeshEdge)contour.get(0)).getNodes();
		HashSet nodesN = ((MeshEdge)contour.get(contour.size()-1)).getNodes();
		nodes1.retainAll(nodesN);
		if (nodes1.isEmpty())
			return false;
		
		return true;
	}

	/** Tests if an given edge can be swapped, and swap it if possible.
	 *  Non iterative method.
	 *  Conditions to swap an edge:
	 *  - the edge is not classified on a wire, neither frozen.
	 *  - the edge is bounding two faces
	 *  - the swapping operation improves the triangle quality.
	 * @param e : a MeshEdge instance, the edge to try to swap.
	 * @return boolean : set to \c false if the edge cannot be swapped.
	 */
	public boolean check(MeshEdge e, boolean normales)
	{
		if (e.isWire()/* || e.isFrozen()*/)
		{
			//logger.warn("wire ou frozen");
			return false;
		}
		MeshFace triangle = null, adj_tri = null;
		HashSet trianglelist = e.getFaces();
		if (trianglelist.size() != 2)
		{
			logger.info("Unable to swap: edge not not bounding two faces ");
			return false;
		}
		Iterator it = trianglelist.iterator();
		triangle = (MeshFace) it.next();
		adj_tri = (MeshFace) it.next();
		MeshNode pt = triangle.apex(e);
		MeshNode adj_pt = adj_tri.apex(e);
		if ( (pt==null)|| (adj_pt == null) )
		{
			logger.error("Unable to swap: null apex");
			return false;
		}
		MeshEdge eSwapped = myMesh.getEdgeDefinedByNodes(pt, adj_pt);
		if (eSwapped != null)
		{
			logger.error("Unable to swap: edge already exists");
			return false;
		}

		// suppose we swap:
		MeshNode e_p1 = (MeshNode)e.getNodes1();
		MeshNode e_p2 = (MeshNode)e.getNodes2();
		
		if (normales==true)
		{
			// test sur les normales aux faces
			MeshNode v1 = Calculs.prodVect3D(e_p1,pt,e_p2);
			MeshNode v2 = Calculs.prodVect3D(e_p1,e_p2,adj_pt);
			v1.setX(v1.getX()+e_p1.getX());
			v1.setY(v1.getY()+e_p1.getY());
			v1.setZ(v1.getZ()+e_p1.getZ());
			v2.setX(v2.getX()+e_p1.getX());
			v2.setY(v2.getY()+e_p1.getY());
			v2.setZ(v2.getZ()+e_p1.getZ());
			double angle1 = Calculs.angleVect(e_p1,v1,v2);
			MeshNode adj_v1 = Calculs.prodVect3D(pt,e_p2,adj_pt);
			MeshNode adj_v2 = Calculs.prodVect3D(pt,adj_pt,e_p1);
			adj_v1.setX(adj_v1.getX()+pt.getX());
			adj_v1.setY(adj_v1.getY()+pt.getY());
			adj_v1.setZ(adj_v1.getZ()+pt.getZ());
			adj_v2.setX(adj_v2.getX()+pt.getX());
			adj_v2.setY(adj_v2.getY()+pt.getY());
			adj_v2.setZ(adj_v2.getZ()+pt.getZ());
			double angle2 = Calculs.angleVect(pt,adj_v1,adj_v2);
			if ( (angle1!=0.) && ( Math.abs(angle1) < Math.abs(angle2) ) )
			{
				//				logger.warn("pas swap");
				//e.setFrozen(true);
				return false;
			}
		}
		
		// Compute quality of current triangles
		double Qtriangle = triangle.qualite();
		double Qadj_tri = adj_tri.qualite();
		// the quality of these 2 triangles is :
		double Q = Math.min(Qtriangle, Qadj_tri);
		
		// Compute quality of swapped triangles
		double qtriangle = MeshFace.qualite(adj_pt, pt, e_p1);
		double qadj_tri = MeshFace.qualite(adj_pt, pt, e_p2);
		// Compute the new quality
		double q = Math.min(qtriangle, qadj_tri);
		// Take the best quality
		if (q > Q)
			return swapEdge(e);

		return false;
	}

	/**
	 *
	 */
	public void nodeErrorDetection()
	{
		logger.debug("nodeErrorDetection: debut");		
		Iterator itn = myMesh.getNodesIterator();
		while (itn.hasNext())
		{
			MeshNode node = (MeshNode)itn.next();
			
			// test pour voir
			if (node.getElements().isEmpty())
				logger.debug("node non linke !");
			
			// List of edges connected to the node
			Iterator itEdges=node.getEdgesIterator();
			HashSet LE = new HashSet();
			while(itEdges.hasNext()) LE.add(itEdges.next());
			
			// List of faces connected to the node
			// HashSet LF = node.getFaces();
			// List of edges belonging to faces of LF and not belonging to LE
			HashSet LEC = new HashSet();
			
			// fill LEC
			HashSet edgesOfLF = new HashSet ();
			Iterator itf = node.getFacesIterator();
			while(itf.hasNext())
			{
				MeshFace face = (MeshFace)itf.next();
				Iterator it=face.getEdgesIterator();
				while(it.hasNext()) edgesOfLF.add(it.next());
			}
			LEC.addAll(edgesOfLF);
			
			edgesOfLF.retainAll(LE);
			LEC.removeAll(edgesOfLF);
			
			//first case: cardinal of LEC = 0. The node should be removed
			if (LEC.size() == 0)
			{
				myMesh.rmNode(node);
				continue;
			}
			
			//second case: a unique contour is detected
			ArrayList contour = isSingleContour(LEC);
			if (contour!=null)
			{
				// This contour is closed. To be manifold around the node, the
				// cardinal of LE must be equal to the cardinal of LF.
				if (contourIsClosed(contour))
				{
					if (node.numberOfEdges() != node.numberOfEdges())
					{
						// to remove a node, we must remove edges linked with before
						Iterator it = node.getEdgesIterator();
						while (it.hasNext())
							myMesh.rmEdge((MeshEdge)it.next());
						myMesh.rmNode(node);
					}
				}
				else // the contour is open. Cardinal of LE must be equal to cardinal of LF plus one.
				{
					// remark: same treatement as third case
					if (node.numberOfEdges() != node.numberOfFaces()+1)
					{
						Iterator it = node.getEdgesIterator();
						while (it.hasNext())
							myMesh.rmEdge((MeshEdge)it.next());
						myMesh.rmNode(node);
					}
				}
			}
			// third case: N contours are detected
		}
		logger.debug("nodeErrorDetection: fin");
	}
	
	
	////////////////////////////////	
	public void runTargetSizeConvergence(double minlen, double maxlen)
	{		
		Iterator ite = myMesh.getGeometryIterator();
		while (ite.hasNext())
		{
			TopoDS_Shape s = (TopoDS_Shape) ite.next();
			if (s.shapeType() == TopAbs_ShapeEnum.FACE)
			{
				MeshOfCAD m = myMesh.getMeshFromMapOfSubMesh(s);
				new MiscAlgos(m).targetSizeConvergence(minlen,maxlen);
			}
		}
	}
	
	/**
	 * This method starts the mesh computation according to the meshing hypothesis.
	 * <ul>
	 *   <li>If BEAM algorithm is requiered, the constraint value is the discretisation length of edges.</li>
	 *   <li>If TRIA3 algorithm is requiered, the constraint value is the area of the faces, so edge discretisation length is computed according to
	 * the formula: <pre>len = Math.sqrt(4*discr/Math.sqrt(3.));</pre></li>
	 * </ul>
	 * Once surface have been discretised, a 3D refine method is applied on edges which length is bigger than the computed constraint value.
	 * Several mothods are available to improve meshing :
	 * <ul>
	 * 	 <li>runRefine</li>
	 * 	 <li>runReduceNodeConnection</li>
	 * 	 <li>runSmoothing</li>
	 * </ul>
	 * @param cons  the value of the tesselation constraint
	 * @param discr  maximal edge length
	 * @return <code>true</code> if the computation complete without error,
	 * <code>false</code> if errors occured
	 */
	public boolean runMesh(MeshConstraint cons, double discr)
	{
		try
		{
			new BasicMesh().compute(myMesh,cons);
			
			/* Improve mesh for Algo TRIA3 only */
			if (hypo.getType()==MeshHypothesis.TRIA3)
			{
				//Refine faces
				runRefine(discr);
				Smoothing.runFaceSmoothing(myMesh, 4);
/*/ essai pour test fonction collapse
				runCollapse(0.);
// fin test collapse*/
				
				
/*/ test methode reduceNodeConnection
				runReduceNodeConnection();
				saveUNV("/usr5/home/cb/Projets/J3DView/apresReduceNC.unv");
// fin nodeConnection */
				
			}
			
/* test methode cleanMesh
			saveUNV("/usr5/home/cb/Projets/J3DView/avantClean.unv");
			runCleanMesh();
			saveUNV("/usr5/home/cb/Projets/J3DView/apresClean.unv");
 */
			
/* test methode improveConnect
			//saveUNV("/home_master/cb/Projets/J3DView/avantImprouveConnect.unv");
			runImproveConnect();
			saveUNV("/home_master/cb/Projets/J3DView/apresImprouveConnect.unv");
*/
			
//* test des methodes "topologic mesh correction"
  			//UNVWriter unv = new UNVWriter(new FileOutputStream("avantCorrection.unv"),new MeshGroup(this));
  			//unv.writeMesh();
//  		runMeshCorrection();
 

/*/ test fonction de lissage
			//saveUNV("/usr5/home/cb/Projets/J3DView/avantlissage.unv");
			runStats();
			runSmoothing(1);
// fin lissage */
		
			
			//			HashSet edges = edgesNotConnectedTwice();
			
			//*/ test fonctions cleanMesh
			//runCleanMesh();
			// */
			runStats();
		} catch (Exception e)
		{
			logger.fatal(e.toString());
			e.printStackTrace();
		}
		return true;
	}
	
	/**
	 * Method runCheckAndSwap.
	 * Browses each edge , compute triangle quality and swap edge if the operation improves triangle quality
	 */
	public void runCheckAndSwap()
	{
		Iterator ite = myMesh.getGeometryIterator();
		while (ite.hasNext())
		{
			TopoDS_Shape s = (TopoDS_Shape) ite.next();
			if (s.shapeType() == TopAbs_ShapeEnum.FACE)
			{
				MeshOfCAD m = myMesh.getMeshFromMapOfSubMesh(s);
				
				// check et swap sur toutes les edges				
				Iterator itc = myMesh.getEdgesIterator();
				while (itc.hasNext())
				{
					new MiscAlgos(m).check((MeshEdge) itc.next(),false);
				}
			}
		}
	}

	/**
	 * Method runRefine.
	 * Method used to refine the mesh in order to verify the constraint value of meshing.
	 * Edges to long are cut in two and new faces are created.
	 * @param discr - double value of mesh constraint
	 */
	public void runRefine(double discr) 
	{
		logger.debug("runRefine : begin");
		HashSet edges=null;
		while ( (edges = getEdgesToRefine(discr)).size()>0 )
		{
			logger.info(" Edges Refine : "+edges.size());
			refine(edges);
		}
		logger.debug("runRefine : end");
	}
	
	/**
	 * Method runCollapse.
	 * Calls the collapse method.
	 * @param len - the max length of edges.
	 * @see #collapse
	 */
	public void runCollapse(double len)
	{
		// collapse des edges de longueurs < a constraint
		if (len == 0.) return;
		int count;
		Iterator ite = myMesh.getGeometryIterator();
		while (ite.hasNext())
		{
			TopoDS_Shape s = (TopoDS_Shape) ite.next();
			if (s.shapeType() == TopAbs_ShapeEnum.FACE)
			{
				MeshOfCAD m = myMesh.getMeshFromMapOfSubMesh(s);
				count = 1;
				while (count > 0)
				{
					count = 0;
					HashSet edgesToCollapse = new HashSet(new MiscAlgos(m).getEdgesInfTo(len));
					logger.info("Nb edges to collapse: "+edgesToCollapse.size());
					Iterator it = edgesToCollapse.iterator();
					while (it.hasNext())
					{
						MeshEdge e=(MeshEdge)it.next();
						if (!e.isWire() && 
							new MiscAlgos(m).collapse((MeshNode)e.getNodes1(),(MeshNode)e.getNodes2()))
						{
							count++;
							break;
						}
					}
				}
			}
		}
	}
	
	
	/**
	 * Method runReduceNodeConnection.
	 * Erases nodes linked with more than 6 faces and rebuild faces.
	 * Nodes classified on Vertex or on Edge (or degenerated edge) are kept.
	 */
	public void runReduceNodeConnection()
	{
		Iterator ite = myMesh.getGeometryIterator();
		while (ite.hasNext())
		{
			TopoDS_Shape s = (TopoDS_Shape) ite.next();
			if (s.shapeType() == TopAbs_ShapeEnum.FACE)
			{
				MeshOfCAD m = myMesh.getMeshFromMapOfSubMesh(s);
				new MiscAlgos(m).reduceNodeConnection();
			}
		}
	}
		
	/**
	 * Method runImproveConnect.
	 * An optimisation operator.
	 * @see #improveConnect
	 */
	public void runImproveConnect()
	{	
		Iterator ite = myMesh.getGeometryIterator();
		while (ite.hasNext())
		{
			TopoDS_Shape s = (TopoDS_Shape) ite.next();
			if (s.shapeType() == TopAbs_ShapeEnum.FACE)
			{
				MeshOfCAD m = myMesh.getMeshFromMapOfSubMesh(s);
				new MiscAlgos(m).improveConnect();
			}
		}
	}
	
	
	/**
	 * Method runMeshCorrection.
	 */
	public void runMeshCorrection()
	{
		//Iterer sur les faces
		Iterator ite = myMesh.getGeometryIterator();
		while (ite.hasNext())
		{
			TopoDS_Shape s = (TopoDS_Shape) ite.next();
			if (s.shapeType() == TopAbs_ShapeEnum.FACE)
			{
				MeshOfCAD m = myMesh.getMeshFromMapOfSubMesh(s);

				// extraction d'un contour
				// choix d'un noeud au hasard, juste pour extraire un contour
				MeshNode no = null;
				Iterator itn = m.getNodesIterator();
				while (itn.hasNext())
				{
					no = (MeshNode)itn.next();
					if  (no.getPosition().getType()==PST_Position.SURFACE)
					{
						logger.info("node selectionne: "+no);
						break;
					}
				}
				HashSet contour = no.getTopologicContour(1,MeshElement.EDGE);
				m.rmFaces(no.getTopologicContour(0,MeshElement.FACE));
				
				// construction de l'arraylist d'edges
				ArrayList alist = new ArrayList();
				Iterator itc = contour.iterator();
				while (itc.hasNext())
				{
					MeshEdge ec = (MeshEdge)itc.next();
					alist.add(ec);
				}

				new MiscAlgos(m).fillContour(alist);
				break;
			}
		}
	}
		
	/**
	 * Method runErrorDetection.   A TESTER
	 */
	public void runErrorDetection() {
		nodeErrorDetection();
	}
	/**
	 * Swaps an edge with the edge defined by the apex of the 2 adjacent triangles.
	 * This method is used to modify, when it is possible, the edge share by two faces.
	 * It returns a boolean variable which indicates if the action has been done or not.
	 * Swapping conditions:
	 * - the edge is connected to two faces
	 * - the edge is not classified on a wire
	 * - swapping the edge improves the triangle qualities
	 * @param e : a MeshEdge instance, the edge to swap
	 * @return boolean - set to true if swap has done succesfully
	 */
	public boolean swapEdge(MeshEdge e)
	{
//		logger.debug("swapEdge("+e+")");
		// Swapping condition: the edge is not classified on a line
		if (e.isFrozen() || e.isWire())
		{
			logger.warn("swapEdge : unable to swap a border edge");
			return false;
		}
		
		// Swaping condition: Ea=Fa=3 ou Eb=Fb=3 forbids swapping
		
		MeshNode na = e.getNodes1();
		MeshNode nb = e.getNodes2();
		
		int ea = na.numberOfEdges();
		int fa = na.numberOfFaces();
		int eb = nb.numberOfEdges();
		int fb = nb.numberOfFaces();
		if ( ((ea == 3)&&(fa == 3)) || ((eb == 3)&&(fb == 3)) )
		{
			return false;
		}
		
		
		// find the 2 triangles with edge E in common
		HashSet trianglelist = e.getFaces();
		//Swapping condition: the edge is connected to two faces
		if (trianglelist.size() != 2)
		{
			System.out.println(
			"MeshMesh - swapEdge : Erreur dans la recherche des triangles voisins (swapping condition)" +trianglelist.size());
			trianglelist.clear();
			return false;
		}
		Iterator itt = trianglelist.iterator();
		MeshFace T1 = (MeshFace) itt.next();
		MeshFace T2 = (MeshFace) itt.next();
		//Find the apex:
		MeshNode a1 = T1.apex(e);
		//a1=(MeshNode)getNodesOfSubMesh(new HashSet()).addIfNotPresent(a1);
		if (a1 == null)
		{
			logger.warn("swapEdge : cannot find the apex of "+e+" in "+T1);
			return false;
		}
		MeshNode a2 = T2.apex(e);
		//a2=(MeshNode)getNodesOfSubMesh(new HashSet()).addIfNotPresent(a2);
		
		if (a2 == null)
		{
			logger.warn("swapEdge : cannot find the apex of "+e+" in "+T2);
			return false;
		}
		
		if (a1.distance(a2)<=0.0)
		{
			return false;
		}
		if (myMesh.getEdgeDefinedByNodes(a1,a2)!=null)
		{
			// TEST MH
			//e.setFrozen(true);
			return false;
		}
		
		// Test if swap possible: somme des angles de part et d'autre de l'edge doit etre < PI
		MeshNode pt = e.getNodes1();
		MeshNode n1 = e.getNodes2();
		double alpha1 = pt.angle(n1, a1);
		double alpha2 = pt.angle(n1, a2);
		double alpha3 = n1.angle(pt, a1);
		double alpha4 = n1.angle(pt, a2);
		if ((Math.abs(alpha1) + Math.abs(alpha2) + MeshMesh.epsilon) >= Math.PI)
			return false;
		if ((Math.abs(alpha3) + Math.abs(alpha4) + MeshMesh.epsilon) >= Math.PI)
			return false;
		
		myMesh.rmFace(T1);
		myMesh.rmFace(T2);
		myMesh.addTriangle(a1, e.getNodes1(), a2);
		myMesh.addTriangle(a1, e.getNodes2(), a2);
		return true;
	}
	
	/**
	 * Returns a HashSet of edges that belong to triangles whose area is bigger than requiered
	 * @param areamax : a double value, the maximum area surface
	 * @return HashSet - a set of edges to refine
	 * */
	public HashSet getEdgesToRefine(double areamax)
	{
		HashSet toreturn = new HashSet();
		Iterator itf = myMesh.getFacesIterator();
		while (itf.hasNext())
		{
			MeshFace f = (MeshFace) itf.next();
			double area = f.computeArea();
			if (area > areamax)
			{
				HashSet edgelist = new HashSet();
				Iterator ite = f.getEdgesIterator();
				while (ite.hasNext())
				{
					MeshEdge e = (MeshEdge)(ite.next());
					if (!e.isWire()&& !e.isFrozen())
						toreturn.add(e);
				}
			}
			
			else if (area == -1)
			{
				toreturn.clear();
				return toreturn;
			}
		}
		return toreturn;
	}
	
	/** Get all the edges which length is superior to a given value.
	 * @param param : a double value, the length value
	 * @return HashSet : the list of edges which length is bigger than the input value.
	 */
	public HashSet getEdgesSupTo(double param)
	{
		HashSet toreturn = new HashSet();				
		Iterator it = myMesh.getEdgesIterator();
		while (it.hasNext())
		{
			MeshEdge e = (MeshEdge) it.next();
			if (e.length() > param)
				toreturn.add(e);
		}
		return toreturn;
	}
	
	public HashSet getEdgesInfTo(double param)
	{
		HashSet toreturn = new HashSet();		
		Iterator it = myMesh.getEdgesIterator();
		while (it.hasNext())
		{
			MeshEdge e = (MeshEdge) it.next();
			if (e.length() < param) toreturn.add(e);
		}
		return toreturn;
	}
	
	/**
	 * Computes the middle point of each edge of the input list.
	 * @param edges : a HashSet of edges
	 */
	public void prepareEdgesToRefine(HashSet edges)
	{
		//Set the midnode for the edges in edges & get the faces
		logger.debug("Nb d'edges a refiner: " + edges.size());
		Iterator it = edges.iterator();
		//logger.info("Computing mid pt ... a bit long ");
		HashSet edgesNotRefined = new HashSet();
		int j=0;
		while (it.hasNext())
		{
			MeshEdge e = (MeshEdge) it.next();
			if (e.isWire())
				continue;

			MeshNode n1 = e.getNodes1();
			MeshNode n2 = e.getNodes2();
			
			MeshNode mid = createMidPt(n1, n2);
			if (mid == null)
			{
				logger.error("MeshMesh - refine : Mid pt is null"+j);
				edgesNotRefined.add(e);
				continue;
			}
			j++;
			mid.setCoord3D();			
			MeshMesh m=myMesh.getMeshFromMapOfSubMesh(mid.getPosition().getShape());			
			mid  = m.addNode(mid);
			e.setMidNode(mid);
		}
		edges.removeAll(edgesNotRefined);
		edgesNotRefined.clear();
		//logger.debug("Found "+edges.size()+" edges to refine");
	}
	
	/**
	 * Method refine.
	 * Calls by runMesh(), and calls refine() method
	 * @param edges - a list of edges to refine
	 * @return boolean - set to \c true if method succedes, to false if it fails
	 */
	public boolean refine(HashSet edges)
	{
		boolean result = false;
		
		prepareEdgesToRefine(edges);
		logger.info(" Fin prepare - refine ");
				
		Iterator ite = myMesh.getGeometryIterator();
		while (ite.hasNext())
		{
			TopoDS_Shape s = (TopoDS_Shape) ite.next();
			if (s.shapeType() == TopAbs_ShapeEnum.FACE)
			{
				MeshOfCAD m = myMesh.getMeshFromMapOfSubMesh(s);				
				result |= new MiscAlgos(m).refineFaces();
				m=null;
			}
		}
		return result;
	}

	
	public boolean refineSubMesh(HashSet edges)
	{
		boolean result = false;		
		prepareEdgesToRefine(edges);
		logger.info(" Fin prepare");			
		result = refineFaces();
		return result;
	}
	
	/**
	 * Method used to sub-devise a sub-set of edges and faces composing the mesh in order to refine its definition.
	 * \n This method uses as input parameter a list of faces.
	 * @param faces : a HashSet of faces to refine
	 */
	private boolean refineFaces()
	{
		boolean refine = false;
		HashSet edgestocheck = new HashSet();
		
		//Create a copy of the list of MeshFace.
		MeshFace[] facesArray=new MeshFace[myMesh.numberOfFaces()];
		Iterator facesIterator=myMesh.getFacesIterator();
		for(int i=0;i<facesArray.length;i++)
			facesArray[i]=(MeshFace)facesIterator.next();
		
		for(int i=0;i<facesArray.length;i++)
		{
			MeshFace f = facesArray[i];
			if (f.numberOfEdges() != 3)
			{
				logger.warn("refineFaces : "+f+"is not a triangle");
				return refine;
			}
			MeshEdge e1 = null;
			MeshEdge e2 = null;
			MeshEdge e3 = null;
			//count mid point
			int count = 0;
			Iterator ite = f.getEdgesIterator();
			while (ite.hasNext())
			{
				MeshEdge e = (MeshEdge) ite.next();
				if (e.getMidNode() != null)
				{
					count++;
					if (e1 == null) e1 = e;
					else if (e2 == null) e2 = e;
					else e3 = e;
				}
				else
				{
					if (e3 == null)
						e3 = e;
					else if (e2 == null)
						e2 = e;
					else
						e1 = e;
				}
			}
			//apply pattern
			switch (count)
			{
				case 0 :
					refine = false;
					break;
				case 1 :
				{
					//e1 is the edge to cut
					MeshNode pt1 = e1.getNodes1();
					MeshNode pt2 = e1.getNodes2();
					MeshNode mid = e1.getMidNode();
					//MeshMesh m=father.getMeshFromMapOfSubMesh(mid.getPstPos().getShape());
					
					MeshNode A;
					if (e2.getNodes1() == pt1 || e2.getNodes1() == pt2)
						A = e2.getNodes2();
					else
						A = e2.getNodes1();
					//Construct 3 new edges;
					MeshEdge ne1 = new MeshEdge(pt1, mid);
					MeshEdge ne2 = new MeshEdge(pt2, mid);
					if (e1.isWire())
					{
						ne1.setWire(true);
						ne2.setWire(true);
						ne1.setFrozen(true);
						ne2.setFrozen(true);
					}
					MeshEdge ne3 = new MeshEdge(mid, A);
					e2 = myMesh.getEdgeDefinedByNodes(pt1, A);
					if (e2 == null)
					{
						logger.warn("Pb getEdgeDefinedbyNodes (Pattern1: e2)");
						return false;
					}
					e3 = myMesh.getEdgeDefinedByNodes(pt2, A);
					if (e3 == null)
					{
						logger.warn("Pb getEdgeDefinedbyNodes (Pattern1: e3)");
						return false;
					}
					ne1 = myMesh.addEdge(ne1);
					ne2 = myMesh.addEdge(ne2);
					ne3 = myMesh.addEdge(ne3);
					e2 = myMesh.addEdge(e2);
					e3 = myMesh.addEdge(e3);
					//Construct two new triangles
					myMesh.addTriangle(ne1, ne3, e2);
					myMesh.addTriangle(ne2, ne3, e3);
					myMesh.rmFace(f);
					//* MHG */ f.clearList();
					// add the new edges to the set
					if(!ne1.isWire() && !ne1.isFrozen()) edgestocheck.add(ne1);
					if(!ne2.isWire() && !ne2.isFrozen()) edgestocheck.add(ne2);
					if(!ne3.isWire() && !ne3.isFrozen()) edgestocheck.add(ne3);
					if(!e2.isWire() && !e2.isFrozen()) edgestocheck.add(e2);
					if(!e3.isWire() && !e3.isFrozen()) edgestocheck.add(e3);
					refine = true;
					break;
				}
				case 2 :
				{
					//e1 & e2 are the edges to cut
					MeshNode pt1 = e3.getNodes1();
					MeshNode pt2 = e3.getNodes2();
					MeshNode mid1 = e1.getMidNode();
					MeshNode mid2 = e2.getMidNode();
					//	MeshMesh m1=father.getMeshFromMapOfSubMesh(mid1.getPstPos().getShape());
					//	MeshMesh m2=father.getMeshFromMapOfSubMesh(mid2.getPstPos().getShape());
					
					MeshNode A = null;
					if (e1.getNodes1() == e2.getNodes1())
					{
						A = e1.getNodes1();
						pt1 = e1.getNodes2();
						pt2 = e2.getNodes2();
					}
					if (e1.getNodes1() == e2.getNodes2())
					{
						A = e1.getNodes1();
						pt1 = e1.getNodes2();
						pt2 = e2.getNodes1();
					}
					if (e1.getNodes2() == e2.getNodes1())
					{
						A = e1.getNodes2();
						pt1 = e1.getNodes1();
						pt2 = e2.getNodes2();
					}
					if (e1.getNodes2() == e2.getNodes2())
					{
						A = e1.getNodes2();
						pt1 = e1.getNodes1();
						pt2 = e2.getNodes1();
					}
					//Construct 6 new edges;
					MeshEdge ne1 = new MeshEdge(A, mid1);
					MeshEdge ne2 = new MeshEdge(mid1, pt1);
					MeshEdge ne3 = new MeshEdge(A, mid2);
					MeshEdge ne4 = new MeshEdge(mid2, pt2);
					MeshEdge ne5 = new MeshEdge(mid1, mid2);
					MeshEdge ne6 = new MeshEdge(mid2, pt1);
					if (e1.isWire())
					{
						ne1.setWire(true);
						ne2.setWire(true);
						ne1.setFrozen(true);
						ne2.setFrozen(true);
					}
					if (e2.isWire())
					{
						ne3.setWire(true);
						ne4.setWire(true);
						ne3.setFrozen(true);
						ne4.setFrozen(true);
						
					}
					ne1 = myMesh.addEdge(ne1);
					ne2 = myMesh.addEdge(ne2);
					ne3 = myMesh.addEdge(ne3);
					ne4 = myMesh.addEdge(ne4);
					ne5 = myMesh.addEdge(ne5);
					ne6 = myMesh.addEdge(ne6);
					e3 = myMesh.addEdge(e3);
					//Construct twthree new triangles
					myMesh.addTriangle(ne1, ne3, ne5);
					myMesh.addTriangle(ne2, ne5, ne6);
					myMesh.addTriangle(ne6, ne4, e3);
					myMesh.rmFace(f);
					//* mhg */ f.clearList();
					// add the new edges to the set
					if(!ne1.isWire() && !ne1.isFrozen()) edgestocheck.add(ne1);
					if(!ne2.isWire() && !ne2.isFrozen()) edgestocheck.add(ne2);
					if(!ne3.isWire() && !ne3.isFrozen()) edgestocheck.add(ne3);
					if(!ne4.isWire() && !ne4.isFrozen()) edgestocheck.add(ne4);
					if(!ne5.isWire() && !ne5.isFrozen()) edgestocheck.add(ne5);
					if(!ne6.isWire() && !ne6.isFrozen()) edgestocheck.add(ne6);
					if(!e3.isWire() && !e3.isFrozen()) edgestocheck.add(e3);
					refine = true;
					break;
				}
				case 3 :
				{
					//e1 & e2 & e3 are the edges to cut
					MeshNode pt1 = null;
					MeshNode pt2 = null;
					MeshNode pt3 = null;
					MeshNode mid1 = e1.getMidNode();
					MeshNode mid2 = e2.getMidNode();
					MeshNode mid3 = e3.getMidNode();
					MeshMesh m1=myMesh.getMeshFromMapOfSubMesh(mid1.getPosition().getShape());
					MeshMesh m2=myMesh.getMeshFromMapOfSubMesh(mid2.getPosition().getShape());
					MeshMesh m3=myMesh.getMeshFromMapOfSubMesh(mid3.getPosition().getShape());
					
					if (e1.getNodes1()==(e2.getNodes1()))
					{
						pt2 = e1.getNodes1();
						pt3 = e2.getNodes2();
						pt1 = e1.getNodes2();
					}
					if (e1.getNodes1()==(e2.getNodes2()))
					{
						pt2 = e1.getNodes1();
						pt3 = e2.getNodes1();
						pt1 = e1.getNodes2();
					}
					if (e1.getNodes2()==(e2.getNodes1()))
					{
						pt2 = e1.getNodes2();
						pt3 = e2.getNodes2();
						pt1 = e1.getNodes1();
					}
					if (e1.getNodes2()==( e2.getNodes2()))
					{
						pt2 = e1.getNodes2();
						pt3 = e2.getNodes1();
						//pt1 = e1.getPt2();
						pt1 = e1.getNodes1();
					}
					//Construct 9 new edges;
					MeshEdge ne1 = new MeshEdge(pt1, mid1);
					MeshEdge ne2 = new MeshEdge(mid1, pt2);
					MeshEdge ne3 = new MeshEdge(pt2, mid2);
					MeshEdge ne4 = new MeshEdge(mid2, pt3);
					MeshEdge ne5 = new MeshEdge(pt3, mid3);
					MeshEdge ne6 = new MeshEdge(mid3, pt1);
					MeshEdge ne7 = new MeshEdge(mid1, mid3);
					MeshEdge ne8 = new MeshEdge(mid1, mid2);
					MeshEdge ne9 = new MeshEdge(mid2, mid3);
					if (e1.isWire())
					{
						ne1.setWire(true);
						ne2.setWire(true);
						ne1.setFrozen(true);
						ne2.setFrozen(true);
					}
					if (e2.isWire())
					{
						ne3.setWire(true);
						ne4.setWire(true);
						ne3.setFrozen(true);
						ne4.setFrozen(true);
					}
					if (e3.isWire())
					{
						ne5.setWire(true);
						ne6.setWire(true);
						ne5.setFrozen(true);
						ne6.setFrozen(true);
					}
					ne1 = myMesh.addEdge(ne1);
					ne2 = myMesh.addEdge(ne2);
					ne3 = myMesh.addEdge(ne3);
					ne4 = myMesh.addEdge(ne4);
					ne5 = myMesh.addEdge(ne5);
					ne6 = myMesh.addEdge(ne6);
					
					ne7 = myMesh.addEdge(ne7);
					ne8 = myMesh.addEdge(ne8);
					ne9 = myMesh.addEdge(ne9);
					//Construct 4 new triangles
					myMesh.addTriangle(ne1,ne6,ne7);
					myMesh.addTriangle(ne2,ne3,ne8);
					myMesh.addTriangle(ne9,ne4,ne5);
					myMesh.addTriangle(ne7,ne8,ne9);
					myMesh.rmFace(f);
					//* mhg */ f.clearList();
					// add the new edges to the set
					if(!ne1.isWire() && !ne1.isFrozen()) edgestocheck.add(ne1);
					if(!ne2.isWire() && !ne2.isFrozen()) edgestocheck.add(ne2);
					if(!ne3.isWire() && !ne3.isFrozen()) edgestocheck.add(ne3);
					if(!ne4.isWire() && !ne4.isFrozen()) edgestocheck.add(ne4);
					if(!ne5.isWire() && !ne5.isFrozen()) edgestocheck.add(ne5);
					if(!ne6.isWire() && !ne6.isFrozen()) edgestocheck.add(ne6);
					// There is no reason to swap ne[7-9]
					
					refine =  true;
					break;
				}
				default :
					logger.warn("Unknown pattern");
					refine = false;
					break;
			}
		}
		
		Iterator it = edgestocheck.iterator();
		while (it.hasNext())
		{
			check((MeshEdge) it.next(), /*false*/true);
		}
		
		return refine;		
	}
	
	
	
	private void setArbitraryMidPoint(MeshEdge edge)
	{
		MeshNode n1=edge.getNodes1();
		MeshNode n2=edge.getNodes2();
		MeshNode mid=new MeshNode(
			(n1.getX()+n2.getX())/2,
			(n1.getY()+n2.getY())/2,
			(n1.getZ()+n2.getZ())/2);
		mid.setElements(edge.getElements());
		edge.setMidNode(myMesh.addNode(mid));
	}
	
	public void setArbitraryMidPoints()
	{
		logger.info("Settings mid points");
		Iterator it=myMesh.getEdgesIterator();
		while(it.hasNext()) setArbitraryMidPoint((MeshEdge)it.next());
	}
	
	public void runStats()
	{
		double anglemin = 360.0;
		double anglemax = -360.0;
		double angle = 0.;
		Iterator itface = myMesh.getFacesIterator();
		while (itface.hasNext())
		{
			MeshFace face = (MeshFace)itface.next();			
			if(face.numberOfNodes() == 3 )
			{
				Iterator itnode = face.getNodesIterator();
				MeshNode n1 = (MeshNode)itnode.next();
				MeshNode n2 = (MeshNode)itnode.next();
				MeshNode n3 = (MeshNode)itnode.next();
				angle = (180.0/Math.PI)* n1.angle(n2,n3);
				if (angle < anglemin) anglemin = angle;
				if (angle > anglemax) anglemax = angle;
				angle = (180.0/Math.PI)*n2.angle(n3,n1);
				if (angle < anglemin) anglemin = angle;
				if (angle > anglemax) anglemax = angle;
				angle = (180.0/Math.PI)*n3.angle(n1,n2);
				if (angle < anglemin) anglemin = angle;
				if (angle > anglemax) anglemax = angle;
				if (angle == 0)
					logger.info("angle nul !");
			}
			
		}
		logger.info("* angle min = "+anglemin);
		logger.info("* angle max = "+anglemax);
		logger.info("Number of nodes: "+myMesh.numberOfNodes());
		logger.info("Number of faces: "+myMesh.numberOfFaces());
	}	
	
	public void setTypeAlgo(int algo)
	{
		hypo = new MeshHypothesis("toto",1,algo);
	}
	
	public MeshConstraint setDiscretisation(double discr)
	{
		// Compute discretisation length
		double length;
		if (hypo.getType()==MeshHypothesis.TRIA3)
			length = Math.sqrt(4*discr/Math.sqrt(3.));
		else
			length = discr;
		
		MeshConstraint cons = new MeshConstraint(1,hypo.getType(),length);
		MeshConstraint scale = new MeshConstraint(1, 10000);
		MeshNode.scale = new Double(scale.getValue()).intValue();
		return cons;
	}	
}
