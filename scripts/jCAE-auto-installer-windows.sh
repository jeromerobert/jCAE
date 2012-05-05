#!/bin/sh

##################################################
## Preliminaries
##################################################

#In addition to dependencies mentioned in the linux installer, the required packages are:


#1. mingw-w64 debian package (http://packages.debian.org/sid/mingw-w64)
#	- This will require editing /etc/apt/sources.list
# Steps
#	$ sudo echo 'deb http://ftp.de.debian.org/debian sid main' >> /etc/apt/sources.list
#	$ sudo apt-get install mingw-w64
#	

#2. wine 1.2
#	$ sudo apt-get install wine1.2

#Some other steps need to be performed before moving ahead
#
#1. sudo gedit /usr/share/cmake-2.8/Modules/Platform/Windows-GNU.cmake 
#	- This requires to be automated

##################################################
## Script Variables
##################################################

# Installer command line arguments
export makeSpeed;

# TODO: check for supplied args
if [ "$1" = "" ]
then
	makeSpeed="2"
else
	makeSpeed=$1
fi

# location variables
export mypwd=$PWD

##################################################
## Detect dependencies
##################################################
# This does not take care of version numbers.

# Step 1: Check for gcc
#echo "Looking for GCC"
#GCC_V=$(gcc --version | grep gcc)
#if [ $? -eq 1 ]
#then
#	echo -e "\033[31m" "gcc Not found" "\033[0m"
#	exit 1
#else
#	echo -e "\033[32m" $GCC_V "\033[0m"
#fi

# Step 2: Check for git
echo "Looking for GIT"
GIT_V=$(git --version | grep git)
if [ $? -eq 1 ]
then
	echo -e "\033[31m" "git Not found" "\033[0m"
	exit 1
else
	echo -e "\033[32m" $GIT_V "\033[0m"
fi

# Step 3: Check for cmake
echo "Looking for CMAKE"
CMAKE_V=$(cmake --version | grep cmake)
if [ $? -eq 1 ]
then
	echo -e "\033[31m" "cmake Not found" "\033[0m"
	exit 1
else
	echo -e "\033[32m" $CMAKE_V "\033[0m"
fi

# Step 4: Check for swig
echo "Looking for SWIG"
SWIG_V=$(swig -version | grep SWIG)
if [ $? -eq 1 ]
then
	echo -e "\033[31m" "swig Not found" "\033[0m"
	exit 1
else
	echo -e "\033[32m" $SWIG_V "\033[0m"
fi

# Step 5: Check for mesa-common-dev
#echo "Looking for mesa-common-dev"
#MCD_V=$(dpkg -l | grep mesa-common-dev)
#if [ $? -eq 1 ]
#then
#	echo -e "\033[31m" "mesa-common-dev Not found" "\033[0m"
#	exit 1
#else
#	echo -e "\033[32m" $MCD_V "\033[0m"
#fi

# Step 6: Check for libxt-dev
#echo "Looking for libxt-dev"
#LIBXT_V=$(dpkg -l | grep libxt-dev)
#if [ $? -eq 1 ]
#then
#	echo -e "\033[31m" "libxt-dev Not found" "\033[0m"
#	exit 1
#else
#	echo -e "\033[32m" $LIBXT_V "\033[0m"
#fi

# Step 7: Check for freeglut3-dev
#echo "Looking for freeglut3-dev"
#LIBFG3_V=$(dpkg -l | grep freeglut3-dev)
#if [ $? -eq 1 ]
#then
#	echo -e "\033[31m" "freeglut3-dev Not found" "\033[0m"
#	exit 1
#else
#	echo -e "\033[32m" $LIBFG3_V "\033[0m"
#fi

# Step 8: Check for openjdk-6-jdk
echo "Looking for openjdk-6-jdk"
LIBJDK_V=$(dpkg -l | grep openjdk-6-jdk)
if [ $? -eq 1 ]
then
	echo -e "\033[31m" "openjdk-6-jdk Not found" "\033[0m"
	exit 1
