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

/**
 * Abstract class to define common methods on edges.
 * We use an half-edge structure to perform mesh traversal.  Vertices can be
 * obtained by {@link #origin}, {@link #destination} and {@link #apex}, and the
 * triangle found at the left of an edge is given by {@link #getTri}.
 *
 * <h2>Geometrical primitives</h2>
 *
 * <p>
 * Consider the <code>AbstractHalfEdge</code> edge <code>e</code> between vertices
 * <em>A</em> and <em>B</em>, starting from <em>A</em>, in image below:
 * </p>
 * <p align="center"><img src="doc-files/AbstractHalfEdge-1.png" alt="[Drawing to illustrate geometrical primitives]"/></p>
 * <p>
 * The following methods can be applied to <code>e</code>:
 * </p>
 * <ul>
 *    <li><code>e.{@link #next}</code> and <code>e.{@link #prev}</code> get respectively next and previous
 *        edges in the same triangle <em>(ABC)</em> in a counterclockwise cycle.</li>
 *    <li><code>e.{@link #sym}</code> gets the opposite <code>AbstractHalfEdge</code>.</li>
 *    <li><code>e.{@link #nextOrigin}</code> returns next edge starting from the same origin <em>A</em>
 *        when cycling counterclockwise around <em>A</em>.</li>
 * </ul>
 * <p><strong>Warning:</strong>
 * As {@link VirtualHalfEdge} instances are handles to edges and not physical objects, these methods
 * modify current instance.  Another set of methods is defined to apply these transformations to
 * another instance, so that current instance is not modified.
 * </p>
 * <ul>
 *    <li><code>e.<a href="#next(org.jcae.mesh.amibe.ds.AbstractHalfEdge)">next</a>(f)</code> and
 *        <code>e.<a href="#prev(org.jcae.mesh.amibe.ds.AbstractHalfEdge)">prev</a>(f)</code>
 *        move <code>f</code> respectively to next and previous edges in the same triangle <em>(ABC)</em>
 *        in a counterclockwise cycle.</li>
 *    <li><code>e.<a href="#sym(org.jcae.mesh.amibe.ds.AbstractHalfEdge)">sym</a>(f)</code>
 *        moves <code>f</code> to opposite of <code>e</code>.</li>
 *    <li><code>e.<a href="#nextOrigin(org.jcae.mesh.amibe.ds.AbstractHalfEdge)">nextOrigin</a>(f)</code>
 *        moves <code>f</code> to the next edge starting from the same origin <em>A</em> when
 *        cycling counterclockwise around <em>A</em>.</li>
 * </ul>
 *
 * <p>
 * For convenience, derived classes may also define the following methods, which
 * are combinations of previous ones:
 * </p>
 * <ul>
 *    <li><code>e.prevOrigin()</code> moves counterclockwise to the previous edge
 *        starting from the same origin.</li>
 *    <li><code>e.nextDest()</code> and <code>e.prevDest()</code> move counterclockwise
 *        to the next (resp. previous) edge with the same destination vertex <em>B</em>.</li>
 *    <li><code>e.nextApex()</code> and <code>e.prevApex()</code> move counterclockwise
 *        to the next (resp. previous) edge with the same apical vertex <em>C</em>.</li>
 * </ul>
 *
 * <h2>Mesh Operations</h2>
 * <p>
 * These operations are abstract methods and are implemented by subclasses:
 * </p>
 * <dl>
 *   <dt>{@link #swap}</dt>
 *   <dd>Swaps an edge.  Return value has the same original and apical vertices as
 *       original edge.  Triangles and edges are modified, objects are not destroyed and
 *       inserted into mesh.
 *       <p align="center"><img src="doc-files/AbstractHalfEdge-4.png" alt="[Image showing edge swap]"/></p>
 *   </dd>
 *   <dt>{@link #split}</dt>
 *   <dd>Splits a vertex to create a new edge.  In figure below, <em>A</em> is duplicated into <em>N</em>,
 *       and two new triangles are created.  Return value has the same original and apical vertices as
 *       original edge, and its destination vertex is <em>N</em>.
 *       <p><strong>Warning:</strong> This method does not check that new triangles are not inverted.</p>
 *       <p align="center"><img src="doc-files/AbstractHalfEdge-3.png" alt="[Image showing vertex split]"/></p>
 *   </dd>
 *   <dt>{@link #collapse}</dt>
 *   <dd>Collapses an edge into a new point.  Triangles, edges and vertices are removed from mesh
 *       and replaced by new objects.  New point may be origin or destination points.
 *       Return value has the new point as its origin, and its apex is the same as in original edge.
 *       When <em>N</em> is <em>A</em>,
 *       <code><a href="#collapse(org.jcae.mesh.amibe.ds.AbstractMesh, org.jcae.mesh.amibe.ds.AbstractVertex)">collapse</a></code>
 *       is the opposite of 
 *       <code><a href="#split(org.jcae.mesh.amibe.ds.AbstractMesh, org.jcae.mesh.amibe.ds.AbstractVertex)">split</a></code>.
 *       <p><strong>Warning:</strong> This method does not check that triangles are not inverted.
 *       Method {@link #canCollapse} <strong>must</strong> have been called to
 *       ensure that this edge collapse is possible, otherwise errors may
 *       occur.  This method is not called automatically because it is
 *       sometimes costful.</p>
 *       <p align="center"><img src="doc-files/AbstractHalfEdge-2.png" alt="[Image showing edge collapse]"/></p>
 *   </dd>
 * </dl>
 * <p>
 * Return values have been chosen so that these methods always return an edge
 * which has the same apical vertex.  As will be explained below, they gracefully
 * work with boundaries and non-manifold meshes.
 * </p>
 *
 * <p>
 * These two methods may also be of interest when writing new algorithms:
 * </p>
 * <dl>
 *   <dt>{@link #canCollapse}</dt>
 *   <dd>Tells whether an edge can be collapsed and replaced by the given vertex.</dd>
 *   <dt>{@link #checkNewRingNormals}</dt>
 *   <dd>Tells whether triangles become inverted if origin point is moved at
 *       given location.</dd>
 * </dl>
 *
 * <h2>Boundaries</h2>
 *
 * <p>
 * In order to simplify mesh operations, outer triangles are added to opposite
 * side of free edges, by linking vertices on boundary to a special vertex
 * {@link Mesh#outerVertex} which represents a point at infinite.  With this
 * convention, all mesh edges have a symmetric edge.  On the other hand, all edges 
 * which have {@link Mesh#outerVertex} as an endpoint have no symmetric edges, so
 * in these special triangles, there is only one edge with a symmetric edge.
 * </p>
 * <p align="center"><img src="doc-files/AbstractHalfEdge-5.png" alt="[Drawing to illustrate outer triangles]"/></p>
 * <p>
 * All <code>AbstractHalfEdge</code> subclasses allow to set attributes on edges.
 * They are defined as bitwise OR values, these values are useful:
 * </p>
 * <dl>
 *   <dt><code>AbstractHalfEdge.BOUNDARY</code></dt>
 *   <dd>This edge is on a mesh boundary (either part of an outer triangle or of
 *       a regular triangle)
 *   <dt><code>AbstractHalfEdge.OUTER</code></dt>
 *   <dd>This edge is part of an outer triangle
 *   <dt><code>AbstractHalfEdge.NONMANIFOLD</code></dt>
 *   <dd>This edge is not manifold (see below)
 * </dl>
 * <p>
 * Other values, namely <code>AbstractHalfEdge.MARKED</code>, <code>AbstractHalfEdge.SWAPPED</code>
 * and <code>AbstractHalfEdge.QUAD</code> may be removed in future releases, please do not use them.
 * </p>
 *
 * <p>For instance, image below is an excerpt of previous image into which edge attributes
 * are displayed: <code>O</code> represents <code>AbstractHalfEdge.OUTER</code>
 * attribute, <code>B</code> represents <code>AbstractHalfEdge.BOUNDARY</code> and <code>|</code>
 * means that attributes are combined.
 * </p>
 * <p align="center"><img src="doc-files/AbstractHalfEdge-6.png" alt="[Drawing to illustrate edge attributes]"/></p>
 *
 * <p>
 * Attributes are handled by these methods:
 * </p>
 * <dl>
 *   <dt>{@link #setAttributes}</dt>
 *   <dd>Combines edge attribute with method argument.</dd>
 *   <dt>{@link #clearAttributes}</dt>
 *   <dd>Combines edge attribute with 2-complement of method argument, which means that
 *       attributes passed as argument are reset.</dd>
 *   <dt>{@link #hasAttributes}</dt>
 *   <dd>Checks whether edge attribute contains any of attributes passed as method argument.</dd>
 * </dl>
 *
 * <p>
 * With these outer triangles, we can also loop around vertices even on mesh boundaries.
 * The {@link #nextOriginLoop} method behaves like {@link #nextOrigin} when edge is not
 * outer, and when it is outer, it turns clockwise until boundary is reached.  For instance
 * on image below, <code>a.nextOriginLoop()</code> returns <code>b</code>, 
 * <code>b.nextOriginLoop()</code> returns <code>o</code>,
 * <code>o.nextOriginLoop()</code> returns <code>d</code> and
 * <code>d.nextOriginLoop()</code> returns <code>a</code>.
 * </p>
 * <p align="center"><img src="doc-files/AbstractHalfEdge-7.png" alt="[Drawing to illustrate nextOriginLoop method]"/></p>
 * <p>
 * With <code>nextOriginLoop</code>, it is easy to loop around a vertex without
 * having to care about boundaries; a canonical way to process all inner triangles 
 * by looping around edge origin is:
 * </p>
 * <pre>
 *   Vertex d = e.destination();
 *   do
 *   {
 *     if (!e.hasAttributes(AbstractHalfEdge.OUTER))
 *     {
 *       Triangle t = e.getTri();
 *       // Do something with t
 *     }
 *     e = e.nextOriginLoop();
 *   }
 *   while (e.destination() != d);
 * </pre>
 * <p>
 * Here is a more complete example to show how to process all neighbors of a manifold vertex:
 * </p>
 * <pre>
 *   // This sample code only works with manifold vertices!
 *   Triangle t = (Triangle) o.getLink();
 *   AbstractHalfEdge e = t.getAbstractHalfEdge();
 *   if (e.destination() == o)
 *     e = e.next();
 *   else if (e.apex() == o)
 *     e = e.prev();
 *   // Now e is an edge starting from vertex o.
 *   Vertex d = e.destination();
 *   do
 *   {
 *     if (!e.hasAttributes(AbstractHalfEdge.OUTER)
 *         || e.hasAttributes(AbstractHalfEdge.BOUNDARY|AbstractHalfEdge.NONMANIFOLD))
 *     {
 *       Vertex v = e.destination();
 *       // Do something with v
 *     }
 *     e = e.nextOriginLoop();
 *   }
 *   while (e.destination() != d);
 * </pre>
 *
 * <h2>Non-manifold edges</h2>
 * <p>
 * A non-manifold edge is bound to more than two triangles.  In image below, edges <code>(AB)</code>,
 * <code>(BC)</code> and <code>(CD)</code> belong to four triangles.
 * </p>
 *       <p align="center"><img src="doc-files/AbstractHalfEdge-8.png" alt="[Image of a non-manifold mesh]"/></p>
 * <p>
 * Edge attributes are printed in blue for left shell:
 * </p>
 *       <p align="center"><img src="doc-files/AbstractHalfEdge-9.png" alt="[Image showing edge attributes for a non-manifold mesh]"/></p>
 *
 * <p>
 * Virtual triangle added to the other side of non-manifold edge is not only useful for looping around vertices,
 * but it also allows looping around this non-manifold edge.  We explained that the two outer edges are not connected
 * to other triangles, so we have two free slots.  We use these two slots to build a circular doubly-linked list to
 * iterate over all half-edges connected together, as is done with radial-edge data structure.
 * </p>
 *
 * <p>
 * But how can we iterate over all neighbors of <code>A</code>?  If we look again at sample code just above,
 * it is obvious that not all neighbors are caught.  We could try to ask non-manifold edges to the rescue,
 * it does not seem trivial.  A simpler solution is to store a list of all triangles connected to each vertex,
 * but it consumes lots of memory.  We call <em>triangle fan</em> the set of triangles which are visited
 * when calling {@link #nextOriginLoop} iteratively.  If vertex is manifold, all neighbors are then reached.
 * If not, we take another triangle which has not been visited yet, and repeat the same procedure until
 * all neighbors have been reached.  A good compromise is to take a single triangle by triangle fans, and they
 * are stored into a triangle array because topology is usually not modified and number of triangle fans does
 * not change.  It is easy to modify sample code above, put it into a method taking a triangle as argument,
 * call it with <code>o.getLink()</code> if it is a <code>Triangle</code> instance, otherwise loop over
 * <code>Triangle[]</code> and call this method for each <code>Triangle</code> instance.
 * </p>
 *
 * <p>
 * Consider the same mesh, from which triangles <code>(ABE)</code>, <code>(CDF)</code> and their symmetric
 * ones have been removed.  Edges <code>(AB)</code> and <code>(CD)</code> are manifold, but <code>(BC)</code> is not.
 * Vertices <code>A</code> and <code>D</code> are manifold, but <code>B</code> and <code>C</code> and connected to
 * three triangle fans, while edge <code>(BC)</code> is connected to four triangles.  All mesh operations described
 * above can gracefully handle such geometries, the only problem is when we try to mesh a surface which is not
 * orientable.
 * </p>
 *       <p align="center"><img src="doc-files/AbstractHalfEdge-10.png" alt="[Image showing a non-manifold edge bound to four triangles while its endpoints are bound to three triangle fans]"/></p>
 *
 */
