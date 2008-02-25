/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2006, by EADS CRC
    Copyright (C) 2007,2008, by EADS France

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

import gnu.trove.THashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Binary trees to store quality factors.
 * These trees are used to sort vertices, edges, or triangles according
 * to their quality factors, and to process them in increasing or decreasing
 * order after they have been sorted.  They differ from casual binary trees in
 * that duplicate quality factors are allowed.  See examples in algorithms from
 * {@link org.jcae.mesh.amibe.algos3d}.
 */
public abstract class QSortedTree<E> implements Serializable
{
	private static Logger logger=Logger.getLogger(QSortedTree.class.getName());	
	protected final Node<E> root = newNode(null, Double.MAX_VALUE);
	// Mapping between objects and tree nodes
	private transient Map<E, Node<E>> map = new THashMap<E, Node<E>>();
	private int nrNodes = 0;
	
	/**
	 * Constructor to cast new nodes into subclass type.
	 */
	protected abstract Node<E> newNode(E o, double v);

	/**
	 * Insert a new note into the binary tree.  This method always returns
	 * <code>true</code>.
	 */
	protected abstract boolean insertNode(Node<E> node);

	/**
	 * Remove a note from the binary tree.  Some algorithms may remove
	 * another node (for instance PRedBlackSortedTree), this method
	 * returns the node which has been removed.
	 */
	protected abstract Node<E> removeNode(Node<E> p);

	@SuppressWarnings("serial")
	public static class Node<E> implements Comparable<Node<E>>, Serializable
	{
		protected E data;
		private double value;
		protected final Node<E> [] child = newChilds();
		protected Node<E> parent = null;
		
		@SuppressWarnings("unchecked")
		protected Node<E> [] newChilds()
		{
			return new Node[2];
		}
	
		public Node(final E o, final double v)
		{
			data = o;
			value = v;
		}
		
		public int compareTo(final Node<E> that)
		{
			if (value < that.value)
				return -1;
			return +1;
		}
		
		public void reset(final double v)
		{
			child[0] = child[1] = parent = null;
			value = v;
		}
		
		public void swap(final Node<E> that)
		{
			final E temp = that.data;
			that.data = data;
			data = temp;
			// For now there is no reason to swap values
			that.value = value;
		}
	
		public double getValue()
		{
			return value;
		}
	
		public E getData()
		{
			return data;
		}
	
