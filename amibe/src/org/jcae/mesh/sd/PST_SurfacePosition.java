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

/** Class to characterise a specific node witch is defined onto a geometric surface.
 *
 * \n
 * @author Cyril BRANDY & Marie-Helene GARAT.
 */
public class PST_SurfacePosition extends PST_Position
{
	/** the geometric surface. */
	public TopoDS_Face face;
	/** the first 2D coordinate of the node lying the surface. */
	public double u;
	/** The second 2D coordinate of the node lying the surface. */
	public double v;
	
	/** Default constructor. */
	public PST_SurfacePosition()
	{
	}
	
	/**
	 * Return the type of node position.
	 * @return int : the type of node position
	 */
	public int getType()
	{
		return PST_Position.SURFACE;
	}
	
	/** Constructor with the surface and both 2D coordinates of the node.
	 * @param face : a TopoDS_Face instance, the geometric surface.
	 * @param u : a double value, first 2D coordinate of the node in relation to the surface.
	 * @param v : a double value, second 2D coordinate of the node in relation to the surface.
	 */
	public PST_SurfacePosition(TopoDS_Face face, double u, double v)
	{
		this();
		this.face = face;
		this.u = u;
		this.v = v;
	}
	
	/** Set the geometric surface and 2D coordinates of the node.
	 * @param face : a TopoDS_Face instance, the geometric surface onto which the node is defined
	 * @param u : a double value, first 2D coordinate of the node in relation to the surface.
	 * @param v : a double value, second 2D coordinate of the node in relation to the surface.
	 */
	public void setSurfacePosition(TopoDS_Face face, double u, double v)
	{
		this.face = face;
		this.u = u;
		this.v = v;
	}
	
	/**
	 * Return the the 2D coordinates of the node related to the surface.
	 * @return Pair : a pair containing both 2D coordinates of the node.
	 */
	public Pair getSurfacePosition()
	{
		return new Pair(new Double(u), new Double(v));
	}
	
	public double [] getParam()
	{
		double [] uv = new double[2];
		uv[0] = u; uv[1] = v;
		return uv;
	}
	
	/**
	 * Method the geometric surface onto which the node is defined.
	 * @return TopoDS_Face : the geometric surface.
	 */
	public TopoDS_Face getSurface()
	{
		return face;
	}
	
	/**
	 * Return the topological entity related to the current surface position.
	 * @return TopoDS_Shape : the topological surface entity.
	 */
	public TopoDS_Shape getShape()
	{
		return (TopoDS_Shape)face;
	}
	
	/** Test surface position equality.
	 * @param o : an Object instance, the node position to compare with the current surface position
	 * @return boolean : set to \c true if node positions are same, \c false if not
	 */
	public boolean equals(Object o)
	{
		if(o instanceof PST_SurfacePosition)
		{
			PST_SurfacePosition p=(PST_SurfacePosition) o;
			if (!(u==p.u))return false;
			if (!(v==p.v)) return false;
			return face.equals(p.face);
		} else return false;
	}
	
	/** Test the surface position is IN or OUT the surface topology
	 * @return double[] : the bornes of the surface in the parametric space
	 *				      return null if the surface position is IN the surface.
	 */
	public double[] isInOut()
	{
		Geom_Surface surface = BRep_Tool.surface(face);
		// out of bounds ?
		double bornes [] = new double [4];
		bornes = surface.bounds();
		if ( (u<bornes[0]) || (u>bornes[1]) || (v<bornes[2]) || (v>bornes[3]) )
			return bornes;
		else return null;
	}

}
