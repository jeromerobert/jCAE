/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005, by EADS CRC

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
public class PAVLSortedTree2
{
	private static Logger logger = Logger.getLogger(PAVLSortedTree2.class);	
	private PAVLNode root;
	// Current node for tree traversal.  There is no reason to have
	// concurrent traversals.
	private PAVLNode travNode;
	private int count = 0;
	
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
		root.child[0].show();
	}
	
	/**
	 * Pretty-print this tree.
	 */
	public void showValues()
	{
		if (root == null)
		{
			System.out.println("Empty tree");
			return;
		}
		System.out.println("Sorted tree:");
		for (PAVLNode current = first(); current != null; current = next())
			System.out.println("  "+current.getKey()+" "+current.getValue());
	}
	
	public void showKeys()
	{
		if (root == null)
		{
			System.out.println("Empty tree");
			return;
		}
		System.out.println("Sorted keys:");
		for (PAVLNode current = last(); current != null; current = prev())
			System.out.println("  "+current.getKey());
	}
	
	/**
	 * Insert a node into the tree.
	 *
	 * @param node      node to insert.
	 */
	public final synchronized void insert(PAVLNode node)
	{
		if (logger.isDebugEnabled())
			logger.debug("insert "+node.getValue()+" "+" value: "+node.getKey());
		count++;
		if (root == null)
		{
			root = new PAVLNode(null, 0.0);
			// A fake node is inserted below root so that root
			// never has to be updated.  Tree begins at
			// root.child[0].
			root.child[0] = node;
			node.parent = root;
			return;
		}
		root.insert(node);
	}
	
	/**
	 * Update the quality factor of an object.
	 * @param node   node being updated.
	 * @param value  new quality factor.
	 * @return the old value.
	 */
	public final synchronized double update(PAVLNode node, double value)
	{
		if (logger.isDebugEnabled())
			logger.debug("Update "+node.getValue()+" to "+value);
		double ret = remove(node);
		node.setKey(value);
		insert(node);
		return ret;
	}
	
	/**
	 * Remove the node associated to an object from the tree.
	 * @param node      node being removed
	 * @return  the quality factor associated to this object.
	 */
	public final synchronized double remove(PAVLNode node)
	{
		if (logger.isDebugEnabled())
			logger.debug("Remove "+node);
		double ret = node.getKey();
		if (logger.isDebugEnabled())
			logger.debug("Value: "+ret);
		count--;
		root.remove(node);
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
		return count;
	}
	
	/**
	 * Return the object with the lowest quality factor.
	 * @return the object with the lowest quality factor.
	 */
	public final synchronized PAVLNode first()
	{
		if (root == null || root.child[0] == null)
			return null;
		travNode = root.child[0];
		while (travNode.child[0] != null)
			travNode = travNode.child[0];
		return travNode;
	}
	
	/**
	 * Return the object with the highest quality factor.
	 * @return the object with the highest quality factor.
	 */
	public final synchronized PAVLNode last()
	{
		if (root == null || root.child[0] == null)
			return null;
		travNode = root.child[0];
		while (travNode.child[1] != null)
			travNode = travNode.child[1];
		return travNode;
	}
	
	/**
	 * When traversing tree, return object with immediate higher quality
	 * factor.
	 * @return the object with the immediate higher quality factor.
	 */
	public final synchronized PAVLNode next()
	{
		if (travNode == null)
			return null;
		PAVLNode right = travNode.child[1];
		if (right != null)
		{
			travNode = right;
			while (travNode.child[0] != null)
				travNode = travNode.child[0];
			return travNode;
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
		return travNode;
	}
	
	/**
	 * When traversing tree, return object with immediate lower quality
	 * factor.
	 * @return the object with the immediate lower quality factor.
	 */
	public final synchronized PAVLNode prev()
	{
		if (travNode == null)
			return null;
		PAVLNode left = travNode.child[0];
		if (left != null)
		{
			travNode = left;
			while (travNode.child[1] != null)
				travNode = travNode.child[1];
			return travNode;
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
		return travNode;
	}
	
	public static void main(String args[])
	{
		PAVLSortedTree2 tree;
		//  Test single right rotation
		tree = new PAVLSortedTree2();
		PAVLNode [] nodes = new PAVLNode[100];
		Integer [] iii = new Integer[100];
		for (int i = 0; i < iii.length; i++)
			iii[i] = new Integer(i);
			
		for (int i = 0; i < 10; i++)
		{
			nodes[i] = new PAVLNode(iii[i], (double) i);
			tree.insert(nodes[i]);
		}
		tree.show();
		tree.remove(nodes[9]);
		tree.remove(nodes[1]);
		tree.remove(nodes[0]);
		tree.show();
		tree = new PAVLSortedTree2();
		for (int i = 9; i >= 0; i--)
		{
			nodes[i] = new PAVLNode(iii[i], (double) i);
			tree.insert(nodes[i]);
		}
		tree.show();
		tree.remove(nodes[0]);
		tree.remove(nodes[8]);
		tree.remove(nodes[9]);
		tree.show();
		tree.showValues();
	}
	
}
