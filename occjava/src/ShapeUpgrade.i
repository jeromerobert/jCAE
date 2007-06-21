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

%{ #include <ShapeUpgrade_RemoveInternalWires.hxx> %}
%{ #include <ShapeUpgrade_Tool.hxx> %}
%{ #include <ShapeUpgrade_ShapeDivideArea.hxx> %}
class ShapeUpgrade_Tool
{
	ShapeUpgrade_Tool()=0;
};

class ShapeUpgrade_RemoveInternalWires : public ShapeUpgrade_Tool
{
	public:
	%rename(perform) Perform;
	%rename(getResult) GetResult;
	ShapeUpgrade_RemoveInternalWires(const TopoDS_Shape& theShape);
	Standard_Boolean Perform() ;
	TopoDS_Shape GetResult() const; 
};

%extend ShapeUpgrade_RemoveInternalWires
{
	void setMinArea(double d)
	{
		self->MinArea()=d;
	}

	double getMinArea()
	{
		return self->MinArea();
	}

	void setRemoveFaceMode(Standard_Boolean b)
	{
		self->RemoveFaceMode()=b;
	}
	
	Standard_Boolean getRemoveFaceMode()
	{
		return self->RemoveFaceMode();
	}
};

class ShapeUpgrade_ShapeDivide
{
	public:
	%rename(getResult) Result;
	%rename(perform) Perform;
	TopoDS_Shape Result() const;
	Standard_Boolean Perform(const Standard_Boolean newContext = Standard_True) ;
};

class ShapeUpgrade_ShapeDivideArea : public ShapeUpgrade_ShapeDivide
{
	public:
	ShapeUpgrade_ShapeDivideArea(const TopoDS_Shape& S);  
};

%extend ShapeUpgrade_ShapeDivideArea
{
	void setMaxArea(double d)
	{
		self->MaxArea()=d;
	}

	double getMaxArea()
	{
		return self->MaxArea();
	}
};

