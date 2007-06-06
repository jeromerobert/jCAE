/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005,2006, by EADS CRC

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
import java.util.HashSet;
import java.util.Stack;
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
	
	private static final Integer [] int3 = new Integer[3];
	static {
		int3[0] = Integer.valueOf(0);
		int3[1] = Integer.valueOf(1);
		int3[2] = Integer.valueOf(2);
	}

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
	 * Create an object to handle data about a triangle.
	 *
	 * @param t  geometrical triangle.
	 * @param o  a number between 0 and 2 determining an edge.
	 */
	public VirtualHalfEdge(Triangle t, int o)
	{
		tri = t;
		localNumber = o;
		pullAttributes();
	}
	
	/**
	 * Find the <code>VirtualHalfEdge</code> joining two given vertices
	 * and move to it.
	 *
	 * @param v1  start point of the desired <code>VirtualHalfEdge</code>
	 * @param v2  end point of the desired <code>VirtualHalfEdge</code>
	 * @return <code>true</code> if an <code>VirtualHalfEdge</code> was
	 *         found, <code>false</code> otherwise.
	 */
	public boolean find(Vertex v1, Vertex v2)
	{
		if (v2.getLink() instanceof Triangle && v2.getLink() instanceof Triangle[])
		{
			boolean ret = find(v2, v1);
			if (!ret)
				return ret;
			sym();
			return !hasAttributes(OUTER);
		}
		bind((Triangle) v1.getLink());
		assert tri.vertex[0] == v1 || tri.vertex[1] == v1 || tri.vertex[2] == v1 : v1+" "+tri;
		if (destination() == v1)
			next();
		else if (apex() == v1)
			prev();
		assert origin() == v1 : v1+" not in "+this;
		Vertex d = destination();
		if (d == v2)
			return true;
		do
		{
			nextOriginLoop();
			if (destination() == v2)
				return true;
		}
		while (destination() != d);
		return false;
	}
	
	// Section: accessors
	
	/**
	 * Return the triangle tied to this object.
	 *
	 * @return the triangle tied to this object.
	 */
	public final Triangle getTri()
	{
		return tri;
	}
	
	/**
	 * Return the edge local number.
	 *
	 * @return the edge local number.
	 */
	public final int getLocalNumber()
	{
		return localNumber;
	}
	
	/**
	 * Set the triangle tied to this object, and resets localNumber.
	 *
	 * @param t  the triangle tied to this object.
	 */
	public final void bind(Triangle t)
	{
		tri = t;
		localNumber = 0;
		pullAttributes();
	}
	
	/**
	 * Set the triangle tied to this object, and the localNumber.
	 *
	 * @param t  the triangle tied to this object.
	 * @param l  the local number.
	 */
	public final void bind(Triangle t, int l)
	{
		tri = t;
		localNumber = l;
		pullAttributes();
	}
	
	// Section: attributes handling
	
	/**
	 * Check if some attributes of this oriented triangle are set.
	 *
	 * @param attr  the attributes to check
	 * @return <code>true</code> if this VirtualHalfEdge has all these
	 * attributes set, <code>false</code> otherwise.
	 */
	public final boolean hasAttributes(int attr)
	{
		return (attributes & attr) != 0;
	}
	
	/**
	 * Set attributes of this oriented triangle.
	 *
	 * @param attr  the attribute of this oriented triangle.
	 */
	public final void setAttributes(int attr)
	{
		attributes |= attr;
		pushAttributes();
	}
	
	/**
	 * Reset attributes of this oriented triangle.
	 *
	 * @param attr   the attributes of this oriented triangle to clear out.
	 */
	public final void clearAttributes(int attr)
	{
		attributes &= ~attr;
		pushAttributes();
	}
	
	// Adjust tri.adjPos after attributes is modified.
	public final void pushAttributes()
	{
		tri.setEdgeAttributes(localNumber, attributes);
	}
	
	// Adjust attributes after tri.adjPos is modified.
	public final void pullAttributes()
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
	
	/**
	 * Copy an <code>VirtualHalfEdge</code> into another <code>VirtualHalfEdge</code>.
	 *
	 * @param src   <code>VirtualHalfEdge</code> being duplicated
	 * @param dest  already allocated <code>VirtualHalfEdge</code> where data are
	 *              copied
	 */
	public static final void copyOTri(VirtualHalfEdge src, VirtualHalfEdge dest)
	{
		dest.tri = src.tri;
		dest.localNumber = src.localNumber;
		dest.attributes = src.attributes;
	}
	
	//  These geometrical primitives have 2 signatures:
	//      fct(this, that)   applies fct to 'this' and stores result
	//                        in an already allocated object 'that'.
	//      fct() transforms current object.
	//  This is definitely not an OO approach, but it is much more
	//  efficient by preventing useless memory allocations.
	//  They do not return any value to make clear that calling
	//  these routines requires extra care.
	
	/**
	 * Copy an <code>VirtualHalfEdge</code> and move to its symmetric edge.
	 *
	 * @param o     source <code>VirtualHalfEdge</code>
	 * @param that  already allocated <code>VirtualHalfEdge</code> where data are
	 *              copied
	 */
	public static final void symOTri(VirtualHalfEdge o, VirtualHalfEdge that)
	{
		that.tri = (Triangle) o.tri.getAdj(o.localNumber);
		that.localNumber = o.tri.getAdjLocalNumber(o.localNumber);
		that.pullAttributes();
	}
	
	public final AbstractHalfEdge sym(AbstractHalfEdge that)
	{
		VirtualHalfEdge dest = (VirtualHalfEdge) that;
		dest.tri = (Triangle) tri.getAdj(localNumber);
		dest.localNumber = tri.getAdjLocalNumber(localNumber);
		dest.pullAttributes();
		return dest;
	}
	
	/**
	 * Move to the symmetric edge.
	 */
	public final AbstractHalfEdge sym()
	{
		int neworient = tri.getAdjLocalNumber(localNumber);
		tri = (Triangle) tri.getAdj(localNumber);
		localNumber = neworient;
		pullAttributes();
		return this;
	}
	
	/**
	 * Copy an <code>VirtualHalfEdge</code> and move it to the counterclockwaise
	 * following edge.
	 *
	 * @param o     source <code>VirtualHalfEdge</code>
	 * @param that  already allocated <code>VirtualHalfEdge</code> where data are
	 *              copied
	 */
	public static final void nextOTri(VirtualHalfEdge o, VirtualHalfEdge that)
	{
		that.tri = o.tri;
		that.localNumber = next3[o.localNumber];
		that.pullAttributes();
	}
	
	public final AbstractHalfEdge next(AbstractHalfEdge that)
	{
		VirtualHalfEdge dest = (VirtualHalfEdge) that;
		dest.tri = tri;
		dest.localNumber = next3[localNumber];
		dest.pullAttributes();
		return dest;
	}
	
	/**
	 * Move to the counterclockwaise following edge.
	 */
	public final AbstractHalfEdge next()
	{
		localNumber = next3[localNumber];
		pullAttributes();
		return this;
	}
	
	/**
	 * Copy an <code>VirtualHalfEdge</code> and move it to the counterclockwaise
	 * previous edge.
	 *
	 * @param o     source <code>VirtualHalfEdge</code>
	 * @param that  already allocated <code>VirtualHalfEdge</code> where data are
	 *              copied
	 */
	public static final void prevOTri(VirtualHalfEdge o, VirtualHalfEdge that)
	{
		that.tri = o.tri;
		that.localNumber = prev3[o.localNumber];
		that.pullAttributes();
	}
	
	public final AbstractHalfEdge prev(AbstractHalfEdge that)
	{
		VirtualHalfEdge dest = (VirtualHalfEdge) that;
		dest.tri = tri;
		dest.localNumber = prev3[localNumber];
		dest.pullAttributes();
		return dest;
	}
	
	/**
	 * Move to the counterclockwaise previous edge.
	 */
	public final AbstractHalfEdge prev()
	{
		localNumber = prev3[localNumber];
		pullAttributes();
		return this;
	}
	
	/**
	 * Copy current <code>VirtualHalfEdge</code> and move it to the counterclockwaise
	 * following edge which has the same origin.
	 *
	 * @param that  already allocated <code>VirtualHalfEdge</code> where data are
	 *              copied
	 */
	public final AbstractHalfEdge nextOrigin(AbstractHalfEdge that)
	{
		return prev(that).sym();
	}
	
	/**
	 * Move counterclockwaise to the following edge with the same origin.
	 */
	public final AbstractHalfEdge nextOrigin()
	{
		return prev().sym();
	}
	
	/**
	 * Copy current <code>VirtualHalfEdge</code> and move it to the counterclockwaise
	 * previous edge which has the same origin.
	 *
	 * @param that  already allocated <code>VirtualHalfEdge</code> where data are
	 *              copied
	 */
	public final AbstractHalfEdge prevOrigin(AbstractHalfEdge that)
	{
		return sym(that).next();
	}
	
	/**
	 * Move counterclockwaise to the previous edge with the same origin.
	 */
	public final AbstractHalfEdge prevOrigin()
	{
		return sym().next();
	}
	
	/**
	 * Copy current <code>VirtualHalfEdge</code> and move it to the counterclockwaise
	 * following edge which has the same destination.
	 *
	 * @param that  already allocated <code>VirtualHalfEdge</code> where data are
	 *              copied
	 */
	public final AbstractHalfEdge nextDest(AbstractHalfEdge that)
	{
		return sym(that).prev();
	}
	
	/**
	 * Move counterclockwaise to the following edge with the same
	 * destination.
	 */
	public final AbstractHalfEdge nextDest()
	{
		return sym().prev();
	}
	
	/**
	 * Copy current <code>VirtualHalfEdge</code> and move it to the counterclockwaise
	 * previous edge which has the same destination.
	 *
	 * @param that  already allocated <code>VirtualHalfEdge</code> where data are
	 *              copied
	 */
	public final AbstractHalfEdge prevDest(AbstractHalfEdge that)
	{
		return next(that).sym();
	}
	
	/**
	 * Move counterclockwaise to the previous edge with the same
	 * destination.
	 */
	public final AbstractHalfEdge prevDest()
	{
		return next().sym();
	}
	
	/**
	 * Copy current <code>VirtualHalfEdge</code> and move it to the counterclockwaise
	 * following edge which has the same apex.
	 *
	 * @param that  already allocated <code>VirtualHalfEdge</code> where data are
	 *              copied
	 */
	public final AbstractHalfEdge nextApex(AbstractHalfEdge that)
	{
		return next(that).sym().next();
	}
	
	/**
	 * Move counterclockwaise to the following edge with the same apex.
	 */
	public final AbstractHalfEdge nextApex()
	{
		return next().sym().next();
	}
	
	/**
	 * Copy an <code>VirtualHalfEdge</code> and move it to the clockwaise
	 * previous edge which has the same apex.
	 *
	 * @param o     source <code>VirtualHalfEdge</code>
	 * @param that  already allocated <code>VirtualHalfEdge</code> where data are
	 *              copied
	 */
	private static final void prevOTriApex(VirtualHalfEdge o, VirtualHalfEdge that)
	{
		prevOTri(o, that);
		that.sym();
		that.prev();
	}
	
	public final AbstractHalfEdge prevApex(AbstractHalfEdge that)
	{
		return prev(that).sym().prev();
	}
	
	/**
	 * Move clockwaise to the previous edge with the same apex.
	 */
	public final AbstractHalfEdge prevApex()
	{
		return prev().sym().prev();
	}
	
	/**
	 * Move counterclockwaise to the following edge with the same apex.
	 * If a boundary is reached, loop backward until another
	 * boundary is found and start again from there.
	 */
	public final AbstractHalfEdge nextApexLoop()
	{
		prev();
		nextOriginLoop();
		next();
		return this;
	}
	
	/**
	 * Move counterclockwaise to the following edge with the same origin.
	 * If a boundary is reached, loop backward until another
	 * boundary is found and start again from there.
	 * Note: outer triangles are taken into account in this loop, because
	 * this is sometimes needed, as in VirtualHalfEdge2D.removeDegenerated().
	 * They have to be explicitly filtered out by testing hasAttributes(OUTER).
	 */
	public final AbstractHalfEdge nextOriginLoop()
	{
		if (hasAttributes(OUTER) && hasAttributes(BOUNDARY | NONMANIFOLD))
		{
			// Loop clockwise to another boundary
			// and start again from there.
			do
			{
				prevOrigin();
			}
			while (!hasAttributes(OUTER));
		}
		else
			nextOrigin();
		return this;
	}
	
	// Section: vertex handling
	
	/**
	 * Returns the start vertex of this edge.
	 *
	 * @return the start vertex of this edge.
	 */
	public Vertex origin()
	{
		return tri.vertex[next3[localNumber]];
	}
	
	/**
	 * Returns the end vertex of this edge.
	 *
	 * @return the end vertex of this edge.
	 */
	public Vertex destination()
	{
		return tri.vertex[prev3[localNumber]];
	}
	
	/**
	 * Returns the apex of this edge.
	 *
	 * @return the apex of this edge.
	 */
	public Vertex apex()
	{
		return tri.vertex[localNumber];
	}
	
	//  The following 3 methods change the underlying triangle.
	//  So they also modify all VirtualHalfEdge bound to this one.
	/**
	 * Sets the start vertex of this edge.
	 *
	 * @param v  the start vertex of this edge.
	 */
	public final void setOrigin(Vertex v)
	{
		tri.vertex[next3[localNumber]] = v;
	}
	
	/**
	 * Sets the end vertex of this edge.
	 *
	 * @param v  the end vertex of this edge.
	 */
	public final void setDestination(Vertex v)
	{
		tri.vertex[prev3[localNumber]] = v;
	}
	
	/**
	 * Sets the apex of this edge.
	 *
	 * @param v  the apex of this edge.
	 */
	public final void setApex(Vertex v)
	{
		tri.vertex[localNumber] = v;
	}
	
	// Section: adjacency
	
	/**
	 * Sets adjacency relations between two triangles.
	 *
	 * @param sym  the triangle bond to this one.
	 */
	public final void glue(AbstractHalfEdge sym)
	{
		VHglue((VirtualHalfEdge) sym);
	}
	private void VHglue(VirtualHalfEdge sym)
	{
		assert !(hasAttributes(NONMANIFOLD) || sym.hasAttributes(NONMANIFOLD)) : this+"\n"+sym;
		tri.setAdj(localNumber, sym.tri);
		tri.setAdjLocalNumber(localNumber, sym.localNumber);
		sym.tri.setAdj(sym.localNumber, tri);
		sym.tri.setAdjLocalNumber(sym.localNumber, localNumber);
	}
	
	/**
	 * Gets adjacency relation for an edge
	 *
	 * @return the triangle bond to this one if this edge is manifold, or an Object otherwise.
	 */
	public final Object getAdj()
	{
		return tri.getAdj(localNumber);
	}
	
	/**
	 * Sets adjacency relation for an edge
	 *
	 * @param link  the triangle bond to this one if this edge is manifold, or an Object otherwise.
	 */
	public final void setAdj(Object link)
	{
		tri.setAdj(localNumber, link);
	}
	
	// Section: 3D geometrical routines
	
	/**
	 * Compute the normal of an edge, in the triangle plane.
	 * This vector is not normalized, it has the same length as
	 * this edge.  The result is stored in the tempD temporary array.
	 * @see #getTempVector
	 * @return the area of this triangle.
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
	 * Compute the normal of this triangle.  The result is stored in
	 * the tempD temporary array.
	 * @see #getTempVector
	 * @return the area of this triangle.
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
	 * Return the area of this triangle.
	 * @return the area of this triangle.
	 * Warning: this method uses tempD, tempD1 and tempD2 temporary arrays.
	 */
	public double computeArea()
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
	 * Return the temporary array TempD.
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
		double s3 = 0.5 * Matrix3D.prodSca(n1, o.outer3D(n, a));
		if (s3 <= 0.0)
			return invalid;
		double s4 = 0.5 * Matrix3D.prodSca(n1, d.outer3D(a, n));
		if (s4 <= 0.0)
			return invalid;
		double p1 = o.distance3D(d) + d.distance3D(a) + a.distance3D(o);
		double s1 = computeArea();
		double p2 = d.distance3D(o) + o.distance3D(n) + n.distance3D(d);
		double s2 = work[0].computeArea();
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
	 * This routine swaps an edge (od) to (na), updates
	 * adjacency relations and backward links between vertices and
	 * triangles.  Current object is transformed from (oda) to (ona)
	 * and not (nao), because this helps turning around o, eg.
	 * at the end of {@link org.jcae.mesh.amibe.patch.OTriangle2D#split3}.
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
	 */
	public final AbstractHalfEdge swap()
	{
		VHswap();
		return this;
	}
	private final void VHswap()
	{
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
		assert !hasAttributes(OUTER | BOUNDARY | NONMANIFOLD);
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
		o.setLink(tri);
		d.setLink(work[2].tri);
		pullAttributes();
	}
	
	/**
	 * Checks that triangles are not inverted if this edge is contracted.
	 *
	 * @param newpt  the point which will become the contraction of
	 *    this edge.
	 * @return <code>false</code> if this edge contraction produces
	 *    an inverted triangle, <code>true</code> otherwise.
	 */
	public final boolean checkNewRingNormals(double [] newpt)
	{
		//  Loop around apex to check that triangles will not be inverted
		Vertex d = destination();
		nextOTri(this, work[0]);
		do
		{
			if (work[0].hasAttributes(OUTER))
			{
				work[0].nextApexLoop();
				continue;
			}
			double area  = work[0].computeNormal3DT();
			double [] nu = work[0].getTempVector();
			double [] x1 = work[0].origin().getUV();
			for (int i = 0; i < 3; i++)
				tempD1[i] = newpt[i] - x1[i];
			if (Matrix3D.prodSca(tempD1, nu) >= - area)
				return false;
			work[0].nextApexLoop();
		}
		while (work[0].origin() != d);
		return true;
	}
	
	/**
	 * Check whether an edge can be contracted.
	 *
	 * @param n the resulting vertex
	 * @return <code>true</code> if this edge can be contracted into the single vertex n, <code>false</code> otherwise.
	 */
	public final boolean canCollapse(AbstractVertex n)
	{
		/*  
		 * Topology check:  (od) cannot be contracted with the pattern
		 * below, because T1 and T2 are then connected to the same
		 * vertices.  This happens for instance when trying to remove
		 * an edge from a tetrahedron.
		 *
		 *                 V
		 *                 +
		 *                /|\
		 *               / | \
		 *              /  |  \
		 *             /  a|   \
		 *            /    +    \
		 *           / T1 / \ T2 \
		 *          /   /     \   \
		 *         /  /         \  \
		 *        / /             \ \
		 *     o +-------------------+ d
		 */
		nextOTri(this, work[0]);
		prevOTri(this, work[1]);
		if (!work[0].hasAttributes(OUTER) && work[0].getAdj() != null && work[1].getAdj() != null)
		{
			work[0].nextDest();
			work[1].prevOrigin();
			if (work[0].origin() == work[1].destination())
				return false;
		}
		symOTri(this, work[0]);
		prevOTri(work[0], work[1]);
		work[0].next();
		if (!work[0].hasAttributes(OUTER) && work[0].getAdj() != null && work[1].getAdj() != null)
		{
			work[0].nextDest();
			work[1].prevOrigin();
			if (work[0].origin() == work[1].destination())
				return false;
		}

		return checkInversion((Vertex) n);
	}
	
	private final boolean checkInversion(Vertex n)
	{
		Vertex o = origin();
		Vertex d = destination();
		Vertex a = apex();
		nextOTri(this, work[0]);
		prevOTri(this, work[1]);
		//  If both vertices are non-manifold, do not contract
		//  TODO: allow contracting non-manifold edges
		if (o.getLink() instanceof Triangle[] && d.getLink() instanceof Triangle[])
			return false;
		//  If both adjacent edges are on a boundary, do not contract
		if ((work[0].hasAttributes(BOUNDARY) || work[0].hasAttributes(NONMANIFOLD)) && (work[1].hasAttributes(BOUNDARY) || work[1].hasAttributes(NONMANIFOLD)))
			return false;
		symOTri(this, work[1]);
		symOTri(this, work[0]);
		work[0].prev();
		work[1].next();
		if ((work[0].hasAttributes(BOUNDARY) || work[0].hasAttributes(NONMANIFOLD)) && (work[1].hasAttributes(BOUNDARY) || work[1].hasAttributes(NONMANIFOLD)))
			return false;
		//  Loop around o to check that triangles will not be inverted
		nextOTri(this, work[0]);
		symOTri(this, work[1]);
		double [] xn = n.getUV();
		do
		{
			//  TODO: allow contracting edges when a vertex is non manifold
			if (work[0].origin().getLink() instanceof Triangle[])
				return false;
			if (work[0].tri != tri && work[0].tri != work[1].tri && !work[0].hasAttributes(OUTER))
			{
				double area  = work[0].computeNormal3DT();
				double [] nu = work[0].getTempVector();
				double [] x1 = work[0].origin().getUV();
				for (int i = 0; i < 3; i++)
					tempD1[i] = xn[i] - x1[i];
				// Two triangles are removed when an edge is contracted.
				// So normally triangle areas should increase.  If they
				// decrease significantly, there may be a problem.
				if (Matrix3D.prodSca(tempD1, nu) >= - area)
					return false;
			}
			work[0].nextApexLoop();
		}
		while (work[0].origin() != d);
		//  Loop around d to check that triangles will not be inverted
		copyOTri(this, work[0]);
		work[0].prev();
		do
		{
			//  TODO: allow contracting edges when a vertex is non manifold
			if (work[0].origin().getLink() instanceof Triangle[])
				return false;
			if (work[0].tri != tri && work[0].tri != work[1].tri && !work[0].hasAttributes(OUTER))
			{
				double area  = work[0].computeNormal3DT();
				double [] nu = work[0].getTempVector();
				double [] x1 = work[0].origin().getUV();
				for (int i = 0; i < 3; i++)
					tempD1[i] = xn[i] - x1[i];
				if (Matrix3D.prodSca(tempD1, nu) >= - area)
					return false;
			}
			work[0].nextApexLoop();
		}
		while (work[0].origin() != a);
		return true;
	}
	
	/**
	 * Contract an edge.
	 * TODO: Attributes are not checked.
	 * @param n the resulting vertex
	 */
	public final AbstractHalfEdge collapse(AbstractMesh m, AbstractVertex n)
	{
		VHcollapse((Mesh) m, (Vertex) n);
		return this;
	}
	private final void VHcollapse(Mesh m, Vertex n)
	{
		Vertex o = origin();
		Vertex d = destination();
		logger.debug("contract ("+o+" "+d+")\ninto "+n);
		/*
		 *           V1                       V1
		 *  V3+-------+-------+ V4   V3 +------+------+ V4
		 *     \ t3  / \ t4  /           \  t3 | t4  / 
		 *      \   /   \   /              \   |   /
		 *       \ / t1  \ /                 \ | /  
		 *      o +-------+ d   ------>      n +
		 *       / \ t2  / \                 / | \
		 *      /   \   /   \              /   |   \
		 *     / t5  \ / t6  \           /  t5 | t6  \
		 *    +-------+-------+         +------+------+
		 *  V5        V2       V6     V5       V2      V6
		 */
		// this = (odV1)
		
		//  Replace o by n in all incident triangles
		copyOTri(this, work[0]);
		do
		{
			work[0].setOrigin(n);
			work[0].nextOriginLoop();
		}
		while (work[0].destination() != d);
		//  Replace d by n in all incident triangles
		symOTri(this, work[0]);
		do
		{
			work[0].setOrigin(n);
			work[0].nextOriginLoop();
		}
		while (work[0].destination() != n);
		//  Update adjacency links.  For clarity, o and d are
		//  written instead of n.
		if (!hasAttributes(OUTER))
		{
			next();                 // (dV1o)
			int attr4 = attributes;
			symOTri(this, work[0]); // (V1dV4)
			next();                 // (V1od)
			int attr3 = attributes;
			symOTri(this, work[1]); // (oV1V3)
			work[0].VHglue(work[1]);
			work[0].attributes |= attr3;
			work[1].attributes |= attr4;
			work[0].pushAttributes();
			work[1].pushAttributes();
			Triangle t34 = work[1].tri;
			if (t34.isOuter())
				t34 = work[0].tri;
			assert !t34.isOuter() : work[0]+"\n"+work[1];
			work[1].destination().setLink(t34);
			n.setLink(t34);
			next();                 // (odV1)
		}
		sym();                          // (doV2)
		if (!hasAttributes(OUTER))
		{
			next();                 // (oV2d)
			int attr5 = attributes;
			symOTri(this, work[0]); // (V2oV5)
			next();                 // (V2do)
			int attr6 = attributes;
			symOTri(this, work[1]); // (dV2V6)
			work[0].VHglue(work[1]);
			work[0].attributes |= attr6;
			work[1].attributes |= attr5;
			work[0].pushAttributes();
			work[1].pushAttributes();
			Triangle t56 = work[0].tri;
			if (t56.isOuter())
				t56 = work[1].tri;
			assert !t56.isOuter();
			work[0].origin().setLink(t56);
			n.setLink(t56);
			next();                 // (doV2)
		}
		symOTri(this, work[0]);
		clearAttributes(MARKED);
		pushAttributes();
		m.remove(tri);
		// By convention, edge is moved into (oV1V3), but this may change.
		// We have to move before removing adjacency relations.
		nextOTri(work[0], this);        // (dV1o)
		sym();                          // (V1dV4)
		sym();                          // (oV1V3)
		work[0].clearAttributes(MARKED);
		work[0].pushAttributes();
		m.remove(work[0].tri);
	}
	
	/**
	 * Split an edge.  This is the opposite of contract.
	 *
	 * @param n the resulting vertex
	 */
	public final AbstractHalfEdge split(AbstractMesh m, AbstractVertex n)
	{
		VHsplit((Mesh) m, (Vertex) n);
		return this;
	}
	private void VHsplit(Mesh m, Vertex n)
	{
		/*
		 *         V1                       V1        
		 *          +                       /|\
		 *         / \                    /  |  \        
		 *        /   \                 / t1 | t3 \       
		 *       / t1  \              /      |      \      
		 *    o +-------+ d  --->  o +-------+-------+ d   
		 *       \ t2  /              \     n|      /      
		 *        \   /                 \ t2 | t4 /       
		 *         \ /                    \  |  /        
		 *          +                       \|/
		 *         V2                       V2
		 */
		// this = (odV1)
		Triangle t1 = tri;
		Triangle t2 = (Triangle) tri.getAdj(localNumber);
		Triangle t3 = (Triangle) m.factory.createTriangle(t1);
		Triangle t4 = (Triangle) m.factory.createTriangle(t2);
		m.add(t3);
		m.add(t4);
		copyOTri(this, work[1]);        // (odV1)
		// Update vertices
		setDestination(n);
		work[1].tri = t3;
		work[1].setOrigin(n);           // (ndV1)
		if (t1.isOuter())
		{
			n.setLink(t2);
			work[1].destination().setLink(t4);
		}
		else
		{
			n.setLink(t1);
			work[1].destination().setLink(t3);
		}
		
		nextOTri(this, work[0]);        // (nV1o)
		work[1].next();                 // (dV1n)
		if (work[0].getAdj() != null)
		{
			work[0].sym();          // (V1d*)
			work[1].VHglue(work[0]);
			nextOTri(this, work[0]);// (nV1o)
		}
		work[1].next();                 // (V1nd)
		work[1].VHglue(work[0]);
		work[0].clearAttributes(BOUNDARY);
		work[1].clearAttributes(BOUNDARY);
		work[1].next();                 // (ndV1)
		
		nextOTriDest(this, work[0]);    // (V2do)
		copyOTri(work[0], work[2]);     // (V2do)
		work[0].setDestination(n);      // (V2no)
		work[2].tri = t4;
		work[2].setApex(n);             // (V2dn)
		if (work[0].getAdj() != null)
		{
			work[0].sym();          // (dV2*)
			work[2].VHglue(work[0]);
			nextOTriDest(this, work[0]);    // (V2no)
		}
		work[2].prev();                 // (nV2d)
		work[2].VHglue(work[0]);
		work[0].clearAttributes(BOUNDARY | MARKED);
		work[2].clearAttributes(BOUNDARY | MARKED);
		work[2].prev();                 // (dnV2)
		work[2].VHglue(work[1]);
	}
	
	public void invertOrientationFace(boolean markLocked)
	{
		assert markLocked == true;
		// Swap origin and destination, update adjacency relations and process
		// neighbours
		Vertex o = origin();
		Vertex d = destination();
		Stack todo = new Stack();
		HashSet seen = new HashSet();
		todo.push(tri);
		todo.push(int3[localNumber]);
		swapVertices(seen, todo);
		assert o == destination() : o+" "+d+" "+this;
	}
	
	private static void swapVertices(HashSet seen, Stack todo)
	{
		VirtualHalfEdge ot = new VirtualHalfEdge();
		VirtualHalfEdge sym = new VirtualHalfEdge();
		while (todo.size() > 0)
		{
			int o = ((Integer) todo.pop()).intValue();
			Triangle t = (Triangle) todo.pop();
			if (seen.contains(t))
				continue;
			seen.add(t);
			// Swap vertices
			Vertex tempV = t.vertex[next3[o]];
			t.vertex[next3[o]] = t.vertex[prev3[o]];
			t.vertex[prev3[o]] = tempV;
			// Swap adjacent triangles
			Object tempA = t.getAdj(next3[o]);
			t.setAdj(next3[o], t.getAdj(prev3[o]));
			t.setAdj(prev3[o], tempA);
			// Swap edge attributes
			int attr = t.getEdgeAttributes(next3[o]);
			t.setEdgeAttributes(next3[o], t.getEdgeAttributes(prev3[o]));
			t.setEdgeAttributes(prev3[o], attr);
			// Fix adjacent triangles
			ot.bind(t);
			for (int i = 0; i < 3; i++)
			{
				ot.next();
				if (!ot.hasAttributes(BOUNDARY) && !ot.hasAttributes(NONMANIFOLD))
				{
					VirtualHalfEdge.symOTri(ot, sym);
					todo.push(sym.tri);
					todo.push(int3[sym.localNumber]);
					sym.tri.setAdj(sym.localNumber, ot.tri);
					sym.tri.setAdjLocalNumber(sym.localNumber, ot.localNumber);
				}
			}
		}
	}
	
	private final String showAdj(int num)
	{
		if (!(tri.getAdj(num) instanceof Triangle))
			return "N/A";
		String r = "";
		Triangle t = (Triangle) tri.getAdj(num);
		if (t == null)
			r+= "null";
		else
			r+= t.hashCode()+"["+tri.getAdjLocalNumber(num)+"]";
		return r;
	}
	
	public String toString()
	{
		String r = "Local number: "+localNumber;
		r += "\nTri hashcode: "+tri.hashCode();
		r += "\nAdjacency: "+showAdj(0)+" "+showAdj(1)+" "+showAdj(2);
		r += "\nAttributes: "+Integer.toHexString(tri.getEdgeAttributes(0))+" "+Integer.toHexString(tri.getEdgeAttributes(1))+" "+Integer.toHexString(tri.getEdgeAttributes(2))+" => "+Integer.toHexString(attributes);
		r += "\nVertices:";
		r += "\n  Origin: "+origin();
		r += "\n  Destination: "+destination();
		r += "\n  Apex: "+apex();
		return r;
	}

	private static void unitTestBuildMesh(Mesh m, Vertex [] v)
	{
		/*
		 *                       v2
		 *                       +
		 *  Initial            / |
		 *  triangulation    /   |
		 *                 /     |
		 *               /       |
		 *             +---------+
		 *             v0        v1
		 *
		 * Final result:
		 *  v4        v3        v2
		 *   +---------+---------+
		 *   | \       |       / |
		 *   |   \     |     /   |
		 *   |     \   |   /     |
		 *   |       \ | /       |
		 *   +---------+---------+
		 *   v5        v0       v1
		 */
		System.out.println("Building mesh...");
		Triangle T = (Triangle) m.factory.createTriangle(v[0], v[1], v[2]);
		m.add(T);
		assert m.isValid();
		// Outer triangles
		Triangle [] O = new Triangle[3];
		O[0] = (Triangle) m.factory.createTriangle(m.outerVertex, v[1], v[0]);
		O[1] = (Triangle) m.factory.createTriangle(m.outerVertex, v[2], v[1]);
		O[2] = (Triangle) m.factory.createTriangle(m.outerVertex, v[0], v[2]);
		for (int i = 0; i < 3; i++)
		{
			O[i].setOuter();
			m.add(O[i]);
		}
		assert m.isValid();
		VirtualHalfEdge ot1 = new VirtualHalfEdge();
		VirtualHalfEdge ot2 = new VirtualHalfEdge(T, 2);
		for (int i = 0; i < 3; i++)
		{
			ot1.bind(O[i]);
			ot1.setAttributes(BOUNDARY);
			ot2.setAttributes(BOUNDARY);
			ot1.VHglue(ot2);
			ot2.next();
		}
		assert ot2.origin() == v[0];
// m.printMesh();
		assert m.isValid();
		ot2.prev();           // (v2,v0,v1)
		ot2.VHsplit(m, v[3]); // (v2,v3,v1)
		assert m.isValid();
		ot2.next();           // (v3,v1,v2)
		ot2.VHswap();         // (v3,v0,v2)
		assert m.isValid();
		/*
		 *            v3        v2
		 *             +---------+
		 *             |       / |
		 *             |     /   |
		 *             |   /     |
		 *             | /       |
		 *             +---------+
		 *             v0       v1
		 */
		ot2.VHsplit(m, v[5]); // (v3,v5,v2)
		assert m.isValid();
		ot2.next();           // (v5,v2,v3)
		ot2.VHswap();         // (v5,v0,v3)
		assert m.isValid();
		/*
		 *            v3        v2
		 *             +---------+
		 *           / |       / |
		 *         /   |     /   |
		 *       /     |   /     |
		 *     /       | /       |
		 *   +---------+---------+
		 *   v5        v0       v1
		 */
		ot2.prev();           // (v3,v5,v0)
		ot2.VHsplit(m, v[4]); // (v3,v4,v0)
		assert m.isValid();
		/*
		 *  v4        v3        v2
		 *   +---------+---------+
		 *   | \       |       / |
		 *   |   \     |     /   |
		 *   |     \   |   /     |
		 *   |       \ | /       |
		 *   +---------+---------+
		 *   v5        v0       v1
		 */
	}
	
	private static void unitTestCheckLoopOrigin(Mesh m, Vertex o, Vertex d)
	{
		VirtualHalfEdge ot1 = new VirtualHalfEdge();
		VirtualHalfEdge ot2 = new VirtualHalfEdge();
		if (!ot2.find(o, d))
		        System.exit(-1);
		copyOTri(ot2, ot1);
		System.out.println("Loop around origin: "+o);
		System.out.println(" first destination: "+d);
		int cnt = 0;
		do
		{
			ot1.nextOriginLoop();
			cnt++;
		}
		while (ot1.destination() != d);
		assert cnt == 4 : "Failed test: LoopOrigin cnt != 4: "+o+" "+d;
	}
	
	private static void unitTestCheckLoopApex(Mesh m, Vertex a, Vertex o)
	{
		VirtualHalfEdge ot1 = new VirtualHalfEdge();
		VirtualHalfEdge ot2 = new VirtualHalfEdge();
		if (!ot2.find(a, o))
		        System.exit(-1);
		nextOTri(ot2, ot1);
		System.out.println("Loop around apex: "+a);
		System.out.println(" first origin: "+o);
		int cnt = 0;
		do
		{
			cnt++;
			ot1.nextApexLoop();
		}
		while (ot1.origin() != o);
		assert cnt == 4 : "Failed test: LoopApex cnt != 4: "+a+" "+o;
	}
	
	private static void unitTestCheckQuality(Mesh m, Vertex o, Vertex d, int expected)
	{
		VirtualHalfEdge ot1 = new VirtualHalfEdge();
		VirtualHalfEdge ot2 = new VirtualHalfEdge();
		if (!ot2.find(o, d))
		        System.exit(-1);
		nextOTri(ot2, ot1);
		System.out.println("Improve triangle quality around origin: "+o);
		System.out.println(" first destination: "+d);
		int cnt = 0;
		while(true)
		{
			if (ot1.checkSwap3D(0.95) >= 0.0)
			{
				// Swap edge
				ot1.VHswap();
				cnt++;
			}
			else
			{
				ot1.nextApexLoop();
				if (ot1.origin() == d)
					break;
			}
		}
		assert cnt == expected : "Failed test: QualityOrigin "+cnt+" != "+expected+": "+o+" "+d;
	}
	
	public static void main(String args[])
	{
		Mesh m = new Mesh();
		Vertex [] v = new Vertex[6];
		v[0] = (Vertex) m.factory.createVertex(0.0, 0.0, 0.0);
		v[1] = (Vertex) m.factory.createVertex(1.0, 0.0, 0.0);
		v[2] = (Vertex) m.factory.createVertex(1.0, 1.0, 0.0);
		v[3] = (Vertex) m.factory.createVertex(0.0, 1.0, 0.0);
		v[4] = (Vertex) m.factory.createVertex(-1.0, 1.0, 0.0);
		v[5] = (Vertex) m.factory.createVertex(-1.0, 0.0, 0.0);
		unitTestBuildMesh(m, v);
		assert m.isValid();
		System.out.println("Checking loops...");
		unitTestCheckLoopOrigin(m, v[3], v[4]);
		unitTestCheckLoopOrigin(m, v[3], v[2]);
		unitTestCheckLoopOrigin(m, v[3], m.outerVertex);
		unitTestCheckLoopApex(m, v[3], v[4]);
		unitTestCheckLoopApex(m, v[3], v[2]);
		unitTestCheckLoopApex(m, v[3], m.outerVertex);
		// Degrade triangle quality and swap edges
		v[1].moveTo(1.0, 0.5, 0.0);
		v[5].moveTo(-1.0, 0.5, 0.0);
		m = new Mesh();
		unitTestBuildMesh(m, v);
		assert m.isValid();
		unitTestCheckQuality(m, v[3], v[4], 2);
		unitTestCheckQuality(m, v[3], v[4], 0);
		assert m.isValid();
		m = new Mesh();
		unitTestBuildMesh(m, v);
		unitTestCheckQuality(m, v[3], v[2], 2);
		unitTestCheckQuality(m, v[3], v[2], 0);
		assert m.isValid();
		m = new Mesh();
		unitTestBuildMesh(m, v);
		unitTestCheckQuality(m, v[3], m.outerVertex, 2);
		unitTestCheckQuality(m, v[3], m.outerVertex, 0);
		assert m.isValid();
	}
	
}