else
	echo -e "\033[32m" $LIBJDK_V "\033[0m"
fi

# Step 9: Check for quilt
echo "Looking for quilt"
LIBQLT_V=$(quilt --version)
if [ $? -eq 1 ]
then
	echo -e "\033[31m" "quilt Not found" "\033[0m"
	exit 1
else
	echo -e "\033[32m" $LIBQLT_V "\033[0m"
fi

# Step 10: Check for ant
echo "Looking for ant"
LIBANT_V=$(ant -version)
if [ $? -eq 1 ]
then
	echo -e "\033[31m" "ant Not found" "\033[0m"
	exit 1
else
	echo -e "\033[32m" $LIBANT_V "\033[0m"
fi

# Step 11: Check for gcc-mingw-w64
echo "Looking for gcc-mingw-w64"
LIBMINGW_V=$(dpkg -l | grep gcc-mingw-w64)
if [ $? -eq 1 ]
then
	echo -e "\033[31m" "gcc-mingw-w64 Not found" "\033[0m"
	exit 1
else
	echo -e "\033[32m" $LIBMINGW_V "\033[0m"
fi

# Step 12: Check for wine
echo "Looking for wine"
LIBWINE_V=$(wine --version)
if [ $? -eq 1 ]
then
	echo -e "\033[31m" "wine Not found" "\033[0m"
	exit 1
else
	echo -e "\033[32m" $LIBWINE_V "\033[0m"
fi

##################################################
## Install JDK 1.7  on wine
##################################################
wineDir=$mypwd/.wine
wineJavaDir=$wineDir/drive_c/Java
wineJavaUrl="http://www.java.net/download/jdk6/6u32/promoted/b02/binaries/jdk-6u32-ea-bin-b02-windows-i586-30_jan_2012.exe"
wineJavaExec="jdk-6u32-ea-bin-b02-windows-i586-30_jan_2012.exe"

# check for $wineDir presence

export WINEPREFIX=$wineDir
ret=$(ls "$wineDir")
if [ $? -ne 0 ]
then
	mkdir $wineDir
	winecfg
fi

# check for jdk installation
ret=$(find "$wineDir" -iname java.exe)
if [ "$ret" = "" ]
then
	# Install jdk
	mkdir $wineJavaDir
	wget $wineJavaUrl
	wine $wineJavaExec /s /v"/qn INSTALLDIR=$wineJavaDir"
fi

##################################################
## Get, Patch (from jCAE) and Install VTK 
## Get jCAE (installation later)
##################################################

# Define abs locations
vtkURL="http://www.vtk.org/files/release/5.6/vtk-5.6.1.tar.gz"
vtkTar="vtk-5.6.1.tar.gz"
vtkDir=$mypwd/VTK
vtkLinBuildDir=$mypwd/vtkLinBuild
vtkLinInstallDir=$mypwd/vtkLinInstall
vtkWinBuildDir=$mypwd/vtkWinBuild
vtkWinInstallDir=$mypwd/vtkWinInstall

jcaeURL=https://github.com/jeromerobert/jCAE.git
jcaeDir=$mypwd/jCAE

# Get vtk-5.6.1, unzip 
ret=$(ls $vtkTar)
if [ $? -ne 0 ]
then
	wget $vtkURL
fi

ret=$(ls $vtkDir)
if [ $? -ne 0 ]
then
	tar -xf $vtkTar
fi

# Get jCAE source (so early to get vtk patch)
ret=$(ls $jcaeDir)
if [ $? -ne 0 ]
then
	git clone $jcaeURL
fi

# Apply patch
cd $vtkDir
$jcaeDir/vtk-util/patch/5.6/apply.sh
cd $mypwd

ret=$(ls $vtkLinBuildDir)
if [ $? -ne 0 ]
then
	mkdir $vtkLinBuildDir
fi

