
# jCAE
from org.jcae.mesh.amibe.ds import Mesh, AbstractHalfEdge
from org.jcae.mesh.amibe.algos3d import Remesh, QEMDecimateHalfEdge, SwapEdge, RemeshPolyline, ImproveVertexValence, SmoothNodes3DBg
from org.jcae.mesh.amibe.traits import MeshTraitsBuilder
from org.jcae.mesh.amibe.projection import MeshLiaison
from org.jcae.mesh.amibe.metrics import EuclidianMetric3D, DistanceMetric
from org.jcae.mesh.xmldata import MeshReader, MeshWriter
from org.jcae.mesh.amibe.metrics.MetricSupport import AnalyticMetric

# Java
from java.util import HashMap
from java.util import ArrayList
from java.util import LinkedHashMap
from java.lang import String, Math

# GNU trove
from gnu.trove.list.array import TIntArrayList

# Python
import sys
from math import cos, pi
from optparse import OptionParser

"""
Remesh an existing mesh around singularities
"""

cmd=("remeshSingularity  ", "<inputDir> <outputDir> <pointMetric> <sizeInf>",
"""Remesh an existing mesh around singularities:
    1) Decimate (optional)
    2) Refine according to pointMetric and sizeInf
    3) Swap edges
    4) Improve vertex valence
    5) Smooth mesh (move vertices)

   inputDir
        input amibe mesh

   outputDir
        output amibe mesh

   pointMetric
        A CSV file containing points which to refine around. Each line must
        contains 7 values: 1, x, y, z, s0 (target size at distance d0), d0, d1
        (distance from which point has no influence).
        Target size at distance d of this point is computed like this:
            a) if d <= d0, s0
            b) if d >= d1, global mesh size sInf
            c) if d0 < d < d1, s0 + (sInf - s0) * (d^2 - d0^2)/(d1^2 - d0^2)

        In addition, the numerical criterion makes sure that the target sizes hi
        and hj of nodes i and j of each edge e_ij are such that hi < rho * hj
        with rho > 1 (default 2.0).

    sizeInf
        Target size sInf""")
parser = OptionParser(usage="amibebatch %s [OPTIONS] %s\n\n%s" % cmd,
    prog="remeshSingularity")
parser.add_option("-a", "--angle", metavar="FLOAT", default=15.0,
                  action="store", type="float", dest="coplanarityAngle",
                  help="angle (in degrees) between face normals to detect "
                  "feature edges (default: 15, superseded if -c is defined)")
parser.add_option("-c", "--coplanarity", metavar="FLOAT",
                  action="store", type="float", dest="coplanarity",
                  help="dot product of face normals to detect feature edges")
parser.add_option("-D", "--decimate-target", metavar="NUMBER",
                  action="store", type="int", dest="decimateTarget",
                  help="decimate mesh before remeshing, keep only NUMBER "
                  "triangles")
parser.add_option("-d", "--decimate", metavar="FLOAT",
                  action="store", type="float", dest="decimateSize",
                  help="decimate mesh before remeshing, specify tolerance")
parser.add_option("--no-preserveGroups", default=True, action="store_false",
                  dest="preserveGroups",
                  help="do not preserve groups: edges adjacent to two "
                  "different groups are handled like normal edges (default: "
                  "preserve groups)")
parser.add_option("-n", "--allowNearNodes",
                  action="store_true", dest="allowNearNodes",
                  help="insert vertices even if this creates a small edge")
parser.add_option("-r", "--rho", metavar="FLOAT", default=2.0,
                  action="store", type="float", dest="rho",
                  help="numerical metric ratio (required: rho > 1, default: 2)")
parser.add_option("-T", "--nearLengthRatio", metavar="FLOAT",
                  default=1.0/Math.sqrt(2.0), action="store", type="float",
                  dest="nearLengthRatio",
                  help="ratio to size target to determine if a vertex is near "
                  "an existing point (default: 1/sqrt(2))")
parser.add_option("-I", "--immutable-border",
                  action="store_true", dest="immutable_border",
                  help="Tag free edges as immutable")
parser.add_option("--record", metavar="PREFIX",
                  action="store", type="string", dest="recordFile",
                  help="record mesh operations in a Python file to replay this "
                  "scenario")

