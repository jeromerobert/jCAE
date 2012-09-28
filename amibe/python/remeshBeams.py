
# jCAE
from org.jcae.mesh.amibe.ds import Mesh, Vertex
from org.jcae.mesh.amibe.traits import MeshTraitsBuilder
from org.jcae.mesh.amibe.metrics import EuclidianMetric3D
from org.jcae.mesh.amibe.algos3d import PolylineFactory, RemeshPolyline
from org.jcae.mesh.xmldata import MeshReader, MeshWriter

from gnu.trove import TIntArrayList

# Java
from java.util import ArrayList
from java.util import LinkedHashMap
from java.lang import String, Math

# Python
import sys
from optparse import OptionParser

"""
Remesh beams
"""

cmd=("remeshBeams ", "<inputDir> <outputDir>", "Remesh beams")
parser = OptionParser(usage="amibebatch %s [OPTIONS] %s\n\n%s" % cmd,
	prog="remeshESN")
parser.add_option("-t", "--size", metavar="FLOAT", default=0.0,
                  action="store", type="float", dest="size",
                  help="target size")

(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 2:
	parser.print_usage()
	sys.exit(1)

xmlDir = args[0]
outDir = args[1]
if options.size:
	size = options.size
else:
	size = 1.0

mtb = MeshTraitsBuilder.getDefault3D()
mtb.addNodeList()
mesh = Mesh(mtb)
MeshReader.readObject3D(mesh, xmlDir)

polylines=PolylineFactory(mesh)
mesh.resetBeams()
for entry in polylines.entrySet():
  groupId = entry.key
  for polyline in entry.value:
		listM = ArrayList()
		for v in polyline:
			listM.add(EuclidianMetric3D(size))
		#for v in polyline:
		#	print v
		#print "Remesh polyline "+str(numPoly+1)+"/"+str(nrPoly)+" of group "+str(bId)+"/"+str(bgroupMap.size())+" "+str(polyline.size())+" vertices"
		result = RemeshPolyline(mesh, polyline, listM).compute()
		for i in xrange(result.size() - 1):
			mesh.addBeam(result.get(i), result.get(i+1), groupId)
		print "  New polyline: "+str(result.size())+" vertices"
		#for v in result:
		#	print v

MeshWriter.writeObject3D(mesh, outDir, "")
