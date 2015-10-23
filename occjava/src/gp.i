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
	#include <Standard_Version.hxx>
	#include <gp_Trsf.hxx>
	jdoubleArray XYZtoDoubleArray(JNIEnv* jenv, const gp_XYZ & xyz)
	{
	    jdouble nativeArray[]={xyz.X(), xyz.Y(), xyz.Z()};
		jdoubleArray toReturn=jenv->NewDoubleArray(3);
		jenv->SetDoubleArrayRegion(toReturn, 0, 3, nativeArray);
		return toReturn;
	}
    inline static void gp_Trsf_setValuesImpl(gp_Trsf * self, const Standard_Real a11,const Standard_Real a12,
        const Standard_Real a13,const Standard_Real a14,const Standard_Real a21,
        const Standard_Real a22,const Standard_Real a23,const Standard_Real a24,
        const Standard_Real a31,const Standard_Real a32,const Standard_Real a33,
        const Standard_Real a34,const Standard_Real Tolang,
        const Standard_Real TolDist) {
        self->SetValues(a11, a12, a13, a14,
                        a21, a22, a23, a24,
                        a31, a32, a33, a34
#if OCC_VERSION_MAJOR >= 6 && OCC_VERSION_MINOR >= 8
                        );
#else
                        ,Tolang, TolDist);
#endif
    }
%}

/**
 * gp_Pnt
 */

%typemap(jni) gp_Pnt, const gp_Pnt&  "jdoubleArray"
%typemap(jtype) gp_Pnt, const gp_Pnt& "double[]"
%typemap(jstype) gp_Pnt, const gp_Pnt& "double[]"

%typemap(in) gp_Pnt, const gp_Pnt&
{
	if(JCALL1(GetArrayLength, jenv, $input)!=3)
		SWIG_JavaThrowException(jenv, SWIG_JavaIllegalArgumentException, "array length must be 3");
	jdouble * naxe=JCALL2(GetDoubleArrayElements, jenv, $input, NULL);
	$1=new gp_Pnt(naxe[0],naxe[1],naxe[2]);
}

%typemap(out) const gp_Pnt&
{
    $result=XYZtoDoubleArray(jenv, $1->XYZ());
}

%typemap(out) gp_Pnt
{
    $result=XYZtoDoubleArray(jenv, $1.XYZ());
}

%typemap(freearg) gp_Pnt, const gp_Pnt&
{
	delete $1;
}

%typemap(javain) gp_Pnt, const gp_Pnt& "$javainput"
%typemap(javaout) gp_Pnt, const gp_Pnt&
{
	return $jnicall;
}

/**
 * gp_Pnt2d
 */

%typemap(jni) gp_Pnt2d, const gp_Pnt2d&  "jdoubleArray"
%typemap(jtype) gp_Pnt2d, const gp_Pnt2d& "double[]"
%typemap(jstype) gp_Pnt2d, const gp_Pnt2d& "double[]"

%typemap(in) gp_Pnt2d, const gp_Pnt2d&
{
	if(JCALL1(GetArrayLength, jenv, $input)!=2)
		SWIG_JavaThrowException(jenv, SWIG_JavaIllegalArgumentException, "array length must be 2");
	jdouble * naxe=JCALL2(GetDoubleArrayElements, jenv, $input, NULL);
	$1=new gp_Pnt2d(naxe[0],naxe[1]);
}

%typemap(freearg) gp_Pnt2d, const gp_Pnt2d&
{
	delete $1;
}

%typemap(javain) gp_Pnt2d, const gp_Pnt2d& "$javainput"
%typemap(javaout) gp_Pnt2d, const gp_Pnt2d&
{
	return $jnicall;
}

%typemap(out) gp_Pnt2d, const gp_Pnt2d&
{
    jdouble nativeArray[]={$1.X(), $1.Y()};
	jdoubleArray toReturn=JCALL1(NewDoubleArray, jenv, 2);
	JCALL4(SetDoubleArrayRegion, jenv, toReturn, 0, 2, nativeArray);
	$result=toReturn;
}
/**
 * gp_Pln
 */
%typemap(jni) gp_Pln, const gp_Pln&  "jdoubleArray"
%typemap(jtype) gp_Pln, const gp_Pln& "double[]"
%typemap(jstype) gp_Pln, const gp_Pln& "double[]"

