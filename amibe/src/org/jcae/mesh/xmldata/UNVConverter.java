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
 * (C) Copyright 2004, by EADS CRC
 */

package org.jcae.mesh.xmldata;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.*;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Extract groups from the full mesh and write them to a UNV file.
 * It renumber elements so there ids are from 1 to n. Although it
 * uses the NIO it may not be performant as efficient as a full
 * dump of the mesh to UNV.
 * @author Jerome Robert
 *
 */
public class UNVConverter
{
	
	public static class FormatD25_16 extends DecimalFormat
	{
		private static String PATERN="0.0000000000000000E00";
		public FormatD25_16()
		{
			super(PATERN);
			DecimalFormatSymbols dfs=getDecimalFormatSymbols();
			dfs.setDecimalSeparator('.');
			setDecimalFormatSymbols(dfs);
		}
		
		/* (non-Javadoc)
		 * @see java.text.NumberFormat#format(double, java.lang.StringBuffer, java.text.FieldPosition)
		 */
		public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos)
		{
			StringBuffer sb=super.format(number, toAppendTo, pos);
			int n=sb.length()-3;
			if(n>0)
			{
				if(sb.charAt(n)=='E')
				{
					sb.setCharAt(n, 'D');
					sb.insert(n+1, '+');
				}
				else if(sb.charAt(n)=='-')
				{
					sb.setCharAt(n-1, 'D');
				}
			}
			n=25-sb.length();
			if(n>0)
			{
				char[] c=new char[n];
				Arrays.fill(c, ' ');
				sb.insert(0, c);
			}
			return sb;
		}
	}
	
	public static class FormatI10 extends NumberFormat
	{

		/* (non-Javadoc)
		 * @see java.text.NumberFormat#format(double, java.lang.StringBuffer, java.text.FieldPosition)
		 */
		public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos)
		{
			return format((long)number, toAppendTo, pos);
		}

		/* (non-Javadoc)
		 * @see java.text.NumberFormat#format(long, java.lang.StringBuffer, java.text.FieldPosition)
		 */
		public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos)
		{
			StringBuffer s=new StringBuffer();
			s.append(number);
			int n=10-s.length();
			if(n>0)
			{
				char[] c=new char[n];
				Arrays.fill(c, ' ');
				toAppendTo.append(c);
				toAppendTo.append(s);
			}
			return toAppendTo;
		}
		/* (non-Javadoc)
		 * @see java.text.NumberFormat#parse(java.lang.String, java.text.ParsePosition)
		 */
		public Number parse(String source, ParsePosition parsePosition)
		{
			throw new UnsupportedOperationException();
		}
	}
	
	/**
	 * A main method for debugging
	 * @param args
	 */
	public static void main(String[] args)
	{
		System.out.println(FORMAT_D25_16.format(1E-24));
		System.out.println(FORMAT_D25_16.format(15E24));
		System.out.println(FORMAT_D25_16.format(Double.POSITIVE_INFINITY));
		System.out.println(FORMAT_D25_16.format(Double.NEGATIVE_INFINITY));
		System.out.println(FORMAT_D25_16.format(Double.MAX_VALUE));
		System.out.println(FORMAT_D25_16.format(Double.MIN_VALUE));
		System.out.println(FORMAT_D25_16.format(Double.NaN));
		try
		{
			int[] ids=new int[83];
			for(int i=0; i<ids.length; i++)
			{
				ids[i]=i;
			}
			new UNVConverter(new File("/home/usr/local2/home/jerome/"), ids).
				writeUNV(new PrintStream(new BufferedOutputStream(new FileOutputStream(new File("/tmp/blub.unv")))));
			//	writeUNV(System.out);
		} catch (ParserConfigurationException e)
		{
			e.printStackTrace();
		} catch (SAXException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private final static String CR=System.getProperty("line.separator");
	private final static NumberFormat FORMAT_D25_16=new FormatD25_16();
	private final static NumberFormat FORMAT_I10=new FormatI10();
	
	private File directory;
	private Document document;
	private int[] groupIds;
	private int[][] groups;
	private String[] names;
	private int numberOfTriangles;
	
	/** workaround for Bug ID4724038, see
	 * http://bugs.sun.com/bugdatabase/view_bug.do;:YfiG?bug_id=4724038
	 */
	public static void clean(final MappedByteBuffer buffer)
	{
		AccessController.doPrivileged(new PrivilegedAction()
		{
			public Object run()
			{
				try
							{
					Method getCleanerMethod = buffer.getClass().getMethod("cleaner", new Class[0]);
					getCleanerMethod.setAccessible(true);
					sun.misc.Cleaner cleaner = (sun.misc.Cleaner)getCleanerMethod.invoke(buffer,new Object[0]);
					if(cleaner!=null)
						cleaner.clean();
							}
				catch(Exception e)
							{
					e.printStackTrace();
							}
				return null;
			}
		});
	}
	
	/**
	 * @param directory The directory which contain the jcae3d file
	 * @param groupIds The list of ids of groups to convert
	 */
	public UNVConverter(File directory, int[] groupIds)
	{
		this.directory=directory;
		this.groupIds=groupIds;
	}

	/**
	 * Convert all groups to UNV
	 * @param directory The directory which contain the jcae3d file
	 */
	public UNVConverter(String directory)
	{
		this.directory=new File(directory);
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
	
	private File getNodeFile()
	{
		Element xmlNodes = (Element) document.getElementsByTagName(
			"nodes").item(0);
		String a=((Element)xmlNodes.getElementsByTagName("file").item(0)).getAttribute("location");
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
		while (!found && i < list.getLength())
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
		} else
		{
			return null;
		}
	}
	
	private void readGroups()
	{
		Element xmlGroups=(Element) document.getElementsByTagName("groups").item(0);
		groups=new int[groupIds.length][];
		numberOfTriangles=0;
		names=new String[groupIds.length];
		for(int i=0; i<groupIds.length; i++)
		{
			Element e=getXmlGroup(xmlGroups, groupIds[i]);
			
			Element nameNode=(Element)e.getElementsByTagName("name").item(0);
			names[i]=nameNode.getChildNodes().item(0).getNodeValue();			
			
			Element numberNode=(Element)e.getElementsByTagName("number").item(0);
			String v=numberNode.getChildNodes().item(0).getNodeValue();
			int number=Integer.parseInt(v);
			groups[i]=new int[number];
			numberOfTriangles+=number;
			if(number==0)
				continue;
			
			String groupFileN=((Element)e.getElementsByTagName("file").item(0)).getAttribute("location");
			String os=((Element)e.getElementsByTagName("file").item(0)).getAttribute("offset");
			File groupFile=new File(directory, groupFileN);
			long offset=Long.parseLong(os);
			
			try
			{
				// Open the file and then get a channel from the stream
				FileInputStream fisG = new FileInputStream(groupFile);
				FileChannel fcG = fisG.getChannel();
				
				// Get the file's size and then map it into memory
				MappedByteBuffer bbG = fcG.map(FileChannel.MapMode.READ_ONLY, offset*4, number*4);
				IntBuffer groupsBuffer = bbG.asIntBuffer();
				
				groupsBuffer.get(groups[i]);
				fcG.close();
				fisG.close();
				clean(bbG);
			} catch(IOException ex)
			{
				ex.printStackTrace();
			}
		}
	}
	
	private int[] readTriangles() throws IOException
	{
		File f=getTriaFile();
		// Open the file and then get a channel from the stream
		FileInputStream fis = new FileInputStream(f);
		FileChannel fc = fis.getChannel();
		
		// Get the file's size and then map it into memory
		MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, f.length());
		IntBuffer trias=bb.asIntBuffer();
		
		int[] ns=new int[3];
		int[] toReturn=new int[numberOfTriangles*3];
		int count=0;
		for(int i=0; i<groups.length; i++)
		{
			for(int j=0; j<groups[i].length; j++)
			{
				trias.position(groups[i][j]*3);
				trias.get(toReturn, count, 3);
				count+=3;
			}
		}
		return toReturn;
	}
	
	public void writeUNV(PrintStream out) throws ParserConfigurationException, SAXException, IOException
	{
		document=XMLHelper.parseXML(new File(directory,"jcae3d"));
		if(groupIds==null)
			groupIds=getAllGroupIDs();
		readGroups();
		int[] triangle=readTriangles();
		TIntIntHashMap amibeNodeToUNVNode=new TIntIntHashMap();
		writeUNVNodes(out, new TIntHashSet(triangle).toArray(), amibeNodeToUNVNode);
		/*System.out.println(new TIntArrayList(triangle).toString());
		System.out.println(new TIntArrayList(amibeNodeToUNVNode.keys()).toString());
		System.out.println(new TIntArrayList(amibeNodeToUNVNode.getValues()).toString());*/
		TIntIntHashMap amibeTriaToUNVTria=new TIntIntHashMap();
		writeUNVTriangles(out, triangle, amibeNodeToUNVNode, amibeTriaToUNVTria);
		triangle=null;
		amibeNodeToUNVNode=null;
		writeUNVGroups(out, amibeTriaToUNVTria);
	}
	
	/**
	 * @param fileName The UNV filename. If the name ends with ".gz" it will
	 * be zlib compressed.
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public void writeUNV(String fileName)
	{
		try
		{
			FileOutputStream fos=new FileOutputStream(fileName);
			BufferedOutputStream bos=new BufferedOutputStream(fos);
			PrintStream pstream;

			if(fileName.endsWith(".gz"))
				pstream=new PrintStream(new GZIPOutputStream(bos));
			else
				pstream=new PrintStream(bos);

			writeUNV(pstream);
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
	 */
	private void writeUNVGroups(PrintStream out, TIntIntHashMap amibeTriaToUNVTria)
	{
		out.println("    -1"+CR+"  2430");
		int count =  0;
		for(int i=0;i<groups.length; i++)
		{
			count++;
			out.println("1      0         0         0         0         0         0      "+groups[i].length);
			out.println(names[i]);
			int countg=0;
			for(int j=0; j<groups[i].length; j++)
			{
				out.print("         8"+FORMAT_I10.format(amibeTriaToUNVTria.get(groups[i][j])));
				countg++;
				if ((countg % 4) == 0)
					out.println("");
			}
			if ((countg % 4) !=0 )
				out.println();
		}
		out.println("    -1");

	}
	
	private void writeUNVNodes(PrintStream out, int[] nodesID, TIntIntHashMap amibeToUNV) throws IOException
	{
		File f=getNodeFile();
		// Open the file and then get a channel from the stream
		FileInputStream fis = new FileInputStream(f);
		FileChannel fc = fis.getChannel();
	
		// Get the file's size and then map it into memory
		int sz = (int)fc.size();
		MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, f.length());
		DoubleBuffer nodesBuffer=bb.asDoubleBuffer();
		
		out.println("    -1"+CR+"  2411");
		int count =  0;
		double x,y,z;
		for(int i=0; i<nodesID.length; i++)
		{
			int iid=nodesID[i]*3;
			x=nodesBuffer.get(iid);
			y=nodesBuffer.get(iid+1);
			z=nodesBuffer.get(iid+2);
			count++;
			amibeToUNV.put(nodesID[i], count);
			out.println(FORMAT_I10.format(count)+"         1         1         1");
			out.println(FORMAT_D25_16.format(x)+FORMAT_D25_16.format(y)+FORMAT_D25_16.format(z));
		}
		out.println("    -1");
		fc.close();
		fis.close();
		clean(bb);
	}
	
	/**
	 * @param out
	 * @param amibeNodeToUNVNode
	 */
	private void writeUNVTriangles(PrintStream out, int[] triangles,
		TIntIntHashMap amibeNodeToUNVNode, TIntIntHashMap amibeTriaToUNVTria)
	{
		out.println("    -1"+CR+"  2412");
		int count=0;
		int triaIndex=0;
		for(int i=0; i<groups.length; i++)
		{
			for(int j=0; j<groups[i].length; j++)
			{
				count++;
				amibeTriaToUNVTria.put(groups[i][j], count);
				out.println(FORMAT_I10.format(count)+"        91         1         1         1         3");
				out.println(
					FORMAT_I10.format(amibeNodeToUNVNode.get(triangles[triaIndex++]))+
					FORMAT_I10.format(amibeNodeToUNVNode.get(triangles[triaIndex++]))+
					FORMAT_I10.format(amibeNodeToUNVNode.get(triangles[triaIndex++])));
			}
		}
		out.println("    -1");
	}
}
