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

import java.util.HashMap;
import java.util.Iterator;
import java.io.Serializable;
import org.apache.log4j.Logger;

/**
 * Red-black binary trees to store quality factors.
 * Main ideas come from Ben Pfaff's <a href="http://adtinfo.org/">GNU libavl</a>.
 * These trees are used to sort vertices, edges, or triangles according
 * to their quality factors, and to process them in increasing or decreasing
 * order after they have been sorted.  See examples in algorithms from
 * {@link org.jcae.mesh.amibe.algos3d}.
 */
public class PRedBlackSortedTree extends QSortedTree
{
	private static Logger logger = Logger.getLogger(PRedBlackSortedTree.class);	
	private static byte BLACK = 0;
	private static byte RED = 1;
	private static class Node extends QSortedTreeNode
	{
		private byte color;
		
		public QSortedTreeNode [] newChilds()
		{
			return new Node[2];
		}

		public Node(Object o, double v)
		{
			super(o, v);
			color = RED;
		}
		
		public void reset(double v)
		{
			super.reset(v);
			color = BLACK;
		}

		public String toString()
		{
			return "Key: "+value+" color "+color;
		}
	}
	
	public final QSortedTreeNode newNode(Object o, double v)
	{
		return new Node(o, v);
	}

	public final void insertNode(QSortedTreeNode o)
	{
		Node node = (Node) o;
		Node current = (Node) root.child[0];
		Node parent = (Node) root;
		int lastDir = 0;
		int sibDir = 0;
		while (current != null)
		{
			if (current.value > node.value)
				lastDir = 0;
			else
				lastDir = 1;
			parent = current;
			current = (Node) current.child[lastDir];
		}
		// Insert node
		parent.child[lastDir] = node;
		node.parent = parent;
		if (parent == root)
		{
			node.color = BLACK;
			return;
		}
		node.color = RED;
		// Recolor and rebalance tree
		for (current = node; current != root; )
		{
			parent = (Node) current.parent;
			if (parent == root || parent.color == BLACK)
				break;
			// Current and parent are both colored RED
			Node grandparent = (Node) parent.parent;
			if (grandparent == root)
				break;
			if (grandparent.child[0] == parent)
				lastDir = 0;
			else
				lastDir = 1;
			sibDir = 1-lastDir;
			Node uncle = (Node) grandparent.child[sibDir];
			if (uncle != null && uncle.color == RED)
			{
				/* Recolor nodes and continue from
				   grandparent
			          gB                   gR
			         / \      ------>     / \
			        pR uR               pB  uB
			         \                    \
			         cR                   cR
				*/
				parent.color = BLACK;
				uncle.color = BLACK;
				grandparent.color = RED;
				current = grandparent;
			}
			else
			{
				if (parent.child[lastDir] != current)
				{
					/* Rotate to put red nodes on the
					   same side
				          gB                   gB
				         / \      ------>     / \
				        pR uB               cR  uB
				         \                  /
				         cR                pR
					*/
					if (lastDir == 0)
						grandparent.child[0] = parent.rotateL();
					else
						grandparent.child[1] = parent.rotateR();
					parent = current;
				}
				/* Rotate on opposite way and recolor
			          gB                   pB
			         / \      ------>     / \
			        pR uB               cR  gR
			       / \                      / \
			      cR xB                    xB uB
				*/
				Node greatgrandparent = (Node) grandparent.parent;
				grandparent.color = RED;
				parent.color = BLACK;
				if (greatgrandparent.child[0] == grandparent)
					lastDir = 0;
				else
					lastDir = 1;
				// lastDir has been modified, so use sibDir here
				if (sibDir == 1)
					greatgrandparent.child[lastDir] = grandparent.rotateR();
				else
					greatgrandparent.child[lastDir] = grandparent.rotateL();
				break;
			}
		}
		((Node) root.child[0]).color = BLACK;
	}
	
