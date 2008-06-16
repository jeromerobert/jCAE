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
import javax.swing.JFrame;

import vtk.vtkInteractorStyleTrackballCamera;
import vtk.vtkRenderer;
import org.jcae.vtk.Utils;
import org.jcae.vtk.Canvas;
import org.jcae.vtk.ViewableCAD;
/**
 *
 * @author Jerome Robert
 */
public class TestCasacadeActor {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
	{
		Utils.loadVTKLibraries();
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Canvas canvas = new Canvas();		
		vtkRenderer renderer = canvas.GetRenderer();
		ViewableCAD viewable = new ViewableCAD(args[0]);
		viewable.addCanvas(canvas);
		renderer.SetBackground(1, 1, 1); // background color white
		vtkInteractorStyleTrackballCamera style = new vtkInteractorStyleTrackballCamera();		
		style.AutoAdjustCameraClippingRangeOn();
		canvas.getIren().SetInteractorStyle(style);
		renderer.ResetCamera();
		frame.add(canvas, BorderLayout.CENTER);
		frame.setVisible(true);
		frame.setSize(800,600);
	}
}
