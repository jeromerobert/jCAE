import org.jcae.mesh.amibe.ds.Mesh
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.amibe.algos3d.Clip
import org.jcae.mesh.xmldata.MeshReader
import org.jcae.mesh.xmldata.MeshWriter
import org.apache.commons.cli.*;

 cmd=["clip     ", "Clip a mesh by a box"]
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
	OptionBuilder.withArgName("LIST").hasArg()
		.withDescription("coordinates of lower-left corner")
		.withLongOpt("lower")
		.create('l'));
options.addOption(
	OptionBuilder.withArgName("LIST").hasArg()
		.withDescription("coordinates of upper-right corner")
		.withLongOpt("upper")
		.create('u'));
CommandLineParser parser = new GnuParser();
CommandLine cmd = parser.parse(options, args, true);
if (cmd.hasOption('h'))
	usage(0, options);

String [] remaining = cmd.getArgs();
if (remaining.length != 2)
	usage(1, options);

String xmlDir = remaining[0]
String outDir = remaining[1]

HashMap<String, String> algoOptions = new HashMap<String, String>();
if (cmd.hasOption('l'))
	algoOptions.put("lower", cmd.getOptionValue('l'));
if (cmd.hasOption('u'))
	algoOptions.put("upper", cmd.getOptionValue('u'));

MeshTraitsBuilder mtb = new MeshTraitsBuilder();
mtb.addTriangleSet();
Mesh mesh = new Mesh(mtb)
MeshReader.readObject3D(mesh, xmlDir)

Clip algo = new Clip(mesh, algoOptions)
algo.compute()
MeshWriter.writeObject3D(mesh, outDir, null)

