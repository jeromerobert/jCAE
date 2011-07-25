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
 * (C) Copyright 2011, by EADS France
 */

package org.jcae.mesh.xmldata;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Pierre Benjamin, Jerome Robert
 */
public class Capitole2Amibe
{
	/**
	 * Map a capitol element ID with Amibe element ID.
	 * This is required because capitol file mix beams and trias while amibe
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
			buffer.rewind();
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

	private static final Logger LOGGER=Logger.getLogger(Capitole2Amibe.class.getName());
	private int nbPoints;
        private int nbTria  ;
        private int nbBeam  ;
	private IDMapping idMapping;

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
		idMapping = new IDMapping();
		double unit = 1.0;
		String line;
		while ((line = in.readLine()) != null) {

			if (line.trim().equals("<POINT>")) {
				// read nodes
				convertNodes(in, nbPoints, unit, out);
			} else if (line.trim().equals("<TRIANGLE>")) {
				// read faces
				convertFaces(in, "TRIANGLE", nbTria, out);
			} else if (line.trim().equals("<WIRES>")) {
				// read faces
				convertFaces(in, "WIRE", nbBeam, out);
			} else if (line.trim().equals("<DIMENSION>")) {
				// read unit
				readDimension(in);
				LOGGER.log(Level.INFO, "NbPoints = {0}", nbPoints);
			}
		}
		idMapping.close();
	}

	private void readDimension(BufferedReader rd) throws IOException
	{
		String line = "";
		StringTokenizer st;
		//first line = nbPoints + comment
		line = rd.readLine();
		st = new StringTokenizer(line);
		this.nbPoints = Integer.parseInt(st.nextToken());

		//second line = nbTriangles + comment
		line = rd.readLine();
		st = new StringTokenizer(line);
		this.nbTria = Integer.parseInt(st.nextToken());

		//third line  = nbEdges   + comment: inutile
		line = rd.readLine();

		//fourth line = nbPoutres + comment
		line = rd.readLine();
		st = new StringTokenizer(line);
		this.nbBeam = Integer.parseInt(st.nextToken());
	}

	private void convertNodes(BufferedReader rd, int nbNodes, double unit, AmibeWriter.Dim3 out)
		throws IOException
	{
		double x,y,z;
		String line = "";
		// Ligne Commentaire
		//Index  T IdType   X      Y    Z  Status Domain
		line = rd.readLine();
		for (int i = 0; i < nbNodes; i++)
		{
			line = rd.readLine();
			//line contains coord x,y,z
			StringTokenizer st = new StringTokenizer(line);
                        st.nextToken();st.nextToken(); st.nextToken();
			String x1 = st.nextToken();
			String y1 = st.nextToken();
			String z1 = st.nextToken();

			x = Double.parseDouble(x1)/unit;
			y = Double.parseDouble(y1)/unit;
			z = Double.parseDouble(z1)/unit;
			out.addNode(x, y, z);
		}
	}

	private void convertFaces(BufferedReader rd, String type, int number,
		AmibeWriter.Dim3 out) throws IOException {
		LOGGER.fine("Reading triangles");
		String line = "";
		int nbTrias = 0;
		int nbBeams = 0;

		//Index  Id   Pt1   Pt2    Pt3   Status Meth RadTyp Domains Object
		line = rd.readLine();
		for (int i = 0; i < number; i++) {
			line = rd.readLine();
			StringTokenizer st = new StringTokenizer(line);
			st.nextToken(); // face index
			if ("TRIANGLE".equals(type)) {
				st = new StringTokenizer(line);
				st.nextToken();
				int p1, p2, p3;
				p1 = Integer.parseInt(st.nextToken());
				p2 = Integer.parseInt(st.nextToken());
				p3 = Integer.parseInt(st.nextToken());
				out.addTriangle(p1 - 1, p2 - 1, p3 - 1);
				idMapping.add(nbTrias, IDMapping.TRIAS);
				nbTrias++;
			} else if ("WIRE".equals(type)) {
				st = new StringTokenizer(line);
				st.nextToken();
				int p1, p2, p3;
				p1 = Integer.parseInt(st.nextToken());
				p2 = Integer.parseInt(st.nextToken());
				out.addBeam(p1 - 1, p2 - 1);
				idMapping.add(nbBeams, IDMapping.BEAMS);
				nbBeams++;

			}
		}
	}

	public static void main(String[] args)
	{
		try
		{
			Capitole2Amibe u=new Capitole2Amibe();
			String capiFile = "/tmp/pouet.geom";
			String amibeDir = "/tmp/pouet";
			if (args.length > 0)
				capiFile = args[0];
			if (args.length > 1)
				amibeDir = args[1];

			u.importMesh(capiFile, amibeDir);
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}
}
