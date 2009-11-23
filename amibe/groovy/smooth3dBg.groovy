/**
 * Smooth mesh.
 */
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;
import org.jcae.mesh.amibe.algos3d.SmoothNodes3DBg;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import java.io.File;
import java.io.IOException;
import org.apache.commons.cli.*;

cmd=["smooth3dBg", "Perform vertex smoothing on 3D mesh"]
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
		.withDescription("show this help message and exit")
		.withLongOpt("help")
		.create('h'));
options.addOption(
	OptionBuilder.withArgName("FLOAT").hasArg()
		.withDescription("dot product of face normals to detect feature edges")
		.withLongOpt("coplanarity")
		.create('c'));
options.addOption(
	OptionBuilder.withArgName("NUMBER").hasArg()
		.withDescription("number of iterations (default: 1)")
		.withLongOpt("iterations")
		.create('i'));
options.addOption(
	OptionBuilder.hasArg(false)
		.withDescription("do not try to preserve patch boundaries")
		.withLongOpt("no-boundaries")
		.create('N'));
options.addOption(
	OptionBuilder.withArgName("FLOAT").hasArg()
		.withDescription("new position = old + r * (new - old)")
		.withLongOpt("relaxation")
		.create('r'));
options.addOption(
	OptionBuilder.hasArg(false)
		.withDescription("update triangle quality within loop")
		.withLongOpt("refresh")
		.create('R'));
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
CommandLineParser parser = new GnuParser();
CommandLine cmd = parser.parse(options, args, true);
if (cmd.hasOption('h'))
	usage(0, options);

String [] remaining = cmd.getArgs();
if (remaining.length != 2)
	usage(1, options);

String xmlDir = remaining[0]
String outDir = remaining[1]

String sIter=cmd.getOptionValue('i', "1");
String sSize=cmd.getOptionValue('s', "-1.0");
String sTolerance=cmd.getOptionValue('t', "2.0");
String sRelaxation=cmd.getOptionValue('r', "0.6");
String sRefresh=String.valueOf(cmd.hasOption('R'));
boolean bnd=!cmd.hasOption('N');

MeshTraitsBuilder mtb = MeshTraitsBuilder.getDefault3D()
mtb.addNodeList()
Mesh mesh = new Mesh(mtb);
try
{
	MeshReader.readObject3D(mesh, xmlDir);
}
catch (IOException ex)
{
	println("File "+xmlDir+File.separator+"jcae3d does not exist!");
	usage(1, options);
}
//HashMap<String, String> decOptions = new HashMap<String, String>();
//decOptions.put("size", "1");
//new LengthDecimateHalfEdge(mesh, decOptions).compute();

HashMap<String, String> opts = new HashMap<String, String>();
opts.put("iterations", sIter);
opts.put("boundaries", ""+bnd);
opts.put("size", sSize);
opts.put("tolerance", sTolerance);
opts.put("relaxation", sRelaxation);
opts.put("refresh", sRefresh);
if (cmd.hasOption('c'))
	opts.put("coplanarity", cmd.getOptionValue('c'));

SmoothNodes3DBg sm = new SmoothNodes3DBg(mesh, opts)
sm.setProgressBarStatus(10000);
sm.compute();

MeshWriter.writeObject3D(sm.getOutputMesh(), outDir, null);

