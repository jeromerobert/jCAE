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
#include <TopoDS_CompSolid.hxx>
#include <TopoDS_Solid.hxx>
#include <TopoDS_Shell.hxx>
#include <TopoDS_Compound.hxx>
#include <TopoDS_Wire.hxx>
#include <TopoDS_Iterator.hxx>
%}

/** 
 * TopoDS_Shape
 */
// Ensure that the Java class will always be the good one, so we can use
// instanceof operator.
// Problem: Returning a shape require 2 initialisation (the TopoDS_Shape
// and after the real TopoDS_XXXXX. It may reduce performance comparing
// to libOccJava.

%typemap(out) TopoDS_Shape
{
	if($1.IsNull())
	{
		$result=0L; //NULL
	}
	else
	{
		TopoDS_Shape * tsp=new TopoDS_Shape();
		tsp->TShape($1.TShape());
		tsp->Location($1.Location());
		tsp->Orientation($1.Orientation());
		$result=(jlong)tsp;
	}
}

%typemap(out) const TopoDS_Shape &, const TopoDS_Compound &
{
	if($1->IsNull())
	{
		$result=0L; //NULL
	}
	else
	{
		$1_basetype * tsp=new $1_basetype();
		tsp->TShape($1->TShape());
		tsp->Location($1->Location());
		tsp->Orientation($1->Orientation());
		$result=(jlong)tsp;
	}
}

%typemap(javaout) TopoDS_Shape*, TopoDS_Shape, const TopoDS_Shape&, const TopoDS_Compound &
{
    long cPtr = $jnicall;
    return (cPtr == 0) ? null : ($javaclassname)TopoDS_Shape.downcast(new $javaclassname(cPtr, $owner));
}

%typemap(javacode) TopoDS_Shape
%{
	private long myTShape;
	public static TopoDS_Shape downcast(TopoDS_Shape in)
	{
		TopoDS_Shape toReturn=null;
		if(in==null)
			return null;
		switch(in.shapeType())
		{
			case TopAbs_ShapeEnum.COMPOUND:
				toReturn=new TopoDS_Compound(getCPtr(in), true);
				break;
			case TopAbs_ShapeEnum.COMPSOLID:
				toReturn=new TopoDS_CompSolid(getCPtr(in), true);
				break;
			case TopAbs_ShapeEnum.SOLID:
				toReturn=new TopoDS_Solid(getCPtr(in), true);
				break;
			case TopAbs_ShapeEnum.SHELL:
				toReturn=new TopoDS_Shell(getCPtr(in), true);
				break;
			case TopAbs_ShapeEnum.FACE:
				toReturn=new TopoDS_Face(getCPtr(in), true);
				break;
			case TopAbs_ShapeEnum.WIRE:
				toReturn=new TopoDS_Wire(getCPtr(in), true);
				break;
			case TopAbs_ShapeEnum.EDGE:
				toReturn=new TopoDS_Edge(getCPtr(in), true);
				break;
			case TopAbs_ShapeEnum.VERTEX:
				toReturn=new TopoDS_Vertex(getCPtr(in), true);
				break;
		}
		in.swigCMemOwn=false;
		toReturn.myTShape=toReturn.getTShape();
		return toReturn;
	}
	
    public boolean isSame(TopoDS_Shape s)
    {
        /*if(myTShape==0)
			myTShape=getTShape();
		
		if(s.myTShape==0)
			s.myTShape=s.getTShape();
		
		if(myTShape==s.myTShape)
        {
            return nativeIsSame(s);
        }
        else return false;*/
		return nativeIsSame(s);
    }
	
    /**
     * Alias on the isSame method for an easy use of this class in java
     * collections
     */
    public boolean equals(Object o)
    {
        if (o instanceof TopoDS_Shape)
        {
            return isSame((TopoDS_Shape)o);
        }
        else return false;
    }

	public int hashCode()
	{
		return hashCode(Integer.MAX_VALUE);
	}
	
	protected static long[] cArrayUnwrap(TopoDS_Shape[] arrayWrapper)
	{
		long[] cArray = new long[arrayWrapper.length];
		for (int i=0; i<arrayWrapper.length; i++)
		cArray[i] = TopoDS_Shape.getCPtr(arrayWrapper[i]);
		return cArray;
	}	
%}

// Note that TopoDS_Shape is no longer abstract (it was in libOccJava)
class TopoDS_Shape
{
	public:
	%rename(shapeType) ShapeType;
	%rename(nativeIsSame) IsSame;
	%rename(orientation) Orientation;
	%rename(reverse) Reverse;
	%rename(reversed) Reversed;
	%rename(hashCode) HashCode;
	TopAbs_ShapeEnum ShapeType();
	Standard_Boolean IsSame(const TopoDS_Shape& other) const;
	TopAbs_Orientation Orientation() const;
	void Reverse() ;
	TopoDS_Shape Reversed() const;
	Standard_Integer HashCode(const Standard_Integer Upper) const;
};

%extend TopoDS_Shape
{
	public:
	//This will be used to speedup the equal operator as in libOccJava
	jlong getTShape()
	{
		return (jlong)&*self->TShape();
	}	
}

class TopoDS_Compound: public TopoDS_Shape
{
};

class TopoDS_CompSolid: public TopoDS_Shape
{
};

class TopoDS_Solid: public TopoDS_Shape
{
};

class TopoDS_Shell: public TopoDS_Shape
{
};

class TopoDS_Face: public TopoDS_Shape
{
};

class TopoDS_Wire: public TopoDS_Shape
{
};

class TopoDS_Edge: public TopoDS_Shape
{
};

class TopoDS_Vertex: public TopoDS_Shape
{
};

class TopoDS_Iterator
{
	%rename(initialize) Initialize;
	%rename(more) More;
	%rename(next) Next;
	%rename(value) Value;
	public:
	TopoDS_Iterator();
	TopoDS_Iterator(const TopoDS_Shape& S,
		const Standard_Boolean cumOri = Standard_True,
		const Standard_Boolean cumLoc = Standard_True);
	void Initialize(const TopoDS_Shape& S,
		const Standard_Boolean cumOri = Standard_True,
		const Standard_Boolean cumLoc = Standard_True) ;
	Standard_Boolean More() const;
	void Next() ;
	const TopoDS_Shape& Value() const;
};
