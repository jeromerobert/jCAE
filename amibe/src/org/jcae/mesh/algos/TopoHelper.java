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
import java.util.*;
import org.jcae.mesh.util.Pair;

/**
 *
 * @author Jerome Robert
 */
public class TopoHelper

{
	/** shape of the geometric entity */
	protected TopoDS_Shape shape;
	
	/** Constructor with a shape
	 * @param shape : a TopoDS_Shape instance, the shape corresponding to the main mesh
	 */
	public TopoHelper(TopoDS_Shape shape)
	{
		this.shape = shape;
	}
	
	/**
	 * Get all TopoDS_Face linked with a giving edge.
	 * @param topoedge : a TopoDS_Edge instance, the edge
	 * @return ArrayList  : an array containing all the faces linked with the edge
	 */
	public ArrayList getTopoFaces(TopoDS_Edge topoedge)
	{
		HashMap ancestors = TopExp.mapShapesAndAncestors(shape,
			TopAbs_ShapeEnum.EDGE, TopAbs_ShapeEnum.FACE);
		ArrayList tfaces = (ArrayList)ancestors.get(topoedge);
		// isSame method between edges depends on the edge orientation
		TopoDS_Edge topoedgebis=(TopoDS_Edge)topoedge.reversed();
		ArrayList tfacesbis = (ArrayList)ancestors.get(topoedgebis);

		if (tfacesbis!=null)
		{
			for (int i = 0; i< tfacesbis.size(); i++)
			{
				if (!tfaces.contains((TopoDS_Face)tfacesbis.get(i)))
					tfaces.add(tfacesbis.get(i));
			}
		}
		return tfaces;
	}
	
	/**
	 * Get all the TopoDS_Face containing a giving node.
	 * @param topovertex : a TopoDS_Vertex instance, the node
	 * @return ArrayList : an array containing all the faces linked with the node
	 */
	public ArrayList getTopoFaces(TopoDS_Vertex topovertex)
	{
		HashMap ancestors = TopExp.mapShapesAndAncestors(shape,
			TopAbs_ShapeEnum.VERTEX, TopAbs_ShapeEnum.FACE);
		ArrayList tfaces = (ArrayList)ancestors.get(topovertex);
		return tfaces;
	}
	
	/**
	 * Get all  the TopoDS_Faces commun with a giving vertex and a giving edge
	 * @param topovertex : a TopoDS_Vertex instance
	 * @param topoedge : a TopoDS_Edge instance, the edge
	 * @return ArrayList : an array containing all the faces linked with the edge and the vertex
	 */
	public ArrayList getTopoFaces(TopoDS_Vertex topovertex, TopoDS_Edge topoedge)
	{
		ArrayList tfacesV = getTopoFaces(topovertex);
		ArrayList tfacesE = getTopoFaces(topoedge);
		ArrayList tfaces = new ArrayList();
		for (int i = 0; i< tfacesV.size(); i++)
		{
			for (int j = 0; j< tfacesE.size(); j++)
			{
				if ( ((TopoDS_Face)tfacesE.get(j)).isSame((TopoDS_Face)tfacesV.get(i)) )
					tfaces.add(tfacesE.get(j));
			}
		}
		return tfaces;
	}
	
	/**
	 * Get all TopoDS_Edge containing a giving node
	 * @param topovertex : a TopoDS_Vertex instance, the node
	 * @return ArrayList : an array containing all the edges containing the node
	 */
	public ArrayList getTopoEdges(TopoDS_Vertex topovertex)
	{
		HashMap ancestors = TopExp.mapShapesAndAncestors(shape,
			TopAbs_ShapeEnum.VERTEX, TopAbs_ShapeEnum.EDGE);
		ArrayList tedges = (ArrayList)ancestors.get(topovertex);
		return tedges;
	}
	
