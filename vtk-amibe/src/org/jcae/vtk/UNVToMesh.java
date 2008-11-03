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

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 *
 * @author ibarz
 */
public class UNVToMesh
{
	private Mesh mesh;

	private static class GroupData extends LeafNode.DataProvider {
		
		GroupData(float[] nodes, int[] indices, int nbrOfCells)
		{
			setNodes(nodes);
			setPolys(nbrOfCells, indices);
		}
		
		@Override
		public float[] getNormals()
		{
			return null;
		}
		
		@Override
		public void load()
		{
			// Do nothing
		}

		@Override
		public void unLoad()
		{
			// Do nothing
		}
		
	}
	
	/**
	 * 
	 * @param filePath
	 * @param groupExtraction indicate wish groups will be extracted. if groupExtraction == Collections.EMPTY_LIST
	 * all the groups are extracted
	 */
	public UNVToMesh(String filePath, Collection<Integer> groupExtraction)
	{
		UNVParser parser = new UNVParser();
		try
		{
			parser.parse(new BufferedReader(new FileReader(filePath)));
		} catch (Exception e)
		{
			System.err.println(e.getMessage());
		}

		// If the set is empty it means that all the groups have to be extracted
		if(groupExtraction == Collections.EMPTY_LIST)
		{
			int nbOfGroups = parser.getGroupNames().length;
			groupExtraction = new ArrayList<Integer>(nbOfGroups);
			for(int i = 0 ; i < nbOfGroups ; ++i)
				groupExtraction.add(i);
		}
		
		float[] nodes = parser.getNodesCoordinates();
		mesh = new Mesh(groupExtraction.size());
		//mesh.setBeams(parser.getBeam2FromGroup(UNVProvider.OTHERS_GROUP));
		
		for(Integer id : groupExtraction)
		{
			int[] triangles =parser.getTria3FromGroup(id);
			int[] quads = parser.getQuad4FromGroup(id);
			
			int[] indices = new int[4 * (triangles.length / 3) +  5 * (quads.length / 4)];
			
			int offSet = 0;
			for(int i = 0 ; i < triangles.length ; )
			{
				indices[offSet++] = 3;
				indices[offSet++] = triangles[i++];
				indices[offSet++] = triangles[i++];
				indices[offSet++] = triangles[i++];
			}
			
			for(int i = 0 ; i < quads.length ; )
			{
				indices[offSet++] = 4;
				indices[offSet++] = triangles[i++];
				indices[offSet++] = triangles[i++];
				indices[offSet++] = triangles[i++];
				indices[offSet++] = triangles[i++];
			}
			
			GroupData groupData = new GroupData(nodes, indices, triangles.length / 3 + quads.length / 4);
			mesh.setGroup(id, groupData);
		}
	}

	public Mesh getMesh()
	{
		return mesh;
	}
}
