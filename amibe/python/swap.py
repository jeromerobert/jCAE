
# jCAE
from org.jcae.mesh.amibe.ds import Mesh, AbstractHalfEdge
from org.jcae.mesh.amibe.algos3d import SwapEdge
from org.jcae.mesh.amibe.projection import MeshLiaison
from org.jcae.mesh.xmldata import MeshReader, MeshWriter

# Java
from java.util import HashMap
from java.lang import String

# Python
import sys
from optparse import OptionParser

"""
Swap edges to improve mesh quality.
"""

cmd=("swap     ", "<inputDir> <outputDir>", "Swap edges to improve mesh quality")
parser = OptionParser(usage="amibebatch %s [OPTIONS] %s\n\n%s" % cmd,
	prog="swap")
parser.add_option("-c", "--coplanarity", metavar="FLOAT", default=0.95,
                  action="store", type="float", dest="coplanarity",
		  help="minimum dot product of face normals allowed for swapping an edge (default 0.95)")
parser.add_option("-g", "--preserveGroups", action="store_true", dest="preserveGroups",
                  help="edges adjacent to two different groups are handled like free edges")
parser.add_option("-I", "--immutable-border",
                  action="store_true", dest="immutable_border",
                  help="Tag free edges as immutable")

(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 2:
	parser.print_usage()
	sys.exit(1)

xmlDir = args[0]
outDir = args[1]

mesh = Mesh()
MeshReader.readObject3D(mesh, xmlDir)
liaison = MeshLiaison(mesh)
if options.immutable_border:
    liaison.mesh.tagFreeEdges(AbstractHalfEdge.IMMUTABLE)
if options.coplanarity:
	liaison.getMesh().buildRidges(options.coplanarity)
if options.preserveGroups:
	liaison.getMesh().buildGroupBoundaries()

opts = HashMap()
opts.put("coplanarity", str(options.coplanarity))
sm = SwapEdge(liaison, opts)
sm.setProgressBarStatus(10000)
sm.compute()

MeshWriter.writeObject3D(liaison.getMesh(), outDir, String())

