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
import java.util.LinkedHashMap;
import java.util.Map;

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
public class TriangleVH extends Triangle
{
	public TriangleVH(TriangleTraitsBuilder ttb)
	{
		super(ttb);
		adj = new AdjacencyVH();
	}

	@Override
	public AbstractHalfEdge getAbstractHalfEdge()
	{
		VirtualHalfEdge ot = new VirtualHalfEdge();
		ot.bind(this);
		return ot;
	}

	private static class AdjacencyVH implements AdjacencyWrapper
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
		
		@Override
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
		 * Return the adjacent AbstractTriangle.
		 * Note: this routine is not very helpful, caller can only check
		 * whether the returned object is null or if its type is AbstractTriangle.
		 * This can be performed by checking {@link AbstractHalfEdge#BOUNDARY}
		 * and {@link AbstractHalfEdge#NONMANIFOLD} attributes.
		 *
		 * @param num  the local number of this edge.
		 * @return the adjacent AbstractTriangle.
		 */
		@Override
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
		@Override
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
		@Override
		public int getAdjLocalNumber(int num)
		{
			return (adjPos >> (2*num)) & 3;
		}
		
		@Override
		public void setAdjLocalNumber(int num, int pos)
		{
			//  Clear previous adjacent position ...
			adjPos &= ~(3 << (2*num));
			//  ... and set it right
			adjPos |= (pos << (2*num));
		}
		
		// Helper functions
		@Override
		public boolean hasFlag(int flag)
		{
			return ((edgeAttributes[0] | edgeAttributes[1] | edgeAttributes[2]) & flag) != 0;
		}
	
		@Override
		public void setFlag(int flag)
		{
			edgeAttributes[0] |= flag;
			edgeAttributes[1] |= flag;
			edgeAttributes[2] |= flag;
		}
	
		@Override
		public void clearFlag(int flag)
		{
			edgeAttributes[0] &= ~flag;
			edgeAttributes[1] &= ~flag;
			edgeAttributes[2] &= ~flag;
		}
	
		@Override
		public int getEdgeAttributes(int num)
		{
			return edgeAttributes[num];
		}
		
		@Override
		public void setEdgeAttributes(int num, int attributes)
		{
			edgeAttributes[num] = (byte) attributes;
		}
		
		@SuppressWarnings("unchecked")
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
				StringBuilder r = new StringBuilder("(");
				LinkedHashMap<Triangle, Integer> a = (LinkedHashMap<Triangle, Integer>) adj[num];
				boolean first = true;
				for (Map.Entry<Triangle, Integer> entry: a.entrySet())
				{
					Triangle t = entry.getKey();
					int i = entry.getValue().intValue();
					if (!first)
						r.append(",");
					r.append(t.hashCode()+"["+i+"]");
					first = false;
				}
				r.append(")");
				return r.toString();
			}
		}
		
		@Override
		public String toString()
		{
			StringBuilder r = new StringBuilder();
			r.append("Adjacency: "+showAdj(0)+" "+showAdj(1)+" "+showAdj(2));
			r.append("\nEdge attributes:");
			for (int i = 0; i < 3; i++)
				r.append(" "+Integer.toHexString(edgeAttributes[i]));
			return r.toString();
		}
	
	}
}
