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

public class AbstractVertex
{
	//  User-defined traits
	protected final VertexTraitsBuilder traitsBuilder;
	protected final Traits traits;
	/**
	 * 2D or 3D coordinates.
	 */
	protected final double [] param;

	public AbstractVertex()
	{
		traitsBuilder = null;
		traits = null;
		param = new double[2];
	}
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
	 * Create a Vertex for a 3D mesh.
	 *
	 * @param x  first coordinate.
	 * @param y  second coordinate.
	 * @param z  third coordinate.
	 */
	public AbstractVertex(double x, double y, double z)
	{
		traitsBuilder = null;
		traits = null;
		param = new double[3];
		param[0] = x;
		param[1] = y;
		param[2] = z;
	}
	
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
	
}
