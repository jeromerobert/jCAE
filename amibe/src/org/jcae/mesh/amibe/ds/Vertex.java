/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005, by EADS CRC
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

package org.jcae.mesh.amibe.ds;

import java.util.logging.Logger;
import org.jcae.mesh.amibe.traits.VertexTraitsBuilder;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.metrics.Location;
import java.util.Iterator;
import java.io.Serializable;
import java.util.Collection;
import java.util.TreeSet;

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
 * There is a special vertex, {@link Mesh#outerVertex}, which represents a
 * vertex at infinite.  It is used to create exterior triangles.
 * </p>
 *
 * <p>
 * Each vertex has a pointer to an incident <code>Triangle</code>,
 * which allows to find any other incident <code>VirtualHalfEdge</code> or
 * <code>Triangle</code>.  For non-manifold vertices, this link points
 * to a <code>Triangle []</code> array, which can be used to retrieve
 * all incident triangles through their adjacency relations.
 * </p>
 */
public class Vertex implements Location, Serializable
{
	private static final long serialVersionUID = 8049983674054731722L;
	private static final Logger logger=Logger.getLogger(Vertex.class.getName());
	
	//  User-defined traits.  They are currently unused.
	//protected final Traits traits;
	/**
	 * 2D or 3D coordinates.
	 */
	protected final double [] param;
	//  ref1d > 0: link to the geometrical node
	//  ref1d = 0: inner node
	//  ref1d < 0: node on an inner boundary  (FIXME: unused for now)
	protected int ref1d;

	//  link can be either:
	//    1. an Triangle, for manifold vertices
	//    2. an Object[2] array, zhere
	//         0: list of head triangles
	//         1: list of incident wires
	protected Object link;
	
	private boolean mutable = true;

	// Used in OEMM
	private int label;
	private boolean readable = true;
	private boolean writable = true;
	
	/**
	 * Constructor.
	 */
	protected Vertex(VertexTraitsBuilder vtb)
	{
		/*if (vtb != null)
			traits = vtb.createTraits();
		else
			traits = null;*/
		param = new double[2];
	}

	/**
	 * Create a Vertex for a 3D mesh.
	 *
	 * @param vtb  traits builder
	 * @param x  first coordinate.
	 * @param y  second coordinate.
	 * @param z  third coordinate.
	 */
	public Vertex(VertexTraitsBuilder vtb, double x, double y, double z)
	{
		/*if (vtb != null)
			traits = vtb.createTraits();
		else
			traits = null;*/
		param = new double[3];
		param[0] = x;
		param[1] = y;
		param[2] = z;
	}
	
	/**
	 * Copy all attributes from another Vertex.
	 *
	 * @param that  the Vertex to be copied.
	 */
	public final void copy(Vertex that)
	{
		assert that.param.length == param.length;
		System.arraycopy(that.param, 0, param, 0, param.length);
		link  = that.link;
		ref1d = that.ref1d;
		label = that.label;
		readable = that.readable;
		writable = that.writable;
		mutable = that.mutable;
	}
	
	/**
	 * Gets 1D reference of this node.
	 * Reference is positive for node of free edges, zero for internal points,
	 * and positive for nodes of group borders.
	 * @return 1D reference of this node
	 */
	public final int getRef()
	{
		return ref1d;
	}
	
	/**
	 * Sets 1D reference of this node.
	 *
	 * @param l  1D reference of this node
	 */
	public final void setRef(int l)
	{
		ref1d = l;
	}
	
	/**
	 * Gets coordinates of this vertex.
	 *
	 * @return coordinates of this vertex
	 */
	public final double [] getUV ()
	{
		return param;
	}
	
	public void moveTo(double u, double v)
	{
		throw new RuntimeException();
	}

	/**
	 * Sets 3D coordinates of this vertex.
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
	}

	/**
	 * Returns the squared distance in 3D space.
	 *
	 * @param end  the node to which distance is computed.
	 * @return the squared distance to <code>end</code>.
	 **/
	public final double sqrDistance3D(Vertex end)
	{
		double x = param[0] - end.param[0];
		double y = param[1] - end.param[1];
		double z = param[2] - end.param[2];
		return x*x+y*y+z*z;
	}
	
	/**
	 * Returns the distance in 3D space.
	 *
	 * @param end  the node to which distance is computed.
	 * @return the distance to <code>end</code>.
	 **/
	public final double distance3D(Vertex end)
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
	public final double angle3D(Vertex n1, Vertex n2)
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
	 * @param work1  double[3] temporary array
	 * @param work2  double[3] temporary array
	 * @param ret array which will store the outer product of the two vectors
	 */
	final void outer3D(Vertex n1, Vertex n2, double [] work1, double [] work2, double [] ret)
	{
		for (int i = 0; i < 3; i++)
		{
			work1[i] = n1.param[i] - param[i];
			work2[i] = n2.param[i] - param[i];
		}
		Matrix3D.prodVect3D(work1, work2, ret);
	}
	
	/**
	 * Get node label.
	 *
	 * @return node label.
	 */
	public final int getLabel()
	{
		return label;
	}
	
	/**
	 * Set node label.
	 *
	 * @param l  node label.
	 */
	public final void setLabel(int l)
	{
		label = l;
	}
	
	/**
	 * Get a finite element containing this Vertex.
	 *
	 * @return a <code>Triangle</code> instance for manifold vertices,
	 * and a <code>Triangle []</code> array otherwise.
	 */
	public final Object getLink()
	{
		return link;
	}
	
	/**
	 * Set link to a finite element containing this Vertex.
	 *
	 * @param o  object linked to this Vertex.
	 */
	public final void setLink(Object o)
	{
		link = o;
	}
	
	/**
	 * Helper methods to avoid duplicated code.  Returns an edge starting from
	 * current vertex.
	 */
	private AbstractHalfEdge getIncidentAbstractHalfEdge()
	{
		assert link instanceof Triangle;
		return getIncidentAbstractHalfEdge((Triangle) link, null);
	}

	public final AbstractHalfEdge getIncidentAbstractHalfEdge(Triangle t, AbstractHalfEdge ot)
	{
		ot = t.getAbstractHalfEdge(ot);
		if (ot.destination() == this)
			ot = ot.next();
		else if (ot.apex() == this)
			ot = ot.prev();
		assert ot.origin() == this;
		return ot;
	}

	public final void setReadable(boolean r)
	{
		readable = r;
	}
	
	public final void setWritable(boolean w)
	{
		writable = w;
	}
	
	public final boolean isReadable()
	{
		return readable;
	}
	
	public boolean isWritable()
	{
		return writable;
	}
	
	/**
	 * Tells whether this vertex is manifold.
	 *
	 * @return <code>true</code> if vertex is manifold, <code>false</code> otherwise
	 */
	public final boolean isManifold()
	{
		return link instanceof Triangle;
	}
	
	private abstract class NeighbourIterator<T> implements Iterator<T>
	{
		private AbstractHalfEdge next;
		private final Vertex start;
		private boolean started;
		private NeighbourIterator(Triangle tri)
		{
			next = getIncidentAbstractHalfEdge(tri, null);
			start = next.destination();
		}
		public abstract T next();
		public final boolean hasNext()
		{
			return !started || next.apex() != start;
		}
		final AbstractHalfEdge advance()
		{
			if (started)
				next = next.nextOriginLoop();
			else
				started = true;
			return next;
		}
		public final void remove()
		{
			throw new RuntimeException("NeighbourIterator.remove not implemented!");
		}
	}

	private final class NeighbourIteratorVertex extends NeighbourIterator<Vertex>
	{
		private NeighbourIteratorVertex(Triangle tri)
		{
			super(tri);
		}
		@Override
		public final Vertex next()
		{
			return advance().destination();
		}
	}

	private final class NeighbourIteratorAbstractHalfEdge extends NeighbourIterator<AbstractHalfEdge>
	{
		private NeighbourIteratorAbstractHalfEdge(Triangle tri)
		{
			super(tri);
		}
		@Override
		public final AbstractHalfEdge next()
		{
			return advance();
		}
	}

	private final class NeighbourIteratorTriangle extends NeighbourIterator<Triangle>
	{
		private NeighbourIteratorTriangle(Triangle tri)
		{
			super(tri);
		}
		@Override
		public final Triangle next()
		{
			return advance().getTri();
		}
	}

	private final class EmptyIterator<E> implements Iterator<E>
	{
		public final E next()
		{
			throw new RuntimeException();
		}
		public final boolean hasNext()
		{
			return false;
		}
		public final void remove()
		{
			throw new RuntimeException("NeighbourIterator.remove not implemented!");
		}
	}

	private final class ChainIterator<E> implements Iterator<E>
	{
		private int index;
		private Iterator<E> current;
		private final Iterator<E>[] iterators;
		private ChainIterator(Iterator<E>[] it)
		{
			iterators = it;
			// Backward processing to avoid comparison with iterators.length
			index   = iterators.length - 1;
			current = iterators[index];
		}
		public final E next()
		{
			if (!current.hasNext())
			{
				index--;
				current = iterators[index];
			}
			return current.next();
		}
		public final boolean hasNext()
		{
			return index > 0 || current.hasNext();
		}
		public final void remove()
		{
			throw new RuntimeException("NeighbourIterator.remove not implemented!");
		}
	}

	/**
	 * Return an iterator over adjacent vertices.
	 * Note: this method works also with non-manifold meshes.
	 *
	 * @return an iterator over adjacent vertices
	 */
	public final Iterator<Vertex> getNeighbourIteratorVertex()
	{
		//if the vertex has no link then we return empty list
		if (link == null)
			return new EmptyIterator<Vertex>();
		if (link instanceof Triangle)
			return new NeighbourIteratorVertex((Triangle) link);
		// Non-manifold vertex
		logger.fine("Non-manifold vertex: "+this);
		Triangle [] t = (Triangle []) link;
		Iterator<Vertex> [] iterators = new NeighbourIteratorVertex[t.length];
		for (int i = 0; i < t.length; i++)
			iterators[i] = new NeighbourIteratorVertex(t[i]);
		return new ChainIterator<Vertex>(iterators);
	}
	
	/**
	 * Return an iterator over incident triangles.
	 * Note: this method works also with non-manifold meshes.
	 *
	 * @return an iterator over incident triangles
	 */
	public final Iterator<Triangle> getNeighbourIteratorTriangle()
	{
		//if the vertex has no link then we return empty list
		if (link == null)
			return new EmptyIterator<Triangle>();
		if (link instanceof Triangle)
			return new NeighbourIteratorTriangle((Triangle) link);
		// Non-manifold vertex
		logger.fine("Non-manifold vertex: "+this);
		Triangle [] t = (Triangle []) link;
		Iterator<Triangle> [] iterators = new NeighbourIteratorTriangle[t.length];
		for (int i = 0; i < t.length; i++)
			iterators[i] = new NeighbourIteratorTriangle(t[i]);
		return new ChainIterator<Triangle>(iterators);
	}
	
	/**
	 * Return an iterator over incident half-edges.
	 * Note: this method works also with non-manifold meshes.
	 *
	 * @return an iterator over incident half-edges.
	 */
	public Iterator<AbstractHalfEdge> getNeighbourIteratorAbstractHalfEdge()
	{
		//if the vertex has no link then we return empty list
		if (link == null)
			return new EmptyIterator<AbstractHalfEdge>();
		if (link instanceof Triangle)
			return new NeighbourIteratorAbstractHalfEdge((Triangle) link);
		// Non-manifold vertex
		logger.fine("Non-manifold vertex: "+this);
		Triangle [] t = (Triangle []) link;
		Iterator<AbstractHalfEdge> [] iterators = new NeighbourIteratorAbstractHalfEdge[t.length];
		for (int i = 0; i < t.length; i++)
			iterators[i] = new NeighbourIteratorAbstractHalfEdge(t[i]);
		return new ChainIterator<AbstractHalfEdge>(iterators);
	}
	
	/**
	 * Check whether this vertex can be modified.
	 *
	 * @return <code>true</code> if this vertex can be modified,
	 * <code>false</otherwise>.
	 */
	public final boolean isMutable()
	{
		return mutable;
	}

	public final void setMutable(boolean m)
	{
		mutable = m;
	}

	/** Fast setMutable for jython */
	public static void setMutable(Iterable<Vertex> vertices, boolean m)
	{
		for(Vertex v:vertices)
			v.mutable = m;
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
	private double discreteCurvatures(double [] meanNormal)
	{
		for (int i = 0; i < 3; i++)
			meanNormal[i] = 0.0;
		double [] vect1 = new double[3];
		double [] vect2 = new double[3];
		double [] vect3 = new double[3];
		double [] p0 = param;
		double mixed = 0.0;
		double gauss = 0.0;
		AbstractHalfEdge ot = getIncidentAbstractHalfEdge();
		Vertex d = ot.destination();
		do
		{
			ot = ot.nextOriginLoop();
			if (ot.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			if (ot.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
			{
				// FIXME: what to do when a boundary
				// is encountered?  For now, return
				// a null vector.
				for (int i = 0; i < 3; i++)
					meanNormal[i] = 0.0;
				return 0.0;
			}
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
	@SuppressWarnings("unused")
	private boolean discreteCurvatureDirections(double [] normal, double[] t1, double [] t2)
	{
		if (!computeUnitNormal(normal))
			return false;
		if (!computeTangentPlane(normal, t1, t2))
			return false;

		// To compute B eigenvectors, we search for the minimum of
		//   E(a,b,c) = sum omega_ij (T(d_ij) B d_ij - kappa_ij)^2
		// d_ij is the unit direction of the edge ij in the tangent
		// plane, so it can be written in the (t1,t2) local frame:
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
		double [] vect1 = new double [3];
		if (!findOptimalSolution(normal, t1, t2, vect1))
			return false;
		// We can eventually compute eigenvectors of B(a b; b c).  
		// Let first compute the eigenvector associated to K1
		double e1, e2;
		if (Math.abs(vect1[1]) < 1.e-10)
		{
			if (Math.abs(vect1[0]) < Math.abs(vect1[2]))
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
			double delta = Math.sqrt((vect1[0]-vect1[2])*(vect1[0]-vect1[2]) + 4.0*vect1[1]*vect1[1]);
			double K1;
			if (vect1[0] + vect1[2] < 0.0)
				K1 = 0.5 * (vect1[0] + vect1[2] - delta);
			else
				K1 = 0.5 * (vect1[0] + vect1[2] + delta);
			e1 = (K1 - vect1[0]) / vect1[1];
			double n = Math.sqrt(e1 * e1 + e2 * e2);
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

	public final boolean computeTangentPlane(double [] normal, double[] t1, double [] t2)
	{
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
		return true;
	}
	
	private boolean findOptimalSolution(double [] normal, double[] t1, double [] t2, double [] ret)
	{
		AbstractHalfEdge ot = getIncidentAbstractHalfEdge();
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
			ot = ot.nextOriginLoop();
			if (ot.hasAttributes(AbstractHalfEdge.OUTER))
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
			// Override vect2
			Matrix3D.prodVect3D(vect1, vect3, vect2);
			double area = 0.5 * Matrix3D.norm(vect2);
			double len2 = Matrix3D.prodSca(vect1, vect1);
			if (len2 < 1.e-12)
				continue;
			double kappa = 2.0 * Matrix3D.prodSca(vect1, normal) / len2;
			double d1 = Matrix3D.prodSca(vect1, t1);
			double d2 = Matrix3D.prodSca(vect1, t2);
			double n = Math.sqrt(d1*d1 + d2*d2);
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
		Matrix3D G = new Matrix3D(g0, g1, g2);
		if (!G.inv())
			return false;
		G.apply(h, ret);
		return true;
	}
	
	private boolean computeUnitNormal(double [] normal)
	{
		discreteCurvatures(normal);
		double n = Matrix3D.norm(normal);
		if (n < 1.e-6)
		{
			// Either this is a saddle point, or surface is
			// planar at this point.  Compute surface normal
			// by averaging triangle normals.
			// Unlike discreteCurvatures(), discreteAverageNormal()
			// ensures that normal vector has a unit length.
			if (!discreteAverageNormal(normal))
				return false;
		}
		else
		{
			for (int i = 0; i < 3; i++)
				normal[i] /= n;
		}
		return true;
	}
	
	// Common area-weighted mean normal
	public final boolean discreteAverageNormal(double [] normal)
	{
		for (int i = 0; i < 3; i++)
			normal[i] = 0.0;
		AbstractHalfEdge ot = getIncidentAbstractHalfEdge();
		double [][] temp = new double[3][3];
		Vertex d = ot.destination();
		do
		{
			ot = ot.nextOriginLoop();
			if (ot.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			double area = Matrix3D.computeNormal3D(param, ot.destination().param, ot.apex().param, temp[0], temp[1], temp[2]);
			for (int i = 0; i < 3; i++)
				normal[i] += area * temp[2][i];
		}
		while (ot.destination() != d);
		double n = Matrix3D.norm(normal);
		if (n < 1.e-6)
			return false;
		for (int i = 0; i < 3; i++)
			normal[i] /= n;
		return true;
	}
	
	@Override
	public String toString ()
	{
		StringBuilder r = new StringBuilder("UV:");
		for (int i = 0; i < param.length; i++)
			r.append(" ").append(param[i]);
		if (ref1d != 0)
			r.append(" ref1d: ").append(ref1d);
		r.append(" hash: ").append(hashCode());
		if (label > 0)
			r.append(" label: ").append(label);
		if (link instanceof Triangle)
			r.append(" link: ").append(link.hashCode());
		else if (link instanceof Triangle[])
		{
			Triangle [] list = (Triangle []) link;
			r.append(" link: [");
			if (list.length > 0)
			{
				r.append(list[0].hashCode());
				for (int i = 1; i < list.length; i++)
					r.append(",").append(list[i].hashCode());
			}
			r.append("]");
		}
		if (!readable)
			r.append(" !R");
		if (!writable)
			r.append(" !W");
		if (!mutable)
			r.append(" !M");
		return r.toString();
	}

	/**
	 * Fill the groups collection with the list of groups adjacent to this
	 * vertex
	 */
	public void getGroups(Collection<Integer> groups)
	{
		Iterator<Triangle> it = getNeighbourIteratorTriangle();
		while(it.hasNext())
			groups.add(it.next().getGroupId());
	}
}
