/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2004 Jerome Robert <jeromerobert@users.sourceforge.net>

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

import org.apache.log4j.Logger;
import org.jcae.mesh.amibe.util.LongLong;
import org.jcae.mesh.amibe.ds.tools.*;
import org.jcae.mesh.amibe.metrics.Metric2D;
import org.jcae.mesh.mesher.ds.MNode1D;
import org.jcae.mesh.cad.*;
import java.util.Random;

public class Vertex
{
	private static Logger logger = Logger.getLogger(Vertex.class);
	//  Set by Mesh.init
	public static Vertex outer = null;
	public static Mesh mesh = null;
	
	private static final Random rand = new Random(139L);
	public double [] param;
	public Triangle tri;
	
	//  Metrics at this location
	private Metric2D m2 = null;
	
	//  Link to the geometrical node, if any
	private MNode1D ref1d = null;
	
	//  These 2 integer arrays are temporary workspaces
	private static final int [] i0 = new int[2];
	private static final int [] i1 = new int[2];
	
	public Vertex(double u, double v)
	{
		param = new double[2];
		param[0] = u;
		param[1] = v;
	}
	
	public Vertex(MNode1D pt, CADGeomCurve2D C2d, CADFace F)
	{
		ref1d = pt;
		if (null != C2d)
			param = C2d.value(pt.getParameter());
		else
		{
			CADVertex V = pt.getRef();
			if (null == V)
				throw new java.lang.RuntimeException("Error in Vertex()");
			param = V.parameters(F);
		}
	}
	
	public double [] getUV ()
	{
		return param;
	}
	
	public MNode1D getRef()
	{
		if (null == ref1d)
			return null;
		return ref1d.getMaster();
	}
	
	public void addToQuadTree()
	{
		logger.debug("Inserted point: "+this);
		mesh.quadtree.add(this);
	}
	
	/**
	 * Returns a triangle containing this point.
	 *
	 * The returned oriented triangle T is noted (oda), and this
	 * algorithm makes sure that there are only three possible
	 * situations at exit:
	 * <ol>
	 *   <li>No vertex of T is Vertex.outer, and 'this' is interior to T.</li>
	 *   <li>No vertex of T is Vertex.outer, and 'this' is on an edge of T.</li>
	 *   <li>Apex is Vertex.outer, ond this.onLeft(d,o) &lt; 0.</li>
	 * </ol>
	 * Origin and destination points are always different from Vertex.outer.
	 *
	 * @return a triangle containing this point.
	 * @see OTriangle#split3
	 */
	public OTriangle getSurroundingOTriangle()
	{
		logger.debug("Searching for the triangle surrounding "+this);
		Triangle t = mesh.quadtree.getNearestVertex(this).tri;
		OTriangle start = new OTriangle(t, 0);
		OTriangle current = start;
		boolean redo = false;
		Vertex o = current.origin();
		Vertex d = current.destination();
		Vertex a = current.apex();
		//  Start from an interior triangle, otherwise the loop below
		//  will exit before real processing can take place.  An
		//  alternative is, when apex is Vertex.outer, to check the sign
		//  of onLeft(o, d), but moving tests out of this loop is
		//  better.
		//  If the new triangle is also outer, this means that (od)
		//  has 2 adjoining outer triangles, this cannot happen when
		//  mesh has been bootstrapped with 3 points (and is one of
		//  the reasons why bootstrapping with only 2 points is a bad
		//  idea).
		if (o == Vertex.outer)
		{
			current.nextOTri();
			current.symOTri();
			redo = true;
		}
		else if (d == Vertex.outer)
		{
			current.prevOTri();
			current.symOTri();
			redo = true;
		}
		else if (a == Vertex.outer)
		{
			current.symOTri();
			redo = true;
		}
		//  Orient triangle so that point is to the left.  Apex may
		//  be Vertex.outer again, but this is case 3 above.
		if (onLeft(current.origin(), current.destination()) < 0L)
		{
			current.symOTri();
			redo = true;
		}
		if (redo)
		{
			o = current.origin();
			d = current.destination();
			a = current.apex();
		}
		while (true)
		{
			assert o != Vertex.outer;
			assert d != Vertex.outer;
			assert onLeft(o, d) >= 0L;
			if (a == Vertex.outer)
				break;
			long d1 = onLeft(d, a);
			long d2 = onLeft(a, o);
			//  Note that for all cases, new origin and destination
			//  points cannot be Vertex.outer.
			if (d1 < 0L && d2 < 0L)
			{
				if (rand.nextBoolean())
					current.prevOTriDest();     // (ad*)
				else
					current.nextOTriOrigin();   // (oa*)
			}
			else if (d1 < 0L && d2 >= 0L)
				current.prevOTriDest();         // (ad*)
			else if (d2 < 0L && d1 >= 0L)
				current.nextOTriOrigin();       // (oa*)
			else
				//  d1 >= 0 && d2 >= 0.  
				break;
			o = current.origin();
			d = current.destination();
			a = current.apex();
		}
		logger.debug("Found: "+current);
		return current;
	}
	
	private long onLeft_isotropic(Vertex v1, Vertex v2)
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
	
	public long onLeft(Vertex v1, Vertex v2)
	{
		return onLeft_isotropic(v1, v2);
	}
	
	public long dot3(Vertex v1, Vertex v2)
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
	
