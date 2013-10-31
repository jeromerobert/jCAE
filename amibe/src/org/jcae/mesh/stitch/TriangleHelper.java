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
}
