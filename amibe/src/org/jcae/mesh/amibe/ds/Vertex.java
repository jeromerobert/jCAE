/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005, by EADS CRC

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
import org.jcae.mesh.amibe.patch.Vertex2D;
import org.jcae.mesh.amibe.metrics.Metric3D;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.io.Serializable;

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
 * Each vertex has a pointer to an incident <code>Triangle</code>,
 * which allows to find any other incident <code>OTriangle</code> or
 * <code>Triangle</code>.  For non-manifold vertices, this link points
 * to a <code>Triangle []</code> array, which can be used to retrieve
 * all incident triangles through their adjacency relations.
 * </p>
 */
public class Vertex implements Serializable
{
	private static Logger logger = Logger.getLogger(Vertex.class);
	
	/**
	 * 2D or 3D coordinates.
	 */
	protected final double [] param;
	//  link can be either:
	//    1. a Triangle, for manifold vertices
	//    2. an Object[2] array, zhere
	//         0: list of head triangles
	//         1: list of incident wires
	protected Object link;
	
	//  ref1d > 0: link to the geometrical node
	//  ref1d = 0: inner node
	//  ref1d < 0: node on an inner boundary
	//  
	protected int ref1d = 0;
	// Used in OEMM
	protected int label = 0;
	private boolean readable = true;
	private boolean writable = true;
	
	/**
	 * Constructor called by Vertex2D.
	 */
	protected Vertex()
	{
		param = new double[2];
	}

	/**
	 * Create a Vertex for a 3D mesh.
	 *
	 * @param x  first coordinate.
	 * @param y  second coordinate.
	 * @param z  third coordinate.
	 */
	private Vertex(double x, double y, double z)
	{
		param = new double[3];
		param[0] = x;
		param[1] = y;
		param[2] = z;
	}
	
	/**
	 * Create a Vertex for a 3D mesh.
	 *
	 * @param x  first coordinate.
	 * @param y  second coordinate.
	 * @param z  third coordinate.
	 */
	public static Vertex valueOf(double x, double y, double z)
	{
		return new Vertex(x, y, z);
	}
	
	/**
	 * Create a Vertex for a 3D mesh.
	 *
	 * @param p  3d coordinates.
	 */
	public static Vertex valueOf(double [] p)
	{
		Vertex ret;
		if (p.length == 2)
			ret = Vertex2D.valueOf(p[0], p[1]);
		else
			ret = new Vertex(p[0], p[1], p[2]);
		return ret;
	}
	
	/**
	 * Copy all attributes from another Vertex.
	 *
	 * @param that  the Vertex to be copied.
	 */
	public void copy(Vertex that)
	{
		assert that.param.length == param.length;
		for (int i = 0; i < param.length; i++)
			param[i] = that.param[i];
		link  = that.link;
		ref1d = that.ref1d;
		label = that.label;
		readable = that.readable;
		writable = that.writable;
	}
	
	/**
	 * Get coordinates of this Vertex.
	 *
	 * @return the coordinates of this Vertex.
	 */
	public double [] getUV ()
	{
		return param;
	}
	
	/**
	 * Set coordinates of this Vertex.
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
	 * Get 1D reference of this node.
	 *
	 * @return 1D reference of this node.
	 */
	public int getRef()
	{
		return ref1d;
	}
	
	/**
	 * Set 1D reference of this node.
	 *
	 * @param l  1D reference of this node.
	 */
	public void setRef(int l)
	{
		ref1d = l;
	}
	
	/**
	 * Get node label.
	 *
	 * @return node label.
	 */
	public int getLabel()
	{
		return label;
	}
	
	/**
	 * Set node label.
	 *
	 * @param l  node label.
	 */
	public void setLabel(int l)
	{
		label = l;
	}
	
	/**
	 * Get a finite element containing this Vertex.
	 *
	 * @return a <code>Triangle</code> instance for manifold vertices,
	 * and a <code>Triangle []</code> array otherwise.
	 */
	public Object getLink()
	{
		return link;
	}
	
	/**
	 * Set link to a finite element containing this Vertex.
	 *
	 * @param o  object linked to this Vertex.
	 */
	public void setLink(Object o)
	{
		link = o;
	}
	
