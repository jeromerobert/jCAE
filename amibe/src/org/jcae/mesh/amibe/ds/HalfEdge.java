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
import java.util.Set;
import java.util.NoSuchElementException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Half-edge data structure.  This is a straightforward implementation of
 * {@link AbstractHalfEdge}, an half-edge is represented by a local number
 * (between 0 and 2) and a triangle.  It has a link to the next edge in the
 * same triangle, and to its symmetric edge.
 */
public class HalfEdge extends AbstractHalfEdge implements Serializable
{
	private static Logger logger=Logger.getLogger(HalfEdge.class.getName());
	private TriangleHE tri;
	private byte localNumber;
	private byte attributes;
	HalfEdge sym;
	HalfEdge next;

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
	
	public final int getAttributes()
	{
		return attributes;
	}
	
	/**
	 * Sets the edge tied to this object.
	 *
	 * @param e  the edge tied to this object
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
		if (sym != null && sym.hashCode() < hashCode())
			return sym;
		return this;
	}
	
	/**
	 * Tells whether edge is connected to a symmetric edge.
	 *
	 * @return <code>true</code> if edge has a symmetric edge, <code>false</code> otherwise.
	 */
	@Override
	public final boolean hasSymmetricEdge()
	{
		return sym != null;
	}

	/**
	 * Moves to symmetric edge.
	 * @return  current instance after its transformation
	 */
	@Override
	public final HalfEdge sym()
	{
		return sym;
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
		that = sym;
		return that;
	}

	/**
	 * Moves counterclockwise to following edge.
	 * @return  current instance after its transformation
	 */
	@Override
	public final HalfEdge next()
	{
		return next;
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
		that = next;
		return that;
	}
	
	/**
	 * Moves counterclockwise to previous edge.
	 * @return  current instance after its transformation
	 */
	@Override
	public final HalfEdge prev()
	{
		return next.next;
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
		that = next.next;
		return that;
	}
	
	/**
	 * Moves counterclockwise to the following edge which has the same origin.
	 * @return  current instance after its transformation
	 */
	@Override
	public final HalfEdge nextOrigin()
	{
		return next.next.sym;
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
		that = next.next.sym;
		return that;
	}
	
	/**
	 * Moves counterclockwise to the following edge which has the same apex.
	 * @return  current instance after its transformation
	 */
	private final HalfEdge nextApex()
	{
		return next.sym.next;
	}
	
	/**
	 * Moves counterclockwise to the previous edge which has the same apex.
	 * @return  current instance after its transformation
	 */
	private final HalfEdge prevApex()
	{
		return next.next.sym.prev();
	}
	
	//  The following 3 methods change the underlying triangle.
	//  So they also modify all HalfEdge bound to this one.
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
	
	/**
	 * Sets next link.
	 */
	public final void setNext(HalfEdge e)
	{
		next = e;
	}
	
	/**
	 * Checks if some attributes of this edge are set.
	 *
	 * @param attr  attributes to check
	 * @return <code>true</code> if this HalfEdge has one of
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
	}
	
	/**
	 * Resets attributes of this edge.
	 *
	 * @param attr   attributes of this edge to clear out
	 */
	@Override
	public final void clearAttributes(int attr)
	{
		attributes &= ~attr;
	}
	
	/**
	 * Returns start vertex of this edge.
	 *
	 * @return start vertex of this edge
	 */
	@Override
	public final Vertex origin()
	{
		return tri.vertex[next3[localNumber]];
	}
	
	/**
	 * Returns end vertex of this edge.
	 *
	 * @return end vertex of this edge
	 */
	@Override
	public final Vertex destination()
	{
		return tri.vertex[prev3[localNumber]];
	}
	
	/**
	 * Returns apex of this edge.
	 *
	 * @return apex of this edge
	 */
	@Override
	public final Vertex apex()
	{
		return tri.vertex[localNumber];
	}
	
	/**
	 * Moves counterclockwise to the following edge which has the same origin.
	 * If a boundary is reached, loop backward until another
	 * boundary is found and start again from there.
	 */
	@Override
	public final HalfEdge nextOriginLoop()
	{
		HalfEdge ret = this;
		if (ret.hasAttributes(OUTER) && ret.hasAttributes(BOUNDARY | NONMANIFOLD))
		{
			// Loop clockwise to another boundary
			// and start again from there.
			do
			{
				ret = ret.sym.next;
			}
			while (!ret.hasAttributes(OUTER));
		}
		else
			ret = ret.nextOrigin();
		return ret;
	}
	
