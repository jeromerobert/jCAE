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

package org.jcae.mesh.sd;

import java.util.Iterator;
import org.jcae.mesh.util.*;

/**
 * A class which describes a volume entity
 * @author Cyril BRANDY & Marie-Helene GARAT
 */
public class MeshVolume extends MeshElement
{
	/** The face list. */
	private HashSet facelist=new HashSet();
	
	/** Constructor */
	public MeshVolume(HashSet facelist)
	{
		this.facelist = facelist;
	}
	
	/** Copy constructor */
	public MeshVolume(MeshVolume v)
	{
		this.facelist = new HashSet(v.facelist);
	}
	
	/** Test element equality */
	public boolean isSameElement(MeshElement elem)
	{
		if (elem.getType() != getType()) return false;
		MeshVolume v = (MeshVolume)elem;
		return this.equals(v);
	}
	
	/** Link the volume */
	public void link(MeshElement e)
	{
		Iterator it = facelist.iterator();
		while (it.hasNext())
		{
			MeshFace f = (MeshFace)it.next();
			f.link(e);
		}
	}
	
	/** Unlink the volume */
	public void unlink(MeshElement e)
	{
		Iterator it = facelist.iterator();
		while (it.hasNext())
		{
			MeshFace f = (MeshFace)it.next();
			f.unlink(e);
		}
	}
	
	/** Test if unused */
	public boolean canDestroy()
	{
		Iterator it = facelist.iterator();
		while (it.hasNext())
		{
			MeshFace f = (MeshFace)it.next();
			if (!f.canDestroy()) return false;
		}
		return true;
	}
	
	public boolean equals(Object o)
	{
		MeshVolume v = (MeshVolume)o;
		return v.getFaces().equals(facelist);
	}
	
	/** Gets the faces list */
	public HashSet getFaces()
	{
		return facelist;
	}
	
	/** Add a face to the volume */
	public MeshFace addFace(MeshFace face)
	{
		return(MeshFace)(facelist.addIfNotPresent(face));
	}
	
	public HashSet getElements()
	{
		Iterator it = getNodesIterator();
		HashSet toreturn = new HashSet();
		while (it.hasNext())
		{
			MeshNode n = (MeshNode)it.next();
			toreturn.addAll(n.getElements());
		}
		return toreturn;
	}
	
	public HashSet getTopologicContour(int topolen, int elemntype)
	{
		return new HashSet();
	}
	
	public int getType()
	{
		return MeshElement.VOLUME;
	}		
	
	public Iterator getEdgesIterator()
	{
		/** @TODO */
		throw new UnsupportedOperationException();
	}
	
	public Iterator getFacesIterator()
	{
		return facelist.iterator();
	}
	
	public Iterator getNodesIterator()
	{
		/** @TODO */
		throw new UnsupportedOperationException();
	}
}
