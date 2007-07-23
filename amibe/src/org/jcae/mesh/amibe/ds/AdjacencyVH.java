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

import java.util.Iterator;
import java.util.ArrayList;

/**
 * A triangular element of the mesh.  Instances of this class carry up
 * all topological information required for adjacency relations.  Their
 * vertices are contained in a {@link Vertex} array, and by convention
 * the local number of an edge is the index of its opposite vertex.  A
 * <code>AbstractTriangle</code> instance has a pointer to its three neighbours
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
 * In his <a href="http://www.cs.cmu.edu/~quake/triangle.html">AbstractTriangle</a>
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
 *   <li>Boundary edges; a virtual AbstractTriangle(outerVertex, v1, v2) is created,
 *       and linked to this edge.  This triangle has an {@link AbstractHalfEdge#OUTER}
 *       flag, and symmetric edges have a {@link AbstractHalfEdge#BOUNDARY} flag.</li>
 *   <li>Non-manifold edges; a virtual AbstractTriangle(outerVertex, v1, v2) is
 *       also created, and linked to this edge.  This triangle has an
 *       {@link AbstractHalfEdge#OUTER} flag, and symmetric edges have a {@link
 *       AbstractHalfEdge#NONMANIFOLD} flag.  The outer triangle contains in
 *       <code>adj[i]</code> a list of all incident edges.  Thus all incident
 *       edges are linked to a different triangle, but all these triangles
 *       contain a pointer to the same list.
 * </ul>
 */
public class AdjacencyVH implements AdjacencyWrapper
{
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
	
	public final void copy(AdjacencyWrapper that)
	{
		AdjacencyVH src = (AdjacencyVH) that;
		for (int i = 0; i < 3; i++)
			adj[i] = src.adj[i];
		adjPos = src.adjPos;
		edgeAttributes[0] = src.edgeAttributes[0];
		edgeAttributes[1] = src.edgeAttributes[1];
		edgeAttributes[2] = src.edgeAttributes[2];
	}
	
	/**
	 * Change the adjacency relation of an edge.
	 * Only one relation is modified.  If both sides have to be modified,
	 * then {@link AbstractHalfEdge#glue} should be used instead.
	 *
	 * @param num the local number of the edge on this AbstractTriangle.
	 * @param that the AbstractTriangle attached to this edge.
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
	 * Return the adjacent AbstractTriangle.
	 * Note: this routine is not very helpful, caller can only check
	 * whether the returned object is null or if its type is AbstractTriangle.
	 * This can be performed by checking {@link AbstractHalfEdge#BOUNDARY}
	 * and {@link AbstractHalfEdge#NONMANIFOLD} attributes.
	 *
	 * @param num  the local number of this edge.
	 * @return the adjacent AbstractTriangle.
	 */
	public Object getAdj(int num)
	{
		return adj[num];
	}
	
	/**
	 * Set the AbstractTriangle adjacent to an edge.
	 * Note: this routine could certainly be replaced by {@link #glue1}.
	 *
	 * @param num  the local number of this edge.
	 * @param link  the adjacent AbstractTriangle.
	 */
	public void setAdj(int num, Object link)
	{
		adj[num] = link;
	}
	
	/**
	 * Return the local number of symmetric edge in adjacent AbstractTriangle.
	 *
	 * @param num  the local number of this edge.
	 * @return the local number of symmetric edge in adjacent AbstractTriangle.
	 */
	public int getAdjLocalNumber(int num)
	{
		return (adjPos >> (2*num)) & 3;
	}
	
	public void setAdjLocalNumber(int num, int pos)
	{
		//  Clear previous adjacent position ...
		adjPos &= ~(3 << (2*num));
		//  ... and set it right
		adjPos |= (pos << (2*num));
	}
	
	// Helper functions
	public boolean hasFlag(int flag)
	{
		return ((edgeAttributes[0] | edgeAttributes[1] | edgeAttributes[2]) & flag) != 0;
	}

	public void setFlag(int flag)
	{
		edgeAttributes[0] |= flag;
		edgeAttributes[1] |= flag;
		edgeAttributes[2] |= flag;
	}

	public void clearFlag(int flag)
	{
		edgeAttributes[0] &= ~flag;
		edgeAttributes[1] &= ~flag;
		edgeAttributes[2] &= ~flag;
	}

	/**
	 * Return the {@link AbstractHalfEdge#OUTER} attribute of its edges.
	 *
	 * @return <code>true</code> if the triangle is outer,
	 * <code>false</code> otherwise.
	 */
	public boolean isOuter()
	{
		return hasFlag(AbstractHalfEdge.OUTER);
	}
	
	/**
	 * Set the {@link AbstractHalfEdge#OUTER} attribute of its three edges.
	 */
	public void setOuter()
	{
		setFlag(AbstractHalfEdge.OUTER);
	}
	
	/**
	 * Return the {@link AbstractHalfEdge#MARKED} attribute of its edges.
	 *
	 * @return <code>true</code> if an edge of this triangle has its
	 * {@link AbstractHalfEdge#MARKED} attribute set, <code>false</code> otherwise.
	 */
	public boolean isMarked()
	{
		return hasFlag(AbstractHalfEdge.MARKED);
	}
	
	/**
	 * Set the {@link AbstractHalfEdge#MARKED} attribute of its three edges.
	 */
	public void setMarked()
	{
		setFlag(AbstractHalfEdge.MARKED);
	}
	
	/**
	 * Clear the {@link AbstractHalfEdge#MARKED} attribute of its three edges.
	 */
	public void unsetMarked()
	{
		clearFlag(AbstractHalfEdge.MARKED);
	}
	
	/**
	 * Return the {@link AbstractHalfEdge#BOUNDARY} attribute of its edges.
	 *
	 * @return <code>true</code> if an edge of this triangle has its
	 * {@link AbstractHalfEdge#BOUNDARY} attribute set, <code>false</code>
	 * otherwise.
	 */
	public boolean isBoundary()
	{
		return hasFlag(AbstractHalfEdge.BOUNDARY);
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
	
	private final String showAdj(int num)
	{
		if (adj[num] == null)
			return "N/A";
		else if (adj[num] instanceof Triangle)
		{
			Triangle t = (Triangle) adj[num];
			if (t == null)
				return "null";
			return t.hashCode()+"["+getAdjLocalNumber(num)+"]";
		}
		else
		{
			StringBuffer r = new StringBuffer("(");
			ArrayList a = (ArrayList) adj[num];
			boolean first = true;
			for (Iterator it = a.iterator(); it.hasNext(); )
			{
				Triangle t = (Triangle) it.next();
				Integer i = (Integer) it.next();
				if (!first)
					r.append(",");
				r.append(t.hashCode()+"["+i+"]");
				first = false;
			}
			r.append(")");
			return r.toString();
		}
	}
	
	public String toString()
	{
		StringBuffer r = new StringBuffer();
		r.append("Adjacency: "+showAdj(0)+" "+showAdj(1)+" "+showAdj(2));
		r.append("\nEdge attributes:");
		for (int i = 0; i < 3; i++)
			r.append(" "+Integer.toHexString(edgeAttributes[i]));
		return r.toString();
	}

}
