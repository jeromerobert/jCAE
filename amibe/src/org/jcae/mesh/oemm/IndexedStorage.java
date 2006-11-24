/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005, by EADS CRC

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

import java.io.File;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import gnu.trove.TIntIterator;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.apache.log4j.Logger;

public class IndexedStorage
{
	private static Logger logger = Logger.getLogger(IndexedStorage.class);	
	
	private static final int TRIANGLE_SIZE_DISPATCHED = 40;
	private static final int VERTEX_SIZE_INDEXED = 12;
	private static final int TRIANGLE_SIZE_INDEXED = 28;
	private static final int VERTEX_SIZE = 24;
	private static final int bufferSize = TRIANGLE_SIZE_DISPATCHED * VERTEX_SIZE_INDEXED * TRIANGLE_SIZE_INDEXED;
	
	private static ByteBuffer bb = ByteBuffer.allocate(bufferSize);
	private static ByteBuffer bbt = ByteBuffer.allocate(bufferSize);
	private static ByteBuffer bbpos = ByteBuffer.allocate(8);
	
	/**
	 */
	public static OEMM indexOEMM(String inFile, String outDir)
	{
		OEMM ret = RawStorage.loadIntermediate(inFile);
		try
		{
			//  Index internal vertices
			logger.debug("Index internal vertices");
			FileInputStream fis = new FileInputStream(ret.getFileName());
			IndexInternalVerticesProcedure iiv_proc = new IndexInternalVerticesProcedure(ret, fis, outDir);
			ret.walk(iiv_proc);
			fis.close();
			
			logger.debug("Store data header on disk");
			PrintStream out = new PrintStream(new FileOutputStream(outDir+File.separator+"files"));
			WriteIndexedStructureProcedure wis_proc = new WriteIndexedStructureProcedure(out, ret.nr_leaves, ret.x0);
			ret.walk(wis_proc);
			out.close();
			
			//  Index external vertices
			logger.debug("Index external vertices");
			fis = new FileInputStream(ret.getFileName());
			IndexExternalVerticesProcedure iev_proc = new IndexExternalVerticesProcedure(ret, fis, outDir);
			ret.walk(iev_proc);
			fis.close();
			
			//  Transform vertex coordinates into doubles
			logger.debug("Transform vertex coordinates into doubles");
			ConvertVertexCoordinatesProcedure cvc_proc = new ConvertVertexCoordinatesProcedure(ret);
			ret.walk(cvc_proc);
			
			//ShowIndexedNodesProcedure debug = new ShowIndexedNodesProcedure();
			//ret.walk(debug);
		}
		catch (FileNotFoundException ex)
		{
			logger.error("File "+inFile+" not found");
		}
		catch (IOException ex)
		{
			logger.error("I/O error when reading inFile  "+inFile);
		}
		return ret;
	}
	
