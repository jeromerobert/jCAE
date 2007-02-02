/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005, by EADS CRC

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

import gnu.trove.TIntArrayList;

/**
 * This class represents octants of a raw OEMM.
 *
 * A raw OEMM is a pointer-based octree, but cells do not contain any data.
 * Only its spatial structure is considered, and it is assumed that the whole
 * tree can reside in memory.
 */
public class OEMMNode
{
	//  Integer coordinates of the lower-left corner
	//  These coordiantes are only used in RawOEMM.CoordProcedure
	//  and thus only useful for debugging purposes.
	public int i0, j0, k0;
	//  Cell size (= 1 << (MAXLEVEL - level))
	public int size;
	//  Number of triangles
	public int tn = 0;
	//  Number of vertices
	public int vn = 0;
	//  Child list
	public OEMMNode[] child = new OEMMNode[8];
	//  Linked list of nodes at the same level
	public Object extra;
	//  Parent node
	//  TODO: The parent pointer can be replaced by a stack
	//        if more room is needed.
	public OEMMNode parent;
	//  Is this node a leaf?
	public boolean isLeaf = true;
	//  Top-level directory
	public String topDir;
	//  File containing vertices and triangles
	public String file;
	//  Counter
	public long counter = 0L;
	
	//  Index for leaves
	public int leafIndex = -1;
	//  Nodes are either
	//    a. leaves and contain
	//         1. vertices with global indices between minIndex and maxIndex
	//         2. a list of adjacent leaves with shared data
	//         3. a leaf index
	//  or
	//    b. non-leaves and internal leaves have an index between minIndex
	//       and maxIndex.
	//  As described in Cignoni's paper, having maxIndex > minIndex + vn
	//  for leaves gives room to add vertices without having to reindex
	//  all nodes.
	public int minIndex = 0;
	public int maxIndex = 0;
	//  List of adjacent leaves with shared data
	public TIntArrayList adjLeaves;
	
	/**
	 * Create a new leaf.
	 * @param s   cell size
	 * @param ii  1st coordinate of its lower-left corner
	 * @param jj  2nd coordinate of its lower-left corner
	 * @param kk  3rd coordinate of its lower-left corner
	 */
	public OEMMNode(int s, int ii, int jj, int kk)
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
	public OEMMNode(int s, int [] ijk)
	{
		size = s;
		int mask = ~(s - 1);
		i0 = ijk[0] & mask;
		j0 = ijk[1] & mask;
		k0 = ijk[2] & mask;
	}
	
	public OEMMNode(String d, String f)
	{
		topDir = d;
		file = f;
	}
	
	public String toString()
	{
		return "Leaf?: "+isLeaf+
		       " Size=" +Integer.toHexString(size)+
		       " IJK "+Integer.toHexString(i0)+" "+Integer.toHexString(j0)+" "+Integer.toHexString(k0)+
		       " NrV="+vn+
		       " NrT="+tn+
		       " index="+leafIndex+
		       " min="+minIndex+
		       " max="+maxIndex+
		       " file="+file;
	}
}
