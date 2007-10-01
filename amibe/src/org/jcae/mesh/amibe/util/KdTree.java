/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005, by EADS CRC
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

package org.jcae.mesh.amibe.util;

import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.ds.AbstractVertex;
import org.jcae.mesh.amibe.ds.Mesh;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.log4j.Logger;

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
 * {@link Cell#BUCKETSIZE} vertices (default is 10).  When this number
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
 *		public Collection<AbstractVertex> vertexList = new ArrayList<AbstractVertex>();
 *		public final int action(Object o, int s, int [] i0)
 *		{
 *			Cell self = (Cell) o;
 *			if (self.nItems > 0)
 *			{
 *				for (int i = 0; i &lt; self.nItems; i++)
 *					vertexList.add((Vertex) self.subCell[i]);
 *			}
 *			return 0;
 *		}
 *	}
 * </pre>
 * <p>
 * This procedure is applied on all cells recursively in prefix order.  If it
 * returns <code>-1</code>, {@link #walk(KdTreeProcedure)} aborts its
 * processing immediately.  A null return value means that processing can
 * continue normally, and a non-null return value means that children nodes are
 * skipped.
 * </p>
 *
 * <p>
 * Distances between vertices can be computed either in Euclidian 2D space, or
 * with a Riemannian metrics.  This is controlled by the
 * {@link org.jcae.mesh.amibe.patch.Mesh2D#pushCompGeom(int)} method.
 * Distances are computed in Euclidian 2D space when its argument is
 * an instance of {@link org.jcae.mesh.amibe.patch.Calculus2D}, and in
 * Riemannian metrics (see {@link org.jcae.mesh.amibe.metrics.Metric2D}) when
 * it is an instance of {@link org.jcae.mesh.amibe.patch.Calculus3D}.
 * By default, distances are computed in Euclidian 2D space.
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
 * The implementation of {@link #getNearestVertex(Mesh, Vertex)} has two
 * differences:
 * </p>
 * <ul>
 *   <li>The starting point is computed by {@link #getNearVertex(Mesh, Vertex)}.
 *       This means that much more cells are skipped.</li>
 *   <li>The ellipsis is replaced by a circle enclosing it, to have simpler
 *       calculus.  Using the real ellipsis could be tested though, it should
 *       also speed up this processing.</li>
 * </ul>
 */
public class KdTree
{
	private static Logger logger=Logger.getLogger(KdTree.class);	
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
		private int nItems = 0;
		
		/**
		 * References to bound objects.  This variable either contains
		 * four references to children nodes (some of which may be
		 * <code>null</code>), or up to {@link #BUCKETSIZE} references
		 * yo vertices.  This compact storage is needed to reduce memory
		 * usage.
		 */
		private Object [] subCell = null;

		public Cell getSubCell(int [] ijk, int size)
		{
			return (Cell) subCell[indexSubCell(ijk, size)];
		}

		public boolean isLeaf()
		{
			return nItems >= 0;
		}

		public int count()
		{
			if (nItems >= 0)
				return nItems;
			return -nItems;
		}

		public Vertex getVertex(int i)
		{
			assert nItems > 0 && i < nItems;
			return (Vertex) subCell[i];
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
	protected final Cell root;
	
	/**
	 * Number of cells.
	 */
	public int nCells = 0;
	
	/**
	 * Conversion between double and integer coordinates.
	 */
	public final double [] x0;
	
	/**
	 * Create a new <code>KdTree</code> of the desired size.
	 *
	 * @param d   dimension (2 or 3)
	 * @param bbmin  coordinates of bottom-left vertex
	 * @param bbmax  coordinates of top-right vertex
	 */
	public KdTree(int d, double [] bbmin, double [] bbmax)
	{
		BUCKETSIZE = 10;
		dimension = d;
		nrSub = 1 << dimension;
		x0 = new double[dimension+1];
		root = new Cell();
		nCells++;
		setup(bbmin, bbmax);
	}
	
	/**
	 * Create a new <code>KdTree</code> of the desired size.
	 *
	 * @param d   dimension (2 or 3)
	 * @param bbmin  coordinates of bottom-left vertex
	 * @param bbmax  coordinates of top-right vertex
	 * @param bucketsize  bucket size
	 */
	public KdTree(int d, double [] bbmin, double [] bbmax, int bucketsize)
	{
		BUCKETSIZE = bucketsize;
		dimension = d;
		nrSub = 1 << dimension;
		x0 = new double[dimension+1];
		root = new Cell();
		nCells++;
		setup(bbmin, bbmax);
	}
	
	private final void setup(double [] bbmin, double [] bbmax)
	{
		double maxDelta = 0.0;
		for (int i = 0; i < dimension; i++)
		{
			x0[i] = bbmin[i];
			double delta = Math.abs(bbmax[i] - bbmin[i]);
			if (delta > maxDelta)
				maxDelta = delta;
		}
		maxDelta *= 1.01;
		x0[dimension] = DGridSize / maxDelta;
	}
	
	/**
	 * Transform double coordinates into integer coordinates.
	 * @param p  double coordinates
	 * @param i  integer coordinates
	 */
	public void double2int(double [] p, int [] i)
	{
		for (int k = 0; k < dimension; k++)
			i[k] = (int) ((p[k] - x0[k]) * x0[dimension]);
	}
	
	/**
	 * Transform integer coordinates into double coordinates.
	 * @param i  integer coordinates
	 * @param p  double coordinates
	 */
	public void int2double(int [] i, double [] p)
	{
		for (int k = 0; k < dimension; k++)
			p[k] = x0[k] + i[k] / x0[dimension];
	}
	
	/**
	 * Return the coordinates of the center of the grid.
	 * @return the coordinates of the center of the grid.
	 */
	public double [] center()
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
			throw new RuntimeException("Exceeded maximal number of levels for kd-trees... Aborting");
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
	 */
	public void add(Vertex v)
	{
		Cell current = root;
		int s = gridSize;
		int [] ij = new int[dimension];
		int [] oldij = new int[dimension];
		double2int(v.getUV(), ij);
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
			Cell [] newSubQuads = new Cell[nrSub];
			//  Move points to their respective subcells.
			for (int i = 0; i < BUCKETSIZE; i++)
			{
				Vertex p = (Vertex) current.subCell[i];
				double2int(p.getUV(), oldij);
				int ind = indexSubCell(oldij, s);
				if (null == newSubQuads[ind])
				{
					newSubQuads[ind] = new Cell();
					nCells++;
					newSubQuads[ind].subCell = new Vertex[BUCKETSIZE];
				}
				Cell target = newSubQuads[ind];
				target.subCell[target.nItems] = current.subCell[i];
				target.nItems++;
			}
			current.subCell = newSubQuads;
			//  current will point to another cell, afjust it now.
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
			current.subCell = new Vertex[BUCKETSIZE];
		current.subCell[current.nItems] = v;
		current.nItems++;
	}
	
	/**
	 * Remove a vertex from the kd-tree.
	 *
	 * @param v  the vertex being removed.
	 */
	public void remove(Vertex v)
	{
		Cell current = root;
		Cell last = root;
		Cell next;
		int lastPos = 0;
		int s = gridSize;
		int [] ij = new int[dimension];
		double2int(v.getUV(), ij);
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

	private static final class GetAllVerticesProcedure implements KdTreeProcedure
	{
		public Collection<AbstractVertex> nodelist = null;
		public GetAllVerticesProcedure(int capacity)
		{
			nodelist = new ArrayList<AbstractVertex>(capacity);
		}
		public int action(Object o, int s, final int [] i0)
		{
			Cell self = (Cell) o;
			if (self.nItems > 0)
			{
				for (int i = 0; i < self.nItems; i++)
					nodelist.add((AbstractVertex) self.subCell[i]);
			}
			return 0;
		}
	}
	
	/**
	 * Return a collection of all vertices.
	 *
	 * @param capacity  initial capacity of the <code>Collection</code>.
	 * @return a collection containing all vertices.
	 */
	public Collection<AbstractVertex> getAllVertices(int capacity)
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
		Cell [] cellStack = new Cell[MAXLEVEL];
		cellStack[l] = root;
		while (true)
		{
			int res = proc.action(cellStack[l], s, i0);
			if (res == -1)
				return false;
			if (cellStack[l].nItems < 0 && res == 0)
			{
				s >>= 1;
				assert s > 0;
				l++;
				assert l <= MAXLEVEL;
				for (int i = 0; i < nrSub; i++)
				{
					if (null != cellStack[l-1].subCell[i])
					{
						cellStack[l] = (Cell) cellStack[l-1].subCell[i];
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
						if (null != cellStack[l-1].subCell[posStack[l]])
							break;
					}
				}
				if (l == 0)
					break;
				cellStack[l] = (Cell) cellStack[l-1].subCell[posStack[l]];
			}
		}
		return true;
	}
	
	/**
	 * Return a stored element of the <code>Octree</code> which is
	 * near from a given vertex.  The algorithm is simplistic: the leaf
	 * which would contains this node is retrieved.  If it contains
	 * vertices, the nearest one is returned (vertices in other leaves may
	 * of course be nearer).  Otherwise the nearest vertex from sibling
	 * children is returned.  The returned vertex is a good starting point
	 * for {@link #getNearestVertex(Mesh, Vertex)}.
	 *
	 * @param v  the node to check.
	 * @return a near vertex.
	 */
	public final Vertex getNearVertex(Mesh mesh, Vertex v)
	{
		Cell current = root;
		if (current.nItems == 0)
			return null;
		Cell last = null;
		int s = gridSize;
		int [] ijk = new int[dimension];
		double2int(v.getUV(), ijk);
		int searchedCells = 0;
		if (logger.isDebugEnabled())
			logger.debug("Near point: "+v);
		while (null != current && current.nItems < 0)
		{
			last = current;
			s >>= 1;
			assert s > 0;
			searchedCells++;
			current = (Cell) current.subCell[indexSubCell(ijk, s)];
		}
		if (null == current)
			return getNearVertexInSubCells(last, mesh, v, searchedCells);
		
		Vertex vQ = (Vertex) current.subCell[0];
		Vertex ret = vQ;
		double retdist = mesh.distance2(v, vQ, v);
		for (int i = 1; i < current.nItems; i++)
		{
			vQ = (Vertex) current.subCell[i];
			double d = mesh.distance2(v, vQ, v);
			if (d < retdist)
			{
				retdist = d;
				ret = vQ;
			}
		}
		if (logger.isDebugEnabled())
			logger.debug("  search in "+searchedCells+"/"+nCells+" cells");
		return ret;
	}
	
	private final Vertex getNearVertexInSubCells(Cell current, Mesh mesh, Vertex v, int searchedCells)
	{
		Vertex ret = null;
		int [] ijk = new int[dimension];
		double dist = -1.0;
		double2int(v.getUV(), ijk);
		if (logger.isDebugEnabled())
			logger.debug("Near point in suboctrees: "+v);
		int l = 0;
		int [] posStack = new int[MAXLEVEL];
		posStack[l] = 0;
		Cell [] cellStack = new Cell[MAXLEVEL];
		cellStack[l] = current;
		while (true)
		{
			searchedCells++;
			if (cellStack[l].nItems < 0)
			{
				l++;
				assert l <= MAXLEVEL;
				for (int i = 0; i < 8; i++)
				{
					if (null != cellStack[l-1].subCell[i])
					{
						cellStack[l] = (Cell) cellStack[l-1].subCell[i];
						posStack[l] = i;
						break;
					}
				}
			}
			else
			{
				for (int i = 0; i < cellStack[l].nItems; i++)
				{
					Vertex vQ = (Vertex) cellStack[l].subCell[i];
					double d = mesh.distance2(v, vQ, v);
					if (d < dist || dist < 0.0)
					{
						dist = d;
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
					else if (null != cellStack[l-1].subCell[posStack[l]])
						break;
				}
				if (l == 0)
					break;
				cellStack[l] = (Cell) cellStack[l-1].subCell[posStack[l]];
			}
		}
		throw new RuntimeException("Near vertex not found");
	}
	
	private final class GetNearestVertexProcedure implements KdTreeProcedure
	{
		private final int [] ijk = new int[dimension];
		private final double i2d;
		private final Mesh mesh;
		public final Vertex fromVertex;
		public Vertex nearestVertex;
		private int idist;
		private double dist;
		public int searchedCells = 0;
		public GetNearestVertexProcedure(Mesh m, Vertex from, Vertex v)
		{
			mesh = m;
			double2int(from.getUV(), ijk);
			nearestVertex = v;
			fromVertex = from;
			dist = mesh.distance2(v, from, v);
			i2d = 1.005 * x0[dimension] * mesh.radius2d(fromVertex);
			dist = mesh.distance(fromVertex, nearestVertex, fromVertex);
			idist = (int) (dist * i2d);
			if (idist > Integer.MAX_VALUE/2)
				idist = Integer.MAX_VALUE/2;
		}
		public int action(Object o, int s, final int [] i0)
		{
			for (int k = 0; k < dimension; k++)
				if ((ijk[k] < i0[k] - idist) || (ijk[k] > i0[k] + s + idist))
					return 1;
			Cell self = (Cell) o;
			searchedCells++;
			if (self.nItems > 0)
			{
				for (int i = 0; i < self.nItems; i++)
				{
					Vertex vtest = (Vertex) self.subCell[i];
					double retdist = mesh.distance(fromVertex, vtest, fromVertex);
					if (retdist < dist)
					{
						dist = retdist;
						nearestVertex = vtest;
						idist = (int) (dist * i2d);
						if (idist > Integer.MAX_VALUE/2)
							idist = Integer.MAX_VALUE/2;
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
	public final Vertex getNearestVertex(Mesh mesh, Vertex v)
	{
		Vertex ret = getNearVertex(mesh, v);
		if (ret == null)
			return null;
		if (logger.isDebugEnabled())
			logger.debug("Nearest point of "+v);
		
		GetNearestVertexProcedure gproc = new GetNearestVertexProcedure(mesh, v, ret);
		walk(gproc);
		ret = gproc.nearestVertex;
		if (logger.isDebugEnabled())
		{
			logger.debug("  search in "+gproc.searchedCells+"/"+nCells+" cells");
			logger.debug("  result: "+ret);
		}
		return ret;
	}
	
	private final class GetNearestVertexDebugProcedure implements KdTreeProcedure
	{
		private final int [] ij = new int[dimension];
		private double dist;
		public final Vertex fromVertex;
		public Vertex nearestVertex;
		public final Mesh mesh;
		public int searchedCells = 0;
		public GetNearestVertexDebugProcedure(Mesh m, Vertex from, Vertex v)
		{
			double2int(from.getUV(), ij);
			nearestVertex = v;
			fromVertex = from;
			mesh = m;
			dist = mesh.distance(fromVertex, v, fromVertex);
		}
		public int action(Object o, int s, final int [] i0)
		{
			Cell self = (Cell) o;
			searchedCells++;
			if (self.nItems > 0)
			{
				for (int i = 0; i < self.nItems; i++)
				{
					Vertex vtest = (Vertex) self.subCell[i];
					double retdist = mesh.distance(fromVertex, vtest, fromVertex);
					if (retdist < dist)
					{
						dist = retdist;
						nearestVertex = vtest;
					}
				}
			}
			return 0;
		}
	}
	
	/**
	 * Slow implementation of {@link #getNearestVertex(Mesh, Vertex)}.
	 * This method should be called only for debugging purpose.
	 *
	 * @param v  the vertex to check.
	 * @return the nearest vertex.
	 */
	public Vertex getNearestVertexDebug(Mesh mesh, Vertex v)
	{
		Vertex ret = getNearVertex(mesh, v);
		assert ret != null;
		if (logger.isDebugEnabled())
			logger.debug("(debug) Nearest point of "+v);
		
		GetNearestVertexDebugProcedure gproc = new GetNearestVertexDebugProcedure(mesh, v, ret);
		walk(gproc);
		ret = gproc.nearestVertex;
		if (logger.isDebugEnabled())
		{
			logger.debug("  search in "+gproc.searchedCells+"/"+nCells+" cells");
			logger.debug("  result: "+ret);
		}
		return ret;
	}
	
	private static final class GetMinSizeProcedure implements KdTreeProcedure
	{
		public int searchedCells = 0;
		public int minSize = gridSize;
		public GetMinSizeProcedure()
		{
		}
		public int action(Object o, int s, final int [] i0)
		{
			searchedCells++;
			if (s < minSize)
				minSize = s;
			return 0;
		}
	}
	
	private final int getMinSize()
	{
		GetMinSizeProcedure gproc = new GetMinSizeProcedure();
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
	
}
