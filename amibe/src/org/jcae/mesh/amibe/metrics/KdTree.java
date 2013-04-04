/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005, by EADS CRC
    Copyright (C) 2007,2008,2009,2010, by EADS France

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

package org.jcae.mesh.amibe.metrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Quadtree structure to store 2D vertices.  When adjacent relations have not
 * yet been set, a quadtree is an efficient way to locate a point among a set
 * of points and triangles.
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
 * Each {@link Cell} contains either vertices or four children nodes
 * (some of them may be <code>null</code>).  A cell can contain at most
 * {@link #BUCKETSIZE} vertices (default is 10).  When this number
 * is exceeded, the cell is splitted and vertices are stored in these children.
 * On the contrary, when all vertices are removed from a cell, it is deleted.
 * And when all children of a cell are null, this cell is removed.
 * </p>
 * 
 * <p>
 * Quadtree cells are very compact, they do not contain any locational
 * information.  It is instead passed to the
 * {@link KdTreeProcedure#action(Object, int, int[])} method.
 * This design had been chosen for performance reasons on large meshes, and in
 * practice it works very well because quadtrees are used for vertex location,
 * and no neighbourhood information is needed.
 * </p>
 *
 * <p>
 * Quadtree traversal is performed by the {@link #walk(KdTreeProcedure)} method.
 * Here is an example to collect all vertices in a list:
 * </p>
 * <pre>
 *	public final class collectAllVerticesProcedure implements KdTreeProcedure
 *	{
 *		public Collection<Vertex> vertexList = new ArrayList<Vertex>();
 *		public final int action(Object o, int s, int [] i0)
 *		{
 *			Cell self = (Cell) o;
 *			if (self.nItems > 0)
 *			{
 *				for (int i = 0; i &lt; self.nItems; i++)
 *					vertexList.add((Vertex) self.subCell[i]);
 *			}
 *			return KdTreeProcedure.OK;
 *		}
 *	}
 * </pre>
 * <p>
 * This procedure is applied on all cells recursively in prefix order.  If it
 * returns {@link KdTreeProcedure#ABORT}, {@link #walk(KdTreeProcedure)} aborts its
 * processing immediately;  {@link KdTreeProcedure#OK} return value means that processing can
 * continue normally, and {@link KdTreeProcedure#SKIPCHILD} return value means that children
 * nodes are skipped.
 * </p>
 *
 * <p>
 * In Euclidian 2D space, vertices which have a distance to a point <code>p</code>
 * lower than <code>d</code> are contained in a circle centered at <code>p</code>
 * with radius <code>d</code>.  With Riemannian metrics, this circle becomes
 * an ellipsis.  This ellipsis is only determined by local properties of the
 * surface at point <code>p</code>.
 * If we already found a point <code>V1</code> at a distance <code>d1</code>,
 * vertices which belong to a quadtree cell not intersecting this ellipsis
 * do not need to be considered.
 * </p>
 *
 * <p>
 * Below is an algorithm to find the nearest vertex in the quadtree of a given
 * point <code>p</code>:
 * </p>
 * <ol type="1">
 *   <li>Initialization: <code>dmin=Double.MAX_VALUE</code>, <code>result=null</code></li>
 *   <li>Traverse all quadtree cells.
 *     <ol type="a">
 *       <li>If this cell does not intersect the ellipsis centered at
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
 * The implementation of {@link #getNearestVertex} has two
 * differences:
 * </p>
 * <ul>
 *   <li>The starting point is computed by {@link #getNearVertex}.
 *       This means that much more cells are skipped.</li>
 *   <li>The ellipsis is replaced by a circle enclosing it, to have simpler
 *       calculus.  Using the real ellipsis could be tested though, it should
 *       also speed up this processing.</li>
 * </ul>
 */
public class KdTree<T extends Location>
{
	private static final Logger logger=Logger.getLogger(KdTree.class.getName());
	/**
	 * Cell of a {@link KdTree}.  Each cell contains either four children nodes
	 * or up to <code>BUCKETSIZE</code> vertices.  When this number is exceeded,
	 * the cell is splitted and vertices are moved to these children.
	 * On the contrary, when all vertices are removed from a cell, it is deleted.
	 * And when all children of a cell are null, this cell is removed.
	 */
	/**
	 * Maximal number of vertices which can be stored in a cell.
	 * This number must be at least 4, because children nodes are
	 * stored in the same place as vertices, and a cell can have at
	 * most 4 children.  Its value is 10.
	 */
	private final int BUCKETSIZE;
		
	public class Cell
	{
		/**
		 * Number of vertices stored below the current cell.  If this cell
		 * has children nodes, this value is negative and its opposite
		 * value is the total number of vertices found in children nodes.
		 * Otherwise, it contains the number of vertices which are stored
		 * in the {@link #subCell} array.
		 */
		private int nItems;
		
		/**
		 * References to bound objects.  This variable either contains
		 * four references to children nodes (some of which may be
		 * <code>null</code>), or up to {@link KdTree#BUCKETSIZE} references
		 * yo vertices.  This compact storage is needed to reduce memory
		 * usage.
		 */
		private Object [] subCell;

		public final boolean isLeaf()
		{
			return nItems >= 0;
		}

		public final int count()
		{
			if (nItems >= 0)
				return nItems;
			return -nItems;
		}

		@SuppressWarnings("unchecked")
		public final T getVertex(int i)
		{
			assert nItems > 0 && i < nItems;
			return (T) subCell[i];
		}
	}
	
	private final int dimension;
	private final int nrSub;

	// Integer coordinates (like gridSize) must be long if MAXLEVEL > 30
	private static final int MAXLEVEL = 30;
	private static final int gridSize = 1 << MAXLEVEL;
	private static final double DGridSize = gridSize;
	
	/**
	 * Root of the kd-tree.
	 */
	private final Cell root;
	
	/**
	 * Number of cells.
	 */
	public int nCells;
	
	/**
	 * Conversion between double and integer coordinates.
	 */
	private final double [] x0;

	/**
	 * Methods only available in 2D.
	 */
	public final K2DInterface k2D;

	/**
	 * Dummy constructor.  This instance must be properly initialised by calling
	 * {@link #setup} before putting elements into it.
	 *
	 * @param d   dimension (2 or 3)
	 */
	public KdTree(int d)
	{
		BUCKETSIZE = 10;
		dimension = d;
		if (d == 2)
			k2D = new K2D();
		else
			k2D = new K2DInvalid();
		nrSub = 1 << dimension;
		x0 = new double[dimension+1];
		root = new Cell();
	}
	
	/**
	 * Create a new <code>KdTree</code> of the desired size.
	 *
	 * @param bbox   coordinates of bottom-left vertex and upper-right vertices
	 */
	public KdTree(double [] bbox)
	{
		this(bbox, 10);
	}
	
	/**
	 * Create a new <code>KdTree</code> of the desired size.
	 *
	 * @param bbox   coordinates of bottom-left vertex and upper-right vertices
	 * @param bucketsize  bucket size
	 */
	KdTree(double [] bbox, int bucketsize)
	{
		BUCKETSIZE = bucketsize;
		dimension = bbox.length / 2;
		if (dimension == 2)
			k2D = new K2D();
		else
			k2D = new K2DInvalid();
		nrSub = 1 << dimension;
		x0 = new double[dimension+1];
		root = new Cell();
		setup(bbox);
	}
	
	/**
	 * Computes {@link #x0} adapted to this bounding box.
	 *
	 * @param bbox   coordinates of bottom-left vertex and upper-right vertices
	 */
	public final void setup(double [] bbox)
	{
		if (bbox.length != 2*dimension)
			throw new IllegalArgumentException();
		if (nCells > 0)
			throw new RuntimeException("KdTree.setup() cannot be called after KdTree has been modified");
		nCells = 1;
		double maxDelta = 0.0;
		for (int i = 0; i < dimension; i++)
		{
			x0[i] = bbox[i];
			double delta = Math.abs(bbox[i+dimension] - bbox[i]);
			if (delta > maxDelta)
				maxDelta = delta;
		}
		maxDelta *= 1.01;
		x0[dimension] = DGridSize / maxDelta;
		if (logger.isLoggable(Level.FINE))
		{
			StringBuilder sb = new StringBuilder("New KdTree int<--->double conversion vector; scale=");
			sb.append(x0[dimension]);
			sb.append("  origin=(");
			sb.append(x0[0]);
			for (int i = 1; i < dimension; i++)
				sb.append(",").append(x0[i]);
			sb.append(")\nBounding box:\n");
			for (int i = 0; i < dimension; i++)
			{
				sb.append(bbox[i]);
				sb.append(" <= x[");
				sb.append(i);
				sb.append("] <= ");
				sb.append(bbox[i+dimension]);
			}
			logger.fine(sb.toString());
		}
	}
	
	/**
	 * Transform double coordinates into integer coordinates.
	 * @param p  double coordinates
	 * @param i  integer coordinates
	 */
	private void double2int(double [] p, int [] i)
	{
		for (int k = 0; k < dimension; k++)
			i[k] = (int) ((p[k] - x0[k]) * x0[dimension]);
	}
	
	private void double2int(T p, int[] i)
	{
		i[0] = (int) ((p.getX() - x0[0]) * x0[dimension]);
		i[1] = (int) ((p.getY() - x0[1]) * x0[dimension]);
		if(i.length > 2)
			i[2] = (int) ((p.getZ() - x0[2]) * x0[dimension]);
	}

	/**
	 * Transform integer coordinates into double coordinates.
	 * @param i  integer coordinates
	 * @param p  double coordinates
	 */
	public final void int2double(int [] i, double [] p)
	{
		for (int k = 0; k < dimension; k++)
			p[k] = x0[k] + i[k] / x0[dimension];
	}

	/**
	 * Return the coordinates of the center of the grid.
	 * @return the coordinates of the center of the grid.
	 */
	@SuppressWarnings("unused")
	private double [] center()
	{
		double [] p = new double[dimension];
		for (int k = 0; k < dimension; k++)
			p[k] = x0[k] + DGridSize * 0.5 / x0[dimension];
		return p;
	}
	
	/*
	 * Return the index of the child node containing a given point.
	 * A KdTree.Cell contains at most 4 children.  Cell size is a power of
	 * two, so locating a vertex can be performed by bitwise operators, as
	 * shown below.
	 * <pre>
	 *      ┌───┬───┐
	 *  &lt;>0 │ 2 │ 3 │   with
	 *      ├───┼───┤    I = i &amp; size
	 *  J=0 │ 0 │ 1 │    J = j &amp; size
	 *      └───┴───┘
	 *      I=0  &lt;>0
	 * </pre>
	 * @param ijk   coordinates of a vertex.
	 * @param size  cell size of children nodes.
	 * @return the index of the child node containing this vertex.
	 */
	private int indexSubCell(int [] ijk, int size)
	{
		int ret = 0;
		if (size == 0)
		{
			double[] coords = new double[3];
			int2double(ijk, coords);
			throw new RuntimeException(
				"Exceeded maximal number of levels for kd-trees around "+
				Arrays.toString(coords) +"... Aborting");
		}
		for (int k = 0; k < dimension; k++)
		{
			if ((ijk[k] & size) != 0)
				ret |= 1 << k;
		}
		return ret;
	}
	
	/**
	 * Add a vertex to the kd-tree.
	 *
	 * @param v  the vertex being added.
	 * @return <code>true</code> if cell was full and had to be split, <code>false</code> otherwise.
	 */
	public final boolean add(T v)
	{
		if (nCells == 0)
			throw new RuntimeException("KdTree.setup() must be called before KdTree.add()");
		boolean ret = false;
		Cell current = root;
		int s = gridSize;
		int [] ij = new int[dimension];
		int [] oldij = new int[dimension];
		double2int(v, ij);
		while (current.nItems < 0)
		{
			//  nItems is negative means that this cell only
			//  contains subcells, and its opposite is the
			//  total number of nodes found in subcells.
			current.nItems--;
			s >>= 1;
			assert s > 0;
			int ind = indexSubCell(ij, s);
			if (null == current.subCell[ind])
			{
				current.subCell[ind] = new Cell();
				nCells++;
			}
			current = (Cell) current.subCell[ind];
		}
		
		//  If current box is full, split it into subcells
		while (current.nItems == BUCKETSIZE)
		{
			s >>= 1;
			assert s > 0;
			ret = true;
			Object[] newSubQuads = new Object[nrSub];
			//  Move points to their respective subcells.
			for (int i = 0; i < BUCKETSIZE; i++)
			{
				T p = current.getVertex(i);
				double2int(p, oldij);
				int ind = indexSubCell(oldij, s);
				Cell target = (Cell) newSubQuads[ind];
				if (null == target)
				{
					target = new Cell();
					newSubQuads[ind] = target;
					nCells++;
					target.subCell = new Object[BUCKETSIZE];
				}
				target.subCell[target.nItems] = current.subCell[i];
				target.nItems++;
			}
			current.subCell = newSubQuads;
			//  current will point to another cell, adjust it now.
			current.nItems = - BUCKETSIZE - 1;
			int ind = indexSubCell(ij, s);
			if (null == current.subCell[ind])
			{
				current.subCell[ind] = new Cell();
				nCells++;
			}
			current = (Cell) current.subCell[ind];
		}
		//  Eventually insert the new point
		if (current.nItems == 0)
			current.subCell = new Object[BUCKETSIZE];
		current.subCell[current.nItems] = v;
		current.nItems++;
		return ret;
	}
	
	/**
	 * Remove a vertex from the kd-tree.
	 *
	 * @param v  the vertex being removed.
	 */
	public final void remove(T v)
	{
		if (nCells == 0)
			throw new RuntimeException("KdTree.setup() must be called before KdTree.remove()");
		Cell current = root;
		Cell last = root;
		Cell next;
		int lastPos = 0;
		int s = gridSize;
		int [] ij = new int[dimension];
		double2int(v, ij);
		while (current.nItems < 0)
		{
			//  nItems is negative
			current.nItems++;
			if (current.nItems == 0)
				last.subCell[lastPos] = null;
			s >>= 1;
			assert s > 0;
			int ind = indexSubCell(ij, s);
			next = (Cell) current.subCell[ind];
			if (null == next)
				throw new RuntimeException("Vertex "+v+" is not present and can not be deleted");
			last = current;
			lastPos = ind;
			current = next;
		}
		int offset = 0;
		for (int i = 0; i < current.nItems; i++)
		{
			if (v == current.subCell[i])
				offset++;
			else if (offset > 0)
				current.subCell[i-offset] = current.subCell[i];
		}
		if (offset == 0)
			throw new RuntimeException("Vertex "+v+" is not present and can not be deleted");
		if (current.nItems > 1)
		{
			current.subCell[current.nItems-1] = null;
			current.nItems--;
		}
		else
			last.subCell[lastPos] = null;
	}

	private final class GetAllVerticesProcedure implements KdTreeProcedure
	{
		private final Collection<T> nodelist;
		private GetAllVerticesProcedure(int capacity)
		{
			nodelist = new ArrayList<T>(capacity);
		}
		public int action(Object o, int s, final int [] i0)
		{
			Cell self = (Cell) o;
			if (self.nItems > 0)
			{
				for (int i = 0; i < self.nItems; i++)
					nodelist.add(self.getVertex(i));
			}
			return KdTreeProcedure.OK;
		}
	}
	
	/**
	 * Return a collection of all vertices.
	 *
	 * @param capacity  initial capacity of the <code>Collection</code>.
	 * @return a collection containing all vertices.
	 */
	public final Collection<T> getAllVertices(int capacity)
	{
		GetAllVerticesProcedure gproc = new GetAllVerticesProcedure(capacity);
		walk(gproc);
		return gproc.nodelist;
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
	 * @see KdTreeProcedure
	 */
	public final boolean walk(KdTreeProcedure proc)
	{
		int s = gridSize;
		int l = 0;
		int [] i0 = new int[dimension];
		int [] posStack = new int[MAXLEVEL];
		posStack[l] = 0;
		Object [] cellStack = new Object[MAXLEVEL];
		cellStack[l] = root;
		while (true)
		{
			int res = proc.action(cellStack[l], s, i0);
			if (res == KdTreeProcedure.ABORT)
				return false;
			Cell current = (Cell) cellStack[l];
			if (current.nItems < 0 && res == KdTreeProcedure.OK)
			{
				s >>= 1;
				assert s > 0;
				l++;
				assert l <= MAXLEVEL;
				for (int i = 0; i < nrSub; i++)
				{
					Object target = current.subCell[i];
					if (null != target)
					{
						cellStack[l] = target;
						posStack[l] = i;
						break;
					}
				}
				for (int k = 0; k < dimension; k++)
					if ((posStack[l] & (1 << k)) != 0)
						i0[k] += s;
			}
			else
			{
				while (l > 0)
				{
					posStack[l]++;
					if (posStack[l] == nrSub)
					{
						for (int k = 0; k < dimension; k++)
							i0[k] -= s;
						s <<= 1;
						l--;
					}
					else
					{
						for (int k = 0; k < dimension; k++)
						{
							if ((posStack[l] & (1 << k)) != 0)
							{
								i0[k] += s;
								break;
							}
							i0[k] -= s;
						}
						if (null != ((Cell) cellStack[l-1]).subCell[posStack[l]])
							break;
					}
				}
				if (l == 0)
					break;
				cellStack[l] = ((Cell) cellStack[l-1]).subCell[posStack[l]];
			}
		}
		return true;
	}

	// Called in log messages
	private String coordinatesToString(T uv)
	{
		return "("+uv.getX()+", "+uv.getY()+", "+uv.getZ()+")";
	}

	/**
	 * Return a stored element of the <code>KdTree</code> which is
	 * near from a given position.  The algorithm is simplistic: the leaf
	 * which would contains this node is retrieved.  If it contains
	 * vertices, the nearest one is returned (vertices in other leaves may
	 * of course be nearer).  Otherwise the nearest vertex from sibling
	 * children is returned.  The returned vertex is a good starting point
	 * for {@link #getNearestVertex}.
	 *
	 * @param uv  coordinates.
	 * @return a near vertex.
	 */
	public final T getNearVertex(Metric metric, T uv)
	{
		if (root.nItems == 0)
			return null;
		Cell current = root;
		Cell last = null;
		int s = gridSize;
		int [] ijk = new int[dimension];
		double2int(uv, ijk);
		int searchedCells = 0;
		if (logger.isLoggable(Level.FINE))
			logger.fine("Near point: "+coordinatesToString(uv));
		while (null != current && current.nItems < 0)
		{
			last = current;
			s >>= 1;
			assert s > 0;
			searchedCells++;
			current = (Cell) current.subCell[indexSubCell(ijk, s)];
		}
		if (null == current)
			return getNearVertexInSubCells(last, metric, uv, searchedCells);
		
		T vQ = current.getVertex(0);
		T ret = vQ;
		double retdist = metric.distance2(uv, vQ);
		for (int i = 1; i < current.nItems; i++)
		{
			vQ = current.getVertex(i);
			double d = metric.distance2(uv, vQ);
			if (d < retdist)
			{
				retdist = d;
				ret = vQ;
			}
		}
		if (logger.isLoggable(Level.FINE))
			logger.fine("  search in "+searchedCells+"/"+nCells+" cells");
		return ret;
	}
	
	private T getNearVertexInSubCells(Cell current, Metric metric, T uv, int searchedCells)
	{
		T ret = null;
		int [] ijk = new int[dimension];
		double dist = -1.0;
		double2int(uv, ijk);
		int l = 0;
		int [] posStack = new int[MAXLEVEL];
		posStack[l] = 0;
		Object [] cellStack = new Object[MAXLEVEL];
		cellStack[l] = current;
		while (true)
		{
			searchedCells++;
			Cell s = (Cell) cellStack[l];
			if (s.nItems < 0)
			{
				l++;
				assert l <= MAXLEVEL;
				for (int i = 0; i < 8; i++)
				{
					if (null != s.subCell[i])
					{
						cellStack[l] = s.subCell[i];
						posStack[l] = i;
						break;
					}
				}
			}
			else
			{
				for (int i = 0; i < s.nItems; i++)
				{
					T vQ = s.getVertex(i);
					double d = metric.distance2(uv, vQ);
					if (d < dist || dist < 0.0)
					{
						dist = d;
						ret = vQ;
					}
				}
				if (null != ret)
				{
					if (logger.isLoggable(Level.FINE))
						logger.fine("  search in "+searchedCells+"/"+nCells+" cells");
					return ret;
				}
				//  Search in siblings
				while (l > 0)
				{
					posStack[l]++;
					if (posStack[l] == 8)
						l--;
					else if (null != ((Cell) cellStack[l-1]).subCell[posStack[l]])
						break;
				}
				if (l == 0)
					break;
				cellStack[l] = ((Cell) cellStack[l-1]).subCell[posStack[l]];
			}
		}
		throw new RuntimeException("Near vertex not found");
	}
	
	private final class GetNearestVertexProcedure implements KdTreeProcedure
	{
		private final int [] ijk = new int[dimension];
		private final T fromPosition;
		private final Metric metric;
		private T nearestVertex;
		private final double [] i2d = new double[dimension];
		private final int [] idist = new int[dimension];
		private double dist;
		private int searchedCells;
		private GetNearestVertexProcedure(Metric m, T from, T v)
		{
			double2int(from, ijk);
			nearestVertex = v;
			fromPosition = from;
			metric = m;
			dist = metric.distance2(fromPosition, nearestVertex);
			double [] r = metric.getUnitBallBBox();
			for (int k = 0; k < dimension; k++)
			{
				i2d[k] = 1.005 * x0[dimension] * r[k];
				idist[k] = (int) (Math.sqrt(dist) * i2d[k]);
				if (idist[k] > Integer.MAX_VALUE/2)
					idist[k] = Integer.MAX_VALUE/2;
			}
		}
		public int action(Object o, int s, final int [] i0)
		{
			for (int k = 0; k < dimension; k++)
				if ((ijk[k] < i0[k] - idist[k]) || (ijk[k] > i0[k] + s + idist[k]))
					return KdTreeProcedure.SKIPCHILD;
			Cell self = (Cell) o;
			searchedCells++;
			if (self.nItems > 0)
			{
				boolean updated = false;
				for (int i = 0; i < self.nItems; i++)
				{
					T vtest = self.getVertex(i);
					double retdist = metric.distance2(fromPosition, vtest);
					if (retdist < dist)
					{
						dist = retdist;
						nearestVertex = vtest;
						updated = true;
					}
				}
				if (updated)
				{
					for (int k = 0; k < dimension; k++)
					{
						idist[k] = (int) (Math.sqrt(dist) * i2d[k]);
						if (idist[k] > Integer.MAX_VALUE/2)
							idist[k] = Integer.MAX_VALUE/2;
					}
				}
			}
			return KdTreeProcedure.OK;
		}
	}

	/**
	 * Return the nearest vertex stored in this <code>KdTree</code>.
	 *
	 * @param uv  coordinates.
	 * @return the nearest vertex.
	 */
	public final T getNearestVertex(Metric metric, T uv)
	{
		if (root.nItems == 0)
			return null;
		T near = getNearVertex(metric, uv);
		if (logger.isLoggable(Level.FINE))
			logger.fine("Nearest point of "+coordinatesToString(uv));
		
		GetNearestVertexProcedure gproc = new GetNearestVertexProcedure(metric, uv, near);
		walk(gproc);
		T ret = gproc.nearestVertex;
		if (logger.isLoggable(Level.FINE))
		{
			logger.fine("  search in "+gproc.searchedCells+"/"+nCells+" cells");
			logger.fine("  result: "+ret);
		}
		return ret;
	}
	
	private final class GetNearestVertexDebugProcedure implements KdTreeProcedure
	{
		private final int [] ij = new int[dimension];
		private double dist;
		private final T fromPosition;
		private T nearestVertex;
		private final Metric metric;
		private int searchedCells;
		private GetNearestVertexDebugProcedure(Metric m, T from, T v)
		{
			double2int(from, ij);
			nearestVertex = v;
			fromPosition = from;
			metric = m;
			dist = metric.distance2(fromPosition, v);
		}
		public int action(Object o, int s, final int [] i0)
		{
			Cell self = (Cell) o;
			searchedCells++;
			if (self.nItems > 0)
			{
				for (int i = 0; i < self.nItems; i++)
				{
					T vtest = self.getVertex(i);
					double retdist = metric.distance2(fromPosition, vtest);
					if (retdist < dist)
					{
						dist = retdist;
						nearestVertex = vtest;
					}
				}
			}
			return KdTreeProcedure.OK;
		}
	}
	
	/**
	 * Slow implementation of {@link #getNearestVertex}.
	 * This method should be called only for debugging purpose.
	 *
	 * @param uv  coordinates.
	 * @return the nearest vertex.
	 */
	public final T getNearestVertexDebug(Metric metric, T uv)
	{
		if (root.nItems == 0)
			return null;
		T ret = getNearVertex(metric, uv);
		assert ret != null;
		if (logger.isLoggable(Level.FINE))
			logger.fine("(debug) Nearest point of "+coordinatesToString(uv));
		
		GetNearestVertexDebugProcedure gproc = new GetNearestVertexDebugProcedure(metric, uv, ret);
		walk(gproc);
		ret = gproc.nearestVertex;
		if (logger.isLoggable(Level.FINE))
		{
			logger.fine("  search in "+gproc.searchedCells+"/"+nCells+" cells");
			logger.fine("  result: "+ret);
		}
		return ret;
	}
	
	private static final class GetMinSizeProcedure implements KdTreeProcedure
	{
		private int searchedCells;
		private int minSize = gridSize;
		public GetMinSizeProcedure()
		{
		}
		public int action(Object o, int s, final int [] i0)
		{
			searchedCells++;
			if (s < minSize)
				minSize = s;
			return KdTreeProcedure.OK;
		}
	}
	
	private int getMinSize()
	{
		GetMinSizeProcedure gproc = new GetMinSizeProcedure();
		walk(gproc);
		int ret = gproc.minSize;
		if (logger.isLoggable(Level.FINE))
		{
			logger.fine("  search in "+gproc.searchedCells+"/"+nCells+" cells");
			logger.fine("  result: "+ret);
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

	// Methods only available in 2D
	public interface K2DInterface<T>
	{
		public long onLeft(T p0, T p1, T p2);
		public long distance2(T p0, T p1);
		public long distance2cached(T p1);
	}
	private final class K2DInvalid<T> implements K2DInterface<T>
	{
		public long onLeft(T p0, T p1, T p2) {
			throw new RuntimeException("Method onLeft only available in 2D");
		}

		public long distance2(T p0, T p1) {
			throw new RuntimeException("Method distance2 only available in 2D");
		}

		public long distance2cached(T p1) {
			throw new RuntimeException("Method distance2cached only available in 2D");
		}

	}
	private final class K2D implements K2DInterface<T>
	{
		// Temporary arrays
		private final int[] i0 = new int[2];
		private final int[] i1 = new int[2];
		private final int[] i2 = new int[2];

		public final long onLeft(T p0, T p1, T p2)
		{
			double2int(p0, i0);
			double2int(p1, i1);
			double2int(p2, i2);
			long x01 = i1[0] - i0[0];
			long y01 = i1[1] - i0[1];
			long x02 = i2[0] - i0[0];
			long y02 = i2[1] - i0[1];
			return x01 * y02 - x02 * y01;
		}

		public final long distance2(T p0, T p1)
		{
			double2int(p0, i0);
			double2int(p1, i1);
			long dx = i1[0] - i0[0];
			long dy = i1[1] - i0[1];
			return dx * dx + dy * dy;
		}
		public final long distance2cached(T p1)
		{
			double2int(p1, i1);
			long dx = i1[0] - i0[0];
			long dy = i1[1] - i0[1];
			return dx * dx + dy * dy;
		}
	}
}
