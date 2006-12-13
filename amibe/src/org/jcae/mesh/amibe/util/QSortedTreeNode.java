/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2006 by EADS CRC

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

import java.io.Serializable;

/**
 * Binary tree node.  
 * These trees are used to sort vertices, edges, or triangles according
 * to their quality factors, and to process them in increasing or decreasing
 * order after they have been sorted.  They differ from casual binary trees in
 * that duplicate quality factors are allowed.
 */
public abstract class QSortedTreeNode implements Serializable
{
	protected final Object data;
	protected double value;
	protected final QSortedTreeNode [] child = newChilds();
	protected QSortedTreeNode parent = null;
	
	public abstract QSortedTreeNode [] newChilds();

	public QSortedTreeNode(Object o, double v)
	{
		data = o;
		value = v;
	}
		
	public void reset(double v)
	{
		child[0] = child[1] = parent = null;
		value = v;
	}
		
	/* Left rotation
	    A                    B
	   / \      ------>     / \
	  T1  B                A   T3
	     / \              / \
	    T2 T3            T1 T2
	*/
	public QSortedTreeNode rotateL()
	{
		//logger.debug("Single left rotation");
		QSortedTreeNode right = child[1];
		child[1] = right.child[0];
		right.child[0] = this;
		right.parent = parent;
		parent = right;
		if (child[1] != null)
			child[1].parent = this;
		return right;
	}
		
	/* Right rotation
	          B                    A
	         / \     ------->     / \
	        A  T3                T1  B
	       / \                      / \
	      T1 T2                    T2 T3
	*/
	public QSortedTreeNode rotateR()
	{
		//logger.debug("Single right rotation");
		QSortedTreeNode left = child[0];
		child[0] = left.child[1];
		left.child[1] = this;
		left.parent = parent;
		parent = left;
		if (child[0] != null)
			child[0].parent = this;
		return left;
	}
	
	/* Right+left rotation
	   A                   A                     B
	  / \      ------>    / \      ------>     /   \
	 T1  C               T1  B                A     C
	    / \                 / \              / \   / \
	   B  T4               T2  C            T1 T2 T3 T4
	  / \                     / \
	 T2 T3                   T3 T4
	*/
	public QSortedTreeNode rotateRL()
	{
		//logger.debug("Right+left rotation");
		QSortedTreeNode right = child[1];          // C
		QSortedTreeNode newRoot = right.child[0];  // B
		// Right rotation
		right.child[0] = newRoot.child[1];
		newRoot.child[1] = right;
		// Left rotation
		child[1] = newRoot.child[0];
		newRoot.child[0] = this;
		// Parent pointers
		newRoot.parent = parent;
		parent = newRoot;
		right.parent = newRoot;
		if (right.child[0] != null)
			right.child[0].parent = right;
		if (child[1] != null)
			child[1].parent = this;
		return newRoot;
	}
	
	/*  Left+right rotation
	    C                    C                    B
	   / \      ------>     / \    ------>      /   \
	  A  T4                B  T4               A     C
	 / \                  / \                 / \   / \
	T1  B                A  T3               T1 T2 T3 T4
	   / \              / \
	  T2 T3            T1 T2
	*/
	public QSortedTreeNode rotateLR()
	{
		//logger.debug("Left+right rotation");
		QSortedTreeNode left = child[0];         // A
		QSortedTreeNode newRoot = left.child[1]; // B

		assert newRoot != null;
		// Left rotation
		left.child[1] = newRoot.child[0];
		newRoot.child[0] = left;
		// Right rotation
		child[0] = newRoot.child[1];
		newRoot.child[1] = this;
		// Parent pointers
		newRoot.parent = parent;
		left.parent = newRoot;
		parent = newRoot;
		if (left.child[1] != null)
			left.child[1].parent = left;
		if (child[0] != null)
			child[0].parent = this;
		return newRoot;
	}

	// The following 4 methods are useful for tree traversal.
	// A NullPointerException is raised if they are used on an empty tree!
	public QSortedTreeNode firstNode()
	{
		QSortedTreeNode current = this;
		while (current.child[0] != null)
			current = current.child[0];
		return current;
	}

	public QSortedTreeNode lastNode()
	{
		QSortedTreeNode current = this;
		while (current.child[1] != null)
			current = current.child[1];
		return current;
	}

	public QSortedTreeNode previousNode()
	{
		QSortedTreeNode current = this;
		if (current.child[0] != null)
		{
			current = current.child[0];
			while (current.child[1] != null)
				current = current.child[1];
			return current;
		}
		while (current.parent != null && current.parent.child[1] != current)
			current = current.parent;
		return current.parent;
	}

	public QSortedTreeNode nextNode()
	{
		QSortedTreeNode current = this;
		if (current.child[1] != null)
		{
			current = current.child[1];
			while (current.child[0] != null)
				current = current.child[0];
			return current;
		}
		// Our implementation has a fake root node.
		while (current.parent.child[0] != current)
			current = current.parent;
		if (current.parent.parent == null)
			return null;
		else
			return current.parent;
	}
}
