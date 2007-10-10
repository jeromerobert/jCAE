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
import java.util.Iterator;

public class PAVLSortedTreeTest
{
	/* Single left rotation
	    A                    B
	   / \      ------>     / \
	  T1  B                A   T3
	     / \              / \
	    T2 T3            T1 T2
	*/
	@Test public void rotateL()
	{
		QSortedTree tree = new PAVLSortedTree();
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
		QSortedTree tree = new PAVLSortedTree();
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
		QSortedTree tree = new PAVLSortedTree();
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
		QSortedTree tree = new PAVLSortedTree();
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
		QSortedTree tree = new PAVLSortedTree();
		tree.insert(Integer.valueOf(6), 6.0);
		tree.insert(Integer.valueOf(2), 2.0);
		tree.insert(Integer.valueOf(7), 7.0);
		tree.insert(Integer.valueOf(1), 1.0);
		tree.insert(Integer.valueOf(4), 4.0);
		tree.insert(Integer.valueOf(5), 5.0);
		tree.insert(Integer.valueOf(3), 3.0);
		int i = 1;
		for (Iterator<QSortedTree.Node> it = tree.iterator(); it.hasNext(); i++)
			assertTrue(it.next().getData().equals(Integer.valueOf(i)));
	}
	@Test public void backwardIterator()
	{
		QSortedTree tree = new PAVLSortedTree();
		tree.insert(Integer.valueOf(6), 6.0);
		tree.insert(Integer.valueOf(2), 2.0);
		tree.insert(Integer.valueOf(7), 7.0);
		tree.insert(Integer.valueOf(1), 1.0);
		tree.insert(Integer.valueOf(4), 4.0);
		tree.insert(Integer.valueOf(5), 5.0);
		tree.insert(Integer.valueOf(3), 3.0);
		int i = 7;
		for (Iterator<QSortedTree.Node> it = tree.backwardIterator(); it.hasNext(); i--)
			assertTrue(it.next().getData().equals(Integer.valueOf(i)));
	}
}
