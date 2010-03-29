
# jCAE
from org.jcae.mesh.amibe.ds import Mesh, Triangle
from org.jcae.mesh.amibe.algos3d import SmoothNodes3DBg
from org.jcae.mesh.amibe.traits import MeshTraitsBuilder
from org.jcae.mesh.amibe.projection import MeshLiaison
from org.jcae.mesh.xmldata import MeshReader, MeshWriter

# Java
from java.util import HashMap
from java.lang import String

# Python
import sys
from optparse import OptionParser

"""
Smooth mesh.
"""

parser = OptionParser(usage="amibebatch smooth3dBg [OPTIONS] <inputDir> <outputDir>\n\nPerform vertex smoothing on 3D mesh", prog="smooth3d")
parser.add_option("-C", "--no-check", action="store_false", dest="check",
                  help="allow moving a vertex even if this decreases its quality")
parser.add_option("-c", "--coplanarity", metavar="FLOAT", default=-1.0,
                  action="store", type="float", dest="coplanarity",
                  help="dot product of face normals to detect feature edges")
parser.add_option("-g", "--preserveGroups", action="store_true", dest="preserveGroups",
                  help="edges adjacent to two different groups are handled like free edges")
parser.add_option("-i", "--iterations", metavar="NUMBER", default=1,
                  action="store", type="int", dest="iterations",
		  help="number of iterations (default: 1)")
parser.add_option("-N", "--no-boundaries", action="store_false", dest="boundaries",
                  help="do not try to preserve patch boundaries")
parser.add_option("-P", "--prefix", metavar="DIR",
                  action="store", type="string", dest="prefix",
                  help="store mesh after each iteration into <DIR>0, <DIR>1, etc")
parser.add_option("-r", "--relaxation", metavar="FLOAT", default=0.6,
                  action="store", type="string", dest="relaxation",
                  help="new position = old + r * (new - old)")
parser.add_option("-R", "--refresh",
                  action="store_true", dest="refresh",
                  help="update triangle quality within loop")
parser.add_option("-s", "--size", metavar="FLOAT", default=-1.0,
                  action="store", type="float", dest="size",
                  help="target size")
parser.add_option("-t", "--tolerance", metavar="FLOAT", default=2.0,
                  action="store", type="float", dest="tolerance",
                  help="process only nodes with quality lower than this value")

(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 2:
	parser.print_usage()
	sys.exit(1)

class MySmoothNodes3DBg(SmoothNodes3DBg):
	prefix = None
	def __init__(self, liaison, opts, prefix):
		SmoothNodes3DBg.__init__(self, liaison, opts)
		self.prefix = prefix
		self.liaison = liaison
		if self.prefix:
			MeshWriter.writeObject3D(liaison.getOutputMesh(), self.prefix + "0", None)
	def postProcessIteration(self, liaison, counter, *args):
		if self.prefix:
			MeshWriter.writeObject3D(liaison.getOutputMesh(), self.prefix + str(counter+1), None)

xmlDir = args[0]
outDir = args[1]

mtb = MeshTraitsBuilder.getDefault3D()
mtb.addNodeList()
mesh = Mesh(mtb)
MeshReader.readObject3D(mesh, xmlDir)
liaison = MeshLiaison(mesh, mtb)
if options.coplanarity:
	liaison.getMesh().buildRidges(options.coplanarity)
if options.preserveGroups:
	liaison.getMesh().buildGroupBoundaries()

opts = HashMap()
opts.put("iterations", str(options.iterations))
opts.put("boundaries", str(options.boundaries))
opts.put("check", str(options.check))
opts.put("size", str(options.size))
opts.put("tolerance", str(options.tolerance))
opts.put("relaxation", str(options.relaxation))
opts.put("refresh", str(options.refresh))
if (options.coplanarity >= 0.0):
	opts.put("coplanarity", str(options.coplanarity))
sm = MySmoothNodes3DBg(liaison, opts, options.prefix)
sm.setProgressBarStatus(10000)
sm.compute()

MeshWriter.writeObject3D(sm.getOutputMesh(), outDir, String())

