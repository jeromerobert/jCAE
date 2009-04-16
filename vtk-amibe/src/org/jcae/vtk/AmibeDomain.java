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

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Logger;
import org.jcae.mesh.xmldata.DoubleFileReader;
import org.jcae.mesh.xmldata.DoubleFileReaderByDirectBuffer;
import org.jcae.mesh.xmldata.IntFileReader;
import org.jcae.mesh.xmldata.IntFileReaderByDirectBuffer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A FEDomain which get data from the XML/binaries file of the amibe mesher
 * @author Jerome Robert
 * @todo implement
 */
public class AmibeDomain
{
	private final static Logger LOGGER=Logger.getLogger(AmibeDomain.class.getName());

	private final File directory;
	private final Document document;
	private final int id;
	private final float[] nodes;
	private final int[] tria3;

	/**
	 * @param directory 
	 * @param document
	 * @param id
	 * @throws IOException
	 */
	public AmibeDomain(File directory, Document document, int id) throws IOException
	{
		this.directory = directory;
		this.document = document;
		this.id = id;
		tria3 = readTria3();
		int[] nodesID = makeNodeIDArray(tria3);
		nodes = readNodes(nodesID);
		LOGGER.finest("number of nodes=" + nodes.length +
			", number of tria3=" + tria3.length / 3.0);
		renumberArray(tria3, nodesID);
	}

	private File getNodeFile()
	{
		Element xmlNodes = (Element) document.getElementsByTagName(
				"nodes").item(0);
		String a = ((Element) xmlNodes.getElementsByTagName("file").item(0)).getAttribute("location");
		return new File(directory, a);
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

	private File getTriaFile()
	{
		Element xmlNodes = (Element) document.getElementsByTagName(
				"triangles").item(0);
		Node fn = xmlNodes.getElementsByTagName("file").item(0);
		String a = ((Element) fn).getAttribute("location");
		return new File(directory, a);
	}

	/**
	 * @param xmlGroups 
	 * @param groupID 
	 * @return the xml element of DOM tree corresponding to the group.
	 */
	public static Element getXmlGroup(Element xmlGroups, int groupID)
	{
		NodeList list = xmlGroups.getElementsByTagName("group");
		int length = list.getLength();
		Element elt = null;
		int i = 0;
		boolean found = false;
		while (!found && i < length)
		{
			elt = (Element) list.item(i);
			int aId = -1;
			try
			{
				aId = Integer.parseInt(elt.getAttribute("id"));
			} catch (Exception e)
			{
				e.printStackTrace(System.out);
			}
			if (groupID == aId)
				found = true;
			else
				i++;
		}
		if (found)
			return elt;
		else
			return null;
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
		return set.toArray();
	}

	private float[] readNodes(int[] nodesID) throws IOException
	{
		File f = getNodeFile();
		DoubleFileReader dfr = new DoubleFileReaderByDirectBuffer(f);

		float[] toReturn = new float[nodesID.length * 3];

		for (int i = 0; i < nodesID.length; i++)
		{
			int ii = i * 3;
			int iid = nodesID[i] * 3;
			toReturn[ii] = (float) dfr.get(iid);
			toReturn[ii + 1] = (float) dfr.get(iid+1);
			toReturn[ii + 2] = (float) dfr.get(iid+2);
		}
		dfr.close();

		return toReturn;
	}

	private int[] readTria3() throws IOException
	{
		Element e = getXmlGroup((Element) document.getElementsByTagName("groups").item(0), id);
		Element numberNode = (Element) e.getElementsByTagName("number").item(0);
		String v = numberNode.getChildNodes().item(0).getNodeValue();
		int number = Integer.parseInt(v);
		if (number == 0)
			return new int[0];
		String groupFileN = ((Element) e.getElementsByTagName("file").item(0)).getAttribute("location");
		String os = ((Element) e.getElementsByTagName("file").item(0)).getAttribute("offset");
		File groupFile = new File(directory, groupFileN);
		int offset = Integer.parseInt(os);
		IntFileReader ifrG = new IntFileReaderByDirectBuffer(groupFile);

		File f = getTriaFile();
		IntFileReader ifrT = new IntFileReaderByDirectBuffer(f);

		int[] toReturn = new int[number * 3];

		for (int i = 0; i < number; i++)
			ifrT.get(ifrG.get(offset+i) * 3, toReturn, i * 3, 3);

		ifrG.close();
		ifrT.close();
		return toReturn;
	}

	private void renumberArray(int[] arrayToRenumber, int[] newIndices)
	{
		TIntIntHashMap map = new TIntIntHashMap(newIndices.length);
		for (int i = 0; i < newIndices.length; i++)
			map.put(newIndices[i], i);
		for (int i = 0; i < arrayToRenumber.length; i++)
			arrayToRenumber[i] = map.get(arrayToRenumber[i]);
	}

	/**
	 * Workaround for Bug ID4724038.
	 * see http://bugs.sun.com/bugdatabase/view_bug.do;:YfiG?bug_id=4724038
	 * 
	 * @param buffer 
	 */
	public static void clean(final MappedByteBuffer buffer)
	{
		try
		{
			Class cleanerClass = Class.forName("sun.misc.Cleaner");
			final Method cleanMethod = cleanerClass.getMethod("clean", null);
			AccessController.doPrivileged(new PrivilegedAction()
			{

				public Object run()
				{
					try
					{
						Method getCleanerMethod = buffer.getClass().getMethod(
								"cleaner", new Class[0]);

						getCleanerMethod.setAccessible(true);
						Object cleaner = getCleanerMethod.invoke(buffer, new Object[0]);
						if (cleaner != null)
							cleanMethod.invoke(cleaner, null);
					} catch (Exception e)
					{
						e.printStackTrace();
					}
					return null;
				}
			});
		} catch (ClassNotFoundException ex)
		{
			//Not a Sun JVM so we exit.
		} catch (SecurityException e)
		{
			e.printStackTrace();
		} catch (NoSuchMethodException e)
		{
			e.printStackTrace();
		}
	}
}
