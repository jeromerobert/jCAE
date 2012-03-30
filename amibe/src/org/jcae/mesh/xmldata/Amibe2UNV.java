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
 * (C) Copyright 2007,2008,2009, by EADS France
 */

package org.jcae.mesh.xmldata;

import java.util.logging.Level;
import org.jcae.mesh.xmldata.AmibeReader.Group;
import org.jcae.mesh.xmldata.AmibeReader.SubMesh;
import org.jcae.mesh.xmldata.MeshExporter.UNV.Unit;
import java.io.File;
import java.io.PrintStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.NumberFormat;
import java.util.logging.Logger;
import org.xml.sax.SAXException;

/**
 * Convert an Amibe mesh to a UNV mesh.
 * The convertion is out-of-core and can handle large mesh.
 * @see MeshExporter.UNV 
 * @author Jerome Robert
 */
public class Amibe2UNV
{
	private final static String CR=System.getProperty("line.separator");			
	private final static NumberFormat FORMAT_I10=new MeshExporter.FormatI10();	
	private static final Logger logger=Logger.getLogger(Amibe2UNV.class.getName());

	/**
	 * A main method for debugging
	 * @param args
	 */
	public static void main(String[] args)
	{		
		try
		{			
			PrintStream p=new PrintStream(new BufferedOutputStream(new FileOutputStream(
				"/tmp/blub2.unv")));
			new MeshExporter.UNV("/home/jerome/OCCShapeGal/amibe1.dir/").write(p);
			//new Amibe2UNV(new File("/home/jerome/jCAE-cvs-head/FLIGHT0.01")).write(p);
			p.close();
						
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	private final File directory;
	private final MeshExporter.UNV unvWriter;
	private double scale = 1.0;
	
	/**
	 * @param directory The directory which contain 3d files
	 */
	public Amibe2UNV(File directory)
	{
		this.directory=directory;
		this.unvWriter = new MeshExporter.UNV(directory.getPath());
	}
	
	public void setUnit(Unit unit)
	{
		unvWriter.setUnit(unit);
	}

	public void setScale(double scale)
	{
		this.scale = scale;
	}

	public void write(String fileName) throws IOException, SAXException
	{
		PrintStream out = new PrintStream(fileName);
		write(out);
		out.close();
	}

	public void write(PrintStream out) throws SAXException, IOException
	{
		AmibeReader.Dim3 ar = new AmibeReader.Dim3(directory.getPath());
		SubMesh sm = ar.getSubmeshes().get(0);
		unvWriter.writeInit(out);
		writeNodes(out);
		out.println("    -1"+CR+"  2412");
		int count = writeTriangles(out, sm);
		writeBeams(out, sm, count);
		out.println("    -1");
		writeGroups(out, sm, count);
	}

	protected String formatGroupName(String name)
	{
		return name;
	}
	/**
	 * @param out
	 * @param count id of the first beam
	 * @throws IOException 
	 */
	private void writeGroups(PrintStream out, AmibeReader.SubMesh subMesh, int count)
			throws SAXException, IOException
	{
		out.println("    -1"+CR+"  2435");
		int i = 0;
		for(Group g:subMesh.getGroups())
		{				
			out.println(FORMAT_I10.format(i+1)+
				"         0         0         0         0         0         0"+
				FORMAT_I10.format(g.getNumberOfTrias()+g.getNumberOfBeams()+g.getNumberOfNodes()));
			
			out.println(formatGroupName(g.getName()));
			int countg=0;
			for(int id:g.readTria3Ids())
			{				
				out.print("         8"
					+FORMAT_I10.format(id+1)
					+"         0         0");
				countg++;
				if ((countg % 2) == 0)
					out.println();
			}
			for(int id:g.readBeamsIds())
			{
				out.print("         8"
					+FORMAT_I10.format(id+count)
					+"         0         0");
				countg++;
				if ((countg % 2) == 0)
					out.println();
			}
			for(int id:g.readNodesIds())
			{
				out.print("         7"
					+FORMAT_I10.format(id+1)
					+"         0         0");
				countg++;
				if ((countg % 2) == 0)
					out.println();
			}
			if ((countg % 2) !=0 )
				out.println();
			i++;
		}
		out.println("    -1");
	}	
	
	private void writeNodes(PrintStream out) throws IOException
	{
		File f=unvWriter.getNodeFile();
		// Open the file and then get a channel from the stream
		FileInputStream fis = new FileInputStream(f);
		FileChannel fc = fis.getChannel();
	
		ByteBuffer bb=ByteBuffer.allocate(3*8);
		int count = 1;
		out.println("    -1"+CR+"  2411");
		
		while(fc.read(bb)!=-1)
		{
			bb.rewind();
			MeshExporter.UNV.writeSingleNode(out, count,
				bb.getDouble()*scale, bb.getDouble()*scale, bb.getDouble()*scale);
			bb.rewind();
			count ++;
		}
		
		out.println("    -1");
		
		fc.close();
		logger.info("Total number of nodes: "+count);
	}
	
	/**
	 * @param out
	 * @throws IOException 
	 */
	private int writeTriangles(PrintStream out, AmibeReader.SubMesh subMesh) throws IOException
	{
		int count = 1;
		if(subMesh.getNumberOfTrias() > 0)
		{
			IntFileReader trias = subMesh.getTriangles();
			long nb = trias.size() / 3;
			for(int i = 0; i<nb; i++)
			{
				int n1 = trias.get();
				int n2 = trias.get();
				int n3 = trias.get();
				if(n1 >= 0)
					MeshExporter.UNV.writeSingleTriangle(out, count,
						n1+1, n2+1, n3+1);
				count ++;
			}
		}
		logger.log(Level.INFO, "Total number of triangles: {0}", count-1);
		return count;
	}

	private void writeBeams(PrintStream out, AmibeReader.SubMesh subMesh, int count) throws IOException
	{
		if(subMesh.getNumberOfBeams() > 0)
		{
			IntFileReader beams = subMesh.getBeams();
			long nb = beams.size() / 2;
			for(int i = 0; i < nb; i++)
			{
				out.println(FORMAT_I10.format(count) +
					"        21         2         1         5         2");
				out.println("         0         1         1");
				out.println(FORMAT_I10.format(beams.get()+1) + FORMAT_I10.format(beams.get()+1));
				count ++;
			}
		}
	}
}
