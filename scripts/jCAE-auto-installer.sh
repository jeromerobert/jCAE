#!/bin/sh

##################################################
## Preliminaries for win32 installation
##################################################

#In addition to dependencies mentioned in the linux installer, the required packages are:


#1. mingw-w64 debian package (http://packages.debian.org/sid/mingw-w64)
#	- This will require editing /etc/apt/sources.list
# Steps
#	$ sudo echo 'deb http://ftp.de.debian.org/debian sid main' >> /etc/apt/sources.list
#	$ sudo apt-get install mingw-w64 gcc-mingw-w64-i686 g++-mingw-w64-i686
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

# Oputput text formatting
bold=`tput bold`
normal=`tput sgr0`
green="\033[32m "
red="\033[31m "
black="\033[0m "

# Installer command line arguments

export targetOS;
export makeSpeed;

echo "$1"
if [ "$1" = "linux" ] || [ "$1" = "windows" ]
then
	targetOS=$1
else
	echo "$red$bold\nPlease specify the target OS (linux/windows) \n$normal $black"
	exit 1
fi

if [ $2 -eq "" ]
then
	makeSpeed="2"
else
	makeSpeed=$2
fi

if [ "$1" = "linux" ] && [ "$JAVA_HOME" = "" ] 
then	
	echo "$red$bold\nPlease specify \$JAVA_HOME environment variable \n$normal $black"
	exit 1
fi

startTime=$(date +%s)

# Relative paths simplified
export mypwd=$PWD


##################################################
## Detect dependencies
##################################################

echo "$green $bold\n Checking for dependencies [ $(date +%s) ]... \n$normal $black" " Time="$(date +%s)

export package=""
export useDpkg=0

checkPackage()
{
	#echo "\n Looking for $package"
	if [ $useDpkg -eq 1 ]
	then
		#echo "\n dpkg"
		ret=$(dpkg -l | grep $package)
		if [ $? -ne 0 ]
		then
			echo "$red$bold\nNot Found $package \n$normal $black"
			exit 1
		else
			echo "$green$bold\nFound $package \n$normal $ret $black"
		fi
	else
		#echo "\n version"
		ret=$($package --version )
		if [ $? -ne 0 ]
		then
			echo "--version failed"
			ret2=$($package -version)
			echo $ret2
			if [ $? -ne 0 ]
			then
				echo "$red $bold\nNot Found $package \n$normal $black"
				exit 1
			else
				echo "$green $bold\nFound $package \n$normal $ret2 $black"
			fi
		else
			echo "$green $bold\nFound $package \n$normal  $ret $black"
		fi
	fi
}

#package="gcc"; useDpkg=0; checkPackage
package="git" ; useDpkg=0; checkPackage
package="cmake"; useDpkg=0; checkPackage
package="swig"; useDpkg=0; checkPackage
#package="mesa-common-dev"; useDpkg=1; checkPackage
#package="libxt-dev"; useDpkg=1; checkPackage
#package="freeglut3-dev"; useDpkg=1; checkPackage
package="openjdk-7-jdk"; useDpkg=1; checkPackage
package="quilt"; useDpkg=0; checkPackage
package="ant"; useDpkg=0; checkPackage

if [ "$targetOS" = "windows" ]
then
	package="gcc-mingw-w64-i686"; useDpkg=1; checkPackage
	package="g++-mingw-w64-i686"; useDpkg=1; checkPackage
	package="wine"; useDpkg=0; checkPackage
fi

##################################################
## Install JDK 1.7  on wine
##################################################


if [ "$targetOS" = "windows" ]
then
	wineDir=$mypwd/.wine
	wineJavaDir=$wineDir/drive_c/Java
	wineJavaUrl="http://www.java.net/download/jdk6/6u32/promoted/b02/binaries/jdk-6u32-ea-bin-b02-windows-i586-30_jan_2012.exe"
	wineJavaExec="jdk-6u32-ea-bin-b02-windows-i586-30_jan_2012.exe"
	export WINEPREFIX=$wineDir

	echo "$green $bold\n Wine configuration and JDK installing ... \n$normal $black"" Time="$(date +%s)
	# check for $wineDir presence
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
		echo "$green $bold\nJDK-6 on wine Installed \n$normal $black" " Time="$(date +%s)
	else
		echo "$green $bold\nJDK-6 on wine already Installed \n$normal $black"
	fi
