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

import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.metrics.Matrix3D;

/**
 * Compute triangle area.
 * This class implements the {@link QualityProcedure#quality(Object)}
 * method to compute triangle area.
 */
public class Area extends QualityProcedure
{
	public static double [] v1 = new double[3];
	public static double [] v2 = new double[3];
	
	public Area()
	{
		setType(QualityProcedure.FACE);
	}
	
	public float quality(Object o)
	{
		if (!(o instanceof Triangle))
			throw new IllegalArgumentException();
		Triangle f = (Triangle) o;
		double [] p1 = f.vertex[0].getUV();
		double [] p2 = f.vertex[1].getUV();
		double [] p3 = f.vertex[2].getUV();
		for (int i = 0; i < 3; i++)
		{
			v1[i] = p2[i] - p1[i];
			v2[i] = p3[i] - p1[i];
		}
		double [] v3 = Matrix3D.prodVect3D(v1, v2);
		return (float) (0.5 * Matrix3D.norm(v3));
	}
}
