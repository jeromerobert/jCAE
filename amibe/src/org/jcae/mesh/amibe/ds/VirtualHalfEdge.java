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

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.NoSuchElementException;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.apache.log4j.Logger;

/*
 * This class is derived from Jonathan Richard Shewchuk's work
 * on AbstractTriangle, see
 *       http://www.cs.cmu.edu/~quake/triangle.html
 * His data structure is very compact, and similar ideas were
 * developed here, but due to Java constraints, this version is a
 * little bit less efficient than its C counterpart.
 *
 * Geometrical primitives and basic routines have been written from
 * scratch, but are in many cases very similar to those defined by
 * Shewchuk since data structures are almost equivalent and there
 * are few ways to achieve the same operations.
 *
 * Other ideas come from Bamg, written by Frederic Hecht
 *       http://www-rocq1.inria.fr/gamma/cdrom/www/bamg/eng.htm
 */

/**
 * A handle to abstract edge instances.
 *
 * <p>
 *   Jonathan Richard Shewchuk
 *   <a href="http://www.cs.cmu.edu/~quake/triangle.html">explains</a>
 *   why triangle-based data structures are more efficient than their
 *   edge-based counterparts.  But mesh operations make heavy use of edges,
 *   and informations about adges are not stored in this data structure in
 *   order to be compact.
 * </p>
 *
 * <p>
 *   A triangle is composed of three edges, so a triangle and a number
 *   between 0 and 2 can represent an edge.  This <code>VirtualHalfEdge</code>
 *   class plays this role, it defines an <em>oriented triangle</em>, or
 *   in other words an oriented edge.  Instances of this class are tied to
 *   their underlying {@link AbstractTriangle} instances, so modifications are not
 *   local to this class!
 * </p>
 *
 * <p>
 *   The main goal of this class is to ease mesh traversal.
 *   Consider the <code>ot</code> {@link VirtualHalfEdge} with a null localNumber of
 *   {@link AbstractTriangle} <code>t</code> below.
 * </p>
 * <pre>
 *                        V2
 *     V5 _________________,_________________ V3
 *        \    &lt;----      / \     &lt;----     /
 *         \     0     _ /   \      1    _ /
 *          \\  t0     ///  /\\\   t1    //
 *           \\1     2///1   0\\\2     0//   t.vertex = { V0, V1, V2 }
 *            \V     //V   t   \\V     //   t0.vertex = { V2, V1, V3 }
 *             \     /           \     /    t1.vertex = { V5, V0, V2 }
 *              \   /      2      \   /     t2.vertex = { V0, V4, V1 }
 *               \ /     ----&gt;     \ /
 *             V0 +-----------------+ V1
 *                 \     &lt;----     /
 *                  \      1    _ /
 *                   \\   t2    //
 *                    \\2     0//
 * </pre>
 * The following methods can be applied to <code>ot</code>:
 * <pre>
 *    ot.next();        // Moves (t,0) to (t,1)
 *    ot.prev();        // Moves (t,0) to (t,2)
 *    ot.sym();         // Moves (t,0) to (t1,2)
 *    ot.nextOrigin();  // Moves (t,0) to (t2,1)
 * </pre>
 * For convenience, following methods are also defined in VirtualHalfEdge2D:
 * <pre>
 *    ot.prevOrigin();  // Moves (t,0) to (t1,0)
 *    ot.nextDest();    // Moves (t,0) to (t1,1)
 *    ot.prevDest();    // Moves (t,0) to (t0,2)
 *    ot.nextApex();    // Moves (t,0) to (t0,0)
 *    ot.prevApex();    // Moves (t,0) to (t2,0)
 * </pre>
 *
 * <p>
 * When an <code>VirtualHalfEdge</code> is traversing the mesh, its reference
 * is not modified, but its instance variables are updated.  In order
 * to prevent object allocations, we try to reuse <code>VirtualHalfEdge</code>
 * objects as much as we can.
 * </p>
 */
public class VirtualHalfEdge extends AbstractHalfEdge
{
	private static Logger logger = Logger.getLogger(VirtualHalfEdge.class);
	
	private static final int [] next3 = { 1, 2, 0 };
	private static final int [] prev3 = { 2, 0, 1 };
	
	private final double [] tempD = new double[3];
	private final double [] tempD1 = new double[3];
	private final double [] tempD2 = new double[3];
	
	//  Complex algorithms require several VirtualHalfEdge, they are
	//  allocated here to prevent allocation/deallocation overhead.
	private static VirtualHalfEdge [] work = new VirtualHalfEdge[4];
	static {
		for (int i = 0; i < 4; i++)
			work[i] = new VirtualHalfEdge();
	}
	
	/*
	 * Vertices can be accessed through
	 *        origin = tri.vertex[next3[localNumber]]
	 *   destination = tri.vertex[prev3[localNumber]]
	 *          apex = tri.vertex[localNumber]
	 * Adjacent triangle is tri.adj[localNumber].tri and its localNumber
	 * is ((tri.adjPos[0] >> (2*localNumber)) & 3)
	 */
	protected Triangle tri = null;
	protected int localNumber = 0;
	protected int attributes = 0;
	
	// Section: constructors
	
	/**
	 * Sole constructor.
	 */
	public VirtualHalfEdge()
	{
	}
	
	/**
	 * Creates an object to handle data about a triangle.
	 *
	 * @param t  geometrical triangle
	 * @param o  a number between 0 and 2 determining an edge
	 */
	public VirtualHalfEdge(Triangle t, int o)
	{
		tri = t;
		localNumber = o;
		pullAttributes();
	}
	
	// Section: accessors
	
	/**
	 * Returns triangle tied to this edge.
	 *
	 * @return triangle tied to this edge
	 */
	@Override
	public final Triangle getTri()
	{
		return tri;
	}
	
	/**
	 * Returns edge local number.
	 *
	 * @return edge local number
	 */
	@Override
	public final int getLocalNumber()
	{
		return localNumber;
	}
	
	/**
	 * Sets triangle tied to this object, and resets localNumber.
	 *
	 * @param t  triangle tied to this object
	 */
	public final void bind(Triangle t)
	{
		tri = t;
		localNumber = 0;
		pullAttributes();
	}
	
	/**
	 * Sets the triangle tied to this object, and the localNumber.
	 *
	 * @param t  triangle tied to this object
	 * @param l  local number
	 */
	public final void bind(Triangle t, int l)
	{
		tri = t;
		localNumber = l;
		pullAttributes();
	}
	
	// Section: attributes handling
	
