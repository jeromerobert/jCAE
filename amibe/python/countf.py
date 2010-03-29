
# jCAE
from org.jcae.mesh.cad import CADShape, CADShapeFactory, CADShapeEnum, CADExplorer

# Python
import sys
from optparse import OptionParser

"""
Count faces in CAD file
"""

cmd=("countf  ", "<cadFile>", "Count faces found in CAD file")
parser = OptionParser(usage="amibebatch %s [OPTIONS] %s\n\n%s" % cmd,
	prog="countf")

(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 1:
	parser.print_usage()
	sys.exit(1)

brepfile = args[0]
factory = CADShapeFactory.getFactory()
shape = factory.newShape(brepfile);
expl = factory.newExplorer()

seen = {}
expl.init(shape, CADShapeEnum.FACE)
while expl.more():
	face = expl.current()
	seen[face] = True
	expl.next()

print("Faces: "+str(len(seen)))
