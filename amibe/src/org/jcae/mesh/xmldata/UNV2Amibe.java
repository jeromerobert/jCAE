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
 * (C) Copyright 2007-2010, by EADS France
 */

package org.jcae.mesh.xmldata;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;


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
	private final static Pattern TOKENIZE = Pattern.compile("\\s+");
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

	private class Element24 extends Element {

		public Element24(String line, BufferedReader in) throws IOException {
			super(line, in);
		}

		@Override
		protected String parse(BufferedReader in) throws IOException {
			nodes = new int[3];
			String l1 = in.readLine();
			String l2 = in.readLine();
			StringTokenizer st = new StringTokenizer(l2);
			nodes[0] = Integer.parseInt(st.nextToken());
			nodes[1] = Integer.parseInt(st.nextToken());
			nodes[2] = Integer.parseInt(st.nextToken());
			return l1 + '\n' + l2;
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

	/**
	 * Map an UNV element ID with Amibe element ID.
	 * This is required because UNV file mix beams and trias while amibe
	 * separate them
	 * This is done in a temporary file to save memory
	 */
	private static class IDMapping
	{
		/**
		 * Element type in the temporary file used to separate beams from trias
		 * in groups.
		 */
		public static int TRIAS = 0, BEAMS = 1;
		private FileChannel channel;
		private File file;
		private ByteBuffer buffer = ByteBuffer.allocate(8);
		public IDMapping() throws IOException
		{
			file = File.createTempFile("amibe", ".bin");
			file.deleteOnExit();
			channel = new RandomAccessFile(file, "rw").getChannel();
		}

		public void close() throws IOException
		{
			channel.close();
			file.delete();
		}

		public void add(int amibeID, int type) throws IOException
		{
			((Buffer)buffer).rewind();
			buffer.putInt(amibeID);
			buffer.putInt(type);
			buffer.rewind();			
			channel.write(buffer);
		}

		public void seek(int id) throws IOException
		{
			buffer.rewind();
			channel.read(buffer, 8*id);
		}

		public int getID()
		{
			return buffer.getInt(0);
		}

		public int getType()
		{
			return buffer.getInt(4);
		}
	}
	
	private static final Logger LOGGER=Logger.getLogger(UNV2Amibe.class.getName());
	private String unitBlock;	
	private String stripedUnvFile;
	private IDMapping idMapping;
	private double scale = 1.0;

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
		if(stripedUnvFile!=null)
			writeStripedUnv(out);
	}
	
	private void importMesh(BufferedReader in, AmibeWriter.Dim3 out) throws IOException
	{
		idMapping = new IDMapping();
		double unit = 1.0;
		String line;
		while ((line=in.readLine())!=null)
		{
			if (line.trim().equals("-1"))
			{
				line = in.readLine().trim();
				if (line.equals("2411") || line.equals("781"))
				{
					// read nodes
					convertNodes(in, unit / scale, out);
				}
				else if (line.equals("2412"))
				{
					// read faces
					convertFaces(in, out);
				}
				else if (line.equals("164"))
				{
					// read unit
					unit = readUnit(in);
				}
				else if ( "2430".equals(line) || "2435".equals(line) ||
					"2477".equals(line) || "2467".equals(line))
				{
					// read groups
					convertGroups(in, line, out);
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
		idMapping.close();
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
		if(elements.isEmpty())
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

	protected boolean skipGroup(String name)
	{
		return false;
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
			String[] tokens = TOKENIZE.split(line);
			// Number of elements
			int nbelem = Integer.parseInt(tokens[tokens.length-1]);
			// Read group name
			String groupName = in.readLine().trim();
			boolean skipGroup = skipGroup(groupName);
			boolean groupCreated = false;
			line = in.readLine().trim();
			if(nbelem == 0)
				continue;
			while (line.charAt(0) == '8' || line.charAt(0) == '7')
			{
				StringTokenizer st = new StringTokenizer(line);
				// read one element over two, the first one doesnt matter
				while(st.hasMoreTokens())
				{
					st.nextToken();
					int ind = Integer.parseInt(st.nextToken());
					if (ind != 0 && !skipGroup)
					{
						ind --;
						if(line.charAt(0) == '8')
						{
							if(!groupCreated)
							{
								out.nextGroup(groupName);
								groupCreated = true;
							}
							idMapping.seek(ind);
							if(idMapping.getType() == IDMapping.BEAMS)
								out.addBeamToGroup(idMapping.getID());
							else
								out.addTriaToGroup(idMapping.getID());
						}
						else //line.charAt(0) == '7'
						{
							if(!groupCreated)
							{
								out.nextNodeGroup(groupName);
								groupCreated = true;
							}
							out.addNodeToGroup(ind);
						}
					}
					
					nbelem--;
					if ("2435".equals(type) || "2477".equals(type) || "2467".equals(type))
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
				line = in.readLine().trim();
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
		}
	}

	private void convertFaces(BufferedReader rd, AmibeWriter.Dim3 out) throws IOException
	{
		LOGGER.fine("Reading triangles");
		String line = "";
		int nbTrias = 0;
		int nbBeams = 0;
		while (!(line=rd.readLine()).trim().equals("-1"))
		{
			// first line: type of object
			StringTokenizer st = new StringTokenizer(line);
			st.nextToken(); // face index
			int type=Integer.parseInt(st.nextToken());
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
					int p1, p2, p3;
					p1 = Integer.parseInt(st.nextToken());
					p2 = Integer.parseInt(st.nextToken());
					p3 = Integer.parseInt(st.nextToken());
					out.addTriangle(p1-1, p2-1, p3-1);
					idMapping.add(nbTrias, IDMapping.TRIAS);
					nbTrias ++;
					break;
				case 94:
					break; //ignored
				case 11: //rod
				case 21: //linear beam
					Element21 beam = new Element21(line, rd);
					out.addBeam(beam.getNode(0)-1, beam.getNode(1)-1);
					idMapping.add(nbBeams, IDMapping.BEAMS);
					nbBeams ++;
					break;
				case 22:
				case 24:  // parabolic beam
					Element24 b = new Element24(line, rd);
					out.addBeam(b.getNode(0) - 1, b.getNode(2) - 1);
					idMapping.add(nbBeams, IDMapping.BEAMS);
					nbBeams++;
					break;
				case 42:
				case 92: //parabolic triangles
					Element92 e=new Element92(line, rd);
					out.addTriangle(e.getNode(0)-1, e.getNode(2)-1, e.getNode(4)-1);
					idMapping.add(nbTrias, IDMapping.TRIAS);
					nbTrias ++;
					break;
				case 118: //tetra
					//skip it
					rd.readLine();
					rd.readLine();
				case 111: //tetra
					//skip it
					rd.readLine();
					break;
				default:
					LOGGER.log(Level.WARNING, "Warning: Section 2412, type {0} unknown", type);
			}			
		}
	}
	
	public final void setStripedUnv(String file)
	{
		stripedUnvFile=file;
	}

	public void setScale(double scale) {
		this.scale = scale;
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
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}
}
