package org.jcae.viewer3d.test;

import java.io.File;
import javax.swing.JFrame;
import org.jcae.viewer3d.View;
import org.jcae.viewer3d.cad.ViewableCAD;
import org.jcae.viewer3d.cad.occ.OCCProvider;
import org.jcae.viewer3d.fe.ViewableFE;
import org.jcae.viewer3d.fe.amibe.AmibeProvider;

/** A "getting started" example */
public class Example1
{
	public static void main(String[] args)
	{
		try
		{
			// The swing frame where we will put the view
			JFrame feFrame=new JFrame();
			feFrame.setSize(800,600);
	
			// Say we want to load an amibe mesh
			AmibeProvider ap=new AmibeProvider(new File("/home/jerome/JCAEProject/amibe1.dir"));
	
			// We want to view a finit element mesh
			ViewableFE ffe=new ViewableFE(ap);
	
			// Put it all in a view
			View feView=new View(feFrame);
			feView.add(ffe);
	
			// Fit the view to the object
			feView.fitAll();
	
			// Put it all in a Swing frame
			feFrame.getContentPane().add(feView);
			feFrame.setVisible(true);
	
			// let's add a CAD viewable loaded from an Opencascade file.
			ViewableCAD fcad=new ViewableCAD(new OCCProvider("/home/jerome/Models/F1.brep"));
			feView.add(fcad);

			// Fit again as we have added a new object
			feView.fitAll();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
