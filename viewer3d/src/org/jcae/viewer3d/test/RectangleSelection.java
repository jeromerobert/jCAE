package org.jcae.viewer3d.test;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
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
			feFrame.setSize(800,600);
	
			// Put it all in a view
			final View feView=new View(feFrame);
	
			// Fit the view to the object
			feView.fitAll();
	
			// Put it all in a Swing frame
			feFrame.getContentPane().add(feView);
			feFrame.setVisible(true);
	
			// let's add a CAD viewable loaded from an Opencascade file.
			ViewableCAD fcad=new ViewableCAD(new OCCProvider("/home/jerome/Models/lego.brep"));
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
				}
			});
			feView.setMouseMode(ViewBehavior.RECTANGLE_MODE);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
