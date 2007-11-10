/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2007, by EADS France

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

package org.jcae.mesh.oemm;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectIterator;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.apache.log4j.Logger;

/**
 * This class builds a mesh from disk.
 */
public class MeshReader extends Storage
{
	private static Logger logger = Logger.getLogger(MeshReader.class);	
	
	private final OEMM oemm;
	// Map between octant index and Mesh instance.
	private TIntObjectHashMap<Mesh> mapNodeToMesh = null;
	// Map between octant index and a list of vertices from adjacent triangles so that all triangles are readable
	private TIntObjectHashMap<List<FakeNonReadVertex>> mapNodeToNonReadVertexList = null;

	/**
	 * Buffer size.  Vertices and triangles are read through buffers to improve
	 * efficiency, buffer size must be a multiple of {@link #TRIANGLE_SIZE} and
	 * {@link #VERTEX_SIZE}.
	 */
	private static final int bufferSize = 24 * VERTEX_SIZE * TRIANGLE_SIZE;
	// bufferSize = 16128
	
	/**
	 * Buffer to improve I/O efficiency.
	 */
	private static ByteBuffer bb = ByteBuffer.allocate(bufferSize);
	
	/**
	 * Constructor.
	 *
	 * @param o OEMM instance
	 */
	public MeshReader(OEMM o)
	{
		oemm = o;
	}

	/**
	 * Allows reading non-readable triangles.
	 * On disk, a triangle is contained in one single leaf.  If it contains vertices from
	 * octants which are not loaded, this triangle can not be drawn and is said to be
	 * non-readable.  If argument is <code>true</code>, such vertices are loaded from disk
	 * to make these triangles as being readable.
	 *
	 * @param loadNonReadableTriangles  behavior expected.
	 */
	public void setLoadNonReadableTriangles(boolean loadNonReadableTriangles)
	{
		if (loadNonReadableTriangles)
			mapNodeToNonReadVertexList = new TIntObjectHashMap<List<FakeNonReadVertex>>();
		else
			mapNodeToNonReadVertexList = null;
	}

	/**
	 * Builds meshes for all octants.  This method maintains a map in memory of meshes
	 * from all octants; each mesh can be retrieved by {@link #getMesh}.
	 *
	 * @param mtb  mesh traits builder used to create <code>Mesh</code> instances
	 */
	public void buildMeshes(MeshTraitsBuilder mtb)
	{
		if (!mtb.hasNodes())
			mtb.addNodeList();
		if (!mtb.hasTriangles())
			mtb.addTriangleList();
		mapNodeToMesh = new TIntObjectHashMap<Mesh>(oemm.getNumberOfLeaves());
		for(OEMM.Node current: oemm.leaves)
			mapNodeToMesh.put(current.leafIndex, new Mesh(mtb));
		mapNodeToNonReadVertexList = new TIntObjectHashMap<List<FakeNonReadVertex>>();
		TIntHashSet loadedLeaves = new TIntHashSet();
		for(OEMM.Node current: oemm.leaves)
		{
			Mesh mesh = mapNodeToMesh.get(current.leafIndex);
			loadedLeaves.clear();
			loadedLeaves.add(current.leafIndex);
			TIntObjectHashMap<Vertex> vertMap = new TIntObjectHashMap<Vertex>();
			readVertices(loadedLeaves, mesh, vertMap, current);
			readTriangles(loadedLeaves, mesh, vertMap, current);
			buildMeshAdjacency(mesh, vertMap);
		}
		if (mapNodeToNonReadVertexList != null) {
			loadVerticesFromUnloadedNodes();
		}
	}
	
	/**
	 * Gets the mesh contained in an octant.
	 *
	 * @param leafIndex  octant index
	 * @return mesh contained in these octants
	 */
	public Mesh getMesh(int leafIndex)
	{
		if (mapNodeToMesh == null)
			throw new RuntimeException("Error: buildMeshes() must be called first!");
		return mapNodeToMesh.get(leafIndex);
	}

	/**
	 * Builds a mesh composed of all octants.
	 * @return mesh contained in all octants
	 */
	public Mesh buildWholeMesh()
	{
		TIntHashSet leaves = new TIntHashSet(oemm.getNumberOfLeaves());
		for(OEMM.Node current: oemm.leaves)
			leaves.add(current.leafIndex);
		return buildMesh(leaves);
	}

	/**
	 * Builds a mesh composed of specified octants.
	 * @param leaves set of selected octants
	 * @return mesh contained in these octants
	 */
	public Mesh buildMesh(TIntHashSet leaves)
	{
		return buildMesh(MeshTraitsBuilder.getDefault3D(), leaves);
	}

	/**
	 * Builds a mesh composed of specified octants.
	 * @param mtb  mesh traits builder used to create <code>Mesh</code> instances
	 * @param leaves set of selected octants
	 * @return mesh contained in these octants
	 */
	public Mesh buildMesh(MeshTraitsBuilder mtb, TIntHashSet leaves)
	{
		if (mapNodeToMesh != null)
			throw new RuntimeException("Error: buildMesh() cannot be called after buildMeshes()!");
		// Mesh needs triangle and vertex collections, otherwise it cannot be stored back on disk
		if (!mtb.hasTriangles())
			mtb.addTriangleList();
		if (!mtb.hasNodes())
			mtb.addNodeList();
		Mesh ret = new Mesh(mtb);
		appendMesh(ret, leaves);
		return ret;
	}

