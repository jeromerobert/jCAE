/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005,2006, by EADS CRC
    Copyright (C) 2007,2008, by EADS France

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

package org.jcae.mesh.amibe.patch;

import org.jcae.mesh.amibe.ds.MNode1D;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.ds.TriangleVH;
import org.jcae.mesh.amibe.traits.VertexTraitsBuilder;

import org.jcae.mesh.cad.CADVertex;
import org.jcae.mesh.cad.CADFace;
import org.jcae.mesh.cad.CADGeomCurve2D;
import org.jcae.mesh.cad.CADGeomSurface;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Vertex of a mesh.
 * When meshing a CAD surface, a vertex has two parameters and a metrics in its
 * tangent plane is computed so that a unit mesh in this metrics comply with
 * user constraints.
 * When the underlying surface is defined by the 3D mesh itself, a vertex has
 * three parameters and the surface is locally interpolated by a quadrics
 * computed from vertex neighbours.
 */
public class Vertex2D extends Vertex
{
	private static final long serialVersionUID = -6099275818186028566L;
	private static final Logger logger=Logger.getLogger(Vertex2D.class.getName());
	private static final Random rand = new Random(139L);
	private static Vertex2D circumcenter = new Vertex2D(null, 0.0, 0.0);
	
	//  These 2 integer arrays are temporary workspaces
	private static final int [] i0 = new int[2];
	private static final int [] i1 = new int[2];

	/**
	 * Metric at this Vertex.  It is managed by Mesh2D.
	 */
	Metric2D metric;

	/**
	 * Create a Vertex for a 2D mesh.
	 *
	 * @param vtb  traits builder
	 * @param u  first coordinate.
	 * @param v  second coordinate.
	 */
	protected Vertex2D(VertexTraitsBuilder vtb, double u, double v)
	{
		super(vtb);
		param[0] = u;
		param[1] = v;
	}
	
	/**
	 * Create a Vertex in the middle of two 2D Vertex.
	 *
	 * @param pt1  first node.
	 * @param pt2  second node.
	 */
	protected static Vertex2D middle(Vertex2D pt1, Vertex2D pt2)
	{
		return new Vertex2D(null,
			0.5 * (pt1.param[0] + pt2.param[0]),
			0.5 * (pt1.param[1] + pt2.param[1])
		);
	}
	
	/**
	 * Create a Vertex from a boundary node.
	 *
	 * @param pt  node on a boundary edge.
	 * @param C2d 2D curve on the face.
	 * @param F   topological face.
	 */
	public static Vertex2D valueOf(MNode1D pt, CADGeomCurve2D C2d, CADFace F)
	{
		Vertex2D ret = new Vertex2D(null, 0.0, 0.0);
		ret.ref1d = pt.getMaster().getLabel();
		if (null != C2d)
		{
			double [] uv = C2d.value(pt.getParameter());
			ret.param[0] = uv[0];
			ret.param[1] = uv[1];
		}
		else
		{
			CADVertex V = pt.getCADVertex();
			if (null == V)
				throw new java.lang.RuntimeException("Error in Vertex()");
			double [] uv = V.parameters(F);
			ret.param[0] = uv[0];
			ret.param[1] = uv[1];
		}
		return ret;
	}
	
	/**
	 * Set the coordinates of this Vertex (2D).
	 *
	 * @param u  first coordinate of the new position
	 * @param v  second coordinate of the new position
	 */
	@Override
	public void moveTo(double u, double v)
	{
		param[0] = u;
		param[1] = v;
	}

	@Override
	public void moveTo(double x, double y, double z)
	{
		throw new RuntimeException();
	}

	/**
	 * Get the normal to the surface at this location.
	 *
	 * @return the normal to the surface at this location.
	 */
	@SuppressWarnings("unused")
	private double [] getNormal(Mesh2D mesh)
	{
		CADGeomSurface surface = mesh.getGeomSurface();
		surface.setParameter(param[0], param[1]);
		return surface.normal();
	}
	
