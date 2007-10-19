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
			return getHalfEdge(num).getAdj();
		}
	
		@Override
		public void setAdj(int num, Object link)
		{
			getHalfEdge(num).setAdj(link);
		}
	
		@Override
		public int getAdjLocalNumber(int num)
		{
			return getHalfEdge(num).getLocalNumber();
		}
	
		@Override
		public void setAdjLocalNumber(int num, int pos)
		{
			throw new RuntimeException();
		}
	
		// Helper functions
		@Override
		public boolean hasFlag(int flag)
		{
			return (edge.hasAttributes(flag) | edge.next().hasAttributes(flag) | edge.prev().hasAttributes(flag));
		}
	
		@Override
		public void setFlag(int flag)
		{
			edge.setAttributes(flag);
			edge.next().setAttributes(flag);
			edge.prev().setAttributes(flag);
		}
	
		@Override
		public void clearFlag(int flag)
		{
			edge.clearAttributes(flag);
			edge.next().clearAttributes(flag);
			edge.prev().clearAttributes(flag);
		}
	
		@Override
		public int getEdgeAttributes(int num)
		{
			return getHalfEdge(num).getAttributes();
		}
		
		@Override
		public void setEdgeAttributes(int num, int attributes)
		{
			getHalfEdge(num).setAttributes((byte) attributes);
		}
		
		private static String showHalfEdge(AbstractHalfEdge a)
		{
			HalfEdge e = (HalfEdge) a;
			if (e == null)
				return "null";
			StringBuilder ret = new StringBuilder(""+e.hashCode()+"(");
			if (e.getAdj() == null)
				ret.append("null");
			else
				ret.append(e.getAdj().hashCode());
			ret.append(")");
			return ret.toString();
		}
	
		private static String showSym(AbstractHalfEdge a)
		{
			HalfEdge e = (HalfEdge) a;
			if (e == null)
				return "null";
			else if (e.getAdj() == null)
				return "null";
			else if (e.getAdj() instanceof HalfEdge)
			{
				HalfEdge sym = (HalfEdge) e.sym();
				return ""+sym.getTri().hashCode()+"["+sym.getLocalNumber()+"]";
			}
			else
				return "NM";
		}
	
		@Override
		public String toString()
		{
			StringBuilder r = new StringBuilder(50);
			r.append("Halfedge: "+showHalfEdge(edge)+" "+showHalfEdge(edge.next())+" "+showHalfEdge(edge.prev()));
			r.append("\nSym: "+showSym(edge)+" "+showSym(edge.next())+" "+showSym(edge.prev()));
			r.append("\nEdge attributes:");
			for (int i = 0; i < 3; i++)
				r.append(" "+Integer.toHexString(getHalfEdge(i).getAttributes()));
			return r.toString();
		}
	}
}
