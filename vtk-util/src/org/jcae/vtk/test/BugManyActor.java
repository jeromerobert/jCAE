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

import java.awt.Color;
import org.jcae.vtk.Utils;
import vtk.*;

/** Very slow with cvs head, ok with paraview 3.2.1 */
public class BugManyActor
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

	private static void addDummyCones(int n, int m, vtkRenderer renderer)
	{
		float[] points =
		{
			0.f, 0.f, 0.f, // A - 0
			1.f, 0.f, 0.f, // B - 1
			0.f, 1.f, 0.f, // C - 2

		};

		int[] cells =
		{
			3, 0, 1, 2, // ABD

		};

		vtkPolyData data = new vtkPolyData();
		data.SetPoints(Utils.createPoints(points));
		data.SetPolys(Utils.createCells(1, cells));
		vtkPolyDataMapper map = new vtkPolyDataMapper();
		
		/*vtkScalarsToColorsPainter color = new vtkScalarsToColorsPainter();
		vtkClipPlanesPainter clip = new vtkClipPlanesPainter();
		vtkDisplayListPainter list = new vtkDisplayListPainter();
		vtkCoincidentTopologyResolutionPainter topo = new vtkCoincidentTopologyResolutionPainter();
		vtkLightingPainter light = new vtkLightingPainter();
		vtkRepresentationPainter representation = new vtkRepresentationPainter();
		color.SetDelegatePainter(clip);
		clip.SetDelegatePainter(list);
		topo.SetDelegatePainter(light);
		light.SetDelegatePainter(representation);
		color.Modified();*/
		
		map.SetInput(data);
		for (int i = 0; i < n; i++)
			for (int j = 0; j < m; j++)
			{
				vtkActor actor = new vtkActor();
				actor.SetPosition(4 * i, 4 * j, 0);
				actor.SetMapper(map);
				if( i % 2 == 0)
				{
					actor.GetProperty().SetColor(1, 0, 0);
					vtkProperty property = actor.MakeProperty();
					Utils.vtkPropertySetColor(property, Color.BLUE);
					actor.SetBackfaceProperty(property);
				}
				renderer.AddActor(actor);
			}
	}

	public static void main(String[] args) throws Exception
	{
		System.loadLibrary("vtkRenderingJava");

		vtkRenderer ren1 = new vtkRenderer();
		addDummyCones(10, 10, ren1);

		vtkRenderWindow renWin = new vtkRenderWindow();
		renWin.AddRenderer(ren1);
		ren1.ResetCamera();

		vtkRenderWindowInteractor iren = new vtkRenderWindowInteractor();
		iren.SetRenderWindow(renWin);

		vtkInteractorStyleTrackballCamera style =
				new vtkInteractorStyleTrackballCamera();
		iren.SetInteractorStyle(style);
		iren.Initialize();
		iren.Start();
	}
}
