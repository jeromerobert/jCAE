/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.
 
	Copyright (C) 2003,2004,2005
                  Jerome Robert <jeromerobert@users.sourceforge.net>
 
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

package org.jcae.mesh.amibe.ds;

import org.jcae.mesh.cad.CADFace;
import org.jcae.mesh.cad.CADGeomSurface;
import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 * 3D node.
 */
public class MNode3D
{
	private static Logger logger=Logger.getLogger(MNode3D.class);

	//  The natural coordinates of the node
	private double[] param = new double[3];
	
	//  Normal to the surface
	private double[] normal = null;
	
	//  Link to the geometrical node, if any
	private int ref1d;
	
	//  ID used for debugging purpose
	private static int id = 0;
	private static HashMap mapHashcodeToID;
	//  Initialize mapHashcodeToID
	static { assert(null != (mapHashcodeToID = new HashMap())); };
	
	/**
	 * Creates a <code>MNode3D</code> instance by specifying its 3D coordinates and its label.
	 *
	 * @param p  a <code>double[3]</code> array containing 3D coordinates.
	 * @param label  a unique label if this node is on a boundary, <code>-1</code> otherwise.
	 */
	public MNode3D(double [] p, int label)
	{
		for (int i = 0; i < 3; i++)
			param[i] = p[i];
		ref1d = label;
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
	 * Returns an arrary containing 3D coordinates.
	 *
	 * @return an arrary containing 3D coordinates.
	 */
	public double[] getXYZ()
	{
		return param;
	}
	
	/**
	 * Returns the coordinate along X-axis.
	 *
	 * @return the coordinate along X-axis.
	 */
	public double getX()
	{
		return param[0];
	}
	
	/**
	 * Returns the coordinate along Y-axis.
	 *
	 * @return the coordinate along Y-axis.
	 */
	public double getY()
	{
		return param[1];
	}
	
	/**
	 * Returns the coordinate along Z-axis.
	 *
	 * @return the coordinate along Z-axis.
	 */
	public double getZ()
	{
		return param[2];
	}
	
	/**
	 * Returns the distance to another <code>MNode3D</code> instance.
	 *
	 * @param end  the node to which distance is computed.
	 * @return the distance to <code>end</code>.
	 **/
	public double distance(MNode3D end)
	{
		double x=param[0] - end.param[0];
		double y=param[1] - end.param[1];
		double z=param[2] - end.param[2];
		return Math.sqrt(x*x+y*y+z*z);
	}
	
	/**
	 * Returns the angle at which a segment is seen.
	 *
	 * @param n1  first node
	 * @param n2  second node
	 * @return the angle at which the segment is seen.
	 **/
	public double angle(MNode3D n1, MNode3D n2)
	{
		double normPn1 = distance(n1);
		double normPn2 = distance(n2);
		if ((normPn1 == 0.0) || (normPn2 == 0.0))
			return 0.0;
		double normPn3 = n1.distance(n2);
		double mu, alpha;
		if (normPn1 < normPn2)
		{
			double temp = normPn1;
			normPn1 = normPn2;
			normPn2 = temp;
		}
		if (normPn2 < normPn3)
			mu = normPn2 - (normPn1 - normPn3);
		else
			mu = normPn3 - (normPn1 - normPn2);
		alpha = 2.0 * Math.atan(Math.sqrt(
			((normPn1-normPn2)+normPn3)*mu/
				((normPn1+(normPn2+normPn3))*((normPn1-normPn3)+normPn2))
		));
        	return alpha;
	}
	
	/**
	 * Returns <code>true</code> if this node can be moved.
	 *
	 * @return <code>true</code> if this node can be moved.
	 **/
	public boolean isMutable()
	{
		return (-1 == ref1d);
	}
	
	/**
	 * Returns a reference to a <code>MNode1D</code> object.
	 * When an edge is common to several faces, boundary nodes
	 * must not be inserted twice.  Uniqueness is tested by checking
	 * that <code>ref1d</code> is null or unique.
	 *
	 * @return a reference to a <code>MNode1D</code> object.
	 **/
	public int getRef()
	{
		return ref1d;
	}
	
	/**
	 * Resets a reference to a <code>MNode1D</code> object.
	 **/
	public void clearRef()
	{
		ref1d = -1;
	}
	
	public void addNormal(double [] n)
	{
		if (normal == null)
		{
			normal = new double[3];
			for (int i = 0; i < 3; i++)
				normal[i] = n[i];
		}
		else
		{
			double [] n2 = new double[normal.length+3];
			System.arraycopy(normal, 0, n2, 0, normal.length);
			for (int i = 0; i < 3; i++)
				n2[normal.length+i] = n[i];
			normal = n2;
		}
	}
	public double [] getNormal()
	{
		return normal;
	}
	
	public String toString()
	{
		String r="MNode3D: id="+getID()+
			" "+param[0]+" "+param[1]+" "+param[2];
		if (-1 != ref1d)
			r+=" ref1d="+ref1d;
		return r;
	}
}
