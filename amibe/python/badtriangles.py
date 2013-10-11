from org.jcae.mesh.amibe.ds import Mesh
from org.jcae.mesh.amibe.traits import MeshTraitsBuilder
from org.jcae.mesh.amibe.validation import *
from org.jcae.mesh.xmldata import MeshReader
from optparse import OptionParser
import sys

cmd=("report  ", "<dir>", "Print bad triangles in a mesh, sorted by quality.")
parser = OptionParser(usage="amibebatch %s [OPTIONS] %s\n\n%s" % cmd,
	prog="report")
parser.add_option("-c", "--criterion", metavar="CLASS",
                  action="store", type="string", dest="crit",
                  help="criterion (default: MinAngleFace)")
parser.add_option("-n", "--number", metavar="NUMBER",
                  action="store", type="int", dest="number",
                  help="Number of triangles to display (default: 10)")
parser.set_defaults(crit="MinAngleFace", number=10)

(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 1:
	parser.print_usage()
	sys.exit(1)

mesh = Mesh(MeshTraitsBuilder())
xmlDir = args[0]
MeshReader.readObject3D(mesh, xmlDir)
qproc = globals()[options.crit]()
print type(mesh.triangles)
for t in QualityProcedure.worstTriangles(qproc, mesh.triangles, options.number):
    print t