(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 4:
	parser.print_usage()
	sys.exit(1)

## Arguments
xmlDir = args[0]
outDir = args[1]
point_metric_file = args[2]
sizeinf = float(args[3])

## Process coplanarity option
coplanarity = cos(options.coplanarityAngle * pi / 180.)
if options.coplanarity:
	coplanarity = options.coplanarity

## Build background mesh
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
liaison.getMesh().buildRidges(coplanarity)
if options.preserveGroups:
	liaison.getMesh().buildGroupBoundaries()

if options.recordFile:
	cmds = [ String("assert self.m.checkNoDegeneratedTriangles()"),
			 String("assert self.m.checkNoInvertedTriangles()"),
			 String("assert self.m.checkVertexLinks()"),
			 String("assert self.m.isValid()") ]
	liaison.getMesh().getTrace().setHooks(cmds)

## Decimate
if options.decimateSize or options.decimateTarget:
	decimateOptions = HashMap()
	if options.decimateSize:
		decimateOptions.put("size", str(options.decimateSize))
	elif options.decimateTarget:
		decimateOptions.put("maxtriangles", str(options.decimateTarget))
	decimateOptions.put("coplanarity", str(coplanarity))
	QEMDecimateHalfEdge(liaison, decimateOptions).compute()
	swapOptions = HashMap()
	swapOptions.put("coplanarity", str(coplanarity))
	SwapEdge(liaison, swapOptions).compute()

## Remesh
refineOptions = HashMap()
refineOptions.put("size", str(sizeinf))
refineOptions.put("coplanarity", str(coplanarity))
refineOptions.put("nearLengthRatio", str(options.nearLengthRatio))
refineOptions.put("project", "false")
if options.allowNearNodes:
	refineOptions.put("allowNearNodes", "true")
refineAlgo = Remesh(liaison, refineOptions)

if options.rho > 1.0:
	## mixed metric
	refineAlgo.setAnalyticMetric(DistanceMetric(sizeinf, point_metric_file, options.rho, True))
else:
	## analytic metric
	refineAlgo.setAnalyticMetric(DistanceMetric(sizeinf, point_metric_file))
refineAlgo.compute();

## Swap
swapOptions = HashMap()
swapOptions.put("coplanarity", str(coplanarity))
swapOptions.put("minCosAfterSwap", "0.3")
SwapEdge(liaison, swapOptions).compute()

## Improve valence
valenceOptions = HashMap()
valenceOptions.put("coplanarity", str(coplanarity))
valenceOptions.put("checkNormals", "false")
ImproveVertexValence(liaison, valenceOptions).compute()

## Smooth
smoothOptions = HashMap()
smoothOptions.put("iterations", str(8))
smoothOptions.put("boundaries", "false")
smoothOptions.put("check", "true")
smoothOptions.put("size", str(-1.0))
smoothOptions.put("tolerance", str(2.0))
smoothOptions.put("relaxation", str(0.6))
smoothOptions.put("refresh", "false")
if (coplanarity >= 0.0):
	smoothOptions.put("coplanarity", str(coplanarity))
smoothAlgo = SmoothNodes3DBg(liaison, smoothOptions)
smoothAlgo.compute()

## Now compute beams
bgroupMap = LinkedHashMap()
newMesh = smoothAlgo.getOutputMesh()
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
	mapGroupToListOfPolylines.put(bId, listOfPolylines)

for bId in bgroupMap.keySet():
	listBeamId = bgroupMap.get(bId)
	listOfPolylines = mapGroupToListOfPolylines.get(bId)
	nrPoly = listOfPolylines.size()
	for numPoly in xrange(nrPoly):
		polyline = listOfPolylines.get(numPoly)
		if options.point_metric_file:
			met = DistanceMetric(options.size, options.point_metric_file)
		else:
			met = ArrayList()
			for v in polyline:
				met.add(EuclidianMetric3D(options.size))
		result = RemeshPolyline(newMesh, polyline, met).compute()
		for i in xrange(result.size() - 1):
			newMesh.addBeam(result.get(i), result.get(i+1), bId)

## Output
MeshWriter.writeObject3D(newMesh, outDir, "")
if options.recordFile:
	liaison.getMesh().getTrace().finish()
