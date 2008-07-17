package org.jcae.viewer3d.test;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import org.jcae.viewer3d.View;
import org.jcae.viewer3d.ViewBehavior;
import org.jcae.viewer3d.cad.ViewableCAD;
import org.jcae.viewer3d.cad.occ.OCCProvider;

/** A "getting started" example */
public class RectangleSelection
{
	public static void main(String[] args)
	{
		try
		{
			// The swing frame where we will put the view
			JFrame feFrame=new JFrame();
			feFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			feFrame.setSize(800,600);
	
			// Put it all in a view
			final View feView=new View(feFrame);
			feView.setFrontClipDistance(100.);
			// Fit the view to the object
			feView.fitAll();
	
			// Put it all in a Swing frame
			feFrame.getContentPane().add(feView);
			feFrame.setVisible(true);
	
			// let's add a CAD viewable loaded from an Opencascade file.
			final ViewableCAD fcad=				
				new ViewableCAD(new OCCProvider("/home/ibarz/models/axe.brep"));				
			feView.add(fcad);

			// Fit again as we have added a new object
			feView.fitAll();
			feView.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent e)
				{					
					if(e.getKeyChar()=='r')
						feView.setMouseMode(ViewBehavior.RECTANGLE_MODE);
					if(e.getKeyChar()=='n')
						feView.setMouseMode(ViewBehavior.DEFAULT_MODE);
					if(e.getKeyChar()=='a')
						fcad.setSelectionAction(ViewableCAD.SELECTION_ADD);
					if(e.getKeyChar()=='d')
						fcad.setSelectionAction(ViewableCAD.SELECTION_REMOVE);
					if(e.getKeyChar()=='i')
						fcad.setSelectionAction(ViewableCAD.SELECTION_INVERT);
					if(e.getKeyChar()=='t')
						fcad.setSelectionAction(ViewableCAD.SELECTION_INTERSECT);
					if(e.getKeyChar()=='v')
						fcad.setSelectionMode(ViewableCAD.VERTEX_SELECTION);
					if(e.getKeyChar()=='f')
						fcad.setSelectionMode(ViewableCAD.FACE_SELECTION);
					if(e.getKeyChar()=='e')
						fcad.setSelectionMode(ViewableCAD.EDGE_SELECTION);
					if(e.getKeyChar()=='n') {
						
						
						System.out.println("FRONT DISTANCE : " + feView.getFrontClipDistance());
						feView.setFrontClipDistance(1.);
						float[] zbuffer = feView.getDepthBuffer(feView.getWidth() / 2,feView.getHeight() / 2,feView.getWidth() / 4, feView.getHeight() / 4);
						
						float min = Float.MAX_VALUE, max = Float.MIN_VALUE;
						
						for(float z : zbuffer)
						{
							min = Math.min(min,z);
							max = Math.max(min,z);
						}
						System.out.println("MIN Z : " + min + " and MAX Z : " + max);
					}
					if(e.getKeyChar()=='s')
					{
						BufferedImage imageBuffer = feView.getScreenShot(feView.getWidth() / 2, feView.getHeight() / 2, feView.getWidth() - (feView.getWidth() /2) , feView.getHeight() - feView.getHeight() / 2);
						try
						{
							ImageIO.write(imageBuffer, "PNG", File.createTempFile("screen", "png"));
						} catch (IOException ex)
						{
							Logger.getLogger(RectangleSelection.class.getName()).log(Level.SEVERE, null, ex);
						}
					}
				}
			});			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