	private void appendMesh(Mesh mesh, TIntHashSet leaves)
	{
		logger.debug("Loading nodes");

		// Reset maps between consecutive calls to buildMesh()
		if (mapNodeToMesh != null)
			mapNodeToMesh.clear();
		if (mapNodeToNonReadVertexList != null)
			mapNodeToNonReadVertexList.clear();
		
		TIntObjectHashMap<Vertex> vertMap = new TIntObjectHashMap<Vertex>();
		
		TIntArrayList sortedLeaves = new TIntArrayList(leaves.toArray());
		sortedLeaves.sort();
		for (int i = 0, n = sortedLeaves.size(); i < n; i++) {
			readVertices(leaves, mesh, vertMap, oemm.leaves[sortedLeaves.get(i)]);
		}
		for (int i = 0, n = sortedLeaves.size(); i < n; i++) {
			readTriangles(leaves, mesh, vertMap, oemm.leaves[sortedLeaves.get(i)]);
		}
		if (mapNodeToNonReadVertexList != null) {
			loadVerticesFromUnloadedNodes();
		}
		buildMeshAdjacency(mesh, vertMap);
		int cnt = 0;
		for (Triangle af: mesh.getTriangles())
		{
			if (af.isWritable())
				cnt++;
		}
		logger.info("Nr. of triangles: "+cnt);
	}

