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
import org.jcae.mesh.amibe.traits.HalfEdgeTraitsBuilder;
import java.util.Iterator;
import java.util.Map;

/**
 * Abstract class to define common methods on edges.
 * We use an half-edge structure to perform
 * mesh traversal.  Vertices can be obtained
 * by {@link #origin}, {@link #destination} and
 * {@link #apex}, and the triangle found at the
 * left of an edge is given by {@link #getTri}.
 */
public abstract class AbstractHalfEdge
{
	//  User-defined traits
	protected final HalfEdgeTraitsBuilder traitsBuilder;
	protected final Traits traits;

	/**
	 * Swaps an edge.
	 *
	 * This routine swaps an edge (od) to (na).  (on) is returned
	 * instead of (na), because this helps turning around o, eg.
	 * at the end of {@link org.jcae.mesh.amibe.patch.VirtualHalfEdge2D#split3}.
	 *
	 *          d                    d
	 *          .                    .
	 *         /|\                  / \
	 *        / | \                /   \
	 *       /  |  \              /     \
	 *    a +   |   + n  ---&gt;  a +-------+ n
	 *       \  |  /              \     /
	 *        \ | /                \   /
	 *         \|/                  \ /
	 *          '                    '
	 *          o                    o
	 * @return swapped edge
	 * @throws IllegalArgumentException if edge is on a boundary or belongs
	 * to an outer triangle.
	 * @see Mesh#edgeSwap
	 */
	protected abstract AbstractHalfEdge swap();

	/**
	 * Checks that triangles are not inverted if origin vertex is moved.
	 *
	 * @param newpt  the new position to be checked.
	 * @return <code>false</code> if the new position produces
	 *    an inverted triangle, <code>true</code> otherwise.
	 */
	public abstract boolean checkNewRingNormals(double [] newpt);

	/**
	 * Checks whether an edge can be contracted into a given vertex.
	 *
	 * @param n  the resulting vertex
	 * @return <code>true</code> if this edge can be contracted into the single vertex n, <code>false</code> otherwise.
	 * @see Mesh#canCollapseEdge
	 */
	protected abstract boolean canCollapse(AbstractVertex n);

	/**
	 * Contracts an edge.
	 *
	 * @param m  mesh
	 * @param n  the resulting vertex
	 * @return edge starting from <code>n</code> and pointing to original apex
	 * @throws IllegalArgumentException if edge belongs to an outer triangle,
	 * because there would be no valid return value.  User must then run this
	 * method against symmetric edge, this is not done automatically.
	 * @see Mesh#edgeCollapse
	 */
	protected abstract AbstractHalfEdge collapse(AbstractMesh m, AbstractVertex n);

	/**
	 * Splits an edge.  This is the opposite of {@link #collapse}.
	 *
	 * @param m  mesh
	 * @param n  the resulting vertex
	 * @see Mesh#vertexSplit
	 */
	protected abstract AbstractHalfEdge split(AbstractMesh m, AbstractVertex n);

	/**
	 * Sets the edge tied to this object.
	 *
	 * @param e  the edge tied to this object
	 */
	public abstract void glue(AbstractHalfEdge e);

	/**
	 * Returns the area of this triangle.
	 *
	 * @return the area of this triangle.
	 */
	public abstract double area();

	public abstract Iterator<AbstractHalfEdge> fanIterator();

	/**
	 * Numeric constants for edge attributes.  Set if edge is on
	 * boundary.
	 * @see #setAttributes
	 * @see #hasAttributes
	 * @see #clearAttributes
	 */
	public static final int BOUNDARY = 1 << 0;
	/**
	 * Numeric constants for edge attributes.  Set if edge is outer.
	 * (Ie. one of its end point is {@link Mesh#outerVertex})
	 * @see #setAttributes
	 * @see #hasAttributes
	 * @see #clearAttributes
	 */
	public static final int OUTER    = 1 << 1;
	/**
	 * Numeric constants for edge attributes.  Set if edge had been
	 * swapped.
	 * @see #setAttributes
	 * @see #hasAttributes
	 * @see #clearAttributes
	 */
	public static final int SWAPPED  = 1 << 2;
	/**
	 * Numeric constants for edge attributes.  Set if edge had been
	 * marked (for any operation).
	 * @see #setAttributes
	 * @see #hasAttributes
	 * @see #clearAttributes
	 */
	public static final int MARKED   = 1 << 3;
	/**
	 * Numeric constants for edge attributes.  Set if edge is the inner
	 * edge of a quadrangle.
	 * @see #setAttributes
	 * @see #hasAttributes
	 * @see #clearAttributes
	 */
	public static final int QUAD     = 1 << 4;
	/**
	 * Numeric constants for edge attributes.  Set if edge is non
	 * manifold.
	 * @see #setAttributes
	 * @see #hasAttributes
	 * @see #clearAttributes
	 */
	public static final int NONMANIFOLD = 1 << 5;
	
	protected static final Integer [] int3 = new Integer[3];
	static {
		int3[0] = Integer.valueOf(0);
		int3[1] = Integer.valueOf(1);
		int3[2] = Integer.valueOf(2);
	}

	public AbstractHalfEdge()
	{
		traitsBuilder = null;
		traits = null;
	}
	public AbstractHalfEdge(HalfEdgeTraitsBuilder builder)
	{
		traitsBuilder = builder;
		if (builder != null)
			traits = builder.createTraits();
		else
			traits = null;
	}

	public abstract AbstractHalfEdge sym(AbstractHalfEdge that);
	public abstract AbstractHalfEdge sym();
	public abstract AbstractHalfEdge next(AbstractHalfEdge that);
	public abstract AbstractHalfEdge next();
	public abstract AbstractHalfEdge prev(AbstractHalfEdge that);
	public abstract AbstractHalfEdge prev();
	public abstract AbstractHalfEdge nextOrigin(AbstractHalfEdge that);
	public abstract AbstractHalfEdge nextOrigin();
	public abstract AbstractHalfEdge nextOriginLoop();

	public abstract int getLocalNumber();
	public abstract Triangle getTri();
	public abstract Object getAdj();
	public abstract Map<Triangle, Integer> getAdjNonManifold();
	public abstract void setAdj(Object link);
	public abstract Vertex origin();
	public abstract Vertex destination();
	public abstract Vertex apex();
	public abstract void setAttributes(int attr);
	public abstract void clearAttributes(int attr);
	public abstract boolean hasAttributes(int attr);
}