	/**
	 * Checks if some attributes of this oriented triangle are set.
	 *
	 * @param attr  attributes to check
	 * @return <code>true</code> if this VirtualHalfEdge has one of
	 * these attributes set, <code>false</code> otherwise
	 */
	@Override
	public final boolean hasAttributes(int attr)
	{
		return (attributes & attr) != 0;
	}
	
	/**
	 * Sets attributes of this edge.
	 *
	 * @param attr  attributes of this edge
	 */
	@Override
	public final void setAttributes(int attr)
	{
		attributes |= attr;
		pushAttributes();
	}
	
	/**
	 * Resets attributes of this oriented triangle.
	 *
	 * @param attr   attributes of this oriented triangle to clear out
	 */
	@Override
	public final void clearAttributes(int attr)
	{
		attributes &= ~attr;
		pushAttributes();
	}
	
	// Adjust tri.adjPos after attributes is modified.
	protected final void pushAttributes()
	{
		tri.setEdgeAttributes(localNumber, attributes);
	}
	
	// Adjust attributes after tri.adjPos is modified.
	protected final void pullAttributes()
	{
		attributes = tri.getEdgeAttributes(localNumber);
	}
	
	/**
	 * Checks whether an edge can be modified.
	 *
	 * @return <code>false</code> if edge is a boundary or outside the mesh,
	 * <code>true</code> otherwise.
	 */
	public final boolean isMutable()
	{
		return !(hasAttributes(BOUNDARY) || hasAttributes(NONMANIFOLD) || hasAttributes(OUTER));
	}
	
	// Section: geometrical primitives
	
	//  These geometrical primitives have 3 signatures:
	//      fct()     transforms current object.
	//      fct(that) copies current instance into 'that' and transforms it
	//      fct(this, that)   applies fct to 'this' and stores result
	//                        in an already allocated object 'that'.
	//  This is definitely not an OO approach, but it is much more
	//  efficient by preventing useless memory allocations.
	//  They do not return any value to make clear that calling
	//  these routines requires extra care.
	
	/**
	 * Moves to symmetric edge.
	 * @return  current instance after its transformation
	 */
	@Override
	public final AbstractHalfEdge sym()
	{
		int neworient = tri.getAdjLocalNumber(localNumber);
		tri = (Triangle) tri.getAdj(localNumber);
		localNumber = neworient;
		pullAttributes();
		return this;
	}
	
	/**
	 * Moves to symmetric edge.
	 * Make <code>that</code> instance be a copy of current
	 * instance, move it to its symmetric edge and return
	 * this instance.  Current instance is not modified.
	 *
	 * @param  that  instance where transformed edge is stored
	 * @return   argument after its transformation
	 */
	@Override
	public final AbstractHalfEdge sym(AbstractHalfEdge that)
	{
		VirtualHalfEdge dest = (VirtualHalfEdge) that;
		dest.tri = (Triangle) tri.getAdj(localNumber);
		dest.localNumber = tri.getAdjLocalNumber(localNumber);
		dest.pullAttributes();
		return dest;
	}
	
	/**
	 * Moves counterclockwise to following edge.
	 * @return  current instance after its transformation
	 */
	@Override
	public final AbstractHalfEdge next()
	{
		localNumber = next3[localNumber];
		pullAttributes();
		return this;
	}
	
	/**
	 * Moves counterclockwise to following edge.
	 * Make <code>that</code> instance be a copy of current
	 * instance, move it counterclockwise to next edge and
	 * return this instance.  Current instance is not modified.
	 *
	 * @param  that  instance where transformed edge is stored
	 * @return   argument after its transformation
	 */
	@Override
	public final AbstractHalfEdge next(AbstractHalfEdge that)
	{
		VirtualHalfEdge dest = (VirtualHalfEdge) that;
		dest.tri = tri;
		dest.localNumber = next3[localNumber];
		dest.pullAttributes();
		return dest;
	}
	
	/**
	 * Moves counterclockwise to previous edge.
	 * @return  current instance after its transformation
	 */
	@Override
	public final AbstractHalfEdge prev()
	{
		localNumber = prev3[localNumber];
		pullAttributes();
		return this;
	}
	
	/**
	 * Moves counterclockwise to previous edge.
	 * Make <code>that</code> instance be a copy of current
	 * instance, move it counterclockwise to previous edge and
	 * return this instance.  Current instance is not modified.
	 *
	 * @param  that  instance where transformed edge is stored
	 * @return   argument after its transformation
	 */
	@Override
	public final AbstractHalfEdge prev(AbstractHalfEdge that)
	{
		VirtualHalfEdge dest = (VirtualHalfEdge) that;
		dest.tri = tri;
		dest.localNumber = prev3[localNumber];
		dest.pullAttributes();
		return dest;
	}
	
	/**
	 * Moves counterclockwise to the following edge which has the same origin.
	 * @return  current instance after its transformation
	 */
	@Override
	public final AbstractHalfEdge nextOrigin()
	{
		return prev().sym();
	}
	
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
	@Override
	public final AbstractHalfEdge nextOrigin(AbstractHalfEdge that)
	{
		return prev(that).sym();
	}
	
	/**
	 * Moves counterclockwise to the following edge which has the same origin.
	 * If a boundary is reached, loop backward until another
	 * boundary is found and start again from there.
	 * Note: outer triangles are taken into account in this loop, because
	 * this is sometimes needed, as in VirtualHalfEdge2D.removeDegenerated().
	 * They have to be explicitly filtered out by testing hasAttributes(OUTER).
	 */
	@Override
	public final AbstractHalfEdge nextOriginLoop()
	{
		if (hasAttributes(OUTER) && hasAttributes(BOUNDARY | NONMANIFOLD))
		{
			// Loop clockwise to another boundary
			// and start again from there.
			do
			{
				sym();
				next();
			}
			while (!hasAttributes(OUTER));
		}
		else
			nextOrigin();
		return this;
	}
	
	// Static methods for VirtualHalfEdge instances, only the most useful methods are defined
	
	/**
	 * Copies a <code>VirtualHalfEdge</code> instance into another <code>VirtualHalfEdge</code>
	 * instance.
	 *
	 * @param src   <code>VirtualHalfEdge</code> being duplicated
	 * @param dest  already allocated <code>VirtualHalfEdge</code> where data are
	 *              copied
	 */
	protected static final void copyOTri(VirtualHalfEdge src, VirtualHalfEdge dest)
	{
		dest.tri = src.tri;
		dest.localNumber = src.localNumber;
		dest.attributes = src.attributes;
	}
	
