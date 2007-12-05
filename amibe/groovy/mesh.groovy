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
import org.apache.log4j.Logger;
import org.apache.commons.cli.*;

cmd=["mesh    ", "Mesh a CAD file"]
usage="<cadFile> <outputDir> <edgeLength> <deflection>"

void usage(int rc, Options options)
{
	HelpFormatter formatter = new HelpFormatter();
	formatter.printHelp("amibebatch "+cmd[0].trim()+" [OPTIONS] "+usage, cmd[1], options, "");
	System.exit(rc);
}

Options options = new Options();
options.addOption(
	OptionBuilder.hasArg(false)
		.withDescription("usage information")
		.withLongOpt("help")
		.create('h'));
/* This options is for experts only!
options.addOption(
	OptionBuilder.withArgName("NUMBER").hasArg()
		.withDescription("tries to eliminate edges smaller than this value (default: patch size/1000)")
		.withLongOpt("epsilon")
		.create('e'));
*/
/* This options is for experts only!
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
options.addOption(
	OptionBuilder.withArgName("NUMBER").hasArg()
		.withDescription("start meshing from this patch number")
		.withLongOpt("from-face")
		.create('f'));
options.addOption(
	OptionBuilder.withArgName("NUMBER").hasArg()
		.withDescription("mesh up to this patch number")
		.withLongOpt("to-face")
		.create('t'));
options.addOption(
	OptionBuilder.withArgName("STRING").hasArg()
		.withDescription("comma separated list of options passed to 2D smoother (available options are: iterations=N,relaxation=N,tolerance=N; run smooth2d -h for details)")
		.withLongOpt("smooth2d")
		.create('s'));

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

Logger logger=Logger.getRootLogger()
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

/*  Uncomment if -e flag is enabled */
// System.setProperty("org.jcae.mesh.amibe.ds.Mesh.epsilon", cmd.getOptionValue('e', "-1.0"));

/*  Uncomment if -E flag is enabled */
// System.setProperty("org.jcae.mesh.amibe.ds.Mesh.cumulativeEpsilon", cmd.getOptionValue('E', "false"));

String unvName = null
if (cmd.hasOption('o'))
	unvName = cmd.getOptionValue('o');

File xmlDirF=new File(outputDir);
xmlDirF.mkdirs();
if(!xmlDirF.exists() || !xmlDirF.isDirectory())
{
	logger.error("Cannot write to ${outputDir}");
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
	mesh1d = new MMesh1D(outputDir+File.separator+brepfile)
	shape = mesh1d.getGeometry();
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
	logger.info("Edges discretized")
}

// Mesh 2D
if (phases[2])
{
	if (mesh1d == null)
	{
		logger.warn("Phase 2 is invoked without phase 1, argument ${brepfile} is ignored")
		mesh1d = MMesh1DReader.readObject(outputDir);
		shape = mesh1d.getGeometry();
		logger.warn("Geometry shape is instead read from "+mesh1d.getGeometryFilename())
		brepfile = new File(mesh1d.getGeometryFilename()).getName()
	}
	mesh1d.duplicateEdges()
	mesh1d.updateNodeLabels()
	
	HashMap<String, String> options2d = new HashMap<String, String>();
	options2d.put("size", ""+leng);
	options2d.put("deflection", ""+defl);
	options2d.put("relativeDeflection", "true");
	options2d.put("isotropic", "true");

	HashMap<String, String> smoothOptions2d = new HashMap<String, String>();
	smoothOptions2d.put("modifiedLaplacian", "true");
	smoothOptions2d.put("refresh", "false");
	smoothOptions2d.put("iterations", "5");
	smoothOptions2d.put("tolerance", "1");
	smoothOptions2d.put("relaxation", "0.6");
	if (cmd.hasOption('s'))
	{
		for (String opt: cmd.getOptionValue('s').split(","))
		{
			String [] keyval = opt.split("=");
			if (keyval.length == 2)
				smoothOptions2d.put(keyval[0], keyval[1]);
			else
				smoothOptions2d.put(keyval[0], "true");
		}
	}

	MeshTraitsBuilder mtb = MeshTraitsBuilder.getDefault2D()
	
	int ifacemin = Integer.parseInt(cmd.getOptionValue('f', "0"))
	int ifacemax = Integer.parseInt(cmd.getOptionValue('t', "0"))

	expl = factory.newExplorer()
	seen = []
	bads = []
	iface = 0
	for (expl.init(shape, CADShapeEnum.FACE); expl.more(); expl.next()) {
		face = expl.current()
		iface ++
		if(cmd.hasOption('x'))
			face.writeNative("face."+iface+".brep");
		if (ifacemin > 0 && iface < ifacemin)
			seen << face
		if (ifacemax > 0 && iface > ifacemax)
			seen << face
		if (! (face in seen)) {
			seen << face
	
			MeshParameters mp = new MeshParameters(options2d);
			Mesh2D mesh = new Mesh2D(mtb, mp, face);
	
			success = true
			try {
				new Initial(mesh, mtb, mesh1d).compute();
			}
			catch(InvalidFaceException ex) {
				logger.error("Face #${iface} is invalid. Skipping ...")
				success = false
			}
			catch(Exception ex) {
				ex.printStackTrace()
				logger.error("Unexpected error when triangulating face #${iface}. Skipping ...")
				success = false
			}
			if (! success) {
				bads << iface
				BRepTools.write(face.getShape(), "error.brep")
				logger.error("Bogus face has been written into error.brep file")
				mesh = new Mesh2D(mtb, mp, face); 
			} else {
				new BasicMesh(mesh).compute()
				new CheckDelaunay(mesh).compute()

				new SmoothNodes2D(mesh, smoothOptions2d).compute()
				logger.info("Face #${iface} has been meshed")
			}
			MeshWriter.writeObject(mesh, outputDir, brepfile, iface)
		}
	}
}

// Mesh 3D
if (phases[3])
{
	if (shape == null)
	{
		logger.warn("Phase 3 is invoked without phase 1, argument ${brepfile} is ignored")
		mesh1d = MMesh1DReader.readObject(outputDir);
		shape = mesh1d.getGeometry();
		logger.warn("Geometry shape is instead read from "+mesh1d.getGeometryFilename())
		brepfile = new File(mesh1d.getGeometryFilename()).getName()
	}
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

