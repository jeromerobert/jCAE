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

package org.jcae.mesh.amibe.util;

import org.apache.log4j.Logger;

/**
 * Cell of PAVL binary trees.
 */
public class PAVLNode
{
	private static Logger logger = Logger.getLogger(PAVLNode.class);	
	public Object data;
	public double key;
	//  balanceFactor = height(rightSubTree) - height(leftSubTree)
	private int balanceFactor = 0;
	protected PAVLNode [] child;
	protected PAVLNode parent;
	
	public PAVLNode(Object o, double k)
	{
		data = o;
		key = k;
		child = new PAVLNode[2];
	}
	
	public double getKey()
	{
		return key;
	}
	
	public void setKey(double k)
	{
		key = k;
	}
	
	public Object getValue()
	{
		return data;
	}
	
	public void setValue(Object o)
	{
		data = o;
	}
	
	/* Single left rotation
	            A                     B
	           / \      ------>     /   \
	          T1  B                A     C
	             / \              / \   / \
	            T2  C            T1 T2 T3 T4
	               / \
	              T3 T4
	WARNING: Balance factors are not updated because they differ
	         when inserting and deleting nodes
	*/
	protected PAVLNode rotateL()
	{
		logger.debug("Single left rotation");
		PAVLNode right = child[1];
		assert right.balanceFactor >= 0;
		child[1] = right.child[0];
		right.child[0] = this;
		right.parent = parent;
		parent = right;
		if (child[1] != null)
			child[1].parent = this;
		return right;
	}
	
