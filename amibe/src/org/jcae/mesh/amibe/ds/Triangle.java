/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005,2006, by EADS CRC

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

import org.jcae.mesh.amibe.patch.Vertex2D;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.ArrayList;
import java.io.Serializable;

/**
 * A triangular element of the mesh.  Instances of this class carry up
 * all topological information required for adjacency relations.  Their
 * vertices are contained in a {@link Vertex} array, and by convention
 * the local number of an edge is the index of its opposite vertex.  A
 * <code>Triangle</code> instance has a pointer to its three neighbours
 * through its edges, and knows the local number of opposite edges in
 * their respective triangles.
 *
 * <pre>
 *                        V2
 *     V5 _________________,_________________ V3
 *        \    &lt;----      / \     &lt;----     /
 *         \     0     _ /   \      1    _ /
 *          \\  t0     ///  /\\\   t1    //
 *           \\1     2///1   0\\\2     0//   t.vertex = { V0, V1, V2 }
 *            \V     //V   t   \\V     //       t.adj = { t1, t0, t2 }
 *             \     /           \     /    opposite edge local number:
 *              \   /      2      \   /         { 2, 2, 1}
 *               \ /     ----&gt;     \ /
 *             V0 +-----------------+ V1
 *                 \     &lt;----     /
 *                  \      1    _ /
 *                   \\   t2    //
 *                    \\2     0//
 * </pre>
 *
 * <p>
 * As local numbers are integers between 0 and 2, a packed representation
 * is wanted to save space.
 * In his <a href="http://www.cs.cmu.edu/~quake/triangle.html">Triangle</a>
 * program, Jonathan Richard Shewchuk uses word alignment of pointers to pack
 * this information into pointers themselves: they are respectively shifted
 * by 0, 1 or 2 bytes for edges 0, 1 and 2.  This very efficient trick
 * can not be performed with Java, and the three numbers are packed into
 * a single byte instead.
 * </p>
 *
 * <p>
 * There are two special cases:
 * </p>
 * <ul>
 *   <li>Boundary edges; a virtual Triangle(Vertex.outer, v1, v2) is created,
 *       and linked to this edge.  This triangle has an {@link OTriangle#OUTER}
 *       flag, and symmetric edges have a {@link OTriangle#BOUNDARY} flag.</li>
 *   <li>Non-manifold edges; a virtual Triangle(Vertex.outer, v1, v2) is
 *       also created, and linked to this edge.  This triangle has an
 *       {@link OTriangle#OUTER} flag, and symmetric edges have a {@link
 *       OTriangle#NONMANIFOLD} flag.  The outer triangle contains in
 *       <code>adj[i]</code> a list of all incident edges.  Thus all incident
 *       edges are linked to a different triangle, but all these triangles
 *       contain a pointer to the same list.
 * </ul>
 */
public class Triangle implements Serializable
{
	/**
	 * Three vertices.
	 */
	public Vertex [] vertex;
	
	/**
	 * Pointers to adjacent elements through edges.
	 */
	private Object [] adj = new Object[3];
	
	/**
	 * Packed representation of adjacent edge local numbers.
	 * adjPos contains the local number of opposite edges in
	 * their respective triangles:
	 * <ul>
	 *     <li>bits 0-1: local number for matte edge 0</li>
	 *     <li>bits 2-3: local number for matte edge 1</li>
	 *     <li>bits 4-5: local number for matte edge 2</li>
	 * </ul>
	*/
	private byte adjPos = 0;
	/**
	 * Edge attributes.
	 */
	private byte [] edgeAttributes = new byte[3];
	// Group id
	private int groupId = -1;
	// Link to an HalfEdge
	private HalfEdge hedge = null;
	
	// We sometimes need to process lists of triangles before mesh
	// connectivity has been set up.  This can be achieved efficiently
	// with a singly linked list.
	// Reference to the next element in the singly linked list.
	private Triangle listNext = null;
	
	public Triangle()
	{
		vertex = new Vertex[3];
	}

