/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

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

import org.jcae.mesh.amibe.ds.MEdge1D;
import org.jcae.mesh.amibe.ds.MNode1D;
import org.jcae.mesh.cad.CADEdge;
import org.jcae.mesh.cad.CADGeomCurve3D;
import org.jcae.mesh.cad.CADShapeFactory;
import org.jcae.mesh.amibe.metrics.Matrix3D;

public class EdgeLength1D extends QualityProcedure
{
	private CADEdge edge = null;
	private final double [] v1 = new double[3];
		
	@Override
	protected void setValidationFeatures()
	{
		usageStr = new String[]{"EdgeLength1D", "edge length for 1D mesh"};
		type = QualityProcedure.EDGE;
	}

	@Override
	public float quality(Object o)
	{
		if (!(o instanceof MEdge1D))
			throw new IllegalArgumentException();
		MEdge1D e = (MEdge1D) o;
		MNode1D n1 = e.getNodes1();
		MNode1D n2 = e.getNodes2();
		CADGeomCurve3D c3d = CADShapeFactory.getFactory().newCurve3D(edge);
		double [] xyz1 = c3d.value(n1.getParameter());
		double [] xyz2 = c3d.value(n2.getParameter());
		for (int i = 0; i < 3; i++)
			v1[i] = xyz2[i] - xyz1[i];
		return (float) Matrix3D.norm(v1);
	}
	
	public void setCADEdge(CADEdge e)
	{
		edge = e;
	}
}
