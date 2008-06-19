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
import vtk.vtkActor;
import vtk.vtkPNGReader;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkTexture;
import vtk.vtkTextureMapToPlane;
import vtk.vtkTexturedSphereSource;
/**
 * This program is an example of a plane projection of a texture in a 3D object.
 * The first argument of the program is the path of the image
 * @author Julian Ibarz
 */
public class TestProjectionTexture {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
	{
		if(args.length != 1)
		{
			System.err.println("This application receive one argument : the path to the image that will be used for the texture");
			return;
		}
		
		Utils.loadVTKLibraries();
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Canvas canvas = new Canvas();		
		vtkRenderer renderer = canvas.GetRenderer();
			
		// create Sphere geometry
		vtkTexturedSphereSource sphere = new vtkTexturedSphereSource();
		sphere.SetRadius( 1.0 );
		sphere.SetThetaResolution(100);
		sphere.SetPhiResolution(100);
		
		// Open the image
		vtkPNGReader reader = new vtkPNGReader();
		reader.SetFileName(args[0]);

		// Define the projection plane 
		vtkTextureMapToPlane projection = new vtkTextureMapToPlane();
		projection.SetOrigin(-1., -1., 0.);
		// The point defining the x axe
		projection.SetPoint1(-1 + 2,-1,0);
		// The point defining the y axe
		projection.SetPoint2(-1,-1 + 2,0);
		
		// Apply the projection to the sphere
		projection.SetInput(sphere.GetOutput());
		projection.Update();
		
		// map to graphics objects
		vtkPolyDataMapper map = new vtkPolyDataMapper();
		// Take the result of the projection texturing
		map.SetInput((vtkPolyData)projection.GetOutput());

		vtkActor aSphere = new vtkActor();
		aSphere.SetMapper(map);
		// Create the texture
		vtkTexture texture = new vtkTexture();
		// Take the image for the texture input
		texture.SetInput(reader.GetOutput());
		// Apply to the actor
		aSphere.GetProperty().SetTexture("pwet", texture);
		
		renderer.AddActor(aSphere);
		
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