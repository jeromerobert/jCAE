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
import org.jcae.mesh.amibe.metrics.Matrix3D;

/**
 * Compute triangle area.
 * This class implements the {@link QualityProcedure#quality(Object)}
 * method to compute triangle area.
 */
public class Area extends QualityProcedure
{
	private static final double [] v1 = new double[3];
	private static final double [] v2 = new double[3];
	private static final double [] v3 = new double[3];
	
	@Override
	protected void setValidationFeatures()
	{
		usageStr = new String[]{"Area", "triangle area"};
		type = QualityProcedure.FACE;
	}

	@Override
	public float quality(Object o)
	{
		if (!(o instanceof Triangle))
			throw new IllegalArgumentException();
		Triangle f = (Triangle) o;
		f.getV1().sub(f.getV0(), v1);
		f.getV2().sub(f.getV0(), v2);
		Matrix3D.prodVect3D(v1, v2, v3);
		return (float) (0.5 * Matrix3D.norm(v3));
	}
}
