
# jCAE
from org.jcae.mesh.amibe.ds import Mesh, Triangle
from org.jcae.mesh.amibe.algos3d import SplitEdge
from org.jcae.mesh.amibe.projection import MeshLiaison
from org.jcae.mesh.amibe.traits import MeshTraitsBuilder
from org.jcae.mesh.xmldata import MeshReader, MeshWriter

# Java
from java.util import HashMap, Map
from java.lang import String, Class

# Python
import sys
from optparse import OptionParser

"""
Splits large edges.
"""

parser = OptionParser(usage="amibebatch split [OPTIONS] <inputDir> <outputDir>\n\nSplit large edges", prog="split")
parser.add_option("-c", "--coplanarity", metavar="FLOAT",
                  action="store", type="float", dest="coplanarity",
		  help="dot product of face normals to detect feature edges")
parser.add_option("-t", "--size", metavar="FLOAT", default=0.0,
                  action="store", type="float", dest="size",
                  help="target size")
parser.add_option("-n", "--maxtriangles", metavar="NUMBER",
                  action="store", type="int", dest="maxtriangles",
                  help="stop splitting when mesh contains NUMBER triangles")

(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 2:
	parser.print_usage()
	sys.exit(1)

xmlDir = args[0]
outDir = args[1]

mtb = MeshTraitsBuilder.getDefault3D()
mesh = Mesh(mtb)
MeshReader.readObject3D(mesh, xmlDir)
liaison = MeshLiaison(mesh, mtb)
if options.coplanarity:
	liaison.getMesh().buildRidges(options.coplanarity)

opts = HashMap()
if options.size:
	opts.put("size", str(options.size))
if options.coplanarity:
	opts.put("coplanarity", str(options.coplanarity))

class SplitDebug(SplitEdge):
	cnt = 0
	liaison = None
	ridges = False
	def __init__(self, liaison, options):
		SplitEdge.__init__(self, liaison, options)
		self.liaison = liaison
		if options.get('coplanarity'):
			ridges = True
	def afterProcessHook(self):
		if self.ridges:
			self.liaison.getMesh().createRidgesGroup("ridges")
		MeshWriter.writeObject3D(self.liaison.getMesh(), "DEBUG"+str(self.cnt), String())
		self.cnt += 1
	def afterSwapHook(self):
		if self.ridges:
			self.liaison.getMesh().createRidgesGroup("ridges")
		MeshWriter.writeObject3D(self.liaison.getMesh(), "DEBUG"+str(self.cnt), String())
		self.cnt += 1

#algo = SplitDebug(liaison, opts)
algo = SplitEdge(liaison, opts)
algo.compute();
if options.coplanarity:
	liaison.getMesh().createRidgesGroup("ridges")
MeshWriter.writeObject3D(liaison.getMesh(), outDir, String())

