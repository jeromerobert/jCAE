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
 * (C) Copyright 2007, by EADS
 */

package org.jcae.mesh.xmldata;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;


/**
 * Convert a UNV mesh to an Amibe mesh.
 * <ul>
 * <li>The convertion is out-of-core and can handle large mesh.</li>
 * <li>The nodes and triangles of the input file must have contiguous ids.</li>
 * <li>Only triangles, nodes, and groups are imported.</li>
 * <li>2412-21 (linear beams) and 2412-92 (parabolic trias) are exported as a minimal unv
 * if setStripedUnv as been called.</li>
 * <li>If the input file do not contains any groups, a group with all triangles is
 * created.</li>
 * </ul>
 * @author Jerome Robert
 */
public class UNV2Amibe
{
	/** A 2412 element which won't be stored into the amibe file */
	private abstract class Element
	{
		private String buffer;
		protected int[] nodes;
				
		protected abstract String parse(BufferedReader in) throws IOException;
		
		public Element(String line, BufferedReader in) throws IOException
		{
			buffer=line+'\n'+parse(in);
		}
		
		public void write(PrintStream out)
		{
			out.println(buffer);
		}
		
		public int getNode(int id)
		{
			return nodes[id];
		}
		
		public int getNbNodes()
		{
			return nodes.length;
		}
	}
	
	private class Element21 extends Element
	{		
		public Element21(String line, BufferedReader in) throws IOException
		{
			super(line, in);
		}

		protected String parse(BufferedReader in) throws IOException
		{
			nodes=new int[2];
			String l1=in.readLine();
			String l2=in.readLine();
			StringTokenizer st=new StringTokenizer(l2);
			nodes[0]=Integer.parseInt(st.nextToken());
			nodes[1]=Integer.parseInt(st.nextToken());
			return l1+'\n'+l2;
		}		
	}

	private class Element92 extends Element
	{		
		public Element92(String line, BufferedReader in) throws IOException
		{
			super(line, in);
		}

		protected String parse(BufferedReader in) throws IOException
		{
			nodes=new int[6];
			String l1=in.readLine();
			StringTokenizer st=new StringTokenizer(l1);
			for(int i=0; i<6; i++)
				nodes[i]=Integer.parseInt(st.nextToken());
			
			return l1;
		}		
	}	
	/**
	 * Contains informations about groups, which will be written to the
	 * XML file
	 */
	private static class Group
	{
		String name;
		long offset;
		int nbElement;
	}
	
	private static Logger logger=Logger.getLogger(UNV2Amibe.class);
	private String unitBlock;
		
	private int numberOfNodes, numberOfTriangles;
	private ArrayList groups=new ArrayList();
	private String stripedUnvFile;
	
	/** a list of 2412 elements which won't be store in the amibe file */
	private ArrayList elements=new ArrayList();
	
	public void importMesh(String input, String output) throws IOException
	{
		importMesh(new File(input), output);
	}

	public void importMesh(File input, String output) throws IOException
	{
		BufferedReader br=new BufferedReader(new FileReader(input));
		importMesh(br, new File(output));
		br.close();
	}

	public void importMesh(BufferedReader in, File outputDir) throws IOException
	{
		outputDir.mkdirs();
		File dir3d=new File(outputDir, "jcae3d.files");
		dir3d.mkdirs();
		File fnode=new File(dir3d, "nodes3d.bin");
		File ftria=new File(dir3d, "triangles3d.bin");
		File fgrp=new File(dir3d, "groups.bin");
		FileChannel cnode=new RandomAccessFile(fnode, "rw").getChannel();
		FileChannel ctria=new FileOutputStream(ftria).getChannel();
		FileChannel cgroups=new FileOutputStream(fgrp).getChannel();
		importMesh(in, cnode, ctria, cgroups);
		checkNoGroup(cgroups);
		cnode.close();
		ctria.close();
		cgroups.close();
		PrintWriter xml=new PrintWriter(new File(outputDir,"jcae3d"));
		writeXML(xml);		
		xml.close();
	}
	
	/**
	 * If the unv do not contains any groups, create one with all
	 * elements 
	 */
	private void checkNoGroup(FileChannel cgroups) throws IOException
	{
		if(groups.size()==0)
		{
			Group g=new Group();
			g.name="EXT";
			g.nbElement=numberOfTriangles;
			g.offset=0;
			groups.add(g);
			ByteBuffer bb=ByteBuffer.allocate(4);
			for(int i=0; i<numberOfTriangles; i++)
			{
				bb.putInt(i);
				bb.rewind();
				cgroups.write(bb);
				bb.rewind();
			}
		}
	}
	
