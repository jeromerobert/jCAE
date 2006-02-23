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
#include <BRepPrimAPI_MakeBox.hxx>
#include <BRepPrimAPI_MakeCone.hxx>
#include <BRepPrimAPI_MakeBox.hxx>
#include <BRepPrimAPI_MakeTorus.hxx>
#include <BRepPrimAPI_MakeCylinder.hxx>
#include <BRepPrimAPI_MakeSphere.hxx>
%}

class BRepPrimAPI_MakeBox : public BRepBuilderAPI_MakeShape
{
	public:
	BRepPrimAPI_MakeBox(const gp_Pnt& P1,const gp_Pnt& P2);
};

class BRepPrimAPI_MakeCone : public BRepBuilderAPI_MakeShape
{
	public:
	BRepPrimAPI_MakeCone(const gp_Ax2& Axes,const Standard_Real R1,
		const Standard_Real R2,const Standard_Real H,const Standard_Real angle);
};

class BRepPrimAPI_MakeCylinder : public BRepBuilderAPI_MakeShape
{
	public:
	BRepPrimAPI_MakeCylinder(const gp_Ax2& Axes,const Standard_Real R,
		const Standard_Real H,const Standard_Real Angle);
};

class BRepPrimAPI_MakeTorus : public BRepBuilderAPI_MakeShape
{
	public:
	BRepPrimAPI_MakeTorus(const gp_Ax2& Axes,const Standard_Real R1,
		const Standard_Real R2);
};

class BRepPrimAPI_MakeSphere : public BRepBuilderAPI_MakeShape
{
	public:
	BRepPrimAPI_MakeSphere(const gp_Pnt& Center,const Standard_Real R);
};
