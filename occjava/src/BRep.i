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
#include <BRep_Tool.hxx>
#include <Geom_Curve.hxx>
#include <BRep_Builder.hxx>
#include <TopoDS_Builder.hxx>
%}

%typemap(javacode) BRep_Tool
%{
	public static double[] range(TopoDS_Edge edge)
	{
		double[] toReturn=new double[2];
		range(edge, toReturn);
		return toReturn;
	}

	public static Geom2d_Curve curveOnSurface(TopoDS_Edge e, TopoDS_Face f, double[] range)
	{
		double[] d2=new double[1];
		Geom2d_Curve toReturn=curveOnSurface(e, f, range, d2);
		range[1]=d2[0];
		return toReturn;
	}

	public static Geom_Curve curve(TopoDS_Edge e, double[] range)
	{
		double[] d2=new double[1];
		Geom_Curve toReturn=curve(e, range, d2);
		range[1]=d2[0];
		return toReturn;
	}
%}

%catches(Standard_NoSuchObject) BRep_Tool::Parameters;

class BRep_Tool
{
	%rename(pnt) Pnt;
	%rename(parameters) Parameters;
	%rename(parameter) Parameter;
	%rename(degenerated) Degenerated;
	%rename(hasContinuity) HasContinuity;
	%rename(continuity) Continuity;
	%rename(tolerance) Tolerance;
	%rename(curve) Curve;
	%rename(surface) Surface;
	%rename(curveOnSurface) CurveOnSurface;
	public:
	static const gp_Pnt Pnt(const TopoDS_Vertex& V) ;
	static gp_Pnt2d Parameters(const TopoDS_Vertex& V,const TopoDS_Face& F) ;
	static Standard_Real Parameter(const TopoDS_Vertex& V,const TopoDS_Edge& E) ;
	static Standard_Boolean Degenerated(const TopoDS_Edge& E) ;
	static Standard_Boolean HasContinuity(const TopoDS_Edge& E,const TopoDS_Face& F1,const TopoDS_Face& F2) ;
	static GeomAbs_Shape Continuity(const TopoDS_Edge& E,const TopoDS_Face& F1,const TopoDS_Face& F2) ;
	static Standard_Real Tolerance(const TopoDS_Face& F) ;
	static Standard_Real Tolerance(const TopoDS_Edge& E) ;
	static Standard_Real Tolerance(const TopoDS_Vertex& V);
	
	/*static const Handle_Geom_Curve& Curve(const TopoDS_Edge& E,
		Standard_Real& First,Standard_Real& Last) ;
	static const Handle_Geom_Surface& Surface(const TopoDS_Face& F) ;
	static const Handle_Geom2d_Curve& CurveOnSurface(const TopoDS_Edge& E,
		const TopoDS_Face& F,Standard_Real& First,Standard_Real& Last) ;*/
};

// Publish methods which return pointer instead of Handle. We do not need
// Handle because Java do the memory managment for us.
%extend BRep_Tool
{
	static Poly_Triangulation * triangulation(const TopoDS_Face& F,TopLoc_Location& L)
	{
		Handle_Poly_Triangulation hgc=BRep_Tool::Triangulation(F,L);
		if(hgc.IsNull())
			return NULL;
		else
			return (Poly_Triangulation*)hgc.get();
	}

	static void range(const TopoDS_Edge& E, double range[2])
	{
		BRep_Tool::Range(E, range[0], range[1]);
	}
	
	// new Handle is a little memory leak as this handle is never deleted
	static Handle_Geom_Curve * curve(const TopoDS_Edge& E,
		Standard_Real& First,Standard_Real& Last)
	{
		Handle_Geom_Curve * hgc=new Handle_Geom_Curve(BRep_Tool::Curve(E, First, Last));
		if(hgc->IsNull())
			return NULL;
		else
			return hgc;
	}
	
	static Handle_Geom_Surface * surface(const TopoDS_Face& F)
	{
		Handle_Geom_Surface * hgc=new Handle_Geom_Surface(BRep_Tool::Surface(F));
		if(hgc->IsNull())
			return NULL;
		else
			return hgc;
	}
	
	static Handle_Geom2d_Curve * curveOnSurface(const TopoDS_Edge& E,
		const TopoDS_Face& F,Standard_Real& First,Standard_Real& Last)
	{
		Handle_Geom2d_Curve * hgc=new Handle_Geom2d_Curve(BRep_Tool::CurveOnSurface(E, F, First, Last));
		if(hgc->IsNull())
			return NULL;
		else
			return hgc;
	}
};

class BRep_Builder: public TopoDS_Builder
{
	%rename(updateVertex) UpdateVertex;
	public:
	BRep_Builder();
	void UpdateVertex(const TopoDS_Vertex& vertex, const Standard_Real u, const Standard_Real v, const TopoDS_Face& face, const Standard_Real tolerance) const;
	void UpdateVertex(const TopoDS_Vertex& vertex, const Standard_Real tolerance) const;
};

