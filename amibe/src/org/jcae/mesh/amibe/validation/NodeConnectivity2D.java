/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005, by EADS CRC

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

package org.jcae.mesh.amibe.validation;

import org.jcae.mesh.amibe.ds.VirtualHalfEdge;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;

public class NodeConnectivity2D extends QualityProcedure
{
	public NodeConnectivity2D()
	{
		setType(QualityProcedure.NODE);
	}
	
	public float quality(Object o)
	{
		if (!(o instanceof Vertex))
			throw new IllegalArgumentException();
		Vertex n = (Vertex) o;
		VirtualHalfEdge start = new VirtualHalfEdge((Triangle) n.getLink(), 0);
		if (n == start.destination())
			start.next();
		else if (n == start.apex())
			start.prev();
		if (!start.isMutable())
			return 1.0f;
		Vertex d = start.destination();
		//  Loop around n
		int count = 0;
		while (true)
		{
			count++;
			if (count >= 12)
				return 0.0f;
			start.prev();
			//  Do not consider boundary nodes
			if (start == null || !start.isMutable())
				return 1.0f;
			start.sym();
			if (start.destination() == d)
				break;
		}
		
		if (count <= 6)
			return (((float) count) / 6.0f);
		else if (count <= 11)
			return (2.0f - ((float) count) / 6.0f);
		else
			return 0.0f;
	}
	
}
