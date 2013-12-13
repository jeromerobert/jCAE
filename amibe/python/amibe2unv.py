
# jCAE
from org.jcae.mesh.xmldata import Amibe2UNV

# Python
import sys
from optparse import OptionParser

"""
Read 3d file and export mesh into a UNV file.
"""

cmd=("amibe2unv  ", "<xmlDir> <unvFile>", "Export an Amibe mesh into a UNV mesh")
parser = OptionParser(usage="amibebatch %s [OPTIONS] %s\n\n%s" % cmd,
	prog="amibe2unv")

(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 2:
	parser.print_usage()
	sys.exit(1)

xmlDir = args[0]
filename = args[1]

a2u = Amibe2UNV(xmlDir)
a2u.write(filename)

