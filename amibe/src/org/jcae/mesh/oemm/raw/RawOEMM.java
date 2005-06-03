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

import org.jcae.mesh.oemm.BinaryTree;
import org.apache.log4j.Logger;

/**
 * This class represents an empty raw OEMM.
 * 
 * A raw OEMM is a pointer-based octree, but cells do not contain any data.
 * Only its spatial structure is considered, and it is assumed that the whole
 * tree can reside in memory.  This class defines the octree structure and
 * how to traverse it.
 */
public class RawOEMM
{
	private static Logger logger = Logger.getLogger(RawOEMM.class);	
	
	public static final int MAXLEVEL = 30;
	
	public static final int OEMM_DUMMY = 0;
	public static final int OEMM_CREATED = 1;
	public static final int OEMM_INITIALIZED = 2;
	
	private static int [] neighborOffset = {
		//  Face neighbors
		 1,  0,  0,
		 0,  1,  0,
		 0,  0,  1,
		//     Symmetry
		-1,  0,  0,
		 0, -1,  0,
		 0,  0, -1,
		//  Edge neighbors
		 1,  1,  0,
		 1,  0,  1,
		 0,  1,  1,
		 1, -1,  0,
		 1,  0, -1,
		 0,  1, -1,
		//     Symmetry
		-1, -1,  0,
		-1,  0, -1,
		 0, -1, -1,
		-1,  1,  0,
		-1,  0,  1,
		 0, -1,  1,
		//  Vertex neighbors
		 1,  1,  1,
		 1,  1, -1,
		 1, -1,  1,
		-1,  1,  1,
		//     Symmetry
		-1, -1, -1,
		-1, -1,  1,
		-1,  1, -1,
		 1, -1, -1
	};
	//  Initialize other neighbor-finding related arrays
	private static int [] neighborMask = new int[26];
	private static int [] neighborValue = new int[26];
	static {
		for (int i = 0; i < neighborOffset.length/3; i++)
		{
			neighborMask[i] = 0;
			neighborValue[i] = 0;
			for (int b = 2; b >= 0; b--)
			{
				neighborMask[i] <<= 1;
				neighborValue[i] <<= 1;
				if (neighborOffset[3*i+b] == 1)
				{
					neighborMask[i]++;
					neighborValue[i]++;
				}
				else if (neighborOffset[3*i+b] == -1)
					neighborMask[i]++;
			}
		}
	}
	private static final int SIZE_DELTA = 4;
	private RawNode [] candidates = new RawNode[SIZE_DELTA*SIZE_DELTA];

	private int [] ijk = new int[3];
	
	private RawNode [] head = new RawNode[MAXLEVEL];
	private RawNode [] tail = new RawNode[MAXLEVEL];
	
	public int status;
	private int nCells;
	private String rawFile;
	private int nr_levels;
	private int gridSize;
	private int minCellSize;
	private double [] x0 = new double[4];
	
	/**
	 * Create an empty raw OEMM.
	 */
	public RawOEMM()
	{
		status = OEMM_DUMMY;
		nr_levels = MAXLEVEL;
		gridSize = 1 << MAXLEVEL;
		minCellSize = 1;
		head[0] = new RawNode(gridSize, 0, 0, 0);
		tail[0] = head[0];
		nCells = 1;
		x0[0] = x0[1] = x0[2] = 0.0;
		x0[3] = 1.0;

	}
	
	/**
	 * Create an empty raw OEMM.
	 * @param file   file name containing the triangle soup
	 * @param lmax   maximal level of the tree
	 * @param umin   coordinates of the lower-left corner of mesh bounding box
	 * @param umax   coordinates of the upper-right corner of mesh bounding box
	 */
	public RawOEMM(String file, int lmax, double [] umin, double [] umax)
	{
		status = OEMM_CREATED;
		rawFile = file;
		nr_levels = lmax;
		if (nr_levels > MAXLEVEL)
		{
			logger.error("Max. level too high");
			nr_levels = MAXLEVEL;
		}
		gridSize = 1 << MAXLEVEL;
		minCellSize = 1 << (MAXLEVEL + 1 - nr_levels);
		double deltaX = Math.abs(umin[0] - umax[0]);
		double deltaY = Math.abs(umin[1] - umax[1]);
		double deltaZ = Math.abs(umin[2] - umax[2]);
		deltaX = Math.max(deltaX, deltaY);
		deltaX = Math.max(deltaX, deltaZ);
		for (int i = 0; i < 3; i++)
			x0[i] = umin[i];
		x0[3] = ((double) gridSize) / deltaX;
		head[0] = new RawNode(gridSize, 0, 0, 0);
		tail[0] = head[0];
		nCells = 1;
	}
	
	
	/**
	 * Convert from double coordinates to integer coordinates.
	 * @param p    double coordinates.
	 * @param ijk  integer coordinates.
	 */
	public final void double2int(double [] p, int [] ijk)
	{
		for (int i = 0; i < 3; i++)
			ijk[i] = (int) ((p[i] - x0[i]) * x0[3]);
	}
	
