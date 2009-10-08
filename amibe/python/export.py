
# jCAE
from org.jcae.mesh.xmldata import MeshExporter

# Java
from java.lang import String

# Python
import sys
from optparse import OptionParser

"""
Read 3d file and export mesh into another format.
"""

parser = OptionParser(usage="amibebatch export [OPTIONS] <xmlDir> <filename>\n\nExport Amibe mesh into other file format", prog="export")
parser.add_option("-f", "--format", metavar="STRING",
                  action="store", type="string", dest="format",
                  help="output file format")
parser.add_option("-F", "--list-formats", action="store_true", dest="listFormats",
                  help="list available output formats")

(options, args) = parser.parse_args(args=sys.argv[1:])

if options.listFormats:
	print "List of supported file formats:"
	print "\tUNV: I-DEAS Universal file format (default suffix: .unv)"
	print "\tMESH: Medit format (default suffix: .mesh)"
	print "\tSTL: STL (default suffix: .stl)"
	print "\tVTK: VTK (default suffix: .vtp)"
	sys.exit(0)

if len(args) != 2:
	parser.print_usage()
	sys.exit(1)

xmlDir = args[0]
filename = args[1]

format = options.format
if not options.format:
	if filename.endswith(".unv") or filename.endswith(".UNV"):
		format = "UNV"
	elif filename.endswith(".mesh") or filename.endswith(".MESH"):
		format = "MESH"
	elif filename.endswith(".stl") or filename.endswith(".STL"):
		format = "STL"
	elif filename.endswith(".vtp"):
		format = "VTK"
	else:
		print "Unknown extension: cannot guess output file format."
		sys.exit(1)

innerClass = None
for klass in MeshExporter.getDeclaredClasses():
	if klass.getName().endswith('$'+format):
		innerClass = klass
		break

if not innerClass:
	print "Unknown file format."
	sys.exit(1)

cons = innerClass.getConstructor([ String ])
cons.newInstance([ xmlDir ]).write(filename)

