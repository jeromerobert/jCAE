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

import org.jcae.mesh.util.*;
import org.jcae.opencascade.jni.*;
import java.util.Iterator;
import org.apache.log4j.Logger;

/** A class inherited from MeshElement class to describe an edge defined by its
 * 2 bounding nodes. A MeshEdge entity is bounded by two node entities and it is
 * used either to define a 1D entity into the F.E. mesh or either to bound face
 * entities.
 * @author Cyril BRANDY & Marie-Helene GARAT.
 */
public class MeshEdge extends MeshElement
{
	private static Logger logger=Logger.getLogger(MeshEdge.class);
	/** node that describe the first edge end. */
	protected MeshNode pt1;
	/** node that describe of the second edge end. */
	protected MeshNode pt2;
	/** The edge middle node. */
	protected MeshNode mid;
	/** A boolean flag to test if the edge is wire or not. */
	protected boolean isWire;
	/** A boolean flag to test if the edge is frozen or not. */
	protected boolean isFrozen;
	/** The hascode */
	private int hashcode;
	
	
	/** Constructor with both bounding nodes.
	 * @param pt1 : a MeshNode instance, one of the two bounding node of the edge.
	 * @param pt2 : a MeshNode instance, the other bounding node of the edge.
	 */
	public MeshEdge(MeshNode pt1, MeshNode pt2)
	{
		this.pt1 = pt1;
		this.pt2 = pt2;
		mid = null;
		isWire = false;
		isFrozen = false;
		setID(super.hashCode());
		hashcode = pt1.hashCode()+pt2.hashCode();
	}
	
	/** Constructor with both bounding nodes and the middle node.
	 * @param pt1 : a MeshNode instance, one of the two bounding node of the edge.
	 * @param pt2 : a MeshNode instance, the other bounding node of the edge.
	 * @param mid : a MeshNode instance, the edge middle node.
	 */
	public MeshEdge(MeshNode pt1, MeshNode pt2, MeshNode mid)
	{
		this(pt1, pt2);
		this.mid = mid;
	}
	
	/** Copy constructor */
	public MeshEdge(MeshEdge e)
	{
		this.pt1 = e.pt1;
		this.pt2 = e.pt2;
		this.mid = e.mid;
		this.isFrozen = e.isFrozen;
		this.isWire = e.isWire;
		this.hashcode = e.hashcode;
	}
	
	/**
	 * Returns the hashcode used to optimise the HashSet access.
	 */
	public int hashCode()
	{
		return hashcode;
	}
	
	/**
	 * Link the MeshElement entity to the current edge.
	 * @param e : a MeshElement instance, the element to link to the edge.
	 */
	public void link(MeshElement e)
	{
		pt1.link(e);
		pt2.link(e);
	}
	
	/**
	 * Unlink the MeshElement entity to the current edge.
	 * @param e : a MeshElement instance, the element to unlink to the edge.
	 */
	public void unlink(MeshElement e)
	{
		pt1.unlink(e);
		pt2.unlink(e);
	}
	
	/**
	 * Returns one of the two MeshNode entities bounding the under focus edge.
	 * @return MeshNode : the first MeshNode entitiy bounding the under focus edge.
	 */
	public MeshNode getNodes1()
	{
		return pt1;
	}
	
	/**
	 * Returns the other MeshNode entitiy bounding the under focus edge.
	 * @return MeshNode : the second MeshNode entitiy bounding the under focus edge.
	 */
	public MeshNode getNodes2()
	{
		return pt2;
	}
	
	/**
	 * Set the first MeshNode entitiy bounding the under focus edge.
	 * @param pt1 : a MeshNode instance, one MeshNode entitiy bounding the under focus edge.
	 */
	public void setNodes1(MeshNode pt1)
	{
		hashcode += pt1.hashCode()-this.pt1.hashCode();
		this.pt1=pt1;
	}
	
	/**
	 * Set the second MeshNode entitiy bounding the under focus edge.
	 * @param pt2 : a MeshNode instance, second MeshNode entitiy bounding the under focus edge.
	 */
	public void setNodes2(MeshNode pt2)
	{
		hashcode += pt2.hashCode()-this.pt2.hashCode();
		this.pt2=pt2;
	}
	
	/**
	 * Compares 2 MeshElement entities.
	 * @param elem : a MeshElement instance, the element entity to compare xith the current edge entity.
	 * @return boolean : set to \c true if the element entity is same as the edge entity, \c false if not.
	 */
	public boolean isSameElement(MeshElement elem)
	{
		if (elem.getType() != getType()) return false;
		return (this.equals(elem));
	}
	
	/**
	 * Get both nodes defining this edge entity
	 * @return HashSet : the both node entities used to define the current edge.
	 */
	public HashSet getNodes()
	{
		HashSet toreturn = new HashSet();
		toreturn.add(pt1);
		toreturn.add(pt2);
		return toreturn;
	}
	
	/**
	 * Get the edges used by this edge entity (not used in this class).
	 * @return HashSet : an empty list in this case.
	 */
	public HashSet getEdges()
	{
		return null;
	}
	
	/**
	 * Get the faces witch are bounding by this edge entity.
	 * @return HashSet : the list of all the faces witch are bounding by the current edge.
	 */
	public HashSet getFaces()
	{
		HashSet faceList=new HashSet();
		Iterator it=pt1.getFaces().iterator();
		while(it.hasNext())
		{
			MeshFace f=(MeshFace)it.next();
			if ( f.numberOfEdges() != 3)
			{
				logger.warn("face a -3 edges");
			}
			if (pt2.getFaces().contains(f) )
				faceList.add(f);
		}
		return faceList;
	}
	
