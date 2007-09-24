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

//A workaround for charset encoding problems
%typemap(jni) jbyte[]  "jbyteArray"
%typemap(jtype) jbyte[] "byte[]"
%typemap(jstype) jbyte[] "byte[]"
%typemap(in) jbyte[]
{
	int length = JCALL1(GetArrayLength, jenv, $input);
	jbyte * name = new jbyte[length+1];
	JCALL4(GetByteArrayRegion, jenv, $input, 0, length, name); 
	name[length] = '\0';
	$1 = name;
}
%typemap(javain) jbyte[] "$javainput"


/**
 * XSControl_Reader
 */
 %{
#include <STEPControl_Reader.hxx>
#include <IGESControl_Reader.hxx>
 %}
class XSControl_Reader
{
	XSControl_Reader()=0;
	%rename(readFile) ReadFile;
	%rename(transferRoots) TransferRoots;
	%rename(clearShapes) ClearShapes;
	%rename(nbRootsForTransfer) NbRootsForTransfer;
	%rename(oneShape) OneShape;
	public:
	IFSelect_ReturnStatus ReadFile(const Standard_CString filename);
	//IFSelect_ReturnStatus ReadFile(jbyte filename[]);
	Standard_Integer TransferRoots() ;
	void ClearShapes();
	Standard_Integer NbRootsForTransfer();
	TopoDS_Shape OneShape() const;
};

%extend XSControl_Reader
{
    //A workaround for charset encoding problems
	IFSelect_ReturnStatus readFile(jbyte filename[])
	{
		return self->ReadFile((char*)filename);
	}
};

class STEPControl_Reader: public XSControl_Reader
{
	public:
	STEPControl_Reader();
};

class IGESControl_Reader: public XSControl_Reader
{
	public:
	IGESControl_Reader();
};


