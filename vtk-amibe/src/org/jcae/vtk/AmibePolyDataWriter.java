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

import gnu.trove.TIntArrayList;
import java.io.IOException;
import org.jcae.mesh.xmldata.AmibeWriter;
import vtk.vtkDataArray;
import vtk.vtkIdTypeArray;
import vtk.vtkIntArray;
import vtk.vtkNativeLibrary;
import vtk.vtkPolyData;


/**
 *
 * Save a vtkPolyData mesh as an Amibe mesh
 * @author Jerome Robert
 */
public class AmibePolyDataWriter
{
    static
    {
		vtkNativeLibrary.LoadNativeLibraries(vtkNativeLibrary.IO,
			vtkNativeLibrary.FILTERING, vtkNativeLibrary.COMMON);
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

    private void convertGroupData(AmibeWriter.Dim3 out) throws IOException
    {
		int nbBeams = polyData.GetNumberOfLines();
		vtkDataArray array = polyData.GetCellData().GetArray("Groups");
		TIntArrayList[] eGroups = new TIntArrayList[groupNames.length];
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
					out.nextGroup(groupNames[i]);
					for(int j:eGroups[i].toNativeArray())
					{
						if(j < nbBeams)
							out.addBeamToGroup(j);
						else
							out.addTriaToGroup(j-nbBeams);
					}
				}
			}
		}

		array = polyData.GetPointData().GetArray("Groups");
		for(TIntArrayList t:eGroups)
			t.clear();
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
					out.nextNodeGroup(groupNames[i]);
					for(int j:eGroups[i].toNativeArray())
						out.addNodeToGroup(j);
				}
			}
		}
    }
}
