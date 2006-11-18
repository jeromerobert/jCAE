/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005, by EADS CRC

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

package org.jcae.mesh.amibe.util;

import org.apache.log4j.Logger;
import org.jcae.mesh.amibe.ds.MNode3D;

/**
 * Octree to store {@link MNode3D} vertices.
 * This class is very similar to {@link QuadTree}, it is useful to find
 * near vertices.  It was needed by {@link org.jcae.mesh.amibe.algos3d.Fuse}
 * to fuse near vertices, but now that 3D meshes are also handled by the
 * {@link org.jcae.mesh.amibe.ds.Mesh} class, it became oobsolete because
 * adjacency relations between vertices are provided by
 * {@link org.jcae.mesh.amibe.ds.Mesh}.
 * Anyway this implementation has not yet been removed.
 *
 * <p>
 * Integer coordinates are used for two reasons: a better control on accuracy
 * of geometrical operations, and simpler operations on vertex location because
 * cells have power of two side length and bitwise operators can be used
 * instead of floating point operations.  The downside is that the conversion
 * between double and integer coordinates must be known in advance, which is
 * why constructor needs a bounding box as argument.
 * </p>
 *
 * <p>
 * Each {@link Cell} contains either vertices or eight children nodes
 * (some of them may be <code>null</code>).  A cell can contain at most
 * <code>BUCKETSIZE</code> vertices (default is 10).  When this number
 * is exceeded, the cell is splitted and vertices are stored in these children.
 * On the contrary, when all vertices are removed from a cell, it is deleted.
 * And when all children of a cell are null, this cell is removed.
 * </p>
 * 
 * <p>
 * Octree cells are very compact, they do not contain any locational
 * information.  It is instead passed to the
 * {@link OctreeProcedure#action(Object, int, int, int, int)} method.
 * This design had been chosen for performance reasons on large meshes, and in
 * practice it works very well because octrees are used for vertex location,
 * and no neighbourhood information is needed.
 * </p>
 *
 * <p>
 * Octree traversal is performed by the {@link #walk(OctreeProcedure)} method.
 * Here is an example to collect all vertices in a list:
 * </p>
 * <pre>
 *	public final class collectAllVerticesProcedure implements OctreeProcedure
 *	{
 *		public ArrayList vertexList = new ArrayList();
 *		public final int action(Object o, int s, int i0, int j0)
 *		{
 *			Cell self = (Cell) o;
 *			if (self.nItems > 0)
 *			{
 *				for (int i = 0; i &lt; self.nItems; i++)
 *					nodelist.add(self.subQuad[i]);
 *			}
 *			return 0;
 *		}
 *	}
 * </pre>
 * <p>
 * This procedure is applied on all cells recursively in prefix order.  If it
 * returns <code>-1</code>, {@link #walk(OctreeProcedure)} aborts its
 * processing immediately.  A null return value means that processing can
 * continue normally, and a non-null return value means that children nodes are
 * skipped.
 * </p>
 *
 * <p>
 * Below is an algorithm to find the nearest vertex in the octree of a given
 * point <code>p</code>:
 * </p>
 * <ol type="1">
 *   <li>Initializion: <code>dmin=Double.MAX_VALUE</code>, <code>result=null</code></li>
 *   <li>Traverse all octree cells.
 *     <ol type="a">
 *       <li>If this cell does not intersect the sphere centered at
 *           <code>p</code> of vertices at a distance lower than <code>dmin</code>,
 *           then skip this cell and its children.</li>
 *       <li>Otherwise, if this cell contains children nodes, do nothing so that
 *           processaing continues normally on children nodes.</li>
 *       <li>Otherwise, this cell contains vertices.  For each vertex, compute
 *           its distance to <code>p</code> and update <code>dmin</code> and
 *           <code>result</code> if it is nearer than the current solution.</li>
 *     </ol></li>
 * </ol>
 *
 * <p>
 * The implementation of {@link #getNearestVertex(MNode3D)} is slightly improved:
 * the starting point is computed by {@link #getNearVertex(MNode3D)}, so that
 * much more cells are skipped.
 * </p>
 */
public class Octree
{
	private static int BUCKETSIZE = 10;
	/**
	 * Cell of an {@link Octree}.  Each cell contains either eight children nodes
	 * or up to <code>BUCKETSIZE</code> vertices.  When this number is exceeded,
	 * the cell is splitted and vertices are moved to these children.
	 * On the contrary, when all vertices are removed from a cell, it is deleted.
	 * And when all children of a cell are null, this cell is removed.
	 */
	protected class Cell
	{
		/**
		 * Number of vertices stored below the current cell.  If this cell
		 * has children nodes, this value is negative and its opposite
		 * value is the total number of vertices found in children nodes.
		 * Otherwise, it contains the number of vertices which are stored
		 * in the {@link #subOctree} array.
		 */
		protected int nItems = 0;
		
