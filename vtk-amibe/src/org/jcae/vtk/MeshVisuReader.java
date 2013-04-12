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
 * (C) Copyright 2008, by EADS France
 */
package org.jcae.vtk;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.iterator.TIntObjectIterator;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.amibe.traits.TriangleTraitsBuilder;
import org.jcae.mesh.oemm.MeshReader;
import org.jcae.mesh.oemm.OEMM;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.oemm.FakeNonReadVertex;

/**
 * This class serves to two things :
 * <p>In MeshVisuBuilder it reads a Mesh from the standard OEMM and build a Mesh Visu.
 * This can be down using buildPreparationMeshVisu.
 * </p>
 * <p>
 * In ViewableOEMM it reads directly in specialised *V files that contains directly the beams
 * representing the mesh. This can be down using buildMeshVisu. Be careful, the edges in the boundaries
 * of the octants are not loaded if the two octants that contains the boundarie are not loaded !
 * </p>
 * @author Julian Ibarz
 */
public class MeshVisuReader extends MeshReader
{
	private static Logger LOGGER = Logger.getLogger(MeshVisuReader.class.getName());

	/**
	 *  This is the structure of the mesh of a leaf
	 */
	class MeshVisu
	{

		public int[] edges;
		public int[] freeEdges;
		public float[] nodes; // The nodes of the other leaves duplicated
	}
	// This contains the coordinates for the quads of the octree
	private float[] nodesQuads;
	private TIntObjectHashMap<MeshVisu> mapLeafToMeshVisu = new TIntObjectHashMap<MeshVisu>();

	public MeshVisuReader(OEMM o)
	{
		super(o);
	}

	public float[] getNodesQuad()
	{
		if (nodesQuads == null)
			nodesQuads = ArrayUtils.doubleToFloat(oemm.getCoords(true));

		return nodesQuads;
	}

	public int[] getLeavesLoaded()
	{
		return mapLeafToMeshVisu.keys();
	}

	MeshVisu[] getMeshes()
	{
		return mapLeafToMeshVisu.values();
	}

	public void buildMeshVisu(int[] leaves)
	{
		if (LOGGER.isLoggable(Level.CONFIG))
			LOGGER.log(Level.CONFIG, "Loading nodes: "+leaves);

		TIntArrayList sortedLeaves = new TIntArrayList(leaves);
		sortedLeaves.sort();

		// Unload the mesh not used
		for (int leaf : mapLeafToMeshVisu.keys())
		{
			if (sortedLeaves.binarySearch(leaf) < 0)
				mapLeafToMeshVisu.remove(leaf);
		}

		for (int i = 0, n = sortedLeaves.size(); i < n; i++)
		{
			int leaf = sortedLeaves.get(i);
			// If already loaded skip
			if (mapLeafToMeshVisu.containsKey(leaf))
				continue;

			MeshVisu mesh = new MeshVisu();
			readVerticesForVisu(mesh, oemm.leaves[leaf]);
			readEdges(mesh, oemm.leaves[leaf]);
			mapLeafToMeshVisu.put(leaf, mesh);
		}
	}

