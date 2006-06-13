REM Roots of required software

REM http://msdn.microsoft.com/vstudio/express/visualc/default.aspx
@SET VSINSTALLDIR=C:\Program Files\Microsoft Visual Studio 8

REM http://www.swig.org
@SET SWIGDIR=%USERPROFILE%\Bureau\swigwin-1.3.28

REM http://java.sun.org
@SET JDKDIR=C:\Program Files\Java\jdk1.5.0_06

REM http://www.opencascade.org
@SET CASROOT=C:\OpenCASCADE6.1.0\ros

REM Set Visual Studio environment variables
set VS80COMNTOOLS=%VSINSTALLDIR%\Common7\Tools\
CALL "%VSINSTALLDIR%\VC\vcvarsall.bat"

REM Extend default Visual Studio environment
set PATH=%SWIGDIR%;%PATH%
set INCLUDE=%JDKDIR%\include;%JDKDIR%\include\win32;%INCLUDE%;%CASROOT%\inc;%INCLUDE%
set LIB=%CASROOT%\win32\lib;%LIB%

REM Run the build
VCBUILD.EXE /u %*

