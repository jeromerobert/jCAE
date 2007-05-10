#ifndef JNISTREAM_H
#define JNISTREAM_H 

// standard C++ with new header file names and std:: namespace
#include <iostream>
#include <jni.h>

class jnistreambuf : public std::streambuf
{
public:
    jnistreambuf(JNIEnv * env, jobject stream, int bufferSize = 1024);
    virtual int overflow( int c = EOF);
    virtual int underflow();
    virtual int sync();
	~jnistreambuf();
private:
	int flush_buffer();
	char * nativeBuffer;
	JNIEnv * env;
	jmethodID readMID, writeMID;
	jobject jstream, fullNioBuffer;
};

#endif
