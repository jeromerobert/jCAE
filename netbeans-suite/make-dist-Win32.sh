#! /bin/sh

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

ln -s ../jre-6-win32/ $JAVA_NAME
rm -rf $JAVA_NAME/plugin $JAVA_NAME/javaw $JAVA_NAME/lib/i386/client/classes.jsa
mkdir -p OpenCASCADE6.2.0/ros/win32
ln -s $CASROOT/../LICENSE OpenCASCADE6.2.0/LICENSE
ln -s $CASROOT/win32-jcae/bin/ OpenCASCADE6.2.0/ros/win32/bin
unzip ../dist/jcae.zip
mv jcae tmp
mv tmp/* .
rmdir tmp

cd ..

rm $VERSION-win32.zip
zip -9r "$VERSION-win32.zip" "$VERSION"

