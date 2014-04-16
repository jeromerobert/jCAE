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

import java.util.Arrays;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Location;
import static org.jcae.mesh.stitch.TriangleHelper.isOnEdge;

/**
 *
 * @author Jerome Robert
 */
class TriangleSplitter {
	private TriangleHelper triangleHelper;
	private final Location splitPoint = new Location();
	private AbstractHalfEdge toSplit;
	private Vertex vertex;

	public void setTriangle(TriangleHelper th)
	{
		this.triangleHelper = th;
	}

	public Vertex getSplitVertex(Mesh mesh)
	{
		if(vertex == null)
		{
			if(toSplit == null)
				return null;
			else
				return mesh.createVertex(splitPoint);
		}
		else
			return vertex;
	}

	public AbstractHalfEdge getSplittedEdge()
	{
		return toSplit;
	}

	/**
	 * Return a point of the projected segment which is close to the split point
	 * @param b the projection of c on the triangle
	 * @param c a point of the projected segment
	 * @param d a point of the projected segment
	 * @return
	 */
	public double getReverseSplitPoint(Location b, Location c, Location d, Location result)
	{
		Location e = vertex;
		if( e == null )
			if(toSplit == null)
				return Double.POSITIVE_INFINITY;
			else
				e = splitPoint;
		return lineToLineDistance(b, c, d, e, result);
	}

	private boolean isVertex(double u, double v, double sqrTol)
	{
		splitPoint.moveTo(triangleHelper.getLocation(u, v));
		if (splitPoint.sqrDistance3D(toSplit.origin()) < sqrTol)
		{
			vertex = toSplit.origin();
			toSplit = null;
			return true;
		}
		else if (splitPoint.sqrDistance3D(toSplit.destination()) < sqrTol)
		{
			vertex = toSplit.destination();
			toSplit = null;
			return true;
		}
		else
		{
			vertex = null;
			return false;
		}
	}

	/**
	 * Intersect an edge of the triangle with the (apex, point=) segment.
	 * @param apex the apex of the edge to intersect
	 * @param p the other point of the segment
	 */
	public void splitApex(Vertex apex, Location p, double sqrTol) {
		Triangle triangle = triangleHelper.getTriangle();
		assert triangle.getV0().sqrDistance3D(p) > 1E-24;
		assert triangle.getV1().sqrDistance3D(p) > 1E-24;
		assert triangle.getV2().sqrDistance3D(p) > 1E-24;
		double[] uv = new double[2];
		triangleHelper.getBarycentricCoords(p, uv);
		double u = 0;
		double v = 0;
		if (apex == triangle.getV2()) {
			if(uv[0] > 1)
			{
				toSplit = null;
				vertex = null;
				return;
			}
			//intersect the segment u = 0
			toSplit = triangle.getAbstractHalfEdge().prev(); // 0->1 = V
			v = uv[1] / (1 - uv[0]);
			if(!isVertex(u, v, sqrTol) && (v < 0 || v > 1 || uv[0] > 1 ))
			{
				toSplit = null;
			}
			else
			{
				assert toSplit == null ||  toSplit.origin() == triangle.getV0() :
					toSplit.origin() + "\n" + triangle;
				assert toSplit == null ||  toSplit.destination() == triangle.getV1();
				assert vertex != null || isOnEdge(splitPoint, apex, p, sqrTol): u+" "+v+" "+Arrays.toString(uv);
				assert vertex == null || isOnEdge(vertex, apex, p, sqrTol):
					u + " " + v + " " + Arrays.toString(uv) + "\n" + vertex + "\n" + apex + "\n" + p;
			}
		} else if (apex == triangle.getV1()) {
			if(uv[1] > 1)
			{
				toSplit = null;
				vertex = null;
				return;
			}
			//intersect the segment v = 0
			toSplit = triangle.getAbstractHalfEdge().next(); // 0->2 = -U
			u = uv[0] / (1 - uv[1]);
			if(!isVertex(u, v, sqrTol) && (u < 0 || u > 1))
			{
				toSplit = null;
			}
			else
			{
				assert toSplit == null ||  toSplit.origin() == triangle.getV2();
				assert toSplit == null ||  toSplit.destination() == triangle.getV0();
				assert isOnEdge(splitPoint, apex, p, sqrTol): u+" "+v+" "+Arrays.toString(uv);
			}
		} else if(apex == triangle.getV0())
		{
			if(uv[0] + uv[1] < 0)
			{
				toSplit = null;
				vertex = null;
				return;
			}			
			//intersect the segement u+v = 1
			toSplit = triangle.getAbstractHalfEdge(); // 1->2
			double tt = uv[0] + uv[1];
			u = uv[0] / tt;
			v = uv[1] / tt;
			if(!isVertex(u, v, sqrTol) && (u < 0 || v < 0 || uv[0] < 0 || uv[1] < 0))
			{
				toSplit = null;
			}
			else
			{
				assert toSplit == null ||  toSplit.origin() == triangle.getV1();
				assert toSplit == null ||  toSplit.destination() == triangle.getV2();
				assert isOnEdge(splitPoint, apex, p, sqrTol): u+" "+v+" "+Arrays.toString(uv);
			}
		}
		else
		{
			//the apex is not in the triangle so
			toSplit = null;
			vertex = null;
		}
	}
	/**
	 * Given a line L1: D+t*DC and L2 E+t*BC find the point of L1 which is the
	 * closest of L2 and return the distance
	 * @param b A point of the direction of L2
	 * @param c A point of L1 and of the direction of L2
	 * @param d A point of L1
	 * @param e A point of L2
	 * @param f set to the point of L1 which is the closest of L2
	 * @return the square of the distance between L1 and L2
	 */
	private static double lineToLineDistance(Location b, Location c,
		Location d, Location e, Location f)
	{
		double a01 = 0;
		double b0 = 0;
		double cc = 0;
		double b1 = 0;
		double normd1 = 0;
		double normd2 = 0;
		//use e as temporary storage for the direction of L1
		f.moveTo(c.getX() - d.getX(),
			c.getY() - d.getY(),
			c.getZ() - d.getZ());

		for(int i = 0; i < 3; i++)
		{
			double diff = d.get(i) - e.get(i);
			double d1 = f.get(i);
			double d2 = c.get(i) - b.get(i);
			normd1 += d1 * d1;
			normd2 += d2 * d2;
			a01 -= d1 * d2;
			b0 += diff * d1;
			cc += diff * diff;
			b1 -= diff * d2;
		}
		double s0;
		if(normd2 < 1E-10)
		{
			//L2 is degenerated so just project e to L1
			s0 = - b0 / Math.sqrt(cc);
		}
		else
		{
			normd1 = Math.sqrt(normd1);
			normd2 = Math.sqrt(normd2);
			b0 /= normd1;
			b1 /= normd2;
			a01 /= normd1 * normd2;
			s0 = (a01 * b1 - b0) / Math.abs(1 - a01 * a01);
		}

		s0 /= normd1;
		f.moveTo(
			d.getX() + s0 * f.getX(),
			d.getY() + s0 * f.getY(),
			d.getZ() + s0 * f.getZ());

		double sqrDist = f.sqrDistance3D(e);
		assert s0 > 0 && s0 < 1 : "\ns0 :" + s0 + "\nb: " + b + "\nc: " + c +
			"\nd: " + d + "\ne: " + e + "\ndist: " + Math.sqrt(sqrDist) +
			"\n" + "normd1: " + normd1 + "\nnormd2: " + normd2 + "\n";
		return sqrDist;
	}

