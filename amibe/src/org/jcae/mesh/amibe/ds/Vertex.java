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

import org.apache.log4j.Logger;
import org.jcae.mesh.amibe.util.LongLong;
import org.jcae.mesh.amibe.ds.tools.*;
import org.jcae.mesh.amibe.metrics.Metric2D;
import org.jcae.mesh.amibe.metrics.Metric3D;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.mesher.ds.MNode1D;
import org.jcae.mesh.cad.*;
import java.util.Random;
import java.util.HashSet;
import java.util.ArrayList;
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
public class Vertex implements Cloneable
{
	private static Logger logger = Logger.getLogger(Vertex.class);
	//  Set by Mesh.init
	public static Vertex outer = null;
	public static Mesh mesh = null;
	private static final Random rand = new Random(139L);
	private static Vertex circumcenter = new Vertex(0.0, 0.0);
	
	//  These 2 integer arrays are temporary workspaces
	private static final int [] i0 = new int[2];
	private static final int [] i1 = new int[2];
	
	public double [] param = null;
	//  link can be either:
	//    1. a Triangle, for manifold vertices
	//    2. an Object[2] array, zhere
	//         0: list of head triangles
	//         1: list of incident wires
	private Object link;
	
	//  Metrics at this location
	private Metric2D m2 = null;
	
	//  ref1d > 0: link to the geometrical node
	//  ref1d = 0: inner node
	//  ref1d < 0: node on an inner boundary
	//  
	private int ref1d = 0;
	// Used in OEMM
	private int label = 0;
	private boolean readable = false;
	private boolean writable = false;
	private boolean modified = false;
	private boolean deleted = false;
	
	/**
	 * Create an interior Vertex for a 2D mesh.
	 *
	 * @param u  first coordinate.
	 * @param v  second coordinate.
	 */
	public Vertex(double u, double v)
	{
		param = new double[2];
		param[0] = u;
		param[1] = v;
	}
	
	/**
	 * Create an interior Vertex for a 3D mesh.
	 *
	 * @param x  first coordinate.
	 * @param y  second coordinate.
	 * @param z  third coordinate.
	 */
	public Vertex(double x, double y, double z)
	{
		param = new double[3];
		param[0] = x;
		param[1] = y;
		param[2] = z;
	}
	
	/**
	 * Create a Vertex from a boundary node.
	 *
	 * @param pt  node on a boundary edge.
	 * @param C2d 2D curve on the face.
	 * @param F   topological face.
	 */
	public Vertex(MNode1D pt, CADGeomCurve2D C2d, CADFace F)
	{
		ref1d = pt.getMaster().getLabel();
		if (null != C2d)
			param = C2d.value(pt.getParameter());
		else
		{
			CADVertex V = pt.getCADVertex();
			if (null == V)
				throw new java.lang.RuntimeException("Error in Vertex()");
			param = V.parameters(F);
		}
	}
	
	/**
	 * Create a Vertex in the middle of two 2D Vertex.
	 *
	 * @param pt1  first node.
	 * @param pt2  second node.
	 */
	public Vertex(Vertex pt1, Vertex pt2)
	{
		param = new double[2];
		param[0] = 0.5 * (pt1.param[0] + pt2.param[0]);
		param[1] = 0.5 * (pt1.param[1] + pt2.param[1]);
	}
	
	/**
	 * Copy the current Vertex into another one.
	 *
	 * @return a new Vertex with the same attributes.
	 */
	public final Object clone()
	{
		Object ret = null;
		try
		{
			ret = super.clone();
			Vertex that = (Vertex) ret;
			that.param = new double[param.length];
			for (int i = 0; i < param.length; i++)
				that.param[i] = param[i];
		}
		catch (java.lang.CloneNotSupportedException ex)
		{}
		return ret;
	}
	
