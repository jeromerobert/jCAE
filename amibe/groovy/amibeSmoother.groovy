/**
 * Smooth mesh.
 */
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;
import org.jcae.mesh.amibe.algos3d.*;
import java.io.File;
import java.io.IOException;
import org.apache.commons.cli.*;
import org.jcae.mesh.amibe.validation.*
import org.jcae.mesh.amibe.ds.Triangle;

void usage(int rc, Options options)
{
	HelpFormatter formatter = new HelpFormatter();
	formatter.printHelp("groovy amibeSmoother.groovy [options] inDir outDir", options);
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
		.withDescription("???")
		.withLongOpt("tolerance")
		.create('t'));
options.addOption(
	OptionBuilder.hasArg(false)
		.withDescription("do not try to preserve patch boundaries")
		.withLongOpt("no-boundaries")
		.create('n'));
options.addOption(
	OptionBuilder.hasArg(false)
		.withDescription("update triangle quality within loop")
		.withLongOpt("refresh")
		.create('r'));
CommandLineParser parser = new GnuParser();
CommandLine cmd = parser.parse(options, args, true);
if (cmd.hasOption('h'))
	usage(0, options);

String [] remaining = cmd.getArgs();
if (remaining.length != 2)
	usage(1, options);

String xmlDir = remaining[0]
String outDir = remaining[1]
String xmlFile = "jcae3d";

String sIter=cmd.getOptionValue('i', "5");
String sSize=cmd.getOptionValue('s', "-1.0");
String sTolerance=cmd.getOptionValue('t', "2.0");
String sRefresh=String.valueOf(cmd.hasOption('r'));
boolean bnd=!cmd.hasOption('n');

Mesh mesh = new Mesh();
try
{
	MeshReader.readObject3D(mesh, xmlDir, xmlFile);
}
catch (IOException ex)
{
	println("File "+xmlDir+File.separator+"jcae3d does not exist!");
	usage();
}
HashMap<String, String> decOptions = new HashMap<String, String>();
decOptions.put("size", "1");
//new LengthDecimateHalfEdge(mesh, decOptions).compute();

HashMap<String, String> opts = new HashMap<String, String>();
opts.put("iterations", sIter);
opts.put("boundaries", ""+bnd);
opts.put("size", sSize);
opts.put("tolerance", sTolerance);
opts.put("refresh", sRefresh);
SmoothNodes3D sm = new SmoothNodes3D(mesh, opts)
sm.setProgressBarStatus(10000);
sm.compute();

MeshWriter.writeObject3D(mesh, outDir, "jcae3d", ".", "dummy.brep");

QualityFloat data = new QualityFloat(mesh.getTriangles().size());
data.setQualityProcedure(new MaxLengthFace());
for (Triangle f: mesh.getTriangles())
{
	if (f.isWritable())
		data.compute(f);
}
data.finish();
data.printStatistics();
float lmin = data.getValueByPercent(0.0);
float lmax = data.getValueByPercent(1.0);

float c = 340.0f;
float fmax = 1000.0f * c/lmax/6.0f
println("Max frequency: "+fmax+"Hz ("+lmax+"mm)")
float l2 = data.getValueByPercent(0.98);
float f2 = 1000.0f * c/l2/10.0f
println("Frequency for 2% criterion: "+f2+"Hz ("+l2+"mm)")
