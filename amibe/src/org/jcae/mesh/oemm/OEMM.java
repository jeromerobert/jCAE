/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005, by EADS CRC
    Copyright (C) 2007,2008, by EADS France

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
import gnu.trove.TIntArrayList;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class represents an empty OEMM.
 * 
 * An OEMM is a pointer-based octree, but cells do not contain any data.
 * Only its spatial structure is considered, and it is assumed that the whole
 * tree can reside in memory.  This class defines the octree structure and
 * how to traverse it.
 *
 * References:
 * <a href="http://vcg.isti.cnr.it/publications/papers/oemm_tvcg.pdf">External Memory Management and Simplification of Huge Meshes</a>, by
 * P. Cignoni, C. Montani, C. Rocchini and R. Scopigno.
 */
public class OEMM implements Serializable
{
	private static final long serialVersionUID = -8244900407797088903L;

	private static Logger logger=Logger.getLogger(OEMM.class.getName());	
	
	/**
	 * Maximal tree depth.
	 */
	public static final int MAXLEVEL = 30;

	/**
	 * Root cell size.
	 */
	private static final int gridSize = 1 << MAXLEVEL;
	private static final double dGridSize = gridSize;
	
	/**
	 * Top-level directory.
	 */
	private transient String topDir;

	/**
	 * Number of leaves.
	 */
	private transient int nr_leaves = 0;

	/**
	 * Total number of cells.
	 */
	private transient int nr_cells = 0;

	/**
	 * Tree depth.
	 */
	private transient int depth = 0;
	
	/**
	 * Double/integer conversion.  First three values contain coordinates
	 * of bottom-left corner, and the last one is a scale factor.
	 * Any coordinate can then been converted from double to integer by this
	 * formula:
	 * <pre>
	 *  I[i] = (D[i] - x0[i]) * x0[3];
	 * </pre>
	 * and inverse conversion is
	 * <pre>
	 *  D[i] = x0[i] + I[i] / x0[3];
	 * </pre>
	 */
	public double [] x0 = new double[4];

	/**
	 * Root cell.
	 */
	protected transient Node root = null;
	
	/**
	 * Array of leaves.
	 */
	public transient Node [] leaves;
	
	/**
	 * Create an empty OEMM.
	 */
	public OEMM(String dir)
	{
		topDir = dir;
	}
	
	/**
	 * Create an empty OEMM with a given depth.
	 */
	public OEMM(int l)
	{
		depth = l;
		if (depth > MAXLEVEL)
		{
			logger.severe("Max. level too high");
			depth = MAXLEVEL;
		}
		else if (depth < 1)
		{
			logger.severe("Max. level too low");
			depth = 1;
		}
	}
	
	/**
	 * This class represents octants of an OEMM.  Octants can either be leaves
	 * or internal nodes.
	 */
	public static class Node implements Serializable
	{
		private static final long serialVersionUID = 7241788498142227257L;

		/**
		 * Integer coordinates of lower-left corner.
		 */
		public int i0, j0, k0;

		/**
		 * Cell size.  It is equal to (1 &lt;&lt; (OEMM.MAXLEVEL - depth))
		 */
		public int size;

		/**
		 * Total number of triangles found in this node and its children.
		 */
		public int tn = 0;

		/**
		 * Number of vertices found in this node and its children.
		 */
		public int vn = 0;

		/**
		 * Array of 8 children nodes.
		 */
		public transient Node[] child = new Node[8];

		/**
		 * Parent node.
		 */
		//  TODO: The parent pointer can be replaced by a stack
		//        if more room is needed.
		public transient Node parent;

		/**
		 * Flag set when this node a leaf.
		 */
		public transient boolean isLeaf = true;

		/**
		 * File containing vertices and triangles.
		 */
		public transient String file;
		private String [] pathComponents;

		/**
		 * Counter.  This is a temporary variable used by some algorithms.
		 */
		public transient long counter = 0L;
		
		/**
		 * Leaf index in {@link OEMM#leaves}.
		 */
		public int leafIndex = -1;

		/**
		 * First index of all vertices found in this node and its children.
		 */
		public int minIndex = 0;

		/**
		 * Maximal index allowed for vertices found in this node and its children.
		 */
		public int maxIndex = 0;

		/**
		 * List of adjacent leaves.
		 */
		public transient TIntArrayList adjLeaves;
		
		/**
		 * Creates a new leaf.
		 * @param s   cell size
		 * @param i0  1st coordinate of its lower-left corner
		 * @param j0  2nd coordinate of its lower-left corner
		 * @param k0  3rd coordinate of its lower-left corner
		 */
		public Node(int s, int i0, int j0, int k0)
		{
			size = s;
			this.i0 = i0;
			this.j0 = j0;
			this.k0 = k0;
		}
		
