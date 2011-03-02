
# jCAE
from org.jcae.mesh.amibe.ds import Mesh, AbstractHalfEdge
from org.jcae.mesh.amibe.validation import *
from org.jcae.mesh.xmldata import MeshReader, MeshExporter
from gnu.trove import TIntHashSet

# Java
from java.lang import Class

# Python
import sys, os
from optparse import OptionParser
import jarray

"""
   Sample class to print quality statistics about an amibe mesh.
   Example: to print histogram about minimum angles:
    amibebatch report -c MinAngleFace -s .0174532925199432957 -b 6,12,18,24,30,36,42,48,54
"""

def list_criteria(option, opt, value, parser):
	listStr = QualityProcedure.getListSubClasses()
	print("List of available criteria for -c option:")
	while len(listStr) > 0:
		print " ", listStr.pop(0)
		print "   ", listStr.pop(0)
	sys.exit(0)

cmd=("report  ", "<dir>", "Print statistics about mesh quality")
parser = OptionParser(usage="amibebatch %s [OPTIONS] %s\n\n%s" % cmd,
	prog="report")
parser.add_option("-b", "--bounds", metavar="LIST",
                  action="store", type="string", dest="bounds",
                  help="comma separated list of values, implies -H")
parser.add_option("-c", "--criterion", metavar="CLASS",
                  action="store", type="string", dest="crit",
                  help="criterion (default: MinAngleFace)")
parser.add_option("-C", "--list-criteria", action="callback", callback=list_criteria,
                  help="lists all available criteria")
parser.add_option("-d", "--detailed", action="store_true", dest="detailed",
                  help="reports statistics by face")
parser.add_option("-f", "--from-face", metavar="NUMBER",
                  action="store", type="int", dest="ifacemin",
                  help="meshing had been started from this patch number")
parser.add_option("-H", "--histogram", action="store_true", dest="histogram",
                  help="prints histogram")
parser.add_option("-m", "--mesh", action="store_true", dest="verboseMesh",
                  help="give details about mesh (number of vertices, triangles, etc)")
parser.add_option("-o", "--output", metavar="BASE",
                  action="store", type="string", dest="outBasename",
                  help="creates <BASE>.mesh and <BASE>.bb MEDIT files")
parser.add_option("-s", "--scale", metavar="NUMBER",
                  action="store", type="float", dest="scaleFactor",
                  help="scale factor (default: 1.0)")
parser.set_defaults(crit="MinAngleFace", scaleFactor=1.0, ifacemin=1)

(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 1:
	parser.print_usage()
	sys.exit(1)

xmlDir = args[0]
if options.bounds:
	options.histogram = True
	bounds = [float(i) for i in options.bounds.split(",")]
else:
	bounds = None

qprocFactory = QualityProcedureFactory("org.jcae.mesh.amibe.validation."+options.crit)
qproc = qprocFactory.buildQualityProcedure()
mtb = qproc.getMeshTraitsBuilder()
if options.verboseMesh:
	if not  mtb.hasNodes():
		mtb.addNodeList()
	ttb = mtb.getTriangleTraitsBuilder()
	if not ttb.hasHalfEdge() and not ttb.hasVirtualHalfEdge():
		ttb.addHalfEdge()

mesh = Mesh(mtb);
MeshReader.readObject3D(mesh, xmlDir)
# Compute mesh quality
if options.verboseMesh:
	nrVertices = 0
	nrNMVertices = 0
	nrEdges = 0
	nrNMEdges = 0
	nrFreeEdges = 0
	nrTriangles = 0
	ot = None
	for t in mesh.getTriangles():
		if not t.isWritable():
			continue
		ot = t.getAbstractHalfEdge(ot)
		nrTriangles += 1
		for i in range(3):
			ot = ot.next()
			nrEdges += 1
			if ot.hasAttributes(AbstractHalfEdge.BOUNDARY):
				nrFreeEdges += 1
				nrEdges += 1
			if ot.hasAttributes(AbstractHalfEdge.NONMANIFOLD):
				nrNMEdges += 1
	nrEdges /= 2

	for v in mesh.getNodes():
		if not v.isWritable():
			continue
		nrVertices += 1
		if not v.isManifold():
			nrNMVertices += 1
	print("Number of triangles: "+str(nrTriangles))
	print("Total number of edges: "+str(nrEdges))
	if nrFreeEdges > 0:
		print("Number of free edges: "+str(nrFreeEdges))
	if nrNMEdges > 0:
		print("Number of non-manifold edges: "+str(nrNMEdges))
	print("Total number of vertices: "+str(nrVertices))
	if nrNMVertices > 0:
		print("Number of non-manifold vertices: "+str(nrNMVertices))

nrFaces = 1
if options.detailed:
	groups = TIntHashSet(mesh.getTriangles().size())
	for f in mesh.getTriangles():
		if f.isWritable():
			i = f.getGroupId() + 1 - options.ifacemin
			if i >= 0:
				groups.add(i)
	nrFaces = groups.size()
mean = mesh.getTriangles().size() / nrFaces
data = jarray.zeros(nrFaces, QualityFloat)
for i in xrange(len(data)):
	data[i] = QualityFloat(mean)
	data[i].setQualityProcedure(qproc)
	data[i].setTarget(options.scaleFactor)
if qproc.getType() == QualityProcedure.FACE:
	for f in mesh.getTriangles():
		if not f.isWritable():
			continue
		i = f.getGroupId() + 1 - options.ifacemin
		if i < 0 or not options.detailed:
			i = 0
		data[i].compute(f)
elif qproc.getType() == QualityProcedure.NODE:
	if options.detailed:
		print("The --detailed option cannot be combined with -c NodeConnectivity")
		sys.exit(1)
	for v in mesh.getNodes():
		if v.isWritable():
			data[0].compute(v)

for i in xrange(len(data)):
	data[i].finish()
	if options.detailed:
		print("Face "+str(i+1))
	if options.histogram:
		# Prints histogram on console
		if bounds:
			data[i].split(bounds)
		else:
			data[i].split(10)
		data[i].printLayers()
	else:
		data[i].printStatistics()

if None != options.outBasename:
	# Prints triangle quality into a .bb file to be displayed by MEDIT
	if options.detailed:
		ids = jarray.zeros(1, "i")
		for i in xrange(len(data)):
			ids[0] = i + options.ifacemin - 1
			data[i].printMeshBB(options.outBasename+"-"+ids[0]+".bb")
			MeshExporter.MESH(File(xmlDir), ids).write(options.outBasename+"-"+ids[0]+".mesh")
	else:
		data[0].printMeshBB(options.outBasename+".bb")
		MeshExporter.MESH(xmlDir).write(options.outBasename+".mesh")