fi

##################################################
## Get, Patch (from jCAE) and Install VTK 
## Get jCAE (installation later)
##################################################

# Define abs locations

vtkURL="http://www.vtk.org/files/release/5.10/vtk-5.10.0-rc3.tar.gz"
vtkTar="vtk-5.10.0-rc3.tar.gz"
vtkDir=$mypwd/VTK5.10.0.RC3
vtkLinBuildDir=$mypwd/vtkLinBuild
vtkLinInstallDir=$mypwd/vtkLinInstall

jcaeURL=https://github.com/jeromerobert/jCAE.git
jcaeDir=$mypwd/jCAE

# Get vtk-5.10.0, unzip 
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

cd $jcaeDir
export jcaeTagName=$(git describe --tags)


# Apply patch
# TODO: Forcibly applying patches without checking. No damage can 
# happen but it would be good to have a way to check to avoid repatching
#cd $vtkDir
#$jcaeDir/vtk-util/patch/5.6/apply.sh
cd $mypwd

ret=$(ls $vtkLinBuildDir)
if [ $? -ne 0 ]
then
	mkdir $vtkLinBuildDir
fi

ret=$(find $vtkLinBuildDir -iname vtk.jar)
if [ "$ret" = "" ]
then
	echo "$green $bold\n Vtk native building... \n$normal $black"" Time="$(date +%s)
	cd $vtkLinBuildDir
	flags="-DCMAKE_INSTALL_PREFIX:PATH=$vtkLinInstallDir"
	flags="$flags -DBUILD_SHARED_LIBS:BOOL=ON"
	flags="$flags -DVTK_WRAP_JAVA:BOOL=ON"
	flags="$flags -DVTK_NO_LIBRARY_VERSION:BOOL=ON"
	flags="$flags -DCMAKE_SKIP_RPATH:BOOL=YES"
	cmake $flags $vtkDir
	VERBOSE=1 make -j$makeSpeed
	echo "$green $bold\nVtk built successfully \n$normal $black"" Time="$(date +%s)
else
	echo "$green $bold\nVtk already built \n$normal $black"
fi

cd $mypwd

if [ "$targetOS" = "linux" ]
then
	ret=$(ls $vtkLinInstallDir)
	if [ $? -ne 0 ]
	then
		mkdir $vtkLinInstallDir
	fi

	ret=$(find $vtkLinInstallDir -iname vtk.jar)
	if [ "$ret" = "" ]
	then
		echo "$green $bold\nVtk installing... \n$normal $black"" Time="$(date +%s)
		cd $vtkLinBuildDir
		make install
		echo "$green $bold\nVtk Installed successfully\n$normal $black"" Time="$(date +%s)
	else
		echo "$green $bold\nVtk already Installed \n$normal $black"
	fi

fi

if [ "$targetOS" = "windows" ]
then
	vtkWinBuildDir=$mypwd/vtkWinBuild
	vtkWinInstallDir=$mypwd/vtkWinInstall

	# Cross build
	ret=$(ls $vtkWinBuildDir)
	if [ $? -ne 0 ]
	then
		mkdir $vtkWinBuildDir
	fi

	ret=$(find $vtkWinBuildDir -iname vtk.jar)
	if [ "$ret" = "" ]
	then
		echo "$green $bold\nVtk ($targetOS) building... \n$normal $black"" Time="$(date +%s)
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
		flags="$flags -DVTK_USE_64BIT_IDS:BOOL=ON"

		flags="$flags -DCMAKE_CXX_FLAGS:STRING=-fpermissive"

		cmake $flags $vtkDir
		cmake $flags $vtkDir
		set -e
		VERBOSE=1 make -j$makeSpeed
		echo "$green $bold\nVtk built successfully \n$normal $black"" Time="$(date +%s)
	else
		echo "$green $bold\nVtk already built \n$normal $black"
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
		echo "$green $bold\nVtk installing... \n$normal $black"" Time="$(date +%s)
		cd $vtkWinBuildDir
		make install
		echo "$green $bold\nVtk installed successfully \n$normal $black"" Time="$(date +%s)	
	else
		echo "$green $bold\nVtk already installed \n$normal $black"	
	fi
