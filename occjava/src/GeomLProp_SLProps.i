/*
 * Project Info:  http://jcae.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2005, by EADS CRC
 */

/**
 * GeomLProp_SLProps
 */
 %{#include "GeomLProp_SLProps.hxx"%}

 %typemap(javacode) GeomLProp_SLProps
%{
	/**
	 * @deprecated Typo mistake in the previous version
	 */
	public void setParameter(double u, double v)
	{
		setParameters(u, v);
	}
	
	public double[] curvatureDirections()
	{
		double[] toReturn=new double[6];
		if(isCurvatureDefined())
		{
			double[] max=new double[3];
			double[] min=new double[3];
			curvatureDirection(max, min);
			System.arraycopy(max, 0, toReturn, 0, 3);
			System.arraycopy(min, 0, toReturn, 3, 3);
		}
		return toReturn;
	}
%}

class GeomLProp_SLProps
{
	%rename(setParameters) SetParameters;
	%rename(value) Value;
	%rename(d1U) D1U;
	%rename(d1V) D1V;
	%rename(d2U) D2U;
	%rename(d2V) D2V;
	%rename(dUV) DUV;
	%rename(isTangentUDefined) IsTangentUDefined;
	%rename(tangentU) TangentU;
	%rename(isTangentVDefined) IsTangentVDefined;
	%rename(tangentV) TangentV;
	%rename(isNormalDefined) IsNormalDefined;
	%rename(normal) Normal;
	%rename(isCurvatureDefined) IsCurvatureDefined;
	%rename(isUmbilic) IsUmbilic;
	%rename(maxCurvature) MaxCurvature;
	%rename(minCurvature) MinCurvature;
	%rename(meanCurvature) MeanCurvature;
	%rename(gaussianCurvature) GaussianCurvature;
	%rename(setSurface) SetSurface;
	public:
	GeomLProp_SLProps(const Standard_Integer N,const Standard_Real Resolution);
	void SetParameters(const Standard_Real U,const Standard_Real V) ;
	const gp_Pnt& Value() const;
	const gp_Vec& D1U() ;
	const gp_Vec& D1V() ;
	const gp_Vec& D2U() ;
	const gp_Vec& D2V() ;
	const gp_Vec& DUV() ;
	Standard_Boolean IsTangentUDefined() ;
	void TangentU(gp_Dir& D) ;
	Standard_Boolean IsTangentVDefined() ;
	void TangentV(gp_Dir& D) ;
	Standard_Boolean IsNormalDefined() ;
	const gp_Dir& Normal() ;
	Standard_Boolean IsCurvatureDefined() ;
	Standard_Boolean IsUmbilic() ;
	Standard_Real MaxCurvature() ;
	Standard_Real MinCurvature() ;
	Standard_Real MeanCurvature() ;
	Standard_Real GaussianCurvature() ;
	void SetSurface(const Geom_Surface * S) ;	
};

%extend GeomLProp_SLProps
{
	void curvatureDirection(double jmax[3], double jmin[3])
	{
		gp_Dir max, min;
		self->CurvatureDirections(max, min);
		jmax[0]=max.X();
		jmax[1]=max.Y();
		jmax[2]=max.Z();
		jmin[3]=min.X();
		jmin[4]=min.Y();
		jmin[5]=min.Z();
	}
};
