
# jCAE
from org.jcae.mesh.amibe.ds import Mesh
from org.jcae.mesh.amibe.algos3d import SwapEdge
from org.jcae.mesh.xmldata import MeshReader, MeshWriter

# Java
from java.util import HashMap
from java.lang import String

# Python
import sys
from optparse import OptionParser

"""
Swap edges to improve mesh quality.
"""

parser = OptionParser(usage="amibebatch swap [OPTIONS] <inputDir> <outputDir>\n\nSwap edges to improve mesh quality", prog="swap")
parser.add_option("-c", "--coplanarity", metavar="FLOAT", default=0.95,
                  action="store", type="float", dest="coplanarity",
		  help="minimum dot product of face normals allowed for swapping an edge (default 0.95)")

(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 2:
	parser.print_usage()
	sys.exit(1)

xmlDir = args[0]
outDir = args[1]

mesh = Mesh()
MeshReader.readObject3D(mesh, xmlDir)

opts = HashMap()
opts.put("coplanarity", str(options.coplanarity))
sm = SwapEdge(mesh, opts)
sm.setProgressBarStatus(10000)
sm.compute()

MeshWriter.writeObject3D(mesh, outDir, String())