fi

cd $mypwd


##################################################
## Get and Install OCE 0.9.1
##################################################

oceURL=https://github.com/tpaviot/oce.git
oceDir=$mypwd/oce

ret=$(ls $oceDir)
if [ $? -ne 0 ]
then
	git clone $oceURL $oceDir
fi

ret=$(git describe | grep OCE-0.9.1)
if [ $? -ne 0 ]
then
	cd $oceDir
	git checkout OCE-0.9.1
	cd $mypwd
fi

if [ "$targetOS" = "linux" ]
then
	oceLinBuildDir=$mypwd/oceLinBuild
	oceLinInstallDir=$mypwd/oceLinInstall
	ret=$(ls $oceLinBuildDir)
	if [ $? -ne 0 ]
	then
		mkdir $oceLinBuildDir	
	fi

	ret=$(find $oceLinBuildDir -iname *Tk*.so*)
	if [ "$ret" = "" ]
	then
		echo "$green $bold\nOce building... \n$normal $black"" Time="$(date +%s)
		cd $oceLinBuildDir
		flags="-DOCE_INSTALL_PREFIX:PATH=$oceLinInstallDir"
		flags="$flags -DOCE_DISABLE_BSPLINE_MESHER:BOOL=ON"
		flags="$flags -DCMAKE_CXX_FLAGS:STRING=-DMMGT_OPT_DEFAULT=0"
		flags="$flags -DOCE_DISABLE_X11=ON"
		flags="$flags -DOCE_NO_LIBRARY_VERSION:BOOL=ON"
		cmake $flags $oceDir
		cd $mypwd
		make -j$makeSpeed	
		echo "$green $bold\nOce built successfully \n$normal $black"" Time="$(date +%s)
	else
		echo "$green $bold\nOce already built \n$normal $black"
	fi

	ret=$(ls $oceLinInstallDir)
	if [ $? -ne 0 ]
	then
		mkdir $oceLinInstallDir	
	fi

	ret=$(find $oceLinInstallDir -iname *Tk*.so*)
	if [ "$ret" = "" ]
	then
		echo "$green $bold\nOce installing... \n$normal $black"" Time="$(date +%s)
		cd $oceLinBuildDir
		make install
		echo "$green $bold\nOce installed successfully \n$normal $black"" Time="$(date +%s)
	else
		echo "$green $bold\nOce already installed \n$normal $black"
	fi
fi

if [ "$targetOS" = "windows" ]
then
	oceWinBuildDir=$mypwd/oceWinBuild
	oceWinInstallDir=$mypwd/oceWinInstall

	ret=$(ls $oceWinBuildDir)
	if [ $? -ne 0 ]
	then
		mkdir $oceWinBuildDir	
	fi

	ret=$(find $oceWinBuildDir -iname *Tk*.dll)
	if [ "$ret" = "" ]
	then
		echo "$green $bold\nOce building... \n$normal $black"" Time="$(date +%s)
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
		echo "$green $bold\nOce built successfully \n$normal $black"" Time="$(date +%s)
	else
		echo "$green $bold\nOce already built \n$normal $black"
	fi

	ret=$(ls $oceWinInstallDir)
	if [ $? -ne 0 ]
	then
		mkdir $oceWinInstallDir
	fi

	ret=$(find $oceWinInstallDir -iname *Tk*.dll)
	if [ "$ret" = "" ]
	then
		echo "$green $bold\nOce installing... \n$normal $black"" Time="$(date +%s)
		cd $oceWinBuildDir
		make install
		echo "$green $bold\nOce installed successfully \n$normal $black"" Time="$(date +%s)
	else
		echo "$green $bold\nOce already installed \n$normal $black"
	fi
fi

cd $mypwd


##################################################
## PATCH # Copy libgcc_s_sjlj-1.dll libstdc++-6.dll
##################################################

if [ "$targetOS" = "windows" ]
then
	cp $(find /usr/lib/ -path *i686*  -iname libstdc++-6.dll -or -iname libgcc_s_sjlj-1.dll ) $oceWinInstallDir/Win32/bin/
fi

##################################################
## Get and Install JYTHON
##################################################

jythonURL=http://sourceforge.net/projects/jython/files/jython/2.5.2/jython_installer-2.5.2.jar
jythonJar=jython_installer-2.5.2.jar
jythonDir=$mypwd/jython

