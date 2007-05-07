/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2006, by EADS CRC

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

import org.jcae.mesh.amibe.traits.Traits;
import org.jcae.mesh.amibe.traits.TriangleTraitsBuilder;
import java.util.Iterator;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

public class AbstractTriangle
{
	/**
	 * Three vertices.
	 */
	public Vertex [] vertex;
	
	// Group id
	private int groupId = -1;
	
	// We sometimes need to process lists of triangles before mesh
	// connectivity has been set up.  This can be achieved efficiently
	// with a singly linked list.
	// Reference to the next element in the singly linked list.
	private AbstractTriangle listNext = null;
	
	//  User-defined traits
	public final TriangleTraitsBuilder traitsBuilder;
	public final Traits traits;

	public AbstractTriangle()
	{
		traitsBuilder = null;
		traits = null;
		vertex = new Vertex[3];
	}
	public AbstractTriangle(TriangleTraitsBuilder builder)
	{
		traitsBuilder = builder;
		if (builder != null)
			traits = builder.createTraits();
		else
			traits = null;
		vertex = new Vertex[3];
	}

	public void copy(AbstractTriangle src)
	{
		for (int i = 0; i < 3; i++)
			vertex[i] = src.vertex[i];
	}
	
	/**
	 * Return the group identifier of this triangle.
	 *
	 * @return the group identifier of this triangle.
	 */
	public int getGroupId()
	{
		return groupId;
	}
	
	/**
	 * Set the group identifier of this triangle.
	 *
	 * @param g  the group identifier of this triangle.
	 */
	public void setGroupId(int g)
	{
		groupId = g;
	}
	
	public String toString()
	{
		StringBuffer r = new StringBuffer();
		r.append("hashcode: "+hashCode());
		r.append("\nVertices:");
		for (int i = 0; i < 3; i++)
			r.append("\n  "+vertex[i]);
		if (listNext != null)
			r.append("\nLink next: "+listNext.hashCode());
		return r.toString();
	}

	/**
	 * Singly linked list of triangles.
	 * We sometimes need to process lists of triangles before mesh
	 * connectivity has been set up.  This can be achieved efficiently
	 * with a singly linked list, but there are few caveats.
	 * <ul>
	 *  <li>A AbstractTriangle can appear in one list only, trying to insert it
	 *      twice will throw a ConcurrentModificationException exception.</li>
	 *  <li>Lists have to be cleared out by calling the {@link #clear} method
	 *      before being freed, otherwise AbstractTriangle can not be inserted into
	 *      other lists.</li>
	 * </ul>
	 * <p>
	 * Here is an example:
	 * </p>
	 *   <pre>
	 *   //  Begin a new list
	 *   AbstractTriangle.List tList = new AbstractTriangle.List();
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
	 *   for (Iterator it = tList.{@link List#iterator}; it.hasNext(); )
	 *   {
	 *     AbstractTriangle t = (AbstractTriangle) it.next();
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
		private final AbstractTriangle listHead = new AbstractTriangle();
		//   Sentinel.  This triangle is always the last triangle of the list.
		private final AbstractTriangle listSentinel = new AbstractTriangle();
		//   Reference to the last collected triangle.
		private AbstractTriangle listTail = listHead;
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
		public void clear()
		{
			AbstractTriangle next;
			for (AbstractTriangle start = listHead; start != listSentinel; start = next)
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
		public final void add(AbstractTriangle o)
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
		 * Check whether this element appears in the list.
		 */
		public boolean contains(AbstractTriangle o)
		{
			return o.listNext != null;
		}
	
		/**
		 * Create an iterator over linked triangles.  Note that the list
		 * can be extended while iterating over elements.
		 */
		public Iterator iterator()
		{
			return new Iterator()
			{
				private AbstractTriangle curr = listHead;
				public boolean hasNext()
				{
					return curr.listNext != listSentinel;
				}
				
				public Object next()
				{
					if (!hasNext())
						throw new NoSuchElementException();
					curr = curr.listNext;
					return curr;
				}
				public void remove()
				{
				}
			};
		}
	}
	
}
