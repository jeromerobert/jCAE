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

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Location;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;

/**
 * Compute various values in a triangle.
 * <ul>
 *  <li>edges vectors</li>
 *  <li>edges norm</li>
 *  <li>normal</li>
 *  <li>barycentric coordinates</li>
 * </ul>
 */
class TriangleHelper {
	/** The edges vectors of the triangle. */
	public final double[][] edges = new double[3][3];
	public final double[] edgesNorm = new double[3];
	public final double[] normal = new double[3];
	public double normAN2;
	private final double[] tmp = new double[3];
	private Triangle triangle;
	private final Location location = new Location();

	public TriangleHelper(Triangle triangle) {
		setTriangle(triangle);
	}

	public TriangleHelper() {
	}

	public final void setTriangle(Triangle triangle) {
		this.triangle = triangle;
		triangle.getV1().sub(triangle.getV0(), edges[0]);
		triangle.getV2().sub(triangle.getV1(), edges[1]);
		//TODO lazy initialisation ?
		triangle.getV0().sub(triangle.getV2(), edges[2]);
		//TODO lazy initialisation ?
		Matrix3D.prodVect3D(edges[0], edges[1], normal);
		normAN2 = Matrix3D.prodSca(normal, normal);
		assert normAN2 > 0: triangle;
		//TODO lazy initialisation ?
		for (int i = 0; i < 3; i++) {
			edgesNorm[i] = Matrix3D.prodSca(edges[i], edges[i]);
		}
	}

	public double getEdgeNorm(int i) {
		//TODO lazy initialisation ?
		return edgesNorm[i];
	}

	public void getBarycentricCoords(Location p, double[] uv) {
		// Compute vectors
		double[] v0 = edges[2]; // - (C-A)
		double[] v1 = edges[0]; // B-A
		p.sub(triangle.getV0(), tmp); // P - A
		// Compute dot products
		double dot00 = edgesNorm[2];
		double dot01 = -Matrix3D.prodSca(v0, v1); //TODO precompute
		double dot02 = -Matrix3D.prodSca(v0, tmp);
		double dot11 = edgesNorm[0];
		double dot12 = Matrix3D.prodSca(v1, tmp);
		// Compute barycentric coordinates
		double invDenom = 1 / (dot00 * dot11 - dot01 * dot01);
		uv[0] = (dot11 * dot02 - dot01 * dot12) * invDenom;
		uv[1] = (dot00 * dot12 - dot01 * dot02) * invDenom;
	}

	/**
	 * Get a location from barycentric coordinates.
	 * Always return the same location instance.
	 * U vector is v0v2 and V is v0v1
	 */
	public Location getLocation(double u, double v) {
		Vertex v0 = triangle.getV0();
		location.moveTo(v0.getX() - u * edges[2][0] + v * edges[0][0],
			v0.getY() - u * edges[2][1] + v * edges[0][1],
			v0.getZ() - u * edges[2][2] + v * edges[0][2]);
		return location;
	}

	/**
	 * Given the 2 quads bellow with A E and B orthogonal, compute the
	 * coordinates of F from other points.
	 * <pre>
	 *          _____C
	 *     _ _F/     |
	 *   D/   |      |
	 *   |    |      |
	 *   |    |      |
	 *   A____E______B
	 * </pre>
	 * @param tol tolerance for determinant check. This is homogenous to a
	 * squared length
	 * @return the square of the distance between E and F
	 */
	public static double reverseProject(Location b, Location c,
		Location d, Location e, Location f, double tol)
	{
		assert f != null;
		assert e.sqrDistance3D(b) > 1E-24: e +" "+b;
		double dei, dej;
		//i = x, j = y
		double cbi = b.getX() - c.getX();
		double cbj = b.getY() - c.getY();
		double dci = c.getX() - d.getX();
		double dcj = c.getY() - d.getY();
		double denum = cbj*dci - cbi * dcj;
		if(Math.abs(denum) < tol)
		{
			//i = x, j = z
			cbi = b.getX() - c.getX();
			cbj = b.getZ() - c.getZ();
			dci = c.getX() - d.getX();
			dcj = c.getZ() - d.getZ();
			denum = cbj*dci - cbi * dcj;
			if(Math.abs(denum) < tol)
			{
				//i = y, j = z
				cbi = b.getY() - c.getY();
				cbj = b.getZ() - c.getZ();
				dei = e.getY() - d.getY();
				dej = e.getZ() - d.getZ();
				dci = c.getY() - d.getY();
				dcj = c.getZ() - d.getZ();
				denum = cbj*dci - cbi * dcj;
			}
			else
			{
				dei = e.getX() - d.getX();
				dej = e.getZ() - d.getZ();
			}
		}
		else
		{
			dei = e.getX() - d.getX();
			dej = e.getY() - d.getY();
		}
		if(Math.abs(denum) < tol)
		{
			f.moveTo(e);
		}
		else
		{
			/*assert Math.abs(denum) > tol : denum + " > " + tol + "\nb=" + b +
				"\nc=" + c + "\nd=" + d + "\ne=" + e + "\ntol=" + tol;*/
			double alpha = (cbj * dei - cbi * dej) / denum;
			f.moveTo(
				d.getX() + alpha * (c.getX() - d.getX()),
				d.getY() + alpha * (c.getY() - d.getY()),
				d.getZ() + alpha * (c.getZ() - d.getZ()));
		}
		assert isOnEdge(f, d, c, 2*tol): Math.abs(denum);
		return f.sqrDistance3D(e);
	}

