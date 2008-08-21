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

import javax.vecmath.Point3d;
import vtk.*;

/**
 * 
 * @author Julian Ibarz
 */
public class BenchMark
{

	/** Create a cone actor */
	/*private static vtkActor createDummyActor()
	{
		vtkConeSource cone = new vtkConeSource();
		cone.ReleaseDataFlagOn();
		cone.SetHeight(3.0);
		cone.SetRadius(1.0);
		cone.SetResolution(4);	*/

		
		/*System.out.println("nombre de cells : " + map.GetInput().GetNumberOfCells());
		System.out.println("nombre de polys : " + map.GetInput().GetNumberOfPolys());
		System.out.println("nombre de polys : " + map.GetInput().GetNumberOfStrips());
		vtkActor coneActor = new vtkActor();
		coneActor.SetMapper(map);
		return coneActor;
	}*/

	private static void addSpheres(int precision, int width, int height, vtkRenderer renderer)
	{
		vtkSphereSource source = new vtkSphereSource();
		source.SetRadius(1.);
		source.SetPhiResolution(precision);
		source.SetThetaResolution(precision);
		
		vtkPolyDataMapper mapper = new vtkPolyDataMapper();
		mapper.SetInput(source.GetOutput());
		
		for (int i = 0; i < width; i++)
			for (int j = 0; j < height; j++)
			{
				vtkActor actor = new vtkActor();
				actor.SetPosition(4 * i, 4 * j, 0);
				actor.SetMapper(mapper);
				renderer.AddActor(actor);
			}
	}
	
	public static void main(String[] args) throws Exception
	{
		// Load dynamic libraries
		System.loadLibrary("vtkRenderingJava");

		if(args.length != 3)
			throw new IllegalArgumentException("The program needs 3 arguments precision width height :" +
					"_ precision : number of split in horizontal & vertical for the sphere ;" +
					"_ width : number of sphere in horizontal " +
					"_ height : number of sphere in vertical.");
		
		int precision = Integer.parseInt("50");
		int width = Integer.parseInt("20");
		int height = Integer.parseInt("20");
		
		vtkRenderer renderer = new vtkRenderer();
		addSpheres(precision, width, height, renderer);

		vtkRenderWindow renWin = new vtkRenderWindow();
		renWin.AddRenderer(renderer);
		renderer.ResetCamera();

		vtkRenderWindowInteractor iren = new vtkRenderWindowInteractor();
		iren.SetRenderWindow(renWin);

		vtkInteractorStyleTrackballCamera style =
				new vtkInteractorStyleTrackballCamera();
		// Do not adjust the camera clipping plane, this is "buggy" when rendering not automatically
		style.AutoAdjustCameraClippingRangeOff();
		
		iren.SetInteractorStyle(style);
		iren.Initialize();
		
		// First render that compile the display list, etc.
		int nbrOfRender = 360 / 4;
		long renderTime = 0;
		vtkCamera camera = renderer.GetActiveCamera();
		camera.SetClippingRange(1., 1000.);
		double[] pos = camera.GetPosition();
		double[] focal = camera.GetFocalPoint();
		double distance = new Point3d(pos).distance(new Point3d(focal));
		
		for(int i = 0 ; i < nbrOfRender ; ++i)
		{
			renderer.GetActiveCamera().SetPosition(
					distance * Math.cos(Math.toRadians(i * 4)) * Math.sin(Math.toRadians(i * 4)) + focal[0], 
					distance * Math.sin(Math.toRadians(i * 4)) * Math.sin(Math.toRadians(i * 4)) + focal[1],
					distance * Math.cos(Math.toRadians(i * 4)) + focal[2]);
			
			long begin = System.nanoTime();
			iren.Render();
			renderTime += System.nanoTime() - begin;
		}
	}
}
