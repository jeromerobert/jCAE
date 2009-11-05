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

import gnu.trove.TIntHashSet;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntIntHashMap;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.PrintStream;
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
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Extract groups from the full mesh and write them to a UNV file.
 * It renumber elements so there ids are from 1 to n. Although it
 * uses the NIO it may not be performant as efficient as a full
 * dump of the mesh to UNV.
 * @author Jerome Robert
 *
 */
abstract public class MeshExporter
{
	private static final Logger logger=Logger.getLogger(MeshExporter.class.getName());
	
	public static class FormatD25_16 extends DecimalFormat
	{
		private static final String PATTERN="0.0000000000000000E00";
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
		@Override
		public final StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos)
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
		@Override
		public final StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos)
		{
			return format((long)number, toAppendTo, pos);
		}

		/* (non-Javadoc)
		 * @see java.text.NumberFormat#format(long, java.lang.StringBuffer, java.text.FieldPosition)
		 */
		@Override
		public final StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos)
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
		@Override
		public final Number parse(String source, ParsePosition parsePosition)
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
			int[] ids=new int[82];
			for(int i=0; i<ids.length; i++)
			{
				ids[i]=i;
			}
			
			PrintStream p=new PrintStream(new BufferedOutputStream(new FileOutputStream(
				"/tmp/blub.unv")));
			new MeshExporter.UNV("/home/jerome/OCCShapeGal/amibe1.dir/").
				write(p);
			p.close();
						
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private final static String CR=System.getProperty("line.separator");
	private final static NumberFormat FORMAT_D25_16=new FormatD25_16();
	private final static NumberFormat FORMAT_I10=new FormatI10();
	
	private final File directory;
	private final Document document;
	private final int[] groupIds;
	int[][] groups;
	String[] names;
	private int numberOfTriangles;
	
	/**
	 * @param directory The directory which contains 3d files
	 * @param groupIds The list of ids of groups to convert
	 */
	MeshExporter(File directory, int[] groupIds)
	{
		this.directory=directory;
		Document d = null;
		try {
			d = XMLHelper.parseXML(new File(directory, JCAEXMLData.xml3dFilename));
		} catch (SAXException ex) {
			logger.log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
		} catch (ParserConfigurationException ex)	{
			logger.log(Level.SEVERE, null, ex);
		}
		document = d;
		if(groupIds!=null)
		{
			this.groupIds=new int[groupIds.length];
			System.arraycopy(groupIds, 0, this.groupIds, 0, groupIds.length);
		} else
			this.groupIds=getAllGroupIDs();
	}

	/**
	 * Convert all groups to UNV
	 * @param directory The directory which contains 3d files
	 */
	MeshExporter(String directory)
	{
		this(new File(directory), null);
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
	
	final File getNodeFile()
	{
		Element xmlNodes = (Element) document.getElementsByTagName(
			"nodes").item(0);
		String a=((Element)xmlNodes.getElementsByTagName("file").item(0)).getAttribute("location");
		return new File(directory, a);
	}
	
	final File getTriaFile()
	{
		Element xmlNodes = (Element) document.getElementsByTagName(
			"triangles").item(0);
		Node fn = xmlNodes.getElementsByTagName("file").item(0);
		String a=((Element)fn).getAttribute("location");
		return new File(directory, a);
	}
	
	final File getNormalFile()
	{
		Element xmlNormals = (Element) document.getElementsByTagName("normals").item(0);
		if (xmlNormals == null)
			return null;
		String a=((Element)xmlNormals.getElementsByTagName("file").item(0)).getAttribute("location");
		return new File(directory, a);
	}

	final File getGroupFile()
	{
		Element xmlGroups = (Element) document.getElementsByTagName("group").item(0);
		if (xmlGroups == null)
			return null;
		String a=((Element)xmlGroups.getElementsByTagName("file").item(0)).getAttribute("location");
		return new File(directory, a);
	}

	/**
	 * @param xmlGroups the xml element of DOM tree corresponding to the tag "groups".
	 * @param id a group id.
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
	
	private void readGroups() throws IOException
	{
		IntFileReader ifrG = new PrimitiveFileReaderFactory().getIntReader(getGroupFile());
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
			
			String os=((Element)e.getElementsByTagName("file").item(0)).getAttribute("offset");
			int offset=Integer.parseInt(os);
			
			ifrG.get(offset, groups[i]);
		}
	}
	
	private int[] readTriangles() throws IOException
	{
		IntFileReader ifrT = new PrimitiveFileReaderFactory().getIntReader(getTriaFile());
		int[] toReturn = new int[numberOfTriangles * 3];
		int count = 0;
		for (int i = 0; i < groups.length; i++)
		{
			for (int j = 0; j < groups[i].length; j++)
			{
				ifrT.get(groups[i][j] * 3, toReturn, count, 3);
				count += 3;
			}
		}
		return toReturn;
	}	
	
	public final void write(PrintStream out) throws ParserConfigurationException, SAXException, IOException
	{
		readGroups();
		int[] nodeIDs=readTriangles();
		TIntIntHashMap amibeNodeToUNVNode=new TIntIntHashMap();
		writeInit(out);
		TIntHashSet nodeset = new TIntHashSet(nodeIDs);
		TIntArrayList nodelist = new TIntArrayList(nodeset.toArray());
		nodelist.sort();
		writeNodes(out, nodelist.toNativeArray(), amibeNodeToUNVNode);
		TIntIntHashMap amibeTriaToUNVTria=new TIntIntHashMap();
		writeTriangles(out, nodeIDs, amibeNodeToUNVNode, amibeTriaToUNVTria);
		try
		{
			// Do not complain if normals had not been written
			writeNormals(out, nodeIDs, amibeNodeToUNVNode, amibeTriaToUNVTria);
		}
		catch (IOException ex)
		{
		}
		nodeIDs=null;
		amibeNodeToUNVNode=null;
		writeGroups(out, amibeTriaToUNVTria);
		writeFinish(out);
	}
	
	/**
	 * @param fileName The UNV filename. If the name ends with ".gz" it will
	 * be zlib compressed.
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public final void write(String fileName)
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
	
	protected abstract void writeNodes(PrintStream out, int[] nodesID,
		TIntIntHashMap amibeToUNV) throws IOException;
	
	protected abstract void writeTriangles(PrintStream out, int[] triangles,
		TIntIntHashMap amibeNodeToUNVNode,
		TIntIntHashMap amibeTriaToUNVTria) throws IOException;
	
	void writeInit(PrintStream out)
		throws IOException
	{
		//To be implemented by instanciating class
	}
	
	void writeFinish(PrintStream out)
		throws IOException
	{
		//To be implemented by instanciating class
	}
	
	void writeNormals(PrintStream out, int[] triangles,
		TIntIntHashMap amibeNodeToUNVNode,
		TIntIntHashMap amibeTriaToUNVTria) throws IOException
	{
		//To be implemented by instanciating class
	}
	
	void writeGroups(PrintStream out,
		TIntIntHashMap amibeTriaToUNVTria) throws IOException
	{
		//To be implemented by instanciating class
	}
	
	public static class UNV extends MeshExporter
	{
		public static enum Unit
		{
			METER,
			MM,
			Unknown
		}
		private Unit unit = Unit.METER;
		
		public UNV(File directory, int[] groupIds)
		{
			super(directory, groupIds);
		}
		
		public UNV(String file)
		{
			super(file);
		}

		public final void setUnit(Unit unit)
		{
			this.unit=unit;
		}
		
		public static void writeSingleNode(PrintStream out, int count, double x, double y, double z)
		{
			out.println(FORMAT_I10.format(count)+"         1         1         1");
			out.println(FORMAT_D25_16.format(x)+FORMAT_D25_16.format(y)+FORMAT_D25_16.format(z));
		}

		// We use our own convention for elements:
		//      2: linear beam
		//      3: linear triangle
		//      4: linear tetrahedron
		public static void writeSingleLinearElement(PrintStream out, int count, int[] ids)
		{
			int type;
			if (ids[0] == 2)
			{
				assert ids.length == 3;
				type = 21;
			}
			else if (ids[0] == 3)
			{
				assert ids.length == 4;
				type = 91;
			}
			else if (ids[0] == 4)
			{
				assert ids.length == 5;
				type = 111;
			}
			else
				throw new IllegalArgumentException();
			out.println(FORMAT_I10.format(count)+FORMAT_I10.format(type)+"         1         1         1"+FORMAT_I10.format(ids.length-1));
			for (int i = 1; i < ids.length; i++)
				out.print(FORMAT_I10.format(ids[i]));
			out.println();
		}

		public static void writeSingleTriangle(PrintStream out, int count, int n0, int n1, int n2)
		{
			out.println(FORMAT_I10.format(count)+"        91         1         1         1         3");
			out.println(FORMAT_I10.format(n0)+FORMAT_I10.format(n1)+FORMAT_I10.format(n2));
		}

		public static void writeSingleGroup(PrintStream out, int groupId, String name, int[] ids)
		{
			out.println(FORMAT_I10.format(groupId)+"         0         0         0         0         0         0"+FORMAT_I10.format(ids.length));
			out.println(name);
			boolean newline = true;
			for(int j : ids)
			{
				out.print(FORMAT_I10.format(8)+FORMAT_I10.format(j)+FORMAT_I10.format(0)+FORMAT_I10.format(0));
				newline = !newline;
				if (newline)
					out.println("");
			}
			if (!newline)
				out.println();
		}

		@Override
		public final void writeInit(PrintStream arg0)
			throws IOException
		{
			if(unit.equals(Unit.Unknown))
				return;

			arg0.println("    -1");
			arg0.println("   164");
			if(unit.equals(Unit.MM))
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
		
		@Override
		public void writeNodes(PrintStream out, int[] nodesID, TIntIntHashMap amibeToUNV) throws IOException
		{
			File f=getNodeFile();
			DoubleFileReader dfrN = new PrimitiveFileReaderFactory().getDoubleReader(f);

			out.println("    -1"+CR+"  2411");
			int count =  0;
			double x,y,z;
			for(int i=0; i<nodesID.length; i++)
			{
				int iid=nodesID[i]*3;
				x=dfrN.get(iid);
				y=dfrN.get(iid+1);
				z=dfrN.get(iid+2);
				count++;
				amibeToUNV.put(nodesID[i], count);
				writeSingleNode(out, count, x, y, z);
			}
			out.println("    -1");
			dfrN.close();
			logger.info("Total number of nodes: "+count);
		}
		
		/**
		 * @param out
		 * @param amibeNodeToUNVNode
		 */
		@Override
		public void writeTriangles(PrintStream out, int[] triangles,
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
					writeSingleTriangle(out, count,
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
		@Override
		public void writeGroups(PrintStream out, TIntIntHashMap amibeTriaToUNVTria)
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
	
	public static class STL extends MeshExporter
	{		
		public STL(File directory, int[] groupIds)
		{
			super(directory, groupIds);
		}

		public STL(String file)
		{
			super(file);
		}
		
		/**
		 * @param out
		 * @param amibeNodeToUNVNode
		 */
		@Override
		public void writeTriangles(PrintStream out, int[] triangles,
			TIntIntHashMap amibeNodeToUNVNode, TIntIntHashMap amibeTriaToUNVTria)
			throws IOException
		{
			File f=getNodeFile();
			DoubleFileReader dfrN = new PrimitiveFileReaderFactory().getDoubleReader(f);
			
			out.println("solid export");
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
						x=dfrN.get(iid);
						y=dfrN.get(iid+1);
						z=dfrN.get(iid+2);
						out.println("     vertex "+x+" "+y+" "+z);
					}
					out.println("   endloop");
					out.println("endfacet");
					count++;
				}
			}
			dfrN.close();
			out.println("endsolid export");
			logger.info("Total number of triangles: "+count);
		}

		@Override
		protected void writeNodes(PrintStream out, int[] nodesID, TIntIntHashMap amibeToUNV)
		{
			//Nothing to do
		}
	}
	
	public static class MESH extends MeshExporter
	{
		public MESH(File directory, int[] groupIds)
		{
			super(directory, groupIds);
		}
		
		public MESH(String file)
		{
			super(file);
		}		
		
		@Override
		public void writeInit(PrintStream out)
			throws IOException
		{
			out.println("\nMeshVersionFormatted 1\n\nDimension\n3");
		}
		@Override
		public void writeFinish(PrintStream out)
			throws IOException
		{
			out.println("\nEnd");
		}
		@Override
		public void writeNodes(PrintStream out, int[] nodesID, TIntIntHashMap amibeToUNV) throws IOException
		{
			File f=getNodeFile();
			DoubleFileReader dfrN = new PrimitiveFileReaderFactory().getDoubleReader(f);
			
			int count =  0;
			double x,y,z;
			out.println("\nVertices\n"+nodesID.length);
			for(int i=0; i<nodesID.length; i++)
			{
				int iid=nodesID[i]*3;
				x=dfrN.get(iid);
				y=dfrN.get(iid+1);
				z=dfrN.get(iid+2);
				count++;
				amibeToUNV.put(nodesID[i], count);
				out.println(x+" "+y+" "+z+" 0");
			}
			dfrN.close();
			logger.info("Total number of nodes: "+count);
		}
		
		/**
		 * @param out
		 * @param amibeNodeToUNVNode
		 */
		@Override
		public void writeTriangles(PrintStream out, int[] triangles,
			TIntIntHashMap amibeNodeToUNVNode, TIntIntHashMap amibeTriaToUNVTria)
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
		
		@Override
		public void writeNormals(PrintStream out, int[] triangles,
			TIntIntHashMap amibeNodeToUNVNode, TIntIntHashMap amibeTriaToUNVTria) throws IOException
		{
			//  Open the input file first so that an exception is
			//  raised if it is not found.
			File f=getNormalFile();
			if (f == null)
				throw new IOException();
			DoubleFileReader dfrN = new PrimitiveFileReaderFactory().getDoubleReader(f);
			
			int count=0;
			for(int i=0; i<groups.length; i++)
				count += groups[i].length;
			
			out.println("\nNormals\n"+(3*count));

			double x,y,z;
			for(int i=0; i<groups.length; i++)
			{
				for(int j=0; j<groups[i].length; j++)
				{
					int iid = (amibeTriaToUNVTria.get(groups[i][j]) - 1)* 9;
					for (int k = 0; k < 3; k++)
					{
						x=dfrN.get(iid);
						y=dfrN.get(iid+1);
						z=dfrN.get(iid+2);
						out.println(x+" "+y+" "+z);
						iid += 3;
					}
				}
			}
			dfrN.close();

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
	
	public static class POLY extends MeshExporter
	{
		public POLY(File directory, int[] groupIds)
		{
			super(directory, groupIds);
		}
		
		public POLY(String file)
		{
			super(file);
		}
		
		@Override
		public void writeFinish(PrintStream out)
			throws IOException
		{
			out.println("# Part 3 - hole list");
			out.println("0");
			out.println("# Part 4 - hole list");
			out.println("0");
		}
		@Override
		public void writeNodes(PrintStream out, int[] nodesID, TIntIntHashMap amibeToUNV) throws IOException
		{
			File f=getNodeFile();
			DoubleFileReader dfrN = new PrimitiveFileReaderFactory().getDoubleReader(f);
			
			int count =  0;
			double x,y,z;
			out.println("# Part 1 - node list");
			out.println(""+nodesID.length+" 3 0 0");
			for(int i=0; i<nodesID.length; i++)
			{
				int iid=nodesID[i]*3;
				x=dfrN.get(iid);
				y=dfrN.get(iid+1);
				z=dfrN.get(iid+2);
				count++;
				amibeToUNV.put(nodesID[i], count);
				out.println("   "+count+" "+x+" "+y+" "+z);
			}
			dfrN.close();
			logger.info("Total number of nodes: "+count);
		}
		
		/**
		 * @param out
		 * @param amibeNodeToUNVNode
		 */
		@Override
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

	/**
	 * Convert an Amibe mesh to a VTK file.
	 * Output file extension should be <cite>.vtp</cite>.
	 * The documentation of the file format may be found here:
	 * <a href="http://www.vtk.org/pdf/file-formats.pdf">
	 * http://www.vtk.org/pdf/file-formats.pdf</a>
	 * @todo output one VTK piece by mesh group, support VTK parellel files.
	 * @author Jerome Robert
	 */
	public static class VTK extends MeshExporter
	{
		private boolean dummyData = true;
		
		public VTK(File directory, int[] groupIds)
		{
			super(directory, groupIds);
		}
		
		public VTK(String file)
		{
			super(file);
		}

		private static long computeNumberOfTriangle(File triaFile) throws IOException
		{
			DataInputStream in = new DataInputStream(
				new BufferedInputStream(new FileInputStream(triaFile)));
			long nbt = triaFile.length()/4/3;
			long toReturn = 0;
			for(int i=0; i<nbt; i++)
			{
				if(in.readInt()>=0)
					toReturn++;
				in.skipBytes(8);
			}
			return toReturn;
		}

		/**
		 * Write VTK header file
		 * @param out The stream to write on 
		 */
		@Override
		public void writeInit(PrintStream out)
			throws IOException
		{
			long numberOfNodes=getNodeFile().length()/8/3;
			long numberOfTriangles=computeNumberOfTriangle(getTriaFile());
			//This is Java so we write in big endian		
			out.println("<VTKFile type=\"PolyData\" version=\"0.1\" byte_order=\"BigEndian\">");
			out.println("<PolyData>");
			
			//Everything in one piece
			//TODO write one piece by group
			out.println("<Piece NumberOfPoints=\""+numberOfNodes+
				"\" NumberOfPolys=\""+numberOfTriangles+"\">");
			
			out.println("<Points><DataArray type=\"Float64\" NumberOfComponents=\"3\" "+
				"format=\"appended\" offset=\"0\"/></Points>");		
			long offset=4+(numberOfNodes*8*3);		
			
			out.println("<Polys><DataArray type=\"Int32\" Name=\"connectivity\""+
				" format=\"appended\" offset=\""+offset+"\"/>");
			offset+=4+numberOfTriangles*4*3;
			
			out.println("<DataArray type=\"Int32\" Name=\"offsets\" format=\"appended\"" +
				" offset=\""+offset+"\"/></Polys>");		
			offset+=4+numberOfTriangles*4;
			
			if(dummyData)
			{
				out.println("<CellData Scalars=\"Dummy\">");
				out.println("\t<DataArray type=\"Float64\" Name=\"Dummy\" format=\"appended\" offset=\""
					+offset+"\"/>");
				offset += 4+numberOfTriangles * 8;
		
				out.println("\t<DataArray type=\"Float64\" Name=\"Dummy x Dummy\" format=\"appended\" offset=\""
					+offset+"\"/>");
				//always keep track of offset in case we want to add thins to the
				//file
				offset += 4+numberOfTriangles * 8;
				
				out.println("\t<DataArray type=\"Float64\" Name=\"Dummy vector\" NumberOfComponents=\"3\""+
					" format=\"appended\" offset=\""+offset+"\"/>");
				//always keep track of offset in case we want to add thins to the
				//file
				offset += 4+numberOfTriangles*8*3;			
				out.println("</CellData>");
			}
			
			out.println("</Piece></PolyData>");
			out.print("<AppendedData encoding=\"raw\"> _");
			out.flush();
		}
	
		/**
		 * Write nodes nodes of the mesh
		 * @param out stream to write on
		 * @throws IOException
		 */
		@Override
		protected void writeNodes(PrintStream out, int[] nodesID, TIntIntHashMap amibeToUNV)
			throws IOException
		{
			DataOutputStream dos=new DataOutputStream(new BufferedOutputStream(out));
			//Write the size of the array in octets
			dos.writeInt(nodesID.length*8*3);
			File f=getNodeFile();
			DoubleFileReader dfrN = new PrimitiveFileReaderFactory().getDoubleReader(f);
			
			int count =  0;
			double x,y,z;
			for(int i=0; i<nodesID.length; i++)
			{
				int iid=nodesID[i]*3;
				x=dfrN.get(iid);
				y=dfrN.get(iid+1);
				z=dfrN.get(iid+2);
				amibeToUNV.put(nodesID[i], count);
				dos.writeDouble(x);
				dos.writeDouble(y);
				dos.writeDouble(z);
				count++;
			}
			dfrN.close();
			logger.info("Total number of nodes: "+count);
			dos.flush();
			out.flush();
		}
		 
		/**
		 * Write triangle connectivity
		 * @param out the stream to write on
		 * @throws IOException
		 */
		@Override
		protected void writeTriangles(PrintStream out, int[] triangles,
			TIntIntHashMap amibeNodeToUNVNode, TIntIntHashMap amibeTriaToUNVTria)
			throws IOException
		{
			DataOutputStream dos=new DataOutputStream(new BufferedOutputStream(out));
			//Write the size of the array in octets
			int nbt = triangles.length/3;
			dos.writeInt(nbt*4*3);
			int count=0;
			int triaIndex=0;
			for(int i=0; i<groups.length; i++)
			{
				for(int j=0; j<groups[i].length; j++)
				{
					amibeTriaToUNVTria.put(groups[i][j], count);
					dos.writeInt(amibeNodeToUNVNode.get(triangles[triaIndex++]));
					dos.writeInt(amibeNodeToUNVNode.get(triangles[triaIndex++]));
					dos.writeInt(amibeNodeToUNVNode.get(triangles[triaIndex++]));
					count++;
				}
			}
			logger.info("Total number of triangles: "+count);
			//Write the size of the array in octets
			dos.writeInt(nbt*4);
			//Write the offset of each cells (in our case triangles) in the
			//connectivity array
			for(int i=1; i<=nbt; i++)
				dos.writeInt(3*i);
			dos.flush();
			out.flush();
		}
		
		@Override
		protected void writeFinish(PrintStream out)
			throws IOException
		{
			DataOutputStream dos=new DataOutputStream(new BufferedOutputStream(out));
			if(dummyData)
			{
				long nbt=getTriaFile().length()/4/3;
				//Write the size of the array in octets
				dos.writeInt((int) nbt*8);		
				for(int i=0; i<nbt; i++)
					dos.writeDouble(i);
				
				dos.writeInt((int) nbt*8);
				for(int i=0; i<nbt; i++)
					dos.writeDouble((double)i*i);
				dos.writeInt((int) nbt*8*3);
				for(int i=0; i<nbt; i++)
				{
					dos.writeDouble(i);
					dos.writeDouble(i);
					dos.writeDouble(i);
				}
			}
			dos.flush();
			out.println("</AppendedData></VTKFile>");
			out.flush();
		}
		
		public boolean isDummyData()
		{
			return dummyData;
		}
		
		/**
		 * Write data cell associated to triangles
		 * It's a scalar double value which is the ID of the triangle.
		 * It won't help you much, it's just to have the code somewhere?
		 */
		public void setDummyData(boolean dummyData)
		{
			this.dummyData = dummyData;
		}
	}
	
}
