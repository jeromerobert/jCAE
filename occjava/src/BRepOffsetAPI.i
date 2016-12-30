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
 * (C) Copyright 2011, by EADS France
 */

%{
#include <BRepOffsetAPI_NormalProjection.hxx>
#include <BRepOffsetAPI_MakeOffsetShape.hxx>
%}

class BRepOffsetAPI_NormalProjection: public BRepBuilderAPI_MakeShape
{
	public:
	%rename(init) Init;
	%rename(add) Add;
	%rename(setParams) SetParams;
	%rename(setMaxDistance) SetMaxDistance;
	%rename(setLimit) SetLimit;
	%rename(compute3d) Compute3d;
	%rename(build) Build;
	%rename(isDone) IsDone;
	%rename(projection) Projection;
	%rename(couple) Couple;
	%rename(generated) Generated;
	%rename(ancestor) Ancestor;
	%rename(buildWire) BuildWire;
	BRepOffsetAPI_NormalProjection();
	BRepOffsetAPI_NormalProjection(const TopoDS_Shape& S);
	void Init(const TopoDS_Shape& S) ;
	void Add(const TopoDS_Shape& ToProj) ;
	void SetParams(const Standard_Real Tol3D,const Standard_Real Tol2D,const GeomAbs_Shape InternalContinuity,const Standard_Integer MaxDegree,const Standard_Integer MaxSeg) ;
	void SetMaxDistance(const Standard_Real MaxDist) ;
	void SetLimit(const Standard_Boolean FaceBoundaries = Standard_True) ;
	void Compute3d(const Standard_Boolean With3d = Standard_True) ;
	virtual  void Build() ;
	Standard_Boolean IsDone() const;
	const TopoDS_Shape& Projection() const;
	const TopoDS_Shape& Couple(const TopoDS_Edge& E) const;
	virtual const TopTools_ListOfShape& Generated(const TopoDS_Shape& S) ;
	const TopoDS_Shape& Ancestor(const TopoDS_Edge& E) const;
	Standard_Boolean BuildWire(TopTools_ListOfShape& Liste) const;
};

class BRepOffsetAPI_MakeOffsetShape : public BRepBuilderAPI_MakeShape {
};