	private void writeXML(PrintWriter writer)
	{
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		writer.println("<!DOCTYPE jcae SYSTEM \"classpath:///org/jcae/mesh/xmldata/jcae.dtd\">");
		writer.println("<jcae><mesh><submesh><nodes>");
		writer.println("<number>"+numberOfNodes+"</number>");
		writer.println("<file format=\"doublestream\" location=\"jcae3d.files/nodes3d.bin\"/>");
		writer.println("</nodes><triangles>");
		writer.println("<number>"+numberOfTriangles+"</number>");
		writer.println("<file format=\"integerstream\" location=\"jcae3d.files/triangles3d.bin\"/>");
		writer.println("</triangles><groups>");
		
		for(int i=0; i<groups.size(); i++)
		{
			Group g=(Group) groups.get(i);
			writer.println("<group id=\""+i+"\">");
			writer.println("<name>"+g.name+"</name>");
			writer.println("<number>"+g.nbElement+"</number>");			
			writer.println(
				"<file format=\"integerstream\" location=\"jcae3d.files/groups.bin\" offset=\""+
				g.offset+"\"/>");
			writer.println("</group>");	
		}
		
		writer.println("</groups></submesh></mesh></jcae>");
	}
	
	private void importMesh(BufferedReader in,
		FileChannel nodeChannel,
		FileChannel faceChannel,
		FileChannel groupChannel) throws IOException
	{
		double unit = 1.0;
		String line;
		while ((line=in.readLine())!=null)
		{
			if (line.trim().equals("-1"))
			{
				line = in.readLine();
				if (line.trim().equals("2411") || line.trim().equals("781"))
				{
					// read nodes
					convertNodes(in, unit, nodeChannel);
				}
				else if (line.trim().equals("2412"))
				{
					// read faces
					convertFaces(in, faceChannel);
				}
				else if (line.trim().equals("164"))
				{
					// read unit
					unit = readUnit(in);
				}
				else if ( (line.trim().equals("2430")) || (line.trim().equals("2435")) )
				{
					// read groups
					convertGroups(in, groupChannel);
				}
				/*else if (line.trim().equals("2414"))
				{
					// read colors
				}*/
				else
				{
					// default group
					// read end of group
					while (!(line=in.readLine().trim()).equals("-1"));
				}
			}
		}
		if(stripedUnvFile!=null)
			writeStripedUnv(nodeChannel);
	}

	/** List of nodes used in elements which are not written in the amibe file */
	private int[] computeListOfNodes()
	{
		HashSet hs=new HashSet();
		for(int i=0; i<elements.size(); i++)
		{
			Element e=(Element)elements.get(i);
			for(int j=0; j<e.getNbNodes(); j++)
				hs.add(Integer.valueOf(e.getNode(j)));					
		}
		
		int[] toReturn=new int[hs.size()];
		Iterator it=hs.iterator();
		
		int k=0;
		while(it.hasNext())
			toReturn[k++]=((Integer)it.next()).intValue();
		
		return toReturn;
	}
	
	private void writeStripedUnv(FileChannel nodeChannel) throws IOException
	{
		if(elements.size()==0)
			return;
		PrintStream stripedUnv=new PrintStream(new FileOutputStream(stripedUnvFile));
		stripedUnv.println("    -1");
		stripedUnv.println("   164");
		stripedUnv.println(unitBlock);
		stripedUnv.println("    -1");
		stripedUnv.println("    -1");
		stripedUnv.println("  2411");
		//write nodes
		int[] nodes=computeListOfNodes();
		ByteBuffer bb=ByteBuffer.allocate(3*8);
		for(int i=0; i<nodes.length; i++)
		{
			nodeChannel.read(bb, 3*8*nodes[i]);
			bb.rewind();
			MeshExporter.writeSingleNodeUNV(stripedUnv, nodes[i],
				bb.getDouble(), bb.getDouble(), bb.getDouble());
			bb.rewind();
		}
		stripedUnv.println("    -1");
		stripedUnv.println("    -1");
		stripedUnv.println("  2412");
		for(int i=0; i<elements.size(); i++)
		{
			Element e=(Element)elements.get(i);
			stripedUnv.println(e.buffer);
		}
		//write elements
		stripedUnv.println("    -1");
		//don't write groups for now :-(
		stripedUnv.close();
	}

