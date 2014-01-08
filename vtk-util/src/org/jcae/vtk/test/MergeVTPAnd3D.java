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

import gnu.trove.list.array.TFloatArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import org.jcae.vtk.Utils;
import vtk.vtkFloatArray;
import vtk.vtkPointData;
import vtk.vtkPolyData;
import vtk.vtkXMLPolyDataReader;
import vtk.vtkXMLPolyDataWriter;

/**
 *
 * @author Julian Ibarz
 */
public class MergeVTPAnd3D
{
	static {
		Utils.loadVTKLibraries();
	}
	
	vtkPolyData data;
	String[] labels;
	ArrayList< TFloatArrayList > scalars = new ArrayList< TFloatArrayList >();
	
	public MergeVTPAnd3D(String vtpInputPath, String threeDInputPath, String vtpOutputPath)
	{
		loadVTP(vtpInputPath);
		loadThree(threeDInputPath);
		write(vtpOutputPath);
	}
	
	private void loadVTP(String path)
	{
		vtkXMLPolyDataReader reader = new vtkXMLPolyDataReader();
		reader.SetFileName(path);
		reader.Update();
		data = reader.GetOutput();
	}

	private void loadThree(String path)
	{
		try
		{
			BufferedReader input = new BufferedReader(new FileReader(path));
			try
			{
				String line = input.readLine();

				if(line == null)
					throw new IllegalArgumentException("The have to contains one line");
				
				// The first line begin with LABEL X Y Z ...
				String[] firstLine = line.split("\\s+");
				// If there is a white space before label, remove it
				if(firstLine[0].isEmpty())
					firstLine = Arrays.copyOfRange(firstLine, 1, firstLine.length);
				
				int beginIndex = Arrays.asList(firstLine).indexOf("Z")+1;
				//System.out.println("DEBUG firstLine : " + Arrays.toString(firstLine));
				if(beginIndex == -1)
				{
					throw new IllegalArgumentException("The .3d file labels must start with LABEL X Y Z or X Y Z...");
				}
				
				// Extract labels
				labels = Arrays.copyOfRange(firstLine, beginIndex, firstLine.length);
				
				// Prepare scalars
				scalars.ensureCapacity(firstLine.length - beginIndex);
				for(int i = beginIndex ; i < firstLine.length ; ++i)
					scalars.add(new TFloatArrayList(data.GetPoints().GetNumberOfPoints()));
				
				// Extract datas
				while((line = input.readLine()) != null)
				{
					String[] stringData = line.split("\\s+");
					
					for(int i = 0 ; i < scalars.size() ; ++i)
					{
						scalars.get(i).add(Float.parseFloat(stringData[i + beginIndex]));
					}
				}
				
				// For debugging
				/*for(TFloatArrayList array : scalars)
				{
					System.out.println("DEBUG : " + array);
				}*/
			} finally
			{
				input.close();
			}
		} catch (IOException ex)
		{
			ex.printStackTrace();
		}
	}
	
	
	private void write(String path)
	{
		// Merge the datas
		vtkPointData pointData = data.GetPointData();
		
		for(int i = 0 ; i < labels.length ; ++i)
		{
			vtkFloatArray scalar = new vtkFloatArray();
			scalar.SetName(labels[i]);
			scalar.SetNumberOfComponents(1);
			scalar.SetJavaArray(scalars.get(i).toArray());
			
			pointData.AddArray(scalar);
		}		
		
		// Write to file
		vtkXMLPolyDataWriter writer = new vtkXMLPolyDataWriter();
		writer.SetFileName(path);
		writer.SetDataModeToAscii();
		writer.SetInputData(data);
		writer.Write();
	}
	
	public static void main(String[] args)
	{
		if(args.length != 3)
		{
			System.err.println("Error, the program need three arguments :");
			System.err.println("1 - .vtp file path output ;");
			System.err.println("2 - .3d file path input ;");
			System.err.println("3 - .vtp file path output");
			return;
		}
		
		new MergeVTPAnd3D(args[0], args[1], args[2]);		
	}
}
