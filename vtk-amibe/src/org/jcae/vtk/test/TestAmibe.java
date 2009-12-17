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
import java.util.Map;
import javax.swing.JFrame;
import org.jcae.vtk.AmibeToMesh;
import org.jcae.vtk.Canvas;
import org.jcae.vtk.SelectionListener;
import org.jcae.vtk.View;
import org.jcae.vtk.Viewable;
import org.jcae.vtk.LeafNode;
import org.jcae.vtk.ViewableMesh;
import vtk.vtkInteractorStyleTrackballCamera;
import vtk.vtkRenderer;

/**
 *
 * @author Julian Ibarz
 */
public class TestAmibe extends ViewableMesh implements SelectionListener, KeyListener
{
	public Canvas canvas;

	public TestAmibe(Map<String, LeafNode.DataProvider> mesh) {
		super(mesh);
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
				switch(getSelectionType())
				{
					case NODE:
						setSelectionType(SelectionType.CELL);
						break;
					case CELL:
						setSelectionType(SelectionType.POINT);
						canvas.lock();
						System.out.println("Capabilities : " + canvas.GetRenderWindow().ReportCapabilities());
						canvas.unlock();
						break;
					case POINT:
						setSelectionType(SelectionType.NODE);
						break;
				}
				break;

			case KeyEvent.VK_M:
				switch(getViewMode())
				{
					case WIRED:
						setViewMode(ViewMode.FILLED);
						break;
					case FILLED:
						setViewMode(ViewMode.WIRED);
						break;
				}
				break;

			case KeyEvent.VK_E:
				// Check the number of actors
				int nbrActor = canvas.GetRenderer().GetNumberOfPropsRendered();
				System.out.println("Number of actors rendered : " + nbrActor);
				System.out.println("Number of actors : " + canvas.GetRenderer().GetViewProps().GetNumberOfItems());
				break;
		}
	}
	public static void main(String[] args)
	{
	try
		{
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		View canvas = new View();
		frame.add(canvas, BorderLayout.CENTER);
		vtkRenderer renderer = canvas.GetRenderer();
		AmibeToMesh reader = new AmibeToMesh(args[0]);
		TestAmibe test = new TestAmibe(reader.getMesh());
		canvas.add(test);
		frame.setSize(800, 600);
		frame.setVisible(true);
		canvas.addKeyListener(test);
		test.canvas = canvas;
		//test.addSelectionListener(test);
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
