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
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.apache.log4j.Logger;

import javax.media.j3d.Appearance;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.ColoringAttributes;

/*^
 * Converts a raw OEMM (Octree External Memory Mesh, out-of-core mesg)
 * into an indexed OEMM.
 *
 * Input file is a triangle soup, i.e. it is a stream of 9 coordiantes, which
 * are the cooordinates of each vertex.  Vertices common to several triangles
 * are duplicated.
 */
public class RawOEMM
{
	private static Logger logger=Logger.getLogger(RawOEMM.class);	
	
	public static final int MAXLEVEL = 30;
	private RawOEMMCell root;
	private int nCells = 1;
	private String rawFile;
	private int nr_levels;
	private int gridSize;
	private double [] x0 = new double[4];
	
	private class RawOEMMCell
	{
		//  Current level
		private int level;
		//  Number of triangles
		public int tn = 0;
		//  Global offest
		private long offset = 0L;
		//  Child list
		private RawOEMMCell[] child = new RawOEMMCell[8];
		//  Is this node a leaf?
		private boolean isLeaf = true;
		public final static int INTERMEDIATE_HEADER_SIZE = 16;
		
		public RawOEMMCell(int l)
		{
			level = l;
		}
		public void add(FileChannel fc, int [] ijk)
			throws java.io.IOException
		{
			assert offset <= fc.size();
			if (offset <= 0L)
			{
				offset = -offset;
				ByteBuffer buf2 = ByteBuffer.allocate(INTERMEDIATE_HEADER_SIZE);
				long blockSize = 36L * (long) tn;
				buf2.putLong(blockSize);
				//  Exact number of triangles
				buf2.putLong(0L);
				buf2.rewind();
				fc.position(offset);
				fc.write(buf2);
				fc.force(false);
				offset += (long) INTERMEDIATE_HEADER_SIZE;
				//  The number of triangles really added is stored in tn
				tn = 0;
			}
			ByteBuffer buf = ByteBuffer.allocate(36);
			for (int i = 0; i < 9; i++)
				buf.putInt(ijk[i]);
			buf.rewind();
			fc.write(buf, offset);
			offset += 36L;
			tn++;
		}
	}
	
	//  Functions applied to each node in tree traversal.
	private abstract class RawOEMMProcedure
	{
		protected static final int PREORDER  = 1;
		protected static final int POSTORDER = 2;
		protected static final int LEAF      = 3;
		
		public static final int OK  = 0;
		public static final int ABORT = 1;
		public static final int SKIPCHILD = 2;
		public static final int SKIPWALK = 4;
		
		private int nrNodes = 0;
		private int nrLeaves = 0;
		
		public void init()
		{
			nrNodes = 0;
			nrLeaves = 0;
		}
		
		public void printStats()
		{
			logger.debug("Leaves: "+nrLeaves+"   Octants: "+nrNodes);
		}
		
		//  This method is called by OcTree.	, it is then applied to
		//  the whole octree.  Its return status may change processing:
		//    ABORT: exit from 	() immediately
		//    SKIPCHILD: skip current cell (ie do not process its children)
		//    SKIPWALK: node was skipped, process normally
		//    OK: process normally
		public abstract int action(RawOEMMCell c, int s, int i, int j, int k, int visit);
		
		public int preorder(RawOEMMCell c, int s, int i0, int j0, int k0)
		{
			int res = 0;
			nrNodes++;
			if (c.isLeaf)
			{
				nrLeaves++;
				logger.debug("Found LEAF: "+s+" "+i0+" "+j0+" "+k0);
				res = action(c, s, i0, j0, k0, LEAF);
			}
			else
			{
				logger.debug("Found PREORDER: "+s+" "+i0+" "+j0+" "+k0);
				res = action(c, s, i0, j0, k0, PREORDER);
			}
			logger.debug("  Res; "+res);
			return res;
		}
		public int postorder(RawOEMMCell c, int s, int i0, int j0, int k0)
		{
			logger.debug("Found POSTORDER: "+s+" "+i0+" "+j0+" "+k0);
			int res = action(c, s, i0, j0, k0, POSTORDER);
			logger.debug("  Res; "+res);
			return res;
		}
	}
	
	//  Initialize octree
	public RawOEMM(String file, int lmax, double [] umin, double [] umax)
	{
		rawFile = file;
		nr_levels = lmax;
		if (nr_levels > MAXLEVEL)
		{
			logger.error("Max. level too high");
			nr_levels = MAXLEVEL;
		}
		gridSize = 1 << MAXLEVEL;
		double deltaX = Math.abs(umin[0] - umax[0]);
		double deltaY = Math.abs(umin[1] - umax[1]);
		double deltaZ = Math.abs(umin[2] - umax[2]);
		deltaX = Math.max(deltaX, deltaY);
		deltaX = Math.max(deltaX, deltaZ);
		for (int i = 0; i < 3; i++)
			x0[i] = umin[i];
		x0[3] = ((double) gridSize) / deltaX;
		root = new RawOEMMCell(0);
	}
	
