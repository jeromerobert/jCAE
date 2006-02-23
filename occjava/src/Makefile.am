lib_LTLIBRARIES=libOccJava.la

IFACEFILES=\
	BRepPrimAPI.i \
	BRep_Tool.i \
	BRepTools.i \
	GeomLProp_SLProps.i \
	gp.i OccJava.i \
	Standard.i \
	TopAbs.i \
	TopoDS.i \
	BRepAlgoAPI.i \
	BRepOffsetAPI_Sewing.i \
	Poly.i \
	Geom.i \
	BRepBuilderAPI.i

BUILT_SOURCES = OccJava_wrap.cxx
JAVAGENSRCDIR=$(top_srcdir)/src-java/org/jcae/opencascade/jni/

$(BUILT_SOURCES) : $(IFACEFILES)
	echo "Running swig on $(IFACEFILES)"
	mkdir -p $(JAVAGENSRCDIR)
	swig -c++ -v $(CPPFLAGS) -Wall -java -package org.jcae.opencascade.jni -outdir $(JAVAGENSRCDIR) OccJava.i

libOccJava_la_SOURCES =	$(BUILT_SOURCES)

