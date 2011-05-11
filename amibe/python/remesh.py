
# jCAE
from org.jcae.mesh.amibe.ds import Mesh, AbstractHalfEdge
from org.jcae.mesh.amibe.algos3d import *
from org.jcae.mesh.amibe.traits import MeshTraitsBuilder
from org.jcae.mesh.amibe.projection import MeshLiaison
from org.jcae.mesh.amibe.metrics import EuclidianMetric3D
from org.jcae.mesh.xmldata import MeshReader, MeshWriter, Amibe2VTK
from org.jcae.mesh.amibe.algos3d import SmoothNodes3DBg, RemeshPolyline

# Java
from java.util import HashMap
from java.util import ArrayList
from java.util import LinkedHashMap

# GNU trove
from gnu.trove import TIntArrayList

# Python
import sys
from optparse import OptionParser

debug_write_counter=1
def writeVTK(liaison):
    global debug_write_counter
    """MeshWriter.writeObject3D(liaison.mesh, "/tmp/tmp.amibe", "")
    Amibe2VTK("/tmp/tmp.amibe").write("/tmp/m%i.vtp" % debug_write_counter);"""
    debug_write_counter=debug_write_counter+1

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

liaison = MeshLiaison(mesh, mtb)
if options.recordFile:
	liaison.getMesh().getTrace().setDisabled(False)
	liaison.getMesh().getTrace().setLogFile(options.recordFile)
	liaison.getMesh().getTrace().createMesh("mesh", liaison.getMesh())
if options.immutable_border:
	liaison.mesh.tagFreeEdges(AbstractHalfEdge.IMMUTABLE)
liaison.getMesh().buildRidges(0.9)
if options.immutable_border_group:
	liaison.mesh.tagGroupBoundaries(AbstractHalfEdge.IMMUTABLE)
else:
	if options.preserveGroups:
		liaison.getMesh().buildGroupBoundaries()

#0
writeVTK(liaison)

opts = HashMap()
opts.put("coplanarity", "0.9")
opts.put("size", str(options.size*0.06))
QEMDecimateHalfEdge(liaison, opts).compute()

#1
writeVTK(liaison)

opts.clear()
opts.put("size", str(options.size))
Remesh(liaison, opts).compute()

#2
writeVTK(liaison)

opts.clear()
opts.put("coplanarity", "0.9")
SwapEdge(liaison, opts).compute()

#3
writeVTK(liaison)

opts.clear()
opts.put("size", str(options.size*0.3))
opts.put("freeEdgesOnly", "true")
LengthDecimateHalfEdge(liaison, opts).compute()

#4
writeVTK(liaison)

opts.clear()
opts.put("coplanarity", "0.9")
ImproveEdgeConnectivity(liaison, opts).compute()

#5
writeVTK(liaison)

opts.clear()
opts.put("coplanarity", "0.9")
opts.put("iterations", str(8))
opts.put("size", str(options.size))
SmoothNodes3DBg(liaison, opts).compute()

#6
writeVTK(liaison)

opts.clear()
opts.put("coplanarity", "0.9")
opts.put("expectInsert", "false")
SwapEdge(liaison, opts).compute()

#7
writeVTK(liaison)

opts.clear()
opts.put("coplanarity", "0.9")
opts.put("size", str(options.size*0.2))
opts.put("maxlength", str(options.size*1.2))
QEMDecimateHalfEdge(liaison, opts).compute()

writeVTK(liaison)

opts.clear()
opts.put("coplanarity", "0.9")
opts.put("expectInsert", "false")
SwapEdge(liaison, opts).compute()

writeVTK(liaison)

opts.clear()
opts.put("coplanarity", "0.75")
opts.put("tolerance", "0.6")
opts.put("iterations", str(8))
SmoothNodes3DBg(liaison, opts).compute()

writeVTK(liaison)

#MeshWriter.writeObject3D(liaison.mesh, outDir, ""
polylines=PolylineFactory(liaison.mesh, 135.0, options.size*0.2)
liaison.mesh.resetBeams()
for entry in polylines.entrySet():
  groupId = entry.key
  for polyline in entry.value:
		listM = ArrayList()
		for v in polyline:
			listM.add(EuclidianMetric3D(options.size))
		#print "Remesh polyline of group "+str(groupId)+"/"+str(polylines.size())+" "+str(polyline.size())+" vertices"
		result = RemeshPolyline(liaison.mesh, polyline, listM).compute()
		for i in xrange(result.size() - 1):
			liaison.mesh.addBeam(result.get(i), result.get(i+1), groupId)
		#print "  New polyline: "+str(result.size())+" vertices"

if options.recordFile:
	liaison.getMesh().getTrace().finish()
MeshWriter.writeObject3D(liaison.mesh, outDir, "")
