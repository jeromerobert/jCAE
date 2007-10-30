/**
 * Sample class to show how to call amibe algorithms.
 */
import org.jcae.mesh.cad.*
import org.jcae.mesh.amibe.*
import org.jcae.mesh.amibe.algos2d.*
import org.jcae.mesh.amibe.metrics.*
import org.jcae.mesh.amibe.patch.Mesh2D
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder
import org.jcae.mesh.amibe.ds.MMesh1D
import org.jcae.mesh.amibe.algos1d.*
import org.jcae.mesh.xmldata.*

if (args.length < 2 || args.length > 4) {
	println "Usage: amibeMesher.groovy filename outputDir edgeLength deflection"
	System.exit(1)
}

String brepfile = args[0]
String outdir = args[1]
leng = Double.parseDouble(args[2])
defl = Double.parseDouble(args[3])

String brepdir = ".";
if (brepfile.indexOf((int) java.io.File.separatorChar) >= 0)
{
	int idx = brepfile.lastIndexOf((int)java.io.File.separatorChar);
	brepdir = brepfile.substring(0, idx);
	brepfile = brepfile.substring(idx+1);
}
String file = brepdir+java.io.File.separator+brepfile

CADShapeFactory factory = CADShapeFactory.getFactory()
shape = factory.newShape(file)

// Mesh 1D
// This method takes as
//    Input : shape (the shape to be meshed)
//    Output: ...

mesh1d = new MMesh1D(shape)
mesh1d.setMaxLength(leng)

if (defl <= 0.0) {
	new UniformLength(mesh1d).compute()
} else {
	mesh1d.setMaxDeflection(defl)
	new UniformLengthDeflection(mesh1d).compute(true)
	new Compat1D2D(mesh1d).compute(true)
}

MMesh1DWriter.writeObject(mesh1d, outdir, "jcae1d", brepdir, brepfile)

// Mesh 2D

Metric2D.setLength(leng)

mesh1d.duplicateEdges()
mesh1d.updateNodeLabels()

MeshTraitsBuilder mtb = MeshTraitsBuilder.getDefault2D()

expl = factory.newExplorer()
seen = []
bads = []
iface = 0
for (expl.init(shape, CADShapeEnum.FACE); expl.more(); expl.next()) {
	face = expl.current()
	iface ++
	if (! (face in seen)) {
		seen << face
		ntry = 0

		Metric3D.setLength(leng)
		Metric3D.setDeflection(defl)
		Metric3D.setRelativeDeflection(true)
		Metric3D.setIsotropic(true)
		Mesh2D mesh = new Mesh2D(mtb, face)

		go = true
		success = true
		while (go) {
			try {
				new BasicMesh(mesh, mesh1d).compute()
				new CheckDelaunay(mesh).compute()
				mesh.removeDegeneratedEdges()
				MeshWriter.writeObject(mesh, outdir, "jcae2d."+iface, brepdir, brepfile, iface)
				go = false
			}
			catch(InitialTriangulationException ex) {
				mesh = new Mesh2D(mtb, face)
				mesh.scaleTolerance(10.0)
				println "Scaling tolerance for face #${iface}"
				ntry ++
			}
			catch(InvalidFaceException ex) {
				println "Face #${iface} is invalid"
				success = false
			}
			catch(Exception ex) {
				ex.printStackTrace()
				success = false
			}
			if (ntry == 20) success = false
			if (! success) go = false
		}

		if (! success) {
			println "Cannot triangulate face #${iface}. Skipping ..."
			bads << iface
			BRepTools.write(face.getShape(), "error.brep")
			exit
		} else {
			println "Face #${iface} has been meshed"
		}
	}
}

// Mesh 3D

expl = factory.newExplorer()
m2dto3d = new MeshToMMesh3DConvert(outdir)
m2dto3d.exportUNV(true, outdir+"/res.unv")

iface = 0
for (expl.init(shape, CADShapeEnum.FACE); expl.more(); expl.next()) {
	iface ++
	m2dto3d.computeRefs("jcae2d."+iface)
}
m2dto3d.initialize("jcae3d", false)
iface = 0
for (expl.init(shape, CADShapeEnum.FACE); expl.more(); expl.next()) {
	face = expl.current()
	iface ++
	m2dto3d.convert("jcae2d."+iface, iface, face)
}
m2dto3d.finish()

