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

/**
 * Implementation of PAVL binary trees to store locational codes.
 * It is based on excellent Ben Pfaff's GNU libavl http://adtinfo.org/.
 * In C, the version with parent pointers (pavl) is slightly faster than avl.
 * Even if there does not seem to be much difference in Java, we chose this
 * version.  A major improvement is to use an int array for storing nodes,
 * so that extra memory is almost never allocated.  On trees with 2 millions
 * of nodes, this optimization gives a speedup of 5.
 */
public class PAVLTreeIntArrayDup
{
	//  A (x,y,z) triplet is stored in an int array of size nrInt.
	//  It is 4 if coordinates are int, 8 if long.
	private static final int nrInt = 4;
	
	private static final int POS_NIL = -1;
	private static final int POS_KEY = 0;
	private static final int POS_VALUE = nrInt;
	private static final int POS_BALANCE = POS_VALUE + 1;
	private static final int POS_CHILD = POS_BALANCE + 1;
	private static final int POS_PARENT = POS_CHILD + 2;
	private static final int TOTAL_SIZE = POS_PARENT + 1;
	private static final int allocNodes = 100000;
	private int [] work = new int[allocNodes*TOTAL_SIZE];
	
	// Root tree
	private int root = POS_NIL;
	// Next free index
	private int nextIndex = 0;
	// Temporary array
	private final int [] temp = new int[nrInt];
	
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
	private static final void mortonCode(int [] ijk, int [] hash)
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

	private static final int dilate4 (int b)
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
	
	//  Inverse operation (not used currently)
	/*
	private static final void mortonCodeInv(int [] hash, int [] ijk)
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

	private static final int contract4 (int w)
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
	*/
	
