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

package org.jcae.mesh.amibe.patch;

import org.apache.log4j.Logger;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.ds.OTriangle;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.util.LongLong;
import org.jcae.mesh.amibe.metrics.Metric2D;
import org.jcae.mesh.mesher.ds.MNode1D;
import org.jcae.mesh.cad.CADVertex;
import org.jcae.mesh.cad.CADFace;
import org.jcae.mesh.cad.CADGeomCurve2D;
import org.jcae.mesh.cad.CADGeomSurface;
import java.util.Random;
import java.util.Iterator;

/**
 * Vertex of a mesh.
 * When meshing a CAD surface, a vertex has two parameters and a metrics in its
 * tangent plane is computed so that a unit mesh in this metrics comply with
 * user constraints.
 * When the underlying surface is defined by the 3D mesh itself, a vertex has
 * three parameters and the surface is locally interpolated by a quadrics
 * computed from vertex neighbours.
 *
 * <p>
 * There is a special vertex, {@link #outer}, which represents a vertex at
 * infinite.  It is used to create exterior triangles.
 * </p>
 */
public class Vertex2D extends Vertex
{
	private static Logger logger = Logger.getLogger(Vertex2D.class);
	/**
	 * Outer vertex.
	 */
	private static final Random rand = new Random(139L);
	private static Vertex2D circumcenter = new Vertex2D(0.0, 0.0);
	
	//  These 2 integer arrays are temporary workspaces
	private static final int [] i0 = new int[2];
	private static final int [] i1 = new int[2];
	
	//  Metrics at this location
	private Metric2D m2 = null;
	
	private Vertex2D()
	{
		super();
	}

	/**
	 * Create an interior Vertex for a 2D mesh.
	 *
	 * @param u  first coordinate.
	 * @param v  second coordinate.
	 */
	private Vertex2D(double u, double v)
	{
		super();
		param[0] = u;
		param[1] = v;
	}
	
	/**
	 * Create an interior Vertex for a 2D mesh.
	 *
	 * @param u  first coordinate.
	 * @param v  second coordinate.
	 */
	public static Vertex2D valueOf(double u, double v)
	{
		return new Vertex2D(u, v);
	}
	