		/**
		 * Creates a new leaf.
		 * @param s   cell size
		 * @param ijk  coordinates of an interior point
		 */
		public Node(int s, int [] ijk)
		{
			size = s;
			int mask = ~(s - 1);
			i0 = ijk[0] & mask;
			j0 = ijk[1] & mask;
			k0 = ijk[2] & mask;
		}
		
		private void readObject(java.io.ObjectInputStream s)
		        throws java.io.IOException, ClassNotFoundException
		{
			s.defaultReadObject();
			int[] adj = (int[]) s.readObject();
			adjLeaves = new TIntArrayList(adj);
			child = new Node[8];
			isLeaf = true;
			updateFilename();
		}

		private void writeObject(java.io.ObjectOutputStream s)
		        throws java.io.IOException
		{
			s.defaultWriteObject();
			s.writeObject(adjLeaves.toNativeArray());
		}

		public void setPathComponents(ArrayList<String> dir, int octant)
		{
			if (dir != null && dir.size() > 0)
			{
				pathComponents = new String[dir.size()+1];
				for (int i = 0; i < pathComponents.length - 1; i++)
					pathComponents[i] = dir.get(i);
			}
			else
				pathComponents = new String[1];
			pathComponents[pathComponents.length - 1] = ""+octant;
			updateFilename();
		}

		private void updateFilename()
		{
			if (pathComponents == null || pathComponents.length == 0)
				return;
			StringBuilder buf = new StringBuilder();
			for (String p: pathComponents)
				buf.append(p+java.io.File.separator);
			file = buf.substring(0, buf.length()-1);
		}

		@Override
		public String toString()
		{
			return " IJK "+Integer.toHexString(i0)+" "+Integer.toHexString(j0)+" "+Integer.toHexString(k0)+
			       " Size=" +Integer.toHexString(size)+
			       " Leaf?: "+isLeaf+
			       " NrV="+vn+
			       " NrT="+tn+
			       " index="+leafIndex+
			       " min="+minIndex+
			       " max="+maxIndex+
			       " file="+file+
			       " adj="+adjLeaves;
		}
		
		/**
		 * This methods calculate mexIndex of OEMM.Node. There is probably 
		 * back with setting of maxIndex value because maxIndex < minIndex.
		 * It is sufficient 
		 * compute getMaxIndex(n) := minIndex(n) + |maxIndex(n) - minIndex(n)|  
		 * 
		 * @return correct value of maxIndex
		 */
		public int getMaxIndex()
		{
			return minIndex + Math.abs(maxIndex - minIndex);
		}
	}

	/**
	 * Remove all cells from a tree.
	 */
	public final void clearNodes()
	{
		nr_cells = 0;
		nr_leaves = 0;
		leaves = null;
		root = null;
	}

	/**
	 * Sets object bounding box.  This method computes {@link #x0}.
	 *
	 * @param bbox  bounding box
	 */
	public final void setBoundingBox(double [] bbox)
	{
		clearNodes();
		double dmax = Double.MIN_VALUE;
		for (int i = 0; i < 3; i++)
		{
			double delta = bbox[i+3] - bbox[i];
			if (delta > dmax)
				dmax = delta;
			x0[i] = bbox[i];
		}
		// Enlarge bounding box by 1% to avoid rounding errors
		for (int i = 0; i < 3; i++)
			x0[i] -= 0.005*dmax;
		dmax *= 1.01;
		x0[3] = dGridSize / dmax;
		logger.fine("Lower left corner : ("+x0[0]+", "+x0[1]+", "+x0[2]+")   Bounding box length: "+dmax);
	}

	/**
	 * Checks whether a bounding box lies within current OEMM.
	 *
	 * @param bbox  bounding box
	 * @return <code>true</code> if bounding box lies within current OEMM,
	 *   <code>false</code> otherwise.
	 */
	public final boolean checkBoundingBox(double [] bbox)
	{
		double xdelta = dGridSize / x0[3];
		return bbox[0] >= x0[0] && bbox[3] <= x0[0]+xdelta &&
		       bbox[1] >= x0[1] && bbox[4] <= x0[1]+xdelta &&
		       bbox[2] >= x0[2] && bbox[5] <= x0[2]+xdelta;
	}

	/**
	 * Returns top-level directory.
	 *
	 * @return top-level directory
	 */
	public final String getDirectory()
	{
		return topDir;
	}
	
	/**
	 * Sets top-level directory.
	 *
	 * @param dir  top-level directory
	 */
	public final void setDirectory(String dir)
	{
		topDir = dir;
	}
	
