/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2004,2005
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

package org.jcae.mesh.amibe.util;

import org.apache.log4j.Logger;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.ds.tools.Calculus;
import java.util.ArrayList;

/**
 * Quadtree structure to store 2D vertices.  Integer coordinates are used
 * for two reasons: a better control on accuracy of geometrical operations,
 * and simpler operations on vertex location because cells have power of two
 * side length and bitwise operators can be used instead of floating point
 * operations.
 * The downside is that the conversion between double and integer coordinates
 * must be known by advance, which is why constructor needs a bounding box
 * as argument.
 */
public class QuadTree
{
	protected class QuadTreeCell
	{
		public static final int BUCKETSIZE = 10;
		//  nItems can either store the number of items in the current cell,
		//  or the total number of vertices below this cell.
		//  In the latter case, nItems can be of type byte
		//  (if BUCKETSIZE < 128) or short.
		protected int nItems = 0;
		//  subQuad contains either 4 subquadtrees or up to BUCKETSIZE
		//  vertices.  This compact storage is needed to reduce memory
		//  usage.
		protected Object [] subQuad = null;
	}
	
	private static Logger logger=Logger.getLogger(QuadTree.class);	
	
	// Integer coordinates (like gridSize) must be long if MAXLEVEL > 30
	public static final int MAXLEVEL = 30;
	public static final int gridSize = 1 << MAXLEVEL;
	
	/**
	 * Root of the quadtree.
	 */
	public QuadTreeCell root;
	
	/**
	 * Number of cells.
	 */
	public int nCells = 1;
	
	/**
	 * Conversion between double and integer coordinates.
	 */
	public final double [] x0 = new double[3];
	
