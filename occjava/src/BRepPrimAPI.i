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
#include <BRep_Builder.hxx>
%}

class BRepBuilderAPI_MakeShape
{
	//Hide the constructor to make this class abstract
	BRepBuilderAPI_MakeShape()=0;
	public:
	%rename(shape) Shape;
	const TopoDS_Shape& Shape() const;
};

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

%{#include <BRepBuilderAPI_Transform.hxx>%}
%{#include <BRepBuilderAPI_ModifyShape.hxx>%}
class BRepBuilderAPI_ModifyShape  : public BRepBuilderAPI_MakeShape
{
	BRepBuilderAPI_ModifyShape()=0;
};

class BRepBuilderAPI_Transform : public BRepBuilderAPI_ModifyShape
{
	%rename(perform) Perform;
	%rename(modifiedShape) ModifiedShape;
	public:
	BRepBuilderAPI_Transform(const gp_Trsf& T);
	BRepBuilderAPI_Transform(const TopoDS_Shape& S,	const gp_Trsf& T,
		const Standard_Boolean Copy = Standard_False);
	void Perform(const TopoDS_Shape& S,
		const Standard_Boolean Copy = Standard_False) ;
	virtual const TopoDS_Shape& ModifiedShape(const TopoDS_Shape& S) const;
};
