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
 * Implementation of AVL binary trees to store locational codes.
 * It is based on excellent Ben Pfaff's GNU libavl, and customized to efficiently
 * find unique vertices from a triangle soup.
 * http://adtinfo.org/
 */
public class AVLTreeDup
{
	private static Logger logger = Logger.getLogger(AVLTreeDup.class);	
	//  A (x,y,z) triplet is stored in an int array of size nrInt.
	//  It is 4 if coordinates are int, 8 if long.
	private static final int nrInt = 4;
	private static int [] temp = new int[nrInt];
	
	private AVLTreeDupNode root;
	private int height = 0;
	private int [] dir;
	
	private class AVLTreeDupNode
	{
		private int [] key = new int[nrInt];
		private int value;
		//  balanceFactor = height(rightSubTree) - height(leftSubTree)
		private int balanceFactor = 0;
		private AVLTreeDupNode [] child = new AVLTreeDupNode[2];
		public AVLTreeDupNode(int [] ijk, int v)
		{
			mortonCode(ijk, key);
			value = v;
		}
		public void show()
		{
			String r = "Key: "+keyString(key)+"  bal. "+balanceFactor;
			if (child[0] != null)
				r += " Left -> "+keyString(child[0].key);
			if (child[1] != null)
				r += " Right -> "+keyString(child[1].key);
			System.out.println(r);
			if (child[0] != null)
				child[0].show();
			if (child[1] != null)
				child[1].show();
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
	public static void mortonCode(int [] ijk, int [] hash)
	{
		int i0 = ijk[0];
		int j0 = ijk[1];
		int k0 = ijk[2];
		for (int ind = nrInt - 1; ind >= 0; ind--)
		{
			hash[ind] = (dilate4(k0) << 2) | (dilate4(j0) << 1) | dilate4(i0);
			i0 >>= 8;
			j0 >>= 8;
			k0 >>= 8;
		}
	}
	//  Inverse operation
	public static void mortonCodeInv(int [] hash, int [] ijk)
	{
		ijk[0] = 0;
		ijk[1] = 0;
		ijk[2] = 0;
		for (int ind = 0; ind < nrInt; ind++)
		{
			ijk[0] <<= 8;
			ijk[1] <<= 8;
			ijk[2] <<= 8;
			ijk[0] |= contract4(hash[ind]);
			ijk[1] |= contract4(hash[ind] >> 1);
			ijk[2] |= contract4(hash[ind] >> 2);
		}
	}
	
	private final static String keyString(int [] key)
	{
		String ret = "";
		for (int i = 0; i < nrInt; i++)
			ret += " 0x"+Integer.toHexString(key[i]);
		return ret;
	}
	
	/**
	 * Insert a node to the tree.
	 *
	 * @param ijk    integer coordinates
	 * @param value  node index
	 * @return if the node is already present, returns its associated value,
	 *         otherwise returns <code>value</code>
	 */
	public final int insert(int [] ijk, int value)
	{
		AVLTreeDupNode node = new AVLTreeDupNode(ijk, value);
		if (logger.isDebugEnabled())
			logger.debug("insertion of key: "+keyString(node.key));
		if (root == null)
		{
			root = node;
			dir = new int[100];
			return value;
		}
		AVLTreeDupNode current = root;
		AVLTreeDupNode parent = root;
		AVLTreeDupNode topNode = root;
		AVLTreeDupNode topNodeParent = root;
		int depth = 0;
		int height = -1;
		int lastDir = 0;
		while (current != null)
		{
			height++;
			int result = compare(current.key, node.key);
			if (logger.isDebugEnabled())
				logger.debug("Comparing "+keyString(current.key)+" with "+keyString(node.key)+"  -> "+result);
			if (result > 0)
				lastDir = 0;
			else if (result < 0)
				lastDir = 1;
			else
				//  This entry is already inserted
				return current.value;
			if (current.balanceFactor != 0)
			{
				//  Only nodes below topNode need to be updated, unless another
				//  unbalanced node is found below this node
				topNode = current;
				topNodeParent = parent;
				depth = 0;
			}
			dir[depth] = lastDir;
			depth++;
			parent = current;
			current = current.child[lastDir];
		}
		if (height >= dir.length - 1)
		{
			//  Enlarge this array for future uses
			dir = new int[height + 100];
		}
		// Insert node
		parent.child[lastDir] = node;
		// Update balance factors
		depth = 0;
		current = topNode;
		while (current != node)
		{
			if (dir[depth] == 0)
				current.balanceFactor--;
			else
				current.balanceFactor++;
			current = current.child[dir[depth]];
			depth++;
		}
		// Balance subtree
		AVLTreeDupNode newRoot = null;
		if (topNode.balanceFactor == -2)
		{
			AVLTreeDupNode left = topNode.child[0];
			if (left.balanceFactor == -1)
			{
				/* Single right rotation
                          C                   B
                         / \     ------->   /   \
                        B  T4              A     C
                       / \                / \   / \
                      A  T3              T1 T2 T3 T4
                     / \
                    T1 T2
*/
				logger.debug("Single right rotation");
				newRoot = left;
				topNode.child[0] = left.child[1];
				left.child[1] = topNode;
				newRoot.balanceFactor = 0;
				topNode.balanceFactor = 0;
			}
			else
			{
				/*  Left+right rotation
                  C                    C                    B
                 / \      ------>     / \    ------>      /   \
                A  T4                B  T4               A     C
               / \                  / \                 / \   / \
              T1  B                A  T3               T1 T2 T3 T4
                 / \              / \
                T2 T3            T1 T2
*/
				logger.debug("Left+right rotation");
				assert left.balanceFactor == 1;
				newRoot = left.child[1];
				assert newRoot != null;
				// Left rotation
				left.child[1] = newRoot.child[0];
				newRoot.child[0] = left;
				// Right rotation
				topNode.child[0] = newRoot.child[1];
				newRoot.child[1] = topNode;
				//  Note: T2 and T3 cannot be both null
				if (newRoot.balanceFactor == -1)
				{
					//  T3 == null
					left.balanceFactor = 0;
					topNode.balanceFactor = 1;
					newRoot.balanceFactor = 0;
				}
				else if (newRoot.balanceFactor == +1)
				{
					//  T2 == null
					left.balanceFactor = -1;
					topNode.balanceFactor = 0;
					newRoot.balanceFactor = 0;
				}
				else
				{
					left.balanceFactor = 0;
					topNode.balanceFactor = 0;
				}
			}
		}
		else if (topNode.balanceFactor == 2)
		{
			AVLTreeDupNode right = topNode.child[1];
			if (right.balanceFactor == 1)
			{
				/* Single left rotation
                            A                     B
                           / \      ------>     /   \
                          T1  B                A     C
                             / \              / \   / \
                            T2  C            T1 T2 T3 T4
                               / \
                              T3 T4
*/
				logger.debug("Single left rotation");
				newRoot = right;
				topNode.child[1] = right.child[0];
				right.child[0] = topNode;
				newRoot.balanceFactor = 0;
				topNode.balanceFactor = 0;
			}
			else
			{
				/* Right+left rotation
               A                      A                     B
              / \      ------>       / \      ------>     /   \
             T1  C                  T1  B                A     C
                / \                    / \              / \   / \
               B  T4                  T2  C            T1 T2 T3 T4
              / \                        / \
             T2 T3                      T3 T4
*/
				logger.debug("Right+left rotation");
				assert right.balanceFactor == -1;
				newRoot = right.child[0];
				// Right rotation
				right.child[0] = newRoot.child[1];
				newRoot.child[1] = right;
				// Left rotation
				topNode.child[1] = newRoot.child[0];
				newRoot.child[0] = topNode;
				if (newRoot.balanceFactor == 1)
				{
					// T2 == null
					topNode.balanceFactor = -1;
					right.balanceFactor = 0;
					newRoot.balanceFactor = 0;
				}
				else if (newRoot.balanceFactor == -1)
				{
					// T3 == null
					topNode.balanceFactor = 0;
					right.balanceFactor = 1;
					newRoot.balanceFactor = 0;
				}
				else
				{
					topNode.balanceFactor = 0;
					right.balanceFactor = 0;
				}
			}
		}
		else
			return value;
		
		// Update link to topNode
		if (topNode == root)
			root = newRoot;
		else if (topNodeParent.child[0] == topNode)
			topNodeParent.child[0] = newRoot;
		else
			topNodeParent.child[1] = newRoot;
		return value;
	}
	
	/**
	 * Return index node.
	 * @param ijk    coordinates
	 * @return node index, or <code>-1</code> if this key does not
	 *         exist in the tree.
	 */
	public final int get(int [] ijk)
	{
		if (root == null)
			return -1;
		AVLTreeDupNode current = root;
		mortonCode(ijk, temp);
		while (current != null)
		{
			int result = compare(current.key, temp);
			if (result > 0)
				current = current.child[0];
			else if (result < 0)
				current = current.child[1];
			else
				return current.value;
		}
		return -1;
	}
	
	// Return -1 if key1 < key2, +1 if key1 > key2 and 0 if key1 == key2
	private static final int compare(int [] key1, int [] key2)
	{
		int ret = 0;
		for (int i = 0; i < nrInt; i++)
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
		b &= 0xff;
		//  u = 00000000 0000b7b6b5b4 00000000 0000b3b2b1b0
		int u = ((b & 0x000000f0) << 12) | (b & 0x0000000f);
		//  v = 000000b7b6 000000b5b4 000000b3b2 000000b1b0
		int v = ((u & 0x000c000c) << 6) | (u & 0x00030003);
		//  w = 000b7000b6 000b5000b4 000b3000b2 000b1000b0
		int w = ((v & 0x02020202) << 3) | (v & 0x01010101);
		return w;
	}
	private final static int contract4 (int w)
	{
		//  Clear unwanted bits
		//  w = 000b7000b6 000b5000b4 000b3000b2 000b1000b0
		w &= 0x11111111;
		//  v = 000000b7b6 000000b5b4 000000b3b2 000000b1b0
		int v = ((w & 0x10101010) >> 3) | (w & 0x01010101);
		//  u = 00000000 0000b7b6b5b4 00000000 0000b3b2b1b0
		int u = ((v & 0x03000300) >> 6) | (v & 0x00030003);
		//  b = b7b6b5b4b3b2b1b0
		int b = ((u & 0x000f0000) >> 12) | (u & 0x0000000f);
		assert b >= 0 && b <= 0xff;
		return b;
	}
	
	public static void main(String args[])
	{
		boolean assertionsEnabled = false;
		assert assertionsEnabled = true;
		if (!assertionsEnabled)
		{
			System.out.println("Assertions must be enabled!");
			System.exit(1);
		}
		int [] ijk = new int[3];
		AVLTreeDup tree = new AVLTreeDup();
		for (int i = 0; i < 3; i++)
			ijk[i] = 0;
		for (int i = 1; i < 6; i++)
		{
			ijk[0] = i;
			tree.insert(ijk, i);
			System.out.println("New tree:");
			tree.root.show();
		}
	}
		
}
