#include "jnistream.hxx"

//#define DEBUG
using namespace std;

static void jthrow(JNIEnv *env, const char *name, const char *msg)
{
	jclass cls = env->FindClass(name);
	/* if cls is NULL, an exception has already been thrown => remove it because it's from the JVM*/
	if (cls == NULL)
	{
		env->ExceptionClear();
		jthrow(env,name,msg);
	}
	else
	{
		if(env->ThrowNew(cls, msg)<0)
		{
			//the throw exception failled
			std::cout<<"#JNI throw exception failled !"<<std::endl;
			std::cout<<name<<"\t : "<<msg<<std::endl;
		}
		/* free the local ref */
		env->DeleteLocalRef(cls);
	}
}

jnistreambuf::~jnistreambuf()
{
	delete[] nativeBuffer;
}

jnistreambuf::jnistreambuf(JNIEnv * env, jobject jstream, int bufferSize):
	env(env), jstream(jstream)
{
#ifdef DEBUG
	cerr << "jnistreambuf::jnistreambuf [start]" << endl;
#endif
	nativeBuffer = new char[bufferSize];
	fullNioBuffer=env->NewDirectByteBuffer(nativeBuffer, bufferSize);

	jclass inputStreamClass = env->FindClass("java/nio/channels/ReadableByteChannel");
	jclass outputStreamClass = env->FindClass("java/nio/channels/WritableByteChannel");
 
	readMID = env->GetMethodID(inputStreamClass, "read","(Ljava/nio/ByteBuffer;)I");  
	writeMID = env->GetMethodID(outputStreamClass, "write","(Ljava/nio/ByteBuffer;)I");  
	
	bool in=env->IsInstanceOf(jstream, inputStreamClass)!=0;
	bool out=env->IsInstanceOf(jstream, outputStreamClass)!=0;

	if(in)
	{
#ifdef DEBUG
	cerr << "jnistreambuf::jnistreambuf: init reading buffer" << endl;
#endif
		setg(nativeBuffer,     // beginning of putback area
			 nativeBuffer,     // read position
			 nativeBuffer);    // end position      
	}

	if(out)
	{
#ifdef DEBUG
	cerr << "jnistreambuf::jnistreambuf: init writting buffer [start]" << endl;
#endif
		setp( nativeBuffer, nativeBuffer + bufferSize );

#ifdef DEBUG
	cerr << "jnistreambuf::jnistreambuf: init writting buffer [done]" << endl;
#endif
	}

	if(!out && !in)
	{
		jthrow(env, "java/lang/ClassCastException", "InputStream or OutputStream expected");
	}

#ifdef DEBUG
	cerr << "jnistreambuf::jnistreambuf [done]" << endl;
#endif
}

int jnistreambuf::underflow()
{
#ifdef DEBUG
	cerr << "jnistreambuf::underflow" << endl;
#endif
	// used for input buffer only
    if ( gptr() && ( gptr() < egptr()))
        return * reinterpret_cast<unsigned char *>( gptr());

	//read the java bytes
    int num = env->CallIntMethod(jstream, readMID, fullNioBuffer);

    if (num <= 0) // ERROR or EOF
        return EOF;

    // reset buffer pointers
    setg( eback(),   // beginning of putback area
          eback(),                 // read position
          eback() + num);          // end of buffer

    // return next character
    return * reinterpret_cast<unsigned char *>( gptr());    
}

int jnistreambuf::overflow( int c)
{
#ifdef DEBUG
	cerr << "jnistreambuf::overflow" << endl;
#endif
	// used for output buffer only
    if (c != EOF) {
        *pptr() = c;
        pbump(1);
    }

    if ( flush_buffer() == EOF)
        return EOF;
    return c;
}

int jnistreambuf::sync()
{
#ifdef DEBUG
	cerr << "jnistreambuf::sync()" << endl;
#endif
    // Changed to use flush_buffer() instead of overflow( EOF)
    // which caused improper behavior with std::endl and flush(),
    // bug reported by Vincent Ricard.
    if ( pptr() && pptr() > pbase()) {
        if ( flush_buffer() == EOF)
            return -1;
    }
    return 0;
}

int jnistreambuf::flush_buffer()
{
#ifdef DEBUG
	cerr << "jnistreambuf::flush_buffer" << endl;
	//for(char * p=pbase(); p!=pptr(); p++) cerr << *p;
#endif
    // Separate the writing of the buffer from overflow() and
    // sync() operation.
    int w = pptr() - pbase();

	//write the c chars
	jobject nioBuffer;
	
	if(pptr()==epptr())
		nioBuffer=fullNioBuffer;
	else
		nioBuffer=env->NewDirectByteBuffer(pbase(), w);

    env->CallIntMethod(jstream, writeMID, nioBuffer);

    pbump( -w);
    return w;
}

