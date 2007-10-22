/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005,2006, by EADS CRC
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

/**
 * Distance computations in 2D Euclidian space.
 * This class is called from {@link org.jcae.mesh.amibe.algos2d.BasicMesh}
 * but may also be used if amibe is extended to mesh 2D surfaces.
 */
public class Calculus2D implements Calculus
{
	/**
	 * Constructor.
	 *
	 * @param  m   the <code>Mesh2D</code> being modified.
	 */
	public Calculus2D(Mesh2D m)
	{
	}

	/**
	 * Returns the 2D distance to another <code>Vertex2D</code> instance.
	 *
	 * @param start  the first node
	 * @param end  the node to which distance is computed.
	 * @param vm  the node at which metrics is evaluated (unused)
	 * @return the distance between the two nodes.
	 */
	@Override
	public double distance(Vertex2D start, Vertex2D end, Vertex2D vm)
	{
		return distance(start, end);
	}
	
	/**
	 * Returns the 2D distance to another <code>Vertex2D</code> instance.
	 *
	 * @param start  the first node
	 * @param end  the node to which distance is computed.
	 * @return the distance between the two nodes.
	 */
	@Override
	public double distance(Vertex2D start, Vertex2D end)
	{
		double [] x0 = start.getUV();
		double [] x1 = end.getUV();
		double dx = x0[0] - x1[0];
		double dy = x0[1] - x1[1];
		return Math.sqrt(dx*dx + dy*dy);
	}
	
	/**
	 * Returns the 2D length of an edge.
	 *
	 * @param ot  the edge being evaluated
	 * @return the distance between its two endpoints.
	 */
	@Override
	public double length(VirtualHalfEdge2D ot)
	{
		return distance((Vertex2D) ot.origin(), (Vertex2D) ot.destination());
	}
	
	/**
	 * Returns the radius of unit ball in 2D.
	 *
	 * @param vm  the node at which metrics is evaluated (unused)
	 * @return this routine always returns 1.0.
	 */
	@Override
	public double radius2d(Vertex2D vm)
	{
		return 1.0;
	}
	
}