echo "$green $bold\nJython installing... \n$normal $black"" Time="$(date +%s)

ret=$(ls $jythonJar)
if [ $? -ne 0 ]
then
	wget $jythonURL
fi

ret=$(ls $jythonDir)
if [ $? -ne 0 ]
then
	java -jar $jythonJar -s -d $jythonDir
	echo "$green $bold\nJython installed successfully \n$normal $black"" Time="$(date +%s)
else
	echo "$green $bold\nJython already installed \n$normal $black"
fi



##################################################
## Get and Install VECMATH
##################################################
vecmathURL=http://ftp.fr.debian.org/debian/pool/main/v/vecmath/libvecmath-java_1.5.2-2_all.deb
vecmathDebian=libvecmath-java_1.5.2-2_all.deb
vecmathDir=$mypwd/vecmath

echo "$green $bold\nVecmath installing... \n$normal $black"" Time="$(date +%s)

ret=$(ls $vecmathDebian)
if [ $? -ne 0 ]
then
	wget $vecmathURL
fi

ret=$(ls $vecmathDir)
if [ $? -ne 0 ]
then
	dpkg-deb -x $vecmathDebian $vecmathDir
	echo "$green $bold\nVecmath installed successfully \n$normal $black"" Time="$(date +%s)
else
	echo "$green $bold\nVecmath already installed \n$normal $black"

fi


##################################################
## Get and Install TROVE
##################################################
troveURL=http://ftp.fr.debian.org/debian/pool/main/t/trove/libtrove-java_2.1.0-2_all.deb
troveDebian=libtrove-java_2.1.0-2_all.deb
troveDir=$mypwd/trove

echo "$green $bold\nTrove installing... \n$normal $black"" Time="$(date +%s)

ret=$(ls $troveDebian)
if [ $? -ne 0 ]
then
	wget $troveURL
fi

ret=$(ls $troveDir)
if [ $? -ne 0 ]
then
	dpkg-deb -x $troveDebian $troveDir
	echo "$green $bold\nTrove installed successfully \n$normal $black"" Time="$(date +%s)
else
	echo "$green $bold\nTrove already installed \n$normal $black"
fi


##################################################
## Get and Install Netbeans 7.1
##################################################
nbURL=http://dlc.sun.com.edgesuite.net/netbeans/7.1.1/final/bundles/netbeans-7.1.1-ml-javase-linux.sh
nbEx=netbeans-7.1.1-ml-javase-linux.sh
nbDir=$mypwd/nb

echo "$green $bold\nNetbeans installing... \n$normal $black"" Time="$(date +%s)

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
	echo "$green $bold\nNetbeans installed successfully \n$normal $black"" Time="$(date +%s)
else
	echo "$green $bold\nNetbeans already installed \n$normal $black"
fi


##################################################
## Get and Install XALAN
## Get XSLs for build-impl.xml creation
##################################################
xalanURL=http://apache.multidist.com/xml/xalan-j/xalan-j_2_7_1-bin-2jars.tar.gz
xalanTar=xalan-j_2_7_1-bin-2jars.tar.gz
xalanDir=$mypwd/xalan-j_2_7_1

echo "$green $bold\nXalan installing... \n$normal $black"" Time="$(date +%s)

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

echo "$green $bold\nXLS fetching... \n$normal $black"" Time="$(date +%s)

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
echo "$green $bold\njCAE build starts... \n$normal $black"" Time="$(date +%s)

export jythonPath=$(find $jythonDir -iname jython.jar)
export trovePath=$(find $troveDir -iname trove.jar)
export vecmathPath=$(find $vecmathDir -iname vecmath.jar)
export vtkPath=$(find $vtkLinBuildDir -iname vtk.jar)

touch jcae.config
echo "" > jcae.config

echo "app.version=$jcaeTagName" >> jcae.config
echo "libs.trove.classpath=$trovePath" >> jcae.config
echo "libs.vecmath.classpath=$vecmathPath" >> jcae.config
echo "libs.VTK.classpath=$vtkPath" >> jcae.config

