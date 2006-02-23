javac -classpath ../lib/occjava.jar Example1.java
export MMGT_OPT=0
LD_LIBRARY_PATH=../src/.libs/:$CASROOT/Linux/lib/ java -classpath ../lib/occjava.jar:. Example1
