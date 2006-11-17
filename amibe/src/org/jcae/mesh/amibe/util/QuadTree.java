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
import org.jcae.mesh.amibe.ds.Vertex2D;
import org.jcae.mesh.amibe.ds.tools.Calculus;
import org.jcae.mesh.amibe.ds.tools.Calculus2D;
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
 *		public final int action(Object o, int s, int i0, int j0)
 *		{
 *			Cell self = (Cell) o;
 *			if (self.nItems > 0)
 *			{
 *				for (int i = 0; i &lt; self.nItems; i++)
 *					vertexList.add(self.subQuad[i]);
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
 * an instance of {@link org.jcae.mesh.amibe.ds.tools.Calculus2D}, and in
 * Riemannian metrics (see {@link org.jcae.mesh.amibe.metrics.Metric2D}) when
 * it is an instance of {@link org.jcae.mesh.amibe.ds.tools.Calculus3D}.
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
	protected class Cell
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
		 * in the {@link #subQuad} array.
		 */
		protected int nItems = 0;
		
		/**
		 * References to bound objects.  This variable either contains
		 * four references to children nodes (some of which may be
		 * <code>null</code>), or up to {@link #BUCKETSIZE} references
		 * yo vertices.  This compact storage is needed to reduce memory
		 * usage.
		 */
		protected Object [] subQuad = null;
	}
	
	private static Logger logger=Logger.getLogger(QuadTree.class);	
	
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
	public final double [] x0 = new double[3];
	
	// Functions to compute distance between vertices
	private Calculus compGeom;
	
	/**
	 * Create a new <code>QuadTree</code> of the desired size.
	 *
	 * @param umin  U-coordinate of the leftmost bottom vertex
	 * @param umax  U-coordinate of the rightmost top vertex
	 * @param vmin  V-coordinate of the leftmost bottom vertex
	 * @param vmax  V-coordinate of the rightmost top vertex
	 */
	public QuadTree(double umin, double umax, double vmin, double vmax)
	{
		double deltaU = 1.01 * Math.abs(umin - umax);
		double deltaV = 1.01 * Math.abs(vmin - vmax);
		assert deltaU > 0.0 && deltaV > 0.0;
		x0[0] = umin;
		x0[1] = vmin;
		x0[2] = ((double) gridSize) / Math.max(deltaU, deltaV);
		root = new Cell();
		compGeom = new Calculus2D(null);
		nCells++;
	}
	
	/**
	 * Set functions to compute distances between vertices.
	 *
	 * @param c   instance of {@link Calculus} to compute distances between vertices.
	 */
	public void setCompGeom(Calculus c)
	{
		compGeom = c;
	}
	
	/**
	 * Transform double coordinates into integer coordinates.
	 * @param p  double coordinates
	 * @param i  integer coordinates
	 */
	public void double2int(double [] p, int [] i)
	{
		i[0] = (int) ((p[0] - x0[0]) * x0[2]);
		i[1] = (int) ((p[1] - x0[1]) * x0[2]);
	}
	
	/**
	 * Transform integer coordinates into double coordinates.
	 * @param i  integer coordinates
	 * @param p  double coordinates
	 */
	public void int2double(int [] i, double [] p)
	{
		p[0] = x0[0] + i[0] / x0[2];
		p[1] = x0[1] + i[1] / x0[2];
	}
	
	/**
	 * Return the coordinates of the center of the grid.
	 * @return the coordinates of the center of the grid.
	 */
	public double [] center()
	{
		double [] p = new double[2];
		p[0] = x0[0] + ((double) gridSize) * 0.5 / x0[2];
		p[1] = x0[1] + ((double) gridSize) * 0.5 / x0[2];
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
	protected static int indexSubQuad(int i, int j, int size)
	{
		int ret = ((j & size) == 0) ? 0 : 2;
		if ((i & size) != 0)
			ret ++;
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
		int [] ij = new int[2];
		int [] oldij = new int[2];
		double2int(v.getUV(), ij);
		while (current.nItems < 0)
		{
			//  nItems is negative means that this cell only
			//  contains subcells, and its opposite is the
			//  total number of nodes found in subcells.
			current.nItems--;
			s >>= 1;
			assert s > 0;
			int ind = indexSubQuad(ij[0], ij[1], s);
			if (null == current.subQuad[ind])
			{
				current.subQuad[ind] = new Cell();
				nCells++;
			}
			current = (Cell) current.subQuad[ind];
		}
		
		//  If current box is full, split it into 4 subquads
		while (current.nItems == Cell.BUCKETSIZE)
		{
			s >>= 1;
			assert s > 0;
			Cell [] newSubQuads = new Cell[4];
			//  Move points to their respective subquadtrees.
			for (int i = 0; i < Cell.BUCKETSIZE; i++)
			{
				Vertex2D p = (Vertex2D) current.subQuad[i];
				double2int(p.getUV(), oldij);
				int ind = indexSubQuad(oldij[0], oldij[1], s);
				if (null == newSubQuads[ind])
				{
					newSubQuads[ind] = new Cell();
					nCells++;
					newSubQuads[ind].subQuad = new Vertex2D[Cell.BUCKETSIZE];
				}
				Cell target = newSubQuads[ind];
				target.subQuad[target.nItems] = current.subQuad[i];
				target.nItems++;
			}
			current.subQuad = newSubQuads;
			//  current will point to another cell, afjust it now.
			current.nItems = - Cell.BUCKETSIZE - 1;
			int ind = indexSubQuad(ij[0], ij[1], s);
			if (null == current.subQuad[ind])
			{
				current.subQuad[ind] = new Cell();
				nCells++;
			}
			current = (Cell) current.subQuad[ind];
		}
		//  Eventually insert the new point
		if (current.nItems == 0)
			current.subQuad = new Vertex2D[Cell.BUCKETSIZE];
		current.subQuad[current.nItems] = v;
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
		int [] ij = new int[2];
		double2int(v.getUV(), ij);
		while (current.nItems < 0)
		{
			//  nItems is negative
			current.nItems++;
			if (current.nItems == 0)
				last.subQuad[lastPos] = null;
			s >>= 1;
			assert s > 0;
			int ind = indexSubQuad(ij[0], ij[1], s);
			next = (Cell) current.subQuad[ind];
			if (null == next)
				throw new RuntimeException("Vertex "+v+" is not present and can not be deleted");
			last = current;
			lastPos = ind;
			current = next;
		}
		int offset = 0;
		for (int i = 0; i < current.nItems; i++)
		{
			if (v == (Vertex2D) current.subQuad[i])
				offset++;
			else if (offset > 0)
				current.subQuad[i-offset] = current.subQuad[i];
		}
		if (offset == 0)
			throw new RuntimeException("Vertex "+v+" is not present and can not be deleted");
		if (current.nItems > 1)
		{
			current.subQuad[current.nItems-1] = null;
			current.nItems--;
		}
		else
			last.subQuad[lastPos] = null;
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
	public Vertex2D getNearVertex(Vertex2D v)
	{
		Cell current = root;
		Cell last = null;
		int s = gridSize;
		int [] ij = new int[2];
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
				current.subQuad[indexSubQuad(ij[0], ij[1], s)];
		}
		if (null == current)
			return getNearVertexInSubquads(last, v, searchedCells);
		
		Vertex2D vQ = (Vertex2D) current.subQuad[0];
		Vertex2D ret = vQ;
		double retdist = compGeom.distance(v, vQ, v);
		for (int i = 1; i < current.nItems; i++)
		{
			vQ = (Vertex2D) current.subQuad[i];
			double d = compGeom.distance(v, vQ, v);
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
	
	private Vertex2D getNearVertexInSubquads(Cell current, Vertex2D v, int searchedCells)
	{
		Vertex2D ret = null;
		int [] ij = new int[2];
		double dist = -1.0;
		double2int(v.getUV(), ij);
		if (logger.isDebugEnabled())
			logger.debug("Near point in subquads: "+v);
		int l = 0;
		int [] posStack = new int[MAXLEVEL];
		posStack[l] = 0;
		Cell [] quadStack = new Cell[MAXLEVEL];
		quadStack[l] = current;
		while (true)
		{
			searchedCells++;
			if (quadStack[l].nItems < 0)
			{
				l++;
				assert l <= MAXLEVEL;
				for (int i = 0; i < 4; i++)
				{
					if (null != quadStack[l-1].subQuad[i])
					{
						quadStack[l] = (Cell) quadStack[l-1].subQuad[i];
						posStack[l] = i;
						break;
					}
				}
			}
			else
			{
				for (int i = 0; i < quadStack[l].nItems; i++)
				{
					Vertex2D vQ = (Vertex2D) quadStack[l].subQuad[i];
					double d = compGeom.distance(v, vQ, v);
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
					if (posStack[l] == 4)
						l--;
					else if (null != quadStack[l-1].subQuad[posStack[l]])
						break;
				}
				if (l == 0)
					break;
				quadStack[l] = (Cell) quadStack[l-1].subQuad[posStack[l]];
			}
		}
		if (logger.isDebugEnabled())
			logger.debug("  search in "+searchedCells+"/"+nCells+" cells");
		return ret;
	}
	
	private final class getNearestVertexProcedure implements QuadTreeProcedure
	{
		private final int [] ij = new int[2];;
		private int idist;
		private double dist, i2d;
		public Vertex2D fromVertex, nearestVertex;
		public int searchedCells = 0;
		public getNearestVertexProcedure(Vertex2D from, Vertex2D v)
		{
			double2int(from.getUV(), ij);
			nearestVertex = v;
			fromVertex = from;
			// FIXME: a factor of 1.005 is added to take rounding
			// errors into account, a better approximation should
			// be used.
			i2d = 1.005 * x0[2] * (compGeom.radius2d(fromVertex));
			dist = compGeom.distance(fromVertex, v, fromVertex);
			idist = (int) (dist * i2d);
			if (idist > Integer.MAX_VALUE/2)
				idist = Integer.MAX_VALUE/2;
		}
		public final int action(Object o, int s, int i0, int j0)
		{
			boolean valid = (ij[0] >= i0 - idist) && (ij[0] <= i0 + s + idist) &&
			                (ij[1] >= j0 - idist) && (ij[1] <= j0 + s + idist);
			if (!valid)
				return 1;
			Cell self = (Cell) o;
			searchedCells++;
			if (self.nItems > 0)
			{
				for (int i = 0; i < self.nItems; i++)
				{
					Vertex2D vtest = (Vertex2D) self.subQuad[i];
					double retdist = compGeom.distance(fromVertex, vtest, fromVertex);
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
	public Vertex2D getNearestVertex(Vertex2D v)
	{
		Cell current = root;
		Vertex2D ret = getNearVertex(v);
		assert ret != null;
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
	
	private final class getNearestVertexDebugProcedure implements QuadTreeProcedure
	{
		private final int [] ij = new int[2];;
		private double dist;
		public Vertex2D fromVertex, nearestVertex;
		public int searchedCells = 0;
		public getNearestVertexDebugProcedure(Vertex2D from, Vertex2D v)
		{
			double2int(from.getUV(), ij);
			nearestVertex = v;
			fromVertex = from;
			dist = compGeom.distance(fromVertex, v, fromVertex);
		}
		public final int action(Object o, int s, int i0, int j0)
		{
			Cell self = (Cell) o;
			searchedCells++;
			if (self.nItems > 0)
			{
				for (int i = 0; i < self.nItems; i++)
				{
					Vertex2D vtest = (Vertex2D) self.subQuad[i];
					double retdist = compGeom.distance(fromVertex, vtest, fromVertex);
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
	public Vertex2D getNearestVertexDebug(Vertex2D v)
	{
		Cell current = root;
		Vertex2D ret = getNearVertex(v);
		assert ret != null;
		if (logger.isDebugEnabled())
			logger.debug("(debug) Nearest point of "+v);
		
		getNearestVertexDebugProcedure gproc = new getNearestVertexDebugProcedure(v, ret);
		walk(gproc);
		ret = gproc.nearestVertex;
		if (logger.isDebugEnabled())
		{
			logger.debug("  search in "+gproc.searchedCells+"/"+nCells+" cells");
			logger.debug("  result: "+ret);
		}
		return ret;
	}
	
	private final class getAllVerticesProcedure implements QuadTreeProcedure
	{
		public ArrayList nodelist = null;
		public getAllVerticesProcedure(int capacity)
		{
			nodelist = new ArrayList(capacity);
		}
		public final int action(Object o, int s, int i0, int j0)
		{
			Cell self = (Cell) o;
			if (self.nItems > 0)
			{
				for (int i = 0; i < self.nItems; i++)
					nodelist.add(self.subQuad[i]);
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
		Cell current = root;
		getAllVerticesProcedure gproc = new getAllVerticesProcedure(capacity);
		walk(gproc);
		return gproc.nodelist;
	}
	
	private final class clearAllMetricsProcedure implements QuadTreeProcedure
	{
		public final int action(Object o, int s, int i0, int j0)
		{
			Cell self = (Cell) o;
			if (self.nItems > 0)
			{
				for (int i = 0; i < self.nItems; i++)
					((Vertex2D) self.subQuad[i]).clearMetrics();
			}
			return 0;
		}
	}
	
	/**
	 * Remove all metrics of vertices stored in this <code>QuadTree</code>.
	 */
	public void clearAllMetrics()
	{
		Cell current = root;
		clearAllMetricsProcedure gproc = new clearAllMetricsProcedure();
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
		int i0 = 0;
		int j0 = 0;
		int [] posStack = new int[MAXLEVEL];
		posStack[l] = 0;
		Cell [] quadStack = new Cell[MAXLEVEL];
		quadStack[l] = root;
		while (true)
		{
			int res = proc.action(quadStack[l], s, i0, j0);
			if (res == -1)
				return false;
			if (quadStack[l].nItems < 0 && res == 0)
			{
				s >>= 1;
				assert s > 0;
				l++;
				assert l <= MAXLEVEL;
				for (int i = 0; i < 4; i++)
				{
					if (null != quadStack[l-1].subQuad[i])
					{
						quadStack[l] = (Cell) quadStack[l-1].subQuad[i];
						posStack[l] = i;
						break;
					}
				}
				if (posStack[l] == 1)
					i0 += s;
				else if (posStack[l] == 2)
					j0 += s;
				else if (posStack[l] == 3)
				{
					i0 += s;
					j0 += s;
				}
			}
			else
			{
				while (l > 0)
				{
					posStack[l]++;
					if (posStack[l] == 4)
					{
						i0 -= s;
						j0 -= s;
						s <<= 1;
						l--;
					}
					else
					{
						if (posStack[l] == 1)
							i0 += s;
						else if (posStack[l] == 2)
						{
							i0 -= s;
							j0 += s;
						}
						else if (posStack[l] == 3)
							i0 += s;
						if (null != quadStack[l-1].subQuad[posStack[l]])
							break;
					}
				}
				if (l == 0)
					break;
				quadStack[l] = (Cell) quadStack[l-1].subQuad[posStack[l]];
			}
		}
		assert i0 == 0;
		assert j0 == 0;
		return true;
	}
	
}
