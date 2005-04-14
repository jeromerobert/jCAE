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

package org.jcae.mesh.mesher.ds;

import org.jcae.mesh.mesher.ds.MNode1D;
import org.jcae.mesh.cad.*;

import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 * 2D node.
 */
public class MNode2D
{
	private static Logger logger=Logger.getLogger(MNode2D.class);

	//  The natural coordinates of the node
	private double[] param = new double[2];
	
	//  Label of the geometrical node, if any
	private int ref1d = -1;
	
	//  Node label
	private int label = -1;
	
	//  ID used for debugging purpose
	private static int id = 0;
	private static HashMap mapHashcodeToID;
	//  Initialize mapHashcodeToID
	static { assert(null != (mapHashcodeToID = new HashMap())); }
	
	/**
	 * Creates a <code>MNode2D</code> instance by specifying its 2D coordinates.
	 *
	 * @param u  coordinate along the X-axis.
	 * @param v  coordinate along the Y-axis.
	 */
	public MNode2D(double u, double v)
	{
		param[0] = u;
		param[1] = v;
		assert(setID());
	}
	
	/**
	 * Creates a <code>MNode2D</code> instance by projecting a
	 * <code>MNode1D</code> node to the current surface.
	 * The <code>MNode1D</code> instance can either be a vertex (in which
	 * case <code>C2d</code> is null and the vertex is retrieved with
	 * <code>pt.getRef()</code>) or a discretization point added during edge
	 * tessellation.
	 *
	 * @param pt  the <code>MNode1D</code> being projected.
	 * @param C2d  a 2D curve representing the edge on which <code>pt</code>
	 * lies.
	 * @param F  current topological face.
	 */
	public MNode2D(MNode1D pt, CADGeomCurve2D C2d, CADFace F)
	{
		ref1d = pt.getMaster().getLabel();
		if (null != C2d)
			param = C2d.value(pt.getParameter());
		else
		{
			CADVertex V = pt.getCADVertex();
			if (null == V)
				throw new java.lang.RuntimeException("Error in MNode2D()");
			param = V.parameters(F);
		}
		assert(setID());
	}
	
	private boolean setID()
	{
		id++;
		mapHashcodeToID.put(this, new Integer(id));
		return true;
	}
	
	/**
	 * Returns the public identifer.
	 *
	 * @return the public identifer.
	 */
	public int getID()
	{
		if (id > 0)
			return ((Integer)mapHashcodeToID.get(this)).intValue();
		else
			return hashCode();
	}
	
	/**
	 * Returns the coordinate along X-axis.
	 *
	 * @return the coordinate along X-axis.
	 */
	public double getU()
	{
		return param[0];
	}
	
	/**
	 * Returns the coordinate along Y-axis.
	 *
	 * @return the coordinate along Y-axis.
	 */
	public double getV()
	{
		return param[1];
	}
	
	/**
	 * Returns the coordinate array.
	 *
	 * @return the coordinate array.
	 */
	public double[] getUV()
	{
		return param;
	}
	
	/**
	 * Sets coordinates.
	 *
	 * @param u   coordinate along X-axis.
	 * @param v   coordinate along Y-axis.
	 */
	public void setUV(double u, double v)
	{
		param[0] = u;
		param[1] = v;
	}
	
	/**
	 * Sets node label.
	 *
	 * @param l  node label
	 */
	public void setLabel(int l)
	{
		label = l;
	}
	
	/**
	 * Returns the node label.
	 *
	 * @return the node label.
	 */
	public int getLabel()
	{
		return label;
	}
	
	/**
	 * Returns a reference to a <code>MNode1D</code> object.
	 * When an edge is common to several faces, boundary nodes
	 * must not be inserted twice in the final mesh.  Uniqueness
	 * is tested by checking that <code>ref1d</code> is null or unique.
	 *
	 * @return a reference to a <code>MNode1D</code> object.
	 **/
	public int getRef()
	{
		return ref1d;
	}
	
	/**
	 * Returns <code>true</code> if this node can be moved.
	 *
	 * @return <code>true</code> if this node can be moved.
	 **/
	public boolean isMutable()
	{
		return (-1 == ref1d && label == -1);
	}
	
	public String toString()
	{
		String r="MNode2D: id="+getID()+
			" "+param[0]+" "+param[1];
		if (-1 != ref1d)
			r+=" ref1d="+ref1d;
		if (-1 != label)
			r+=" label="+label;
		return r;
	}
}
