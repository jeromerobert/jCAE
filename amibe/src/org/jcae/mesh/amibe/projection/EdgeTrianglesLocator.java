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
 * (C) Copyright 2015, by Airbus Group SAS
 */
package org.jcae.mesh.amibe.projection;

import java.util.Collection;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.util.HashFactory;

/**
 * Find triangles which are close to an edge
 * @author Jerome Robert
 */
public class EdgeTrianglesLocator {
	private final Collection<Triangle> trianglesInAABB = HashFactory.createSet();
	private final Collection<Triangle> trianglesInOBB = HashFactory.createSet();
	private final double[] aabb = new double[6];
	private final double[] matrix = new double[12];
	private final TriangleInterAABB taabb = new TriangleInterAABB();
	private final TriangleKdTree kdTree;

	private static void compteEdgeAABB(Vertex v1, Vertex v2, double[] aabb)
	{
		aabb[0] = Math.min(v1.getX(), v2.getX());
		aabb[1] = Math.min(v1.getY(), v2.getY());
		aabb[2] = Math.min(v1.getZ(), v2.getZ());
		aabb[3] = Math.max(v1.getX(), v2.getX());
		aabb[4] = Math.max(v1.getY(), v2.getY());
		aabb[5] = Math.max(v1.getZ(), v2.getZ());
	}

	private static double normalize(double[] m, int c)
	{
		c = c * 4;
		double norm = Math.sqrt(m[c] * m[c] + m[c+1] * m[c+1] + m[c+2] * m[c+2]);
		m[c] /= norm;
		m[c+1] /= norm;
		m[c+2] /= norm;
		return norm;
	}

	/**
	 * Compute a transformation where the image of v1,v2 is along x.
	 * @param m a 3x4 matrix, rotation and translation
	 * see From Efficient Construction of Perpendicular Vectors without Branching
	 * by Michael M. Stark
	 */
	private static double nullSpace(Vertex v1, Vertex v2, double[] m)
	{
		// image of X
		m[0] = v2.getX() - v1.getX();
		m[1] = v2.getY() - v1.getY();
		m[2] = v2.getZ() - v1.getZ();
		double norm = normalize(m, 0);
		// compute a vector very not collinear to X
		double x = Math.abs(m[0]);
		double y = Math.abs(m[1]);
		double z = Math.abs(m[2]);
		boolean uyx = (x - y) < 0;
		boolean uzx = (x - z) < 0;
		boolean uzy = (y - z) < 0;
		boolean xm = uyx && uzx;
		boolean ym = !xm && uzy;
		boolean zm = !(xm || ym);
		x = xm ? 1 : 0;
		y = ym ? 1 : 0;
		z = zm ? 1 : 0;
		// image of Y
		m[4] = m[2] * y - m[1] * z;
		m[5] = m[0] * z - m[2] * x;
		m[6] = m[1] * x - m[0] * y;
		normalize(m, 1);
		// image of Z
		m[8] = m[1] * m[6] - m[2] * m[5];
		m[9] = m[4] * m[2] - m[6] * m[0];
		m[10] = m[0] * m[5] - m[1] * m[4];

		// translation
		x = -v1.getX();
		y = -v1.getY();
		z = -v1.getZ();
		m[3] = m[0] * x + m[1] * y + m[2] * z;
		m[7] = m[4] * x + m[5] * y + m[6] * z;
		m[11] = m[8] * x + m[9] * y + m[10] * z;
		return norm;
	}

	public EdgeTrianglesLocator(TriangleKdTree kdTree) {
		this.kdTree = kdTree;
	}

	public void locate(Vertex v1, Vertex v2, int group, double tolerance) {
		compteEdgeAABB(v1, v2, aabb);
		for (int i = 0; i < 3; i++) {
			aabb[i] -= tolerance;
			aabb[3 + i] += tolerance;
		}
		trianglesInAABB.clear();
		kdTree.getNearTriangles(aabb, trianglesInAABB, group);
		double norm = nullSpace(v1, v2, matrix);
		aabb[0] = 0;
		aabb[1] = -tolerance;
		aabb[2] = -tolerance;
		aabb[3] = norm;
		aabb[4] = tolerance;
		aabb[5] = tolerance;
		trianglesInOBB.clear();
		for (Triangle t : trianglesInAABB) {
			if (isValidTriangle(t)) {
				taabb.setTriangle(t, matrix);
				if (taabb.triBoxOverlap(aabb, true)) {
					trianglesInOBB.add(t);
				}
			}
		}
	}

	public Collection<Triangle> getResult() {
		return trianglesInOBB;
	}
	protected boolean isValidTriangle(Triangle t) {
		return true;
	}
}
