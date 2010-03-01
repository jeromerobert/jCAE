/**
 * Run a viewer to display an Amibe mesh.
 */

import java.io.File;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import org.jcae.mesh.oemm.OEMM;
import org.jcae.mesh.oemm.Storage;
import javax.swing.JFrame;
import org.jcae.vtk.AmibeToMesh;
import org.jcae.vtk.BoraToMesh;
import org.jcae.vtk.Canvas;
import org.jcae.vtk.Palette;
import org.jcae.vtk.UNVToMesh;
import org.jcae.vtk.View;
import org.jcae.vtk.Viewable;
import org.jcae.vtk.ViewableCAD;
import org.jcae.vtk.ViewableMesh;
import org.jcae.vtk.ViewableOEMM;

import vtk.vtkRenderer as VtkRenderer;
import vtk.vtkInteractorStyleTrackballCamera as VtkInteractorStyleTrackballCamera;

import org.apache.commons.cli.*;

 cmd=["viewvtk ", "Display CAD (brep, stp or iges files) or mesh (Amibe or UNV format)"]
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

JFrame frame = new JFrame("jCAE Demo");
frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
View canvas = new View();
frame.add(canvas, BorderLayout.CENTER);
VtkRenderer renderer = canvas.GetRenderer();

File handle = new File(xmlDir);
Viewable viewable;
if (handle.isDirectory())
{
	if (new File(handle, "jcae3d").exists())
	{
		AmibeToMesh reader = new AmibeToMesh(xmlDir);
		viewable = new ViewableMesh(new Palette(32));
		viewable.addTriangles(reader.getTriangles());
		viewable.addBeams(reader.getBeams());
	}
	else if (new File(handle, "model").exists())
	{
		BoraToMesh reader = new BoraToMesh(xmlDir);
		viewable = new ViewableMesh(reader.getMesh());
	}
	else if (new File(handle, "oemm").exists())
	{
		OEMM oemm = Storage.readOEMMStructure(xmlDir);
		viewable = new ViewableOEMM(oemm);
	}
	else
		through new RuntimeException("Cannot find file");
	canvas.add(viewable);
}
else if (xmlDir.endsWith(".unv"))
{
	viewable = new ViewableMesh(new UNVToMesh(xmlDir, Collections.EMPTY_LIST).getMesh())
	canvas.add(viewable);
}
else
{
	viewable = new ViewableCAD(xmlDir);
	canvas.add(viewable);
}
frame.setSize(800,600);
frame.setVisible(true);

public class MyKeyListener implements KeyListener
{
	private final View view;
	public MyKeyListener(View v)
	{
		view = v;
	}
        public void keyReleased(KeyEvent e)
        {
                // DO Nothing
        }

        public void keyTyped(KeyEvent e)
        {
                // DO Nothing
        }

        public void keyPressed(KeyEvent e)
        {
                switch (e.getKeyCode())
                {
                        case KeyEvent.VK_F:
                                view.getCurrentViewable().setSelectionType(Viewable.SelectionType.CELL);
                                break;
                        case KeyEvent.VK_V:
                                view.getCurrentViewable().setSelectionType(Viewable.SelectionType.POINT);
                                view.lock();
                                System.out.println("Capabilities: " +view.GetRenderWindow().ReportCapabilities());
                                view.unlock();
                                break;
                        case KeyEvent.VK_E:
                                // Check the number of actors
                                int nbrActor = view.GetRenderer().GetNumberOfPropsRendered();
                                System.out.println("Number of actors rendered : " + nbrActor);
                                System.out.println("Number of actors : " + view.GetRenderer().GetViewProps().GetNumberOfItems());
                }
        }

}
canvas.addKeyListener(new MyKeyListener(canvas));

VtkInteractorStyleTrackballCamera style = new VtkInteractorStyleTrackballCamera();
style.AutoAdjustCameraClippingRangeOn();
canvas.getIren().SetInteractorStyle(style);
renderer.ResetCamera();

