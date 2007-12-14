#! /bin/sh

# Change this to your own OpenCASCADE installation
: ${CASROOT=/home/jerome/OpenCASCADE6.2.0/ros}

# Name of the Java directory in the final bundle
JAVA_NAME=jre-6
# Name of the Groovy directory in the final bundle
GROOVY_NAME=groovy-1.1-beta-2
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
test -d "$JRE_HOME" || JRE_HOME=$(readlink -f $(dirname $(readlink -f $(which java)))/..)	
if [ ! -d "$JRE_HOME" ]; then
	echo "Invalid JRE directory. Cannot find $JRE_HOME"
	exit 0
fi

test -d "$GROOVY_HOME" || GROOVY_HOME=$(readlink -f $(dirname $(readlink -f $(which groovy)))/..)	
if [ ! -d "$GROOVY_HOME" ]; then
	echo "Invalid Groovy directory. Cannot find $GROOVY_HOME"
	exit 0
fi

CASCADE_LIB=$CASROOT/Linux-jcae/lib
test -d "$CASCADE_LIB" || CASCADE_LIB=$CASROOT/Linux/lib
if [ ! -d "$CASCADE_LIB" ]; then
	echo "Invalid OpenCASCADE directory. Cannot find $CASCADE_LIB"
	exit 0
fi

ln -s $JRE_HOME $JAVA_NAME
rm -rf $JAVA_NAME/plugin $JAVA_NAME/javaw $JAVA_NAME/lib/i386/client/classes.jsa || true
mkdir -p $GROOVY_NAME/embeddable $GROOVY_NAME/lib
ln -s $GROOVY_HOME/embeddable/groovy-all-*.jar $GROOVY_NAME/embeddable/
ln -s $GROOVY_HOME/lib/commons-cli*.jar $GROOVY_NAME/lib/
mkdir -p OpenCASCADE6.2.0/ros/Linux/
ln -s $CASROOT/../LICENSE OpenCASCADE6.2.0/LICENSE
ln -s $CASCADE_LIB OpenCASCADE6.2.0/ros/Linux/lib
unzip -q ../dist/jcae.zip
mkdir jcae/jcae/groovy
sed -e 's/^memory=7000m/memory=500m/' ../../amibe/groovy/amibebatch > jcae/bin/amibebatch
cp ../../amibe/groovy/*.groovy jcae/jcae/groovy
chmod a+x jcae/jcae/groovy/*.groovy jcae/bin/amibebatch
mv jcae tmp
mv tmp/* .
rmdir tmp
mkdir -p docs/getting_started
cp ../../htdocs/src/documentation/content/xdocs/getting_started/*.{html,png} docs/getting_started/

cd ..
rm $VERSION-Linux.tar.bz2 || true
tar cfj $VERSION-Linux.tar.bz2 --totals --owner 0 --group 0 --exclude=.svn -h $VERSION