		@Override
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
		public Node<E> rotateL()
		{
			//if (logger.isLoggable(Level.FINE))
			//	logger.fine("Single left rotation around "+this);
			final Node<E> right = child[1];
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
		public Node<E> rotateR()
		{
			//if (logger.isLoggable(Level.FINE))
			//	logger.fine("Single right rotation around "+this);
			final Node<E> left = child[0];
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
		public Node<E> rotateRL()
		{
			//if (logger.isLoggable(Level.FINE))
			//	logger.fine("Right+left rotation around "+this);
			final Node<E> right = child[1];          // C
			final Node<E> newRoot = right.child[0];  // B
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
		public Node<E> rotateLR()
		{
			//if (logger.isLoggable(Level.FINE))
			//	logger.fine("Left+right rotation around "+this);
			final Node<E> left = child[0];         // A
			final Node<E> newRoot = left.child[1]; // B
	
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
		Node<E> firstNode()
		{
			Node<E> current = this;
			while (current.child[0] != null)
				current = current.child[0];
			return current;
		}
	
		Node<E> lastNode()
		{
			Node<E> current = this;
			while (current.child[1] != null)
				current = current.child[1];
			return current;
		}
	
		public Node<E> previousNode()
		{
			Node<E> current = this;
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
	
		public Node<E> nextNode()
		{
			Node<E> current = this;
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
			return current.parent;
		}
	}

	protected void readObject(java.io.ObjectInputStream s)
		throws java.io.IOException, ClassNotFoundException
	{
		s.defaultReadObject();
		map = new THashMap<E, Node<E>>(nrNodes);
		if (nrNodes == 0)
			return;
		for (Iterator<Node<E>> it = iterator(); it.hasNext(); )
		{
			Node<E> current = it.next();
			map.put(current.getData(), current);
		}
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
	public final void insert(E o, double value)
	{
		assert map.get(o) == null : "Object already in tree: "+o;
		Node<E> node = newNode(o, value);
		if (logger.isLoggable(Level.FINE))
			logger.fine("Insert "+node+" "+" value: "+value+" "+o);
		map.put(o, node);
		nrNodes++;
		insertNode(node);
	}

	/**
	 * Remove the node associated to an object from the tree.
	 * @param o      object being removed
	 * @return  <code>true</code> if node was present in tree,
	 * </code>false</code> otherwise.
	 */
	public final boolean remove(E o)
	{
		Node<E> p = map.get(o);
		if (logger.isLoggable(Level.FINE))
			logger.fine("Remove "+p+" "+o);
		if (p == null)
			return false;
		nrNodes--;
		map.remove(o);
		Node<E> r = removeNode(p);
		// PRedBlackSortedTree implementation may swap p
		// and r nodes and remove r, we then need to
		// update map.
		if (r != p)
		{
			map.remove(r.getData());
			map.put(p.getData(), p);
		}
		return true;
	}

	/**
	 * Update the quality factor of an object, if it was already
	 * present in tree.
	 *
	 * @param o      object being updated
	 * @param value  new quality factor
	 * @return <code>true</code> if object was present in tree,
	 *         <code>false</code> otherwise.
	 */
	public final boolean update(E o, double value)
	{
		Node<E> p = map.get(o);
		if (logger.isLoggable(Level.FINE))
			logger.fine("Update "+p+" content to "+value);
		if (p == null)
			return false;
		Node<E> r = removeNode(p);
		// PRedBlackSortedTree implementation may swap p
		// and r nodes and remove r, we then need to
		// update map.
		if (r != p)
		{
			map.remove(r.getData());
			map.put(p.getData(), p);
		}
		r.reset(value);
		insertNode(r);
		return true;
	}
	
	/**
	 * Clear this tree.
	 */
	public void clear()
	{
		// Unlink all nodes to help garbage collector
		for (Node<E> p: map.values())
		{
			p.data = null;
			p.child[0] = p.child[1] = null;
			p.parent = null;
		}
		map.clear();
		root.child[0] = root.child[1] = null;
		nrNodes = 0;
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
	
	private static <E> void showNode(Node<E> node)
	{
		System.out.print(node.toString());
		if (node.child[0] != null)
			System.out.print(" Left -> "+node.child[0].getValue());
		if (node.child[1] != null)
			System.out.print(" Right -> "+node.child[1].getValue());
		if (node.parent != null)
			System.out.print(" Parent -> "+node.parent.getValue());
		System.out.println("");
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
	
	private static <E> void showNodeValues(Node<E> node)
	{
		if (node.child[0] != null)
		{
			assert node.child[0].parent == node;
			showNodeValues(node.child[0]);
		}
		System.out.println("Key: "+node.getValue()+ "Obj: "+node.getData());
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
	public final boolean contains(E o)
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
	
	/**
	 * Return the value found at root binary tree.  As trees are balanced, this is a
	 * good approximation of tree median value.
	 * @return the value found at root binary tree.
	 */
	public final double getRootValue()
	{
		return root.child[0].getValue();
	}
	
	private Iterator<Node<E>> nullIterator = new Iterator<Node<E>>()
	{
		public boolean hasNext() { return false; }
		public Node<E> next() { throw new NoSuchElementException(); }
		public void remove() { throw new RuntimeException(); }
	};

	public Iterator<Node<E>> iterator()
	{
		if (nrNodes == 0)
			return nullIterator;
		return new Iterator<Node<E>>()
		{
			private Node<E> current = root;
			private Node<E> next = root.child[0].firstNode();
			public boolean hasNext()
			{
				return next != null;
			}
			public Node<E> next()
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
	
	public Iterator<Node<E>> backwardIterator()
	{
		if (nrNodes == 0)
			return nullIterator;
		return new Iterator<Node<E>>()
		{
			private Node<E> current = root;
			private Node<E> next = root.child[0].lastNode();
			public boolean hasNext()
			{
				return next != null;
			}
			public Node<E> next()
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
	
}
