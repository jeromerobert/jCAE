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
 * (C) Copyright 2018, by Airbus S.A.S.
 */

package org.jcae.mesh.xmldata;

import gnu.trove.set.hash.TIntHashSet;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.projection.MeshLiaison;

/**
 * Project the high order vertices of a GMSH binary file to an Amibe
 * MeshLiaison.
 * The input GMSH file is modified in place.
 * @author Jerome Robert
 */
public class GMSHHOProjector {

	private final static int DATA_SIZE = 8;
	private final MeshLiaison liaison;
	private int verticesOffset;
	private final Vertex tmpVertex;
	private int numberOfElements;
	private int numberOfVertices;

	/**
	 * Read a string from the current position to the next 0x0a char
	 */
	private String readWord(FileChannel channel) throws IOException {
		ByteBuffer word = ByteBuffer.allocate(20);
		// for Java 9
		Buffer bword = word;
		long p = channel.position();
		channel.read(word);
		bword.rewind();
		while(word.get() != 0x0a) {
			//keep going
		}
		int size = word.position();
		byte[] r = new byte[size - 1];
		channel.position(p + size);
		bword.rewind();
		word.get(r);
		return new String(r);
	}

	private final static int[] NODE_NUMBER = {
		2, // line
		3, // triangle
		4, // quadrangle
		4, // tetrahedron
		8, // hexahedron
		6, // prism
		5, // pyramid
		3, // second order line
		6, // second order triangle
		9, // second order quadrangle
		10, // second order tetrahedron
	};

	/**
	 *
	 * @param liaison The MeshLiaison object on which to project
	 */
	public GMSHHOProjector(MeshLiaison liaison) {
		this.liaison = liaison;
		tmpVertex = liaison.getMesh().createVertex(0, 0, 0);
	}

	private boolean toProject(int type) {
		// For now project only order 2 triangle and quadrangle
		return type == 9 || type == 10;
	}

	private TIntHashSet selectVertices(FileChannel channel) throws IOException {
		// Jump after $Elements
		channel.position(
			verticesOffset + numberOfVertices * (4 + 3 * DATA_SIZE) + 21);
		String word = readWord(channel);
		numberOfElements = Integer.parseInt(word);
		int processedElements = 0;
		ByteBuffer rawElementHeaderBuffer = ByteBuffer.allocate(3 * 4);
		rawElementHeaderBuffer.order(ByteOrder.nativeOrder());
		IntBuffer elementHeaderBuffer = rawElementHeaderBuffer.asIntBuffer();
		TIntHashSet verticesToProject = new TIntHashSet();
		// loop on block of elements
		while (processedElements < numberOfElements) {
			assert(rawElementHeaderBuffer.position() == 0);
			channel.read(rawElementHeaderBuffer);
			((Buffer)rawElementHeaderBuffer).rewind();
			((Buffer)elementHeaderBuffer).rewind();
			assert(elementHeaderBuffer.position() == 0);
			int elmType = elementHeaderBuffer.get();
			int numElmFollow = elementHeaderBuffer.get();
			int numTag = elementHeaderBuffer.get();
			// memory size of one element
			int recordSize = 4 + numTag * 4 + NODE_NUMBER[elmType - 1] * 4;
			processedElements += numElmFollow;
			if (toProject(elmType)) {
				// offset of high order vertices in the element buffer
				int hoOffset = recordSize - NODE_NUMBER[elmType - 1] * 2;
				ByteBuffer elementBuffer = ByteBuffer.allocate(recordSize);
				elementBuffer.order(ByteOrder.nativeOrder());
				// loop on elements in the block
				for (int i = 0; i < numElmFollow; i++) {
					channel.read(elementBuffer);
					for (int j = 0; j < NODE_NUMBER[elmType - 1] / 2; j++) {
						verticesToProject.add(elementBuffer.getInt(hoOffset + 4 * j)-1);
					}
					((Buffer)elementBuffer).rewind();
				}
			} else {
				// skip this block of elements
				channel.position(channel.position() + numElmFollow * recordSize);
			}
		}
		return verticesToProject;
	}

	/**
	 * @param fileName The binary .msh file to project
	 * @throws IOException
	 */
	public void project(String fileName) throws IOException {
		FileChannel f = null;
		try {
			f = new RandomAccessFile(fileName, "rw").getChannel();
			project(f);
		} finally {
			if(f != null)
				f.close();
		}
	}

	public void project(FileChannel channel) throws IOException {
		channel.position(0x2f); // skip header and $Nodes
		String word = readWord(channel);
		numberOfVertices = Integer.parseInt(word);
		verticesOffset = 0x2f + word.length() + 1;
		int[] verticesToProject = selectVertices(channel).toArray();
		Arrays.sort(verticesToProject);
		int vertexRecordSize = 4 + 3 * DATA_SIZE;
		ByteBuffer vertexBuffer = ByteBuffer.allocate(vertexRecordSize);
		vertexBuffer.order(ByteOrder.nativeOrder());
		for(int i = 0; i < verticesToProject.length; i++) {
			channel.position(verticesOffset + verticesToProject[i] * vertexRecordSize);
			channel.read(vertexBuffer);
			// FIXME: This can create bad tetrahedron if the projection point is too
			// far. The best would be to check the tetrahedron quality after moving
			// the point. But 1- this would be expensive, 2- we don't have data
			// structure to do that. A cheap solution would be to check that
			// the projection distance is always smaller than a fraction of the
			// edge size.
			tmpVertex.moveTo(
				vertexBuffer.getDouble(4 + DATA_SIZE * 0),
				vertexBuffer.getDouble(4 + DATA_SIZE * 1),
				vertexBuffer.getDouble(4 + DATA_SIZE * 2));
			liaison.move(tmpVertex, tmpVertex, false);
			vertexBuffer.putDouble(4 + DATA_SIZE * 0, tmpVertex.getX());
			vertexBuffer.putDouble(4 + DATA_SIZE * 1, tmpVertex.getY());
			vertexBuffer.putDouble(4 + DATA_SIZE * 2, tmpVertex.getZ());
			((Buffer)vertexBuffer).rewind();
			channel.position(verticesOffset + verticesToProject[i] * vertexRecordSize);
			channel.write(vertexBuffer);
			((Buffer)vertexBuffer).rewind();
		}
	}
}
