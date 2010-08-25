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


import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.logging.Logger;


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
		String buffer;
		int[] nodes;
				
		protected abstract String parse(BufferedReader in) throws IOException;
		
		public Element(String line, BufferedReader in) throws IOException
		{
			buffer=line+'\n'+parse(in);
		}
		
		public void write(PrintStream out)
		{
			out.println(buffer);
		}
		
		public final int getNode(int id)
		{
			return nodes[id];
		}
		
		public final int getNbNodes()
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

		@Override
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

		@Override
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
	
	private static final Logger LOGGER=Logger.getLogger(UNV2Amibe.class.getName());
	private String unitBlock;
		
	private int numberOfNodes, numberOfTriangles;
	private String stripedUnvFile;
	
	/** a list of 2412 elements which won't be store in the amibe file */
	private final ArrayList<Element> elements=new ArrayList<Element>();
	
	public final void importMesh(String input, String output) throws IOException
	{
		importMesh(new File(input), output);
	}

	public final void importMesh(File input, String output) throws IOException
	{
		BufferedReader br=new BufferedReader(new FileReader(input));
		importMesh(br, output);
		br.close();
	}

	public final void importMesh(BufferedReader in, String outputDir) throws IOException
	{
		AmibeWriter.Dim3 out = new AmibeWriter.Dim3(outputDir);
		out.setFixNoGroup(true);
		importMesh(in, out);
		out.finish();
	}
	
	private void importMesh(BufferedReader in, AmibeWriter.Dim3 out) throws IOException
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
					convertNodes(in, unit, out);
				}
				else if (line.trim().equals("2412"))
				{
					// read faces
					convertFaces(in, out);
				}
				else if (line.trim().equals("164"))
				{
					// read unit
					unit = readUnit(in);
				}
				else if ( (line.trim().equals("2430")) || (line.trim().equals("2435")) )
				{
					// read groups
					convertGroups(in, line.trim(), out);
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
			writeStripedUnv(out);
	}

	/** List of nodes used in elements which are not written in the amibe file */
	private int[] computeListOfNodes()
	{
		TreeSet<Integer> hs=new TreeSet<Integer>();
		for(int i=0; i<elements.size(); i++)
		{
			Element e=elements.get(i);
			for(int j=0; j<e.getNbNodes(); j++)
				hs.add(Integer.valueOf(e.getNode(j)));					
		}
		
		int[] toReturn=new int[hs.size()];
		Iterator<Integer> it=hs.iterator();
		
		int k=0;
		while(it.hasNext())
			toReturn[k++]=it.next().intValue();
		
		return toReturn;
	}
	
	private void writeStripedUnv(AmibeWriter out) throws IOException
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
		double[] nc = new double[3];
		for(int i=0; i<nodes.length; i++)
		{
			out.getNode(nodes[i]-1, nc);
			MeshExporter.UNV.writeSingleNode(stripedUnv, nodes[i], nc[0], nc[1], nc[2]);
		}
		stripedUnv.println("    -1");
		stripedUnv.println("    -1");
		stripedUnv.println("  2412");
		for(int i=0; i<elements.size(); i++)
		{
			Element e=elements.get(i);
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
	private void convertGroups(BufferedReader in, String type, AmibeWriter out) throws IOException
	{
		String line = in.readLine();
		while(!line.trim().equals("-1"))
		{
			// read the number of elements to read in the last number of the line
			StringTokenizer st = new StringTokenizer(line);
			String snb = "";
			// Block number
			st.nextToken();
			while(st.hasMoreTokens())
			{
				snb = st.nextToken();
			}
			// Number of elements
			int nbelem = Integer.valueOf(snb).intValue();
			// Read group name
			out.nextGroup(in.readLine().trim());
			while ((line= in.readLine().trim()).startsWith("8"))
			{
				st = new StringTokenizer(line);
				// read one element over two, the first one doesnt matter
				while(st.hasMoreTokens())
				{
					st.nextToken();
					int ind = Integer.parseInt(st.nextToken());
					if (ind != 0)
						out.addTriaToGroup(ind-1);
					
					nbelem--;
					if (type.equals("2435"))
					{
						st.nextToken();
						st.nextToken();
					}
				}
				if  (nbelem <= 0)
				{
					line = in.readLine();
					break;
				}
			}
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

	private void convertNodes(BufferedReader rd, double unit, AmibeWriter.Dim3 out)
		throws IOException
	{
		double x,y,z;
		String line = "";		
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
			out.addNode(x, y, z);
			numberOfNodes++;
		}
	}

	private void convertFaces(BufferedReader rd, AmibeWriter.Dim3 out) throws IOException
	{
		LOGGER.fine("Reading triangles");
		String line = "";

		int p1, p2, p3;
		while (!(line=rd.readLine()).trim().equals("-1"))
		{
			// first line: type of object
			StringTokenizer st = new StringTokenizer(line);
			st.nextToken(); // face index
			int type=Integer.parseInt(st.nextToken());
			//write degenerated triangle if
			p1 = 1; p2 = 1; p3 = 1;  
			switch(type)
			{
				case 41:
				case 51:
				case 61:
				case 74:
				case 91:
					line=rd.readLine();
					// triangle
					st = new StringTokenizer(line);
					p1 = Integer.parseInt(st.nextToken());
					p2 = Integer.parseInt(st.nextToken());
					p3 = Integer.parseInt(st.nextToken());					
					break;
				case 94:
					break; //ignored
				case 11: //rod
				case 21: //linear beam
					elements.add(new Element21(line, rd));
					break;
				case 92: //parabolic triangles
					Element92 e=new Element92(line, rd);					
					p1=e.getNode(0);
					p2=e.getNode(2);
					p3=e.getNode(4);					
					break;
				case 118: //tetra
					//skip it
					rd.readLine();
					rd.readLine();
					break;
				default:
					System.out.println("Warning: Section 2412, type "+type+" unknown");
			}
			out.addTriangle(p1-1, p2-1, p3-1);
			numberOfTriangles++;
		}
	}
	
	public final void setStripedUnv(String file)
	{
		stripedUnvFile=file;
	}
	
	public static void main(String[] args)
	{
		try
		{
			UNV2Amibe u=new UNV2Amibe();
			String unvFile = "/home/jerome/Models/unv/FlightSMALL.unv";
			String stripedFile = "/tmp/FlightSMALL1-strp.unv";
			String amibeDir = "/tmp/pouet";
			if (args.length > 0)
				unvFile = args[0];
			if (args.length > 1)
				amibeDir = args[1];
			if (args.length > 2)
				stripedFile = args[2];
			u.setStripedUnv(stripedFile);
			u.importMesh(unvFile, amibeDir);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
