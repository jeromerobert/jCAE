#! /bin/sh -x

CASROOT=/home/jerome/OpenCASCADE6.2.0/ros
JAVA_NAME=jre-6
VERSION=jcae-@JCAE_VERSION@
rm -rf $VERSION
mkdir $VERSION
cd $VERSION

cat > jcae.sh <<EOF
#! /bin/sh
MEMORY=512m
ZFACTORABS=20
ZFACTORREL=2
MMGT_OPT=0
export MMGT_OPT
jcaeHome=\`dirname \$0\`
LD_LIBRARY_PATH=\$jcaeHome/OpenCASCADE6.2.0/ros/Linux/lib:\$jcaeHome/jcae/modules/lib/:\$LD_LIBRARY_PATH
export LD_LIBRARY_PATH
\$jcaeHome/bin/jcae --jdkhome \$jcaeHome/$JAVA_NAME -J-Xmx\$MEMORY -J-Djavax.media.j3d.zFactorAbs=\$ZFACTORABS -J-Djavax.media.j3d.zFactorRel=\$ZFACTORREL
EOF

chmod +x jcae.sh

test -z "$JRE_HOME" && JRE_HOME=../jre-6-Linux
test ! -d "$JRE_HOME" && JRE_HOME=$(readlink -f $(dirname $(readlink -f $(which java)))/..)	
CASCADE_LIB=$CASROOT/Linux-jcae/lib
test ! -d "$CASCADE_LIB" && CASCADE_LIB=$CASROOT/Linux/lib

if [ ! -d "$CASCADE_LIB" ]; then
	echo "Invalid OpenCASCADE directory. Cannot find $CASCADE_LIB"
	exit 0
fi

if [ ! -d "$JRE_HOME" ]; then
	echo "Invalid JRE directory. Cannot find $JRE_HOME"
	exit 0
fi

ln -s $JRE_HOME $JAVA_NAME
rm -rf $JAVA_NAME/plugin $JAVA_NAME/javaw $JAVA_NAME/lib/i386/client/classes.jsa
mkdir -p OpenCASCADE6.2.0/ros/Linux/
ln -s $CASROOT/../LICENSE OpenCASCADE6.2.0/LICENSE
ln -s $CASCADE_LIB OpenCASCADE6.2.0/ros/Linux/lib
unzip -q ../dist/jcae.zip
mv jcae tmp
mv tmp/* .
rmdir tmp

cd ..
rm $VERSION-Linux.tar.bz2
tar cfj $VERSION-Linux.tar.bz2 --totals --owner 0 --group 0 -h $VERSION

