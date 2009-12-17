
# jCAE
from org.jcae.mesh.oemm import OEMM, Storage
from org.jcae.vtk import AmibeToMesh, Canvas, UNVToMesh, View, Viewable, ViewableCAD, ViewableMesh, ViewableOEMM, Utils

# Swing
from java.awt import BorderLayout
from java.awt.event import KeyEvent, KeyListener
from javax.swing import JFrame

# VTK
from vtk import vtkRenderer, vtkInteractorStyleTrackballCamera

# Python
import sys, os
from optparse import OptionParser

parser = OptionParser(usage="amibebatch view [OPTIONS] <dir|file>\n\nDisplay CAD (brep, stp or iges files) or mesh (Amibe or UNV format)", prog="view")
parser.add_option("-b", "--batch", metavar="<PNG file name>", action="store",
	type="string", dest="batch", help="Take a screenshot and exit")
(options, args) = parser.parse_args(args=sys.argv[1:])

if (len(args) != 1):
	parser.print_usage()
	sys.exit(1)

xmlDir = args[0]

frame = JFrame("jCAE Demo")
frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
canvas = View()
frame.add(canvas, BorderLayout.CENTER)

if (os.path.isdir(xmlDir)):
	if (os.path.exists(os.path.join(xmlDir, "jcae3d"))):
		reader = AmibeToMesh(xmlDir)
		viewable = ViewableMesh(reader.getTriangles())
	elif (os.path.exists(os.path.join(xmlDir, "oemm"))):
		oemm = Storage.readOEMMStructure(xmlDir)
		viewable = ViewableOEMM(oemm)
	else:
		raise IOError, "Cannot find file"
elif (xmlDir.endswith(".unv")):
	viewable = ViewableMesh(UNVToMesh(xmlDir, Collections.EMPTY_LIST).getMesh())
else:
	viewable = ViewableCAD(xmlDir)
canvas.add(viewable)
frame.setSize(800,600)

class MyKeyListener(KeyListener):
	def __init__(self, view):
		self.view = view
	def keyTyped(self, e):
		pass
	def keyReleased(self, e):
		pass
	def keyPressed(self, e):
		code = int(e.getKeyCode())
		if (code == KeyEvent.VK_F):
			self.view.getCurrentViewable().setSelectionType(Viewable.SelectionType.CELL)
		elif (code == KeyEvent.VK_V):
			self.view.getCurrentViewable().setSelectionType(Viewable.SelectionType.POINT)
			self.view.lock()
			print("Capabilities: " +self.view.GetRenderWindow().ReportCapabilities())
			self.view.unlock()
		elif (code == KeyEvent.VK_E):
			nbrActor = self.view.GetRenderer().GetNumberOfPropsRendered()
			print("Number of actors rendered : " + str(nbrActor))
			print("Number of actors : " + str(self.view.GetRenderer().GetViewProps().GetNumberOfItems()))
		elif code == KeyEvent.VK_P:
			print self.view.GetRenderer().GetActiveCamera()

canvas.addKeyListener(MyKeyListener(canvas))

style = vtkInteractorStyleTrackballCamera()
style.AutoAdjustCameraClippingRangeOn()
canvas.getIren().SetInteractorStyle(style)

frame.setVisible(True)
canvas.cameraManager.fitAll()

if options.batch:
	Utils.takeScreenshot(canvas, options.batch)
	sys.exit(0)

