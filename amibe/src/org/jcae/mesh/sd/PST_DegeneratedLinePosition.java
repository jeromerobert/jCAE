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

import org.jcae.opencascade.jni.TopoDS_Edge;
import org.jcae.opencascade.jni.*;

/** Class to characterise a specific node witch is defined along a degenerated edge.
 * 
 * \n
 * @author Cyril BRANDY & Marie-Helene GARAT.
 */
public class PST_DegeneratedLinePosition extends PST_LinePosition {
	/** The topological surface. */
	private TopoDS_Face face;
	/** The topological vertex. */
	private TopoDS_Vertex v;
	
	/**
	 * Default constructor.
	 */
	public PST_DegeneratedLinePosition()
	{	
		face=null;
	}
	
	/**
	 * Return the type of node position.
	 * @return int : the type of node position
	 */
	public int getType()
	{
		return PST_Position.DEGENERATEDLINE;
	}
	
	/**	
	 * Constructor with the edge, the curvilinear coordinate, the vertex and the surface.
	 * @param e : a TopoDS_Edge instance, the geometric curve
	 * @param t : a double value, the curvilinear coordinate of the node on edge
	 * @param v : a TopoDS_Vertex instance, the vertex entity
	 * @param face : a TopoDS_Face instance, the geometric surface
	 */
	public PST_DegeneratedLinePosition(TopoDS_Edge e, double t, TopoDS_Vertex v,TopoDS_Face face) {
		super(e, t);
		this.face=face;
		this.v=v;
	}
	
	/**
	 * Return the topological surface underlying.
	 * @return TopoDS_Face : the surface containing node.
	 */
	public TopoDS_Face getGeometry() {
        return face;
    }
    
	/**
	 * Return the geometric node related to the node position.
	 * @return TopoDS_Vertex : the vertex related to the node position.
	 */
    public TopoDS_Vertex getVertex(){
    	return v;
    }
    
	/**
	 * Test if the vertex related to node position exists.
	 * @return boolean : set to \c true if vertex exists, \c false if not
	 */
    public boolean isVertex(){
    	return (v!=null);
    }
}
