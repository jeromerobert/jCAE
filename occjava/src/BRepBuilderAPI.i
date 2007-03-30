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
#include <BRepBuilderAPI_Transform.hxx>
#include <BRepBuilderAPI_ModifyShape.hxx>
#include <BRepBuilderAPI_MakeShape.hxx>
#include <BRepBuilderAPI_MakeWire.hxx>
#include <BRepBuilderAPI_MakeVertex.hxx>
#include <BRepBuilderAPI_MakeEdge.hxx>
#include <BRepBuilderAPI_MakeFace.hxx>
#include <BRepBuilderAPI_MakeSolid.hxx>
%}

class BRepBuilderAPI_MakeShape
{
	//Hide the constructor to make this class abstract
	BRepBuilderAPI_MakeShape()=0;
	public:
	%rename(shape) Shape;
	const TopoDS_Shape& Shape() const;
};

class BRepBuilderAPI_ModifyShape: public BRepBuilderAPI_MakeShape
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

class BRepBuilderAPI_MakeVertex: public BRepBuilderAPI_MakeShape
{
	%rename(vertex) Vertex;
	public:
	BRepBuilderAPI_MakeVertex(const gp_Pnt& P);
	//const TopoDS_Vertex& Vertex() const;
};

class BRepBuilderAPI_MakeWire : public BRepBuilderAPI_MakeShape
{
	%rename(wire) Wire;
	%rename(add) Add;
	%rename(isDone) IsDone;
	public:
	BRepBuilderAPI_MakeWire();
	BRepBuilderAPI_MakeWire(const TopoDS_Edge& E);
	BRepBuilderAPI_MakeWire(const TopoDS_Edge& E1,const TopoDS_Edge& E2);
	BRepBuilderAPI_MakeWire(const TopoDS_Edge& E1,const TopoDS_Edge& E2,
		const TopoDS_Edge& E3);
	BRepBuilderAPI_MakeWire(const TopoDS_Edge& E1,const TopoDS_Edge& E2,
		const TopoDS_Edge& E3,const TopoDS_Edge& E4);
	BRepBuilderAPI_MakeWire(const TopoDS_Wire& W);
	BRepBuilderAPI_MakeWire(const TopoDS_Wire& W,const TopoDS_Edge& E);
	void Add(const TopoDS_Edge& E) ;
	void Add(const TopoDS_Wire& W) ;
	void Add(const TopTools_ListOfShape & shapes);
	Standard_Boolean IsDone() const;
	//const TopoDS_Wire& Wire() const;
};

class BRepBuilderAPI_MakeEdge : public BRepBuilderAPI_MakeShape
{
	%rename(edge) Edge;
	%rename(isDone) IsDone;
	public:
	BRepBuilderAPI_MakeEdge();
	BRepBuilderAPI_MakeEdge(const TopoDS_Vertex& V1,const TopoDS_Vertex& V2);
	BRepBuilderAPI_MakeEdge(const gp_Pnt& P1,const gp_Pnt& P2);
    BRepBuilderAPI_MakeEdge(const gp_Circ& L);
    BRepBuilderAPI_MakeEdge(const gp_Circ& L,const Standard_Real p1,const Standard_Real p2);
    BRepBuilderAPI_MakeEdge(const gp_Circ& L,const gp_Pnt& P1,const gp_Pnt& P2);
    BRepBuilderAPI_MakeEdge(const gp_Circ& L,const TopoDS_Vertex& V1,const TopoDS_Vertex& V2);
	Standard_Boolean IsDone() const;
	//const TopoDS_Edge& Edge() const;
};

class BRepBuilderAPI_MakeFace  : public BRepBuilderAPI_MakeShape
{
	%rename(face) Face;
	public:
	BRepBuilderAPI_MakeFace(const TopoDS_Wire& W,
		const Standard_Boolean OnlyPlane = Standard_False);
    BRepBuilderAPI_MakeFace(const TopoDS_Face& F,const TopoDS_Wire& W);
	//const TopoDS_Face& Face() const;
};

class BRepBuilderAPI_MakeSolid: public BRepBuilderAPI_MakeShape
{
	%rename(add) Add;
	%rename(isDone) IsDone;
	%rename(isDeleted) IsDeleted;
	public:
	BRepBuilderAPI_MakeSolid();
	BRepBuilderAPI_MakeSolid(const TopoDS_CompSolid& S);
	BRepBuilderAPI_MakeSolid(const TopoDS_Shell& S);
	BRepBuilderAPI_MakeSolid(const TopoDS_Shell& S1,const TopoDS_Shell& S2);
	BRepBuilderAPI_MakeSolid(const TopoDS_Shell& S1,const TopoDS_Shell& S2,const TopoDS_Shell& S3);
	BRepBuilderAPI_MakeSolid(const TopoDS_Solid& So);
	BRepBuilderAPI_MakeSolid(const TopoDS_Solid& So,const TopoDS_Shell& S);
	void Add(const TopoDS_Shell& S) ;
	Standard_Boolean IsDone() const;
	Standard_Boolean IsDeleted(const TopoDS_Shape& S) ;
};
