/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2005
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

package org.jcae.mesh.oemm.raw;

import org.apache.log4j.Logger;
import javax.media.j3d.Appearance;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.ColoringAttributes;

/**
 * This class represents an empty raw OEMM.
 * 
 * A raw OEMM is a pointer-based octree, but cells do not contain any data.  Only
 * its spatial structure is considered, and it is assumed that the whole tree can
 * reside in memory.  This class defines the octree structure and how to traverse it.
 */
public class RawOEMM
{
	private static Logger logger=Logger.getLogger(RawOEMM.class);	
	
	public static final int MAXLEVEL = 30;
	public static final int OEMM_DUMMY = 0;
	public static final int OEMM_CREATED = 1;
	public static final int OEMM_INITIALIZED = 2;
	private RawNode root;
	public int status;
	private int nCells = 1;
	private String rawFile;
	private int nr_levels;
	private int gridSize;
	private int minCellSize;
	private double [] x0 = new double[4];
	
	/**
	 * Create an empty raw OEMM.
	 */
	public RawOEMM()
	{
		status = OEMM_DUMMY;
		nr_levels = MAXLEVEL;
		gridSize = 1 << MAXLEVEL;
		minCellSize = 1;
		root = new RawNode(gridSize, 0, 0, 0);
		x0[0] = x0[1] = x0[2] = 0.0;
		x0[3] = 1.0;

	}
	
