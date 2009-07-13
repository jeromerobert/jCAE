/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2005, by EADS CRC
    Copyright (C) 2008,2009, by EADS France
 
    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.
 
    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.
 
    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.xmldata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.GZIPOutputStream;
import java.util.logging.Logger;


public class UNVGenericWriter
{
	private static Logger logger=Logger.getLogger(UNVGenericWriter.class.getName());
	private String unvFile;
	private PrintStream streamN, streamT, streamG;
	private final static String CR=System.getProperty("line.separator");
	private final static int BUFFER_SIZE = 16 * 1024;
	
	public UNVGenericWriter(String unvFile)
	{
		this.unvFile = unvFile;
		try
		{
			BufferedOutputStream bosN, bosT, bosG;
			bosN=new BufferedOutputStream(new FileOutputStream(unvFile));
			bosT=new BufferedOutputStream(new FileOutputStream(unvFile+"T"));
			bosG=new BufferedOutputStream(new FileOutputStream(unvFile+"G"));
			if(unvFile.endsWith(".gz"))
			{
				streamN = new PrintStream(new GZIPOutputStream(bosN));
				streamT = new PrintStream(new GZIPOutputStream(bosT));
				streamG = new PrintStream(new GZIPOutputStream(bosG));
			}
			else
			{
				streamN = new PrintStream(bosN);
				streamT = new PrintStream(bosT);
				streamG = new PrintStream(bosG);
			}
			streamN.println("    -1"+CR+"  2411");
			streamT.println("    -1"+CR+"  2412");
			streamG.println("    -1"+CR+"  2435");
		}
		catch(FileNotFoundException ex)
		{
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	
	public void writeNode(int label, double [] coord)
	{
		MeshExporter.UNV.writeSingleNode(streamN, label, coord[0], coord[1], coord[2]);
	}
	
	public void writeTriangle(int label, int [] ind)
	{
		MeshExporter.UNV.writeSingleTriangle(streamT, label, ind[0], ind[1], ind[2]);
	}
	
	public void writeGroup(int groupId, String name, int [] ids)
	{
		MeshExporter.UNV.writeSingleGroup(streamG, groupId, name, ids);
	}
	
	public void finish(int nr, int nrIntNodes, int nrTriangles, double [] coordRefs)
	{
		int nrNodes = nrIntNodes + nr;
		logger.fine("Append coordinates of "+nr+" nodes");
		for (int i = 0; i < nr; i++)
			MeshExporter.UNV.writeSingleNode(streamN, i+nrIntNodes+1, coordRefs[3*i], coordRefs[3*i+1], coordRefs[3*i+2]);
		try
		{
			streamN.println("    -1");
			streamT.println("    -1");
			streamG.println("    -1");
			streamN.close();
			streamT.close();
			streamG.close();
			// Now concatenate all files
			FileChannel fc = new FileOutputStream(unvFile, true).getChannel();
			ByteBuffer bb = ByteBuffer.allocate(BUFFER_SIZE);
			appendFile(fc, unvFile+"T", bb);
			appendFile(fc, unvFile+"G", bb);
			fc.close();
			// And delete old files
			new File(unvFile+"T").delete();
			new File(unvFile+"G").delete();
			logger.info("Total number of nodes: "+nrNodes);
			logger.info("Total number of triangles: "+nrTriangles);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	
	private void appendFile(FileChannel fc, String file, ByteBuffer bb)
		throws IOException, FileNotFoundException
	{
		FileChannel fci = new FileInputStream(file).getChannel();
		bb.clear();
		while (true)
		{
			bb.rewind();
			int nr = fci.read(bb);
			bb.flip();
			if (nr > 0)
				fc.write(bb);
			if (nr < bb.capacity())
				break;
		}
		fci.close();
	}
}

