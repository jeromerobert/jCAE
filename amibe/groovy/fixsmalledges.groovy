/**
 * Removes small edges from a CAO
 */
import org.jcae.opencascade.jni.FixSmallEdges;
import org.apache.commons.cli.*;

cmd=["fixsmalledges", "Remove small edges from a CAD file"]
usage="<input CAD file> <tolerance> <output BRep file>"

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

CommandLineParser parser = new GnuParser();
CommandLine cmd = parser.parse(options, args, true);
if (cmd.hasOption('h'))
	usage(0, options);

String [] remaining = cmd.getArgs();
if (remaining.length != 3)
	usage(1, options);

FixSmallEdges.main(remaining)


