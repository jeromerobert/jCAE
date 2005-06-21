/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2005
                  Jerome Robert <jeromerobert@users.sourceforge.net>

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

import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import org.apache.log4j.Logger;

/**
 * This class implements out-of-core storage of raw OEMM structures.
 */
public class RawStorage
{
	private static Logger logger = Logger.getLogger(RawStorage.class);	

	//  In the raw file, a triangle has 9 double coordinates and an int.
	private static final long TRIANGLE_SIZE_RAW = 76L;
	//  In the dispatched file, a triangle has 9 int coordinates and an int.
	private static final long TRIANGLE_SIZE_DISPATCHED = 40L;
	
	/**
	 * Build a raw OEMM and count the number of triangles which have to be assigned
	 * to each leaf.
	 *
	 * A triangle soup is read from the file associated with a raw OEMM, deepest cells
	 * for triangle vertices are created if needed, and triangle counters are updated.
	 * When this routine returns, all leaf nodes have been created and each node knowa
	 * how many triangles will be assigned to it in later stages.
	 * 
	 * @param  tree  a raw OEMM
	 */
	public static void countTriangles(RawOEMM tree)
	{
		if (tree.status != RawOEMM.OEMM_CREATED)
		{
			logger.error("The RawOEMM must first be initialized by calling RawOEMM(String file, int lmax, double [] umin, double [] umax)");
			return;
		}
		logger.debug("Reading "+tree.getFileName()+" and count triangles");
		long tcount = 0;
		try
		{
			int [] ijk = new int[3];
			double [] xyz = new double[3];
			OEMMNode [] cells = new OEMMNode[3];
			FileInputStream fs = new FileInputStream(tree.getFileName());
			DataInputStream coordsIn = new DataInputStream(new BufferedInputStream(fs));
			long size = fs.getChannel().size();
			for(long nr = 0L; nr < size; nr += TRIANGLE_SIZE_RAW)
			{
				for (int i = 0; i < 3; i++)
				{
					xyz[0] = coordsIn.readDouble();
					xyz[1] = coordsIn.readDouble();
					xyz[2] = coordsIn.readDouble();
					tree.double2int(xyz, ijk);
					cells[i] = tree.build(0, ijk);
				}
				coordsIn.readInt();
				cells[0].tn++;
				if (cells[1] != cells[0])
					cells[1].tn++;
				if (cells[2] != cells[0] && cells[2] != cells[1])
					cells[2].tn++;
				tcount++;
			}
			coordsIn.close();
		}
		catch (FileNotFoundException ex)
		{
			logger.error("File "+tree.getFileName()+" not found");
		}
		catch (IOException ex)
		{
			logger.error("I/O error when reading file "+tree.getFileName());
		}
		logger.info("Number of triangles: "+tcount);
		tree.status = RawOEMM.OEMM_INITIALIZED;
		tree.printInfos();
	}
	
