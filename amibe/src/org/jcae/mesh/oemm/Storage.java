/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2007,2008, by EADS France

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

import java.io.EOFException;
import java.io.File;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.IntBuffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Comparator;
import java.util.List;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIterator;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectIterator;
import gnu.trove.TObjectIntHashMap;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntByteHashMap;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.oemm.OEMM.Node;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class converts between disk and memory formats.
 * {@link org.jcae.mesh.MeshOEMMIndex} is an example to show how to
 * convert a triangle soup into an out-of-core mesh data structure.
 * When an OEMM is generated on disk, octants can be loaded and unloaded
 * on demand.  An {@link OEMM} instance is first created by calling
 * {@link #readOEMMStructure}.  Then {@link MeshReader#buildMesh} can be called
 * to select which octants are loaded into memory.
 *
 * As can be seen in {@link #readOEMMStructure}, {@link OEMM} instances
 * are serialized on disk.  But this is different with partial mesh data
 * structure.  Each child octant has a local number between 0 and 7 depending
 * on its spatial localization.  A similar structure is kept on disk, each
 * child octant is put into a seperate directory named after its number.
 * Octant leaves contain vertex and triangle data.  A file with a "v" suffix
 * contains coordinates of vertices stored as 3 double values.  A file with
 * a "a" suffix contains for each vertex the list of leaves which are
 * connected to this vertex.  This allows setting <code>readable</code> and
 * <code>writable</code> attributes, as explained in original paper.
 * A file with a "t" suffix contains for each triangle 6 int values, the first
 * 3 are leaf numbers for its 3 vertices, and last 3 are local vertex number
 * in their respective leaf.
 */
public class Storage
{
	private static final Logger logger=Logger.getLogger(Storage.class.getName());
	
	/**
	 * Number of bytes per triangle.  On disk a triangle is represented by
	 * <ul>
	 *  <li>3 int : leaf numbers for each vertex</li>
	 *  <li>3 int : local vertex indices</li>
	 *  <li>1 int : group number</li>
	 * </ul>
	 */
	protected static final int TRIANGLE_SIZE = 28;

	/**
	 * Number of bytes per vertex.  On disk a vertex is represented by 3 double
	 * (coordinates).
	 */
	protected static final int VERTEX_SIZE = 24;

	/**
	 * Creates an {@link OEMM} instance from its disk representation.
	 *
	 * @param dir   directory containing disk representation
	 * @return an {@link OEMM} instance
	 */
	public static OEMM readOEMMStructure(String dir)
	{
		OEMM ret = new OEMM(dir);
		logger.info("Build an OEMM from "+ret.getFileName());
		try
		{
			ObjectInputStream os = new ObjectInputStream(new FileInputStream(new File(ret.getFileName())));
			ret = (OEMM) os.readObject();
			// Reset nr_leaves and nr_cells because they are
			// incremented by OEMM.insert()
			int nrl = ((Integer) os.readObject()).intValue();
			ret.clearNodes();
			ret.leaves = new OEMM.Node[nrl];
			for (int i = 0; i < nrl; i++)
			{
				OEMM.Node n = (OEMM.Node) os.readObject();
				ret.insert(n);
				ret.leaves[i] = n;
			}
			os.close();
			ret.setDirectory(dir);
			ret.printInfos();
		}
		catch (IOException ex)
		{
			logger.severe("I/O error when reading indexed file in "+dir);
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		catch (ClassNotFoundException ex)
		{
			logger.severe("I/O error when reading indexed file in "+dir);
			ex.printStackTrace();
			throw new RuntimeException();
		}
		return ret;
	}
	
	/**
	 * Stores an {@link OEMM} instance to its disk representation.
	 *
	 * @param oemm stored object
	 */
	private static void storeOEMMStructure(OEMM oemm)
	{
		if (logger.isLoggable(Level.INFO)) {
			logger.info("storeOEMMStructure");
		}
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(oemm.getFileName()))));
			oos.writeObject(oemm);
			oos.writeObject(Integer.valueOf(oemm.leaves.length));
			for (OEMM.Node node : oemm.leaves) {
				oos.writeObject(node);
			}
			
		} catch (IOException e) {
			logger.severe("I/O error when reading indexed file in " + oemm.getDirectory());
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			if (oos != null) {
				try {
					oos.close();
				} catch(IOException e) {
					//ignore this
				}
			}
		}
	}
	
	/**
	 * Saves mesh on the disk into octree structure. The o-nodes, that 
	 * will be saved, are deduced from mesh (from position of vertices). 
	 *
	 * @param oemm OEMM instance
	 * @param mesh  mesh to be stored onto disk
	 * @param storedLeaves  set of leaves to store
	 * TODO There is no support for moving vertices into another octree node. 
	 */
	public static void saveNodes(OEMM oemm, Mesh mesh, TIntHashSet storedLeaves)
	{
		logger.fine("saveNodes started");
		removeNonReferencedVertices(mesh);
		// For each Vertex, find its enclosing octant leaf.
		// Side-effect: storedLeaves may be modified if new leaves have to be added.
		TObjectIntHashMap<Vertex> mapVertexToLeafindex = getMapVertexToLeafindex(oemm, mesh, storedLeaves);
		storeVertices(oemm, mesh, storedLeaves, mapVertexToLeafindex);
		storeTriangles(oemm, mesh, storedLeaves, mapVertexToLeafindex);
		
		storeOEMMStructure(oemm);
		logger.fine("saveNodes ended");
	}
	
	/**
	 * Removes vertices from mesh that are not in any triangle of mesh except vertices 
	 * that are read only. 
	 * @param mesh
	 */
	private static void removeNonReferencedVertices(Mesh mesh)
	{
		List<Vertex> tempCollection = new ArrayList<Vertex>();
		tempCollection.addAll(mesh.getNodes());
		mesh.getNodes().clear();
		
		//actually we do not need create map (set is enough) but we index with label of vertex 
		// - it should be faster. Also, we could control that there is 
		// no different vertices with the same label.
		TIntObjectHashMap<Vertex> referencedVertices = getMapLabelToVertex(mesh);
		
		TIntHashSet processedVertIndex = new TIntHashSet(referencedVertices.size());
		for (Object o: referencedVertices.getValues()) 
		{
			Vertex v = (Vertex) o;
			mesh.add(v);
			processedVertIndex.add(v.getLabel());
		}
		for (Vertex v: tempCollection)
		{
			int label = v.getLabel();
			if (processedVertIndex.contains(label))
				continue;
			processedVertIndex.add(label);
			if (!v.isWritable()) {
				mesh.add(v);
			}
		}
	}
	
	/**
	 * Returns a map of vertex label into vertex.
	 *
	 * @param mesh  mesh
	 * @return a map of vertex label into vertex.
	 * @throws RuntimeException There are different vertices with the same label in the mesh.
	 */
	private static TIntObjectHashMap<Vertex> getMapLabelToVertex(Mesh mesh)
	{
		TIntObjectHashMap<Vertex> referencedVertices = new TIntObjectHashMap<Vertex>(mesh.getTriangles().size() / 2);
		for(Triangle tr: mesh.getTriangles())
		{
			for (int i = 0; i < 3; i++)
			{
				Vertex vertex = tr.vertex[i];
				if (!vertex.isReadable() || vertex instanceof FakeNonReadVertex) {
					continue;
				}
				//check that there are no different vertices with the same label
				int label = vertex.getLabel();
				Vertex oldVertex = referencedVertices.get(label);
				if (oldVertex != vertex)
				{
					if (oldVertex != null)
						throw new RuntimeException("There are different vertices with the same label!");
					referencedVertices.put(label, vertex);
				}
			}
		}
		return referencedVertices;
	}
	
	/**
	 * Locates mesh vertices.
	 *
	 * @param oemm
	 * @param mesh
	 * @param storedLeaves 
	 * @param  
	 */
	private static TObjectIntHashMap<Vertex> getMapVertexToLeafindex(OEMM oemm, Mesh mesh, TIntHashSet storedLeaves)
	{
		int positions[] = new int[3];
		TObjectIntHashMap<Vertex> ret = new TObjectIntHashMap<Vertex>(mesh.getNodes().size());
		for(Vertex v: mesh.getNodes())
		{
			oemm.double2int(v.getUV(), positions);
			Node n = null;
			try {
				n = oemm.search(positions);
			} catch (IllegalArgumentException e) {
				//ingore this - try move vertex more closer
				logger.warning("A vertex has moved and requires a new leaf to be created");
				n = createNewNode(oemm, positions);
				storedLeaves.add(n.leafIndex);
			}
			ret.put(v, n.leafIndex);
		}
		return ret;
	}
	
	/**
	 * Stores vertices of the mesh into oemm structure on the disk. 
	 * @param oemm
	 * @param mesh
	 * @param storedLeaves  set of leaves to store
	 */
	private static void storeVertices(OEMM oemm, Mesh mesh, TIntHashSet storedLeaves, TObjectIntHashMap<Vertex> mapVertexToLeafindex)
	{
		TIntObjectHashMap<ArrayList<Vertex>> mapLeafindexToVertexList = new TIntObjectHashMap<ArrayList<Vertex>>(storedLeaves.size());
		byte[] byteBuffer = new byte[256];
		TIntObjectHashMap<VertexIndexHolder> old2newIndex = new TIntObjectHashMap<VertexIndexHolder>();
		TIntIntHashMap new2oldIndex = new TIntIntHashMap();
		TIntHashSet nodes4Update = new TIntHashSet();
		TIntHashSet addedNeighbour = new TIntHashSet();
		TIntHashSet movedVertices = new TIntHashSet();
		int[] positions = new int[3];
		for (TIntIterator it = storedLeaves.iterator(); it.hasNext(); )
		{
			int leafIndex = it.next();
			mapLeafindexToVertexList.put(leafIndex, new ArrayList<Vertex>());
		}
		
		collectAllVertices(oemm, mesh, mapVertexToLeafindex, mapLeafindexToVertexList, movedVertices);

		for (TIntObjectIterator<ArrayList<Vertex>> it = mapLeafindexToVertexList.iterator(); it.hasNext();)
		{
			it.advance();
			Node node = oemm.leaves[it.key()];
			List<Vertex> vertexList = it.value();
			sortVertexList(vertexList);
			
			reindexVerticesInNode(node, vertexList, old2newIndex, new2oldIndex);
			
			List<TIntArrayList> adjacencyFileWithoutLoadedNodes = readAdjacencyFile(oemm, node, storedLeaves);
			removeLoadedAdjacentNodes(node, storedLeaves);
			TIntByteHashMap nodeIndex2adjIndex = makeNodeIndex2adjIndexMap(node);
			if (vertexList.size() > node.vn) {
				throw new RuntimeException("Cannot add/delete vertex yet");
			}
			
			// Write vertex coordinates
			DataOutputStream fc;
			try {
				fc = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(getVerticesFile(oemm, node))));
			} catch (FileNotFoundException e) {
				logger.severe("I/O error when writing file "+getVerticesFile(oemm, node));
				e.printStackTrace();
				throw new RuntimeException(e);
			}
			try {
				for (Vertex vertex: vertexList)
					writeDoubleArray(fc, vertex.getUV());
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			} finally {
				try {
					fc.close();
				} catch (IOException ex) {
					//ignore this
				}
			}
			
			// Write adjacency
			DataOutputStream afc = null;
			try {
				afc = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(getAdjacencyFile(oemm, node))));
			} catch (FileNotFoundException e) {
				logger.severe("I/O error when writing file "+getAdjacencyFile(oemm, node));
				e.printStackTrace();
				throw new RuntimeException(e);
			}
			try {
				int counter = 0;
				for (Vertex vertex: vertexList) {
					assert ((counter + node.minIndex) == vertex.getLabel());
					
					int neighbours = 0;
					addedNeighbour.clear();
					for (Iterator<Vertex> itnv = vertex.getNeighbourIteratorVertex(); itnv.hasNext(); )
					{
						Vertex neighbour = itnv.next();
						if (neighbour == mesh.outerVertex)
							continue;
						int nodeNumber;
						if (mapVertexToLeafindex.containsKey(neighbour))
							nodeNumber = mapVertexToLeafindex.get(neighbour);
						else
							nodeNumber = searchNode(oemm, neighbour, positions);
						if (nodeNumber != node.leafIndex && !addedNeighbour.contains(nodeNumber))
						{
							if (!nodeIndex2adjIndex.containsKey(nodeNumber))
							{
								//throw new UnsupportedOperationException ("Add adjacent nodes not implemented yet");
								byte index = (byte) (node.adjLeaves.size() & 0xff);
								node.adjLeaves.add(nodeNumber);
								nodeIndex2adjIndex.put(nodeNumber, index);
							}
							byteBuffer[neighbours] = nodeIndex2adjIndex.get(nodeNumber);
							neighbours++;
							addedNeighbour.add(nodeNumber);
						}
					}
					//add adjacent non-loaded nodes
					int label = vertex.getLabel();
					int lastIndex = getLaterIndex(new2oldIndex, label);
					int localIndex = lastIndex - node.minIndex;
					if (!movedVertices.contains(lastIndex) && localIndex < adjacencyFileWithoutLoadedNodes.size() ) {
						TIntArrayList list = adjacencyFileWithoutLoadedNodes.get(localIndex);
						for (int i = 0, n = list.size(); i < n; i++)
						{
							int adjacent = list.get(i);
							if (!addedNeighbour.contains(adjacent)) {
								addedNeighbour.add(adjacent);
								byteBuffer[neighbours] = nodeIndex2adjIndex.get(adjacent);
								neighbours++;
							}
						}
						if (new2oldIndex.containsKey(label)) {
							addRequiredNodes4Update(node, storedLeaves, nodes4Update, byteBuffer, neighbours);
						}
					}
					afc.writeByte(neighbours);
					afc.write(byteBuffer, 0, neighbours);
					counter++;
				}
				node.vn = vertexList.size();
			} catch (IOException e) {
				
				e.printStackTrace();
				throw new RuntimeException(e);
			} finally {
				try {
					afc.close();
				} catch (IOException ex) {
					//ignore this
				}
			}
		}
		
		updateNonReadNodes(oemm, nodes4Update, old2newIndex);
	}

	/**
	 * It browses all nodes of the mesh and it fills map of node index to list 
	 * of contained vertices.
	 * @param oemm
	 * @param mesh
	 * @param mapVertexToLeafindex
	 * @param mapLeafindexToVertexList
	 * @param movedVertices 
	 */
	private static void collectAllVertices(OEMM oemm, Mesh mesh, TObjectIntHashMap<Vertex> mapVertexToLeafindex, TIntObjectHashMap<ArrayList<Vertex>> mapLeafindexToVertexList, TIntHashSet movedVertices)
	{
		for(Vertex vertex: mesh.getNodes())
		{
			assert mapVertexToLeafindex.containsKey(vertex);
			int index = mapVertexToLeafindex.get(vertex);
			assert mapLeafindexToVertexList.contains(index);
			List<Vertex> vertices = mapLeafindexToVertexList.get(index);
			if (vertices == null) {
				throw new UnsupportedOperationException("Cannot put vertex into octree node: "+index+". Node is not loaded!");
			}
			Node n = oemm.leaves[index];
			int label = vertex.getLabel();
			if (label >= n.minIndex && label <= n.maxIndex && (label - n.minIndex + 1) > n.vn ) {
				System.out.println("Vertex: " + label + " added!!!");
			}
			if (label < n.minIndex || label > n.maxIndex) {
				//throw new RuntimeException("Cannot move vertex between leafs");
				//experimental implementation
				if (n.getMaxIndex() - n.minIndex < n.vn) {
					throw new UnsupportedOperationException("Cannot put vertex into octree node: " 
							+n.leafIndex + ". It contains " + n.vn + " of vertices.");
				}
				if (!vertex.isWritable()) {
					throw new UnsupportedOperationException("Cannot move non-writable vertices!!");
				}
				int newLabel = n.minIndex + n.vn;
				n.vn++;
				vertex.setLabel(newLabel);
				movedVertices.add(newLabel);
			}
			vertices.add(vertex);
		}
	}
	
	/**
	 * It makes ascending sort of list of vertices in respect of their label.
	 * @param list
	 */
	private static void sortVertexList(List<Vertex> list)
	{
		Collections.sort(list, new Comparator<Vertex>() {
			public int compare(Vertex o1, Vertex o2) {
				return (o1.getLabel()<o2.getLabel() ? -1 : (o1.getLabel()==o2.getLabel() ? 0 : 1));
			}
		});
	}
	
	/**
	 * Reindexes vertices in given oemm node. List of vertices must satisfy
	 * for every n in <0,n-1> this condition: label(v_n) + 1 = label(v_n + 1).  
	 * @param node node being reindexed
	 * @param vertices list of vertices contained in the node
	 * @param old2newIndex map of old label to new label
	 * @param new2oldIndex map of new label to old label
	 */
	private static void reindexVerticesInNode(Node node, List<Vertex> vertices,
			TIntObjectHashMap<VertexIndexHolder> old2newIndex,
			TIntIntHashMap new2oldIndex)
	{
		//assert node.vn == vertices.size();
		Vertex[] values = vertices.toArray(new Vertex[vertices.size()]);
		vertices.clear();
		int upperBound = values.length - 1;
		for (int i = 0; i <= upperBound; i++) {
			//there is hole in labeling. We move vertex from the tail and reindex it
			while (values[i].getLabel() > node.minIndex + vertices.size())
			{
				Vertex moved = values[upperBound]; 
				upperBound--; 
				int oldIndex = moved.getLabel();
				//new index is next empty index
				int newIndex = vertices.size() + node.minIndex;
				old2newIndex.put(oldIndex, new VertexIndexHolder(node, newIndex));
				new2oldIndex.put(newIndex, oldIndex);
				moved.setLabel(newIndex);
				vertices.add(moved);
			}
			if (i > upperBound) {
				break;
			}
			vertices.add(values[i]);
		}
	}
	
	/**
	 * Remove adjacent nodes for given node. It removes nodes that will be stored, because
	 * adjacency will be constructed again.
	 * 
	 * @param node
	 * @param storedLeaves
	 */
	private static void removeLoadedAdjacentNodes(Node node, TIntHashSet storedLeaves)
	{
		TIntArrayList list = new TIntArrayList(node.adjLeaves.toNativeArray());
		node.adjLeaves.clear();
		for (int nodeValue: list.toNativeArray()) {
			if (!storedLeaves.contains(nodeValue)) {
				node.adjLeaves.add(nodeValue);
			}
		}
		
	}
	
	/**
	 * It add leafIndex of nodes that are not loaded but it is necessary update 
	 * index of vertices in triangles.
	 * @param node
	 * @param storedLeaves
	 * @param nodes4Update
	 * @param byteBuffer
	 * @param neighbours
	 */
	private static void addRequiredNodes4Update(OEMM.Node node, TIntHashSet storedLeaves,
			TIntHashSet nodes4Update, byte[] byteBuffer, int neighbours)
	{
		for (int i = 0; i < neighbours; i++) {
			int nodeNumber = node.adjLeaves.get(byteBuffer[i]);
			if (!storedLeaves.contains(nodeNumber)) {
				nodes4Update.add(nodeNumber);
			}
		}
		
	}
	
	/**
	 * It updates labels of reindexed vertices in the triangle file of the non-loaded nodes.
	 * 
	 * @param oemm
	 * @param nodes4Update
	 * @param old2newIndex
	 */
	private static void updateNonReadNodes(OEMM oemm, TIntHashSet nodes4Update, TIntObjectHashMap<VertexIndexHolder> old2newIndex)
	{
		int[] leaf = new int[3];
		int[] localIndices = new int[3];
		
		int maxTn = 0;
		for (TIntIterator it = nodes4Update.iterator(); it.hasNext();)
		{
			int tn = oemm.leaves[it.next()].tn;
			if (tn > maxTn)
				maxTn = tn;
		}
		ByteBuffer bb = ByteBuffer.allocate(maxTn * TRIANGLE_SIZE);
		for (TIntIterator it = nodes4Update.iterator(); it.hasNext();)
		{
			OEMM.Node node = oemm.leaves[it.next()];
			FileChannel fc = null;
			try {
				fc = new RandomAccessFile(getTrianglesFile(oemm, node),"rw").getChannel();
			} catch (FileNotFoundException e) {
				logger.log(Level.SEVERE, "Couldn't find file " + getTrianglesFile(oemm, node), e);
				throw new RuntimeException(e);
			}
			
			bb.clear();
			IntBuffer bbI = bb.asIntBuffer();
			try {
				fc.read(bb);
			} catch (IOException e) {
				logger.log(Level.SEVERE, "I/O error in operation with file " + getTrianglesFile(oemm, node), e);
				throw new RuntimeException(e);
			}
			bb.flip();

			boolean fileModified = false;
			for (int i = 0; i < node.tn; i++)
			{
				boolean modified_triangle = false;
				bbI.mark();
				bbI.get(leaf);
				bbI.get(localIndices);
				int group = bbI.get();
				
				for (int ii = 0; ii < 3; ii++)
				{
					OEMM.Node oldNode = oemm.leaves[leaf[ii]];
					int globalIndexOfNode = oldNode.minIndex + localIndices[ii];
					if (!old2newIndex.containsKey(globalIndexOfNode)) {
						assert 0 <= localIndices[ii] && localIndices[ii] <= oldNode.vn;
						continue;
					}
					modified_triangle = true;
					
					VertexIndexHolder newIndex = old2newIndex.get(globalIndexOfNode);
					leaf[ii] = newIndex.getContainedNode().leafIndex;
					localIndices[ii] = newIndex.getLocalIndex();
					assert 0 <= localIndices[ii] && localIndices[ii] <= newIndex.containedNode.vn;
				}
				if (modified_triangle)
				{
					fileModified = true;
					bbI.reset();
					bbI.put(leaf);
					bbI.put(localIndices);
					bbI.put(group);
				}
			}
			if (fileModified)
			{
				try {
					fc.position(0L);
					fc.write(bb);
				} catch (IOException e) {
					logger.log(Level.SEVERE, "I/O error in operation with file " + getTrianglesFile(oemm, node), e);
					throw new RuntimeException(e);
				}
			}
			try {
				fc.close();
			} catch (IOException e) {
				//ignore this
			}
		}
	}
	
	/**
	 * Get label of vertex before reindexing. It means old label whether was reindexed, 
	 * or present label otherwise.  
	 * @param new2oldIndex
	 * @param counter new label of vertex
	 * @return
	 */
	private static int getLaterIndex(TIntIntHashMap new2oldIndex, int counter)
	{
		if (new2oldIndex.containsKey(counter))
			return new2oldIndex.get(counter);
		return counter;
	}
	
	/**
	 * Creates map of leafIndex of adjacent node to local index. This is done 
	 * for specific node.
	 * @param node
	 * @return 
	 */
	private static TIntByteHashMap makeNodeIndex2adjIndexMap(Node node)
	{
		TIntByteHashMap nodeIndex2adjIndex = new TIntByteHashMap(node.adjLeaves.size());
		for(int i = 0; i < node.adjLeaves.size(); i++) {
			nodeIndex2adjIndex.put(node.adjLeaves.get(i), (byte)(0xff & i));
		}
		return nodeIndex2adjIndex;
	}

	/**
	 * Stores triangles of the mesh into oemm structure on the disk. 
	 *
	 * @param oemm
	 * @param mesh
	 * @param storedLeaves 
	 */
	private static void storeTriangles(OEMM oemm, Mesh mesh, TIntHashSet storedLeaves, TObjectIntHashMap<Vertex> mapVertexToLeafindex)
	{
		TIntObjectHashMap<ArrayList<Triangle>> mapLeafindexToTriangleList = new TIntObjectHashMap<ArrayList<Triangle>>();
		int[] leaf = new int[3];
		int[] pointIndex = new int[3];
		int[] positions = new int[3];
		for (TIntIterator it = storedLeaves.iterator(); it.hasNext(); )
		{
			int leafIndex = it.next();
			mapLeafindexToTriangleList.put(leafIndex, new ArrayList<Triangle>());
		}

		collectAllTriangles(oemm, mesh, mapVertexToLeafindex, mapLeafindexToTriangleList);
		for (TIntObjectIterator<ArrayList<Triangle>> it = mapLeafindexToTriangleList.iterator(); it.hasNext();)
		{
			it.advance();
			Node node = oemm.leaves[it.key()];
			List<Triangle> triangleList = it.value();
			
			DataOutputStream fc;
			try {
				fc = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(getTrianglesFile(oemm, node))));
			} catch (FileNotFoundException e1) {
				logger.severe("I/O error when reading indexed file "+getTrianglesFile(oemm, node));
				e1.printStackTrace();
				throw new RuntimeException(e1);
			}
			try {
				
				for (Triangle triangle: triangleList)
				{
					triangle.setGroupId(node.leafIndex);
					for (int i = 0; i < 3; i++)
					{
						Vertex v = triangle.vertex[i];
						Node foundNode = oemm.leaves[searchNode(oemm, v, positions)];
						leaf[i] = foundNode.leafIndex;
						assert leaf[i] < oemm.leaves.length; 
						pointIndex[i] = v.getLabel() - foundNode.minIndex;
						assert pointIndex[i] < oemm.leaves[leaf[i]].vn; 
					}
					writeIntArray(fc, leaf);
					writeIntArray(fc, pointIndex);
					fc.writeInt(triangle.getGroupId());
				}
				node.tn = triangleList.size();
			
			} catch (IOException e) {
				logger.log(Level.SEVERE, "Error in saving to " + getTrianglesFile(oemm, node), e);
				e.printStackTrace();
				throw new RuntimeException(e);
			} finally {
				try {
					fc.close();
				} catch (IOException e) {
					//ignore this
				}
			}
		}
	}

	/**
	 * It browses all triangle of the mesh and it fills map of node index to list 
	 * of contained vertices.
	 * @param oemm
	 * @param mesh
	 * @param mapLeafindexToTriangleList
	 */
	private static void collectAllTriangles(OEMM oemm, Mesh mesh, TObjectIntHashMap<Vertex> mapVertexToLeafindex, TIntObjectHashMap<ArrayList<Triangle>> mapLeafindexToTriangleList)
	{
		int positions[] = new int[3];
		for(Triangle tr: mesh.getTriangles())
		{
			boolean hasOuterEdge = tr.vertex[0].getUV().length == 2 || tr.vertex[1].getUV().length == 2 || tr.vertex[2].getUV().length == 2;
			assert hasOuterEdge == tr.hasAttributes(AbstractHalfEdge.OUTER);
			if (tr.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			// By convention, if T=(V1,V2,V3) and each Vi is contained in node Ni,
			// then T belongs to min(Ni)
			int nodeNumber = Integer.MAX_VALUE;
			for(Vertex v: tr.vertex)
			{
				int n;
				if (mapVertexToLeafindex.containsKey(v))
					n = mapVertexToLeafindex.get(v);
				else
					n = searchNode(oemm, v, positions);
				if (n < nodeNumber)
					nodeNumber = n;
			}
			List<Triangle> triangles = mapLeafindexToTriangleList.get(nodeNumber);
			if (triangles == null) {
				throw new UnsupportedOperationException("Cannot put triangle into octree node: " 
						+nodeNumber + ". Node is not loaded!");
			}
			triangles.add(tr);
		}
	}

	private static File getAdjacencyFile(OEMM oemm, Node node)
	{
		return new File(oemm.getDirectory(), node.file+"a");
	}
	
	protected static File getTrianglesFile(OEMM oemm, Node node)
	{
		return new File(oemm.getDirectory(), node.file+"t");
	}

	protected static File getVerticesFile(OEMM oemm, OEMM.Node current)
	{
		return new File(oemm.getDirectory(), current.file+"v");
	}

	/**
	 * Read adjacency file and returns List of adjacent nodes without nodes
	 * that are loaded. 
	 * @param oemm  OEMM instance
	 * @param node  OEMM node
	 * @param storedLeaves  set of node indices being loaded
	 * @return a <code>List<TIntArrayList></code> instance; for each vertex, returns indices of adjacent
	 *      and non-loaded nodes
	 */
	static List<TIntArrayList> readAdjacencyFile(OEMM oemm, Node node, TIntHashSet storedLeaves)
	{
		List<TIntArrayList> result = new ArrayList<TIntArrayList>();
		TIntArrayList nullList = new TIntArrayList();
		DataInputStream dis = null;
		try {
			dis= new DataInputStream(new BufferedInputStream(new FileInputStream(getAdjacencyFile(oemm, node))));
		} catch (FileNotFoundException e) {
			logger.log(Level.SEVERE, "Problem with opening " + getAdjacencyFile(oemm, node).getPath() + ". It may be new node.", e);
			return result;
		}
		
		try {
			while (true)
			{
				int count;
				try {
					count = dis.readByte();
				} catch (EOFException e) {
					break;
				}
				if (count == 0)
					result.add(nullList);
				else
				{
					TIntArrayList row = null;
					for (int i = 0; i < count; i++)
					{
						byte adjacentLeave = dis.readByte();
						int leafIndex = node.adjLeaves.get(adjacentLeave);
						if (storedLeaves == null || !storedLeaves.contains(leafIndex))
						{
							if (row == null)
								row = new TIntArrayList(count-i);
							row.add(leafIndex);
						}
					}
					if (row == null)
						row = nullList;
					result.add(row);
				}
			}
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Problem with reading " + getAdjacencyFile(oemm, node).getPath(), e);
			throw new RuntimeException(e);
		} finally {
			try {
				dis.close();
			} catch (IOException e) {
				///ignore this
			}
		}
		
		return result;
	}
	
	private static void getFile(OEMM oemm, Node n, StringBuilder sb)
	{
		if (n.parent == null) {
			return;
		}
		getFile(oemm, n.parent, sb);
		int octant = -1;
		for (int i = 0; i < n.parent.child.length; i++) {
			if (n == n.parent.child[i]) {
				octant = i;
			}
		}
		assert octant != -1;
		sb.append(File.separatorChar).append(octant);
		if (!n.isLeaf) {
			new File(oemm.getDirectory() + File.separator + sb).mkdir();
		}
	}

	private static Node createNewNode(OEMM oemm, int[] positions)
	{
		Node n;
		logger.info("Creating new leaf node.");
		n = oemm.build(positions);
		n.adjLeaves = new TIntArrayList();
		n.leafIndex = oemm.leaves.length;
		Node prev = oemm.leaves[oemm.leaves.length - 1];
		n.minIndex = prev.minIndex - prev.getMaxIndex() + prev.minIndex - 1;
		n.maxIndex = n.minIndex - prev.getMaxIndex() + prev.minIndex;
		StringBuilder sb = new StringBuilder();
		getFile(oemm,n, sb);
		n.file = sb.toString();
		Node [] newLeaves = new Node[oemm.leaves.length + 1];
		System.arraycopy(oemm.leaves, 0, newLeaves, 0, oemm.leaves.length + 1);
		newLeaves[n.leafIndex] = n;
		return n;
	}
	
	private static int searchNode(OEMM oemm, Vertex vert, int[] positions)
	{
		if (vert instanceof FakeNonReadVertex) {
			return ((FakeNonReadVertex) vert).getOEMMIndex();
		}
		double[] coords = vert.getUV();
		return searchNode(oemm, coords, positions);
	}

	private static int searchNode(OEMM oemm, double[] coords, int[] positions)
	{
		oemm.double2int(coords, positions);
		return oemm.search(positions).leafIndex;
	}
	
	public static void readIntArray(DataInputStream fc, int[] buffer) throws IOException
	{
		for(int i = 0 ; i < buffer.length ; ++i)
			buffer[i] = fc.readInt();
	}
	
	private static void writeIntArray(DataOutputStream fc, int[] pointIndex) throws IOException
	{
		for(int val: pointIndex)
			fc.writeInt(val);
	}
	
	private static void writeDoubleArray(DataOutputStream fc, double[] uv) throws IOException
	{
		for (double val: uv)
			fc.writeDouble(val);
	}
	
	/**
	 * Class that helps to hold global index of vertex in local index way  - 
	 * <containing_node, local_index_in_node>
	 * @author koz01
	 *
	 */
	private static class VertexIndexHolder
	{
		OEMM.Node containedNode;
		
		private int localIndex;
		
		public VertexIndexHolder(OEMM.Node node, int globalIndex) {
			assert node.minIndex <= globalIndex && globalIndex <= node.getMaxIndex();
			
			containedNode = node;
			localIndex = globalIndex - node.minIndex;
			assert localIndex >= 0 && localIndex < node.vn;
			
		}

		public final OEMM.Node getContainedNode() {
			return containedNode;
		}

		public final int getLocalIndex() {
			return localIndex;
		}
	}
	
}
