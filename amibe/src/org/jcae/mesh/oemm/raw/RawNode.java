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

/**
 * This class represents octants of a raw OEMM.
 *
 * A raw OEMM is a pointer-based octree, but cells do not contain any data.
 * Only its spatial structure is considered, and it is assumed that the whole
 * tree can reside in memory.
 */
public class RawNode
{
	//  Integer coordinates of the lower-left corner
	//  These coordiantes are only used in RawOEMM.CoordProcedure
	//  and thus only useful for debugging purposes.
	public int i0, j0, k0;
	//  Cell size (= 1 << (MAXLEVEL - level))
	public int size;
	//  Number of triangles
	public int tn = 0;
	//  Child list
	public RawNode[] child = new RawNode[8];
	//  Neighbor list
	//      0-5 : face neighbors
	//     6-17 : edge neighbors
	//    18-25 : vertex neighbors
	//  This arrangement is made so that shared data are first searched
	//  into face neighbors, which should normally contain most shared
	//  data, then edge neighbors and vertex neighbors.
	//  TODO: leaves need neighbor list whereas non-leaves
	//        need child list, so the latter could be stored
	//        within the latter to save memory space
	public RawNode[] neighbor = new RawNode[26];
	//  Parent node
	//  TODO: The parent pointer can be replaced by a stack
	//        if more room is needed.
	public RawNode parent;
	//  Is this node a leaf?
	public boolean isLeaf = true;
	//  Private counter
	public long counter = 0L;
	
	/**
	 * Create a new leaf.
	 * @param s   cell size
	 * @param ii  1st coordinate of its lower-left corner
	 * @param jj  2nd coordinate of its lower-left corner
	 * @param kk  3rd coordinate of its lower-left corner
	 */
	public RawNode(int s, int ii, int jj, int kk)
	{
		size = s;
		i0 = ii;
		j0 = jj;
		k0 = kk;
	}
	
	/**
	 * Create a new leaf.
	 * @param s   cell size
	 * @param ijk  coordinates of an interior point
	 */
	public RawNode(int s, int [] ijk)
	{
		size = s;
		i0 = ijk[0] & (~(s-1));
		j0 = ijk[1] & (~(s-1));
		k0 = ijk[2] & (~(s-1));
	}
	
	
	public String toString()
	{
		return "" +Integer.toHexString(size)+" "+Integer.toHexString(i0)+" "+Integer.toHexString(j0)+" "+Integer.toHexString(k0);
	}
}
