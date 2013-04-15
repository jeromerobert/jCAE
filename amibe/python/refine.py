
# jCAE
from org.jcae.mesh.amibe.ds import Mesh, AbstractHalfEdge
from org.jcae.mesh.amibe.algos3d import Remesh, QEMDecimateHalfEdge, SwapEdge, RemeshPolyline
from org.jcae.mesh.amibe.traits import MeshTraitsBuilder
from org.jcae.mesh.amibe.projection import MeshLiaison
from org.jcae.mesh.amibe.metrics import EuclidianMetric3D, DistanceMetric
from org.jcae.mesh.xmldata import MeshReader, MeshWriter
from org.jcae.mesh.amibe.metrics.MetricSupport import AnalyticMetricInterface

# Java
from java.util import HashMap
from java.util import ArrayList
from java.util import LinkedHashMap
from java.lang import String, Math

# GNU trove
from gnu.trove.list.array import TIntArrayList

# Python
import sys
from optparse import OptionParser

"""
Remesh an existing mesh.
"""

cmd=("refine  ", "<inputDir> <outputDir>", "Remesh an existing mesh")
parser = OptionParser(usage="amibebatch %s [OPTIONS] %s\n\n%s" % cmd,
	prog="refine")
parser.add_option("-c", "--coplanarity", metavar="FLOAT",
                  action="store", type="float", dest="coplanarity",
		  help="dot product of face normals to detect feature edges")
parser.add_option("-D", "--decimate-target", metavar="NUMBER",
                  action="store", type="int", dest="decimateTarget",
                  help="decimate mesh before remeshing, keep only NUMBER triangles")
parser.add_option("-d", "--decimate", metavar="FLOAT",
                  action="store", type="float", dest="decimateSize",
                  help="decimate mesh before remeshing, specify tolerance")
parser.add_option("-f", "--features", action="store_true", dest="features",
                  help="only remesh feature edges (boundaries, ridges, nonmanifold)")
parser.add_option("-g", "--preserveGroups", action="store_true", dest="preserveGroups",
                  help="edges adjacent to two different groups are handled like free edges")
parser.add_option("-m", "--metricsFile", metavar="STRING",
                  action="store", type="string", dest="metricsFile",
                  help="name of a file containing metrics map")
parser.add_option("-P", "--point-metric", metavar="STRING",
                  action="store", type="string", dest="point_metric_file",
                  help="""A CSV file containing points which to refine around. Each line must contains 6 values:
                  - 1
                  - x, y, z
                  - the distance of the source where the target size is defined
                  - the target size at the given distance""")
parser.add_option("-p", "--project",
                  action="store_true", dest="project",
                  help="project vertices onto local surface")
parser.add_option("-n", "--allowNearNodes",
                  action="store_true", dest="allowNearNodes",
                  help="insert vertices even if this creates a small edge")
parser.add_option("-t", "--size", metavar="FLOAT", default=0.0,
                  action="store", type="float", dest="size",
                  help="target size")
parser.add_option("-T", "--nearLengthRatio", metavar="FLOAT", default=1.0 / Math.sqrt(2.0),
                  action="store", type="float", dest="nearLengthRatio",
		  help="ratio to size target to determine if a vertex is near an existing point (default: 1/sqrt(2)")
parser.add_option("-I", "--immutable-border",
                  action="store_true", dest="immutable_border",
                  help="Tag free edges as immutable")
parser.add_option("--record", metavar="PREFIX",
                  action="store", type="string", dest="recordFile",
                  help="record mesh operations in a Python file to replay this scenario")

