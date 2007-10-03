/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005, by EADS CRC
    Copyright (C) 2007, by EADS France
 
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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.amibe.patch;

public interface Calculus
{
	/**
	 * Returns the length of an edge.
	 * @param ot  edge being measured
	 */
	public double length(VirtualHalfEdge2D ot);
	
	/**
	 * Returns the distance between two points.
	 * Metrics on both points is used to interpolate this length.
	 *
	 * @param start  the start node
	 * @param end  the end node
	 * @return the distance between these two points.
	 */
	public double distance(Vertex2D start, Vertex2D end);
	
	/**
	 * Returns the distance between two points.
	 * This distance is evaluated by using metrics at a given point.
	 *
	 * @param start  the start node
	 * @param end  the end node
	 * @param vm  the vertex on which metrics is evaluated
	 * @return the distance between these two points.
	 */
	public double distance(Vertex2D start, Vertex2D end, Vertex2D vm);
	
	/**
	 * Returns the 2D radius of the unit ball centered at a point.
	 * This routine returns a radius such that the 2D circle centered
	 * at a given vertex will have a distance lower than 1 in the
	 * desired 2D or 3D space.  This method is used by
	 * {@link org.jcae.mesh.amibe.util.KdTree#getNearestVertex}
	 *
	 * @param vm  the vertex on which metrics is evaluated
	 * @return the radius in 2D space.
	 */
	public double radius2d(Vertex2D vm);
}