		/**
		 * References to bound objects.  This variable either contains
		 * four references to children nodes (some of which may be
		 * <code>null</code>), or up to <code>BUCKETSIZE</code> references
		 * yo vertices.  This compact storage is needed to reduce memory
		 * usage.
		 */
		protected Object [] subOctree = null;
	}
	
	private static Logger logger=Logger.getLogger(Octree.class);	
	
	// Integer coordinates (like gridSize) must be long if MAXLEVEL > 30
	private static final int MAXLEVEL = 30;
	private static final int gridSize = 1 << MAXLEVEL;
	
	/**
	 * Root of the octree.
	 */
	public Cell root;
	
	/**
	 * Number of cells.
	 */
	public int nCells = 0;
	
	/**
	 * Conversion between double and integer coordinates.
	 */
	public final double [] x0 = new double[4];
	
	/**
	 * Create a new <code>Octree</code> of the desired size.
	 * @param umin  3D coordinates of the leftmost bottom vertex
	 * @param umax  3D coordinates of the rightmost top vertex
	 */
	public Octree(double [] umin, double [] umax)
	{
		double deltaX = 1.01 * Math.abs(umin[0] - umax[0]);
		double deltaY = 1.01 * Math.abs(umin[1] - umax[1]);
		double deltaZ = 1.01 * Math.abs(umin[2] - umax[2]);
		deltaX = Math.max(deltaX, deltaY);
		deltaX = Math.max(deltaX, deltaZ);
		for (int i = 0; i < 3; i++)
			x0[i] = umin[i];
		x0[3] = ((double) gridSize) / deltaX;
		root = new Cell();
		nCells++;
	}
	
	/**
	 * Set bucket size.  This method must be called before adding vertices
	 * into the octree.
	 *
	 * @param n  the desired bucket size.
	 */
	public final void setBucketSize(int n)
	{
		if (root.nItems != 0)
			throw new RuntimeException("setBucketSize must be called before adding items!");
		BUCKETSIZE = n;
	}
	
	/**
	 * Transform double coordinates into integer coordinates.
	 * @param p  double coordinates
	 * @param ijk  converted integer coordinates
	 */
	public final void double2int(double [] p, int [] ijk)
	{
		for (int i = 0; i < 3; i++)
			ijk[i] = (int) ((p[i] - x0[i]) * x0[3]);
	}
	
	/**
	 * Transform integer coordinates into double coordinates.
	 * @param ijk  integer coordinates
	 * @param p  converted double coordinates
	 */
	public final void int2double(int [] ijk, double [] p)
	{
		for (int i = 0; i < 3; i++)
			p[i] = x0[i] + ijk[i] / x0[3];
	}
	
