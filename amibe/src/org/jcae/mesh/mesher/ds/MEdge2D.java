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

import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;

/**
 * 2D edge.
 */
public class MEdge2D
{
	private static Logger logger=Logger.getLogger(MEdge2D.class);

	// First end point
	private MNode2D pt1;
	
	// Second end point
	private MNode2D pt2;
	
	// Middle point
	private MNode2D mid;
	
	// A reference to a MEdge1D entity
	private MEdge1D ref1d;
	
	//  Edge length, stored for performance reasons
	private double length = -1.0;
	
	//. Tells if edge is temporarily frozen
	private boolean isFrozen = false;
	
	//  ID used for debugging purpose
	private static int id = 0;
	private static HashMap mapHashcodeToID;
	//  Initialize mapHashcodeToID
	static { assert(null != (mapHashcodeToID = new HashMap())); };
	
	/**
	 * Creates an edge bounded by two <code>MNode2D</code> instances, and
	 * bind it to a <code>MEdge1D</code> instance.
	 *
	 * @param begin  first end point,
	 * @param end  second end point,
	 * @param edge1d  1D edge to link against.
	 */
	public MEdge2D(MNode2D begin, MNode2D end, MEdge1D edge1d)
	{
		pt1 = begin;
		pt2 = end;
		ref1d = edge1d;
		assert(setID());
	}
	