	/**
	 * Convert from integer coordinates to double coordinates.
	 * @param ijk  integer coordinates.
	 * @param p    double coordinates.
	 */
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
	private static final int indexSubOctree(int size, int [] ijk)
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
	
	/**
	 * Return the octant of an OEMM structure containing a given point.
	 *
	 * @param size    the returned octant must have this size.  If this value is 0,
	 *                the deepest octant is returned.
	 * @param ijk     integer coordinates of an interior node
	 * @param create  if set to <code>true</code>, cells are created if needed.  Otherwise
	 *                the desired octant must exist.
	 * @return  the octant of the desired size containing this point.
	 */
	public final RawNode search(int size, int [] ijk, boolean create)
	{
		RawNode current = head[0];
		if (size == 0)
			size = minCellSize;
		return search(current, this, 0, size, ijk, create);
	}
	
	/**
	 * Return the octant of an OEMM structure containing a given point.
	 *
	 * @param fromNode start node
	 * @param oemm     main OEMM structure
	 * @param level    level of start node in oemm
	 * @param size     the returned octant must have this size.  If this value is 0,
	 *                 the deepest octant is returned.
	 * @param ijk      integer coordinates of an interior node
	 * @param create   if set to <code>true</code>, cells are created if needed.  Otherwise
	 *                 the desired octant must exist.
	 * @return  the octant of the desired size containing this point.
	 */
	private static final RawNode search(RawNode fromNode, RawOEMM oemm, int level, int size, int [] ijk, boolean create)
	{
		assert size > 0;
		RawNode current = fromNode;
		int s = current.size;
		while (s > size)
		{
			if (current.isLeaf && !create)
				return current;
			s >>= 1;
			level++;
			assert s > 0;
			int ind = indexSubOctree(s, ijk);
			if (null == current.child[ind])
			{
				if (!create)
					throw new RuntimeException("Element not found... Aborting");
				current.child[ind] = new RawNode(s, ijk);
				current.child[ind].parent = current;
				if (oemm.head[level] != null)
				{
					oemm.tail[level].next = current.child[ind];
					oemm.tail[level] = oemm.tail[level].next;
				}
				else
				{
					oemm.head[level] = current.child[ind];
					oemm.tail[level] = oemm.head[level];
				}
				current.isLeaf = false;
				oemm.nCells++;
			}
			current = current.child[ind];
		}
		return current;
	}
	
	/**
	 * Return the octant of an OEMM structure containing a given point
	 *        with a size at least equal to those of start node.
	 *
	 * @param fromNode start node
	 * @param ijk      integer coordinates of an interior node
	 * @return  the octant of the desired size containing this point.
	 */
	private RawNode search(RawNode fromNode, int [] ijk)
	{
		int i1 = ijk[0];
		if (i1 < 0 || i1 > gridSize)
			return null;
		int j1 = ijk[1];
		if (j1 < 0 || j1 > gridSize)
			return null;
		int k1 = ijk[2];
		if (k1 < 0 || k1 > gridSize)
			return null;
		//  Neighbor octant is within OEMM bounds
		//  First climb tree until an octant enclosing this
		//  point is encountered.
		RawNode ret = fromNode;
		int i2, j2, k2;
		do
		{
			if (null == ret.parent)
				break;
			ret = ret.parent;
			int mask = ~(ret.size - 1);
			i2 = i1 & mask;
			j2 = j1 & mask;
			k2 = k1 & mask;
		}
		while (i2 != ret.i0 || j2 != ret.j0 || k2 != ret.k0);
		//  Now find the deepest matching octant.
		int s = ret.size;
		while (s > fromNode.size && !ret.isLeaf)
		{
			s >>= 1;
			assert s > 0;
			int ind = indexSubOctree(s, ijk);
			if (null == ret.child[ind])
				break;
			ret = ret.child[ind];
		}
		return ret;
	}
	
	public void printInfos()
	{
		logger.info("Number of nodes: "+nCells);
		logger.info("Max level: "+nr_levels);
	}
	
	public final String getFileName()
	{
		return rawFile;
	}
	
