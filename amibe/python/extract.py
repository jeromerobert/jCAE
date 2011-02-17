# jCAE
from org.jcae.mesh.amibe.ds import Mesh
from org.jcae.mesh.xmldata import SubMeshWorker, MeshReader, MeshWriter

# Java
from java.lang import String

# Python
import sys, os
from optparse import OptionParser

"""
   Extract specified groups from a mesh
"""

cmd=("extract    ", "<inputDir> <outputDir> <groupName> [<groupName>...]", "Extract specified groups")
parser = OptionParser(usage="amibebatch %s [OPTIONS] %s\n\n%s" % cmd,
	prog="extract")

(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) < 3:
	parser.print_usage()
	sys.exit(1)

xmlDir = args[0]
outDir = args[1]

s = SubMeshWorker(xmlDir)
groups = args[2:]
extractedDir = s.extractGroups(groups)
m = Mesh()
MeshReader.readObject3D(m, extractedDir)
MeshWriter.writeObject3D(m, outDir, String())

