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

public class AdjacencyHE implements AdjacencyWrapper
{
	/**
	 * Pointers to adjacent elements through edges.
	 */
	private HalfEdge edge = null;

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
	
	public HalfEdge getHalfEdge(int num)
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

	public void setHalfEdge(int num, HalfEdge e)
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

	public Object getAdj(int num)
	{
		return getHalfEdge(num).getAdj();
	}

	public void setAdj(int num, Object link)
	{
		getHalfEdge(num).setAdj(link);
	}

	public int getAdjLocalNumber(int num)
	{
		return getHalfEdge(num).getLocalNumber();
	}

	public void setAdjLocalNumber(int num, int pos)
	{
		throw new RuntimeException();
	}

	// Helper functions
	public boolean hasFlag(int flag)
	{
		return (edge.hasAttributes(flag) | edge.next().hasAttributes(flag) | edge.prev().hasAttributes(flag));
	}

	public void setFlag(int flag)
	{
		edge.setAttributes(flag);
		edge.next().setAttributes(flag);
		edge.prev().setAttributes(flag);
	}

	public void clearFlag(int flag)
	{
		edge.clearAttributes(flag);
		edge.next().clearAttributes(flag);
		edge.prev().clearAttributes(flag);
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
		edge.clearAttributes(AbstractHalfEdge.MARKED);
		edge.next().clearAttributes(AbstractHalfEdge.MARKED);
		edge.prev().clearAttributes(AbstractHalfEdge.MARKED);
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
		throw new RuntimeException("Not implemented yet");
	}
	
	public boolean isWritable()
	{
		throw new RuntimeException("Not implemented yet");
	}
	
	public void setReadable(boolean b)
	{
		throw new RuntimeException("Not implemented yet");
	}
	
	public void setWritable(boolean b)
	{
		throw new RuntimeException("Not implemented yet");
	}
	
	public int getEdgeAttributes(int num)
	{
		return getHalfEdge(num).getAttributes();
	}
	
	public void setEdgeAttributes(int num, int attributes)
	{
		getHalfEdge(num).setAttributes((byte) attributes);
	}
	
	private static String showHalfEdge(AbstractHalfEdge a)
	{
		HalfEdge e = (HalfEdge) a;
		if (e == null)
			return "null";
		String ret = ""+e.hashCode()+"(";
		if (e.getAdj() == null)
			ret += "null";
		else
			ret += e.getAdj().hashCode();
		ret += ")";
		return ret;
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
			return "xxx";
	}

	@Override
	public String toString()
	{
		StringBuffer r = new StringBuffer(50);
		r.append("Halfedge: "+showHalfEdge(edge)+" "+showHalfEdge(edge.next())+" "+showHalfEdge(edge.prev()));
		r.append("\nSym: "+showSym(edge)+" "+showSym(edge.next())+" "+showSym(edge.prev()));
		r.append("\nEdge attributes:");
		for (int i = 0; i < 3; i++)
			r.append(" "+Integer.toHexString(getHalfEdge(i).getAttributes()));
		return r.toString();
	}
}
