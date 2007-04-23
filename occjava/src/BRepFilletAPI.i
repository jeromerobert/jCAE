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
 * (C) Copyright 2007, by EADS France
 */
%{#include <BRepFilletAPI_MakeFillet.hxx>%}
%{#include <BRepFilletAPI_MakeChamfer.hxx>%}

class BRepFilletAPI_LocalOperation: public BRepBuilderAPI_MakeShape
{
};

%rename(Rational) ChFi3d_Rational;
%rename(QuasiAngular) ChFi3d_QuasiAngular;
%rename(Polynomial) ChFi3d_Polynomial;

enum ChFi3d_FilletShape {
    ChFi3d_Rational,
    ChFi3d_QuasiAngular,
    ChFi3d_Polynomial
};

class BRepFilletAPI_MakeFillet: public BRepFilletAPI_LocalOperation
{
    %rename(add) Add; 
  public:
    BRepFilletAPI_MakeFillet(const TopoDS_Shape& shape, const ChFi3d_FilletShape type = ChFi3d_Rational);
    void Add(const Standard_Real radius, const TopoDS_Edge& edge) ;
};

class BRepFilletAPI_MakeChamfer: public BRepFilletAPI_LocalOperation
{
    %rename(add) Add; 
  public:
    BRepFilletAPI_MakeChamfer(const TopoDS_Shape& shape);
    void Add(const Standard_Real distance, const TopoDS_Edge& edge, const TopoDS_Face& face);
};

