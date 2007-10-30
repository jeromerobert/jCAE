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

/**
 * A triangular element of the mesh.  Instances of this class carry up
 * all topological information required for adjacency relations.  Their
 * vertices are contained in a {@link Vertex} array, and by convention
 * the local number of an edge is the index of its opposite vertex.  A
 * <code>TriangleVH</code> instance has a pointer to its three neighbours
 * through its edges, and knows the local number of opposite edges in
 * their respective triangles.  It also stores as a byte array attributes
 * of its three edges.
 *
 * <p>
 * As local numbers are integers between 0 and 2, a packed representation
 * is wanted to save space.  They can be stored within 6 bits, edge 0
 * is stored in the two least significant bits, edge 1 in next two and edge
 * 2 in next two, so <code>adjPos=(localNumberSymEdge0&lt;&lt;0)+(localNumberSymEdge1&lt;&lt;2)+(localNumberSymEdge2&lt;&lt;4)</code>.
 * </p>
 *
 * <p align="center"><img src="doc-files/TriangleVH-1.png" alt="[Image showing adjacency symmetric edges]"/></p>
 * <p>
 * Vertices data are colored in black, edges in red and triangles in blue.
 * Triangle <code>a</code> contains these members:
 * </p>
 * <ul>
 *   <li><code>a.vertex = { A, B, C }</code></li>
 *   <li><code>a.adjacentTriangles = { c, d, b }</code></li>
 *   <li><code>a.adjPos = (0 &lt;&lt;  0) + (1 &lt;&lt;  2) + (0 &lt;&lt;  0) = 2</li>
 * </ul>
 *
 * <p>
 * There are two special cases:
 * </p>
 * <ul>
 *   <li>Boundary edges; a virtual AbstractTriangle(outerVertex, v1, v2) is created,
 *       and linked to this edge.  This triangle has an {@link AbstractHalfEdge#OUTER}
 *       attribute, and boundary edges have a {@link AbstractHalfEdge#BOUNDARY} attribute.</li>
 *   <li>Non-manifold edges; a virtual AbstractTriangle(outerVertex, v1, v2) is
 *       also created, and linked to this edge.  This triangle has an
 *       {@link AbstractHalfEdge#OUTER} attribute, non-manifold edge and its symmetric edge
 *       have a {@link AbstractHalfEdge#NONMANIFOLD} attribute, and other two edges are used
 *       to build a circular doubly-linked list of all symmetric edges.</li>
 * </ul>
 */
public class TriangleVH extends Triangle
{
	/**
	 * Pointers to adjacent elements through edges.
	 */
	private TriangleVH [] adjacentTriangles = new TriangleVH[3];
	
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
		
	/**
	 * Constructor.
	 */
	public TriangleVH(TriangleTraitsBuilder ttb)
	{
		super(ttb);
	}

	@Override
	public final void copy(AbstractTriangle that)
	{
		super.copy(that);
		TriangleVH src = (TriangleVH) that;
		for (int i = 0; i < 3; i++)
			adjacentTriangles[i] = src.adjacentTriangles[i];
		adjPos = src.adjPos;
		edgeAttributes[0] = src.edgeAttributes[0];
		edgeAttributes[1] = src.edgeAttributes[1];
		edgeAttributes[2] = src.edgeAttributes[2];
	}
		
	/**
	 * Gets a <code>VirtualHalfEdge</code> instance bound to this triangle.
	 * This method allocates a new {@link VirtualHalfEdge} instance and binds
	 * it to this triangle.
	 * @return  a new <code>VirtualHalfEdge</code> instance bound to this triangle
	 */
	@Override
	public VirtualHalfEdge getAbstractHalfEdge()
	{
		VirtualHalfEdge ot = new VirtualHalfEdge();
		ot.bind(this);
		return ot;
	}

