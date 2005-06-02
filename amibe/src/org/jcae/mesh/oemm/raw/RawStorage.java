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

package org.jcae.mesh.oemm.raw;

import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.apache.log4j.Logger;

/**
 * This class implements out-of-core storage of raw OEMM structures.
 */
public class RawStorage
{
	private static Logger logger=Logger.getLogger(RawStorage.class);	
	private final static int INTERMEDIATE_HEADER_SIZE = 8 + 5*4;
	private final static long INTERMEDIATE_HEADER_OFFSET_NT = 4L;
	
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
			RawNode [] cells = new RawNode[3];
			FileInputStream fs = new FileInputStream(tree.getFileName());
			DataInputStream coordsIn = new DataInputStream(new BufferedInputStream(fs));
			long size = fs.getChannel().size();
			for(long nr = 0L; nr < size; nr += 72L)
			{
				for (int i = 0; i < 3; i++)
				{
					xyz[0] = coordsIn.readDouble();
					xyz[1] = coordsIn.readDouble();
					xyz[2] = coordsIn.readDouble();
					tree.double2int(xyz, ijk);
					cells[i] = RawOEMM.search(tree, 0, ijk, true);
				}
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
	 * @param  file  the output file
	 */
	public static final void dispatch(RawOEMM tree, String file)
	{
		if (tree.status != RawOEMM.OEMM_INITIALIZED)
		{
			logger.error("The RawOEMM must first be initialized by calling countTriangles()");
			return;
		}
		logger.debug("Raw OEMM: computing global offset for raw file");
		//  First compute offset in output file for each octant
		ComputeOffset proc = new ComputeOffset();
		tree.walk(proc);
		
		logger.debug("Raw OEMM: dispatch triangles into raw OEMM");
		//  TODO: Output must be buffered
		try
		{
			int [] ijk = new int[9];
			double [] xyz = new double[3];
			RawNode [] cells = new RawNode[3];
			FileInputStream fs = new FileInputStream(tree.getFileName());
			DataInputStream coordsIn = new DataInputStream(new BufferedInputStream(fs));
			long size = fs.getChannel().size();
			RandomAccessFile raf = new RandomAccessFile(file, "rw");
			FileChannel fc = raf.getChannel();
			raf.setLength(proc.getOffset());
			for(long nr = 0L; nr < size; nr += 72L)
			{
				for (int i = 0; i < 3; i++)
				{
					xyz[0] = coordsIn.readDouble();
					xyz[1] = coordsIn.readDouble();
					xyz[2] = coordsIn.readDouble();
					tree.double2int(xyz, ijk);
					cells[i] = RawOEMM.search(tree, 0, ijk, false);
					if (i < 2)
					{
						for (int j = 0; j < 3; j++)
							ijk[6-3*i+j] = ijk[j];
					}
				}
				addToCell(fc, cells[0], ijk);
				if (cells[0] != cells[1])
					addToCell(fc, cells[1], ijk);
				if (cells[2] != cells[0] && cells[2] != cells[1])
					addToCell(fc, cells[2], ijk);
			}
			coordsIn.close();
			//  Adjust block size
			StoreBlockSizeProcedure sproc = new StoreBlockSizeProcedure(fc);
			tree.walk(sproc);
			raf.close();
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
	public static RawOEMM loadIntermediate(String file)
	{
		logger.debug("Loading intermediate raw OEMM from "+file);
		RawOEMM ret = new RawOEMM();
		try
		{
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			FileInputStream fs = new FileInputStream(file);
			DataInputStream bufIn = new DataInputStream(new BufferedInputStream(fs));
			long len = fs.getChannel().size();
			long pos = 0L;
			int [] ijk = new int[3];
			while (pos < len)
			{
				long blockSize = bufIn.readLong();
				int size = bufIn.readInt();
				ijk[0] = bufIn.readInt();
				ijk[1] = bufIn.readInt();
				ijk[2] = bufIn.readInt();
				int nr = bufIn.readInt();
				bufIn.skipBytes((int) blockSize);
				pos += INTERMEDIATE_HEADER_SIZE + blockSize;
				RawOEMM.search(ret, size, ijk, true);
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
		return ret;
	}
	
	public static RawNode readBlockHeader(DataInputStream bufIn)
	{
		RawNode ret = null;
		try
		{
			int [] ijk = new int[3];
			long blockSize = bufIn.readLong();
			int size = bufIn.readInt();
			ijk[0] = bufIn.readInt();
			ijk[1] = bufIn.readInt();
			ijk[2] = bufIn.readInt();
			ret = new RawNode(size, ijk);
			ret.tn = bufIn.readInt();
			//  The number of trailing bytes is stored
			//  in this member which is now unused.
			ret.counter = blockSize - 36L * (long) ret.tn;
		}
		catch (IOException ex)
		{
			logger.error("I/O error when reading file "+bufIn);
		}
		return ret;
	}
	
	/**
	 * Display debugging information about an intermediate OEMM.
	 *
	 * @param  file  file containing the intermediate OEMM.
	 */
	public static void showIntermediateRawStorage(String file)
	{
		try
		{
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			FileInputStream fs = new FileInputStream(file);
			DataInputStream bufIn = new DataInputStream(new BufferedInputStream(fs));
			long len = fs.getChannel().size();
			long pos = 0L;
			int iBlock = 0;
			long total = 0L;
			while (pos < len)
			{
				iBlock++;
				long blockSize = bufIn.readLong();
				int size = bufIn.readInt();
				int i0 = bufIn.readInt();
				int j0 = bufIn.readInt();
				int k0 = bufIn.readInt();
				int nr = bufIn.readInt();
				logger.debug("block "+iBlock+": triangles="+nr+"  block size= "+blockSize);
				bufIn.skipBytes((int) blockSize);
				pos += INTERMEDIATE_HEADER_SIZE + blockSize;
				total += (long) nr;
			}
			logger.debug("Total number of triangles: "+total);
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
	}
	
	private static void addToCell(FileChannel fc, RawNode current, int [] ijk)
		throws java.io.IOException
	{
		assert current.counter <= fc.size();
		if (current.counter <= 0L)
		{
			current.counter = -current.counter;
			ByteBuffer buf2 = ByteBuffer.allocate(INTERMEDIATE_HEADER_SIZE);
			long blockSize = 36L * (long) current.tn;
			buf2.putLong(blockSize);
			buf2.putInt(current.size);
			buf2.putInt(current.i0);
			buf2.putInt(current.j0);
			buf2.putInt(current.k0);
			//  Exact number of triangles, this must be the
			//  last item of block header
			buf2.putInt(0);
			buf2.rewind();
			fc.position(current.counter);
			fc.write(buf2);
			fc.force(false);
			current.counter += (long) INTERMEDIATE_HEADER_SIZE;
			//  The number of triangles really added is stored in tn
			current.tn = 0;
		}
		ByteBuffer buf = ByteBuffer.allocate(36);
		for (int i = 0; i < 9; i++)
			buf.putInt(ijk[i]);
		buf.rewind();
		fc.write(buf, current.counter);
		current.counter += 36L;
		current.tn++;
	}
	
	private static class ComputeOffset extends TraversalProcedure
	{
		private long offset = 0L;
		public final int action(RawNode current, int visit)
		{
			if (visit != LEAF)
				return SKIPWALK;
			current.counter = -offset;
			offset += 36L * (long) current.tn + (long) INTERMEDIATE_HEADER_SIZE;
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
	
	//  By convention, offsets are negative when block headers have not been
	//  written onto file.
	private final static class StoreBlockSizeProcedure extends TraversalProcedure
	{
		private FileChannel channel;
		private ByteBuffer buf = ByteBuffer.allocate(8);
		public StoreBlockSizeProcedure(FileChannel fc)
		{
			channel = fc;
		}
		public final int action(RawNode current, int visit)
		{
			if (visit != LEAF)
				return SKIPWALK;
			// The number of triangles is the last item of block header
			long offset = current.counter - 36L * (long) current.tn - INTERMEDIATE_HEADER_OFFSET_NT;
			buf.putInt(0, current.tn);
			buf.rewind();
			try
			{
				channel.write(buf, offset);
			}
			catch (IOException ex)
			{
				logger.error("I/O error when writing intermediate raw OEMM");
			}
			return OK;
		}
	}
	
}