	/**
	 * Create a triangle with three vertices.
	 *
	 * @param a  first vertex.
	 * @param b  second vertex.
	 * @param c  third vertex.
	 */
	public Triangle(Vertex a, Vertex b, Vertex c)
	{
		assert a != b && b != c && c != a : a+" "+b+" "+c;
		if (a instanceof Vertex2D)
			vertex = new Vertex2D[3];
		else
			vertex = new Vertex[3];
 		vertex[0] = a;
 		vertex[1] = b;
 		vertex[2] = c;
		if (vertex[0].getLink() == null)
			vertex[0].setLink(this);
		if (vertex[1].getLink() == null)
			vertex[1].setLink(this);
		if (vertex[2].getLink() == null)
			vertex[2].setLink(this);
	}
	
	public Triangle(Vertex [] v)
	{
		if (v[0] instanceof Vertex2D)
			vertex = new Vertex2D[v.length];
		else
			vertex = new Vertex[v.length];
		for (int i = v.length - 1; i >= 0; i--)
			vertex[i] = v[i];
	}
	
	/**
	 * Clone an existing triangle.
	 */
	public Triangle(Triangle that)
	{
		if (that.vertex[0] instanceof Vertex2D)
			vertex = new Vertex2D[3];
		else
			vertex = new Vertex[3];
		copy(that);
	}
	
