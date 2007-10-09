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

import org.jcae.mesh.amibe.traits.HalfEdgeTraitsBuilder;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.NoSuchElementException;
import java.io.Serializable;
import org.apache.log4j.Logger;

public class HalfEdge extends AbstractHalfEdge implements Serializable
{
	private static Logger logger = Logger.getLogger(HalfEdge.class);
	private TriangleHE tri;
	private byte localNumber = 8;
	private int attributes = 8;
	// For non manifold edges, a virtual triangle is added
	//   Triangle(outerVertex, edge.origin(), edge.destination())
	// and sym points to an edge of this triangle.  It is said to
	// be outer.  The list of adjacent HalfEdge is stored in this
	// triangle, more specifically in sym.next.sym
	// This is very handy because all HalfEdge of non-outer triangles
	// can be considered as being manifold.
	private Object sym = null;
	private HalfEdge next = null;

	private static final int [] next3 = { 1, 2, 0 };
	private static final int [] prev3 = { 2, 0, 1 };
	private static final double [][] temp = new double[4][3];
	
	public HalfEdge (HalfEdgeTraitsBuilder htb, TriangleHE tri, byte localNumber, byte attributes)
	{
		super(htb);
		this.tri = tri;
		this.localNumber = localNumber;
		this.attributes = attributes;
	}
	
	public void copy(HalfEdge src)
	{
		// Do not override tri!
		localNumber = src.localNumber;
		attributes = src.attributes;
		sym = src.sym;
	}
	
	/**
	 * Return the triangle tied to this object.
	 *
	 * @return the triangle tied to this object.
	 */
	@Override
	public final Triangle getTri()
	{
		return tri;
	}
	
	/**
	 * Return the edge local number.
	 *
	 * @return the edge local number.
	 */
	@Override
	public final int getLocalNumber()
	{
		return localNumber;
	}
	
	public final int getAttributes()
	{
		return attributes;
	}
	
	/**
	 * Set the edge tied to this object.
	 *
	 * @param e  the edge tied to this object.
	 */
	@Override
	public final void glue(AbstractHalfEdge e)
	{
		HEglue((HalfEdge) e);
	}
	private final void HEglue(HalfEdge s)
	{
		sym = s;
		if (s != null)
			s.sym = this;
	}
	
	public final HalfEdge notOriented()
	{
		assert sym instanceof HalfEdge;
		if (sym != null && sym.hashCode() < hashCode())
			return (HalfEdge) sym;
		return this;
	}
	
	/**
	 * Get the symmetric edge.
	 */
	@Override
	public final Object getAdj()
	{
		return sym;
	}

	/**
	 * Set the sym link.
	 */
	@Override
	public final void setAdj(Object e)
	{
		sym = e;
	}

	private final HalfEdge HEsym()
	{
		return (HalfEdge) sym;
	}

	@Override
	public final AbstractHalfEdge sym()
	{
		return (AbstractHalfEdge) sym;
	}

	@Override
	public final AbstractHalfEdge sym(AbstractHalfEdge that)
	{
		that = (AbstractHalfEdge) sym;
		return that;
	}

	/**
	 * Move to the next edge.
	 */
	@Override
	public final AbstractHalfEdge next()
	{
		return next;
	}
	
	@Override
	public final AbstractHalfEdge next(AbstractHalfEdge that)
	{
		that = next;
		return that;
	}
	
	/**
	 * Move to the previous edge.
	 */
	@Override
	public final AbstractHalfEdge prev()
	{
		return next.next;
	}
	
	@Override
	public final AbstractHalfEdge prev(AbstractHalfEdge that)
	{
		that = next.next;
		return that;
	}
	
	/**
	 * Move counterclockwise to the following edge with the same origin.
	 */
	@Override
	public final AbstractHalfEdge nextOrigin()
	{
		return next.next.sym();
	}
	
	@Override
	public final AbstractHalfEdge nextOrigin(AbstractHalfEdge that)
	{
		that = next.next.sym();
		return that;
	}
	
	/**
	 * Move counterclockwise to the previous edge with the same origin.
	 */
	public final AbstractHalfEdge prevOrigin()
	{
		return HEsym().next;
	}
	
	public final AbstractHalfEdge prevOrigin(AbstractHalfEdge that)
	{
		that = HEsym().next;
		return that;
	}
	
	/**
	 * Move counterclockwise to the following edge with the same
	 * destination.
	 */
	public final AbstractHalfEdge nextDest()
	{
		return HEsym().prev();
	}
	
