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

import static org.junit.Assert.*;
import org.junit.Test;

public class PAVLTreeIntArrayDupTest
{
	static int [] ijk = new int[3];
	static
	{
		for (int i = 0; i < 3; i++)
			ijk[i] = 0;
	}

	@Test public void rightRotation()
	{
		//  Test single right rotation
		PAVLTreeIntArrayDup tree = new PAVLTreeIntArrayDup();
		ijk[0] = 10; tree.insert(ijk, ijk[0]);
		ijk[0] = 8; tree.insert(ijk, ijk[0]);
		ijk[0] = 6; tree.insert(ijk, ijk[0]);
		ijk[0] = 4; tree.insert(ijk, ijk[0]);
		ijk[0] = 2; tree.insert(ijk, ijk[0]);
		ijk[0] = 1; tree.insert(ijk, ijk[0]);
	}
	
	@Test public void leftRightRotation()
	{
		//  Test left+right rotation
		PAVLTreeIntArrayDup tree = new PAVLTreeIntArrayDup();
		tree = new PAVLTreeIntArrayDup();
		ijk[0] = 10; tree.insert(ijk, ijk[0]);
		ijk[0] = 8; tree.insert(ijk, ijk[0]);
		ijk[0] = 6; tree.insert(ijk, ijk[0]);
		ijk[0] = 4; tree.insert(ijk, ijk[0]);
		ijk[0] = 2; tree.insert(ijk, ijk[0]);
		ijk[0] = 5; tree.insert(ijk, ijk[0]);
	}
	
	@Test public void leftRotation()
	{
		//  Test single left rotation
		PAVLTreeIntArrayDup tree = new PAVLTreeIntArrayDup();
		ijk[0] = 2; tree.insert(ijk, ijk[0]);
		ijk[0] = 4; tree.insert(ijk, ijk[0]);
		ijk[0] = 6; tree.insert(ijk, ijk[0]);
		ijk[0] = 8; tree.insert(ijk, ijk[0]);
		ijk[0] = 10; tree.insert(ijk, ijk[0]);
		ijk[0] = 9; tree.insert(ijk, ijk[0]);
	}
	
	@Test public void rightLeftRotation()
	{
		//  Test right+left rotation
		PAVLTreeIntArrayDup tree = new PAVLTreeIntArrayDup();
		ijk[0] = 2; tree.insert(ijk, ijk[0]);
		ijk[0] = 4; tree.insert(ijk, ijk[0]);
		ijk[0] = 6; tree.insert(ijk, ijk[0]);
		ijk[0] = 8; tree.insert(ijk, ijk[0]);
		ijk[0] = 10; tree.insert(ijk, ijk[0]);
		ijk[0] = 5; tree.insert(ijk, ijk[0]);
	}
	
	@Test public void dummy()
	{
		//  Test single right rotation
		PAVLTreeIntArrayDup tree = new PAVLTreeIntArrayDup();
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
	}
	
	@Test public void large()
	{
		PAVLTreeIntArrayDup tree = new PAVLTreeIntArrayDup();
		// This number can be increased to test performance
		final int size = 200000;
		int dup = 0;
		for (int i = 0; i < size; i++)
		{
			ijk[0] = i / 6;
			if (tree.insert(ijk, i) != i)
				dup++;
		}
		assertTrue("Invalid number of duplicates: "+dup, dup == (size * 5 / 6));
		assertTrue("Invalid number of nodes: "+tree.size(), tree.size() == (1 + size / 6));
	}
}