	private static class IndexInternalVerticesProcedure extends TraversalProcedure
	{
		private OEMM oemm;
		private FileChannel fc;
		private String outDir;
		private int globalIndex = 0;
		private ArrayList path = new ArrayList();
		private int [] ijk = new int[3];
		private int room = 0;
		public IndexInternalVerticesProcedure(OEMM o, FileInputStream in, String dir)
		{
			oemm = o;
			fc = in.getChannel();
			outDir = dir;
			PAVLTreeIntArrayDup [] vertices = new PAVLTreeIntArrayDup[oemm.nr_leaves];
			room = ((1 << 31) - 3*oemm.head[0].tn) / oemm.nr_leaves;
		}
		public final int action(OEMMNode current, int octant, int visit)
		{
			if (current.parent == null && visit != LEAF)
				return SKIPWALK;
			if (visit == POSTORDER)
			{
				path.remove(path.size() - 1);
				return SKIPWALK;
			}
			else if (visit == PREORDER)
			{
				path.add(new String(""+octant));
				return SKIPWALK;
			}
			
			logger.debug("Indexing internal vertices of node "+(current.leafIndex+1)+"/"+oemm.nr_leaves);
			ijk[0] = current.i0;
			ijk[1] = current.j0;
			ijk[2] = current.k0;
			String dir = outDir;
			for (int i = 0; i < path.size(); i++)
				dir += File.separator + (String) path.get(i);
			File d = new File(dir);
			d.mkdirs();
			current.file = dir + File.separator + octant;
			PAVLTreeIntArrayDup inner = new PAVLTreeIntArrayDup();
			PAVLTreeIntArrayDup outer = new PAVLTreeIntArrayDup();
			int nrExternal = 0;
			int nrDuplicates = 0;
			int index = 0;
			int fakeIndex = 0;
			IndexedVertex [] innerVertices = new IndexedVertex[3*current.tn];
			//  Leaves have less than 256 neighbors
			TIntHashSet set = new TIntHashSet(256);
			current.adjLeaves = new TIntArrayList(20);
			try
			{
				int [] leaf = new int[3];
				int [] pointIndex = new int[3];
				fc.position(current.counter);
				bbpos.rewind();
				fc.read(bbpos);
				bbpos.flip();
				long pos = bbpos.getLong();
				assert pos == current.counter : ""+pos+" != "+current.counter;
				bb.clear();
				IntBuffer bbI = bb.asIntBuffer();
				int tCount = 0;
				int remaining = current.tn;
				for (int nblock = (remaining * TRIANGLE_SIZE_DISPATCHED) / bufferSize; nblock >= 0; --nblock)
				{
					bb.rewind();
					fc.read(bb);
					bbI.rewind();
					int nf = bufferSize / TRIANGLE_SIZE_DISPATCHED;
					if (remaining < nf)
						nf = remaining;
					remaining -= nf;
					for(int nr = 0; nr < nf; nr ++)
					{
						for (int i = 0; i < 3; i++)
						{
							bbI.get(ijk);
							if (ijk[0] < current.i0 || ijk[0] >= current.i0 + current.size ||
							    ijk[1] < current.j0 || ijk[1] >= current.j0 + current.size ||
							    ijk[2] < current.k0 || ijk[2] >= current.k0 + current.size)
							{
								// Find its bounding node to update
								// adjacency relations.
								OEMMNode node = oemm.search(ijk);
								leaf[i] = node.leafIndex;
								fakeIndex--;
								pointIndex[i] = outer.insert(ijk, fakeIndex);
								if (pointIndex[i] == fakeIndex)
									nrExternal++;
								else
									nrDuplicates++;
							}
							else
							{
								leaf[i] = current.leafIndex;
								pointIndex[i] = inner.insert(ijk, index);
								if (pointIndex[i] == index)
								{
									innerVertices[index] = new IndexedVertex(ijk);
									index++;
								}
								else
								{
									nrDuplicates++;
								}
							}
						}
						//  Group number
						bbI.get();
						for (int i = 0; i < 3; i++)
						{
							if (leaf[i] != current.leafIndex)
								continue;
							for (int j = 0; j < 3; j++)
							{
								if (i == j || leaf[j] == current.leafIndex)
									continue;
								if (!set.contains(leaf[j]))
								{
									set.add(leaf[j]);
									current.adjLeaves.add(leaf[j]);
								}
								innerVertices[pointIndex[i]].adj.add(leaf[j]);
							}
						}
						//  Triangles are stored in the node with lowest leafIndex
						if (leaf[0] >= current.leafIndex && leaf[1] >= current.leafIndex && leaf[2] >= current.leafIndex )
							tCount++;
					}
				}
				//  Adjust data information
				current.vn = index;
				current.minIndex = globalIndex;
				current.maxIndex = globalIndex + current.vn + room - 1;
				globalIndex += index + room;
				
				//  Now store data structure onto disk
				DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(current.file+"h")));
				//  Leaf index
				output.writeInt(current.leafIndex);
				//  Cell size
				output.writeInt(current.size);
				//  Coordiantes
				output.writeInt(current.i0);
				output.writeInt(current.j0);
				output.writeInt(current.k0);
				//  Number of neighboring nodes sharing data
				output.writeInt(set.size());
				TIntIntHashMap map = new TIntIntHashMap(set.size());
				int cnt = 0;
				for (TIntIterator it = set.iterator(); it.hasNext();)
				{
					int ind = it.next();
					output.writeInt(ind);
					map.put(ind, cnt);
					cnt++;
				}
				//  First global index
				output.writeInt(current.minIndex);
				//  Last available global index
				output.writeInt(current.maxIndex);
				//  Number of inner vertices
				output.writeInt(current.vn);
				//  Number of triangles
				output.writeInt(tCount);
				//  These two integers are not used during normal
				//  processing, but they are needed to recover
				//  from a crash in IndexExternalVerticesProcedure
				//  without having to run IndexInternalVerticesProcedure
				//  again
				output.writeLong(current.counter);
				output.writeInt(current.tn);
				
