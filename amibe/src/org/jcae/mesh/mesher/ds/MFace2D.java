/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.
 
	Copyright (C) 2003 Jerome Robert <jeromerobert@users.sourceforge.net>
 
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
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.mesher.ds;

import gnu.trove.THashSet;

import org.jcae.mesh.cad.CADGeomSurface;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;

/**
 * 2D triangle.
 */
public class MFace2D
{
	static private Logger logger=Logger.getLogger(MFace2D.class);
	
	/** The list of edges bounding the face. */
	protected MEdge2D[] edgelist=new MEdge2D[3];
	
	//  ID used for debugging purpose.
	private static int id = 0;
	private static HashMap mapHashcodeToID;
	//  Initialize mapHashcodeToID
	static { assert(null != (mapHashcodeToID = new HashMap())); }
	
	/**
	 * Creates a triangle bounded by three <code>MEdge2D</code> instances.
	 *
	 * @param e1  first edge
	 * @param e2  second edge
	 * @param e3  third edge
	 */
	public MFace2D(MEdge2D e1, MEdge2D e2, MEdge2D e3)
	{
		edgelist[0]=e1;
		edgelist[1]=e2;
		edgelist[2]=e3;		
		Iterator it=getNodesIterator();
		while (it.hasNext())
		{
			MNode2D n=(MNode2D) it.next();
			n.link(this);
		}
		assert(setID());
	}
	
	private boolean setID()
	{
		id++;
		mapHashcodeToID.put(this, new Integer(id));
		return true;
	}
	
	/**
	 * Returns the public identifer.
	 *
	 * @return the public identifer.
	 */
	public int getID()
	{
		if (id > 0)
			return ((Integer)mapHashcodeToID.get(this)).intValue();
		else
			return hashCode();
	}
	
	/**
	 * Returns the set of nodes of this triangle.
	 *
	 * @return the set of nodes of this triangle.
	 */
	public Collection getNodes()
	{			
		THashSet toReturn=new THashSet(3);
		for(int i=0;i<edgelist.length;i++)
		{
			MEdge2D e = edgelist[i];
			toReturn.add(e.getNodes1());
			toReturn.add(e.getNodes2());
		}		
		return toReturn;
	}
	
	/**
	 * Returns an iterator over the nodes of this triangle.
	 *
	 * @return an iterator over the nodes of this triangle.
	 */
	public Iterator getNodesIterator()
	{			
		return getNodes().iterator();
	}
	
	/**
	 * Returns the set of edges of this triangle.
	 * This is a copy of edge list, so modifications performed on
	 * this <code>HashSet</code> will be lost.
	 *
	 * @return the set of edges of this triangle.
	 */
	public HashSet getEdges()
	{
		return new HashSet(java.util.Arrays.asList(edgelist));
	}
	
	/**
	 * Returns an iterator over the edges of this triangle.
	 *
	 * @return an iterator over the edges of this triangle.
	 */
	public Iterator getEdgesIterator()
	{
		return java.util.Arrays.asList(edgelist).iterator();
	}
	
	/**
	 * Returns the number of edges of this triangle.
	 *
	 * @return the number of edges of this triangle.
	 */
	public int numberOfEdges()
	{
		return edgelist.length;
	}
	
	/**
	 * Adds an edge to the list of edges, if it was not already present.
	 *
	 * @param edge   the edge to be added to the list.
	 */
	public void addEdge(MEdge2D edge)
	{
		for(int i=0;i<edgelist.length;i++)
			if(edgelist[i] == edge)
				return;

		MEdge2D[] na=new MEdge2D[edgelist.length+1];
		System.arraycopy(edgelist, 0, na, 0, edgelist.length);
		na[edgelist.length]=edge;
		edgelist=na;		
		edge.link(this);
	}
	
	/**
	 * In the list of edges, replace an edge by another one.  If
	 * <code>edge</code> and <code>by</code> are the same object, this routine
	 * immediately returns.  If <code>edge</code> is not in the edge list, an
	 * exception is raised.
	 *
	 * @param edge   the edge to be substituted,
	 * @param by     the new edge.
	 * @throws NoSuchElementException if <code>edge</code> is not present in
	 * the edge list.
	 */
	public void substEdge(MEdge2D edge, MEdge2D by)
	{
		if (edge == by)
			return;
		for(int i=0;i<edgelist.length;i++)
		{
			MEdge2D e = edgelist[i];
			if (e == edge)
			{
				edgelist[i] = by;
				return;
			}
		}
		throw new NoSuchElementException("MEdge2D : "+edge);
	}
	
	/**
	 * Returns the opposite node of a given edge.
	 *
	 * @param edge  an edge from the edge list
	 * @return the opposite node of a given edge.
	 * @throws NoSuchElementException if <code>edge</code> is not present in
	 * the edge list.
	 */
	public MNode2D apex(MEdge2D edge)
	{
		Collection triangleNodes = getNodes();
		triangleNodes.remove(edge.getNodes1());
		triangleNodes.remove(edge.getNodes2());
		if (1 != triangleNodes.size())
			throw new NoSuchElementException("MEdge2D : "+edge);
		return (MNode2D)triangleNodes.iterator().next();
	}
	
	/**
	 * Link nodes to a 1D finite element.
	 *
	 * @param e  the 1D finite element to be linked against.
	 */
	public void link(MEdge2D e)
	{
		Iterator it=getNodesIterator();
		while (it.hasNext())
		{
			MNode2D n=(MNode2D) it.next();
			n.link(e);
		}
	}
	
	/**
	 * Link nodes to the current triangle.
	 */
	public void link()
	{
		Iterator it=getNodesIterator();
		while (it.hasNext())
		{
			MNode2D n=(MNode2D) it.next();
			n.link(this);
		}
	}
	
	/**
	 * Unlink nodes to a 1D finite element.
	 *
	 * @param e  the 1D finite element to be removed from the list of 1D
	 * elements.
	 */
	public void unlink(MEdge2D e)
	{
		Iterator it=getNodesIterator();
		while (it.hasNext())
		{
			MNode2D n=(MNode2D) it.next();
			n.unlink(e);
		}
	}
	
	/**
	 * Unlink nodes to the current triangle.
	 */
	public void unlink()
	{
		Iterator it=getNodesIterator();
		while (it.hasNext())
		{
			MNode2D n=(MNode2D) it.next();
			n.unlink(this);
		}
	}
	
	/**
	 * Checks the validity of a 2D triangle.  This method is called within
	 * assertions, this is why it returns a <code>boolean</code>.
	 *
	 * @return <code>true</code> if all checks pass.
	 * @throws RuntimeException with an appropriate error message if a check
	 * fails.
	 */
	public boolean isValid()
	{
		if (3 != getEdges().size())
			throw new RuntimeException(""+this+" Error: found "+getEdges().size()+" edges");
		if (3 != getNodes().size())
			throw new RuntimeException(""+this+" Error: found "+getNodes().size()+" nodes");
		for (Iterator itn=getNodesIterator(); itn.hasNext();)
		{
			MNode2D n = (MNode2D) itn.next();
			if (!n.getElements().contains(this))
				throw new RuntimeException(""+this+" Error: node "+n+" is not linked against current triangle");
		}
		return true;
	}
	
	public String toString()
	{		
		String r="MFace2D: id="+getID();
		for (int i=0;i<edgelist.length;i++)
			r+=" e"+(i+1)+"="+edgelist[i].getID();
		int i=0;
		for (Iterator it = getNodesIterator(); it.hasNext(); i++)
			r+=" n"+(i+1)+"="+((MNode2D) it.next()).getID();
		return r;
	}
	
}
