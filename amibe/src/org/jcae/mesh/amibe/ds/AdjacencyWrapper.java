/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005,2006, by EADS CRC
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

/**
 * Interface describing adjacency relations between triangles.
 */
public interface AdjacencyWrapper
{

	/**
	 * Gets symmetric of edge <code>num</code>.
	 *
	 * @return  symmetric of edge <code>num</code>.
	 */
	public AdjacencyWrapper getAdj(int num);

	/**
	 * Sets symmetric of edge <code>num</code>.
	 *
	 * @param  link   object to link to edge <code>num</code>.
	 */
	public void setAdj(int num, AdjacencyWrapper link);

	/**
	 * Sets attributes for all edges of this triangle.
	 *
	 * @param attr  attributes to set on edges
	 */
	public void setAttributes(int attr);

	/**
	 * Resets attributes for all edges of this triangle.
	 *
	 * @param attr  attributes to reset on edges
	 */
	public void clearAttributes(int attr);

	/**
	 * Checks if some attributes of this triangle are set.
	 *
	 * @param attr  attributes to check
	 * @return <code>true</code> if any edge of this triangle has
	 * one of these attributes set, <code>false</code> otherwise
	 */
	public boolean hasAttributes(int attr);

}