	/**
	 * Return a triangle containing this point.
	 *
	 * The returned oriented triangle T is noted (oda), and this
	 * algorithm makes sure that there are only three possible
	 * situations at exit:
	 * <ol>
	 *   <li>No vertex of T is Mesh.outerVertex, and 'this' is interior
	 *       to T.</li>
	 *   <li>No vertex of T is Mesh.outerVertex, and 'this' is on an
	 *       edge of T.</li>
	 *   <li>Apex is Mesh.outerVertex, and this.onLeft(d,o) &lt; 0.</li>
	 * </ol>
	 * Origin and destination points are always different from
	 * {@link org.jcae.mesh.amibe.ds.Mesh#outerVertex}.
	 *
	 * @return a triangle containing this point.
	 * @see VirtualHalfEdge2D#split3
	 */
	public VirtualHalfEdge2D getSurroundingOTriangle(Mesh2D mesh)
	{
		if (logger.isLoggable(Level.FINE))
			logger.fine("Searching for the triangle surrounding "+this);
		TriangleVH t = (TriangleVH) mesh.getKdTree().getNearVertex(mesh.getMetric(this), this).getLink();
		VirtualHalfEdge2D current = new VirtualHalfEdge2D(t, 0);
		boolean redo = false;
		Vertex2D o = (Vertex2D) current.origin();
		Vertex2D d = (Vertex2D) current.destination();
		Vertex2D a = (Vertex2D) current.apex();
		//  Start from an interior triangle, otherwise the loop below
		//  will exit before real processing can take place.  An
		//  alternative is, when apex is outerVertex, to check the sign
		//  of onLeft(o, d), but moving tests out of this loop is
		//  better.
		//  If the new triangle is also outer, this means that (od)
		//  has 2 adjoining outer triangles, this cannot happen when
		//  mesh has been bootstrapped with 3 points (and is one of
		//  the reasons why bootstrapping with only 2 points is a bad
		//  idea).
		if (o == mesh.outerVertex)
		{
			current.next();
			current.sym();
			redo = true;
		}
		else if (d == mesh.outerVertex)
		{
			current.prev();
			current.sym();
			redo = true;
		}
		else if (a == mesh.outerVertex)
		{
			current.sym();
			redo = true;
		}
		//  Orient triangle so that point is to the left.  Apex may
		//  be outerVertex again, but this is case 3 above.
		if (onLeft(mesh, (Vertex2D) current.origin(), (Vertex2D) current.destination()) < 0L)
		{
			current.sym();
			redo = true;
		}
		if (redo)
		{
			o = (Vertex2D) current.origin();
			d = (Vertex2D) current.destination();
			a = (Vertex2D) current.apex();
		}
		while (true)
		{
			assert o != mesh.outerVertex;
			assert d != mesh.outerVertex;
			if (a == mesh.outerVertex)
				break;
			long d1 = onLeft(mesh, d, a);
			long d2 = onLeft(mesh, a, o);
			//  Note that for all cases, new origin and destination
			//  points cannot be outerVertex.
			if (d1 < 0L && d2 < 0L)
			{
				if (rand.nextBoolean())
				{
					current.next();         // (dao)
					current.sym();          // (ad*)
				}
				else
					current.nextOrigin();   // (oa*)
			}
			else if (d1 < 0L)
			{
				current.next();                 // (dao)
				current.sym();                  // (ad*)
			}
			else if (d2 < 0L)
				current.nextOrigin();           // (oa*)
			else
				//  d1 >= 0 && d2 >= 0.  
				break;
			o = (Vertex2D) current.origin();
			d = (Vertex2D) current.destination();
			a = (Vertex2D) current.apex();
		}
		if (logger.isLoggable(Level.FINE))
			logger.fine("Found: "+current);
		return current;
	}
	
	/**
	 * Test the position of this vertex with respect to a segment.
	 * Integer coordinates are used with 2D Euclidian metric
	 * to provide exact computations.  This is important because
	 * this method is called by {@link #getSurroundingOTriangle}
	 * to find the triangle enclosing a vertex, or by
	 * {@link VirtualHalfEdge2D#forceBoundaryEdge(Mesh2D,Vertex2D)} to compute
	 * segment intersection.
	 *
	 * @param mesh  underlying Mesh2D instance
	 * @param v1   first vertex of the segment
	 * @param v2   second vertex of the segment
	 * @return the signed area of the triangle composed of these three
	 * vertices. It is positive if the vertex is on the left of this
	 * segment, and negative otherwise.
	 */
	public long onLeft(Mesh2D mesh, Vertex2D v1, Vertex2D v2)
	{
		assert this != mesh.outerVertex;
		assert v1 != mesh.outerVertex;
		assert v2 != mesh.outerVertex;
		mesh.getKdTree().double2int(param, i0);
		mesh.getKdTree().double2int(v1.param, i1);
		long x01 = i1[0] - i0[0];
		long y01 = i1[1] - i0[1];
		mesh.getKdTree().double2int(v2.param, i1);
		long x02 = i1[0] - i0[0];
		long y02 = i1[1] - i0[1];
		return x01 * y02 - x02 * y01;
	}
	
