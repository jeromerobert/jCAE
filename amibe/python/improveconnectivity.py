
# jCAE
from org.jcae.mesh.amibe.algos3d import ImproveEdgeConnectivity
from org.jcae.mesh.amibe.ds import Mesh
from org.jcae.mesh.amibe.traits import MeshTraitsBuilder
from org.jcae.mesh.amibe.projection import MeshLiaison
from org.jcae.mesh.xmldata import MeshReader, MeshWriter


# Java
from java.util import HashMap
from java.lang import String

# Python
import sys, os
from optparse import OptionParser

"""
   Swap edges to improve node connectivity
"""

cmd=("valence    ", "<inputDir> <outputDir>", "Swap edges to improve node connectivity")
parser = OptionParser(usage="amibebatch %s [OPTIONS] %s\n\n%s" % cmd,
	prog="valence")
parser.add_option("-g", "--preserveGroups", action="store_true", dest="preserveGroups",
                  help="edges adjacent to two different groups are handled like free edges")
parser.add_option("-c", "--coplanarity", metavar="FLOAT", default=0.95,
                  action="store", type="float", dest="coplanarity",
		  help="minimum dot product of face normals when building feature edges (default 0.95)")

(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 2:
	parser.print_usage()
	sys.exit(1)

xmlDir = args[0]
outDir = args[1]

mtb = MeshTraitsBuilder.getDefault3D()
mesh = Mesh(mtb)
MeshReader.readObject3D(mesh, xmlDir)
liaison = MeshLiaison.create(mesh, mtb)

if options.coplanarity:
	liaison.getMesh().buildRidges(options.coplanarity)
if options.preserveGroups:
	liaison.getMesh().buildGroupBoundaries()

opts = HashMap()
if options.coplanarity:
	opts.put("coplanarity", str(options.coplanarity))
ImproveEdgeConnectivity(liaison, opts).compute()
MeshWriter.writeObject3D(liaison.getMesh(), outDir, String())

