
# jCAE
from org.jcae.mesh.amibe.ds import Mesh
from org.jcae.mesh.amibe.algos3d import Remesh
from org.jcae.mesh.amibe.traits import MeshTraitsBuilder
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

parser = OptionParser(usage="amibebatch remesh [OPTIONS] <inputDir> <outputDir>\n\nRemesh an existing mesh", prog="remesh")
parser.add_option("-c", "--coplanarity", metavar="FLOAT", default=-1.0,
                  action="store", type="float", dest="coplanarity",
		  help="dot product of face normals to detect feature edges")
parser.add_option("-t", "--size", metavar="FLOAT", default=0.0,
                  action="store", type="float", dest="size",
                  help="target size")
parser.add_option("-m", "--metricsFile", metavar="STRING",
                  action="store", type="string", dest="metricsFile",
                  help="name of a file containing metrics map")
parser.add_option("-p", "--project",
                  action="store_true", dest="project",
                  help="project vertices onto local surface")
parser.add_option("-d", "--decimate", metavar="FLOAT",
                  action="store", type="float", dest="decimateSize",
                  help="decimate mesh before remeshing, specify tolerance")
parser.add_option("-D", "--decimate-target", metavar="NUMBER",
                  action="store", type="int", dest="decimateTarget",
                  help="decimate mesh before remeshing, keep only NUMBER triangles")

(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 2:
	parser.print_usage()
	sys.exit(1)

xmlDir = args[0]
outDir = args[1]

mtb = MeshTraitsBuilder.getDefault3D()
mtb.addNodeList()
mesh = Mesh(mtb)
MeshReader.readObject3D(mesh, xmlDir)

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
if options.decimateSize:
	opts.put("decimateSize", str(options.decimateSize))
elif options.decimateTarget:
	opts.put("decimateTarget", str(options.decimateTarget))

algo = Remesh(mesh, opts)
class RemeshMetric(Remesh.AnalyticMetricInterface):
	def getTargetSize(self, x, y, z):
		return min(200.0, (x - 9000.0)*(x - 9000.0) / 2250.0)

if setAnalytic:
	algo.setAnalyticMetric(RemeshMetric());
algo.compute();
MeshWriter.writeObject3D(algo.getOutputMesh(), outDir, String())

