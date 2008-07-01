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

import gnu.trove.TFloatArrayList;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
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
import vtk.vtkGlobalJavaHash;

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

	private static Logger logger = Logger.getLogger(MeshVisuReader.class.getName());

	/**
	 *  This is the structure of the mesh of a leaf
	 */
	class MeshVisu
	{

		public int[] edges = null;
		public int[] freeEdges = null;
		public float[] nodes = null; // The nodes of the other leaves duplicated
	}	// This contains the coordinates for the quads of the octree
	private float[] nodesQuads = null;
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

	public MeshVisu[] getMeshes()
	{
		MeshVisu[] values = new MeshVisu[mapLeafToMeshVisu.size()];
		mapLeafToMeshVisu.getValues(values);

		return values;
	}

	public void buildMeshVisu(int[] leaves)
	{
		logger.fine("Loading nodes");

		TIntArrayList sortedLeaves = new TIntArrayList(leaves);
		sortedLeaves.sort();

		// Unload the mesh not used
		for (int leaf : mapLeafToMeshVisu.keys())
			if (sortedLeaves.binarySearch(leaf) < 0)
				mapLeafToMeshVisu.remove(leaf);
		System.out.println("SIZE MAP : " + mapLeafToMeshVisu.size());
		System.out.println("SIZE GARBAGE MAP : " + vtkGlobalJavaHash.PointerToReference.size());

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

	// Build the vertices arrays and edges arrays
		/*nodes = new float[vertMap.size() * 3];
	int indexNode = 0;
	TIntIntHashMap mapIndexToNodeIndex = new TIntIntHashMap(vertMap.size());
	
	TIntObjectIterator<float[]> iter = vertMap.iterator();
	for (int j = 0; iter.hasNext(); ++j)
	{
	iter.advance();
	mapIndexToNodeIndex.put(iter.key(), j);
	
	float[] coords = iter.value();
	nodes[indexNode] = coords[0];
	++indexNode;
	nodes[indexNode] = coords[1];
	++indexNode;
	nodes[indexNode] = coords[2];
	++indexNode;
	}*/
	}

	private void readVerticesForVisu(MeshVisu mesh, OEMM.Node current)
	{
		try
		{
			logger.info("Reading " + current.vn + " vertices from " + getVerticesFile(oemm, current));
			double[] xyz = new double[3];
			mesh.nodes = new float[current.vn * 3];
			FileChannel fc = new FileInputStream(getVerticesFile(oemm, current)).getChannel();
			buffer.clear();
			DoubleBuffer bbD = buffer.asDoubleBuffer();
			int remaining = current.vn;
			int offSet = 0;
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
					mesh.nodes[offSet++] = (float) xyz[0];
					mesh.nodes[offSet++] = (float) xyz[1];
					mesh.nodes[offSet++] = (float) xyz[2];
				}
			}
			fc.close();
		} catch (IOException ex)
		{
			logger.severe("I/O error when reading file " + getVerticesFile(oemm, current));
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
			System.out.println("MIN INDEX : " + current.minIndex);

			for (int i = 0; i < 2; ++i)
			{
				if (i == 0)
					logger.info("READING EDGES");
				else
					logger.info("READING FREE EDGES");
				// Read the number of edges components
				ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.SIZE / 8);
				IntBuffer bufferInteger = byteBuffer.asIntBuffer();
				byteBuffer.rewind();
				fc.read(byteBuffer);
				bufferInteger.rewind();
				int nbrOfEdgesComponents = bufferInteger.get(0);

				logger.info("Reading " + nbrOfEdgesComponents / 2 + " edges from " + MeshVisuBuilder.getEdgesFile(oemm, current));

				// Read the edges
				byteBuffer = ByteBuffer.allocate((Integer.SIZE / 8) * nbrOfEdgesComponents);
				bufferInteger = byteBuffer.asIntBuffer();
				byteBuffer.rewind();
				fc.read(byteBuffer);
				bufferInteger.rewind();
				int[] temp = new int[nbrOfEdgesComponents];
				bufferInteger.get(temp);

				/*TIntArrayList edgesCleaned = new TIntArrayList(leafEdges.size());
				System.out.println("edges not cleaned : " + leafEdges.size() / 2);
				// Build the edges arrays removing edges with vertices not loaded
				for (int j = 0; j < leafEdges.size(); j += 2)
				{
				int begin = leafEdges.getQuick(j);
				int end = leafEdges.getQuick(j + 1);
				
				// One vertice is not loaded -> remove the edge
				if (vertMap.get(begin) == null || vertMap.get(end) == null)
				{
				continue;
				}
				edgesCleaned.add(mapIndexToNodeIndex.get(begin));
				edgesCleaned.add(mapIndexToNodeIndex.get(end));
				}
				System.out.println("edges cleaned : " + edgesCleaned.size() / 2);*/

				if (i == 0)
					mesh.edges = temp;
				else
					mesh.freeEdges = temp;
			}

			// Read the number of fake vertice
			ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.SIZE / 8);
			IntBuffer bufferInteger = byteBuffer.asIntBuffer();
			byteBuffer.rewind();
			fc.read(byteBuffer);
			bufferInteger.rewind();
			int nbrOfFakeVerticeComponent = bufferInteger.get(0);

			// Read fake vertice				
			logger.info("Reading " + nbrOfFakeVerticeComponent / 3 + " fake vertice from " + MeshVisuBuilder.getEdgesFile(oemm, current));
			byteBuffer = ByteBuffer.allocate((Float.SIZE / 8) * nbrOfFakeVerticeComponent);
			FloatBuffer bufferFloat = byteBuffer.asFloatBuffer();
			byteBuffer.rewind();
			fc.read(byteBuffer);
			bufferFloat.rewind();
			float[] fakeVertice = new float[nbrOfFakeVerticeComponent];
			bufferFloat.get(fakeVertice);
			
			// Merging vertice and fake vertice
			float[] vertice = mesh.nodes;
			logger.info("Merging" + fakeVertice.length + " into " + vertice.length + " vertice.");
			mesh.nodes = new float[vertice.length + fakeVertice.length];
			System.arraycopy(vertice, 0, mesh.nodes, 0, vertice.length);
			System.arraycopy(fakeVertice, 0, mesh.nodes, vertice.length, fakeVertice.length);
			
		} catch (IOException ex)
		{
			logger.severe("I/O error when reading indexed file " + getTrianglesFile(oemm, current));
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}

	public MeshVisu buildPreparationMesh(int leave)
	{
		//buildMeshes(new MeshTraitsBuilder())
		MeshTraitsBuilder mtb = new MeshTraitsBuilder();
		mtb.addNodeList();
		mtb.addTriangleList();
		// Mesh need adjancy informations to compute the edges
		TriangleTraitsBuilder ttb = mtb.getTriangleTraitsBuilder();
		ttb.addHalfEdge();

		mapNodeToNonReadVertexList = new TIntObjectHashMap<List<FakeNonReadVertex>>();
		Mesh mesh = buildMesh(mtb, new TIntHashSet(new int[]
				{
					leave
				}));

		return constructEdges(mesh, leave);
	}

	private int getNbrOfFakeVertice()
	{
		List<?>[] fakeArrayVertice = new List<?>[mapNodeToNonReadVertexList.size()];
		mapNodeToNonReadVertexList.getValues(fakeArrayVertice);
		int nbrOfFakeVertice = 0;

		for (List<?> vertice : fakeArrayVertice)
			nbrOfFakeVertice += vertice.size();

		return nbrOfFakeVertice;
	}

	private MeshVisu constructEdges(Mesh mesh, int leave)
	{
		MeshVisu toReturn = new MeshVisu();

		TIntArrayList edges = new TIntArrayList(mesh.getTriangles().size() * 3);
		// This is empiric allocation, in general freeEdges dont are very numerous
		TIntArrayList freeEdges = new TIntArrayList(mesh.getTriangles().size());
		TFloatArrayList nodes = new TFloatArrayList();
		// The offSet is the number of non fake vertice (because we will append later the vertice and the fake vertice)
		int offSet = mesh.getNodes().size() - getNbrOfFakeVertice();
		logger.info("offSet of fake vertice : " + offSet);
		
		// Compute the offSet manually
		
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
						ID = offSet;
						double[] coords = vertex.getUV();
						assert coords.length == 3;
						nodes.add((float) coords[0]);
						nodes.add((float) coords[1]);
						nodes.add((float) coords[2]);
						++offSet;
					} else
						ID = vertex.getLabel() - oemm.leaves[leave].minIndex;
					fakeEdges.add(ID);
				}
			}
		}

		toReturn.edges = edges.toNativeArray();
		toReturn.freeEdges = freeEdges.toNativeArray();
		toReturn.nodes = nodes.toNativeArray();

		return toReturn;
	}
	/*
	 * 	private static final int [] meshFreeEdges(Mesh mesh)
	{
	Collection<Triangle> triList = mesh.getTriangles();
	int nrt = 0;
	for (Triangle t: triList)
	{
	if (!t.isReadable())
	continue;
	AbstractHalfEdge e = t.getAbstractHalfEdge();
	for (int j = 0; j < 3; j++)
	{
	e = e.next();
	if (!e.origin().isWritable() && !e.destination().isWritable())
	continue;
	if (e.hasAttributes(AbstractHalfEdge.BOUNDARY))
	nrt++;
	}
	}
	int [] ret = new int[2*nrt];
	int i = 0;
	for (Triangle t: triList)
	{
	if (!t.isReadable())
	continue;
	AbstractHalfEdge e = t.getAbstractHalfEdge();
	for (int j = 0; j < 3; j++)
	{
	e = e.next();
	if (!e.origin().isWritable() && !e.destination().isWritable())
	continue;
	if (e.hasAttributes(AbstractHalfEdge.BOUNDARY))
	{
	ret[2*i] = e.origin().getLabel();
	ret[2*i+1] = e.destination().getLabel();
	i++;
	}
	}
	}
	return ret;
	}*/
}
