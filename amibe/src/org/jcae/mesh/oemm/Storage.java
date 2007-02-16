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

import java.io.File;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectIterator;
import gnu.trove.TIntHashSet;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.apache.log4j.Logger;

public class Storage
{
	private static Logger logger = Logger.getLogger(Storage.class);	
	
	private static final int TRIANGLE_SIZE = 28;
	private static final int VERTEX_SIZE = 24;
	// bufferSize = 16128
	private static final int bufferSize = 24 * VERTEX_SIZE * TRIANGLE_SIZE;
	
	private static ByteBuffer bb = ByteBuffer.allocate(bufferSize);
	
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
			ret.leaves = new OEMMNode[nrl];
			for (int i = 0; i < nrl; i++)
			{
				OEMMNode n = (OEMMNode) os.readObject();
				ret.insert(n);
				ret.leaves[i] = n;
			}
			os.close();
			ret.setDirectory(dir);
			ret.printInfos();
		}
		catch (IOException ex)
		{
			logger.error("I/O error when reading indexed file in "+dir);
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		catch (ClassNotFoundException ex)
		{
			logger.error("I/O error when reading indexed file in "+dir);
			ex.printStackTrace();
			throw new RuntimeException();
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
		ReadTrianglesProcedure rt_proc = new ReadTrianglesProcedure(vertMap, leaves, ret);
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
		public final int action(OEMM oemm, OEMMNode current, int octant, int visit)
		{
			if (visit != LEAF || !leaves.contains(current.leafIndex))
				return OK;
			try
			{
				logger.debug("Reading vertices from "+oemm.getDirectory()+File.separator+current.file+"v");
				Vertex [] vert = new Vertex[current.vn];
				double [] xyz = new double[3];
				FileChannel fc = new FileInputStream(new File(oemm.getDirectory(), current.file+"v")).getChannel();
				DataInputStream bufIn = new DataInputStream(new BufferedInputStream(new FileInputStream(new File(oemm.getDirectory(), current.file+"a"))));
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
				logger.error("I/O error when reading file "+oemm.getDirectory()+File.separator+current.file+"i");
				ex.printStackTrace();
				throw new RuntimeException(ex);
			}
			return OK;
		}
	}
	
	private static class ReadTrianglesProcedure extends TraversalProcedure
	{
		private TIntObjectHashMap vertMap;
		private TIntHashSet leaves;
		private Mesh mesh;
		public ReadTrianglesProcedure(TIntObjectHashMap map, TIntHashSet set, Mesh m)
		{
			vertMap = map;
			leaves = set;
			mesh = m;
		}
		public final int action(OEMM oemm, OEMMNode current, int octant, int visit)
		{
			if (visit != LEAF || !leaves.contains(current.leafIndex))
				return OK;
			try
			{
				logger.debug("Reading triangles from "+oemm.getDirectory()+File.separator+current.file+"t");
				FileChannel fc = new FileInputStream(new File(oemm.getDirectory(), current.file+"t")).getChannel();
				Vertex [] vert = new Vertex[3];
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
				logger.error("I/O error when reading indexed file "+oemm.getDirectory()+File.separator+current.file+"t");
				ex.printStackTrace();
				throw new RuntimeException(ex);
			}
			return OK;
		}
	}
	
}
