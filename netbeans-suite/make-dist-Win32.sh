#! /bin/sh -xe

# Change this to your own OpenCASCADE installation
CASROOT=/home/jerome/OpenCASCADE6.2.0-win32/ros

# Name of the Java directory in the final bundle
JAVA_NAME=jre-6
VERSION=jcae-@JCAE_VERSION@
rm -rf $VERSION
mkdir $VERSION
cd $VERSION

cat > jcae.bat <<EOF
set MEMORY=512m
set ZFACTORABS=0
set ZFACTORREL=0
set MMGT_OPT=0
set jcaeHome=%~dp0
set PATH=%jcaeHome%OpenCASCADE6.2.0/ros/win32/bin;%jcaeHome%jcae/modules/lib/;%PATH%
"%jcaeHome%/bin/jcae" --jdkhome "%jcaeHome%/$JAVA_NAME" -J-Xmx%MEMORY% -J-Djavax.media.j3d.zFactorAbs=%ZFACTORABS% -J-Djavax.media.j3d.zFactorRel=%ZFACTORREL%
EOF

test -z "$JRE_HOME" && JRE_HOME=../jre-6-win32
CASCADE_LIB=$CASROOT/win32-jcae/bin
test ! -d "$CASCADE_LIB" && CASCADE_LIB=$CASROOT/win32/bin

if [ ! -d "$CASCADE_LIB" ]; then
	echo "Invalid OpenCASCADE directory. Cannot find $CASCADE_LIB"
	exit 0
fi

if [ ! -d "$JRE_HOME" ]; then
	echo "Invalid JRE directory. Cannot find $JRE_HOME"
	exit 0
fi

ln -s $JRE_HOME $JAVA_NAME
rm -rf $JAVA_NAME/plugin $JAVA_NAME/javaw $JAVA_NAME/lib/i386/client/classes.jsa || true
mkdir -p OpenCASCADE6.2.0/ros/win32
ln -s $CASROOT/../LICENSE OpenCASCADE6.2.0/LICENSE
ln -s $CASCADE_LIB OpenCASCADE6.2.0/ros/win32/bin
unzip ../dist/jcae.zip
mv jcae tmp
mv tmp/* .
rmdir tmp

cd ..

rm $VERSION-win32.zip || true
zip -9r "$VERSION-win32.zip" "$VERSION"

