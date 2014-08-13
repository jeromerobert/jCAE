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
 * (C) Copyright 2012, by EADS France
 */

package org.jcae.mesh.amibe.projection;

import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;

/**
 * Check if a triangle intersect an ABB.
 * From http://fileadmin.cs.lth.se/cs/Personal/Tomas_Akenine-Moller/code/tribox3.txt
 * @author Jerome Robert
 */
public class TriangleInterAABB {
	private final static int X = 0, Y = 1, Z = 2;

	private static void cross(double[] dest, double[] v1, double[] v2) {
		dest[0] = v1[1] * v2[2] - v1[2] * v2[1];
		dest[1] = v1[2] * v2[0] - v1[0] * v2[2];
		dest[2] = v1[0] * v2[1] - v1[1] * v2[0];
	}

	private static double dot(double[] v1, double[] v2) {
		return v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2];
	}

	private static void sub(double[] dest, double[] v1, double[] v2)
	{
		dest[0] = v1[0] - v2[0];
		dest[1] = v1[1] - v2[1];
		dest[2] = v1[2] - v2[2];
	}

	private void findMinMax(double x0, double x1, double x2) {
		min = x0;
		max = x0;
		if (x1 < min) {
			min = x1;
		}
		if (x1 > max) {
			max = x1;
		}
		if (x2 < min) {
			min = x2;
		}
		if (x2 > max) {
			max = x2;
		}
	}

	private double[] vmin = new double[3];
	private double[] vmax = new double[3];
	private boolean planeBoxOverlap(double[] maxbox)
	{
		int q;
		double v;
		for (q = X; q <= Z; q++) {
			v = v0[q];
			if (normal[q] > 0.0f) {
				vmin[q] = -maxbox[q] - v;
				vmax[q] = maxbox[q] - v;
			} else {
				vmin[q] = maxbox[q] - v;
				vmax[q] = -maxbox[q] - v;
			}
		}
		if (dot(normal, vmin) > 0.0f) {
			return false;
		}
		if (dot(normal, vmax) >= 0.0f) {
			return true;
		}
		return false;
	}

	private boolean axisTestX01(double a, double b, double fa, double fb,
		double[] boxhalfsize) {
		double p0 = a * v0[Y] - b * v0[Z];
		double p2 = a * v2[Y] - b * v2[Z];
		if (p0 < p2) {
			min = p0;
			max = p2;
		} else {
			min = p2;
			max = p0;
		}
		double rad = fa * boxhalfsize[Y] + fb * boxhalfsize[Z];
		return min <= rad && max >= -rad;
	}

	private boolean axisTestX2(double a, double b, double fa, double fb,
		double[] boxhalfsize) {
		double p0 = a * v0[Y] - b * v0[Z];
		double p1 = a * v1[Y] - b * v1[Z];
		if (p0 < p1) {
			min = p0;
			max = p1;
		} else {
			min = p1;
			max = p0;
		}
		double rad = fa * boxhalfsize[Y] + fb * boxhalfsize[Z];
		return min <= rad && max >= -rad;
	}

	private boolean axisTestY02(double a, double b, double fa, double fb,
		double[] boxhalfsize) {
		double p0 = -a * v0[X] + b * v0[Z];
		double p2 = -a * v2[X] + b * v2[Z];
		if (p0 < p2) {
			min = p0;
			max = p2;
		} else {
			min = p2;
			max = p0;
		}
		double rad = fa * boxhalfsize[X] + fb * boxhalfsize[Z];
		return min <= rad && max >= -rad;
	}

	private boolean axisTestY1(double a, double b, double fa, double fb,
		double[] boxhalfsize) {
		double p0 = -a * v0[X] + b * v0[Z];
		double p1 = -a * v1[X] + b * v1[Z];
		if (p0 < p1) {
			min = p0;
			max = p1;
		} else {
			min = p1;
			max = p0;
		}
		double rad = fa * boxhalfsize[X] + fb * boxhalfsize[Z];
		return min <= rad && max >= -rad;
	}

	private boolean axisTestZ12(double a, double b, double fa, double fb,
		double[] boxhalfsize) {
		double p1 = a * v1[X] - b * v1[Y];
		double p2 = a * v2[X] - b * v2[Y];
		if (p2 < p1) {
			min = p2;
			max = p1;
		} else {
			min = p1;
			max = p2;
		}
		double rad = fa * boxhalfsize[X] + fb * boxhalfsize[Y];
		return min <= rad && max >= -rad;
	}

	private boolean axisTestZ0(double a, double b, double fa, double fb,
		double[] boxhalfsize) {
		double p0 = a * v0[X] - b * v0[Y];
		double p1 = a * v1[X] - b * v1[Y];
		if (p0 < p1) {
			min = p0;
			max = p1;
		} else {
			min = p1;
			max = p0;
		}
		double rad = fa * boxhalfsize[X] + fb * boxhalfsize[Y];
		return min <= rad && max >= -rad;
	}

	private final double[] v0 = new double[3];
	private final double[] v1 = new double[3];
	private final double[] v2 = new double[3];
	private final double[] normal = new double[3];
	private final double[] e0 = new double[3];
	private final double[] e1 = new double[3];
	private final double[] e2 = new double[3];
	private double min, max;
	private double[][] triverts = new double[3][3];
	private double[] boxcenter = new double[3];
	private double[] boxhalfsize = new double[3];

	public void setTriangle(Triangle triangle)
	{
		for(int i = 0; i < 3; i++)
		{
			triverts[i][0] = triangle.getV(i).getX();
			triverts[i][1] = triangle.getV(i).getY();
			triverts[i][2] = triangle.getV(i).getZ();
		}
		computeTriangleEdge();
	}

	/**
	 * @param triangle
	 * @param m a 3x4 matrix, useful to check OBB intersection instead of AABB
	 */
	public void setTriangle(Triangle triangle, double[] m)
	{
		for(int i = 0; i < 3; i++)
		{
			Vertex v = triangle.getV(i);
			double x = v.getX();
			double y = v.getY();
			double z = v.getZ();
			triverts[i][0] = m[0] * x + m[1] * y + m[2] * z + m[3];
			triverts[i][1] = m[4] * x + m[5] * y + m[6] * z + m[7];
			triverts[i][2] = m[8] * x + m[9] * y + m[10] * z + m[11];
		}
		computeTriangleEdge();
	}

	private void computeTriangleEdge()
	{
		sub(e0, triverts[1], triverts[0]);      /* tri edge 0 */
		sub(e1, triverts[2], triverts[1]);      /* tri edge 1 */
		sub(e2, triverts[0], triverts[2]);      /* tri edge 2 */
		cross(normal, e0, e1);
	}

	public boolean triBoxOverlap(double[] bounds, boolean testABB) {
		for(int j = 0; j < 3; j++)
		{
			boxcenter[j] = (bounds[j] + bounds[j+3]) / 2.0;
			boxhalfsize[j] = (bounds[j+3] - bounds[j]) / 2.0;
		}

		if(testABB)
		{
			for(int i = 0; i < 3; i++)
			if( triverts[i][X] < boxcenter[X] + boxhalfsize[X] &&
				triverts[i][Y] < boxcenter[Y] + boxhalfsize[Y] &&
				triverts[i][Z] < boxcenter[Z] + boxhalfsize[Z] &&
				triverts[i][X] > boxcenter[X] - boxhalfsize[X] &&
				triverts[i][Y] > boxcenter[Y] - boxhalfsize[Y] &&
				triverts[i][Z] > boxcenter[Z] - boxhalfsize[Z])
				return true;
		}
		/*    use separating axis theorem to test overlap between triangle and box */
		/*    need to test for overlap in these directions: */
		/*    1) the {x,y,z}-directions (actually, since we use the AABB of the triangle */
		/*       we do not even need to test these) */
		/*    2) normal of the triangle */
		/*    3) crossproduct(edge from tri, {x,y,z}-directin) */
		/*       this gives 3x3=9 more tests */
		double fex, fey, fez;

		/* This is the fastest branch on Sun */
		/* move everything so that the boxcenter is in (0,0,0) */
		sub(v0, triverts[0], boxcenter);

		/* Bullet 2: */
		/*  test if the box intersects the plane of the triangle */
		/*  compute plane equation of triangle: normal*x+d=0 */
		if (!planeBoxOverlap(boxhalfsize)) {
			return false;
		}

		sub(v2, triverts[2], boxcenter);

		/* Bullet 3:  */
		/*  test the 9 tests first (this was faster) */
		fey = Math.abs(e0[Y]);
		fez = Math.abs(e0[Z]);
		if(!axisTestX01(e0[Z], e0[Y], fez, fey, boxhalfsize))
			return false;

		sub(v1, triverts[1], boxcenter);
		fex = Math.abs(e0[X]);
		if(!axisTestY02(e0[Z], e0[X], fez, fex, boxhalfsize))
			return false;
		if(!axisTestZ12(e0[Y], e0[X], fey, fex, boxhalfsize))
			return false;

		fey = Math.abs(e1[Y]);
		fez = Math.abs(e1[Z]);
		if(!axisTestX01(e1[Z], e1[Y], fez, fey, boxhalfsize))
			return false;
		fex = Math.abs(e1[X]);
		if(!axisTestY02(e1[Z], e1[X], fez, fex, boxhalfsize))
			return false;
		if(!axisTestZ0(e1[Y], e1[X], fey, fex, boxhalfsize))
			return false;

		fey = Math.abs(e2[Y]);
		fez = Math.abs(e2[Z]);
		if(!axisTestX2(e2[Z], e2[Y], fez, fey, boxhalfsize))
			return false;
		fex = Math.abs(e2[X]);
		if(!axisTestY1(e2[Z], e2[X], fez, fex, boxhalfsize))
			return false;
		if(!axisTestZ12(e2[Y], e2[X], fey, fex, boxhalfsize))
			return false;

		return true;
	}
}
