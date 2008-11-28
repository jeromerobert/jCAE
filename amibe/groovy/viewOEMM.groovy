/**
 * Run a viewer to display an Amibe mesh.
 */
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIterator;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.vecmath.Point3d;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.oemm.OEMM;
import org.jcae.mesh.oemm.Storage;
import org.jcae.mesh.oemm.MeshReader;
import org.jcae.viewer3d.FPSBehavior;
import org.jcae.viewer3d.OEMMBehavior;
import org.jcae.viewer3d.OEMMViewer;
import org.jcae.viewer3d.View;
import org.jcae.viewer3d.bg.ViewableBG;

import org.apache.commons.cli.*;

 cmd=["viewOEMM ", "Display two OEMM containing a very large mesh and its decimation"]
usage="<full_OEMM_dir> <decimated_OEMM_dir>"

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
if (remaining.length != 2)
	usage(1, options);

System.setProperty("org.jcae.viewer3d.OEMMBehavior.maxNumberOfTriangles", "300000");

class MyPropertyChangeListener implements PropertyChangeListener
{
	public void propertyChange(PropertyChangeEvent evt) {
		System.out.println("FPS>" + evt.getNewValue());
	}
}

class MyKeyAdapter extends KeyAdapter
{
	private final View bgView;
	private boolean showOctree = false;
	private boolean showAxis = true;
	private boolean showFPS = true;
	final OEMM oemm;
	final OEMM decimatedOemm;
	final ViewableBG octree;
	final ViewableBG fps;
	final OEMMBehavior oemmBehavior;

	MyKeyAdapter(View v, String full, String decimated)
	{
		bgView = v;
		oemm = Storage.readOEMMStructure(full);
		decimatedOemm = Storage.readOEMMStructure(decimated);
		octree = new ViewableBG(OEMMViewer.bgOEMM(oemm, true));

		BranchGroup bg=new BranchGroup();
		oemmBehavior=new OEMMBehavior(bgView, oemm, decimatedOemm);
		bg.addChild(oemmBehavior);
		bgView.addBranchGroup(bg);

		FPSBehavior fpsB = new FPSBehavior();
		fpsB.setSchedulingBounds(new BoundingSphere(new Point3d(), Double.MAX_VALUE));
		fpsB.addPropertyChangeListener(new MyPropertyChangeListener());
		BranchGroup fpsBG = new BranchGroup();
		fpsBG.addChild(fpsB);
		fps = new ViewableBG(fpsBG);
		if (showFPS)
			bgView.add(fps);

	}

	static final void printInteractiveUsage()
	{
		System.out.println("Key usage:");
		System.out.println("  ?: Display this help message");
		System.out.println("  q: Exit");
		System.out.println("  o: Toggle display of octree boxes");
		System.out.println("  a: Toggle axis display");
		System.out.println("  F: Toggle FPS display");
		System.out.println("  i: Print selected nodes");
		System.out.println("  v: Highlight octree nodes containing fine mesh");
		System.out.println("  f: Toggle freeze of coarse/fine mesh adaptation");
		System.out.println("  n: Print mesh normals in selected octree nodes");
		System.out.println("  p: Print mesh statistics");
		System.out.println("  c: Print cache statistics");
	}

	static final void printMeshStatistics(String header, OEMM oemm)
	{
		int triangles = 0;
		int vertices = 0;
		for(OEMM.Node current: oemm.leaves)
		{
			triangles += current.tn;
			vertices += current.vn;
		}
		System.out.println(header+": "+triangles+" triangles and "+vertices+" vertices");
	}
	public void keyPressed(KeyEvent event)
	{
		char k = event.getKeyChar();
		if(k == '?')
		{
			printInteractiveUsage();
		}
		else if (k == 'o')
		{
			showOctree = !showOctree;
			if (showOctree)
			{
				bgView.add(octree);
				bgView.setCurrentViewable(octree);
			}
			else
				bgView.remove(octree);
		}
		else if (k == 'a')
		{
			showAxis = !showAxis;
			bgView.setOriginAxisVisible(showAxis);
		}
		else if (k == 'F')
		{
			showFPS = !showFPS;
			if (showFPS)
			{
				bgView.add(fps);
				bgView.setCurrentViewable(fps);
			}
			else
				bgView.remove(fps);

		}
		else if (k == 'i')
		{
			System.out.println("Selected: " + octree.getResultSet());
		}
		else if (k == 'v')
		{
			octree.unselectAll();
			for (int i: oemmBehavior.getIds()) {
				octree.highlight(i, true);
			}
		}
		else if (k == 'f')
		{
			oemmBehavior.switchFreeze();
		}
		else if (k == 'n')
		{
			MeshReader fineReader = new MeshReader(oemm);
			MeshReader coarseReader = new MeshReader(decimatedOemm);
			double[] tempD1 = new double[3];
			double[] tempD2 = new double[3];
			double[] tempn1 = new double[3];
			double[] tempn2 = new double[3];
			TIntHashSet leaves = new TIntHashSet();
			for (TIntIterator it = octree.getResultSet().iterator(); it.hasNext(); )
			{
				int i = it.next();
				leaves.clear();
				leaves.add(i);
				Mesh mesh = fineReader.buildMesh(leaves);
				Mesh coarseMesh = coarseReader.buildMesh(leaves);
				Triangle tf = mesh.getTriangles().iterator().next();
				Matrix3D.computeNormal3D(tf.vertex[0].getUV(), tf.vertex[1].getUV(), tf.vertex[2].getUV(), tempD1, tempD2, tempn1);
				Triangle tc = coarseMesh.getTriangles().iterator().next();
				Matrix3D.computeNormal3D(tc.vertex[0].getUV(), tc.vertex[1].getUV(), tc.vertex[2].getUV(), tempD1, tempD2, tempn2);
				System.out.println("Coarse normal for first triangle of leaf: " + i + " [" 
						+ tempn1[0] + ", "+ tempn1[1] + ", "+ tempn1[2] + "] and fine " +
						" [" 
						+ tempn2[0] + ", "+ tempn2[1] + ", "+ tempn2[2] + "]" + " and "
						+ " orientation: " + (tempn1[0]*tempn2[0] + tempn1[1]*tempn2[1] + tempn1[2]*tempn2[2]  ));
			}
		}
		else if (k == 'c')
		{
			for (TIntIterator it = octree.getResultSet().iterator(); it.hasNext(); )
			{
				int i = it.next();
				Point3d vector = oemmBehavior.getVoxel(i);

				System.out.println("Node: " + i + ", vector: [" + vector.x + ", " + vector.y + ", " + vector.z + "]");
			}
			System.out.println("Visible oemm nodes: " + oemmBehavior.getNumberOfVisibleFineElements() + ", cache: " + oemmBehavior.getNumberOfCacheNodes());
		}
		else if (k == 'p')
		{
			printMeshStatistics("Coarse mesh", decimatedOemm);
			printMeshStatistics("Fine mesh", oemm);
		}
		else if (k == 'q')
			System.exit(0);
	}
}

JFrame feFrame=new JFrame("jcae-viewer3d-fd demo");
feFrame.setSize(800,600);
feFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
final View bgView=new View(feFrame);

bgView.addKeyListener(new MyKeyAdapter(bgView, remaining[0], remaining[1]));
bgView.fitAll();
bgView.setOriginAxisVisible(true);
feFrame.getContentPane().add(bgView);
feFrame.setVisible(true);


