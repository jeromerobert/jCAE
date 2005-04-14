/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2004 Jerome Robert <jeromerobert@users.sourceforge.net>

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

public class Octree
{
	private static int BUCKETSIZE = 10;
	protected class OctreeCell
	{
		public static final int SUBOCTREES = 8;
		//  nItems can either store the number of items in the current cell,
		//  or the total number of vertices below this cell.
		//  In the latter case, nItems can be of type byte
		//  (if BUCKETSIZE < 128) or short.
		protected int nItems = 0;
		//  subOctree contains either SUBOCTREES suboctrees or up to
		//  BUCKETSIZE vertices.  This compact storage is needed to
		//  reduce memory usage.
		protected Object [] subOctree = null;
	}
	
	private static Logger logger=Logger.getLogger(Octree.class);	
	
	public OctreeCell root;
	public int nCells = 1;
	// Integer coordinates (like gridSize) must be long if MAXLEVEL > 30
	public static final int MAXLEVEL = 30;
	public static final int gridSize = 1 << MAXLEVEL;
	// Convcrsion between double and integer coordinates
	public final double [] x0 = new double[4];
	
	public Octree(double [] umin, double [] umax)
	{
		double deltaX = Math.abs(umin[0] - umax[0]);
		double deltaY = Math.abs(umin[1] - umax[1]);
		double deltaZ = Math.abs(umin[2] - umax[2]);
		deltaX = Math.max(deltaX, deltaY);
		deltaX = Math.max(deltaX, deltaZ);
		for (int i = 0; i < 3; i++)
			x0[i] = umin[i];
		x0[3] = ((double) gridSize) / deltaX;
		root = new OctreeCell();
		nCells++;
	}
	
	public final void setBucketSize(int n)
	{
		BUCKETSIZE = n;
	}
	
	public final void double2int(double [] p, int [] ijk)
	{
		for (int i = 0; i < 3; i++)
			ijk[i] = (int) ((p[i] - x0[i]) * x0[3]);
	}
	
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
	public static final int indexSubOctree(int [] ijk, int size)
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
	
