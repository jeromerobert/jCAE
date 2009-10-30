/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2006 by EADS CRC
    Copyright (C) 2007 by EADS France

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

import java.util.Iterator;

import static org.junit.Assert.*;
import org.junit.Ignore;

@Ignore("Utility class")
public class QSortedTreeTest
{
	protected QSortedTree<Integer> tree;

	private Integer [] unitTestInit(int n)
	{
		assert tree.isEmpty();
		Integer [] ret = new Integer[n];
		for (int i = 0; i < ret.length; i++)
			ret[i] = Integer.valueOf(i);
		for (int i = 0; i < ret.length; i++)
			tree.insert(ret[i], i);
		return ret;
	}
	
	protected final void unitTestIterator(int n)
	{
		Integer [] iii = unitTestInit(n);
		int i = 0;
		for (Iterator<QSortedTree.Node<Integer>> it = tree.iterator(); it.hasNext(); i++)
		{
			QSortedTree.Node<Integer> node = it.next();
			assertTrue("Wrong iterator: "+i+" != "+node.getData(), node.getData().equals(Integer.valueOf(i)));
		}
		assertTrue("Wrong iterator", i == n);
		i = n-1;
		for (Iterator<QSortedTree.Node<Integer>> it = tree.backwardIterator(); it.hasNext(); i--)
		{
			QSortedTree.Node<Integer> node = it.next();
			assertTrue("Wrong backward iterator: "+i+" != "+node.getData(), node.getData().equals(Integer.valueOf(i)));
		}
		assertTrue("Wrong backward iterator", i == -1);
		for (i = 0; i < iii.length; i++)
			tree.remove(iii[i]);
		assertTrue("Tree not empty", tree.isEmpty());
	}
	
	protected final void unitTest1(int n)
	{
		// Remove in ascending order
		Integer [] iii = unitTestInit(n);
		for (int i = 0; i < iii.length; i++)
			tree.remove(iii[i]);
		assertTrue("Tree not empty", tree.isEmpty());
	}
	
	protected final void unitTest2(int n)
	{
		// Remove in descending order
		Integer [] iii = unitTestInit(n);
		for (int i = iii.length - 1; i >= 0; i--)
			tree.remove(iii[i]);
		assertTrue("Tree not empty", tree.isEmpty());
	}
	
	protected final void unitTest3(int n)
	{
		Integer [] iii = unitTestInit(n);
		for (int i = 0; i < iii.length / 2; i++)
		{
			tree.remove(iii[i]);
			tree.remove(iii[i+iii.length / 2]);
		}
		assertTrue("Tree not empty", tree.isEmpty());
	}
	
	protected final void unitTest4(int n)
	{
		Integer [] iii = unitTestInit(n);
		for (int i = 0; i < iii.length / 2; i++)
		{
			tree.remove(iii[iii.length / 2+i]);
			tree.remove(iii[iii.length / 2-1-i]);
		}
		assertTrue("Tree not empty", tree.isEmpty());
	}
	
	protected final void unitTest5(int n, int s)
	{
		int prime = gnu.trove.PrimeFinder.nextPrime(n);
		Integer [] iii = unitTestInit(prime);
		int index = 1;
		for (int i = 0; i < prime; i++)
		{
			index += s;
			while (index >= prime)
				index -= prime;
			tree.remove(iii[index]);
		}
		assertTrue("Tree not empty", tree.isEmpty());
	}
	
	protected final void unitTest6(int n, int s)
	{
		int prime = gnu.trove.PrimeFinder.nextPrime(n);
		Integer [] iii = new Integer[prime];
		for (int i = 0; i < iii.length; i++)
			iii[i] = Integer.valueOf(i);
		int index = 1;
		for (int i = 0; i < prime; i++)
		{
			index += s;
			while (index >= prime)
				index -= prime;
			tree.insert(iii[index], index);
		}
		index = 3;
		for (int i = 0; i < prime; i++)
		{
			index += s;
			while (index >= prime)
				index -= prime;
			tree.remove(iii[index]);
		}
		assertTrue("Tree not empty", tree.isEmpty());
	}
}
