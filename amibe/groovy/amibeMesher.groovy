/**
 * Sample class to show how to call amibe algorithms.
 */
import org.jcae.mesh.cad.*
import org.jcae.mesh.amibe.*
import org.jcae.mesh.amibe.algos2d.*
import org.jcae.mesh.amibe.patch.Mesh2D
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder
import org.jcae.mesh.amibe.ds.MMesh1D
import org.jcae.mesh.amibe.ds.MeshParameters
import org.jcae.mesh.amibe.algos1d.*
import org.jcae.mesh.xmldata.*
import org.jcae.opencascade.jni.BRepTools
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
String outputDir = remaining[1]
leng = Double.parseDouble(remaining[2])
defl = Double.parseDouble(remaining[3])

String brepdir = ".";
if (brepfile.indexOf((int) File.separatorChar) >= 0)
{
	int idx = brepfile.lastIndexOf((int) File.separatorChar);
	brepdir = brepfile.substring(0, idx);
	brepfile = brepfile.substring(idx+1);
}

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
double epsilon = 0.0;
boolean accumulateEpsilon = false;

String unvName = null
if (cmd.hasOption('o'))
	unvName = cmd.getOptionValue('o');

File xmlDirF=new File(outputDir);
xmlDirF.mkdirs();
if(!xmlDirF.exists() || !xmlDirF.isDirectory())
{
	println("Cannot write to "+outputDir);
	System.exit(1);
}

// Mesh 1D
// This method takes as
//    Input : shape (the shape to be meshed)
//    Output: ...

CADShapeFactory factory = CADShapeFactory.getFactory()

MMesh1D mesh1d = null
CADShape shape = null;
if (phases[1])
{
	if (brepfile.endsWith(".step") || brepfile.endsWith(".stp") || brepfile.endsWith(".igs"))
	{
		shape = CADShapeFactory.getFactory().newShape(brepdir+File.separator+brepfile);
		brepfile = brepfile.substring(0, brepfile.lastIndexOf('.')) + ".tmp.brep";
		shape.writeNative(outputDir+File.separator+brepfile);
	}
	else if (!brepdir.equals(outputDir))
	{
		FileInputStream is = null;
		FileOutputStream os = null;
		try {
			is = new FileInputStream(brepdir+File.separator+brepfile);
			FileChannel iChannel = is.getChannel();
			os = new FileOutputStream(new File(outputDir, brepfile), false);
			FileChannel oChannel = os.getChannel();
			oChannel.transferFrom(iChannel, 0, iChannel.size());
		}
		finally {
			if (is != null)
				is.close();
			if (os != null)
				os.close();
		}
	}
	if (shape == null)
		shape = factory.newShape(outputDir+File.separator+brepfile)
	mesh1d = new MMesh1D(shape)
	HashMap<String, String> options1d = new HashMap<String, String>();
	options1d.put("size", ""+leng);
	if (defl <= 0.0) {
		new UniformLength(mesh1d, options1d).compute()
	} else {
		options1d.put("deflection", ""+defl);
		options1d.put("relativeDeflection", "true");
		new UniformLengthDeflection(mesh1d, options1d).compute()
		new Compat1D2D(mesh1d, options1d).compute()
	}
	
	MMesh1DWriter.writeObject(mesh1d, outputDir, brepfile)
}

// Mesh 2D
if (phases[2])
{
	if (mesh1d == null)
	{
		mesh1d = MMesh1DReader.readObject(outputDir);
		shape = mesh1d.getGeometry();
	}
	mesh1d.duplicateEdges()
	mesh1d.updateNodeLabels()
	
	HashMap<String, String> options2d = new HashMap<String, String>();
	options2d.put("size", ""+leng);
	options2d.put("deflection", ""+defl);
	options2d.put("relativeDeflection", "true");
	options2d.put("isotropic", "true");

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
	
			MeshParameters mp = new MeshParameters(options2d);
			Mesh2D mesh = new Mesh2D(mtb, mp, face);
	
			success = true
			try {
				new Initial(mesh, mtb, mesh1d).compute();
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
			if (! success) {
				bads << iface
				BRepTools.write(face.getShape(), "error.brep")
				mesh = new Mesh2D(mtb, mp, face); 
			} else {
				new BasicMesh(mesh).compute()
				new CheckDelaunay(mesh).compute()

				HashMap<String, String> smoothOptions2d = new HashMap<String, String>();
				smoothOptions2d.put("iterations", "5");
				smoothOptions2d.put("tolerance", "1");
				smoothOptions2d.put("modifiedLaplacian", "true");
				smoothOptions2d.put("refresh", "false");
				smoothOptions2d.put("relaxation", "0.6");
				new SmoothNodes2D(mesh, smoothOptions2d).compute()
				println "Face #${iface} has been meshed"
			}
			MeshWriter.writeObject(mesh, outputDir, brepfile, iface)
		}
	}
}

// Mesh 3D
if (phases[3])
{
	if (shape == null)
		shape = factory.newShape(outputDir+File.separator+brepfile)
	expl = factory.newExplorer()
	m2dto3d = new MeshToMMesh3DConvert(outputDir, brepfile)
	m2dto3d.exportUNV(unvName != null, unvName);
	
	iface = 0
	for (expl.init(shape, CADShapeEnum.FACE); expl.more(); expl.next()) {
		iface ++
		m2dto3d.computeRefs(iface)
	}
	m2dto3d.initialize(false)
	iface = 0
	for (expl.init(shape, CADShapeEnum.FACE); expl.more(); expl.next()) {
		face = expl.current()
		iface ++
		m2dto3d.convert(iface, face)
	}
	m2dto3d.finish()
}

