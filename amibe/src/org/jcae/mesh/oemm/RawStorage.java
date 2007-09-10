/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005, by EADS CRC
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
import java.io.RandomAccessFile;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.ObjectOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import gnu.trove.TIntIterator;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import org.apache.log4j.Logger;

/**
 * Convert a triangle soup into an OEMM data structure.  The different steps
 * can be found in {@link org.jcae.mesh.MeshOEMMIndex}, here is a summary:
 * <ol>
 *  <li>Initialize an {@link OEMM} instance with desired depth.</li>
 *  <li>Read triangle soup and count triangle in each OEMM cell.</li>
 *  <li>Merge adjacent cells when they contain few triangles.</li>
 *  <li>Copy triangle soup into a dispatched file in which triangles
 *      are sorted by octants.</li>
 *  <li>In each octant, write an indexed OEMM data structure.</li>
 * </ol>
 */
public class RawStorage
{
	private static Logger logger = Logger.getLogger(RawStorage.class);	

	//  In triangle soup, a triangle has 9 double coordinates and two ints.
	private static final int TRIANGLE_SIZE_RAW = 80;
	//  In dispatched file, a triangle has 9 int coordinates and an int.
	private static final int TRIANGLE_SIZE_DISPATCHED = 40;
	//  In intermediate file, a vertex has 3 integer coordiantes and a
	//  triangle has 2 int[3] arrays and an int.
	private static final int VERTEX_SIZE_INDEXED = 12;
	private static final int TRIANGLE_SIZE_INDEXED = 28;
	// bufferSize = 26880
	// As TRIANGLE_SIZE_RAW is 2*TRIANGLE_SIZE_DISPATCHED, the latter
	// does not need to be taken into account
	private static final int bufferSize = (TRIANGLE_SIZE_RAW * VERTEX_SIZE_INDEXED * TRIANGLE_SIZE_INDEXED);
	private static ByteBuffer bb = ByteBuffer.allocate(bufferSize);
	private static ByteBuffer bbt = ByteBuffer.allocate(bufferSize);
	private static ByteBuffer bbpos = ByteBuffer.allocate(8);

	public static interface SoupReaderInterface
	{
		public void processVertex(int i, double [] xyz);
		public void processTriangle(int group);
	}

