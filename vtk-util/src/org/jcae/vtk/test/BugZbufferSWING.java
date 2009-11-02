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

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import javax.swing.JFrame;
import org.jcae.vtk.Canvas;
import vtk.vtkActor;
import vtk.vtkConeSource;
import vtk.vtkExtractSelectedFrustum;
import vtk.vtkFloatArray;
import vtk.vtkInteractorStyleRubberBand3D;
import vtk.vtkPolyDataMapper;

public class BugZbufferSWING implements  MouseListener {
	@Override
	public void mouseClicked(MouseEvent e)
	{
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
		
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
	}

	@Override
	public void mousePressed(MouseEvent e)
	{		
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{	System.out.println("TEST !");
			vtkFloatArray beforeNative = new vtkFloatArray();
			canvas.GetRenderWindow().GetZbufferData(50, 50, 100, 100, beforeNative);
			float[] before = beforeNative.GetJavaArray();
			System.out.println("ZBUFFER : " + Arrays.toString(before));
	}
	public static Canvas canvas;
	public static void main(String[] args)
	{		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		canvas = new Canvas();
		vtkExtractSelectedFrustum selector = new vtkExtractSelectedFrustum();
		
		// create sphere geometry
		vtkConeSource cone = new vtkConeSource();
		cone.SetHeight(3.0);
		cone.SetRadius(1.0);
		cone.SetResolution(10);
		// map to graphics objects
		vtkPolyDataMapper map = new vtkPolyDataMapper();
		map.SetInput(cone.GetOutput());

		// actor coordinates geometry, properties, transformation
		vtkActor aSphere = new vtkActor();
		aSphere.SetMapper(map);
		aSphere.GetProperty().SetColor(0, 0, 1); // color blue
		canvas.GetRenderer().AddActor(aSphere);
		
		BugZbufferSWING test = new BugZbufferSWING();
		vtkInteractorStyleRubberBand3D rectangleStyle =
			new vtkInteractorStyleRubberBand3D();
		canvas.getIren().SetInteractorStyle(rectangleStyle);
		canvas.addMouseListener(test);
		frame.add(canvas);
		frame.setSize(800, 600);
		frame.setVisible(true);

	}
}
