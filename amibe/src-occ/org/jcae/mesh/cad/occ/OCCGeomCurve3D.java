/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005, by EADS CRC

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

package org.jcae.mesh.cad.occ;

import org.jcae.mesh.cad.CADGeomCurve3D;
import org.jcae.mesh.cad.CADEdge;
import org.jcae.opencascade.jni.BRep_Tool;
import org.jcae.opencascade.jni.Geom_Curve;
import org.jcae.opencascade.jni.GeomAdaptor_Curve;
import org.jcae.opencascade.jni.GProp_GProps;
import org.jcae.opencascade.jni.BRepGProp;

public class OCCGeomCurve3D implements CADGeomCurve3D
{
	private GeomAdaptor_Curve myCurve = null;
	private double [] range = new double[2];
	private OCCDiscretizeCurve3D discret = null;
	private double len = 0.0;
	
	public OCCGeomCurve3D(CADEdge E)
	{
		if (!(E instanceof OCCEdge))
			throw new IllegalArgumentException();
		OCCEdge occEdge = (OCCEdge) E;
		Geom_Curve curve = BRep_Tool.curve(occEdge.getShape(), range);
		if (curve == null)
			throw new RuntimeException();
		myCurve = new GeomAdaptor_Curve(curve);
		GProp_GProps myProps = new GProp_GProps();
		BRepGProp.linearProperties (occEdge.getShape(), myProps);
		len = myProps.mass();
	}
	
	public double [] value(double p)
	{
		assert myCurve != null;
		return myCurve.value((float) p);
	}
	
	public double [] getRange()
	{
		return range;
	}
	
	public void discretize(double maxlen, double deflection, boolean relDefl)
	{
		if (discret == null)
			discret = new OCCDiscretizeCurve3D(myCurve, range[0], range[1]);
		discret.discretizeMaxDeflection(deflection, relDefl);
		if (maxlen > 0.0)
		{
			for (int i = 0; i < discret.nbPoints()-1; i++)
				discret.discretizeSubsegmentMaxLength(i, maxlen);
		}
	}
	
	public void discretize(double maxlen)
	{
		if (discret == null)
			discret = new OCCDiscretizeCurve3D(myCurve, range[0], range[1]);
		discret.discretizeMaxLength(maxlen);
	}
	
	public void discretize(int n)
	{
		if (discret == null)
			discret = new OCCDiscretizeCurve3D(myCurve, range[0], range[1]);
		discret.discretizeNrPoints(n);
	}
	
	public void splitSubsegment(int numseg, int nrsub)
	{
		if (discret == null)
			discret = new OCCDiscretizeCurve3D(myCurve, range[0], range[1]);
		discret.splitSubsegment(numseg, nrsub);
	}
	
	public void setDiscretization(double [] param)
	{
		if (discret == null)
			discret = new OCCDiscretizeCurve3D(myCurve, range[0], range[1]);
		discret.setDiscretization(param);
	}
	
	public int nbPoints()
	{
		assert discret != null;
		return discret.nbPoints();
	}
	
	public double parameter(int index)
	{
		assert discret != null;
		return discret.parameter(index);
	}
	
	public double length()
	{
		return len;
	}
	
}
