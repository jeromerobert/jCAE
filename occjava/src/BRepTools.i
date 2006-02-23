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

%{#include <BRepTools.hxx>%}

// define another read method that match the one of libOccJava (the one below don't).
%typemap(javacode) BRepTools
%{
	public static TopoDS_Shape read(String file, BRep_Builder builder)
	{
		TopoDS_Shape toReturn=new TopoDS_Shape();
		if(read(toReturn, file, builder))
			return TopoDS_Shape.downcast(toReturn);
		else
			return null;
	}
%}

class BRepTools
{
	//Hide the constructor to make this class entirely static.
	BRepTools()=0;
	public:
	
	%rename(read) Read;
	static Standard_Boolean Read(TopoDS_Shape& shape,
		const Standard_CString file, const BRep_Builder& builder) ;
	
	%rename(write) Write;
	static Standard_Boolean Write(const TopoDS_Shape& Sh,
		const Standard_CString file);
};

/**
 * BRepTools_WireExplorer
 */
%{#include <BRepTools_WireExplorer.hxx>%}
class BRepTools_WireExplorer
{
	%rename(init) Init;
	%rename(more) More;
	%rename(next) Next;
	%rename(current) Current;
	%rename(orientation) Orientation;
	%rename(currentVertex) CurrentVertex;
	%rename(clear) Clear;
	public:
	BRepTools_WireExplorer();
	BRepTools_WireExplorer(const TopoDS_Wire& W);
	BRepTools_WireExplorer(const TopoDS_Wire& W,const TopoDS_Face& F);
	void Init(const TopoDS_Wire& W) ;
	void Init(const TopoDS_Wire& W,const TopoDS_Face& F) ;
	Standard_Boolean More() const;
	void Next() ;
	const TopoDS_Edge& Current() const;
	TopAbs_Orientation Orientation() const;
	const TopoDS_Vertex& CurrentVertex() const;
	void Clear() ;
};

%{#include <BRepTools_Sewing.hxx>%}
class BRepTools_Sewing 
{
	%rename(init) Init;
	%rename(add) Add;
	%rename(perform) Perform;
	%rename(sewedShape) SewedShape;
	%rename(nbContigousEdges) NbContigousEdges;
	%rename(contigousEdge) ContigousEdge;
	%rename(contigousEdgeCouple) ContigousEdgeCouple;
	
	public:
	BRepTools_Sewing(const Standard_Real tolerance = 1.0e-06,
		const Standard_Boolean option1 = Standard_True,
		const Standard_Boolean option2 = Standard_True);
	void Init(const Standard_Real tolerance,const Standard_Boolean option1,
		const Standard_Boolean option2) ;
	void Add(const TopoDS_Shape& shape) ;
	void Perform() ;
	const TopoDS_Shape& SewedShape() const;
	Standard_Integer NbContigousEdges() const;
	const TopoDS_Edge& ContigousEdge(const Standard_Integer index) const;
	const TopTools_ListOfShape& ContigousEdgeCouple(const Standard_Integer index) const;
};
