/*
 * Project Info:  http://jcae.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2013, by EADS France
 */
package org.jcae.mesh.stitch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.KdTree;
import org.jcae.mesh.amibe.metrics.Location;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.metrics.Metric;
import org.jcae.mesh.amibe.projection.TriangleKdTree;
import org.jcae.mesh.amibe.util.HashFactory;

/**
 * Compute the triangles intersections between 2 groups of triangles
 * @author Jerome Robert
 */
public class Intersection {
	private final Mesh mesh;
	private final TriangleKdTree triangleKDTree;
	private final Metric metric = new Metric(){
		private double[] unit = new double[]{1,1,1};
		public double distance2(Location p1, Location p2) {
			return p1.sqrDistance3D(p2);
		}

		public double[] getUnitBallBBox() {
			return unit;
		}
	};

	public Intersection(Mesh mesh, TriangleKdTree triangleKDTree) {
		this.mesh = mesh;
		this.triangleKDTree = triangleKDTree;
	}

	private void computeAABB(Triangle t, double[] aabb)
	{
		for(int i = 0; i < 3; i++)
			aabb[i] = Double.POSITIVE_INFINITY;
		for(int i = 0; i < 3; i++)
			aabb[i+3] = Double.NEGATIVE_INFINITY;
		for(int i = 0; i < 3; i++)
		{
			Vertex v = t.getV(i);
			aabb[0] = Math.min(v.getX(), aabb[0]);
			aabb[1] = Math.min(v.getY(), aabb[1]);
			aabb[2] = Math.min(v.getZ(), aabb[2]);
			aabb[3] = Math.max(v.getX(), aabb[3]);
			aabb[4] = Math.max(v.getY(), aabb[4]);
			aabb[5] = Math.max(v.getZ(), aabb[5]);
		}
	}

	/**
	 * Intersect group1 with group2
	 * @return the created beams
	 */
	public List<Vertex> intersect(int group1, int group2, double tolerance)
	{
		double tol2 = tolerance * tolerance;
		double[] aabb = new double[6];
		ArrayList<Triangle> triangles = new ArrayList<Triangle>();
		ArrayList<Vertex> toReturn = new ArrayList<Vertex>();
		KdTree<Vertex> kdTree = new KdTree<Vertex>(triangleKDTree.getBounds());
		Set<Triangle> done = HashFactory.createSet();
		for(Triangle t: mesh.getTriangles())
		{
			if(t.getGroupId() == group1)
			{
				computeAABB(t, aabb);
				triangles.clear();
				triangleKDTree.getNearTriangles(aabb, triangles, group2);
				Vertex v1 = mesh.createVertex(0, 0, 0);
				Vertex v2 = mesh.createVertex(0, 0, 0);
				for(Triangle t2: triangles)
				{
					if(done.add(t2) && intersect(t, t2, v1, v2))
					{
						Vertex nv1 = kdTree.getNearestVertex(metric, v1);
						Vertex vv1;
						if(nv1 != null && nv1.sqrDistance3D(v1) < tol2)
							vv1 = nv1;
						else
						{
							kdTree.add(v1);
							vv1 = v1;
							v1 = mesh.createVertex(0, 0, 0);
						}
						
						Vertex nv2 = kdTree.getNearestVertex(metric, v2);
						Vertex vv2;
						if(nv2 != null && nv2.sqrDistance3D(v2) < tol2)
							vv2 = nv2;
						else
						{
							kdTree.add(v2);
							vv2 = v2;
							v2 = mesh.createVertex(0, 0, 0);
						}

						if(vv1 != vv2)
						{
							toReturn.add(vv1);
							toReturn.add(vv2);
						}
					}
				}
				done.clear();
			}
		}
		return toReturn;
	}

	// lazily translated from vtkIntersectionPolyDataFilter::TriangleTriangleIntersection
	private final double[] temp1 = new double[3];
	private final double[] temp2 = new double[3];
	private final double[] n1 = new double[3];
	private final double[] n2 = new double[3];
	private final double[] dist1 = new double[3];
	private final double[] dist2 = new double[3];
	private final double[] p = new double[3];
	private final double[] v = new double[3];
	private final double[] t1 = new double[2];
	private final double[] t2 = new double[2];
	private final double[] x = new double[3];
	private double t;