	public final AbstractHalfEdge nextDest(AbstractHalfEdge that)
	{
		that = HEsym().prev();
		return that;
	}
	
	/**
	 * Move counterclockwise to the previous edge with the same
	 * destination.
	 */
	public final AbstractHalfEdge prevDest()
	{
		return next.sym();
	}
	
	public final AbstractHalfEdge prevDest(AbstractHalfEdge that)
	{
		that = next.sym();
		return that;
	}
	
	/**
	 * Move counterclockwise to the following edge with the same apex.
	 */
	public final AbstractHalfEdge nextApex()
	{
		return next.HEsym().next;
	}
	
	public final AbstractHalfEdge nextApex(AbstractHalfEdge that)
	{
		that = next.HEsym().next;
		return that;
	}
	
	/**
	 * Move clockwise to the previous edge with the same apex.
	 */
	public final AbstractHalfEdge prevApex()
	{
		return next.next.HEsym().prev();
	}
	
	public final AbstractHalfEdge prevApex(AbstractHalfEdge that)
	{
		that = next.next.HEsym().prev();
		return that;
	}
	
	//  The following 3 methods change the underlying triangle.
	//  So they also modify all HalfEdge bound to this one.
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
	 * Set the next link.
	 */
	public final void setNext(HalfEdge e)
	{
		next = e;
	}
	
	/**
	 * Check if some attributes of this edge are set.
	 *
	 * @param attr  the attributes to check
	 * @return <code>true</code> if this HalfEdge has all these
	 * attributes set, <code>false</code> otherwise.
	 */
	@Override
	public final boolean hasAttributes(int attr)
	{
		return (attributes & attr) != 0;
	}
	
	/**
	 * Set attributes of this edge.
	 *
	 * @param attr  the attribute of this edge.
	 */
	@Override
	public final void setAttributes(int attr)
	{
		attributes |= attr;
	}
	
	/**
	 * Reset attributes of this edge.
	 *
	 * @param attr   the attributes of this edge to clear out.
	 */
	@Override
	public final void clearAttributes(int attr)
	{
		attributes &= ~attr;
	}
	
	/**
	 * Returns the start vertex of this edge.
	 *
	 * @return the start vertex of this edge.
	 */
	@Override
	public final Vertex origin()
	{
		return tri.vertex[next3[localNumber]];
	}
	
	/**
	 * Returns the end vertex of this edge.
	 *
	 * @return the end vertex of this edge.
	 */
	@Override
	public final Vertex destination()
	{
		return tri.vertex[prev3[localNumber]];
	}
	
	/**
	 * Returns the apex of this edge.
	 *
	 * @return the apex of this edge.
	 */
	@Override
	public final Vertex apex()
	{
		return tri.vertex[localNumber];
	}
	
	/**
	 * Move counterclockwise to the following edge with the same origin.
	 * If a boundary is reached, loop backward until another
	 * boundary is found and start again from there.
	 */
	@Override
	public final AbstractHalfEdge nextOriginLoop()
	{
		HalfEdge ret = this;
		if (ret.hasAttributes(OUTER) && ret.hasAttributes(BOUNDARY | NONMANIFOLD))
		{
			// Loop clockwise to another boundary
			// and start again from there.
			do
			{
				ret = (HalfEdge) ret.prevOrigin();
			}
			while (!ret.hasAttributes(OUTER));
		}
		else
			ret = (HalfEdge) ret.nextOrigin();
		return ret;
	}
	
	/**
	 * Move counterclockwise to the following edge with the same apex.
	 * If a boundary is reached, loop backward until another
	 * boundary is found and start again from there.
	 */
	public final HalfEdge nextApexLoop()
	{
		HalfEdge ret = this;
		if (ret.hasAttributes(OUTER) && ret.next.next.hasAttributes(BOUNDARY | NONMANIFOLD))
		{
			// Loop clockwise to another boundary
			// and start again from there.
			do
			{
				ret = (HalfEdge) ret.prevApex();
			}
			while (!ret.hasAttributes(OUTER));
		}
		else
			ret = (HalfEdge) ret.nextApex();
		return ret;
	}
	
