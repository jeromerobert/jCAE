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

import org.jcae.mesh.amibe.metrics.Metric;

public interface Metric2D extends Metric
{
	/**
	 * Return the dot product of two vectors in this Riemannian metric.
	 *
	 * @param x0 first coordinate of the first vector.
	 * @param y0 second coordinate of the first vector.
	 * @param x1 first coordinate of the second vector.
	 * @param y1 second coordinate of the second vector.
	 * @return the dot product of two vectors in this Riemannian metric.
	 */
	double dot(double x0, double y0, double x1, double y1);

	/**
	 * Return an orthogonal vector in this metric.
	 * This vector O is such that
	 *   dot(transp(O), O) = 0.
	 *   dot(transp(O), transp(O)) = det(M) dot(V, V).
	 *
	 * @param x0 first coordinate
	 * @param y0 second coordinate
	 * @param result an allocated array to store result
	 */
	void computeOrthogonalVector(double x0, double y0, double[] result);

	/**
	 * Return the inverse metric.
	 *
	 * @return the inverse metric, or <code>null<code> if metric is singular.
	 */
	Metric2D getInverse();

	/**
	 * Return the determinant of this metric.
	 *
	 * @return the determinant of this metric.
	 */
	double det();

	/**
	 * Test whether this metric is Euclidian.
	 *
	 * @return <code>true</code> if this metric is quasi-Euclidian,
	 * <code>false</code> otherwise.
	 */
	boolean isPseudoIsotropic();

}
