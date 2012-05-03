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
wineJavaUrl="http://download.java.net/jdk7u6/archive/b07/binaries/jdk-7u6-ea-bin-b07-windows-i586-23_apr_2012.exe"
wineJavaExec="jdk-7u6-ea-bin-b07-windows-i586-23_apr_2012.exe"

# check for $wineDir presence
ret=$(ls "$wineDir")
if [ $? -ne 0 ]
then
	mkdir $wineDir
	winecfg
fi

export WINEPREFIX=$wineDir

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
	flags="$flags -DCMAKE_TOOLCHAIN_FILE:FILEPATH=$jcaeDir/vtk-util/toolchain-x86_64-w64-mingw32.cmake"
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
	flags="$flags -DCMAKE_TOOLCHAIN_FILE:FILEPATH=$jcaeDir/vtk-util/toolchain-x86_64-w64-mingw32.cmake"
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

cd oceWinInstall/Win32/bin/

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
## Get and Install Netbeans 7.1
##################################################

echo -e "\033[32m" "Fetching Netbeans 7.1" "\033[0m" 
wget http://dlc.sun.com.edgesuite.net/netbeans/7.1.1/final/bundles/netbeans-7.1.1-ml-javase-linux.sh

if [ $? -eq 1 ]
then
     echo -e "\033[31m" "Netbeans setup Not found" "\033[0m" 
     exit 1
else
	chmod a+x netbeans-7.1.1-ml-javase-linux.sh
        mkdir nb
        ./netbeans-7.1.1-ml-javase-linux.sh --silent "-J-Dnb-base.installation.location=nb/"
fi

if [ $? -eq 1 ]
then
     echo -e "\033[31m" "Netbeans could not be installed" "\033[0m" 
     exit 1
else
     echo -e "\033[32m" "Netbeans Installed successfully" "\033[0m" 
fi

##################################################
## Get and Install JYTHON
##################################################

echo -e "\033[32m" "Fetching Jython" "\033[0m" 
wget http://sourceforge.net/projects/jython/files/jython/2.5.2/jython_installer-2.5.2.jar

if [ $? -eq 1 ]
then
     echo -e "\033[31m" "Jython server Not found" "\033[0m" 
     exit 1
else
	java -jar jython_installer-2.5.2.jar -s -d jython 
fi

if [ $? -eq 1 ]
then
     echo -e "\033[31m" "Jython could not be installed" "\033[0m" 
     exit 1
else
	echo -e "\033[32m" "Jython Installed successfully" "\033[0m" 
fi

##################################################
## Get and Install VECMATH
##################################################

echo -e "\033[32m" "Fetching vecmath" "\033[0m" 
wget http://ftp.fr.debian.org/debian/pool/main/v/vecmath/libvecmath-java_1.5.2-2_all.deb

if [ $? -eq 1 ]
then
     echo -e "\033[31m" "Vecmath server Not found" "\033[0m" 
     exit 1
else
	dpkg-deb -x libvecmath-java_1.5.2-2_all.deb vecmath
fi

if [ $? -eq 1 ]
then
     echo -e "\033[31m" "Vecmath could not be installed" "\033[0m" 
     exit 1
else
	echo -e "\033[32m" "Vecmath Installed successfully" "\033[0m" 
fi


##################################################
## Get and Install TROVE
##################################################

echo -e "\033[32m" "Fetching trove" "\033[0m" 
wget http://ftp.fr.debian.org/debian/pool/main/t/trove/libtrove-java_2.1.0-2_all.deb

if [ $? -eq 1 ]
then
     echo -e "\033[31m" "trove server Not found" "\033[0m" 
     exit 1
else
	dpkg-deb -x libtrove-java_2.1.0-2_all.deb trove
fi

if [ $? -eq 1 ]
then
     echo -e "\033[31m" "trove could not be installed" "\033[0m" 
     exit 1
else
	echo -e "\033[32m" "trove Installed successfully" "\033[0m" 
fi

##################################################
## Get and Install XALAN
## Get XSLs for build-impl.xml creation
##################################################

wget http://mirror.mkhelif.fr/apache//xml/xalan-j/xalan-j_2_7_1-bin-2jars.tar.gz
tar xf xalan-j_2_7_1-bin-2jars.tar.gz
rm xalan-j_2_7_1-bin-2jars.tar.gz
cd xalan-j_2_7_1
export CLASSPATH=$CLASSPATH:$PWD/xalan.jar
export CLASSPATH=$CLASSPATH:$PWD/serializer.jar
export CLASSPATH=$CLASSPATH:$PWD/xercesImpl.jar
export CLASSPATH=$CLASSPATH:$PWD/xml-apis.jar
export CLASSPATH=$CLASSPATH:$PWD/xsltc.jar
cd ..

