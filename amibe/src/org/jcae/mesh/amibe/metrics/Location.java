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

public interface Location
{
	/**
	 * Gets coordinates of this vertex.  Array has length 2 in 2D and 3 in 3D.
	 *
	 * @return coordinates of this vertex
	 */
	double[] getUV();

	/**
	 * Move vertex to this position, if in 2D.
	 *
	 * @param u first coordinate
	 * @param v second coordinate
	 */
	void moveTo(double u, double v);

	/**
	 * Move vertex to this position, if in 3D.
	 *
	 * @param x first coordinate
	 * @param y second coordinate
	 * @param z third coordinate
	 */
	void moveTo(double x, double y, double z);
}
