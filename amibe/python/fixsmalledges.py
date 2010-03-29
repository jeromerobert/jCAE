
# jCAE
from org.jcae.opencascade.jni import FixSmallEdges

# Python
import sys
from optparse import OptionParser

"""
Removes small edges from a CAO
"""

cmd=("fixsmalledges", "<input CAD file> <tolerance> <output BRep file>", "Remove small edges from a CAD file")
parser = OptionParser(usage="amibebatch %s [OPTIONS] %s\n\n%s" % cmd,
	prog="fixsmalledges")
(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 3:
	parser.print_usage()
	sys.exit(1)

FixSmallEdges.main(args)

