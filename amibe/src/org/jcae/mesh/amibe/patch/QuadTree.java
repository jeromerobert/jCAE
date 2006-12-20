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

package org.jcae.mesh.amibe.patch;

import org.apache.log4j.Logger;
import java.util.ArrayList;

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
 * {@link QuadTreeProcedure#action(Object, int, int, int)} method.
 * This design had been chosen for performance reasons on large meshes, and in
 * practice it works very well because quadtrees are used for vertex location,
 * and no neighbourhood information is needed.
 * </p>
 *
 * <p>
 * Quadtree traversal is performed by the {@link #walk(QuadTreeProcedure)} method.
 * Here is an example to collect all vertices in a list:
 * </p>
 * <pre>
 *	public final class collectAllVerticesProcedure implements QuadTreeProcedure
 *	{
 *		public ArrayList vertexList = new ArrayList();
 *		public final int action(Object o, int s, int [] i0)
 *		{
 *			Cell self = (Cell) o;
 *			if (self.nItems > 0)
 *			{
 *				for (int i = 0; i &lt; self.nItems; i++)
 *					vertexList.add(self.subCell[i]);
 *			}
 *			return 0;
 *		}
 *	}
 * </pre>
 * <p>
 * This procedure is applied on all cells recursively in prefix order.  If it
 * returns <code>-1</code>, {@link #walk(QuadTreeProcedure)} aborts its
 * processing immediately.  A null return value means that processing can
 * continue normally, and a non-null return value means that children nodes are
 * skipped.
 * </p>
 *
 * <p>
 * Distances between vertices can be computed either in Euclidian 2D space, or
 * with a Riemannian metrics.  This is controlled by the {@link #setCompGeom(Calculus)}
 * method.  Distances are computed in Euclidian 2D space when its argument is
 * an instance of {@link Calculus2D}, and in
 * Riemannian metrics (see {@link org.jcae.mesh.amibe.metrics.Metric2D}) when
 * it is an instance of {@link Calculus3D}.
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
 * The implementation of {@link #getNearestVertex(Vertex2D)} has two differences:
 * </p>
 * <ul>
 *   <li>The starting point is computed by {@link #getNearVertex(Vertex2D)}.  This
 *       means that much more cells are skipped.</li>
 *   <li>The ellipsis is replaced by a circle enclosing it, to have simpler
 *       calculus.  Using the real ellipsis could be tested though, it should
 *       also speed up this processing.</li>
 * </ul>
 */
public class QuadTree
{
	/**
	 * Cell of a {@link QuadTree}.  Each cell contains either four children nodes
	 * or up to <code>BUCKETSIZE</code> vertices.  When this number is exceeded,
	 * the cell is splitted and vertices are moved to these children.
	 * On the contrary, when all vertices are removed from a cell, it is deleted.
	 * And when all children of a cell are null, this cell is removed.
	 */
	protected static class Cell
	{
		/**
		 * Maximal number of vertices which can be stored in a cell.
		 * This number must be at least 4, because children nodes are
		 * stored in the same place as vertices, and a cell can have at
		 * most 4 children.  Its value is 10.
		 */
		protected static final int BUCKETSIZE = 10;
		
		/**
		 * Number of vertices stored below the current cell.  If this cell
		 * has children nodes, this value is negative and its opposite
		 * value is the total number of vertices found in children nodes.
		 * Otherwise, it contains the number of vertices which are stored
		 * in the {@link #subCell} array.
		 */
		protected int nItems = 0;
		
		/**
		 * References to bound objects.  This variable either contains
		 * four references to children nodes (some of which may be
		 * <code>null</code>), or up to {@link #BUCKETSIZE} references
		 * yo vertices.  This compact storage is needed to reduce memory
		 * usage.
		 */
		protected Object [] subCell = null;
	}
	
	private static Logger logger=Logger.getLogger(QuadTree.class);	
	
	private final int dimension = 2;
	private final int nrSub = 1 << dimension;

	// Integer coordinates (like gridSize) must be long if MAXLEVEL > 30
	private static final int MAXLEVEL = 30;
	private static final int gridSize = 1 << MAXLEVEL;
	
	/**
	 * Root of the quadtree.
	 */
	public Cell root;
	
	/**
	 * Number of cells.
	 */
	public int nCells = 0;
	
	/**
	 * Conversion between double and integer coordinates.
	 */
	public final double [] x0;
	
	/**
	 * Create a new <code>QuadTree</code> of the desired size.
	 *
	 * @param umin  U-coordinate of the leftmost bottom vertex
	 * @param umax  U-coordinate of the rightmost top vertex
	 * @param vmin  V-coordinate of the leftmost bottom vertex
	 * @param vmax  V-coordinate of the rightmost top vertex
	 */
	public QuadTree(double [] bbmin, double [] bbmax)
	{
		x0 = new double[dimension+1];
		double maxDelta = 0.0;
		for (int i = 0; i < dimension; i++)
		{
			x0[i] = bbmin[i];
			double delta = Math.abs(bbmax[i] - bbmin[i]);
			if (delta > maxDelta)
				maxDelta = delta;
		}
		maxDelta *= 1.01;
		x0[dimension] = ((double) gridSize) / maxDelta;
		root = new Cell();
		nCells++;
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
			p[k] = x0[k] + ((double) gridSize) * 0.5 / x0[dimension];
		return p;
	}
	
	/**
	 * Return the index of the child node containing a given point.
	 * A quadtree cell contains at most 4 children.  Cell size is a power of
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
	 * @param i     first coordinate of a vertex.
	 * @param j     second coordinate of a vertex.
	 * @param size  cell size of children nodes.
	 * @return the index of the child node containing this vertex.
	 */
	private int indexSubQuad(int [] ijk, int size)
	{
		int ret = 0;
		if (size == 0)
			throw new RuntimeException("Exceeded maximal number of levels for quadtrees... Aborting");
		for (int k = 0; k < dimension; k++)
		{
			if ((ijk[k] & size) != 0)
				ret |= 1 << k;
		}
		return ret;
	}
	
	/**
	 * Add a vertex to the quadtree.
	 *
	 * @param v  the vertex being added.
	 */
	public void add(Vertex2D v)
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
			int ind = indexSubQuad(ij, s);
			if (null == current.subCell[ind])
			{
				current.subCell[ind] = new Cell();
				nCells++;
			}
			current = (Cell) current.subCell[ind];
		}
		
		//  If current box is full, split it into 4 subquads
		while (current.nItems == Cell.BUCKETSIZE)
		{
			s >>= 1;
			assert s > 0;
			Cell [] newSubQuads = new Cell[nrSub];
			//  Move points to their respective subquadtrees.
			for (int i = 0; i < Cell.BUCKETSIZE; i++)
			{
				Vertex2D p = (Vertex2D) current.subCell[i];
				double2int(p.getUV(), oldij);
				int ind = indexSubQuad(oldij, s);
				if (null == newSubQuads[ind])
				{
					newSubQuads[ind] = new Cell();
					nCells++;
					newSubQuads[ind].subCell = new Vertex2D[Cell.BUCKETSIZE];
				}
				Cell target = newSubQuads[ind];
				target.subCell[target.nItems] = current.subCell[i];
				target.nItems++;
			}
			current.subCell = newSubQuads;
			//  current will point to another cell, afjust it now.
			current.nItems = - Cell.BUCKETSIZE - 1;
			int ind = indexSubQuad(ij, s);
			if (null == current.subCell[ind])
			{
				current.subCell[ind] = new Cell();
				nCells++;
			}
			current = (Cell) current.subCell[ind];
		}
		//  Eventually insert the new point
		if (current.nItems == 0)
			current.subCell = new Vertex2D[Cell.BUCKETSIZE];
		current.subCell[current.nItems] = v;
		current.nItems++;
	}
	
	/**
	 * Remove a vertex from the quadtree.
	 *
	 * @param v  the vertex being removed.
	 */
	public void remove(Vertex2D v)
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
			int ind = indexSubQuad(ij, s);
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
			if (v == (Vertex2D) current.subCell[i])
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
	
	/**
	 * Return a stored element of the <code>QuadTree</code> which is
	 * near from a given vertex.  The algorithm is simplistic: the leaf
	 * which would contain this vertex is retrieved.  If it contains
	 * vertices, the nearest one is returned (vertices in other leaves may
	 * of course be nearer).  Otherwise the nearest vertex from sibling
	 * children is returned.  The returned vertex is a good starting point
	 * for {@link #getNearestVertex(Vertex2D)}.
	 *
	 * @param v  the vertex to check.
	 * @return a near vertex.
	 */
	public Vertex2D getNearVertex(Mesh2D mesh, Vertex2D v)
	{
		Cell current = root;
		Cell last = null;
		int s = gridSize;
		int [] ij = new int[dimension];
		double2int(v.getUV(), ij);
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
				current.subCell[indexSubQuad(ij, s)];
		}
		if (null == current)
			return getNearVertexInSubquads(last, mesh, v, searchedCells);
		
		Vertex2D vQ = (Vertex2D) current.subCell[0];
		Vertex2D ret = vQ;
		double retdist = mesh.compGeom().distance(v, vQ, v);
		for (int i = 1; i < current.nItems; i++)
		{
			vQ = (Vertex2D) current.subCell[i];
			double d = mesh.compGeom().distance(v, vQ, v);
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
	
	private Vertex2D getNearVertexInSubquads(Cell current, Mesh2D mesh, Vertex2D v, int searchedCells)
	{
		Vertex2D ret = null;
		int [] ij = new int[dimension];
		double dist = -1.0;
		double2int(v.getUV(), ij);
		if (logger.isDebugEnabled())
			logger.debug("Near point in subquads: "+v);
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
				for (int i = 0; i < nrSub; i++)
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
					Vertex2D vQ = (Vertex2D) cellStack[l].subCell[i];
					double d = mesh.compGeom().distance(v, vQ, v);
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
					if (posStack[l] == nrSub)
						l--;
					else if (null != cellStack[l-1].subCell[posStack[l]])
						break;
				}
				if (l == 0)
					break;
				cellStack[l] = (Cell) cellStack[l-1].subCell[posStack[l]];
			}
		}
		if (logger.isDebugEnabled())
			logger.debug("  search in "+searchedCells+"/"+nCells+" cells");
		return ret;
	}
	
	private final class GetNearestVertexProcedure implements QuadTreeProcedure
	{
		private final int [] ij = new int[dimension];;
		private final Mesh2D mesh;
		private int idist;
		private double dist, i2d;
		public final Vertex2D fromVertex;
		public Vertex2D nearestVertex;
		public int searchedCells = 0;
		public GetNearestVertexProcedure(Mesh2D m, Vertex2D from, Vertex2D v)
		{
			double2int(from.getUV(), ij);
			nearestVertex = v;
			fromVertex = from;
			mesh = m;
			// FIXME: a factor of 1.005 is added to take rounding
			// errors into account, a better approximation should
			// be used.
			i2d = 1.005 * x0[dimension] * (mesh.compGeom().radius2d(fromVertex));
			dist = mesh.compGeom().distance(fromVertex, v, fromVertex);
			idist = (int) (dist * i2d);
			if (idist > Integer.MAX_VALUE/2)
				idist = Integer.MAX_VALUE/2;
		}
		public final int action(Object o, int s, final int [] i0)
		{
			for (int k = 0; k < dimension; k++)
				if ((ij[k] < i0[k] - idist) || (ij[k] > i0[k] + s + idist))
					return 1;
			Cell self = (Cell) o;
			searchedCells++;
			if (self.nItems > 0)
			{
				for (int i = 0; i < self.nItems; i++)
				{
					Vertex2D vtest = (Vertex2D) self.subCell[i];
					double retdist = mesh.compGeom().distance(fromVertex, vtest, fromVertex);
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
	 * Return the nearest vertex stored in this <code>QuadTree</code>.
	 * Computing distance to all vertices in the quadtree would be very
	 * time consuming.  To speed up processing, whole quadtree cells are
	 * ignored if their distance to the vertex is greater than the current
	 * minimum.  The {@link #getNearVertex(Vertex2D)} method is used to find the
	 * initial minimum.  It is very fast and provides a good candidate,
	 * so that the ratio of quadtree cells visited over the number of
	 * quadtree cells is very low.
	 *
	 * @param v  the vertex to check.
	 * @return the nearest vertex.
	 */
	public Vertex2D getNearestVertex(Mesh2D mesh, Vertex2D v)
	{
		Vertex2D ret = getNearVertex(mesh, v);
		assert ret != null;
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
	
	private final class GetNearestVertexDebugProcedure implements QuadTreeProcedure
	{
		private final int [] ij = new int[dimension];;
		private double dist;
		public final Vertex2D fromVertex;
		public Vertex2D nearestVertex;
		public final Mesh2D mesh;
		public int searchedCells = 0;
		public GetNearestVertexDebugProcedure(Mesh2D m, Vertex2D from, Vertex2D v)
		{
			double2int(from.getUV(), ij);
			nearestVertex = v;
			fromVertex = from;
			mesh = m;
			dist = mesh.compGeom().distance(fromVertex, v, fromVertex);
		}
		public final int action(Object o, int s, final int [] i0)
		{
			Cell self = (Cell) o;
			searchedCells++;
			if (self.nItems > 0)
			{
				for (int i = 0; i < self.nItems; i++)
				{
					Vertex2D vtest = (Vertex2D) self.subCell[i];
					double retdist = mesh.compGeom().distance(fromVertex, vtest, fromVertex);
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
	 * Slow implementation of {@link #getNearestVertex(Vertex2D)}.
	 * This method should be called only for debugging purpose.
	 *
	 * @param v  the vertex to check.
	 * @return the nearest vertex.
	 */
	public Vertex2D getNearestVertexDebug(Mesh2D mesh, Vertex2D v)
	{
		Vertex2D ret = getNearVertex(mesh, v);
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
	
	private static class GetAllVerticesProcedure implements QuadTreeProcedure
	{
		public ArrayList nodelist = null;
		public GetAllVerticesProcedure(int capacity)
		{
			nodelist = new ArrayList(capacity);
		}
		public final int action(Object o, int s, final int [] i0)
		{
			Cell self = (Cell) o;
			if (self.nItems > 0)
			{
				for (int i = 0; i < self.nItems; i++)
					nodelist.add(self.subCell[i]);
			}
			return 0;
		}
	}
	
	/**
	 * Return a list of all vertices.
	 *
	 * @param capacity  initial capacity of the <code>ArrayList</code>.
	 * @return a list containing all vertices.
	 */
	public ArrayList getAllVertices(int capacity)
	{
		GetAllVerticesProcedure gproc = new GetAllVerticesProcedure(capacity);
		walk(gproc);
		return gproc.nodelist;
	}
	
	private static class ClearAllMetricsProcedure implements QuadTreeProcedure
	{
		public final int action(Object o, int s, final int [] i0)
		{
			Cell self = (Cell) o;
			if (self.nItems > 0)
			{
				for (int i = 0; i < self.nItems; i++)
					((Vertex2D) self.subCell[i]).clearMetrics();
			}
			return 0;
		}
	}
	
	/**
	 * Remove all metrics of vertices stored in this <code>QuadTree</code>.
	 */
	public void clearAllMetrics()
	{
		ClearAllMetricsProcedure gproc = new ClearAllMetricsProcedure();
		walk(gproc);
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
	 * @see QuadTreeProcedure
	 */
	public final boolean walk(QuadTreeProcedure proc)
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
							else
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
	
}
