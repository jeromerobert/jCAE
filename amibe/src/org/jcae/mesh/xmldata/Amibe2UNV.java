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
import javax.xml.parsers.ParserConfigurationException;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
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

	public void write(PrintStream out) throws ParserConfigurationException, SAXException, IOException
	{
		unvWriter.writeInit(out);
		writeNodes(out);	
		writeTriangles(out);
		writeGroups(out);
	}
	
	/**
	 * @param out
	 * @throws IOException 
	 */
	private void writeGroups(PrintStream out)
			throws ParserConfigurationException, SAXException, IOException
	{
		Document document=XMLHelper.parseXML(new File(directory, JCAEXMLData.xml3dFilename));
		Element xmlGroups=(Element) document.getElementsByTagName("groups").item(0);
		NodeList nl=xmlGroups.getElementsByTagName("group");
		int[] groupIds=new int[nl.getLength()];
		String[] names=new String[groupIds.length];
		long[] groupOffsets=new long[groupIds.length];
		int[] groupSize=new int[groupIds.length];
		File groupFile=unvWriter.getGroupFile();
		for(int i=0; i<groupIds.length; i++)
		{
			Element e=(Element) nl.item(i);
			groupIds[i]=Integer.parseInt(e.getAttribute("id"));
			Element nameNode=(Element)e.getElementsByTagName("name").item(0);
			names[i]=nameNode.getChildNodes().item(0).getNodeValue();

			Element numberNode=(Element)e.getElementsByTagName("number").item(0);
			String v=numberNode.getChildNodes().item(0).getNodeValue();
			groupSize[i]=Integer.parseInt(v);
			String os=((Element)e.getElementsByTagName("file").item(0)).getAttribute("offset");
			groupOffsets[i]=Long.parseLong(os);
		}
		FileChannel fc = new FileInputStream(groupFile).getChannel();	
		ByteBuffer bb=ByteBuffer.allocate(4);
		out.println("    -1"+CR+"  2435");
		for(int i=0;i<names.length; i++)
		{				
			out.println(FORMAT_I10.format(i+1)+
				"         0         0         0         0         0         0"+
				FORMAT_I10.format(groupSize[i]));
			
			out.println(names[i]);
			int countg=0;
			fc.position(groupOffsets[i]*4);
			for(int j=0; j<groupSize[i]; j++)
			{				
				bb.rewind();
				fc.read(bb);
				out.print("         8"
					+FORMAT_I10.format(bb.getInt(0)+1)
					+"         0         0");
				countg++;
				if ((countg % 2) == 0)
					out.println("");
			}
			if ((countg % 2) !=0 )
				out.println();
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
				bb.getDouble(), bb.getDouble(), bb.getDouble());
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
	private void writeTriangles(PrintStream out) throws IOException
	{
		FileChannel fc = new FileInputStream(unvWriter.getTriaFile()).getChannel();
		ByteBuffer bb=ByteBuffer.allocate(3*4);
		int count = 1;
		out.println("    -1"+CR+"  2412");
		
		while(fc.read(bb)!=-1)
		{
			bb.rewind();
			MeshExporter.UNV.writeSingleTriangle(out, count,
				bb.getInt()+1, bb.getInt()+1, bb.getInt()+1);
			bb.rewind();
			count ++;
		}
		
		out.println("    -1");		
		logger.info("Total number of triangles: "+count);
	}
}
