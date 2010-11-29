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
import org.jcae.vtk.Canvas;
import org.jcae.vtk.Utils;
import vtk.vtkActor;
import vtk.vtkConeSource;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;

/**
 *
 * @author ibarz
 */
public class BugMemory
{
	static vtkConeSource cone;
	static vtkPolyDataMapper map;
	public static  void test1()
	{
			// create sphere geometry
			cone = new vtkConeSource();
			cone.SetHeight(3.0);
			cone.SetRadius(1.0);
			cone.SetResolution(50000);
			cone.Update();
			vtkPolyData data = cone.GetOutput();
	}
	
	public static void test2()
	{
		// map to graphics objects
			map = new vtkPolyDataMapper();
			map.SetInput(cone.GetOutput());

			map.Update();
	}
	
	public static void main(String[] args)
	{
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		Canvas canvas = new Canvas();
		frame.setSize(600, 400);
		frame.add(canvas, BorderLayout.CENTER);

		canvas.GetRenderer().AddActor(Utils.createDummyActor());
		canvas.RenderSecured();

		int i = 0;
		vtkActor last = null;
		while (true)
		{
			++i;
			test1();
			test2();
			// actor coordinates geometry, properties, transformation
			vtkActor aSphere = new vtkActor();
			aSphere.SetMapper(map);
			aSphere.GetProperty().SetColor(0, 0, 1); // color blue
			if(i % 1000 == 0)
			{
				System.out.println("GC ! ");
				//System.gc();
			}
			/*canvas.GetRenderer().AddActor(aSphere);
			canvas.GetRenderer().RenderSecured();
			if(last != null)
				canvas.GetRenderer().RemoveActor(last);
			last = aSphere;*/
			
			/*if(i % 1000 == 0)			
			for(Object o:vtkGlobalJavaHash.PointerToReference.values())
			{
				WeakReference wr = (WeakReference) o;
				vtkObjectBase ob = (vtkObjectBase) wr.get();
				if(ob != null && ob.GetReferenceCount() <= 1)
				{
					System.out.println("coucou la voila");
				}
			}*/
		}
	}
}
