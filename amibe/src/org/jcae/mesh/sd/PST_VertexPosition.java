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

package org.jcae.mesh.sd;

import org.jcae.opencascade.jni.*;

/** Class to characterise a specific node witch is defined in relation with a topological vertex entity.
 *
 * \n
 * @author Cyril BRANDY & Marie-Helene GARAT.
 */
public class PST_VertexPosition extends PST_Position
{
	/** The topological vertex entity. */
	public TopoDS_Vertex vertex;
	/** An array of the 2D coordinates of the node. */
	private double[] uv;
	/** The curvilinear coordinate of the node on the edge.*/
	public double param=0.0;
	
	/** Default constructor. */
	public PST_VertexPosition()
	{
		uv=null;
	}
	
	/**
	 * Return the type of node position.
	 * @return int : the type of node position
	 */
	public int getType()
	{
		return PST_Position.VERTEX;
	}
	
	/** Constructor with the topological vertex entity.
	 * @param vertex : a TopoDS_Vertex instance, the vertex entity
	 */
	public PST_VertexPosition(TopoDS_Vertex vertex)
	{
		this();
		this.vertex = vertex;
	}
	
	/** Set a topological vertex entity.
	 * @param vertex : a TopoDS_Vertex instance, the topological vertex
	 */
	public void setVertex(TopoDS_Vertex vertex)
	{
		this.vertex = vertex;
	}
	
	/**
	 * Get the topological vertex entity.
	 * @return TopoDS_Vertex : the topological vertex
	 */
	public TopoDS_Vertex getVertex()
	{
		return vertex;
	}
	
	/** Test vertex position equality.
	 * @param o : an Object instance, the node position to compare with the current vertex position
	 * @return boolean : set to \c true if node positions are same, \c false if not
	 */
	public boolean equals(Object o)
	{
		if(o instanceof PST_VertexPosition)
		{
			PST_VertexPosition p=(PST_VertexPosition) o;
			if (param==p.param) return vertex.isSame(p.vertex);
		}
		return false;
	}
	
	/**
	 * Return the topological entity related to the current vertex position.
	 * @return TopoDS_Shape : the topological vertex entity.
	 */
	public TopoDS_Shape getShape()
	{
		return (TopoDS_Shape)getVertex();
	}
	
	/**
	 * Return the 2D coordinates of the node.
	 * @return double[] : an array containing both node's coordinates (in 2D space)
	 */
	public double[] getUV()
	{
		return uv;
	}
	
	
}
