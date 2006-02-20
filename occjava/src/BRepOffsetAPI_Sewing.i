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

%{#include <BRepOffsetAPI_Sewing.hxx>%}
class BRepOffsetAPI_Sewing
{
	%rename(init) Init;
	%rename(load) Load;
	%rename(add) Add;
	%rename(perform) Perform;
	%rename(sewedShape) SewedShape;
	%rename(nbFreeEdges) NbFreeEdges;
	%rename(freeEdge) FreeEdge;
	%rename(nbMultipleEdges) NbMultipleEdges;
	%rename(multipleEdge) MultipleEdge;
	%rename(nbDegeneratedShapes) NbDegeneratedShapes;
	%rename(degeneratedShape) DegeneratedShape;
	%rename(nbDeletedFaces) NbDeletedFaces;
	%rename(deletedFace) DeletedFace;
	%rename(isDegenerated) IsDegenerated;
	%rename(isModified) IsModified;
	%rename(modified) Modified;
	%rename(dump) Dump;
	public:
	BRepOffsetAPI_Sewing(const Standard_Real tolerance = 1.0e-06,
		const Standard_Boolean option = Standard_True,
		const Standard_Boolean cutting = Standard_True,
		const Standard_Boolean nonmanifold = Standard_False);
	void Init(const Standard_Real tolerance,
		const Standard_Boolean option = Standard_True,
		const Standard_Boolean cutting = Standard_True,
		const Standard_Boolean nonmanifold = Standard_False) ;
	void Load(const TopoDS_Shape& shape) ;
	void Add(const TopoDS_Shape& shape) ;
	void Perform() ;
	const TopoDS_Shape& SewedShape() const;
	Standard_Integer NbFreeEdges() const;
	const TopoDS_Edge& FreeEdge(const Standard_Integer index) const;
	Standard_Integer NbMultipleEdges() const;
	const TopoDS_Edge& MultipleEdge(const Standard_Integer index) const;
	Standard_Integer NbDegeneratedShapes() const;
	const TopoDS_Shape& DegeneratedShape(const Standard_Integer index) const;
	Standard_Integer NbDeletedFaces() const;
	const TopoDS_Face& DeletedFace(const Standard_Integer index) const;
	Standard_Boolean IsDegenerated(const TopoDS_Shape& shape) const;
	Standard_Boolean IsModified(const TopoDS_Shape& shape) const;
	const TopoDS_Shape& Modified(const TopoDS_Shape& shape) const;
	void Dump() const;
};