	/**
	 * Moves counterclockwise to the following edge which has the same apex.
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
				ret = ret.prevApex();
			}
			while (!ret.hasAttributes(OUTER));
		}
		else
			ret = ret.nextApex();
		return ret;
	}
	
	/**
	 * Checks the dihedral angle of an edge.
	 * Warning: this method uses temp[0], temp[1], temp[2] and temp[3] temporary arrays.
	 *
	 * @param minCos  if the dot product of the normals to adjacent
	 *    triangles is lower than minCos, then <code>-1.0</code> is
	 *    returned.
	 * @return the minimum quality of the two triangles generated
	 *    by swapping this edge.
	 */
	public final double checkSwap3D(double minCos)
	{
		double invalid = -1.0;
		// Check if there is an adjacent edge
		if (hasAttributes(OUTER | BOUNDARY | NONMANIFOLD))
			return invalid;
		// Check for coplanarity
		HalfEdge f = sym;
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
		o.outer3D(n, a, temp[1], temp[3], temp[0]);
		double s3 = 0.5 * Matrix3D.prodSca(temp[2], temp[0]);
		if (s3 <= 0.0)
			return invalid;
		d.outer3D(a, n, temp[1], temp[3], temp[0]);
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
	 * @return swapped edge, origin and apical vertices are the same as in original edge
	 * @throws IllegalArgumentException if edge is on a boundary or belongs
	 * to an outer triangle.
	 * @see Mesh#edgeSwap
	 */
	@Override
	protected final HalfEdge swap()
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
		e[2] = sym.next;
		e[3] = sym.next.next;
		e[4] = this;
		e[5] = sym;
		//  Clear SWAPPED flag for all edges of the 2 triangles
		for (int i = 0; i < 6; i++)
		{
			e[i].clearAttributes(SWAPPED);
			e[i].sym.clearAttributes(SWAPPED);
		}
		//  Adjust vertices
		Vertex n = e[5].apex();
		e[4].setDestination(n);           // (ona)
		e[5].setDestination(a);           // (dan)
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
		replaceVertexLinks(o, T1, T2, T1);
		replaceVertexLinks(d, T1, T2, T2);
		// Be consistent with AbstractHalfEdge.swap()
		return e[2];
	}
	