	/**
	 * Returns file name containing {@link OEMM} data structure.
	 *
	 * @return file name
	 */
	public final String getFileName()
	{
		return topDir+java.io.File.separator+"oemm";
	}

	/**
	 * Returns number of leaves.
	 *
	 * @return number of leaves
	 */
	public final int getNumberOfLeaves()
	{
		return nr_leaves;
	}

	/**
	 * Returns size of deepest cell.
	 *
	 * @return size of deepest cell
	 */
	protected final int minCellSize()
	{
		return (1 << (MAXLEVEL + 1 - depth));
	}

	/**
	 * Returns size of cells at a given height.  By convention, height is set
	 * to 0 for bottom leaves.
	 *
	 * @param h  cell height
	 * @return size of cells at given height
	 */
	public final int cellSizeByHeight(int h)
	{
		if (h < depth)
			return (1 << (MAXLEVEL + 1 - depth + h));
		return gridSize;
	}

	/**
	 * Prints tree stats.
	 */
	public final void printInfos()
	{
		logger.info("Number of leaves: "+nr_leaves);
		logger.info("Number of octants: "+nr_cells);
		logger.info("Depth: "+depth);
	}
	
	/**
	 * Converts from double coordinates to integer coordinates.
	 * @param p    double coordinates.
	 * @param ijk  integer coordinates.
	 */
	public final void double2int(double [] p, int [] ijk)
	{
		for (int i = 0; i < 3; i++)
			ijk[i] = (int) ((p[i] - x0[i]) * x0[3]);
	}
	
	/**
	 * Converts from integer coordinates to double coordinates.
	 * @param ijk  integer coordinates.
	 * @param p    double coordinates.
	 */
	public final void int2double(int [] ijk, double [] p)
	{
		for (int i = 0; i < 3; i++)
			p[i] = x0[i] + ijk[i] / x0[3];
	}
	
	/**
	 * Traverses the whole OEMM structure.
	 *
	 * @param proc    procedure called on each octant.
	 * @return  <code>true</code> if the whole structure has been traversed,
	 *          <code>false</code> if traversal aborted.
	 */
	public final boolean walk(TraversalProcedure proc)
	{
		if (logger.isLoggable(Level.FINE))
			logger.fine("walk: init "+proc.getClass().getName());
		int s = gridSize;
		int l = 0;
		int i0 = 0;
		int j0 = 0;
		int k0 = 0;
		int [] posStack = new int[depth];
		posStack[l] = 0;
		Node [] octreeStack = new Node[depth];
		octreeStack[l] = root;
		proc.init(this);
		while (true)
		{
			int res = 0;
			int visit = octreeStack[l].isLeaf ? TraversalProcedure.LEAF : TraversalProcedure.PREORDER;
			if (logger.isLoggable(Level.FINE))
				logger.fine("Found "+(octreeStack[l].isLeaf ? "LEAF" : "PREORDER")+Integer.toHexString(s)+" "+Integer.toHexString(i0)+" "+Integer.toHexString(j0)+" "+Integer.toHexString(k0)+" "+octreeStack[l]);
			res = proc.action(this, octreeStack[l], posStack[l], visit);
			logger.fine("  Res; "+res);
			if (res == TraversalProcedure.ABORT)
				return false;
			if (!octreeStack[l].isLeaf && res == TraversalProcedure.OK)
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
					logger.fine("Empty node skipped: pos="+i);
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
						if (logger.isLoggable(Level.FINE))
							logger.fine("Found POSTORDER: "+Integer.toHexString(s)+" "+Integer.toHexString(i0)+" "+Integer.toHexString(j0)+" "+Integer.toHexString(k0)+" "+octreeStack[l]);
						res = proc.action(this, octreeStack[l], posStack[l], TraversalProcedure.POSTORDER);
						logger.fine("  Res; "+res);
					}
					else
					{
						if (null != octreeStack[l-1].child[posStack[l]])
							break;
						if (logger.isLoggable(Level.FINE))
							logger.fine("Empty node skipped: pos="+posStack[l]);
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
	/**
	 * Returns local index of cell containing a given point.
	 * @param size  size of child cells
	 * @param ijk   integer coordinates of desired point
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
	 * Builds an octant containing a given point if it does not already exist.
	 *
	 * @param ijk     integer coordinates of an interior node
	 * @return  the octant of the smallest size containing this point.
	 *          It is created if it does not exist.
	 */
	public final Node build(int [] ijk)
	{
		return search(minCellSize(), ijk, true, null);
	}
	
	/**
	 * Inserts an octant into the tree structure if it does not already exist.
	 *
	 * @param current     node being inserted.
	 */
	public final void insert(Node current)
	{
		int [] ijk = new int[3];
		ijk[0] = current.i0;
		ijk[1] = current.j0;
		ijk[2] = current.k0;
		search(current.size, ijk, true, current);
	}
	
	/**
	 * Returns the octant of an OEMM structure containing a given point.
	 *
	 * @param ijk     integer coordinates of an interior node
	 * @return  the octant of the smallest size containing this point.
	 */
	public final Node search(int [] ijk)
	{
		return search(0, ijk, false, null);
	}
	
	/**
	 * Returns the octant of an OEMM structure containing a given point.
	 *
	 * @param size     the returned octant must have this size.  If this value is 0,
	 *                 the deepest octant is returned.
	 * @param ijk      integer coordinates of an interior node
	 * @param create   if set to <code>true</code>, cells are created if needed.  Otherwise
	 *                 the desired octant must exist.
	 * @return  the octant of the desired size containing this point.
	 */
	private final Node search(int size, int [] ijk, boolean create, Node node)
	{
		if (root == null)
		{
			if (!create)
				throw new RuntimeException("Root element not found... Aborting");
			createRootNode(node);
			if (size == gridSize)
			{
				root.isLeaf = true;
				nr_leaves++;
				if (depth == 0)
					depth++;
			}
		}
		Node current = root;
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
					throw new IllegalArgumentException("Element not found... Aborting "+current+" "+Integer.toHexString(s)+" "+ind);
				if (level >= depth)
					depth = level + 1;
				if (depth > MAXLEVEL)
					throw new RuntimeException("Too many octree levels... Aborting");
				if (s == size && node != null)
					current.child[ind] = node;
				else
					current.child[ind] = new Node(s, ijk);
				current.child[ind].parent = current;
				current.isLeaf = false;
				nr_cells++;
				if (s == size)
					nr_leaves++;
			}
			current = current.child[ind];
		}
		return current;
	}

