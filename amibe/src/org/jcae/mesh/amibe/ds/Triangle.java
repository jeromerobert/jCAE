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
import java.util.NoSuchElementException;
import java.util.Iterator;
import java.util.ArrayList;
import org.apache.log4j.Logger;

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
 * a single byte instead.  As attributes on edges are also needed, all
 * edge data are packed into a single {@link #adjPos} integer insstance
 * variable.
 * </p>
 *
 * <p>
 * Algorithms do often need to compute lists of triangles.  In order to
 * avoid allocation of these lists, a singly linked list is provided by this
 * class.  It uses class variables, so only one list can be active at
 * any time.  Here is an example:
 * </p>
 *   <pre>
 *   //  Begin a new list
 *   Triangle.{@link #listLock};
 *   ...
 *   //  In a loop, add triangles to this list.
 *     tri.{@link #listCollect};
 *   //  Check whether a triangle is contained in this list.
 *   //  This is very fast because it tests if its link pointer
 *   //  is <code>null</code> or not.
 *     if (tri.{@link #isListed}) {
 *        ...
 *     }
 *   //  Loop over collected triangles.
 *   for (Iterator it = Triangle.{@link #getTriangleListIterator}; it.hasNext(); )
 *   {
 *     Triangle t = (Triangle) it.next();
 *     ...
 *   }
 *   //  When finished, remove all links between triangles
 *   Triangle.{@link #listRelease};
 *   </pre>
 * <p>
 * New elements are added at the end of the list so that {@link #listCollect} can
 * be called while {@link #getTriangleListIterator} is in action.
 * </p>
 */
public class Triangle
{
	private static Logger logger = Logger.getLogger(Triangle.class);
	/**
	 * Three vertices.
	 */
	public Vertex [] vertex;
	
	/**
	 * Pointers to adjacent elements through edges.
	 */
	private Object [] adj = new Object[3];
	
	/**
	 * Edge packed data.
	 * Byte 0 contains the local number of opposite edges in
	 * their respective triangles:
	 * <ul>
	 *     <li>bits 0-1: local number for matte edge 0</li>
	 *     <li>bits 2-3: local number for matte edge 1</li>
	 *     <li>bits 4-5: local number for matte edge 2</li>
	 * </ul>
	 *  Bytes 1, 2 and 3 carry up bitfield attributes for edges 0, 1 and 2.
	*/
	private byte [] adjPos = new byte[4];
	private int groupId = -1;
	// Reference to the next element in the singly linked list.
	private Triangle listNext = null;
	// Reference to edge V0-V1, for algorithms based on edges
	public HalfEdge edge = null;
	
	// We need to process lists of triangles, and sometimes make sure
	// that triangles are processed only once.  This can be achieved
	// efficiently with a singly linked list.
	//   Head of the list.  Triangles are linked from this instance.
	private static final Triangle listHead = new Triangle();
	//   Sentinel.  This triangle is always the last triangle of the list.
	private static final Triangle listSentinel = new Triangle();
	//   Reference to the last collected triangle.
	private static Triangle listTail = null;
	//   Number of collected items (for debugging purpose, can be removed).
	private static int listSize = 0;
	
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
		vertex[0].setLink(this);
		vertex[1].setLink(this);
		vertex[2].setLink(this);
	}
	
	public Triangle(Vertex [] v)
	{
		if (v[0] instanceof Vertex2D)
			vertex = new Vertex2D[v.length];
		else
			vertex = new Vertex[v.length];
		for (int i = v.length - 1; i >= 0; i--)
		{
			vertex[i] = v[i];
			vertex[i].setLink(this);
		}
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
		for (int i = 0; i < adjPos.length; i++)
			adjPos[i] = that.adjPos[i];
		if (that.listNext != null)
			listCollect();
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
		adjPos[0] &= ~(3 << (2*num));
		//  ... and set it right
		adjPos[0] |= (thatnum << (2*num));
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
		return (adjPos[0] >> (2*num)) & 3;
	}
	
	/**
	 * Return the 2D centroid of this triangle.
	 *
	 * @return the 2D centroid of this triangle.
	 */
	public Vertex2D centroid()
	{
		double [] p1 = vertex[0].getUV();
		double [] p2 = vertex[1].getUV();
		double [] p3 = vertex[2].getUV();
		return Vertex2D.valueOf(
			(p1[0]+p2[0]+p3[0])/3.0,
			(p1[1]+p2[1]+p3[1])/3.0
		);
	}
	
	// Helper functions
	private boolean isFlagged(int flag)
	{
		return ((adjPos[1] | adjPos[2] | adjPos[3]) & flag) != 0;
	}

	private void setFlag(int flag)
	{
		adjPos[1] |= flag;
		adjPos[2] |= flag;
		adjPos[3] |= flag;
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
		adjPos[1] &= ~OTriangle.MARKED;
		adjPos[2] &= ~OTriangle.MARKED;
		adjPos[3] &= ~OTriangle.MARKED;
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
	
	/**
	 * Swap attributes for edges 1 and 2.
	 * @see OTriangle#invertOrientationFace
	 */
	void swapAttributes12()
	{
		byte temp = adjPos[2];
		adjPos[2] = adjPos[3];
		adjPos[3] = temp;
	}
	
	/**
	 * Swap attributes for edges 0 and 1.
	 * @see OTriangle#invertOrientationFace
	 */
	void swapAttributes01()
	{
		byte temp = adjPos[1];
		adjPos[1] = adjPos[2];
		adjPos[2] = temp;
	}
	
	/**
	 * Swap attributes for edges 0 and 2.
	 * @see OTriangle#invertOrientationFace
	 */
	void swapAttributes02()
	{
		byte temp = adjPos[1];
		adjPos[1] = adjPos[3];
		adjPos[3] = temp;
	}
	
	public boolean isReadable()
	{
		return (adjPos[1] & 0x80) != 0;
	}
	
	public boolean isWritable()
	{
		return (adjPos[2] & 0x80) != 0;
	}
	
	public void setReadable(boolean b)
	{
		if (b)
			adjPos[1] |= 0x80;
		else
			adjPos[1] &= ~0x80;
	}
	
	public void setWritable(boolean b)
	{
		if (b)
			adjPos[2] |= 0x80;
		else
			adjPos[2] &= ~0x80;
	}
	
	public int getEdgeAttributes(int num)
	{
		return adjPos[num+1];
	}
	
	public void setEdgeAttributes(int num, int attributes)
	{
		adjPos[num+1] = (byte) attributes;
	}
	
	/**
	 * Initialize a triangle linked list.  There can be only one
	 * active linked list.
	 *
	 * @throws ConcurrentModificationException if this method is
	 * called again before this list has been released.
	 */
	public static void listLock()
	{
		if (listTail != null)
			throw new ConcurrentModificationException();
		listTail = listHead;
		listTail.listNext = listSentinel;
		listSize = 0;
	}
	
	/**
	 * Release the triangle linked list.  This method has to be called
	 * before creating a new list.
	 * @throws NoSuchElementException if no list has been created.
	 */
	public static void listRelease()
	{
		if (listTail == null)
			throw new NoSuchElementException();
		Triangle next;
		for (Triangle start = listHead; start != listSentinel; start = next)
		{
			next = start.listNext;
			start.listNext = null;
			listSize--;
		}
		listSize++;
		assert listSize == 0;
		listTail = null;
	}
	
	/**
	 * Add the current triangle to the end of the list.
	 * @throws ConcurrentModificationException if this element is
	 * already linked.
	 */
	public final void listCollect()
	{
		assert listTail != null;
		assert listTail.listNext == listSentinel : listTail;
		if (listNext != null)
			throw new ConcurrentModificationException();
		listTail.listNext = this;
		listTail = this;
		listNext = listSentinel;
		listSize++;
	}
	
	/**
	 * Check whether this element is linked.
	 */
	public boolean isListed()
	{
		return listNext != null;
	}
	
	/**
	 * Create an iterator over linked triangles.
	 *
	 * @throws NoSuchElementException if no list has been created.
	 */
	public static Iterator getTriangleListIterator()
	{
		if (listTail == null)
			throw new NoSuchElementException();
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
	
	public void createEdges()
	{
		HalfEdge hedge0 = new HalfEdge(this, (byte) 0, adjPos[1]);
		HalfEdge hedge1 = new HalfEdge(this, (byte) 1, adjPos[2]);
		HalfEdge hedge2 = new HalfEdge(this, (byte) 2, adjPos[3]);
		hedge0.setNext(hedge1);
		hedge1.setNext(hedge2);
		hedge2.setNext(hedge0);
		edge = hedge0;
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
				r+= t.hashCode()+"["+(((adjPos[0] & (3 << (2*num))) >> (2*num)) & 3)+"]";
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
			r += " "+Integer.toHexString(adjPos[1+i] & 0xff);
		if (listNext != null)
			r += "\nLink next: "+listNext.hashCode();
		if (edge != null)
			r += "\nHalfedge: "+edge.hashCode()+" "+edge.next().hashCode()+" "+edge.next().next().hashCode();
		return r;
	}

}