ret=$(find $vtkLinBuildDir -iname vtk.jar)
if [ "$ret" = "" ]
then
	cd $vtkLinBuildDir
	flags="-DCMAKE_INSTALL_PREFIX:PATH=$vtkLinInstallDir"
	flags="$flags -DBUILD_SHARED_LIBS:BOOL=ON"
	flags="$flags -DVTK_WRAP_JAVA:BOOL=ON"
	flags="$flags -DCMAKE_CXX_FLAGS:STRING=-fpermissive"
	cmake $flags $vtkDir
	make -j$makeSpeed
fi
cd $mypwd

# Cross build
ret=$(ls $vtkWinBuildDir)
if [ $? -ne 0 ]
then
	mkdir $vtkWinBuildDir
fi

ret=$(find $vtkWinBuildDir -iname vtk.jar)
if [ "$ret" = "" ]
then
	cd $vtkWinBuildDir
	flags=" -DBUILD_SHARED_LIBS:BOOL=ON"
	flags="$flags -DVTK_WRAP_JAVA:BOOL=ON"
	flags="$flags -DBUILD_TESTING:BOOL=OFF"
	flags="$flags -DCMAKE_SHARED_LINKER_FLAGS:STRING=-Wl,--kill-at"
	flags="$flags -DJAVA_ARCHIVE:FILEPATH=/usr/bin/jar"

	flags="$flags -DJAVA_AWT_INCLUDE_PATH:PATH=$wineJavaDir/include"
	flags="$flags -DJAVA_AWT_LIBRARY:FILEPATH=$wineJavaDir/lib/jawt.lib"
	flags="$flags -DJAVA_COMPILE:FILEPATH=$wineJavaDir/bin/javac.exe"
	flags="$flags -DJAVA_INCLUDE_PATH:PATH=$wineJavaDir/include"
	flags="$flags -DJAVA_INCLUDE_PATH2:PATH=$wineJavaDir/include/win32"
	flags="$flags -DJAVA_JVM_LIBRARY:FILEPATH=$wineJavaDir/lib/jvm.lib"
	flags="$flags -DJAVA_RUNTIME:FILEPATH=$wineJavaDir/bin/java.exe"

	flags="$flags -DCMAKE_INSTALL_PREFIX:FILEPATH=$vtkWinInstallDir/"
	flags="$flags -DCMAKE_TOOLCHAIN_FILE:FILEPATH=$jcaeDir/vtk-util/toolchain-i686-w64-mingw32.cmake"
	flags="$flags -DVTKCompileTools_DIR:FILEPATH=$vtkLinBuildDir"

	flags="$flags -DCMAKE_CXX_FLAGS:STRING=-fpermissive"

	cmake $flags $vtkDir
	cmake $flags $vtkDir

	make -j$makeSpeed
fi

ret=$(ls $vtkWinInstallDir)
if [ $? -ne 0 ]
then
	cd $mypwd
	mkdir $vtkWinInstallDir
fi

ret=$(find $vtkWinInstallDir -iname vtk.jar)
if [ "$ret" = "" ]
then
	cd $vtkWinBuildDir
	make install
fi

cd $mypwd

##################################################
## Get and Install OCE 0.9.1
##################################################

oceURL=https://github.com/tpaviot/oce.git
oceDir=$mypwd/oce
oceWinBuildDir=$mypwd/oceWinBuild
oceWinInstallDir=$mypwd/oceWinInstall

cd $mypwd
echo -e "\033[32m Fetching oce from Github \033[0m"
ret=$(ls $oceDir)
if [ $? -ne 0 ]
then
	git clone $oceURL $oceDir
fi

cd $oceDir
git checkout OCE-0.9.1
cd $mypwd

ret=$(ls $oceWinBuildDir)
if [ $? -ne 0 ]
then
	mkdir $oceWinBuildDir	
fi

