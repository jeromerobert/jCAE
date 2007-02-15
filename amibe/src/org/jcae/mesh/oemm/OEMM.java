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

import java.io.Serializable;
import org.apache.log4j.Logger;

/**
 * This class represents an empty OEMM.
 * 
 * A raw OEMM is a pointer-based octree, but cells do not contain any data.
 * Only its spatial structure is considered, and it is assumed that the whole
 * tree can reside in memory.  This class defines the octree structure and
 * how to traverse it.
 *
 * References:
 * External Memory Management and Simplification of Huge Meshes
 * P. Cignoni, C. Montani, C. Rocchini, R. Scopigno
 * http://vcg.isti.cnr.it/publications/papers/oemm_tvcg.pdf
 */
public class OEMM implements Serializable
{
	private static final long serialVersionUID = -1375654681644527771L;

	private static Logger logger = Logger.getLogger(OEMM.class);	
	
	public static final int MAXLEVEL = 30;
	protected static final int gridSize = 1 << MAXLEVEL;
	
	public static final int OEMM_DUMMY = 0;
	public static final int OEMM_CREATED = 1;
	public static final int OEMM_INITIALIZED = 2;
	
	protected transient String topDir;
	public int status;
	private transient int nr_leaves = 0;
	private transient int nr_cells = 0;
	private transient int depth = 0;
	// Double-to-integer conversion
	public double [] x0 = new double[4];
	public double xdelta;
	
	protected transient OEMMNode root = null;
	
	// Array of leaves
	public transient OEMMNode [] leaves;
	
	/**
	 * Create an empty OEMM.
	 */
	public OEMM(String dir)
	{
		topDir = dir;
		status = OEMM_DUMMY;
	}
	
	/**
	 * Create an empty OEMM with a given depth.
	 */
	public OEMM(int l)
	{
		depth = l;
		if (depth > MAXLEVEL)
		{
			logger.error("Max. level too high");
			depth = MAXLEVEL;
		}
		else if (depth < 1)
		{
			logger.error("Max. level too low");
			depth = 1;
		}
	}
	
	public final void clearNodes()
	{
		nr_cells = 0;
		nr_leaves = 0;
		leaves = null;
		root = null;
	}

	protected final void reset(double [] bbox)
	{
		clearNodes();
		xdelta = Double.MIN_VALUE;
		for (int i = 0; i < 3; i++)
		{
			double delta = bbox[i+3] - bbox[i];
			if (delta > xdelta)
				xdelta = delta;
			x0[i] = bbox[i];
		}
		// Enlarge bounding box by 1% to avoid rounding errors
		for (int i = 0; i < 3; i++)
			x0[i] -= 0.005*xdelta;
		xdelta *= 1.01;
		x0[3] = ((double) gridSize) / xdelta;
		logger.debug("Lower left corner : ("+x0[0]+", "+x0[1]+", "+x0[2]+")   Bounding box length: "+xdelta);
	}

	public final String getFileName()
	{
		return topDir+java.io.File.separator+"oemm";
	}

	public final void setTopDir(String dir)
	{
		topDir = dir;
	}
	
	public final int getNumberOfLeaves()
	{
		return nr_leaves;
	}

	protected final int minCellSize()
	{
		return (1 << (MAXLEVEL + 1 - depth));
	}

