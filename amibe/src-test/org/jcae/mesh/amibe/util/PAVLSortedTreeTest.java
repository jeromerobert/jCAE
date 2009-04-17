/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

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

package org.jcae.mesh.amibe.util;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import java.util.Iterator;

public class PAVLSortedTreeTest extends QSortedTreeTest
{
	@Before public void createTree()
	{
		tree = new PAVLSortedTree<Integer>();
	}

	/* Single left rotation
	    A                    B
	   / \      ------>     / \
	  T1  B                A   T3
	     / \              / \
	    T2 T3            T1 T2
	*/
	@Test public void rotateL()
	{
		tree.insert(Integer.valueOf(1), 1.0);
		assertTrue(tree.getRootValue() == 1.0);
		tree.insert(Integer.valueOf(2), 2.0);
		assertTrue(tree.getRootValue() == 1.0);
		tree.insert(Integer.valueOf(3), 3.0);
		assertTrue(tree.getRootValue() == 2.0);
	}

	/* Single right rotation
	          B                    A
	         / \     ------->     / \
	        A  T3                T1  B
	       / \                      / \
	      T1 T2                    T2 T3
	*/
	@Test public void rotateR()
	{
		tree.insert(Integer.valueOf(3), 3.0);
		assertTrue(tree.getRootValue() == 3.0);
		tree.insert(Integer.valueOf(2), 2.0);
		assertTrue(tree.getRootValue() == 3.0);
		tree.insert(Integer.valueOf(1), 1.0);
		assertTrue(tree.getRootValue() == 2.0);
	}

	/* Right+left rotation
	     A                      A                     B
	    / \      ------>       / \      ------>     /   \
	   T1  C                  T1  B                A     C
	      / \                    / \              / \   / \
	     B  T4                  T2  C            T1 T2 T3 T4
	    / \                        / \
	   T2 T3                      T3 T4
	*/
	@Test public void rotateRL()
	{
		tree.insert(Integer.valueOf(2), 2.0);
		tree.insert(Integer.valueOf(1), 1.0);
		tree.insert(Integer.valueOf(6), 6.0);
		tree.insert(Integer.valueOf(4), 4.0);
		tree.insert(Integer.valueOf(7), 7.0);
		tree.insert(Integer.valueOf(5), 5.0);
		tree.insert(Integer.valueOf(3), 3.0);
		assertTrue(tree.getRootValue() == 4.0);
	}

	/*  Left+right rotation
	      C                  C                    B
	     / \    ------>     / \    ------>      /   \
	    A  T4              B  T4               A     C
	   / \                / \                 / \   / \
	  T1  B              A  T3               T1 T2 T3 T4
	     / \            / \
	    T2 T3          T1 T2
	*/
	@Test public void rotateLR()
	{
		tree.insert(Integer.valueOf(6), 6.0);
		tree.insert(Integer.valueOf(2), 2.0);
		tree.insert(Integer.valueOf(7), 7.0);
		tree.insert(Integer.valueOf(1), 1.0);
		tree.insert(Integer.valueOf(4), 4.0);
		tree.insert(Integer.valueOf(5), 5.0);
		tree.insert(Integer.valueOf(3), 3.0);
		assertTrue(tree.getRootValue() == 4.0);
	}
	@Test public void iterator()
	{
		tree.insert(Integer.valueOf(6), 6.0);
		tree.insert(Integer.valueOf(2), 2.0);
		tree.insert(Integer.valueOf(7), 7.0);
		tree.insert(Integer.valueOf(1), 1.0);
		tree.insert(Integer.valueOf(4), 4.0);
		tree.insert(Integer.valueOf(5), 5.0);
		tree.insert(Integer.valueOf(3), 3.0);
		int i = 1;
		for (Iterator<QSortedTree.Node<Integer>> it = tree.iterator(); it.hasNext(); i++)
			assertTrue(it.next().getData().equals(Integer.valueOf(i)));
	}
	@Test public void backwardIterator()
	{
		tree.insert(Integer.valueOf(6), 6.0);
		tree.insert(Integer.valueOf(2), 2.0);
		tree.insert(Integer.valueOf(7), 7.0);
		tree.insert(Integer.valueOf(1), 1.0);
		tree.insert(Integer.valueOf(4), 4.0);
		tree.insert(Integer.valueOf(5), 5.0);
		tree.insert(Integer.valueOf(3), 3.0);
		int i = 7;
		for (Iterator<QSortedTree.Node<Integer>> it = tree.backwardIterator(); it.hasNext(); i--)
			assertTrue(it.next().getData().equals(Integer.valueOf(i)));
	}

	@Test public void foobar()
	{
		// Check with various lengths
		for (int n = 10; n < 100; n+=2)
		{
			unitTest1(n);
			unitTest2(n);
			unitTest3(n);
			unitTest4(n);
		}
	}

	@Test public void random()
	{
		// Insert and remove in random order
		for (int n = 3; n < 100; n+=2)
		{
			unitTest5(200, n);
			unitTest6(200, n);
		}
	}

	// This test is meant for timing purposes, one has then to increase n
	@Test public void large()
	{
		int n = 1000;
		unitTest1(n);
		unitTestIterator(n);
	}
}
