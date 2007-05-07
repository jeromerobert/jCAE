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
	/**
	 * Read a shape from a file.
	 * This is an helper method. It do not exists in Opencascade.
	 * @param file the file to read
	 * @param builder the builder which will be used to create the shape (i.e. <code>new BRep_Builder()</code>).
	 */
	public static TopoDS_Shape read(String file, BRep_Builder builder)
	{
		TopoDS_Shape toReturn=new TopoDS_Shape();
		if(read(toReturn, file, builder))
			return TopoDS_Shape.downcast(toReturn);
		else
			return null;
	}
%}

%typemap(javaimports) BRepTools "
/** Provides various utilities for BRep. */"

%typemap(javadestruct, methodname="delete", methodmodifiers="private synchronized") BRepTools {};

class BRepTools
{
	%javamethodmodifiers Read(TopoDS_Shape& ,const Standard_CString, const BRep_Builder&) "
	/**
	 * Reads a shape from a file.
	 * @param shape an empty shape created with <code>new TopoDS_Shape()</code>
	 * @param builder used to build the shape (i.e. <code>new BRep_Builder()</code>).
	 * @return false on IO or file format errors.
	 */
	public";

	//Hide the constructor to make this class entirely static.
	BRepTools()=0;
	public:
	
	%rename(read) Read;
	static Standard_Boolean Read(TopoDS_Shape& shape,
		const Standard_CString file, const BRep_Builder& builder) ;
	
	%javamethodmodifiers Write(const TopoDS_Shape&, const Standard_CString)"
	/**
	 * Write a shape to a file.
	 * @param shape the shape to write
	 * @param file the file where to write the shape
	 * @return false on IO error.
	 */
	public";

	%rename(write) Write;
	static Standard_Boolean Write(const TopoDS_Shape& shape,
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

%{#include <BRepTools_Quilt.hxx>%}
class BRepTools_Quilt
{
    %rename(bind) Bind;
    %rename(add) Add;
    %rename(isCopied) IsCopied;
    %rename(copy) Copy;
    %rename(shells) Shells;
    public:
    BRepTools_Quilt();
    void Bind(const TopoDS_Edge& Eold,const TopoDS_Edge& Enew) ;
    void Bind(const TopoDS_Vertex& Vold,const TopoDS_Vertex& Vnew) ;
    void Add(const TopoDS_Shape& S) ;
    Standard_Boolean IsCopied(const TopoDS_Shape& S) const;
    const TopoDS_Shape& Copy(const TopoDS_Shape& S) const;
    TopoDS_Shape Shells() const;
};