mkdir xsls
cd xsls
wget http://hg.netbeans.org/releases/raw-file/5dfb0137e99e/java.j2seproject/src/org/netbeans/modules/java/j2seproject/resources/build-impl.xsl
mv build-impl.xsl project-build-impl.xsl
wget http://hg.netbeans.org/main/raw-file/c2719a24ed74/apisupport.ant/src/org/netbeans/modules/apisupport/project/suite/resources/build-impl.xsl
mv build-impl.xsl suite-build-impl.xsl
wget http://hg.netbeans.org/main/raw-file/c2719a24ed74/apisupport.ant/src/org/netbeans/modules/apisupport/project/resources/build-impl.xsl 
mv build-impl.xsl module-build-impl.xsl
wget http://hg.netbeans.org/main/raw-file/c2719a24ed74/apisupport.ant/src/org/netbeans/modules/apisupport/project/suite/resources/platform.xsl
cd ..


##################################################
## jCAE INSTALLATION
##################################################


#-------------------------------------------------
## Set envirmonment variables
#-------------------------------------------------
export mypwd=$PWD

export jythonPath=$mypwd/$(find jython/ -iname jython.jar)
export trovePath=$mypwd/$(find trove/ -iname trove.jar)
export vecmathPath=$mypwd/$(find vecmath/ -iname vecmath.jar)
export vtkPath=$mypwd/$(find vtkWinInstall/ -iname vtk.jar)

touch jcae.config

echo "libs.trove.classpath=$trovePath" >> jcae.config
echo "libs.vecmath.classpath=$vecmathPath" >> jcae.config
echo "libs.VTK.classpath=$vtkPath" >> jcae.config

echo "arch.win32=true" >> jcae.config
echo "path.occ.win32=$mypwd/oceWinInstall/Win32/bin/" >> jcae.config
echo "path.jython.win32=$mypwd/jython/" >> jcae.config
echo "vtk.dir.win32=$mypwd/vtkWinInstall/bin/" >> jcae.config
echo "path.occjava.win32=$mypwd/occjavaBuild/OccJava.dll" >> jcae.config

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$mypwd/oceWinInstall/Win32/bin/
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$mypwd/vtkWinInstall/bin/

#-------------------------------------------------
## Build jCAE projects (vtk-util, occjava, etc)
#-------------------------------------------------

## build OccJava
cd jCAE/occjava/
mkdir build
cd build
cp ../../vtk-util/toolchain-i686-w64-mingw32.cmake .
#cmake -DOCE_DIR=../../oceBuild/lib/oce-0.9.0 ..
cmake -DJAVA_ARCHIVE:FILEPATH=/usr/bin/jar -DJAVA_AWT_INCLUDE_PATH:PATH=$HOME/.wine/drive_c/programs/Java/include/ -DJAVA_AWT_LIBRARY:FILEPATH=$HOME/.wine/drive_c/programs/Java/lib/jawt.lib -DJAVA_COMPILE:FILEPATH=/usr/bin/javac -DJAVA_INCLUDE_PATH:PATH=$HOME/.wine/drive_c/programs/Java/include -DJAVA_INCLUDE_PATH2:PATH=$HOME/.wine/drive_c/programs/Java/include/win32 -DJAVA_JVM_LIBRARY:FILEPATH=$HOME/.wine/drive_c/programs/Java/lib/jvm.lib -DJAVA_RUNTIME:FILEPATH=$HOME/.wine/drive_c/programs/Java/jre/bin/java.exe -DOCE_DIR=$mypwd/oceWinInstall/cmake/ -DCMAKE_TOOLCHAIN_FILE:FILEPATH=toolchain-i686-w64-mingw32.cmake -DSWIG_DIR=/usr/bin/ -DSWIG_EXECUTABLE=/usr/bin/swig ..
make
mkdir ../../../occjavaBuild
cp *.dll* ../../../occjavaBuild/
cd ../../../

## building vtk-util
cd jCAE/vtk-util/
mkdir nbproject/private
touch nbproject/private/private.properties
cat ../../jcae.config >> nbproject/private/private.properties
ant config
ant clean
ant -Dnbplatform.default.netbeans.dest.dir="$mypwd/nb/" -Dnbplatform.default.harness.dir="$mypwd/nb/harness/"  
cd ../..

