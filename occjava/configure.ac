# libOccJava is a Java wrapper for a small subset of Opencascade Libraries (www.opencascade.org)
#
#    Copyright (C) 2003 Jerome Robert <jeromerobert@users.sourceforge.net>
#
#    This library is free software; you can redistribute it and/or
#    modify it under the terms of the GNU Lesser General Public
#    License as published by the Free Software Foundation; either
#    version 2.1 of the License, or (at your option) any later version.
#
#    This library is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
#    Lesser General Public License for more details.
#
#    You should have received a copy of the GNU Lesser General Public
#    License along with this library; if not, write to the Free Software
#    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
#
#  
#  

AC_PREREQ([2.57])
AC_INIT([src/OccJava.i])
AM_INIT_AUTOMAKE(libOccJava, 1.0)
AM_CONFIG_HEADER([config.h:config.h.in])

AC_PROG_CXX
AC_PROG_CC
AC_PROG_INSTALL
AC_DISABLE_STATIC
AC_PROG_LIBTOOL

##############################
# Opencascade
##############################
AC_ARG_WITH(occ, AC_HELP_STRING([--with-occ],[Opencascade root directory ($CASROOT)]), [CASROOT=$withval])
AC_ARG_WITH(stlport-lib, AC_HELP_STRING([--with-stlport-lib],[Directory where to find libstlport_gcc.so]), [STLPORT_LIBDIR=$withval])
AC_ARG_WITH(stlport-inc, AC_HELP_STRING([--with-stlport-inc],[Directory where to find stlport headers]), [STLPORT_INCDIR=$withval])

# Opencascade need g++, not gcc
AC_LANG_PUSH(C++)

# Some header used by Opencascade
AC_CHECK_HEADERS([limits.h] [iostream])

# Configuring for Opencascade
if test -z "$CASROOT";
then
	# CASROOT not set, this is not an official release.
	# See if we can found includes and libs under /usr
	if test -d "/usr/include/opencascade";
	then
		occinc="/usr/include/opencascade"
	else
		AC_MSG_FAILURE("Opencascade not found");
	fi
	if test -r "/usr/lib/libTKernel.so";
	then
		occlib="/usr/lib"
	else
		AC_MSG_FAILURE("Opencascade not found");
	fi
else
	occinc="$CASROOT/inc"
	occlib="$CASROOT/`uname -s`/lib"
fi
CPPFLAGS="${CPPFLAGS} -I$occinc -DLIN"
LDFLAGS="${LDFLAGS} -L$occlib"
# OpenCascade libraries have no -rpath information, set
# LD_RUN_PATH (or LD_LIBRARY_PATH) so that we can set LIBS
# to the libs we need, without their dependencies.
eval "$shlibpath_var=\${$shlibpath_var}\${${shlibpath_var}:+:}\$occlib"
export $shlibpath_var
LIBS="${LIBS} -lTKBRep -lTKBool -lTKG2d -lTKG3d -lTKGeomBase -lTKIGES -lTKMath -lTKOffset -lTKXSBase -lTKernel -lTKSTEP -lTKPrim -lTKTopAlgo -lTKFillet"

if test -r "$occlib/libTKMesh.so"
then
	LIBS="${LIBS} -lTKMesh"
fi

if test -z "${STLPORT_LIBDIR}" ;
then
	STLPORT_LIBDIR="${CASROOT}/../3rdparty/`uname -s`/stlport" ;
fi

if test -r "${STLPORT_LIBDIR}/libstlport_gcc.so" ;
then 
	AC_MSG_NOTICE([Assuming opencascade is linked to ${STLPORT_LIBDIR}/libstlport_gcc.so])
	LIBS="${LIBS} -lstlport_gcc" ;
	LDFLAGS="${LDFLAGS} -L${STLPORT_LIBDIR}" ;
	CPPFLAGS="-I${STLPORT_INCDIR} ${CPPFLAGS}"
	AC_MSG_CHECKING([stlport headers])
	AC_COMPILE_IFELSE(AC_LANG_PROGRAM([#include <iostream>],[
#ifndef _STLP_IOSTREAM
#error Not using stlport headers
#endif
]), AC_MSG_RESULT(yes), AC_MSG_FAILURE(no))
fi

AC_MSG_CHECKING([if we can link with Opencascade])
AC_LINK_IFELSE(AC_LANG_PROGRAM([#include <gp.hxx>],[gp::Resolution]),AC_MSG_RESULT(yes),AC_MSG_FAILURE("Cannot link with Opencascade. Use --with-occ flag or set CASROOT environment variable."))

# back to gcc
AC_LANG_POP

#############################
# End of Opencascade
#############################

#############################
# Swig
#############################
# -fpermissive is required with g++ 4.6 until a better fix is found
CXXFLAGS="${CXXFLAGS} -fpermissive"
AC_PROG_SWIG(1.3.40)
SWIG_ENABLE_CXX

########
# JNI
########
AC_PROG_JAVAC
AC_JNI_INCLUDE_DIR
for JNI_INCLUDE_DIR in $JNI_INCLUDE_DIRS
do
	CPPFLAGS="$CPPFLAGS -I$JNI_INCLUDE_DIR"
done

AC_CONFIG_FILES([Makefile src/Makefile])
AC_OUTPUT

