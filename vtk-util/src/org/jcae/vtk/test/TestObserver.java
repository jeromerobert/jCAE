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

import org.jcae.vtk.Utils;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import vtk.vtkCanvas;
import vtk.vtkGenericRenderWindowInteractor;
import vtk.vtkInteractorStyleTrackballCamera;
import vtk.vtkRenderer;

/**
 *
 * @author Jerome Robert
 */
public class TestObserver {
	private static class EventHandler
	{
		private final vtkGenericRenderWindowInteractor iren;

		private EventHandler(vtkGenericRenderWindowInteractor iren) {
			this.iren = iren;
		}
		
		public void leftButtonPressEvent()
		{
			Thread.dumpStack();
			int[] p = iren.GetEventPosition();
			System.out.println(p[0]+", "+p[1]);
		}
		
		public void leftButtonReleaseEvent()
		{
			System.out.println("leftButtonReleaseEvent");
		}
	}

	public static void main(String[] args)
	{
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		final vtkCanvas canvas = new vtkCanvas();
		final vtkRenderer renderer = canvas.GetRenderer();
		renderer.AddActor(Utils.createDummyAssembly(8, 8));
		renderer.SetBackground(1, 1, 1); // background color white
		canvas.setMinimumSize(new Dimension(0, 0));
		canvas.setPreferredSize(new Dimension(0, 0));
		vtkInteractorStyleTrackballCamera style = new vtkInteractorStyleTrackballCamera();		
		style.AutoAdjustCameraClippingRangeOn();
		canvas.getIren().SetInteractorStyle(style);
		renderer.ResetCamera();
		frame.add(canvas, BorderLayout.CENTER);
		frame.setVisible(true);
		frame.setSize(800,600);
		EventHandler eventHandler = new EventHandler(canvas.getIren());
		canvas.getIren().AddObserver("LeftButtonPressEvent", eventHandler, "leftButtonPressEvent");
		canvas.getIren().AddObserver("LeftButtonReleaseEvent", eventHandler, "leftButtonReleaseEvent");
		canvas.getIren().InvokeEvent("LeftButtonReleaseEvent");
	}
}
