/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005, by EADS CRC
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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.amibe.ds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;


/**
 * Group of triangles in the 3D mesh.
 */
public class MGroup3D
{
	//  Group name
	private final String name;
	
	//  Group id
	private final int id;
	
	//  Face list.
	private Collection<Triangle> facelist = new ArrayList<Triangle>();
	
	/**
	 * Creates a group.
	 *
	 * @param i  group number.
	 * @param n  group name.
	 * @param faces  set of faces.
	 */
	public MGroup3D(int i, String n, Collection<Triangle> faces)
	{
		id = i;
		name = n;
		facelist = new ArrayList<Triangle>(faces);
	}
	
	/**
	 * Returns the group name.
	 *
	 * @return the group name.
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 * Returns the group id.
	 *
	 * @return the group id.
	 */
	public int getId()
	{
		return id;
	}
	
	/**
	 * Returns an iterator over the set of faces.
	 *
	 * @return an iterator over the set of faces.
	 */
	public Iterator<Triangle> getFacesIterator()
	{
		return facelist.iterator();
	}
	
	/**
	 * Returns the number of faces.
	 *
	 * @return the number of faces.
	 */
	public int numberOfFaces()
	{
		return facelist.size();
	}
	
	/**
	 * Merges a group to this one
	 *
	 * @return the merged group.
	 */
	public MGroup3D merge(MGroup3D that)
	{
		facelist.addAll(that.facelist);
		that.facelist.clear();
		return this;
	}
	
}