	//  Read input file and count for each leaf how many triangles may be stored
	//  within this leaf.
	public void countTriangles()
	{
		long tcount = 0;
		try
		{
			int [] ijk = new int[3];
			double [] xyz = new double[3];
			RawOEMMCell [] cells = new RawOEMMCell[3];
			FileInputStream fs = new FileInputStream(rawFile);
			DataInputStream coordsIn = new DataInputStream(new BufferedInputStream(fs));
			long size = fs.getChannel().size();
			for(long nr = 0L; nr < size; nr += 72L)
			{
				for (int i = 0; i < 3; i++)
				{
					xyz[0] = coordsIn.readDouble();
					xyz[1] = coordsIn.readDouble();
					xyz[2] = coordsIn.readDouble();
					double2int(xyz, ijk);
					cells[i] = findDeepestCell(ijk);
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
			logger.error("File "+rawFile+" not found");
		}
		catch (IOException ex)
		{
			logger.error("I/O error when reading file "+rawFile);
		}
		logger.info("Number of triangles: "+tcount);
		logger.info("Number of octrees: "+nCells);
		logger.info("Max levels: "+nr_levels);
	}
	
	//  Conversion between floating-point and integer coordinates
	public final void double2int(double [] p, int [] ijk)
	{
		for (int i = 0; i < 3; i++)
			ijk[i] = (int) ((p[i] - x0[i]) * x0[3]);
	}
	
	public final void int2double(int [] ijk, double [] p)
	{
		for (int i = 0; i < 3; i++)
			p[i] = x0[i] + ijk[i] / x0[3];
	}
	
	/*         k=0          k=1
	 *      .-------.    .-------.
	 *      | 2 | 3 |    | 6 | 7 |
	 *   j  |---+---|    |---+---|
	 *      | 0 | 1 |    | 4 | 5 |
	 *      `-------'    `-------'
	 *          i          
	 */
	public static final int indexSubOctree(int [] ijk, int size)
	{
		int ret = 0;
		if (size == 0)
			throw new RuntimeException("Exceeded maximal number of levels for octrees... Aborting");
		for (int i = 0; i < 3; i++)
		{
			if ((ijk[i] & size) != 0)
				ret |= 1 << i;
		}
		return ret;
	}
	
	public final RawOEMMCell findDeepestCell(int [] ijk)
	{
		RawOEMMCell current = root;
		int s = gridSize;
		int l = 0;
		while (l < nr_levels)
		{
			l++;
			s >>= 1;
			assert s > 0;
			int ind = indexSubOctree(ijk, s);
			if (null == current.child[ind])
			{
				current.child[ind] = new RawOEMMCell(l);
				current.isLeaf = false;
				nCells++;
			}
			current = current.child[ind];
		}
		return current;
	}
	
	public final RawOEMMCell findCell(int [] ijk)
	{
		RawOEMMCell current = root;
		int s = gridSize;
		int l = 0;
		while (l < nr_levels)
		{
			l++;
			s >>= 1;
			assert s > 0;
			if (current.isLeaf)
				return current;
			int ind = indexSubOctree(ijk, s);
			current = current.child[ind];
			if (null == current)
				throw new RuntimeException("Element not found... Aborting");
		}
		return current;
	}
	
	public final int getMaxLevel()
	{
		return nr_levels;
	}
	
	public final boolean walk(RawOEMMProcedure proc)
	{
		logger.debug("walk: init "+proc.getClass().getName());
		int s = gridSize;
		int l = 0;
		int i0 = 0;
		int j0 = 0;
		int k0 = 0;
		int [] posStack = new int[nr_levels+1];
		posStack[l] = 0;
		RawOEMMCell [] octreeStack = new RawOEMMCell[nr_levels+1];
		octreeStack[l] = root;
		proc.init();
		while (true)
		{
			int res = proc.preorder(octreeStack[l], s, i0, j0, k0);
			if (res == RawOEMMProcedure.ABORT)
				return false;
			if (!octreeStack[l].isLeaf && (res == RawOEMMProcedure.OK || res == RawOEMMProcedure.SKIPWALK))
			{
				s >>= 1;
				assert s > 0;
				l++;
				assert l <= nr_levels;
				for (int i = 0; i < 8; i++)
				{
					if (null != octreeStack[l-1].child[i])
					{
						octreeStack[l] = octreeStack[l-1].child[i];
						posStack[l] = i;
						break;
					}
					else
						logger.debug("Empty node skipped: pos="+i);
				}
				if ((posStack[l] & 1) != 0)
					i0 += s;
				if ((posStack[l] & 2) != 0)
					j0 += s;
				if ((posStack[l] & 4) != 0)
					k0 += s;
			}
			else
			{
				while (l > 0)
				{
					posStack[l]++;
					if ((posStack[l] & 1) != 0)
						i0 += s;
					else
					{
						i0 -= s;
						if (posStack[l] == 2 || posStack[l] == 6)
							j0 += s;
						else
						{
							j0 -= s;
							if (posStack[l] == 4)
								k0 += s;
							else
								k0 -= s;
						}
					}
					if (posStack[l] == 8)
					{
						s <<= 1;
						l--;
						logger.debug("Found POSTORDER: "+s+" "+i0+" "+j0+" "+k0);
						res = proc.postorder(octreeStack[l], s, i0, j0, k0);
						logger.debug("  Res; "+res);
					}
					else
					{
						if (null != octreeStack[l-1].child[posStack[l]])
							break;
						else
							logger.debug("Empty node skipped: pos="+posStack[l]);
					}
				}
				if (l == 0)
					break;
				octreeStack[l] = octreeStack[l-1].child[posStack[l]];
			}
		}
		assert i0 == 0;
		assert j0 == 0;
		assert k0 == 0;
		return true;
	}
	
	public void aggregate(int max, int delta)
	{
		// Disabled for now
		if (true) return;
		SumTrianglesProcedure proc = new SumTrianglesProcedure();
		walk(proc);
		logger.info("Nr triangles: "+root.tn);
		proc.printStats();
		AggregateProcedure aproc = new AggregateProcedure(max, delta);
		walk(aproc);
		ClearTrianglesProcedure cproc = new ClearTrianglesProcedure();
		walk(cproc);
		walk(proc);
		logger.info("Nr triangles: "+root.tn);
		proc.printStats();
	}
	
	public void dispatch(String file)
	{
		//  First compute offset in output file for each octant
		ComputeOffsetprocedure proc = new ComputeOffsetprocedure();
		walk(proc);
		
		//  TODO: Output must be buffered
		try
		{
			int [] ijk = new int[9];
			double [] xyz = new double[3];
			RawOEMMCell [] cells = new RawOEMMCell[3];
			FileInputStream fs = new FileInputStream(rawFile);
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
					double2int(xyz, ijk);
					cells[i] = findCell(ijk);
					if (i < 2)
					{
						for (int j = 0; j < 3; j++)
							ijk[6-3*i+j] = ijk[j];
					}
				}
				cells[0].add(fc, ijk);
				if (cells[0] != cells[1])
					cells[1].add(fc, ijk);
				if (cells[2] != cells[0] && cells[2] != cells[1])
					cells[2].add(fc, ijk);
			}
			coordsIn.close();
			//  Adjust block size
			StoreBlockSizeProcedure sproc = new StoreBlockSizeProcedure(fc);
			walk(sproc);
			raf.close();
		}
		catch (FileNotFoundException ex)
		{
			logger.error("File "+rawFile+" not found");
		}
		catch (IOException ex)
		{
			logger.error("I/O error when reading file  "+rawFile);
		}
	}
	
