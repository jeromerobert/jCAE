from org.jcae.mesh.amibe.ds import Mesh
from org.jcae.mesh.amibe.traits import MeshTraitsBuilder
from org.jcae.mesh.amibe.algos3d import RandomizeGroups
from org.jcae.mesh.xmldata import *
from optparse import OptionParser
import sys

cmd=("random  ", "<dir>", "Put random triangles in a given group.")
parser = OptionParser(usage="amibebatch %s [OPTIONS] %s\n\n%s" % cmd,	prog="random")
parser.add_option("-r", "--ratio", metavar="FLOAT", default=0.03, action="store",
                  type="float", dest="ratio",
                  help="The ratio of triangles to put in the new group")
parser.add_option("-n", "--name", metavar="STRING", action="store",
                  type="string", dest="group_name", default="random",
                  help="""The name of group to be created.""")

(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 1:
	parser.print_usage()
	sys.exit(1)

mesh = Mesh(MeshTraitsBuilder.getDefault3D())
xmlDir = args[0]
MeshReader.readObject3D(mesh, xmlDir)
RandomizeGroups(options.ratio, options.group_name).compute(mesh, True)
MeshWriter.writeObject3D(mesh, xmlDir, None)