if [ "$targetOS" = "linux" ]
then
	echo "arch.linux=true" >> jcae.config
	echo "path.occ.linux=$oceLinInstallDir/lib/" >> jcae.config # TODO: Check here why TK*.so not copied to cluster
	echo "path.jython.linux=$jythonDir" >> jcae.config
	echo "vtk.dir.linux=$vtkLinInstallDir/lib/vtk-5.10/" >> jcae.config
	echo "path.occjava.linux=$mypwd/occjavaInstall/libOccJava.so" >> jcae.config

	ret=$(find /usr/lib/ -iname libstdc++.so.6 | head -1)
	echo "path.libstdc++=$ret" >> jcae.config

	export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$oceLinInstallDir/lib/
	export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$vtkLinInstallDir/lib/vtk-5.10/
fi

if [ "$targetOS" = "windows" ]
then
	echo "arch.win32=true" >> jcae.config
	echo "path.occ.win32=$oceWinInstallDir/Win32/bin/" >> jcae.config
	echo "path.jython.win32=$jythonDir" >> jcae.config
	echo "vtk.dir.win32=$vtkWinInstallDir/bin/" >> jcae.config
	echo "path.occjava.win32=$mypwd/occjavaBuild/OccJava.dll" >> jcae.config

	export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$oceWinInstallDir/Win32/bin/
	export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$vtkWinInstallDir/bin/
fi


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
#echo "$green $bold\nCleaning previous jCAE(if any)... \n$normal $black"" Time="$(date +%s)
#rm -rf $jcaeDir
rm -rf $mypwd/occjavaInstall
#git clone $jcaeURL


#-------------------------------------------------
## Build jCAE projects (vtk-util, occjava, etc)
#-------------------------------------------------

echo "$green $bold\nBuilding OccJava... \n$normal $black"" Time="$(date +%s)

## build OccJava
occjavaDir=$jcaeDir/occjava
occjavaBuildDir=$mypwd/occjavaBuild
occjavaInstallDir=$mypwd/occjavaInstall/