	/**
	 * Gets a <code>VirtualHalfEdge</code> instance bound to this triangle.
	 * If argument is null, this method behaves as if no argument was passed.
	 * Otherwise, argument is an existing {@link VirtualHalfEdge} instance
	 * which is bound to this triangle and returned.
	 * @param  that   either <code>null</code> or an existing <code>VirtualHalfEdge</code>
	 *                instance which is modified
	 * @return  a <code>VirtualHalfEdge</code> instance bound to this triangle
	 */
	@Override
	public VirtualHalfEdge getAbstractHalfEdge(AbstractHalfEdge that)
	{
		if (that == null)
			that = new VirtualHalfEdge();
		VirtualHalfEdge ot = (VirtualHalfEdge) that;
		ot.bind(this);
		return ot;
	}

	/**
	 * Returns the adjacent TriangleVH through an edge.
	 *
	 * @param num  the local number of this edge.
	 * @return the adjacent TriangleVH
	 */
	public TriangleVH getAdj(int num)
	{
		return adjacentTriangles[num];
	}
	
	/**
	 * Sets TriangleVH adjacent to an edge.
	 *
	 * @param num  the local number of this edge
	 * @param link  adjacent TriangleVH
	 */
	public void setAdj(int num, TriangleVH link)
	{
		adjacentTriangles[num] = link;
	}
	
	/**
	 * Gets local number of a symmetric edge.
	 *
	 * @param num   edge local number
	 * @return  local number of this symmetric edge
	 */
	public int getAdjLocalNumber(int num)
	{
		return (adjPos >> (2*num)) & 3;
	}

	/**
	 * Sets local number of a symmetric edge.
	 *
	 * @param num   edge local number
	 * @param pos   local number of symmetric edge
	 */
	public void setAdjLocalNumber(int num, int pos)
	{
		//  Clear previous adjacent position ...
		adjPos &= ~(3 << (2*num));
		//  ... and set it right
		adjPos |= (pos << (2*num));
	}

	/**
	 * Gets attributes of edge <code>num</code>.
	 *
	 * @param num  local edge number
	 * @return  attributes of this edge
	 */
	public int getEdgeAttributes(int num)
	{
		return edgeAttributes[num];
	}

	/**
	 * Sets attributes of edge <code>num</code>.
	 *
	 * @param num   local edge number
	 * @param attr  attributes to set on this edge
	 */
	public void setEdgeAttributes(int num, int attr)
	{
		edgeAttributes[num] = (byte) attr;
	}

	// Helper functions
	/**
	 * Sets attributes for all edges of this triangle.
	 *
	 * @param attr  attributes to set on edges
	 */
	@Override
	public void setAttributes(int attr)
	{
		edgeAttributes[0] |= attr;
		edgeAttributes[1] |= attr;
		edgeAttributes[2] |= attr;
	}

	/**
	 * Resets attributes for all edges of this triangle.
	 *
	 * @param attr  attributes to reset on edges
	 */
	@Override
	public void clearAttributes(int attr)
	{
		edgeAttributes[0] &= ~attr;
		edgeAttributes[1] &= ~attr;
		edgeAttributes[2] &= ~attr;
	}

	/**
	 * Checks if some attributes of this triangle are set.
	 *
	 * @param attr  attributes to check
	 * @return <code>true</code> if any edge of this triangle has
	 * one of these attributes set, <code>false</code> otherwise
	 */
	@Override
	public boolean hasAttributes(int attr)
	{
		return ((edgeAttributes[0] | edgeAttributes[1] | edgeAttributes[2]) & attr) != 0;
	}

	private final String showAdj(int num)
	{
		if (adjacentTriangles[num] == null)
			return "N/A";
		return adjacentTriangles[num].hashCode()+"["+getAdjLocalNumber(num)+"]";
	}
	
	@Override
	public String toString()
	{
		StringBuilder r = new StringBuilder(super.toString());
		r.append("Adjacency: "+showAdj(0)+" "+showAdj(1)+" "+showAdj(2));
		r.append("\nEdge attributes:");
		for (int i = 0; i < 3; i++)
			r.append(" "+edgeAttributes[i]);
		return r.toString();
	}
	
}
