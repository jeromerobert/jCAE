/**
 * Sample class to print quality statistics about edge lengths
 */
import org.jcae.mesh.amibe.ds.MMesh1D;
import org.jcae.mesh.amibe.ds.MEdge1D;
import org.jcae.mesh.amibe.ds.MNode1D;
import org.jcae.mesh.xmldata.MMesh1DReader;
import org.jcae.mesh.amibe.validation.*;
import org.apache.commons.cli.*;
import org.jcae.mesh.cad.CADEdge;

void usage(int rc, Options options)
{
	HelpFormatter formatter = new HelpFormatter();
	formatter.printHelp("groovy amibeReporter1D.groovy [options] xmlDir", "Prints statistics about 1D mesh stored in xmlDir directory", options, "");
	System.exit(rc);
}

Options options = new Options();
options.addOption(
	OptionBuilder.hasArg(false)
		.withDescription("usage information")
		.withLongOpt("help")
		.create('h'));
options.addOption(
	OptionBuilder.withArgName("LIST").hasArg()
		.withDescription("comma separated list of values, implies -H (default: 0.2,0.4,0.6,0.8)")
		.withLongOpt("bounds")
		.create('b'));
options.addOption(
	OptionBuilder.hasArg(false)
		.withDescription("prints histogram")
		.withLongOpt("histogram")
		.create('H'));
CommandLineParser parser = new GnuParser();
CommandLine cmd = parser.parse(options, args, true);
if (cmd.hasOption('h'))
	usage(0, options);

String [] remaining = cmd.getArgs();
if (remaining.length != 1)
	usage(1, options);

String xmlDir = remaining[0]
boolean histogram = cmd.hasOption('H') || cmd.hasOption('b');

String [] sBounds=cmd.getOptionValue('b', "0.2,0.4,0.6,0.8").split(",");
Float [] bounds = new Float[sBounds.length];
for (int i = 0; i < bounds.length; i++)
	bounds[i] = Float.parseFloat(sBounds[i]);

EdgeLength1D qproc = new EdgeLength1D();

QualityFloat data = new QualityFloat();
data.setQualityProcedure(qproc);

MMesh1D mesh = MMesh1DReader.readObject(xmlDir)
Iterator<CADEdge> it = mesh.getTEdgeIterator();
while (it.hasNext())
{
	CADEdge E = it.next();
	if (E.isDegenerated())
		continue;
	qproc.setCADEdge(E);
	Iterator<MEdge1D> ite = mesh.getEdgelistFromMap(E).iterator();
	while (ite.hasNext())
	{
		MEdge1D edge = ite.next();
		data.compute(edge);
	}
}

data.finish();
if (histogram)
{
	// Prints histogram on console
	data.split(bounds);
	data.printLayers();
}
else
	data.printStatistics();

