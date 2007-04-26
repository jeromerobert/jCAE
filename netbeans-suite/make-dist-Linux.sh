#! /bin/sh -x

JAVA_NAME=jre-6
VERSION=jcae-0.14.1
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

ln -s ../jre-6-Linux/ $JAVA_NAME
rm -rf $JAVA_NAME/plugin $JAVA_NAME/javaw $JAVA_NAME/lib/i386/client/classes.jsa
mkdir -p OpenCASCADE6.2.0/ros/Linux/
ln -s $CASROOT/../LICENSE OpenCASCADE6.2.0/LICENSE
ln -s $CASROOT/Linux-jcae/lib OpenCASCADE6.2.0/ros/Linux/lib
unzip ../dist/jcae.zip
mv jcae tmp
mv tmp/* .
rmdir tmp

cd ..
rm $VERSION-Linux.tar.bz2
tar cvfj $VERSION-Linux.tar.bz2 --totals --owner 0 --group 0 -h $VERSION