	/**
	 * Copies a <code>VirtualHalfEdge</code> instance and move to its symmetric edge.
	 *
	 * @param o     source <code>VirtualHalfEdge</code>
	 * @param that  already allocated <code>VirtualHalfEdge</code> where data are
	 *              copied
	 */
	protected static final void symOTri(VirtualHalfEdge o, VirtualHalfEdge that)
	{
		that.tri = (Triangle) o.tri.getAdj(o.localNumber);
		that.localNumber = o.tri.getAdjLocalNumber(o.localNumber);
		that.pullAttributes();
	}
	
	/**
	 * Copies a <code>VirtualHalfEdge</code> instance and move it counterclockwise to
	 * following edge.
	 *
	 * @param o     source <code>VirtualHalfEdge</code>
	 * @param that  already allocated <code>VirtualHalfEdge</code> where data are
	 *              copied
	 */
	protected static final void nextOTri(VirtualHalfEdge o, VirtualHalfEdge that)
	{
		that.tri = o.tri;
		that.localNumber = next3[o.localNumber];
		that.pullAttributes();
	}
	
	/**
	 * Copies a <code>VirtualHalfEdge</code> instance and move it counterclockwise to
	 * previous edge.
	 *
	 * @param o     source <code>VirtualHalfEdge</code>
	 * @param that  already allocated <code>VirtualHalfEdge</code> where data are
	 *              copied
	 */
	protected static final void prevOTri(VirtualHalfEdge o, VirtualHalfEdge that)
	{
		that.tri = o.tri;
		that.localNumber = prev3[o.localNumber];
		that.pullAttributes();
	}
	
	// Section: vertex handling
	
	/**
	 * Returns start vertex of this edge.
	 *
	 * @return start vertex of this edge
	 */
	@Override
	public Vertex origin()
	{
		return (Vertex) tri.vertex[next3[localNumber]];
	}
	
	/**
	 * Returns end vertex of this edge.
	 *
	 * @return end vertex of this edge
	 */
	@Override
	public Vertex destination()
	{
		return (Vertex) tri.vertex[prev3[localNumber]];
	}
	
	/**
	 * Returns apex of this edge.
	 *
	 * @return apex of this edge
	 */
	@Override
	public Vertex apex()
	{
		return (Vertex) tri.vertex[localNumber];
	}
	
	//  The following 3 methods change the underlying triangle.
	//  So they also modify all VirtualHalfEdge bound to this one.
	/**
	 * Sets start vertex of this edge.
	 *
	 * @param v  start vertex of this edge
	 */
	public final void setOrigin(Vertex v)
	{
		tri.vertex[next3[localNumber]] = v;
	}
	
	/**
	 * Sets end vertex of this edge.
	 *
	 * @param v  end vertex of this edge
	 */
	public final void setDestination(Vertex v)
	{
		tri.vertex[prev3[localNumber]] = v;
	}
	
	/**
	 * Sets apex of this edge.
	 *
	 * @param v  apex of this edge
	 */
	public final void setApex(Vertex v)
	{
		tri.vertex[localNumber] = v;
	}
	
	// Section: adjacency
	
	/**
	 * Sets adjacency relations between two triangles.
	 *
	 * @param sym  the edge tied to this object
	 */
	@Override
	public final void glue(AbstractHalfEdge sym)
	{
		VHglue((VirtualHalfEdge) sym);
	}
	private void VHglue(VirtualHalfEdge sym)
	{
		tri.setAdj(localNumber, sym.tri);
		tri.setAdjLocalNumber(localNumber, sym.localNumber);
		if (sym.tri != null)
		{
			sym.tri.setAdj(sym.localNumber, tri);
			sym.tri.setAdjLocalNumber(sym.localNumber, localNumber);
		}
	}
	
	/**
	 * Gets adjacency relation for an edge
	 *
	 * @return the triangle bond to this one if this edge is manifold, or an Object otherwise
	 */
	@Override
	public final Object getAdj()
	{
		return tri.getAdj(localNumber);
	}
	
	/**
	 * Sets adjacency relation for an edge
	 *
	 * @param link  the triangle bond to this one if this edge is manifold, or an Object otherwise
	 */
	@Override
	public final void setAdj(Object link)
	{
		tri.setAdj(localNumber, link);
	}
	
	// Section: 3D geometrical routines
	
	/**
	 * Computes the normal of an edge, in the triangle plane.
	 * This vector is not normalized, it has the same length as
	 * this edge.  The result is stored in the tempD temporary array.
	 * @see #getTempVector
	 * @return  triangle area
	 * Warning: this method uses tempD, tempD1 and tempD2 temporary arrays.
	 */
	public double computeNormal3DT()
	{
		double [] p0 = origin().getUV();
		double [] p1 = destination().getUV();
		double [] p2 = apex().getUV();
		tempD1[0] = p1[0] - p0[0];
		tempD1[1] = p1[1] - p0[1];
		tempD1[2] = p1[2] - p0[2];
		tempD[0] = p2[0] - p0[0];
		tempD[1] = p2[1] - p0[1];
		tempD[2] = p2[2] - p0[2];
		Matrix3D.prodVect3D(tempD1, tempD, tempD2);
		double norm = Matrix3D.norm(tempD2);
		if (norm != 0.0)
		{
			tempD2[0] /= norm;
			tempD2[1] /= norm;
			tempD2[2] /= norm;
		}
		Matrix3D.prodVect3D(tempD1, tempD2, tempD);
		return 0.5*norm;
	}
	
	/**
	 * Computes the normal of this triangle.  The result is stored in
	 * the tempD temporary array.
	 * @see #getTempVector
	 * @return  triangle area
	 * Warning: this method uses tempD, tempD1 and tempD2 temporary arrays.
	 */
	public double computeNormal3D()
	{
		double [] p0 = origin().getUV();
		double [] p1 = destination().getUV();
		double [] p2 = apex().getUV();
		tempD1[0] = p1[0] - p0[0];
		tempD1[1] = p1[1] - p0[1];
		tempD1[2] = p1[2] - p0[2];
		tempD2[0] = p2[0] - p0[0];
		tempD2[1] = p2[1] - p0[1];
		tempD2[2] = p2[2] - p0[2];
		Matrix3D.prodVect3D(tempD1, tempD2, tempD);
		double norm = Matrix3D.norm(tempD);
		if (norm != 0.0)
		{
			tempD[0] /= norm;
			tempD[1] /= norm;
			tempD[2] /= norm;
		}
		return 0.5*norm;
	}
	