public abstract class AbstractHalfEdge
{
	//  User-defined traits
	protected final HalfEdgeTraitsBuilder traitsBuilder;
	protected final Traits traits;

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

	/**
	 * Moves to symmetric edge.
	 * @return  current instance after its transformation
	 */
	public abstract AbstractHalfEdge sym();

	/**
	 * Moves to symmetric edge.
	 * Make <code>that</code> instance be a copy of current
	 * instance, move it to its symmetric edge and return
	 * this instance.  Current instance is not modified.
	 *
	 * @param  that  instance where transformed edge is stored
	 * @return   argument after its transformation
	 */
	public abstract AbstractHalfEdge sym(AbstractHalfEdge that);

	/**
	 * Moves counterclockwise to following edge.
	 * @return  current instance after its transformation
	 */
	public abstract AbstractHalfEdge next();

	/**
	 * Moves counterclockwise to following edge.
	 * Make <code>that</code> instance be a copy of current
	 * instance, move it counterclockwise to next edge and
	 * return this instance.  Current instance is not modified.
	 *
	 * @param  that  instance where transformed edge is stored
	 * @return   argument after its transformation
	 */
	public abstract AbstractHalfEdge next(AbstractHalfEdge that);

	/**
	 * Moves counterclockwise to previous edge.
	 * @return  current instance after its transformation
	 */
	public abstract AbstractHalfEdge prev();