%typemap(in) gp_Pln, const gp_Pln&
{
	if(JCALL1(GetArrayLength, jenv, $input)!=4)
		SWIG_JavaThrowException(jenv, SWIG_JavaIllegalArgumentException, "array length must be 4");
	jdouble * naxe=JCALL2(GetDoubleArrayElements, jenv, $input, NULL);
	$1=new gp_Pln(naxe[0],naxe[1],naxe[2],naxe[4]);
}

%typemap(freearg) gp_Pln, const gp_Pln&
{
	delete $1;
}

%typemap(javain) gp_Pln, const gp_Pln& "$javainput"
%typemap(javaout) gp_Pln, const gp_Pln&
{
	return $jnicall;
}

%typemap(out) gp_Pln, const gp_Pln &
{
	double a, b, c, d;
	$1.Coefficients(a, b, c, d);
	jdouble nativeArray[] = {a, b, c, d};
	jdoubleArray toReturn = JCALL1(NewDoubleArray, jenv, 4);
	JCALL4(SetDoubleArrayRegion, jenv, toReturn, 0, 4, nativeArray);
	$result=toReturn;
}

/**
 * gp_Vec
 */
%typemap(jni) gp_Vec, const gp_Vec&  "jdoubleArray"
%typemap(jtype) gp_Vec, const gp_Vec& "double[]"
%typemap(jstype) gp_Vec, const gp_Vec& "double[]"

%typemap(in) gp_Vec, const gp_Vec&
{
	if(JCALL1(GetArrayLength, jenv, $input)!=3)
		SWIG_JavaThrowException(jenv, SWIG_JavaIllegalArgumentException, "array length must be 3");
	jdouble * naxe=JCALL2(GetDoubleArrayElements, jenv, $input, NULL);
	$1=new gp_Vec(naxe[0],naxe[1],naxe[2]);
}

%typemap(freearg) gp_Vec, const gp_Vec&
{
	delete $1;
}

%typemap(javain) gp_Vec, const gp_Vec& "$javainput"
%typemap(javaout) gp_Vec, const gp_Vec&
{
	return $jnicall;
}

%typemap(out) gp_Vec, const gp_Vec&
{
	$result=XYZtoDoubleArray(jenv, $1->XYZ());
}

/**
 * gp_Dir
 */
%typemap(jni) gp_Dir, const gp_Dir&, gp_Dir&  "jdoubleArray"
%typemap(jtype) gp_Dir, const gp_Dir&, gp_Dir& "double[]"
%typemap(jstype) gp_Dir, const gp_Dir&, gp_Dir& "double[]"

%typemap(in) gp_Dir, const gp_Dir&
{
	if(JCALL1(GetArrayLength, jenv, $input)!=3)
		SWIG_JavaThrowException(jenv, SWIG_JavaIllegalArgumentException, "array length must be 3");
	jdouble * naxe=JCALL2(GetDoubleArrayElements, jenv, $input, NULL);
	$1=new gp_Dir(naxe[0],naxe[1],naxe[2]);
}

%typemap(freearg) gp_Dir, const gp_Dir&
{
	delete $1;
}

%typemap(out) const gp_Dir&
{
    $result=XYZtoDoubleArray(jenv, $1->XYZ());
}

%typemap(out) gp_Dir
{
    $result=XYZtoDoubleArray(jenv, $1.XYZ());
}

%typemap(javain) gp_Dir, const gp_Dir&, gp_Dir&  "$javainput"
%typemap(javaout) gp_Dir, const gp_Dir&, gp_Dir&
{
	return $jnicall;
}

/**
 * gp_Ax2
 */
%typemap(jni) gp_Ax2, const gp_Ax2&  "jdoubleArray"
%typemap(jtype) gp_Ax2, const gp_Ax2& "double[]"
%typemap(jstype) gp_Ax2, const gp_Ax2& "double[]"

%typemap(in) gp_Ax2, const gp_Ax2&
{
	if(JCALL1(GetArrayLength, jenv, $input)!=6)
		SWIG_JavaThrowException(jenv, SWIG_JavaIllegalArgumentException, "array length must be 6");
	jdouble * naxe=JCALL2(GetDoubleArrayElements, jenv, $input, NULL);
	$1=new gp_Ax2(gp_Pnt(naxe[0],naxe[1],naxe[2]), gp_Dir(naxe[3], naxe[4], naxe[5]));
}

%typemap(freearg) gp_Ax2, const gp_Ax2&
{
	delete $1;
}

%typemap(out) gp_Ax2, const gp_Ax2&
{
	##error TODO
}

%typemap(javain) gp_Ax2, const gp_Ax2& "$javainput"
%typemap(javaout) gp_Ax2, const gp_Ax2&
{
	return $jnicall;
}