	/**
	 * Return the index of the child node containing a given point.
	 * An octree node contains at most 8 children.  Cell size is a power of
	 * two, so locating a vertex can be performed by bitwise operators, as
	 * shown below.
	 * <pre>
	 *         K=0          &lt;>0
	 *      ┌───┬───┐    ┌───┬───┐
	 *  &lt;>0 │ 2 │ 3 │    │ 6 │ 7 │   with I = ijk[0] &amp; size
	 *      ├───┼───┤    ├───┼───┤        J = ijk[1] &amp; size
	 *  J=0 │ 0 │ 1 │    │ 4 │ 5 │        K = ijk[2] &amp; size
	 *      └───┴───┘    └───┴───┘
	 *      I=0  &lt;>0       0  &lt;>0
	 * </pre>
	 *
	 * @param ijk   vertex location
	 * @param size  cell size of children nodes.
	 * @return the index of the child node containing this vertex.
	 */
	private static final int indexSubOctree(int [] ijk, int size)
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
	 * Add a {@link MNode3D} to the octree.
	 * @param v  the node to add.
	 */
	public final void add(MNode3D v)
	{
		Cell current = root;
		int s = gridSize;
		int [] ijk = new int[3];
		int [] oldijk = new int[3];
		double2int(v.getXYZ(), ijk);
		while (current.nItems < 0)
		{
			current.nItems--;
			s >>= 1;
			assert s > 0;
			int ind = indexSubOctree(ijk, s);
			if (null == current.subOctree[ind])
			{
				current.subOctree[ind] = new Cell();
				nCells++;
			}
			current = (Cell) current.subOctree[ind];
		}
		
		//  If current box is full, split it into 8 suboctrees
		while (current.nItems == BUCKETSIZE)
		{
			s >>= 1;
			assert s > 0;
			Cell [] newSubOctrees = new Cell[8];
			//  Move points to their respective suboctrees.
			for (int i = 0; i < BUCKETSIZE; i++)
			{
				MNode3D p = (MNode3D) current.subOctree[i];
				double2int(p.getXYZ(), oldijk);
				int ind = indexSubOctree(oldijk, s);
				if (null == newSubOctrees[ind])
				{
					newSubOctrees[ind] = new Cell();
					nCells++;
					newSubOctrees[ind].subOctree = new MNode3D[BUCKETSIZE];
				}
				Cell target = newSubOctrees[ind];
				target.subOctree[target.nItems] = current.subOctree[i];
				target.nItems++;
			}
			current.subOctree = newSubOctrees;
			//  current will point to another cell, adjust it now.
			current.nItems = - BUCKETSIZE - 1;
			int ind = indexSubOctree(ijk, s);
			if (null == current.subOctree[ind])
			{
				current.subOctree[ind] = new Cell();
				nCells++;
			}
			current = (Cell) current.subOctree[ind];
		}
		//  Eventually insert the new point
		if (current.nItems == 0)
			current.subOctree = new MNode3D[BUCKETSIZE];
		current.subOctree[current.nItems] = v;
		current.nItems++;
	}
	
	/**
	 * Remove a {@link MNode3D} from the octree.
	 * @param v  the node to remove.
	 */
	public final void remove(MNode3D v)
	{
		Cell current = root;
		Cell last = root;
		Cell next;
		int lastPos = 0;
		int s = gridSize;
		int [] ijk = new int[3];
		double2int(v.getXYZ(), ijk);
		while (current.nItems < 0)
		{
			//  nItems is negative
			current.nItems++;
			if (current.nItems == 0)
			{
				// TODO: can this happen?
				last.subOctree[lastPos] = null;
				nCells--;
			}
			s >>= 1;
			assert s > 0;
			int ind = indexSubOctree(ijk, s);
			next = (Cell) current.subOctree[ind];
			if (null == next)
				throw new RuntimeException("MNode3D "+v+" is not present and can not be deleted");
			last = current;
			lastPos = ind;
			current = next;
		}
		int offset = 0;
		for (int i = 0; i < current.nItems; i++)
		{
			if (v == (MNode3D) current.subOctree[i])
				offset++;
			else if (offset > 0)
				current.subOctree[i-offset] = current.subOctree[i];
		}
		if (offset == 0)
			throw new RuntimeException("MNode3D "+v+" is not present and can not be deleted");
		current.nItems--;
		if (current.nItems > 0)
			current.subOctree[current.nItems] = null;
		else
		{
			logger.debug("Last point removed, deleting node");
			last.subOctree[lastPos] = null;
			nCells--;
		}
	}
	
	/**
	 * Return a stored element of the <code>Octree</code> which is
	 * near from a given vertex.  The algorithm is simplistic: the leaf
	 * which would contains this node is retrieved.  If it contains
	 * vertices, the nearest one is returned (vertices in other leaves may
	 * of course be nearer).  Otherwise the nearest vertex from sibling
	 * children is returned.  The returned vertex is a good starting point
	 * for {@link #getNearestVertex(MNode3D)}.
	 *
	 * @param v  the node to check.
	 * @return a near vertex.
	 */
	public final MNode3D getNearVertex(MNode3D v)
	{
		Cell current = root;
		if (current.nItems == 0)
			return null;
		Cell last = null;
		int s = gridSize;
		int [] ijk = new int[3];
		int [] retijk = new int[3];
		double2int(v.getXYZ(), ijk);
		int searchedCells = 0;
		if (logger.isDebugEnabled())
			logger.debug("Near point: "+v);
		while (null != current && current.nItems < 0)
		{
			last = current;
			s >>= 1;
			assert s > 0;
			searchedCells++;
			current = (Cell)
				current.subOctree[indexSubOctree(ijk, s)];
		}
		if (null == current)
			return getNearVertexInSubOctrees(last, v, s << 1, searchedCells);
		
		MNode3D vQ = (MNode3D) current.subOctree[0];
		MNode3D ret = vQ;
		double2int(vQ.getXYZ(), retijk);
		long ldist =
				((long) (ijk[0] - retijk[0])) * ((long) (ijk[0] - retijk[0])) +
				((long) (ijk[1] - retijk[1])) * ((long) (ijk[1] - retijk[1])) +
				((long) (ijk[2] - retijk[2])) * ((long) (ijk[2] - retijk[2]));
		for (int i = 1; i < current.nItems; i++)
		{
			vQ = (MNode3D) current.subOctree[i];
			double2int(vQ.getXYZ(), retijk);
			long d =
				((long) (ijk[0] - retijk[0])) * ((long) (ijk[0] - retijk[0])) +
				((long) (ijk[1] - retijk[1])) * ((long) (ijk[1] - retijk[1])) +
				((long) (ijk[2] - retijk[2])) * ((long) (ijk[2] - retijk[2]));
			if (d < ldist)
			{
				ldist = d;
				ret = vQ;
			}
		}
		if (logger.isDebugEnabled())
			logger.debug("  search in "+searchedCells+"/"+nCells+" cells");
		return ret;
	}
	
