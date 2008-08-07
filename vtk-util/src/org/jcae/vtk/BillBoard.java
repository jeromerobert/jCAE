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
package org.jcae.vtk;


import vtk.vtkActor2D;
import vtk.vtkCoordinate;
import vtk.vtkFloatArray;
import vtk.vtkImageData;
import vtk.vtkPNGReader;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper2D;
import vtk.vtkTexture;
import vtk.vtkTexturedActor2D;

/**
 *
 * @author ibarz
 */
public class BillBoard
{
	vtkTexturedActor2D actor;
	vtkCoordinate pos;

	public BillBoard(String texturePath)
	{
		vtkPNGReader reader = new vtkPNGReader();
		reader.SetFileName(texturePath);
		vtkImageData image = reader.GetOutput();
		vtkTexture texture = new vtkTexture();
		texture.SetInput(image);
		image.Update();
		int[] dimensions = image.GetDimensions();

		construct(texture, dimensions[0], dimensions[1]);
	}

	public BillBoard(vtkTexture texture, int width, int height)
	{
		construct(texture, width, height);
	}

	private void construct(vtkTexture texture, int width, int height)
	{
		float[] points =
		{
			0.f, 0.f, 0.f, // A - 0
			width, 0.f, 0.f, // B - 1
			width, height, 0.f, // C - 2
			0.f, height, 0.f  // D - 3
		};

		int[] cells =
		{
			4, 0, 1, 2, 3 // ABCD
		};

		vtkPolyData data = new vtkPolyData();
		data.SetPoints(Utils.createPoints(points));
		data.SetPolys(Utils.createCells(1, cells));

		float[] tCoords =
		{
			0.f, 0.f, // A - 0
			1.f, 0.f, // B - 1
			1.f, 1.f, // C - 2
			0.f, 1.f,  // D - 3
		};

		vtkFloatArray nativeCoords = new vtkFloatArray();
		nativeCoords.SetJavaArray(tCoords);
		nativeCoords.SetNumberOfComponents(2);
		nativeCoords.SetName("TextureCoordinates");

		data.GetPointData().SetTCoords(nativeCoords);

		pos = new vtkCoordinate();
		pos.SetCoordinateSystemToWorld();
		vtkCoordinate coordView = new vtkCoordinate();
		coordView.SetReferenceCoordinate(pos);
		coordView.SetCoordinateSystemToViewport();

		vtkPolyDataMapper2D mapper = new vtkPolyDataMapper2D();
		mapper.SetInput(data);
		mapper.SetTransformCoordinate(coordView);

		actor = new vtkTexturedActor2D();
		actor.SetMapper(mapper);
		actor.SetTexture(texture);
	}

	public void setPosition(float x, float y, float z)
	{
		pos.SetValue(x, y, z);
	}

	public vtkTexturedActor2D getActor()
	{
		return actor;
	}
}

