/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005, by EADS CRC
    Copyright (C) 2007,2009, by EADS France

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

import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;

/**
 * Compute angles between adjacent triangles.
 * This class implements the {@link QualityProcedure#quality(Object)}
 * method to compute angles between adjacent triangles.  The inner
 * products between the normal to the triangle and the normal to
 * adjacent triangles are computed, and the quality of the triangle
 * is set to the minimal value.  This is very useful to detect
 * inverted triangles in 3D on smooth surfaces.
 */
public class DihedralAngle extends QualityProcedure
{
	private AbstractHalfEdge ot;
	private AbstractHalfEdge sym;

	private final double[] temp1 = new double[3];
	private final double[] temp2 = new double[3];
	private final double[] temp3 = new double[3];
	private final double[] temp4 = new double[3];
	
	@Override
	protected void setValidationFeatures()
	{
		usageStr = new String[]{"DihedralAngle", "smallest dot product of normals with adjacent triangles"};
		type = QualityProcedure.FACE;
	}


	/**
	 * Returns <code>MeshTraitsBuilder</code> instance needed by this class.
	 */
	@Override
	protected final MeshTraitsBuilder getMeshTraitsBuilder()
	{
		return MeshTraitsBuilder.getDefault3D();
	}

	@Override
	public float quality(Object o)
	{
		if (!(o instanceof Triangle))
			throw new IllegalArgumentException();
		Triangle t = (Triangle) o;
		ot = t.getAbstractHalfEdge(ot);
		sym = t.getAbstractHalfEdge(sym);
		float ret = 1.0f;
		for (int i = 0; i < 3; i++)
		{
			ot = ot.next();
			if (ot.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
				continue;
			if (!ot.hasSymmetricEdge())
				continue;
			sym = ot.sym(sym);
			if (t.getGroupId() != sym.getTri().getGroupId())
				continue;
			double [] p0 = ot.origin().getUV();
			double [] p1 = ot.destination().getUV();
			double [] p2 = ot.apex().getUV();
			double [] p3 = sym.apex().getUV();

			Matrix3D.computeNormal3D(p0, p1, p2, temp1, temp2, temp3);
			Matrix3D.computeNormal3D(p1, p0, p3, temp1, temp2, temp4);
			float dot = (float) Matrix3D.prodSca(temp3, temp4);
			if (dot < ret)
				ret = dot;
		}
		return ret;
	}

}

