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
import java.awt.Dimension;
import javax.swing.JFrame;
import org.jcae.vtk.Canvas;
import org.jcae.vtk.Utils;
import vtk.vtkActor;
import vtk.vtkAssemblyPath;
import vtk.vtkGenericRenderWindowInteractor;
import vtk.vtkInteractorStyle;
import vtk.vtkInteractorStyleTrackballCamera;
import vtk.vtkPropPicker;
import vtk.vtkRenderer;

/**
 *
 * @author Jerome Robert
 */
public class TestPropPicker {
	private static class EventHandler
	{
		private vtkGenericRenderWindowInteractor iren;
		private vtkPropPicker picker = new vtkPropPicker();
		private Canvas canvas;
		
		private EventHandler(Canvas canvas)
		{
			this.canvas = canvas;
			this.iren = canvas.getIren();			
			this.iren.SetPicker(picker);
		}
		
		public void leftButtonPressEvent()
		{
			Thread.dumpStack();
			int[] e = iren.GetEventPosition();						
			//The picking is slow, might be related to the bug of performance
			//when there is a lot of actors
			if(picker.Pick(new double[]{e[0], e[1], 0}, canvas.GetRenderer())!=0)				
			{
				vtkAssemblyPath path = picker.GetPath();
				if(path!=null)
				{
					//Draw a box around the actor
					vtkInteractorStyle style = (vtkInteractorStyle) iren.GetInteractorStyle();
					style.HighlightProp(path.GetLastNode().GetViewProp());					
					//change the color of the actor
					vtkActor a=(vtkActor)path.GetLastNode().GetViewProp();						
					a.GetProperty().SetColor(1,0,0);
					canvas.RenderSecured();
				}
			}
		}		
	}
	
    public static void main(String[] args)
	{
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		final Canvas canvas = new Canvas()
		{
			/** Workaround for http://www.vtk.org/Bug/view.php?id=6268 */
			@Override
			public void setSize(int x, int y) {
				super.setSize(x, y);
				Lock();
				rw.SetSize(x,y);
				iren.SetSize(x, y);
				iren.ConfigureEvent();
				UnLock();
			}
		};
		final vtkRenderer renderer = canvas.GetRenderer();
		renderer.AddActor(Utils.createDummyAssembly(4, 4));
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
		EventHandler eventHandler = new EventHandler(canvas);
		canvas.getIren().AddObserver("LeftButtonPressEvent", eventHandler, "leftButtonPressEvent");
	}
}