	public final void printInfos()
	{
		logger.info("Number of leaves: "+nr_leaves);
		logger.info("Number of octants: "+nr_cells);
		logger.info("Depth: "+depth);
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
			logger.error("The OEMM must be filled in first!... Aborting");
			System.exit(9);
		}
		int s = gridSize;
		int l = 0;
		int i0 = 0;
		int j0 = 0;
		int k0 = 0;
		int [] posStack = new int[depth];
		posStack[l] = 0;
		OEMMNode [] octreeStack = new OEMMNode[depth];
		octreeStack[l] = root;
		proc.init(this);
		while (true)
		{
			int res = proc.preorder(this, octreeStack[l], posStack[l]);
			if (res == TraversalProcedure.ABORT)
				return false;
			if (!octreeStack[l].isLeaf && (res == TraversalProcedure.OK || res == TraversalProcedure.SKIPWALK))
			{
				s >>= 1;
				assert s > 0;
				l++;
				assert l < depth;
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
						logger.debug("Found POSTORDER: "+Integer.toHexString(s)+" "+Integer.toHexString(i0)+" "+Integer.toHexString(j0)+" "+Integer.toHexString(k0));
						res = proc.postorder(this, octreeStack[l], posStack[l]);
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
		proc.finish(this);
		return true;
	}
	
	/*         k=0          k=1
	 *      .-------.    .-------.
	 *      | 2 | 3 |    | 6 | 7 |
	 *   j  |---+---|    |---+---|
	 *      | 0 | 1 |    | 4 | 5 |
	 *      `-------'    `-------'
	 *          i          
	 */
	protected static final int indexSubOctree(int size, int [] ijk)
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
	 * Build an octant containing a given point if it does not already exist.
	 *
	 * @param ijk     integer coordinates of an interior node
	 * @return  the octant of the smallest size containing this point.
	 *          It is created if it does not exist.
	 */
	public final OEMMNode build(int [] ijk)
	{
		return search(minCellSize(), ijk, true, null);
	}
	
	/**
	 * Insert an octant into the tree structure if it does not already exist.
	 *
	 * @param current     node being inserted.
	 */
	public final void insert(OEMMNode current)
	{
		int [] ijk = new int[3];
		ijk[0] = current.i0;
		ijk[1] = current.j0;
		ijk[2] = current.k0;
		search(current.size, ijk, true, current);
	}
	
	/**
	 * Return the octant of an OEMM structure containing a given point.
	 *
	 * @param ijk     integer coordinates of an interior node
	 * @return  the octant of the smallest size containing this point.
	 */
	public final OEMMNode search(int [] ijk)
	{
		return search(0, ijk, false, null);
	}
	
	/**
	 * Return the octant of an OEMM structure containing a given point.
	 *
	 * @param size     the returned octant must have this size.  If this value is 0,
	 *                 the deepest octant is returned.
	 * @param ijk      integer coordinates of an interior node
	 * @param create   if set to <code>true</code>, cells are created if needed.  Otherwise
	 *                 the desired octant must exist.
	 * @return  the octant of the desired size containing this point.
	 */
	private final OEMMNode search(int size, int [] ijk, boolean create, OEMMNode node)
	{
		if (root == null)
		{
			if (!create)
				throw new RuntimeException("Element not found... Aborting ");
			createRootNode(node);
			if (size == gridSize)
			{
				root.isLeaf = true;
				nr_leaves++;
				if (depth == 0)
					depth++;
			}
		}
		OEMMNode current = root;
		int level = 0;
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
					throw new RuntimeException("Element not found... Aborting "+current+" "+Integer.toHexString(s)+" "+ind);
				if (level >= depth)
					depth = level + 1;
				if (depth > MAXLEVEL)
					throw new RuntimeException("Too many octree levels... Aborting");
				if (s == size && node != null)
					current.child[ind] = node;
				else
					current.child[ind] = new OEMMNode(s, ijk);
				current.child[ind].parent = current;
				postInsertNode(current.child[ind], level);
				current.isLeaf = false;
				nr_cells++;
				if (s == size)
					nr_leaves++;
			}
			current = current.child[ind];
		}
		return current;
	}

	protected void createRootNode(OEMMNode node)
	{
		if (node != null && node.size == gridSize)
		{
			// This happens only when OEMM has only one leaf
			// and is read from disk, root has to be set to
			// this leaf.
			root = node;
		}
		else
			root = new OEMMNode(gridSize, 0, 0, 0);
		nr_cells++;
	}

	protected void postInsertNode(OEMMNode node, int level)
	{
	}
	
	protected final void mergeChildren(OEMMNode node)
	{
		assert !node.isLeaf;
		for (int ind = 0; ind < 8; ind++)
		{
			if (node.child[ind] != null)
			{
				assert node.child[ind].isLeaf;
				node.child[ind] = null;
				nr_leaves--;
				nr_cells--;
			}
		}
		node.isLeaf = true;
		nr_leaves++;
	}

	public double [] getCoords(boolean onlyLeaves)
	{
		CoordProcedure proc = new CoordProcedure(onlyLeaves, nr_cells, nr_leaves);
		walk(proc);
		return proc.coord;
	}
	
	public final class CoordProcedure extends TraversalProcedure
	{
		public final double [] coord;
		private int index;
		private boolean onlyLeaves;
		public CoordProcedure(boolean b, int nC, int nL)
		{
			onlyLeaves = b;
			if (onlyLeaves)
				coord = new double[72*nL];
			else
				coord = new double[72*nC];
		}
		public final int action(OEMM oemm, OEMMNode current, int octant, int visit)
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
