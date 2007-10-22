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

import org.jcae.mesh.amibe.traits.TriangleTraitsBuilder;
import java.io.Serializable;

/**
 * A triangle containing adjacency relations.
 */
public abstract class Triangle extends AbstractTriangle implements Serializable
{
	/**
	 * Wrapper object to which mathods on adjacency relations are delegated.
	 */
	protected AdjacencyWrapper adj = null;

	public Triangle(TriangleTraitsBuilder ttb)
	{
		super(ttb);
	}

	public final void copy(Triangle src)
	{
		super.copy(src);
		adj.copy(src.adj);
	}
	
	/**
	 * Return the adjacent AbstractTriangle.
	 * Note: this routine is not very helpful, caller can only check
	 * whether the returned object is null or if its type is AbstractTriangle.
	 * This can be performed by checking {@link AbstractHalfEdge#BOUNDARY}
	 * and {@link AbstractHalfEdge#NONMANIFOLD} attributes.
	 *
	 * @param num  the local number of this edge.
	 * @return the adjacent AbstractTriangle.
	 */
	public Object getAdj(int num)
	{
		return adj.getAdj(num);
	}
	
	/**
	 * Set the AbstractTriangle adjacent to an edge.
	 *
	 * @param num  the local number of this edge.
	 * @param link  the adjacent AbstractTriangle.
	 */
	public void setAdj(int num, Object link)
	{
		adj.setAdj(num, link);
	}
	
	/**
	 * Sets attributes for all edges of this triangle.
	 *
	 * @param attr  attributes to set on edges
	 */
	public void setAttributes(int attr)
	{
		adj.setAttributes(attr);
	}
	
	/**
	 * Resets attributes for all edges of this triangle.
	 *
	 * @param attr  attributes to reset on edges
	 */
	public void clearAttributes(int attr)
	{
		adj.clearAttributes(attr);
	}
	
	/**
	 * Checks if some attributes of this triangle are set.
	 *
	 * @param attr  attributes to check
	 * @return <code>true</code> if any edge of this triangle has
	 * one of these attributes set, <code>false</code> otherwise
	 */
	public boolean hasAttributes(int attr)
	{
		return adj.hasAttributes(attr);
	}
	
	public int getEdgeAttributes(int num)
	{
		return adj.getEdgeAttributes(num);
	}
	
	public void setEdgeAttributes(int num, int attributes)
	{
		adj.setEdgeAttributes(num, attributes);
	}
	
	public abstract AbstractHalfEdge getAbstractHalfEdge();

	@Override
	public String toString()
	{
		String r = super.toString();
		if (adj != null)
			r +="\n"+adj.toString();
		return r;
	}

}
