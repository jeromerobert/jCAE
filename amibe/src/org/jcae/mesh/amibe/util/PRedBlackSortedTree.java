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
 * A red-black tree has the following properties:
 * <ol>
 * <li>Null nodes are black.</li>
 * <li>A red node has no red child.</li>
 * <li>Paths from any node to external leaves contain the same number of black
 *     nodes.</li>
 * </ol>
 * By convention, root node is always black in order to simplify node insertion
 * and removal.  Node insertions and removals are explained in detail at
 * <a href="http://en.wikipedia.org/wiki/Red-Black_tree">wikipedia</a>.
 */
public class PRedBlackSortedTree extends QSortedTree
{
	private static Logger logger = Logger.getLogger(PRedBlackSortedTree.class);	
	private static class Node extends QSortedTree.Node
	{
		private boolean isRed;
		
		public QSortedTree.Node [] newChilds()
		{
			return new Node[2];
		}

		public Node(Object o, double v)
		{
			super(o, v);
			isRed = true;
		}
		
		public void reset(double v)
		{
			super.reset(v);
			isRed = true;
		}

		public String toString()
		{
			return super.toString()+" "+(isRed ? "red" : "black");
		}
	}

	public final QSortedTree.Node newNode(Object o, double v)
	{
		return new Node(o, v);
	}

	// Helper function
	private static final boolean isRedNode(QSortedTree.Node x)
	{
		return (x != null) && ((Node) x).isRed;
	}
	
	public final boolean insertNode(QSortedTree.Node o)
	{
		Node p = (Node) o;
		Node current = (Node) root.child[0];
		Node q = (Node) root;
		int lastDir = 0;
		while (current != null)
		{
			if (p.compareTo(current) < 0)
				lastDir = 0;
			else
				lastDir = 1;
			q = current;
			current = (Node) current.child[lastDir];
		}
		// Insert node
		q.child[lastDir] = p;
		p.parent = q;
		// Node color is red, so property 3 is preserved.
		// We must check if property 2 is violated, in which
		// case our tree has to be rebalanced and/or repainted.
		// Case I1: root node
		if (q == root)
		{
			// We enforce root node to be black, this eases
			// other cases below.
			logger.debug("Case I1");
			p.isRed = false;
			assert !((Node) root.child[0]).isRed;
			return true;
		}
		for (; p != root.child[0]; )
		{
			q = (Node) p.parent;
			// If parent is black, property 2 is preserved,
			// everything is fine.
			// Case I2: parent is black
			if (!q.isRed)
			{
				logger.debug("Case I2");
				assert !((Node) root.child[0]).isRed;
				return true;
			}
			// Parent is red, so it cannot be the root tree,
			// and grandparent is black.
			Node grandparent = (Node) q.parent;
			assert grandparent != root;
			if (grandparent.child[0] == q)
				lastDir = 0;
			else
				lastDir = 1;
			int sibDir = 1 - lastDir;
			Node uncle = (Node) grandparent.child[sibDir];
			if (isRedNode(uncle))
			{
				// Case I3: uncle is red
				/* Paint nodes and continue from grandparent
				     gB                gR
				    / \   ------>     / \
				   qR uR            qB  uB
				    \                 \
				    pR                pR
				*/
				logger.debug("Case I3");
				q.isRed = false;
				uncle.isRed = false;
				grandparent.isRed = true;
				p = grandparent;
			}
			else
			{
				assert !isRedNode(uncle);
				if (q.child[lastDir] != p)
				{
					/* Rotate to put red nodes on the
					   same side
					     gB            gB
					    / \   ---->   / \
					   qR uB        pR  uB
					    \           /
					    pR         qR
					*/
					logger.debug("Case I4");
					if (lastDir == 0)
						grandparent.child[0] = q.rotateL();
					else
						grandparent.child[1] = q.rotateR();
					p = q;
					q = (Node) p.parent;
				}
				/* Rotate on opposite way and recolor.  Either
				   uncle is null, or we come from case 3 and
				   current node has 2 black children.
				        gB                qB
				       / \   ------>     /  \
				      qR uB            pR   gR
				     / \              / \   / \
				    pR zB            xB yB zB uB
				   / \
				  xB yB
				*/
				assert (uncle == null && q.child[sibDir] == null &&
				  p.child[0] == null && p.child[1] == null) ||
				 (uncle != null && q.child[sibDir] != null &&
				  p.child[0] != null && p.child[1] != null);
				logger.debug("Case I5");

				Node greatgrandparent = (Node) grandparent.parent;
				grandparent.isRed = true;
				q.isRed = false;
				if (greatgrandparent.child[0] == grandparent)
					lastDir = 0;
				else
					lastDir = 1;
				// lastDir has been modified, so use sibDir here
				if (sibDir == 1)
					greatgrandparent.child[lastDir] = grandparent.rotateR();
				else
					greatgrandparent.child[lastDir] = grandparent.rotateL();
				assert !((Node) root.child[0]).isRed;
				return true;
			}
		}
		((Node) root.child[0]).isRed = false;
		assert isValid();
		return true;
	}
	