	/**
	 * Intersection an edge of the triangle with the (p1, p2) segment
	 * @param sqrTol update splitVertex property instead of splittedEdge if the
	 * split point is closer from a triangle vertex than this distance
	 * @return the intersected edge of the triangle
	 */
	public void split(Location p1, Location p2, double sqrTol)
	{
		toSplit = null;
		vertex = null;
		Triangle triangle = triangleHelper.getTriangle();
		double[] uvO = new double[2];
		double[] uvD = new double[2];
		triangleHelper.getBarycentricCoords(p1, uvO);
		triangleHelper.getBarycentricCoords(p2, uvD);
		double deltaU = uvD[0] - uvO[0];
		double deltaV = uvD[1] - uvO[1];
		double u = 0;
		double v = 0;
		//intersect the segment u = 0
		if (uvO[0] < 0 && uvD[0] > 0) {
			v = uvO[1] - uvO[0] / deltaU * deltaV;
			toSplit = triangle.getAbstractHalfEdge().prev(); // 0->1 = V
			if(!isVertex(u, v, sqrTol) && (v < 0 || v > 1))
				toSplit = null;
		} else {
			//intersect the segment v = 0
			if (uvO[1] < 0 && uvD[1] > 0) {
				u = uvO[0] - uvO[1] / deltaV * deltaU;
				toSplit = triangle.getAbstractHalfEdge().next(); // 0->2 = -U
				if(!isVertex(u, v, sqrTol) && (u < 0 || u > 1))
					toSplit = null;
			} else {
				double tt = (1 - uvO[0] - uvO[1]) / (deltaU + deltaV);
				if (tt > 0 && tt < 1) {
					toSplit = triangle.getAbstractHalfEdge(); // 1->2
					u = uvO[0] + tt * deltaU;
					v = uvO[1] + tt * deltaV;
					assert Math.abs(u + v - 1) < 1E-4: tt + " " + (u + v);
					if(!isVertex(u, v, sqrTol))
					{
						if (!(v > 0 && u > 0))
							toSplit = null;
					}
				}
			}
		}
		assert !(toSplit != null && vertex != null);
		if (toSplit != null) {
			assert isOnEdge(splitPoint, toSplit.origin(), toSplit.destination(), sqrTol):
				triangle + "\np1:" + p1 + "\np2:" + p2 + "\nuv:" +
				Arrays.toString(uvO) + " " + Arrays.toString(uvD)+"\nsqrTol: "+sqrTol+"\nu+v: "+(u+v)+" "+(deltaU+deltaV);
			assert isOnEdge(splitPoint, p1, p2, 10000) : Arrays.toString(uvO) + " " + Arrays.toString(uvD);
		}
	}
}
