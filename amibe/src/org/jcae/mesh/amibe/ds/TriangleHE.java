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

public class TriangleHE extends Triangle
{
	public TriangleHE(TriangleTraitsBuilder ttb)
	{
		super(ttb);
		adj = new AdjacencyHE();
	}

	@Override
	public AbstractHalfEdge getAbstractHalfEdge()
	{
		return ((AdjacencyHE) adj).getHalfEdge(0);
	}

	public HalfEdge getHalfEdge()
	{
		return ((AdjacencyHE) adj).getHalfEdge(0);
	}

	public void setHalfEdge(HalfEdge e)
	{
		((AdjacencyHE) adj).setHalfEdge(0, e);
	}

	private static class AdjacencyHE implements AdjacencyWrapper
	{
		/**
		 * Pointers to adjacent elements through edges.
		 */
		private HalfEdge edge = null;
	
		@Override
		public final void copy(AdjacencyWrapper that)
		{
			if (!(that instanceof AdjacencyHE))
				throw new IllegalArgumentException();
			AdjacencyHE src = (AdjacencyHE) that;
			HalfEdge to = edge;
			HalfEdge from = src.edge;
			for (int i = 0; i < 3; i++)
			{
				to.copy(from);
				to = (HalfEdge) to.next();
				from = (HalfEdge) from.next();
			}
		}
		
		private HalfEdge getHalfEdge(int num)
		{
			if (num == 0)
				return edge;
			else if (num == 1)
				return (HalfEdge) edge.next();
			else if (num == 2)
				return (HalfEdge) edge.prev();
			else
				throw new RuntimeException();
		}
	
		private void setHalfEdge(int num, HalfEdge e)
		{
			if (num == 0)
				edge = e;
			else if (num == 1)
				edge.setNext(e);
			else if (num == 2)
				((HalfEdge) edge.next()).setNext(e);
			else
				throw new RuntimeException();
		}
	
		@Override
		public Object getAdj(int num)
		{
			return getHalfEdge(num).sym();
		}
	
		@Override
		public void setAdj(int num, Object link)
		{
			getHalfEdge(num).glue((HalfEdge) link);
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
			edge.setAttributes(attr);
			edge.next().setAttributes(attr);
			edge.prev().setAttributes(attr);
		}
	
		/**
		 * Resets attributes for all edges of this triangle.
		 *
		 * @param attr  attributes to reset on edges
		 */
		@Override
		public void clearAttributes(int attr)
		{
			edge.clearAttributes(attr);
			edge.next().clearAttributes(attr);
			edge.prev().clearAttributes(attr);
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
			return (edge.hasAttributes(attr) | edge.next().hasAttributes(attr) | edge.prev().hasAttributes(attr));
		}
	
		private static String showHalfEdge(AbstractHalfEdge a)
		{
			HalfEdge e = (HalfEdge) a;
			if (e == null)
				return "null";
			StringBuilder ret = new StringBuilder(""+e.hashCode()+"(");
			if (!e.hasSymmetricEdge())
				ret.append("null");
			else
				ret.append(e.sym().hashCode());
			ret.append(")");
			return ret.toString();
		}
	
		private static String showSym(AbstractHalfEdge a)
		{
			HalfEdge e = (HalfEdge) a;
			if (e == null)
				return "null";
			else if (!e.hasSymmetricEdge())
				return "null";
			else
			{
				HalfEdge sym = (HalfEdge) e.sym();
				return ""+sym.getTri().hashCode()+"["+sym.getLocalNumber()+"]";
			}
		}
	
		@Override
		public String toString()
		{
			StringBuilder r = new StringBuilder(50);
			r.append("Halfedge: "+showHalfEdge(edge)+" "+showHalfEdge(edge.next())+" "+showHalfEdge(edge.prev()));
			r.append("\nSym: "+showSym(edge)+" "+showSym(edge.next())+" "+showSym(edge.prev()));
			r.append("\nEdge attributes:");
			for (int i = 0; i < 3; i++)
				r.append(" "+getHalfEdge(i).getAttributes());
			return r.toString();
		}
	}
}