	/**
	 * Get the common geometric surface of 2 vertices
	 * @param tvertex1 : a TopoDS_Vertex instance, one vertex
	 * @param tvertex2 : a TopoDS_Vertex instance, other vertex
	 * @return TopoFace : the common surface of both vertices, return \c null if the common face does not exist.
	 */
	public TopoDS_Face getTopoFace(TopoDS_Vertex tvertex1, TopoDS_Vertex tvertex2)
	{
		HashMap ancestors = TopExp.mapShapesAndAncestors(shape,
			TopAbs_ShapeEnum.VERTEX, TopAbs_ShapeEnum.FACE);
		ArrayList tfaces1 = (ArrayList)ancestors.get(tvertex1);
		ArrayList tfaces2 = (ArrayList)ancestors.get(tvertex2);
		
		for (int i = 0; i<tfaces1.size(); i++)
		{
			TopoDS_Face face = (TopoDS_Face)tfaces1.get(i);
			for (int j = 0; j<tfaces2.size();j++)
			{
				if ( ((TopoDS_Face)tfaces2.get(j)).isSame(face) ) return face;
			}
		}
		return null;
	}
	
	/**
	 * Get all TopoDS_Face containing two edges.
	 * @param tedge1 : a TopoDS_Edge instance, one edge
	 * @param tedge2 : a TopoDS_Edge instance, another edge
	 * @return ArrayList - an array containing all the faces containing both edges
	 */
	public ArrayList getTopoFaces(TopoDS_Edge tedge1, TopoDS_Edge tedge2)
	{
		HashMap ancestors = TopExp.mapShapesAndAncestors(shape,
			TopAbs_ShapeEnum.EDGE, TopAbs_ShapeEnum.FACE);
		ArrayList tfaces = new ArrayList();
		TopoDS_Edge tedge11=null;
		if (tedge1.orientation()==TopAbs_Orientation.FORWARD)
			tedge11=(TopoDS_Edge)tedge1.oriented(TopAbs_Orientation.REVERSED);
		if (tedge1.orientation()==TopAbs_Orientation.REVERSED)
			tedge11=(TopoDS_Edge)tedge1.oriented(TopAbs_Orientation.FORWARD);
		TopoDS_Edge tedge21=null;
		if (tedge2.orientation()==TopAbs_Orientation.FORWARD)
			tedge21=(TopoDS_Edge)tedge2.oriented(TopAbs_Orientation.REVERSED);
		if (tedge2.orientation()==TopAbs_Orientation.REVERSED)
			tedge21=(TopoDS_Edge)tedge2.oriented(TopAbs_Orientation.FORWARD);
		
		ArrayList tfaces1 = (ArrayList)ancestors.get(tedge1);
		ArrayList tfaces2 = (ArrayList)ancestors.get(tedge2);
		if (ancestors.get(tedge11)!=null)
			tfaces1.addAll((ArrayList)ancestors.get(tedge11));
		if (ancestors.get(tedge21)!=null)
			tfaces2.addAll((ArrayList)ancestors.get(tedge21));
		
		for (int i = 0; i<tfaces1.size(); i++)
		{
			TopoDS_Face face = (TopoDS_Face)tfaces1.get(i);
			for (int j = 0; j < tfaces2.size(); j++)
			{
				if (((TopoDS_Face)tfaces2.get(j)).isSame(face)) tfaces.add(face);
			}
		}
		
		return tfaces;
	}
	
	/**
	 * Get the common geometric edge of 2 vertices
	 * @param tvertex1 : a TopoDS_Vertex instance, one vertex
	 * @param tvertex2 : a TopoDS_Vertex instance, other vertex
	 * @return TopoDS_Edge : the common edge of both vertices, return \c null if the common edge does not exist.
	 */
	public TopoDS_Edge getTopoEdge(TopoDS_Vertex tvertex1, TopoDS_Vertex tvertex2)
	{
		HashMap ancestors = TopExp.mapShapesAndAncestors(shape,
			TopAbs_ShapeEnum.VERTEX, TopAbs_ShapeEnum.EDGE);
		ArrayList tedges1 = (ArrayList)ancestors.get(tvertex1);
		ArrayList tedges2 = (ArrayList)ancestors.get(tvertex2);
		for (int i = 0; i<tedges1.size(); i++)
		{
			TopoDS_Edge edge = (TopoDS_Edge)tedges1.get(i);
			for (int j = 0; j < tedges2.size(); j++)
			{
				if (((TopoDS_Edge)tedges2.get(j)).isSame(edge)) return edge;
			}
		}
		return null;
	}
	
