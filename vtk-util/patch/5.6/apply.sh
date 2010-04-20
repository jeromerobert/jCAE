#!/bin/sh
# Script that apply all the patchs in correct order to the VTK library.
# You must be on the root of the VTK source directory when executing this script.

scriptDir=$(dirname $0)

[ -L patches ] || ln -s $scriptDir patches
quilt push -a