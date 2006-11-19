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

import org.jcae.mesh.cad.CADGeomSurface;
import org.jcae.opencascade.jni.Geom_Surface;
import org.jcae.opencascade.jni.GeomLProp_SLProps;

public class OCCGeomSurface implements CADGeomSurface
{
	private Geom_Surface mySurface = null;
	private GeomLProp_SLProps myLprop = null;
	
	public OCCGeomSurface()
	{
	}
	
	public void setSurface(Object o)
	{
		mySurface = (Geom_Surface) o;
	}
	
	public Object getSurface()
	{
		return mySurface;
	}
	
	public double [] value(double u, double v)
	{
		return mySurface.value(u, v);
	}
	
	public void dinit(int degree)
	{
		myLprop = new GeomLProp_SLProps(degree, 0.0001);
		myLprop.setSurface(mySurface);
	}
	
	public void setParameter(double u, double v)
	{
		assert null != myLprop;
		myLprop.setParameter(u, v);
	}
	
	public double [] d1U()
	{
		assert null != myLprop;
		return myLprop.d1U();
	}
	
	public double [] d1V()
	{
		assert null != myLprop;
		return myLprop.d1V();
	}
	
	public double [] d2U()
	{
		assert null != myLprop;
		return myLprop.d2U();
	}
	
	public double [] d2V()
	{
		assert null != myLprop;
		return myLprop.d2V();
	}
	
	public double [] dUV()
	{
		assert null != myLprop;
		return myLprop.dUV();
	}
	
	public double [] normal()
	{
		assert null != myLprop;
		return myLprop.normal();
	}
	
	public double minCurvature()
	{
		assert null != myLprop;
		return myLprop.minCurvature();
	}
	
	public double maxCurvature()
	{
		assert null != myLprop;
		return myLprop.maxCurvature();
	}
	
	public double meanCurvature()
	{
		assert null != myLprop;
		return myLprop.meanCurvature();
	}
	
	public double gaussianCurvature()
	{
		assert null != myLprop;
		return myLprop.gaussianCurvature();
	}
	
	public double [] curvatureDirections()
	{
		assert null != myLprop;
		return myLprop.curvatureDirections();
	}
	
}
