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


%typemap(jni) const TopTools_ListOfShape& "jlongArray"
%typemap(jtype) const TopTools_ListOfShape& "long[]"
%typemap(jstype) const TopTools_ListOfShape& "TopoDS_Shape[]"

%typemap(javain) const TopTools_ListOfShape& "TopoDS_Shape.cArrayUnwrap($javainput)"

%typemap(in) const TopTools_ListOfShape& (jlong *jarr, jsize sz)
{
	int i;
	if (!$input)
	{
		SWIG_JavaThrowException(jenv, SWIG_JavaNullPointerException, "null array");
		return $null;
	}
	sz = JCALL1(GetArrayLength, jenv, $input);
	jarr = JCALL2(GetLongArrayElements, jenv, $input, 0);
	if (!jarr)
	{
		return $null;
	}
	$1 = new TopTools_ListOfShape();
	for (i=0; i<sz; i++)
	{
		$1->Append(*(TopoDS_Shape*)jarr[i]);
	}
}

%typemap(freearg) const TopTools_ListOfShape&
%{
	delete $1;
%}

