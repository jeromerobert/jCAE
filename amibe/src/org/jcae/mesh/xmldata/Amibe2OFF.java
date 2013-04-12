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

package org.jcae.mesh.xmldata;

import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.map.hash.TIntIntHashMap;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.xmldata.AmibeReader.Group;
import org.jcae.mesh.xmldata.AmibeReader.SubMesh;
import org.xml.sax.SAXException;

/**
 * Amibe to Geomview Object File Format (.off).
 * http://people.sc.fsu.edu/~jburkardt/data/off/off.html
 * @author Jerome Robert
 */
public class Amibe2OFF {
	private final AmibeReader.Dim3 amibeReader;

	public Amibe2OFF(String directory) throws SAXException, IOException {
		amibeReader = new AmibeReader.Dim3(directory);
	}
	public Amibe2OFF(AmibeReader.Dim3  amibeReader) throws SAXException, IOException {
		this.amibeReader = amibeReader;
	}

	public void write(PrintStream out, String groupName) throws SAXException, IOException
	{
		SubMesh sm = amibeReader.getSubmeshes().get(0);
		Group group = sm.getGroup(groupName);
		out.println("OFF");
		int[] trias = group.readTria3();
		int[] nodesIds = new TIntHashSet(trias).toArray();
		Arrays.sort(nodesIds);
		out.println(nodesIds.length+" "+group.getNumberOfTrias());
		TIntIntHashMap nodeMap = new TIntIntHashMap(nodesIds.length);
		DoubleFileReader nodes = sm.getNodes();
		double[] coords = new double[3];
		int k = 0;
		for(int id:nodesIds)
		{
			nodes.get(3*id, coords);
			out.println(coords[0]+" "+coords[1]+" "+coords[2]);
			nodeMap.put(id, k++);
		}
		nodesIds = null;
		nodes.close();
		for(int i = 0; i < trias.length / 3; i++)
		{
			out.println("3 " +
				nodeMap.get(trias[3 * i]) + " " +
				nodeMap.get(trias[3 * i + 1]) + " " +
				nodeMap.get(trias[3 * i + 2]));
		}
	}

	public void writeAllGroups(String directory) throws IOException, SAXException
	{
		new File(directory).mkdirs();
		SubMesh sm = amibeReader.getSubmeshes().get(0);
		for(Group g:sm.getGroups())
			write(new File(directory, g.getName()+".off").getPath(), g.getName());
	}

	public void write(String fileName, String group) throws IOException, SAXException
	{
		PrintStream out = new PrintStream(fileName);
		write(out, group);
		out.close();
	}
	public void write(PrintStream out) throws SAXException, IOException
	{
		SubMesh sm = amibeReader.getSubmeshes().get(0);
		IntFileReader trias = sm.getTriangles();
		int[] buffer = new int[3];
		int realTriaNb = 0;
		for(int i = 0; i < sm.getNumberOfTrias(); i++)
		{
			trias.get(buffer);
			if(buffer[0] >= 0 && buffer[1] >=0 && buffer[2] >= 0)
				realTriaNb ++;
		}
		out.println("OFF");
		out.println(sm.getNumberOfNodes()+" "+realTriaNb);
		DoubleFileReader nodes = sm.getNodes();
		for(int i = 0; i< sm.getNumberOfNodes() ; i++)
			out.println(nodes.get()+" "+nodes.get()+" "+nodes.get());
		nodes.close();
		for(int i = 0; i < sm.getNumberOfTrias(); i++)
		{
			trias.get(buffer);
			if(buffer[0] >= 0 && buffer[1] >=0 && buffer[2] >= 0)
				out.println("3 "+buffer[0]+" "+buffer[1]+" "+buffer[2]);
		}
	}

	public void write(String fileName) throws IOException, SAXException
	{
		PrintStream out = new PrintStream(fileName);
		write(out);
		out.close();
	}
}