	private Mesh mesh;
	
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
		root = new QuadTreeCell();
		nCells++;
	}
	
	/**
	 * Bind a {@link Mesh} instance to this quadtree.
	 *
	 * @param m  mesh
	 */
	public void bindMesh(Mesh m)
	{
		mesh = m;
		m.quadtree = this;
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
	 * A quadtree node contains at most 4 children.  Cell size is a power of
	 * two, so locating a vertex can be performed by bitwise operators, as
	 * shown below.
	 * <pre>
	 *      ┌───┬───┐
	 *  &lt;>0 │ 2 │ 3 │   with I = i &amp; size
	 *      ├───┼───┤    J = j &amp; size
	 *  J=0 │ 0 │ 1 │ 
	 *      └───┴───┘ 
	 *      I=0  &lt;>0
	 * </pre>
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
	public void add(Vertex v)
	{
		QuadTreeCell current = root;
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
				current.subQuad[ind] = new QuadTreeCell();
				nCells++;
			}
			current = (QuadTreeCell) current.subQuad[ind];
		}
		
		//  If current box is full, split it into 4 subquads
		while (current.nItems == QuadTreeCell.BUCKETSIZE)
		{
			s >>= 1;
			assert s > 0;
			QuadTreeCell [] newSubQuads = new QuadTreeCell[4];
			//  Move points to their respective subquadtrees.
			for (int i = 0; i < QuadTreeCell.BUCKETSIZE; i++)
			{
				Vertex p = (Vertex) current.subQuad[i];
				double2int(p.getUV(), oldij);
				int ind = indexSubQuad(oldij[0], oldij[1], s);
				if (null == newSubQuads[ind])
				{
					newSubQuads[ind] = new QuadTreeCell();
					nCells++;
					newSubQuads[ind].subQuad = new Vertex[QuadTreeCell.BUCKETSIZE];
				}
				QuadTreeCell target = newSubQuads[ind];
				target.subQuad[target.nItems] = current.subQuad[i];
				target.nItems++;
			}
			current.subQuad = newSubQuads;
			//  current will point to another cell, afjust it now.
			current.nItems = - QuadTreeCell.BUCKETSIZE - 1;
			int ind = indexSubQuad(ij[0], ij[1], s);
			if (null == current.subQuad[ind])
			{
				current.subQuad[ind] = new QuadTreeCell();
				nCells++;
			}
			current = (QuadTreeCell) current.subQuad[ind];
		}
		//  Eventually insert the new point
		if (current.nItems == 0)
			current.subQuad = new Vertex[QuadTreeCell.BUCKETSIZE];
		current.subQuad[current.nItems] = v;
		current.nItems++;
	}
	
	/**
	 * Remove a vertex from the quadtree.
	 *
	 * @param v  the vertex being removed.
	 */
	public void remove(Vertex v)
	{
		QuadTreeCell current = root;
		QuadTreeCell last = root;
		QuadTreeCell next;
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
			next = (QuadTreeCell) current.subQuad[ind];
			if (null == next)
				throw new RuntimeException("Vertex "+v+" is not present and can not be deleted");
			last = current;
			lastPos = ind;
			current = next;
		}
		int offset = 0;
		for (int i = 0; i < current.nItems; i++)
		{
			if (v == (Vertex) current.subQuad[i])
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
	 * near from a given vertex.  The algorithm is simplistic: the leaf which
	 * would contains this node is retrieved.  If it contains vertices, the
	 * nearest one is returned (vertices in other leaves may of course be
	 * nearer).  Otherwise the nearest vertex from sibling children is
	 * returned.  The returned vertex is a good starting point for
	 * {@link #getNearestVertex}.
	 *
	 * @param v  the node to check.
	 * @return a near vertex.
	 */
	public Vertex getNearVertex(Vertex v)
	{
		QuadTreeCell current = root;
		QuadTreeCell last = null;
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
			current = (QuadTreeCell)
				current.subQuad[indexSubQuad(ij[0], ij[1], s)];
		}
		if (null == current)
			return getNearVertexInSubquads(last, v, searchedCells);
		
		Vertex vQ = (Vertex) current.subQuad[0];
		Vertex ret = vQ;
		double retdist = mesh.compGeom().distance(v, vQ, v);
		for (int i = 1; i < current.nItems; i++)
		{
			vQ = (Vertex) current.subQuad[i];
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
	
	private Vertex getNearVertexInSubquads(QuadTreeCell current, Vertex v, int searchedCells)
	{
		Vertex ret = null;
		int [] ij = new int[2];
		double dist = -1.0;
		double2int(v.getUV(), ij);
		if (logger.isDebugEnabled())
			logger.debug("Near point in subquads: "+v);
		int l = 0;
		int [] posStack = new int[MAXLEVEL];
		posStack[l] = 0;
		QuadTreeCell [] quadStack = new QuadTreeCell[MAXLEVEL];
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
						quadStack[l] = (QuadTreeCell) quadStack[l-1].subQuad[i];
						posStack[l] = i;
						break;
					}
				}
			}
			else
			{
				for (int i = 0; i < quadStack[l].nItems; i++)
				{
					Vertex vQ = (Vertex) quadStack[l].subQuad[i];
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
					if (posStack[l] == 4)
						l--;
					else if (null != quadStack[l-1].subQuad[posStack[l]])
						break;
				}
				if (l == 0)
					break;
				quadStack[l] = (QuadTreeCell) quadStack[l-1].subQuad[posStack[l]];
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
		public Vertex fromVertex, nearestVertex;
		public int searchedCells = 0;
		private final Calculus comp = mesh.compGeom();
		public getNearestVertexProcedure(Vertex from, Vertex v)
		{
			double2int(from.getUV(), ij);
			nearestVertex = v;
			fromVertex = from;
			// FIXME: a factor of 1.005 is added to take rounding
			// errors into account, a better approximation should
			// be used.
			i2d = 1.005 * x0[2] * (comp.radius2d(fromVertex));
			dist = comp.distance(fromVertex, v, fromVertex);
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
			QuadTreeCell self = (QuadTreeCell) o;
			searchedCells++;
			if (self.nItems > 0)
			{
				for (int i = 0; i < self.nItems; i++)
				{
					Vertex vtest = (Vertex) self.subQuad[i];
					double retdist = comp.distance(fromVertex, vtest, fromVertex);
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
	 *
	 * @param v  the node to check.
	 * @return the nearest vertex.
	 */
	public Vertex getNearestVertex(Vertex v)
	{
		QuadTreeCell current = root;
		Vertex ret = getNearVertex(v);
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
		public Vertex fromVertex, nearestVertex;
		public int searchedCells = 0;
		public getNearestVertexDebugProcedure(Vertex from, Vertex v)
		{
			double2int(from.getUV(), ij);
			nearestVertex = v;
			fromVertex = from;
			dist = mesh.compGeom().distance(fromVertex, v, fromVertex);
		}
		public final int action(Object o, int s, int i0, int j0)
		{
			QuadTreeCell self = (QuadTreeCell) o;
			searchedCells++;
			if (self.nItems > 0)
			{
				for (int i = 0; i < self.nItems; i++)
				{
					Vertex vtest = (Vertex) self.subQuad[i];
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
	 * Slow implementation of {@link #getNearestVertex}.
	 * This method should be called only for debugging purpose.
	 *
	 * @param v  the node to check.
	 * @return the nearest vertex.
	 */
	public Vertex getNearestVertexDebug(Vertex v)
	{
		QuadTreeCell current = root;
		Vertex ret = getNearVertex(v);
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
			QuadTreeCell self = (QuadTreeCell) o;
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
		QuadTreeCell current = root;
		getAllVerticesProcedure gproc = new getAllVerticesProcedure(capacity);
		deambulate(gproc);
		return gproc.nodelist;
	}
	
	private final class clearAllMetricsProcedure implements QuadTreeProcedure
	{
		public clearAllMetricsProcedure()
		{
		}
		public final int action(Object o, int s, int i0, int j0)
		{
			QuadTreeCell self = (QuadTreeCell) o;
			if (self.nItems > 0)
			{
				for (int i = 0; i < self.nItems; i++)
					((Vertex) self.subQuad[i]).clearMetrics();
			}
			return 0;
		}
	}
	
	/**
	 * Remove all metrics of vertices stored in this <code>QuadTree</code>.
	 */
	public void clearAllMetrics()
	{
		QuadTreeCell current = root;
		clearAllMetricsProcedure gproc = new clearAllMetricsProcedure();
		deambulate(gproc);
	}
	
	public final boolean walk(QuadTreeProcedure proc)
	{
		int s = gridSize;
		int l = 0;
		int i0 = 0;
		int j0 = 0;
		int [] posStack = new int[MAXLEVEL];
		posStack[l] = 0;
		QuadTreeCell [] quadStack = new QuadTreeCell[MAXLEVEL];
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
						quadStack[l] = (QuadTreeCell) quadStack[l-1].subQuad[i];
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
				quadStack[l] = (QuadTreeCell) quadStack[l-1].subQuad[posStack[l]];
			}
		}
		assert i0 == 0;
		assert j0 == 0;
		return true;
	}
	
	//  Similar to walk() but do not maintain i0,j0
	public boolean deambulate(QuadTreeProcedure proc)
	{
		int s = gridSize;
		int l = 0;
		int [] posStack = new int[MAXLEVEL];
		posStack[l] = 0;
		QuadTreeCell [] quadStack = new QuadTreeCell[MAXLEVEL];
		quadStack[l] = root;
		while (true)
		{
			int res = proc.action(quadStack[l], s, 0, 0);
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
						quadStack[l] = (QuadTreeCell) quadStack[l-1].subQuad[i];
						posStack[l] = i;
						break;
					}
				}
			}
			else
			{
				while (l > 0)
				{
					posStack[l]++;
					if (posStack[l] == 4)
					{
						s <<= 1;
						l--;
					}
					else if (null != quadStack[l-1].subQuad[posStack[l]])
							break;
				}
				if (l == 0)
					break;
				quadStack[l] = (QuadTreeCell) quadStack[l-1].subQuad[posStack[l]];
			}
		}
		return true;
	}
	
}
