#! /bin/sh -e

# Configure you path
: ${CASROOT=/home/jerome/home2/OpenCASCADE6.2.0/ros}
LOG4J=/usr/share/java/log4j-1.2.jar
TROVE=/home/jerome/home2/JAVA/trove-2.0/lib/trove.jar
# G++ 3.3 compiler (required by opencascade)
CXX33=g++-3.3
JRE_HOME=/usr/lib/jvm/java-6-sun/jre/


echo "This script will prepare a clean checkout to build the whole platform (amibe, viewer3d and occjava also). It is not intented to be use for daily development."

cd `dirname $0`/..
JCAE_ROOT=$PWD
export CASROOT
export JRE_HOME

echo Build occjava
cd occjava
./autogen.sh
./configure CXX=$CXX33
make

echo Build amibe
mkdir $JCAE_ROOT/amibe/lib || true
cd $JCAE_ROOT/amibe/lib
ln -sf $LOG4J log4j.jar
ln -sf $TROVE trove.jar
ln -sf ../../occjava/lib/occjava.jar
cd ..
ant jar

echo Build viewer3d
mkdir $JCAE_ROOT/viewer3d/lib || true
cd $JCAE_ROOT/viewer3d/lib
ln -sf ../../occjava/lib/occjava.jar
ln -sf ../../amibe/lib/jcae.jar
ln -sf $LOG4J log4j.jar
ln -sf $TROVE trove.jar
cd ..
ant jar

echo Build viewer3d with support for other amibe formats
mkdir $JCAE_ROOT/viewer3d-amibe/lib || true
cd $JCAE_ROOT/viewer3d-amibe/lib
ln -sf ../../viewer3d/lib/jcae-viewer3d.jar
ln -sf ../../amibe/lib/jcae.jar
ln -sf $LOG4J log4j.jar
ln -sf $TROVE trove.jar
cd ..
ant jar

echo Configure netbeans-suite
cd $JCAE_ROOT/netbeans-suite/jcae-netbeans/release/modules/ext/
ln -sf ../../../../../amibe/lib/jcae.jar
ln -sf ../../../../../amibe/lib/jcae-mesherocc.jar
ln -sf ../../../../../viewer3d/lib/jcae-viewer3d.jar
ln -sf ../../../../../viewer3d-amibe/lib/jcae-viewer3d-amibe.jar
ln -sf $LOG4J log4j.jar
ln -sf $TROVE trove.jar
cd $JCAE_ROOT/netbeans-suite/occjava/release/modules/lib/
ln -sf ../../../../../occjava/src/.libs/libOccJava.so
cd $JCAE_ROOT/netbeans-suite/occjava/release/modules/ext/
ln -sf ../../../../../occjava/lib/occjava.jar

echo Open the netbeans-suite project in Netbeans and run "Build zip distribution". Then run the make-dist-Linux.sh and you are done.

