/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005,2006, by EADS CRC
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

import org.jcae.mesh.amibe.patch.Mesh2D;
import org.jcae.mesh.amibe.patch.Vertex2D;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Matrix3D;

public class AbsoluteDeflection2D extends QualityProcedure
{
	private final Mesh2D mesh;
	private final Vertex [] p = new Vertex[4];
	private final double [] v1 = new double[3];
	private final double [] v2 = new double[3];
	private final double [] v3 = new double[3];
	private final double [] v4 = new double[3];
	private final Vertex2D c;
	
	public AbsoluteDeflection2D(Mesh2D m)
	{
		mesh = m;
		c = (Vertex2D) mesh.createVertex(0.0, 0.0);
	}
	
	@Override
	protected void setValidationFeatures()
	{
		usageStr = new String[]{"AbsoluteDeflection2D", "absolute deflection for 2D parameterized meshes"};
		type = QualityProcedure.FACE;
	}

	@Override
	public float quality(Object o)
	{
		if (!(o instanceof Triangle))
			throw new IllegalArgumentException();
		Triangle t = (Triangle) o;
		mesh.moveVertexToCentroid(c, t);
		double [] uv = c.getUV();
		double [] xyz = mesh.getGeomSurface().value(uv[0], uv[1]);
		p[3] = mesh.createVertex(xyz);
		for (int i = 0; i < 3; i++)
		{
			uv = t.vertex[i].getUV();
			xyz = mesh.getGeomSurface().value(uv[0], uv[1]);
			p[i] = mesh.createVertex(xyz);
		}
		double [] xyz0 = p[0].getUV();
		double [] xyz1 = p[1].getUV();
		double [] xyz2 = p[2].getUV();
		double [] xyz3 = p[3].getUV();
		for (int i = 0; i < 3; i++)
		{
			v1[i] = xyz1[i] - xyz0[i];
			v2[i] = xyz2[i] - xyz0[i];
			v3[i] = xyz3[i] - xyz0[i];
		}
		Matrix3D.prodVect3D(v1, v2, v4);
		double norm = Matrix3D.norm(v4);
		double dist = 0.0;
		if (norm > 0.0)
		{
			dist = Math.abs(Matrix3D.prodSca(v4, v3));
			dist /= Matrix3D.norm(v4);
		}
		return (float) dist;
	}
}
