
# jCAE
from org.jcae.mesh.cad import *
from org.jcae.mesh.amibe.ds import MMesh1D, MeshParameters
from org.jcae.mesh.amibe.algos1d import *
from org.jcae.mesh.amibe.algos2d import *
from org.jcae.mesh.amibe import *
from org.jcae.mesh.amibe.patch import Mesh2D
from org.jcae.mesh.amibe.patch import InvalidFaceException
from org.jcae.mesh.amibe.traits import MeshTraitsBuilder
from org.jcae.mesh.xmldata import *
from org.jcae.opencascade.jni import BRepTools

# Java
from java.lang import System
from java.util import HashMap
from java.util.logging import Logger

# Python
import sys, os
from optparse import OptionParser, SUPPRESS_HELP
from array import array

"""
Sample class to show how to call amibe algorithms.
"""

cmd=("mesh    ", "<cadFile> <outputDir> <edgeLength> <deflection>", "Mesh a CAD file")
parser = OptionParser(usage="amibebatch %s [OPTIONS] %s\n\n%s" % cmd,
	prog="mesh")
##  tries to eliminate edges smaller than this value (default: patch size/1000)
#parser.add_option("-e", "--epsilon", metavar="NUMBER", default=-1.0,
#                  action="store", type="float", dest="epsilon",
#                  help=SUPPRESS_HELP)
##  tries to merge small edges together
#parser.add_option("-E", "--cumulativeEpsilon", action="store_true", dest="cumulativeEpsilon",
#                  help=SUPPRESS_HELP)
parser.add_option("-f", "--from-face", metavar="NUMBER", default=1,
                  action="store", type="int", dest="ifacemin",
                  help="start meshing from this patch number")
parser.add_option("-o", "--output", metavar="FILE",
                  action="store", type="string", dest="output",
                  help="exports UNV file (for phase 3)")
parser.add_option("-p", "--phase", metavar="LIST", default="1,2,3",
                  action="store", type="string", dest="phases",
                  help="comma separated list of phases (Default: 1,2,3)")
parser.add_option("-s", "--smooth2d", metavar="LIST",
                  action="store", type="float", dest="smooth2d",
                  help="comma separated list of options passed to 2D smoother (available options are: iterations=N,relaxation=N,tolerance=N; run smooth2d -h for details)")
parser.add_option("-t", "--to-face", metavar="NUMBER", default=0,
                  action="store", type="int", dest="ifacemax",
                  help="mesh up to this patch number")
parser.add_option("-x", "--explodeBrep",
                  action="store_true", dest="explodeBrep",
                  help="writes face.<i>.brep file before meshing each patch")
parser.set_defaults(crit="MinAngleFace", scaleFactor=1.0, ifacemin=0, ifacemax=0)

(options, args) = parser.parse_args(args=sys.argv[1:])

if len(args) != 4:
	parser.print_usage()
	sys.exit(1)

brepfile = args[0]
outputDir = args[1]
leng = float(args[2])
defl = float(args[3])

logger=Logger.getLogger("mesh")
(brepdir, brepfile) = os.path.split(brepfile)
if brepdir is None:
	brepdir = "."

phases = [False, False, False, False]
for s in options.phases.split(","):
	p = int(s)
	if p < 1 or p > 3:
		parser.print_usage()
		sys.exit(1)
	phases[p] = True

#System.setProperty("org.jcae.mesh.amibe.ds.Mesh.epsilon", str(options.epsilon))
#System.setProperty("org.jcae.mesh.amibe.ds.Mesh.cumulativeEpsilon", str(options.cumulativeEpsilon))

unvName = options.output

try:
	os.makedirs(outputDir)
except:
	pass
if not os.path.exists(outputDir) or not os.path.isdir(outputDir):
	logger.severe("Cannot write to "+outputDir)
	sys.exit(1)

"""
Mesh 1D
This method takes as
   Input : shape (the shape to be meshed)
   Output: ...
"""

factory = CADShapeFactory.getFactory()

mesh1d = None
shape = None
if phases[1]:
	if brepfile.endswith(".step") or brepfile.endswith(".stp") or brepfile.endswith(".igs"):
		shape = CADShapeFactory.getFactory().newShape(os.path.join(brepdir, brepfile))
		brepfile = os.path.splitext(brepfile)[0] + ".tmp.brep"
		shape.writeNative(os.path.join(outputDir, brepfile))
	elif brepdir != outputDir:
		try:
			input = open(os.path.join(brepdir, brepfile), 'r')
			buffer = input.read()
			input.close()
			output = open(os.path.join(outputDir, brepfile), 'w')
			output.write(buffer)
			output.close()
		except:
			pass
	mesh1d = MMesh1D(os.path.join(outputDir, brepfile))
	shape = mesh1d.getGeometry()
	options1d = HashMap()
	options1d.put("size", str(leng))
	if defl <= 0.0:
		UniformLength(mesh1d, options1d).compute()
	else:
		options1d.put("deflection", str(defl))
		options1d.put("relativeDeflection", "true")
		UniformLengthDeflection(mesh1d, options1d).compute()
		Compat1D2D(mesh1d, options1d).compute()
	
	MMesh1DWriter.writeObject(mesh1d, outputDir, brepfile)
	logger.info("Edges discretized")

