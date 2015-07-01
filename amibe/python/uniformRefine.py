
# jCAE
from org.jcae.mesh.amibe.ds import Mesh, AbstractHalfEdge
from org.jcae.mesh.amibe.traits import MeshTraitsBuilder, TriangleTraitsBuilder
from org.jcae.mesh.amibe.projection import MeshLiaison, QuadricProjection
from org.jcae.mesh.xmldata import MeshReader, MeshWriter

# Java
from java.util import HashMap
from java.util import ArrayList
from java.util import LinkedHashMap
from java.lang import String, Math

# GNU trove
from gnu.trove.list.array import TIntArrayList

# Python
import sys
from optparse import OptionParser

"""
Remesh an existing mesh.
"""

cmd=("uniformRefine", "<inputDir> <outputDir>", "Cut all edges of an existing mesh")
parser = OptionParser(usage="amibebatch %s [OPTIONS] %s\n\n%s" % cmd,
	prog="uniformRefine")
parser.add_option("-p", "--project",
                  action="store_true", dest="project",
                  help="Project new vertices onto approximated surface")
parser.add_option("-c", "--coplanarity", metavar="FLOAT",
                  action="store", type="float", dest="coplanarity",
                  help="dot product of face normals to detect feature edges")

(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 2:
	parser.print_usage()
	sys.exit(1)

xmlDir = args[0]
outDir = args[1]

# Original mesh will be treated as a background mesh
background_mtb = MeshTraitsBuilder.getDefault3D()
background_mtb.addNodeSet()
background_mesh = Mesh(background_mtb)
MeshReader.readObject3D(background_mesh, xmlDir)
if options.coplanarity:
	background_mesh.buildRidges(options.coplanarity)

# New mesh must not have connectivity
new_mtb = MeshTraitsBuilder()
new_mtb.addTriangleList()
new_mtb.addNodeList()
new_mesh = Mesh(new_mtb)

for point in background_mesh.getNodes():
	new_mesh.add(point)

# Split triangles into 4 new triangles
mapSeenTriangles = {}
for triangle in background_mesh.getTriangles():
	if triangle.hasAttributes(AbstractHalfEdge.OUTER): continue
	edge = triangle.getAbstractHalfEdge()
	newVertices = [None, None, None]
	for i in xrange(3):
		edge = edge.next()
		sym = edge.sym()
		if not sym: continue
		#  May be outer, but it does not matter
		symTri = sym.getTri()
		if symTri in mapSeenTriangles:
			v = mapSeenTriangles[symTri][sym.getLocalNumber()]
		else:
			v = new_mesh.createVertex(0,0,0)
			v.middle(edge.origin(), edge.destination())
			new_mesh.add(v)
		newVertices[edge.getLocalNumber()] = v
	mapSeenTriangles[triangle] = newVertices
	t1 = new_mesh.createTriangle(triangle.v0, newVertices[2], newVertices[1])
	t1.setGroupId(triangle.getGroupId())
	new_mesh.add(t1)
	t2 = new_mesh.createTriangle(triangle.v1, newVertices[0], newVertices[2])
	t2.setGroupId(triangle.getGroupId())
	new_mesh.add(t2)
	t3 = new_mesh.createTriangle(triangle.v2, newVertices[1], newVertices[0])
	t3.setGroupId(triangle.getGroupId())
	new_mesh.add(t3)
	t4 = new_mesh.createTriangle(newVertices[0], newVertices[1], newVertices[2])
	t4.setGroupId(triangle.getGroupId())
	new_mesh.add(t4)
	if not options.project: continue
	# Project inserted point onto surface
	edge = triangle.getAbstractHalfEdge()
	for i in xrange(3):
		edge = edge.next()
		v = newVertices[i]
		if not v: continue
		pos = [0.0]*3
		v.get(pos)
		proj = QuadricProjection(edge.origin())
		if proj.canProject() and not proj.project(v):
			v.moveTo(pos)

# Retrieve original group names
for groupId in xrange(1,background_mesh.getNumberOfGroups()+1):
        new_mesh.setGroupName(groupId, background_mesh.getGroupName(groupId))

MeshWriter.writeObject3D(new_mesh, outDir, "")