	/**
	 * Returns the area of triangle bound to this edge.
	 *
	 * @return  triangle area
	 * Warning: this method uses tempD, tempD1 and tempD2 temporary arrays.
	 */
	@Override
	public double area()
	{
		double [] p0 = origin().getUV();
		double [] p1 = destination().getUV();
		double [] p2 = apex().getUV();
		tempD1[0] = p1[0] - p0[0];
		tempD1[1] = p1[1] - p0[1];
		tempD1[2] = p1[2] - p0[2];
		tempD2[0] = p2[0] - p0[0];
		tempD2[1] = p2[1] - p0[1];
		tempD2[2] = p2[2] - p0[2];
		Matrix3D.prodVect3D(tempD1, tempD2, tempD);
		return 0.5 * Matrix3D.norm(tempD);
	}
	
	/**
	 * Returns the temporary array TempD.
	 */
	public double [] getTempVector()
	{
		return tempD;
	}
	
	// Section: algorithms
	
	/**
	 * Checks the dihedral angle of an edge.
	 *
	 * @param minCos  if the dot product of the normals to adjacent
	 *    triangles is lower than monCos, then <code>-1.0</code> is
	 *    returned.
	 * @return the minimum quality of the two trianglles generated
	 *    by swapping this edge.
	 */
	public final double checkSwap3D(double minCos)
	{
		double invalid = -1.0;
		// Check if there is an adjacent edge
		if (hasAttributes(OUTER | BOUNDARY | NONMANIFOLD))
			return invalid;
		// Check for coplanarity
		symOTri(this, work[0]);
		computeNormal3D();
		double [] n1 = getTempVector();
		work[0].computeNormal3D();
		double [] n2 = work[0].getTempVector();
		if (Matrix3D.prodSca(n1, n2) < minCos)
			return invalid;
		// Check for quality improvement
		Vertex o = origin();
		Vertex d = destination();
		Vertex a = apex();
		Vertex n = work[0].apex();
		// Check for inverted triangles
		o.outer3D(n, a, n2);
		double s3 = 0.5 * Matrix3D.prodSca(n1, n2);
		if (s3 <= 0.0)
			return invalid;
		d.outer3D(a, n, n2);
		double s4 = 0.5 * Matrix3D.prodSca(n1, n2);
		if (s4 <= 0.0)
			return invalid;
		double p1 = o.distance3D(d) + d.distance3D(a) + a.distance3D(o);
		double s1 = area();
		double p2 = d.distance3D(o) + o.distance3D(n) + n.distance3D(d);
		double s2 = work[0].area();
		// No need to multiply by 12.0 * Math.sqrt(3.0)
		double Qbefore = Math.min(s1/p1/p1, s2/p2/p2);
		
		double p3 = o.distance3D(n) + n.distance3D(a) + a.distance3D(o);
		double p4 = d.distance3D(a) + a.distance3D(n) + n.distance3D(d);
		double Qafter = Math.min(s3/p3/p3, s4/p4/p4);
		if (Qafter > Qbefore)
			return Qafter;
		return invalid;
	}
	
	/**
	 * Swaps an edge.
	 *
	 * @return swapped edge, origin and apical vertices are the same as in original edge
	 * @throws IllegalArgumentException if edge is on a boundary or belongs
	 * to an outer triangle.
	 * @see Mesh#edgeSwap
	 */
	@Override
	protected final AbstractHalfEdge swap()
	{
		VHswap();
		return this;
	}
	private final void VHswap()
	{
		if (hasAttributes(OUTER | BOUNDARY | NONMANIFOLD))
			throw new IllegalArgumentException("Cannot swap "+this);
		Vertex o = origin();
		Vertex d = destination();
		Vertex a = apex();
		/*
		 *            d                    d
		 *            .                    .
		 *           /|\                  / \
		 *       a1 / | \ a4         a1  /   \ a4
		 *         /  |  \              /     \
		 *      a +   |   + n  --->  a +-------+ n
		 *         \  |  /              \     /
		 *       a2 \ | / a3         a2  \   / a3
		 *           \|/                  \ /
		 *            '                    '
		 *            o                    o
		 */
		// T1 = (oda)  --> (ona)
		// T2 = (don)  --> (dan)
		copyOTri(this, work[0]);        // (oda)
		symOTri(this, work[1]);         // (don)
		symOTri(this, work[2]);         // (don)
		Vertex n = work[1].apex();
		//  Clear SWAPPED flag for all edges of the 2 triangles
		for (int i = 0; i < 3; i++)
		{
			work[0].clearAttributes(SWAPPED);
			work[1].clearAttributes(SWAPPED);
			work[0].next();
			work[1].next();
		}
		work[1].next();                 // (ond)
		int attr3 = work[1].attributes;
		work[1].sym();                  // a3 = (no*)
		work[1].VHglue(work[0]);
		work[0].attributes = attr3;
		work[0].pushAttributes();
		work[0].next();                 // (dao)
		copyOTri(work[0], work[1]);     // (dao)
		int attr1 = work[1].attributes;
		work[0].sym();                  // a1 = (ad*)
		work[2].VHglue(work[0]);
		work[2].attributes = attr1;
		work[2].pushAttributes();
		work[2].next();                 // (ond)
		work[2].VHglue(work[1]);
		//  Mark new edge
		work[1].attributes = 0;
		work[2].attributes = 0;
		work[1].setAttributes(SWAPPED);
		work[2].setAttributes(SWAPPED);
		//  Adjust vertices
		work[2].setOrigin(a);           // (and)
		work[1].setOrigin(n);           // (nao)
		//  Fix links to triangles
		replaceVertexLinks(o, tri, work[2].tri, tri);
		replaceVertexLinks(d, tri, work[2].tri, work[2].tri);
		pullAttributes();
	}
	
	/**
	 * Checks that triangles are not inverted if origin vertex is moved.
	 *
	 * @param newpt  the new position to be checked
	 * @return <code>false</code> if the new position produces
	 *    an inverted triangle, <code>true</code> otherwise.
	 * Warning: this method uses work[0] and work[1] temporary arrays.
	 */
	@Override
	public final boolean checkNewRingNormals(double [] newpt)
	{
		Vertex o = origin();
		if (o.getLink() instanceof Triangle)
			return checkNewRingNormalsSameFan(newpt, null, null);
		for (Triangle start: (Triangle []) o.getLink())
		{
			work[1].bind(start);
			if (work[1].destination() == o)
				work[1].next();
			else if (work[1].apex() == o)
				work[1].prev();
			assert work[1].origin() == o;
			if (!work[1].checkNewRingNormalsSameFan(newpt, null, null))
				return false;
		}
		return true;
	}
	
