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
import java.util.NoSuchElementException;
import java.io.Serializable;
import org.apache.log4j.Logger;

/**
 * Binary trees to store quality factors.
 * Main ideas come from Ben Pfaff's <a href="http://adtinfo.org/">GNU libavl</a>.
 * These trees are used to sort vertices, edges, or triangles according
 * to their quality factors, and to process them in increasing or decreasing
 * order after they have been sorted.  They differ from casual binary trees in
 * that duplicate quality factors are allowed.
 * See examples in algorithms from
 * {@link org.jcae.mesh.amibe.algos3d}.
 */
public abstract class QSortedTree implements Serializable
{
	private static Logger logger = Logger.getLogger(QSortedTree.class);	
	protected final QSortedTreeNode root = newNode(null, Double.MAX_VALUE);
	// Mapping between objects and tree nodes
	protected transient HashMap map = new HashMap();
	private int nrNodes = 0;
	
	/**
	 * Constructor to cast new nodes into subclass type.
	 */
	protected abstract QSortedTreeNode newNode(Object o, double v);

	/**
	 * Insert a new note into the binary tree.
	 */
	protected abstract void insertNode(QSortedTreeNode node);

	/**
	 * Remove a note from the binary tree.
	 */
	protected abstract QSortedTreeNode removeNode(QSortedTreeNode p);

	protected void readObject(java.io.ObjectInputStream s)
		throws java.io.IOException, ClassNotFoundException
	{
		s.defaultReadObject();
		map = new HashMap(nrNodes);
		if (nrNodes == 0)
			return;
		for (QSortedTreeNode current = root.child[0].firstNode(); current != null; current = current.nextNode())
			map.put(current.data, current);
	}

	/**
	 * Tell whether this tree is empty.
	 */
	public boolean isEmpty()
	{
		return root.child[0] == null;
	}
	
	/**
	 * Insert a node to the tree.  Tree is sorted according to
	 * <code>value</code>, and duplicates are not checked.
	 * @param o      object
	 * @param value  quality factor
	 */
	public final void insert(Object o, double value)
	{
		assert map.get(o) == null;
		QSortedTreeNode node = newNode(o, value);
		if (logger.isDebugEnabled())
			logger.debug("Insert "+node+" "+" value: "+value+" "+o);
		map.put(o, node);
		nrNodes++;
		insertNode(node);
	}

	/**
	 * Remove the node associated to an object from the tree.
	 * @param o      object being removed
	 * @return  the quality factor associated to this object.
	 */
	public final double remove(Object o)
	{
		QSortedTreeNode p = (QSortedTreeNode) map.get(o);
		if (logger.isDebugEnabled())
			logger.debug("Remove "+p+" "+o);
		if (p == null)
			return -1.0;
		map.remove(o);
		nrNodes--;
		p = removeNode(p);
		return p.value;
	}

	/**
	 * Update the quality factor of an object.
	 * @param o      object being updated
	 * @param value  new quality factor
	 */
	public final void update(Object o, double value)
	{
		remove(o);
		insert(o, value);
	}
	
	public void copyNode(QSortedTreeNode src, QSortedTreeNode dest)
	{
		map.remove(src.data);
		dest.data = src.data;
		dest.value = src.value;
		map.put(dest.data, dest);
	}

	/**
	 * Pretty-print this tree.
	 */
	public void show()
	{
		if (isEmpty())
		{
			System.out.println("Empty tree");
			return;
		}
		System.out.println("Tree:");
		showNode(root.child[0]);
	}
	
	private static void showNode(QSortedTreeNode node)
	{
		String r = node.toString();
		if (node.child[0] != null)
			r += " Left -> "+node.child[0].value;
		if (node.child[1] != null)
			r += " Right -> "+node.child[1].value;
		if (node.parent != null)
			r += " Parent -> "+node.parent.value;
		System.out.println(r);
		if (node.child[0] != null)
		{
			assert node.child[0].parent == node : "Invalid parent pointer: "+node.child[0].parent+" != "+node;
			showNode(node.child[0]);
		}
		if (node.child[1] != null)
		{
			assert node.child[1].parent == node : "Invalid parent pointer: "+node.child[1].parent+" != "+node;
			showNode(node.child[1]);
		}
	}
	
	/**
	 * Pretty-print this tree.
	 */
	public void showValues()
	{
		if (isEmpty())
		{
			System.out.println("Empty tree");
			return;
		}
		System.out.println("Tree:");
		showNodeValues(root.child[0]);
	}
	
	private static void showNodeValues(QSortedTreeNode node)
	{
		System.out.println(node.toString());
		if (node.child[0] != null)
		{
			assert node.child[0].parent == node;
			showNodeValues(node.child[0]);
		}
		if (node.child[1] != null)
		{
			assert node.child[1].parent == node;
			showNodeValues(node.child[1]);
		}
	}
	
	public void showKeys()
	{
		if (isEmpty())
		{
			System.out.println("Empty tree");
			return;
		}
		System.out.println("Sorted keys:");
		for (Iterator itt = backwardIterator(); itt.hasNext(); )
		{
			QSortedTreeNode node = (QSortedTreeNode) map.get(itt.next());
			assert node != null;
			System.out.println("  "+node.value);
		}
	}
	
	/**
	 * Checks whether an object exist is the tree.
	 * @param o      object being checked
	 * @return <code>true</code> if this tree contains this object,
	 *   <code>false</code> otherwise.
	 */
	public final boolean contains(Object o)
	{
		return map.containsKey(o);
	}
	
	/**
	 * Gets the quallity factor of an object.
	 * @param o      object.
	 * @return  the quality factor associated to this object.
	 */
	public final double getKey(Object o)
	{
		QSortedTreeNode p = (QSortedTreeNode) map.get(o);
		if (p == null)
			return -1.0;
		return p.value;
	}
	
	/**
	 * Return the object with the lowest quality factor.
	 * @return the object with the lowest quality factor.
	 */
	public final int size()
	{
		assert nrNodes == map.size() : "size error: "+nrNodes+" != "+map.size();
		return nrNodes;
	}
	
	private static Iterator nullIterator = new Iterator()
	{
		public boolean hasNext() { return false; }
		public Object next() { throw new NoSuchElementException(); }
		public void remove() { throw new RuntimeException(); }
	};

	public Iterator iterator()
	{
		if (nrNodes == 0)
			return nullIterator;
		return new Iterator()
		{
			private QSortedTreeNode current = root;
			private QSortedTreeNode next = root.child[0].firstNode();
			public boolean hasNext()
			{
				return next != null;
			}
			public Object next()
			{
				current = next;
				if (current == null)
					throw new NoSuchElementException();
				next = next.nextNode();
				return current.data;
			}
			public void remove()
			{
				// Not supported yet!
				throw new RuntimeException();
			}
		};
	}
	
	public Iterator backwardIterator()
	{
		if (nrNodes == 0)
			return nullIterator;
		return new Iterator()
		{
			private QSortedTreeNode current = root;
			private QSortedTreeNode next = root.child[0].lastNode();
			public boolean hasNext()
			{
				return next != null;
			}
			public Object next()
			{
				current = next;
				if (current == null)
					throw new NoSuchElementException();
				next = next.previousNode();
				return current.data;
			}
			public void remove()
			{
				// Not supported yet!
				throw new RuntimeException();
			}
		};
	}
	
}
