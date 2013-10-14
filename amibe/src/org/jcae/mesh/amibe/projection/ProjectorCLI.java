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
 * (C) Copyright 2013, by EADS France
 */


package org.jcae.mesh.amibe.projection;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Location;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.xmldata.GPure2Amibe;
import org.jcae.mesh.xmldata.MeshReader;
import org.xml.sax.SAXException;

/**
 * A command line interface to project a set of points to a set of triangles.
 * @author Jerome Robert
 */
public class ProjectorCLI {

	/** Create an input file for debugging */
	private static void createInputFile(Mesh mesh, String pointSetFile) throws IOException
	{
		FileChannel channel = new FileOutputStream(pointSetFile).getChannel();
		ByteBuffer bb = ByteBuffer.allocate(8*3);
		bb.order(ByteOrder.nativeOrder());
		Random r = new Random();
		double error = 10;
		for(Vertex v: mesh.getNodes())
		{
			for(int i = 0 ; i < 200; i++)
			{
				bb.putDouble(v.getX()+r.nextDouble()*error);
				bb.putDouble(v.getY()+r.nextDouble()*error);
				bb.putDouble(v.getZ()+r.nextDouble()*error);
				bb.rewind();
				channel.write(bb);
				bb.rewind();
			}
		}
		channel.close();
	}

	private static void project(String gpureFile, String amibeDir,
		String pointSetFile, String outputFile) throws IOException, SAXException
	{
		if(gpureFile != null)
			new GPure2Amibe(amibeDir).read(gpureFile);
		MeshTraitsBuilder mtb = new MeshTraitsBuilder();
		mtb.addNodeList();
		Mesh mesh = new Mesh(mtb);
		MeshReader.readObject3D(mesh, amibeDir);
		TriangleKdTree kdTree = new TriangleKdTree(mesh);
		FileChannel inChannel = new FileInputStream(pointSetFile).getChannel();
		FileChannel outChannel = new FileOutputStream(outputFile).getChannel();
		ByteBuffer bb = ByteBuffer.allocate(8 * 3 * 1000);
		bb.order(ByteOrder.nativeOrder());
		long nbPoints = inChannel.size() / 3 / 8;
		Location location = new Location();
		Location projection = new Location();
		for(long i = 0; i < nbPoints;)
		{
			int read = inChannel.read(bb);
			int readPoints = read / 3 / 8;
			bb.rewind();
			for(int j = 0; j < readPoints; j++)
			{
				location.moveTo(bb.getDouble(), bb.getDouble(), bb.getDouble());
				kdTree.getClosestTriangle(location, projection, -1);
				bb.position(bb.position() - 3 * 8);
				bb.putDouble(projection.getX());
				bb.putDouble(projection.getY());
				bb.putDouble(projection.getZ());
			}
			i += readPoints;
			bb.rewind();
			bb.limit(read);
			outChannel.write(bb);
			bb.clear();
		}
		inChannel.close();
		outChannel.close();
	}

	public static void main(final String[] args) {
		String gpureFile = null;
		String amibeDir;
		String pointSetFile;
		String outputFile;
		if(args.length == 4)
		{
			gpureFile = args[0];
			amibeDir = args[1];
			pointSetFile = args[2];
			outputFile = args[3];
		}
		else if(args.length == 3)
		{
			amibeDir = args[0];
			pointSetFile = args[1];
			outputFile = args[2];
		}
		else
		{
			System.out.println(ProjectorCLI.class.getName()+
				" <gpure file> <temporary amibe dir> <input point file> <output point file>");
			System.out.println(ProjectorCLI.class.getName()+
				" <amibe dir> <input point file> <output point file>");
			return;
		}
		try {
			project(gpureFile, amibeDir, pointSetFile, outputFile);
		} catch (Exception ex) {
			Logger.getLogger(ProjectorCLI.class.getName()).log(Level.SEVERE,
				null, ex);
		}
	}
}