	private void readVerticesForVisu(MeshVisu mesh, OEMM.Node current)
	{
		try
		{
			if (LOGGER.isLoggable(Level.FINE))
				LOGGER.log(Level.FINE, "Reading " + current.vn + " vertices from " + getVerticesFile(oemm, current));
			
			double[] xyz = new double[3];
			mesh.nodes = new float[current.vn * 3];
			FileChannel fc = new FileInputStream(getVerticesFile(oemm, current)).getChannel();
			buffer.clear();
			DoubleBuffer bbD = buffer.asDoubleBuffer();
			int remaining = current.vn;
			int offset = 0;
			for (int nblock = (remaining * VERTEX_SIZE) / BUFFER_SIZE; nblock >= 0; --nblock)
			{
				buffer.rewind();
				fc.read(buffer);
				bbD.rewind();
				int nf = BUFFER_SIZE / VERTEX_SIZE;
				if (remaining < nf)
					nf = remaining;
				remaining -= nf;
				for (int nr = 0; nr < nf; nr++)
				{
					bbD.get(xyz);
					mesh.nodes[offset++] = (float) xyz[0];
					mesh.nodes[offset++] = (float) xyz[1];
					mesh.nodes[offset++] = (float) xyz[2];
				}
			}
			fc.close();
		}
		catch (IOException ex)
		{
			LOGGER.severe("I/O error when reading file " + getVerticesFile(oemm, current));
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Reads triangle file, create Triangle instances and store them into mesh.
	 */
	private void readEdges(MeshVisu mesh, OEMM.Node current)
	{
		try
		{
			FileChannel fc = new FileInputStream(MeshVisuBuilder.getEdgesFile(oemm, current)).getChannel();

			String [] label = new String[] { "Reading edges", "Reading free edges" };
			for (int i = 0; i < 2; ++i)
			{
				LOGGER.fine(label[i]);
				// Read the number of edges components
				ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.SIZE / 8);
				IntBuffer bufferInteger = byteBuffer.asIntBuffer();
				byteBuffer.rewind();
				fc.read(byteBuffer);
				bufferInteger.rewind();
				int nbrOfEdgesComponents = bufferInteger.get(0);

				if (LOGGER.isLoggable(Level.FINE))
					LOGGER.log(Level.FINE, "Reading " + (nbrOfEdgesComponents / 2) + " edges from " + MeshVisuBuilder.getEdgesFile(oemm, current));

				// Read the edges
				byteBuffer = ByteBuffer.allocate((Integer.SIZE / 8) * nbrOfEdgesComponents);
				bufferInteger = byteBuffer.asIntBuffer();
				byteBuffer.rewind();
				fc.read(byteBuffer);
				bufferInteger.rewind();
				int[] temp = new int[nbrOfEdgesComponents];
				bufferInteger.get(temp);

				if (i == 0)
					mesh.edges = temp;
				else
					mesh.freeEdges = temp;
			}

			// Read the number of fake vertices
			ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.SIZE / 8);
			IntBuffer bufferInteger = byteBuffer.asIntBuffer();
			byteBuffer.rewind();
			fc.read(byteBuffer);
			bufferInteger.rewind();
			int nbrOfFakeVerticesComponent = bufferInteger.get(0);

			// Read fake vertices				
			if (LOGGER.isLoggable(Level.FINE))
				LOGGER.log(Level.FINE, "Reading " + (nbrOfFakeVerticesComponent / 3) + " fake vertices from " + MeshVisuBuilder.getEdgesFile(oemm, current));
			byteBuffer = ByteBuffer.allocate((Float.SIZE / 8) * nbrOfFakeVerticesComponent);
			FloatBuffer bufferFloat = byteBuffer.asFloatBuffer();
			byteBuffer.rewind();
			fc.read(byteBuffer);
			bufferFloat.rewind();
			float[] fakeVertices = new float[nbrOfFakeVerticesComponent];
			bufferFloat.get(fakeVertices);
			
			// Merging vertices and fake vertices
			float[] vertices = mesh.nodes;
			if (LOGGER.isLoggable(Level.FINE))
				LOGGER.log(Level.FINE, "Merging " + fakeVertices.length + " into " + vertices.length + " vertices.");
			mesh.nodes = new float[vertices.length + fakeVertices.length];
			System.arraycopy(vertices, 0, mesh.nodes, 0, vertices.length);
			System.arraycopy(fakeVertices, 0, mesh.nodes, vertices.length, fakeVertices.length);
			
		}
		catch (IOException ex)
		{
			LOGGER.severe("I/O error when reading indexed file " + getTrianglesFile(oemm, current));
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}

	MeshVisu buildMeshVisu(int leaf)
	{
		MeshTraitsBuilder mtb = new MeshTraitsBuilder();
		mtb.addNodeList();
		mtb.addTriangleList();
		// Mesh need adjancy informations to compute the edges
		TriangleTraitsBuilder ttb = mtb.getTriangleTraitsBuilder();
		ttb.addHalfEdge();

		mapNodeToNonReadVertexList = new TIntObjectHashMap<List<FakeNonReadVertex>>();
		Mesh mesh = buildMesh(mtb, new TIntHashSet(new int[]{ leaf  }));

		return constructEdges(mesh, leaf);
	}

	private int getNbrOfFakeVertices()
	{
		int nbrOfFakeVertices = 0;
		for (TIntObjectIterator<List<FakeNonReadVertex>> it = mapNodeToNonReadVertexList.iterator(); it.hasNext(); )
		{
			it.advance();
			nbrOfFakeVertices += it.value().size();
		}

		return nbrOfFakeVertices;
	}

	private MeshVisu constructEdges(Mesh mesh, int leave)
	{
		MeshVisu toReturn = new MeshVisu();

		TIntArrayList edges = new TIntArrayList(mesh.getTriangles().size() * 3);
		// This is empiric allocation, in general freeEdges dont are very numerous
		TIntArrayList freeEdges = new TIntArrayList(mesh.getTriangles().size());
		TFloatArrayList nodes = new TFloatArrayList();
		// offset is the number of non fake vertices
		// (because we will append later real and fake vertices)
		int offset = mesh.getNodes().size() - getNbrOfFakeVertices();
		LOGGER.finer("offset of fake vertices " + offset);
		
		// Compute offset
		for (Triangle tri : mesh.getTriangles())
		{
			if (!tri.isReadable())
				continue;

			AbstractHalfEdge edge = tri.getAbstractHalfEdge();

			for (int i = 0; i < 3; ++i)
			{
				edge = edge.next();

				// Conditions
				if (edge.hasAttributes(AbstractHalfEdge.MARKED))
					continue;
				if (!edge.origin().isReadable() || !edge.destination().isReadable())
					continue;

				// Mark the edge
				edge.setAttributes(AbstractHalfEdge.MARKED);
				if (edge.hasSymmetricEdge())
					edge.sym().setAttributes(AbstractHalfEdge.MARKED);

				TIntArrayList fakeEdges = null;

				// Save the edge
				if (edge.hasAttributes(AbstractHalfEdge.BOUNDARY))
					fakeEdges = freeEdges;
				else
					fakeEdges = edges;

				Vertex vertex = edge.origin();
				int ID = -1;

				for (int j = 0; j < 2; ++j)
				{
					if (j == 0)
						vertex = edge.origin();
					else
						vertex = edge.destination();

					if (vertex instanceof FakeNonReadVertex)
					{
						ID = offset;
						nodes.add((float) vertex.getX());
						nodes.add((float) vertex.getY());
						nodes.add((float) vertex.getZ());
						++offset;
					} else
						ID = vertex.getLabel() - oemm.leaves[leave].minIndex;
					fakeEdges.add(ID);
				}
			}
		}

		toReturn.edges = edges.toArray();
		toReturn.freeEdges = freeEdges.toArray();
		toReturn.nodes = nodes.toArray();

		return toReturn;
	}
}
