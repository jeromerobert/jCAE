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

//Refine the catch keywoard
%{#include <Standard_ErrorHandler.hxx>%}

/*%exception
{
	try
	{
		$action
	}
	catch(Standard_Failure) 
	{
		SWIG_JavaThrowException(jenv, SWIG_JavaRuntimeException, Standard_Failure::Caught()->DynamicType()->Name());
		return $null;
	}
}*/
// Now we bind Opencascade types with Java types.
// /usr/share/swig1.3/java/java.swg contains many simple example to do that.
/**
 * Standard_CString
 */
%typemap(jni) Standard_CString "jstring"
%typemap(jtype) Standard_CString "String"
%typemap(jstype) Standard_CString "String"

%typemap(in) Standard_CString
{
  $1 = const_cast<$1_ltype>(JCALL2(GetStringUTFChars, jenv, $input, 0));
}

%typemap(out) Standard_CString %{ #error TODO %}

%typemap(javain) Standard_CString "$javainput"
%typemap(javaout) Standard_CString 
{
	return $jnicall;
}

/**
 * Standard_Boolean
 */
%typemap(jni) Standard_Boolean "jboolean"
%typemap(jtype) Standard_Boolean "boolean"
%typemap(jstype) Standard_Boolean "boolean"

%typemap(in) Standard_Boolean
{
  $1 = $input;
}

%typemap(out) Standard_Boolean %{ $result = (jboolean)$1; %}

%typemap(javain) Standard_Boolean "$javainput"
%typemap(javaout) Standard_Boolean 
{
	return $jnicall;
}

%typedef double Standard_Real;
%typedef int Standard_Integer;
%typedef double Quantity_Length;
%typedef double Quantity_Parameter;

/**
 * Standard_OStream
 */
%{ #include "jnistream.hxx" %}
%typemap(jni) Standard_OStream& "jobject"
%typemap(jtype) Standard_OStream& "java.nio.channels.WritableByteChannel"
%typemap(jstype) Standard_OStream& "java.nio.channels.WritableByteChannel"

%typemap(in) Standard_OStream&
%{
    jnistreambuf buf(jenv, $input);
    Standard_OStream os(&buf);
	$1=&os;
%}

%typemap(freearg) Standard_OStream&
%{
    $1->flush();
%}

%typemap(javain) Standard_OStream& "$javainput"
%typemap(javaout) Standard_OStream& 
{
	return $jnicall;
}

/**
 * Standard_IStream
 */
%typemap(jni) Standard_IStream& "jobject"
%typemap(jtype) Standard_IStream& "java.nio.channels.ReadableByteChannel"
%typemap(jstype) Standard_IStream& "java.nio.channels.ReadableByteChannel"

%typemap(in) Standard_IStream&
%{
	jnistreambuf buf(jenv, $input);
	Standard_IStream is(&buf);
	$1=&is;
%}

%typemap(javain) Standard_IStream& "$javainput"
%typemap(javaout) Standard_IStream& 
{
	return $jnicall;
}

%rename(Standard_Type) Handle_Standard_Type;
%typemap(javacode) Handle_Standard_Type %{  
  public boolean equals(Object obj) {
    boolean equal = false;
    if (obj instanceof $javaclassname)
      equal = ((($javaclassname)obj).swigCPtr == this.swigCPtr);
    return equal;
  }
  
  public int hashCode() { 
	   // Not sure if this is optimal as far hash-functions go but at least this is correct.
	   // I assume C objects are 64 bit aligned so getting rid of the 3 LSB seems like a good idea,
	   // and regardless this is semantically correct.
       return (int) (this.swigCPtr>>3);
    }
%}

class Handle_Standard_Type {};