	public final void copy(Triangle that)
	{
		for (int i = 0; i < 3; i++)
		{
			vertex[i] = that.vertex[i];
			adj[i] = that.adj[i];
		}
		adjPos = that.adjPos;
		edgeAttributes[0] = that.edgeAttributes[0];
		edgeAttributes[1] = that.edgeAttributes[1];
		edgeAttributes[2] = that.edgeAttributes[2];
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
	
	/**
	 * Change the adjacency relation of an edge.
	 * Only one relation is modified.  If both sides have to be modified,
	 * then {@link OTriangle#glue} should be used instead.
	 *
	 * @param num the local number of the edge on this Triangle.
	 * @param that the Triangle attached to this edge.
	 * @param thatnum the local number of the edge on the symmetric
	 *        triangle.
	 */
	public void glue1(int num, Triangle that, int thatnum)
	{
		adj[num] = that;
		//  Clear previous adjacent position ...
		adjPos &= ~(3 << (2*num));
		//  ... and set it right
		adjPos |= (thatnum << (2*num));
	}
	
	/**
	 * Return the adjacent Triangle.
	 * Note: this routine is not very helpful, caller can only check
	 * whether the returned object is null or if its type is Triangle.
	 * This can be performed by checking {@link OTriangle#BOUNDARY}
	 * and {@link OTriangle#NONMANIFOLD} attributes.
	 *
	 * @param num  the local number of this edge.
	 * @return the adjacent Triangle.
	 */
	public Object getAdj(int num)
	{
		return adj[num];
	}
	
	/**
	 * Set the Triangle adjacent to an edge.
	 * Note: this routine could certainly be replaced by {@link #glue1}.
	 *
	 * @param num  the local number of this edge.
	 * @param link  the adjacent Triangle.
	 */
	public void setAdj(int num, Object link)
	{
		adj[num] = link;
	}
	
	/**
	 * Return the local number of symmetric edge in adjacent Triangle.
	 *
	 * @param num  the local number of this edge.
	 * @return the local number of symmetric edge in adjacent Triangle.
	 */
	public int getAdjLocalNumber(int num)
	{
		return (adjPos >> (2*num)) & 3;
	}
	
	// Helper functions
	private boolean isFlagged(int flag)
	{
		return ((edgeAttributes[0] | edgeAttributes[1] | edgeAttributes[2]) & flag) != 0;
	}

	private void setFlag(int flag)
	{
		edgeAttributes[0] |= flag;
		edgeAttributes[1] |= flag;
		edgeAttributes[2] |= flag;
	}

	/**
	 * Return the {@link OTriangle#OUTER} attribute of its edges.
	 *
	 * @return <code>true</code> if the triangle is outer,
	 * <code>false</code> otherwise.
	 */
	public boolean isOuter()
	{
		return isFlagged(OTriangle.OUTER);
	}
	
	/**
	 * Set the {@link OTriangle#OUTER} attribute of its three edges.
	 */
	public void setOuter()
	{
		setFlag(OTriangle.OUTER);
	}
	
	/**
	 * Return the {@link OTriangle#MARKED} attribute of its edges.
	 *
	 * @return <code>true</code> if an edge of this triangle has its
	 * {@link OTriangle#MARKED} attribute set, <code>false</code> otherwise.
	 */
	public boolean isMarked()
	{
		return isFlagged(OTriangle.MARKED);
	}
	
	/**
	 * Set the {@link OTriangle#MARKED} attribute of its three edges.
	 */
	public void setMarked()
	{
		setFlag(OTriangle.MARKED);
	}
	
	/**
	 * Clear the {@link OTriangle#MARKED} attribute of its three edges.
	 */
	public void unsetMarked()
	{
		edgeAttributes[0] &= ~OTriangle.MARKED;
		edgeAttributes[1] &= ~OTriangle.MARKED;
		edgeAttributes[2] &= ~OTriangle.MARKED;
	}
	
	/**
	 * Return the {@link OTriangle#BOUNDARY} attribute of its edges.
	 *
	 * @return <code>true</code> if an edge of this triangle has its
	 * {@link OTriangle#BOUNDARY} attribute set, <code>false</code>
	 * otherwise.
	 */
	public boolean isBoundary()
	{
		return isFlagged(OTriangle.BOUNDARY);
	}
	
	public boolean isReadable()
	{
		return (edgeAttributes[1] & 0x80) != 0;
	}
	
	public boolean isWritable()
	{
		return (edgeAttributes[2] & 0x80) != 0;
	}
	
	public void setReadable(boolean b)
	{
		if (b)
			edgeAttributes[1] |= 0x80;
		else
			edgeAttributes[1] &= ~0x80;
	}
	
	public void setWritable(boolean b)
	{
		if (b)
			edgeAttributes[2] |= 0x80;
		else
			edgeAttributes[2] &= ~0x80;
	}
	
	public int getEdgeAttributes(int num)
	{
		return edgeAttributes[num];
	}
	
	public void setEdgeAttributes(int num, int attributes)
	{
		edgeAttributes[num] = (byte) attributes;
	}
	
	public void setHalfEdge(HalfEdge e)
	{
		hedge = e;
	}
	
	public HalfEdge getHalfEdge()
	{
		return hedge;
	}
	
	private final String showAdj(int num)
	{
		String r = "";
		if (adj[num] == null)
			return "N/A";
		else if (adj[num] instanceof Triangle)
		{
			Triangle t = (Triangle) adj[num];
			if (t == null)
				r+= "null";
			else
				r+= t.hashCode()+"["+getAdjLocalNumber(num)+"]";
		}
		else
		{
			r+= "(";
			ArrayList a = (ArrayList) adj[num];
			boolean first = true;
			for (Iterator it = a.iterator(); it.hasNext(); )
			{
				Triangle t = (Triangle) it.next();
				Integer i = (Integer) it.next();
				if (!first)
					r+= ",";
				r+= t.hashCode()+"["+i+"]";
				first = false;
			}
			r+= ")";
		}
		return r;
	}
	
	private static String showHalfEdge(HalfEdge e)
	{
		String ret = ""+e.hashCode()+"(";
		if (e.sym() == null)
			ret += "null";
		else
			ret += e.sym().hashCode();
		ret += ")";
		return ret;
	}

	public String toString()
	{
		String r = "";
		r += "hashcode: "+hashCode();
		r += "\nVertices:";
		for (int i = 0; i < 3; i++)
			r += "\n  "+vertex[i];
		r += "\nAdjacency: "+showAdj(0)+" "+showAdj(1)+" "+showAdj(2);
		r += "\nEdge attributes:";
		for (int i = 0; i < 3; i++)
			r += " "+Integer.toHexString(edgeAttributes[i]);
		if (listNext != null)
			r += "\nLink next: "+listNext.hashCode();
		if (hedge != null)
			r += "\nHalfedge: "+showHalfEdge(hedge)+" "+showHalfEdge(hedge.next())+" "+showHalfEdge(hedge.next().next());
		return r;
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
	 *   for (Iterator it = tList.{@link List#iterator}; it.hasNext(); )
	 *   {
	 *     Triangle t = (Triangle) it.next();
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
		private final Triangle listHead = new Triangle();
		//   Sentinel.  This triangle is always the last triangle of the list.
		private final Triangle listSentinel = new Triangle();
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
		public void clear()
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
		 * Check whether this element appears in the list.
		 */
		public boolean contains(Triangle o)
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
				private Triangle curr = listHead;
				public boolean hasNext()
				{
					return curr.listNext != listSentinel;
				}
				
				public Object next()
				{
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