	//  Current vertex is symmetric apical vertex
	protected final boolean inCircle2D(Mesh2D mesh, VirtualHalfEdge2D ot)
	{
		assert this != mesh.outerVertex;
		Vertex2D v1 = (Vertex2D) ot.origin();
		Vertex2D v2 = (Vertex2D) ot.destination();
		Vertex2D v3 = (Vertex2D) ot.apex();
		assert v1 != mesh.outerVertex;
		assert v2 != mesh.outerVertex;
		assert v3 != mesh.outerVertex;
		assert v1.onLeft(mesh, v2, v3) >= 0L : ot+" "+v1.onLeft(mesh, v2, v3);
		assert v1.onLeft(mesh, this, v2) >= 0L : ot+" "+v1.onLeft(mesh, this, v2);
		long d1 = v1.onLeft(mesh, v3, this);
		if (d1 >= 0L)
			return false;
		long d2 = v1.onLeft(mesh, v2, v3);
		long d3 = v1.onLeft(mesh, this, v2);
		if (d2 <= 0L && d3 <= 0L)
			return true;
		//  Here, d1 < 0, d2 >= 0 and d3 >= 0
		long o1 = v1.distance2(mesh, v2);
		long o2 = v1.distance2cached(mesh, this);
		long o3 = v1.distance2cached(mesh, v3);
		LongLong l1 = new LongLong(o1, d1);
		LongLong l2 = new LongLong(o2, d2);
		LongLong l3 = new LongLong(o3, d3);
		l1.add(l2);
		l1.add(l3);
		return l1.isNegative();
	}
	
	/*
	   Consider a vector V(x,y) and a 2d metrics M(E,F,F,G)
	   Then orth(M,V) = (-Fx-Gy, Ex+Fy) verifies:
	     * trans(orth(M,V)) M V = 0  (i.e. orth(M,V) is orthogonal to V)
	     * trans(orth(M,V)) M orth(M,V) = det(M) trans(V) M V
	   Let us call V12 = (v2-v1), V13 = (v3-v1) and V23 = (v3-v2)
	   The circumcenter C verifies:
	     C = middle(v1,v2) + x orth(M,V12)
	       = middle(v1,v3) + y orth(M,V13)
	     ==> x orth(M,V12) - y orth(M,V13) = 0.5 V23
	         x = <V23, V13> / (2 <orth(M,V12), V13>)
	*/
	private static Vertex2D circumcenter(Metric2D m2d, Vertex2D v1, Vertex2D v2, Vertex2D v3, double[] po)
		throws RuntimeException
	{
		double [] p1 = v1.getUV();
		double [] p2 = v2.getUV();
		double [] p3 = v3.getUV();
		//  Metrics on current vertex
		double x12 = p2[0] - p1[0];
		double y12 = p2[1] - p1[1];
		double x23 = p3[0] - p2[0];
		double y23 = p3[1] - p2[1];
		double x31 = p1[0] - p3[0];
		double y31 = p1[1] - p3[1];
		
		double num = m2d.dot(x23, y23, x31, y31);
		m2d.computeOrthogonalVector(x12, y12, po);
		double den = 2.0 * m2d.dot(po[0], po[1], x31, y31);
		//  Flat triangles cannot be computed accurately, we
		//  consider arbitrarily that C is returned if
		//     distance(C, middle(v1, v2)) < 1000 * distance(v1, v2)
		//     <=> |num/den| trans(po) M po < 1000 * trans(v12) M v12
		//     <=> num * num * det(M) < 1000000 * den * den
		if (den != 0.0 && num * num * m2d.det() < 1000000.0 * den * den)
		{
			circumcenter.param[0] = 0.5*(p1[0]+p2[0]) + po[0] * num / den;
			circumcenter.param[1] = 0.5*(p1[1]+p2[1]) + po[1] * num / den;
			return circumcenter;
		}
		throw new RuntimeException("Circumcenter cannot be computed");
	}
	
