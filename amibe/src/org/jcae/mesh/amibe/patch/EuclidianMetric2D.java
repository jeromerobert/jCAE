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

package org.jcae.mesh.amibe.patch;

import org.jcae.mesh.amibe.metrics.Location;

final class EuclidianMetric2D implements Metric2D
{
	private final double [] unit_bounds = new double[]{1.0, 1.0};

	/**
	 * Return 2D Euclidian square distance between two points.
	 *
	 * @param p1  coordinates of the first node
	 * @param p2  coordinates of the second node
	 * @return 2D Euclidian square distance between these two points.
	 */
	public final double distance2(Location p1, Location p2)
	{
		return (p1.getX() - p2.getX()) * (p1.getX() - p2.getX()) +
		       (p1.getY() - p2.getY()) * (p1.getY() - p2.getY());
	}

	/**
	 * Return pair <code>(1, 1)</code>.
	 *
	 * @return a double[2] array with values <code>(1, 1)</code>
	 */
	public final double [] getUnitBallBBox()
	{
		return unit_bounds;
	}

	/**
	 * Return the 2D Euclidian dot product of two vectors.
	 *
	 * @param x0 first coordinate of the first vector.
	 * @param y0 second coordinate of the first vector.
	 * @param x1 first coordinate of the second vector.
	 * @param y1 second coordinate of the second vector.
	 * @return the 2D Euclidian dot product of these two vectors.
	 */
	public final double dot(double x0, double y0, double x1, double y1)
	{
		return x0 * x1 + y0 * y1;
	}

	/**
	 * Return an orthogonal vector.  If <code>V=(x0,y0)</code>, then
	 * <code>orth(V)=(-y0,x0)</code> is such that
	 *   dot(orth(V), V) = 0
	 *   dot(orth(V), orth(V)) = dot(V, V)
	 *
	 * @param x0 first coordinate
	 * @param y0 second coordinate
	 * @param result an allocated array to store result
	 */
	public final void computeOrthogonalVector(double x0, double y0, double[] result)
	{
		result[0] = - y0;
		result[1] = x0;
	}

	/**
	 * Return this instance.  An Euclidian metric is its inverse, this instance
	 * is returned to not create unnecessary objects.
	 *
	 * @return this instance, which is also the inverse metric.
	 */
	public final Metric2D getInverse()
	{
		return this;
	}

	/**
	 * Return the determinant of this metric, which is 1.
	 *
	 * @return 1.
	 */
	public final double det()
	{
		return 1.0;
	}

	public final boolean isPseudoIsotropic()
	{
		return true;
	}

}
