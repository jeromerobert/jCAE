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
import javax.swing.JFrame;
import org.jcae.vtk.AmibeToMesh;
import org.jcae.vtk.Canvas;
import org.jcae.vtk.SelectionListener;
import org.jcae.vtk.View;
import org.jcae.vtk.Viewable;
import org.jcae.vtk.ViewableMesh;
import vtk.vtkInteractorStyleTrackballCamera;
import vtk.vtkRenderer;

/**
 *
 * @author Julian Ibarz
 */
public class TestAmibe implements SelectionListener, KeyListener {
	ViewableMesh viewable;
	public Canvas canvas;

	public TestAmibe(ViewableMesh viewable)
	{
		this.viewable = viewable;
	}
	
	public void selectionChanged(Viewable viewable)
	{
		viewable.highLight();
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
				viewable.setSelectionType(Viewable.SelectionType.CELL);
				break;
			case KeyEvent.VK_V:
				viewable.setSelectionType(Viewable.SelectionType.POINT);
				canvas.lock();
				System.out.println("Capabilities : " +canvas.GetRenderWindow().ReportCapabilities());
				canvas.unlock();
				break;
			case KeyEvent.VK_E:
				// Check the number of actors
				int nbrActor = canvas.GetRenderer().GetNumberOfPropsRendered();
				System.out.println("Number of actors rendered : " + nbrActor);
				System.out.println("Number of actors : " + canvas.GetRenderer().GetViewProps().GetNumberOfItems());
		}
	}
    public static void main(String[] args) {
        try
		{
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		View canvas = new View();
		frame.add(canvas, BorderLayout.CENTER);
		vtkRenderer renderer = canvas.GetRenderer();
		int[] groups = new int[33];
			for(int i = 0 ; i < 33 ; ++i)
				groups[i] = i;
		AmibeToMesh reader = new AmibeToMesh(args[0], groups);	
		ViewableMesh rbh = new ViewableMesh(reader.getMesh());
		canvas.add(rbh);
		frame.setSize(800, 600);
		TestAmibe test = new TestAmibe(rbh);
		canvas.addKeyListener(test);
		//rbh.setViewMode(ViewableMesh.ViewMode.WIRED);
		test.canvas = canvas;
		//canvas.addKeyListener(test);
		//rbh.addSelectionListener(test);
		vtkInteractorStyleTrackballCamera style = new vtkInteractorStyleTrackballCamera();
		//canvas.getIren().SetPicker(new vtkAreaPicker());
		style.AutoAdjustCameraClippingRangeOn();
		canvas.getIren().SetInteractorStyle(style);
		renderer.ResetCamera();

			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
    }
}
