#!/bin/sh
# Script that apply all the patchs in correct order to the VTK library. You must be on the root of the VTK source directory when executing this script.

scriptDir=$(dirname $0)
echo "Applying depth patch"
patch -p1 < $scriptDir/01_depth.patch
echo "Applying display list patch"
patch -p0 < $scriptDir/02_displayList.patch
echo "Applying offset patch"
patch -p0 < $scriptDir/03_offset.patch
echo "Applying translucent patch"
patch -p1 < $scriptDir/04_translucent.patch
echo "Applying underscore patch"
patch -p0 < $scriptDir/05_underscore.patch
echo "Applying warning patch"
patch -p0 < $scriptDir/06_warning.patch
