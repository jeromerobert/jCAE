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

package org.jcae.mesh.algos;

import java.util.Iterator;
import org.jcae.mesh.sd.*;
import org.jcae.mesh.util.*;

/**
 * This class inherites from MeshFace and describes triangular faces entities.
 * @author Cyril BRANDY & Marie-Helene GARAT
 */
public class Triangle2D extends MeshFace
{
	/** Flag to descride if the face is an external or an internal face of the geometric surface.
	 * value set to \n -1 if the face state is unknown,
	 * \n			  0 if the face is part of the geometric surface
	 * \n			  1 if the face is outside the geometric surface
	 */	
	protected int flagExt=-1;
	
	/** A list of inner nodes contained in the triangle2D entity. */
	protected HashSet nodes;
	
	/** Default constructor */
	/*public Triangle2D()
	{
		this.nodes=new HashSet();
		flagExt = -1;
	}*/
	
	/** Constructor from a MeshFace entity.
	 * @param face : a MeshFace instance, the MeshFace entity
	 */
	/*public Triangle2D(MeshFace face)
	{
		super(face);
	}*/
	
	/** Copy constructor */
	/*public Triangle2D(Triangle2D t)
	{
		super((MeshFace)t);
		this.nodes = new HashSet(t.nodes);
		flagExt = t.getFlag();
	}*/
	
	/** Create a Triangle */
	Triangle2D(MeshEdge e1, MeshEdge e2, MeshEdge e3)
	{
		super(e1, e2, e3);
		nodes=new HashSet();
	}
	
	/** Fills the set of inner nodes.
	 * @param nodelist : a HashSet of inner nodes to add into the node list.
	 */
	public void setNodes(HashSet nodelist)
	{
		nodes.clear();
		nodes.addAll(nodelist);
	}
	
	/** Get the list of inner nodes contained into the trianglar face.
	 * @return HashSet : the list of inner nodes.
	 */
	public HashSet getNodelist()
	{
		return nodes;
	}
	
	public void addNodelist(HashSet set)
	{
		nodes.addAll(set);
	}
	
	public void addNodelist(MeshNode2D pt)
	{
		nodes.add(pt);
	}
	
	/** Get the number of inner nodes in the list.
	 * @return int : the number of inner nodes.
	 */
	public int getNbNodes()
	{
		return nodes.size();
	}
	
	/** Tests if the node is into the triangle.
	 * @param pt : a MeshNode2D instance, the point to test
	 * @return boolean : set to \c true if the input node is contained into the triangle.
	 */
	public boolean ptInTriangle(MeshNode2D pt)
	{
		Iterator node_it = getNodesIterator();
		MeshNode2D P1 = (MeshNode2D)node_it.next();
		MeshNode2D P2 = (MeshNode2D)node_it.next();
		MeshNode2D P3 = (MeshNode2D)node_it.next();
		if (pt.orient2D(P1, P2) >= 0.0 && pt.orient2D(P2, P3) >= 0.0 && pt.orient2D(P3, P1) >= 0.0) return true;
		else if (pt.orient2D(P1, P2) <= 0.0 && pt.orient2D(P2, P3) <= 0.0 && pt.orient2D(P3, P1) <= 0.0) return true;
		else
			return false;
	}
	
	/** Fills the set of nodes, giving those of a bigger triangle.
	 * @param triangle0 : a Triangle2D instance as the bigger triangle
	 */
	public void fillTriangle2D(Triangle2D triangle0)
	{
		
		HashSet nodelist0 = triangle0.getNodelist();
		nodes.clear();
		Iterator it = nodelist0.iterator();
		while (it.hasNext())
		{
			MeshNode2D pt = (MeshNode2D)it.next();
			if (ptInTriangle(pt))
			{
				nodes.add(pt);
			}
		}
		nodelist0.removeAll(nodes);
	}
	
	/** Updates the list of inner nodes after a transformation of the triangle.
	 * @param list : a HashSet of inner nodes
	 */
	public void updateNodes(HashSet list)
	{
		nodes.clear();
		Iterator it=list.iterator();
		while (it.hasNext())
		{
			MeshNode2D n=(MeshNode2D)it.next();
			if (ptInTriangle(n)) nodes.add(n);
		}
	}

	/** Set the flag value.
	 * @param flag : an integer value, the flag value (face state).
	 */
	public void setFlag(int flag)
	{
		flagExt = flag;
	}
	
	/** Get the flag value.
	 * @return int : the flag value.
	 */
	public int getFlag()
	{
		return flagExt;
	}	
	
	public String toString()
	{
		String r=super.toString()+" "+flagExt+System.getProperty("line.separator");
		Iterator it=nodes.iterator();
		while(it.hasNext()) r+="\t"+it.next()+System.getProperty("line.separator");
		return r;
	}
}