	public final void add(MNode3D v)
	{
		OctreeCell current = root;
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
				current.subOctree[ind] = new OctreeCell();
				nCells++;
			}
			current = (OctreeCell) current.subOctree[ind];
		}
		
		//  If current box is full, split it into SUBOCTREES suboctrees
		while (current.nItems == BUCKETSIZE)
		{
			s >>= 1;
			assert s > 0;
			OctreeCell [] newSubOctrees = new OctreeCell[OctreeCell.SUBOCTREES];
			//  Move points to their respective suboctrees.
			for (int i = 0; i < BUCKETSIZE; i++)
			{
				MNode3D p = (MNode3D) current.subOctree[i];
				double2int(p.getXYZ(), oldijk);
				int ind = indexSubOctree(oldijk, s);
				if (null == newSubOctrees[ind])
				{
					newSubOctrees[ind] = new OctreeCell();
					nCells++;
					newSubOctrees[ind].subOctree = new MNode3D[BUCKETSIZE];
				}
				OctreeCell target = newSubOctrees[ind];
				target.subOctree[target.nItems] = current.subOctree[i];
				target.nItems++;
			}
			current.subOctree = newSubOctrees;
			//  current will point to another cell, adjust it now.
			current.nItems = - BUCKETSIZE - 1;
			int ind = indexSubOctree(ijk, s);
			if (null == current.subOctree[ind])
			{
				current.subOctree[ind] = new OctreeCell();
				nCells++;
			}
			current = (OctreeCell) current.subOctree[ind];
		}
		//  Eventually insert the new point
		if (current.nItems == 0)
			current.subOctree = new MNode3D[BUCKETSIZE];
		current.subOctree[current.nItems] = v;
		current.nItems++;
	}
	
	public final void remove(MNode3D v)
	{
		OctreeCell current = root;
		OctreeCell last = root;
		OctreeCell next;
		int lastPos = 0;
		int s = gridSize;
		int [] ijk = new int[3];
		double2int(v.getXYZ(), ijk);
		while (current.nItems < 0)
		{
			//  nItems is negative
			current.nItems++;
			if (current.nItems == 0)
				last.subOctree[lastPos] = null;
			s >>= 1;
			assert s > 0;
			int ind = indexSubOctree(ijk, s);
			next = (OctreeCell) current.subOctree[ind];
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
		if (current.nItems > 1)
		{
			current.subOctree[current.nItems-1] = null;
			current.nItems--;
		}
		else
			last.subOctree[lastPos] = null;
	}
	
	public final MNode3D getNearVertex(MNode3D v)
	{
		OctreeCell current = root;
		if (current.nItems == 0)
			return null;
		OctreeCell last = null;
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
			current = (OctreeCell)
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
	
	private final MNode3D getNearVertexInSubOctrees(OctreeCell current, MNode3D v, int dist, int searchedCells)
	{
		MNode3D ret = null;
		int [] ijk = new int[3];
		int [] retijk = new int[3];
		double2int(v.getXYZ(), ijk);
		long ldist = ((long) dist) * ((long) dist);
		if (logger.isDebugEnabled())
			logger.debug("Near point in suboctrees: "+v);
		int l = 0;
		int [] posStack = new int[MAXLEVEL];
		posStack[l] = 0;
		OctreeCell [] octreeStack = new OctreeCell[MAXLEVEL];
		octreeStack[l] = current;
		while (true)
		{
			searchedCells++;
			if (octreeStack[l].nItems < 0)
			{
				l++;
				assert l <= MAXLEVEL;
				for (int i = 0; i < OctreeCell.SUBOCTREES; i++)
				{
					if (null != octreeStack[l-1].subOctree[i])
					{
						octreeStack[l] = (OctreeCell) octreeStack[l-1].subOctree[i];
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
					if (posStack[l] == OctreeCell.SUBOCTREES)
						l--;
					else if (null != octreeStack[l-1].subOctree[posStack[l]])
						break;
				}
				if (l == 0)
					break;
				octreeStack[l] = (OctreeCell) octreeStack[l-1].subOctree[posStack[l]];
			}
		}
		if (logger.isDebugEnabled())
			logger.debug("  search in "+searchedCells+"/"+nCells+" cells");
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
			OctreeCell self = (OctreeCell) o;
			searchedCells++;
			if (s < minSize)
				minSize = s;
			return 0;
		}
	}
	
	public final int getMinSize()
	{
		OctreeCell current = root;
		getMinSizeProcedure gproc = new getMinSizeProcedure();
		deambulate(gproc);
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
			OctreeCell self = (OctreeCell) o;
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
	
	public final MNode3D getNearestVertex(MNode3D v)
	{
		OctreeCell current = root;
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
	
	public final boolean walk(OctreeProcedure proc)
	{
		int s = gridSize;
		int l = 0;
		int i0 = 0;
		int j0 = 0;
		int k0 = 0;
		int [] posStack = new int[MAXLEVEL];
		posStack[l] = 0;
		OctreeCell [] octreeStack = new OctreeCell[MAXLEVEL];
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
				for (int i = 0; i < OctreeCell.SUBOCTREES; i++)
				{
					if (null != octreeStack[l-1].subOctree[i])
					{
						octreeStack[l] = (OctreeCell) octreeStack[l-1].subOctree[i];
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
					if (posStack[l] == OctreeCell.SUBOCTREES)
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
				octreeStack[l] = (OctreeCell) octreeStack[l-1].subOctree[posStack[l]];
			}
		}
		assert i0 == 0;
		assert j0 == 0;
		assert k0 == 0;
		return true;
	}
	
	//  Similar to walk() but do not maintain i0,j0,k0
	public final boolean deambulate(OctreeProcedure proc)
	{
		int s = gridSize;
		int l = 0;
		int [] posStack = new int[MAXLEVEL];
		posStack[l] = 0;
		OctreeCell [] octreeStack = new OctreeCell[MAXLEVEL];
		octreeStack[l] = root;
		while (true)
		{
			int res = proc.action(octreeStack[l], s, 0, 0, 0);
			if (res == -1)
				return false;
			if (octreeStack[l].nItems < 0 && res == 0)
			{
				s >>= 1;
				assert s > 0;
				l++;
				assert l <= MAXLEVEL;
				for (int i = 0; i < OctreeCell.SUBOCTREES; i++)
				{
					if (null != octreeStack[l-1].subOctree[i])
					{
						octreeStack[l] = (OctreeCell) octreeStack[l-1].subOctree[i];
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
					if (posStack[l] == OctreeCell.SUBOCTREES)
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
				octreeStack[l] = (OctreeCell) octreeStack[l-1].subOctree[posStack[l]];
			}
		}
		return true;
	}
	
}