	/**
	 * Create an empty raw OEMM.
	 * @param file   file name containing the triangle soup
	 * @param lmax   maximal level of the tree
	 * @param umin   coordinates of the lower-left corner of mesh bounding box
	 * @param umax   coordinates of the upper-right corner of mesh bounding box
	 */
	public RawOEMM(String file, int lmax, double [] umin, double [] umax)
	{
		status = OEMM_CREATED;
		rawFile = file;
		nr_levels = lmax;
		if (nr_levels > MAXLEVEL)
		{
			logger.error("Max. level too high");
			nr_levels = MAXLEVEL;
		}
		gridSize = 1 << MAXLEVEL;
		minCellSize = 1 << (MAXLEVEL + 1 - nr_levels);
		double deltaX = Math.abs(umin[0] - umax[0]);
		double deltaY = Math.abs(umin[1] - umax[1]);
		double deltaZ = Math.abs(umin[2] - umax[2]);
		deltaX = Math.max(deltaX, deltaY);
		deltaX = Math.max(deltaX, deltaZ);
		for (int i = 0; i < 3; i++)
			x0[i] = umin[i];
		x0[3] = ((double) gridSize) / deltaX;
		root = new RawNode(gridSize, 0, 0, 0);
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
	
	/*         k=0          k=1
	 *      .-------.    .-------.
	 *      | 2 | 3 |    | 6 | 7 |
	 *   j  |---+---|    |---+---|
	 *      | 0 | 1 |    | 4 | 5 |
	 *      `-------'    `-------'
	 *          i          
	 */
	private static final int indexSubOctree(int size, int [] ijk)
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
	 * Return the octant of an OEMM structure containing a given point.
	 *
	 * @param oemm    main OEMM structure
	 * @param size    the returned octant must have this size.  If this value is 0,
	 *                the deepest octant is returned.
	 * @param ijk     integer coordinates of an interior node
	 * @param create  if set to <code>true</code>, cells are created if needed.  Otherwise
	 *                the desired octant must exist.
	 * @return  the octant of the desired size containing this point.
	 */
	public static final RawNode search(RawOEMM oemm, int size, int [] ijk, boolean create)
	{
		RawNode current = oemm.root;
		int s = current.size;
		if (size == 0)
			size = oemm.minCellSize;
		while (s > size)
		{
			if (current.isLeaf && !create)
				return current;
			s >>= 1;
			assert s > 0;
			int ind = indexSubOctree(s, ijk);
			if (null == current.child[ind])
			{
				if (!create)
					throw new RuntimeException("Element not found... Aborting");
				current.child[ind] = new RawNode(s, ijk);
				current.child[ind].parent = current;
				current.isLeaf = false;
				oemm.nCells++;
			}
			current = current.child[ind];
		}
		return current;
	}
	
	public void printInfos()
	{
		logger.info("Number of nodes: "+nCells);
		logger.info("Max level: "+nr_levels);
	}
	
	public final String getFileName()
	{
		return rawFile;
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
		if (status != OEMM_INITIALIZED)
		{
			logger.error("The raw OEMM must be filled in first!... Aborting");
			return false;
		}
		int s = gridSize;
		int l = 0;
		int i0 = 0;
		int j0 = 0;
		int k0 = 0;
		int [] posStack = new int[nr_levels+1];
		posStack[l] = 0;
		RawNode [] octreeStack = new RawNode[nr_levels+1];
		octreeStack[l] = root;
		proc.init();
		while (true)
		{
			int res = proc.preorder(octreeStack[l]);
			if (res == TraversalProcedure.ABORT)
				return false;
			if (!octreeStack[l].isLeaf && (res == TraversalProcedure.OK || res == TraversalProcedure.SKIPWALK))
			{
				s >>= 1;
				assert s > 0;
				l++;
				assert l <= nr_levels;
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
						logger.debug("Found POSTORDER: "+s+" "+i0+" "+j0+" "+k0);
						res = proc.postorder(octreeStack[l]);
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
		return true;
	}
	
	public void aggregate(int max, int delta)
	{
		// Disabled for now
		if (true) return;
		SumTrianglesProcedure proc = new SumTrianglesProcedure();
		walk(proc);
		logger.info("Nr triangles: "+root.tn);
		proc.printStats();
		AggregateProcedure aproc = new AggregateProcedure(max, delta);
		walk(aproc);
		ClearTrianglesProcedure cproc = new ClearTrianglesProcedure();
		walk(cproc);
		walk(proc);
		logger.info("Nr triangles: "+root.tn);
		proc.printStats();
	}
	
	/**
	 * Returns a BranchGroup to be displayed by java3d.
	 *
	 * @param onlyLeaves   if set to <code>true</code>, only leaves are taken into
	 *                     account, otherwise all nodes are considered.
	 */
	public BranchGroup bgOctree(boolean onlyLeaves)
	{
		BranchGroup bg=new BranchGroup();
		
		CoordProcedure proc = new CoordProcedure(onlyLeaves, nCells);
		walk(proc);
		QuadArray quad = new QuadArray(24*nCells, QuadArray.COORDINATES);
		quad.setCapability(QuadArray.ALLOW_FORMAT_READ);
		quad.setCapability(QuadArray.ALLOW_COUNT_READ);
		quad.setCapability(QuadArray.ALLOW_COORDINATE_READ);
		quad.setCoordinates(0, proc.coord);
		Appearance quadApp = new Appearance();
		quadApp.setPolygonAttributes(new PolygonAttributes(PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_NONE, 0));
		quadApp.setColoringAttributes(new ColoringAttributes(0,1,0,ColoringAttributes.SHADE_GOURAUD));
		Shape3D shapeQuad=new Shape3D(quad, quadApp);
		shapeQuad.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		bg.addChild(shapeQuad);
		return bg;
	}
	
	private final class SumTrianglesProcedure extends TraversalProcedure
	{
		public final int action(RawNode current, int visit)
		{
			if (visit != POSTORDER)
				return SKIPWALK;
			if (current.isLeaf)
				return OK;
			for (int i = 0; i < 8; i++)
			{
				if (current.child[i] == null)
					continue;
				current.tn += current.child[i].tn;
			}
			return OK;
		}
	}
	
	private final class ClearTrianglesProcedure extends TraversalProcedure
	{
		public final int action(RawNode current, int visit)
		{
			if (visit != PREORDER)
				return SKIPWALK;
			current.tn = 0;
			return OK;
		}
	}
	
	private final class AggregateProcedure extends TraversalProcedure
	{
		private int max_triangles;
		private int delta;
		public AggregateProcedure(int m, int d)
		{
			max_triangles = m;
			delta = 1 << d;
		}
		public final int action(RawNode current, int visit)
		{
			if (visit != PREORDER)
				return SKIPWALK;
			if (current.tn > max_triangles || current.size > delta * minCellSize)
				return OK;
			logger.debug("Aggregate node "+current+"   NrT="+current.tn);
			for (int i = 0; i < 8; i++)
				current.child[i] = null;
			current.isLeaf = true;
			return SKIPCHILD;
		}
	}
	
	private final class CoordProcedure extends TraversalProcedure
	{
		public final double [] coord;
		private int index;
		private boolean onlyLeaves;
		public CoordProcedure(boolean b, int n)
		{
			onlyLeaves = b;
			coord = new double[72*n];
		}
		public final int action(RawNode current, int visit)
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