	/*
	 * Warning: this method uses work[0] temporary array.
	 */
	private final boolean checkNewRingNormalsSameFan(double [] newpt, Triangle t1, Triangle t2)
	{
		Vertex d = destination();
		copyOTri(this, work[0]);
		do
		{
			if (work[0].tri != t1 && work[0].tri != t2 && !work[0].hasAttributes(OUTER))
			{
				double [] x1 = work[0].destination().getUV();
				work[0].next();
				double area  = work[0].computeNormal3DT();
				double [] nu = work[0].getTempVector();
				work[0].prev();
				for (int i = 0; i < 3; i++)
					tempD1[i] = newpt[i] - x1[i];
				// Two triangles are removed when an edge is contracted.
				// So normally triangle areas should increase.  If they
				// decrease significantly, there may be a problem.
				if (Matrix3D.prodSca(tempD1, nu) >= - area)
					return false;
			}
			work[0].nextOriginLoop();
		}
		while (work[0].destination() != d);
		return true;
	}
	
	/**
	 * Checks whether an edge can be contracted.
	 *
	 * @param n the resulting vertex
	 * @return <code>true</code> if this edge can be contracted into the single vertex n, <code>false</code> otherwise
	 * @see Mesh#canCollapseEdge
	 * Warning: this method uses work[0], work[1] and work[2] temporary arrays.
	 */
	@Override
	protected final boolean canCollapse(AbstractVertex n)
	{
		// Be consistent with collapse()
		if (hasAttributes(OUTER))
			return false;
		if (logger.isDebugEnabled())
			logger.debug("can contract? ("+origin()+" "+destination()+") into "+n);
		double [] xn = n.getUV();
		if ((origin().getLink() instanceof Triangle) && (destination().getLink() instanceof Triangle))
		{
			// Mesh is locally manifold.  This is the most common
			// case, do not create an HashSet to store only two
			// triangles.
			Triangle t1 = tri;
			symOTri(this, work[1]);
			Triangle t2 = work[1].tri;
			// Check that origin vertex can be moved
			if (!checkNewRingNormalsSameFan(xn, t1, t2))
				return false;
			// Check that destination vertex can be moved
			if (!work[1].checkNewRingNormalsSameFan(xn, t1, t2))
				return false;
			//  Topology check.
			return canCollapseTopology();
		}

		// At least one vertex is non manifold.  Store all triangles
		// which will be removed in an HashSet so that they are
		// ignored when checking for degenerated triangles.
		Collection<Triangle> ignored = new HashSet<Triangle>();
		for (Iterator<AbstractHalfEdge> it = fanIterator(); it.hasNext(); )
		{
			VirtualHalfEdge f = (VirtualHalfEdge) it.next();
			ignored.add(f.tri);
			symOTri(f, work[1]);
			ignored.add(work[1].tri);
		}
		
		// Check that origin vertex can be moved
		if (!checkNewRingNormalsNonManifoldVertex(xn, ignored))
			return false;
		// Check that destination vertex can be moved
		symOTri(this, work[2]);
		if (!work[2].checkNewRingNormalsNonManifoldVertex(xn, ignored))
			return false;
		ignored.clear();

		//  Topology check.
		//  See in AbstractHalfEdgeTest.buildMeshTopo() why this
		//  check is needed.
		//  When edge is non manifold, we do not use Vertex.getNeighboursNodes()
		//  because checks have to be performed by fans.
		for (Iterator<AbstractHalfEdge> it = fanIterator(); it.hasNext(); )
		{
			VirtualHalfEdge f = (VirtualHalfEdge) it.next();
			if (!f.canCollapseTopology())
				return false;
		}
		return true;
	}
	/*
	 * Warning: this method uses work[0] and work[1] temporary arrays.
	 */
	private final boolean checkNewRingNormalsNonManifoldVertex(double [] newpt, Collection<Triangle> ignored)
	{
		Vertex o = origin();
		if (o.getLink() instanceof Triangle)
			return checkNewRingNormalsSameFanNonManifoldVertex(newpt, ignored);
		for (Triangle start: (Triangle []) o.getLink())
		{
			work[1].bind(start);
			if (work[1].destination() == o)
				work[1].next();
			else if (work[1].apex() == o)
				work[1].prev();
			assert work[1].origin() == o;
			if (!work[1].checkNewRingNormalsSameFanNonManifoldVertex(newpt, ignored))
				return false;
		}
		return true;
	}
	/*
	 * Warning: this method uses work[0] temporary array.
	 */
	private final boolean checkNewRingNormalsSameFanNonManifoldVertex(double [] newpt, Collection<Triangle> ignored)
	{
		// Loop around origin.  We need to copy current instance
		// into work[0] because loop may be interrupted.
		copyOTri(this, work[0]);
		Vertex d = destination();
		do
		{
			if (!ignored.contains(work[0].tri) && !work[0].hasAttributes(OUTER))
			{
				double [] x1 = work[0].destination().getUV();
				work[0].next();
				double area  = work[0].computeNormal3DT();
				double [] nu = work[0].getTempVector();
				work[0].prev();
				for (int i = 0; i < 3; i++)
					tempD1[i] = newpt[i] - x1[i];
				// Two triangles are removed when an edge is contracted.
				// So normally triangle areas should increase.  If they
				// decrease significantly, there may be a problem.
				if (Matrix3D.prodSca(tempD1, nu) >= - area)
					return false;
			}
			work[0].nextOriginLoop();
		}
		while (work[0].destination() != d);
		return true;
	}
	
	/**
	 * Topology check.
	 * See in AbstractHalfEdgeTest.buildMeshTopo() why this
	 * check is needed.
	 * Warning: this method uses work[0] temporary array.
	 */
	private final boolean canCollapseTopology()
	{
		Collection<Vertex> neighbours = new HashSet<Vertex>();
		// We need to copy current instance into work[0]
		// because second loop may be interrupted.
		copyOTri(this, work[0]);
		Vertex d = work[0].destination();
		do
		{
			// Warning: mesh.outerVertex is intentionnally not filtered out
			neighbours.add(work[0].destination());
			work[0].nextOriginLoop();
		}
		while (work[0].destination() != d);
		work[0].sym();
		int cnt = 0;
		d = work[0].destination();
		do
		{
			// Warning: mesh.outerVertex is intentionnally not filtered out
			if (neighbours.contains(work[0].destination()))
			{
				if (cnt > 1)
					return false;
				cnt++;
			}
			work[0].nextOriginLoop();
		}
		while (work[0].destination() != d);
		return true;
	}
	
