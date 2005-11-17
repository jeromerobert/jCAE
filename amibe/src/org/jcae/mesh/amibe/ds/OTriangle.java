/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2004,2005
                  Jerome Robert <jeromerobert@users.sourceforge.net>

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

import java.util.Random;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;
import org.jcae.mesh.amibe.metrics.Metric3D;
import org.apache.log4j.Logger;

/*
 * This class is derived from Jonathan R. Shewchuk's work
 * on Triangle, see
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
 * A handle to {@link Triangle} objects.
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
 *   between 0 and 2 can represent an edge.  This <code>OTriangle</code>
 *   class plays this role, it defines an <em>oriented triangle</em>, or
 *   in other words an oriented edge.  Instances of this class are tied to
 *   their underlying {@link Triangle} instances, so modifications are not
 *   local to this class!
 * </p>
 *
 * <p>
 *   The main goal of this class is to ease mesh traversal.
 *   Consider the <code>ot</code> {@link OTriangle} with a null localNumber of
 *   {@link Triangle} <code>t</code>below.
 * </p>
 * <pre>
 *                        V2
 *     V5 _________________,________________, V3
 *        \    &lt;----      / \     &lt;----     /
 *         \     1       /   \      1      /
 *          \   t3    -.//  /\\\   t0   _,/
 *           \      0 ///1   0\\\2    0 //   t.vertex = { V0, V1, V2 }
 *            \      //V   t   \\V     //   t0.vertex = { V2, V1, V3 }
 *             \     /           \     /    t2.vertex = { V0, V4, V1 }
 *              \   /      2      \   /     t3.vertex = { V5, V0, V2 }
 *               \ /     ----&gt;     \ /
 *             V0 +-----------------+ V1
 *                 \     &lt;----     /
 *                  \      1      /
 *                   \    t2   _,/
 *                    \       0//
 * </pre>
 * The following methods can be applied to <code>ot</code>:
 * <pre>
 *    ot.nextOTri();        // Moves (t,0) to (t,1)
 *    ot.prevOTri();        // Moves (t,0) to (t,2)
 *    ot.symOTri();         // Moves (t,0) to (t0,2)
 *    ot.nextOTriOrigin();  // Moves (t,0) to (t2,1)
 *    ot.prevOTriOrigin();  // Moves (t,0) to (t0,0)
 *    ot.nextOTriDest();    // Moves (t,0) to (t0,1)
 *    ot.prevOTriDest();    // Moves (t,0) to (t3,0)
 *    ot.nextOTriApex();    // Moves (t,0) to (t3,1)
 *    ot.prevOTriApex();    // Moves (t,0) to (t2,0)
 * </pre>
 */
public class OTriangle implements Cloneable
{
	private static Logger logger = Logger.getLogger(OTriangle.class);
	
	private static final int [] next3 = { 1, 2, 0 };
	private static final int [] prev3 = { 2, 0, 1 };
	private static final OTriangle otVoid = new OTriangle();
	
	private double [] tempD = new double[3];
	private double [] tempD1 = new double[3];
	private double [] tempD2 = new double[3];
	
	/**
	 * Numeric constants for edge attributes.  Set if edge is on
	 * boundary.
	 * @see #setAttributes
	 * @see #hasAttributes
	 */
	public static final int BOUNDARY = 1 << 0;
	/**
	 * Numeric constants for edge attributes.  Set if edge is outer.
	 * (Ie. one of its end point is {@link Vertex#outer})
	 * @see #setAttributes
	 * @see #hasAttributes
	 */
	public static final int OUTER    = 1 << 1;
	/**
	 * Numeric constants for edge attributes.  Set if edge had been
	 * swapped.
	 * @see #setAttributes
	 * @see #hasAttributes
	 */
	public static final int SWAPPED  = 1 << 2;
	/**
	 * Numeric constants for edge attributes.  Set if edge had been
	 * marked (for any operation).
	 * @see #setAttributes
	 * @see #hasAttributes
	 */
	public static final int MARKED   = 1 << 3;
	/**
	 * Numeric constants for edge attributes.  Set if edge is the inner
	 * edge of a quadrangle.
	 * @see #setAttributes
	 * @see #hasAttributes
	 */
	public static final int QUAD     = 1 << 4;
	/**
	 * Numeric constants for edge attributes.  Set if edge is non
	 * manifold.
	 * @see #setAttributes
	 * @see #hasAttributes
	 */
	public static final int NONMANIFOLD = 1 << 5;
	
	//  Complex algorithms require several OTriangle, they are
	//  allocated here to prevent allocation/deallocation overhead.
	private static OTriangle [] work = new OTriangle[4];
	static {
		for (int i = 0; i < 4; i++)
			work[i] = new OTriangle();
	}
	
