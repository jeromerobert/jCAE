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
 * Compute maximal angle by triangle.
 * This class implements the {@link QualityProcedure#quality(Object)}
 * method to compute maximal angles of triangles.
 */
public class MaxAngleFace extends QualityProcedure
{
	@Override
	protected void setValidationFeatures()
	{
		usageStr = new String[]{"MaxAngleFace", "maximum angle for 3 edges"};
		type = QualityProcedure.FACE;
	}

	@Override
	public float quality(Object o)
	{
		if (!(o instanceof Triangle))
			throw new IllegalArgumentException();
		Triangle f = (Triangle) o;
		
		Vertex n1 = f.vertex[0];
		Vertex n2 = f.vertex[1];
		Vertex n3 = f.vertex[2];
		double a1 = Math.abs(n1.angle3D(n2, n3));
		double a2 = Math.abs(n2.angle3D(n3, n1));
		double a3 = Math.abs(n3.angle3D(n1, n2));
		if (a2 > a1)
			a1 = a2;
		if (a3 > a1)
			a1 = a3;
		return (float) a1;
	}
	
}