	/**
	 * Contracts an edge.
	 *
	 * @param m mesh
	 * @param n the resulting vertex
	 * @return edge starting from <code>n</code> and with the same apex
	 * @throws IllegalArgumentException if edge belongs to an outer triangle,
	 * because there would be no valid return value.  User must then run this
	 * method against symmetric edge, this is not done automatically.
	 * @see Mesh#edgeCollapse
	 */
	@Override
	protected final AbstractHalfEdge collapse(AbstractMesh m, AbstractVertex n)
	{
		if (hasAttributes(OUTER))
			throw new IllegalArgumentException("Cannot contract "+this);
		Vertex o = origin();
		Vertex d = destination();
		Vertex v = (Vertex) n;
		Mesh mesh = (Mesh) m;
		assert o.isWritable() && d.isWritable(): "Cannot contract "+this;
		if (logger.isDebugEnabled())
			logger.debug("contract ("+o+" "+d+")");
		//  Replace o by n in all incident triangles
		if (o.getLink() instanceof Triangle)
			replaceEndpointsSameFan(v);
		else
			replaceEndpointsNonManifold(o, v);
		//  Replace d by n in all incident triangles
		symOTri(this, work[2]);
		if (d.getLink() instanceof Triangle)
			work[2].replaceEndpointsSameFan(v);
		else
			replaceEndpointsNonManifold(d, v);
		//  Set v links
		deepCopyVertexLinks(o, d, v);
		if (logger.isDebugEnabled())
			logger.debug("new point: "+v);
		if (mesh.hasNodes())
		{
			mesh.remove(o);
			mesh.remove(d);
			mesh.add(v);
		}
		if (!hasAttributes(NONMANIFOLD))
		{
			work[2].VHcollapseSameFan(mesh, v);
			return VHcollapseSameFan(mesh, v);
		}
		// Edge is non-manifold
		assert work[2].hasAttributes(OUTER);
		// VHcollapseSameFan may modify internal data structure
		// used by fanIterator(), we need a copy.
		Map<Triangle, Integer> copy = new LinkedHashMap<Triangle, Integer>();
		for (Iterator<AbstractHalfEdge> it = fanIterator(); it.hasNext(); )
		{
			VirtualHalfEdge h = (VirtualHalfEdge) it.next();
			copy.put(h.tri, int3[h.localNumber]);
		}
		Triangle ret = null;
		int num = -1;
		for (Map.Entry<Triangle, Integer> entry: copy.entrySet())
		{
			Triangle t = entry.getKey();
			int l = entry.getValue().intValue();
			work[2].bind(t, l);
			assert !work[2].hasAttributes(OUTER);
			work[2].sym();
			assert work[2].hasAttributes(OUTER);
			work[2].VHcollapseSameFan(mesh, v);
			work[2].bind(t, l);
			work[2].VHcollapseSameFan(mesh, v);
			if (t == tri)
			{
				ret = work[0].tri;
				num = work[0].localNumber;
			}
		}
		assert ret != null;
		bind(ret, num);
		return this;
	}

	/*
	 * Warning: this method uses work[0] and work[1] temporary arrays.
	 */
	private VirtualHalfEdge VHcollapseSameFan(Mesh m, Vertex n)
	{
		/*
		 *           V1                       V1
		 *  V3+-------+-------+ V4   V3 +------+------+ V4
		 *     \ t3  / \ t4  /           \  t3 | t4  / 
		 *      \   /   \   /   ------>    \   |   /
		 *       \ / t1  \ /                 \ | /  
		 *      o +-------+ d                n +
		 */
		// this = (odV1)
		if (hasAttributes(NONMANIFOLD) && hasAttributes(OUTER))
		{
			// All we have to do here is to remove t1
			m.remove(tri);
			return null;
		}
		//  Update adjacency links.  For clarity, o and d are
		//  written instead of n.
		next();                         // (dV1o)
		int attr4 = attributes;
		VirtualHalfEdge vh4 = (getAdj() == null ? null : work[0]);
		if (vh4 != null)
			symOTri(this, vh4);     // (V1dV4)
		next();                         // (V1od)
		int attr3 = attributes;
		VirtualHalfEdge vh3 = (getAdj() == null ? null : work[1]);
		if (vh3 != null)
			symOTri(this, vh3);     // (oV1V3)
		if (!hasAttributes(OUTER))
		{
			Triangle t34 = work[1].tri;
			if (t34.isOuter())
				t34 = work[0].tri;
			assert !t34.isOuter() : work[0]+"\n"+work[1];
			// Update links of V1 and n
			replaceVertexLinks(origin(), tri, t34);
			replaceVertexLinks(n, tri, t34);
		}
		if (vh3 != null && vh3.hasAttributes(NONMANIFOLD))
			vh3.VHglue(vh4);
		else if (vh4 != null && vh4.hasAttributes(NONMANIFOLD))
			vh4.VHglue(vh3);
		else if (vh3 != null)
			vh3.VHglue(vh4);
		else if (vh4 != null)
			vh4.VHglue(vh3);
		if (vh3 != null)
		{
			vh3.attributes |= attr4;
			vh3.pushAttributes();
		}
		if (vh4 != null)
		{
			vh4.attributes |= attr3;
			vh4.pushAttributes();
		}
		next();                         // (odV1)
		// Remove t1
		m.remove(tri);
		// By convention, edge is moved into (dV4V1), but this may change.
		// If vh4 is null, edge is outer and return value does not matter
		return (vh4 == null ? null : (VirtualHalfEdge) vh4.next());
	}
	
