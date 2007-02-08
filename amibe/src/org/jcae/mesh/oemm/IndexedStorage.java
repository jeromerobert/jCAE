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
import java.util.HashSet;
import gnu.trove.TIntIterator;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectIterator;
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
	// bufferSize = 16128
	private static final int bufferSize = TRIANGLE_SIZE_DISPATCHED * VERTEX_SIZE_INDEXED * TRIANGLE_SIZE_INDEXED;
	
	private static ByteBuffer bb = ByteBuffer.allocate(bufferSize);
	private static ByteBuffer bbt = ByteBuffer.allocate(bufferSize);
	private static ByteBuffer bbpos = ByteBuffer.allocate(8);
	
	/**
	 */
	public static OEMM indexOEMM(String inFile, String outDir)
	{
		OEMM ret = RawStorage.loadIntermediate(inFile);
		if (logger.isDebugEnabled())
			ret.printInfos();
		try
		{
			//  Index internal vertices
			logger.info("Write octree cells onto disk");
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
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		catch (IOException ex)
		{
			logger.error("I/O error when reading inFile  "+inFile);
			ex.printStackTrace();
			throw new RuntimeException(ex);
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
				path.add(""+octant);
				return SKIPWALK;
			}
			
			logger.debug("Indexing internal vertices of node "+(current.leafIndex+1)+"/"+oemm.nr_leaves);
			ijk[0] = current.i0;
			ijk[1] = current.j0;
			ijk[2] = current.k0;
			current.topDir = outDir;
			StringBuffer sbdir = new StringBuffer();
			if (path.size() > 0)
			{
				sbdir.append((String) path.get(0));
				for (int i = 1; i < path.size(); i++)
					sbdir.append(File.separator + (String) path.get(i));
				String dir = sbdir.toString();
				File d = new File(outDir, dir);
				d.mkdirs();
				current.file = dir + File.separator + octant;
			}
			else
			{
				new File(outDir).mkdirs();
				current.file = ""+octant;
			}
			PAVLTreeIntArrayDup inner = new PAVLTreeIntArrayDup();
			PAVLTreeIntArrayDup outer = new PAVLTreeIntArrayDup();
			int nrExternal = 0;
			int nrDuplicates = 0;
			int index = 0;
			int fakeIndex = 0;
			TIntHashSet [] localAdjSet = new TIntHashSet[3*current.tn];
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
				// In this first loop, vertices are read from
				// intermediate OEMM file.  Internal vertices
				// are written into fcv via bbt buffer.
				// As bbt and bb have the same size, bbtI
				// does not overflow.
				// TODO: write directly into final "v" file.
				FileChannel fcv = new FileOutputStream(new File(current.topDir, current.file+"i")).getChannel();
				bbt.clear();
				IntBuffer bbtI = bbt.asIntBuffer();
				for (int nblock = (remaining * TRIANGLE_SIZE_DISPATCHED) / bufferSize; nblock >= 0; --nblock)
				{
					bb.rewind();
					fc.read(bb);
					bbI.rewind();
					bbtI.rewind();
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
									bbtI.put(ijk);
									localAdjSet[index] = new TIntHashSet();
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
								localAdjSet[pointIndex[i]].add(leaf[j]);
							}
						}
						//  Triangles are stored in the node with lowest leafIndex
						if (leaf[0] >= current.leafIndex && leaf[1] >= current.leafIndex && leaf[2] >= current.leafIndex )
							tCount++;
					}
					bbt.position(4*bbtI.position());
					bbt.flip();
					fcv.write(bbt);
					bbt.clear();
				}
				fcv.close();

				//  Adjust data information
				current.vn = index;
				current.minIndex = globalIndex;
				current.maxIndex = globalIndex + current.vn + room - 1;
				globalIndex += index + room;
				
				current.adjLeaves = new TIntArrayList(set.size());
				TIntIntHashMap invMap = new TIntIntHashMap(set.size());
				int cnt = 0;
				for (TIntIterator it = set.iterator(); it.hasNext();)
				{
					int ind = it.next();
					current.adjLeaves.add(ind);
					invMap.put(ind, cnt);
					cnt++;
				}
				// tCount will be the nymber of triangles
				// written onto disk, but we still need the
				// old value.
				int tn = current.tn;
				current.tn = tCount;
				writeHeaderOEMMNode(current);
				current.tn = tn;
				
				FileChannel fca = new FileOutputStream(new File(current.topDir, current.file+"a")).getChannel();
				bb.clear();
				//  Inner vertices of this node
				int room = bb.capacity();
				for (int i = 0; i < index; i++)
				{
					int n = localAdjSet[i].size();
					if (room < 1 + n)
					{
						bb.flip();
						fca.write(bb);
						bb.clear();
						room = bb.capacity();
					}
					//     Adjacent leaves
					bb.put((byte) n);
					for (TIntIterator it = localAdjSet[i].iterator(); it.hasNext();)
						bb.put((byte) invMap.get(it.next()));
					room -= 1 + n;
				}
				bb.flip();
				fca.write(bb);
				//  Triangles will be written during 2nd pass
				fca.close();
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
		private int [] ijk = new int[3];
		private PAVLTreeIntArrayDup [] vertices;
		private boolean [] needed;
		private int nr_ld_leaves = 0;
		public IndexExternalVerticesProcedure(OEMM o, FileInputStream in, String dir)
		{
			oemm = o;
			fc = in.getChannel();
			vertices = new PAVLTreeIntArrayDup[oemm.nr_leaves];
			needed = new boolean[vertices.length];
		}
		public final int action(OEMMNode current, int octant, int visit)
		{
			if (visit != LEAF)
				return SKIPWALK;
			logger.debug("Indexing external vertices of node "+(current.leafIndex+1)+"/"+oemm.nr_leaves);
			// Only adjacent leaves are needed, drop others
			// to free memory.
			// TODO: Add a better mamory management system.
			for (int i = 0; i < needed.length; i++)
				needed[i] = false;
			needed[current.leafIndex] = true;
			for (int i = 0; i < current.adjLeaves.size(); i++)
				needed[current.adjLeaves.get(i)] = true;
			for (int i = 0; i < needed.length; i++)
			{
				if (!needed[i])
					vertices[i] = null;
			}
			
			//  Load needed vertices
			for (int i = 0; i < vertices.length; i++)
			{
				if (needed[i] && vertices[i] == null)
				{
					nr_ld_leaves++;
					vertices[i] = loadVerticesInAVLTreeDup(oemm.leaves[i]);
				}
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
				FileChannel fct = new FileOutputStream(new File(current.topDir, current.file+"t")).getChannel();
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
		public void finish()
		{
			logger.debug("Total number of leaves loaded: "+nr_ld_leaves);
		}
	}
	
	private static class ConvertVertexCoordinatesProcedure extends TraversalProcedure
	{
		private OEMM oemm;
		private int [] ijk = new int[3];
		private double [] xyz = new double[3];
		public ConvertVertexCoordinatesProcedure(OEMM o)
		{
			oemm = o;
		}
		public final int action(OEMMNode current, int octant, int visit)
		{
			if (visit != LEAF)
				return SKIPWALK;
			logger.debug("Converting coordinates of node "+(current.leafIndex+1)+"/"+oemm.nr_leaves);
			
			try
			{
				FileChannel fci = new FileInputStream(new File(current.topDir, current.file+"i")).getChannel();
				FileChannel fco = new FileOutputStream(new File(current.topDir, current.file+"v")).getChannel();
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
				new File(current.topDir, current.file+"i").delete();
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
	
	private static OEMMNode readHeaderOEMMNode(String dir, String file)
	{
		OEMMNode ret = new OEMMNode(dir, file);
		try
		{
			DataInputStream bufIn = new DataInputStream(new BufferedInputStream(new FileInputStream(new File(dir, file+"h"))));
			// Leaf index
			ret.leafIndex = bufIn.readInt();
			// Cell size
			ret.size = bufIn.readInt();
			// Coordinates
			ret.i0 = bufIn.readInt();
			ret.j0 = bufIn.readInt();
			ret.k0 = bufIn.readInt();
			//  Number of neighboring nodes sharing data
			int nr = bufIn.readInt();
			ret.adjLeaves = new TIntArrayList(nr);
			for (int i = 0; i < nr; i++)
				ret.adjLeaves.add(bufIn.readInt());
			//  First global index
			ret.minIndex = bufIn.readInt();
			//  Last available global index
			ret.maxIndex = bufIn.readInt();
			//  Number of inner vertices
			ret.vn = bufIn.readInt();
			//  Number of triangles
			ret.tn = bufIn.readInt();
			bufIn.close();
		}
		catch (IOException ex)
		{
			logger.error("I/O error when reading indexed file "+dir+File.separator+file+"h");
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		return ret;
	}
	
	public static void writeHeaderOEMMNode(OEMMNode current)
	{
		try
		{
			DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(current.topDir, current.file+"h"))));
			//  Leaf index
			output.writeInt(current.leafIndex);
			//  Cell size
			output.writeInt(current.size);
			//  Coordiantes
			output.writeInt(current.i0);
			output.writeInt(current.j0);
			output.writeInt(current.k0);
			//  Number of neighboring nodes sharing data
			int n = current.adjLeaves.size();
			output.writeInt(n);
			for (int i = 0; i < n; i++)
				output.writeInt(current.adjLeaves.get(i));
			//  First global index
			output.writeInt(current.minIndex);
			//  Last available global index
			output.writeInt(current.maxIndex);
			//  Number of inner vertices
			output.writeInt(current.vn);
			//  Number of triangles
			output.writeInt(current.tn);
			output.close();
		}
		catch (IOException ex)
		{
			logger.error("I/O error when writing header file "+current.topDir+File.separator+current.file+"h");
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
			FileChannel fc = new FileInputStream(new File(current.topDir, current.file+"i")).getChannel();
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
			logger.error("I/O error when reading indexed file "+current.topDir+File.separator+current.file+"i");
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
			for (int i = 0; i < nrleaves; i++)
			{
				OEMMNode n = readHeaderOEMMNode(dir, r.readLine());
				ret.insert(n);
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
	
	public static Mesh loadNodes(OEMM oemm, TIntHashSet leaves)
	{
		return loadNodes(oemm, leaves, true);
	}

	public static Mesh loadNodes(OEMM oemm, TIntHashSet leaves, boolean adjacency)
	{
		logger.info("Loading nodes");
		Mesh ret = new Mesh();
		TIntObjectHashMap vertMap = new TIntObjectHashMap();
		ReadVerticesProcedure rv_proc = new ReadVerticesProcedure(ret, vertMap, leaves);
		oemm.walk(rv_proc);
		ReadTrianglesProcedure rt_proc = new ReadTrianglesProcedure(oemm, vertMap, leaves, ret);
		oemm.walk(rt_proc);
		
		int nrv = 0;
		for (TIntObjectIterator it = vertMap.iterator(); it.hasNext(); )
		{
			it.advance();
			Vertex v = (Vertex) it.value();
			if (v.getLink() != null)
				nrv++;
		}
		Vertex [] vertices = new Vertex[nrv];
		nrv = 0;
		for (TIntObjectIterator it = vertMap.iterator(); it.hasNext(); )
		{
			it.advance();
			Vertex v = (Vertex) it.value();
			if (v.getLink() != null)
			{
				vertices[nrv] = v;
				nrv++;
			}
		}

		if (adjacency)
		{
			ret.buildAdjacency(vertices, -1.0);
			// Outer triangles have been added, mark these triangles
			for (Iterator it = ret.getTriangles().iterator(); it.hasNext(); )
			{
				Triangle t = (Triangle) it.next();
				if (t.isOuter())
				{
					t.setReadable(false);
					t.setWritable(false);
				}
			}
		}
		logger.info("Nr. of triangles: "+ret.getTriangles().size());
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
		public ReadVerticesProcedure(Mesh m, TIntObjectHashMap map, TIntHashSet set)
		{
			vertMap = map;
			leaves = set;
		}
		public final int action(OEMMNode current, int octant, int visit)
		{
			if (visit != LEAF || !leaves.contains(current.leafIndex))
				return SKIPWALK;
			try
			{
				logger.debug("Reading vertices from "+current.topDir+File.separator+current.file+"v");
				Vertex [] vert = new Vertex[current.vn];
				double [] xyz = new double[3];
				FileChannel fc = new FileInputStream(new File(current.topDir, current.file+"v")).getChannel();
				DataInputStream bufIn = new DataInputStream(new BufferedInputStream(new FileInputStream(new File(current.topDir, current.file+"a"))));
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
						int n = (int) bufIn.readByte();
						//  Read neighbors
						boolean writable = true;
						for (int j = 0; j < n; j++)
						{
							int num = (int) bufIn.readByte();
							if (!leaves.contains(current.adjLeaves.get(num)))
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
				bufIn.close();
			}
			catch (IOException ex)
			{
				logger.error("I/O error when reading file "+current.topDir+File.separator+current.file+"i");
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
				logger.debug("Reading triangles from "+current.topDir+File.separator+current.file+"t");
				FileChannel fc = new FileInputStream(new File(current.topDir, current.file+"t")).getChannel();
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
							}
							else
							{
								writable = false;
								vert[j] = null;
							}
						}
						// group number
						bbI.get();
						if (!writable)
							continue;
						Triangle t = new Triangle(vert[0], vert[1], vert[2]);
						t.setGroupId(0);
						vert[0].setLink(t);
						vert[1].setLink(t);
						vert[2].setLink(t);
						t.setReadable(readable);
						t.setWritable(writable);
						mesh.add(t);
					}
				}
				fc.close();
			}
			catch (IOException ex)
			{
				logger.error("I/O error when reading indexed file "+current.topDir+File.separator+current.file+"t");
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
	
	/*
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
	*/
	
}
