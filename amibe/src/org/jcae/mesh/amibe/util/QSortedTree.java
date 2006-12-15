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
	protected final Node root = newNode(null, Double.MAX_VALUE);
	// Mapping between objects and tree nodes
	protected transient HashMap map = new HashMap();
	private int nrNodes = 0;
	
	/**
	 * Constructor to cast new nodes into subclass type.
	 */
	protected abstract Node newNode(Object o, double v);

	/**
	 * Insert a new note into the binary tree.
	 */
	protected abstract void insertNode(Node node);

	/**
	 * Remove a note from the binary tree.
	 */
	protected abstract Node removeNode(Node p);

	public static class Node implements Comparable, Serializable
	{
		private Object data;
		private double value;
		protected final Node [] child = newChilds();
		protected Node parent = null;
		
		public Node [] newChilds()
		{
			return new Node[2];
		}
	
		public Node(Object o, double v)
		{
			data = o;
			value = v;
		}
		
		public int compareTo(Object o)
		{
			Node that = (Node) o;
			if (value < that.value)
				return -1;
			else
				return +1;
		}
		
		public void reset(double v)
		{
			child[0] = child[1] = parent = null;
			value = v;
		}
		
		public void swap(Node that)
		{
			Object temp = that.data;
			that.data = data;
			data = temp;
			// For now there is no reason to swap values
			that.value = value;
		}
	
		public double getValue()
		{
			return value;
		}
	
		public Object getData()
		{
			return data;
		}
	
		public String toString()
		{
			return "Key: "+value+" obj. "+Integer.toHexString(data.hashCode());
		}
	
		/* Left rotation
		    A                    B
		   / \      ------>     / \
		  T1  B                A   T3
		     / \              / \
		    T2 T3            T1 T2
		*/
		public Node rotateL()
		{
			//logger.debug("Single left rotation");
			Node right = child[1];
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
		public Node rotateR()
		{
			//logger.debug("Single right rotation");
			Node left = child[0];
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
		public Node rotateRL()
		{
			//logger.debug("Right+left rotation");
			Node right = child[1];          // C
			Node newRoot = right.child[0];  // B
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
		public Node rotateLR()
		{
			//logger.debug("Left+right rotation");
			Node left = child[0];         // A
			Node newRoot = left.child[1]; // B
	
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
		public Node firstNode()
		{
			Node current = this;
			while (current.child[0] != null)
				current = current.child[0];
			return current;
		}
	
		public Node lastNode()
		{
			Node current = this;
			while (current.child[1] != null)
				current = current.child[1];
			return current;
		}
	
		public Node previousNode()
		{
			Node current = this;
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
	
		public Node nextNode()
		{
			Node current = this;
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

	protected void readObject(java.io.ObjectInputStream s)
		throws java.io.IOException, ClassNotFoundException
	{
		s.defaultReadObject();
		map = new HashMap(nrNodes);
		if (nrNodes == 0)
			return;
		for (Node current = root.child[0].firstNode(); current != null; current = current.nextNode())
			map.put(current.getData(), current);
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
		Node node = newNode(o, value);
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
		Node p = (Node) map.get(o);
		if (logger.isDebugEnabled())
			logger.debug("Remove "+p+" "+o);
		if (p == null)
			return -1.0;
		nrNodes--;
		map.remove(o);
		Node r = removeNode(p);
		if (r != p)
		{
			map.remove(r.getData());
			map.put(p.getData(), p);
		}
		return r.getValue();
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
	
	private static void showNode(Node node)
	{
		String r = node.toString();
		if (node.child[0] != null)
			r += " Left -> "+node.child[0].getValue();
		if (node.child[1] != null)
			r += " Right -> "+node.child[1].getValue();
		if (node.parent != null)
			r += " Parent -> "+node.parent.getValue();
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
	
	private static void showNodeValues(Node node)
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
			private Node current = root;
			private Node next = root.child[0].firstNode();
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
				return current;
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
			private Node current = root;
			private Node next = root.child[0].lastNode();
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
				return current;
			}
			public void remove()
			{
				// Not supported yet!
				throw new RuntimeException();
			}
		};
	}
	
	protected Integer [] unitTestInit(int n)
	{
		assert isEmpty();
		Integer [] ret = new Integer[n];
		for (int i = 0; i < ret.length; i++)
			ret[i] = new Integer(i);
		for (int i = 0; i < ret.length; i++)
			insert(ret[i], (double) i);
		return ret;
	}
	
	protected void unitTest1(int n)
	{
		// Remove in ascending order
		Integer [] iii = unitTestInit(n);
		for (int i = 0; i < iii.length; i++)
			remove(iii[i]);
		if (!isEmpty())
			throw new RuntimeException();
	}
	
	protected void unitTest2(int n)
	{
		// Remove in descending order
		Integer [] iii = unitTestInit(n);
		for (int i = iii.length - 1; i >= 0; i--)
			remove(iii[i]);
		if (!isEmpty())
			throw new RuntimeException();
	}
	
	protected void unitTest3(int n)
	{
		Integer [] iii = unitTestInit(n);
		for (int i = 0; i < iii.length / 2; i++)
		{
			remove(iii[i]);
			remove(iii[i+iii.length / 2]);
		}
		if (!isEmpty())
			throw new RuntimeException();
	}
	
	protected void unitTest4(int n)
	{
		Integer [] iii = unitTestInit(n);
		for (int i = 0; i < iii.length / 2; i++)
		{
			remove(iii[iii.length / 2+i]);
			remove(iii[iii.length / 2-1-i]);
		}
		if (!isEmpty())
			throw new RuntimeException();
	}
	
	protected void unitTest5(int n, int s)
	{
		int prime = gnu.trove.PrimeFinder.nextPrime(n);
		Integer [] iii = unitTestInit(prime);
		int index = 1;
		for (int i = 0; i < prime; i++)
		{
			index += s;
			while (index >= prime)
				index -= prime;
			remove(iii[index]);
		}
		if (!isEmpty())
			throw new RuntimeException();
	}
	
	protected void unitTest6(int n, int s)
	{
		int prime = gnu.trove.PrimeFinder.nextPrime(n);
		Integer [] iii = new Integer[prime];
		for (int i = 0; i < iii.length; i++)
			iii[i] = new Integer(i);
		int index = 1;
		for (int i = 0; i < prime; i++)
		{
			index += s;
			while (index >= prime)
				index -= prime;
			insert(iii[index], (double) index);
		}
		index = 3;
		for (int i = 0; i < prime; i++)
		{
			index += s;
			while (index >= prime)
				index -= prime;
			remove(iii[index]);
		}
		if (!isEmpty())
			throw new RuntimeException();
	}
}