	public final QSortedTree.Node removeNode(QSortedTree.Node o)
	{
		Node p = (Node) o;
		Node ret = p;
		Node q = (Node) p.parent;
		int lastDir = 0;
		if (q.child[1] == p)
			lastDir = 1;
		if (p.child[1] == null)
		{
			q.child[lastDir] = p.child[0];
			if (q.child[lastDir] != null)
				q.child[lastDir].parent = q;
		}
		else
		{
			// p has a right child.  Replace p by its
			// successor, and update q and lastDir.
			Node r = (Node) p.nextNode();
			// Do not modify p's color!
			p.swap(r);
			p = r;
			q = (Node) p.parent;
			if (q.child[0] == p)
				lastDir = 0;
			else
				lastDir = 1;
			assert p.child[0] == null;
			q.child[lastDir] = p.child[1];
			if (q.child[lastDir] != null)
				q.child[lastDir].parent = q;
			ret = r;
		}
		// p is the node to be removed, q its parent and
		// lastDir so that q.child[lastDir] == p;
		if (p.isRed)
			return ret;
		// p is a black node, so q is no more balanced, its
		// lastDir child of q has 1 black node less than its
		// sibling.
		for (;;)
		{
			p = (Node) q.child[lastDir];
			if (isRedNode(p))
			{
				p.isRed = false;
				logger.debug("Red node :-)");
				break;
			}
			// Case R1: root tree
			if (q == root)
			{
				// All paths have one less black node
				logger.debug("Case R1");
				break;
			}
			int sibDir = 1 - lastDir;
			Node grandparent = (Node) q.parent;
			int gLastDir = 0;
			if (grandparent.child[1] == q)
				gLastDir = 1;
			Node sibling = (Node) q.child[sibDir];
			// A black node is removed, so its sibling cannot
			// be null.
			assert sibling != null : q.toString();
			if (sibling.isRed)
			{
				// Case R2: sibling is red
				logger.debug("Case R2");
				sibling.isRed = false;
				q.isRed = true;
				assert sibling.child[0] != null;
				assert sibling.child[1] != null;

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
				gLastDir = lastDir;
				sibling = (Node) q.child[sibDir];
			}
			// Now sibling is black
			assert !sibling.isRed;
			if (!q.isRed && !isRedNode(sibling.child[0]) && !isRedNode(sibling.child[1]))
			{
				// Case R3: parent, sibling and sibling's
				// children are black
				logger.debug("Case R3");
				sibling.isRed = true;
			}
			else
			{
				if (!isRedNode(sibling.child[0]) && !isRedNode(sibling.child[1]))
				{
					// Case R4: sibling and sibling's
					// children are black, but parent is
					// red.
					assert q.isRed;
					logger.debug("Case R4");
					sibling.isRed = true;
					q.isRed = false;
					break;
				}
				else
				{
					if (isRedNode(sibling.child[lastDir]) && !isRedNode(sibling.child[sibDir]))
					{
						// Case R5: sibling is black, left child is
						// red and right child is black.
						// Rotate at sibling and paint nodes
						// so that sibling.child[sibDir] is red.
						logger.debug("Case R5");
						Node y = (Node) sibling.child[lastDir];
						y.isRed = false;
						sibling.isRed = true;
						if (lastDir == 0)
							q.child[sibDir] = sibling.rotateR();
						else
							q.child[sibDir] = sibling.rotateL();
						sibling = y;
						/* Example with lastDir == 0
						  q*       q*
						  / \ ---> / \
						 xB sB    xB yB
						   / \      / \
						  yR  zB   aB  sR
						 / \          / \
						aB bB        bB  zB
						*/
					}
					logger.debug("Case R6");
					// Case R6: sibling is black and its right child
					// is red.
					/*
					  q*       qB          s*
					 / \ ---> / \  --->   / \
					xB sB    xB s*      qB   yB
					  / \      / \     / \   / \
					 aB  yR   aB  yB  xB aB bB zB
					    / \      / \
					   bB  zB   bB  zB
					*/
					assert !isRedNode(sibling) && isRedNode(sibling.child[sibDir]);
					sibling.isRed = q.isRed;
					q.isRed = false;
					((Node) sibling.child[sibDir]).isRed = false;
					if (lastDir == 0)
						grandparent.child[gLastDir] = q.rotateL();
					else
						grandparent.child[gLastDir] = q.rotateR();
					break;
				}
			}
			if (q.parent.child[0] == q)
				lastDir = 0;
			else
				lastDir = 1;
			q = (Node) q.parent;
		}
		assert isValid();
		return ret;
	}
	
