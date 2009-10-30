/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005,2006, by EADS CRC
    Copyright (C) 2007, by EADS France

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

package org.jcae.mesh.amibe.ds;

import org.jcae.mesh.amibe.traits.TriangleTraitsBuilder;
import java.util.Iterator;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.io.Serializable;

/**
 * A triangle containing adjacency relations.
 */
public class Triangle implements Serializable
{
	private static final long serialVersionUID = 3698940897637489316L;
	//  User-defined traits
	//protected final Traits traits;

	/**
	 * Three vertices.
	 */
	public Vertex [] vertex;
	
	// Group id
	private int groupId = -1;
	
	private boolean readable = true;
	private boolean writable = true;

	// We sometimes need to process lists of triangles before mesh
	// connectivity has been set up.  This can be achieved efficiently
	// with a singly linked list.
	// Reference to the next element in the singly linked list.
	private Triangle listNext;
	
	public Triangle(TriangleTraitsBuilder builder)
	{
		/*if (builder != null)
			traits = builder.createTraits();
		else
			traits = null;*/
		vertex = new Vertex[3];
	}

	public void copy(Triangle src)
	{
		System.arraycopy(src.vertex, 0, vertex, 0, 3);
		readable = src.readable;
		writable = src.writable;
	}
	
	/**
	 * Sets attributes for all edges of this triangle.
	 *
	 * @param attr  attributes to set on edges
	 */
	public void setAttributes(int attr)
	{
		throw new RuntimeException();
	}
	
	/**
	 * Resets attributes for all edges of this triangle.
	 *
	 * @param attr  attributes to reset on edges
	 */
	public void clearAttributes(int attr)
	{
		throw new RuntimeException();
	}
	
	/**
	 * Checks if some attributes of this triangle are set.
	 *
	 * @param attr  attributes to check
	 * @return <code>true</code> if any edge of this triangle has
	 * one of these attributes set, <code>false</code> otherwise
	 */
	public boolean hasAttributes(int attr)
	{
		throw new RuntimeException();
	}
	
	/**
	 * Gets an <code>AbstractHalfEdge</code> instance bound to this triangle.
	 */
	public AbstractHalfEdge getAbstractHalfEdge()
	{
		throw new RuntimeException();
	}

	/**
	 * Gets an <code>AbstractHalfEdge</code> instance bound to this triangle.
	 */
	public AbstractHalfEdge getAbstractHalfEdge(AbstractHalfEdge that)
	{
		throw new RuntimeException();
	}

	/**
	 * Return the group identifier of this triangle.
	 *
	 * @return the group identifier of this triangle.
	 */
	public final int getGroupId()
	{
		return groupId;
	}
	
	/**
	 * Set the group identifier of this triangle.
	 *
	 * @param g  the group identifier of this triangle.
	 */
	public final void setGroupId(int g)
	{
		groupId = g;
	}
	
	public final void setReadable(boolean b)
	{
		readable = b;
	}
	
	public final void setWritable(boolean b)
	{
		writable = b;
	}
	
	public boolean isReadable()
	{
		return readable;
	}
	
	public final boolean isWritable()
	{
		return writable;
	}
	
	@Override
	public String toString()
	{
		StringBuilder r = new StringBuilder();
		r.append("hashcode: ").append(hashCode());
		if (!readable)
			r.append(" !r");
		if (!writable)
			r.append(" !w");
		if (groupId >= 0)
			r.append("\nGroup: ").append(groupId);
		r.append("\nVertices:");
		for (int i = 0; i < 3; i++)
			r.append("\n  ").append(vertex[i]);
		if (listNext != null)
			r.append("\nLink next: ").append(listNext.hashCode());
		return r.toString();
	}