	/**
	 * Moves counterclockwise to previous edge.
	 * Make <code>that</code> instance be a copy of current
	 * instance, move it counterclockwise to previous edge and
	 * return this instance.  Current instance is not modified.
	 *
	 * @param  that  instance where transformed edge is stored
	 * @return   argument after its transformation
	 */
	public abstract AbstractHalfEdge prev(AbstractHalfEdge that);

	/**
	 * Moves counterclockwise to the following edge which has the same origin.
	 * @return  current instance after its transformation
	 */
	public abstract AbstractHalfEdge nextOrigin();

	/**
	 * Moves counterclockwise to the following edge which has the same origin.
	 * Make <code>that</code> instance be a copy of current
	 * instance, move it counterclockwise to the following edge which
	 * has the same origin and return this instance.  Current instance is
	 * not modified.
	 *
	 * @param  that  instance where transformed edge is stored
	 * @return   argument after its transformation
	 */
	public abstract AbstractHalfEdge nextOrigin(AbstractHalfEdge that);

	/**
	 * Moves counterclockwise to the following edge which has the same origin.
	 * If a boundary is reached, loop backward until another
	 * boundary is found and start again from there.
	 * @return  current instance after its transformation
	 */
	public abstract AbstractHalfEdge nextOriginLoop();