	/**
	 * Given 2 triangles A, B, C and A, D, E with D on AB and unknown and on E
	 * on AC, use the Thales theorem to compute the DE distance, then if DE is
	 * smaller than sqrMaxDist, set location of E.
	 * @return true if DE is smaller than sqrMaxDist
	 */
	public static boolean thalesSqrDistance(Location a, Location b,
		Location c, Location d, double sqrMaxDist, Location e)
	{
		double[] tmp = new double[3];
		double norm2 = a.sqrDistance3D(d) * b.sqrDistance3D(c) / a.sqrDistance3D(b);
		if(norm2 > sqrMaxDist)
			return false;
		double norm = Math.sqrt(norm2);
		e.moveTo(
			d.getX() + norm * tmp[0],
			d.getY() + norm * tmp[1],
			d.getZ() + norm * tmp[2]);
		return true;
	}

	/**
	 * Compute the distance between p and the p1 p2 segment assuming that
	 * p projection on p1 p2 is between p1 and p2
	 */
	public static double sqrDistance(Location p1, Location p2, Location p)
	{
		double[] cross = new double[3];
		double[] edge1 = new double[3];
		double[] edge2 = new double[3];
		p.sub(p1, edge1);
		p2.sub(p1, edge2);
		Matrix3D.prodVect3D(edge1, edge2, cross);
		return Matrix3D.prodSca(cross, cross) / Matrix3D.prodSca(edge2, edge2);
	}

	/** Debugging method to check that a point is on a segment */
	public static boolean isOnEdge(Location p, Location s1, Location s2, double sqrTol)
	{
		if(p.sqrDistance3D(s1) < sqrTol || p.sqrDistance3D(s2) < sqrTol)
			return true;
		double[] vector1 = new double[3];
		double[] vector2 = new double[3];
		s2.sub(s1, vector1);
		p.sub(s1, vector2);
		double[] cross = new double[3];
		Matrix3D.prodVect3D(vector1, vector2, cross);
		double edgeNorm = Matrix3D.prodSca(vector1, vector1);
		double normMP2 = Matrix3D.prodSca(cross, cross) / edgeNorm;
		double dot = Double.NaN;
		if(normMP2 < sqrTol)
		{
			dot = Matrix3D.prodSca(vector2, vector1);
			dot /= edgeNorm;
			if(dot >= 0 && dot <= 1)
				return true;
		}
		System.err.println(p+" is not on "+s1+"-"+s2);
		System.err.println(Math.sqrt(normMP2)+" "+dot+" "+sqrTol);
		System.err.println(Math.sqrt(p.sqrDistance3D(s1))+ " " +Math.sqrt(p.sqrDistance3D(s2)));
		return false;
	}

	public Triangle getTriangle() {
		return triangle;
	}

	public static void test()
	{
		Mesh mesh = new Mesh(MeshTraitsBuilder.getDefault3D());
		Vertex v0 = mesh.createVertex(0, 0, 0);
		Vertex v1 = mesh.createVertex(1, 0, 0);
		Vertex v2 = mesh.createVertex(0, 1, 0);
		Triangle triangle = mesh.createTriangle(v0, v1, v2);
		TriangleHelper th = new TriangleHelper(triangle);
		TriangleSplitter ts = new TriangleSplitter();
		ts.setTriangle(th);
		ts.split(new Location(-0.5, 0.5, 0), new Location(2, 0.5, 0), 1);
		assert ts.getSplitPoint().sqrDistance3D(new Location(0,0.5,0)) < 1E-6;

		v2.moveTo(0.5, 0.1, 0.1);
		assert TriangleHelper.sqrDistance(v0, v1, v2) - 0.02 < 1E-8;

		v0.moveTo(5500.0, 3500.0, 0.0);
		v1.moveTo(-500.0, 3500.0, 0.0);
		v2.moveTo(295.4250418574649, 928.3645397543528, -19.548979250276396);
		th.setTriangle(th.getTriangle());
		Location p1 = new Location(-1.8504563775477978, 1000.2540420059955, -19.002491845838513);
		Location p2 = new Location(195.4932520809972, 980.6765334491752, -19.151315547512326);
		ts.split(p1, p2, 0.010000000000000002);
		assert ts.getSplitVertex() == null && ts.getSplittedEdge() == null;

		v0.moveTo(4000.0, -2250.0, -1000.0);
		v1.moveTo(-2000.0, 3000.0, -1000.0);
		v2.moveTo(1000.0, 750.0, -1000.0);
		th.setTriangle(th.getTriangle());
		ts.split(new Location(1000.0, -334.87, -1000.0),
			new Location(1000.0, 0, -1000.0), 1);
		assert ts.getSplittedEdge() == null;

		v0.moveTo(5500.0, 3500.0, 0.0);
		v1.moveTo(-500.0, 3500.0, 0.0);
		v2.moveTo(-500.0, 500.0, 0.0);
		th.setTriangle(th.getTriangle());
		p1 = new Location(555.5819503922005, 831.444099215599, 0.0);
		p2 = new Location(707.106, 707.108, 0.0);
		ts.split(p1, p2, 0.010000000000000002);
		assert ts.getSplitVertex() == null && ts.getSplittedEdge() == null;

		Location b=new Location(555.569, 831.47, 0.0);
		Location c = new Location(555.569, 831.47, 0.0);
		Location d= new Location(382.682, 923.88, 0.0);
		Location e= new Location(544.4204001835601, 837.4290490264581, 0.0);
		TriangleHelper.reverseProject(b, c, d, e, new Location(), 1.0);
	}
}
