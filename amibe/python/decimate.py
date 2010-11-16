
# jCAE
from org.jcae.mesh.amibe.ds import Mesh, AbstractHalfEdge
from org.jcae.mesh.amibe.projection import MeshLiaison
from org.jcae.mesh.xmldata import MeshReader, MeshWriter

# Java
from java.util import HashMap, Map
from java.lang import String, Class

# Python
import sys
from optparse import OptionParser

"""
Decimate a mesh.
"""

defaultAlgo = "QEMDecimateHalfEdge"
cmd=("decimate", "<inputDir> <outputDir>", "Decimate a mesh")
parser = OptionParser(usage="amibebatch %s [OPTIONS] %s\n\n%s" % cmd,
	prog="decimate")
parser.add_option("-A", "--list-algorithm", action="store_true", dest="listAlgorithm",
                  help="lists all available decimation algorithms")
parser.add_option("-a", "--algorithm", metavar="STRING", default=defaultAlgo,
                  action="store", type="string", dest="algorithm",
		  help="decimation algorithm (default: "+defaultAlgo+")")
parser.add_option("-f", "--freeEdgeTol", metavar="FLOAT",
                  action="store", type="float", dest="freeEdgeTol",
                  help="Decimate free edges whose length is smaller than tolerance.  -t value is used if not specified. (for LengthDecimateHalfEdge only)")
parser.add_option("-g", "--preserveGroups", action="store_true", dest="preserveGroups",
                  help="edges adjacent to two different groups are handled like free edges")
parser.add_option("-m", "--maxlength", metavar="FLOAT",
                  action="store", type="float", dest="maxlength",
                  help="no edges longer than this value are created")
parser.add_option("-n", "--targetTriangles", metavar="NUMBER",
                  action="store", type="int", dest="targetTriangles",
                  help="stops iterations when mesh contains this number of triangles")
parser.add_option("-O", "--freeEdgesOnly", action="store_true", dest="freeEdgesOnly",
                  help="removes only free edges")
parser.add_option("-t", "--tolerance", metavar="FLOAT",
                  action="store", type="float", dest="tolerance",
                  help="geometry error allowed when decimating")
parser.add_option("-I", "--immutable-border",
                  action="store_true", dest="immutable_border",
                  help="Tag free edges as immutable")
parser.add_option("-G", "--immutable-border-group",
                  action="store_true", dest="immutable_border_group",
                  help="Tag border group edges as immutable")
                  
(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 2:
	parser.print_usage()
	sys.exit(1)

if options.listAlgorithm:
	print "Available algorithms for decimation:" 
	print "    LengthDecimateHalfEdge" 
	print "    QEMDecimateHalfEdge" 
	sys.exit(0)

xmlDir = args[0]
outDir = args[1]

opts = HashMap()
if options.tolerance:
	opts.put("size", str(options.tolerance))
elif options.targetTriangles:
	opts.put("maxtriangles", str(options.targetTriangles))
if options.freeEdgesOnly:
	opts.put("freeEdgesOnly", "true");
if options.freeEdgeTol:
	opts.put("freeEdgeTol", str(options.freeEdgeTol))
if options.maxlength:
	opts.put("maxlength", str(options.maxlength))

mesh = Mesh()
MeshReader.readObject3D(mesh, xmlDir)

liaison = MeshLiaison(mesh)
if options.immutable_border:
    liaison.mesh.tagFreeEdges(AbstractHalfEdge.IMMUTABLE)

if options.immutable_border_group:
    liaison.mesh.tagGroupBoundaries(AbstractHalfEdge.IMMUTABLE)
else:
    if options.preserveGroups:
	liaison.getMesh().buildGroupBoundaries()
    
cons = Class.forName("org.jcae.mesh.amibe.algos3d."+options.algorithm).getConstructor([ MeshLiaison, Map ])
cons.newInstance([ liaison, opts ]).compute()

MeshWriter.writeObject3D(liaison.getMesh(), outDir, String())

