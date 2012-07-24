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

import gnu.trove.TIntIntHashMap;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Convert Hypermesh .hmascii file to Amibe
 * @author Jerome Robert
 */
public class HM2Amibe {
	private final String outputDir;
	private AmibeWriter.Dim3 out;
	private final static Pattern TOKENIZE = Pattern.compile("[\\(,]");
	private TIntIntHashMap nodesMap = new TIntIntHashMap();
	public HM2Amibe(String outputDir) {
		this.outputDir = outputDir;
	}

	public void read(String fileName) throws IOException
	{
		BufferedReader in = new BufferedReader(new FileReader(fileName));
		read(in);
		in.close();
	}

	private void parseNodes(BufferedReader in) throws IOException
	{
		String line = in.readLine();
		while(line != null && !"BEGIN NODES".equals(line))
			line = in.readLine();
		line = in.readLine();
		int amibeID = 0;
		while(line != null && !"END NODES".equals(line))
		{
			String[] tokens = TOKENIZE.split(line);
			int id = Integer.parseInt(tokens[1]);
			double x = Double.parseDouble(tokens[2]);
			double y = Double.parseDouble(tokens[3]);
			double z = Double.parseDouble(tokens[4]);
			out.addNode(x, y, z);
			nodesMap.put(id, amibeID++);
			line = in.readLine();
		}
	}

	private void parseElements(BufferedReader in) throws IOException
	{
		String line = in.readLine();
		while(line != null && !"BEGIN COMPONENTS".equals(line))
			line = in.readLine();
		line = in.readLine();
		int[] ids = new int[3];
		while(line != null && !"END COMPONENTS".equals(line))
		{
			String[] tokens = TOKENIZE.split(line);
			if("*tria3".equals(tokens[0]))
			{
				for(int i = 0; i < 3; i++)
					ids[i] = nodesMap.get(Integer.parseInt(tokens[3+i]));
				out.addTriangle(ids);
			}
			else if("*rod".equals(tokens[0]))
			{
				int id1 = nodesMap.get(Integer.parseInt(tokens[3]));
				int id2 = nodesMap.get(Integer.parseInt(tokens[4]));
				out.addBeam(id1, id2);
			}
			line = in.readLine();
		}
	}


	public void read(BufferedReader in) throws IOException
	{
		out = new AmibeWriter.Dim3(outputDir);
		out.setFixNoGroup(true);
		parseNodes(in);
		parseElements(in);
		out.finish();
	}
}