	protected final boolean inCircle(Mesh2D mesh, VirtualHalfEdge2D ot)
	{
		//  vcX: vertices of current edge
		//  vaX: apices
		assert this != mesh.outerVertex;
		Vertex2D vc1 = (Vertex2D) ot.origin();
		Vertex2D vc2 = (Vertex2D) ot.destination();
		Vertex2D va3 = (Vertex2D) ot.apex();
		// va0 = this
		assert vc1 != mesh.outerVertex;
		assert vc2 != mesh.outerVertex;
		assert va3 != mesh.outerVertex;
		// Special case when vc1, vc2 and va3 are aligned
		if (va3.onLeft(mesh, vc1, vc2) == 0L)
		{
			if (onLeft(mesh, vc1, vc2) == 0L)
			{
				long l1 = vc1.distance2(mesh, vc2);
				return (distance2(mesh, vc1) < l1 && distance2(mesh, vc2) < l1 && va3.distance2(mesh, vc1) < l1 && va3.distance2(mesh, vc2) < l1);
			}
			if (vc1.onLeft(mesh, va3, this) >= 0L || vc2.onLeft(mesh, va3, this) <= 0L)
				return false;
			long l1 = vc1.distance2(mesh, vc2);
			return (va3.distance2(mesh, vc1) < l1 && va3.distance2(mesh, vc2) < l1);
		}
		// Do not swap if triangles are inverted in 2d space
		if (vc1.onLeft(mesh, va3, this) >= 0L || vc2.onLeft(mesh, va3, this) <= 0L)
			return false;

		double [] orth = new double[2];
		try {
			Metric2D mA = mesh.getMetric(this);
			Metric2D mB = mesh.getMetric(va3);
			Vertex2D C3 = circumcenter(mB, vc1, vc2, va3, orth);
			double ret = Math.sqrt(
				mB.distance2(C3.param, param) /
				mB.distance2(C3.param, va3.param));
			Vertex2D C0 = circumcenter(mA, vc1, vc2, va3, orth);
			ret += Math.sqrt(
				mA.distance2(C0.param, param) /
				mA.distance2(C0.param, va3.param));
			return (ret < 2.0);
		}
		catch (RuntimeException ex)
		{
		}
		try {
			// Test the swapped edge
			// this -> vc2   vc1 -> this   vc2 -> va3  va3 -> vc1
			Metric2D mA = mesh.getMetric(vc2);
			Metric2D mB = mesh.getMetric(vc1);
			Vertex2D C3 = circumcenter(mB, this, va3, vc1, orth);
			double ret = Math.sqrt(
				mB.distance2(C3.param, vc2.param) /
				mB.distance2(C3.param, vc1.param));
			// FIXME: mesh.getMetric(this) gives better results than mA,
			// see for instance sphere.brep with an edge length of 0.005
			// That sounds wrong, it needs to be investigated.
			Vertex2D C0 = circumcenter(mesh.getMetric(this), this, va3, vc1, orth);
			ret += Math.sqrt(
				mA.distance2(C0.param, vc2.param) /
				mA.distance2(C0.param, vc1.param));
			return (ret > 2.0);
		}
		catch (RuntimeException ex)
		{
			return inCircle2D(mesh, ot);
		}
	}
	
	protected final boolean isSmallerDiagonale(Mesh2D mesh, VirtualHalfEdge2D ot)
	{
		//  vcX: vertices of current edge
		//  vaX: apices
		assert this != mesh.outerVertex;
		Vertex2D vc1 = (Vertex2D) ot.origin();
		Vertex2D vc2 = (Vertex2D) ot.destination();
		Vertex2D va3 = (Vertex2D) ot.apex();
		// va0 = this
		assert vc1 != mesh.outerVertex;
		assert vc2 != mesh.outerVertex;
		assert va3 != mesh.outerVertex;
		// Do not swap if triangles are reversed in 2d space
		if (vc1.onLeft(mesh, va3, this) >= 0L || vc2.onLeft(mesh, va3, this) <= 0L)
			return true;
		
		//  Add a 0.5 factor so that edges are swapped only if
		//  there is a significant gain.
		Metric2D mc1 = mesh.getMetric(vc1);
		Metric2D mc2 = mesh.getMetric(vc2);
		Metric2D ma3 = mesh.getMetric(va3);
		Metric2D m0 = mesh.getMetric(this);
		return (Math.sqrt(mc1.distance2(va3.param, param)) +
		        Math.sqrt(mc2.distance2(va3.param, param)) > 0.5 * (
		        Math.sqrt(ma3.distance2(vc1.param, vc2.param)) +
		        Math.sqrt(m0.distance2(vc1.param, vc2.param))));
	}
	
	protected boolean isPseudoIsotropic(Mesh2D mesh)
	{
		return mesh.getMetric(this).isPseudoIsotropic();
	}
	
	private final long distance2(Mesh2D mesh, Vertex2D that)
	{
		mesh.getKdTree().double2int(param, i0);
		mesh.getKdTree().double2int(that.param, i1);
		long dx = i0[0] - i1[0];
		long dy = i0[1] - i1[1];
		return dx * dx + dy * dy;
	}
	private final long distance2cached(Mesh2D mesh, Vertex2D that)
	{
		mesh.getKdTree().double2int(that.param, i1);
		long dx = i0[0] - i1[0];
		long dy = i0[1] - i1[1];
		return dx * dx + dy * dy;
	}
	
	@Override
	public String toString ()
	{
		StringBuilder r = new StringBuilder("UV:");
		for (int i = 0; i < param.length; i++)
			r.append(" "+param[i]);
		if (ref1d != 0)
			r.append(" ref1d: "+ref1d);
		r.append(" hash: "+hashCode());
		if (link != null)
			r.append(" link: "+link.hashCode());
		return r.toString();
	}
	
}