## building jcae/occjava
cd jCAE/jcae/occjava
mkdir nbproject/private
touch nbproject/private/private.properties
cat ../../../jcae.config >> nbproject/private/private.properties
java org.apache.xalan.xslt.Process -IN ./nbproject/project.xml -XSL ../../../xsls/project-build-impl.xsl -OUT ./nbproject/build-impl.xml
ant -Dnbplatform.default.netbeans.dest.dir="$mypwd/nb/" -Dnbplatform.default.harness.dir="$mypwd/nb/harness/" jar
cd ../../..

## building amibe
cd jCAE/amibe
mkdir nbproject/private
touch nbproject/private/private.properties
cat ../../jcae.config >> nbproject/private/private.properties
java org.apache.xalan.xslt.Process -IN ./nbproject/project.xml -XSL ../../xsls/project-build-impl.xsl -OUT ./nbproject/build-impl.xml
ant -Dnbplatform.default.netbeans.dest.dir="$mypwd/nb/" -Dnbplatform.default.harness.dir="$mypwd/nb/harness/" -f nbbuild.xml jar
cd ../..

## building vtk-amibe (src location at jCAE/vtk-amibe/, DONT know why?)
cd jCAE/jcae/vtk-amibe
mkdir nbproject/private
touch nbproject/private/private.properties
cat ../../../jcae.config >> nbproject/private/private.properties
java org.apache.xalan.xslt.Process -IN ./nbproject/project.xml -XSL ../../../xsls/project-build-impl.xsl -OUT ./nbproject/build-impl.xml
ant -Dnbplatform.default.netbeans.dest.dir="$mypwd/nb/" -Dnbplatform.default.harness.dir="$mypwd/nb/harness/" jar
cd ../../..

## building vtk-amibe-occ
cd jCAE/vtk-amibe-occ
mkdir nbproject/private
touch nbproject/private/private.properties
cat ../../jcae.config >> nbproject/private/private.properties
java org.apache.xalan.xslt.Process -IN ./nbproject/project.xml -XSL ../../xsls/project-build-impl.xsl -OUT ./nbproject/build-impl.xml
ant -Dnbplatform.default.netbeans.dest.dir="$mypwd/nb/" -Dnbplatform.default.harness.dir="$mypwd/nb/harness/" jar
cd ../..

#-------------------------------------------------
## Build jCAE modules 
#-------------------------------------------------

cd jCAE/jcae

modules="amibe amibe-occ core jython mesh-algos occjava-nb trove tweakui vecmath vtk vtk-util"
for module in $modules
do
  cd "$module"
  java org.apache.xalan.xslt.Process -IN ./nbproject/project.xml -XSL ../../../xsls/module-build-impl.xsl -OUT ./nbproject/build-impl.xml
  cd ..
done
cd ../..

#-------------------------------------------------
## Generate platform.xml.
#-------------------------------------------------

# This is a now an automated step. 
cd jCAE/jcae
java org.apache.xalan.xslt.Process -IN ./nbproject/project.xml -XSL ../../xsls/platform.xsl -OUT ./nbproject/platform.xml

#-------------------------------------------------
## Build suite as Zip distribution
#-------------------------------------------------
mkdir nbproject/private
touch nbproject/private/private.properties
cat ../../jcae.config > nbproject/private/private.properties

mkdir vtk/nbproject/private
cp nbproject/private/private.properties vtk/nbproject/private/

mkdir vecmath/nbproject/private
cp nbproject/private/private.properties vecmath/nbproject/private/

mkdir trove/nbproject/private
cp nbproject/private/private.properties trove/nbproject/private/

echo "path.jre.win32=$HOME/.wine/drive_c/programs/Java/jre/" >> nbproject/private/private.properties

java org.apache.xalan.xslt.Process -IN ./nbproject/project.xml -XSL ../../xsls/suite-build-impl.xsl -OUT ./nbproject/build-impl.xml
ant -Dnbplatform.default.netbeans.dest.dir="$mypwd/nb/" -Dnbplatform.default.harness.dir="$mypwd/nb/harness/" build-zip
cd ../..

mv jCAE/jcae/dist jCAE-zipped

#-------------------------------------------------
## Clean
#-------------------------------------------------

rm jython*.jar
rm libtrove*.deb
rm libvecmath*.deb
rm vtk-5.6.1.tar.gz 
rm -rf VTK
rm -rf oce
rm netbeans-7.1-ml-linux.sh

