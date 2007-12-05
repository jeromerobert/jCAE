/**
 * Read 3d file and export mesh into another format.
 */
import org.jcae.mesh.xmldata.MeshExporter
import java.lang.reflect.Constructor;
import org.apache.commons.cli.*;

cmd=["export  ", "Export Amibe mesh into other file format"]
usage="<xmlDir> <filename>"

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
	OptionBuilder.withArgName("STRING").hasArg()
		.withDescription("output file format")
		.withLongOpt("format")
		.create('f'));
options.addOption(
	OptionBuilder.hasArg(false)
		.withDescription("list available output formats")
		.withLongOpt("list-formats")
		.create('F'));
CommandLineParser parser = new GnuParser();
CommandLine cmd = parser.parse(options, args, true);
if (cmd.hasOption('h'))
	usage(0, options);
if (cmd.hasOption('F'))
{
	println("List of supported file formats:")
	println("\tUNV: I-DEAS Universal file format (default suffix: .unv)")
	println("\tMESH: Medit format (default suffix: .mesh)")
	println("\tSTL: STL (default suffix: .stl)")
	println("\tVTK: VTK (default suffix: .vtp)")
	System.exit(0);
}

String [] remaining = cmd.getArgs();
if (remaining.length != 2)
	usage(1, options);

String xmlDir = remaining[0]
String filename = remaining[1]

String format = null;
if (cmd.hasOption('f'))
	format=cmd.getOptionValue('f');
else if (filename.endsWith(".unv") || filename.endsWith(".UNV"))
	format="UNV"
else if (filename.endsWith(".mesh") || filename.endsWith(".MESH"))
	format="MESH"
else if (filename.endsWith(".stl") || filename.endsWith(".STL"))
	format="STL"
else if (filename.endsWith(".vtp"))
	format="VTK"
else
	throw new IllegalArgumentException("Unknown extension: cannot guess output file format")

Class<?> [] innerClasses = org.jcae.mesh.xmldata.MeshExporter.class.getDeclaredClasses();
Class innerClass=null;
for (int i = 0; i < innerClasses.length; i++)
{
	if (innerClasses[i].getName().endsWith('$'+format))
	{
		innerClass = innerClasses[i];
		break;
	}
}
Constructor cons = innerClass.getConstructor(String.class);

MeshExporter export = cons.newInstance(xmlDir);
export.write(filename)