	/**
	 * Create a Vertex in the middle of two 2D Vertex.
	 *
	 * @param pt1  first node.
	 * @param pt2  second node.
	 */
	public static Vertex2D middle(Vertex2D pt1, Vertex2D pt2)
	{
		return new Vertex2D(
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
		Vertex2D ret = new Vertex2D(0.0, 0.0);
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
	public void moveTo(double u, double v)
	{
		param[0] = u;
		param[1] = v;
		m2 = null;
	}
	
	/**
	 * Get the normal to the surface at this location.
	 *
	 * @return the normal to the surface at this location.
	 */
	public double [] getNormal(Mesh2D mesh)
	{
		CADGeomSurface surface = mesh.getGeomSurface();
		surface.setParameter(param[0], param[1]);
		return surface.normal();
	}
	
	/**
	 * Return the 2D centroid of a list of vertices.
	 *
	 * @param v array
	 * @return the 2D centroid of these vertices.
	 */
	public static Vertex2D centroid(Vertex2D [] v)
	{
		double x = 0.0, y = 0.0;
		if (v.length == 0)
			return Vertex2D.valueOf(0.0, 0.0);
		for (int i = 0; i < v.length; i++)
		{
			double [] p = v[i].getUV();
			x += p[0];
			y += p[1];
		}
		x /= v.length;
		y /= v.length;
		return Vertex2D.valueOf(x, y);
	}
	
	/**
	 * Return a triangle containing this point.
	 *
	 * The returned oriented triangle T is noted (oda), and this
	 * algorithm makes sure that there are only three possible
	 * situations at exit:
	 * <ol>
	 *   <li>No vertex of T is Vertex2D.outer, and 'this' is interior to T.</li>
	 *   <li>No vertex of T is Vertex2D.outer, and 'this' is on an edge of T.</li>
	 *   <li>Apex is Vertex2D.outer, ond this.onLeft(d,o) &lt; 0.</li>
	 * </ol>
	 * Origin and destination points are always different from Vertex2D.outer.
	 *
	 * Note that this algorithm had been initially written to take outer
	 * triangles into account.  Later on, <code>BasicMesh</code> had been
	 * rewritten to work without outer triangles, this method had then
	 * to be adapted too, and was much heavier.  Eventually changes
	 * in <code>BasicMesh</code> had been reverted and outer triangles
	 * are back, but this routine has not been modified.  It should be
	 * cleaned up.
	 *
	 * @return a triangle containing this point.
	 * @see OTriangle2D#split3
	 */
	public OTriangle2D getSurroundingOTriangle(Mesh2D mesh)
	{
		if (logger.isDebugEnabled())
			logger.debug("Searching for the triangle surrounding "+this);
		Triangle.List tList = new Triangle.List();
		Triangle t = (Triangle) mesh.quadtree.getNearestVertex(this).link;
		OTriangle2D start = new OTriangle2D(t, 0);
		OTriangle2D current = getSurroundingOTriangleStart(mesh, start, tList);
		if (current == null)
		{
			// First, try with neighbours
			for (Iterator it = tList.iterator(); it.hasNext(); )
			{
				t = (Triangle) it.next();
				start.bind(t);
				current = null;
				for (int i = 0; i < 3; i++)
				{
					start.nextOTri();
					if (!start.hasAttributes(OTriangle.BOUNDARY))
					{
						start.symOTri();
						current = getSurroundingOTriangleStart(mesh, start, tList);
						if (current != null)
							break;
						start.symOTri();
					}
				}
				if (current != null)
					break;
			}
		}
		if (current == null)
		{
			// As a last resort, check with all triangles
			for (Iterator it = mesh.getTriangles().iterator(); it.hasNext();)
			{
				t = (Triangle) it.next();
				start.bind(t);
				current = getSurroundingOTriangleStart(mesh, start, tList);
				if (current != null)
					break;
			}
		}
		tList.clear();
		assert current != null;
		return current;
	}
	
	private OTriangle2D getSurroundingOTriangleStart(Mesh2D mesh, OTriangle2D current, Triangle.List tList)
	{
		boolean redo = false;
		Vertex2D o = (Vertex2D) current.origin();
		Vertex2D d = (Vertex2D) current.destination();
		Vertex2D a = (Vertex2D) current.apex();
		//  Start from an interior triangle, otherwise the loop below
		//  will exit before real processing can take place.  An
		//  alternative is, when apex is Vertex2D.outer, to check the sign
		//  of onLeft(o, d), but moving tests out of this loop is
		//  better.
		//  If the new triangle is also outer, this means that (od)
		//  has 2 adjoining outer triangles, this cannot happen when
		//  mesh has been bootstrapped with 3 points (and is one of
		//  the reasons why bootstrapping with only 2 points is a bad
		//  idea).
		if (o == Vertex2D.outer)
		{
			current.nextOTri();
			if (current.hasAttributes(OTriangle.BOUNDARY))
				return null;
			current.symOTri();
			redo = true;
		}
		else if (d == Vertex2D.outer)
		{
			current.prevOTri();
			if (current.hasAttributes(OTriangle.BOUNDARY))
				return null;
			current.symOTri();
			redo = true;
		}
		else if (a == Vertex2D.outer)
		{
			if (current.hasAttributes(OTriangle.BOUNDARY))
				return null;
			current.symOTri();
			redo = true;
		}
		//  Orient triangle so that point is to the left.  Apex may
		//  be Vertex2D.outer again, but this is case 3 above.
		if (onLeft(mesh, (Vertex2D) current.origin(), (Vertex2D) current.destination()) < 0L)
		{
			if (current.hasAttributes(OTriangle.BOUNDARY) && !redo)
				return null;
			current.symOTri();
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
			assert o != Vertex2D.outer;
			assert d != Vertex2D.outer;
			if (a == Vertex2D.outer)
				break;
			if (tList.contains(current.getTri()))
				return null;
			tList.add(current.getTri());
			long d1 = onLeft(mesh, d, a);
			long d2 = onLeft(mesh, a, o);
			//  Note that for all cases, new origin and destination
			//  points cannot be Vertex2D.outer.
			if (d1 < 0L && d2 < 0L)
			{
				if (rand.nextBoolean())
					current.prevOTriDest();     // (ad*)
				else
					current.nextOTriOrigin();   // (oa*)
			}
			else if (d1 < 0L)
				current.prevOTriDest();         // (ad*)
			else if (d2 < 0L)
				current.nextOTriOrigin();       // (oa*)
			else
				//  d1 >= 0 && d2 >= 0.  
				break;
			o = (Vertex2D) current.origin();
			d = (Vertex2D) current.destination();
			a = (Vertex2D) current.apex();
			if (current.hasAttributes(OTriangle.BOUNDARY))
				return null;
		}
		if (logger.isDebugEnabled())
			logger.debug("Found: "+current);
		return current;
	}
	
	/**
	 * Test the position of this vertex with respect to a segment.
	 * Integer coordinates are used with 2D Euclidian metric
	 * to provide exact computations.  This is important because
	 * this method is called by {@link #getSurroundingOTriangle}
	 * to find the triangle enclosing a vertex, or by
	 * {@link OTriangle2D#forceBoundaryEdge(Mesh2D,Vertex2D)} to compute
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
		assert this != Vertex.outer;
		assert v1 != Vertex.outer;
		assert v2 != Vertex.outer;
		mesh.quadtree.double2int(param, i0);
		mesh.quadtree.double2int(v1.param, i1);
		long x01 = i1[0] - i0[0];
		long y01 = i1[1] - i0[1];
		mesh.quadtree.double2int(v2.param, i1);
		long x02 = i1[0] - i0[0];
		long y02 = i1[1] - i0[1];
		return x01 * y02 - x02 * y01;
	}
	
	/* Unused
	public long dot3(Vertex2D v1, Vertex2D v2)
	{
		mesh.quadtree.double2int(param, i0);
		mesh.quadtree.double2int(v1.param, i1);
		long x01 = i1[0] - i0[0];
		long y01 = i1[1] - i0[1];
		mesh.quadtree.double2int(v2.param, i1);
		long x02 = i1[0] - i0[0];
		long y02 = i1[1] - i0[1];
		return x01 * x02 + y01 * y02;
	}
	
	public final boolean inCircle(Vertex2D v1, Vertex2D v2, Vertex2D v3)
	{
		assert this != Vertex2D.outer;
		assert v1 != Vertex2D.outer;
		assert v2 != Vertex2D.outer;
		assert v3 != Vertex2D.outer;
		// v3.onLeft(v1, v2) >= 0 and onLeft(v1, v2) <= 0
		long d1 = onLeft(v1, v2);
		long d2 = onLeft(v2, v3);
		long d3 = onLeft(v3, v1);
		if (d1 >= 0L && d2 >= 0L && d3 >= 0L)
			return false;
		if (d1 <= 0L && d2 <= 0L && d3 <= 0L)
			return true;
		long o1 = distance2(v1);
		long o2 = distance2cached(v2);
		long o3 = distance2cached(v3);
		LongLong l1 = new LongLong(o3, d1);
		LongLong l2 = new LongLong(o1, d2);
		LongLong l3 = new LongLong(o2, d3);
		if (d1 >= 0L)
		{
			if (d2 >= 0L)
			{
				//  Then d3 < 0
				l2.add(l3);
				l2.add(l1);
			}
			else
			{
				l2.add(l1);
				l2.add(l3);
			}
		}
		else
		{
			if (d2 >= 0L)
			{
				l2.add(l1);
				l2.add(l3);
			}
			else
			{
				l2.add(l3);
				l2.add(l1);
			}
		}
		return l2.isNegative();
	}
	*/
	
	//  Current vertex is symmetric apical vertex
	public final boolean inCircleTest2(Mesh2D mesh, OTriangle2D ot)
	{
		assert this != Vertex2D.outer;
		Vertex2D v1 = (Vertex2D) ot.origin();
		Vertex2D v2 = (Vertex2D) ot.destination();
		Vertex2D v3 = (Vertex2D) ot.apex();
		assert v1 != Vertex2D.outer;
		assert v2 != Vertex2D.outer;
		assert v3 != Vertex2D.outer;
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
	private Vertex2D circumcenter(Mesh2D mesh, Vertex2D v1, Vertex2D v2, Vertex2D v3)
		throws RuntimeException
	{
		double [] p1 = v1.getUV();
		double [] p2 = v2.getUV();
		double [] p3 = v3.getUV();
		//  Metrics on current vertex
		Metric2D m2d = getMetrics(mesh);
		double x12 = p2[0] - p1[0];
		double y12 = p2[1] - p1[1];
		double x23 = p3[0] - p2[0];
		double y23 = p3[1] - p2[1];
		double x31 = p1[0] - p3[0];
		double y31 = p1[1] - p3[1];
		
		double num = m2d.dot(x23, y23, x31, y31);
		double [] po = m2d.orth(x12, y12);
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
	
	public final boolean inCircleTest3(Mesh2D mesh, OTriangle2D ot)
	{
		//  vcX: vertices of current edge
		//  vaX: apices
		assert this != Vertex2D.outer;
		Vertex2D vc1 = (Vertex2D) ot.origin();
		Vertex2D vc2 = (Vertex2D) ot.destination();
		Vertex2D va3 = (Vertex2D) ot.apex();
		// va0 = this
		assert vc1 != Vertex2D.outer;
		assert vc2 != Vertex2D.outer;
		assert va3 != Vertex2D.outer;
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
		
		try {
			Vertex2D C3 = va3.circumcenter(mesh, vc1, vc2, va3);
			double ret =
				mesh.compGeom().distance(C3, this, va3) /
				mesh.compGeom().distance(C3, va3, va3);
			Vertex2D C0 = circumcenter(mesh, vc1, vc2, va3);
			ret +=
				mesh.compGeom().distance(C0, this, this) /
				mesh.compGeom().distance(C0, va3, this);
			return (ret < 2.0);
		}
		catch (RuntimeException ex)
		{
		}
		try {
			// Test the swapped edge
			// this -> vc2   vc1 -> this   vc2 -> va3  va3 -> vc1
			Vertex2D C3 = vc1.circumcenter(mesh, this, va3, vc1);
			double ret =
				mesh.compGeom().distance(C3, vc2, vc1) /
				mesh.compGeom().distance(C3, vc1, vc1);
			Vertex2D C0 = circumcenter(mesh, this, va3, vc1);
			ret +=
				mesh.compGeom().distance(C0, vc2, vc2) /
				mesh.compGeom().distance(C0, vc1, vc2);
			return (ret > 2.0);
		}
		catch (RuntimeException ex)
		{
			return inCircleTest2(mesh, ot);
		}
	}
	
	public final boolean isSmallerDiagonale(Mesh2D mesh, OTriangle2D ot)
	{
		//  vcX: vertices of current edge
		//  vaX: apices
		assert this != Vertex2D.outer;
		Vertex2D vc1 = (Vertex2D) ot.origin();
		Vertex2D vc2 = (Vertex2D) ot.destination();
		Vertex2D va3 = (Vertex2D) ot.apex();
		// va0 = this
		assert vc1 != Vertex2D.outer;
		assert vc2 != Vertex2D.outer;
		assert va3 != Vertex2D.outer;
		// Do not swap if triangles are reversed in 2d space
		if (vc1.onLeft(mesh, va3, this) >= 0L || vc2.onLeft(mesh, va3, this) <= 0L)
			return true;
		
		//  Add a 0.5 factor so that edges are swapped only if
		//  there is a significant gain.
		return (mesh.compGeom().distance(va3, this, vc1) +
		        mesh.compGeom().distance(va3, this, vc2) > 0.5 * (
		        mesh.compGeom().distance(vc1, vc2, va3) +
		        mesh.compGeom().distance(vc1, vc2, this)));
	}
	
	public boolean isPseudoIsotropic(Mesh2D mesh)
	{
		Metric2D m2d = getMetrics(mesh);
		return m2d.isPseudoIsotropic();
	}
	
	public final long distance2(Mesh2D mesh, Vertex2D that)
	{
		mesh.quadtree.double2int(param, i0);
		mesh.quadtree.double2int(that.param, i1);
		long dx = i0[0] - i1[0];
		long dy = i0[1] - i1[1];
		return dx * dx + dy * dy;
	}
	private final long distance2cached(Mesh2D mesh, Vertex2D that)
	{
		mesh.quadtree.double2int(that.param, i1);
		long dx = i0[0] - i1[0];
		long dy = i0[1] - i1[1];
		return dx * dx + dy * dy;
	}
	
	/**
	 * Get the 2D Riemannian metrics at this point.  This metrics
	 * is computed and then stored into a private instance member.
	 * This cached value can be discarded by calling {@link #clearMetrics}.
	 *
	 * @param mesh  underlying Mesh2D instance
	 * @return the 2D Riemannian metrics at this point.
	 */
	public Metric2D getMetrics(Mesh2D mesh)
	{
		if (null == m2)
		{
			Calculus curr = mesh.compGeom();
			if (curr instanceof Calculus2D)
				m2 = new Metric2D();
			else
				m2 = new Metric2D(mesh.getGeomSurface(), this);
		}
		return m2;
	}
	
	/**
	 * Clear the 2D Riemannian metrics at this point.
	 */
	public void clearMetrics()
	{
		m2 = null;
	}
	
	public String toString ()
	{
		if (this == Vertex2D.outer)
			return "outer";
		String r = "UV:";
		for (int i = 0; i < param.length; i++)
			r += " "+param[i];
		if (ref1d != 0)
			r += " ref1d: "+ref1d;
		r += " hash: "+hashCode();
		if (link != null)
			r += " link: "+link.hashCode();
		return r;
	}
	
}
