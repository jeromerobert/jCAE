
# jCAE
from org.jcae.mesh.amibe.ds import Mesh, AbstractHalfEdge
from org.jcae.mesh.amibe.traits import MeshTraitsBuilder
from org.jcae.mesh.xmldata import MeshReader

# Python
import sys, os
from optparse import OptionParser

"""
   Print statistics about mesh elements.
"""

parser = OptionParser(usage="amibebatch classify [OPTIONS] <dir>\n\nPrint statistics about mesh elements", prog="classify")
parser.add_option("-c", "--coplanarity", metavar="FLOAT",
                  action="store", type="float", dest="coplanarity",
		  help="dot product of face normals to detect ridges (default: no ridge detection)")

(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 1:
	parser.print_usage()
	sys.exit(1)

xmlDir = args[0]

mtb = MeshTraitsBuilder.getDefault3D()
mtb.addNodeList()
mesh = Mesh(mtb)
MeshReader.readObject3D(mesh, xmlDir)

if options.coplanarity:
	mesh.buildRidges(options.coplanarity)

nrNodes = 0
nrNMNodes = 0
nrNMEdges = 0
nrFreeEdges = 0
nrSharpEdges = 0
nrTriangles = 0
ot = None
for t in mesh.getTriangles():
	if t.hasAttributes(AbstractHalfEdge.OUTER):
		continue
	ot = t.getAbstractHalfEdge(ot)
	nrTriangles += 1
	for i in range(3):
		ot = ot.next()
		if ot.hasAttributes(AbstractHalfEdge.BOUNDARY):
			nrFreeEdges += 1
		if ot.hasAttributes(AbstractHalfEdge.NONMANIFOLD):
			nrNMEdges += 1
		if ot.hasAttributes(AbstractHalfEdge.SHARP):
			nrSharpEdges += 1
			if ot.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD):
				nrSharpEdges += 1
nrSharpEdges = nrSharpEdges / 2

for v in mesh.getNodes():
	if not v.isWritable():
		continue
	nrNodes += 1
	if not v.isManifold():
		nrNMNodes += 1
print("Number of triangles: "+str(nrTriangles))
print("Number of free edges: "+str(nrFreeEdges))
if nrNMEdges > 0:
	print("Number of non-manifold edges: "+str(nrNMEdges))
if options.coplanarity:
	print("Number of sharp edges: "+str(nrSharpEdges))
if nrNMNodes > 0:
	print("Number of non-manifold vertices: "+str(nrNMNodes))
print("Total number of vertices: "+str(nrNodes))
