
# jCAE
from org.jcae.mesh.amibe.ds import Mesh, AbstractHalfEdge
from org.jcae.mesh.amibe.algos3d import Remesh, QEMDecimateHalfEdge, SwapEdge, PointMetric
from org.jcae.mesh.amibe.traits import MeshTraitsBuilder
from org.jcae.mesh.amibe.projection import MeshLiaison
from org.jcae.mesh.xmldata import MeshReader, MeshWriter
from org.jcae.mesh.amibe.algos3d import SmoothNodes3DBg
# Java
from java.util import HashMap
from java.lang import String, Math

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
opts.put("maxtriangles", "1")
opts.put("maxlength", str(options.size*1.1))
QEMDecimateHalfEdge(liaison, opts).compute()

opts.clear()
opts.put("coplanarity", "0.9")
SwapEdge(liaison, opts).compute()

opts.clear()
opts.put("coplanarity", "0.9")
opts.put("size", str(options.size))
opts.put("iterations", str(1))
SmoothNodes3DBg(liaison, opts).compute()

opts.clear()
opts.put("coplanarity", "0.9")
opts.put("maxtriangles", "1")
opts.put("maxlength", str(options.size*1.1))
QEMDecimateHalfEdge(liaison, opts).compute()

opts.clear()
opts.put("coplanarity", "0.9")
opts.put("size", str(options.size))
opts.put("iterations", str(10))
SmoothNodes3DBg(liaison, opts).compute

opts.put("coplanarity", "0.5")
opts.put("tolerance", "0.3")
opts.put("iterations", str(2))
algo = SmoothNodes3DBg(liaison, opts)
algo.compute()

MeshWriter.writeObject3D(algo.getOutputMesh(), outDir, String())

