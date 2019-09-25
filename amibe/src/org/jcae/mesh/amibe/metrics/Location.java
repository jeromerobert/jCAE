/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2009, by EADS France

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

package org.jcae.mesh.amibe.metrics;

import org.jcae.mesh.amibe.ds.Vertex;

public class Location
{
	private double x, y, z;
	public Location()
	{
	}

	public Location(Location l)
	{
		moveTo(l.getX(), l.getY(), l.getZ());
	}

	public Location(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Gets coordinates of this vertex.  Array has length 2 in 2D and 3 in 3D.
	 *
	 * @return coordinates of this vertex
	 */
	public final double getX()
	{
		return x;
	}

	public final double getY()
	{
		return y;
	}

	public double getZ()
	{
		return z;
	}

	public final void get(double[] destination)
	{
		destination[0] = getX();
		destination[1] = getY();
		destination[2] = getZ();
	}

	public double get(int i)
	{
		switch(i)
		{
		case 0: return x;
		case 1: return y;
		case 2: return z;
		default:
			throw new ArrayIndexOutOfBoundsException();
		}
	}
	/**
	 * Move vertex to this position, if in 3D.
	 *
	 * @param x first coordinate
	 * @param y second coordinate
	 * @param z third coordinate
	 */
	public final void moveTo(double x, double y, double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public final void moveTo(Location other)
	{
		this.x = other.getX();
		this.y = other.getY();
		this.z = other.getZ();
	}

	/** result = this - other */
	public void sub(Location other, double[] result)
	{
		result[0] = getX() - other.getX();
		result[1] = getY() - other.getY();
		result[2] = getZ() - other.getZ();
	}

	/** Add other to this */
	public void add(Location other)
	{
		x += other.getX();
		y += other.getY();
		z += other.getZ();
	}

	/** Set this location to the middle of l1 and l2 */
	public void middle(Location l1, Location l2)
	{
		moveTo(
			(l1.getX() + l2.getX()) / 2,
			(l1.getY() + l2.getY()) / 2,
			(l1.getZ() + l2.getZ()) / 2);
	}

	public void scale(double alpha)
	{
		this.x *= alpha;
		this.y *= alpha;
		this.z *= alpha;
	}

	public int dim()
	{
		return 3;
	}

	/**
	 * Returns the squared distance in 3D space.
	 *
	 * @param end  the node to which distance is computed.
	 * @return the squared distance to <code>end</code>.
	 **/
	public double sqrDistance3D(Location end)
	{
		double dx = getX() - end.getX();
		double dy = getY() - end.getY();
		double dz = getZ() - end.getZ();
		return dx*dx+dy*dy+dz*dz;
	}

	/**
	 * Returns the distance in 3D space.
	 *
	 * @param end  the node to which distance is computed.
	 * @return the distance to <code>end</code>.
	 **/
	public double distance3D(Location end)
	{
		return Math.sqrt(sqrDistance3D(end));
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ", " + z + ")";
	}

	public static double dot(double[] v1, double[] v2)
	{
		return v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2];
	}

	/**
	 * Return the distance between this and an edge
	 * @param origin the origin of the edge
	 * @param destination the destination of the edge
	 * @param projection the projection of this on the edge
	 * @return the square of the distance
	 */
	public double sqrDistance3D(Location origin, Location destination, Location projection) {
		// adapted from https://www.geometrictools.com/GTEngine/Include/Mathematics/GteDistPointSegment.h
		double direction[] = new double[3];
		destination.sub(origin, direction);
		double diff[] = new double[3];
		sub(destination, diff);
		if(dot(direction, diff) < 0) {
			projection.moveTo(origin);
			sub(origin, diff);
			double t = dot(direction, diff);
			if (t > 0)  {
				double sqrLength = dot(direction, direction);
				if (sqrLength > 0) {
					t /= sqrLength;
					projection.moveTo(
						origin.getX() + t * direction[0],
						origin.getY() + t * direction[1],
						origin.getZ() + t * direction[2]);
				}
			}
		} else {
			projection.moveTo(destination);
		}

		sub(projection, diff);
		return dot(diff, diff);
	}
}
