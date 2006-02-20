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
%}

/*%rename(Geom_Geometry) Geom_Geometry;
%rename(Geom_Curve) Geom_Curve;
%rename(Geom_Surface) Geom_Surface;
%rename(Geom2d_Geometry) Geom2d_Geometry;
%rename(Geom2d_Curve) Geom2d_Curve;*/

class Geom_Geometry
{
	Geom_Geometry()=0;
};

class Geom_Curve: public Geom_Geometry
{
	%rename(firstParameter) FirstParameter;
	%rename(lastParameter) LastParameter;
	%rename(isClosed) IsClosed;
	%rename(isPeriodic) IsPeriodic;
	%rename(period) Period;
	
	Geom_Curve()=0;	
	public:
	Standard_Real FirstParameter();
	Standard_Real LastParameter();
	Standard_Boolean IsClosed();
	Standard_Boolean IsPeriodic();
	Standard_Real Period();
};


class Geom_Surface: public Geom_Geometry
{
	Geom_Surface()=0;
};

%extend Geom_Surface
{
	const gp_Pnt & value(const Standard_Real U,const Standard_Real V) const
	{
		return (self)->Value(U, V);
	}
}

class Geom2d_Geometry
{
	Geom2d_Geometry()=0;
};

class Geom2d_Curve: public Geom2d_Geometry
{
	Geom2d_Curve()=0;
};

