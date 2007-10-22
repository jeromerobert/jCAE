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
 *   <li>Boundary edges; a virtual AbstractTriangle(outerVertex, v1, v2) is created,
 *       and linked to this edge.  This triangle has an {@link AbstractHalfEdge#OUTER}
 *       attribute, and symmetric edges have a {@link AbstractHalfEdge#BOUNDARY} attribute.</li>
 *   <li>Non-manifold edges; a virtual AbstractTriangle(outerVertex, v1, v2) is
 *       also created, and linked to this edge.  This triangle has an
 *       {@link AbstractHalfEdge#OUTER} attribute, symmetric edge has a {@link
 *       AbstractHalfEdge#NONMANIFOLD} attribute, and other two edges are used to build
 *       a circular doubly-linked list of all symmetric edges.</li>
 * </ul>
 */
public class TriangleVH extends Triangle implements AdjacencyWrapper
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
		
	@Override
	public AbstractHalfEdge getAbstractHalfEdge()
	{
		VirtualHalfEdge ot = new VirtualHalfEdge();
		ot.bind(this);
		return ot;
	}

	/**
	 * Gets local number of a symmetric edge.  With <code>TriangleVH</code>,
	 * <code>getAdj(int)</code> returns a <code>TriangleVH</code> instance,
	 * but we also need the local number of symmetric edge in adjacent triangle.
	 * This is performed by this method.
	 *
	 * @param num   edge local number
	 * @return  local number of this symmetric edge
	 */
	public int getAdjLocalNumber(int num)
	{
		return (adjPos >> (2*num)) & 3;
	}

	/**
	 * Sets local number of a symmetric edge.  With <code>TriangleVH</code>,
	 * <code>setAdj(int, Object)</code> sets <code>TriangleVH</code> instance
	 * adjacent to an edge, but we also need to store the local number of symmetric
	 * edge in adjacent triangle.  This is performed by this method.
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

	public void setEdgeAttributes(int num, int attributes)
	{
		edgeAttributes[num] = (byte) attributes;
	}

	public int getEdgeAttributes(int num)
	{
		return edgeAttributes[num];
	}
		
	/**
	 * Implement AdjacencyWrapper methods.
	 */

	/**
	 * Returns the adjacent TriangleVH through an edge.
	 *
	 * @param num  the local number of this edge.
	 * @return the adjacent TriangleVH
	 */
	@Override
	public AdjacencyWrapper getAdj(int num)
	{
		return adjacentTriangles[num];
	}
	
	/**
	 * Sets TriangleVH adjacent to an edge.
	 *
	 * @param num  the local number of this edge
	 * @param link  adjacent TriangleVH
	 */
	@Override
	public void setAdj(int num, AdjacencyWrapper link)
	{
		adjacentTriangles[num] = (TriangleVH) link;
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

	@SuppressWarnings("unchecked")
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