	/**
	 * 
	 * @param in
	 * @return ArrayList of Group
	 * @throws IOException
	 */
	private void convertGroups(BufferedReader in, FileChannel gChannel) throws IOException
	{
		String line = in.readLine();
		long offset=0;
		ByteBuffer bb=ByteBuffer.allocate(4);
		while(!line.trim().equals("-1"))
		{
			// read the number of elements to read in the last number of the line
			StringTokenizer st = new StringTokenizer(line);
			Group g=new Group();
			// Read group name
			g.name = in.readLine().trim();
			g.offset=offset;
			// read the group
			while ((line= in.readLine().trim()).startsWith("8"))
			{
				st = new StringTokenizer(line);
				// read one element over two, the first one doesnt matter
				while(st.hasMoreTokens())
				{
					st.nextToken();
					int ind = Integer.parseInt(st.nextToken());
					if (ind != 0)
					{
						g.nbElement++;
						offset++;
						bb.putInt(ind-1);
						bb.rewind();
						gChannel.write(bb);
						bb.rewind();
					}
				}
			}
			groups.add(g);
		}
	}

	private double readUnit(BufferedReader rd) throws IOException
	{
		double unit = 1.0;
		String line = "";
		//retrieve the second line		
		
		unitBlock = rd.readLine()+'\n';
		line = rd.readLine();
		unitBlock += line +'\n';
				
		// fisrt number : the unit
		StringTokenizer st = new StringTokenizer(line);
		String unite = st.nextToken();
		unite = unite.replace('D','E');
		unit = Double.parseDouble(unite); 
		while(!(line=rd.readLine().trim()).equals("-1"))
			unitBlock += line;

		return unit;
	}

	private void convertNodes(BufferedReader rd, double unit, FileChannel nodeChannel)
		throws IOException
	{
		double x,y,z;
		String line = "";
		ByteBuffer bb=ByteBuffer.allocate(3*8);
		while(!(line=rd.readLine().trim()).equals("-1"))
		{
			StringTokenizer st = new StringTokenizer(line);
			st.nextToken(); //node id
			line = rd.readLine();
			
			//line contains coord x,y,z
			st = new StringTokenizer(line);
			String x1 = st.nextToken();
			String y1 = st.nextToken();
			String z1;
			try
			{
				z1 = st.nextToken();
			}
			catch (java.util.NoSuchElementException ex)
			{
				z1="0.0";
			}
			
			x1 = x1.replace('D','E');
			y1 = y1.replace('D','E');
			z1 = z1.replace('D','E');			
			x = Double.parseDouble(x1)/unit;
			y = Double.parseDouble(y1)/unit;
			z = Double.parseDouble(z1)/unit;
			bb.putDouble(x);
			bb.putDouble(y);
			bb.putDouble(z);
			bb.rewind();
			nodeChannel.write(bb);
			bb.rewind();
			numberOfNodes++;
		}
	}

	private void convertFaces(BufferedReader rd, FileChannel faceChannel) throws IOException
	{
		logger.debug("Reading triangles");
		String line = "";

		ByteBuffer bb=ByteBuffer.allocate(3*4);
		int p1, p2, p3;
		while (!(line=rd.readLine()).trim().equals("-1"))
		{
			// first line: type of object
			StringTokenizer st = new StringTokenizer(line);
			st.nextToken(); // face index
			int type=Integer.parseInt(st.nextToken());
			//write degenerated triangle if
			p1 = 0; p2 = 0; p3 = 0;  
			switch(type)
			{
				case 74:
				case 91:
					line=rd.readLine();
					// triangle
					st = new StringTokenizer(line);
					p1 = Integer.parseInt(st.nextToken());
					p2 = Integer.parseInt(st.nextToken());
					p3 = Integer.parseInt(st.nextToken());					
					break;
				case 94: break; //ignored
				case 21: //linear beam
					elements.add(new Element21(line, rd));
					break;
				case 92: //parabolic triangles
					Element92 e=new Element92(line, rd);
					elements.add(e);
					p1=e.getNode(0);
					p2=e.getNode(2);
					p3=e.getNode(4);					
					break;
				default:
					System.out.println("Warning: Section 2412, type "+type+" unknown");
			}
			bb.putInt(p1-1);
			bb.putInt(p2-1);
			bb.putInt(p3-1);
			bb.rewind();
			faceChannel.write(bb);
			bb.rewind();
			numberOfTriangles++;
		}
	}
	
	public void setStripedUnv(String file)
	{
		stripedUnvFile=file;
	}
	
	public static void main(String[] args)
	{
		try
		{
			UNV2Amibe u=new UNV2Amibe();
			u.setStripedUnv("/tmp/toto.unv");
			u.importMesh("/home/jerome/Models/unv/FlightSMALL.unv", "/tmp");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