	/**
	 * Creates an edge bounded by two <code>MNode2D</code> instances.
	 *
	 * @param begin  first end point,
	 * @param end  second end point,
	 */
	public MEdge2D(MNode2D begin, MNode2D end)
	{
		pt1 = begin;
		pt2 = end;
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
	 * Computes the middle point.
	 */
	public void setMidPoint()
	{
		if (!isMutable())
			return;
		double u = 0.5 * (pt1.getU() + pt2.getU());
		double v = 0.5 * (pt1.getV() + pt2.getV());
		mid = new MNode2D(u, v);
	}
	
	/**
	 * Forget about the middle point.
	 */
	public void resetMidPoint()
	{
		mid = null;
	}
	
	/**
	 * Returns the middle point, if it has previously been computed, or
	 * <code>null</code> otherwise.
	 *
	 * @return the middle point.
	 */
	public MNode2D getMidPoint()
	{
		return mid;
	}
	
	/**
	 * Returns the first <code>MNode2D</code> end point.
	 *
	 * @return the first <code>MNode2D</code> end point.
	 */
	public MNode2D getNodes1()
	{
		return pt1;
	}
	
	/**
	 * Returns the second <code>MNode2D</code> end point.
	 *
	 * @return the second <code>MNode2D</code> end point.
	 */
	public MNode2D getNodes2()
	{
		return pt2;
	}
	
	/**
	 * Replaces an end point by a new node.
	 *
	 * @param pt  the end point being substituted.
	 * @param by  the new end point
	 * @throws NoSuchElementException if <code>pt</code> is not an end point.
	 */
	public void substNode(MNode2D pt, MNode2D by)
	{
		length = -1.0;
		if (pt1 == pt)
			pt1 = by;
		else if (pt2 == pt)
			pt2 = by;
		else
			throw new NoSuchElementException("MNode2D : "+pt);
	}
	
	/**
	 * Swap nodes.
	 */
	public void swapNodes()
	{
		MNode2D temp = pt1;
		pt1 = pt2;
		pt2 = temp;
	}
	
	/**
	 * Returns the reference edge.
	 * If there is no reference (i.e. this edge is not on boundary),
	 * <code>null</code> is returned.
	 *
	 * @return the reference edge if it does exist, <code>null</code> otherwise.
	 */
	public MEdge1D getRef()
	{
		if (null == ref1d)
			return null;
		return ref1d.getMaster();
	}
	
	/**
	 * Tells if this edge can be altered.
	 *
	 * @return <code>true</code> if this edge can be altered, and
	 * <code>false</code> otherwise.
	 */
	public boolean isMutable()
	{
		return (null == ref1d && !isFrozen);
	}
	
	/**
	 * Set the <code>isFrozen</code> flag.
	 *
	 * @param frozen   the flag value.
	 */
	public void setFrozen(boolean frozen)
	{
		isFrozen = frozen;
	}
	
	/**
	 * Returns the <code>isFrozen<code> flag.
	 *
	 * @return the <code>isFrozen<code> flag.
	 */
	public boolean isFrozen()
	{
		return isFrozen;
	}
	
	/**
	 * Returns the cached edge length.
	 *
	 * @return the cached edge length.
	 */
	public double getLength()
	{
		return length;
	}
	
	/**
	 * Sets the cached edge length.
	 *
	 * @param l  the cached edge length.
	 */
	public void setLength(double l)
	{
		length = l;
	}
	
	/**
	 * Returns the set of faces which are bounded by this edge entity.
	 *
	 * @return the set of faces which are bounded by this edge entity.
	 */
	public HashSet getFaces()
	{
		HashSet faceList=new HashSet();
		Iterator it=pt1.getElementsIterator();
		while(it.hasNext())
		{
			Object o = it.next();
			if (o instanceof MFace2D)
			{
				MFace2D f = (MFace2D) o;
				if (pt2.getElements().contains(f) )
					faceList.add(f);
			}
		}
		return faceList;
	}
	
	/**
	 * Returns an iterator over the faces bounded by this edge.
	 *
	 * @return an iterator over the faces bounded by this edge.
	 */
	public Iterator getFacesIterator()
	{
		return getFaces().iterator();
	}
	
	/**
	 * Link end nodes to a 1D finite element.
	 *
	 * @param e  the 1D finite element to be linked against.
	 */
	public void link(MEdge2D e)
	{
		pt1.link(e);
		pt2.link(e);
	}
	
	/**
	 * Link end nodes to a 2D finite element.
	 *
	 * @param f  the 2D finite element to be linked against.
	 */
	public void link(MFace2D f)
	{
		pt1.link(f);
		pt2.link(f);
	}
	
	/**
	 * Unlink end nodes to a 1D finite element.
	 *
	 * @param e  the 1D finite element to be removed from the list of 1D
	 * elements.
	 */
	public void unlink(MEdge2D e)
	{
		pt1.unlink(e);
		pt2.unlink(e);
	}
	
	/**
	 * Unlink end nodes to a 2D finite element.
	 *
	 * @param f  the 2D finite element to be removed from the list of 2D
	 * elements.
	 */
	public void unlink(MFace2D f)
	{
		pt1.unlink(f);
		pt2.unlink(f);
	}
	
	/**
	 * Checks whether an edge can be removed or not.  Boundary edges
	 * can not be removed, as well as edges which are 1D finite elements.
	 *
	 * @return <code>true</code> if this entity can be removed,
	 * <code>false</code> otherwise.
	 */
	public boolean canDestroy()
	{
		if (!isMutable())
			return false;
		Iterator it=pt1.getElements().iterator();
		while(it.hasNext())
		{
			if (pt2.getElements().contains(it.next()))
				return false;
		}
		return true;
	}
	
	/**
	 * Checks whether triangles bounded to this edge are inverted.
	 *
	 * @return <code>true</code> if triangles are not inverted,
	 * <code>false</code> otherwise.
	 */
/*
	public boolean checkNoInvertedTriangles()
	{
		if (!isMutable())
			return true;
		HashSet faces = getFaces();
		if (faces.size() != 2)
			return true;
		Iterator itf = faces.iterator();
		MNode2D apex1 = ((MFace2D) itf.next()).apex(this);
		MNode2D apex2 = ((MFace2D) itf.next()).apex(this);
		return (apex1.orient2d(pt1, pt2) * apex2.orient2d(pt1, pt2) <= 0.0);
	}
*/
	
	public String toString()
	{
		String r="MEdge2D: id="+getID()+
			" n1="+pt1.getID()+" n2="+pt2.getID();
		if (null != ref1d)
			r+=" ref1d="+ref1d.getID();
		return r;
	}
	
}
