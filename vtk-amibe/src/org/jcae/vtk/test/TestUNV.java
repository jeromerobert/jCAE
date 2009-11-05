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

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collections;
import javax.swing.JFrame;
import org.jcae.vtk.Canvas;
import org.jcae.vtk.SelectionListener;
import org.jcae.vtk.UNVToMesh;
import org.jcae.vtk.View;
import org.jcae.vtk.Viewable;
import org.jcae.vtk.AmibeViewable;
import vtk.vtkInteractorStyleTrackballCamera;
import vtk.vtkRenderer;

/**
 *
 * @author Julian Ibarz
 */
public class TestUNV implements SelectionListener, KeyListener
{
	AmibeViewable viewable;
	public Canvas canvas;

	public TestUNV(AmibeViewable viewable)
	{
		this.viewable = viewable;
	}

	public void selectionChanged(Viewable viewable)
	{
		viewable.highlight();
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
			case KeyEvent.VK_G:
				viewable.setSelectionType(Viewable.SelectionType.NODE);
				break;
			case KeyEvent.VK_F:
				viewable.setSelectionType(Viewable.SelectionType.CELL);
				break;
			case KeyEvent.VK_A:
				viewable.setAppendSelection(!viewable.getAppendSelection());
				break;
			case KeyEvent.VK_V:
				viewable.setSelectionType(Viewable.SelectionType.POINT);
				canvas.lock();
				System.out.println("Capabilities : " + canvas.GetRenderWindow().ReportCapabilities());
				canvas.unlock();
				break;
				case KeyEvent.VK_O:
					viewable.setViewMode(AmibeViewable.ViewMode.FILLED);
					break;
			case KeyEvent.VK_I:
					viewable.setViewMode(AmibeViewable.ViewMode.WIRED);
				break;
				
		}
	}

	public static void main(String[] args)
	{
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		View canvas = new View();
		frame.add(canvas, BorderLayout.CENTER);
		vtkRenderer renderer = canvas.GetRenderer();
		AmibeViewable rbh = new AmibeViewable(new UNVToMesh(args[0], Collections.EMPTY_LIST).getMesh());
		canvas.add(rbh);
		frame.setSize(800, 600);
		frame.setVisible(true);
		TestUNV test = new TestUNV(rbh);
		//rbh.setViewMode(ViewableMesh.ViewMode.WIRED);
		test.canvas = canvas;
		canvas.addKeyListener(test);
		rbh.addSelectionListener(test);
		vtkInteractorStyleTrackballCamera style = new vtkInteractorStyleTrackballCamera();
		//canvas.getIren().SetPicker(new vtkAreaPicker());
		style.AutoAdjustCameraClippingRangeOn();
		canvas.getIren().SetInteractorStyle(style);
		renderer.ResetCamera();
		// A label example
		/*vtkTextActor textActor = new vtkTextActor();
		textActor.ScaledTextOff();
		textActor.SetDisplayPosition(600, 40);
		textActor.SetInput("This is a label example");
		
		canvas.GetRenderer().AddActor(textActor);*/

		// A display orientation axes example
		/*vtkAxesActor axes = new vtkAxesActor();
		axes.AxisLabelsOn();
		
		vtkOrientationMarkerWidget marker = new vtkOrientationMarkerWidget();
		marker.SetOrientationMarker(axes);
		marker.SetViewport(0., 0., .3, .3);
		
		// A bug occurs :
		//# An unexpected error has been detected by Java Runtime Environment:
		//#
		//#  SIGSEGV (0xb) at pc=0xb2d5082b, pid=17249, tid=2971425680
		//#
		//# Java VM: Java HotSpot(TM) Client VM (1.6.0-b105 mixed mode, sharing)
		//# Problematic frame:
		//# C  [libvtkRenderingJava.so.5.1.0+0x36d82b]  _ZN11vtkRenderer15GetActiveCameraEv+0x9
		//#
		//# An error report file with more information is saved as hs_err_pid17249.log
		//#
		//# If you would like to submit a bug report, please visit:
		//#   http://java.sun.com/webapps/bugreport/crash.jsp
		//#
		// And sometines this error :
		//#
		//# An unexpected error has been detected by Java Runtime Environment:
		//#
		//#  SIGSEGV (0xb) at pc=0xb1314362, pid=4223, tid=2970426256
		//#
		//# Java VM: Java HotSpot(TM) Client VM (1.6.0-b105 mixed mode, sharing)
		//# Problematic frame:
		//# C  [libvtkWidgetsJava.so.5.1.0+0x1f1362]  _ZN26vtkOrientationMarkerWidget24ExecuteCameraUpdateEventEP9vtkObjectmPv+0x28
		//#
		//# An error report file with more information is saved as hs_err_pid4223.log
		//#
		//# If you would like to submit a bug report, please visit:
		//#   http://java.sun.com/webapps/bugreport/crash.jsp
		//#
		// And sometimes a warning occurs :
		// Generic Warning: In /home/ibarz/lib/VTK/Common/vtkObject.cxx, line 532
		// Passive observer should not call AddObserver or RemoveObserver in callback.
		// Sometimes, we can see the "repere" not moving juste before the crash.
		// TODO : make a bug report ?
		marker.SetInteractor(canvas.getIren());
		marker.SetEnabled(1);
		marker.SetInteractive(0);*/

		
		
		

	// Delete all the java wrapped VTK objects
	//vtkGlobalJavaHash.DeleteAll();
	}
}
