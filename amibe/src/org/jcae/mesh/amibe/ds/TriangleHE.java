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

public class TriangleHE extends Triangle
{
	// In this class, adjacency relations are supported by an HalfEdge instance,
	// which is created by ElementFactory.createTriangle.
	
	private HalfEdge e0;

	/**
	 * Constructor.
	 */
	public TriangleHE(TriangleTraitsBuilder ttb)
	{
		super(ttb);
		// This constructor is called by ElementFactory.createTriangle,
		// and it also creates an HalfEdge instance and set e0.
	}

	@Override
	public final void copy(Triangle src)
	{
		super.copy(src);
		e0.copy(((TriangleHE) src).e0);
	}
	
	/**
	 * Gets an <code>AbstractHalfEdge</code> instance bound to this triangle.
	 * This method returns <code>HalfEdge</code> 0.
	 * @return  an <code>HalfEdge</code> instance bound to this triangle
	 */
	@Override
	public HalfEdge getAbstractHalfEdge()
	{
		return e0;
	}

	/**
	 * Gets an <code>AbstractHalfEdge</code> instance bound to this triangle.
	 * This method returns <code>HalfEdge</code> 0, argument is unused
	 * @param  that   dummy argument
	 * @return  an <code>HalfEdge</code> instance bound to this triangle
	 */
	@Override
	public HalfEdge getAbstractHalfEdge(AbstractHalfEdge that)
	{
		return e0;
	}

	/**
	 * Sets <code>HalfEdge</code> 0 of this triangle.  This method must not
	 * be called, it is meant to be used by {@link ElementFactory#createTriangle}.
	 *
	 * @param e  edge to set as edge 0 of this triangle
	 */
	void setHalfEdge(HalfEdge e)
	{
		e0 = e;
	}

	/**
	 * Sets attributes for all edges of this triangle.
	 *
	 * @param attr  attributes to set on edges
	 */
	@Override
	public void setAttributes(int attr)
	{
		e0.setAttributes(attr);
		e0.next().setAttributes(attr);
		e0.prev().setAttributes(attr);
	}
	
	/**
	 * Resets attributes for all edges of this triangle.
	 *
	 * @param attr  attributes to reset on edges
	 */
	@Override
	public void clearAttributes(int attr)
	{
		e0.clearAttributes(attr);
		e0.next().clearAttributes(attr);
		e0.prev().clearAttributes(attr);
	}
	
	/**
	 * Checks if some attributes of this triangle are set.
	 *
	 * @param attr  attributes to check
	 * @return <code>true</code> if any edge of this triangle has
	 * one of these attributes set, <code>false</code> otherwise
	 */
	@Override
	public boolean hasAttributes(int attr)
	{
		return e0.hasAttributes(attr) || e0.next().hasAttributes(attr) || e0.prev().hasAttributes(attr);
	}
	
}
