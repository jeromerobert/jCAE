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

public final class EuclidianMetric3D implements Metric
{
	private final double [] unit_bounds = new double[]{1.0, 1.0, 1.0};
	private final double scale;

	public EuclidianMetric3D()
	{
		scale = 1.0;
	}

	public EuclidianMetric3D(double s)
	{
		scale = 1.0 / (s*s);
		unit_bounds[0] = unit_bounds[1] = unit_bounds[2] = s;
	}

	/**
	 * Return 3D Euclidian square distance between two points.
	 *
	 * @param p1  coordinates of the first node
	 * @param p2  coordinates of the second node
	 * @return 3D Euclidian square distance between these two points.
	 */
	@Override
	public double distance2(Location p1, Location p2)
	{
		double dx = p1.getX() - p2.getX();
		double dy = p1.getY() - p2.getY();
		double dz = p1.getZ() - p2.getZ();
		return scale * (dx * dx + dy * dy + dz * dz);
	}

	/**
	 * Return triplet <code>(1, 1, 1)</code>.
	 *
	 * @return a double[3] array with values <code>(1, 1, 1)</code>
	 */
	public double [] getUnitBallBBox()
	{
		return unit_bounds;
	}

}