	/**
	 * Creates root node.
	 */
	private void createRootNode(Node node)
	{
		if (node != null && node.size == gridSize)
		{
			// This happens only when OEMM has only one leaf
			// and is read from disk, root has to be set to
			// this leaf.
			root = node;
		}
		else
			root = new Node(gridSize, 0, 0, 0);
		nr_cells++;
	}

	/**
	 * Merges all children of a given cell.
	 *
	 * @param node   cell to be merged
	 */
	protected final void mergeChildren(Node node)
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

	/**
	 * Returns the adjacent node located at a given point with the
	 * same size.
	 *
	 * @param fromNode start node
	 * @param ijk      integer coordinates of lower-left corner
	 * @return  the octant of the desired size containing this point.
	 */
	public static final Node searchAdjacentNode(Node fromNode, int [] ijk)
	{
		//  Check ijk against OEMM bounds
		if (!checkBounds(ijk))
			return null;
		//  Climb tree until an octant enclosing this
		//  point is encountered.
		Node ret = searchEnclosingNode(fromNode, ijk);
		//  Now find the deepest matching octant.
		int s = ret.size;
		while (s > fromNode.size)
		{
			s >>= 1;
			assert s > 0;
			int ind = indexSubOctree(s, ijk);
			if (null == ret.child[ind])
				return null;
			ret = ret.child[ind];
		}
		assert (ijk[0] == ret.i0 && ijk[1] == ret.j0 && ijk[2] == ret.k0);
		return ret;
	}

	private static boolean checkBounds(final int [] ijk)
	{
		for (int i = 0; i < 3; i++)
		{
			if (ijk[i] < 0 || ijk[i] >= gridSize)
				return false;
		}
		return true;
	}
	
	private static Node searchEnclosingNode(final Node fromNode,final  int [] ijk)
	{
		Node ret = fromNode;
		while (null != ret.parent)
		{
			ret = ret.parent;
			int mask = ~(ret.size - 1);
			if ((ijk[0] & mask) == ret.i0 && (ijk[1] & mask) == ret.j0 && (ijk[2] & mask) == ret.k0)
				return ret;
		}
		return ret;
	}
	/**
	 * Returns coordinates of all cell corners.
	 *
	 * @param onlyLeaves  if set to <code>true</code>, only leaf cells are
	 * considered, otherwise all cells are considered.
	 * @return  an array containing 6 quads defined by the position of the corners for each cells (the 6 quads represents a cube).
	 */
	public double [] getCoords(boolean onlyLeaves)
	{
		CoordProcedure proc = new CoordProcedure(onlyLeaves, nr_cells, nr_leaves);
		walk(proc);
		return proc.coord;
	}
	
	private final class CoordProcedure extends TraversalProcedure
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
		@Override
		public final int action(OEMM oemm, Node current, int octant, int visit)
		{
			if (visit != PREORDER && visit != LEAF)
				return OK;
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

	public int getDepth()
	{
		return depth;
	}
	
}
