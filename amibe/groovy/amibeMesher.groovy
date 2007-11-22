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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import org.apache.commons.cli.*;

void usage(int rc, Options options)
{
	HelpFormatter formatter = new HelpFormatter();
	formatter.printHelp("groovy amibeMesher.groovy [options] cadFile outputDir edgeLength deflection", options);
	System.exit(rc);
}

Options options = new Options();
options.addOption(
	OptionBuilder.hasArg(false)
		.withDescription("usage information")
		.withLongOpt("help")
		.create('h'));
/* These options are for experts only!
options.addOption(
	OptionBuilder.withArgName("NUMBER").hasArg()
		.withDescription("tries to eliminate edges smaller than this value (default: patch size/1000)")
		.withLongOpt("epsilon")
		.create('e'));
options.addOption(
	OptionBuilder.hasArg(false)
		.withDescription("tries to merge small edges together")
		.withLongOpt("cumulativeEpsilon")
		.create('E'));
*/
options.addOption(
	OptionBuilder.withArgName("FILE").hasArg()
		.withDescription("exports UNV file (for phase 3)")
		.withLongOpt("output")
		.create('o'));
options.addOption(
	OptionBuilder.withArgName("LIST").hasArg()
		.withDescription("comma separated list of phases (Default: 1,2,3)")
		.withLongOpt("phase")
		.create('p'));
options.addOption(
	OptionBuilder.hasArg(false)
		.withDescription("writes face.<i>.brep file before meshing each patch")
		.withLongOpt("explodeBrep")
		.create('x'));

CommandLineParser parser = new GnuParser();
CommandLine cmd = parser.parse(options, args, true);
if (cmd.hasOption('h'))
	usage(0, options);

String [] remaining = cmd.getArgs();
if (remaining.length != 4)
	usage(1, options);

String brepfile = remaining[0]
String outdir = remaining[1]
leng = Double.parseDouble(remaining[2])
defl = Double.parseDouble(remaining[3])

String brepdir = ".";
if (brepfile.indexOf((int) File.separatorChar) >= 0)
{
	int idx = brepfile.lastIndexOf((int) File.separatorChar);
	brepdir = brepfile.substring(0, idx);
	brepfile = brepfile.substring(idx+1);
}
String file = brepdir+File.separator+brepfile

String [] sPhases=cmd.getOptionValue('p', "1,2,3").split(",");
boolean [] phases=new boolean[4]
for (String str: sPhases)
{
	int p = Integer.parseInt(str)
	if (p < 1 || p > 3)
		usage(1, options);
	phases[p] = true
}

//String sEpsilon = cmd.getOptionValue('e', "-1.0");
System.setProperty("org.jcae.mesh.amibe.ds.Mesh.epsilon", "0.0");
System.setProperty("org.jcae.mesh.amibe.ds.Mesh.cumulativeEpsilon", "false");

String unvName = null
if (cmd.hasOption('o'))
	unvName = cmd.getOptionValue('o');

// Mesh 1D
// This method takes as
//    Input : shape (the shape to be meshed)
//    Output: ...

CADShapeFactory factory = CADShapeFactory.getFactory()

if (phases[1])
{
	shape = factory.newShape(file)
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
	if(cmd.hasOption('p'))
	{
		// --phase option has been specified, it is likely that
		// this script will be run for different phases.
		// Copy CAD file into outdir so that this file will be
		// found by next runs
		FileInputStream is = null;
		FileOutputStream os = null;
		try {
			is = new FileInputStream(brepfile);
			FileChannel iChannel = is.getChannel();
			os = new FileOutputStream(new File(outdir, brepfile), false);
			FileChannel oChannel = os.getChannel();
			oChannel.transferFrom(iChannel, 0, iChannel.size());
		}
		finally {
			if (is != null) is.close();
			if (os != null) os.close();
		}
	}
}
else
{
	mesh1d = MMesh1DReader.readObject(outdir, "jcae1d");
	shape = mesh1d.getGeometry();
	mesh1d.setMaxLength(leng);
}

// Mesh 2D
if (phases[2])
{
	
	mesh1d.duplicateEdges()
	mesh1d.updateNodeLabels()
	
	Metric2D.setLength(leng)
	MeshTraitsBuilder mtb = MeshTraitsBuilder.getDefault2D()
	
	expl = factory.newExplorer()
	seen = []
	bads = []
	iface = 0
	for (expl.init(shape, CADShapeEnum.FACE); expl.more(); expl.next()) {
		face = expl.current()
		iface ++
		if(cmd.hasOption('x'))
			face.writeNative("face."+iface+".brep");
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
					new Initial(mesh, mesh1d.boundaryNodes(mesh)).compute();
					go = false
				}
				catch(InitialTriangulationException ex) {
					mesh = new Mesh2D(mtb, face)
					mesh.scaleTolerance(10.0)
					println "Scaling tolerance for face #${iface}"
					ntry ++
				}
				catch(InvalidFaceException ex) {
					println "Face #${iface} is invalid. Skipping ..."
					success = false
				}
				catch(Exception ex) {
					ex.printStackTrace()
					println "Unexpected error when triangulating face #${iface}. Skipping ..."
					success = false
				}
				if (ntry == 20) {
					println "Cannot triangulate face #${iface}. Skipping ..."
					success = false
				}
				if (! success) go = false
			}
	
			if (! success) {
				bads << iface
				BRepTools.write(face.getShape(), "error.brep")
				mesh = new Mesh2D(mtb, face); 
			} else {
				new BasicMesh(mesh).compute()
				new CheckDelaunay(mesh).compute()
				println "Face #${iface} has been meshed"
			}
			MeshWriter.writeObject(mesh, outdir, "jcae2d."+iface, brepdir, brepfile, iface)
		}
	}
}

// Mesh 3D
if (phases[3])
{
	expl = factory.newExplorer()
	m2dto3d = new MeshToMMesh3DConvert(outdir, '.', brepfile)
	m2dto3d.exportUNV(unvName != null, unvName);
	
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
}

