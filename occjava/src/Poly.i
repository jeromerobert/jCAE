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

%{#include <Poly_Triangulation.hxx>%}

%typemap(jni) Poly_Array1OfTriangle&  "jintArray"
%typemap(jtype) Poly_Array1OfTriangle& "int[]"
%typemap(jstype) Poly_Array1OfTriangle& "int[]"
%typemap(javaout) Poly_Array1OfTriangle&
{
	return $jnicall;
}

%typemap(out) const Poly_Array1OfTriangle&
{
    int i,j,s;
    const Poly_Array1OfTriangle &Triangles  = *$1;
    s=Triangles.Length()*3;
    jint * iarray=new jint[s];
    Standard_Integer n1,n2,n3;

    for(j=0,i=Triangles.Lower();i<=Triangles.Upper();j+=3,i++)
    {
        Triangles(i).Get(n1,n2,n3);
        iarray[j]=n1-1;
        iarray[j+1]=n2-1;
        iarray[j+2]=n3-1;
    }

    jintArray array=JCALL1(NewIntArray, jenv, s);
	JCALL4(SetIntArrayRegion, jenv, array, 0, s, iarray);
    delete[] iarray;
    $result=array;
}

class Poly_Triangulation
{
	%rename(deflection) Deflection;
	%rename(removeUVNodes) RemoveUVNodes;
	%rename(nbNodes) NbNodes;
	%rename(hasUVNodes) HasUVNodes;
	%rename(nbTriangles) NbTriangles;
	%rename(triangles) Triangles;
	%rename(nodes) Nodes;
	%rename(uvNodes) UVNodes;
	
	public:
	Poly_Triangulation(const Standard_Integer nbNodes,
		const Standard_Integer nbTriangles,const Standard_Boolean UVNodes);
	Standard_Real Deflection() const;
	void Deflection(const Standard_Real D) ;
	void RemoveUVNodes() ;
	Standard_Integer NbNodes() const;
	Standard_Integer NbTriangles() const;
	Standard_Boolean HasUVNodes() const;
	const Poly_Array1OfTriangle& Triangles() const;
	const TColgp_Array1OfPnt& Nodes() const;
	const TColgp_Array1OfPnt2d& UVNodes() const;
};
