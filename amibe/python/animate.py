
# jCAE
from org.jcae.mesh.oemm import OEMM, Storage
from org.jcae.vtk import Palette, AmibeToMesh, Canvas, UNVToMesh, PickContext, View, Viewable, ViewableCAD, OldViewableMesh, ViewableOEMM

# Swing
from java.awt import BorderLayout, Color
from java.awt.event import KeyEvent, KeyListener
from javax.swing import JFrame, JSlider
from javax.swing.event import ChangeListener

# VTK
from vtk import vtkInteractorStyleTrackballCamera

# Python
import sys, os
import math
from optparse import OptionParser

parser = OptionParser(usage="amibebatch animate [OPTIONS] <dir>\n\nDisplay meshes stored in <dir>0, <dir>1, etc)", prog="animate")
(options, args) = parser.parse_args(args=sys.argv[1:])

if (len(args) != 1):
	parser.print_usage()
	sys.ext(1)

index = 0
xmlDir = args[0]
palette = Palette()
palette.addColor(Color.lightGray)

class MyViewableMesh(OldViewableMesh):
	def __init__(self, mesh):
		OldViewableMesh.__init__(self, mesh, palette)
		self.setSelectionType(Viewable.SelectionType.CELL)
	def manageSelection(self, pickContext):
		self.super__manageSelection(pickContext)
		self.highlight()
		map = pickContext.getMapOfSelectedCells()
		for k in map.keySet():
			list = map.get(k)
			data = k.getDataProvider()
			data.load()
			polys = data.getPolys()
			nodes = data.getNodes()
			data.unLoad()
			for i in list:
				assert polys[4*i] == 3
				print "Triangle nr. %d" % i
				v = []
				for j in [1, 2, 3]:
					pt = polys[4*i+j]
					print "  Vertex nr. %d : %g %g %g" % (pt, nodes[3*pt], nodes[3*pt+1], nodes[3*pt+2])
					v.append([nodes[3*pt], nodes[3*pt+1], nodes[3*pt+2]])
				v[2] = [v[2][i] - v[0][i] for i in range(3)]
				v[1] = [v[1][i] - v[0][i] for i in range(3)]
				v[0] = [v[1][1]*v[2][2] - v[1][2]*v[2][1],v[1][2]*v[2][0] -v[1][0]*v[2][2],v[1][0]*v[2][1] -v[1][1]*v[2][0] ]
				print "  Area: "+str(math.sqrt(v[0][0]*v[0][0] + v[0][1]*v[0][1] + v[0][2]*v[0][2]))

def load(dir):
	if (os.path.isdir(dir) and os.path.exists(os.path.join(dir, "jcae3d"))):
		reader = AmibeToMesh(dir)
		print("Loading "+dir)
		return MyViewableMesh(reader.getMesh())
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
		newViewable = load(self.dir+"0")
		if newViewable:
			self.add(newViewable)
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
		if newViewable:
			self.rawAdd(newViewable)
			if (oldViewable):
				self.remove(oldViewable)
		return index

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
	def __init__(self, view, slider):
		self.view   = view
		self.slider = slider
	def keyTyped(self, e):
		pass
	def keyReleased(self, e):
		code = int(e.getKeyCode())
		if (code == KeyEvent.VK_SHIFT):
			self.view.setMouseMode(View.MouseMode.POINT_SELECTION)
		pass
	def keyPressed(self, e):
		code = int(e.getKeyCode())
		if (code == KeyEvent.VK_ADD or code == KeyEvent.VK_PLUS):
			self.slider.setValue(self.slider.getValue() + 1)
		elif (code == KeyEvent.VK_SUBTRACT or code == KeyEvent.VK_MINUS):
			self.slider.setValue(self.slider.getValue() - 1)
		elif (code == KeyEvent.VK_SHIFT):
			self.view.setMouseMode(View.MouseMode.RECTANGLE_SELECTION)

canvas.addKeyListener(MyKeyListener(canvas, jSlider1))

style = vtkInteractorStyleTrackballCamera()
style.AutoAdjustCameraClippingRangeOn()
canvas.getIren().SetInteractorStyle(style)
canvas.GetRenderer().ResetCamera()

