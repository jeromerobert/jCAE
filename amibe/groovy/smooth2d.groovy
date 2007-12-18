/**
 * Smooth mesh.
 */
import org.jcae.mesh.cad.*
import org.jcae.mesh.amibe.ds.MMesh1D;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.MeshParameters;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.amibe.patch.Mesh2D;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;
import org.jcae.mesh.xmldata.MMesh1DReader;
import org.jcae.mesh.xmldata.MeshToMMesh3DConvert;
import org.jcae.mesh.amibe.algos2d.SmoothNodes2D;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import org.apache.commons.cli.*;
import org.jcae.mesh.amibe.validation.*
import org.jcae.mesh.amibe.ds.Triangle;

// line indented to prevent smooth2d from being listed as an amibebatch command
  cmd=["smooth2d", "Perform vertex smoothing on 2D mesh"]
usage="<inputDir> <outputDir>"

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
options.addOption(
	OptionBuilder.withArgName("NUMBER").hasArg()
		.withDescription("number of iterations (default: 5)")
		.withLongOpt("iterations")
		.create('i'));
options.addOption(
	OptionBuilder.withArgName("FLOAT").hasArg()
		.withDescription("target size")
		.withLongOpt("size")
		.create('s'));
options.addOption(
	OptionBuilder.withArgName("FLOAT").hasArg()
		.withDescription("process only nodes with quality lower than this value")
		.withLongOpt("tolerance")
		.create('t'));
options.addOption(
	OptionBuilder.withArgName("FLOAT").hasArg()
		.withDescription("new position = old + r * (new - old)")
		.withLongOpt("relaxation")
		.create('r'));
options.addOption(
	OptionBuilder.hasArg(false)
		.withDescription("use interpolated metrics for distance computations")
		.withLongOpt("interpolate")
		.create('I'));
options.addOption(
	OptionBuilder.hasArg(false)
		.withDescription("update triangle quality within loop")
		.withLongOpt("refresh")
		.create('R'));
CommandLineParser parser = new GnuParser();
CommandLine cmd = parser.parse(options, args, true);
if (cmd.hasOption('h'))
	usage(0, options);

String [] remaining = cmd.getArgs();
if (remaining.length != 2)
	usage(1, options);

String xmlDir = remaining[0]
String outputDir = remaining[1]

String sIter=cmd.getOptionValue('i', "5");
String sSize=cmd.getOptionValue('s', "-1.0");
String sTolerance=cmd.getOptionValue('t', "2.0");
String sRelaxation=cmd.getOptionValue('r', "0.6");
String sInterpolate=String.valueOf(cmd.hasOption('I'));
String sRefresh=String.valueOf(cmd.hasOption('R'));

CADShapeFactory factory = CADShapeFactory.getFactory()

MMesh1D mesh1d = MMesh1DReader.readObject(xmlDir);
CADShape shape = mesh1d.getGeometry();
String brepfile = mesh1d.getGeometryFilename();

mesh1d.duplicateEdges()
mesh1d.updateNodeLabels()

HashMap<String, String> options2d = new HashMap<String, String>();
options2d.put("isotropic", "true");

HashMap<String, String> smoothOptions2d = new HashMap<String, String>();
smoothOptions2d.put("iterations", "5");
smoothOptions2d.put("tolerance", "1");
smoothOptions2d.put("modifiedLaplacian", "true");
smoothOptions2d.put("refresh", "false");
smoothOptions2d.put("relaxation", "0.6");

MeshTraitsBuilder mtb = MeshTraitsBuilder.getDefault2D()
CADExplorer expl = factory.newExplorer()

seen = []
iface = 0

for (expl.init(shape, CADShapeEnum.FACE); expl.more(); expl.next()) {
	face = expl.current()
	iface ++
	if (! (face in seen)) {
		seen << face

		MeshParameters mp = new MeshParameters(options2d);
		Mesh2D mesh = new Mesh2D(mtb, mp, face);
		MeshReader.readObject(mesh, xmlDir, iface)
		new SmoothNodes2D(mesh, smoothOptions2d).compute()
		MeshWriter.writeObject(mesh, outputDir, brepfile, iface)

		// Copy geometry file
		FileInputStream is = new FileInputStream(xmlDir+File.separator+brepfile);
		FileChannel iChannel = is.getChannel();
		FileOutputStream os = new FileOutputStream(new File(outputDir, brepfile), false);
		FileChannel oChannel = os.getChannel();
		oChannel.transferFrom(iChannel, 0, iChannel.size());
		if (is != null)
			is.close();
		if (os != null)
			os.close();
	}
}

expl = factory.newExplorer()
m2dto3d = new MeshToMMesh3DConvert(outputDir, brepfile)

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

