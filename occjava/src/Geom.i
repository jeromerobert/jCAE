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

%{
#include <Geom_Curve.hxx>
#include <Geom_Surface.hxx>
#include <Geom2d_Curve.hxx>
#include <Geom_Geometry.hxx>
#include <Geom2d_Geometry.hxx>
#include <Geom_BoundedCurve.hxx>
#include <Geom_BSplineCurve.hxx>
#include <Geom_TrimmedCurve.hxx>
%}

%rename(Geom_Geometry) Handle_Geom_Geometry;
%rename(Geom_Curve) Handle_Geom_Curve;
%rename(Geom_Surface) Handle_Geom_Surface;
%rename(Geom2d_Geometry) Handle_Geom2d_Geometry;
%rename(Geom2d_Curve) Handle_Geom2d_Curve;
%rename(Geom_BoundedCurve) Handle_Geom_BoundedCurve;
%rename(Geom_BSplineCurve) Handle_Geom_BSplineCurve;
%rename(Geom_TrimmedCurve) Handle_Geom_TrimmedCurve;
class Handle_Geom_Geometry
{
	Handle_Geom_Geometry()=0;
};

class Handle_Geom_Curve: public Handle_Geom_Geometry
{
	Handle_Geom_Curve()=0;	
};

%extend Handle_Geom_Curve
{
	Standard_Real firstParameter()
	{
		return (*self)->FirstParameter();
	}
	
	Standard_Real lastParameter()
	{
		return (*self)->LastParameter();
	}
	
	Standard_Boolean isClosed()
	{
		return (*self)->IsClosed();
	}
	
	Standard_Boolean isPeriodic()
	{
		return (*self)->IsPeriodic();
	}
	
	Standard_Real period()
	{
		return (*self)->Period();
	}
}

class Handle_Geom_Surface: public Handle_Geom_Geometry
{
	Handle_Geom_Surface()=0;
};

%extend Handle_Geom_Surface
{
	gp_Pnt value(const Standard_Real U,const Standard_Real V) const
	{
		return (*self)->Value(U, V);
	}

	%javamethodmodifiers bounds(double bounds[4]) const "
	/**
	 * Return the bounds of the parameters of the surface.
	 * @param bounds  an array of size 4 which will receive {Umin, Umax, Vmin, Vmax}
	 */
	public";
	void bounds(double bounds[4]) const
	{
		(*self)->Bounds(bounds[0], bounds[1], bounds[2], bounds[3]);
	}
	
	%javamethodmodifiers bounds(Standard_Real&, Standard_Real&, Standard_Real&, Standard_Real&) const "
	/**
	 * @deprecated use bounds(double[]) , it do not need to allocate 4 arrays.
	 */
	public";
	void bounds(Standard_Real& U1,Standard_Real& U2,Standard_Real& V1,Standard_Real& V2) const
	{
		(*self)->Bounds(U1,U2,V1,V2);
	}
	
	Standard_Boolean isUClosed() const
	{
		return (*self)->IsUClosed();
	}
	
	Standard_Boolean isVClosed() const
	{
		return (*self)->IsVClosed();
	}
	
	Handle_Geom_Curve uIso(const Standard_Real U) const
	{
		return (*self)->UIso(U);
	}
	
	Handle_Geom_Curve vIso(const Standard_Real V) const
	{
		return (*self)->VIso(V);
	}
}

class Handle_Geom2d_Geometry
{
	Handle_Geom2d_Geometry()=0;
};

class Handle_Geom2d_Curve: public Handle_Geom2d_Geometry
{
	Handle_Geom2d_Curve()=0;
};

class Handle_Geom_BoundedCurve : public Handle_Geom_Curve {
	Handle_Geom_BoundedCurve()=0;
};

class Handle_Geom_BSplineCurve : public Handle_Geom_BoundedCurve {
	Handle_Geom_BSplineCurve()=0;
};

%extend Handle_Geom_BSplineCurve
{
//  void movePointAndTangent(const Standard_Real U,const gp_Pnt& P,const gp_Vec& Tangent,const Standard_Real Tolerance,const Standard_Integer StartingCondition,const Standard_Integer EndingCondition,Standard_Integer& *OUTPUT);
  
  void setKnot(const Standard_Integer Index,const Standard_Real K)
  {
    (*self)->SetKnot(Index,K);
  }
  
  void setKnot(const Standard_Integer Index,const Standard_Real K,const Standard_Integer M)
  {
    (*self)->SetKnot(Index,K,M);
  }

  void setPeriodic()
  {
    (*self)->SetPeriodic();
  }
  
  void setNotPeriodic()
  {
    (*self)->SetNotPeriodic();
  }
  
  void setOrigin(const Standard_Integer Index)
  {
     (*self)->SetOrigin(Index);
  }

  void setOrigin(const Standard_Real U,const Standard_Real Tol)
  {
     (*self)->SetOrigin(U,Tol);
  }
  
  void setPole(const Standard_Integer Index,const gp_Pnt& P)
  {
    (*self)->SetPole(Index,P);
  }
  
  void setPole(const Standard_Integer Index,const gp_Pnt& P,const Standard_Real Weight)
  {
    (*self)->SetPole(Index,P,Weight);
  }
  
  void movePoint(const Standard_Real U,const gp_Pnt& P,const Standard_Integer Index1,const Standard_Integer Index2,Standard_Integer& FirstModifiedPole,Standard_Integer& LastModifiedPole)
  {
    (*self)->MovePoint(U,P,Index1,Index2,FirstModifiedPole,LastModifiedPole);
  }

  void movePointAndTangent(const Standard_Real U,const gp_Pnt& P,const gp_Vec& Tangent,const Standard_Real Tolerance,const Standard_Integer StartingCondition,const Standard_Integer EndingCondition)
  {
    Standard_Integer ErrorStatus =0;
	(*self)->MovePointAndTangent(U,P,Tangent,Tolerance,StartingCondition,EndingCondition,ErrorStatus);
  }
  
  Standard_Boolean isClosed() const
  {
    return (*self)->IsClosed();
  }
  
  Standard_Boolean isPeriodic() const
  {
    return (*self)->IsPeriodic();
  }
  
  Standard_Boolean isRational() const
  {
    return (*self)->IsRational();
  }
  
  GeomAbs_Shape continuity() const
  {
    return (*self)->Continuity();
  }
  
  Standard_Integer Degree() const
  {
    return (*self)->Degree();
  }
  
  /*
  gp_Vec dN(const Standard_Real U,const Standard_Integer N) const
  {
    return (*self)->DN(U,N);
  }
  */
  
  gp_Pnt localValue(const Standard_Real U,const Standard_Integer FromK1,const Standard_Integer ToK2) const
  {
    return (*self)->LocalValue(U,FromK1,ToK2);
  }
  
  gp_Pnt endPoint() const
  {
    return (*self)->EndPoint();
  }
  
  gp_Pnt startPoint() const
  {
    return (*self)->StartPoint();
  }
  
  Standard_Integer nbKnots() const
  {
    return (*self)->NbKnots();
  }
  
  Standard_Integer nbPoles() const
  {
    return (*self)->NbPoles();
  }
  
  gp_Pnt pole(const Standard_Integer Index) const
  {
    return (*self)->Pole(Index);
  }
  
  Standard_Real weight(const Standard_Integer Index) const
  {
    return (*self)->Weight(Index);
  }
  
}

class Handle_Geom_TrimmedCurve : public Handle_Geom_BoundedCurve {
	Handle_Geom_TrimmedCurve()=0;
};