	private boolean intersect(Triangle pts1, Triangle pts2, Vertex v1, Vertex v2) {
		// Compute supporting plane normals.
		Matrix3D.computeNormal3D(pts1.getV0(), pts1.getV1(), pts1.getV2(), temp1, temp2, n1);
		Matrix3D.computeNormal3D(pts2.getV0(), pts2.getV1(), pts2.getV2(), temp1, temp2, n2);
		double s1 = -Matrix3D.prodSca(n1, pts1.getV0());
		double s2 = -Matrix3D.prodSca(n2, pts2.getV0());

		// Compute signed distances of points p1, q1, r1 from supporting
		// plane of second triangle.
		for(int i = 0; i < 3; i++)
			dist1[i] = Matrix3D.prodSca(n2, pts1.getV(i)) + s2;

		// If signs of all points are the same, all the points lie on the
		// same side of the supporting plane, and we can exit early.
		if ((dist1[0] * dist1[1] > 0.0) && (dist1[0] * dist1[2] > 0.0)) {
			return false;
		}

		// Do the same for p2, q2, r2 and supporting plane of first
		// triangle.
		for (int i = 0; i < 3; i++) {
			dist2[i] = Matrix3D.prodSca(n1, pts2.getV(i)) + s1;
		}

		// If signs of all points are the same, all the points lie on the
		// same side of the supporting plane, and we can exit early.
		if ((dist2[0] * dist2[1] > 0.0) && (dist2[0] * dist2[2] > 0.0)) {
			return false;
		}

		// Check for coplanarity of the supporting planes.
		if (Math.abs(n1[0] - n2[0]) < 1e-9
			&& Math.abs(n1[1] - n2[1]) < 1e-9
			&& Math.abs(n1[2] - n2[2]) < 1e-9
			&& Math.abs(s1 - s2) < 1e-9)
		{
			return false;
		}

		// There are more efficient ways to find the intersection line (if
		// it exists), but this is clear enough.

		// Find line of intersection (L = p + t*v) between two planes.
		double n1n2 = Matrix3D.prodSca(n1, n2);
		double a = (s1 - s2 * n1n2) / (n1n2 * n1n2 - 1.0);
		double b = (s2 - s1 * n1n2) / (n1n2 * n1n2 - 1.0);
		p[0] = a * n1[0] + b * n2[0];
		p[1] = a * n1[1] + b * n2[1];
		p[2] = a * n1[2] + b * n2[2];
		Matrix3D.prodVect3D(n1, n2, v);
		double normV = Matrix3D.norm(v);
		for (int i = 0; i < 3; i++) {
			v[i] /= normV;
		}

		int index1 = 0, index2 = 0;
		for (int i = 0; i < 3; i++) {
			int id1 = i, id2 = (i + 1) % 3;

			// Find t coordinate on line of intersection between two planes.
			if (intersectWithLine(pts1.getV(id1), pts1.getV(id2), n2,
				pts2.getV0(), x)) {
				if(index1 >= 2)
					//something strange append so we don't intersect
					return false;
				t1[index1++] = Matrix3D.prodSca(x, v) - Matrix3D.prodSca(p, v);
			}

			if (intersectWithLine(pts2.getV(id1), pts2.getV(id2), n1,
				pts1.getV0(), x)) {
				if(index2 >= 2)
					//something strange append so we don't intersect
					return false;
				t2[index2++] = Matrix3D.prodSca(x, v) - Matrix3D.prodSca(p, v);
			}
		}

		// Check if only one edge or all edges intersect the supporting
		// planes intersection.
		if (index1 != 2 || index2 != 2) {
			return false;
		}

		// Check for NaNs
		if (Double.isNaN(t1[0]) || Double.isNaN(t1[1])
			|| Double.isNaN(t2[0]) || Double.isNaN(t2[1])) {
			return false;
		}

		if (t1[0] > t1[1]) {
			double tmp = t1[0];
			t1[0] = t1[1];
			t1[1] = tmp;
		}
		if (t2[0] > t2[1]) {
			double tmp = t2[0];
			t2[0] = t2[1];
			t2[1] = tmp;
		}
		// Handle the different interval configuration cases.
		double tt1, tt2;
		if (t1[1] < t2[0] || t2[1] < t1[0]) {
			return false; // No overlap
		} else if (t1[0] < t2[0]) {
			if (t1[1] < t2[1]) {
				tt1 = t2[0];
				tt2 = t1[1];
			} else {
				tt1 = t2[0];
				tt2 = t2[1];
			}
		} else // t1[0] >= t2[0]
		{
			if (t1[1] < t2[1]) {
				tt1 = t1[0];
				tt2 = t1[1];
			} else {
				tt1 = t1[0];
				tt2 = t2[1];
			}
		}

		// Create actual intersection points.
		v1.moveTo(p[0] + tt1 * v[0], p[1] + tt1 * v[1], p[2] + tt1 * v[2]);
		v2.moveTo(p[0] + tt2 * v[0], p[1] + tt2 * v[1], p[2] + tt2 * v[2]);
		return true;
	}

	private final double[] p21 = new double[3];
	/*
	 * Given a line defined by the two points p1,p2; and a plane defined by the
	 * normal n and point p0, compute an intersection. The parametric
	 * coordinate along the line is returned in t, and the coordinates of
	 * intersection are returned in x. A zero is returned if the plane and line
	 * do not intersect between (0<=t<=1). If the plane and line are parallel,
	 * zero is returned and t is set to VTK_LARGE_DOUBLE.
	 */
	private boolean intersectWithLine(Vertex p1, Vertex p2, double[] n,
		Vertex p0, double[] x) {
		double num, den;
		double fabsden, fabstolerance;

		// Compute line vector
		p2.sub(p1, p21);

		// Compute denominator.  If ~0, line and plane are parallel.
		num = Matrix3D.prodSca(n, p0) - Matrix3D.prodSca(n, p1);
		den = n[0] * p21[0] + n[1] * p21[1] + n[2] * p21[2];

		// If denominator with respect to numerator is "zero", then the line and
		// plane are considered parallel.

		// trying to avoid an expensive call to fabs()
		if (den < 0.0) {
			fabsden = -den;
		} else {
			fabsden = den;
		}
		if (num < 0.0) {
			fabstolerance = -num * 1E-6;
		} else {
			fabstolerance = num * 1E-6;
		}
		if (fabsden <= fabstolerance) {
			t = Double.MAX_VALUE;
			return false;
		}

		// valid intersection
		t = num / den;

		x[0] = p1.getX() + t * p21[0];
		x[1] = p1.getY() + t * p21[1];
		x[2] = p1.getZ() + t * p21[2];

		return t >= 0.0 && t <= 1.0;
	}
}