ret=$(find $oceWinBuildDir -iname *Tk*.dll)
if [ "$ret" = "" ]
then
	cd $oceWinBuildDir
	flags="-DOCE_INSTALL_PREFIX:PATH=$oceWinInstallDir"
	flags="$flags -DOCE_DISABLE_BSPLINE_MESHER:BOOL=ON"
	flags="$flags -DCMAKE_CXX_FLAGS:STRING=-DMMGT_OPT_DEFAULT=0"
	flags="$flags -DOCE_DISABLE_X11=ON"
	flags="$flags -DJAVA_ARCHIVE:FILEPATH=/usr/bin/jar"
	flags="$flags -DJAVA_AWT_INCLUDE_PATH:PATH=$wineJavaDir/include"
	flags="$flags -DJAVA_AWT_LIBRARY:FILEPATH=$wineJavaDir/lib/jawt.lib"
	flags="$flags -DJAVA_COMPILE:FILEPATH=$wineJavaDir/jre/bin/javacpl.exe"
	flags="$flags -DJAVA_INCLUDE_PATH:PATH=$wineJavaDir/include"
	flags="$flags -DJAVA_INCLUDE_PATH2:PATH=$wineJavaDir/include/win32"
	flags="$flags -DJAVA_JVM_LIBRARY:FILEPATH=$wineJavaDir/lib/jvm.lib"
	flags="$flags -DJAVA_RUNTIME:FILEPATH=$wineJavaDir/jre/bin/java.exe"
	flags="$flags -DCMAKE_TOOLCHAIN_FILE:FILEPATH=$jcaeDir/vtk-util/toolchain-i686-w64-mingw32.cmake"
	flags="$flags -DCMAKE_CXX_FLAGS:STRING=-fpermissive"
	cmake $flags $oceDir
	make -j$makeSpeed
fi

ret=$(ls $oceWinInstallDir)
if [ $? -ne 0 ]
then
	mkdir $oceWinInstallDir
fi

ret=$(find $oceWinInstallDir -iname *Tk*.dll)
if [ "$ret" = "" ]
then
	cd $oceWinBuildDir
	make install
fi

cd $mypwd

##################################################
## PATCH # Copy libgcc_s_sjlj-1.dll libstdc++-6.dll
##################################################

cd $oceWinInstallDir/Win32/bin/

Is64=$(objdump -f TKBO.dll | grep 64)

if [$Is64 -eq ""]
then
	cp $(find /usr/lib/ -path *i686*  -iname libstdc++-6.dll) .
	cp $(find /usr/lib/ -path *i686*  -iname libgcc_s_sjlj-1.dll) .
else
	cp $(find /usr/lib/ -path *x86_64*  -iname libstdc++-6.dll) .
	cp $(find /usr/lib/ -path *x86_64*  -iname libgcc_s_sjlj-1.dll) .
fi

cd ../../../


##################################################
## Get and Install JYTHON
##################################################

jythonURL=http://sourceforge.net/projects/jython/files/jython/2.5.2/jython_installer-2.5.2.jar
jythonJar=jython_installer-2.5.2.jar
jythonDir=$mypwd/jython

ret=$(ls $jythonJar)
if [ $? -ne 0 ]
then
	wget $jythonURL
fi

ret=$(ls $jythonDir)
if [ $? -ne 0 ]
then
	java -jar $jythonJar -s -d $jythonDir
fi


##################################################
## Get and Install VECMATH
##################################################
vecmathURL=http://ftp.fr.debian.org/debian/pool/main/v/vecmath/libvecmath-java_1.5.2-2_all.deb
vecmathDebian=libvecmath-java_1.5.2-2_all.deb
vecmathDir=$mypwd/vecmath

ret=$(ls $vecmathDebian)
if [ $? -ne 0 ]
then
	wget $vecmathURL
fi

ret=$(ls $vecmathDir)
if [ $? -ne 0 ]
then
	dpkg-deb -x $vecmathDebian $vecmathDir
fi


##################################################
## Get and Install TROVE
##################################################
troveURL=http://ftp.fr.debian.org/debian/pool/main/t/trove/libtrove-java_2.1.0-2_all.deb
troveDebian=libtrove-java_2.1.0-2_all.deb
troveDir=$mypwd/trove
ret=$(ls $troveDebian)
if [ $? -ne 0 ]
then
	wget $troveURL
fi