# Mesh 2D
if phases[2]:
	if mesh1d is None:
		logger.warn("Phase 2 is invoked without phase 1, argument "+brepfile+" is ignored")
		mesh1d = MMesh1DReader.readObject(outputDir)
		shape = mesh1d.getGeometry()
		logger.warn("Geometry shape is instead read from "+mesh1d.getGeometryFilename())
		brepfile = mesh1d.getGeometryFilename()
	mesh1d.duplicateEdges()
	mesh1d.updateNodeLabels()
	
	options2d = HashMap()
	options2d.put("size", str(leng))
	options2d.put("deflection", str(defl))
	options2d.put("relativeDeflection", "true")
	options2d.put("isotropic", "true")

	smoothOptions2d = HashMap()
	smoothOptions2d.put("modifiedLaplacian", "true")
	smoothOptions2d.put("refresh", "false")
	smoothOptions2d.put("iterations", "5")
	smoothOptions2d.put("tolerance", "1")
	smoothOptions2d.put("relaxation", "0.6")
	if options.smooth2d:
		for opt in options.smooth2d.split(","):
			keyval = opt.split("=")
			if len(keyval) == 2:
				smoothOptions2d.put(keyval[0], keyval[1])
			else:
				smoothOptions2d.put(keyval[0], "true")

	mtb = MeshTraitsBuilder.getDefault2D()
	
	expl = factory.newExplorer()
	seen = {}
	bads = {}
	iface = 0
	expl.init(shape, CADShapeEnum.FACE)
	while expl.more():
		face = expl.current()
		iface += 1
		if options.explodeBrep:
			face.writeNative("face."+str(iface)+".brep")
		if (options.ifacemin > 0 and iface < options.ifacemin):
			seen[face] = True
		if (options.ifacemax > 0 and iface > options.ifacemax):
			seen[face] = True
		if face not in seen:
			seen[face] = True
	
			mp = MeshParameters(options2d)
			mesh = Mesh2D(mtb, mp, face)
	
			success = True
			try:
				Initial(mesh, mtb, mesh1d).compute()
			except InvalidFaceException:
				logger.severe("Face "+str(iface)+" is invalid. Skipping ...")
				success = False
			except Exception:
				logger.severe("Unexpected error when triangulating face "+str(iface)+". Skipping ...")
				success = False
			if not success:
				bads[iface] = True
				BRepTools.write(face.getShape(), "error.brep")
				logger.severe("Bogus face has been written into error.brep file")
				mesh = Mesh2D(mtb, mp, face)
			else:
				BasicMesh(mesh).compute()
				SmoothNodes2D(mesh, smoothOptions2d).compute()
				ConstraintNormal3D(mesh).compute()
				CheckDelaunay(mesh).compute()

				logger.info("Face "+str(iface)+" has been meshed")
			MeshWriter.writeObject(mesh, outputDir, brepfile, iface)
		expl.next()

# Mesh 3D
if phases[3]:
	if shape is None:
		logger.warn("Phase 3 is invoked without phase 1, argument "+brepfile+" is ignored")
		mesh1d = MMesh1DReader.readObject(outputDir)
		shape = mesh1d.getGeometry()
		brepfile = mesh1d.getGeometryFilename()
		logger.warn("Geometry shape is instead read from "+brepfile)

	expl = factory.newExplorer()
	m2dto3d = MeshToMMesh3DConvert(outputDir, brepfile, shape)
	m2dto3d.exportUNV(unvName != None, unvName)
	
	iface = 0
	expl.init(shape, CADShapeEnum.FACE)
	while expl.more():
		iface += 1
		expl.next()

	m2dto3d.collectBoundaryNodes(array('i', xrange(1, iface+1)))
	m2dto3d.beforeProcessingAllShapes(False)
	iface = 0
	expl.init(shape, CADShapeEnum.FACE)
	while expl.more():
		face = expl.current()
		iface += 1
		m2dto3d.processOneShape(iface, str(iface), iface)
		expl.next()
	m2dto3d.afterProcessingAllShapes()

