
# jCAE
from org.jcae.mesh.amibe.ds import Mesh
from org.jcae.mesh.amibe.traits import MeshTraitsBuilder
from org.jcae.mesh.xmldata import MeshWriter
from org.jcae.mesh.amibe.util import UNVReader

# Java
from java.lang import String

# Python
import sys
from optparse import OptionParser

"""
Convert a UNV file into amibe format.
"""

parser = OptionParser(usage="amibebatch unv2amibe [OPTIONS] <unvFile> <amibeDir>\n\nConvert a UNV file into amibe format", prog="unv2amibe")
(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 2:
	parser.print_usage()
	sys.exit(1)

unvFile = args[0]
outDir = args[1]

mesh = Mesh(MeshTraitsBuilder())
UNVReader.readMesh(mesh, unvFile);
MeshWriter.writeObject3D(mesh, outDir, String());