	/**
	 * Read the triangle soup another time, and dispatch triangles into an intermediate
	 * OEMM data structure.
	 *
	 * The data structure has been setup in {@link #countTriangles}, and willl now be
	 * written onto disk as a linear octree.  Each block is composed of a header containing:
	 * <ol>
	 * <li>Block size.</li>
	 * <li>Cell size (in integer coordinates).</li>
	 * <li>Integer coordinates of its lower-left corner.</li>
	 * <li>Exact number of triangles stored in this leaf.</li>
	 * </ol>
	 * It is followed by the integer coordinates of triangle vertices.
	 * 
	 * @param  tree  a raw OEMM
	 * @param  structFile  output file containing octree data structure
	 * @param  dataFile  dispatched data file
	 */
	public static final void dispatch(RawOEMM tree, String structFile, String dataFile)
	{
		if (tree.status < RawOEMM.OEMM_INITIALIZED)
		{
			logger.error("The RawOEMM must first be initialized by calling countTriangles()");
			return;
		}
		logger.debug("Raw OEMM: computing global offset for raw file");
		//  For each octant, compute its index and its offset in output file.
		ComputeOffsetProcedure proc = new ComputeOffsetProcedure();
		tree.walk(proc);
		long outputFileSize = proc.getOffset();
		ComputeMinMaxIndicesProcedure cmmi_proc = new ComputeMinMaxIndicesProcedure();
		tree.walk(cmmi_proc);
		
		logger.debug("Raw OEMM: dispatch triangles into raw OEMM");
		try
		{
			int [] ijk = new int[9];
			double [] xyz = new double[3];
			OEMMNode [] cells = new OEMMNode[3];
			FileInputStream fs = new FileInputStream(tree.getFileName());
			DataInputStream coordsIn = new DataInputStream(new BufferedInputStream(fs));
			long size = fs.getChannel().size();
			RandomAccessFile raf = new RandomAccessFile(dataFile, "rw");
			FileChannel fc = raf.getChannel();
			raf.setLength(outputFileSize);
			for(long nr = 0L; nr < size; nr += TRIANGLE_SIZE_RAW)
			{
				for (int i = 0; i < 3; i++)
				{
					xyz[0] = coordsIn.readDouble();
					xyz[1] = coordsIn.readDouble();
					xyz[2] = coordsIn.readDouble();
					tree.double2int(xyz, ijk);
					cells[i] = tree.search(ijk);
					if (i < 2)
					{
						for (int j = 0; j < 3; j++)
							ijk[6-3*i+j] = ijk[j];
					}
				}
				int attribute = coordsIn.readInt();
				addToCell(fc, cells[0], ijk, attribute);
				if (cells[1] != cells[0])
					addToCell(fc, cells[1], ijk, attribute);
				if (cells[2] != cells[0] && cells[2] != cells[1])
					addToCell(fc, cells[2], ijk, attribute);
			}
			coordsIn.close();
			logger.debug("Raw OEMM: flush buffers");
			FlushBuffersProcedure fb_proc = new FlushBuffersProcedure(fc);
			tree.walk(fb_proc);
			raf.close();
			
			//  Write octree data structure onto disk
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(structFile)));
			WriteStructureProcedure wh_proc = new WriteStructureProcedure(out, dataFile, tree.nr_leaves, tree.x0);
			tree.walk(wh_proc);
			out.close();
		}
		catch (FileNotFoundException ex)
		{
			logger.error("File "+tree.getFileName()+" not found");
		}
		catch (IOException ex)
		{
			logger.error("I/O error when reading file  "+tree.getFileName());
		}
	}
	
	/**
	 * Extracts a raw OEMM from an intermediate OEMM.
	 *
	 * @param  file  file containing the intermediate OEMM.
	 * @return RawOEMM a raw OEMM.
	 */
	public static OEMM loadIntermediate(String file)
	{
		logger.debug("Loading intermediate raw OEMM from "+file);
		OEMM ret = new OEMM("(null)");
		try
		{
			DataInputStream bufIn = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
			int [] ijk = new int[3];
			int version = bufIn.readInt();
			assert version == 1;
			int nrleaves = bufIn.readInt();
			int nrbytes = bufIn.readInt();
			byte [] name = new byte[nrbytes+1];
			bufIn.read(name, 0, nrbytes);
			ret = new OEMM(new String(name));
			ret.leaves = new OEMMNode[nrleaves];
			for (int i = 0; i < 4; i++)
				ret.x0[i] = bufIn.readDouble();
			//  ret.nr_leaves will be set by this loop in ret.build()
			for (int i = 0; i < nrleaves; i++)
			{
				long position = bufIn.readLong();
				int nr = bufIn.readInt();
				int size = bufIn.readInt();
				ijk[0] = bufIn.readInt();
				ijk[1] = bufIn.readInt();
				ijk[2] = bufIn.readInt();
				OEMMNode n = ret.build(size, ijk);
				n.counter = position;
				n.tn = nr;
				n.leafIndex = i;
				n.isLeaf = true;
				ret.leaves[i] = n;
				if (!ret.head[0].isLeaf)
					ret.head[0].tn += n.tn;
			}
			bufIn.close();
		}
		catch (FileNotFoundException ex)
		{
			logger.error("File "+file+" not found");
		}
		catch (IOException ex)
		{
			logger.error("I/O error when reading file "+file);
		}
		ret.status = RawOEMM.OEMM_INITIALIZED;
		//  Adjust minIndex and maxIndex values
		ComputeMinMaxIndicesProcedure cmmi_proc = new ComputeMinMaxIndicesProcedure();
		ret.walk(cmmi_proc);
		return ret;
	}
	
	private static void addToCell(FileChannel fc, OEMMNode current, int [] ijk, int attribute)
		throws java.io.IOException
	{
		assert current.counter <= fc.size();
		//  With 20 millions of triangles, unbuffered output took 420s
		//  and buffered output 180s (4K buffer cache)
		Int10Array list = (Int10Array) current.extra;
		if (current.extra == null)
		{
			current.extra = new Int10Array();
			list = (Int10Array) current.extra;
		}
		else if (list.isFull())
		{
			// Flush buffer
			int size = list.size();
			ByteBuffer buf = ByteBuffer.allocate(4*size);
			IntBuffer bufInt = buf.asIntBuffer();
			bufInt.put(list.toNativeArray(), 0, size);
			buf.rewind();
			fc.write(buf, current.counter);
			current.counter += 4*size;
			list.clear();
		}
		list.add(ijk);
		list.add(attribute);
		current.tn++;
	}
	
	private static class ComputeOffsetProcedure extends TraversalProcedure
	{
		private long offset = 0L;
		public final int action(OEMMNode current, int octant, int visit)
		{
			if (visit != LEAF)
				return SKIPWALK;
			current.counter = offset;
			offset += TRIANGLE_SIZE_DISPATCHED * (long) current.tn;
			//  Reinitialize this counter for further processing
			current.tn = 0;
			current.extra = null;
			return OK;
		}
		public long getOffset()
		{
			return offset;
		}
		public void init()
		{
			super.init();
			offset = 0L;
		}
	}
	
	private static class ComputeMinMaxIndicesProcedure extends TraversalProcedure
	{
		private int nrLeaves = 0;
		public final int action(OEMMNode current, int octant, int visit)
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
	
	private static class FlushBuffersProcedure extends TraversalProcedure
	{
		private FileChannel fc;
		public FlushBuffersProcedure(FileChannel channel)
		{
			fc = channel;
		}
		public final int action(OEMMNode current, int octant, int visit)
		{
			if (visit != LEAF || current.extra == null)
				return SKIPWALK;
			Int10Array list = (Int10Array) current.extra;
			if (list.isEmpty())
				return OK;
			// Flush buffer
			int size = list.size();
			ByteBuffer buf = ByteBuffer.allocate(4*size);
			IntBuffer bufInt = buf.asIntBuffer();
			bufInt.put(list.toNativeArray(), 0, size);
			buf.rewind();
			try
			{
				assert current.counter <= fc.size();
				fc.write(buf, current.counter);
			}
			catch (IOException ex)
			{
				logger.error("I/O error when writing file");
			}
			current.counter += 4*size;
			return OK;
		}
	}
	
	private static class WriteStructureProcedure extends TraversalProcedure
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
		public final int action(OEMMNode current, int octant, int visit)
		{
			if (visit != LEAF)
				return SKIPWALK;
			try
			{
				//  Offset in data file
				//  This offset had been shifted when writing triangles
				current.counter -= TRIANGLE_SIZE_DISPATCHED * current.tn;
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
	
	//  With 20 millions of triangles, writing dispatched file took 230s with
	//  TIntArrayList and 180s with Int10Array
	private static class Int10Array
	{
		private static final int capacity = 4090;  // Must be a multiple of 10!
		private int [] data = new int[capacity];
		private int offset = 0;
		public int size()
		{
			return offset;
		}
		public boolean isEmpty()
		{
			return (offset == 0);
		}
		public boolean isFull()
		{
			return (offset >= capacity);
		}
		public void add(int [] src)
		{
			System.arraycopy(src, 0, data, offset, src.length);
			offset += src.length;
		}
		public void add(int val)
		{
			data[offset] = val;
			offset++;
		}
		public int [] toNativeArray()
		{
			return data;
		}
		public void clear()
		{
			offset = 0;
		}
	}
}
