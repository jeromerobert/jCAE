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

import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
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

	private Map<String, LeafNode.DataProvider> nodes;

	public Map<String, LeafNode.DataProvider> getMesh()
	{
		return Collections.unmodifiableMap(nodes);
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
		return list.toNativeArray();
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
	
	private static class GroupData extends LeafNode.DataProvider
	{
		private final AmibeReader.Dim3 provider;
		private final String id;
		
		GroupData(AmibeReader.Dim3  provider, String id)
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
				int[] beams = g.readBeams();
				int[] nodesID = makeNodeIDArray(triangles, beams);
				setNodes(sm.readNodes(nodesID));
				renumberArray(nodesID, triangles, beams);
				setPolys(triangles.length/3, Utils.createTriangleCells(triangles, 0));
				setLines(Utils.createBeamCells(beams));
			} catch (IOException ex) {
				LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			}
		}
	}
	
	public AmibeToMesh(String filePath)
		throws ParserConfigurationException, SAXException, IOException
	{
		this(filePath, null);
	}

	public AmibeToMesh(String filePath, String[] groupExtraction)
		throws ParserConfigurationException, SAXException, IOException
	{		
		AmibeReader.Dim3 reader = new AmibeReader.Dim3(filePath);
		if(groupExtraction == null)
		{
			List<Group> grps = reader.getSubmeshes().get(0).getGroups();
			groupExtraction = new String[grps.size()];
			for(int i=0; i<groupExtraction.length; i++)
				groupExtraction[i]=grps.get(i).getName();
		}

		nodes = new HashMap<String, LeafNode.DataProvider>(groupExtraction.length);
		for(String id : groupExtraction)
			nodes.put(id, new GroupData(reader, id));
	}
}
