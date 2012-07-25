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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Convert afront .m file file to Amibe
 * see http://afront.sourceforge.net/
 * @author Jerome Robert
 */
public class AFront2Amibe {
	private final String outputDir;
	private AmibeWriter.Dim3 out;
	private final static Pattern TOKENIZE = Pattern.compile("\\s+");
	public AFront2Amibe(String outputDir) {
		this.outputDir = outputDir;
	}

	public void read(String fileName) throws IOException
	{
		BufferedReader in = new BufferedReader(new FileReader(fileName));
		read(in);
		in.close();
		if(fileName.endsWith(".m"))
		{
			String pattern = fileName.substring(0, fileName.length() - 2);
			String failSafeFile = pattern+".failsafe.txt";
			if(new File(failSafeFile).exists())
			{
				in = new BufferedReader(new FileReader(failSafeFile));
				readFailSafe(in);
				in.close();
			}
		}
		out.finish();
	}

	public void read(BufferedReader in) throws IOException
	{
		out = new AmibeWriter.Dim3(outputDir);
		out.setFixNoGroup(true);
		String line = in.readLine();
		while(line != null)
		{
			String[] tokens = TOKENIZE.split(line);
			if("Vertex".equals(tokens[0]))
			{
				out.addNode(
					Double.parseDouble(tokens[2]),
					Double.parseDouble(tokens[3]),
					Double.parseDouble(tokens[4]));
			}
			else if("Face".equals(tokens[0]))
			{
				out.addTriangle(
					Integer.parseInt(tokens[2])-1,
					Integer.parseInt(tokens[3])-1,
					Integer.parseInt(tokens[4])-1);
			}
			line = in.readLine();
		}
	}

	public void readFailSafe(BufferedReader in) throws IOException {
		String line = in.readLine();
		int k = 1;
		while(line != null)
		{
			int nbNodes = Integer.parseInt(TOKENIZE.split(line)[1]);
			out.nextNodeGroup("failsafe"+(k++));
			for(int i = 0; i < nbNodes; i++)
				out.addNodeToGroup(Integer.parseInt(in.readLine()));
			line = in.readLine();
		}
	}
}
