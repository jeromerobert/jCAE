package org.jcae.viewer3d.test;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.vecmath.Point3d;
import org.jcae.opencascade.Utilities;
import org.jcae.opencascade.jni.*;
import org.jcae.viewer3d.SelectionListener;
import org.jcae.viewer3d.cad.CADSelection;
import org.jcae.viewer3d.cad.ViewableCAD;
import org.jcae.viewer3d.cad.occ.OCCProvider;
import org.jcae.viewer3d.post.TextureFitter;

/**
 * A demo of the TextureFitter class
 * @author Jerome Robert
 */
public class TextureFitterTest
{
	private ViewableCAD fullViewable;
	private TopoDS_Shape fullShape;
	
	private ViewableCAD faceViewable;
	private TopoDS_Shape faceShape;

	private TextureFitter view;
	private int step=0;
	private BufferedImage image;
	
	private static Point3d[] point3ds=new Point3d[]{
		new Point3d(105.876103680699, 154.355112438377, 444.233449262934),
		new Point3d(15649.9293772451, 170.548470749469, 494.066332188722),
		new Point3d(5504.83546309108, 31.9677707249711, 67.6028607292355)};

	private static Point3d[] point2ds=new Point3d[]{
		new Point3d(7838, 408, 0),
		new Point3d(65, 379, 0),
		new Point3d(5137, 605, 0)};

	/*private static Point3d[] point3ds=new Point3d[]{
		new Point3d(15425.4735650595, 101.630084521872, 281.979264993215),
		new Point3d(6972.95999971717, 93.695636325025, 257.562067493599),
		new Point3d(834.41408284612, 172.766820120453, 500.893004108474)};

	private static Point3d[] point2ds=new Point3d[]{
		new Point3d(178, 491, 0),
		new Point3d(4405, 505, 0),
		new Point3d(7474, 378, 0)};*/

	/**
	 * Add interactions with the view.
	 * Pressing space will allow to pick vertex, then display the texture.
	 * + - and direction keys allow to manually fit the texture.
	 * @author Jerome Robert
	 */
	private class MyKeyListener extends KeyAdapter
	{
		private int currentPoint=0;
		public void keyPressed(KeyEvent e)
		{
			System.out.println(e.getKeyCode());
			if(e.getKeyCode()==KeyEvent.VK_SPACE)
			{
				switch(step)
				{
					case 0:
						firstStep();
						step++;
						break;
					case 1:
						secondStep();
						TextureFitter.displayMatrixInfo(
							TextureFitter.getTransform(point2ds, point3ds));
						step++;
						break;
				}
			}
			
			boolean changed=false;
			
			switch(e.getKeyCode())
			{
				case KeyEvent.VK_UP:
					point2ds[currentPoint].y--;
					changed=true;
					break;
				case KeyEvent.VK_DOWN:
					point2ds[currentPoint].y++;
					changed=true;
					break;
				case KeyEvent.VK_LEFT:
					point2ds[currentPoint].x--;
					changed=true;
					break;
				case KeyEvent.VK_RIGHT:
					point2ds[currentPoint].x++;
					changed=true;
					break;				
				case KeyEvent.VK_ADD:
					currentPoint++;
					changed=true;
					break;				
				case KeyEvent.VK_SUBTRACT:
					currentPoint--;
					changed=true;
					break;				
			}
			
			if(changed)
			{				
				System.out.println("Point "+currentPoint+" : "+
					point2ds[currentPoint].x +" "+point2ds[currentPoint].y);
				view.updateTexture(point2ds, point3ds);
				TextureFitter.displayMatrixInfo(
					TextureFitter.getTransform(point2ds, point3ds));

			}
		}
	}
	
	/**
	 * Replace the full geometry by the selected faces
	 */
	private void firstStep()
	{
		//Get selected faces
		faceShape=TextureFitter.getSelectFaces(fullViewable, fullShape);
		
		//save it for later
		BRepTools.write(faceShape, "toto.brep");
		
		//remove the full geometry view
		view.remove(fullViewable);
		
		//display only the selected faces
		faceViewable=new ViewableCAD(new OCCProvider(faceShape));
		view.add(faceViewable);
		
		// switch from face picking mode to vertex picking mode
		faceViewable.setSelectionMode(ViewableCAD.VERTEX_SELECTION);
		
		// display the coordinates of the selected vertex
		faceViewable.addSelectionListener(new VertexSelectionListener());		
	}
	
	/**
	 * Display the selected the texture
	 */
	private void secondStep()
	{
		try
		{
			//display the texture
			image=ImageIO.read(
				new File("/home/jerome/ndtkit/190887A.png"));
			view.displayTexture(faceShape, point2ds, point3ds, image);

			//and the full CAD
			/*view.remove(faceViewable);
			view.add(fullViewable);*/
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
		
	}
	
	/**
	 * A simple SelectionListener which display the picked coordinates
	 * in the console
	 * @author Jerome Robert
	 */
	private class VertexSelectionListener implements SelectionListener
	{
		public void selectionChanged()
		{					
			CADSelection[] ss=faceViewable.getSelection();
			for(int i=0; i<ss.length; i++)
			{
				int[] ids=ss[i].getVertexIDs();
				for(int j=0; j<ids.length; j++)
				{
					TopoDS_Vertex v=Utilities.getVertex(faceShape, ids[j]);
					double[] coords=BRep_Tool.pnt(v);
					System.out.println("Vertex selected: "+ids[j]+
						" ("+coords[0]+", "+coords[1]+", "+coords[2]+")");
				}
			}
		}		
	}
	
	public TextureFitterTest()
	{
		JFrame frame=new JFrame();
		frame.setSize(800,600);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		view=new TextureFitter(frame);
		
		fullShape=Utilities.readFile("/home/jerome/ndtkit/g53330052-200.igs");
		OCCProvider occProvider=new OCCProvider(fullShape);
		fullViewable=new ViewableCAD(occProvider);
		view.add(fullViewable);
		frame.add(view);
		view.fitAll();
		frame.setVisible(true);
		view.setOriginAxisVisible(true);
		view.addKeyListener(new MyKeyListener());
	}

	public static void main(String[] args)
	{
		new TextureFitterTest();
	}
}