	public final boolean inCircle(Vertex v1, Vertex v2, Vertex v3)
	{
		assert this != Vertex.outer;
		assert v1 != Vertex.outer;
		assert v2 != Vertex.outer;
		assert v3 != Vertex.outer;
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
	
	//  Current vertex is symmetric apical vertex
	public final boolean inCircleTest2(OTriangle ot)
	{
		assert this != Vertex.outer;
		Vertex v1 = ot.origin();
		Vertex v2 = ot.destination();
		Vertex v3 = ot.apex();
		assert v1 != Vertex.outer;
		assert v2 != Vertex.outer;
		assert v3 != Vertex.outer;
		assert v1.onLeft(v2, v3) >= 0L : ot+" "+v1.onLeft(v2, v3);
		assert v1.onLeft(this, v2) >= 0L : ot+" "+v1.onLeft(this, v2);
		long d1 = v1.onLeft(v3, this);
		if (d1 >= 0L)
			return false;
		long d2 = v1.onLeft(v2, v3);
		long d3 = v1.onLeft(this, v2);
		if (d2 <= 0L && d3 <= 0L)
			return true;
		//  Here, d1 < 0, d2 >= 0 and d3 >= 0
		long o1 = v1.distance2(v2);
		long o2 = v1.distance2cached(this);
		long o3 = v1.distance2cached(v3);
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
	public Vertex circumcenter(Vertex v1, Vertex v2, Vertex v3)
		throws RuntimeException
	{
		double [] p1 = v1.getUV();
		double [] p2 = v2.getUV();
		double [] p3 = v3.getUV();
		//  Metrics on current vertex
		Metric2D m2d = getMetrics(mesh.getGeomSurface());
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
		//     distance(C, middle(v1, v2)) < 10 * distance(v1, v2)
		//     <=> |num/den| trans(po) M po < 10 * trans(v12) M v12
		//     <=> num * num * det(M) < 100 * den * den
		if (num * num * m2d.det() < 100.0 * den * den)
		{
			return new Vertex(
				0.5*(p1[0]+p2[0]) + po[0] * num / den,
				0.5*(p1[1]+p2[1]) + po[1] * num / den
			);
		}
		throw new RuntimeException("Circumcenter cannot be computed");
	}
	
	public final boolean inCircleTest3(OTriangle ot)
	{
		//  vcX: vertices of current edge
		//  vaX: apices
		assert this != Vertex.outer;
		Vertex vc1 = ot.origin();
		Vertex vc2 = ot.destination();
		Vertex va3 = ot.apex();
		// va0 = this
		assert vc1 != Vertex.outer;
		assert vc2 != Vertex.outer;
		assert va3 != Vertex.outer;
		// Do not swap if triangles are reversed in 2d space
		if (vc1.onLeft(va3, this) >= 0L || vc2.onLeft(va3, this) <= 0L)
			return false;
		
		try {
			Vertex C3 = va3.circumcenter(vc1, vc2, va3);
			Vertex C0 = circumcenter(vc1, vc2, va3);
			double ret =
				mesh.compGeom().distance(C3, this, va3) /
				mesh.compGeom().distance(C3, va3, va3) +
				mesh.compGeom().distance(C0, this, this) /
				mesh.compGeom().distance(C0, va3, this);
			return (ret < 2.0);
		}
		catch (RuntimeException ex)
		{
			return inCircleTest2(ot);
		}
	}
	
	public final boolean isSmallerDiagonale(OTriangle ot)
	{
		//  vcX: vertices of current edge
		//  vaX: apices
		assert this != Vertex.outer;
		Vertex vc1 = ot.origin();
		Vertex vc2 = ot.destination();
		Vertex va3 = ot.apex();
		// va0 = this
		assert vc1 != Vertex.outer;
		assert vc2 != Vertex.outer;
		assert va3 != Vertex.outer;
		// Do not swap if triangles are reversed in 2d space
		if (vc1.onLeft(va3, this) >= 0L || vc2.onLeft(va3, this) <= 0L)
			return true;
		
		return (mesh.compGeom().distance(va3, this, vc1) +
		        mesh.compGeom().distance(va3, this, vc2) >
		        mesh.compGeom().distance(vc1, vc2, va3) +
		        mesh.compGeom().distance(vc1, vc2, this));
	}
	
	public boolean isPseudoIsotropic()
	{
		Metric2D m2d = getMetrics(mesh.getGeomSurface());
		return m2d.isPseudoIsotropic();
	}
	
	public final int distance(Vertex that)
	{
		return (int) Math.sqrt(distance2(that));
	}
	
	public final long distance2(Vertex that)
	{
		mesh.quadtree.double2int(param, i0);
		mesh.quadtree.double2int(that.param, i1);
		long dx = i0[0] - i1[0];
		long dy = i0[1] - i1[1];
		return dx * dx + dy * dy;
	}
	private final long distance2cached(Vertex that)
	{
		mesh.quadtree.double2int(that.param, i1);
		long dx = i0[0] - i1[0];
		long dy = i0[1] - i1[1];
		return dx * dx + dy * dy;
	}
	
	public Metric2D getMetrics(CADGeomSurface surf)
	{
		if (null == m2)
		{
			Calculus curr = mesh.compGeom();
			if (curr instanceof Calculus2D)
				m2 = new Metric2D();
			else
				m2 = new Metric2D(surf, this);
		}
		return m2;
	}
	
	public void clearMetrics()
	{
		m2 = null;
	}
	
	public String toString ()
	{
		if (this == Vertex.outer)
			return "outer";
		String r = "UV: "+param[0]+" "+param[1];
		if (ref1d != null)
			r += " ref1d: "+ref1d;
		return r;
	}
	
}
