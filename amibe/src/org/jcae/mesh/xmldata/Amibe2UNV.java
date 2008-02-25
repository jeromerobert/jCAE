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
 * (C) Copyright 2007,2008, by EADS France
 */

package org.jcae.mesh.xmldata;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.NumberFormat;
import java.util.zip.GZIPOutputStream;
import javax.xml.parsers.ParserConfigurationException;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
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
	private static Logger logger=Logger.getLogger(MeshExporter.class.getName());
	public final static int UNIT_METER=1;
	public final static int UNIT_MM=5;
	
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
	private File directory;
	private Document document;
	private int[] groupIds;
	protected String[] names;			
	private int unit=1;

	private long[] groupOffsets;
	private int[] groupSize;
	private File groupFile;
	
	/** @param directory The directory which contain 3d files */
	public Amibe2UNV(File directory)
	{
		this.directory=directory;
	}
	
	private int[] getAllGroupIDs()
	{
		Element xmlGroups=(Element) document.getElementsByTagName("groups").item(0);
		NodeList nl=xmlGroups.getElementsByTagName("group");
		int[] toReturn=new int[nl.getLength()];
		for(int i=0; i<toReturn.length; i++)
		{
			Element e=(Element) nl.item(i);
			toReturn[i]=Integer.parseInt(e.getAttribute("id"));
		}
		return toReturn;
	}
	
	protected File getNodeFile()
	{
		Element xmlNodes = (Element) document.getElementsByTagName(
			"nodes").item(0);
		String a=((Element)xmlNodes.getElementsByTagName("file").item(0)).getAttribute("location");
		return new File(directory, a);
	}
	
	protected File getNormalFile()
	{
		Element xmlNormals = (Element) document.getElementsByTagName(
			"normals").item(0);
		String a=((Element)xmlNormals.getElementsByTagName("file").item(0)).getAttribute("location");
		return new File(directory, a);
	}
	
	private File getTriaFile()
	{
		Element xmlNodes = (Element) document.getElementsByTagName(
			"triangles").item(0);
		Node fn = xmlNodes.getElementsByTagName("file").item(0);
		String a=((Element)fn).getAttribute("location");
		return new File(directory, a);
	}	
	
	/**
	 * @param the xml element of DOM tree corresponding to the tag "groups".
	 * @param a group.
	 * @return the xml element of DOM tree corresponding to the group.
	 */
	private Element getXmlGroup(Element xmlGroups, int id)
	{
		NodeList list = xmlGroups.getElementsByTagName("group");
		Element elt = null;
		int i = 0;
		boolean found = false;
		int length=list.getLength();
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
			if (id == aId)
			{
				found = true;
			} else
			{
				i++;
			}
		}
		if (found)
		{
			return elt;
		}
		return null;
	}
	
	private void readGroups()
	{
		Element xmlGroups=(Element) document.getElementsByTagName("groups").item(0);
		names=new String[groupIds.length];
		groupOffsets=new long[groupIds.length];
		groupSize=new int[groupIds.length];
		for(int i=0; i<groupIds.length; i++)
		{
			Element e=getXmlGroup(xmlGroups, groupIds[i]);
			
			Element nameNode=(Element)e.getElementsByTagName("name").item(0);
			names[i]=nameNode.getChildNodes().item(0).getNodeValue();			
			
			Element numberNode=(Element)e.getElementsByTagName("number").item(0);
			String v=numberNode.getChildNodes().item(0).getNodeValue();
			groupSize[i]=Integer.parseInt(v);
			String groupFileN=((Element)e.getElementsByTagName("file").item(0)).getAttribute("location");
			String os=((Element)e.getElementsByTagName("file").item(0)).getAttribute("offset");
			groupFile=new File(directory, groupFileN);
			groupOffsets[i]=Long.parseLong(os);
		}
	}
		
	public void setUnit(int unit)
	{
		this.unit=unit;
	}
	public void write(PrintStream out) throws ParserConfigurationException, SAXException, IOException
	{
		document=XMLHelper.parseXML(new File(directory, JCAEXMLData.xml3dFilename));
		if(groupIds==null)
			groupIds=getAllGroupIDs();		
		readGroups();
		writeInit(out);
		writeNodes(out);	
		writeTriangles(out);
		writeGroups(out);
	}
	
	/**
	 * @param fileName The UNV filename. If the name ends with ".gz" it will
	 * be zlib compressed.
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public void write(String fileName)
	{
		logger.info("Export into file "+fileName+" (format "+getClass().getSimpleName()+")");
		try
		{
			FileOutputStream fos=new FileOutputStream(fileName);
			BufferedOutputStream bos=new BufferedOutputStream(fos);
			PrintStream pstream;

			if(fileName.endsWith(".gz"))
				pstream=new PrintStream(new GZIPOutputStream(bos));
			else
				pstream=new PrintStream(bos);

			write(pstream);
			pstream.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		catch (ParserConfigurationException e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		catch (SAXException e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @param out
	 * @param amibeTriaToUNVTria
	 * @throws IOException 
	 */
	private void writeGroups(PrintStream out) throws IOException
	{
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
	
	private void writeInit(PrintStream arg0)
	{
		arg0.println("    -1");
		arg0.println("   164");
		if(unit==UNIT_MM)
		{
			arg0.println("         5mm (milli-newton)            2");
			arg0.println("  1.00000000000000000D+03  1.00000000000000000D+03  1.00000000000000000D+00");
		}
		else
		{
			arg0.println("         1Meter (newton)               2");
			arg0.println("  1.00000000000000000D+00  1.00000000000000000D+00  1.00000000000000000D+00");				
		}
		arg0.println("  2.73149999999999977D+02");
		arg0.println("    -1");
	}
	
	private void writeNodes(PrintStream out) throws IOException
	{
		File f=getNodeFile();
		// Open the file and then get a channel from the stream
		FileInputStream fis = new FileInputStream(f);
		FileChannel fc = fis.getChannel();
	
		ByteBuffer bb=ByteBuffer.allocate(3*8);
		int count = 1;
		out.println("    -1"+CR+"  2411");
		
		while(fc.read(bb)!=-1)
		{
			bb.rewind();
			MeshExporter.writeSingleNodeUNV(out, count,
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
	 * @param amibeNodeToUNVNode
	 * @throws IOException 
	 */
	private void writeTriangles(PrintStream out) throws IOException
	{
		FileChannel fc = new FileInputStream(getTriaFile()).getChannel();
		ByteBuffer bb=ByteBuffer.allocate(3*4);
		int count = 1;
		out.println("    -1"+CR+"  2412");
		
		while(fc.read(bb)!=-1)
		{
			bb.rewind();
			MeshExporter.writeSingleTriangleUNV(out, count,
				bb.getInt()+1, bb.getInt()+1, bb.getInt()+1);
			bb.rewind();
			count ++;
		}
		
		out.println("    -1");		
		logger.info("Total number of triangles: "+count);
	}
}
