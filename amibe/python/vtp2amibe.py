
# jCAE
from org.jcae.mesh.amibe.ds import Mesh, Vertex
from org.jcae.mesh.amibe.traits import MeshTraitsBuilder
from org.jcae.mesh.xmldata import MeshWriter
from org.jcae.mesh.amibe.util import UNVReader

# Java
from java.lang import String

# VTK
from org.jcae.vtk import Utils
from vtk import vtkXMLPolyDataReader, vtkPolyData

# Python
import sys
from optparse import OptionParser
import jarray

"""
Convert a .vtp file into amibe format.
"""

parser = OptionParser(usage="amibebatch vtp2amibe [OPTIONS] <vtpFile> <amibeDir>\n\nConvert a .vtp file into amibe format", prog="vtp2amibe")
(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 2:
	parser.print_usage()
	sys.exit(1)

vtpFile = args[0]
outDir = args[1]

Utils.loadVTKLibraries()
reader = vtkXMLPolyDataReader()
reader.SetFileName(vtpFile)
reader.Update()
 
polydata = reader.GetOutput()

mesh = Mesh(MeshTraitsBuilder())
vertices = jarray.zeros(polydata.GetNumberOfPoints(), Vertex)
coord = jarray.zeros(3, "d")
for i in xrange(len(vertices)):
	polydata.GetPoint(i, coord)
	vertices[i] = mesh.createVertex(coord)

indices = Utils.getValues(polydata.GetPolys())
i = 0
while i < len(indices):
	if (indices[i] == 3):
		mesh.add(mesh.createTriangle(
			vertices[indices[i+1]],
			vertices[indices[i+2]],
			vertices[indices[i+3]]))
	i += indices[i] + 1

MeshWriter.writeObject3D(mesh, outDir, String())
