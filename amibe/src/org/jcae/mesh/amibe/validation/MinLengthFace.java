/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005, by EADS CRC
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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

package org.jcae.mesh.amibe.validation;

import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;

/**
 * Compute maximal edge length by triangle.
 * This class implements the {@link QualityProcedure#quality(Object)}
 * method to compute minimal edge length of triangles.
 */

public class MinLengthFace extends QualityProcedure
{
	public MinLengthFace()
	{
		setType(QualityProcedure.FACE);
	}
	
	@Override
	public float quality(Object o)
	{
		if (!(o instanceof Triangle))
			throw new IllegalArgumentException();
		double l1, l2, l3;
		Triangle f = (Triangle) o;
		Vertex n1 = f.vertex[0];
		Vertex n2 = f.vertex[1];
		Vertex n3 = f.vertex[2];
		l1 = n1.distance3D(n2);
		l2 = n2.distance3D(n3);
		l3 = n3.distance3D(n1);
		if (l2 < l1)
			l1 = l2;
		if (l3 < l1)
			l1 = l3;
		return (float) l1;
	}
	
}