ret=$(ls $troveDir)
if [ $? -ne 0 ]
then
	dpkg-deb -x $troveDebian $troveDir
fi


##################################################
## Get and Install Netbeans 7.1
##################################################
nbURL=http://dlc.sun.com.edgesuite.net/netbeans/7.1.1/final/bundles/netbeans-7.1.1-ml-javase-linux.sh
nbEx=netbeans-7.1.1-ml-javase-linux.sh
nbDir=$mypwd/nb

ret=$(ls $nbEx)
if [ $? -ne 0 ]
then
	wget $nbURL
	chmod a+x $nbEx
fi

ret=$(ls $nbDir)
if [ $? -ne 0 ]
then
	mkdir $nbDir
	./$nbEx --silent "-J-Dnb-base.installation.location=$nbDir"
fi


##################################################
## Get and Install XALAN
## Get XSLs for build-impl.xml creation
##################################################
xalanURL=http://mirror.mkhelif.fr/apache//xml/xalan-j/xalan-j_2_7_1-bin-2jars.tar.gz
xalanTar=xalan-j_2_7_1-bin-2jars.tar.gz
xalanDir=$mypwd/xalan-j_2_7_1

ret=$(ls $xalanTar)
if [ $? -ne 0 ]
then
	wget $xalanURL
fi

ret=$(ls $xalanDir)
if [ $? -ne 0 ]
then
	tar xf $xalanTar	
fi

export CLASSPATH=$CLASSPATH:$xalanDir/xalan.jar
export CLASSPATH=$CLASSPATH:$xalanDir/serializer.jar
export CLASSPATH=$CLASSPATH:$xalanDir/xercesImpl.jar
export CLASSPATH=$CLASSPATH:$xalanDir/xml-apis.jar
export CLASSPATH=$CLASSPATH:$xalanDir/xsltc.jar

################
#XSLS
################
xslDir=$mypwd/xsls
nbProjectXslURL=http://hg.netbeans.org/releases/raw-file/5dfb0137e99e/java.j2seproject/src/org/netbeans/modules/java/j2seproject/resources/build-impl.xsl
nbSuiteXslURL=http://hg.netbeans.org/main/raw-file/c2719a24ed74/apisupport.ant/src/org/netbeans/modules/apisupport/project/suite/resources/build-impl.xsl
nbModuleXslURL=http://hg.netbeans.org/main/raw-file/c2719a24ed74/apisupport.ant/src/org/netbeans/modules/apisupport/project/resources/build-impl.xsl
nbPlatformXslURL=http://hg.netbeans.org/main/raw-file/c2719a24ed74/apisupport.ant/src/org/netbeans/modules/apisupport/project/suite/resources/platform.xsl

ret=$(ls $xslDir)
if [ $? -ne 0 ]
then
	mkdir $xslDir	
fi

ret=$(ls -1 $xslDir | wc -l)
if [ "$ret" != "4" ]
then
	ret=$(ls $xslDir/project-build-impl.xsl)
	if [ $? -ne 0 ]
	then
		wget $nbProjectXslURL
		mv build-impl.xsl $xslDir/project-build-impl.xsl
	fi

	ret=$(ls $xslDir/suite-build-impl.xsl)
	if [ $? -ne 0 ]
	then
		wget $nbSuiteXslURL
		mv build-impl.xsl $xslDir/suite-build-impl.xsl
	fi

	ret=$(ls $xslDir/module-build-impl.xsl)
	if [ $? -ne 0 ]
	then
		wget $nbModuleXslURL
		mv build-impl.xsl $xslDir/module-build-impl.xsl
	fi

	ret=$(ls $xslDir/platform.xsl)
	if [ $? -ne 0 ]
	then
		wget $nbPlatformXslURL
		mv platform.xsl $xslDir/platform.xsl	
	fi
fi


##################################################
## jCAE INSTALLATION
##################################################

#-------------------------------------------------
## Set envirmonment variables
#-------------------------------------------------