	public final double removeNode(QSortedTreeNode o)
	{
		Node p = (Node) o;
		double ret = p.value;
		if (logger.isDebugEnabled())
			logger.debug("Value: "+ret);
		int lastDir = 0;
		Node q = (Node) p.parent;
		if (q.child[1] == p)
			lastDir = 1;
		int sibdir = 1 - lastDir;
		Node r = (Node) p.child[1];
		if (r == null)
		{
			q.child[lastDir] = p.child[0];
			if (q.child[lastDir] != null)
				q.child[lastDir].parent = q;
		}
		else
		{
			byte color = RED;
			if (r.child[0] == null)
			{
				// Replace p by r
				r.parent = q;
				q.child[lastDir] = r;
				r.child[0] = p.child[0];
				if (r.child[0] != null)
					r.child[0].parent = r;
				color = p.color;
				p.color = r.color;
				r.color = color;
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
				if (r.child[0] != null)
					r.child[0].parent = r;
				r.child[1].parent = r;
				r.parent = q;
				color = p.color;
				p.color = r.color;
				r.color = color;
				// Tree above s needs to be rebalanced
				q = s;
				lastDir = 0;
			}
		}
		if (p.color == RED)
			return ret;
		for (;;)
		{
			p = (Node) q.child[lastDir];
			if (p != null && p.color == RED)
			{
				p.color = BLACK;
				break;
			}
			if (q == root.child[0])
				break;
			int sibDir = 1 - lastDir;
			Node grandparent = (Node) q.parent;
			int gLastDir = 0;
			if (grandparent.child[1] == grandparent)
				gLastDir = 1;
			Node sibling = (Node) q.child[sibDir];
			if (sibling.color == RED)
			{
				sibling.color = BLACK;
				q.color = RED;

				/* Example with lastDir == 0
				     qB          qR             sB => gB
				     / \   ----> / \    ---->   / \
				    pB sR       pB sB          qR  bB
				       / \         / \        / \
				      aB bB       aB bB      pB aB => sB
				*/
				if (lastDir == 0)
					grandparent.child[gLastDir] = q.rotateL();
				else
					grandparent.child[gLastDir] = q.rotateR();
				grandparent = sibling;
				sibling = (Node) q.child[sibDir];
			}
			// Now sibling is black; if its 2 children are also
			// black, paint it into red and check again from
			// grandparent.
			if ((sibling.child[0] == null || ((Node) sibling.child[0]).color == BLACK) &&
			    (sibling.child[1] == null || ((Node) sibling.child[1]).color == BLACK))
				sibling.color = RED;
			else
			{
				// A sibling's children is red.
				if (sibling.child[sibDir] == null || ((Node) sibling.child[sibDir]).color == BLACK)
				{
					// y is red: rotate at sibling and paint nodes
					// so that sibling.child[sibDir] is red.
					Node y = (Node) sibling.child[lastDir];
					y.color = BLACK;
					sibling.color = RED;
					if (lastDir == 0)
						q.child[sibDir] = y.rotateR();
					else
						q.child[sibDir] = y.rotateL();
					sibling = y;
					/* Example with lastDir == 0
					    gB       gB       gB
					    / \ ---> / \ ---> / \
					   qR  bB   qR  bB   qB  bB
					  / \      / \      / \
					 xB sB    xB yR    xB yB
					   / \      / \      / \
					  yR  zB   T1  sB   T1  sR
					 / \          / \      / \
					T1 T2        T2  zB   T2  zB
					*/
				}
				/* We know that sibling is black, its lastDir
				   child is black and its sibDir child is red.
				  q*            qB              s*      
				 / \    --->   / \    --->    /   \      
				xB sB         xB s*         qB     yB     
				  / \           / \        / \    / \     
				 T1  yR        T1  yB    xB  T1 T2  zB    
				    / \           / \
				   T2  zB        T2  zB
				*/
				sibling.color = q.color;
				q.color = BLACK;
				((Node) sibling.child[sibDir]).color = BLACK;
				if (lastDir == 0)
					grandparent.child[gLastDir] = q.rotateR();
				else
					grandparent.child[gLastDir] = q.rotateL();
				break;
			}
			q = (Node) q.parent;
			if (q.parent.child[0] == q)
				lastDir = 0;
			else
				lastDir = 1;
		}
		return ret;
	}
	
	public static void main(String args[])
	{
		PRedBlackSortedTree tree;
		//  Test single right rotation
		tree = new PRedBlackSortedTree();
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
		tree = new PRedBlackSortedTree();
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
