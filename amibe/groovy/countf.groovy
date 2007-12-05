/**
 * Count faces in CAD file
 */
import org.jcae.mesh.cad.CADShape
import org.jcae.mesh.cad.CADShapeFactory
import org.jcae.mesh.cad.CADShapeEnum
import org.jcae.mesh.cad.CADExplorer
import org.apache.commons.cli.*;

cmd=["countf  ", "Count faces found in CAD file"]
usage="<cadFile>"

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
if (remaining.length != 1)
	usage(1, options);

String brepfile = remaining[0]
CADShapeFactory factory = CADShapeFactory.getFactory()
CADShape shape = factory.newShape(brepfile);
CADExplorer expl = factory.newExplorer()

seen = []
for (expl.init(shape, CADShapeEnum.FACE); expl.more(); expl.next()) {
	face = expl.current()
	if (! (face in seen))
		seen << face
}
println("Faces: "+seen.size())
