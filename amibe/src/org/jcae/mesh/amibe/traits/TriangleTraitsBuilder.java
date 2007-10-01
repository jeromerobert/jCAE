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

package org.jcae.mesh.amibe.traits;

public class TriangleTraitsBuilder extends TraitsBuilder
{
	private static final int BITSHALLOWHALFEDGE = 13;
	private static final int BITHALFEDGE     = 14;

	public static final int SHALLOWHALFEDGE  = 1 << BITSHALLOWHALFEDGE;
	public static final int HALFEDGE         = 1 << BITHALFEDGE;

	/**
	 * Let {@link org.jcae.mesh.amibe.ds.ElementFactory#createTriangle} create
	 * {@link org.jcae.mesh.amibe.ds.TriangleVH} instances.
	 *
	 * @return  this instance
	 */
	public TriangleTraitsBuilder addShallowHalfEdge()
	{
		attributes |= SHALLOWHALFEDGE;
		return this;
	}

	/**
	 * Let {@link org.jcae.mesh.amibe.ds.ElementFactory#createTriangle} create
	 * {@link org.jcae.mesh.amibe.ds.TriangleHE} instances.
	 *
	 * @return  this instance
	 */
	public TriangleTraitsBuilder addHalfEdge()
	{
		attributes |= HALFEDGE;
		return this;
	}

	// For performance reasons, adjacency relations are not stored in
	// traits, but directly in Triangle subclass.

}
