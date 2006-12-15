/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005,2006 by EADS CRC

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
 * PAVL binary trees to store quality factors.
 * Main ideas come from Ben Pfaff's <a href="http://adtinfo.org/">GNU libavl</a>.
 * These trees are used to sort vertices, edges, or triangles according
 * to their quality factors, and to process them in increasing or decreasing
 * order after they have been sorted.  See examples in algorithms from
 * {@link org.jcae.mesh.amibe.algos3d}.
 */
public class PAVLSortedTree extends QSortedTree
{
	private static Logger logger = Logger.getLogger(PAVLSortedTree.class);	
	
	private static class Node extends QSortedTree.Node
	{
		//  balanceFactor = height(rightSubTree) - height(leftSubTree)
		private int balanceFactor = 0;
		
		public Node(Object o, double v)
		{
			super(o, v);
		}
		
		public QSortedTree.Node [] newChilds()
		{
			return new Node[2];
		}

		public void reset(double v)
		{
			super.reset(v);
			balanceFactor = 0;
		}
		
		/* Single left rotation
		    A                    B
		   / \      ------>     / \
		  T1  B                A   T3
		     / \              / \
		    T2 T3            T1 T2
		*/
		public QSortedTree.Node rotateL()
		{
			Node right = (Node) super.rotateL();
			if (right.balanceFactor != 0)
			{
				assert right.balanceFactor == 1;
				right.balanceFactor = 0;
				balanceFactor = 0;
			}
			else
			{
				// This case happens only when removing
				// a node below T1.
				right.balanceFactor = -1;
				balanceFactor = 1;
			}
			return right;
		}
		