				output.close();
				
				FileChannel fcv = new FileOutputStream(current.file+"i").getChannel();
				DataOutputStream outAdj = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(current.file+"a")));
				//  Inner vertices of this node
				bb.clear();
				bbI.clear();
				remaining = index;
				int i = 0;
				for (int nblock = (remaining * VERTEX_SIZE_INDEXED) / bufferSize; nblock >= 0; --nblock)
				{
					int nf = bufferSize / VERTEX_SIZE_INDEXED;
					if (remaining < nf)
						nf = remaining;
					remaining -= nf;
					bbI.rewind();
					for(int nr = 0; nr < nf; nr ++)
					{
						IndexedVertex c = innerVertices[i];
						//     Coordinates
						bbI.put(c.ijk);
						//     Adjacent leaves
						outAdj.writeInt(c.adj.size());
						for (TIntIterator it = c.adj.iterator(); it.hasNext();)
						{
							int ind = it.next();
							outAdj.writeByte((byte) map.get(ind));
						}
						i++;
					}
					bb.position(4*bbI.position());
					bb.flip();
					fcv.write(bb);
				}
				//  Triangles will be written during 2nd pass
				fcv.close();
				outAdj.close();
			}
			catch (IOException ex)
			{
				logger.error("I/O error when reading intermediate file");
				ex.printStackTrace();
				throw new RuntimeException(ex);
			}
			logger.debug("number of internal vertices: "+index);
			logger.debug("number of external vertices: "+nrExternal);
			logger.debug("number of duplicated vertices: "+nrDuplicates);
			return OK;
		}
	}
	
	private static class IndexExternalVerticesProcedure extends TraversalProcedure
	{
		private OEMM oemm;
		private FileChannel fc;
		private String outDir;
		private int [] ijk = new int[3];
		private PAVLTreeIntArrayDup [] vertices;
		public IndexExternalVerticesProcedure(OEMM o, FileInputStream in, String dir)
		{
			oemm = o;
			fc = in.getChannel();
			outDir = dir;
			vertices = new PAVLTreeIntArrayDup[oemm.nr_leaves];
		}
		public final int action(OEMMNode current, int octant, int visit)
		{
			if (current.parent == null || visit != LEAF)
				return SKIPWALK;
			logger.debug("Indexing external vertices of node "+(current.leafIndex+1)+"/"+oemm.nr_leaves);
/*
			//  TODO:  Add a better memory management system
			System.gc();
			if (Runtime.getRuntime().freeMemory() < 100000000L)
			{
				logger.debug("Purging cached vertices to release memory");
				for (int i = 0; i < vertices.length; i++)
					vertices[i] = null;
			}
*/
			for (int i = 0; i < vertices.length; i++)
				vertices[i] = null;
			
			//  Load inner vertices...
			if (vertices[current.leafIndex] == null)
				vertices[current.leafIndex] = loadVerticesInAVLTreeDup(current);
			//  ... and adjacent ones.
			for (int i = 0; i < current.adjLeaves.size(); i++)
			{
				int ind = current.adjLeaves.get(i);
				if (vertices[ind] == null)
					vertices[ind] = loadVerticesInAVLTreeDup(oemm.leaves[ind]);
			}
			
			try
			{
				int [] leaf = new int[3];
				int [] pointIndex = new int[3];
				fc.position(current.counter);
				bbpos.rewind();
				fc.read(bbpos);
				bbpos.flip();
				long pos = bbpos.getLong();
				assert pos == current.counter : ""+pos+" != "+current.counter;
				FileChannel fct = new FileOutputStream(current.file+"t").getChannel();
				OEMMNode [] cell = new OEMMNode[3];
				bb.clear();
				IntBuffer bbI = bb.asIntBuffer();
				bbt.clear();
				IntBuffer bbtI = bbt.asIntBuffer();
				int remaining = current.tn;
				for (int nblock = (remaining * TRIANGLE_SIZE_DISPATCHED) / bufferSize; nblock >= 0; --nblock)
				{
					bb.rewind();
					fc.read(bb);
					bbI.rewind();
					int nf = bufferSize / TRIANGLE_SIZE_DISPATCHED;
					if (remaining < nf)
						nf = remaining;
					remaining -= nf;
					for(int nr = 0; nr < nf; nr ++)
					{
						for (int i = 0; i < 3; i++)
						{
							bbI.get(ijk);
							leaf[i] = oemm.search(ijk).leafIndex;
							pointIndex[i] = vertices[leaf[i]].get(ijk);
						}
						int groupNumber = bbI.get();
						if (leaf[0] >= current.leafIndex && leaf[1] >= current.leafIndex && leaf[2] >= current.leafIndex)
						{
							bbtI.put(leaf);
							bbtI.put(pointIndex);
							bbtI.put(groupNumber);
							if (!bbtI.hasRemaining())
							{
								bbt.clear();
								fct.write(bbt);
								bbtI.rewind();
							}
						}
					}
				}
				if (bbtI.position() > 0)
				{
					bbt.position(4*bbtI.position());
					bbt.flip();
					fct.write(bbt);
				}
				fct.close();
			}
			catch (IOException ex)
			{
				logger.error("I/O error when reading intermediate file");
				ex.printStackTrace();
				throw new RuntimeException(ex);
			}
			return OK;
		}
	}
	
	private static class ConvertVertexCoordinatesProcedure extends TraversalProcedure
	{
		private OEMM oemm;
		private FileChannel fc;
		private int [] ijk = new int[3];
		private double [] xyz = new double[3];
		public ConvertVertexCoordinatesProcedure(OEMM o)
		{
			oemm = o;
		}
		public final int action(OEMMNode current, int octant, int visit)
		{
			if (current.parent == null || visit != LEAF)
				return SKIPWALK;
			logger.debug("Converting coordinates of node "+(current.leafIndex+1)+"/"+oemm.nr_leaves);
			
			try
			{
				FileChannel fci = new FileInputStream(current.file+"i").getChannel();
				FileChannel fco = new FileOutputStream(current.file+"v").getChannel();
				bb.clear();
				IntBuffer bbI = bb.asIntBuffer();
				bbt.clear();
				DoubleBuffer bbtD = bbt.asDoubleBuffer();
				bb.limit(bb.capacity() / 2);
				int remaining = current.vn;
				for (int nblock = (remaining * 2 * VERTEX_SIZE_INDEXED) / bufferSize; nblock >= 0; --nblock)
				{
					bb.rewind();
					fci.read(bb);
					bbI.rewind();
					bbtD.rewind();
					int nf = bufferSize / VERTEX_SIZE_INDEXED / 2;
					if (remaining < nf)
						nf = remaining;
					remaining -= nf;
					for(int nr = 0; nr < nf; nr ++)
					{
						bbI.get(ijk);
						oemm.int2double(ijk, xyz);
						bbtD.put(xyz);
					}
					bbt.position(8*bbtD.position());
					bbt.flip();
					fco.write(bbt);
					
				}
				fci.close();
				fco.close();
				new File(current.file+"i").delete();
			}
			catch (IOException ex)
			{
				logger.error("I/O error when converting coordinates file");
				ex.printStackTrace();
				throw new RuntimeException(ex);
			}
			return OK;
		}
	}
	
	public static void readHeaderOEMMNode(OEMMNode current)
	{
		try
		{
			DataInputStream bufIn = new DataInputStream(new BufferedInputStream(new FileInputStream(current.file+"h")));
			// Leaf index
			current.leafIndex = bufIn.readInt();
			// Cell size
			current.size = bufIn.readInt();
			// Coordinates
			current.i0 = bufIn.readInt();
			current.j0 = bufIn.readInt();
			current.k0 = bufIn.readInt();
			//  Number of neighboring nodes sharing data
			int nr = bufIn.readInt();
			current.adjLeaves = new TIntArrayList(nr);
			for (int i = 0; i < nr; i++)
				current.adjLeaves.add(bufIn.readInt());
			//  First global index
			current.minIndex = bufIn.readInt();
			//  Last available global index
			current.maxIndex = bufIn.readInt();
			//  Number of inner vertices
			current.vn = bufIn.readInt();
			//  Number of triangles
			current.tn = bufIn.readInt();
			//  When IndexExternalVerticesProcedure is called
			//  whereas IndexInternalVerticesProcedure was not
			//  (e.g. after a crash to not reindex internal
			//  vertices), uncomment the folllowing 2 lines
			//current.counter = bufIn.readLong();
			//current.tn = bufIn.readInt();
			bufIn.close();
		}
		catch (IOException ex)
		{
			logger.error("I/O error when reading indexed file "+current.file+"h");
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	
	private static PAVLTreeIntArrayDup loadVerticesInAVLTreeDup(OEMMNode current)
	{
		PAVLTreeIntArrayDup ret = new PAVLTreeIntArrayDup();
		int [] ijk = new int[3];
		try
		{
			FileChannel fc = new FileInputStream(current.file+"i").getChannel();
			int index = 0;
			bb.clear();
			IntBuffer bbI = bb.asIntBuffer();
			int remaining = current.vn;
			for (int nblock = (remaining * VERTEX_SIZE_INDEXED) / bufferSize; nblock >= 0; --nblock)
			{
				bb.rewind();
				fc.read(bb);
				bbI.rewind();
				int nf = bufferSize / VERTEX_SIZE_INDEXED;
				if (remaining < nf)
					nf = remaining;
				remaining -= nf;
				for(int nr = 0; nr < nf; nr ++)
				{
					bbI.get(ijk);
					ret.insert(ijk, index);
					index++;
				}
			}
			fc.close();
		}
		catch (IOException ex)
		{
			logger.error("I/O error when reading indexed file "+current.file+"i");
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}

		return ret;
	}
	
	public static OEMM buildOEMMStructure(String dir)
	{
		logger.info("Building an OEMM from "+dir+File.separator+"files");
		OEMM ret = new OEMM(dir+File.separator+"files");
		try
		{
			DataInputStream structIn = new DataInputStream(new BufferedInputStream(new FileInputStream(ret.structFile)));
			BufferedReader r = new BufferedReader(new InputStreamReader(structIn));
			for (int i = 0; i < 4; i++)
			{
				long d = Long.parseLong(r.readLine());
				ret.x0[i] = Double.longBitsToDouble(d);
			}
			int nrleaves = Integer.parseInt(r.readLine());
			ret.leaves = new OEMMNode[nrleaves];
			int [] ijk = new int[3];
			for (int i = 0; i < nrleaves; i++)
			{
				OEMMNode fake = new OEMMNode(r.readLine());
				ijk[0] = fake.i0;
				ijk[1] = fake.j0;
				ijk[2] = fake.k0;
				OEMMNode n = ret.build(fake.size, ijk);
				n.tn = fake.tn;
				n.vn = fake.vn;
				n.file = fake.file;
				n.leafIndex = fake.leafIndex;
				n.minIndex = fake.minIndex;
				n.maxIndex = fake.maxIndex;
				n.adjLeaves = fake.adjLeaves;
				ret.leaves[i] = n;
			}
			structIn.close();
			ret.status = OEMM.OEMM_INITIALIZED;
		}
		catch (IOException ex)
		{
			logger.error("I/O error when reading indexed file in "+dir);
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		return ret;
	}
	
	public static double [] getMeshOEMMCoords(OEMM oemm, TIntHashSet leaves)
	{
		logger.info("Creation of a mesh from reading selected nodes from an OEMM");
		Mesh mesh = loadNodes(oemm, leaves);
		Collection triList = mesh.getTriangles();
		int nrt = 0;
		for (Iterator it = triList.iterator(); it.hasNext(); )
		{
			Triangle t = (Triangle) it.next();
			if (!t.isOuter())
				nrt++;
		}
		logger.info("Number of triangles for this selection: "+nrt);
		double [] coord = new double[9*nrt];
		int i = 0;
		for (Iterator it = triList.iterator(); it.hasNext(); )
		{
			Triangle t = (Triangle) it.next();
			if (t.isOuter())
				continue;
			for (int j = 0; j < 3; j++)
			{
				double [] xyz = t.vertex[j].getUV();
				for (int k = 0; k < 3; k++)
					coord[9*i+3*j+k] = xyz[k];
			}
			i++;
		}
		return coord;
	}
	
	public static Mesh loadNodes(OEMM oemm, TIntHashSet leaves)
	{
		logger.info("Loading nodes");
		Mesh ret = new Mesh();
		ret.setType(Mesh.MESH_OEMM);
		TIntObjectHashMap vertMap = new TIntObjectHashMap();
		ReadVerticesProcedure rv_proc = new ReadVerticesProcedure(ret, vertMap, leaves);
		oemm.walk(rv_proc);
		ReadTrianglesProcedure rt_proc = new ReadTrianglesProcedure(oemm, vertMap, leaves, ret);
		oemm.walk(rt_proc);
		
		Object [] oArray = vertMap.getValues();
		int nrv = 0;
		for (int i = 0; i < oArray.length; i++)
		{
			Vertex v = (Vertex) oArray[i];
			if (v.getLink() != null)
				nrv++;
		}
		Vertex [] vertices = new Vertex[nrv];
		nrv = 0;
		for (int i = 0; i < oArray.length; i++)
		{
			Vertex v = (Vertex) oArray[i];
			if (v.getLink() != null)
			{
				vertices[nrv] = v;
				nrv++;
			}
		}
		ret.buildAdjacency(vertices, -1.0);
		return ret;
	}
	
	public static Mesh loadNodesNeighbours(OEMM oemm, int leaf)
	{
		TIntHashSet leaves = new TIntHashSet(oemm.leaves[leaf].adjLeaves.toNativeArray());
		leaves.add(leaf);
		return loadNodes(oemm, leaves);
	}
	
	private static class ReadVerticesProcedure extends TraversalProcedure
	{
		private TIntObjectHashMap vertMap;
		private TIntHashSet leaves;
		private Mesh mesh;
		public ReadVerticesProcedure(Mesh m, TIntObjectHashMap map, TIntHashSet set)
		{
			mesh = m;
			vertMap = map;
			leaves = set;
		}
		public final int action(OEMMNode current, int octant, int visit)
		{
			if (visit != LEAF || !leaves.contains(current.leafIndex))
				return SKIPWALK;
			try
			{
				logger.debug("Reading vertices from "+current.file+"v");
				Vertex [] vert = new Vertex[current.vn];
				double [] xyz = new double[3];
				FileChannel fc = new FileInputStream(current.file+"v").getChannel();
				DataInputStream bufIn = new DataInputStream(new BufferedInputStream(new FileInputStream(current.file+"a")));
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
						vert[index] = Vertex.valueOf(xyz);
						vert[index].setLabel(current.minIndex + index);
						vert[index].setReadable(true);
						int n = bufIn.readInt();
						//  Read neighbors
						boolean writable = true;
						for (int j = 0; j < n; j++)
						{
							int num = (int) bufIn.readByte();
							if (!leaves.contains(num))
							{
								writable = false;
								for (j++; j < n; j++)
									bufIn.readByte();
								break;
							}
						}
						vert[index].setWritable(writable);
						vertMap.put(current.minIndex + index, vert[index]);
						index++;
					}
				}
				fc.close();
			}
			catch (IOException ex)
			{
				logger.error("I/O error when reading file "+current.file+"i");
				ex.printStackTrace();
				throw new RuntimeException(ex);
			}
			return OK;
		}
	}
	
	private static class ReadTrianglesProcedure extends TraversalProcedure
	{
		private OEMM oemm;
		private TIntObjectHashMap vertMap;
		private TIntHashSet leaves;
		private Mesh mesh;
		public ReadTrianglesProcedure(OEMM o, TIntObjectHashMap map, TIntHashSet set, Mesh m)
		{
			oemm = o;
			vertMap = map;
			leaves = set;
			mesh = m;
		}
		public final int action(OEMMNode current, int octant, int visit)
		{
			if (visit != LEAF || !leaves.contains(current.leafIndex))
				return SKIPWALK;
			try
			{
				logger.debug("Reading triangles from "+current.file+"t");
				FileChannel fc = new FileInputStream(current.file+"t").getChannel();
				Vertex [] vert = new Vertex[3];
				int [] leaf = new int[3];
				int [] pointIndex = new int[3];
				int remaining = current.tn;
				bb.clear();
				IntBuffer bbI = bb.asIntBuffer();
				for (int nblock = (remaining * TRIANGLE_SIZE_INDEXED) / bufferSize; nblock >= 0; --nblock)
				{
					bb.rewind();
					fc.read(bb);
					bbI.rewind();
					int nf = bufferSize / TRIANGLE_SIZE_INDEXED;
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
								vert[j] = (Vertex) vertMap.get(globalIndex);
								assert vert[j] != null;
								if (!vert[j].isWritable())
									writable = false;
							}
							else
								readable = false;
						}
						int groupNumber = bbI.get();
						if (readable)
						{
							Triangle t = new Triangle(vert[0], vert[1], vert[2]);
							vert[0].setLink(t);
							vert[1].setLink(t);
							vert[2].setLink(t);
							if (vert[0] == Vertex.outer || vert[1] == Vertex.outer || vert[2] == Vertex.outer)
								t.setOuter();
							t.setWritable(writable);
							mesh.add(t);
						}
					}
				}
				fc.close();
			}
			catch (IOException ex)
			{
				logger.error("I/O error when reading indexed file "+current.file+"t");
				ex.printStackTrace();
				throw new RuntimeException(ex);
			}
			return OK;
		}
	}
	
	private static class WriteIndexedStructureProcedure extends TraversalProcedure
	{
		private PrintStream out;
		public WriteIndexedStructureProcedure(PrintStream outStream, int l, double [] x0)
		{
			out = outStream;
			//  Integer <-> Double scaling
			for (int i = 0; i < 4; i++)
			{
				long d = Double.doubleToLongBits(x0[i]);
				out.println(""+d);
			}
			//  Number of leaves
			out.println(""+l);
		}
		public final int action(OEMMNode current, int octant, int visit)
		{
			if (visit != LEAF)
				return SKIPWALK;
			//  Number of triangles bytes in file name
			out.println(current.file);
			return OK;
		}
	}
	
	private static class ShowIndexedNodesProcedure extends TraversalProcedure
	{
		public final int action(OEMMNode current, int octant, int visit)
		{
			if (visit == POSTORDER)
				return SKIPWALK;
			//  Number of triangles bytes in file name
			System.out.println("Node number: "+current.leafIndex);
			System.out.println("  Size: "+Integer.toHexString(current.size));
			System.out.println("  Location: "+Integer.toHexString(current.i0)+" "+Integer.toHexString(current.j0)+" "+Integer.toHexString(current.k0));
			System.out.println("  Nr of triangles: "+current.tn);
			System.out.println("  Nr of vertices: "+current.vn);
			System.out.println("  Min index: "+Integer.toHexString(current.minIndex));
			System.out.println("  Max index: "+Integer.toHexString(current.maxIndex));
			return OK;
		}
	}
	
	private static class IndexedVertex
	{
		// Integer coordinates
		public int[] ijk = new int[3];
		// This set is used to build the ol array.
		public TIntHashSet adj = new TIntHashSet();
		public IndexedVertex(int [] coord)
		{
			System.arraycopy(coord, 0, ijk, 0, 3);
		}
	}
}
