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
#include <BRepPrimAPI_MakePrism.hxx>
#include <BRepPrimAPI_MakeRevol.hxx>
#include <BRepOffsetAPI_MakePipe.hxx>
%}

class BRepPrimAPI_MakeBox : public BRepBuilderAPI_MakeShape
{
	public:
	BRepPrimAPI_MakeBox(const gp_Pnt& P1,const gp_Pnt& P2);
};

class BRepPrimAPI_MakeCone : public BRepBuilderAPI_MakeShape
{
	public:
	BRepPrimAPI_MakeCone(const gp_Ax2& axes, const Standard_Real baseRadius,
		const Standard_Real topRadius,const Standard_Real height, const Standard_Real angle);
};


class BRepPrimAPI_MakeCylinder : public BRepBuilderAPI_MakeShape
{
	public:
%javamethodmodifiers BRepPrimAPI_MakeCylinder(const gp_Ax2& axes,const Standard_Real radius,
		const Standard_Real height,const Standard_Real angle) "
  /**
   * @param axes is {X, Y, Z, directionX, directionY, directionZ}
   */
public";
	BRepPrimAPI_MakeCylinder(const gp_Ax2& axes,const Standard_Real radius,
		const Standard_Real height,const Standard_Real angle);
	BRepPrimAPI_MakeCylinder(const gp_Ax2& axes,const Standard_Real radius,
		const Standard_Real height);
};

class BRepPrimAPI_MakeTorus : public BRepBuilderAPI_MakeShape
{
	public:
	BRepPrimAPI_MakeTorus(const gp_Ax2& Axes,const Standard_Real R1,
		const Standard_Real R2);
	BRepPrimAPI_MakeTorus(const gp_Ax2& Axes, const Standard_Real R1,
		const Standard_Real R2, const Standard_Real angle1,
		const Standard_Real angle2, const Standard_Real angle);
};

class BRepPrimAPI_MakeSphere : public BRepBuilderAPI_MakeShape
{
	public:
	BRepPrimAPI_MakeSphere(const gp_Pnt& center,const Standard_Real radius);
	BRepPrimAPI_MakeSphere(const gp_Ax2& axis,const Standard_Real R,const Standard_Real angle1,const Standard_Real angle2,const Standard_Real angle3);
};

class BRepPrimAPI_MakeSweep  : public BRepBuilderAPI_MakeShape
{
};

class BRepPrimAPI_MakePrism  : public BRepPrimAPI_MakeSweep
{
    public:
    BRepPrimAPI_MakePrism(const TopoDS_Shape& baseShape, const gp_Vec& extrudeDirection, const
        Standard_Boolean Copy = Standard_False,
        const Standard_Boolean Canonize = Standard_True);

    // gp_Vec and gp_Dir are both translated to double[] so this contructor
    // will conflict with the previous one
    // TODO: Change the signature to avoir conflict (easy but I have no time for now)
    // BRepPrimAPI_MakePrism(const TopoDS_Shape& S,const gp_Dir& D,const
    //      Standard_Boolean Inf = Standard_True,const Standard_Boolean Copy =
    //      Standard_False,const Standard_Boolean Canonize = Standard_True);
};

class BRepPrimAPI_MakeRevol  : public BRepPrimAPI_MakeSweep {
    public:
    BRepPrimAPI_MakeRevol(const TopoDS_Shape& shape, const gp_Ax1& axis, const Standard_Real angle, const Standard_Boolean copy = Standard_False);
    BRepPrimAPI_MakeRevol(const TopoDS_Shape& shape, const gp_Ax1& axis, const Standard_Boolean copy = Standard_False);
};

class BRepOffsetAPI_MakePipe  : public BRepPrimAPI_MakeSweep {
    public:
	BRepOffsetAPI_MakePipe(const TopoDS_Wire& Spine,const TopoDS_Shape& Profile);
};