	private void replaceEndpointsSameFan(Vertex n)
	{
		Vertex d = destination();
		do
		{
			setOrigin(n);
			nextOriginLoop();
		}
		while (destination() != d);
	}
	/*
	 * Warning: this method uses work[0] temporary array.
	 */
	private static final void replaceEndpointsNonManifold(Vertex o, Vertex n)
	{
		Triangle [] oList = (Triangle []) o.getLink();
		for (Triangle t: oList)
		{
			work[0].bind(t);
			if (work[0].destination() == o)
				work[0].next();
			else if (work[0].apex() == o)
				work[0].prev();
			assert work[0].origin() == o : ""+o+" not in "+work[0];
			work[0].replaceEndpointsSameFan(n);
		}
	}
	private static void replaceVertexLinks(Vertex o, Triangle oldT1, Triangle oldT2, Triangle newT)
	{
		if (o.getLink() instanceof Triangle)
			o.setLink(newT);
		else
		{
			Triangle [] tArray = (Triangle []) o.getLink();
			for (int i = 0; i < tArray.length; i++)
			{
				if (tArray[i] == oldT1 || tArray[i] == oldT2)
				{
					logger.debug("replaceVertexLinks: "+tArray[i]+" --> "+newT);
					tArray[i] = newT;
				}
			}
		}
	}
	private static void replaceVertexLinks(Vertex o, Triangle oldT, Triangle newT)
	{
		if (o.getLink() instanceof Triangle)
			o.setLink(newT);
		else
		{
			Triangle [] tArray = (Triangle []) o.getLink();
			for (int i = 0; i < tArray.length; i++)
			{
				if (tArray[i] == oldT)
				{
					logger.debug("replaceVertexLinks: "+i+" "+o+" "+tArray[i]);
					tArray[i] = newT;
					logger.debug(" --> "+newT);
				}
			}
		}
	}
	/*
	 * Warning: this method uses work[0] and work[1] temporary arrays.
	 */
	private static void deepCopyVertexLinks(Vertex o, Vertex d, Vertex v)
	{
		boolean ot = o.getLink() instanceof Triangle;
		boolean dt = d.getLink() instanceof Triangle;
		//  Prepare vertex links first
		if (ot && dt)
		{
			v.setLink(d.getLink());
		}
		else if (ot)
		{
			Triangle [] dList = (Triangle []) d.getLink();
			Triangle [] nList = new Triangle[dList.length];
			System.arraycopy(dList, 0, nList, 0, dList.length);
			v.setLink(nList);
		}
		else if (dt)
		{
			Triangle [] oList = (Triangle []) o.getLink();
			Triangle [] nList = new Triangle [oList.length];
			System.arraycopy(oList, 0, nList, 0, oList.length);
			v.setLink(nList);
		}
		else
		{
			// Vertex.setLinkFan() cannot be called here because fans from
			// o and d have to be merged.
			Triangle [] oList = (Triangle []) o.getLink();
			Triangle [] dList = (Triangle []) d.getLink();
			Triangle [] nList = new Triangle[oList.length+dList.length];
			System.arraycopy(oList, 0, nList, 0, oList.length);
			System.arraycopy(dList, 0, nList, oList.length, dList.length);
			ArrayList<Triangle> res = new ArrayList<Triangle>();
			Set<Triangle> allTriangles = new HashSet<Triangle>();
			// o and d have already been replaced by v
			for (Triangle t: nList)
			{
				if (!allTriangles.contains(t))
					res.add(t);
				allTriangles.add(t);
				work[0].bind(t);
				if (work[0].origin() != v)
					work[0].next();
				if (work[0].origin() != v)
					work[0].next();
				if (work[0].origin() == v)
				{
					// Add all triangles of the same fan to allTriangles
					boolean found = false;
					Vertex end = work[0].destination();
					do
					{
						work[0].nextOriginLoop();
						allTriangles.add(work[0].tri);
						if (work[0].destination() == v)
						{
							found = true;
							copyOTri(work[0], work[1]);
						}
					}
					while (work[0].destination() != end);
					if (found)
					{
						work[1].next();
						end = work[1].destination();
						do
						{
							work[1].nextOriginLoop();
							allTriangles.add(work[1].tri);
						}
						while (work[1].destination() != end);
					}
				}
				boolean found = false;
				if (work[0].destination() == v)
				{
					found = true;
					work[0].next();
				}
				else if (work[0].apex() == v)
				{
					found = true;
					work[0].prev();
				}
				if (found)
				{
					// Add all triangles of the same fan to allTriangles
					found = false;
					Vertex end = work[0].destination();
					do
					{
						work[0].nextOriginLoop();
						allTriangles.add(work[0].tri);
						if (work[0].destination() == v)
						{
							found = true;
							copyOTri(work[0], work[1]);
						}
					}
					while (work[0].destination() != end);
					if (found)
					{
						work[1].next();
						end = work[1].destination();
						do
						{
							work[1].nextOriginLoop();
							allTriangles.add(work[1].tri);
						}
						while (work[1].destination() != end);
					}
				}
			}
			v.setLink(new Triangle[res.size()]);
			res.toArray((Triangle[]) v.getLink());
		}
	}
	
	/**
	 * Splits an edge.  This is the opposite of collapse.
	 *
	 * @param m  mesh
	 * @param n  vertex being inserted
	 * @return edge starting from origin and pointing to <code>n</code>
	 * @see Mesh#vertexSplit
	 */
	@Override
	protected final AbstractHalfEdge split(AbstractMesh m, AbstractVertex n)
	{
		if (logger.isDebugEnabled())
			logger.debug("split edge ("+origin()+" "+destination()+") by adding vertex "+n);
		Mesh mesh = (Mesh) m;
		if (mesh.hasNodes())
			mesh.add(n);
		Vertex v = (Vertex) n;
		if (!hasAttributes(NONMANIFOLD))
		{
			v.setLink(tri);
			VHsplitSameFan(mesh, v);
			return this;
		}
		// VHsplitSameFan may modify internal data structure
		// used by fanIterator(), we need a copy.
		Map<Triangle, Integer> copy = new LinkedHashMap<Triangle, Integer>();
		// Set vertex links
		ArrayList<Triangle> link = new ArrayList<Triangle>();
		for (Iterator<AbstractHalfEdge> it = fanIterator(); it.hasNext(); )
		{
			VirtualHalfEdge f = (VirtualHalfEdge) it.next();
			link.add(f.tri);
			copy.put(f.tri, int3[f.localNumber]);
		}
		v.setLink(new Triangle[link.size()]);
		link.toArray((Triangle[]) v.getLink());
		link.clear();
		Triangle f = null;
		int fEdge = -1;
		for (Map.Entry<Triangle, Integer> entry: copy.entrySet())
		{
			Triangle t = entry.getKey();
			int l = entry.getValue().intValue();
			work[3].bind(t, l);
			work[3].VHsplitSameFan(mesh, v);
			// New edge is in work[1]
			if (f == null)
			{
				f = work[0].tri;
				fEdge = work[0].localNumber;
				// Initializes an empty cycle
				nextOTri(work[0], work[3]);
				work[0].prev();
				work[0].VHglue(work[3]);
			}
			else
			{
				// Adds work[1] to the cycle
				work[0].prev();
				work[3].bind(f, fEdge);
				work[3].next();
				copyOTri(work[3], work[2]);
				// Store old sym into work[3]
				work[3].sym();
				work[0].VHglue(work[2]);
				work[0].prev();
				work[0].VHglue(work[3]);
			}
		}
		return this;
	}
	/*
	 * Warning: this method uses work[0], work[1] and work[2] temporary arrays.
	 */
	private void VHsplitSameFan(Mesh m, Vertex n)
	{
		if (hasAttributes(OUTER))
			throw new IllegalArgumentException("Cannot split "+this);

		/*
		 *            V1                             V1
		 *            /'\                            /|\
		 *          /     \                        /  |  \
		 *        /      h1 \                    /    |    \
		 *      /             \                /    n1|   h1 \
		 *    /       t1        \            /   t1   |  t3    \
		 * o +-------------------+ d ---> o +---------+---------+ d
		 *    \       t2        /            \   t4   |  t2    /
		 *      \             /                \      |n2    /
		 *        \h2       /                    \h2  |    /
		 *          \     /                        \  |  /
		 *            \,/                            \|/
		 *            V2                             V2
		 */
		splitVertexAddOneTriangle(m, n);
		symOTri(this, work[0]);
		work[0].splitVertexAddOneTriangle(m, n);
		
		// Now we must update links:
		// 1. Link together t1/t4 and t2/t3.
		Triangle t1 = tri;
		nextOTri(this, work[0]);        // (nV1o)
		work[0].sym();                  // (V1nd)
		work[0].next();                 // (ndV1)
		Triangle t3 = work[0].tri;

		symOTri(this, work[1]);         // (dnV2)
		work[0].VHglue(work[1]);
		Triangle t2 = work[1].tri;
		work[1].next();                 // (nV2d)
		symOTri(work[1], work[0]);      // (V2no)
		work[0].next();                 // (noV2)
		VHglue(work[0]);
		Triangle t4 = work[0].tri;
		work[1].prev();                 // (dnV2)
		// 2. Remove links between outer triangles
		if (t2.isOuter())
		{
			// Remove links between t2 and t4,
			// and link h2.sym to n2
			work[0].next();         // (oV2n)
			int l4 = work[0].localNumber;
			work[0].next();         // (V2no)
			symOTri(work[0], work[1]);    // (nV2d)
			work[0].prev();         // (oV2n)
			if (work[0].getAdj() != null)
			{
				work[0].sym();
				work[0].VHglue(work[1]);
				work[0].bind(t4, l4); // (oV2n)
				work[0].setAdj(null);
				work[0].next(); // (v2no)
				work[0].setAdj(null);
			}
			else
			{
				work[0].next(); // (v2no)
				work[0].setAdj(null);
				work[1].setAdj(null);
				work[0].prev(); // (ov2n)
			}
			// t2 now contains good links, t4 may need
			// to be fixed.
			// Move work[1] so that d == work[1].origin()
			work[1].prev();         // (dnV2)
			// Move work[0], this value will be used by split()
			work[0].next();         // (noV2)
		}

		Triangle t14 = (t1.isOuter() ? t4 : t1);
		Triangle t23 = (t2.isOuter() ? t3 : t2);
		//  Update vertex links
		replaceVertexLinks(n, t1, t2, t14);
		replaceVertexLinks(work[1].origin(), t1, t2, t23);
		replaceVertexLinks(origin(), t1, t2, t14);
	}
	
