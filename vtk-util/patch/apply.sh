#!/bin/sh
# Script that apply all the patchs in correct order to the VTK library.
# You must be on the root of the VTK source directory when executing this script.

patch_opts=  # set to -R to unapply patches
scriptDir=$(dirname $0)
echo "Applying depth patch"
patch -p1 $patch_opts < $scriptDir/01_depth.patch
echo "Applying display list patch"
patch -p0 $patch_opts < $scriptDir/02_displayList.patch
echo "Applying offset patch"
patch -p0 $patch_opts < $scriptDir/03_offset.patch
echo "Applying translucent patch"
patch -p1 $patch_opts < $scriptDir/04_translucent.patch
echo "Applying underscore patch"
patch -p0 $patch_opts < $scriptDir/05_underscore.patch
echo "Applying warning patch"
patch -p0 $patch_opts < $scriptDir/06_warning.patch