	/**
	 * Get the elements witch are bounding by this edge entity.
	 * @return HashSet : the list of all the elements witch are bounding by the current edge.
	 */
	public HashSet getElements()
	{
		HashSet elemlist1 = new HashSet(pt1.getElements());
		elemlist1.retainAll(pt2.getElements());
		return elemlist1;
	}
	
	/**
	 * Set the middle node.
	 * @param mid : a MeshNode instance, the middle mode of the current edge.
	 */
	public void setMidNode(MeshNode mid)
	{
		this.mid = mid;
	}
	
	/**
	 * Get the middle node of the edge.
	 * @return MeshNode : the middle mode of the current edge.
	 */
	public MeshNode getMidNode()
	{
		return mid;
	}
	
	/**
	 * Test if the edge can be delete.
	 * @return boolean : set to \c true if the edge is unused, so it can be deleted. Set to \c false if not.
	 */
	public boolean canDestroy()
	{
		if (isWire || isFrozen)
			return false;
		Iterator it=pt1.getElements().iterator();
		while(it.hasNext())
		{
			MeshElement f=(MeshElement)it.next();
			if (pt2.getElements().contains(f))
				return false;
		}
		return true;
	}
	
	/**
	 * Equal operator.
	 *
	 * Comparison of edges upon natural coordinates of their bounding nodes.
	 *
	 * @param o : an Object instance, the edge to compare with the current edge
	 * @return boolean : set to \c true if the 2 edges are the same
	 * @see MeshNode#equals
	 */
	public boolean equals(Object o)
	{
		if (o instanceof MeshEdge)
		{
			MeshEdge e = (MeshEdge)o;
			if (pt1.equals(e.pt1) && pt2.equals(e.pt2))	return true;
			if (pt2.equals(e.pt1) && pt1.equals(e.pt2)) return true;
		}
		return false;
	}
	
	/**
	 * Set the isWire flag.
	 * @param param : a boolean value, the flag value.
	 */
	public void setWire(boolean param)
	{
		isWire = param;
	}
	
	/**
	 * Returns the state of the isWire flag.
	 * @return boolean : \c true if the edge is a wire, \c false if not.
	 */
	public boolean isWire()
	{
		return isWire;
	}
	
	/**
	 * Set the isFrozen flag.
	 * @param frozen : a boolean value, the flag value.
	 */
	public void setFrozen(boolean frozen)
	{
		isFrozen = frozen;
	}
	
	/**
	 * Returns the state of the isFrozen flag.
	 * @return boolean : \c true if the edge is frozen (impossible to swap), \c false if not.
	 */
	public boolean isFrozen()
	{
		return isFrozen;
	}
	
	/**
	 * Print on screen.
	 */
	public String toString()
	{
		return "id="+getID()+" n1="+pt1.getID() + " n2=" + pt2.getID();
	}
	
	public int getType()
	{
		return MeshElement.EDGE;
	}
	
	/**
	 * Returns the length of the edge
	 * @return double : edge length
	 */
	
	public double length()
	{
		return pt1.distance(pt2);
	}
	
	/**
	 * Check whether edge contraction preserves topology.
	 * @return boolean : set to \c true if the edge can be contracted
	 */
	public boolean canContract()
	{
		HashSet faces = new HashSet(getFaces());
		
		if (faces.size() != 2) return false;
		
		Iterator it = faces.iterator();
		MeshNode apex1 = ((MeshFace)it.next()).apex(this);
		MeshNode apex2 = ((MeshFace)it.next()).apex(this);
		
		HashSet nodes1 = (HashSet) pt1.getTopologicContour(1, MeshElement.NODE);
		HashSet nodes2 = (HashSet) pt2.getTopologicContour(1, MeshElement.NODE);
		nodes1.retainAll(nodes2);
		nodes1.remove(apex1);
		nodes1.remove(apex2);
		return (nodes1.size() == 0);
	}
	
	/**
	 * Find an edge having same ends 3D coordinates
	 * It is not the same method as equals, here we compare 3D coordinates and not PST_Position.
	 * @param edge - the edge to compare with current edge
	 * @return a boolean value - set to true if edge has the same end nodes (in 3D coordinates) as current edge.
	 */
	public boolean hasSameEndsCoords(MeshEdge edge)
	{
		MeshNode n1 = edge.getNodes1();
		MeshNode n2 = edge.getNodes2();
		if ( (pt1.getX()==n1.getX()) && (pt1.getY()==n1.getY()) && (pt1.getZ()==n1.getZ()) && (pt2.getX()==n2.getX()) && (pt2.getY()==n2.getY()) && (pt2.getZ()==n2.getZ()) )
			return true;
		if ( (pt1.getX()==n2.getX()) && (pt1.getY()==n2.getY()) && (pt1.getZ()==n2.getZ()) && (pt2.getX()==n1.getX()) && (pt2.getY()==n1.getY()) && (pt2.getZ()==n1.getZ()) )
			return true;
		
		return false;
	}
	
	public Iterator getEdgesIterator()
	{
		return new SingletonIterator(this);
	}
	
	public Iterator getFacesIterator()
	{
		/* @TODO */
		throw new UnsupportedOperationException();
	}
	
	public Iterator getNodesIterator()
	{
		return new Iterator()
		{
			int state=0;
			public boolean hasNext()
			{
				return state<2;
			}
			
			public Object next()
			{
				switch(state)
				{
					case 0: state++; return pt1;
					case 1: state++; return pt2;
					default: throw new IndexOutOfBoundsException();
				}
			}
			
			public void remove()
			{
				throw new UnsupportedOperationException();
			}
		};
	}	
}