	public BranchGroup bgOctree(boolean onlyLeaves)
	{
		BranchGroup bg=new BranchGroup();
		
		CoordProcedure proc = new CoordProcedure(onlyLeaves, nCells);
		walk(proc);
		QuadArray quad = new QuadArray(24*nCells, QuadArray.COORDINATES);
		quad.setCapability(QuadArray.ALLOW_FORMAT_READ);
		quad.setCapability(QuadArray.ALLOW_COUNT_READ);
		quad.setCapability(QuadArray.ALLOW_COORDINATE_READ);
		quad.setCoordinates(0, proc.coord);
		Appearance quadApp = new Appearance();
		quadApp.setPolygonAttributes(new PolygonAttributes(PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_NONE, 0));
		quadApp.setColoringAttributes(new ColoringAttributes(0,1,0,ColoringAttributes.SHADE_GOURAUD));
		Shape3D shapeQuad=new Shape3D(quad, quadApp);
		shapeQuad.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		bg.addChild(shapeQuad);
		return bg;
	}
	
	private final class SumTrianglesProcedure extends RawOEMMProcedure
	{
		public final int action(RawOEMMCell current, int s, int i0, int j0, int k0, int visit)
		{
			if (visit != POSTORDER)
				return SKIPWALK;
			if (current.isLeaf)
				return OK;
			for (int i = 0; i < 8; i++)
			{
				if (current.child[i] == null)
					continue;
				current.tn += current.child[i].tn;
			}
			return OK;
		}
	}
	private final class ClearTrianglesProcedure extends RawOEMMProcedure
	{
		public final int action(RawOEMMCell current, int s, int i0, int j0, int k0, int visit)
		{
			if (visit != PREORDER)
				return SKIPWALK;
			current.tn = 0;
			return OK;
		}
	}
	
