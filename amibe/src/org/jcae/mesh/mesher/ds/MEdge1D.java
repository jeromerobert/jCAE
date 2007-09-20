/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005, by EADS CRC
 
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
import gnu.trove.TObjectIntHashMap;

/**
 * 1D edge.
 */

public class MEdge1D
{
	//  First end point
	private MNode1D pt1;
	
	//  Second end point
	private MNode1D pt2;
	
	private static int id = 0;
	private static TObjectIntHashMap<MEdge1D> mapHashcodeToID;
	//  Initialize mapHashcodeToID
	static { assert(null != (mapHashcodeToID = new TObjectIntHashMap<MEdge1D>())); }
	
	/**
	 * Creates an edge bounded by two <code>MNode1D</code> instances.
	 *
	 * @param  begin   first end point,
	 * @param  end     second end point,
	 */
	public MEdge1D(MNode1D begin, MNode1D end)
	{
		pt1 = begin;
		pt2 = end;
		assert(setID());
	}
	
	private boolean setID()
	{
		id++;
		mapHashcodeToID.put(this, id);
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
			return mapHashcodeToID.get(this);
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
	
	@Override
	public String toString()
	{
		String r = "MEdge1D: id="+getID()+
			" n1="+pt1.getID()+" n2="+pt2.getID();
		return r;
	}
	
}