	/**
	 * Traverse the whole OEMM structure.
	 *
	 * @param proc    the procedure called on each octant.
	 * @return  <code>true</code> if the whole structure has been traversed,
	 *          <code>false</code> if traversal aborted.
	 */
	public final boolean walk(TraversalProcedure proc)
	{
		logger.debug("walk: init "+proc.getClass().getName());
		if (status < OEMM_INITIALIZED)
		{
			logger.error("The raw OEMM must be filled in first!... Aborting");
			return false;
		}
		int s = gridSize;
		int l = 0;
		int i0 = 0;
		int j0 = 0;
		int k0 = 0;
		int [] posStack = new int[nr_levels+1];
		posStack[l] = 0;
		RawNode [] octreeStack = new RawNode[nr_levels+1];
		octreeStack[l] = head[0];
		proc.init();
		while (true)
		{
			int res = proc.preorder(octreeStack[l]);
			if (res == TraversalProcedure.ABORT)
				return false;
			if (!octreeStack[l].isLeaf && (res == TraversalProcedure.OK || res == TraversalProcedure.SKIPWALK))
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
						res = proc.postorder(octreeStack[l]);
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
	
	public void aggregate(int max)
	{
		SumTrianglesProcedure st_proc = new SumTrianglesProcedure();
		walk(st_proc);
		logger.debug("Nr triangles: "+head[0].tn);
		st_proc.printStats();
		for (int level = nr_levels - 1; level > 0; level--)
		{
			int merged = 0;
			logger.debug(" Checking neighbors at level "+level);
			for (RawNode current = head[level]; current != null; current = current.next)
			{
				if (current.isLeaf || current.tn > max)
					continue;
				//  This node is not a leaf and its children
				//  can be merged if neighbors have a difference
				//  level lower than SIZE_DELTA
				if (current.size <= SIZE_DELTA * minCellSize ||  checkLevelNeighbors(current))
				//if (checkLevelNeighbors(current))
				{
					//  Note: Do not yet delete children
					current.isLeaf = true;
					merged++;
				}
			}
			logger.debug(" Merged nodes: "+merged);
		}
		//  Disoplay stats again to see if they change
		walk(st_proc);
		st_proc.printStats();
		logger.debug("Nr triangles: "+head[0].tn);
	}
	
	private boolean checkLevelNeighbors(RawNode current)
	{
		int minSize = current.size / SIZE_DELTA;
		int pos = 0;
		for (int i = 0; i < neighborOffset.length/3; i++)
		{
			ijk[0] = current.i0 + neighborOffset[3*i]   * current.size;
			ijk[1] = current.j0 + neighborOffset[3*i+1] * current.size;
			ijk[2] = current.k0 + neighborOffset[3*i+2] * current.size;
			RawNode n = search(current, ijk);
			if (n == null || n.isLeaf || n.size > current.size)
				continue;
			//  Check if children are not too deep in the tree.
			pos = 0;
			candidates[pos] = n;
			while (pos >= 0)
			{
				RawNode c = candidates[pos];
				pos--;
				if (c.size < minSize || SIZE_DELTA <= 1)
					return false;
				if (c.isLeaf)
					continue;
				for (int ind = 0; ind < 8; ind++)
				{
					if (c.child[ind] != null && (ind & neighborMask[i]) == neighborValue[i])
					{
						pos++;
						candidates[pos] = c.child[ind];
					}
				}
			}
		}
		return true;
	}
	
	public double [] getCoords(boolean onlyLeaves)
	{
		CoordProcedure proc = new CoordProcedure(onlyLeaves, nCells);
		walk(proc);
		return proc.coord;
	}
	
	private final class SumTrianglesProcedure extends TraversalProcedure
	{
		public final int action(RawNode current, int visit)
		{
			if (current.isLeaf)
				return OK;
			if (visit != POSTORDER)
			{
				current.tn = 0;
				return SKIPWALK;
			}
			for (int i = 0; i < 8; i++)
			{
				if (current.child[i] == null)
					continue;
				current.tn += current.child[i].tn;
			}
			return OK;
		}
	}
	
	public final class CoordProcedure extends TraversalProcedure
	{
		public final double [] coord;
		private int index;
		private boolean onlyLeaves;
		public CoordProcedure(boolean b, int n)
		{
			onlyLeaves = b;
			coord = new double[72*n];
		}
		public final int action(RawNode current, int visit)
		{
			if (visit != PREORDER && visit != LEAF)
				return SKIPWALK;
			if (onlyLeaves && !current.isLeaf)
				return OK;
			int [] ii = { current.i0, current.j0, current.k0 };
			double [] p = new double[3];
			double [] p2 = new double[3];
			int2double(ii, p);
			ii[0] += current.size;
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
