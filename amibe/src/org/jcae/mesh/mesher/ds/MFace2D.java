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
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;

/**
 * 2D triangle.
 */
public class MFace2D
{
	static private Logger logger=Logger.getLogger(MFace2D.class);
	
	/** List of nodes. */
	protected MNode2D[] nodelist=new MNode2D[3];
	
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
	public MFace2D(MNode2D n1, MNode2D n2, MNode2D n3)
	{
		nodelist[0]=n1;
		nodelist[1]=n2;
		nodelist[2]=n3;		
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
	 * Returns an iterator over the nodes of this triangle.
	 *
	 * @return an iterator over the nodes of this triangle.
	 */
	public Iterator getNodesIterator()
	{
		return new Iterator()
		{
			private int pos = -1;
			public boolean hasNext()
			{
				return (pos+1 < nodelist.length && null != nodelist[pos+1]);
			}
			
			public Object next()
			{
				pos++;
				if (pos >= nodelist.length || null == nodelist[pos])
					return new NoSuchElementException();
				return nodelist[pos];
			}
			public void remove()
			{
				//  Not needed
			}
		};
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
		return true;
	}
	
	public String toString()
	{		
		String r="MFace2D: id="+getID();
		for (int i=0;i<nodelist.length;i++)
			r+=" e"+(i+1)+"="+nodelist[i].getID();
		return r;
	}
	
}
