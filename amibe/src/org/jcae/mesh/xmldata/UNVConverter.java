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
import gnu.trove.TIntArrayList;
import gnu.trove.TIntIntHashMap;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.apache.log4j.Logger;

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
	private static Logger logger=Logger.getLogger(UNVConverter.class);
	
	public static class FormatD25_16 extends DecimalFormat
	{
		private static String PATTERN="0.0000000000000000E00";
		public FormatD25_16()
		{
			super(PATTERN);
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
			
			PrintStream p=new PrintStream(new BufferedOutputStream(new FileOutputStream(new File("/tmp/blub.unv"))));
			new UNVConverter(new File("/home/usr/local2/home/jerome/"), ids).
				writeUNV(p);
			p.close();
			//	writeUNV(System.out);
			
			/*int[] ids=new int[2];
			for(int i=0; i<ids.length; i++)
			{
				ids[i]=i;
			}
			new UNVConverter(new File("/home/usr/local2/home/jerome/enorme.2/"), ids).
				writeUNV(new PrintStream(new BufferedOutputStream(new FileOutputStream(new File("/tmp/blub.unv")))));	
			*/
			
			
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
	
	/**
	 * Workaround for Bug ID4724038.
	 * see http://bugs.sun.com/bugdatabase/view_bug.do;:YfiG?bug_id=4724038
	 */
	public static void clean(final MappedByteBuffer buffer)
	{
		try
		{
			Class cleanerClass=Class.forName("sun.misc.Cleaner");
			final Method cleanMethod=cleanerClass.getMethod("clean", null);
			AccessController.doPrivileged(new PrivilegedAction()
			{
				public Object run()
				{
					try
					{
						Method getCleanerMethod = buffer.getClass().getMethod(
							"cleaner", new Class[0]);
						
						getCleanerMethod.setAccessible(true);
						Object cleaner = getCleanerMethod.invoke(buffer,new Object[0]);
						if(cleaner!=null)
						{
							cleanMethod.invoke(cleaner, null);
						}
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
					return null;
				}
			});
		}		
		catch(ClassNotFoundException ex)
		{
			//Not a Sun JVM so we exit.
		}
		catch (SecurityException e)
		{
			e.printStackTrace();
		}
		catch (NoSuchMethodException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * @param directory The directory which contain the jcae3d file
	 * @param groupIds The list of ids of groups to convert
	 */
	public UNVConverter(File directory, int[] groupIds)
	{
		this.directory=directory;
		this.groupIds=new int[groupIds.length];
		System.arraycopy(groupIds, 0, this.groupIds, 0, groupIds.length);
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
	
	private File getNormalFile()
	{
		Element xmlNormals = (Element) document.getElementsByTagName(
			"normals").item(0);
		String a=((Element)xmlNormals.getElementsByTagName("file").item(0)).getAttribute("location");
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
				
				// Map the file into memory
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
		File f = getTriaFile();
		// Open the file and then get a channel from the stream
		FileInputStream fis = new FileInputStream(f);
		FileChannel fc = fis.getChannel();
		int[] toReturn = new int[numberOfTriangles * 3];
		int count = 0;
		ByteBuffer bb = ByteBuffer.allocateDirect(3 * 4);
		IntBuffer tria = bb.asIntBuffer();
		for (int i = 0; i < groups.length; i++)
		{
			for (int j = 0; j < groups[i].length; j++)
			{
				fc.read(bb, groups[i][j] * 3 * 4);
				bb.rewind();
				tria.get(toReturn, count, 3);
				count += 3;
			}
		}
		return toReturn;
	}	
	
	public void writeUNV(String fileName)
	{
		writeMesh(fileName, "UNV");
	}
	
	public void writeUNV(PrintStream out) throws ParserConfigurationException, SAXException, IOException
	{
		writeMesh(out, "UNV");
	}
	
	public void writeMESH(String fileName)
	{
		writeMesh(fileName, "MESH");
	}
	
	public void writeMESH(PrintStream out) throws ParserConfigurationException, SAXException, IOException
	{
		writeMesh(out, "MESH");
	}
	
	public void writePOLY(String fileName)
	{
		writeMesh(fileName, "POLY");
	}
	
	public void writePOLY(PrintStream out) throws ParserConfigurationException, SAXException, IOException
	{
		writeMesh(out, "POLY");
	}
	
	public void writeSTL(String fileName)
	{
		writeMesh(fileName, "STL");
	}
	
	public void writeSTL(PrintStream out) throws ParserConfigurationException, SAXException, IOException
	{
		writeMesh(out, "STL");
	}
	
	private void writeMesh(PrintStream out, String meshType) throws ParserConfigurationException, SAXException, IOException
	{
		document=XMLHelper.parseXML(new File(directory,"jcae3d"));
		if(groupIds==null)
			groupIds=getAllGroupIDs();
		readGroups();
		int[] triangle=readTriangles();
		TIntIntHashMap amibeNodeToUNVNode=new TIntIntHashMap();
		WriteMeshProcedures proc;
		if (meshType.equals("UNV"))
			proc = new WriteMeshUNV();
		else if (meshType.equals("POLY"))
			proc = new WriteMeshPOLY();
		else if (meshType.equals("STL"))
			proc = new WriteMeshSTL();
		else
			proc = new WriteMeshMESH();
		proc.writeInit(out);
		TIntHashSet nodeset = new TIntHashSet(triangle);
		TIntArrayList nodelist = new TIntArrayList(nodeset.toArray());
		nodelist.sort();
		proc.writeNodes(out, nodelist.toNativeArray(), amibeNodeToUNVNode);
		TIntIntHashMap amibeTriaToUNVTria=new TIntIntHashMap();
		proc.writeTriangles(out, triangle, amibeNodeToUNVNode, amibeTriaToUNVTria);
		proc.writeNormals(out, triangle, amibeNodeToUNVNode, amibeTriaToUNVTria);
		triangle=null;
		amibeNodeToUNVNode=null;
		proc.writeGroups(out, amibeTriaToUNVTria);
		proc.writeFinish(out);
	}
	
	/**
	 * @param fileName The UNV filename. If the name ends with ".gz" it will
	 * be zlib compressed.
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	private void writeMesh(String fileName, String meshType)
	{
		logger.info("Export into file "+fileName+" (format "+meshType+")");
		try
		{
			FileOutputStream fos=new FileOutputStream(fileName);
			BufferedOutputStream bos=new BufferedOutputStream(fos);
			PrintStream pstream;

			if(fileName.endsWith(".gz"))
				pstream=new PrintStream(new GZIPOutputStream(bos));
			else
				pstream=new PrintStream(bos);

			writeMesh(pstream, meshType);
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
	
	private abstract class WriteMeshProcedures
	{
		public abstract void writeNodes(PrintStream out, int[] nodesID,
			TIntIntHashMap amibeToUNV) throws IOException;
		public abstract void writeTriangles(PrintStream out, int[] triangles,
			TIntIntHashMap amibeNodeToUNVNode,
			TIntIntHashMap amibeTriaToUNVTria) throws IOException;
		public void writeInit(PrintStream out)
		{
		}
		public void writeFinish(PrintStream out)
		{
		}
		public void writeNormals(PrintStream out, int[] triangles,
			TIntIntHashMap amibeNodeToUNVNode,
			TIntIntHashMap amibeTriaToUNVTria) throws IOException
		{
		}
		public void writeGroups(PrintStream out,
			TIntIntHashMap amibeTriaToUNVTria) throws IOException
		{
		}
	}
	
	public static void writeSingleNodeUNV(PrintStream out, int count, double x, double y, double z)
	{
		out.println(FORMAT_I10.format(count)+"         1         1         1");
		out.println(FORMAT_D25_16.format(x)+FORMAT_D25_16.format(y)+FORMAT_D25_16.format(z));
	}
	
	public static void writeSingleTriangleUNV(PrintStream out, int count, int n0, int n1, int n2)
	{
		out.println(FORMAT_I10.format(count)+"        91         1         1         1         3");
		out.println(FORMAT_I10.format(n0)+FORMAT_I10.format(n1)+FORMAT_I10.format(n2));
	}
	
	public static void writeSingleGroupUNV(PrintStream out, String name, int first, int count)
	{
		out.println("1      0         0         0         0         0         0      "+count);
		out.println(name);
		int countg=0;
		for(int j=0; j<count; j++)
		{
			out.print("         8"+FORMAT_I10.format(j+first));
			countg++;
			if (countg == 4)
			{
				out.println("");
				countg = 0;
			}
		}
		if (countg != 0)
			out.println();
	}
	
	private class WriteMeshUNV extends WriteMeshProcedures
	{
		public void writeInit(PrintStream arg0)
		{
			arg0.println("    -1");
			arg0.println("   164");
			arg0.println("         1Meter (newton)               2");
			arg0.println("  1.00000000000000000D+00  1.00000000000000000D+00  1.00000000000000000D+00");
			arg0.println("  2.73149999999999977D+02");
			arg0.println("    -1");
		}	
		
		public void writeNodes(PrintStream out, int[] nodesID, TIntIntHashMap amibeToUNV) throws IOException
		{
			File f=getNodeFile();
			// Open the file and then get a channel from the stream
			FileInputStream fis = new FileInputStream(f);
			FileChannel fc = fis.getChannel();
		
			// Map the file into memory
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
				writeSingleNodeUNV(out, count, x, y, z);
			}
			out.println("    -1");
			fc.close();
			fis.close();
			clean(bb);
			logger.info("Total number of nodes: "+count);
		}
		
		/**
		 * @param out
		 * @param amibeNodeToUNVNode
		 */
		public void writeTriangles(PrintStream out, int[] triangles,
			TIntIntHashMap amibeNodeToUNVNode, TIntIntHashMap amibeTriaToUNVTria)
			throws IOException
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
					writeSingleTriangleUNV(out, count,
						amibeNodeToUNVNode.get(triangles[triaIndex++]),
						amibeNodeToUNVNode.get(triangles[triaIndex++]),
						amibeNodeToUNVNode.get(triangles[triaIndex++]));
				}
			}
			out.println("    -1");
			logger.info("Total number of triangles: "+count);
		}
		
		/**
		 * @param out
		 * @param amibeTriaToUNVTria
		 */
		public void writeGroups(PrintStream out, TIntIntHashMap amibeTriaToUNVTria)
			throws IOException
		{
			out.println("    -1"+CR+"  2435");
			for(int i=0;i<groups.length; i++)
			{				
				out.println(FORMAT_I10.format(i+1)+
					"         0         0         0         0         0         0"+
					FORMAT_I10.format(groups[i].length));
				
				out.println(names[i]);
				int countg=0;
				for(int j=0; j<groups[i].length; j++)
				{
					out.print("         8"
						+FORMAT_I10.format(amibeTriaToUNVTria.get(groups[i][j]))
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
	}
	
	private class WriteMeshSTL extends WriteMeshProcedures
	{
		public void writeNodes(PrintStream out, int[] nodesID, TIntIntHashMap amibeToUNV) throws IOException
		{
		}
		
		/**
		 * @param out
		 * @param amibeNodeToUNVNode
		 */
		public void writeTriangles(PrintStream out, int[] triangles,
			TIntIntHashMap amibeNodeToUNVNode, TIntIntHashMap amibeTriaToUNVTria)
			throws IOException
		{
			File f=getNodeFile();
			// Open the file and then get a channel from the stream
			FileInputStream fis = new FileInputStream(f);
			FileChannel fc = fis.getChannel();
		
			// Map the file into memory
			MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, f.length());
			DoubleBuffer nodesBuffer=bb.asDoubleBuffer();
			
			out.println("solid");
			int count=0;
			double x,y,z;
			for(int i=0; i<groups.length; i++)
			{
				for(int j=0; j<groups[i].length; j++)
				{
					out.println("facet");
					out.println("   outer loop");
					for(int k=0; k < 3; k++)
					{
						int iid=triangles[count*3+k]*3;
						x=nodesBuffer.get(iid);
						y=nodesBuffer.get(iid+1);
						z=nodesBuffer.get(iid+2);
						out.println("     vertex "+x+" "+y+" "+z);
					}
					out.println("   endloop");
					out.println("endfacet");
					count++;
				}
			}
			fc.close();
			fis.close();
			clean(bb);
			logger.info("Total number of nodes: "+count);
			out.println("endsolid");
			logger.info("Total number of triangles: "+count);
		}
		
		/**
		 * @param out
		 * @param amibeTriaToUNVTria
		 */
		public void writeGroups(PrintStream out, TIntIntHashMap amibeTriaToUNVTria)
			throws IOException
		{
		}
	}
	
	private class WriteMeshMESH extends WriteMeshProcedures
	{
		public void writeInit(PrintStream out)
		{
			out.println("\nMeshVersionFormatted 1\n\nDimension\n3");
		}
		public void writeFinish(PrintStream out)
		{
			out.println("\nEnd");
		}
		public void writeNodes(PrintStream out, int[] nodesID, TIntIntHashMap amibeToUNV) throws IOException
		{
			File f=getNodeFile();
			// Open the file and then get a channel from the stream
			FileInputStream fis = new FileInputStream(f);
			FileChannel fc = fis.getChannel();
			
			// Map the file into memory
			MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, f.length());
			DoubleBuffer nodesBuffer=bb.asDoubleBuffer();
			
			int count =  0;
			double x,y,z;
			out.println("\nVertices\n"+nodesID.length);
			for(int i=0; i<nodesID.length; i++)
			{
				int iid=nodesID[i]*3;
				x=nodesBuffer.get(iid);
				y=nodesBuffer.get(iid+1);
				z=nodesBuffer.get(iid+2);
				count++;
				amibeToUNV.put(nodesID[i], count);
				out.println(x+" "+y+" "+z+" 0");
			}
			fc.close();
			fis.close();
			clean(bb);
			logger.info("Total number of nodes: "+count);
		}
		
		/**
		 * @param out
		 * @param amibeNodeToUNVNode
		 */
		public void writeTriangles(PrintStream out, int[] triangles,
			TIntIntHashMap amibeNodeToUNVNode, TIntIntHashMap amibeTriaToUNVTria)
			throws IOException
		{
			int count=0;
			for(int i=0; i<groups.length; i++)
				count += groups[i].length;
			
			out.println("\nTriangles\n"+count);
			int triaIndex=0;
			count=0;
			for(int i=0; i<groups.length; i++)
			{
				for(int j=0; j<groups[i].length; j++)
				{
					count++;
					amibeTriaToUNVTria.put(groups[i][j], count);
					out.println(amibeNodeToUNVNode.get(triangles[triaIndex++])+" "+amibeNodeToUNVNode.get(triangles[triaIndex++])+" "+amibeNodeToUNVNode.get(triangles[triaIndex++])+" "+(i+1));
				}
			}
			logger.info("Total number of triangles: "+count);
		}
		
		public void writeNormals(PrintStream out, int[] triangles,
			TIntIntHashMap amibeNodeToUNVNode, TIntIntHashMap amibeTriaToUNVTria) throws IOException
		{
			//  Open the input file first so that an exception is
			//  raised if it is not found.
			File f=getNormalFile();
			FileInputStream fis = new FileInputStream(f);
			FileChannel fc = fis.getChannel();
			
			int count=0;
			for(int i=0; i<groups.length; i++)
				count += groups[i].length;
			
			out.println("\nNormals\n"+(3*count));
			
			// Map the file into memory
			MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, f.length());
			DoubleBuffer normalsBuffer=bb.asDoubleBuffer();
			
			double x,y,z;
			for(int i=0; i<groups.length; i++)
			{
				for(int j=0; j<groups[i].length; j++)
				{
					int iid = (amibeTriaToUNVTria.get(groups[i][j]) - 1)* 9;
					for (int k = 0; k < 3; k++)
					{
						x=normalsBuffer.get(iid);
						y=normalsBuffer.get(iid+1);
						z=normalsBuffer.get(iid+2);
						out.println(x+" "+y+" "+z);
						iid += 3;
					}
				}
			}
			fc.close();
			fis.close();
			clean(bb);
			out.println("\nNormalAtTriangleVertices\n"+(3*count));
			for(int i=0; i<groups.length; i++)
			{
				for(int j=0; j<groups[i].length; j++)
				{
					int nT = amibeTriaToUNVTria.get(groups[i][j]);
					out.println(nT+" 1 "+(3*nT-2));
					out.println(nT+" 2 "+(3*nT-1));
					out.println(nT+" 3 "+(3*nT));
				}
			}
		}
	}
	
	private class WriteMeshPOLY extends WriteMeshProcedures
	{
		public void writeFinish(PrintStream out)
		{
			out.println("# Part 3 - hole list");
			out.println("0");
			out.println("# Part 4 - hole list");
			out.println("0");
		}
		public void writeNodes(PrintStream out, int[] nodesID, TIntIntHashMap amibeToUNV) throws IOException
		{
			File f=getNodeFile();
			// Open the file and then get a channel from the stream
			FileInputStream fis = new FileInputStream(f);
			FileChannel fc = fis.getChannel();
			
			// Map the file into memory
			MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, f.length());
			DoubleBuffer nodesBuffer=bb.asDoubleBuffer();
			
			int count =  0;
			double x,y,z;
			out.println("# Part 1 - node list");
			out.println(""+nodesID.length+" 3 0 0");
			for(int i=0; i<nodesID.length; i++)
			{
				int iid=nodesID[i]*3;
				x=nodesBuffer.get(iid);
				y=nodesBuffer.get(iid+1);
				z=nodesBuffer.get(iid+2);
				count++;
				amibeToUNV.put(nodesID[i], count);
				out.println("   "+count+" "+x+" "+y+" "+z);
			}
			fc.close();
			fis.close();
			clean(bb);
			logger.info("Total number of nodes: "+count);
		}
		
		/**
		 * @param out
		 * @param amibeNodeToUNVNode
		 */
		public void writeTriangles(PrintStream out, int[] triangles,
			TIntIntHashMap amibeNodeToUNVNode, TIntIntHashMap amibeTriaToUNVTria)
		{
			int count=0;
			for(int i=0; i<groups.length; i++)
				count += groups[i].length;
			
			out.println("# Part 2 - element list");
			out.println(""+count+" 0");
			int triaIndex=0;
			count=0;
			for(int i=0; i<groups.length; i++)
			{
				for(int j=0; j<groups[i].length; j++)
				{
					count++;
					amibeTriaToUNVTria.put(groups[i][j], count);
					out.println("   1 0 0");
					out.println("   3 "+
							amibeNodeToUNVNode.get(triangles[triaIndex++])+" "+amibeNodeToUNVNode.get(triangles[triaIndex++])+" "+amibeNodeToUNVNode.get(triangles[triaIndex++]));
				}
			}
		}
	}
}
