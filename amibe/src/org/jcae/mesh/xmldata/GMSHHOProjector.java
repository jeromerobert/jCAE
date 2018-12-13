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

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Location;
import org.jcae.mesh.amibe.projection.MeshLiaison;

/**
 * Project the high order vertices of a GMSH binary file to an Amibe
 * MeshLiaison.
 * The input GMSH file is modified in place.
 * @author Jerome Robert
 */
public class GMSHHOProjector {
	private final static Logger LOGGER = Logger.getLogger(GMSHHOProjector.class.getName());
	private final static int DATA_SIZE = 8;
	private final MeshLiaison liaison;
	private int verticesOffset;
	private final Vertex tmpVertex, tmpVertex2;
	private int numberOfElements;
	private int numberOfVertices;
	private int vertexRecordSize;
	private double projectionRatioThreshold = 0.2 * 0.2;
	private ByteBuffer vertexBuffer;

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
		tmpVertex2 = liaison.getMesh().createVertex(0, 0, 0);
	}

	private boolean toProject(int type) {
		// For now project only order 2 triangle and quadrangle
		return type == 9 || type == 10;
	}

	/**
	 * Select quadratic vertices
	 * @param channel The file to read
	 * @param edges The edges (extremities ID) where each quadratic vertex is
	 * @return Map quadratic vertices ID to edges
	 * @throws IOException
	 */
	private TIntIntHashMap selectVertices(FileChannel channel, TIntArrayList edges) throws IOException {
		// Jump after $Elements
		channel.position(
			verticesOffset + numberOfVertices * (4 + 3 * DATA_SIZE) + 21);
		String word = readWord(channel);
		numberOfElements = Integer.parseInt(word);
		int processedElements = 0;
		ByteBuffer rawElementHeaderBuffer = ByteBuffer.allocate(3 * 4);
		rawElementHeaderBuffer.order(ByteOrder.nativeOrder());
		IntBuffer elementHeaderBuffer = rawElementHeaderBuffer.asIntBuffer();
		edges.ensureCapacity(5000);
		TIntIntHashMap verticesToProject = new TIntIntHashMap(5000);
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
			processedElements += numElmFollow;
			// linear vertex number in the element
			int nodeNumber = NODE_NUMBER[elmType - 1] / 2;
			// memory size of one element
			int recordSize = 4 + numTag * 4 + NODE_NUMBER[elmType - 1] * 4;
			if (toProject(elmType)) {
				// offset of high order vertices in the element buffer
				int hoOffset = recordSize - nodeNumber * 4;
				int vertOffset = 4 + numTag * 4;
				ByteBuffer elementBuffer = ByteBuffer.allocate(recordSize);
				elementBuffer.order(ByteOrder.nativeOrder());
				// loop on elements in the block
				for (int i = 0; i < numElmFollow; i++) {
					channel.read(elementBuffer);
					for (int j = 0; j < nodeNumber; j++) {
						int quadVert = elementBuffer.getInt(hoOffset + 4 * j)-1;
						int edgeVert1 = elementBuffer.getInt(vertOffset + 4 * j)-1;
						int edgeVert2 = elementBuffer.getInt(vertOffset + 4 * (j+1))-1;
						edges.add(edgeVert1);
						edges.add(edgeVert2);
						verticesToProject.put(quadVert, (edges.size() / 2) - 1);
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

	/**
	 * Return the edges non-quadratic vertices coordinates
	 * @param channel The file to read
	 * @param verticesID The list of vertices to read
	 * @param vertToCoord Maps the returned coordinate array ID to the verticesID array
	 * @return The coordinates of the vertices in the ordered describes by vertToCoord
	 * @throws IOException
	 */
	private double[] readEdgeVertices(FileChannel channel, TIntArrayList verticesID,
		TIntIntHashMap vertToCoord) throws IOException {
		int[] sortedEdgesVertices = new TIntHashSet(verticesID).toArray();
		// sort vertices for efficient I/O
		Arrays.sort(sortedEdgesVertices);

		double[] edgesCoord = new double[verticesID.size() * 2 * 3];
		int k = 0;
		for(int i = 0; i < sortedEdgesVertices.length; i++) {
			channel.position(verticesOffset + sortedEdgesVertices[i] * vertexRecordSize);
			((Buffer)vertexBuffer).rewind();
			channel.read(vertexBuffer);
			vertToCoord.put(sortedEdgesVertices[i], i);
			for(int dim = 0; dim < 3; dim++)
				edgesCoord[k++]=vertexBuffer.getDouble(4 + DATA_SIZE * dim);
		}
		return edgesCoord;
	}

	/** Compute the square of the length of edges */
	private double[] edgesLength(FileChannel channel, TIntArrayList edges) throws IOException {
		// Map vertex to edgesCoord array id
		TIntIntHashMap vertToCoord = new TIntIntHashMap(edges.size());
		double[] edgesCoord = readEdgeVertices(channel, edges, vertToCoord);
		double[] edgesLength = new double[edges.size() / 2];
		for(int i = 0; i < edges.size() / 2; i++) {
			int coord1 = vertToCoord.get(edges.get(2*i));
			int coord2 = vertToCoord.get(edges.get(2*i+1));
			for(int j = 0; j < 3; j++) {
				double v = edgesCoord[coord1 * 3 + j] - edgesCoord[coord2 * 3 + j];
				edgesLength[i] += v * v;
			}
		}
		return edgesLength;
	}

	public void project(FileChannel channel) throws IOException {
		channel.position(0x2f); // skip header and "$Nodes"
		String word = readWord(channel);
		numberOfVertices = Integer.parseInt(word);
		verticesOffset = 0x2f + word.length() + 1;
		vertexRecordSize = 4 + 3 * DATA_SIZE;
		vertexBuffer = ByteBuffer.allocate(vertexRecordSize);
		vertexBuffer.order(ByteOrder.nativeOrder());

		TIntArrayList edges = new TIntArrayList();
		// Vertices to project mapped to their edge
		TIntIntHashMap verticesToEdges = selectVertices(channel, edges);
		double[] edgesLength = edgesLength(channel, edges);
		assert(edgesLength.length == edges.size() / 2);

		int[] verticesToProject = verticesToEdges.keys();
		Arrays.sort(verticesToProject);
		for(int i = 0; i < verticesToProject.length; i++) {
			channel.position(verticesOffset + verticesToProject[i] * vertexRecordSize);
			((Buffer)vertexBuffer).rewind();
			channel.read(vertexBuffer);
			tmpVertex.moveTo(
				vertexBuffer.getDouble(4 + DATA_SIZE * 0),
				vertexBuffer.getDouble(4 + DATA_SIZE * 1),
				vertexBuffer.getDouble(4 + DATA_SIZE * 2));
			tmpVertex2.moveTo(tmpVertex);
			liaison.move(tmpVertex, tmpVertex, false);
			double projectionDistance = tmpVertex.sqrDistance3D(tmpVertex2);
			double edgeLength = edgesLength[verticesToEdges.get(verticesToProject[i])];
			// Risk of creating degenerated tetrahedron
			if(projectionDistance / edgeLength > projectionRatioThreshold) {
				LOGGER.warning("Do not project " + new Location(tmpVertex2));
			} else {
				vertexBuffer.putDouble(4 + DATA_SIZE * 0, tmpVertex.getX());
				vertexBuffer.putDouble(4 + DATA_SIZE * 1, tmpVertex.getY());
				vertexBuffer.putDouble(4 + DATA_SIZE * 2, tmpVertex.getZ());
				((Buffer)vertexBuffer).rewind();
				channel.position(verticesOffset + verticesToProject[i] * vertexRecordSize);
				channel.write(vertexBuffer);
			}
		}
	}

	public void setProjectionRatioThreshold(double projectionRatioThreshold) {
		this.projectionRatioThreshold = projectionRatioThreshold * projectionRatioThreshold;
	}

}
