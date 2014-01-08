/*
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
 * (C) Copyright 2012, by EADS France
 */

package org.jcae.vtk;

import gnu.trove.list.array.TIntArrayList;
import java.io.IOException;
import org.jcae.mesh.xmldata.AmibeWriter;
import vtk.vtkDataArray;
import vtk.vtkIdTypeArray;
import vtk.vtkIntArray;
import vtk.vtkNativeLibrary;
import vtk.vtkPolyData;
import vtk.vtkXMLPolyDataReader;


/**
 *
 * Save a vtkPolyData mesh as an Amibe mesh
 * @author Jerome Robert
 */
public class AmibePolyDataWriter
{
    static
    {
		vtkNativeLibrary.LoadNativeLibraries(vtkNativeLibrary.VTKCOMMONDATAMODEL);
    }

	private final vtkPolyData polyData;
	private final String[] groupNames;

	public AmibePolyDataWriter(vtkPolyData polyData, String[] groupNames)
	{
		this.polyData = polyData;
		this.groupNames = groupNames;
	}

    public void write(String outDir) throws IOException
    {
		// Amibe writer object for 3D elements
		AmibeWriter.Dim3 out = new AmibeWriter.Dim3(outDir);
		out.setFixNoGroup(true);
		//Read Point Data
		int n = polyData.GetNumberOfPoints();
		for (int i = 0; i < n; i++)
			out.addNode(polyData.GetPoint(i));

		//Beams
		vtkIdTypeArray lineIdArray = polyData.GetLines().GetData();
		n = lineIdArray.GetSize();
		for (int i = 0; i < n; i += 3)
			out.addBeam(lineIdArray.GetValue(i + 1), lineIdArray.GetValue(i + 2));

		//Triangles
		polyData.GetPolys().Squeeze();
		vtkIdTypeArray polyIdArray = polyData.GetPolys().GetData();
		n = polyIdArray.GetSize();
		for (int j = 0; j < n; j += 4)
		{
			if (polyIdArray.GetValue(j) != 3)
				throw new IllegalArgumentException(
					"Expected cell type 3, found " + polyIdArray.GetValue(j));

			out.addTriangle(polyIdArray.GetValue(j + 1),
				polyIdArray.GetValue(j + 2), polyIdArray.GetValue(j + 3));
		}

		convertGroupData(out);
		out.finish();
    }

	/** Create group names if they are not provided */
	private String[] createGroupNames(vtkDataArray triGrps, vtkDataArray nodeGrps)
	{
		int nbGroups = 0;
		if(triGrps instanceof vtkIntArray)
			nbGroups = ((vtkIntArray)triGrps).GetValueRange()[1];
		if(nodeGrps instanceof vtkIntArray)
			nbGroups = Math.max(((vtkIntArray)nodeGrps).GetValueRange()[1], nbGroups);
		String[] toReturn;
		if(groupNames == null)
		{
			toReturn = new String[nbGroups];
			for(int i = 0; i < nbGroups; i++)
				toReturn[i] = Integer.toString(i);
		}
		else
			toReturn = groupNames;
		if(toReturn.length < nbGroups)
			throw new IllegalArgumentException(
				"Invalid group names. Expected size is "+nbGroups+" will provided is "+toReturn.length);
		return toReturn;
	}

    private void convertGroupData(AmibeWriter.Dim3 out) throws IOException
    {
		int nbBeams = polyData.GetNumberOfLines();
		vtkDataArray array = polyData.GetCellData().GetArray("Groups");
		vtkDataArray nArray = polyData.GetPointData().GetArray("Groups");
		String[] lGrpNames = createGroupNames(array, nArray);
		TIntArrayList[] eGroups = new TIntArrayList[lGrpNames.length];
		for(int i = 0; i < eGroups.length; i++)
			eGroups[i] = new TIntArrayList();
		if(array instanceof vtkIntArray)
		{
			int[] groups = ((vtkIntArray)array).GetJavaArray();
			for(int i = 0; i < groups.length; i++)
			{
				if(groups[i] > 0)
					eGroups[groups[i]-1].add(i);
			}
			for(int i = 0; i < eGroups.length; i++)
			{
				if(!eGroups[i].isEmpty())
				{
					out.nextGroup(lGrpNames[i]);
					for(int j:eGroups[i].toArray())
					{
						if(j < nbBeams)
							out.addBeamToGroup(j);
						else
							out.addTriaToGroup(j-nbBeams);
					}
				}
			}
		}

		for(TIntArrayList t:eGroups)
			t.clear();
		if(nArray instanceof vtkIntArray)
		{
			int[] groups = ((vtkIntArray)nArray).GetJavaArray();
			for(int i = 0; i < groups.length; i++)
			{
				if(groups[i] > 0)
					eGroups[groups[i]-1].add(i);
			}
			for(int i = 0; i < eGroups.length; i++)
			{
				if(!eGroups[i].isEmpty())
				{
					out.nextNodeGroup(lGrpNames[i]);
					for(int j:eGroups[i].toArray())
						out.addNodeToGroup(j);
				}
			}
		}
    }

	/** Shortcut to convert a VTP file to an Amibe directory */
	public static void vtpToAmibe(String inputFile, String outputDir) throws IOException
	{
		vtkXMLPolyDataReader reader = new vtkXMLPolyDataReader();
		reader.SetFileName(inputFile);
		reader.Update();
		new AmibePolyDataWriter(reader.GetOutput(), null).write(outputDir);
	}
}
