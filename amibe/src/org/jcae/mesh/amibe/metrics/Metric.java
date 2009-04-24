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

public interface Metric
{
	/**
	 * Return square distance between two points with this metric.
	 *
	 * @param p1  coordinates of the first node
	 * @param p2  coordinates of the second node
	 * @return square distance between two points with this metric.
	 */
	double distance2(double [] p1, double [] p2);

	/**
	 * Return dimensions of the unit ball transformed by this metric.
	 * For instance, when this method returns <code>{a, b, c}</code>
	 * in 3D, this means that unit ball in this metric is enclosed in
	 * [-a,a]x[-b,b]x[-c,c] region.  This is needed by
	 * KdTree to eliminate octants which are too for away when looking
	 * for nearest neighbours.
	 *
	 * @return dimensions of the unit ball transformed by this metric.
	 */
	double [] getUnitBallBBox();
}