mkdir $occjavaBuildDir
rm -rf $occjavaBuildDir/*

mkdir $occjavaInstallDir
cd $occjavaBuildDir

if [ "$targetOS" = "linux" ]
then
	flags=""
	flags="$flags -DSWIG_DIR=/usr/bin/"
	flags="$flags -DSWIG_EXECUTABLE=/usr/bin/swig"
	flags="$flags -DOCE_DIR=$oceLinInstallDir/lib/oce-0.9.1"

	cmake $flags $occjavaDir
	make
	cp *.so* $occjavaInstallDir
fi

if [ "$targetOS" = "windows" ]
then
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
	cp *.dll* $occjavaInstallDir
fi

## building vtk-util
echo "$green $bold\nBuilding vtk-util... \n$normal $black"" Time="$(date +%s)
cd $jcaeDir/vtk-util/
mkdir nbproject/private
touch nbproject/private/private.properties
cat $mypwd/jcae.config > nbproject/private/private.properties
ant config
ant clean
ant -Dnbplatform.default.netbeans.dest.dir="$nbDir/" -Dnbplatform.default.harness.dir="$nbDir/harness/"
cd $mypwd

## building jcae/occjava
echo "$green $bold\nBuilding jcae/occjava... \n$normal $black"" Time="$(date +%s)
cd $jcaeDir/jcae/occjava
mkdir nbproject/private
touch nbproject/private/private.properties
cat $mypwd/jcae.config > nbproject/private/private.properties
java org.apache.xalan.xslt.Process -IN nbproject/project.xml -XSL $xslDir/project-build-impl.xsl -OUT nbproject/build-impl.xml
ant -Dnbplatform.default.netbeans.dest.dir="$nbDir/" -Dnbplatform.default.harness.dir="$nbDir/harness/" jar
cd $mypwd

## building amibe
echo "$green $bold\nBuilding amibe... \n$normal $black"" Time="$(date +%s)
cd $jcaeDir/amibe
mkdir nbproject/private
touch nbproject/private/private.properties
cat $mypwd/jcae.config > nbproject/private/private.properties
java org.apache.xalan.xslt.Process -IN nbproject/project.xml -XSL $xslDir/project-build-impl.xsl -OUT nbproject/build-impl.xml
ant -Dnbplatform.default.netbeans.dest.dir="$nbDir/" -Dnbplatform.default.harness.dir="$nbDir/harness/" -f nbbuild.xml jar
cd $mypwd

## building vtk-amibe (src location at jCAE/vtk-amibe/, DONT know why?)
echo "$green $bold\nBuilding vtk-amibe... \n$normal $black"" Time="$(date +%s)
cd $jcaeDir/jcae/vtk-amibe
mkdir nbproject/private
touch nbproject/private/private.properties
cat $mypwd/jcae.config > nbproject/private/private.properties
java org.apache.xalan.xslt.Process -IN nbproject/project.xml -XSL $xslDir/project-build-impl.xsl -OUT nbproject/build-impl.xml
ant -Dnbplatform.default.netbeans.dest.dir="$nbDir/" -Dnbplatform.default.harness.dir="$nbDir/harness/" jar
cd $mypwd

## building vtk-amibe-occ
echo "$green $bold\nBuilding vtk-amibe-occ... \n$normal $black"" Time="$(date +%s)
cd $jcaeDir/vtk-amibe-occ
mkdir nbproject/private
touch nbproject/private/private.properties
cat $mypwd/jcae.config > nbproject/private/private.properties
java org.apache.xalan.xslt.Process -IN nbproject/project.xml -XSL $xslDir/project-build-impl.xsl -OUT nbproject/build-impl.xml
ant -Dnbplatform.default.netbeans.dest.dir="$nbDir/" -Dnbplatform.default.harness.dir="$nbDir/harness/" jar
cd $mypwd

#-------------------------------------------------
## Build jCAE modules 
#-------------------------------------------------

echo "$green $bold\nGenerating buildfiles for jCAE modules... \n$normal $black"" Time="$(date +%s)
cd $jcaeDir/jcae

modules="amibe amibe-occ core jython mesh-algos occjava-nb trove tweakui vecmath vtk vtk-util"
for module in $modules
do
	cd $jcaeDir/jcae/"$module"
	java org.apache.xalan.xslt.Process -IN nbproject/project.xml -XSL $xslDir/module-build-impl.xsl -OUT nbproject/build-impl.xml
done


#-------------------------------------------------
## Generate platform.xml.
#-------------------------------------------------

# This is a now an automated step. 
echo "$green $bold\nGenerating platform.xml for jCAE... \n$normal $black"" Time="$(date +%s)
cd $jcaeDir/jcae
java org.apache.xalan.xslt.Process -IN nbproject/project.xml -XSL $xslDir/platform.xsl -OUT nbproject/platform.xml

#-------------------------------------------------
## Build suite as Zip distribution
#-------------------------------------------------
echo "$green $bold\nBuilding jCAE suite... \n$normal $black"" Time="$(date +%s)
mkdir nbproject/private
touch nbproject/private/private.properties
cat $mypwd/jcae.config > nbproject/private/private.properties

mkdir vtk/nbproject/private
cp nbproject/private/private.properties vtk/nbproject/private/

mkdir vecmath/nbproject/private
cp nbproject/private/private.properties vecmath/nbproject/private/

mkdir trove/nbproject/private
cp nbproject/private/private.properties trove/nbproject/private/

if [ "$targetOS" = "linux" ]
then
 	echo "path.jre.linux=$JAVA_HOME/jre" >> nbproject/private/private.properties
elif [ "$targetOS" = "windows" ]
then
	echo "path.jre.win32=$wineJavaDir/jre" >> nbproject/private/private.properties 
fi

java org.apache.xalan.xslt.Process -IN ./nbproject/project.xml -XSL $xslDir/suite-build-impl.xsl -OUT nbproject/build-impl.xml
ant -Dnbplatform.default.netbeans.dest.dir="$nbDir" -Dnbplatform.default.harness.dir="$nbDir/harness/" build-zip
echo "$green $bold\njCAE built successfully \n$normal $black"" Time="$(date +%s)
cd $mypwd

#-------------------------------------------------
## Copy Zip distro to jCAE-zipped
#-------------------------------------------------
ret=$(ls jCAE-zipped)
if [ $? -ne 0 ]
then
	mkdir jCAE-zipped
fi
rm jCAE-zipped/*
mv $jcaeDir/jcae/dist/* jCAE-zipped/
echo "$green $bold\njCAE Zipped successfully in $mypwd/jCAE-zipped  \n$normal $black"" Time="$(date +%s)
