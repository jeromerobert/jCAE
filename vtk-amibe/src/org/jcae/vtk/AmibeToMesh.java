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
 * (C) Copyright 2009, by EADS France
 */

package org.jcae.vtk;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.map.hash.TIntIntHashMap;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.xmldata.AmibeReader;
import org.jcae.mesh.xmldata.AmibeReader.Group;
import org.jcae.mesh.xmldata.AmibeReader.SubMesh;
import org.xml.sax.SAXException;

/**
 *
 * @author Jerome Robert
 */
public class AmibeToMesh
{
	private final static Logger LOGGER=Logger.getLogger(AmibeToMesh.class.getName());

	private Map<String, LeafNode.DataProvider> triangles =
		new HashMap<String, LeafNode.DataProvider>();
	private Map<String, LeafNode.DataProvider> beams =
		new HashMap<String, LeafNode.DataProvider>();

	int triaCount = 0;
	
	public Map<String, LeafNode.DataProvider> getTriangles()
	{
		return Collections.unmodifiableMap(triangles);
	}

	public Map<String, LeafNode.DataProvider> getBeams()
	{
		return Collections.unmodifiableMap(beams);
	}
	/**
	 * Create the list of needed nodes for a triangle array
	 * @param trias the triangles which require nodes
	 * @return the nodes id
	 */
	private static int[] makeNodeIDArray(int[] ... ids)
	{
		int n = 0;
		for(int[] id:ids)
			n += id.length;
		TIntHashSet set = new TIntHashSet(n/2);
		for(int[] id:ids)
			set.addAll(id);
		TIntArrayList list = new TIntArrayList(set.size());
		list.add(set.toArray());
		list.sort();
		return list.toArray();
	}

	private static  void renumberArray(int[] newIndices, int[] ... arraysToRenumber)
	{
		TIntIntHashMap map = new TIntIntHashMap(newIndices.length);
		for (int i = 0; i < newIndices.length; i++)
			map.put(newIndices[i], i);
		for(int[] arrayToRenumber:arraysToRenumber)
			for (int i = 0; i < arrayToRenumber.length; i++)
				arrayToRenumber[i] = map.get(arrayToRenumber[i]);
	}

	/** Create an index array for triangles to be used as input from createCells */
	// copied from vtk-util to avoid loading VTK native libraries
	private static int[] createTriangleCells(int[] cells, int offsetID)
	{
		int k = 0;
		int nCell = cells.length / 3;
		int[] fCells = new int[nCell * 4];
		for (int i = 0; i < nCell * 3; i += 3)
		{
			fCells[k++] = 3;
			fCells[k++] = cells[i] + offsetID;
			fCells[k++] = cells[i + 1] + offsetID;
			fCells[k++] = cells[i + 2] + offsetID;
		}
		return fCells;
	}
	/**
	 * Create an index array for beams to be used as input from createCells
	 * change {a, b, c, d} to {2, a, b, 2, c, d}
	 */
	// copied from vtk-util to avoid loading VTK native libraries
	private static int[] createBeamCells(int[] beams)
	{
		int numberOfBeam = beams.length / 2;
		int k = 0;
		int j = 0;
		int[] fCells = new int[3 * numberOfBeam];
		for (int i = 0; i < numberOfBeam; i++)
		{
			fCells[k++] = 2;
			fCells[k++] = beams[j++];
			fCells[k++] = beams[j++];
		}
		return fCells;
	}
	private static class TriaData extends LeafNode.DataProvider
	{
		private final AmibeReader.Dim3 provider;
		private final String id;
		
		TriaData(AmibeReader.Dim3  provider, String id)
		{
			this.provider = provider;
			this.id = id;
		}

		@Override
		public void load()
		{
			try {
				SubMesh sm = provider.getSubmeshes().get(0);
				Group g = sm.getGroup(id);
				int[] triangles = g.readTria3();
				int[] nodesID = makeNodeIDArray(triangles);
				setNodes(sm.readNodes(nodesID));
				renumberArray(nodesID, triangles);
				setPolys(triangles.length/3, createTriangleCells(triangles, 0));
			} catch (IOException ex) {
				LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			}
		}
	}

	private static class BeamData extends LeafNode.DataProvider
	{
		private final AmibeReader.Dim3 provider;
		private final String id;

		BeamData(AmibeReader.Dim3  provider, String id)
		{
			this.provider = provider;
			this.id = id;
		}

		@Override
		public void load()
		{
			try {
				SubMesh sm = provider.getSubmeshes().get(0);
				Group g = sm.getGroup(id);
				int[] beams = g.readBeams();
				int[] nodesID = makeNodeIDArray(beams);
				setNodes(sm.readNodes(nodesID));
				renumberArray(nodesID, beams);
				setLines(createBeamCells(beams));
			} catch (IOException ex) {
				LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			}
		}
	}
	
	public AmibeToMesh(String filePath) throws SAXException, IOException
	{
		this(filePath, null);
	}

	public AmibeToMesh(String filePath, String[] groupExtraction)
		throws SAXException, IOException
	{		
		triaCount = 0;
		AmibeReader.Dim3 reader = new AmibeReader.Dim3(filePath);
		SubMesh sm = reader.getSubmeshes().get(0);
		List<Group> grps = sm.getGroups();
		if(groupExtraction == null)
		{			
			groupExtraction = new String[grps.size()];
			for(int i=0; i<groupExtraction.length; i++)
				groupExtraction[i]=grps.get(i).getName();
		}

		for(String id : groupExtraction)
		{
			Group g = sm.getGroup(id);
			if(g != null)
			{
				if(g.getNumberOfTrias() > 0)
				{
					triaCount += g.getNumberOfTrias();	
					triangles.put(id, new TriaData(reader, id));
				}
				if(g.getNumberOfBeams() > 0)
					beams.put(id, new BeamData(reader, id));
			}
		}
	}
	
	public int getNumberOfTriangles()
	{
		return triaCount;
	}
}