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
 * (C) Copyright 2011, by EADS France
 */

package org.jcae.mesh.xmldata;

import gnu.trove.list.array.TIntArrayList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.jcae.mesh.xmldata.AmibeWriter.Dim3;

/**
 *
 * @author Jerome Robert
 */
public class GMSH2Amibe {
	private final String outputDir;
	private Dim3 out;
	private final static Pattern TOKENIZE = Pattern.compile("(?!^)[\\s\"]+");
	private TIntArrayList[] triaGroups, beamGroups;
	public GMSH2Amibe(String outputDir) {
		this.outputDir = outputDir;
	}

	public void read(String fileName) throws IOException
	{
		BufferedReader in = new BufferedReader(new FileReader(fileName));
		read(in);
		in.close();
	}

	private String[] parsePhysicalNames(BufferedReader in) throws IOException
	{
		int nb = Integer.parseInt(in.readLine());
		String[] toReturn = new String[nb];
		for(int i = 0; i < nb; i++)
			toReturn[i] = TOKENIZE.split(in.readLine())[2];
		return toReturn;
	}

	private void parseNodes(BufferedReader in) throws IOException
	{
		int nb = Integer.parseInt(in.readLine());
		for(int i = 0; i < nb; i++)
		{
			String[] line = TOKENIZE.split(in.readLine());
			double x = Double.parseDouble(line[1]);
			double y = Double.parseDouble(line[2]);
			double z = Double.parseDouble(line[3]);
			out.addNode(x, y, z);
		}
	}

	private void parseElements(BufferedReader in, int nbGroups) throws IOException
	{
		int nb = Integer.parseInt(in.readLine());
		int beamCounter = 0;
		int triaCounter = 0;
		for(int i = 0; i < nb; i++)
		{
			String[] line = TOKENIZE.split(in.readLine());
			int nbTags = Integer.parseInt(line[2]);
			if("1".equals(line[1]))
			{
				out.addBeam(
					Integer.parseInt(line[3+nbTags])-1,
					Integer.parseInt(line[4+nbTags])-1);
				addElementToGroup(beamGroups, Integer.parseInt(line[3]), beamCounter++);
			}
			else if("2".equals(line[1]))
			{
				out.addTriangle(
					Integer.parseInt(line[3+nbTags])-1,
					Integer.parseInt(line[4+nbTags])-1,
					Integer.parseInt(line[5+nbTags])-1);
				addElementToGroup(triaGroups, Integer.parseInt(line[3]), triaCounter++);
			}
		}
	}

	private void writeGroups(String[] groupNames) throws IOException
	{
		for(int i = 0; i < groupNames.length; i++)
		{
			out.nextGroup(groupNames[i]);
			if(triaGroups[i] != null)
			{
				for(int e:triaGroups[i].toArray())
					out.addTriaToGroup(e);
			}
			if(beamGroups[i] != null)
			{
				for(int e:beamGroups[i].toArray())
					out.addBeamToGroup(e);
			}
		}
	}

	private void addElementToGroup(TIntArrayList[] groups, int groupID, int element)
	{
		groupID --;
		if(groups[groupID] == null)
			groups[groupID] = new TIntArrayList();
		groups[groupID].add(element);
	}

	public void read(BufferedReader in) throws IOException
	{
		out = new AmibeWriter.Dim3(outputDir);
		String[] groupNames = null;
		String blockName = in.readLine();
		if(blockName != null)
			blockName.trim();
		while(blockName != null)
		{
			if("$PhysicalNames".equals(blockName))
			{
				groupNames = parsePhysicalNames(in);
				in.readLine(); //$EndPhysicalNames
				triaGroups = new TIntArrayList[groupNames.length];
				beamGroups = new TIntArrayList[groupNames.length];
			}
			else if("$Nodes".equals(blockName))
			{
				parseNodes(in);
				in.readLine(); //$EndNodes
			}
			else if("$Elements".equals(blockName))
			{
				if(groupNames == null)
					throw new IOException("Block $PhysicalNames should be declared before block $Elements");
				parseElements(in, groupNames.length);
				in.readLine(); //$EndElements
			}
			else
				skipBlock(in);
			blockName = in.readLine();
			if(blockName != null)
				blockName.trim();
		}
		writeGroups(groupNames);
		out.finish();
	}

	private void skipBlock(BufferedReader in) throws IOException {
		String buffer = in.readLine();
		while(buffer != null)
		{
			if(buffer.startsWith("$"))
				return;
			buffer = in.readLine();
		}
	}

	public static void main(final String[] args) {
		try {
			String amibe = args[1]+".amibe";
			new GMSH2Amibe(amibe).read(args[0]);
			new Amibe2UNV(new File(amibe)).write(args[1]+".unv");
			new Amibe2VTK(new File(amibe)).write(args[1]+".vtp");
		} catch (Exception ex) {
			Logger.getLogger(GMSH2Amibe.class.getName()).log(Level.SEVERE, null,
				ex);
		}
	}
}
