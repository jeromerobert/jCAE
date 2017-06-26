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
#include <BRepOffsetAPI_MakeOffset.hxx>
#include <BRepOffsetAPI_MakeThickSolid.hxx>
#include <BRepOffsetAPI_MakePipeShell.hxx>
%}

%include "BRepPrimAPI.i"

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

enum BRepOffset_Mode
{
	BRepOffset_Skin,
	BRepOffset_Pipe,
	BRepOffset_RectoVerso
};

class BRepOffsetAPI_MakeOffsetShape : public BRepBuilderAPI_MakeShape {
public:
	BRepOffsetAPI_MakeOffsetShape(const TopoDS_Shape& S,
		const Standard_Real Offset,
		const Standard_Real Tol,
		const BRepOffset_Mode Mode = BRepOffset_Skin,
		const Standard_Boolean Intersection = Standard_False,
		const Standard_Boolean SelfInter = Standard_False,
		const GeomAbs_JoinType Join = GeomAbs_Arc);
	void Build();
};

class BRepOffsetAPI_MakeThickSolid  : public BRepOffsetAPI_MakeOffsetShape {
public:
	BRepOffsetAPI_MakeThickSolid(
		const TopoDS_Shape& S, 
		const TopTools_ListOfShape& ClosingFaces, 
		const Standard_Real Offset, 
		const Standard_Real Tol);
};

class BRepOffsetAPI_MakeOffset : public BRepBuilderAPI_MakeShape {
public:
	BRepOffsetAPI_MakeOffset(const TopoDS_Wire& Spine,
		const GeomAbs_JoinType Join = GeomAbs_Arc);
	void Perform(const Standard_Real Offset,const Standard_Real Alt = 0.0);
	virtual void Build();
};
enum BRepBuilderAPI_TransitionMode
{
	BRepBuilderAPI_Transformed,
	BRepBuilderAPI_RightCorner,
	BRepBuilderAPI_RoundCorner
};

class BRepOffsetAPI_MakePipeShell : public BRepPrimAPI_MakeSweep {
public:
	BRepOffsetAPI_MakePipeShell(const TopoDS_Wire& Spine);
	virtual void Build();
	Standard_Boolean MakeSolid();
	void Add(const TopoDS_Shape& Profile,
		const Standard_Boolean WithContact = Standard_False,
		const Standard_Boolean WithCorrection = Standard_False);
	void SetTransitionMode(const BRepBuilderAPI_TransitionMode Mode = BRepBuilderAPI_Transformed);
	void SetMode(const gp_Ax2& Axe);
	void SetMode(const Standard_Boolean IsFrenet = Standard_False);
	Standard_Boolean SetMode(const TopoDS_Shape& SpineSupport);
};