	/**
	 * Returns triangle tied to this edge.
	 *
	 * @return triangle tied to this edge
	 */
	public abstract Triangle getTri();

	/**
	 * Returns edge local number.
	 *
	 * @return edge local number
	 */
	public abstract int getLocalNumber();

	public abstract Object getAdj();
	public abstract void setAdj(Object link);

	/**
	 * Returns start vertex of this edge.
	 *
	 * @return start vertex of this edge
	 */
	public abstract Vertex origin();

	/**
	 * Returns end vertex of this edge.
	 *
	 * @return end vertex of this edge
	 */
	public abstract Vertex destination();
	
	/**
	 * Returns apex of this edge.
	 *
	 * @return apex of this edge
	 */
	public abstract Vertex apex();

	/**
	 * Sets attributes of this edge.
	 *
	 * @param attr  attributes of this edge
	 */
	public abstract void setAttributes(int attr);

	/**
	 * Resets attributes of this edge.
	 *
	 * @param attr   attributes of this edge to clear out
	 */
	public abstract void clearAttributes(int attr);

	/**
	 * Checks if some attributes of this edge are set.
	 *
	 * @param attr  attributes to check
	 * @return <code>true</code> if this AbstractHalfEdge has
	 *         one of these attributes set, <code>false</code>
	 *         otherwise
	 */
	public abstract boolean hasAttributes(int attr);

