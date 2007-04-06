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
import java.util.StringTokenizer;
import org.apache.log4j.Logger;


/**
 * Convert a UNV mesh to an Amibe mesh.
 * The convertion is out-of-core and can handle large mesh.
 * The nodes and triangles of the input file must have contiguous ids.
 * Only triangles, nodes, and groups are imported.
 * If the input file do not contains any groups, a group with all triangles is
 * created
 * @author Jerome Robert
 */
public class UNV2Amibe
{
	private static class Group
	{
		String name;
		long offset;
		int nbElement;
	}
	
	private static Logger logger=Logger.getLogger(UNV2Amibe.class);
		
	private int numberOfNodes, numberOfTriangles;
	private ArrayList groups=new ArrayList();
	
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
		FileChannel cnode=new FileOutputStream(fnode).getChannel();
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
			System.out.println(line);
			if (line.trim().equals("-1"))
			{
				line = in.readLine();
				System.out.println(line);
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

	private static double readUnit(BufferedReader rd) throws IOException
	{
		double unit = 1.0;
		String line = "";
		//retrieve the second line
		
		line = rd.readLine();
		line = rd.readLine();
		
		// fisrt number : the unit
		StringTokenizer st = new StringTokenizer(line);
		String unite = st.nextToken();
		unite = unite.replace('D','E');
		unit = new Double(unite).doubleValue();
		while(!(line=rd.readLine().trim()).equals("-1"));

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
		
		while (!(line=rd.readLine().trim()).equals("-1"))
		{
			// first line: type of object
			StringTokenizer st = new StringTokenizer(line);
			st.nextToken(); // face index
			String type = st.nextToken();

			if (type.equals("74") || type.equals("91"))
			{
				line=rd.readLine();
				// triangle
				st = new StringTokenizer(line);
				int p1 = Integer.parseInt(st.nextToken());
				int p2 = Integer.parseInt(st.nextToken());
				int p3 = Integer.parseInt(st.nextToken());					

				bb.putInt(p1-1);
				bb.putInt(p2-1);
				bb.putInt(p3-1);
				bb.rewind();
				faceChannel.write(bb);
				bb.rewind();
				numberOfTriangles++;
			}
			else if (type.equals("94"))
			{
				//ignored
			}
			else
				throw new RuntimeException("Type "+type+" unknown");
		}
	}
	
	public static void main(String[] args)
	{
		try
		{
			new UNV2Amibe().importMesh("/tmp/cobraFuselage_0_r16.unv", "/tmp");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