	private static final String keyString(int [] data, int offKey)
	{
		StringBuffer ret = new StringBuffer();
		for (int i = 0; i < nrInt; i++)
			ret.append(" 0x"+Integer.toHexString(data[offKey+i]));
		return ret.toString();
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
		if (nextIndex >= work.length)
		{
			int [] work2 = new int[work.length+allocNodes*TOTAL_SIZE];
			System.arraycopy(work, 0, work2, 0, work.length);
			work = work2;
		}
		int node = nextIndex;
		mortonCode(ijk, temp);
		// Creates a new node.  If the node is already present,
		// nextIndex won't get incremented.
		//    Key
		System.arraycopy(temp, 0, work, nextIndex+POS_KEY, nrInt);
		//   Value
		work[node+POS_VALUE] = value;
		//   Balance
		work[node+POS_BALANCE] = 0;
		//   Pointers
		work[node+POS_CHILD] = POS_NIL;
		work[node+POS_CHILD+1] = POS_NIL;
		work[node+POS_PARENT] = POS_NIL;
		
		if (root == POS_NIL)
		{
			// Empty tree
			root = nextIndex;
			nextIndex += TOTAL_SIZE;
			return value;
		}
		int current = root;
		int parent = root;
		int topNode = root;
		int lastDir = 0;
		while (current != POS_NIL)
		{
			int result = compare(work, current, work, node);
			if (result > 0)
				lastDir = 0;
			else if (result < 0)
				lastDir = 1;
			else
				//  This entry is already inserted
				return work[current+POS_VALUE];
			if (work[current+POS_BALANCE] != 0)
				topNode = current;
			parent = current;
			current = work[current+POS_CHILD+lastDir];
		}
		// Insert node
		nextIndex += TOTAL_SIZE;
		work[parent+POS_CHILD+lastDir] = node;
		work[node+POS_PARENT] = parent;
		// Update balance factors
		for (current = node; current != topNode; current = parent)
		{
			parent = work[current+POS_PARENT];
			if (work[parent+POS_CHILD] == current)
				work[parent+POS_BALANCE]--;
			else
				work[parent+POS_BALANCE]++;
		}
		parent = work[topNode+POS_PARENT];
		// Balance subtree
		int newRoot = POS_NIL;
		if (work[topNode+POS_BALANCE] == -2)
		{
			int left = work[topNode+POS_CHILD];
			if (work[left+POS_BALANCE] == POS_NIL)
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
				newRoot = left;
				work[topNode+POS_CHILD] = work[left+POS_CHILD+1];
				work[left+POS_CHILD+1] = topNode;
				work[left+POS_PARENT] = work[topNode+POS_PARENT];
				work[topNode+POS_PARENT] = left;
				if (work[topNode+POS_CHILD] != POS_NIL)
					work[work[topNode+POS_CHILD]+POS_PARENT] = topNode;
				work[newRoot+POS_BALANCE] = 0;
				work[topNode+POS_BALANCE] = 0;
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
				assert work[left+POS_BALANCE] == 1;
				newRoot = work[left+POS_CHILD+1];
				assert newRoot != POS_NIL;
				// Left rotation
				work[left+POS_CHILD+1] = work[newRoot+POS_CHILD];
				work[newRoot+POS_CHILD] = left;
				// Right rotation
				work[topNode+POS_CHILD] = work[newRoot+POS_CHILD+1];
				work[newRoot+POS_CHILD+1] = topNode;
				// Parent pointers
				work[newRoot+POS_PARENT] = work[topNode+POS_PARENT];
				work[left+POS_PARENT] = newRoot;
				work[topNode+POS_PARENT] = newRoot;
				if (work[newRoot+POS_BALANCE] == -1)
				{
					work[left+POS_BALANCE] = 0;
					work[topNode+POS_BALANCE] = 1;
					work[newRoot+POS_BALANCE] = 0;
				}
				else if (work[newRoot+POS_BALANCE] == +1)
				{
					work[left+POS_BALANCE] = -1;
					work[topNode+POS_BALANCE] = 0;
					work[newRoot+POS_BALANCE] = 0;
				}
				else
				{
					work[left+POS_BALANCE] = 0;
					work[topNode+POS_BALANCE] = 0;
				}
				if (work[left+POS_CHILD+1] != POS_NIL)
					work[work[left+POS_CHILD+1]+POS_PARENT] = left;
				if (work[topNode+POS_CHILD] != POS_NIL)
					work[work[topNode+POS_CHILD]+POS_PARENT] = topNode;
			}
		}
		else if (work[topNode+POS_BALANCE] == 2)
		{
			int right = work[topNode+POS_CHILD+1];
			if (work[right+POS_BALANCE] == 1)
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
				newRoot = right;
				work[topNode+POS_CHILD+1] = work[right+POS_CHILD];
				work[right+POS_CHILD] = topNode;
				work[right+POS_PARENT] = work[topNode+POS_PARENT];
				work[topNode+POS_PARENT] = right;
				if (work[topNode+POS_CHILD+1] != POS_NIL)
					work[work[topNode+POS_CHILD+1]+POS_PARENT] = topNode;
				work[newRoot+POS_BALANCE] = 0;
				work[topNode+POS_BALANCE] = 0;
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
				assert work[right+POS_BALANCE] == -1;
				newRoot = work[right+POS_CHILD];
				// Right rotation
				work[right+POS_CHILD] = work[newRoot+POS_CHILD+1];
				work[newRoot+POS_CHILD+1] = right;
				// Left rotation
				work[topNode+POS_CHILD+1] = work[newRoot+POS_CHILD];
				work[newRoot+POS_CHILD] = topNode;
				// Parent pointers
				work[newRoot+POS_PARENT] = work[topNode+POS_PARENT];
				work[topNode+POS_PARENT] = newRoot;
				work[right+POS_PARENT] = newRoot;
				if (work[newRoot+POS_BALANCE] == 1)
				{
					work[topNode+POS_BALANCE] = -1;
					work[right+POS_BALANCE] = 0;
					work[newRoot+POS_BALANCE] = 0;
				}
				else if (work[newRoot+POS_BALANCE] == -1)
				{
					work[topNode+POS_BALANCE] = 0;
					work[right+POS_BALANCE] = 1;
					work[newRoot+POS_BALANCE] = 0;
				}
				else
				{
					work[topNode+POS_BALANCE] = 0;
					work[right+POS_BALANCE] = 0;
				}
				if (work[right+POS_CHILD] != POS_NIL)
					work[work[right+POS_CHILD]+POS_PARENT] = right;
				if (work[topNode+POS_CHILD+1] != POS_NIL)
					work[work[topNode+POS_CHILD+1]+POS_PARENT] = topNode;
			}
		}
		else
			return value;
		
		// Update link to topNode
		if (topNode == root)
			root = newRoot;
		else if (work[parent+POS_CHILD] == topNode)
			work[parent+POS_CHILD] = newRoot;
		else
			work[parent+POS_CHILD+1] = newRoot;
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
		if (root == POS_NIL)
			return POS_NIL;
		int current = root;
		mortonCode(ijk, temp);
		while (current != POS_NIL)
		{
			int result = compare(work, current, temp, 0);
			if (result > 0)
				current = work[current+POS_CHILD];
			else if (result < 0)
				current = work[current+POS_CHILD+1];
			else
				return work[current+POS_VALUE];
		}
		return -1;
	}
	
	// Return sign(key1 - key2)
	private static final int compare(int [] arr1, int off1, int [] arr2, int off2)
	{
		int ret = 0;
		for (int i = 0; i < nrInt; i++)
		{
			ret = arr1[off1+i] - arr2[off2+i];
			if (ret != 0)
				return ret;
		}
		return 0;
	}
	
	public int size()
	{
		return (nextIndex + 1) / TOTAL_SIZE;
	}
	
	public void show()
	{
		System.out.println("Tree");
		showNode(root);
	}
	
	private void showNode(int current)
	{
		StringBuffer r = new StringBuffer("Key: "+keyString(work, work[current+POS_KEY])+"  bal. "+work[current+POS_BALANCE]);
		if (work[current+POS_CHILD] != POS_NIL)
			r.append(" Left -> "+keyString(work, work[work[current+POS_CHILD]+POS_KEY]));
		if (work[current+POS_CHILD+1] != POS_NIL)
			r.append(" Right -> "+keyString(work, work[work[current+POS_CHILD+1]+POS_KEY]));
		if (work[current+POS_PARENT] != POS_NIL)
			r.append(" Parent -> "+keyString(work, work[work[current+POS_PARENT]+POS_KEY]));
		System.out.println(r.toString());
		if (work[current+POS_CHILD] != POS_NIL)
		{
			assert work[work[current+POS_CHILD]+POS_PARENT] == current;
			showNode(work[current+POS_CHILD]);
		}
		if (work[current+POS_CHILD+1] != POS_NIL)
		{
			assert work[work[current+POS_CHILD+1]+POS_PARENT] == current;
			showNode(work[current+POS_CHILD+1]);
		}
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
		for (int i = 0; i < 3; i++)
			ijk[i] = 0;
		PAVLTreeIntArrayDup tree;
/*
		//  Test single right rotation
		tree = new PAVLTreeIntArrayDup();
		ijk[0] = 10; tree.insert(ijk, ijk[0]);
		ijk[0] = 8; tree.insert(ijk, ijk[0]);
		ijk[0] = 6; tree.insert(ijk, ijk[0]);
		ijk[0] = 4; tree.insert(ijk, ijk[0]);
		ijk[0] = 2; tree.insert(ijk, ijk[0]);
		ijk[0] = 1; tree.insert(ijk, ijk[0]);
		System.out.println("Tree:");
		tree.show();
		//  Test left+right rotation
		tree = new PAVLTreeIntArrayDup();
		ijk[0] = 10; tree.insert(ijk, ijk[0]);
		ijk[0] = 8; tree.insert(ijk, ijk[0]);
		ijk[0] = 6; tree.insert(ijk, ijk[0]);
		ijk[0] = 4; tree.insert(ijk, ijk[0]);
		ijk[0] = 2; tree.insert(ijk, ijk[0]);
		ijk[0] = 5; tree.insert(ijk, ijk[0]);
		System.out.println("Tree:");
		tree.show();
		//  Test single left rotation
		tree = new PAVLTreeIntArrayDup();
		ijk[0] = 2; tree.insert(ijk, ijk[0]);
		ijk[0] = 4; tree.insert(ijk, ijk[0]);
		ijk[0] = 6; tree.insert(ijk, ijk[0]);
		ijk[0] = 8; tree.insert(ijk, ijk[0]);
		ijk[0] = 10; tree.insert(ijk, ijk[0]);
		ijk[0] = 9; tree.insert(ijk, ijk[0]);
		System.out.println("Tree:");
		tree.show();
		//  Test right+left rotation
		tree = new PAVLTreeIntArrayDup();
		ijk[0] = 2; tree.insert(ijk, ijk[0]);
		ijk[0] = 4; tree.insert(ijk, ijk[0]);
		ijk[0] = 6; tree.insert(ijk, ijk[0]);
		ijk[0] = 8; tree.insert(ijk, ijk[0]);
		ijk[0] = 10; tree.insert(ijk, ijk[0]);
		ijk[0] = 5; tree.insert(ijk, ijk[0]);
		System.out.println("Tree:");
		tree.show();
		//  Other tests
		tree = new PAVLTreeIntArrayDup();
		for (int i = 0; i < 3; i++)
			ijk[i] = 0;
		for (int i = 1; i < 8; i++)
		{
			ijk[0] = 4*i;
			tree.insert(ijk, ijk[0]);
		}
		for (int i = 1; i < 20; i++)
		{
			ijk[0] = i;
			tree.insert(ijk, ijk[0]);
		}
		System.out.println("Tree:");
		tree.show();
*/
		tree = new PAVLTreeIntArrayDup();
		int dup = 0;
		for (int i = 0; i < 2000000; i++)
		{
			ijk[0] = i / 6;
			if (tree.insert(ijk, i) != i)
				dup++;
		}
		System.out.println("Number of duplicates: "+dup);
		System.out.println("Number of nodes: "+tree.size());
	}
		
}