	/**
	 * Checks the dihedral angle of an edge.
	 * Warning: this method uses temp[0], temp[1], temp[2] and temp[3] temporary arrays.
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
		HalfEdge f = HEsym();
		Vertex o = origin();
		Vertex d = destination();
		Vertex a = apex();
		double s1 = Matrix3D.computeNormal3D(o.getUV(), d.getUV(), a.getUV(), temp[0], temp[1], temp[2]);
		double s2 = Matrix3D.computeNormal3D(f.tri.vertex[0].getUV(), f.tri.vertex[1].getUV(), f.tri.vertex[2].getUV(), temp[0], temp[1], temp[3]);
		if (Matrix3D.prodSca(temp[2], temp[3]) < minCos)
			return invalid;
		// Check for quality improvement
		Vertex n = f.apex();
		// Check for inverted triangles
		o.outer3D(n, a, temp[0]);
		double s3 = 0.5 * Matrix3D.prodSca(temp[2], temp[0]);
		if (s3 <= 0.0)
			return invalid;
		d.outer3D(a, n, temp[0]);
		double s4 = 0.5 * Matrix3D.prodSca(temp[2], temp[0]);
		if (s4 <= 0.0)
			return invalid;
		double p1 = o.distance3D(d) + d.distance3D(a) + a.distance3D(o);
		double p2 = d.distance3D(o) + o.distance3D(n) + n.distance3D(d);
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
	@Override
	protected final AbstractHalfEdge swap()
	{
		return HEswap();
	}
	private final HalfEdge HEswap()
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
		 *       s0 / | \ s3         s0  /   \ s3
		 *         /  |  \              / T2  \
		 *      a + T1|T2 + n  --->  a +-------+ n
		 *         \  |  /              \ T1  /
		 *       s1 \ | / s2         s1  \   / s2
		 *           \|/                  \ /
		 *            '                    '
		 *            o                    o
		 */
		// T1 = (oda)  --> (ona)
		// T2 = (don)  --> (dan)
		HalfEdge [] e = new HalfEdge[6];
		e[0] = next;
		e[1] = next.next;
		e[2] = HEsym().next;
		e[3] = HEsym().next.next;
		e[4] = this;
		e[5] = HEsym();
		//  Clear SWAPPED flag for all edges of the 2 triangles
		for (int i = 0; i < 6; i++)
		{
			e[i].clearAttributes(SWAPPED);
			e[i].HEsym().clearAttributes(SWAPPED);
		}
		//  Adjust vertices
		Vertex n = e[5].apex();
		e[4].setDestination(n);           // (ona)
		e[5].setDestination(a);           // (dan)
		//  Adjust edge informations
		//    T1: e[1] is unchanged
		TriangleHE T1 = e[1].tri;
		e[1].next = e[2];
		e[2].next = e[4];
		e[4].next = e[1];
		e[2].tri = e[4].tri = T1;
		e[2].localNumber = (byte) next3[e[1].localNumber];
		e[4].localNumber = (byte) prev3[e[1].localNumber];
		//    T2: e[3] is unchanged
		TriangleHE T2 = e[3].tri;
		e[3].next = e[0];
		e[0].next = e[5];
		e[5].next = e[3];
		e[0].tri = e[5].tri = T2;
		e[0].localNumber = (byte) next3[e[3].localNumber];
		e[5].localNumber = (byte) prev3[e[3].localNumber];
		//  Adjust edge pointers of triangles
		if (e[1].localNumber == 1)
			T1.setHalfEdge(e[4]);
		else if (e[1].localNumber == 2)
			T1.setHalfEdge(e[2]);
		if (e[3].localNumber == 1)
			T2.setHalfEdge(e[5]);
		else if (e[3].localNumber == 2)
			T2.setHalfEdge(e[0]);
		//  Mark new edges
		e[4].attributes = 0;
		e[5].attributes = 0;
		e[4].setAttributes(SWAPPED);
		e[5].setAttributes(SWAPPED);
		//  Fix links to triangles
		o.setLink(T1);
		d.setLink(T2);
		// Be consistent with AbstractHalfEdge.swap()
		return e[2];
	}
	
	/**
	 * Return the area of this triangle.
	 * @return the area of this triangle.
	 * Warning: this method uses temp[0], temp[1] and temp[2] temporary arrays.
	 */
	@Override
	public double area()
	{
		double [] p0 = origin().getUV();
		double [] p1 = destination().getUV();
		double [] p2 = apex().getUV();
		temp[1][0] = p1[0] - p0[0];
		temp[1][1] = p1[1] - p0[1];
		temp[1][2] = p1[2] - p0[2];
		temp[2][0] = p2[0] - p0[0];
		temp[2][1] = p2[1] - p0[1];
		temp[2][2] = p2[2] - p0[2];
		Matrix3D.prodVect3D(temp[1], temp[2], temp[0]);
		return 0.5 * Matrix3D.norm(temp[0]);
	}
	
	/**
	 * Check whether an edge can be contracted into a given vertex.
	 *
	 * @param n the resulting vertex
	 * @return <code>true</code> if this edge can be contracted into the single vertex n, <code>false</code> otherwise.
	 * @see Mesh#canCollapseEdge
	 */
	@Override
	protected final boolean canCollapse(AbstractVertex n)
	{
		// Be consistent with collapse()
		if (hasAttributes(OUTER))
			return false;
		double [] xn = ((Vertex) n).getUV();
		if ((origin().getLink() instanceof Triangle) && (destination().getLink() instanceof Triangle))
		{
			// Mesh is locally manifold.  This is the most common
			// case, do not create an HashSet to store only two
			// triangles.
			Triangle t1 = tri;
			Triangle t2 = HEsym().tri;
			// Check that origin vertex can be moved
			if (!checkNewRingNormalsSameFan(xn, t1, t2))
				return false;
			// Check that destination vertex can be moved
			if (!HEsym().checkNewRingNormalsSameFan(xn, t1, t2))
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
			HalfEdge f = (HalfEdge) it.next();
			ignored.add(f.tri);
			ignored.add(f.HEsym().tri);
		}
		
		// Check that origin vertex can be moved
		if (!checkNewRingNormalsNonManifoldVertex(xn, ignored))
			return false;
		// Check that destination vertex can be moved
		if (!HEsym().checkNewRingNormalsNonManifoldVertex(xn, ignored))
			return false;
		ignored.clear();

		//  Topology check.
		//  See in AbstractHalfEdgeTest.buildMeshTopo() why this
		//  check is needed.
		//  When edge is non manifold, we do not use Vertex.getNeighboursNodes()
		//  because checks have to be performed by fans.
		for (Iterator<AbstractHalfEdge> it = fanIterator(); it.hasNext(); )
		{
			HalfEdge f = (HalfEdge) it.next();
			if (!f.canCollapseTopology())
				return false;
		}
		return true;
	}

	/**
	 * Topology check.
	 * See in AbstractHalfEdgeTest.buildMeshTopo() why this
	 * check is needed.
	 */
	private final boolean canCollapseTopology()
	{
		Collection<Vertex> neighbours = new HashSet<Vertex>();
		AbstractHalfEdge ot = this;
		Vertex d = ot.destination();
		do
		{
			// Warning: mesh.outerVertex is intentionnally not filtered out
			neighbours.add(ot.destination());
			ot = ot.nextOriginLoop();
		}
		while (ot.destination() != d);
		ot = ot.sym();
		int cnt = 0;
		d = ot.destination();
		do
		{
			// Warning: mesh.outerVertex is intentionnally not filtered out
			if (neighbours.contains(ot.destination()))
			{
				if (cnt > 1)
					return false;
				cnt++;
			}
			ot = ot.nextOriginLoop();
		}
		while (ot.destination() != d);
		return true;
	}
	
	/**
	 * Checks that triangles are not inverted if origin vertex is moved.
	 *
	 * @param newpt  the new position to be checked.
	 * @return <code>false</code> if the new position produces
	 *    an inverted triangle, <code>true</code> otherwise.
	 * Warning: this method uses temp[0], temp[1], temp[2] and temp[3] temporary arrays.
	 */
	@Override
	public final boolean checkNewRingNormals(double [] newpt)
	{
		Vertex o = origin();
		if (o.getLink() instanceof Triangle)
			return checkNewRingNormalsSameFan(newpt, null, null);
		for (Triangle start: (Triangle []) o.getLink())
		{
			HalfEdge f = (HalfEdge) start.getAbstractHalfEdge();
			if (f.destination() == o)
				f = (HalfEdge) f.next();
			else if (f.apex() == o)
				f = (HalfEdge) f.prev();
			assert f.origin() == o;
			if (!f.checkNewRingNormalsSameFan(newpt, null, null))
				return false;
		}
		return true;
	}

	private final boolean checkNewRingNormalsSameFan(double [] newpt, Triangle t1, Triangle t2)
	{
		// Loop around origin
		HalfEdge f = this;
		Vertex d = f.destination();
		double [] xo = origin().getUV();
		do
		{
			if (f.tri != t1 && f.tri != t2 && !f.hasAttributes(OUTER))
			{
				double [] x1 = f.destination().getUV();
				double area  = Matrix3D.computeNormal3DT(x1, f.apex().getUV(), xo, temp[0], temp[1], temp[2]);
				for (int i = 0; i < 3; i++)
					temp[3][i] = newpt[i] - x1[i];
				// Two triangles are removed when an edge is contracted.
				// So normally triangle areas should increase.  If they
				// decrease significantly, there may be a problem.
				if (Matrix3D.prodSca(temp[3], temp[2]) >= - area)
					return false;
			}
			f = (HalfEdge) f.nextOriginLoop();
		}
		while (f.destination() != d);
		return true;
	}

	private final boolean checkNewRingNormalsNonManifoldVertex(double [] newpt, Collection<Triangle> ignored)
	{
		Vertex o = origin();
		if (o.getLink() instanceof Triangle)
			return checkNewRingNormalsSameFanNonManifoldVertex(newpt, ignored);
		for (Triangle start: (Triangle []) o.getLink())
		{
			HalfEdge f = (HalfEdge) start.getAbstractHalfEdge();
			if (f.destination() == o)
				f = (HalfEdge) f.next();
			else if (f.apex() == o)
				f = (HalfEdge) f.prev();
			assert f.origin() == o;
			if (!f.checkNewRingNormalsSameFanNonManifoldVertex(newpt, ignored))
				return false;
		}
		return true;
	}
	private final boolean checkNewRingNormalsSameFanNonManifoldVertex(double [] newpt, Collection<Triangle> ignored)
	{
		// Loop around origin
		HalfEdge f = this;
		Vertex d = f.destination();
		double [] xo = origin().getUV();
		do
		{
			if (!ignored.contains(f.tri) && !f.hasAttributes(OUTER))
			{
				double [] x1 = f.destination().getUV();
				double area  = Matrix3D.computeNormal3DT(x1, f.apex().getUV(), xo, temp[0], temp[1], temp[2]);
				for (int i = 0; i < 3; i++)
					temp[3][i] = newpt[i] - x1[i];
				// Two triangles are removed when an edge is contracted.
				// So normally triangle areas should increase.  If they
				// decrease significantly, there may be a problem.
				if (Matrix3D.prodSca(temp[3], temp[2]) >= - area)
					return false;
			}
			f = (HalfEdge) f.nextOriginLoop();
		}
		while (f.destination() != d);
		return true;
	}
	
	/**
	 * Contract an edge.
	 *
	 * @param m mesh
	 * @param n the resulting vertex
	 * @return edge starting from <code>n</code> and pointing to original apex
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
		assert o.isWritable() && d.isWritable(): "Cannot contract "+this;
		if (logger.isDebugEnabled())
			logger.debug("contract ("+o+" "+d+")");
		//  Replace o by n in all incident triangles
		if (o.getLink() instanceof Triangle)
			replaceEndpointsSameFan(v);
		else
			replaceEndpointsNonManifold(o, v);
		//  Replace d by n in all incident triangles
		HalfEdge e = HEsym();
		if (d.getLink() instanceof Triangle)
			e.replaceEndpointsSameFan(v);
		else
			replaceEndpointsNonManifold(d, v);
		//  Set v links
		deepCopyVertexLinks(o, d, v);
		if (logger.isDebugEnabled())
			logger.debug("new point: "+v);

		if (!hasAttributes(NONMANIFOLD))
			return HEcollapseSameFan((Mesh) m, v, true);
		// Edge is non-manifold
		assert e.hasAttributes(OUTER);
		AbstractHalfEdge ret = null;
		// HEcollapseSameFan may modify LinkedHashMap structure
		// used by fanIterator(), we need a copy.
		ArrayList<AbstractHalfEdge> copy = new ArrayList<AbstractHalfEdge>();
		for (Iterator<AbstractHalfEdge> it = fanIterator(); it.hasNext(); )
			copy.add(it.next());
		for (AbstractHalfEdge ah: copy)
		{
			HalfEdge h = (HalfEdge) ah;
			assert !h.hasAttributes(OUTER);
			if (h == this)
				ret = h.HEcollapseSameFan((Mesh) m, v, false);
			else
				h.HEcollapseSameFan((Mesh) m, v, false);
		}
		assert ret != null;
		return ret;
	}

	private HalfEdge HEcollapseSameFan(Mesh m, Vertex n, boolean manifold)
	{
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
		
		//  Update adjacency links.  For clarity, o and d are
		//  written instead of n.
		HalfEdge e, f, s;
		e = this;
		Triangle t1 = e.tri;
		e = e.HEsym();
		Triangle t2 = e.tri;
		e = e.HEsym();
		e = next;               // (dV1o)
		int attr4 = e.attributes;
		s = e.HEsym();          // (V1dV4)
		e = e.next;             // (V1od)
		int attr3 = e.attributes;
		f = e.HEsym();          // (oV1V3)
		if (f != null && f.hasAttributes(NONMANIFOLD))
		{
			// e is listed in adjacency list and
			// has to be replaced by s
			e.replaceEdgeLinks(s);
			f.HEglue(s);
		}
		else if (s != null && s.hasAttributes(NONMANIFOLD))
		{
			// s.HEsym() is listed in adjacency list and
			// has to be replaced by f
			s.HEsym().replaceEdgeLinks(f);
			s.HEglue(f);
		}
		else if (f != null)
			f.HEglue(s);
		else if (s != null)
			s.HEglue(null);
		if (f != null)
			f.attributes |= attr4;
		if (s != null)
			s.attributes |= attr3;
		if (!hasAttributes(OUTER))
		{
			TriangleHE t34 = f.tri;
			if (t34.isOuter())
				t34 = s.tri;
			assert !t34.isOuter() : s+"\n"+f;
			replaceVertexLinks(f.destination(), t1, t2, t34);
			replaceVertexLinks(n, t1, t2, t34);
		}
		e = e.next;             // (odV1)
		e = e.HEsym();          // (doV2)
		if (manifold)
		{
			e = e.next;     // (oV2d)
			int attr5 = e.attributes;
			s = e.HEsym();  // (V2oV5)
			e = e.next;     // (V2do)
			int attr6 = e.attributes;
			f = e.HEsym();  // (dV2V6)
			if (f != null && f.hasAttributes(NONMANIFOLD))
			{
				// e is listed in adjacency list and
				// has to be replaced by s
				e.replaceEdgeLinks(s);
				f.HEglue(s);
			}
			else if (s != null && s.hasAttributes(NONMANIFOLD))
			{
				// s.HEsym() is listed in adjacency list and
				// has to be replaced by f
				s.HEsym().replaceEdgeLinks(f);
				s.HEglue(f);
			}
			else if (f != null)
				f.HEglue(s);
			else if (s != null)
				s.HEglue(null);
			if (f != null)
				f.attributes |= attr5;
			if (s != null)
				s.attributes |= attr6;
			if (!e.hasAttributes(OUTER))
			{
				TriangleHE t56 = s.tri;
				if (t56.isOuter())
					t56 = f.tri;
				assert !t56.isOuter();
				replaceVertexLinks(s.origin(), t1, t2, t56);
				replaceVertexLinks(n, t1, t2, t56);
			}
			e = e.next;     // (doV2)
		}
		else
		{
			assert e.hasAttributes(OUTER);
		}
		// Must be called before T2 is removed
		s = e.HEsym();                 // (odV1)
		// Remove T2
		m.remove(e.tri);
		// Must be called before T1 is removed
		e = s.next.HEsym().HEsym();    // (oV1V3)
		// Remove T1
		m.remove(s.tri);

		// Check that all o and d instances have been removed
		// This is costful, it is disabled by default but may
		// be enabled when debugging.
		/*
		boolean checkVertices = true;
		if (checkVertices)
		{
			for (AbstractTriangle at: m.getTriangles())
			{
				Triangle t = (Triangle) at;
				assert t.vertex[0] != o && t.vertex[1] != o && t.vertex[2] != o : "Vertex "+o+" found in "+t;
				assert t.vertex[0] != d && t.vertex[1] != d && t.vertex[2] != d : "Vertex "+d+" found in "+t;
			}
		}
		*/
		// By convention, edge is moved into (dV4V1), but this may change.
		// This is why V1 cannot be m.outerVertex, otherwise we cannot
		// ensure that return HalfEdge is (oV1V3)
		return e;
	}
	
	private void replaceEndpointsSameFan(Vertex n)
	{
		HalfEdge e = this;
		Vertex d = destination();
		do
		{
			e.setOrigin(n);
			e = (HalfEdge) e.nextOriginLoop();
		}
		while (e.destination() != d);
	}
	private static final void replaceEndpointsNonManifold(Vertex o, Vertex n)
	{
		Triangle [] oList = (Triangle []) o.getLink();
		for (Triangle t: oList)
		{
			TriangleHE tHE = (TriangleHE) t;
			HalfEdge f = tHE.getHalfEdge();
			if (f.origin() != o)
				f = (HalfEdge) f.next();
			if (f.origin() != o)
				f = (HalfEdge) f.next();
			assert f.origin() == o : ""+o+" not in "+f;
			f.replaceEndpointsSameFan(n);
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
			for (Triangle t: nList)
			{
				if (allTriangles.contains(t))
					continue;
				allTriangles.add(t);
				res.add(t);
				AbstractHalfEdge h = t.getAbstractHalfEdge();
				if (h.origin() != o)
					h = h.next();
				if (h.origin() != o)
					h = h.next();
				if (h.origin() == o)
				{
					// Add all triangles of the same fan to allTriangles
					AbstractHalfEdge both = null;
					Vertex end = h.destination();
					do
					{
						h = h.nextOriginLoop();
						allTriangles.add(h.getTri());
						if (h.destination() == d)
							both = h;
					}
					while (h.destination() != end);
					if (both != null)
					{
						both = both.next();
						end = both.destination();
						do
						{
							both = both.nextOriginLoop();
							allTriangles.add(both.getTri());
						}
						while (both.destination() != end);
					}
				}
				if (h.origin() != d)
					h = h.next();
				if (h.origin() != d)
					h = h.next();
				if (h.origin() == d)
				{
					// Add all triangles of the same fan to allTriangles
					AbstractHalfEdge both = null;
					Vertex end = h.destination();
					do
					{
						h = h.nextOriginLoop();
						allTriangles.add(h.getTri());
						if (h.destination() == o)
							both = h;
					}
					while (h.destination() != end);
					if (both != null)
					{
						both = both.next();
						end = both.destination();
						do
						{
							both = both.nextOriginLoop();
							allTriangles.add(both.getTri());
						}
						while (both.destination() != end);
					}
				}
			}
			v.setLink(new Triangle[res.size()]);
			res.toArray((Triangle[]) v.getLink());
		}
	}
	private void replaceEdgeLinks(HalfEdge that)
	{
		// Current instance is a non-manifold edge which has been
		// replaced by 'that'.  Replace all occurrences in adjacency
		// list.
		assert hasAttributes(NONMANIFOLD) && !hasAttributes(OUTER);
		HalfEdge e = this;
		final LinkedHashMap<Triangle, Integer> list = (LinkedHashMap<Triangle, Integer>) e.HEsym().next.sym;
		Integer I = list.get(tri);
		assert I != null && I.intValue() == localNumber;
		list.remove(tri);
		list.put(that.tri, int3[that.localNumber]);
	}
	
	/**
	 * Split an edge.  This is the opposite of {@link #collapse}.
	 *
	 * @param m mesh
	 * @param n the resulting vertex
	 * @see Mesh#vertexSplit
	 */
	@Override
	protected final AbstractHalfEdge split(AbstractMesh m, AbstractVertex n)
	{
		if (logger.isDebugEnabled())
			logger.debug("split edge ("+origin()+" "+destination()+") by adding vertex "+n);
		Vertex v = (Vertex) n;
		if (!hasAttributes(NONMANIFOLD))
		{
			v.setLink(tri);
			HEsplitSameFan((Mesh) m, v);
			return this;
		}
		// Set vertex links
		ArrayList<Triangle> link = new ArrayList<Triangle>();
		for (Iterator<AbstractHalfEdge> it = fanIterator(); it.hasNext(); )
		{
			HalfEdge f = (HalfEdge) it.next();
			link.add(f.tri);
		}
		v.setLink(new Triangle[link.size()]);
		link.toArray((Triangle[]) v.getLink());
		link.clear();
		for (Iterator<AbstractHalfEdge> it = fanIterator(); it.hasNext(); )
		{
			HalfEdge f = (HalfEdge) it.next();
			f.HEsplitSameFan((Mesh) m, v);
		}
		return this;
	}
	private final void HEsplitSameFan(Mesh m, Vertex n)
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
		 *      \             /                \    n2|   h2 /
		 *        \      h2 /                    \    |    /
		 *          \     /                        \  |  /
		 *            \,/                            \|/
		 *            V2                             V2
		 */
		splitVertexAddOneTriangle(m, n);
		HEsym().splitVertexAddOneTriangle(m, n);
		
		Triangle t1 = tri;
		// t1 is still glued to t2, it has to be glued to t4, and t3 to t2.
		HalfEdge f = next;              // (nV1o)
		f = (HalfEdge) f.prevOrigin();  // (ndV1)
		Triangle t3 = f.tri;

		HalfEdge g = HEsym();           // (dnV2)
		f.HEglue(g);
		Triangle t2 = g.tri;
		g = (HalfEdge) g.prevDest();    // (V2no)
		g = (HalfEdge) g.next();        // (noV2)
		HEglue(g);
		Triangle t4 = g.tri;
		if (t2.isOuter())
		{
			// Remove links between t2 and t4
			g = (HalfEdge) g.prev();// (V2no)
			f = g.HEsym();         ;// (nV2d)
			f.HEglue(null);
			g.HEglue(null);
			// Move f so that d == f.destination()
			f = (HalfEdge) f.next();// (V2dn)
		}

		Triangle t14 = (t1.isOuter() ? t4 : t1);
		Triangle t23 = (t2.isOuter() ? t3 : t2);
		//  Update vertex links
		replaceVertexLinks(n, t1, t2, t14);
		replaceVertexLinks(f.destination(), t1, t2, t23);
		replaceVertexLinks(origin(), t1, t2, t14);
	}
	
	private final void splitVertexAddOneTriangle(Mesh m, Vertex n)
	{
		/*
		 *            V1                             V1
		 *            /'\                            /|\
		 *          /     \                        /  |  \
		 *        /      h1 \                    /  n1| h1 \
		 *      /             \                /      |      \
		 *    /       t1        \            /   t1   |  t3    \
		 * o +-------------------+ d ---> o +---------+---------+ d
		 */
		HalfEdge h1 = next;             // (dV1o)
		TriangleHE t1 = tri;
		TriangleHE t3 = (TriangleHE) m.createTriangle(t1);
		m.add(t3);
		
		// (dV1) is not modified by this operation, so we move
		// h1 into t3 so that it does not need to be updated by
		// the caller.
		HalfEdge n1 = t3.getHalfEdge();
		if (h1.localNumber == 1)
			n1 = n1.next;
		else if (h1.localNumber == 2)
			n1 = n1.next.next;
		// Update forward links
		HalfEdge h1next = h1.next;
		h1.next = n1.next;
		h1.next.next.next = h1;
		n1.next = h1next;
		n1.next.next.next = n1;
		if (t1.getHalfEdge() == h1)
		{
			t1.setHalfEdge(n1);
			t3.setHalfEdge(h1);
		}
		// Update Triangle links
		n1.tri = t1;
		h1.tri = t3;

		// Update vertices
		n1.setOrigin(n);
		h1.setApex(n);

		// If h1 is non-manifold, update adjacency list
		if (h1.hasAttributes(NONMANIFOLD))
			n1.replaceEdgeLinks(h1);

		// Inner edge
		h1.next.HEglue(n1);

		// Clear BOUNDARY and NONMANIFOLD flags on inner edges
		h1.next.clearAttributes(BOUNDARY | NONMANIFOLD);
		n1.clearAttributes(BOUNDARY | NONMANIFOLD);
	}
	
	private final Iterator<AbstractHalfEdge> identityFanIterator()
	{
		final HalfEdge current = this;
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
	
	@Override
	public final Iterator<AbstractHalfEdge> fanIterator()
	{
		if (!hasAttributes(NONMANIFOLD))
			return identityFanIterator();
		HalfEdge e = this;
		logger.debug("Non manifold fan iterator");
		if (!e.hasAttributes(OUTER))
			e = e.HEsym();
		final LinkedHashMap<Triangle, Integer> list = (LinkedHashMap<Triangle, Integer>) e.next.sym;
		return new Iterator<AbstractHalfEdge>()
		{
			private Iterator<Map.Entry<Triangle, Integer>> it = list.entrySet().iterator();
			public boolean hasNext()
			{
				return it.hasNext();
			}
			public AbstractHalfEdge next()
			{
				Map.Entry<Triangle, Integer> entry = it.next();
				HalfEdge f = (HalfEdge) entry.getKey().getAbstractHalfEdge();
				int l = entry.getValue().intValue();
				if (l == 1)
					f = (HalfEdge) f.next;
				else if (l == 2)
					f = (HalfEdge) f.next.next;
				return f;
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
		r.append("\nTriangle: "+tri.hashCode());
		r.append("\nGroup: "+tri.getGroupId());
		r.append("\nLocal number: "+localNumber);
		if (sym != null)
		{
			if (sym instanceof HalfEdge)
			{
				HalfEdge e = (HalfEdge) sym;
				r.append("\nSym: "+e.tri.hashCode()+"["+e.localNumber+"]");
			}
			else
			{
				LinkedHashMap<Triangle, Integer> list = (LinkedHashMap<Triangle, Integer>) sym;
				r.append("\nSym: (");
				for (Map.Entry<Triangle, Integer> entry: list.entrySet())
					r.append(entry.getKey().hashCode()+"["+entry.getValue().intValue()+"],");
				r.setCharAt(r.length()-1, ')');
			}
		}
		r.append("\nAttributes: "+Integer.toHexString(attributes));
		r.append("\nVertices:");
		r.append("\n  Origin: "+origin());
		r.append("\n  Destination: "+destination());
		r.append("\n  Apex: "+apex());
		return r.toString();
	}

}
