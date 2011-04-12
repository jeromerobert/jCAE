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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import org.jcae.mesh.xmldata.AmibeReader.SubMesh;
import org.xml.sax.SAXException;

/**
 * http://tetgen.berlios.de/fformats.smesh.html
 * @author Jerome Robert
 */
public class Amibe2SMESH {
	private final File directory;

	public Amibe2SMESH(File directory) {
		this.directory=directory;
	}

	public void write(PrintStream out) throws SAXException, IOException
	{
		AmibeReader.Dim3 ar = new AmibeReader.Dim3(directory.getPath());
		SubMesh sm = ar.getSubmeshes().get(0);
		out.println(sm.getNumberOfNodes()+" 3 0 0");
		DoubleFileReader nodes = sm.getNodes();
		for(int i = 0; i< sm.getNumberOfNodes() ; i++)
			out.println(i+" "+nodes.get()+" "+nodes.get()+" "+nodes.get());
		nodes.close();
		IntFileReader trias = sm.getTriangles();
		int[] buffer = new int[3];
		int realTriaNb = 0;
		for(int i = 0; i < sm.getNumberOfTrias(); i++)
		{
			trias.get(buffer);
			if(buffer[0] >= 0 && buffer[1] >=0 && buffer[2] >= 0)
				realTriaNb ++;
		}
		out.println(realTriaNb+" 0");
		for(int i = 0; i < sm.getNumberOfTrias(); i++)
		{
			trias.get(buffer);
			if(buffer[0] >= 0 && buffer[1] >=0 && buffer[2] >= 0)
				out.println("3 "+buffer[0]+" "+buffer[1]+" "+buffer[2]);
		}
		out.println(0);
		out.println(0);
	}

	public void write(String fileName) throws IOException, SAXException
	{
		PrintStream out = new PrintStream(fileName);
		write(out);
		out.close();
	}
}