	private final MNode3D getNearVertexInSubOctrees(Cell current, MNode3D v, int dist, int searchedCells)
	{
		MNode3D ret = null;
		int [] ijk = new int[3];
		int [] retijk = new int[3];
		double2int(v.getXYZ(), ijk);
		// Cell diagonal is of length sqrt(3)*dist
		long ldist = 3L * ((long) dist) * ((long) dist);
		if (logger.isDebugEnabled())
			logger.debug("Near point in suboctrees: "+v);
		int l = 0;
		int [] posStack = new int[MAXLEVEL];
		posStack[l] = 0;
		Cell [] octreeStack = new Cell[MAXLEVEL];
		octreeStack[l] = current;
		while (true)
		{
			searchedCells++;
			if (octreeStack[l].nItems < 0)
			{
				l++;
				assert l <= MAXLEVEL;
				for (int i = 0; i < 8; i++)
				{
					if (null != octreeStack[l-1].subOctree[i])
					{
						octreeStack[l] = (Cell) octreeStack[l-1].subOctree[i];
						posStack[l] = i;
						break;
					}
				}
			}
			else
			{
				for (int i = 0; i < octreeStack[l].nItems; i++)
				{
					MNode3D vQ = (MNode3D) octreeStack[l].subOctree[i];
					double2int(vQ.getXYZ(), retijk);
					long d =
						((long) (ijk[0] - retijk[0])) * ((long) (ijk[0] - retijk[0])) +
						((long) (ijk[1] - retijk[1])) * ((long) (ijk[1] - retijk[1])) +
						((long) (ijk[2] - retijk[2])) * ((long) (ijk[2] - retijk[2]));
					if (d < ldist)
					{
						ldist = d;
						ret = vQ;
					}
				}
				if (null != ret)
				{
					if (logger.isDebugEnabled())
						logger.debug("  search in "+searchedCells+"/"+nCells+" cells");
					return ret;
				}
				//  Search in siblings
				while (l > 0)
				{
					posStack[l]++;
					if (posStack[l] == 8)
						l--;
					else if (null != octreeStack[l-1].subOctree[posStack[l]])
						break;
				}
				if (l == 0)
					break;
				octreeStack[l] = (Cell) octreeStack[l-1].subOctree[posStack[l]];
			}
		}
		if (logger.isDebugEnabled())
			logger.debug("  search in "+searchedCells+"/"+nCells+" cells");
		assert ret != null;
		return ret;
	}
	
	private final class getMinSizeProcedure implements OctreeProcedure
	{
		public int searchedCells = 0;
		public int minSize = gridSize;
		public getMinSizeProcedure()
		{
		}
		public final int action(Object o, int s, int i0, int j0, int k0)
		{
			Cell self = (Cell) o;
			searchedCells++;
			if (s < minSize)
				minSize = s;
			return 0;
		}
	}
	
	private final int getMinSize()
	{
		Cell current = root;
		getMinSizeProcedure gproc = new getMinSizeProcedure();
		walk(gproc);
		int ret = gproc.minSize;
		if (logger.isDebugEnabled())
		{
			logger.debug("  search in "+gproc.searchedCells+"/"+nCells+" cells");
			logger.debug("  result: "+ret);
		}
		return ret;
	}
	
	public final int getMaxLevel()
	{
		int size = getMinSize();
		int ret = 1;
		while (size < gridSize)
		{
			size <<= 1;
			ret++;
		}
		return ret;
	}
	
