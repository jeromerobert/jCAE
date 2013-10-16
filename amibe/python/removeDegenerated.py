
# jCAE
from org.jcae.mesh.amibe.ds import Mesh
from org.jcae.mesh.amibe.algos3d import RemoveDegeneratedTriangles
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
Remove degenerated triangles
"""

cmd=("removeDegenerated   ", "<inputDir> <outputDir>",
    "Remove degenerated triangles")
parser = OptionParser(usage="amibebatch %s [OPTIONS] %s\n\n%s" % cmd,
    prog="removeDegenerated")
parser.add_option("-r", "--rho", metavar="FLOAT", default=100.0,
                  action="store", type="float", dest="rho",
                  help="max tolerated anisotropy")

(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 2:
	parser.print_usage()
	sys.exit(1)

## Arguments
xmlDir = args[0]
outDir = args[1]

## Build background mesh
mtb = MeshTraitsBuilder.getDefault3D()
mesh = Mesh(mtb)
MeshReader.readObject3D(mesh, xmlDir)
liaison = MeshLiaison.create(mesh, mtb)

## Process algo
opts = HashMap()
opts.put("rho", str(options.rho))

algo = RemoveDegeneratedTriangles(liaison, opts)
algo.compute();

## Output mesh
MeshWriter.writeObject3D(liaison.getMesh(), outDir, String())
