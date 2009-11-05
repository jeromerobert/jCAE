/*
 * Project Info:  http://jcae.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2008, by EADS France
 */

package org.jcae.vtk.test;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.ToolTipManager;
import org.jcae.vtk.ViewableCAD;
import org.jcae.vtk.Utils;
import vtk.vtkActor;
import org.jcae.vtk.Canvas;
import org.jcae.vtk.UNVToMesh;
import org.jcae.vtk.AmibeViewable;
import vtk.vtkCanvas;
import vtk.vtkFileOutputWindow;

/** Test that VTK canvas can be properly integrated in a Swing GUI
 * TODO : Don't work actually !
 */
public class TestGUI implements KeyListener
{

	public vtkCanvas canvas;

	public TestGUI()
	{

	}
	public static TestGUI instance = new TestGUI();

	public static void main(String[] args)
	{
		try
		{
			Utils.loadVTKLibraries();
			JPopupMenu.setDefaultLightWeightPopupEnabled(false);
			ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
			JFrame frame = new JFrame();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			JSplitPane split = new JSplitPane();
			JTabbedPane tabbedPane = new JTabbedPane();

			Canvas c1 = new Canvas();
			instance.canvas = c1;
			Canvas c2 = new Canvas();
			Canvas c3 = new Canvas();

			c1.addKeyListener(instance);
			c2.addKeyListener(instance);
			c3.addKeyListener(instance);
			ArrayList<vtkActor> actors = new ArrayList<vtkActor>();

			if (args[0].equalsIgnoreCase("cao"))
			{
				ViewableCAD cascadeActorManager = new ViewableCAD(args[1]);
				cascadeActorManager.addCanvas(c1);

				//actors.add(cascadeActorManager.getBackFaces());
				//actors.add(cascadeActorManager.getEdgesActor());
				//actors.add(cascadeActorManager.getFacesActor());
				//actors.add(cascadeActorManager.getVerticesActor());
			} else if (args[0].equalsIgnoreCase("mailleur"))
			{
				AmibeViewable viewer = new AmibeViewable(new UNVToMesh(args[1],Collections.EMPTY_SET).getMesh());
				viewer.addCanvas(c1);
				//actors.add(viewer.computeActor());
			} else
				throw new RuntimeException("Type of viewer unknown");

			for (vtkActor actor : actors)
			{
				c2.GetRenderer().AddActor(actor);
				c3.GetRenderer().AddActor(actor);
			}

			c1.GetRenderer().ResetCamera();
			c2.GetRenderer().ResetCamera();
			c3.GetRenderer().ResetCamera();
			split.add(c1, JSplitPane.BOTTOM);
			//split.setContinuousLayout(true);
			split.add(c2, JSplitPane.TOP);
			tabbedPane.add(split, "Splitted");
			tabbedPane.add(c3, "Not splitted");
			JMenuBar bar = new JMenuBar();
			JMenu menu = new JMenu("File");
			menu.add(new JMenuItem("New"));
			menu.add(new JMenuItem("Open"));
			menu.add(new JMenuItem("Close"));
			menu.add(new JMenuItem("Save"));
			menu.add(new JMenuItem("Obiwan Kenobi"));
			menu.add(new JMenuItem("Exit"));

			bar.add(menu);

			// Variables declaration - do not modify
			JToolBar jToolBar1 = new javax.swing.JToolBar();
			JButton jButton1 = new javax.swing.JButton();

			jToolBar1.setRollover(true);

			jButton1.setText("jButton1");
			jButton1.setFocusable(false);
			jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
			jButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
			jToolBar1.add(jButton1);

			frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
			frame.setJMenuBar(bar);
			frame.add(jToolBar1);
			frame.add(tabbedPane);

			frame.pack();



			frame.setSize(800, 600);
			frame.setVisible(true);
			split.setDividerLocation(0.5);

			// DEBUG ON
			vtkFileOutputWindow output = new vtkFileOutputWindow();
			output.SetInstance(output);
			output.SetFileName("debug.log");

			System.out.println("Fichier de log : " + output.GetFileName());

			c1.GetRenderer().DebugOn();
			c1.GetRenderWindow().DebugOn();
			c2.GetRenderer().DebugOn();
			c2.GetRenderWindow().DebugOn();
		//c3.GetRenderer().DebugOn();
		} catch (Exception ex)
		{
			Logger.getLogger(TestVTK.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void keyPressed(KeyEvent e)
	{
		System.out.println("Key Pressed");
	}

	public void keyReleased(KeyEvent e)
	{
		System.out.println("Key Released");

		if (e.getKeyCode() == KeyEvent.VK_C)
		{
				Utils.takeScreenshot(canvas);
		}
	}

	public void keyTyped(KeyEvent arg0)
	{

	}
//
}
