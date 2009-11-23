/**
 * Run a viewer to display an Amibe mesh.
 */
import java.io.File;
import java.io.PrintWriter;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import org.jcae.viewer3d.fe.FEProvider;
import org.jcae.viewer3d.fe.amibe.AmibeProvider;
import org.jcae.viewer3d.fe.unv.UNVProvider;
import org.jcae.viewer3d.fe.ViewableFE;
import org.jcae.viewer3d.cad.ViewableCAD;
import org.jcae.viewer3d.cad.occ.OCCProvider;
import org.jcae.viewer3d.View;
import org.jcae.viewer3d.ViewBehavior;
import org.apache.commons.cli.*;

cmd=["view    ", "Display CAD (brep, stp or iges files) or mesh (Amibe or UNV format)"]
usage="<dir|file>"

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
CommandLineParser parser = new GnuParser();
CommandLine cmd = parser.parse(options, args, true);
if (cmd.hasOption('h'))
	usage(0, options);

String [] remaining = cmd.getArgs();
if (remaining.length != 1)
	usage(1, options);

String xmlDir = remaining[0]

class MyKeyAdapter extends KeyAdapter
{
	private final View view;
	private boolean clipped;
	MyKeyAdapter(View v)
	{
		view = v;
	}
	public void keyTyped(KeyEvent e)
	{
		char c = e.getKeyChar();
		if(c=='?')
		{
			view.println("Key usage:");
			view.println(" ?  Display this help message");
			view.println(" c  Enter clip mode (any other key to escape)");
			view.println(" r  Restore view");
		}
		else
		{
			if(c=='c')
				clipped = !clipped;
			else
				clipped = false;
			if (clipped)
				view.setMouseMode(ViewBehavior.CLIP_RECTANGLE_MODE);
			else
				view.setMouseMode(ViewBehavior.DEFAULT_MODE);
			if(c=='r')
			{
				view.println("> Restore view");
				view.removeModelClip();
			}
		}
	}
}

JFrame feFrame=new JFrame("jCAE Demo");
feFrame.setSize(800,600);
feFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

View bgView=new View(feFrame);
bgView.setPrintWriter(new PrintWriter(System.out));
File handle = new File(xmlDir);
if (handle.isDirectory())
	bgView.add(new ViewableFE(new AmibeProvider(handle)));
else if (xmlDir.endsWith(".unv"))
	bgView.add(new ViewableFE(new UNVProvider(handle)));
else
	bgView.add(new ViewableCAD(new OCCProvider(xmlDir)));
bgView.fitAll();
bgView.addKeyListener(new MyKeyAdapter(bgView));
feFrame.getContentPane().add(bgView);
feFrame.setVisible(true);
bgView.setOriginAxisVisible(true);

