
# jCAE
from org.jcae.mesh.amibe.ds import Mesh, AbstractHalfEdge
from org.jcae.mesh.amibe.algos3d import Remesh, QEMDecimateHalfEdge, SwapEdge, PointMetric
from org.jcae.mesh.amibe.traits import MeshTraitsBuilder
from org.jcae.mesh.amibe.projection import MeshLiaison
from org.jcae.mesh.amibe.metrics import EuclidianMetric3D
from org.jcae.mesh.xmldata import MeshReader, MeshWriter
from org.jcae.mesh.amibe.algos3d import SmoothNodes3DBg, RemeshPolyline

# Java
from java.util import HashMap
from java.util import ArrayList
from java.util import LinkedHashMap
from java.lang import String, Math

# GNU trove
from gnu.trove import TIntArrayList

# Python
import sys
from optparse import OptionParser

"""
Remesh an existing mesh.
"""

cmd=("remesh  ", "<inputDir> <outputDir>", "Remesh an existing mesh")
parser = OptionParser(usage="amibebatch %s [OPTIONS] %s\n\n%s" % cmd,
	prog="remesh")
parser.add_option("-g", "--preserveGroups", action="store_true", dest="preserveGroups",
                  help="edges adjacent to two different groups are handled like free edges")
parser.add_option("-t", "--size", metavar="FLOAT", default=0.0,
                  action="store", type="float", dest="size",
                  help="target size")
parser.add_option("-I", "--immutable-border",
                  action="store_true", dest="immutable_border",
                  help="Tag free edges as immutable")
parser.add_option("-G", "--immutable-border-group",
                  action="store_true", dest="immutable_border_group",
                  help="Tag border group edges as immutable")

(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 2:
	parser.print_usage()
	sys.exit(1)

xmlDir = args[0]
outDir = args[1]

mtb = MeshTraitsBuilder.getDefault3D()
mtb.addNodeSet()
mesh = Mesh(mtb)
MeshReader.readObject3D(mesh, xmlDir)

liaison = MeshLiaison(mesh, mtb)
if options.immutable_border:
    liaison.mesh.tagFreeEdges(AbstractHalfEdge.IMMUTABLE)
liaison.getMesh().buildRidges(0.9)
if options.immutable_border_group:
    liaison.mesh.tagGroupBoundaries(AbstractHalfEdge.IMMUTABLE)
else:
    if options.preserveGroups:
	liaison.getMesh().buildGroupBoundaries()

opts = HashMap()
opts.put("coplanarity", "0.9")
opts.put("size", str(options.size*0.06))
QEMDecimateHalfEdge(liaison, opts).compute()

opts.clear()
opts.put("coplanarity", "0.9")
SwapEdge(liaison, opts).compute()

opts.clear()
opts.put("size", str(options.size))
Remesh(liaison, opts).compute()

opts.clear()
opts.put("coplanarity", "0.9")
SwapEdge(liaison, opts).compute()

opts.clear()
opts.put("allowNearNodes", "true")
opts.put("size", str(options.size))
Remesh(liaison, opts).compute()

opts.clear()
opts.put("coplanarity", "0.9")
SwapEdge(liaison, opts).compute()

opts.clear()
opts.put("coplanarity", "0.9")
opts.put("size", str(options.size*0.1))
opts.put("maxlength", str(options.size*1.1))
QEMDecimateHalfEdge(liaison, opts).compute()

opts.clear()
opts.put("coplanarity", "0.9")
SwapEdge(liaison, opts).compute()

opts.clear()
opts.put("coplanarity", "0.9")
opts.put("iterations", str(8))
SmoothNodes3DBg(liaison, opts).compute()

opts.clear()
opts.put("coplanarity", "0.9")
opts.put("size", str(options.size*0.2))
opts.put("maxlength", str(options.size*1.2))
QEMDecimateHalfEdge(liaison, opts).compute()

opts.clear()
opts.put("coplanarity", "0.75")
opts.put("tolerance", "0.6")
opts.put("iterations", str(8))
SmoothNodes3DBg(liaison, opts).compute()

#MeshWriter.writeObject3D(liaison.mesh, outDir, "")

# Now remesh beams
bgroupMap = LinkedHashMap()
for i in xrange(liaison.mesh.getBeams().size() / 2):
	bId = liaison.mesh.getBeamGroup(i)
	listBeamId = bgroupMap.get(bId)
	if listBeamId is None:
		listBeamId = TIntArrayList(100)
		bgroupMap.put(bId, listBeamId)
	listBeamId.add(i)

vertices = ArrayList(liaison.mesh.getBeams())
liaison.mesh.resetBeams()
mapGroupToListOfPolylines = LinkedHashMap()
for bId in bgroupMap.keySet():
	listBeamId = bgroupMap.get(bId)
	listOfPolylines = ArrayList()
	polyline = ArrayList()
	lastVertex = None
	for i in xrange(listBeamId.size()):
		b = listBeamId.get(i) 
		if lastVertex != vertices.get(2*b):
			# New polyline
			polyline = ArrayList()
			listOfPolylines.add(polyline)
			polyline.add(vertices.get(2*b))
		lastVertex = vertices.get(2*b+1)
		polyline.add(lastVertex)
	#print "Group "+str(bId)+" contains "+str(listOfPolylines.size())+" polylines and "+str(listBeamId.size()+1)+" vertices"
	mapGroupToListOfPolylines.put(bId, listOfPolylines)

for bId in bgroupMap.keySet():
	listBeamId = bgroupMap.get(bId)
	listOfPolylines = mapGroupToListOfPolylines.get(bId)
	nrPoly = listOfPolylines.size()
	for numPoly in xrange(nrPoly):
		polyline = listOfPolylines.get(numPoly)
		listM = ArrayList()
		for v in polyline:
			listM.add(EuclidianMetric3D(options.size))
		#print "Remesh polyline "+str(numPoly+1)+"/"+str(nrPoly)+" of group "+str(bId)+"/"+str(bgroupMap.size())+" "+str(polyline.size())+" vertices"
		result = RemeshPolyline(liaison.mesh, polyline, listM).compute()
		for i in xrange(result.size() - 1):
			liaison.mesh.addBeam(result.get(i), result.get(i+1), bId)
		#print "  New polyline: "+str(result.size())+" vertices"

MeshWriter.writeObject3D(liaison.mesh, outDir, "")