	/*
	 * Warning: this method uses work[1] and work[2] temporary arrays.
	 */
	private final void splitVertexAddOneTriangle(Mesh m, Vertex n)
	{
		/*
		 *            V1                             V1
		 *            /'\                            /|\
		 *          /     \                        /  |  \
		 *        /      w1 \                    /  w1| w2 \
		 *      /             \                /      |      \
		 *    /       t1        \            /   t1   |  t3    \
		 * o +-------------------+ d ---> o +---------+---------+ d
		 */
		TriangleVH t1 = (TriangleVH) tri;
		TriangleVH t3 = (TriangleVH) m.createTriangle(t1);
		m.add(t3);
		
		if (!hasAttributes(OUTER))
		{
			nextOTri(this, work[2]);                // (dV1o)
			symOTri(work[2], work[1]);              // (V1d*)
			work[2].bind(t3, work[2].localNumber);  // (dV1n)
			work[1].VHglue(work[2]);
		}

		next();                         // (nV1o)
		work[1].bind(t3, localNumber);  // (dV1n)

		// Update Triangle links
		tri = t1;
		work[1].tri = t3;

		// Update vertices
		setOrigin(n);
		work[1].setApex(n);

		// Inner edge
		work[1].next();                 // (V1nd)
		VHglue(work[1]);

		// Clear BOUNDARY and NONMANIFOLD flags on inner edges
		work[1].clearAttributes(BOUNDARY | NONMANIFOLD);
		clearAttributes(BOUNDARY | NONMANIFOLD);
		prev();                         // (onV1)
	}
	
	private final Iterator<AbstractHalfEdge> identityFanIterator()
	{
		final VirtualHalfEdge current = this;
		logger.debug("Manifold fan iterator");
		return new Iterator<AbstractHalfEdge>()
		{
			private boolean nextFan = true;
			public boolean hasNext()
			{
				return nextFan;
			}
			public AbstractHalfEdge next()
			{
				if (!nextFan)
					throw new NoSuchElementException();
				nextFan = false;
				return current;
			}
			public void remove()
			{
			}
		};
	}
	
	/**
	 * Returns an iterator over triangle fans connected to this edge.  If edge is
	 * manifold, this iterator contains a single value, which is this edge.
	 * But if it is non-manifold and bound to <em>n</em> triangles, this iterator
	 * returns successively the <em>n</em> edges contained in these triangles and
	 * connected to the same endpoints.
	 *
	 * @return  iterator over triangle fans connected to this edge
	 */
	@Override
	public final Iterator<AbstractHalfEdge> fanIterator()
	{
		if (!hasAttributes(NONMANIFOLD))
			return identityFanIterator();
		logger.debug("Non manifold fan iterator");
		return new Iterator<AbstractHalfEdge>()
		{
			private Triangle last = (Triangle) tri.getAdj(localNumber);
			private int lastNumber = tri.getAdjLocalNumber(localNumber);
			VirtualHalfEdge ret = new VirtualHalfEdge();
			VirtualHalfEdge current = new VirtualHalfEdge();
			public boolean hasNext()
			{
				return last != current.tri;
			}
			public AbstractHalfEdge next()
			{
				if (current.tri == null)
				{
					current.bind(last, lastNumber);
					current.prev();
				}
				current.prev();
				current.sym();
				copyOTri(current, ret);
				ret.next();
				ret.sym();
				return ret;
			}
			public void remove()
			{
			}
		};
	}

	@Override
	public String toString()
	{
		StringBuilder r = new StringBuilder();
		r.append("hashCode: "+hashCode());
		r.append("\nTri hashcode: "+tri.hashCode());
		r.append("\nGroup: "+tri.getGroupId());
		r.append("\nLocal number: "+localNumber);
		if (getAdj() != null)
			r.append("\nSym: "+tri.getAdj(localNumber).hashCode()+"["+tri.getAdjLocalNumber(localNumber)+"]");
		r.append("\nAttributes: "+Integer.toHexString(tri.getEdgeAttributes(localNumber)));
		r.append("\nVertices:");
		r.append("\n  Origin: "+origin());
		r.append("\n  Destination: "+destination());
		r.append("\n  Apex: "+apex());
		return r.toString();
	}

}