	/**
	 * Singly linked list of triangles.
	 * We sometimes need to process lists of triangles before mesh
	 * connectivity has been set up.  This can be achieved efficiently
	 * with a singly linked list, but there are few caveats.
	 * <ul>
	 *  <li>A Triangle can appear in one list only, trying to insert it
	 *      twice will throw a ConcurrentModificationException exception.</li>
	 *  <li>Lists have to be cleared out by calling the {@link #clear} method
	 *      before being freed, otherwise Triangle can not be inserted into
	 *      other lists.</li>
	 * </ul>
	 * <p>
	 * Here is an example:
	 * </p>
	 *   <pre>
	 *   //  Begin a new list
	 *   Triangle.List tList = new Triangle.List();
	 *   ...
	 *   //  In a loop, add triangles to this list.
	 *     tList.{@link List#add}(tri);
	 *   //  Check whether a triangle is contained in this list.
	 *   //  This is very fast because it tests if its link pointer
	 *   //  is <code>null</code> or not.
	 *     if (tList.{@link List#contains}(tri)) {
	 *        ...
	 *     }
	 *   //  Loop over collected triangles.
	 *   for (Iterator<Triangle> it = tList.{@link List#iterator}; it.hasNext(); )
	 *   {
	 *     Triangle t = it.next();
	 *     ...
	 *   }
	 *   //  When finished, remove all links between triangles
	 *   tList.{@link List#clear};
	 *   </pre>
	 * <p>
	 * New elements are added at the end of the list so that {@link List#add} can
	 * be called while {@link List#iterator} is in action.
	 * </p>
	 */
	public static class List
	{
		//   Head of the list.  Triangles are linked from this instance.
		private final Triangle listHead = new Triangle(null);
		//   Sentinel.  This triangle is always the last triangle of the list.
		private final Triangle listSentinel = new Triangle(null);
		//   Reference to the last collected triangle.
		private Triangle listTail = listHead;
		//   Number of collected items (for debugging purpose, can be removed).
		private int listSize = 0;

		/**
		 * Initialize a triangle linked list.
		 */
		public List()
		{
			listTail.listNext = listSentinel;
		}
	
		/**
		 * Unmark triangles.  This method must be called before freeing
		 * the list.
		 */
		public final void clear()
		{
			Triangle next;
			for (Triangle start = listHead; start != listSentinel; start = next)
			{
				next = start.listNext;
				start.listNext = null;
				listSize--;
			}
			listSize++;
			assert listSize == 0;
			listTail = listHead;
			listTail.listNext = listSentinel;
		}
	
		/**
		 * Add the current triangle to the end of the list.
		 * @throws ConcurrentModificationException if this element is
		 * already linked.
		 */
		public final void add(Triangle o)
		{
			assert listTail != null;
			assert listTail.listNext == listSentinel : listTail;
			if (o.listNext != null)
				throw new ConcurrentModificationException();
			listTail.listNext = o;
			listTail = o;
			o.listNext = listSentinel;
			listSize++;
		}

		/**
		 * Add the current triangle to the end of the list.  This method
		 * does nothing if this element is already linked.
		 */
		public final void addAllowDuplicates(Triangle o)
		{
			assert listTail != null;
			assert listTail.listNext == listSentinel : listTail;
			if (o.listNext == null)
			{
				listTail.listNext = o;
				listTail = o;
				o.listNext = listSentinel;
				listSize++;
			}
		}

		/**
		 * Check whether this element appears in the list.
		 */
		public final boolean contains(Triangle o)
		{
			return o.listNext != null;
		}

		/**
		 * Get list size.
		 */
		public final int size()
		{
			return listSize;
		}

		/**
		 * Check whether this list is empty.
		 */
		public final boolean isEmpty()
		{
			return listTail == listHead && listTail.listNext == listSentinel;
		}

		/**
		 * Create an iterator over linked triangles.  Note that the list
		 * can be extended while iterating over elements.
		 */
		public final Iterator<Triangle> iterator()
		{
			return new Iterator<Triangle>()
			{
				private Triangle curr = listHead;
				private Triangle prev = listHead;
				public boolean hasNext()
				{
					return curr.listNext != listSentinel;
				}
				
				public Triangle next()
				{
					if (!hasNext())
						throw new NoSuchElementException();
					prev = curr;
					curr = curr.listNext;
					return curr;
				}
				public void remove()
				{
					if (listTail == curr)
						listTail = prev;
					prev.listNext = curr.listNext;
					curr.listNext = null;
					curr = prev;
					listSize--;
				}
			};
		}
	}
	
}
