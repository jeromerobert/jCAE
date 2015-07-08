
# jCAE
from org.jcae.mesh.amibe.ds import Mesh, AbstractHalfEdge
from org.jcae.mesh.amibe.algos3d import SwapEdge, ImproveVertexValence, SmoothNodes3DBg
from org.jcae.mesh.amibe.algos3d import RemoveDegeneratedTriangles
from org.jcae.mesh.amibe.traits import MeshTraitsBuilder
from org.jcae.mesh.amibe.projection import MeshLiaison
from org.jcae.mesh.xmldata import MeshReader, MeshWriter

# Java
from java.util import HashMap
from java.lang import String

# Python
import sys
from math import cos, pi
from optparse import OptionParser

"""
Clean an existing mesh
"""

cmd=("clean", "<inputDir> <outputDir>", "Clean an existing mesh")
parser = OptionParser(usage="amibebatch %s [OPTIONS] %s\n\n%s" % cmd, prog="clean")
parser.add_option("-a", "--angle", metavar="FLOAT", default=15.0,
                  action="store", type="float", dest="coplanarityAngle",
                  help="angle (in degrees) between face normals to detect "
                  "feature edges (default: 15, superseded if -c is defined)")
parser.add_option("-c", "--coplanarity", metavar="FLOAT",
                  action="store", type="float", dest="coplanarity",
                  help="dot product of face normals to detect feature edges")
parser.add_option("--no-preserveGroups", default=True, action="store_false",
                  dest="preserveGroups",
                  help="do not preserve groups: edges adjacent to two "
                  "different groups are handled like normal edges (default: "
                  "preserve groups)")
parser.add_option("-e", "--eratio", metavar="FLOAT", default=10.0,
                  action="store", type="float", dest="eratio",
                  help="remove triangles whose edge ratio is greater than "
                  "tolerance (default: 10.0)")
parser.add_option("-I", "--immutable-border",
                  action="store_true", dest="immutable_border",
                  help="Tag free edges as immutable")
parser.add_option("-M", "--immutable-groups", metavar="STRING",
                  action="store", type="string", dest="immutable_groups_file",
                  help="A text file containing the list of groups which whose "
                  "elements and nodes must not be modified by this algorithm.")
parser.add_option("--record", metavar="PREFIX",
                  action="store", type="string", dest="recordFile",
                  help="record mesh operations in a Python file to replay this "
                  "scenario")

(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 2:
    parser.print_usage()
    sys.exit(1)

## Arguments
xmlDir = args[0]
outDir = args[1]

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

immutable_groups = []
if options.immutable_groups_file:
    f = open(options.immutable_groups_file)
    immutable_groups = f.read().split()
    f.close()
    liaison.mesh.tagGroups(immutable_groups, AbstractHalfEdge.IMMUTABLE)

if options.recordFile:
    cmds = [ String("assert self.m.checkNoDegeneratedTriangles()"),
             String("assert self.m.checkNoInvertedTriangles()"),
             String("assert self.m.checkVertexLinks()"),
             String("assert self.m.isValid()") ]
    liaison.getMesh().getTrace().setHooks(cmds)

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
SmoothNodes3DBg(liaison, smoothOptions).compute()

## Remove Degenerated
rdOptions = HashMap()
rdOptions.put("rho", str(options.eratio))
RemoveDegeneratedTriangles(liaison, rdOptions).compute()

## Output
MeshWriter.writeObject3D(liaison.getMesh(), outDir, "")
if options.recordFile:
    liaison.getMesh().getTrace().finish()
