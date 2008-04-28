/**
 * SwapEdge
 */
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;
import org.jcae.mesh.amibe.algos3d.SwapEdge;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import java.io.File;
import java.io.IOException;
import org.apache.commons.cli.*;

cmd=["swap     ", "Swap edges to improve mesh quality"]
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
	OptionBuilder.withArgName("FLOAT").hasArg()
		.withDescription("Planar angle")
		.withLongOpt("angle")
		.create('a'));
CommandLineParser parser = new GnuParser();
CommandLine cmd = parser.parse(options, args, true);
if (cmd.hasOption('h'))
	usage(0, options);

String [] remaining = cmd.getArgs();
if (remaining.length != 2)
	usage(1, options);

String xmlDir = remaining[0]
String outDir = remaining[1]

String angle=cmd.getOptionValue('a', "0.95");

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

HashMap<String, String> opts = new HashMap<String, String>();
opts.put("angle", angle);
SwapEdge se = new SwapEdge(mesh, opts)
se.setProgressBarStatus(10000);
se.compute();

MeshWriter.writeObject3D(mesh, outDir, null);

