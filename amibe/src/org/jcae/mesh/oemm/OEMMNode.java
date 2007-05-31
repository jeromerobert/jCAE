/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005, by EADS CRC
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

package org.jcae.mesh.oemm;

import java.io.Serializable;
import gnu.trove.TIntArrayList;

/**
 * This class represents octants of an OEMM.  Octants can either be leaves or internal
 * nodes.
 */
public class OEMMNode implements Serializable
{
	private static final long serialVersionUID = 1632948181488810349L;

	/**
	 * Integer coordinates of lower-left corner.
	 */
	public int i0, j0, k0;

	/**
	 * Cell size.  It is equal to (1 &lt;&lt; (OEMM.MAXLEVEL - depth))
	 */
	public int size;

	/**
	 * Total number of triangles found in this node and its children.
	 */
	public int tn = 0;

	/**
	 * Number of vertices found in this node and its children.
	 */
	public int vn = 0;

	/**
	 * Array of 8 children nodes.
	 */
	public transient OEMMNode[] child = new OEMMNode[8];

	/**
	 * Parent node.
	 */
	//  TODO: The parent pointer can be replaced by a stack
	//        if more room is needed.
	public transient OEMMNode parent;

	/**
	 * Flag set when this node a leaf.
	 */
	public transient boolean isLeaf = true;

	/**
	 * File containing vertices and triangles.
	 */
	public String file;

	/**
	 * Counter.  This is a temporary variable used by some algorithms.
	 */
	public transient long counter = 0L;
	
	/**
	 * Leaf index in {@link OEMM#leaves}.
	 */
	public int leafIndex = -1;

	/**
	 * First index of all vertices found in this node and its children.
	 */
	public int minIndex = 0;

	/**
	 * Maximal index allowed for vertices found in this node and its children.
	 */
	public int maxIndex = 0;

	/**
	 * List of adjacent leaves.
	 */
	public TIntArrayList adjLeaves;
	
	/**
	 * Creates a new leaf.
	 * @param s   cell size
	 * @param i0  1st coordinate of its lower-left corner
	 * @param j0  2nd coordinate of its lower-left corner
	 * @param k0  3rd coordinate of its lower-left corner
	 */
	public OEMMNode(int s, int i0, int j0, int k0)
	{
		size = s;
		this.i0 = i0;
		this.j0 = j0;
		this.k0 = k0;
	}
	
	/**
	 * Creates a new leaf.
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
	
	private void readObject(java.io.ObjectInputStream s)
	        throws java.io.IOException, ClassNotFoundException
	{
		s.defaultReadObject();
		child = new OEMMNode[8];
		isLeaf = true;
	}

	public String toString()
	{
		return " IJK "+Integer.toHexString(i0)+" "+Integer.toHexString(j0)+" "+Integer.toHexString(k0)+
		       " Size=" +Integer.toHexString(size)+
		       " Leaf?: "+isLeaf+
		       " NrV="+vn+
		       " NrT="+tn+
		       " index="+leafIndex+
		       " min="+minIndex+
		       " max="+maxIndex+
		       " file="+file+
		       " adj="+adjLeaves;
	}
}