		/* Single right rotation
		          B                    A
		         / \     ------->     / \
		        A  T3                T1  B
		       / \                      / \
		      T1 T2                    T2 T3
		*/
		public QSortedTree.Node rotateR()
		{
			//logger.debug("Single right rotation");
			Node left = (Node) super.rotateR();
			if (left.balanceFactor != 0)
			{
				assert left.balanceFactor == -1;
				left.balanceFactor = 0;
				balanceFactor = 0;
			}
			else
			{
				// This case happens only when removing
				// a node below T4.
				left.balanceFactor = 1;
				balanceFactor = -1;
			}
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
		public QSortedTree.Node rotateRL()
		{
			Node newRoot = (Node) super.rotateRL();
			assert balanceFactor == 2;
			if (newRoot.balanceFactor == 1)
			{
				// T2 is null, T3 != null
				newRoot.balanceFactor = 0;
				balanceFactor = -1;
				((Node) newRoot.child[1]).balanceFactor = 0;
			}
			else if (newRoot.balanceFactor == -1)
			{
				// T3 is null, T2 != null
				newRoot.balanceFactor = 0;
				balanceFactor = 0;
				((Node) newRoot.child[1]).balanceFactor = 1;
			}
			else
			{
				// T2 and T3 != null
				balanceFactor = 0;
				((Node) newRoot.child[1]).balanceFactor = 0;
			}
			return newRoot;
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
		public QSortedTree.Node rotateLR()
		{
			Node newRoot = (Node) super.rotateLR();
			assert balanceFactor == -2;
			// Balance factors, T1 and T4 are not null,
			// and T2 and T3 cannot be both null.
			if (newRoot.balanceFactor == -1)
			{
				// T3 is null, T2 != null
				newRoot.balanceFactor = 0;
				balanceFactor = 1;
				((Node) newRoot.child[0]).balanceFactor = 0;
			}
			else if (newRoot.balanceFactor == 1)
			{
				// T2 is null, T3 != null
				newRoot.balanceFactor = 0;
				balanceFactor = 0;
				((Node) newRoot.child[0]).balanceFactor = -1;
			}
			else
			{
				// T2 and T3 != null
				balanceFactor = 0;
				((Node) newRoot.child[0]).balanceFactor = 0;
			}
			return newRoot;
		}

		public String toString()
		{
			return super.toString()+" bal. "+balanceFactor;
		}
	}
	
	public final QSortedTree.Node newNode(Object o, double v)
	{
		return new Node(o, v);
	}

	public final boolean insertNode(QSortedTree.Node o)
	{
		Node node = (Node) o;
		Node current = (Node) root.child[0];
		Node parent = (Node) root;
		Node topNode = current;
		int lastDir = 0;
		while (current != null)
		{
			if (node.compareTo(current) < 0)
				lastDir = 0;
			else
				lastDir = 1;
			if (current.balanceFactor != 0)
				topNode = current;
			parent = current;
			current = (Node) current.child[lastDir];
		}
		// Insert node
		parent.child[lastDir] = node;
		node.parent = parent;
		if (topNode == null)
			return true;
		// Update balance factors
		for (current = node; current != topNode; current = parent)
		{
			parent = (Node) current.parent;
			if (parent.child[0] == current)
				parent.balanceFactor--;
			else
				parent.balanceFactor++;
		}
		parent = (Node) topNode.parent;
		// Balance subtree
		Node newRoot = null;
		if (topNode.balanceFactor == -2)
		{
			Node left = (Node) topNode.child[0];
			if (left.balanceFactor == -1)
				newRoot = (Node) topNode.rotateR();
			else
				newRoot = (Node) topNode.rotateLR();
		}
		else if (topNode.balanceFactor == 2)
		{
			Node right = (Node) topNode.child[1];
			if (right.balanceFactor == 1)
				newRoot = (Node) topNode.rotateL();
			else
				newRoot = (Node) topNode.rotateRL();
		}
		else
			return true;
		
		if (parent.child[0] == topNode)
			parent.child[0] = newRoot;
		else
			parent.child[1] = newRoot;
		return true;
	}
	
	public final QSortedTree.Node removeNode(QSortedTree.Node o)
	{
		assert o != null;
		Node p = (Node) o;
		Node ret = p;
		if (logger.isDebugEnabled())
			logger.debug("Value: "+ret);
		int lastDir = 0;
		Node q = (Node) p.parent;
		if (q.child[1] == p)
			lastDir = 1;
		if (p.child[1] == null)
		{
			/* Deletion of p
			    q                    q
			   / \      ------>     / \
			  T1  p                T1 T2
			     /
			    T2
			*/
			q.child[lastDir] = p.child[0];
			if (q.child[lastDir] != null)
				q.child[lastDir].parent = p.parent;
		}
		else if (p.child[0] == null)
		{
			/* Deletion of p
			    q                    q
			   / \      ------>     / \
			  T1  p                T1 T2
			       \
			       T2
			*/
			q.child[lastDir] = p.child[1];
			if (q.child[lastDir] != null)
				q.child[lastDir].parent = p.parent;
		}
		else
		{
			//  p has two children.
			Node r = (Node) p.child[1];
			if (r.child[0] == null)
			{
				/* Deletion of p
				    q                    q
				   / \      ------>     / \
				  T1  p                T1  r
				     / \                  / \
				    T2  r                T2 T3
				         \
				         T3
				*/
				r.child[0] = p.child[0];
				q.child[lastDir] = r;
				r.parent = q;
				r.child[0].parent = r;
				r.balanceFactor = p.balanceFactor;
				// Tree above r needs to be rebalanced
				q = r;
				lastDir = 1;
			}
			else
			{
				//  Swap p with its successor node in the tree,
				//  then delete p
				r = (Node) r.child[0];
				while (r.child[0] != null)
					r = (Node) r.child[0];
				Node s = (Node) r.parent;
				s.child[0] = r.child[1];
				if (s.child[0] != null)
					s.child[0].parent = s;
				r.child[0] = p.child[0];
				r.child[1] = p.child[1];
				q.child[lastDir] = r;
				r.child[0].parent = r;
				r.child[1].parent = r;
				r.parent = q;
				r.balanceFactor = p.balanceFactor;
				// Tree above s needs to be rebalanced
				q = s;
				lastDir = 0;
			}
		}
		// A node in the direction lastDir has been deleted from q,
		// so the nodes above may need to be updated.
		int dir = lastDir;
		while (q != root)
		{
			Node y = q;
			q = (Node) q.parent;
			lastDir = dir;
			if (q.child[0] == y)
				dir = 0;
			else
				dir = 1;
			if (lastDir == 0)
			{
				y.balanceFactor++;
				// A node had been deleted on the left
				// branch of q.  If the new balance is 0,
				// height tree has changed, so upper nodes
				// need to be checked too.  If it is 1,
				// its height had not changed and processing
				// can stop.  If it is 2, this node needs
				// to be rebalanced, and processing can stop
				// if its balance factor becomes 1.
				if (y.balanceFactor == 2)
				{
					if (((Node) y.child[1]).balanceFactor == -1)
						q.child[dir] = y.rotateRL();
					else
						q.child[dir] = y.rotateL();
				}
				if (y.balanceFactor == 1)
					break;
			}
			else
			{
				y.balanceFactor--;
				if (y.balanceFactor == -2)
				{
					if (((Node) y.child[0]).balanceFactor == 1)
						q.child[dir] = y.rotateLR();
					else
						q.child[dir] = y.rotateR();
				}
				if (y.balanceFactor == -1)
					break;
			}
		}
		return ret;
	}

	public static void main(String args[])
	{
		QSortedTree tree = new PAVLSortedTree();
		// Check with various lengths
		for (int n = 10; n < 100; n+=2)
		{
			try
			{
				tree.unitTest1(n);
				tree.unitTest2(n);
				tree.unitTest3(n);
				tree.unitTest4(n);
			}
			catch (Exception ex)
			{
				System.out.println("Failed with length "+n);
				ex.printStackTrace();
				System.exit(1);
			}
		}
		// Insert and remove in random order
		for (int n = 3; n < 100; n+=2)
		{
			try
			{
				tree.unitTest5(200, n);
				tree.unitTest6(200, n);
			}
			catch (Exception ex)
			{
				System.out.println("Failed with step "+n);
				ex.printStackTrace();
				System.exit(1);
			}
		}
		System.out.println("ok");
	}
}
