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

%{#include <ShapeBuild_ReShape.hxx>%}

class ShapeBuild_ReShape
{
	public:
	
	%rename(replace) Replace;
	%rename(apply) Apply;

	ShapeBuild_ReShape();
	void Replace(const TopoDS_Shape& shape,const TopoDS_Shape& newshape,const Standard_Boolean oriented = Standard_False) ;
	TopoDS_Shape Apply(const TopoDS_Shape& shape,const TopAbs_ShapeEnum until = TopAbs_SHAPE) ;
};