	private static final Triangle dummy = new Triangle();
	
	/*
	 * Vertices can be accessed through
	 *        origin = tri.vertex[next3[localNumber]]
	 *   destination = tri.vertex[prev3[localNumber]]
	 *          apex = tri.vertex[localNumber]
	 * Adjacent triangle is tri.adj[localNumber].tri and its localNumber
	 * is ((tri.adjPos >> (2*localNumber)) & 3)
	 */
	protected Triangle tri = null;
	protected int localNumber = 0;
	protected int attributes = 0;
	
	/**
	 * Sole constructor.
	 */
	public OTriangle()
	{
	}
	
	/**
	 * Create an object to handle data about a triangle.
	 *
	 * @param t  geometrical triangle.
	 * @param o  a number between 0 and 2 determining an edge.
	 */
	public OTriangle(Triangle t, int o)
	{
		tri = t;
		localNumber = o;
		attributes = (tri.adjPos >> (8*(1+localNumber))) & 0xff;
	}
	
	public final Object clone()
	{
		Object ret = null;
		try
		{
			ret = super.clone();
			// No swallow copy for private arrays
			OTriangle that = (OTriangle) ret;
			that.tempD = new double[3];
			that.tempD1 = new double[3];
			that.tempD2 = new double[3];
		}
		catch (java.lang.CloneNotSupportedException ex)
		{
		}
		return ret;
	}
	/**
	 * Return the triangle tied to this object.
	 *
	 * @return the triangle tied to this object.
	 */
	public final Triangle getTri()
	{
		return tri;
	}
	
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
		attributes = (tri.adjPos >> 8) & 0xff;
	}
	
	public final void bind(Triangle t, int o)
	{
		tri = t;
		localNumber = o;
		pullAttributes();
	}
	
	/**
	 * Check if some attributes of this oriented triangle are set.
	 *
	 * @param attr  the attributes to check
	 * @return <code>true</code> if this OTriangle has all these
	 * attributes set, <code>false</code> otherwise.
	 */
	public final boolean hasAttributes(int attr)
	{
		return (attributes & attr) == attr;
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
		tri.adjPos &= ~(0xff << (8*(1+localNumber)));
		tri.adjPos |= ((attributes & 0xff) << (8*(1+localNumber)));
	}
	
	// Adjust attributes after tri.adjPos is modified.
	public final void pullAttributes()
	{
		attributes = (tri.adjPos >> (8*(1+localNumber))) & 0xff;
	}
	
	/**
	 * Copy an <code>OTriangle</code> into another <code>OTriangle</code>.
	 *
	 * @param src   <code>OTriangle</code> being duplicated
	 * @param dest  already allocated <code>OTriangle</code> where data are
	 *              copied
	 */
	public static final void copyOTri(OTriangle src, OTriangle dest)
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
	 * Copy an <code>OTriangle</code> and move to its symmetric edge.
	 *
	 * @param o     source <code>OTriangle</code>
	 * @param that  already allocated <code>OTriangle</code> where data are
	 *              copied
	 */
	public static final void symOTri(OTriangle o, OTriangle that)
	{
		that.tri = (Triangle) o.tri.getAdj(o.localNumber);
		that.localNumber = ((o.tri.adjPos >> (2*o.localNumber)) & 3);
		that.attributes = (that.tri.adjPos >> (8*(1+that.localNumber))) & 0xff;
	}
	
	/**
	 * Move to the symmetric edge.
	 */
	public final void symOTri()
	{
		int neworient = ((tri.adjPos >> (2*localNumber)) & 3);
		tri = (Triangle) tri.getAdj(localNumber);
		localNumber = neworient;
		attributes = (tri.adjPos >> (8*(1+localNumber))) & 0xff;
	}
	
	/**
	 * Copy an <code>OTriangle</code> and move it to the counterclockwaise
	 * following edge.
	 *
	 * @param o     source <code>OTriangle</code>
	 * @param that  already allocated <code>OTriangle</code> where data are
	 *              copied
	 */
	public static final void nextOTri(OTriangle o, OTriangle that)
	{
		that.tri = o.tri;
		that.localNumber = next3[o.localNumber];
		that.attributes = (that.tri.adjPos >> (8*(1+that.localNumber))) & 0xff;
	}
	
	/**
	 * Move to the counterclockwaise following edge.
	 */
	public final void nextOTri()
	{
		localNumber = next3[localNumber];
		attributes = (tri.adjPos >> (8*(1+localNumber))) & 0xff;
	}
	
	/**
	 * Copy an <code>OTriangle</code> and move it to the counterclockwaise
	 * previous edge.
	 *
	 * @param o     source <code>OTriangle</code>
	 * @param that  already allocated <code>OTriangle</code> where data are
	 *              copied
	 */
	public static final void prevOTri(OTriangle o, OTriangle that)
	{
		that.tri = o.tri;
		that.localNumber = prev3[o.localNumber];
		that.attributes = (that.tri.adjPos >> (8*(1+that.localNumber))) & 0xff;
	}
	
	/**
	 * Move to the counterclockwaise previous edge.
	 */
	public final void prevOTri()
	{
		localNumber = prev3[localNumber];
		attributes = (tri.adjPos >> (8*(1+localNumber))) & 0xff;
	}
	
	/**
	 * Copy an <code>OTriangle</code> and move it to the counterclockwaise
	 * following edge which has the same origin.
	 *
	 * @param o     source <code>OTriangle</code>
	 * @param that  already allocated <code>OTriangle</code> where data are
	 *              copied
	 */
	public static final void nextOTriOrigin(OTriangle o, OTriangle that)
	{
		prevOTri(o, that);
		that.symOTri();
	}
	
	/**
	 * Move counterclockwaise to the following edge with the same origin.
	 */
	public final void nextOTriOrigin()
	{
		prevOTri();
		symOTri();
	}
	
	/**
	 * Copy an <code>OTriangle</code> and move it to the counterclockwaise
	 * previous edge which has the same origin.
	 *
	 * @param o     source <code>OTriangle</code>
	 * @param that  already allocated <code>OTriangle</code> where data are
	 *              copied
	 */
	public static final void prevOTriOrigin(OTriangle o, OTriangle that)
	{
		symOTri(o, that);
		that.nextOTri();
	}
	
	/**
	 * Move counterclockwaise to the previous edge with the same origin.
	 */
	public final void prevOTriOrigin()
	{
		symOTri();
		nextOTri();
	}
	
	/**
	 * Copy an <code>OTriangle</code> and move it to the counterclockwaise
	 * following edge which has the same destination.
	 *
	 * @param o     source <code>OTriangle</code>
	 * @param that  already allocated <code>OTriangle</code> where data are
	 *              copied
	 */
	public static final void nextOTriDest(OTriangle o, OTriangle that)
	{
		symOTri(o, that);
		that.prevOTri();
	}
	
	/**
	 * Move counterclockwaise to the following edge with the same
	 * destination.
	 */
	public final void nextOTriDest()
	{
		symOTri();
		prevOTri();
	}
	
	/**
	 * Copy an <code>OTriangle</code> and move it to the counterclockwaise
	 * previous edge which has the same destination.
	 *
	 * @param o     source <code>OTriangle</code>
	 * @param that  already allocated <code>OTriangle</code> where data are
	 *              copied
	 */
	public static final void prevOTriDest(OTriangle o, OTriangle that)
	{
		nextOTri(o, that);
		that.symOTri();
	}
	
	/**
	 * Move counterclockwaise to the previous edge with the same
	 * destination.
	 */
	public final void prevOTriDest()
	{
		nextOTri();
		symOTri();
	}
	
	/**
	 * Copy an <code>OTriangle</code> and move it to the counterclockwaise
	 * following edge which has the same apex.
	 *
	 * @param o     source <code>OTriangle</code>
	 * @param that  already allocated <code>OTriangle</code> where data are
	 *              copied
	 */
	public static final void nextOTriApex(OTriangle o, OTriangle that)
	{
		nextOTri(o, that);
		that.symOTri();
		that.nextOTri();
	}
	
	/**
	 * Move counterclockwaise to the following edge with the same apex.
	 */
	public final void nextOTriApex()
	{
		nextOTri();
		symOTri();
		nextOTri();
	}
	
	/**
	 * Copy an <code>OTriangle</code> and move it to the counterclockwaise
	 * previous edge which has the same apex.
	 *
	 * @param o     source <code>OTriangle</code>
	 * @param that  already allocated <code>OTriangle</code> where data are
	 *              copied
	 */
	public static final void prevOTriApex(OTriangle o, OTriangle that)
	{
		prevOTri(o, that);
		that.symOTri();
		that.prevOTri();
	}
	
	/**
	 * Move counterclockwaise to the previous edge with the same apex.
	 */
	public final void prevOTriApex()
	{
		prevOTri();
		symOTri();
		prevOTri();
	}
	
	/**
	 * Returns the start vertex of this edge.
	 *
	 * @return the start vertex of this edge.
	 */
	public final Vertex origin()
	{
		return tri.vertex[next3[localNumber]];
	}
	
	/**
	 * Returns the end vertex of this edge.
	 *
	 * @return the end vertex of this edge.
	 */
	public final Vertex destination()
	{
		return tri.vertex[prev3[localNumber]];
	}
	
	/**
	 * Returns the apex of this edge.
	 *
	 * @return the apex of this edge.
	 */
	public final Vertex apex()
	{
		return tri.vertex[localNumber];
	}
	
	//  The following 3 methods change the underlying triangle.
	//  So they also modify all OTriangle bound to this one.
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
	
	/**
	 * Sets adjacency relations between two triangles.
	 *
	 * @param sym  the triangle bond to this one.
	 */
	public final void glue(OTriangle sym)
	{
		assert !(hasAttributes(NONMANIFOLD) || sym.hasAttributes(NONMANIFOLD)) : this+"\n"+sym;
		tri.glue1(localNumber, sym.tri, sym.localNumber);
		sym.tri.glue1(sym.localNumber, tri, localNumber);
	}
	
	/**
	 * Sets adjacency relation for a triangle.
	 *
	 * @param link  the triangle bond to this one of this edge is manifold, or an Object otherwise.
	 */
	public final Object getAdj()
	{
		return tri.getAdj(localNumber);
	}
	
	public final void setAdj(Object link)
	{
		tri.setAdj(localNumber, link);
	}
	
	public Iterator getOTriangleAroundApexIterator()
	{
		final OTriangle ot = this;
		return new Iterator()
		{
			private Vertex first = ot.origin();
			private boolean lookAhead = false;
			private boolean init = true;
			private int state = 0;
			public boolean hasNext()
			{
				if (init)
					return true;
				if (!lookAhead)
				{
					next();
					lookAhead = true;
				}
				return !(state > 0 && ot.origin() == first);
			}
			public Object next()
			{
				if (init)
				{
					init = false;
					if (ot.origin() == Vertex.outer)
						state = 2;
					return ot;
				}
				if (lookAhead)
				{
					lookAhead = false;
					return ot;
				}
				lookAhead = false;
				if (state == 0)
					state = 1;
				if (ot.hasAttributes(OUTER) && state == 1)
				{
					// Loop clockwise to another boundary
					// and start again from there.
					state = 2;
					ot.prevOTri();
					ot.nextOTriDest();
					while (true)
					{
						if (ot.hasAttributes(OUTER))
							break;
						ot.prevOTri();
						ot.nextOTriDest();
					}
				}
				else
				{
					ot.prevOTriDest();
					ot.nextOTri();
				}
				return ot;
			}
			public void remove()
			{
			}
		};
	}
	
	public Iterator getOTriangleAroundOriginIterator()
	{
		final OTriangle ot = this;
		return new Iterator()
		{
			private Vertex first = ot.destination();
			private boolean lookAhead = false;
			private boolean init = true;
			private int state = 0;
			public boolean hasNext()
			{
				if (init)
					return true;
				if (!lookAhead)
				{
					next();
					lookAhead = true;
				}
				return !(state > 0 && ot.destination() == first);
			}
			public Object next()
			{
				if (init)
				{
					init = false;
					if (ot.destination() == Vertex.outer)
						state = 2;
					return ot;
				}
				if (lookAhead)
				{
					lookAhead = false;
					return ot;
				}
				lookAhead = false;
				if (state == 0)
					state = 1;
				if (ot.hasAttributes(OUTER) && state == 1)
				{
					// Loop clockwise to another boundary
					// and start again from there.
					state = 2;
					ot.prevOTriOrigin();
					while (true)
					{
						if (ot.hasAttributes(OUTER))
							break;
						ot.prevOTriOrigin();
					}
				}
				else
					ot.nextOTriOrigin();
				return ot;
			}
			public void remove()
			{
			}
		};
	}
	
	/**
	 * Checks whether an edge can be swapped.
	 *
	 * @return <code>false</code> if edge is a boundary or outside the mesh,
	 * <code>true</code> otherwise.
	 */
	public final boolean isMutable()
	{
		return !(hasAttributes(BOUNDARY) || hasAttributes(OUTER));
	}
	
	/**
	 * Checks whether an edge is Delaunay.
	 *
	 * As apical vertices are already computed by calling routines,
	 * they are passed as parameters for efficiency reasons.
	 *
	 * @param apex2  apex of the symmetric edge
	 * @return <code>true</code> if edge is Delaunay, <code>false</code>
	 * otherwise.
	 */
	public final boolean isDelaunay(Vertex apex2)
	{
		if (apex2.isPseudoIsotropic())
			return isDelaunay_isotropic(apex2);
		return isDelaunay_anisotropic(apex2);
	}
	
	private final boolean isDelaunay_isotropic(Vertex apex2)
	{
		assert Vertex.outer != origin();
		assert Vertex.outer != destination();
		assert Vertex.outer != apex();
		Vertex vA = origin();
		Vertex vB = destination();
		Vertex v1 = apex();
		long tp1 = vA.onLeft(vB, v1);
		long tp2 = vB.onLeft(vA, apex2);
		long tp3 = apex2.onLeft(vB, v1);
		long tp4 = v1.onLeft(vA, apex2);
		if (Math.abs(tp3) + Math.abs(tp4) < Math.abs(tp1)+Math.abs(tp2) )
			return true;
		if (tp1 > 0L && tp2 > 0L)
		{
			if (tp3 <= 0L || tp4 <= 0L)
				return true;
		}
		return !apex2.inCircleTest2(this);
	}
	
	private final boolean isDelaunay_anisotropic(Vertex apex2)
	{
		assert Vertex.outer != origin();
		assert Vertex.outer != destination();
		assert Vertex.outer != apex();
		if (apex2 == Vertex.outer)
			return true;
		return !apex2.inCircleTest3(this);
	}
	
	/**
	 * Check whether an edge can be contracted.
	 * @return <code>true</code> if this edge can be contracted, <code>flase</code> otherwise.
	 */
	public final boolean canContract(Vertex n)
	{
		if (n.mesh.getType() == Mesh.MESH_3D && !checkInversion(n))
				return false;
		
		//  Topology check
		//  TODO: normally this check could be removed, but the
		//        following test triggers an error:
		//    * mesh Scie_shell.brep with deflexion=0.2 aboslute
		//    * decimate with length=6
		ArrayList link = origin().getNeighboursNodes();
		link.retainAll(destination().getNeighboursNodes());
		return link.size() < 3;
	}
	
	/**
	 * Swaps an edge.
	 *
	 * This routine swaps an edge (od) to (na), updates
	 * adjacency relations and backward links between vertices and
	 * triangles.  Current object is transformed from (oda) to (ona)
	 * and not (nao), because this helps turning around o, e.g.
	 * at the end of {@link #split3}.
	 *
	 * @param a  apex of the current edge
	 * @param n  apex of the symmetric edge
	 * @return a handle to (ona) oriented triangle.
	 * otherwise.
	 */
	public final OTriangle swap()
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
		assert !this.hasAttributes(OUTER | BOUNDARY);
		copyOTri(this, work[0]);        // (oda)
		symOTri(this, work[1]);         // (don)
		symOTri(this, work[2]);         // (don)
		Vertex n = work[1].apex();
		//  Clear SWAPPED flag for all edges of the 2 triangles
		for (int i = 0; i < 3; i++)
		{
			work[0].clearAttributes(SWAPPED);
			work[1].clearAttributes(SWAPPED);
			work[0].nextOTri();
			work[1].nextOTri();
		}
		work[1].nextOTri();             // (ond)
		int attr3 = work[1].attributes;
		work[1].symOTri();              // a3 = (no*)
		work[1].glue(work[0]);
		work[0].attributes = attr3;
		work[0].pushAttributes();
		work[0].nextOTri();             // (dao)
		copyOTri(work[0], work[1]);     // (dao)
		int attr1 = work[1].attributes;
		work[0].symOTri();              // a1 = (ad*)
		work[2].glue(work[0]);
		work[2].attributes = attr1;
		work[2].pushAttributes();
		work[2].nextOTri();             // (ond)
		work[2].glue(work[1]);
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
		return this;  // (ona)
	}
	
	public double [] getTempVector()
	{
		return tempD;
	}
	
	private final boolean checkInversion(Vertex n)
	{
		Vertex o = origin();
		Vertex d = destination();
		nextOTri(this, work[0]);
		prevOTri(this, work[1]);
		//  If both adjacent edges are on a boundary, do not contract
		if (work[0].hasAttributes(BOUNDARY) && work[1].hasAttributes(BOUNDARY))
				return false;
		symOTri(this, work[1]);
		symOTri(this, work[0]);
		work[0].prevOTri();
		work[1].nextOTri();
		if (work[0].hasAttributes(BOUNDARY) && work[1].hasAttributes(BOUNDARY))
				return false;
		//  Loop around o to check that triangles will not be inverted
		nextOTri(this, work[0]);
		symOTri(this, work[1]);
		double [] v1 = new double[3];
		double [] xn = n.getUV();
		double [] xo = o.getUV();
		for (Iterator it = work[0].getOTriangleAroundApexIterator(); it.hasNext(); )
		{
			work[0] = (OTriangle) it.next();
			if (work[0].tri != tri && work[0].tri != work[1].tri && !work[0].hasAttributes(OUTER))
			{
				work[0].computeNormal3DT();
				double [] nu = work[0].getTempVector();
				double [] x1 = work[0].origin().getUV();
				for (int i = 0; i < 3; i++)
					v1[i] = xn[i] - x1[i];
				if (Metric3D.prodSca(v1, nu) >= 0.0)
					return false;
			}
		}
		//  Loop around d to check that triangles will not be inverted
		copyOTri(this, work[0]);
		work[0].prevOTri();
		xo = d.getUV();
		for (Iterator it = work[0].getOTriangleAroundApexIterator(); it.hasNext(); )
		{
			work[0] = (OTriangle) it.next();
			if (work[0].tri != tri && work[0].tri != work[1].tri && !work[0].hasAttributes(OUTER))
			{
				work[0].computeNormal3DT();
				double [] nu = work[0].getTempVector();
				double [] x1 = work[0].origin().getUV();
				for (int i = 0; i < 3; i++)
					v1[i] = xn[i] - x1[i];
				if (Metric3D.prodSca(v1, nu) >= 0.0)
					return false;
			}
		}
		return true;
	}
	
	// Warning: this vectore is not normalized, it has the same length as
	// this.
	public void computeNormal3DT()
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
		Metric3D.prodVect3D(tempD1, tempD, tempD2);
		double norm = Metric3D.norm(tempD2);
		if (norm != 0.0)
		{
			tempD2[0] /= norm;
			tempD2[1] /= norm;
			tempD2[2] /= norm;
		}
		Metric3D.prodVect3D(tempD1, tempD2, tempD);
	}
	
	public void computeNormal3D()
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
		Metric3D.prodVect3D(tempD1, tempD2, tempD);
		double norm = Metric3D.norm(tempD);
		if (norm != 0.0)
		{
			tempD[0] /= norm;
			tempD[1] /= norm;
			tempD[2] /= norm;
		}
	}
	
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
		Metric3D.prodVect3D(tempD1, tempD2, tempD);
		return 0.5 * Metric3D.norm(tempD);
	}
	
	/**
	 * Contract an edge.
	 * TODO: Attributes are not checked.
	 * @param n the resulting vertex
	 */
	public final void contract(Vertex n)
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
		//  NOTE: if t5 is outer, it will not be updated by this loop
		copyOTri(this, work[0]);
		for (Iterator it = work[0].getOTriangleAroundOriginIterator(); it.hasNext(); )
		{
			work[0] = (OTriangle) it.next();
			work[0].setOrigin(n);
		}
		//  Replace d by n in all incident triangles
		//  NOTE: if t4 is outer, it will not be updated by this loop
		symOTri(this, work[0]);
		for (Iterator it = work[0].getOTriangleAroundOriginIterator(); it.hasNext(); )
		{
			work[0] = (OTriangle) it.next();
			work[0].setOrigin(n);
		}
		//  Update adjacency links.  For clarity, o and d are
		//  written instead of n.
		if (!hasAttributes(OUTER))
		{
			nextOTri();             // (dV1o)
			int attr4 = attributes;
			symOTri(this, work[0]); // (V1dV4)
			//  See NOTE above
			work[0].setDestination(n);
			nextOTri();             // (V1od)
			int attr3 = attributes;
			symOTri(this, work[1]); // (oV1V3)
			work[0].glue(work[1]);
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
			nextOTri();             // (odV1)
		}
		symOTri();                      // (doV2)
		if (!hasAttributes(OUTER))
		{
			nextOTri();             // (oV2d)
			int attr5 = attributes;
			symOTri(this, work[0]); // (V2oV5)
			//  See NOTE above
			work[0].setDestination(n);
			nextOTri();             // (V2do)
			int attr6 = attributes;
			symOTri(this, work[1]); // (dV2V6)
			work[0].glue(work[1]);
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
			nextOTri();             // (doV2)
		}
		clearAttributes(MARKED);
		pushAttributes();
		symOTri();
		clearAttributes(MARKED);
		pushAttributes();
	}
	
	/**
	 * Split an edge.  This is the opposite of contract.
	 * @param n the resulting vertex
	 * @return the newly created edge
	 */
	public final void split(Vertex n)
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
		Triangle t3 = new Triangle(t1);
		Triangle t4 = new Triangle(t2);
		t3.addToMesh();
		t4.addToMesh();
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
		work[1].nextOTri();             // (dV1n)
		if (work[0].getAdj() != null)
		{
			work[0].symOTri();      // (V1d*)
			work[1].glue(work[0]);
			nextOTri(this, work[0]);// (nV1o)
		}
		work[1].nextOTri();             // (V1nd)
		work[1].glue(work[0]);
		work[0].clearAttributes(BOUNDARY);
		work[1].clearAttributes(BOUNDARY);
		work[1].nextOTri();             // (ndV1)
		
		nextOTriDest(this, work[0]);    // (V2do)
		copyOTri(work[0], work[2]);     // (V2do)
		work[0].setDestination(n);      // (V2no)
		work[2].tri = t4;
		work[2].setApex(n);             // (V2dn)
		if (work[0].getAdj() != null)
		{
			work[0].symOTri();      // (dV2*)
			work[2].glue(work[0]);
			nextOTriDest(this, work[0]);    // (V2no)
		}
		work[2].prevOTri();             // (nV2d)
		work[2].glue(work[0]);
		work[0].clearAttributes(BOUNDARY | MARKED);
		work[2].clearAttributes(BOUNDARY | MARKED);
		work[2].prevOTri();             // (dnV2)
		work[2].glue(work[1]);
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
		todo.push(new Integer(localNumber));
		swapVertices(seen, todo);
		assert o == destination() : o+" "+d+" "+this;
	}
	
	private static void swapVertices(HashSet seen, Stack todo)
	{
		OTriangle ot = new OTriangle();
		OTriangle sym = new OTriangle();
		while (todo.size() > 0)
		{
			int o = ((Integer) todo.pop()).intValue();
			Triangle t = (Triangle) todo.pop();
			if (seen.contains(t))
				continue;
			seen.add(t);
			Vertex temp = t.vertex[next3[o]];
			t.vertex[next3[o]] = t.vertex[prev3[o]];
			t.vertex[prev3[o]] = temp;
			Object a = t.getAdj(next3[o]);
			t.setAdj(next3[o], t.getAdj(prev3[o]));
			t.setAdj(prev3[o], a);
			// Swap attributes for edges
			if (o == 0)
				t.swapAttributes12();
			else if (o == 1)
				t.swapAttributes02();
			else
				t.swapAttributes01();
			// Fix adjacent triangles
			ot.bind(t);
			for (int i = 0; i < 3; i++)
			{
				ot.nextOTri();
				if (!ot.hasAttributes(BOUNDARY))
				{
					if (!ot.hasAttributes(NONMANIFOLD))
					{
						OTriangle.symOTri(ot, sym);
						todo.push(sym.tri);
						todo.push(new Integer(sym.localNumber));
						sym.tri.glue1(sym.localNumber, ot.tri, ot.localNumber);
					}
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
			r+= t.hashCode()+"["+(((tri.adjPos & (3 << (2*num))) >> (2*num)) & 3)+"]";
		return r;
	}
	
	public String toString()
	{
		String r = "Local number: "+localNumber;
		r += "\nTri hashcode: "+tri.hashCode();
		r += "\nAdjacency: "+showAdj(0)+" "+showAdj(1)+" "+showAdj(2);
		r += "\nAttributes: "+Integer.toHexString((tri.adjPos >> 8) & 0xff)+" "+Integer.toHexString((tri.adjPos >> 16) & 0xff)+" "+Integer.toHexString((tri.adjPos >> 24) & 0xff)+" => "+Integer.toHexString(attributes);
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
		m.setType(Mesh.MESH_3D);
		System.out.println("Building mesh...");
		Triangle T = new Triangle(v[0], v[1], v[2]);
		T.addToMesh();
		assert m.isValid();
		// Outer triangles
		Triangle [] O = new Triangle[3];
		O[0] = new Triangle(Vertex.outer, v[1], v[0]);
		O[1] = new Triangle(Vertex.outer, v[2], v[1]);
		O[2] = new Triangle(Vertex.outer, v[0], v[2]);
		for (int i = 0; i < 3; i++)
		{
			O[i].setOuter();
			O[i].addToMesh();
		}
		assert m.isValid();
		OTriangle ot1 = new OTriangle();
		OTriangle ot2 = new OTriangle(T, 2);
		for (int i = 0; i < 3; i++)
		{
			ot1.bind(O[i]);
			ot1.setAttributes(BOUNDARY);
			ot2.setAttributes(BOUNDARY);
			ot1.glue(ot2);
			ot2.nextOTri();
		}
		assert ot2.origin() == v[0];
// m.printMesh();
		assert m.isValid();
		ot2.prevOTri();  // (v2,v0,v1)
		ot2.split(v[3]); // (v2,v3,v1)
		assert m.isValid();
		ot2.nextOTri();  // (v3,v1,v2)
		ot2.swap();      // (v3,v0,v2)
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
		ot2.split(v[5]); // (v3,v5,v2)
		assert m.isValid();
		ot2.nextOTri();  // (v5,v2,v3)
		ot2.swap();      // (v5,v0,v3)
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
		ot2.prevOTri();  // (v3,v5,v0)
		ot2.split(v[4]); // (v3,v4,v0)
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
		OTriangle ot2 = o.findOTriangle(d);
		assert ot2 != null;
		OTriangle ot1 = new OTriangle();
		copyOTri(ot2, ot1);
		System.out.println("Loop around origin: "+o);
		System.out.println(" first destination: "+d);
		int cnt = 0;
		for (Iterator it = ot1.getOTriangleAroundOriginIterator(); it.hasNext(); )
		{
			ot1 = (OTriangle) it.next();
			cnt++;
		}
		assert cnt == 4 : "Failed test: LoopOrigin cnt != 4: "+o+" "+d;
	}
	
	private static void unitTestCheckLoopApex(Mesh m, Vertex a, Vertex o)
	{
		OTriangle ot2 = a.findOTriangle(o);
		assert ot2 != null;
		OTriangle ot1 = new OTriangle();
		nextOTri(ot2, ot1);
		System.out.println("Loop around apex: "+a);
		System.out.println(" first origin: "+o);
		int cnt = 0;
		for (Iterator it = ot1.getOTriangleAroundApexIterator(); it.hasNext(); )
		{
			ot1 = (OTriangle) it.next();
			cnt++;
		}
		assert cnt == 4 : "Failed test: LoopApex cnt != 4: "+a+" "+o;
	}
	
	private static void unitTestCheckQuality(Mesh m, Vertex o, Vertex d, int expected)
	{
		OTriangle ot2 = o.findOTriangle(d);
		assert ot2 != null;
		OTriangle ot1 = new OTriangle();
		OTriangle sym = new OTriangle();
		nextOTri(ot2, ot1);
		System.out.println("Improve triangle quality around origin: "+o);
		System.out.println(" first destination: "+d);
		int cnt = 0;
		for (Iterator it = ot1.getOTriangleAroundApexIterator(); it.hasNext(); )
		{
			OTriangle ot = (OTriangle) it.next();
			if (ot.hasAttributes(OTriangle.OUTER) || ot.hasAttributes(OTriangle.BOUNDARY) || ot.getAdj() == null)
				continue;
			OTriangle.symOTri(ot, sym);
			Vertex a = sym.apex();
			double p1 = ot.origin().distance3D(ot.destination()) + ot.destination().distance3D(ot.apex()) + ot.apex().distance3D(ot.origin());
			double s1 = ot.computeArea();
			double p2 = sym.origin().distance3D(sym.destination()) + sym.destination().distance3D(sym.apex()) + sym.apex().distance3D(sym.origin());
			double s2 = sym.computeArea();
			double Qbefore = 12.0 * Math.sqrt(3.0) * Math.min(s1/p1/p1, s2/p2/p2);
			
			double p3 = ot.origin().distance3D(sym.apex()) + sym.apex().distance3D(ot.apex()) + ot.apex().distance3D(ot.origin());
			double s3 = ot.origin().area3D(sym.apex(), ot.apex());
			double p4 = sym.origin().distance3D(ot.apex()) + ot.apex().distance3D(sym.apex()) + sym.apex().distance3D(sym.origin());
			double s4 = sym.origin().area3D(ot.apex(), sym.apex());
			double Qafter = 12.0 * Math.sqrt(3.0) * Math.min(s3/p3/p3, s4/p4/p4);
			if (Qbefore < Qafter)
			{
				// Swap edge
				ot.swap();
				cnt++;
			}
		}
		assert cnt == expected : "Failed test: QualityOrigin cnt != "+expected+": "+o+" "+d;
	}
	
	public static void main(String args[])
	{
		Mesh m = new Mesh();
		Vertex [] v = new Vertex[6];
		v[0] = new Vertex(0.0, 0.0, 0.0);
		v[1] = new Vertex(1.0, 0.0, 0.0);
		v[2] = new Vertex(1.0, 1.0, 0.0);
		v[3] = new Vertex(0.0, 1.0, 0.0);
		v[4] = new Vertex(-1.0, 1.0, 0.0);
		v[5] = new Vertex(-1.0, 0.0, 0.0);
		unitTestBuildMesh(m, v);
		assert m.isValid();
		System.out.println("Checking loops...");
		unitTestCheckLoopOrigin(m, v[3], v[4]);
		unitTestCheckLoopOrigin(m, v[3], v[2]);
		unitTestCheckLoopOrigin(m, v[3], Vertex.outer);
		unitTestCheckLoopApex(m, v[3], v[4]);
		unitTestCheckLoopApex(m, v[3], v[2]);
		unitTestCheckLoopApex(m, v[3], Vertex.outer);
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
		unitTestCheckQuality(m, v[3], Vertex.outer, 2);
		unitTestCheckQuality(m, v[3], Vertex.outer, 0);
		assert m.isValid();
	}
}