	/**
	 * Returns the area of triangle bound to this edge.
	 *
	 * @return triangle area
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
	 * Checks whether an edge can be contracted into a given vertex.
	 *
	 * @param v the resulting vertex
	 * @return <code>true</code> if this edge can be contracted into the single vertex n, <code>false</code> otherwise
	 * @see Mesh#canCollapseEdge
	 */
	@Override
	protected final boolean canCollapse(Vertex v)
	{
		// Be consistent with collapse()
		if (hasAttributes(OUTER))
			return false;
		double [] xn = v.getUV();
		if (origin().isManifold() && destination().isManifold())
		{
			// Mesh is locally manifold.  This is the most common
			// case, do not create an HashSet to store only two
			// triangles.
			Triangle t1 = tri;
			Triangle t2 = sym.tri;
			// Check that origin vertex can be moved
			if (!checkNewRingNormalsSameFan(xn, t1, t2))
				return false;
			// Check that destination vertex can be moved
			if (!sym.checkNewRingNormalsSameFan(xn, t1, t2))
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
			ignored.add(f.sym.tri);
		}
		
		// Check that origin vertex can be moved
		if (!checkNewRingNormalsNonManifoldVertex(xn, ignored))
			return false;
		// Check that destination vertex can be moved
		if (!sym.checkNewRingNormalsNonManifoldVertex(xn, ignored))
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
	 * @param newpt  the new position to be checked
	 * @return <code>false</code> if the new position produces
	 *    an inverted triangle, <code>true</code> otherwise.
	 * Warning: this method uses temp[0], temp[1], temp[2] and temp[3] temporary arrays.
	 */
	@Override
	public final boolean checkNewRingNormals(double [] newpt)
	{
		Vertex o = origin();
		if (o.isManifold())
			return checkNewRingNormalsSameFan(newpt, null, null);
		for (Triangle start: (Triangle []) o.getLink())
		{
			HalfEdge f = (HalfEdge) start.getAbstractHalfEdge();
			if (f.destination() == o)
				f = f.next;
			else if (f.apex() == o)
				f = f.next.next;
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
			f = f.nextOriginLoop();
		}
		while (f.destination() != d);
		return true;
	}

	private final boolean checkNewRingNormalsNonManifoldVertex(double [] newpt, Collection<Triangle> ignored)
	{
		Vertex o = origin();
		if (o.isManifold())
			return checkNewRingNormalsSameFanNonManifoldVertex(newpt, ignored);
		for (Triangle start: (Triangle []) o.getLink())
		{
			HalfEdge f = (HalfEdge) start.getAbstractHalfEdge();
			if (f.destination() == o)
				f = f.next;
			else if (f.apex() == o)
				f = f.next.next;
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
			f = f.nextOriginLoop();
		}
		while (f.destination() != d);
		return true;
	}
	
	/**
	 * Contracts an edge.
	 *
	 * @param m mesh
	 * @param v the resulting vertex
	 * @return edge starting from <code>n</code> and with the same apex
	 * @throws IllegalArgumentException if edge belongs to an outer triangle,
	 * because there would be no valid return value.  User must then run this
	 * method against symmetric edge, this is not done automatically.
	 * @see Mesh#edgeCollapse
	 */
	@Override
	protected final HalfEdge collapse(Mesh m, Vertex v)
	{
		if (hasAttributes(OUTER))
			throw new IllegalArgumentException("Cannot contract "+this);
		Vertex o = origin();
		Vertex d = destination();
		assert o.isWritable() && d.isWritable(): "Cannot contract "+this;
		if (logger.isLoggable(Level.FINE))
			logger.fine("contract ("+o+" "+d+")");
		if (o.isManifold())
			replaceEndpointsSameFan(v);
		else
			replaceEndpointsNonManifold(o, v);
		HalfEdge e = sym;
		if (d.isManifold())
			e.replaceEndpointsSameFan(v);
		else
			replaceEndpointsNonManifold(d, v);
		deepCopyVertexLinks(o, d, v);
		if (logger.isLoggable(Level.FINE))
			logger.fine("new point: "+v);
		if (m.hasNodes())
		{
			m.remove(o);
			m.remove(d);
			m.add(v);
		}
		if (!hasAttributes(NONMANIFOLD))
		{
			e.HEcollapseSameFan(m, v);
			return HEcollapseSameFan(m, v);
		}
		// Edge is non-manifold
		assert e.hasAttributes(OUTER);
		HalfEdge ret = null;
		// HEcollapseSameFan may modify internal data structure
		// used by fanIterator(), we need a copy.
		ArrayList<AbstractHalfEdge> copy = new ArrayList<AbstractHalfEdge>();
		for (Iterator<AbstractHalfEdge> it = fanIterator(); it.hasNext(); )
			copy.add(it.next());
		for (AbstractHalfEdge ah: copy)
		{
			HalfEdge h = (HalfEdge) ah;
			assert !h.hasAttributes(OUTER);
			h.sym.HEcollapseSameFan(m, v);
			if (h == this)
				ret = h.HEcollapseSameFan(m, v);
			else
				h.HEcollapseSameFan(m, v);
		}
		assert ret != null;
		return ret;
	}

	private HalfEdge HEcollapseSameFan(Mesh m, Vertex n)
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
		HalfEdge e, f, s;
		e = next;               // (dV1o)
		int attr4 = e.attributes;
		s = e.sym;              // (V1dV4)
		e = e.next;             // (V1od)
		int attr3 = e.attributes;
		f = e.sym;              // (oV1V3)
		Triangle t34 = (f == null ? ( s == null ? null : s.tri ) : f.tri);
		if (t34 != null)
		{
			if (t34.hasAttributes(OUTER) && s != null)
				t34 = s.tri;
			replaceVertexLinks(apex(), tri, t34);
			replaceVertexLinks(n, tri, t34);
		}
		if (f != null && f.hasAttributes(NONMANIFOLD))
			f.HEglue(s);
		else if (s != null && s.hasAttributes(NONMANIFOLD))
			s.HEglue(f);
		else if (f != null)
			f.HEglue(s);
		else if (s != null)
			s.sym = null;

		if (f != null)
			f.attributes |= attr4;
		if (s != null)
			s.attributes |= attr3;
		// Remove t1
		m.remove(tri);
		// By convention, edge is moved into (dV4V1)
		// If s is null, edge is outer and return value does not matter
		return (s == null ? null : s.next);
	}
	
