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

import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 * Implementation of PAVL binary trees to store quality factors.
 * It is based on excellent Ben Pfaff's GNU libavl http://adtinfo.org/
 * These trees are used to sort vertices, edges, or triangles according
 * to their quality factors.  They are traversed according to quality
 * factors, and duplicate values are allowed.
 * For these reasons, this class needed some customization.
 */
public class PAVLSortedTree
{
	private static Logger logger = Logger.getLogger(PAVLSortedTree.class);	
	private PAVLSortedTreeNode root;
	// Current node for tree traversal.  As there is no reason to have
	// concurrent traversals, we do not need to define proper traversers.
	private PAVLSortedTreeNode travNode;
	// Mapping between objects and tree nodes
	private HashMap map = new HashMap();
	private int count = 0;
	
	private class PAVLSortedTreeNode
	{
		private double value;
		private Object data;
		//  balanceFactor = height(rightSubTree) - height(leftSubTree)
		private int balanceFactor = 0;
		private PAVLSortedTreeNode [] child = new PAVLSortedTreeNode[2];
		private PAVLSortedTreeNode parent = null;
		
		public PAVLSortedTreeNode(Object o, double v)
		{
			data = o;
			value = v;
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
		public PAVLSortedTreeNode rotateL()
		{
			//logger.debug("Single left rotation");
			PAVLSortedTreeNode right = child[1];
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
		public PAVLSortedTreeNode rotateR()
		{
			//logger.debug("Single right rotation");
			PAVLSortedTreeNode left = child[0];
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
		public PAVLSortedTreeNode rotateRL()
		{
			//logger.debug("Right+left rotation");
			assert balanceFactor == 2;
			PAVLSortedTreeNode right = child[1];
			assert right.balanceFactor == -1;
			PAVLSortedTreeNode newRoot = right.child[0];
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
		public PAVLSortedTreeNode rotateLR()
		{
			//logger.debug("Left+right rotation");
			assert balanceFactor == -2;
			PAVLSortedTreeNode left = child[0];
			assert left.balanceFactor == 1;
			PAVLSortedTreeNode newRoot = left.child[1];

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
	}
	
	/**
	 * Pretty-print this tree.
	 */
	public void show()
	{
		if (root == null)
		{
			System.out.println("Empty tree");
			return;
		}
		System.out.println("Tree:");
		showNode(root.child[0]);
	}
	
	private static void showNode(PAVLSortedTreeNode node)
	{
		String r = "Key: "+node.value+"  bal. "+node.balanceFactor;
		if (node.child[0] != null)
			r += " Left -> "+node.child[0].value;
		if (node.child[1] != null)
			r += " Right -> "+node.child[1].value;
		if (node.parent != null)
			r += " Parent -> "+node.parent.value;
		System.out.println(r);
		if (node.child[0] != null)
		{
			assert node.child[0].parent == node;
			showNode(node.child[0]);
		}
		if (node.child[1] != null)
		{
			assert node.child[1].parent == node;
			showNode(node.child[1]);
		}
	}
	
	public void showValues()
	{
		if (root == null)
		{
			System.out.println("Empty tree");
			return;
		}
		System.out.println("Sorted values:");
		for (Object current = last(); current != null; current = prev())
		{
			PAVLSortedTreeNode node = (PAVLSortedTreeNode) map.get(current);
			assert node != null;
			System.out.println("  "+node.value);
		}
	}
	
	/**
	 * Insert a node to the tree.  Tree is soorted according to
	 * <code>value</code>, and duplicates are not checked.
	 * @param o      object
	 * @param value  quality factor
	 */
	public final synchronized void insert(Object o, double value)
	{
		PAVLSortedTreeNode node = new PAVLSortedTreeNode(o, value);
		map.put(o, node);
		if (root == null)
		{
			root = new PAVLSortedTreeNode(null, 0.0);
			// A fake node is inserted below root so that root
			// never has to be updated.  Tree begins at
			// root.child[0].
			root.child[0] = node;
			node.parent = root;
			return;
		}
		count++;
		PAVLSortedTreeNode current = root.child[0];
		PAVLSortedTreeNode parent = root;
		PAVLSortedTreeNode topNode = current;
		int lastDir = 0;
		while (current != null)
		{
			if (current.value > node.value)
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
		PAVLSortedTreeNode newRoot = null;
		if (topNode.balanceFactor == -2)
		{
			PAVLSortedTreeNode left = topNode.child[0];
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
			PAVLSortedTreeNode right = topNode.child[1];
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
	 * Update the quality factor of an object.
	 * @param o      object being updated
	 * @param value  new quality factor
	 */
	public final synchronized void update(Object o, double value)
	{
		logger.debug("Update "+o+" to "+value);
		remove(o);
		insert(o, value);
	}
	
	/**
	 * Remove the node associated to an object from the tree.
	 * @param o      object being removed
	 */
	public final synchronized double remove(Object o)
	{
		PAVLSortedTreeNode p = (PAVLSortedTreeNode) map.get(o);
		logger.debug("Remove "+p+" "+o);
		if (p == null)
			return -1.0;
		assert p != null;
		double ret = p.value;
		map.remove(o);
		count--;
		int lastDir = 0;
		PAVLSortedTreeNode q = p.parent;
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
			PAVLSortedTreeNode r = p.child[1];
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
				PAVLSortedTreeNode s = r.parent;
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
		while (q != root)
		{
			PAVLSortedTreeNode y = q;
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
					PAVLSortedTreeNode right = y.child[1];
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
					PAVLSortedTreeNode x = y.child[0];
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
		if (root.child[0] == null)
			root = null;
		return ret;
	}
	
	/**
	 * Return the object with the lowest quality factor.
	 * @return the object with the lowest quality factor.
	 */
	public final int size()
	{
		return map.size();
	}
	
	/**
	 * Return the object with the lowest quality factor.
	 * @return the object with the lowest quality factor.
	 */
	public final synchronized Object first()
	{
		if (root == null || root.child[0] == null)
			return null;
		travNode = root.child[0];
		while (travNode.child[0] != null)
			travNode = travNode.child[0];
		return travNode.data;
	}
	
	/**
	 * Return the object with the highest quality factor.
	 * @return the object with the highest quality factor.
	 */
	public final synchronized Object last()
	{
		if (root == null || root.child[0] == null)
			return null;
		travNode = root.child[0];
		while (travNode.child[1] != null)
			travNode = travNode.child[1];
		return travNode.data;
	}
	
	/**
	 * When traversing tree, return object with immediate higher quality
	 * factor.
	 * @return the object with the immediate higher quality factor.
	 */
	public final synchronized Object next()
	{
		if (travNode == null)
			return null;
		PAVLSortedTreeNode right = travNode.child[1];
		if (right != null)
		{
			travNode = right;
			while (travNode.child[0] != null)
				travNode = travNode.child[0];
			return travNode.data;
		}
		// This loop always terminate
		while (travNode.parent.child[0] != travNode)
			travNode = travNode.parent;
		travNode = travNode.parent;
		if (travNode == root)
		{
			travNode = null;
			return null;
		}
		return travNode.data;
	}
	
	/**
	 * When traversing tree, return object with immediate lower quality
	 * factor.
	 * @return the object with the immediate lower quality factor.
	 */
	public final synchronized Object prev()
	{
		if (travNode == null)
			return null;
		PAVLSortedTreeNode left = travNode.child[0];
		if (left != null)
		{
			travNode = left;
			while (travNode.child[1] != null)
				travNode = travNode.child[1];
			return travNode.data;
		}
		// This loop always terminate
		while (travNode.parent.child[1] != travNode && travNode.parent != root)
			travNode = travNode.parent;
		travNode = travNode.parent;
		if (travNode == root)
		{
			travNode = null;
			return null;
		}
		return travNode.data;
	}
	
	public static void main(String args[])
	{
		PAVLSortedTree tree;
		//  Test single right rotation
		tree = new PAVLSortedTree();
		Integer [] iii = new Integer[100];
		for (int i = 0; i < iii.length; i++)
			iii[i] = new Integer(i);
			
		for (int i = 0; i < 10; i++)
			tree.insert(iii[i], (double) i);
		tree.show();
		tree.remove(iii[9]);
		tree.remove(iii[1]);
		tree.remove(iii[0]);
		tree.show();
		tree = new PAVLSortedTree();
		for (int i = 9; i >= 0; i--)
			tree.insert(iii[i], (double) i);
		tree.show();
		tree.remove(iii[0]);
		tree.remove(iii[8]);
		tree.remove(iii[9]);
		tree.show();
		tree.showValues();
	}
		
}
