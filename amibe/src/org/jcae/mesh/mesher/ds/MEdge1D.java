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

import org.jcae.mesh.mesher.ds.MNode1D;
import java.util.HashSet;
import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 * 1D edge.
 */

public class MEdge1D
{
	private static Logger logger=Logger.getLogger(MEdge1D.class);
	
	//  First end point
	private MNode1D pt1;
	
	//  Second end point
	private MNode1D pt2;
	
	//  A boolean flag to test if the edge is a finite element.
	//  Not used yet.
	private boolean isFE;
	
	//  In order to mesh surfaces in 2D space, some boundaries may need
	//  to be duplicated.  This member keeps track of these duplicates
	//  so that they can be removed when transported into 3D space.
	private MEdge1D master;
	
	private static int id = 0;
	private static HashMap mapHashcodeToID;
	//  Initialize mapHashcodeToID
	static { assert(null != (mapHashcodeToID = new HashMap())); };
	
	/**
	 * Creates an edge bounded by two <code>MNode1D</code> instances.
	 *
	 * @param  begin   first end point,
	 * @param  end     second end point,
	 * @param  isFiniteElement  <code>true</code> if this edge is a 1D finite
	 * element.
	 */
	public MEdge1D(MNode1D begin, MNode1D end, boolean isFiniteElement)
	{
		pt1 = begin;
		pt2 = end;
		isFE = isFiniteElement;
		assert(setID());
	}
	
	/**
	 * Creates an edge bounded by two <code>MNode1D</code> instances.
	 *
	 * @param  begin   first end point,
	 * @param  end     second end point,
	 * @param  ref     a master <code>MEdge1D</code> instance,
	 * @param  isFiniteElement  <code>true</code> if this edge is a 1D finite
	 * element.
	 */
	public MEdge1D(MNode1D begin, MNode1D end, MEdge1D ref, boolean isFiniteElement)
	{
		pt1 = begin;
		pt2 = end;
		master = ref;
		isFE = isFiniteElement;
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
	 * Returns the first <code>MNode1D</code> instance.
	 *
	 * @return the first <code>MNode1D</code> instance.
	 */
	public MNode1D getNodes1()
	{
		return pt1;
	}
	
	/**
	 * Returns the second <code>MNode1D</code> instance.
	 *
	 * @return the second <code>MNode1D</code> instance.
	 */
	public MNode1D getNodes2()
	{
		return pt2;
	}
	
	/**
	 * Sets the first <code>MNode1D</code> instance.
	 *
	 * @param pt  the new <code>MNode1D<code> instance for the first point.
	 * @return pt
	 */
	public MNode1D setNodes1(MNode1D pt)
	{
		pt1 = pt;
		return pt1;
	}
	
	/**
	 * Returns the master edge if it does exist, otherwise current edge.
	 * Return value is a valid object, it cannot be null.
	 *
	 * @return the master edge if it does exist, otherwise current edge.
	 */
	public MEdge1D getMaster()
	{
		if (null != master)
			return master;
		return this;
	}
	
	public String toString()
	{
		String r = "MEdge1D: id="+getID()+
			" n1="+pt1.getID()+" n2="+pt2.getID();
		if (null != master)
			r += "  master -> " + master;
		return r;
	}
	
}
