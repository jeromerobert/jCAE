
# jCAE
from org.jcae.mesh.oemm import OEMM, Storage
from org.jcae.vtk import ColorManager, AmibeToMesh, Canvas, UNVToMesh, View, Viewable, ViewableCAD, ViewableMesh, ViewableOEMM

# Swing
from java.awt import BorderLayout, Color
from java.awt.event import KeyEvent, KeyListener
from javax.swing import JFrame, JSlider
from javax.swing.event import ChangeListener

# VTK
from vtk import vtkInteractorStyleTrackballCamera

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

def load(dir):
	if (os.path.isdir(dir) and os.path.exists(os.path.join(dir, "jcae3d"))):
		reader = AmibeToMesh(dir)
		print("Loading "+dir)
		return ViewableMesh(reader.getMesh(), MyColorManager(Color.BLUE))
	return None

class MyView(View):
	def __init__(self, dir):
		self.dir = dir
		i = -1
		while True:
			i += 1
			if not(os.path.isdir(dir+str(i)) and os.path.exists(os.path.join(dir+str(i), "jcae3d"))):
				break
		self.max = i
		self.add(load(self.dir+"0"))
	def getMaxIndex(self):
		return self.max
	def rawAdd(self, viewable):
		self.viewables.add(viewable)
		viewable.addCanvas(self)
		self.setCurrentViewable(viewable)
	def update(self, index):
		if index > self.max:
			index = self.max
		if index < 0:
			index = 0
		oldViewable = self.getCurrentViewable()
		newViewable = load(self.dir+str(index))
		if (newViewable):
			self.rawAdd(newViewable)
			if (oldViewable):
				self.remove(oldViewable)
		return index

class MyColorManager(ColorManager):
	color = Color.BLACK
	def __init__(self, color):
		self.color = color
	def setColor(self, color):
		pass
	def getColor(self):
		return self.color

class SliderListener(ChangeListener):
	def __init__(self, view):
		self.view = view
	def stateChanged(self, evt):
		s = evt.getSource()
		self.view.update(s.getValue())

frame = JFrame("Mesh taken from "+xmlDir+"* directories")
frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
canvas = MyView(xmlDir)
frame.add(canvas, BorderLayout.CENTER)

jSlider1 = JSlider()
jSlider1.setValue(0)
jSlider1.setMaximum(canvas.getMaxIndex())
jSlider1.setPaintLabels(True)
jSlider1.addChangeListener(SliderListener(canvas))
frame.add(jSlider1, BorderLayout.SOUTH)

frame.setSize(800,600)
frame.setVisible(True)

class MyKeyListener(KeyListener):
	def __init__(self, slider):
		self.slider = slider
	def keyTyped(self, e):
		pass
	def keyReleased(self, e):
		pass
	def keyPressed(self, e):
		code = int(e.getKeyCode())
		oldIndex = self.slider.getValue()
		newIndex = oldIndex
		if (code == KeyEvent.VK_ADD or code == KeyEvent.VK_PLUS):
			newIndex = oldIndex + 1
		elif (code == KeyEvent.VK_SUBTRACT or code == KeyEvent.VK_MINUS):
			newIndex = oldIndex - 1
		if newIndex != oldIndex:
			self.slider.setValue(newIndex)

canvas.addKeyListener(MyKeyListener(jSlider1))

style = vtkInteractorStyleTrackballCamera()
style.AutoAdjustCameraClippingRangeOn()
canvas.getIren().SetInteractorStyle(style)
canvas.GetRenderer().ResetCamera()

