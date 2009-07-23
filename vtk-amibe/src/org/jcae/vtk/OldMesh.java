/*
 * Project Info:  http://jcae.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2008, by EADS France
 */
package org.jcae.vtk;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Represent a mesh
 * @author Julian Ibarz
 * @deprecated kept to maintain compatibility with old meshes. @see Mesh to use with Bora
 */
public class OldMesh
{
	private final HashMap<Integer, LeafNode.DataProvider> groups;
	
	int getNbOfGroups()
	{
		return groups.size();
	}

	OldMesh()
	{
		this(0);
	}
	
	OldMesh(int nbOfGroups)
	{
		groups = new HashMap<Integer, LeafNode.DataProvider>(nbOfGroups);
	}

	/**
	 * Construct a submesh of a mesh with the groups given in the tab
	 */
	OldMesh(OldMesh mesh, int[] extractedGroups)
	{
		groups = new HashMap<Integer, LeafNode.DataProvider>(extractedGroups.length);
		
		for(int id : extractedGroups)
		{
			groups.put(id, mesh.getGroup(id));
		}
	}

	LeafNode.DataProvider getGroup(int id)
	{
		return groups.get(id);
	}
	
	void setGroup(int id, LeafNode.DataProvider group)
	{
		groups.put(id, group);
	}
	
	Collection<LeafNode.DataProvider> getGroups()
	{
		return groups.values();
	}
	
	Set<Entry<Integer, LeafNode.DataProvider>> getGroupSet()
	{
		return groups.entrySet();
	}
}