export jythonPath=$(find $jythonDir -iname jython.jar)
export trovePath=$(find $troveDir -iname trove.jar)
export vecmathPath=$(find $vecmathDir -iname vecmath.jar)
export vtkPath=$(find $vtkLinBuildDir -iname vtk.jar)

touch jcae.config
echo "" > jcae.config

echo "libs.trove.classpath=$trovePath" >> jcae.config
echo "libs.vecmath.classpath=$vecmathPath" >> jcae.config
echo "libs.VTK.classpath=$vtkPath" >> jcae.config

echo "arch.win32=true" >> jcae.config
echo "path.occ.win32=$oceWinInstallDir/Win32/bin/" >> jcae.config
echo "path.jython.win32=$jythonDir" >> jcae.config
echo "vtk.dir.win32=$vtkWinInstallDir/bin/" >> jcae.config
echo "path.occjava.win32=$mypwd/occjavaBuild/OccJava.dll" >> jcae.config

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$mypwd/oceWinInstall/Win32/bin/
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$mypwd/vtkWinInstall/bin/

#-------------------------------------------------
## Clean jCAE and occjava 
#-------------------------------------------------
# Cleaning jCAE from any previous wrong built requires 
# an extraordinary amount of checking/editing in 
# a lot of properties files. 
# TODO: One idea would be to run -
# cd $mypwd; 
# ant -Dnbplatform.default.netbeans.dest.dir="$nbDir" -Dnbplatform.default.harness.dir="$nbDir/harness/" clean
# Will try it in the unified script

# For the time being, cleaning is done like this, as it is not as time oriented task as vtk/oce/xalan/netbeans
rm -rf $jcaeDir
rm -rf $mypwd/occjavaInstall
git clone $jcaeURL


#-------------------------------------------------
## Build jCAE projects (vtk-util, occjava, etc)
#-------------------------------------------------

## build OccJava
occjavaDir=$jcaeDir/occjava
occjavaBuildDir=$mypwd/occjavaBuild
occjavaInstallDir=$mypwd/occjavaInstall/

mkdir $occjavaBuildDir
cd $occjavaBuildDir

flags=""
flags="$flags -DSWIG_DIR=/usr/bin/"
flags="$flags -DSWIG_EXECUTABLE=/usr/bin/swig"
flags="$flags -DOCE_DIR=$oceWinInstallDir/cmake/"

flags="$flags -DJAVA_ARCHIVE:FILEPATH=/usr/bin/jar"
flags="$flags -DJAVA_AWT_INCLUDE_PATH:PATH=$wineJavaDir/include"
flags="$flags -DJAVA_AWT_LIBRARY:FILEPATH=$wineJavaDir/lib/jawt.lib"
flags="$flags -DJAVA_COMPILE:FILEPATH=$wineJavaDir/jre/bin/javacpl.exe"
flags="$flags -DJAVA_INCLUDE_PATH:PATH=$wineJavaDir/include"
flags="$flags -DJAVA_INCLUDE_PATH2:PATH=$wineJavaDir/include/win32"
flags="$flags -DJAVA_JVM_LIBRARY:FILEPATH=$wineJavaDir/lib/jvm.lib"
flags="$flags -DJAVA_RUNTIME:FILEPATH=$wineJavaDir/jre/bin/java.exe"
flags="$flags -DCMAKE_TOOLCHAIN_FILE:FILEPATH=$jcaeDir/vtk-util/toolchain-i686-w64-mingw32.cmake"
flags="$flags -DCMAKE_CXX_FLAGS:STRING=-fpermissive"

cmake $flags $occjavaDir
make

mkdir $occjavaInstallDir
cp *.dll* $occjavaInstallDir
cd $mypwd

## building vtk-util
cd $jcaeDir/vtk-util/
mkdir nbproject/private
touch nbproject/private/private.properties
cat $mypwd/jcae.config >> nbproject/private/private.properties
ant config
ant clean
ant -Dnbplatform.default.netbeans.dest.dir="$nbDir/" -Dnbplatform.default.harness.dir="$nbDir/harness/"
cd $mypwd

