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

import java.util.ArrayList;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 * Merge adjacent octree nodes.
 * Children are merged if these two conditions are met: the total number of
 * triangles in merged nodes does not exceed a given threshold, and levels of
 * adjacent nodes do not differ more than MAX_DELTA_LEVEL.
 * This process is an optimization to have fewer octree nodes.  
*/
public class Aggregate
{
	private static Logger logger = Logger.getLogger(Aggregate.class);	
	
	// Maximum level difference between adjacent cells.
	// With a difference of N, a node has at most
	// (6*(2^N)*(2^N) + 12*(2^N) + 8)
	// = 6*(2^N+1)*(2^N+1)+2 neighbors.
	// In her paper, Cignoni takes N=3, but we consider N=2
	// instead so that upper bound is less than 256 and
	// neighbor indices can be stored in byte arrays.
	// In practice, N=3 or 4 should also work.
	private static final int MAX_DELTA_LEVEL = 2;

	private static final int [] neighborOffset = {
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
	//  Adjacent nodes can be connected by faces (6), edges (12)
	//  or vertices (8).
	private static final int [] neighborMask = new int[26];
	private static final int [] neighborValue = new int[26];
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

	/**
	 * Merge nodes.
	 *
	 * @param oemm  OEMM instance
	 * @param max   maximal number of triangles in merged cells
	 * @return total number of merged nodes
	 */
	public final static int compute(OEMM oemm, int max)
	{
		if (MAX_DELTA_LEVEL <= 0)
			return 0;

		// Array of linked lists of non-leaf octree cells
		ArrayList [] nonLeaves = new ArrayList[OEMM.MAXLEVEL];
		// A bottom-up traversal is the most efficient way to
		// merge nodes when both conditions above are met.
		// We first walk through the whole tree to compute total
		// number of triangles in non-leaf nodes, and for each
		// depth a linked list of non-leaf nodes
		PreProcessOEMM st_proc = new PreProcessOEMM(nonLeaves);
		oemm.walk(st_proc);

		// If a cell is smaller than minCellSize() << MAX_DELTA_LEVEL
		// depth of adjacent nodes can not differ more than
		// MAX_DELTA_LEVEL, and checkLevelNeighbors() can safely
		// be skipped.  The cellSizeByHeight() method ensures that
		// this variable does not overflow.
		int minSize = oemm.cellSizeByHeight(MAX_DELTA_LEVEL+1);
		// checkLevelNeighbors() needs a stack of OEMMNode instances,
		// allocate it here.
		OEMMNode [] nodeStack = new OEMMNode[4*OEMM.MAXLEVEL];

		int ret = 0;
		for (int level = OEMM.MAXLEVEL - 1; level >= 0; level--)
		{
			if (nonLeaves[level] == null)
				continue;
			int merged = 0;
			logger.debug(" Checking neighbors at level "+level);
			for (Iterator it = nonLeaves[level].iterator(); it.hasNext(); )
			{
				OEMMNode current = (OEMMNode) it.next();
				assert !current.isLeaf;
				if (current.tn > max)
					continue;
				//  This node is not a leaf and its children
				//  can be merged if neighbors have a difference
				//  level lower than MAX_DELTA_LEVEL
				if (current.size < minSize || checkLevelNeighbors(oemm, current, nodeStack))
				{
					for (int ind = 0; ind < 8; ind++)
						if (current.child[ind] != null)
							merged++;
					oemm.mergeChildren(current);
				}
			}
			nonLeaves[level] = null;
			logger.debug(" Merged octree cells: "+merged);
			ret += merged;
		}
		logger.info("Merged octree cells: "+ret);
		return ret;
	}
	
	private static final boolean checkLevelNeighbors(OEMM oemm, OEMMNode current, OEMMNode [] nodeStack)
	{
		// If an adjacent node has a size lower than minSize, children
		// nodes must not be merged
		int minSize = current.size >> MAX_DELTA_LEVEL;
		logger.debug("Checking neighbors of "+current);
		int [] ijk = new int[3];
		for (int i = 0; i < neighborOffset.length/3; i++)
		{
			ijk[0] = current.i0 + neighborOffset[3*i]   * current.size;
			ijk[1] = current.j0 + neighborOffset[3*i+1] * current.size;
			ijk[2] = current.k0 + neighborOffset[3*i+2] * current.size;
			OEMMNode n = oemm.searchFromNode(current, ijk);
			if (n == null || n.isLeaf || n.size > current.size)
				continue;
			assert n.size == current.size;
			logger.debug("Node "+n+" contains "+Integer.toHexString(ijk[0])+" "+Integer.toHexString(ijk[1])+" " +Integer.toHexString(ijk[2]));
			//  We found the adjacent node with same size,
			//  and have now to find all its children which are
			//  adjacent to current node.
			int pos = 0;
			nodeStack[pos] = n;
			while (pos >= 0)
			{
				OEMMNode c = nodeStack[pos];
				pos--;
				if (c.tn == 0)
					continue;
				if (c.isLeaf)
				{
					if (c.size < minSize)
					{
						logger.debug("Found too deep neighbor: "+c+"    "+c.tn);
						return false;
					}
					continue;
				}
				for (int ind = 0; ind < 8; ind++)
				{
					// Only push children on the "right"
					// side, at most 4 nodes are added.
					if (c.child[ind] != null && (ind & neighborMask[i]) == neighborValue[i])
					{
						pos++;
						nodeStack[pos] = c.child[ind];
					}
				}
			}
		}
		return true;
	}
	
	private static final class PreProcessOEMM extends TraversalProcedure
	{
		private int depth = 0;
		private int maxDepth = 0;
		private final ArrayList [] nonLeaves;
		public PreProcessOEMM(ArrayList [] a)
		{
			nonLeaves = a;
		}
		public final int action(OEMM oemm, OEMMNode current, int octant, int visit)
		{
			if (visit == PREORDER)
			{
				current.tn = 0;
				if (nonLeaves[depth] == null)
					nonLeaves[depth] = new ArrayList();
				nonLeaves[depth].add(current);
				depth++;
				if (depth > maxDepth)
					maxDepth = depth;
			}
			else if (visit == POSTORDER)
			{
				depth--;
				for (int i = 0; i < 8; i++)
					if (current.child[i] != null)
						current.tn += current.child[i].tn;
			}
			return OK;
		}
	}
	
}
