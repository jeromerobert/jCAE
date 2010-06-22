
# jCAE
from org.jcae.mesh.amibe.ds import Mesh
from org.jcae.mesh.xmldata import MeshReader, MeshWriter

# Java
from java.lang import String

# Python
import sys, os
from optparse import OptionParser

"""
   Automatic partitioning of a mesh, based on feature edges
"""

cmd=("partition  ", "<inputDir> <outputDir>", "Automatic partitioning of a mesh, based on feature edges")
parser = OptionParser(usage="amibebatch %s [OPTIONS] %s\n\n%s" % cmd,
	prog="report")
parser.add_option("-c", "--coplanarity", metavar="FLOAT", default=0.95,
                  action="store", type="float", dest="coplanarity",
		  help="minimum dot product of face normals when building feature edges (default 0.95)")

(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 2:
	parser.print_usage()
	sys.exit(1)

xmlDir = args[0]
outDir = args[1]

mesh = Mesh()
MeshReader.readObject3D(mesh, xmlDir)
mesh.buildRidges(options.coplanarity)
mesh.buildPartition()
MeshWriter.writeObject3D(mesh, outDir, String())