	/**
	 * Get the geometric edge common of 2 nodes
	 * @param n1 : a MeshNode instance, first node
	 * @param n2 : a MeshNode instance, second node
	 * @return TopoDS_Edge : the common edge of both nodes, return \c null if the common edge does not exist.
	 */
	public TopoDS_Edge getTopoEdge(MeshNode n1, MeshNode n2)
	{
		TopoDS_Edge edge = new TopoDS_Edge();
		// case the 2 nodes are PST_Position.EDGE
		if (((n1.getPosition()).getType() == PST_Position.EDGE) 
			&& ((n2.getPosition()).getType() == PST_Position.EDGE))
		{
			PST_LinePosition pos1 = (PST_LinePosition)n1.getPosition();
			PST_LinePosition pos2 = (PST_LinePosition)n2.getPosition();
			Pair Et1 = pos1.getCurvePosition();
			Pair Et2 = pos2.getCurvePosition();
			if (((TopoDS_Edge)(Et1.first)).isSame((TopoDS_Edge)Et2.first))
				edge = (TopoDS_Edge)Et1.first;
			else
				edge = null;
		}
		// case the 2 nodes are vertices
		else if (((n1.getPosition()).getType() == PST_Position.VERTEX)
			&& ((n2.getPosition()).getType() == PST_Position.VERTEX))
		{
			PST_VertexPosition pos1 = (PST_VertexPosition)(n1.getPosition());
			TopoDS_Vertex tvertex1 = pos1.getVertex();
			PST_VertexPosition pos2 = (PST_VertexPosition)(n2.getPosition());
			TopoDS_Vertex tvertex2 = pos2.getVertex();
			edge = getTopoEdge(tvertex1,tvertex2);
		}
		else if (((n1.getPosition()).getType() == PST_Position.EDGE)
			&& ((n2.getPosition()).getType() == PST_Position.VERTEX))
		{
			PST_LinePosition pos1 = (PST_LinePosition)n1.getPosition();
			PST_VertexPosition pos2 = (PST_VertexPosition)(n2.getPosition());
			Pair Et1 = pos1.getCurvePosition();
			ArrayList tedges2 = getTopoEdges(pos2.getVertex());
			for (int i = 0; i<tedges2.size(); i++)
			{
				if (((TopoDS_Edge)Et1.first).isSame((TopoDS_Edge)tedges2.get(i))) edge = (TopoDS_Edge)tedges2.get(i);
				else edge = null;
			}
			
		}
		else if (((n1.getPosition()).getType() == PST_Position.VERTEX)
			&& ((n2.getPosition()).getType() == PST_Position.EDGE))
		{
			PST_VertexPosition pos1 = (PST_VertexPosition)(n1.getPosition());
			PST_LinePosition pos2 = (PST_LinePosition)n2.getPosition();
			Pair Et2 = pos2.getCurvePosition();
			ArrayList tedges1 = getTopoEdges(pos1.getVertex());
			for (int i = 0; i<tedges1.size(); i++)
			{
				if (((TopoDS_Edge)Et2.first).isSame((TopoDS_Edge)tedges1.get(i)))
					edge = (TopoDS_Edge)tedges1.get(i);
				else edge = null;
			}
			
		}
		else
			edge = null;
		return edge;
	}
	/** Returns the geometric shape of the mesh.
	 * Allows to get the reference of the geometry linked to the MeshEntMesh entity.
	 * @return TopoDS_Shape : the geometric shape of the main mesh
	 */
	public TopoDS_Shape getGeometry()
	{
		return shape;
	}
	void setShape(TopoDS_Shape shape)
	{
		this.shape=shape;
	}
}
