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
 * (C) Copyright 2007, by EADS France
 */
package org.jcae.vtk;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import java.io.IOException;
import java.util.logging.Logger;
import org.jcae.mesh.xmldata.AmibeReader;
import org.jcae.mesh.xmldata.AmibeReader.SubMesh;

/**
 * A FEDomain which get data from the XML/binaries file of the amibe mesher
 * @author Jerome Robert
 * @todo implement
 */
public class AmibeDomain
{
	private final static Logger LOGGER=Logger.getLogger(AmibeDomain.class.getName());
	private final float[] nodes;
	private final int[] tria3;

	/**
	 * @param directory 
	 * @param document
	 * @param id
	 * @throws IOException
	 */
	public AmibeDomain(AmibeReader reader, String id) throws IOException
	{
		SubMesh sm = reader.getSubmeshes().get(0);
		tria3 = sm.getGroup(id).readTria3();
		int[] nodesID = makeNodeIDArray(tria3);
		nodes = sm.readNodes(nodesID);
		LOGGER.finest("number of nodes=" + nodes.length +
			", number of tria3=" + tria3.length / 3.0);
		renumberArray(tria3, nodesID);
	}

	public float[] getNodes()
	{
		return nodes;
	}

	public int getNumberOfNodes()
	{
		return nodes.length / 3;
	}

	public int getNumberOfTria3()
	{
		return tria3.length / 3;
	}

	public int[] getTria3()
	{
		return tria3;
	}

	public int[] getQuad4()
	{
		return new int[0];
	}

	/**
	 * Create the list of needed nodes for a triangle array
	 * @param trias the triangles which require nodes
	 * @return the nodes id
	 */
	private int[] makeNodeIDArray(int[] trias)
	{
		TIntHashSet set = new TIntHashSet(trias.length / 2);
		for (int index : trias)
			set.add(index);
		TIntArrayList list = new TIntArrayList(set.size());
		list.add(set.toArray());
		list.sort();
		return list.toNativeArray();
	}

	private void renumberArray(int[] arrayToRenumber, int[] newIndices)
	{
		TIntIntHashMap map = new TIntIntHashMap(newIndices.length);
		for (int i = 0; i < newIndices.length; i++)
			map.put(newIndices[i], i);
		for (int i = 0; i < arrayToRenumber.length; i++)
			arrayToRenumber[i] = map.get(arrayToRenumber[i]);
	}

}
