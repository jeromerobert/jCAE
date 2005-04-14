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

package org.jcae.mesh.amibe.ds;

import java.util.ArrayList;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 * 3D discretization of the whole shape.
 * This class contains the 3D mesh of the whole shape.  Here is the typical
 * lifecycle of these instances:
 * <ol>
 *   <li>A geometry file is read and a 1D mesh is generated.</li>
 *   <li>A <code>MMesh3D</code> instance is created.</li>
 *   <li>For all topological surfaces of this shape, computes its 2D meshing
 *       (the 1D mesh is identical for all surfaces) and imports all meshes
 *       into the 3D space by calling {@link #addSubMesh2D}.</li>
 *   <li>Write the 3D mesh onto disk and deletes this instance.</li>
 * </ol>
 */

public class MMesh3D
{
	private static Logger logger=Logger.getLogger(MMesh3D.class);	

	//  Group list.
	private ArrayList grouplist = new ArrayList();
	
	//  Face list.
	private ArrayList facelist = new ArrayList();
	
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
		return facelist.iterator();
	}
	
	/**
	 * Returns the number of faces.
	 *
	 * @return the number of faces.
	 */
	public int getNumberOfFaces()
	{
		return facelist.size();
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
	
	public void addFace(MFace3D f)
	{
		facelist.add(f);
	}
	
	public void addNode(MNode3D n)
	{
		nodelist.add(n);
	}
	
	public void removeNode(MNode3D n)
	{
		nodelist.remove(n);
	}	
	
	public String toString()
	{
		String cr=System.getProperty("line.separator");
		String r="MMesh3D";
		logger.debug("Printing "+r+"...");
		r+=cr;
		for(Iterator it=nodelist.iterator();it.hasNext();)
		{
			MNode3D node=(MNode3D)it.next();
			r+=node+cr;
		}
		for(Iterator it=facelist.iterator();it.hasNext();)
		{
			MFace3D face=(MFace3D)it.next();
			r+=face+cr;
		}
		logger.debug("...done");
		return r;
	}
}
