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

package org.jcae.mesh.oemm;

import org.apache.log4j.Logger;

/**
 * Implementation of B-Trees to store locational codes.
 */
public class BTree
{
	private static Logger logger=Logger.getLogger(BTree.class);	
	//  A (x,y,z) triplet is stored in an int array of size nrInt.
	private static final int nrInt = 3;
	
	private BTreeNode root;
	
	private class BTreeNode
	{
		private int [] key = new int[nrInt];
		private long value;
		private BTreeNode left, right;
		public BTreeNode(int [] ijk, long v)
		{
			mortonCode(ijk, key);
			value = v;
		}
	}
	
	/*
	 * Octants are numbered
	 *         k=0          k=1
	 *      .-------.    .-------.
	 *      | 2 | 3 |    | 6 | 7 |
	 *   j  |---+---|    |---+---|
	 *      | 0 | 1 |    | 4 | 5 |
	 *      `-------'    `-------'
	 *          i          
	 * 
	 * Thus if a node has a binary representation of (a single byte
	 * per coordinate is used for simplicity):
	 *   (i7i6i5i4i3i2i1i0, j7j6j5j4j3j2j1j0, k7k6k5k4k3k2k1k0)
	 * we compute the key:
	 *  0k7j7i70k6j6i6 0k5j5i50k4j4i4 0k3j3i30k2j2i2 0k1j1i10k0j0i0
	 * Then all keys in octant 0 are lower than those of octant 1,
	 * etc.  This has many advantages, e.g. if an octant is split,
	 * nodes can be dispatched very efficiently.
	 */
	private static void mortonCode(int [] ijk, int [] hash)
	{
		int i0 = ijk[0];
		int j0 = ijk[1];
		int k0 = ijk[2];
		for (int ind = 0; ind < nrInt; ind++)
		{
			hash[ind] = (dilate4(k0 & 0xff) << 2) | (dilate4(j0 & 0xff) << 1) | dilate4(i0 & 0xff);
			i0 >>= 8;
			j0 >>= 8;
			k0 >>= 8;
		}
	}
	
	private final static String keyString(int [] key)
	{
		String ret = "";
		for (int i = nrInt - 1; i >= 0; i--)
			ret += " 0x"+Integer.toHexString(key[i]);
		return ret;
	}
	
	/**
	 * Insert a node to the tree.
	 *
	 * @param ijk    integer coordinates
	 * @param value  node index
	 * @return <code>true</code> if the node is inserted, <code>false</code>
	 *         if it is aready present in the tree.
	 */
	public final boolean insert(int [] ijk, long value)
	{
		BTreeNode node = new BTreeNode(ijk, value);
		if (logger.isDebugEnabled())
			logger.debug("insertion of key: "+keyString(node.key));
		if (root == null)
		{
			root = node;
			return true;
		}
		BTreeNode parent = null;
		BTreeNode current = root;
		boolean left = false;
		while (current != null)
		{
			int result = compare(current.key, node.key);
			if (logger.isDebugEnabled())
				logger.debug("Comparing "+keyString(current.key)+" with "+keyString(node.key)+"  -> "+result);
			if (result > 0)
			{
				parent = current;
				current = current.left;
				left = true;
			}
			else if (result < 0)
			{
				parent = current;
				current = current.right;
				left = false;
			}
			else
				//  This entry is already inserted
				return false;
		}
		// TODO: balance tree
		if (left)
			parent.left = node;
		else
			parent.right = node;
		return true;
	}
	
	/**
	 * Return index node.
	 * @param key    locational code
	 * @return node index, or <code>-1</code> if this key does not
	 *         exist in the tree.
	 */
	public final long get(int [] key)
	{
		if (root == null)
			return -1;
		BTreeNode current = root;
		while (current != null)
		{
			int result = compare(current.key, key);
			if (result > 0)
				current = current.left;
			else if (result < 0)
				current = current.right;
			else
				return current.value;
		}
		return -1;
	}
	
	private static final int compare(int [] key1, int [] key2)
	{
		int ret = 0;
		for (int i = nrInt - 1; i >= 0; i--)
		{
			ret = key1[i] - key2[i];
			if (ret != 0)
				return ret;
		}
		return 0;
	}
	
	private final static int dilate4 (int b)
	{
		//  byte b = b7b6b5b4b3b2b1b0
		//  u = 00000000 0000b7b6b5b4 00000000 0000b3b2b1b0
		int u = ((b & 0x000000f0) << 12) | (b & 0x0000000f);
		//  v = 000000b7b6 000000b5b4 000000b3b2 000000b1b0
		int v = ((u & 0x000c000c) << 6) | (u & 0x00030003);
		//  w = 000b7000b6 000b5000b4 000b3000b2 000b1000b0
		int w = ((v & 0x02020202) << 3) | (v & 0x01010101);
		return w;
	}
	
}