	private boolean isValid()
	{
		// Call debugIsValid() only when debugging, otherwise
		// tree manipulations are way too slow.
		return true;
		//return debugIsValid();
		//return checkParentPointers();
	}

	public boolean checkParentPointers()
	{
		if (root.child[0] == null)
			return true;
		for (Node current = (Node) root.child[0].firstNode(); current != null; current = (Node) current.nextNode())
		{
			if (current.child[0] != null && current.child[0].parent != current)
				return false;
			if (current.child[1] != null && current.child[1].parent != current)
				return false;
		}
		return true;
	}

	public boolean debugIsValid()
	{
		Node current = (Node) root.child[0];
		if (isRedNode(current))
			return false;
		if (current == null)
			return true;
		int blackNodes = 0;
		int seenRoot = 0;
		while (current.child[0] != null)
		{
			if (!isRedNode(current))
				blackNodes++;
			current = (Node) current.child[0];
		}
		// Now traverse the tree
		while (current != root)
		{
			if (!isRedNode(current))
				blackNodes--;
			else if (isRedNode(current.child[0]) || isRedNode(current.child[1]))
				return false;
			if (current.child[0] != null && current.child[0].compareTo(current) > 0)
				return false;
			if (current.child[1] != null && current.child[1].compareTo(current) < 0)
				return false;
			if (current.child[1] != null)
			{
				current = (Node) current.child[1];
				if (!isRedNode(current))
					blackNodes++;
				else if (isRedNode(current.child[0]) || isRedNode(current.child[1]))
					return false;
				while (current.child[0] != null)
				{
					current = (Node) current.child[0];
					if (!isRedNode(current))
						blackNodes++;
					else if (isRedNode(current.child[0]) || isRedNode(current.child[1]))
						return false;
				}
			}
			else
			{
				// Walk upwards
				while (current.parent.child[0] != current)
				{
					if (!isRedNode(current))
						blackNodes--;
					else if (isRedNode(current.child[0]) || isRedNode(current.child[1]))
						return false;
					current = (Node) current.parent;
				}
				current = (Node) current.parent;
			}
		}
		return true;
	}

	public static void main(String args[])
	{
		QSortedTree tree = new PRedBlackSortedTree();
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
