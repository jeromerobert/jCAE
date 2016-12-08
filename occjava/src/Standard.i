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

%typemap(javaimports) Standard_Transient
%{
    import java.util.*;
    import java.lang.reflect.*;
%}

%typemap(javacode) Standard_Transient
%{
    /** Cache for fast constructor search */
    private static Map<String, Constructor> constructors =
        new HashMap<String, Constructor>();

    /**
     * Downcast a Handle_Standard_Transient pointer to the lowest
     * available class in OccJava
     * @parameter ptr the pointer to get an object from
     * @parameter fallback the Class to use as return type if the lowest
     * one is not yet implemented in OccJava
     */
    public static Object downcastHandle(long ptr, Class fallback) {
        if(ptr == 0)
            return null;
        return downcastImpl(ptr, dynamicHandleType(ptr), fallback);
    }

    /**
     * Downcast a Standard_Transient pointer to the lowest
     * available class in OccJava
     * @parameter ptr the pointer to get an object from
     * @parameter fallback the Class to use as return type if the lowest
     * one is not yet implemented in OccJava
     */
    public static Object downcast(long ptr, Class fallback) {
        if(ptr == 0)
            return null;
        return downcastImpl(ptr, dynamicType(ptr), fallback);
    }

    private static Object downcastImpl(long ptr, String cName, Class fallback) {
        Constructor ct = constructors.get(cName);
        try {
            if(ct == null) {
                // TODO: may be something about Handle_ prefix
                Class clazz;
                try {
                    clazz = Class.forName("org.jcae.opencascade.jni." + cName);
                } catch(ClassNotFoundException ex) {
                    clazz = fallback;
                }
                ct = clazz.getDeclaredConstructor(Long.TYPE, Boolean.TYPE);
                constructors.put(cName, ct);
            }
            return ct.newInstance(ptr, true);

        // TODO: replace with ReflectiveOperationException ?
        } catch(NoSuchMethodException ex) {
            ex.printStackTrace();
        } catch(InstantiationException ex) {
            ex.printStackTrace();
        } catch(IllegalAccessException ex) {
            ex.printStackTrace();
        } catch(InvocationTargetException ex) {
            ex.printStackTrace();
        }
        return null;
    }
%}

class Standard_Transient {
    Standard_Transient()=0;
};

%extend Standard_Transient {
    /*
     * There is nothing very usefull in Standard_Type but the
     * type name so we remove a layer and just return the name.
     * We also want to be able to get the type without actually
     * instantiating the Java object so we make it static.
     */
    static const char * dynamicType(jlong ptr) {
        Standard_Transient * t = reinterpret_cast<Standard_Transient*>(ptr);
        return t->DynamicType()->Name();
    }
    static const char * dynamicHandleType(jlong ptr) {
        Handle_Standard_Transient * t = reinterpret_cast<Handle_Standard_Transient*>(ptr);
        return (*t)->DynamicType()->Name();
    }
}

