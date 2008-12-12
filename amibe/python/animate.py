
# jCAE
from org.jcae.mesh.oemm import OEMM, Storage
from org.jcae.vtk import ColorManager, AmibeToMesh, Canvas, UNVToMesh, View, Viewable, ViewableCAD, ViewableMesh, ViewableOEMM

# Swing
from java.awt import BorderLayout, Color
from java.awt.event import KeyEvent, KeyListener
from javax.swing import JFrame

# VTK
from vtk import vtkRenderer, vtkInteractorStyleTrackballCamera

# Python
import sys, os
from optparse import OptionParser

parser = OptionParser(usage="amibebatch animate [OPTIONS] <dir>\n\nDisplay meshes stored in <dir>0, <dir>1, etc)", prog="animate")
(options, args) = parser.parse_args(args=sys.argv[1:])

if (len(args) != 1):
	parser.print_usage()
	sys.ext(1)

index = 0
xmlDir = args[0]

class MyView(View):
	def rawAdd(self, viewable):
		self.viewables.add(viewable)
		viewable.addCanvas(self)
		self.setCurrentViewable(viewable)

class MyColorManager(ColorManager):
	color = Color.BLACK
	def __init__(self, color):
		self.color = color
	def setColor(self, color):
		pass
	def getColor(self):
		return self.color

frame = JFrame("jCAE Demo")
frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
canvas = MyView()
frame.add(canvas, BorderLayout.CENTER)
renderer = canvas.GetRenderer()

def load(dir):
	if (os.path.isdir(dir) and os.path.exists(os.path.join(dir, "jcae3d"))):
		reader = AmibeToMesh(dir)
		print("Loading "+dir)
		return ViewableMesh(reader.getMesh(), MyColorManager(Color.BLUE))
	return Node

frame.setSize(800,600)
frame.setVisible(True)

class MyKeyListener(KeyListener):
	def __init__(self, view, dir):
		self.view = view
		self.dir = dir
		self.index = 0
		self.viewable = load(self.dir+str(self.index))
		self.view.add(self.viewable)
	def keyTyped(self, e):
		pass
	def keyReleased(self, e):
		pass
	def keyPressed(self, e):
		code = int(e.getKeyCode())
		newIndex = self.index
		if (code == KeyEvent.VK_ADD or code == KeyEvent.VK_PLUS):
			newIndex = self.index + 1
		elif (code == KeyEvent.VK_SUBTRACT or code == KeyEvent.VK_MINUS):
			newIndex = self.index - 1
			if newIndex < 0:
				newIndex = 0
		if newIndex != self.index:
			newViewable = load(self.dir+str(newIndex))
			self.view.rawAdd(newViewable)
			self.view.remove(self.viewable)
			self.index = newIndex
			self.viewable = newViewable

canvas.addKeyListener(MyKeyListener(canvas, xmlDir))

style = vtkInteractorStyleTrackballCamera()
style.AutoAdjustCameraClippingRangeOn()
canvas.getIren().SetInteractorStyle(style)
renderer.ResetCamera()

