
# jCAE
from org.jcae.mesh.xmldata import UNV2Amibe

# Java
from java.lang import String

# Python
import sys
from optparse import OptionParser

"""
Convert a UNV file into amibe format and write it on disk
"""

cmd=("unv2amibe  ", "<unvFile> <amibeDir>", "Convert a UNV file into amibe format")
parser = OptionParser(usage="amibebatch %s [OPTIONS] %s\n\n%s" % cmd, prog="unv2amibe")
(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 2:
	parser.print_usage()
	sys.exit(1)

unvFile = args[0]
outDir = args[1]

u2a = UNV2Amibe()
u2a.importMesh(unvFile, outDir)

