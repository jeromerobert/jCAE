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

import org.apache.log4j.Logger;

/**
 * Helper class to merge neighbor octree cells.
 * The sole purpose of this class is to provide an {@link #aggregate}
 * method, which is called to merge neighbor cells when they contain
 * few triangles.
 */
public class RawOEMM extends OEMM
{
	private static Logger logger = Logger.getLogger(RawOEMM.class);	
	
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
					neighborMask[i]++;
				else if (neighborOffset[3*i+b] == -1)
				{
					neighborMask[i]++;
					neighborValue[i]++;
				}
			}
		}
	}
	private static final int SIZE_DELTA = 4;
	private OEMMNode [] candidates = new OEMMNode[SIZE_DELTA*SIZE_DELTA];

	// Array of doubly-linked lists of octree cellsm needed by aggregate()
	private transient OEMMNode [] head = new OEMMNode[MAXLEVEL];
	private transient OEMMNode [] tail = new OEMMNode[MAXLEVEL];

	/**
	 * Create an empty raw OEMM.
	 * @param lmax   maximal level of the tree
	 * @param umin   coordinates of the lower-left corner of mesh bounding box
	 * @param umax   coordinates of the upper-right corner of mesh bounding box
	 */
	public RawOEMM(int lmax, double [] umin, double [] umax)
	{
		super("(null)");
		double [] bbox = new double[6];
		for (int i = 0; i < 3; i++)
		{
			bbox[i] = umin[i];
			bbox[i+3] = umax[i];
		}
		reset(bbox);
		head = new OEMMNode[MAXLEVEL];
		tail = new OEMMNode[MAXLEVEL];
		//  Adjust status, nr_levels and x0
		status = OEMM_CREATED;
		nr_levels = lmax;
		if (nr_levels > MAXLEVEL)
		{
			logger.error("Max. level too high");
			nr_levels = MAXLEVEL;
		}
		else if (nr_levels <= 1)
			nr_levels = 1;
	}
	
	// Called by OEMM.search()
	protected final void createRootNode(OEMMNode node)
	{
		super.createRootNode(node);
		head = new OEMMNode[MAXLEVEL];
		tail = new OEMMNode[MAXLEVEL];
		head[0] = root;
		tail[0] = head[0];
	}
	
	// Called by OEMM.search()
	// Add the inserted node into doubly-linked list for current level.
	protected final void postInsertNode(OEMMNode node, int level)
	{
		super.postInsertNode(node, level);
		if (head[level] != null)
		{
			tail[level].extra = node;
			tail[level] = (OEMMNode) tail[level].extra;
		}
		else
		{
			head[level] = node;
			tail[level] = head[level];
		}
	}

	public void aggregate(int max)
	{
		int minCellSize = 1 << (MAXLEVEL + 1 - nr_levels);
		SumTrianglesProcedure st_proc = new SumTrianglesProcedure();
		walk(st_proc);
		int total = 0;
		for (int level = nr_levels - 1; level >= 0; level--)
		{
			int merged = 0;
			logger.debug(" Checking neighbors at level "+level);
			for (OEMMNode current = head[level]; current != null; current = (OEMMNode) current.extra)
			{
				if (current.isLeaf || current.tn > max)
					continue;
				//  This node is not a leaf and its children
				//  can be merged if neighbors have a difference
				//  level lower than SIZE_DELTA
				if (current.size <= SIZE_DELTA * minCellSize ||  checkLevelNeighbors(current))
				{
					for (int ind = 0; ind < 8; ind++)
						if (current.child[ind] != null)
							merged++;
					mergeChildren(current);
				}
			}
			logger.debug(" Merged octree cells: "+merged);
			total += merged;
		}
		logger.info("Merged octree cells: "+total);
	}
	
	/**
	 * Return the octant of an OEMM structure containing a given point
	 * with a size at least equal to those of start node.
	 *
	 * @param fromNode start node
	 * @param ijk      integer coordinates of an interior node
	 * @return  the octant of the desired size containing this point.
	 */
	private OEMMNode searchFromNode(OEMMNode fromNode, int [] ijk)
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
		OEMMNode ret = fromNode;
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
	
	private boolean checkLevelNeighbors(OEMMNode current)
	{
		int minSize = current.size / SIZE_DELTA;
		int pos = 0;
		logger.debug("Checking neighbors of "+current);
		int [] ijk = new int[3];
		for (int i = 0; i < neighborOffset.length/3; i++)
		{
			ijk[0] = current.i0 + neighborOffset[3*i]   * current.size;
			ijk[1] = current.j0 + neighborOffset[3*i+1] * current.size;
			ijk[2] = current.k0 + neighborOffset[3*i+2] * current.size;
			OEMMNode n = searchFromNode(current, ijk);
			if (n == null || n.isLeaf || n.size > current.size)
				continue;
			assert n.size == current.size;
			logger.debug("Node "+n+" contains "+Integer.toHexString(ijk[0])+" "+Integer.toHexString(ijk[1])+" " +Integer.toHexString(ijk[2]));
			//  Check if children are not too deep in the tree.
			pos = 0;
			candidates[pos] = n;
			while (pos >= 0)
			{
				OEMMNode c = candidates[pos];
				pos--;
				if (c.tn == 0)
					continue;
				if (c.isLeaf)
				{
					if (c.size <= minSize || SIZE_DELTA <= 1)
					{
						logger.debug("Found too deep neighbor: "+c+"    "+c.tn);
						return false;
					}
					continue;
				}
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
	
	private static class SumTrianglesProcedure extends TraversalProcedure
	{
		public final int action(OEMMNode current, int octant, int visit)
		{
			if (current.isLeaf)
				return OK;
			if (visit != POSTORDER)
			{
				current.tn = 0;
				return SKIPWALK;
			}
			for (int i = 0; i < 8; i++)
				if (current.child[i] != null)
					current.tn += current.child[i].tn;
			return OK;
		}
	}
	
}
