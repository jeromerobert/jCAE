/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2006, by EADS CRC
    Copyright (C) 2007, by EADS France

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

package org.jcae.mesh.amibe.ds;

import org.jcae.mesh.amibe.traits.Traits;
import org.jcae.mesh.amibe.traits.VertexTraitsBuilder;
import org.jcae.mesh.amibe.metrics.Matrix3D;

public class AbstractVertex
{
	//  User-defined traits
	protected final VertexTraitsBuilder traitsBuilder;
	protected final Traits traits;
	/**
	 * 2D or 3D coordinates.
	 */
	protected final double [] param;
	//  ref1d > 0: link to the geometrical node
	//  ref1d = 0: inner node
	//  ref1d < 0: node on an inner boundary
	protected int ref1d = 0;

	public AbstractVertex(VertexTraitsBuilder builder)
	{
		traitsBuilder = builder;
		if (builder != null)
			traits = builder.createTraits();
		else
			traits = null;
		param = new double[2];
	}

	/**
	 * Creates a vertex for a 3D mesh.
	 *
	 * @param builder  traits builder
	 * @param x  first coordinate.
	 * @param y  second coordinate.
	 * @param z  third coordinate.
	 */
	public AbstractVertex(VertexTraitsBuilder builder, double x, double y, double z)
	{
		traitsBuilder = builder;
		if (builder != null)
			traits = builder.createTraits();
		else
			traits = null;
		param = new double[3];
		param[0] = x;
		param[1] = y;
		param[2] = z;
	}
	
	/**
	 * Gets 1D reference of this node.
	 *
	 * @return 1D reference of this node
	 */
	public int getRef()
	{
		return ref1d;
	}
	
	/**
	 * Sets 1D reference of this node.
	 *
	 * @param l  1D reference of this node
	 */
	public void setRef(int l)
	{
		ref1d = l;
	}
	
	/**
	 * Gets coordinates of this vertex.
	 *
	 * @return coordinates of this vertex
	 */
	public double [] getUV ()
	{
		return param;
	}
	
	/**
	 * Sets 3D coordinates of this vertex.
	 *
	 * @param x  first coordinate of the new position
	 * @param y  second coordinate of the new position
	 * @param z  third coordinate of the new position
	 */
	public void moveTo(double x, double y, double z)
	{
		param[0] = x;
		param[1] = y;
		param[2] = z;
	}
	
	/**
	 * Returns the distance in 3D space.
	 *
	 * @param end  the node to which distance is computed.
	 * @return the distance to <code>end</code>.
	 **/
	public double distance3D(AbstractVertex end)
	{
		double x = param[0] - end.param[0];
		double y = param[1] - end.param[1];
		double z = param[2] - end.param[2];
		return Math.sqrt(x*x+y*y+z*z);
	}
	
	/**
	 * Returns the angle at which a segment is seen.
	 *
	 * @param n1  first node
	 * @param n2  second node
	 * @return the angle at which the segment is seen.
	 **/
	public double angle3D(AbstractVertex n1, AbstractVertex n2)
	{
		double normPn1 = distance3D(n1);
		double normPn2 = distance3D(n2);
		if ((normPn1 == 0.0) || (normPn2 == 0.0))
			return 0.0;
		double normPn3 = n1.distance3D(n2);
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
	 * Returns the outer product of two vectors.  This method
	 * computes the outer product of two vectors starting from
	 * the current vertex.
	 *
	 * @param n1  end point of the first vector
	 * @param n2  end point of the second vector
	 * @param work1  double[3] temporary array
	 * @param work2  double[3] temporary array
	 * @param ret array which will store the outer product of the two vectors
	 */
	public void outer3D(AbstractVertex n1, AbstractVertex n2, double [] work1, double [] work2, double [] ret)
	{
		for (int i = 0; i < 3; i++)
		{
			work1[i] = n1.param[i] - param[i];
			work2[i] = n2.param[i] - param[i];
		}
		Matrix3D.prodVect3D(work1, work2, ret);
	}
	
	public boolean isReadable()
	{
		return true;
	}
	
	public boolean isWritable()
	{
		return true;
	}
	
}
