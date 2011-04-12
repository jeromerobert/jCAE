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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.xmldata.AmibeReader.SubMesh;
import org.xml.sax.SAXException;

/**
 * Netgen compatible .surf format
 * @author Jerome Robert
 */
public class Amibe2SURF {
	private final File directory;

	public Amibe2SURF(File directory) {
		this.directory=directory;
	}

	public void write(PrintStream out) throws SAXException, IOException
	{
		AmibeReader.Dim3 ar = new AmibeReader.Dim3(directory.getPath());
		SubMesh sm = ar.getSubmeshes().get(0);
		out.println("#");
		out.println(sm.getNumberOfNodes());
		DoubleFileReader nodes = sm.getNodes();
		for(int i = 0; i< sm.getNumberOfNodes() ; i++)
			out.println(nodes.get()+" "+nodes.get()+" "+nodes.get());
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
		out.println(realTriaNb);
		for(int i = 0; i < sm.getNumberOfTrias(); i++)
		{
			trias.get(buffer);
			if(buffer[0] >= 0 && buffer[1] >=0 && buffer[2] >= 0)
				out.println((buffer[0]+1)+" "+(buffer[1]+1)+" "+(buffer[2]+1));
		}
	}

	public void write(String fileName) throws IOException, SAXException
	{
		PrintStream out = new PrintStream(fileName);
		write(out);
		out.close();
	}

	public static void main(final String[] args) {
		try {
			new Amibe2SURF(new File("/home/robert/tetramesh/mobilette.amibe")).write("/home/robert/tetramesh/mobilette.surf");
		} catch (IOException ex) {
			Logger.getLogger(Amibe2SMESH.class.getName()).log(Level.SEVERE, null,
				ex);
		} catch (SAXException ex) {
			Logger.getLogger(Amibe2SMESH.class.getName()).log(Level.SEVERE, null,
				ex);
		}
	}
}