/**
 * gp_Ax1
 */
%typemap(jni) gp_Ax1, const gp_Ax1&  "jdoubleArray"
%typemap(jtype) gp_Ax1, const gp_Ax1& "double[]"
%typemap(jstype) gp_Ax1, const gp_Ax1& "double[]"

%typemap(in) gp_Ax1, const gp_Ax1&
{
	if(JCALL1(GetArrayLength, jenv, $input)!=6)
		SWIG_JavaThrowException(jenv, SWIG_JavaIllegalArgumentException, "array length must be 6");
	jdouble * naxe=JCALL2(GetDoubleArrayElements, jenv, $input, NULL);
	$1=new gp_Ax1(gp_Pnt(naxe[0],naxe[1],naxe[2]), gp_Dir(naxe[3], naxe[4], naxe[5]));
}

%typemap(freearg) gp_Ax1, const gp_Ax1&
{
	delete $1;
}

%typemap(out) gp_Ax1, const gp_Ax1&
{
	##error TODO
}

%typemap(javain) gp_Ax1, const gp_Ax1& "$javainput"
%typemap(javaout) gp_Ax1, const gp_Ax1&
{
	return $jnicall;
}

/**
 * gp_Trsf
 */
 
%rename(GP_Trsf) gp_Trsf;

%typemap(javacode) gp_Trsf
%{
	public void setValues(double[] matrix) {
        setValues(matrix, 0, 0);
    }
    /** @Deprecated Since OCCT 6.8 tolAng and tolDist arguments not longer exists */
    @Deprecated
	public void setValues(double[] matrix, double tolAng, double tolDist)
	{
		if(matrix.length!=12)
			throw new IllegalArgumentException("matrix length must be 12");
		setValues(
			matrix[0], matrix[1], matrix[2], matrix[3],
			matrix[4], matrix[5], matrix[6], matrix[7],
			matrix[8], matrix[9], matrix[10], matrix[11],
			tolAng, tolDist);
	}
%}

class gp_Trsf
{
	%rename(setRotation) SetRotation;
	%rename(setTranslation) SetTranslation;
	%rename(setValues) SetValues;
	public:
	gp_Trsf();
	void SetRotation(const gp_Ax1& A1,const Standard_Real Ang) ;
	void SetTranslation(const gp_Vec& V) ;
};


%extend gp_Trsf
{
    //TODO Should be generated as deprecated but I don't know how to do
    void setValues(const Standard_Real a11,const Standard_Real a12,
        const Standard_Real a13,const Standard_Real a14,const Standard_Real a21,
        const Standard_Real a22,const Standard_Real a23,const Standard_Real a24,
        const Standard_Real a31,const Standard_Real a32,const Standard_Real a33,
        const Standard_Real a34,const Standard_Real Tolang,
        const Standard_Real TolDist) {
        gp_Trsf_setValuesImpl(self, a11, a12, a13, a14,
                        a21, a22, a23, a24,
                        a31, a32, a33, a34
                        ,Tolang, TolDist);
    }
    void setValues(const Standard_Real a11,const Standard_Real a12,
        const Standard_Real a13,const Standard_Real a14,const Standard_Real a21,
        const Standard_Real a22,const Standard_Real a23,const Standard_Real a24,
        const Standard_Real a31,const Standard_Real a32,const Standard_Real a33,
        const Standard_Real a34) {
        gp_Trsf_setValuesImpl(self, a11, a12, a13, a14,
                        a21, a22, a23, a24,
                        a31, a32, a33, a34, 0, 0);
    }

	/** Easy to use with javax.vecmath.Matrix4D */
	void getValues(double matrix[16])
	{
		int k=0;
		for(int i=1; i<=3; i++)
			for(int j=1; j<=4; j++)
				matrix[k++]=self->Value(i,j);
		matrix[12]=0;
		matrix[13]=0;
		matrix[14]=0;
		matrix[15]=1;
	}
}

/**
 * GP_Circ
 */
%{#include <gp_Circ.hxx>%}
%rename(GP_Circ) gp_Circ;
class gp_Circ
{
    public:
    gp_Circ(const gp_Ax2& axis, const Standard_Real radius);
};

/**
 * GP_Parab
 */
%{#include <gp_Parab.hxx>%}
%rename(GP_Parab) gp_Parab;
class gp_Parab
{
    public:
	gp_Parab(const gp_Ax2& A2,const Standard_Real Focal);
	gp_Parab(const gp_Ax1& D,const gp_Pnt& F);
};