	private final class AggregateProcedure extends RawOEMMProcedure
	{
		private int max_triangles;
		private int delta;
		public AggregateProcedure(int m, int d)
		{
			max_triangles = m;
			delta = d;
		}
		public final int action(RawOEMMCell current, int s, int i0, int j0, int k0, int visit)
		{
			if (visit != PREORDER)
				return SKIPWALK;
			if (current.tn > max_triangles || current.level + delta < nr_levels)
				return OK;
			logger.debug("Aggregate node "+s+" "+i0+" "+j0+" "+k0+"   NrT="+current.tn);
			for (int i = 0; i < 8; i++)
				current.child[i] = null;
			current.isLeaf = true;
			return SKIPCHILD;
		}
	}
	
	//  By convention, offsets are negative when block headers have not been
	//  written onto file.
	private final class ComputeOffsetprocedure extends RawOEMMProcedure
	{
		private long offset = 0L;
		public final int action(RawOEMMCell current, int s, int i0, int j0, int k0, int visit)
		{
			if (visit != LEAF)
				return SKIPWALK;
			current.offset = -offset;
			offset += 36L * (long) current.tn + (long) RawOEMMCell.INTERMEDIATE_HEADER_SIZE;
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
	private final class StoreBlockSizeProcedure extends RawOEMMProcedure
	{
		private FileChannel channel;
		private ByteBuffer buf = ByteBuffer.allocate(8);
		public StoreBlockSizeProcedure(FileChannel fc)
		{
			channel = fc;
		}
		public final int action(RawOEMMCell current, int s, int i0, int j0, int k0, int visit)
		{
			if (visit != LEAF)
				return SKIPWALK;
			long offset = current.offset - 36L * (long) current.tn - 8L;
			buf.putLong(0, current.tn);
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
	
	public void showIntermediateStorage(String file)
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
				long nr = bufIn.readLong();
				logger.debug("block "+iBlock+": triangles="+nr+"  block size= "+blockSize);
				bufIn.skipBytes((int) blockSize);
				pos += 16L + blockSize;
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
	
	private final class CoordProcedure extends RawOEMMProcedure
	{
		public final double [] coord;
		private int index;
		private boolean onlyLeaves;
		public CoordProcedure(boolean b, int n)
		{
			onlyLeaves = b;
			coord = new double[72*n];
		}
		public final int action(RawOEMMCell current, int s, int i0, int j0, int k0, int visit)
		{
			if (visit != PREORDER && visit != LEAF)
				return SKIPWALK;
			if (onlyLeaves && !current.isLeaf)
				return OK;
			int [] ii = { i0, j0, k0 };
			double [] p = new double[3];
			double [] p2 = new double[3];
			int2double(ii, p);
			ii[0] += s;
			int2double(ii, p2);
			double ds = p2[0] - p[0];
			double offset = 0.0;
			for (int i = 0; i < 2; i++)
			{
				//  0xy
				coord[index]   = p[0];
				coord[index+1] = p[1];
				coord[index+2] = p[2]+offset;
				index += 3;
				coord[index]   = p[0]+ds;
				coord[index+1] = p[1];
				coord[index+2] = p[2]+offset;
				index += 3;
				coord[index]   = p[0]+ds;
				coord[index+1] = p[1]+ds;
				coord[index+2] = p[2]+offset;
				index += 3;
				coord[index]   = p[0];
				coord[index+1] = p[1]+ds;
				coord[index+2] = p[2]+offset;
				index += 3;
				//  0xz
				coord[index]   = p[0];
				coord[index+1] = p[1]+offset;
				coord[index+2] = p[2];
				index += 3;
				coord[index]   = p[0];
				coord[index+1] = p[1]+offset;
				coord[index+2] = p[2]+ds;
				index += 3;
				coord[index]   = p[0]+ds;
				coord[index+1] = p[1]+offset;
				coord[index+2] = p[2]+ds;
				index += 3;
				coord[index]   = p[0]+ds;
				coord[index+1] = p[1]+offset;
				coord[index+2] = p[2];
				index += 3;
				//  0yz
				coord[index]   = p[0]+offset;
				coord[index+1] = p[1];
				coord[index+2] = p[2];
				index += 3;
				coord[index]   = p[0]+offset;
				coord[index+1] = p[1]+ds;
				coord[index+2] = p[2];
				index += 3;
				coord[index]   = p[0]+offset;
				coord[index+1] = p[1]+ds;
				coord[index+2] = p[2]+ds;
				index += 3;
				coord[index]   = p[0]+offset;
				coord[index+1] = p[1];
				coord[index+2] = p[2]+ds;
				index += 3;
				offset += ds;
			}
			return OK;
		}
	}
	
}