	/**
	 * Set link to an array of Triangles.  This routine eliminates
	 * duplicates to keep only one Triangle by fan.
	 *
	 * @param triangles  initial set of adjacent triangles.
	 */
	public void setLinkFan(Collection triangles)
	{
		OTriangle ot = new OTriangle();
		ArrayList res = new ArrayList();
		Collection allTriangles = new HashSet();
		for (Iterator it = triangles.iterator(); it.hasNext(); )
		{
			Triangle t = (Triangle) it.next();
			if (allTriangles.contains(t))
				continue;
			allTriangles.add(t);
			res.add(t);
			ot.bind(t);
			if (ot.destination() == this)
				ot.nextOTri();
			else if (ot.apex() == this)
				ot.prevOTri();
			assert ot.origin() == this;
			// Add all triangles of the same fan to allTriangles
			Vertex d = ot.destination();
			do
			{
				ot.nextOTriOriginLoop();
				allTriangles.add(ot.getTri());
			}
			while (ot.destination() != d);
		}
		Triangle [] lArray = new Triangle[res.size()];
		for (int i = 0, n = res.size(); i < n; i++)
			lArray[i] = (Triangle) res.get(i);
		link = lArray;
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
	 * Get the list of adjacent vertices.
	 * Note: this method works also with non-manifold meshes.
	 *
	 * @return the list of adjacent vertices.
	 */
	public Collection getNeighboursNodes()
	{
		Collection ret = new LinkedHashSet();
		if (link instanceof Triangle)
			appendNeighboursTri((Triangle) link, ret);
		else
		{
			// Non-manifold vertex
			logger.debug("Non-manifold vertex: "+this);
			Triangle [] t = (Triangle []) link;
			for (int i = 0; i < t.length; i++)
				appendNeighboursTri(t[i], ret);
		}
		return ret;
	}
	
	private void appendNeighboursTri(Triangle tri, Collection nodes)
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
			if (!ot.hasAttributes(OTriangle.OUTER))
				nodes.add(ot.destination());
			ot.nextOTriOriginLoop();
			assert ot.origin() == this : ot+" should originate from "+this;
		}
		while (ot.destination() != d);
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
		discreteCurvatures(normal);
		double n = Matrix3D.norm(normal);
		if (n < 1.e-6)
		{
			// Either this is a saddle point, or surface is
			// planar at this point.  Compute surface normal
			// by averaging triangle normals.
			if (!discreteAverageNormal(normal))
				return false;
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
	
	/**
	 * Project a point on the approximated surface.  This algorithm is
	 * described by Pascal J. Frey in
	 * <a href="http://www.lis.inpg.fr/pages_perso/attali/DEA-IVR/PAPERS/frey00.ps">About Surface Remeshing</a>.
	 * The idea if to approximate locally the surface by a quadric
	 * <code>F(x,y) = a x^2 + b xy + c y^2 - z</code>.
	 * To that end, the local frame at the current vertex is
	 * computed.  The <code>(x,y)</code> coordinates of neighbour
	 * vertices are computed in that frame, and we search for the quadric
	 * which fits best for all neighbour vertices (in a least squares
	 * sense).  The vertex is then projected onto this quadric.
	 *
	 * Note1: Several improvements exist in the litterature, see eg.
	 * <a href="http://prism.asu.edu/research/data/publications/paper05_cestmubbp.pdf">this paper</a>
	 * by Anshuman Razdan and MyungSoo Bae for a survey of several
	 * methods.
	 * Note2: According to Pascal J. Frey, the key point is to have
	 * reliable input.  We can have good approximation of quadrics
	 * is the normal to the surface is accurate, and normal to the
	 * surface can be approximated accurately if the quadric is
	 * precise.  So we should certainly read normals from a file
	 * if they are available.
	 *
	 * @param pt   point to project on the approximated surface.
	 * @return <code>true</code> if projection has been performed
	 * successfully, <code>false</code> otherwise.
	 */
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
			if (ot.hasAttributes(OTriangle.OUTER))
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
		StringBuffer r = new StringBuffer();
		r.append("UV:");
		for (int i = 0; i < param.length; i++)
			r.append(" "+param[i]);
		if (ref1d != 0)
			r.append(" ref1d: "+ref1d);
		r.append(" hash: "+hashCode());
		if (link != null)
		{
			if (link instanceof Triangle)
				r.append(" link: "+link.hashCode());
			else if (link instanceof Triangle[])
			{
				Triangle [] list = (Triangle []) link;
				r.append(" link: ["+list[0].hashCode());
				for (int i = 1; i < list.length; i++)
					r.append(","+list[i].hashCode());
				r.append("]");
			}
		}
		if (!readable)
			r.append(" !R");
		if (!writable)
			r.append(" !W");
		return r.toString();
	}
	
}
