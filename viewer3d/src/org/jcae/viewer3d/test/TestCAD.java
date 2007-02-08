package org.jcae.viewer3d.test;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.io.File;
import javax.swing.*;
import org.jcae.viewer3d.View;
import org.jcae.viewer3d.cad.ViewableCAD;
import org.jcae.viewer3d.cad.occ.OCCProvider;

public class TestCAD
{
	static  class Loader implements Runnable
	{
		private Container component;
		private String fileName;

		public Loader(String fileName, Container component)
		{
			this.fileName=fileName;
			this.component=component;
		}
		
		public void run()
		{
			View view=new View(false, true);
			OCCProvider occProvider=new OCCProvider(fileName);
			System.out.println("Loading "+fileName);
			ViewableCAD c=new ViewableCAD(occProvider);
			view.add(c);
			component.add(view);//, fileName);
			view.fitAll();
			System.out.println(fileName+" Loaded");
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		String modelDir="/home/jerome/Models";
		System.setProperty("javax.media.j3d.zFactorAbs", "0");		
		System.setProperty("javax.media.j3d.zFactorRel", "0");		
		JFrame frame=new JFrame();
		frame.setSize(800,600);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		Container container = new JPanel();
		container.setLayout(new GridLayout(2,3));
		frame.setContentPane(container);		
		// Using libOccJava with multithreading make it crash ???
		/*		
		new Thread(new Loader(modelDir+File.separator+"tube2_biseau.brep", container)).start();
		new Thread(new Loader(modelDir+File.separator+"hammer.brep", container)).start();
		new Thread(new Loader(modelDir+File.separator+"flight_solid.brep", container)).start();
		new Thread(new Loader(modelDir+File.separator+"66_shaver3.brep", container)).start();
		*/
		new Loader(modelDir+File.separator+"tube2_biseau.brep", container).run();
		new Loader(modelDir+File.separator+"hammer.brep", container).run();
		new Loader(modelDir+File.separator+"flight_solid.brep", container).run();
		new Loader(modelDir+File.separator+"66_shaver3.brep", container).run();
		new Loader(modelDir+File.separator+"lego.brep", container).run();
		frame.setVisible(true);
	}
	
}
