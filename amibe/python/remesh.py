
# jCAE
from org.jcae.mesh.amibe.ds import Mesh, AbstractHalfEdge
from org.jcae.mesh.amibe.algos3d import Remesh, QEMDecimateHalfEdge, SwapEdge, PointMetric
from org.jcae.mesh.amibe.traits import MeshTraitsBuilder
from org.jcae.mesh.amibe.projection import MeshLiaison
from org.jcae.mesh.xmldata import MeshReader, MeshWriter

# Java
from java.util import HashMap
from java.lang import String

# Python
import sys
from optparse import OptionParser

"""
Remesh an existing mesh.
"""

cmd=("remesh  ", "<inputDir> <outputDir>", "Remesh an existing mesh")
parser = OptionParser(usage="amibebatch %s [OPTIONS] %s\n\n%s" % cmd,
	prog="remesh")
parser.add_option("-c", "--coplanarity", metavar="FLOAT", default=-1.0,
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
                  help="""A CSV file containing points which to refine around. Each line must contains 5 floating point values:
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
parser.add_option("-I", "--immutable-border",
                  action="store_true", dest="immutable_border",
                  help="Tag free edges as immutable")

(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 2:
	parser.print_usage()
	sys.exit(1)

xmlDir = args[0]
outDir = args[1]

mtb = MeshTraitsBuilder.getDefault3D()
if options.decimateSize or options.decimateTarget:
	mtb.addNodeSet()
else:
	mtb.addNodeList()
mesh = Mesh(mtb)
MeshReader.readObject3D(mesh, xmlDir)

liaison = MeshLiaison(mesh, mtb)
if options.immutable_border:
    liaison.mesh.tagFreeEdges(AbstractHalfEdge.IMMUTABLE)
if options.coplanarity:
	liaison.getMesh().buildRidges(options.coplanarity)
if options.preserveGroups:
	liaison.getMesh().buildGroupBoundaries()

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
class RemeshMetric(Remesh.AnalyticMetricInterface):
	def getTargetSize(self, x, y, z):
		return min(200.0, (x - 9000.0)*(x - 9000.0) / 2250.0)

if options.point_metric_file:
    algo.setAnalyticMetric(PointMetric(options.size, options.point_metric_file))
elif setAnalytic:
	algo.setAnalyticMetric(RemeshMetric());

algo.compute();
MeshWriter.writeObject3D(algo.getOutputMesh(), outDir, String())