(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 2:
	parser.print_usage()
	sys.exit(1)

xmlDir = args[0]
outDir = args[1]

mtb = MeshTraitsBuilder.getDefault3D()
if options.recordFile:
	mtb.addTraceRecord()
mtb.addNodeSet()
mesh = Mesh(mtb)
if options.recordFile:
	mesh.getTrace().setDisabled(True)
MeshReader.readObject3D(mesh, xmlDir)

liaison = MeshLiaison.create(mesh, mtb)
if options.recordFile:
	liaison.getMesh().getTrace().setDisabled(False)
	liaison.getMesh().getTrace().setLogFile(options.recordFile)
	liaison.getMesh().getTrace().createMesh("mesh", liaison.getMesh())
if options.immutable_border:
	liaison.mesh.tagFreeEdges(AbstractHalfEdge.IMMUTABLE)
if options.coplanarity:
	liaison.getMesh().buildRidges(options.coplanarity)
if options.preserveGroups:
	liaison.getMesh().buildGroupBoundaries()

if options.recordFile:
	cmds = [ String("assert self.m.checkNoDegeneratedTriangles()"), String("assert self.m.checkNoInvertedTriangles()"), String("assert self.m.checkVertexLinks()"), String("assert self.m.isValid()") ]
	liaison.getMesh().getTrace().setHooks(cmds)

opts = HashMap()
setAnalytic = False
if options.size:
	opts.put("size", str(options.size))
elif options.metricsFile:
	opts.put("metricsFile", options.metricsFile)
else:
	setAnalytic = True
if options.coplanarity:
	opts.put("coplanarity", str(options.coplanarity))
if options.project:
	opts.put("project", "true")
if options.allowNearNodes:
	opts.put("allowNearNodes", "true")
if options.features:
	opts.put("features", "true")
opts.put("nearLengthRatio", str(options.nearLengthRatio))

if options.decimateSize or options.decimateTarget:
	decimateOptions = HashMap()
	if options.decimateSize:
		decimateOptions.put("size", str(options.decimateSize))
	elif options.decimateTarget:
		decimateOptions.put("maxtriangles", str(options.decimateTarget))
	if options.coplanarity:
		decimateOptions.put("coplanarity", str(options.coplanarity))
	QEMDecimateHalfEdge(liaison, decimateOptions).compute()
	swapOptions = HashMap()
	if options.coplanarity:
		swapOptions.put("coplanarity", str(options.coplanarity))
	SwapEdge(liaison, swapOptions).compute()

algo = Remesh(liaison, opts)
class RemeshMetric(AnalyticMetricInterface):
	def getTargetSize(self, x, y, z):
		return min(200.0, (x - 9000.0)*(x - 9000.0) / 2250.0)

if options.point_metric_file:
    algo.setAnalyticMetric(DistanceMetric(options.size, options.point_metric_file))
elif setAnalytic:
	algo.setAnalyticMetric(RemeshMetric());

algo.compute();
#MeshWriter.writeObject3D(algo.getOutputMesh(), outDir, String())
if options.recordFile:
	liaison.getMesh().getTrace().finish()

# Now compute beams
bgroupMap = LinkedHashMap()
newMesh = algo.getOutputMesh()
#print "beams size: "+str(mesh.getBeams().size())
for i in xrange(newMesh.getBeams().size() / 2):
	bId = newMesh.getBeamGroup(i)
	listBeamId = bgroupMap.get(bId)
	if listBeamId is None:
		listBeamId = TIntArrayList(100)
		bgroupMap.put(bId, listBeamId)
	listBeamId.add(i)

vertices = ArrayList(newMesh.getBeams())
newMesh.resetBeams()
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
		if options.point_metric_file:
			met = DistanceMetric(options.size, options.point_metric_file)
		elif setAnalytic:
			met = RemeshMetric()
		else:
			met = ArrayList()
			for v in polyline:
				met.add(EuclidianMetric3D(options.size))
		#print "Remesh polyline "+str(numPoly+1)+"/"+str(nrPoly)+" of group "+str(bId)+"/"+str(bgroupMap.size())+" "+str(polyline.size())+" vertices"
		result = RemeshPolyline(newMesh, polyline, met).compute()
		for i in xrange(result.size() - 1):
			newMesh.addBeam(result.get(i), result.get(i+1), bId)
		#print "  New polyline: "+str(result.size())+" vertices"

MeshWriter.writeObject3D(newMesh, outDir, "")