## building jcae/occjava
cd $jcaeDir/jcae/occjava
mkdir nbproject/private
touch nbproject/private/private.properties
cat $mypwd/jcae.config >> nbproject/private/private.properties
java org.apache.xalan.xslt.Process -IN nbproject/project.xml -XSL $xslDir/project-build-impl.xsl -OUT nbproject/build-impl.xml
ant -Dnbplatform.default.netbeans.dest.dir="$nbDir/" -Dnbplatform.default.harness.dir="$nbDir/harness/" jar
cd $mypwd

## building amibe
cd $jcaeDir/amibe
mkdir nbproject/private
touch nbproject/private/private.properties
cat $mypwd/jcae.config >> nbproject/private/private.properties
java org.apache.xalan.xslt.Process -IN nbproject/project.xml -XSL $xslDir/project-build-impl.xsl -OUT nbproject/build-impl.xml
ant -Dnbplatform.default.netbeans.dest.dir="$nbDir/" -Dnbplatform.default.harness.dir="$nbDir/harness/" -f nbbuild.xml jar
cd $mypwd

## building vtk-amibe (src location at jCAE/vtk-amibe/, DONT know why?)
cd $jcaeDir/jcae/vtk-amibe
mkdir nbproject/private
touch nbproject/private/private.properties
cat $mypwd/jcae.config >> nbproject/private/private.properties
java org.apache.xalan.xslt.Process -IN nbproject/project.xml -XSL $xslDir/project-build-impl.xsl -OUT nbproject/build-impl.xml
ant -Dnbplatform.default.netbeans.dest.dir="$nbDir/" -Dnbplatform.default.harness.dir="$nbDir/harness/" jar
cd $mypwd

## building vtk-amibe-occ
cd $jcaeDir/vtk-amibe-occ
mkdir nbproject/private
touch nbproject/private/private.properties
cat $mypwd/jcae.config >> nbproject/private/private.properties
java org.apache.xalan.xslt.Process -IN nbproject/project.xml -XSL $xslDir/project-build-impl.xsl -OUT nbproject/build-impl.xml
ant -Dnbplatform.default.netbeans.dest.dir="$nbDir/" -Dnbplatform.default.harness.dir="$nbDir/harness/" jar
cd $mypwd

#-------------------------------------------------
## Build jCAE modules 
#-------------------------------------------------

cd $jcaeDir/jcae

modules="amibe amibe-occ core jython mesh-algos occjava-nb trove tweakui vecmath vtk vtk-util"
for module in $modules
do
  cd $jcaeDir/jcae
  cd "$module"
  java org.apache.xalan.xslt.Process -IN nbproject/project.xml -XSL $xslDir/module-build-impl.xsl -OUT nbproject/build-impl.xml
done


#-------------------------------------------------
## Generate platform.xml.
#-------------------------------------------------

# This is a now an automated step. 
cd $jcaeDir/jcae
java org.apache.xalan.xslt.Process -IN nbproject/project.xml -XSL $xslDir/platform.xsl -OUT nbproject/platform.xml

#-------------------------------------------------
## Build suite as Zip distribution
#-------------------------------------------------
mkdir nbproject/private
touch nbproject/private/private.properties
cat $mypwd/jcae.config > nbproject/private/private.properties

mkdir vtk/nbproject/private
cp nbproject/private/private.properties vtk/nbproject/private/

mkdir vecmath/nbproject/private
cp nbproject/private/private.properties vecmath/nbproject/private/

mkdir trove/nbproject/private
cp nbproject/private/private.properties trove/nbproject/private/

echo "path.jre.win32=$wineJavaDir/jre" >> nbproject/private/private.properties

java org.apache.xalan.xslt.Process -IN ./nbproject/project.xml -XSL $xslDir/suite-build-impl.xsl -OUT nbproject/build-impl.xml
ant -Dnbplatform.default.netbeans.dest.dir="$nbDir" -Dnbplatform.default.harness.dir="$nbDir/harness/" build-zip
cd $mypwd

mv $jcaeDir/jcae/dist jCAE-zipped