	private final class getNearestVertexProcedure implements OctreeProcedure
	{
		private final int [] ijk = new int[3];;
		private final int [] retijk = new int[3];;
		private int idist;
		private long ldist;
		public MNode3D nearestVertex;
		public int searchedCells = 0;
		public getNearestVertexProcedure(MNode3D from, MNode3D v)
		{
			double2int(from.getXYZ(), ijk);
			nearestVertex = v;
			double2int(nearestVertex.getXYZ(), retijk);
			ldist =
				((long) (ijk[0] - retijk[0])) * ((long) (ijk[0] - retijk[0])) +
				((long) (ijk[1] - retijk[1])) * ((long) (ijk[1] - retijk[1])) +
				((long) (ijk[2] - retijk[2])) * ((long) (ijk[2] - retijk[2]));
			idist = (int) Math.sqrt(ldist);
		}
		public final int action(Object o, int s, int i0, int j0, int k0)
		{
			boolean valid = (i0 - idist <= ijk[0] && ijk[0] <= i0 + s + idist &&
			                 j0 - idist <= ijk[1] && ijk[1] <= j0 + s + idist &&
			                 k0 - idist <= ijk[2] && ijk[2] <= k0 + s + idist);
			if (!valid)
				return 1;
			Cell self = (Cell) o;
			searchedCells++;
			if (self.nItems > 0)
			{
				for (int i = 0; i < self.nItems; i++)
				{
					MNode3D vtest = (MNode3D) self.subOctree[i];
					double2int(vtest.getXYZ(), retijk);
					long retdist =
						((long) (ijk[0] - retijk[0])) * ((long) (ijk[0] - retijk[0])) +
						((long) (ijk[1] - retijk[1])) * ((long) (ijk[1] - retijk[1])) +
						((long) (ijk[2] - retijk[2])) * ((long) (ijk[2] - retijk[2]));
					if (retdist < ldist)
					{
						ldist = retdist;
						nearestVertex = vtest;
						idist = (int) Math.sqrt(ldist);
					}
				}
			}
			return 0;
		}
	}
	
	/**
	 * Return the nearest vertex stored in this <code>Octree</code>.
	 *
	 * @param v  the node to check.
	 * @return the nearest vertex.
	 */
	public final MNode3D getNearestVertex(MNode3D v)
	{
		Cell current = root;
		MNode3D ret = getNearVertex(v);
		if (ret == null)
			return null;
		if (logger.isDebugEnabled())
			logger.debug("Nearest point of "+v);
		
		getNearestVertexProcedure gproc = new getNearestVertexProcedure(v, ret);
		walk(gproc);
		ret = gproc.nearestVertex;
		if (logger.isDebugEnabled())
		{
			logger.debug("  search in "+gproc.searchedCells+"/"+nCells+" cells");
			logger.debug("  result: "+ret);
		}
		return ret;
	}
	
	/**
	 * Perform an action on all cells in prefix order.
	 *
	 * The procedure is applied to the root cell, then recursively to
	 * its children.  If it returns <code>-1</code>, processing aborts
	 * immediately and <code>false</code> is returned.  If the
	 * procedure returns <code>1</code>, cell children are not processed.
	 *
	 * @param proc  procedure to apply on each cell.
	 * @return <code>true</code> if all cells have been traversed, <code>false</code> otherwise.
	 * @see OctreeProcedure
	 */
	public final boolean walk(OctreeProcedure proc)
	{
		int s = gridSize;
		int l = 0;
		int i0 = 0;
		int j0 = 0;
		int k0 = 0;
		int [] posStack = new int[MAXLEVEL];
		posStack[l] = 0;
		Cell [] octreeStack = new Cell[MAXLEVEL];
		octreeStack[l] = root;
		while (true)
		{
			int res = proc.action(octreeStack[l], s, i0, j0, k0);
			if (res == -1)
				return false;
			if (octreeStack[l].nItems < 0 && res == 0)
			{
				s >>= 1;
				assert s > 0;
				l++;
				assert l <= MAXLEVEL;
				for (int i = 0; i < 8; i++)
				{
					if (null != octreeStack[l-1].subOctree[i])
					{
						octreeStack[l] = (Cell) octreeStack[l-1].subOctree[i];
						posStack[l] = i;
						break;
					}
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
					}
					else
					{
						if (null != octreeStack[l-1].subOctree[posStack[l]])
							break;
					}
				}
				if (l == 0)
					break;
				octreeStack[l] = (Cell) octreeStack[l-1].subOctree[posStack[l]];
			}
		}
		assert i0 == 0;
		assert j0 == 0;
		assert k0 == 0;
		return true;
	}
	
}