	/**
	 * Get the coordinates of this Vertex.
	 *
	 * @return the coordinates of this Vertex.
	 */
	public double [] getUV ()
	{
		return param;
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
	 * Set the coordinates of this Vertex (3D).
	 *
	 * @param x  first coordinate of the new position
	 * @param y  second coordinate of the new position
	 * @param z  third coordinate of the new position
	 */
	public void moveTo(double x, double y, double z)
	{
		param[0] = x;
		param[1] = y;
		param[2] = z;
		m2 = null;
	}
	
	/**
	 * Get the normal to the surface at this location.
	 *
	 * @return the normal to the surface at this location.
	 */
	public double [] getNormal ()
	{
		CADGeomSurface surface = mesh.getGeomSurface();
		surface.setParameter(param[0], param[1]);
		return surface.normal();
	}
	
	/**
	 * Get the 1D reference of this node.
	 *
	 * @return the 1D reference of this node.
	 */
	public int getRef()
	{
		return ref1d;
	}
	
	/**
	 * Set the 1D reference of this node.
	 *
	 * @param l  the 1D reference of this node.
	 */
	public void setRef(int l)
	{
		ref1d = l;
	}
	
	public int getLabel()
	{
		return label;
	}
	
	public void setLabel(int l)
	{
		label = l;
	}
	
	public Object getLink()
	{
		return link;
	}
	
	public void setLink(Object o)
	{
		link = o;
	}
	
	public void setReadable(boolean r)
	{
		readable = r;
	}
	
	public void setWritable(boolean w)
	{
		writable = w;
	}
	
	public boolean isReadable()
	{
		return readable;
	}
	
	public boolean isWritable()
	{
		return writable;
	}
	
	/**
	 * Add this Vertex to the quadtree.
	 */
	public void addToQuadTree()
	{
		if (logger.isDebugEnabled())
			logger.debug("Inserted point: "+this);
		mesh.quadtree.add(this);
	}
	
	/**
	 * Remove this Vertex from the quadtree.
	 */
	public void removeFromQuadTree()
	{
		if (logger.isDebugEnabled())
			logger.debug("Point removed: "+this);
		mesh.quadtree.remove(this);
	}
	
	/**
	 * Return a triangle containing this point.
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
	public OTriangle2D getSurroundingOTriangle()
	{
		if (logger.isDebugEnabled())
			logger.debug("Searching for the triangle surrounding "+this);
		Triangle.listLock();
		Triangle t = (Triangle) mesh.quadtree.getNearestVertex(this).link;
		OTriangle2D start = new OTriangle2D(t, 0);
		OTriangle2D current = getSurroundingOTriangleStart(start);
		if (current == null)
		{
			// First, try with neighbours
			for (Iterator it = Triangle.getTriangleListIterator(); it.hasNext(); )
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
						current = getSurroundingOTriangleStart(start);
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
				current = getSurroundingOTriangleStart(start);
				if (current != null)
					break;
			}
		}
		Triangle.listRelease();
		assert current != null;
		return current;
	}
	
	private OTriangle2D getSurroundingOTriangleStart(OTriangle2D current)
	{
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
			if (current.hasAttributes(OTriangle.BOUNDARY))
				return null;
			current.symOTri();
			redo = true;
		}
		else if (d == Vertex.outer)
		{
			current.prevOTri();
			if (current.hasAttributes(OTriangle.BOUNDARY))
				return null;
			current.symOTri();
			redo = true;
		}
		else if (a == Vertex.outer)
		{
			if (current.hasAttributes(OTriangle.BOUNDARY))
				return null;
			current.symOTri();
			redo = true;
		}
		//  Orient triangle so that point is to the left.  Apex may
		//  be Vertex.outer again, but this is case 3 above.
		if (onLeft(current.origin(), current.destination()) < 0L)
		{
			if (current.hasAttributes(OTriangle.BOUNDARY))
				return null;
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
			if (a == Vertex.outer)
				break;
			if (current.tri.isListed() || current.hasAttributes(OTriangle.BOUNDARY))
				return null;
			current.tri.listCollect();
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
			else if (d1 < 0L)
				current.prevOTriDest();         // (ad*)
			else if (d2 < 0L)
				current.nextOTriOrigin();       // (oa*)
			else
				//  d1 >= 0 && d2 >= 0.  
				break;
			o = current.origin();
			d = current.destination();
			a = current.apex();
		}
		if (logger.isDebugEnabled())
			logger.debug("Found: "+current);
		return current;
	}
	
	/**
	 * Get the list of adjacent vertices.
	 * Note: this method is meant to deal with non-manifold meshes.
	 *
	 * @return the list of adjacent vertices.
	 */
	public ArrayList getNeighboursNodes()
	{
		ArrayList ret = new ArrayList();
		HashSet nodes = new HashSet();
		OTriangle ot = new OTriangle();
		for (Iterator it = getNeighboursTriangles().iterator(); it.hasNext(); )
		{
			Triangle t = (Triangle) it.next();
			ot.bind(t);
			if (ot.origin() != this)
				ot.nextOTri();
			if (ot.origin() != this)
				ot.nextOTri();
			assert ot.origin() == this : this+" not in "+ot;
			Vertex d = ot.destination();
			if (!nodes.contains(d))
			{
				nodes.add(d);
				ret.add(d);
			}
			Vertex a = ot.apex();
			if (!nodes.contains(a))
			{
				nodes.add(a);
				ret.add(a);
			}
		}
		return ret;
	}
	
	private ArrayList getNeighboursTriangles()
	{
		ArrayList tri = new ArrayList();
		HashSet triSet = new HashSet();
		assert link != null : this;
		if (link instanceof Triangle)
			appendNeighboursTri((Triangle) link, tri, triSet);
		else
		{
			// Non-manifold vertex
			logger.debug("Non-manifold vertex: "+this);
			Triangle [] t = (Triangle []) link;
			for (int i = 0; i < t.length; i++)
				appendNeighboursTri(t[i], tri, triSet);
		}
		return tri;
	}
	
	private void appendNeighboursTri(Triangle tri, ArrayList ret, HashSet triSet)
	{
		assert tri.vertex[0] == this || tri.vertex[1] == this || tri.vertex[2] == this;
		OTriangle ot = new OTriangle(tri, 0);
		if (ot.origin() != this)
			ot.nextOTri();
		if (ot.origin() != this)
			ot.nextOTri();
		assert ot.origin() == this : this+" not in "+ot;
		Vertex d = ot.destination();
		do
		{
			ot.nextOTriOriginLoop();
			assert ot.origin() == this : ot+" should originate from "+this;
			Triangle t = ot.getTri();
			if (!t.isOuter())
				ret.add(t);
		}
		while (ot.destination() != d);
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
	
	/**
	 * Returns the distance in 3D space.
	 *
	 * @param end  the node to which distance is computed.
	 * @return the distance to <code>end</code>.
	 **/
	public double distance3D(Vertex end)
	{
		double x = param[0] - end.param[0];
		double y = param[1] - end.param[1];
		double z = param[2] - end.param[2];
		return Math.sqrt(x*x+y*y+z*z);
	}
	
	/**
	 * Returns the angle at which a segment is seen.
	 *
	 * @param n1  first node
	 * @param n2  second node
	 * @return the angle at which the segment is seen.
	 **/
	public double angle3D(Vertex n1, Vertex n2)
	{
		double normPn1 = distance3D(n1);
		double normPn2 = distance3D(n2);
		if ((normPn1 == 0.0) || (normPn2 == 0.0))
			return 0.0;
		double normPn3 = n1.distance3D(n2);
		double mu, alpha;
		if (normPn1 < normPn2)
		{
			double temp = normPn1;
			normPn1 = normPn2;
			normPn2 = temp;
		}
		if (normPn2 < normPn3)
			mu = normPn2 - (normPn1 - normPn3);
		else
			mu = normPn3 - (normPn1 - normPn2);
		alpha = 2.0 * Math.atan(Math.sqrt(
			((normPn1-normPn2)+normPn3)*mu/
				((normPn1+(normPn2+normPn3))*((normPn1-normPn3)+normPn2))
		));
		return alpha;
	}
	
	/**
	 * Returns the outer product of two vectors.  This method
         * computes the outer product of two vectors starting from
         * the current vertex.
	 *
	 * @param n1  end point of the first vector
	 * @param n2  end point of the second vector
	 * @return the outer product of the two vectors
	 **/
	public double [] outer3D(Vertex n1, Vertex n2)
	{
		double [] vect1 = new double[3];
		double [] vect2 = new double[3];
		for (int i = 0; i < 3; i++)
		{
			vect1[i] = n1.param[i] - param[i];
			vect2[i] = n2.param[i] - param[i];
		}
		return Matrix3D.prodVect3D(vect1, vect2);
	}
	
	/* Unused
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
	*/
	
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
	private Vertex circumcenter(Vertex v1, Vertex v2, Vertex v3)
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
		// Special case when vc1, vc2 and va3 are aligned
		if (va3.onLeft(vc1, vc2) == 0L)
		{
			if (onLeft(vc1, vc2) == 0L)
			{
				long l1 = vc1.distance2(vc2);
				return (distance2(vc1) < l1 && distance2(vc2) < l1 && va3.distance2(vc1) < l1 && va3.distance2(vc2) < l1);
			}
			if (vc1.onLeft(va3, this) >= 0L || vc2.onLeft(va3, this) <= 0L)
				return false;
			long l1 = vc1.distance2(vc2);
			return (va3.distance2(vc1) < l1 && va3.distance2(vc2) < l1);
		}
		// Do not swap if triangles are inverted in 2d space
		if (vc1.onLeft(va3, this) >= 0L || vc2.onLeft(va3, this) <= 0L)
			return false;
		
		try {
			Vertex C3 = va3.circumcenter(vc1, vc2, va3);
			double ret =
				mesh.compGeom().distance(C3, this, va3) /
				mesh.compGeom().distance(C3, va3, va3);
			Vertex C0 = circumcenter(vc1, vc2, va3);
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
			Vertex C3 = vc1.circumcenter(this, va3, vc1);
			double ret =
				mesh.compGeom().distance(C3, vc2, vc1) /
				mesh.compGeom().distance(C3, vc1, vc1);
			Vertex C0 = circumcenter(this, va3, vc1);
			ret +=
				mesh.compGeom().distance(C0, vc2, vc2) /
				mesh.compGeom().distance(C0, vc1, vc2);
			return (ret > 2.0);
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
		
		//  Add a 0.5 factor so that edges are swapped only if
		//  there is a significant gain.
		return (mesh.compGeom().distance(va3, this, vc1) +
		        mesh.compGeom().distance(va3, this, vc2) > 0.5 * (
		        mesh.compGeom().distance(vc1, vc2, va3) +
		        mesh.compGeom().distance(vc1, vc2, this)));
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
	
        /**
         * Get the 2D Riemannian metrics at this point.  This metrics
         * is computed and then stored into a private instance member.
         * This cached value can be discarded by calling {@link clearMetrics}.
         *
         * @param surf  the geometric  surface on which the current
         *              point is located
         * @return the 2D Riemannian metrics at this point.
         */
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
	
        /**
         * Clear the 2D Riemannian metrics at this point.
         */
	public void clearMetrics()
	{
		m2 = null;
	}
	
        /**
         * Check whether this vertex can be modified.
         *
         * @return <code>true</code> if this vertex can be modified,
         * <code>false</otherwise>.
         */
	public boolean isMutable()
	{
		return ref1d <= 0;
	}
	
	/**
	 * Returns the discrete Gaussian curvature and the mean normal.
         * These discrete operators are described in "Discrete
         * Differential-Geometry Operators for Triangulated
         * 2-Manifolds", Mark Meyer, Mathieu Desbrun, Peter Schröder,
         * and Alan H. Barr.
         *   http://www.cs.caltech.edu/~mmeyer/Publications/diffGeomOps.pdf
         *   http://www.cs.caltech.edu/~mmeyer/Publications/diffGeomOps.pdf
         * Note: on a sphere, the Gaussian curvature is very accurate,
         *       but not the mean curvature.
         *       Guoliang Xu suggests improvements in his papers
         *           http://lsec.cc.ac.cn/~xuguo/xuguo3.htm
	 */
	public double discreteCurvatures(double [] meanNormal)
	{
		for (int i = 0; i < 3; i++)
			meanNormal[i] = 0.0;
		assert link instanceof Triangle;
		OTriangle ot = new OTriangle((Triangle) link, 0);
		if (ot.origin() != this)
			ot.nextOTri();
		if (ot.origin() != this)
			ot.nextOTri();
		assert ot.origin() == this;
		double [] vect1 = new double[3];
		double [] vect2 = new double[3];
		double [] vect3 = new double[3];
		double [] p0 = param;
		double mixed = 0.0;
		double gauss = 0.0;
		Vertex d = ot.destination();
		do
		{
			ot.nextOTriOriginLoop();
			if (ot.hasAttributes(OTriangle.BOUNDARY))
			{
				// FIXME: what to do when a boundary
				// is encountered?  For now, return
				// a null vector.
				for (int i = 0; i < 3; i++)
					meanNormal[i] = 0.0;
				return 0.0;
			}
			if (ot.hasAttributes(OTriangle.OUTER))
				continue;
			double [] p1 = ot.destination().getUV();
			double [] p2 = ot.apex().getUV();
			vect1[0] = p1[0] - p0[0];
			vect1[1] = p1[1] - p0[1];
			vect1[2] = p1[2] - p0[2];
			vect2[0] = p2[0] - p1[0];
			vect2[1] = p2[1] - p1[1];
			vect2[2] = p2[2] - p1[2];
			vect3[0] = p0[0] - p2[0];
			vect3[1] = p0[1] - p2[1];
			vect3[2] = p0[2] - p2[2];
			double c12 = Matrix3D.prodSca(vect1, vect2);
			double c23 = Matrix3D.prodSca(vect2, vect3);
			double c31 = Matrix3D.prodSca(vect3, vect1);
			// Override vect2
			Matrix3D.prodVect3D(vect1, vect3, vect2);
			double area = 0.5 * Matrix3D.norm(vect2);
			if (c31 > 0.0)
				mixed += 0.5 * area;
			else if (c12 > 0.0 || c23 > 0.0)
				mixed += 0.25 * area;
			else
			{
				// Non-obtuse triangle
				if (area > 0.0 && area > - 1.e-6 * (c12+c23))
					mixed -= 0.125 * 0.5 * (c12 * Matrix3D.prodSca(vect3, vect3) + c23 * Matrix3D.prodSca(vect1, vect1)) / area;
			}
			gauss += Math.abs(Math.atan2(2.0 * area, -c31));
			for (int i = 0; i < 3; i++)
				meanNormal[i] += 0.5 * (c12 * vect3[i] - c23 * vect1[i]) / area;
		}
		while (ot.destination() != d);
		for (int i = 0; i < 3; i++)
			meanNormal[i] /= 2.0 * mixed;
		// Discrete gaussian curvature
		return (2.0 * Math.PI - gauss) / mixed;
	}
	
	/**
	 * Compute the discrete local frame at this vertex.
         * These discrete operators are described in "Discrete
         * Differential-Geometry Operators for Triangulated
         * 2-Manifolds", Mark Meyer, Mathieu Desbrun, Peter Schröder,
         * and Alan H. Barr.
         *   http://www.cs.caltech.edu/~mmeyer/Publications/diffGeomOps.pdf
	 */
	public boolean discreteCurvatureDirections(double [] normal, double[] t1, double [] t2)
	{
		double Kg = discreteCurvatures(normal);
		double n = Matrix3D.norm(normal);
		double Kh = 0.5 * n;
		if (n < 1.e-6)
		{
			// Either this is a saddle point, or surface is
			// planar at this point.  Compute surface normal
			// by averaging triangle normals.
			if (!discreteAverageNormal(normal))
				return false;
			Kh = 0.0;
		}
		else
		{
			for (int i = 0; i < 3; i++)
				normal[i] /= n;
		}
		// We are looking for eigenvectors of the curvature
		// matrix B(a b; b c).  
		// Firstly set (t1,t2) to be an arbitrary map of the
		// tangent plane.
		for (int i = 0; i < 3; i++)
			t2[i] = 0.0;
		if (Math.abs(normal[0]) < Math.abs(normal[1]))
			t2[0] = 1.0;
		else
			t2[1] = 1.0;
		Matrix3D.prodVect3D(normal, t2, t1);
		n = Matrix3D.norm(t1);
		if (n < 1.e-6)
			return false;
		for (int i = 0; i < 3; i++)
			t1[i] /= n;
		Matrix3D.prodVect3D(normal, t1, t2);
		// To compute B eigenvectors, we search for the minimum of
		//   E(a,b,c) = sum omega_ij (T(d_ij) B d_ij - kappa_ij)^2
		// d_ij is the unit direction of the edge ij in the tangent
		// plane, so it can be written in the (t1,t2) basis:
		//   d_ij = d1_ij t1 + d2_ij t2
		// Then
		//   T(d_ij) B d_ij = a d1_ij^2 + 2b d1_ij d2_ij + c d2_ij^2
		// We solve grad E = 0
		//   dE/da = 2 d1_ij^2 (a d1_ij^2 + 2b d1_ij d2_ij + c d2_ij^2 - kappa_ij)
		//   dE/db = 4 d1_ij d2_ij (a d1_ij^2 + 2b d1_ij d2_ij + c d2_ij^2 - kappa_ij)
		//   dE/dc = 2 d2_ij^2 (a d1_ij^2 + 2b d1_ij d2_ij + c d2_ij^2 - kappa_ij)
		// We may decrease the dimension by using a+c=Kh identity,
		// but we found that Kh is much less accurate than Kg on
		// a sphere, so we do not use this identity.
		//   (1/2) grad E = G (a b c) - H
		OTriangle ot = new OTriangle((Triangle) link, 0);
		if (ot.origin() != this)
			ot.nextOTri();
		if (ot.origin() != this)
			ot.nextOTri();
		assert ot.origin() == this;
		double [] vect1 = new double[3];
		double [] vect2 = new double[3];
		double [] vect3 = new double[3];
		double [] g0 = new double[3];
		double [] g1 = new double[3];
		double [] g2 = new double[3];
		double [] h = new double[3];
		for (int i = 0; i < 3; i++)
			g0[i] = g1[i] = g2[i] = h[i] = 0.0;
		Vertex d = ot.destination();
		do
		{
			ot.nextOTriOriginLoop();
			if (ot.hasAttributes(OTriangle.OUTER))
				continue;
			double [] p1 = ot.destination().getUV();
			double [] p2 = ot.apex().getUV();
			for (int i = 0; i < 3; i++)
			{
				vect1[i] = p1[i] - param[i];
				vect2[i] = p2[i] - p1[i];
				vect3[i] = param[i] - p2[i];
			}
			double c12 = Matrix3D.prodSca(vect1, vect2);
			double c23 = Matrix3D.prodSca(vect2, vect3);
			double c31 = Matrix3D.prodSca(vect3, vect1);
			// Override vect2
			Matrix3D.prodVect3D(vect1, vect3, vect2);
			double area = 0.5 * Matrix3D.norm(vect2);
			double len2 = Matrix3D.prodSca(vect1, vect1);
			if (len2 < 1.e-12)
				continue;
			double kappa = 2.0 * Matrix3D.prodSca(vect1, normal) / len2;
			double d1 = Matrix3D.prodSca(vect1, t1);
			double d2 = Matrix3D.prodSca(vect1, t2);
			n = Math.sqrt(d1*d1 + d2*d2);
			if (n < 1.e-6)
				continue;
			d1 /= n;
			d2 /= n;
			double omega = 0.5 * (c12 * Matrix3D.prodSca(vect3, vect3) + c23 * Matrix3D.prodSca(vect1, vect1)) / area;
			g0[0] += omega * d1 * d1 * d1 * d1;
			g0[1] += omega * 2.0 * d1 * d1 * d1 * d2;
			g0[2] += omega * d1 * d1 * d2 * d2;
			g1[1] += omega * 4.0 * d1 * d1 * d2 * d2;
			g1[2] += omega * 2.0 * d1 * d2 * d2 * d2;
			g2[2] += omega * d2 * d2 * d2 * d2;
			h[0] += omega * kappa * d1 * d1;
			h[1] += omega * kappa * 2.0 * d1 * d2;
			h[2] += omega * kappa * d2 * d2;
		}
		while (ot.destination() != d);
		g1[0] = g0[1];
		g2[0] = g0[2];
		g2[1] = g1[2];
		Metric3D G = new Metric3D(g0, g1, g2);
		Metric3D Ginv = G.inv();
		if (Ginv == null)
			return false;
		double [] abc = Ginv.apply(h);
		// We can eventually compute eigenvectors of B(a b; b c).  
		// Let first compute the eigenvector associated to K1
		double e1, e2;
		if (Math.abs(abc[1]) < 1.e-10)
		{
			if (Math.abs(abc[0]) < Math.abs(abc[2]))
			{
				e1 = 0.0;
				e2 = 1.0;
			}
			else
			{
				e1 = 1.0;
				e2 = 0.0;
			}
		}
		else
		{
			e2 = 1.0;
			double delta = Math.sqrt((abc[0]-abc[2])*(abc[0]-abc[2]) + 4.0*abc[1]*abc[1]);
			double K1;
			if (abc[0] + abc[2] < 0.0)
				K1 = 0.5 * (abc[0] + abc[2] - delta);
			else
				K1 = 0.5 * (abc[0] + abc[2] + delta);
			e1 = (K1 - abc[0]) / abc[1];
			n = Math.sqrt(e1 * e1 + e2 * e2);
			e1 /= n;
			e2 /= n;
		}
		for (int i = 0; i < 3; i++)
		{
			double temp = e1 * t1[i] + e2 * t2[i];
			t2[i] = - e2 * t1[i] + e1 * t2[i];
			t1[i] = temp;
		}
		return true;
	}
	
	// Common area-weighted mean normal
	private boolean discreteAverageNormal(double [] normal)
	{
		for (int i = 0; i < 3; i++)
			normal[i] = 0.0;
		assert link instanceof Triangle;
		OTriangle ot = new OTriangle((Triangle) link, 0);
		if (ot.origin() != this)
			ot.nextOTri();
		if (ot.origin() != this)
			ot.nextOTri();
		assert ot.origin() == this;
		Vertex d = ot.destination();
		do
		{
			ot.nextOTriOriginLoop();
			if (ot.hasAttributes(OTriangle.OUTER))
				continue;
			double area = ot.computeNormal3D();
			double [] nu = ot.getTempVector();
			for (int i = 0; i < 3; i++)
				normal[i] += area * nu[i];
		}
		while (ot.destination() != d);
		double n = Matrix3D.norm(normal);
		if (n < 1.e-6)
			return false;
		for (int i = 0; i < 3; i++)
			normal[i] /= n;
		return true;
	}
	
	public boolean discreteProject(Vertex pt)
	{
		double [] normal = new double[3];
		// TODO: Check why discreteCurvatures(normal) does not work well
		if (!discreteAverageNormal(normal))
			return false;
		// We search for the quadric
		//   F(x,y) = a x^2 + b xy + c y^2 - z
		// which fits best for all neighbour vertices.
		// Firstly set (t1,t2) to be an arbitrary map of the
		// tangent plane.
		double [] t1 = new double[3];
		double [] t2 = new double[3];
		for (int i = 0; i < 3; i++)
			t2[i] = 0.0;
		if (Math.abs(normal[0]) < Math.abs(normal[1]))
			t2[0] = 1.0;
		else
			t2[1] = 1.0;
		Matrix3D.prodVect3D(normal, t2, t1);
		double n = Matrix3D.norm(t1);
		if (n < 1.e-6)
			return false;
		for (int i = 0; i < 3; i++)
			t1[i] /= n;
		Matrix3D.prodVect3D(normal, t1, t2);
		// Transformation matrix
		Matrix3D Pinv = new Matrix3D(t1, t2, normal);
		Matrix3D P = (Matrix3D) Pinv.transp();
		OTriangle ot = new OTriangle((Triangle) link, 0);
		if (ot.origin() != this)
			ot.nextOTri();
		if (ot.origin() != this)
			ot.nextOTri();
		assert ot.origin() == this;
		double [] vect1 = new double[3];
		double [] g0 = new double[3];
		double [] g1 = new double[3];
		double [] g2 = new double[3];
		double [] h = new double[3];
		double dmin = Double.MAX_VALUE;
		for (int i = 0; i < 3; i++)
			g0[i] = g1[i] = g2[i] = h[i] = 0.0;
		Vertex d = ot.destination();
		do
		{
			ot.nextOTriOriginLoop();
			if (ot.destination() == Vertex.outer)
				continue;
			double [] p1 = ot.destination().getUV();
			for (int i = 0; i < 3; i++)
				vect1[i] = p1[i] - param[i];
			dmin = Math.min(dmin, Matrix3D.norm(vect1));
			// Find coordinates in the local frame (t1,t2,n)
			double [] loc = P.apply(vect1);
			h[0] += loc[2] * loc[0] * loc[0];
			h[1] += loc[2] * loc[0] * loc[1];
			h[2] += loc[2] * loc[1] * loc[1];
			g0[0] += loc[0] * loc[0] * loc[0] * loc[0];
			g0[1] += loc[0] * loc[0] * loc[0] * loc[1];
			g0[2] += loc[0] * loc[0] * loc[1] * loc[1];
			g1[2] += loc[0] * loc[1] * loc[1] * loc[1];
			g2[2] += loc[1] * loc[1] * loc[1] * loc[1];
		}
		while (ot.destination() != d);
		g1[1] = g0[2];
		g1[0] = g0[1];
		g2[0] = g0[2];
		g2[1] = g1[2];
		Metric3D G = new Metric3D(g0, g1, g2);
		Metric3D Ginv = G.inv();
		if (Ginv == null)
			return false;
		double [] abc = Ginv.apply(h);
		// Now project pt onto this quadric
		for (int i = 0; i < 3; i++)
			vect1[i] = pt.param[i] - param[i];
		double [] loc = P.apply(vect1);
		loc[2] = abc[0] * loc[0] * loc[0] + abc[1] * loc[0] * loc[1] + abc[2] * loc[1] * loc[1];
		double [] glob = Pinv.apply(loc);
		pt.moveTo(param[0] + glob[0], param[1] + glob[1], param[2] + glob[2]);
		return true;
	}
	
	public String toString ()
	{
		if (this == Vertex.outer)
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
