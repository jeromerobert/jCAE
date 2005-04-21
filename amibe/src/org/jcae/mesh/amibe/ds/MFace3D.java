/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005
                  Jerome Robert <jeromerobert@users.sourceforge.net>
 
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

package org.jcae.mesh.amibe.ds;

import java.util.Iterator;
import java.util.HashMap;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;

/**
 * 3D triangle.
 * The <code>MFace3D</code> class is a very simplistic container for triangles
 * in 3D space.  The only operation performed on 3D meshes is writing them into
 * a <code>UNV</code> file, so there is no need to worry with complicated data
 * structures.
 */
public class MFace3D
{
	static private Logger logger=Logger.getLogger(MFace3D.class);
	
	//  The list of nodes composing the triangle
	private MNode3D[] nodelist = new MNode3D[3];		
	
	//  ID used for debugging purpose
	private static int id = 0;
	private static HashMap mapHashcodeToID;
	//  Initialize mapHashcodeToID
	static { assert(null != (mapHashcodeToID = new HashMap())); };
	
	/**
	 * Creates a triangle composed of three <code>MNode3D</code> instances.
	 *
	 * @param n1  first node
	 * @param n2  second node
	 * @param n3  third node
	 */
	public MFace3D(MNode3D n1, MNode3D n2, MNode3D n3)
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
	 * In the list of nodes, replace a node by another one.  If
	 * <code>node</code> and <code>that</code> are the same object, this
	 * routine immediately returns.  If <code>node</code> is not in the
	 * node list, an exception is raised.
	 *
	 * @param node   the node to be substituted,
	 * @param that   the new node.
	 * @throws NoSuchElementException if <code>node</code> is not present in
	 * the node list.
	 */
	public void substNode(MNode3D node, MNode3D that)
	{
		if (node == that)
			return;
		for(int i=0;i<nodelist.length;i++)
		{
			MNode3D n = nodelist[i];
			if (n == node)
			{
				nodelist[i] = that;
				return;
			}
		}
		throw new NoSuchElementException("MNode3D : "+node);
	}
	
	public String toString()
	{		
		String r="MFace3D: id="+getID();
		for (int i=0;i<nodelist.length;i++)
			r+=" n"+(i+1)+"="+nodelist[i].getID();
		return r;
	}
	
}
