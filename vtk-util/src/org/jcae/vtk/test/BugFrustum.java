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

import java.util.Arrays;
import javax.swing.JFrame;
import vtk.vtkActor;
import vtk.vtkCanvas;
import vtk.vtkConeSource;
import vtk.vtkDataSetMapper;
import vtk.vtkDoubleArray;
import vtk.vtkExtractSelectedFrustum;
import vtk.vtkFloatArray;
import vtk.vtkPlanes;
import vtk.vtkPolyDataMapper;

public class BugFrustum {

	public static void main(String[] args)
	{		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		vtkCanvas canvas = new vtkCanvas();
		vtkExtractSelectedFrustum selector = new vtkExtractSelectedFrustum();
		
		// create sphere geometry
		vtkConeSource cone = new vtkConeSource();
		cone.SetHeight(3.0);
		cone.SetRadius(1.0);
		cone.SetResolution(10);
		// map to graphics objects
		vtkPolyDataMapper map = new vtkPolyDataMapper();
		map.SetInputConnection(cone.GetOutputPort());

		// actor coordinates geometry, properties, transformation
		vtkActor aSphere = new vtkActor();
		aSphere.SetMapper(map);
		aSphere.GetProperty().SetColor(0, 0, 1); // color blue
		aSphere.GetProperty().SetRepresentationToWireframe();
		canvas.GetRenderer().AddActor(aSphere);
		
		frame.add(canvas);
		frame.setSize(800, 600);
		frame.setVisible(true);
		
		selector.CreateFrustum(new double[] {
			0.,-10.,10.,1.0,
			0.,-10.,-10.,1.0,
			0.,10.,10.,1.0,
			0.,10.,-10.,1.0,
			10.,-10.,10.,1.0,
			10.,-10.,-10.,1.0,
			10.,10.,10.,1.0,
			10.,10.,-10.,1.0,
		});
		
		/*selector.CreateFrustum(new double[] {
			0.,-10.,-10.,1.0,
			0.,-10.,10.,1.0,
			0.,10.,-10.,1.0,
			0.,10.,10.,1.0,
			10.,-10.,-10.,1.0,
			10.,-10.,10.,1.0,
			10.,10.,-10.,1.0,
			10.,10.,10.,1.0,
		});*/
		
		//selector.SetContainingCells(1);
		
		vtkPlanes planes = selector.GetFrustum();
		vtkDoubleArray normals = (vtkDoubleArray)planes.GetNormals();
		System.out.println("Normals : " + Arrays.toString(normals.GetJavaArray()));
		System.out.println("Points : " + Arrays.toString(((vtkFloatArray)planes.GetPoints().GetData()).GetJavaArray()));
		System.out.println("EVALUATION : " +planes.FunctionValue(1001,11,11));
		System.out.println("planes." + planes.GetPlane(0).FunctionValue(-11,0,0));
		
		selector.SetInputConnection(cone.GetOutputPort());
		//UNCOMMENT TO CHECK THE BOUNDARIES
		//selector.ShowBoundsOn(); 
		// UNCOMMENT TO SEE THAT THE frustum extractor doesn't work inside and outside the boundaries
		//selector.InsideOutOn();
		//canvas.lock();
		selector.Update();
		//canvas.unlock();
		vtkActor actorFrustum = new vtkActor();
		vtkDataSetMapper mapFrustum = new vtkDataSetMapper();
		mapFrustum.SetInputConnection(selector.GetOutputPort());
		actorFrustum.SetMapper(mapFrustum);
		canvas.GetRenderer().AddActor(actorFrustum);

		canvas.lock();
		canvas.GetRenderer().ResetCamera();
		canvas.unlock();
		// COMMENT TO SEE THAT THE CONE IS IN THE BOUNDARIES
		canvas.GetRenderer().RemoveActor(aSphere); 
	}
}
