/**
 * Convert a UNV file into amibe format.
 */
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.xmldata.MeshWriter;
import org.jcae.mesh.amibe.util.UNVReader;
import org.apache.commons.cli.*;

void usage(int rc, Options options)
{
	HelpFormatter formatter = new HelpFormatter();
	formatter.printHelp("groovy unv2amibe.groovy [options] unvFile", options);
	System.exit(rc);
}

Options options = new Options();
options.addOption(
	OptionBuilder.hasArg(false)
		.withDescription("usage information")
		.withLongOpt("help")
		.create('h'));
options.addOption(
	OptionBuilder.withArgName("PATH").hasArg()
		.withDescription("output directory (default is current directory)")
		.withLongOpt("outputdir")
		.create('o'));
CommandLineParser parser = new GnuParser();
CommandLine cmd = parser.parse(options, args, true);
if (cmd.hasOption('h'))
	usage(0, options);

String [] remaining = cmd.getArgs();
if (remaining.length != 1)
	usage(1, options);

String unvFile = remaining[0];
String outDir = cmd.getOptionValue('o', '.');
Mesh mesh = new Mesh(new MeshTraitsBuilder());
UNVReader.readMesh(mesh, unvFile);
MeshWriter.writeObject3D(mesh, outDir, "jcae3d", ".", "dummy.brep");
