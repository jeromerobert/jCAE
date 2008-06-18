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

import org.jcae.vtk.BillBoard;
import java.util.Arrays;
import org.jcae.vtk.Utils;
import vtk.vtkCoordinate;
import vtk.vtkFloatArray;
import vtk.vtkImageData;
import vtk.vtkInteractorStyleTrackballCamera;
import vtk.vtkPNGReader;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper2D;
import vtk.vtkRenderWindow;
import vtk.vtkRenderWindowInteractor;
import vtk.vtkRenderer;
import vtk.vtkTexture;
import vtk.vtkTexturedActor2D;

/**
 *
 * @author ibarz
 */
public class TestBillBoard
{

	public static void main(String[] args)
	{
		Utils.loadVTKLibraries();
		///home/ibarz/test_projection.png
		///home/ibarz/zebra/core/src/org/zebra/core/tag/tagError8.png
		vtkPNGReader reader = new vtkPNGReader();
		reader.SetFileName(args[0]);
		vtkImageData image = reader.GetOutput();
		vtkTexture texture = new vtkTexture();
		texture.SetInput(image);
		image.Update();
		int[] dimensions = image.GetDimensions();
		
		vtkRenderer ren1 = new vtkRenderer();

		int c = 3;
		int l = 30;
		for (int i = 0; i < c; ++i)
			for (int j = 0; j < l; ++j)
			{
				BillBoard tag = new BillBoard(texture, dimensions[0], dimensions[1]);
				tag.setPosition(i, j, 0);

				ren1.AddViewProp(tag.getActor());
			}
		vtkRenderWindow renWin = new vtkRenderWindow();
		renWin.AddRenderer(ren1);
		ren1.GetActiveCamera().SetPosition(c/2, l/2, Math.max(c, l)*2);
		ren1.GetActiveCamera().SetFocalPoint(c/2, l/2, 0.);

		vtkRenderWindowInteractor iren = new vtkRenderWindowInteractor();
		iren.SetRenderWindow(renWin);

		vtkInteractorStyleTrackballCamera style =
				new vtkInteractorStyleTrackballCamera();
		iren.SetInteractorStyle(style);
		iren.Initialize();
		iren.Start();
		
	}
}
