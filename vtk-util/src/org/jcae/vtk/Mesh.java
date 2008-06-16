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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import vtk.vtkPolyData;

/**
 * Represent a mesh
 * @author Julian Ibarz
 */
public class Mesh
{
	/*private float[] nodes;
	private int[] beams;*/
	private HashMap<Integer, LeafNode.DataProvider> groups;
	
	int getNbOfGroups()
	{
		return groups.size();
	}

	Mesh(/*float[] nodes*/)
	{
		this(0);
	}
	
	Mesh(/*float[] nodes, */int nbOfGroups)
	{
		//this.nodes = nodes;
		groups = new HashMap<Integer, LeafNode.DataProvider>(nbOfGroups);
	}

	/**
	 * Construct a submesh of a mesh with the groups given in the tab
	 */
	Mesh(Mesh mesh, int[] extractedGroups)
	{
		groups = new HashMap<Integer, LeafNode.DataProvider>(extractedGroups.length);
		
		for(int id : extractedGroups)
		{
			groups.put(id, mesh.getGroup(id));
		}
	}

	/*public int[] getBeams()
	{
		return beams;
	}

	public void setBeams(int[] beams)
	{
		this.beams = beams;
	}*/
	
	/*float[] getNodes()
	{
		return this.nodes;
	}
	
	void setNodes(float[] nodes)
	{
		this.nodes = nodes;
	}*/
	
	LeafNode.DataProvider getGroup(int id)
	{
		return groups.get(id);
	}
	
	void setGroup(int id, LeafNode.DataProvider group)
	{
		groups.put(id, group);
	}
	
	Collection<LeafNode.DataProvider> getGroups()
	{
		return groups.values();
	}
	
	Set<Entry<Integer, LeafNode.DataProvider>> getGroupSet()
	{
		return groups.entrySet();
	}

	/*float[] getNode(int index)
	{
		float[] toReturn = new float[3];
		int indexNode = index * 3;

		toReturn[0] = nodes[indexNode++];
		toReturn[1] = nodes[indexNode++];
		toReturn[2] = nodes[indexNode];

		return toReturn;
	}*/

	/**
	 * Give the ID of the triangles which are in the group ID group
	 * @param group
	 * @return
	 */
	/*int[] getGroupTriangles(int group)
	{
		return groups.get(group).triangles;
	}

	int[] getTrianglesGroups(int[] groups)
	{
		// Get the different triangles by group
		ArrayList<int[]> trianglesByGroup = new ArrayList<int[]>(groups.length);
		int nbOfTriangles = 0;
		for (int group : groups)
		{
			trianglesByGroup.add(getGroupTriangles(group));
			nbOfTriangles += trianglesByGroup.get(trianglesByGroup.size() - 1).length;
		}

		int[] triangles = new int[nbOfTriangles];
		// Insert the different triangles in one array
		int offset = 0;
		for (int[] tri : trianglesByGroup)
		{
			System.arraycopy(tri, 0, triangles, offset, tri.length);
			offset += tri.length;
		}

		return triangles;
	}

	int getNbOfNodes()
	{
		return nodes.length / 3;
	}*/
}