	/* Single right rotation
	                  C                   B
	                 / \     ------->   /   \
	                B  T4              A     C
	               / \                / \   / \
	              A  T3              T1 T2 T3 T4
	             / \
	            T1 T2
	WARNING: Balance factors are not updated because they differ
	         when inserting and deleting nodes
	*/
	protected PAVLNode rotateR()
	{
		logger.debug("Single right rotation");
		PAVLNode left = child[0];
		assert left.balanceFactor <= 0;
		child[0] = left.child[1];
		left.child[1] = this;
		left.parent = parent;
		parent = left;
		if (child[0] != null)
			child[0].parent = this;
		return left;
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
	protected PAVLNode rotateRL()
	{
		logger.debug("Right+left rotation");
		assert balanceFactor == 2;
		PAVLNode right = child[1];
		assert right.balanceFactor == -1;
		PAVLNode newRoot = right.child[0];
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
		if (newRoot.balanceFactor == 1)
		{
			balanceFactor = -1;
			right.balanceFactor = 0;
			newRoot.balanceFactor = 0;
		}
		else if (newRoot.balanceFactor == -1)
		{
			balanceFactor = 0;
			right.balanceFactor = 1;
			newRoot.balanceFactor = 0;
		}
		else
		{
			balanceFactor = 0;
			right.balanceFactor = 0;
		}
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
	protected PAVLNode rotateLR()
	{
		logger.debug("Left+right rotation");
		assert balanceFactor == -2;
		PAVLNode left = child[0];
		assert left.balanceFactor == 1;
		PAVLNode newRoot = left.child[1];

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
		if (newRoot.balanceFactor == -1)
		{
			left.balanceFactor = 0;
			balanceFactor = 1;
			newRoot.balanceFactor = 0;
		}
		else if (newRoot.balanceFactor == 1)
		{
			left.balanceFactor = -1;
			balanceFactor = 0;
			newRoot.balanceFactor = 0;
		}
		else
		{
			left.balanceFactor = 0;
			balanceFactor = 0;
		}
		if (left.child[1] != null)
			left.child[1].parent = left;
		if (child[0] != null)
			child[0].parent = this;
		return newRoot;
	}
	
	/**
	 * Insert a node into this subtree.
	 *
	 * @param node    node to insert.
	 */
	public final synchronized void insert(PAVLNode node)
	{
		PAVLNode parent = this;
		PAVLNode current = child[0];
		PAVLNode topNode = current;
		int lastDir = 0;
		while (current != null)
		{
			if (current.key > node.key)
				lastDir = 0;
			else
				lastDir = 1;
			if (current.balanceFactor != 0)
				topNode = current;
			parent = current;
			current = current.child[lastDir];
		}
		// Insert node
		parent.child[lastDir] = node;
		node.parent = parent;
		// Update balance factors
		for (current = node; current != topNode; current = parent)
		{
			parent = current.parent;
			if (parent.child[0] == current)
				parent.balanceFactor--;
			else
				parent.balanceFactor++;
		}
		parent = topNode.parent;
		// Balance subtree
		PAVLNode newRoot = null;
		if (topNode.balanceFactor == -2)
		{
			PAVLNode left = topNode.child[0];
			if (left.balanceFactor == -1)
			{
				newRoot = topNode.rotateR();
				newRoot.balanceFactor = 0;
				topNode.balanceFactor = 0;
			}
			else
				newRoot = topNode.rotateLR();
		}
		else if (topNode.balanceFactor == 2)
		{
			PAVLNode right = topNode.child[1];
			if (right.balanceFactor == 1)
			{
				newRoot = topNode.rotateL();
				topNode.balanceFactor = 0;
				newRoot.balanceFactor = 0;
			}
			else
				newRoot = topNode.rotateRL();
		}
		else
			return;
		
		if (parent.child[0] == topNode)
			parent.child[0] = newRoot;
		else
			parent.child[1] = newRoot;
	}
	
	/**
	 * Remove a node from this subtree.  there is no check to ensure that
	 * this node really belongs to this subtree.
	 *
	 * @param p      node being removed
	 */
	protected final synchronized void remove(PAVLNode p)
	{
		int lastDir = 0;
		PAVLNode q = p.parent;
		if (q.child[1] == p)
			lastDir = 1;
		if (p.child[1] == null)
		{
			/* Deletion of C
			    A                    A
			   / \      ------>     / \
			  T1  C                T1  B
			     /
			    B
			*/
			q.child[lastDir] = p.child[0];
			if (q.child[lastDir] != null)
				q.child[lastDir].parent = p.parent;
		}
		else if (p.child[0] == null)
		{
			/* Deletion of A
			    C                    C
			   / \      ------>     / \
			  A   T1               B  T1
			   \  
			    B
			*/
			q.child[lastDir] = p.child[1];
			if (q.child[lastDir] != null)
				q.child[lastDir].parent = p.parent;
		}
		else
		{
			//  p has two children.  Swap p with its
			//  successor node in the tree, then delete p
			PAVLNode r = p.child[1];
			if (r.child[0] == null)
			{
				// Move r into p
				r.child[0] = p.child[0];
				q.child[lastDir] = r;
				r.parent = p.parent;
				r.child[0].parent = r;
				r.balanceFactor = p.balanceFactor;
				// Tree above r needs to be rebalanced
				q = r;
				lastDir = 1;
			}
			else
			{
				// Walk on the left branch to a leaf 
				r = r.child[0];
				while (r.child[0] != null)
					r = r.child[0];
				PAVLNode s = r.parent;
				s.child[0] = r.child[1];
				if (s.child[0] != null)
					s.child[0].parent = s;
				r.child[0] = p.child[0];
				r.child[1] = p.child[1];
				q.child[lastDir] = r;
				r.child[0].parent = r;
				r.child[1].parent = r;
				r.parent = p.parent;
				r.balanceFactor = p.balanceFactor;
				// Tree above s needs to be rebalanced
				q = s;
				lastDir = 0;
			}
		}
		// A node in the direction lastDir has been deleted from q,
		// so the nodes above may need to be updated.
		while (q != this)
		{
			PAVLNode y = q;
			q = y.parent;
			if (lastDir == 0)
			{
				if (q.child[0] == y)
					lastDir = 0;
				else
					lastDir = 1;
				y.balanceFactor++;
				// Previous balance was -1, 0 or 1.
				// A node had been deleted on the left
				// branch of q.  If the new balance is 0,
				// height tree has changed, so upper nodes
				// need to be checked too.  If it is 1,
				// its height had not changed and processing
				// can stop.  If it is 2, this node needs
				// to be rebalanced.
				if (y.balanceFactor == 1)
					break;
				else if (y.balanceFactor == 2)
				{
					PAVLNode right = y.child[1];
					if (right.balanceFactor == -1)
						q.child[lastDir] = y.rotateRL();
					else
					{
						q.child[lastDir] = y.rotateL();
						if (right.balanceFactor == 0)
						{
							right.balanceFactor = -1;
							y.balanceFactor = 1;
							break;
						}
						else
						{
							right.balanceFactor = 0;
							y.balanceFactor = 0;
							y = right;
						}
					}
				}
			}
			else
			{
				if (q.child[0] == y)
					lastDir = 0;
				else
					lastDir = 1;
				y.balanceFactor--;
				if (y.balanceFactor == -1)
					break;
				else if (y.balanceFactor == -2)
				{
					PAVLNode x = y.child[0];
					if (x.balanceFactor == 1)
						q.child[lastDir] = y.rotateLR();
					else
					{
						q.child[lastDir] = y.rotateR();
						if (x.balanceFactor == 0)
						{
							x.balanceFactor = 1;
							y.balanceFactor = -1;
							break;
						}
						else
						{
							x.balanceFactor = 0;
							y.balanceFactor = 0;
							y = x;
						}
					}
				}
			}
		}
	}
	
	protected void show()
	{
		String r = "Key: "+key+"  bal. "+balanceFactor;
		if (child[0] != null)
			r += " Left -> "+child[0].key;
		if (child[1] != null)
			r += " Right -> "+child[1].key;
		if (parent != null)
			r += " Parent -> "+parent.key;
		System.out.println(r);
		if (child[0] != null)
		{
			assert child[0].parent == this;
			child[0].show();
		}
		if (child[1] != null)
		{
			assert child[1].parent == this;
			child[1].show();
		}
	}
	
}