	/**
	 * Swaps an edge.
	 *
	 * @return swapped edge, origin and apical vertices are the same as in original edge
	 * @throws IllegalArgumentException if edge is on a boundary or belongs
	 * to an outer triangle.
	 * @see Mesh#edgeSwap
	 */
	protected abstract AbstractHalfEdge swap();

	/**
	 * Checks that triangles are not inverted if origin vertex is moved.
	 *
	 * @param newpt  the new position to be checked
	 * @return <code>false</code> if the new position produces
	 *    an inverted triangle, <code>true</code> otherwise.
	 */
	public abstract boolean checkNewRingNormals(double [] newpt);

	/**
	 * Checks whether an edge can be contracted into a given vertex.
	 *
	 * @param n  the resulting vertex
	 * @return <code>true</code> if this edge can be contracted into the single vertex n, <code>false</code> otherwise
	 * @see Mesh#canCollapseEdge
	 */
	protected abstract boolean canCollapse(AbstractVertex n);

	/**
	 * Contracts an edge.
	 *
	 * @param m  mesh
	 * @param n  the resulting vertex
	 * @return edge starting from <code>n</code> and with the same apex
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
	 * @return edge starting from <code>n</code> and pointing to original apex
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
	 * Returns the area of triangle bound to this edge.
	 *
	 * @return triangle area
	 */
	public abstract double area();

	/**
	 * Returns an iterator over triangle fans connected to this edge.  If edge is
	 * manifold, this iterator contains a single value, which is this edge.
	 * But if it is non-manifold and bound to <em>n</em> triangles, this iterator
	 * returns successively the <em>n</em> edges contained in these triangles and
	 * connected to the same endpoints.
	 *
	 * @return  iterator over triangle fans connected to this edge
	 */
	public abstract Iterator<AbstractHalfEdge> fanIterator();

}
