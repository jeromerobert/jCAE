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
import org.jcae.mesh.util.Pair;

/** Class to characterise a specific node witch is defined along a geometric edge.
 *
 * \n
 * @author Cyril BRANDY & Marie-Helene GARAT.
 */
public class PST_LinePosition extends PST_Position
{
	/** The geometric edge entity. */
	public TopoDS_Edge edge;
	/** The curvilinear coordinate of the node along the geometric edge. */
	public double t;
	
	/** Default constructor. */
	public PST_LinePosition()
	{
	}
	
	/**
	 * Return the type of node position.
	 * @return int : the type of node position
	 */
	public int getType()
	{
		return PST_Position.EDGE;
	}
	
	/** Constructor with the geometric edge and corresponding node parameter.
	 * @param e : a TopoDS_Edge instance, the geometric edge containing the node.
	 * @param t : a double value, the curvilinear coordinate of the node along the edge.
	 */
	public PST_LinePosition(TopoDS_Edge e, double t)
	{
		this.edge = e;
		this.t = t;
	}
	
	/** Set the node position along an edge.
	 * @param e : a TopoDS_Edge instance, the geometrical edge
	 * @param t : a double value, the node's curvilinear coordinate on the edge
	 */
	public void setCurvePosition(TopoDS_Edge e, double t)
	{
		edge = e;
		this.t = t;
	}
	
	/**
	 * Get the node position along an geometrical edge.
	 * @return Pair : a pair defining the geometrical edge and the node's curvilinear coordinate on the edge.
	 */
	public Pair getCurvePosition()
	{
		return new Pair(edge, new Double(t));
	}
	
	/**
	 * Return the topological entity related to the current line position.
	 */
	public TopoDS_Shape getShape()
	{
		return (TopoDS_Shape)edge;
	}
	
	/**
	 * Return the curvilinear coordinate of the node on the edge
	 * @return double : the curvilinear coordinate of the node
	 */
	public double getParam()
	{
		return t;
	}
	
	/** Set the geometrical edge of the node.
	 * @param E : a TopoDS_Edge instance, the geometric edge.
	 */
	public void setEdge(TopoDS_Edge E)
	{
		this.edge=E;
	}
	
	/** Test the node position equality.
	 * @param o : an Object instance, the entity to compare with the current
	 */
	public boolean equals(Object o)
	{
		if(o instanceof PST_LinePosition)
		{
			PST_LinePosition p=(PST_LinePosition)o;
			if (t!=p.t) return false;
			if (!edge.equals(p.edge)) return false;		
			return true;
		} else return false;
	}
}
