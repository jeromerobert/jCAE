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


/** An abstract class to define the basic components of a mesh.
 *
 * \n This class entity defines all the basic components of a mesh.
 * \n MeshElement entities could be a node, an edge or a volume entities
 * \n
 * @author Cyril BRANDY & Marie-Helene GARAT.
 */
public abstract class MeshElement
{
	public static final int INDEFINI = 0; /**< type value INDEFINI. */
	public static final int NODE = 1; /**< type value NODE. */
	public static final int EDGE = 2; /**< type value EDGE. */
	public static final int FACE = 3; /**< type value FACE. */
	public static final int VOLUME = 4; /**< type value VOLUME. */
	
	private int id;	
	
	public int getID()
	{
		return id;
	}
	
	public void setID(int id)
	{
		this.id=id;
	}
	
	/** Returns the element type
	 * @return int : the type entity.
	 */
	public int getType()
	{
		return INDEFINI;
	}
	
	/** Test type equality.
	 * @param elem : a MeshElement instance, the entity to compare with the current entity
	 * @return boolean : set to \c true if both entities have same type, \c false if not.
	 */
	public boolean isSameType(MeshElement elem)
	{
		return getType() == elem.getType();
	}
	
	/**
	 * Abstract method to test element equality.
	 * @param elem : a MeshElement instance, the MeshElement entity to compare with the current one.
	 * @return boolean : set to \c true if both entities are same, \c false if not.
	 */
	public abstract boolean isSameElement(MeshElement elem);
	
	/**
	 * Abstract method to test if the entity is unused.
	 * @return boolean : set to \c true if the entity is unused so it can be deleted, \c false if not.
	 */
	public abstract boolean canDestroy();
	
	/**
	 * Abstract method to get the nodes linked with the element.
	 * @return HashSet : the list of nodes linked with this current entity
	 */
	public abstract Iterator getNodesIterator();
	
	/**
	 * Abstract method to get the edges linked with the element.
	 * @return HashSet : the list of edges linked with this current entity
	 */
	public abstract Iterator getEdgesIterator();
	
	/**
	 * Abstract method to get the faces linked with the element.
	 * @return HashSet : the list of faces linked with this current entity
	 */
	public abstract Iterator getFacesIterator();
	
	public int numberOfEdges()
	{
		Iterator it=getEdgesIterator();
		int n=0;
		while(it.hasNext())
		{
			n++;
			it.next();
		}
		return n;
	}
	
	public int numberOfFaces()
	{
		Iterator it=getFacesIterator();
		int n=0;
		while(it.hasNext())
		{
			n++;
			it.next();
		}
		return n;
	}
	
	public int numberOfNodes()
	{
		Iterator it=getNodesIterator();
		int n=0;
		while(it.hasNext())
		{
			n++;
			it.next();
		}
		return n;
	}
	
	/** Links the element to the face.
	 * @param e : a MeshElement instance, the element to add to the edges list.
	 */
	public void link(MeshElement e)
	{
		Iterator it=getNodesIterator();
		while(it.hasNext())
		{
			MeshNode n=(MeshNode)it.next();
			n.link(e);
		}		
	}
	
	/** Unlinks the element to the face.
	 * @param e : a MeshElement instance, the element to unlink to the face.
	 */
	public void unlink(MeshElement e)
	{
		Iterator it=getNodesIterator();
		while(it.hasNext())
		{
			MeshNode n=(MeshNode)it.next();
			n.unlink(e);
		}		
	}	
	
	public void setFinitElement(boolean b)
	{
		if(true) link(this);
		else unlink(this);
	}	
	
	/**
	 * Method used to build a list of entities having a specific type (define in input argument) which admit a topological
	 * distance (topological length) from the under focus MeshNode entity.
	 */
	public HashSet getTopologicContour(int topolen, int elemtype)
	{
		/*HashSet toreturn = new HashSet();
		int nb = 0;
		HashSet faces = new HashSet();
		HashSet edges = new HashSet();
		HashSet nodes = new HashSet();

		// topolen = 0
		if (getType() == NODE)
			nodes.add(this);
		else
			nodes.addAll(getNodes());
		Iterator it = nodes.iterator();
		while(it.hasNext())
		{
			MeshNode n = (MeshNode) it.next();
			faces.addAll(n.getFaces());
			edges.addAll(n.getEdges());
		}
		
		//  In the loop below we collect all entities closer than
		//    topolen.  The final set of entities is obtained by
		//    removing the previous list.
		HashSet prevFaces = new HashSet();
		HashSet prevEdges = new HashSet();
		HashSet prevNodes = new HashSet();
		HashSet newFaces = new HashSet(faces);
		HashSet newEdges = new HashSet(edges);
		HashSet newNodes = new HashSet(nodes);

		if (elemtype == EDGE)
			nb++;

		while(nb < topolen)
		{
			nb++;
			prevFaces.clear();
			prevFaces.addAll(faces);
			prevEdges.clear();
			prevEdges.addAll(edges);
			prevNodes.clear();
			prevNodes.addAll(nodes);
			
			// Update newEdges and newNodes; they contain new entities, and some older too, but quite few
			newEdges.clear();
			newNodes.clear();
			it = newFaces.iterator();
			while(it.hasNext())
			{
				MeshFace f = (MeshFace)it.next();
				newNodes.addAll(f.getNodes());
				newEdges.addAll(f.getEdges());
			}
			// Update newFaces; this one only contains new faces
			it = newEdges.iterator();
			while(it.hasNext())
			{
				MeshEdge e = (MeshEdge)it.next();
				Iterator itf = e.getFaces().iterator();
				while(itf.hasNext())
				{
					MeshFace f = (MeshFace)itf.next();
					if (!faces.contains(f))
						newFaces.add(f);
				}
			}
			faces.addAll(newFaces);
			edges.addAll(newEdges);
			nodes.addAll(newNodes);
		}

		switch (elemtype)
		{
			case NODE :
				it = newNodes.iterator();
				while(it.hasNext())
				{
					MeshNode n = (MeshNode)it.next();
					if (!prevNodes.contains(n))
						toreturn.add(n);
				}
				break;
			case EDGE :
				Iterator itf = faces.iterator();
				while(itf.hasNext())
				{
					MeshFace f = (MeshFace) itf.next();
					it = f.getEdges().iterator();
					while(it.hasNext())
					{
						MeshEdge e = (MeshEdge)it.next();
						if (!edges.contains(e))
							toreturn.add(e);
					}
				}
				break;
			case FACE :
				toreturn.addAll(newFaces);
				break;
			case VOLUME :
				toreturn = null;
				break;
			default :
				toreturn = null;
				break;
		}
		
		return toreturn;*/
		return null;
	}
	
}