	/**
	 * Reads a triangle soup and executes a procedure on all triangles
	 * and vertices.
	 * 
	 * @param  file  triangle soup file name
	 * @param  proc  a {@link SoupReaderInterface} instance, its
	 *  {@link SoupReaderInterface#processVertex} method is called for each
	 *  vertex, and {@link SoupReaderInterface#processTriangle} is called
	 *  for each triangle.
	 */
	public static void readSoup(String file, SoupReaderInterface proc)
	{
		double [] xyz = new double[3];
		boolean hasNext = true;
		bb.clear();
		DoubleBuffer bbD = bb.asDoubleBuffer();
		IntBuffer bbI = bb.asIntBuffer();
		try
		{
			FileChannel fc = new FileInputStream(file).getChannel();
			while (hasNext)
			{
				bb.rewind();
				int nr = fc.read(bb);
				if (nr < bufferSize)
					hasNext = false;
				bbD.rewind();
				for(; nr > 0; nr -= TRIANGLE_SIZE_RAW)
				{
					for (int i = 0; i < 3; i++)
					{
						bbD.get(xyz);
						proc.processVertex(i, xyz);
					}
					bbD.get();
					bbI.position(2*bbD.position() - 2);
					int attribute = bbI.get();
					proc.processTriangle(attribute);
				}
			}
			fc.close();
		}
		catch (FileNotFoundException ex)
		{
			logger.error("File "+file+" not found");
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		catch (IOException ex)
		{
			logger.error("I/O error when reading "+file);
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Builds an OEMM and counts the number of triangles which have to be
	 * assigned to each leaf.
	 *
	 * A triangle soup is read from the file associated with an OEMM,
	 * deepest cells for triangle vertices are created if needed, and
	 * triangle counters are updated.  When this routine returns, all leaf
	 * nodes have been created and each node knowa how many triangles will
	 * be assigned to it in later stages.
	 * 
	 * @param  tree  an OEMM
	 * @param  soupFile  triangle soup file name
	 * @return <code>false</code> if a vertex was found outside of octree
	 *         bounds, <code>true</code> otherwise.
	 */
	public static boolean countTriangles(OEMM tree, String soupFile)
	{
		return countTriangles(tree, soupFile, true);
	}

	/**
	 * Counts the number of triangles which have to be assigned to each leaf.
	 *
	 * A triangle soup is read from the file associated with an OEMM,
	 * deepest cells for triangle vertices are created if needed, and
	 * triangle counters are updated.  When this routine returns, all leaf
	 * nodes have been created and each node know how many triangles will
	 * be assigned to it in later stages.
	 * 
	 * @param  tree  an OEMM
	 * @param  soupFile  triangle soup file name
	 * @param  build  if <code>true</code>, OEMM instance is built.  Otherwise,
	 *         it is supposed to have already been built.
	 * @return <code>false</code> if a vertex was found outside of octree
	 *         bounds, <code>true</code> otherwise.
	 */
	public static boolean countTriangles(OEMM tree, String soupFile, boolean build)
	{
		if (tree == null)
		{
			logger.error("OEMM not created!");
			return false;
		}
		logger.info("Count triangles");
		logger.debug("Reading "+soupFile+" and count triangles");
		CountTriangles ct = new CountTriangles(tree, build);
		readSoup(soupFile, ct);
		logger.info("Number of triangles: "+ct.getTriangleCount());
		double [] bbox = ct.getBoundingBox();
		if (!tree.checkBoundingBox(bbox))
		{
			if (!build)
				throw new RuntimeException("Some vertices of this soup file do not fit octree");
			tree.setBoundingBox(bbox);
			return false;
		}
		if (build)
			tree.printInfos();
		return true;
	}
	
	private static final class CountTriangles implements SoupReaderInterface
	{
		private OEMM.Node [] cells = new OEMM.Node[3];
		private OEMM oemm;
		private long nrTriangles = 0;
		private int [] ijk = new int[3];
		private double [] bbox = new double[6];
		private boolean build;
		public CountTriangles(OEMM o, boolean b)
		{
			oemm = o;
			build = b;
			if (!build)
			{
				for (int i = 0; i < oemm.getNumberOfLeaves(); i++)
					oemm.leaves[i].tn = 0;
			}
			for (int k = 0; k < 3; k++)
			{
				bbox[k] = Double.MAX_VALUE;
				bbox[k+3] = Double.MIN_VALUE;
			}
		}
		public void processVertex(int i, double [] xyz)
		{
			for (int k = 0; k < 3; k++)
			{
				if (xyz[k] < bbox[k])
					bbox[k] = xyz[k];
				if (xyz[k] > bbox[k+3])
					bbox[k+3] = xyz[k];
			}
			oemm.double2int(xyz, ijk);
			if (build)
				cells[i] = oemm.build(ijk);
			else
				cells[i] = oemm.search(ijk);
		}
		public void processTriangle(int group)
		{
			nrTriangles++;
			cells[0].tn++;
			if (cells[1] != cells[0])
				cells[1].tn++;
			if (cells[2] != cells[0] && cells[2] != cells[1])
				cells[2].tn++;
		}
		long getTriangleCount()
		{
			return nrTriangles;
		}
		double [] getBoundingBox()
		{
			return bbox;
		}
	}

	/**
	 * Reads a triangle soup and dispatches triangles into an intermediate
	 * OEMM data structure.
	 *
	 * The data structure has been setup in {@link #countTriangles}, and
	 * will now be written onto disk as a linear octree.  Each block is
	 * composed of a header containing:
	 * <ol>
	 *   <li>Block size.</li>
	 *   <li>Cell size (in integer coordinates).</li>
	 *   <li>Integer coordinates of its lower-left corner.</li>
	 *   <li>Exact number of triangles stored in this leaf.</li>
	 * </ol>
	 * It is followed by the integer coordinates of triangle vertices.
	 * 
	 * @param  tree  an OEMM
	 * @param  soupFile  triangle soup file name
	 * @param  structFile  output file containing dispatched data structure
	 * @param  dataFile  dispatched data file
	 */
	public static final void dispatch(OEMM tree, String soupFile, String structFile, String dataFile)
	{
		if (tree == null)
		{
			logger.error("OEMM not initialized!");
			return;
		}
		logger.info("Put triangles into a linearized octree");
		logger.debug("Raw OEMM: compute global offset for raw file");
		//  For each octant, compute its index and its offset in
		//  output file.
		ComputeOffsetProcedure co_proc = new ComputeOffsetProcedure();
		tree.walk(co_proc);
		long outputFileSize = co_proc.getOffset();
		logger.debug("Raw OEMM: compute min/max indices");
		ComputeMinMaxIndicesProcedure cmmi_proc = new ComputeMinMaxIndicesProcedure();
		tree.walk(cmmi_proc);
		
		logger.debug("Raw OEMM: dispatch triangles into raw OEMM");
		try
		{
			RandomAccessFile raf = new RandomAccessFile(dataFile, "rw");
			FileChannel fc = raf.getChannel();
			raf.setLength(outputFileSize);

			Map<OEMM.Node, ByteBuffer> buffers = new HashMap<OEMM.Node, ByteBuffer>();
			DispatchTriangles dt = new DispatchTriangles(tree, fc, buffers);
			readSoup(soupFile, dt);

			logger.debug("Raw OEMM: flush buffers");
			FlushBuffersProcedure fb_proc = new FlushBuffersProcedure(fc, buffers);
			tree.walk(fb_proc);
			raf.close();
			
			//  Write octree data structure onto disk
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(structFile)));
			WriteStructureProcedure wh_proc = new WriteStructureProcedure(out, dataFile, tree.getNumberOfLeaves(), tree.x0);
			tree.walk(wh_proc);
			out.close();
		}
		catch (FileNotFoundException ex)
		{
			logger.error("File "+soupFile+" not found");
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		catch (IOException ex)
		{
			logger.error("I/O error when reading file  "+soupFile);
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	
	private static final class DispatchTriangles implements SoupReaderInterface
	{
		private OEMM.Node [] cells = new OEMM.Node[3];
		private int [] ijk9 = new int[9];
		private OEMM oemm;
		private FileChannel fc;
		private Map<OEMM.Node, ByteBuffer> buffers;
		public DispatchTriangles(OEMM o, FileChannel f, Map<OEMM.Node, ByteBuffer> m)
		{
			oemm = o;
			fc = f;
			buffers = m;
		}
		public void processVertex(int i, double [] xyz)
		{
			oemm.double2int(xyz, ijk9);
			cells[i] = oemm.search(ijk9);
			if (i < 2)
			{
				for (int j = 0; j < 3; j++)
					ijk9[3*i+3+j] = ijk9[j];
			}
		}
		public void processTriangle(int group)
		{
			try
			{
				addToCell(fc, cells[0], buffers, ijk9, group);
				if (cells[1] != cells[0])
					addToCell(fc, cells[1], buffers, ijk9, group);
				if (cells[2] != cells[0] && cells[2] != cells[1])
					addToCell(fc, cells[2], buffers, ijk9, group);
			}
			catch (IOException ex)
			{
				logger.error("I/O error when writing dispatched file");
				ex.printStackTrace();
				throw new RuntimeException(ex);
			}
		}
	}

	private static final void addToCell(FileChannel fc, OEMM.Node current, Map<OEMM.Node, ByteBuffer> buffers, int [] ijk, int attribute)
		throws IOException
	{
		assert current.counter <= fc.size();
		//  With 20 millions of triangles, unbuffered output took 420s
		//  and buffered output 180s (4K buffer cache)
		ByteBuffer list = buffers.get(current);
		if (list == null)
		{
			//  Must be a multiple of 10!
			list = ByteBuffer.allocate(4000);
			buffers.put(current, list);
			list.putLong(current.counter);
			list.flip();
			fc.write(list, current.counter);
			current.counter += list.limit();
			list.clear();
		}
		else if (!list.hasRemaining())
		{
			// Flush buffer
			list.flip();
			fc.write(list, current.counter);
			current.counter += list.limit();
			list.clear();
		}
		for (int i = 0; i < ijk.length; i++)
			list.putInt(ijk[i]);
		list.putInt(attribute);
		current.tn++;
	}
	
	private static final class ComputeOffsetProcedure extends TraversalProcedure
	{
		private long offset = 0L;
		public final int action(OEMM oemm, OEMM.Node current, int octant, int visit)
		{
			if (visit != LEAF)
				return OK;
			current.counter = offset;
			offset += 8L + TRIANGLE_SIZE_DISPATCHED * (long) current.tn;
			//  Reinitialize this counter for further processing
			current.tn = 0;
			return OK;
		}
		public long getOffset()
		{
			return offset;
		}
		public void init(OEMM oemm)
		{
			super.init(oemm);
			offset = 0L;
		}
	}
	
	private static final class ComputeMinMaxIndicesProcedure extends TraversalProcedure
	{
		private int nrLeaves = 0;
		public final int action(OEMM oemm, OEMM.Node current, int octant, int visit)
		{
			if (visit == PREORDER)
				current.minIndex = nrLeaves;
			else if (visit == POSTORDER)
				current.maxIndex = nrLeaves;
			else if (visit == LEAF)
			{
				current.leafIndex = nrLeaves;
				nrLeaves++;
			}
			return OK;
		}
	}
	
	private static final class FlushBuffersProcedure extends TraversalProcedure
	{
		private FileChannel fc;
		private Map<OEMM.Node, ByteBuffer> buffers;
		public FlushBuffersProcedure(FileChannel channel, Map<OEMM.Node, ByteBuffer> m)
		{
			fc = channel;
			buffers = m;
		}
		public final int action(OEMM oemm, OEMM.Node current, int octant, int visit)
		{
			if (visit != LEAF)
				return OK;
			ByteBuffer list = buffers.get(current);
			if (list == null)
			{
				list = ByteBuffer.allocate(8);
				list.putLong(current.counter);
				list.rewind();
				try
				{
					assert current.counter <= fc.size();
					fc.write(list, current.counter);
				}
				catch (IOException ex)
				{
					logger.error("I/O error when writing file");
				}
				return OK;
			}
			// Flush buffer
			try
			{
				assert current.counter <= fc.size();
				list.flip();
				fc.write(list, current.counter);
				current.counter += list.limit();
			}
			catch (IOException ex)
			{
				logger.error("I/O error when writing file");
			}
			return OK;
		}
	}
	
	private static final class WriteStructureProcedure extends TraversalProcedure
	{
		private DataOutputStream out;
		public WriteStructureProcedure(DataOutputStream outStream, String dataFile, int l, double [] x0)
			throws IOException
		{
			out = outStream;
			//  Format version
			out.writeInt(1);
			//  Number of leaves
			out.writeInt(l);
			//  Number of bytes in data file name
			out.writeInt(dataFile.length());
			//  Data file
			out.writeBytes(dataFile);
			//  Integer <--> double coordinates
			for (int i = 0; i < 4; i++)
				out.writeDouble(x0[i]);
		}
		public final int action(OEMM oemm, OEMM.Node current, int octant, int visit)
		{
			if (visit != LEAF)
				return OK;
			try
			{
				//  Offset in data file
				//  This offset had been shifted when writing triangles
				current.counter -= 8L + TRIANGLE_SIZE_DISPATCHED * current.tn;
				out.writeLong(current.counter);
				//  Number of triangles really found
				out.writeInt(current.tn);
				//  Edge size
				out.writeInt(current.size);
				//  Lower-left corner
				out.writeInt(current.i0);
				out.writeInt(current.j0);
				out.writeInt(current.k0);
			}
			catch (IOException ex)
			{
				logger.error("I/O error when writing intermediate raw OEMM");
			}
			return OK;
		}
	}
	
	// TODO: This method may surely be replaced by Storage.readOEMMStructure()
	private static OEMM readDispatchedStructure(String structFile)
	{
		logger.debug("Loading dispatched OEMM structure from "+structFile);
		OEMM ret = new OEMM("(null)");
		try
		{
			DataInputStream bufIn = new DataInputStream(new BufferedInputStream(new FileInputStream(structFile)));
			int [] ijk = new int[3];
			int version = bufIn.readInt();
			assert version == 1;
			int nrleaves = bufIn.readInt();
			int nrbytes = bufIn.readInt();
			byte [] name = new byte[nrbytes+1];
			bufIn.read(name, 0, nrbytes);
			ret = new OEMM(new String(name));
			ret.leaves = new OEMM.Node[nrleaves];
			for (int i = 0; i < 4; i++)
				ret.x0[i] = bufIn.readDouble();
			for (int i = 0; i < nrleaves; i++)
			{
				long position = bufIn.readLong();
				int nr = bufIn.readInt();
				int size = bufIn.readInt();
				ijk[0] = bufIn.readInt();
				ijk[1] = bufIn.readInt();
				ijk[2] = bufIn.readInt();
				OEMM.Node n = new OEMM.Node(size, ijk);
				ret.insert(n);
				n.counter = position;
				n.tn = nr;
				n.leafIndex = i;
				n.isLeaf = true;
				ret.leaves[i] = n;
			}
			bufIn.close();
		}
		catch (FileNotFoundException ex)
		{
			logger.error("File "+structFile+" not found");
		}
		catch (IOException ex)
		{
			logger.error("I/O error when reading file "+structFile);
		}
		//  Adjust minIndex and maxIndex values
		ComputeMinMaxIndicesProcedure cmmi_proc = new ComputeMinMaxIndicesProcedure();
		ret.walk(cmmi_proc);
		return ret;
	}
	
	/**
	 * Transforms dispatched file into an OEMM.
	 *
	 * @param structFile  dispatched file.
	 * @param outDir  directory in which OEMM structure will be stored.
	 */
	public static void indexOEMM(String structFile, String outDir)
	{
		try
		{
			OEMM ret = readDispatchedStructure(structFile);
			if (logger.isDebugEnabled())
				ret.printInfos();
			logger.info("Write octree cells onto disk");
			OEMM fake = new OEMM(outDir);
			logger.debug("Store data header on disk");
			new File(outDir).mkdirs();
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(fake.getFileName())));
			oos.writeObject(ret);
			oos.writeObject(Integer.valueOf(ret.getNumberOfLeaves()));
			
			//  Index internal vertices
			logger.debug("Index internal vertices");
			FileInputStream fis = new FileInputStream(ret.getFileName());
			IndexInternalVerticesProcedure iiv_proc = new IndexInternalVerticesProcedure(fis, oos, outDir);
			ret.walk(iiv_proc);
			fis.close();
			oos.close();
			
			//  Index external vertices
			logger.debug("Index external vertices");
			fis = new FileInputStream(ret.getFileName());
			//  We have a handle on triangle soup, which will be
			//  no more read, we can now set output diirectory
			//  to its final value.
			ret.setDirectory(outDir);
			IndexExternalVerticesProcedure iev_proc = new IndexExternalVerticesProcedure(fis);
			ret.walk(iev_proc);
			fis.close();
			
			//  Transform vertex coordinates into doubles
			logger.debug("Transform vertex coordinates into doubles");
			ConvertVertexCoordinatesProcedure cvc_proc = new ConvertVertexCoordinatesProcedure();
			ret.walk(cvc_proc);
			
			//ShowIndexedNodesProcedure debug = new ShowIndexedNodesProcedure();
			//ret.walk(debug);
		}
		catch (FileNotFoundException ex)
		{
			logger.error("File "+structFile+" not found");
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		catch (IOException ex)
		{
			logger.error("I/O error when reading file  "+structFile);
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	
	private static class IndexInternalVerticesProcedure extends TraversalProcedure
	{
		private FileChannel fc;
		private ObjectOutputStream oos;
		private String outDir;
		private int globalIndex = 0;
		private ArrayList<String> path = new ArrayList<String>();
		private int [] ijk = new int[3];
		private int room = 0;
		public IndexInternalVerticesProcedure(FileInputStream in, ObjectOutputStream headerOut, String dir)
		{
			fc = in.getChannel();
			outDir = dir;
			oos = headerOut;
		}
		public void init(OEMM oemm)
		{
			super.init(oemm);
			room = ((1 << 31) - 3*oemm.root.tn) / oemm.getNumberOfLeaves();
		}
		public final int action(OEMM oemm, OEMM.Node current, int octant, int visit)
		{
			if (current == oemm.root && visit != LEAF)
				return OK;
			if (visit == POSTORDER)
			{
				path.remove(path.size() - 1);
				return OK;
			}
			else if (visit == PREORDER)
			{
				path.add(""+octant);
				return OK;
			}
			
			if (logger.isDebugEnabled())
				logger.debug("Indexing internal vertices of node "+(current.leafIndex+1)+"/"+oemm.getNumberOfLeaves());
			ijk[0] = current.i0;
			ijk[1] = current.j0;
			ijk[2] = current.k0;
			if (path.size() > 0)
			{
				StringBuffer sbdir = new StringBuffer(path.get(0));
				for (int i = 1; i < path.size(); i++)
					sbdir.append(File.separator + path.get(i));
				String dir = sbdir.toString();
				File d = new File(outDir, dir);
				d.mkdirs();
				current.setPathComponents(path, octant);
			}
			else
			{
				new File(outDir).mkdirs();
				current.setPathComponents(null, octant);
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
				FileChannel fcv = new FileOutputStream(new File(outDir, current.file+"i")).getChannel();
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
								OEMM.Node node = oemm.search(ijk);
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
							assert (leaf[i] == current.leafIndex && pointIndex[i] >= 0) || (leaf[i] != current.leafIndex && pointIndex[i] < 0);
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
				oos.writeObject(current);
				current.tn = tn;
				
				FileChannel fca = new FileOutputStream(new File(outDir, current.file+"a")).getChannel();
				bb.clear();
				//  Inner vertices of this node
				int freeSpace = bb.capacity();
				for (int i = 0; i < index; i++)
				{
					int n = localAdjSet[i].size();
					if (freeSpace < 1 + n)
					{
						bb.flip();
						fca.write(bb);
						bb.clear();
						freeSpace = bb.capacity();
					}
					//     Adjacent leaves
					bb.put((byte) n);
					for (TIntIterator it = localAdjSet[i].iterator(); it.hasNext();)
						bb.put((byte) invMap.get(it.next()));
					freeSpace -= 1 + n;
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
		private FileChannel fc;
		private int [] ijk = new int[3];
		private PAVLTreeIntArrayDup [] vertices;
		private boolean [] needed;
		private int nr_ld_leaves = 0;
		public IndexExternalVerticesProcedure(FileInputStream in)
		{
			fc = in.getChannel();
		}
		public void init(OEMM oemm)
		{
			vertices = new PAVLTreeIntArrayDup[oemm.getNumberOfLeaves()];
			needed = new boolean[vertices.length];
		}
		public final int action(OEMM oemm, OEMM.Node current, int octant, int visit)
		{
			if (visit != LEAF)
				return OK;
			if (logger.isDebugEnabled())
				logger.debug("Indexing external vertices of node "+(current.leafIndex+1)+"/"+oemm.getNumberOfLeaves());
			int rc = OK;
			try
			{
				rc = processIndexExternalVerticesProcedure(oemm, current);
			}
			catch (OutOfMemoryError ex)
			{
				for (int i = 0; i < needed.length; i++)
				{
					needed[i] = false;
					vertices[i] = null;
				}
				rc = processIndexExternalVerticesProcedure(oemm, current);
			}
			return rc;
		}

		private final int processIndexExternalVerticesProcedure(OEMM oemm, OEMM.Node current)
		{
			needed[current.leafIndex] = true;
			for (int i = 0; i < current.adjLeaves.size(); i++)
				needed[current.adjLeaves.get(i)] = true;
			for (int i = 0; i < vertices.length; i++)
			{
				if (needed[i] && vertices[i] == null)
				{
					nr_ld_leaves++;
					vertices[i] = loadVerticesInAVLTreeDup(oemm.getDirectory(), oemm.leaves[i]);
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
				FileChannel fct = new FileOutputStream(new File(oemm.getDirectory(), current.file+"t")).getChannel();
				bb.clear();
				IntBuffer bbI = bb.asIntBuffer();
				bbt.clear();
				IntBuffer bbtI = bbt.asIntBuffer();
				int remaining = current.tn;
				// If TRIANGLE_SIZE_INDEXED > TRIANGLE_SIZE_DISPATCHED
				// the following loop must be fixed to use the larger
				// value.
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
		private int [] ijk = new int[3];
		private double [] xyz = new double[3];
		public final int action(OEMM oemm, OEMM.Node current, int octant, int visit)
		{
			if (visit != LEAF)
				return OK;
			if (logger.isDebugEnabled())
				logger.debug("Converting coordinates of node "+(current.leafIndex+1)+"/"+oemm.getNumberOfLeaves());
			
			try
			{
				FileChannel fci = new FileInputStream(new File(oemm.getDirectory(), current.file+"i")).getChannel();
				FileChannel fco = new FileOutputStream(new File(oemm.getDirectory(), current.file+"v")).getChannel();
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
				new File(oemm.getDirectory(), current.file+"i").delete();
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
	
	private static PAVLTreeIntArrayDup loadVerticesInAVLTreeDup(String outDir, OEMM.Node current)
	{
		PAVLTreeIntArrayDup ret = new PAVLTreeIntArrayDup();
		int [] ijk = new int[3];
		try
		{
			FileChannel fc = new FileInputStream(new File(outDir, current.file+"i")).getChannel();
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
			logger.error("I/O error when reading indexed file "+outDir+File.separator+current.file+"i");
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}

		return ret;
	}
	
}
