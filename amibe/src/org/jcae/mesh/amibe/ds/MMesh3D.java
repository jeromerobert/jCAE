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

package org.jcae.mesh.amibe.ds;

import java.util.ArrayList;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 * 3D discretization of the whole shape.
 * This class contains the 3D mesh of the whole shape.
 */

public class MMesh3D extends Mesh
{
	private static Logger logger=Logger.getLogger(MMesh3D.class);	

	//  Group list.
	private ArrayList grouplist = new ArrayList();
	
	//  Node list.
	private ArrayList nodelist = new ArrayList();
	
	/**
	 * Returns an iterator over the set of nodes.
	 *
	 * @return an iterator over the set of nodes.
	 */
	public Iterator getNodesIterator()
	{
		return nodelist.iterator();
	}
	
	/**
	 * Returns the number of nodes.
	 *
	 * @return the number of nodes.
	 */
	public int getNumberOfNodes()
	{
		return nodelist.size();
	}
	
	/**
	 * Returns an iterator over the set of faces.
	 *
	 * @return an iterator over the set of faces.
	 */
	public Iterator getFacesIterator()
	{
		return triangleList.iterator();
	}
	
	/**
	 * Returns the number of faces.
	 *
	 * @return the number of faces.
	 */
	public int getNumberOfFaces()
	{
		return triangleList.size();
	}
	
	/**
	 * Returns an iterator over the set of groups.
	 *
	 * @return an iterator over the set of groups.
	 */
	public Iterator getGroupsIterator()
	{
		return grouplist.iterator();
	}
	
	/**
	 * Returns the number of groups.
	 *
	 * @return the number of groups.
	 */
	public int getNumberOfGroups()
	{
		return grouplist.size();
	}
	
	public void addGroup(MGroup3D g)
	{
		grouplist.add(g);
	}
	
	public void addFace(Triangle f)
	{
		triangleList.add(f);
	}
	
	public void addNode(Vertex n)
	{
		nodelist.add(n);
	}
	
	public void removeNode(Vertex n)
	{
		nodelist.remove(n);
	}	
	
	public String toString()
	{
		String cr=System.getProperty("line.separator");
		String r="MMesh3D";
		if (logger.isDebugEnabled())
			logger.debug("Printing "+r+"...");
		r+=cr;
		for(Iterator it=nodelist.iterator();it.hasNext();)
		{
			Vertex node=(Vertex)it.next();
			r+=node+cr;
		}
		for(Iterator it=triangleList.iterator();it.hasNext();)
		{
			Triangle face=(Triangle)it.next();
			r+=face+cr;
		}
		logger.debug("...done");
		return r;
	}
}
