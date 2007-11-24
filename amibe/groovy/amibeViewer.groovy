/**
 * Run a viewer to display an Amibe mesh.
 */
import java.io.File;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import org.jcae.viewer3d.fe.amibe.AmibeProvider;
import org.jcae.viewer3d.fe.ViewableFE;
import org.jcae.viewer3d.View;
import org.apache.commons.cli.*;

void usage(int rc, Options options)
{
	HelpFormatter formatter = new HelpFormatter();
	formatter.printHelp("groovy amibeViewer.groovy xmlDir", "Launch a view to display mesh stored in xmlDir directory", options, "");
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

String xmlDir = remaining[0]

JFrame feFrame=new JFrame("jCAE Demo");
feFrame.setSize(800,600);
feFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

View bgView=new View(feFrame);
AmibeProvider ap = new AmibeProvider(new File(xmlDir));
bgView.add(new ViewableFE(ap));
bgView.fitAll();
feFrame.getContentPane().add(bgView);
feFrame.setVisible(true);
bgView.setOriginAxisVisible(true);

