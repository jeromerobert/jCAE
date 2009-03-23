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

%{#include <BRepAlgoAPI_BooleanOperation.hxx>%}
%{#include <BRepAlgoAPI_Fuse.hxx>%}
%{#include <BRepAlgoAPI_Common.hxx>%}
%{#include <BRepAlgoAPI_Cut.hxx>%}
%{#include <BRepAlgoAPI_Section.hxx>%}

class BRepAlgoAPI_BooleanOperation: public BRepBuilderAPI_MakeShape
{
	%rename(modified) Modified;
	%rename(isDeleted) IsDeleted;
	%rename(modified2) Modified2;
	%rename(generated) Generated;
	%rename(hasModified) HasModified;
	%rename(hasGenerated) HasGenerated;
	%rename(hasDeleted) HasDeleted;
	BRepAlgoAPI_BooleanOperation()=0;
	public:
	virtual const TopTools_ListOfShape& Modified(const TopoDS_Shape& aS) ;
	virtual Standard_Boolean IsDeleted(const TopoDS_Shape& aS) ;
	virtual const TopTools_ListOfShape& Modified2(const TopoDS_Shape& aS) ;
	virtual const TopTools_ListOfShape& Generated(const TopoDS_Shape& S) ;
	virtual Standard_Boolean HasModified() const;
	virtual Standard_Boolean HasGenerated() const;
	virtual Standard_Boolean HasDeleted() const;
};

class BRepAlgoAPI_Fuse: public BRepAlgoAPI_BooleanOperation
{
	public:
	BRepAlgoAPI_Fuse(const TopoDS_Shape& S1,const TopoDS_Shape& S2);
};

class BRepAlgoAPI_Common: public BRepAlgoAPI_BooleanOperation
{
	public:
	BRepAlgoAPI_Common(const TopoDS_Shape& S1,const TopoDS_Shape& S2);
};

class BRepAlgoAPI_Cut: public BRepAlgoAPI_BooleanOperation
{
	public:
	BRepAlgoAPI_Cut(const TopoDS_Shape& S1,const TopoDS_Shape& S2);
};

class BRepAlgoAPI_Section: public BRepAlgoAPI_BooleanOperation
{
	public:
	BRepAlgoAPI_Section(const TopoDS_Shape& S1,const TopoDS_Shape& S2);
};

