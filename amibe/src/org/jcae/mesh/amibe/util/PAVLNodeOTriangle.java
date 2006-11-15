/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005, by EADS CRC

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

package org.jcae.mesh.amibe.util;

import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.OTriangle;
import org.apache.log4j.Logger;

/**
 * Node of PAVL binary trees designed to store {@link OTriangle} instances.
 * As {@link OTriangle} are handles on {@link Triangle} instances, they
 * must not be copied into a tree.  A reference to the underlying
 * {@link Triangle} and its local number are copied instead.
 */
public class PAVLNodeOTriangle extends PAVLNode
{
	/**
	 * Local number of this edge into its {@link Triangle}.
	 */
	public int localNumber;
	
	public PAVLNodeOTriangle(OTriangle ot, double k)
	{
		super(ot.getTri(), k);
		localNumber = ot.getLocalNumber();
		child = new PAVLNodeOTriangle[2];
	}
	
	/**
	 * Get the edge local number in its {@link Triangle}.
	 *
	 * @return the edge local number.
	 */
	public int getLocalNumber()
	{
		return localNumber;
	}
	
	/**
	 * Get the {@link Triangle} containing the stored {@link OTriangle}.
	 *
	 * @return the {@link Triangle} containing the stored {@link OTriangle}.
	 */
	public Triangle getTriangle()
	{
		return (Triangle) data;
	}
	
}
