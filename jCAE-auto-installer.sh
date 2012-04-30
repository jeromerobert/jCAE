#!/bin/sh

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

if [ $1 -eq "linux" ] || [ $1 -eq "windows" ]
then
	targetOS=$1
else
	echo "$red$bold\n Please specify the target OS (linux/windows) \n$normal $black"
fi

if [ $2 -eq "" ]
then
	makeSpeed="2"
else
	makeSpeed=$2
fi

# Relative paths simplified



##################################################
## Detect dependencies
##################################################


export package=""
export useDpkg=0

checkPackage()
{
	#echo "\n Looking for $package"
	if [ $useDpkg -eq 1 ]
	then
		#echo "\n dpkg"
		ret=$(dpkg -l | grep $package)
		if [ $? -eq 1 ]
		then
			echo "$red$bold\n Not Found $package \n$normal $black"
			exit 1
		else
			echo "$green$bold\n Found $package \n$normal $ret $black"
		fi
	else
		#echo "\n version"
		ret=$($package --version )
		if [ $? -eq 1 ]
		then
			echo "--version failed"
			ret2=$($package -version)
			echo $ret2
			if [ $? -eq 1 ]
			then
				echo "$red $bold\n Not Found $package \n$normal $black"
				exit 1
			else
				echo "$green $bold\n Found $package \n$normal $ret2 $black"
			fi
		else
			echo "$green $bold\n Found $package \n$normal  $ret $black"
		fi
	fi
}

package="gcc"; useDpkg=0; checkPackage
package="git" ; useDpkg=0; checkPackage
package="cmake"; useDpkg=0; checkPackage
package="swig"; useDpkg=0; checkPackage
package="mesa-common-dev"; useDpkg=1; checkPackage
package="libxt-dev"; useDpkg=1; checkPackage
package="freeglut3-dev"; useDpkg=1; checkPackage
package="openjdk-6-jdk"; useDpkg=1; checkPackage
package="quilt"; useDpkg=0; checkPackage
package="ant"; useDpkg=0; checkPackage

package="gcc-mingw-w64-x86-64"; useDpkg=1; checkPackage
package="wine"; useDpkg=0; checkPackage
