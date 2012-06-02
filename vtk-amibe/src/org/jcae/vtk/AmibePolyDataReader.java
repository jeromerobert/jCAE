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
 * (C) Copyright 2012, by EADS France
 */


package org.jcae.vtk;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.xmldata.AmibeReader;
import org.jcae.mesh.xmldata.AmibeReader.Group;
import org.jcae.mesh.xmldata.DoubleFileReader;
import org.jcae.mesh.xmldata.IntFileReader;
import org.xml.sax.SAXException;
import vtk.vtkDoubleArray;
import vtk.vtkIntArray;
import vtk.vtkNativeLibrary;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkXMLPolyDataWriter;

/**
 * Read an Amibe mesh to a vtkPolyData object.
 * This class is incore and Amibe2VTP is out-of-ocre.
 * This class support node groups and Amibe2VTP currently doesn't.
 * @author Jerome Robert
 */
public class AmibePolyDataReader {
    static
    {
		vtkNativeLibrary.LoadNativeLibraries(vtkNativeLibrary.IO,
			vtkNativeLibrary.FILTERING, vtkNativeLibrary.COMMON);
    }
	private final vtkPolyData polyData = new vtkPolyData();
	private final String[] groupNames;
	public AmibePolyDataReader(String path) throws IOException, SAXException
	{
		AmibeReader.Dim3 amibeReader = new AmibeReader.Dim3(path);
		AmibeReader.SubMesh sm = amibeReader.getSubmeshes().get(0);
		DoubleFileReader nodes = sm.getNodes();
		double[] points = new double[sm.getNumberOfNodes()*3];
		nodes.get(points);
		nodes.close();
		vtkPoints vp = new vtkPoints();
		vp.SetDataTypeToDouble();
		((vtkDoubleArray) vp.GetData()).SetJavaArray(points);
		points = null;
		polyData.SetPoints(vp);
		if(sm.getNumberOfTrias() > 0)
		{
			IntFileReader triaReader = sm.getTriangles();
			int[] triangles = new int[sm.getNumberOfTrias()*3];
			triaReader.get(triangles);
			triaReader.close();
			polyData.SetPolys(Utils.createCells(
				sm.getNumberOfTrias(), Utils.createTriangleCells(triangles, 0)));
		}
		if(sm.getNumberOfBeams() > 0)
		{
			IntFileReader beamsReader = sm.getBeams();
			int[] beams = new int[sm.getNumberOfBeams()*2];
			beamsReader.get(beams);
			beamsReader.close();
			polyData.SetLines(Utils.createCells(
				sm.getNumberOfBeams(), Utils.createBeamCells(beams)));
		}

		int[] pointGroups = new int[sm.getNumberOfNodes()];
		int[] elementGroups = new int[sm.getNumberOfBeams() + sm.getNumberOfTrias()];
		int gid = 1;
		groupNames = new String[sm.getGroups().size()];
		for(Group g:sm.getGroups())
		{
			for(int bid:g.readBeamsIds())
				elementGroups[bid] = gid;
			for(int bid:g.readTria3Ids())
				elementGroups[bid+sm.getNumberOfBeams()] = gid;
			for(int id:g.readNodesIds())
				pointGroups[id] = gid;
			groupNames[gid - 1] = g.getName();
			gid ++;
		}
		vtkIntArray vElementGroups = new vtkIntArray();
		vElementGroups.SetName("Groups");
		vElementGroups.SetJavaArray(elementGroups);
		vtkIntArray vPointGroups = new vtkIntArray();
		vPointGroups.SetName("Groups");
		vPointGroups.SetJavaArray(pointGroups);
		polyData.GetCellData().AddArray(vElementGroups);
		polyData.GetPointData().AddArray(vPointGroups);
	}

	public vtkPolyData getPolyData()
	{
		return polyData;
	}

	public String[] getGroupNames()
	{
		return groupNames;
	}
}