	/**
	 * Reads vertex coordinates, create Vertex instances and store them into a map.
	 */
	private void readVertices(TIntHashSet leaves, Mesh mesh, TIntObjectHashMap<Vertex> vertMap, OEMM.Node current)
	{
		try
		{
			logger.debug("Reading "+current.vn+" vertices from "+getVerticesFile(oemm, current));
			Vertex [] vert = new Vertex[current.vn];
			double [] xyz = new double[3];
			List<TIntArrayList> listAdjacentLeaves = readAdjacencyFile(oemm, current, leaves);
			FileChannel fc = new FileInputStream(getVerticesFile(oemm, current)).getChannel();
			bb.clear();
			DoubleBuffer bbD = bb.asDoubleBuffer();
			int remaining = current.vn;
			int index = 0;
			for (int nblock = (remaining * VERTEX_SIZE) / bufferSize; nblock >= 0; --nblock)
			{
				bb.rewind();
				fc.read(bb);
				bbD.rewind();
				int nf = bufferSize / VERTEX_SIZE;
				if (remaining < nf)
					nf = remaining;
				remaining -= nf;
				for(int nr = 0; nr < nf; nr ++)
				{
					bbD.get(xyz);
					vert[index] = mesh.createVertex(xyz);
					vert[index].setLabel(current.minIndex + index);
					vert[index].setReadable(true);
					boolean writable = listAdjacentLeaves.get(index).isEmpty();
					vert[index].setWritable(writable);
					vertMap.put(current.minIndex + index, vert[index]);
					mesh.add(vert[index]);
					index++;
					
				}
			}
			fc.close();
		}
		catch (IOException ex)
		{
			logger.error("I/O error when reading file "+getVerticesFile(oemm, current));
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Reads triangle file, create Triangle instances and store them into mesh.
	 */
	private void readTriangles(TIntHashSet leaves, Mesh mesh, TIntObjectHashMap<Vertex> vertMap, OEMM.Node current)
	{
		try
		{
			logger.debug("Reading "+current.tn+" triangles from "+getTrianglesFile(oemm, current));
			FileChannel fc = new FileInputStream(getTrianglesFile(oemm, current)).getChannel();
			Vertex [] vert = new Vertex[3];
			TIntHashSet processedNode = new TIntHashSet();
			int [] leaf = new int[3];
			int [] pointIndex = new int[3];
			int remaining = current.tn;
			bb.clear();
			IntBuffer bbI = bb.asIntBuffer();
			for (int nblock = (remaining * TRIANGLE_SIZE) / bufferSize; nblock >= 0; --nblock)
			{
				bb.rewind();
				fc.read(bb);
				bbI.rewind();
				int nf = bufferSize / TRIANGLE_SIZE;
				if (remaining < nf)
					nf = remaining;
				remaining -= nf;
				for(int nr = 0; nr < nf; nr ++)
				{
					boolean readable = true;
					boolean writable = true;
					bbI.get(leaf);
					bbI.get(pointIndex);
					for (int j = 0; j < 3; j++)
					{
						int globalIndex = oemm.leaves[leaf[j]].minIndex + pointIndex[j];
						if (leaves.contains(leaf[j]))
						{
							vert[j] = vertMap.get(globalIndex);
							assert vert[j] != null;
						}
						else
						{
							writable = false;
							vert[j] = vertMap.get(globalIndex);
							if (vert[j] == null) {
								vert[j] = new FakeNonReadVertex(oemm, leaf[j], pointIndex[j]);
								vertMap.put(globalIndex, vert[j]);
								if (mapNodeToNonReadVertexList != null)
								{
									FakeNonReadVertex vertex = (FakeNonReadVertex) vert[j];
									List<FakeNonReadVertex> vertices = mapNodeToNonReadVertexList.get(leaf[j]);
									if (vertices == null) {
										vertices = new ArrayList<FakeNonReadVertex>();
										mapNodeToNonReadVertexList.put(leaf[j], vertices);
									}
									vertices.add(vertex);
								}
							}
						}
					}
					// group number
					bbI.get();
					createTriangle(current.leafIndex, vert, readable, writable, mesh);
					// When called from buildMeshes(), cross boundary triangles are put into
					// all crossed octants.
					if (mapNodeToMesh != null && mapNodeToNonReadVertexList != null)
					{
						processedNode.clear();
						for (int j = 0; j < 3; j++) {
							if (vert[j] instanceof FakeNonReadVertex) {
								FakeNonReadVertex fnrVertex = (FakeNonReadVertex) vert[j];
								int leafIndex = fnrVertex.getOEMMIndex();
								if (!processedNode.contains(leafIndex)) {
									Mesh altMesh = mapNodeToMesh.get(leafIndex);
									createTriangle(leafIndex, vert, false, false, altMesh);
									processedNode.add(leafIndex);
								}
							}
						}
					}
				}
			}
			fc.close();
		}
		catch (IOException ex)
		{
			logger.error("I/O error when reading indexed file "+getTrianglesFile(oemm, current));
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}

	private static void createTriangle(int groupId, Vertex[] vert, boolean readable, boolean writable, Mesh mesh)
	{
		Triangle t = mesh.createTriangle(vert[0], vert[1], vert[2]);
		t.setGroupId(groupId);
		vert[0].setLink(t);
		vert[1].setLink(t);
		vert[2].setLink(t);
		t.setReadable(readable);
		t.setWritable(writable);
		mesh.add(t);
	}

	/**
	 * Builds mesh adjacency.
	 */
	private static void buildMeshAdjacency(Mesh mesh, TIntObjectHashMap<Vertex> vertMap)
	{
		if (!mesh.hasAdjacency())
			return;
		// Remove dangling vertices which are not connected to any triangle.
		int nrv = 0;
		for (TIntObjectIterator<Vertex> it = vertMap.iterator(); it.hasNext(); )
		{
			it.advance();
			Vertex v = it.value();
			if (v.getLink() != null)
				nrv++;
		}
		Vertex [] vertices = new Vertex[nrv];
		nrv = 0;
		for (TIntObjectIterator<Vertex> it = vertMap.iterator(); it.hasNext(); )
		{
			it.advance();
			Vertex v = it.value();
			if (v.getLink() != null)
			{
				vertices[nrv] = v;
				nrv++;
			}
		}
		mesh.buildAdjacency(vertices, -1.0);
	}
	
	/**
	 * This method reads coordinates of non-readable vertices. These vertices are
	 * set as readable after that.
	 */
	private void loadVerticesFromUnloadedNodes()
	{
		assert mapNodeToNonReadVertexList != null;
		int lastLimit = bb.limit();
		
		try {
			bb.limit(VERTEX_SIZE);
			bb.rewind();
			DoubleBuffer dbb = bb.asDoubleBuffer();
			
			for (TIntObjectIterator<List<FakeNonReadVertex>> it = mapNodeToNonReadVertexList.iterator(); it.hasNext(); )
			{
				it.advance();
				OEMM.Node node = oemm.leaves[it.key()];
				List<FakeNonReadVertex> list = it.value();
				sortFakeNonReadVertexList(list);
				FileChannel fch = null;
				try {
					fch = new FileInputStream(getVerticesFile(oemm, node)).getChannel();
					for (FakeNonReadVertex vertex: list) {
						
						fch.position(VERTEX_SIZE * vertex.getLocalNumber());
						bb.rewind();
						fch.read(bb);
						dbb.rewind();
						dbb.get(vertex.getUV());
						vertex.setReadable(true);
					}
				} catch (FileNotFoundException e) {
					logger.error("Cannot find file: " + getTrianglesFile(oemm, node).getAbsolutePath(), e);
					throw new RuntimeException(e);
				} catch (IOException e) {
					logger.error("Cannot operate with file: " + getTrianglesFile(oemm, node).getAbsolutePath(), e);
					throw new RuntimeException(e);
				} finally {
					if (fch != null) {
						try {
							fch.close();
						} catch (IOException e) {
							//ignore this
						}
					}
				}
			}
		} finally {
			bb.limit(lastLimit);
		}
	}
	
	/**
	 * Sorts Vertex according to their label.
	 */
	private static void sortFakeNonReadVertexList(List<FakeNonReadVertex> list)
	{
		Collections.sort(list, new Comparator<FakeNonReadVertex>()
		{
			public int compare(FakeNonReadVertex o1, FakeNonReadVertex o2) {
				return (o1.getLabel()<o2.getLabel() ? -1 : (o1.getLabel()==o2.getLabel() ? 0 : 1));
			}
			
		});
	}
	
}