	private void replaceEndpointsSameFan(Vertex n)
	{
		HalfEdge e = this;
		Vertex d = destination();
		do
		{
			e.setOrigin(n);
			e = e.nextOriginLoop();
		}
		while (e.destination() != d);
	}
	private static final void replaceEndpointsNonManifold(Vertex o, Vertex n)
	{
		Triangle [] oList = (Triangle []) o.getLink();
		for (Triangle t: oList)
		{
			TriangleHE tHE = (TriangleHE) t;
			HalfEdge f = tHE.getAbstractHalfEdge();
			if (f.destination() == o)
				f = f.next;
			else if (f.apex() == o)
				f = f.next.next;
			assert f.origin() == o : ""+o+" not in "+f;
			f.replaceEndpointsSameFan(n);
		}
	}
	private static void replaceVertexLinks(Vertex o, Triangle oldT1, Triangle oldT2, Triangle newT)
	{
		if (o.isManifold())
			o.setLink(newT);
		else
		{
			Triangle [] tArray = (Triangle []) o.getLink();
			for (int i = 0; i < tArray.length; i++)
			{
				if (tArray[i] == oldT1 || tArray[i] == oldT2)
				{
					logger.fine("replaceVertexLinks: "+i+" "+o+" "+tArray[i]);
					tArray[i] = newT;
					logger.fine(" --> "+newT);
				}
			}
		}
	}
	private static void replaceVertexLinks(Vertex o, Triangle oldT, Triangle newT)
	{
		if (o.isManifold())
			o.setLink(newT);
		else
		{
			Triangle [] tArray = (Triangle []) o.getLink();
			for (int i = 0; i < tArray.length; i++)
			{
				if (tArray[i] == oldT)
				{
					logger.fine("replaceVertexLinks: "+i+" "+o+" "+tArray[i]);
					tArray[i] = newT;
					logger.fine(" --> "+newT);
				}
			}
		}
	}
	private static void deepCopyVertexLinks(Vertex o, Vertex d, Vertex v)
	{
		boolean ot = o.isManifold();
		boolean dt = d.isManifold();
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
				AbstractHalfEdge h = t.getAbstractHalfEdge();
				if (h.origin() != v)
					h = h.next();
				if (h.origin() != v)
					h = h.next();
				if (h.origin() == v)
				{
					// Add all triangles of the same fan to allTriangles
					AbstractHalfEdge both = null;
					Vertex end = h.destination();
					do
					{
						h = h.nextOriginLoop();
						allTriangles.add(h.getTri());
						if (h.destination() == v)
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
				boolean found = false;
				if (h.destination() == v)
				{
					h = h.next();
					found = true;
				}
				else if (h.apex() == v)
				{
					h = h.prev();
					found = true;
				}
				if (found)
				{
					// Add all triangles of the same fan to allTriangles
					AbstractHalfEdge both = null;
					Vertex end = h.destination();
					do
					{
						h = h.nextOriginLoop();
						allTriangles.add(h.getTri());
						if (h.destination() == v)
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
	
	/**
	 * Splits an edge.  This is the opposite of {@link #collapse}.
	 *
	 * @param m  mesh
	 * @param v  vertex being inserted
	 * @return edge starting from origin and pointing to <code>n</code>
	 * @see Mesh#vertexSplit
	 */
	@Override
	protected final HalfEdge split(Mesh m, Vertex v)
	{
		if (logger.isLoggable(Level.FINE))
			logger.fine("split edge ("+origin()+" "+destination()+") by adding vertex "+v);
		if (m.hasNodes())
			m.add(v);
		if (!hasAttributes(NONMANIFOLD))
		{
			v.setLink(tri);
			HalfEdge g = HEsplitSameFan(m, v);
			if (g.hasAttributes(OUTER))
			{
				// Remove links between t2 and t4
				g = g.next;             // (nV2d)
				HalfEdge f = g.sym;     // (V2no)
				f.sym = null;
				g.sym = null;
			}
			return this;
		}
		// HEsplitSameFan may modify internal data structure
		// used by fanIterator(), we need a copy.
		ArrayList<AbstractHalfEdge> copy = new ArrayList<AbstractHalfEdge>();
		// Set vertex links
		ArrayList<Triangle> link = new ArrayList<Triangle>();
		int cnt = 0;
		for (Iterator<AbstractHalfEdge> it = fanIterator(); it.hasNext(); )
		{
			HalfEdge f = (HalfEdge) it.next();
			link.add(f.tri);
			copy.add(f);
			cnt++;
		}
		v.setLink(new Triangle[cnt]);
		link.toArray((Triangle[]) v.getLink());
		link.clear();
		// Rebuild circular linked lists.
		// TODO: Use a better algorithm to avoid array allocation
		HalfEdge [] hOuter = new HalfEdge[2*cnt];
		cnt = 0;
		Vertex o = origin();
		for (AbstractHalfEdge ah: copy)
		{
			HalfEdge f = (HalfEdge) ah;
			HalfEdge g = f.HEsplitSameFan(m, v);
			if (f.origin() == o)
			{
				hOuter[2*cnt] = f.sym;
				hOuter[2*cnt+1] = g;
			}
			else
			{
				hOuter[2*cnt] = g;
				hOuter[2*cnt+1] = f.sym;
			}
			assert hOuter[2*cnt].origin() == o || hOuter[2*cnt].destination() == o;
			cnt++;
		}
		for (int j = 0; j < 2; j++)
		{
			// Initializes an empty cycle
			HalfEdge head = hOuter[j].next;
			head.HEglue(head.next);
			for (int i = 1; i < cnt; i++)
			{
				// Adds hOuter[2*i+j] to current cycle
				HalfEdge oldSym = head.sym;
				head.HEglue(hOuter[2*i+j].next.next);
				hOuter[2*i+j].next.HEglue(oldSym);
			}
		}
		return this;
	}

	private final HalfEdge HEsplitSameFan(Mesh m, Vertex n)
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
		sym.splitVertexAddOneTriangle(m, n);
		
		// Now we must update links:
		// 1. Link together t1/t4 and t2/t3.
		Triangle t1 = tri;
		HalfEdge f = next;              // (nV1o)
		f = f.sym.next;                 // (ndV1)
		Triangle t3 = f.tri;

		HalfEdge g = sym;               // (dnV2)
		f.HEglue(g);
		Triangle t2 = g.tri;
		f = g.next.sym.next;            // (noV2)
		HEglue(f);
		Triangle t4 = f.tri;

		Triangle t14 = (t1.hasAttributes(OUTER) ? t4 : t1);
		Triangle t23 = (t2.hasAttributes(OUTER) ? t3 : t2);
		//  Update vertex links
		replaceVertexLinks(n, t1, t2, t14);
		replaceVertexLinks(g.origin(), t1, t2, t23);
		replaceVertexLinks(origin(), t1, t2, t14);
		return g;
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
		HalfEdge n1 = t3.getAbstractHalfEdge();
		boolean updateRefHalfEdge = false;
		if (h1.localNumber == 1)
			n1 = n1.next;
		else if (h1.localNumber == 2)
			n1 = n1.next.next;
		else
			updateRefHalfEdge = true;
		assert h1.localNumber == n1.localNumber : "Wrong local numbers: "+n1+"\n"+h1;
		// Update forward links
		HalfEdge h1next = h1.next;
		h1.next = n1.next;
		h1.next.next.next = h1;
		n1.next = h1next;
		n1.next.next.next = n1;
		if (updateRefHalfEdge)
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
		logger.fine("Non manifold fan iterator");
		return new Iterator<AbstractHalfEdge>()
		{
			private HalfEdge last = sym.next.next;
			private HalfEdge current = null;
			public boolean hasNext()
			{
				return last != current;
			}
			public AbstractHalfEdge next()
			{
				if (current == null)
					current = last;
				current = current.next.next.sym;
				return current.next.sym;
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
			r.append("\nSym: "+sym.hashCode()+"   T="+sym.tri.hashCode()+"["+sym.localNumber+"]");
		r.append("\nAttributes: "+attributes);
		r.append("\nVertices:");
		r.append("\n  Origin: "+origin());
		r.append("\n  Destination: "+destination());
		r.append("\n  Apex: "+apex());
		return r.toString();
	}

	/**
	 * Methods needed by AdjacencyWrapper.
	 */
	
	private void copyFields(HalfEdge src)
	{
		// Do not override tri!
		localNumber = src.localNumber;
		attributes = src.attributes;
		sym = src.sym;
	}
	
	public final void copy(HalfEdge that)
	{
		HalfEdge to = this;
		for (int i = 0; i < 3; i++)
		{
			to.copyFields(that);
			to = to.next;
			that = that.next;
		}
	}
	
}
