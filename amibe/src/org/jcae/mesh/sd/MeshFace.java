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
import org.apache.log4j.Logger;
import java.util.Collection;

/** A class inherited from MeshElement class to describe an face defined by its 
 * bounding edges. A MeshFace entity is bounded by edge entities. Face entities
 * are used either to define a 2 dimensional entity into F.E. mesh or either to
 * bound volume entities.
 * @author Cyril BRANDY & Marie-Helene GARAT.
 */
public class MeshFace extends MeshElement
{
	static private Logger logger=Logger.getLogger(MeshFace.class);
	
	/** The list of edges bounding the face. */
	private MeshEdge[] edgelist=new MeshEdge[3];
	
	/** Create a Triangle */
	protected MeshFace(MeshEdge e1, MeshEdge e2, MeshEdge e3)
	{
		edgelist=new MeshEdge[3];		
		edgelist[0]=e1;
		edgelist[1]=e2;
		edgelist[2]=e3;		
	}
	
	MeshFace()
	{
	}
	
	/** Constructor with the edges bounding the face. */
	MeshFace(HashSet list)
	{
		int i=0;
		edgelist=new MeshEdge[list.size()];
		Iterator it=list.iterator();
		while(it.hasNext())
		{
			edgelist[i]=(MeshEdge)it.next();
			i++;
		}		
	}
	
	/** Copy constructor */
	MeshFace(MeshFace f)
	{
		edgelist=(MeshEdge[])f.edgelist.clone();
	}
	
	/** Returns the HashCode.
	 * @return int : the hashcode. 		A AMELIORER TODO !!!!!!!
	 */
	public int hashCode()
	{
		int r=0;
		for(int i=0;i<edgelist.length;i++) r+=edgelist[i].hashCode();
		return r;
	}
	
	/** Test face equality.
	 *
	 * @param elem : a MeshElement instance, the element to compare with the current face
	 * @return boolean : set to \c true if the node is same as the element
	 **/
	public boolean isSameElement(MeshElement elem)
	{
		if (elem.getType() != getType()) return false;
		return (getNodes().containsAll(((MeshFace)elem).getNodes()));
	}
	
	/** Adds an edge to the list of edges bounding the current face.
	 * @param edge : a MeshEdge instance, the edge to add to the list.
	 * @return MeshEdge : the edge added if the list does not contain it or the edge already contained in the list.
	 */
	MeshEdge addEdge(MeshEdge edge)
	{
		for(int i=0;i<edgelist.length;i++)
			if(edgelist[i].equals(edge)) return edgelist[i];

		MeshEdge[] na=new MeshEdge[edgelist.length+1];
		System.arraycopy(edgelist, 0, na, 0, edgelist.length);
		na[edgelist.length]=edge;
		edgelist=na;		
		return edge;
	}
	
	void addAllEdges(Collection edges)
	{
		Iterator it=edges.iterator();
		while(it.hasNext())
		{
			MeshEdge e=(MeshEdge)it.next();
			addEdge(e);
		}
	}
	
	/** Test if the face is unused.
	 * @return boolean : set to \c true if the face is unused so it can be deleted.
	 */
	public boolean canDestroy()
	{
		Iterator it = getNodes().iterator();
		HashSet result = new HashSet();
		
		while (it.hasNext())
		{
			MeshNode n = (MeshNode)it.next();
			result.retainAll(n.getElements());
			if (result.size() == 0) return true;
		}
		if (result.size() == 0) return true;
		return false;
	}
	
	/** Get the nodes used by this face.
	 * @return HasSet : the list of nodes in relation to the edges boundins the face.
	 */
	private HashSet getNodes()
	{			
		HashSet toReturn=new HashSet();
		for(int i=0;i<edgelist.length;i++)
		{
			MeshEdge e = edgelist[i];
			toReturn.add(e.getNodes1());
			toReturn.add(e.getNodes2());
		}		
		
		return toReturn;
	}
	
	/** Get the list of the edges bounding the face. Modifing this list will
	 * not modify the MeshFace. Use AddEdge and AddAllEdge to do so.
	 * @return HashSet : the list of the edges bounding the current face.
	 */
	public Iterator getEdgesIterator()
	{
		return java.util.Arrays.asList(edgelist).iterator();
	}
	
	/** Method unused in this class. */
	public HashSet getFaces()
	{
		return null;
	}
	
	/**
	 * Get the list of the elements which are bounded by the current face.
	 * @return HashSet : the list of MeshElement entities that bound the current face.
	 */
	public HashSet getElements()
	{
		HashSet toreturn = new HashSet();
		for(int i=0;i<edgelist.length;i++)
			toreturn.addAll(edgelist[i].getElements());

		return toreturn;
	}
	
	/** Get the number of edges bounding the under focus face.
	 * @return int : the number of edges bounding the current face.
	 */
	public int nbEdges()
	{
		return edgelist.length;
	}
	
	/** Comparaison of faces.
	 *
	 * @param o : an Object instance, the face to compare with the current face
	 * @return boolean : set to \c true if the 2 faces are the same
	 **/
	public boolean equals(Object o)
	{
		if(o instanceof MeshFace)
		{			
			MeshFace f = (MeshFace)o;
			if(f.edgelist.length!=edgelist.length) return false;
			for(int i=0;i<edgelist.length;i++)
			{
				int j;
				for(j=0;j<edgelist.length;j++)
					if(edgelist[j].equals(f.edgelist[i])) break;
				if(j>=edgelist.length) return false;
			}
			return true;
		} else return false;
	}
	
	/** Print to screen. */
	public String toString()
	{		
		String r="id="+getID();
		for(int i=0;i<edgelist.length;i++) r+=" "+edgelist[i].getID();
		return r;
	}
	
	/** Computes the quality of the triangular face giving its three vertices.
	 * \n
	 * The quality of a triangle is the smallest angle between its edges. The best triangle is the equilateral one,
	 * @param n0 : a MeshNode instance, first node of the face
	 * @param n1 : a MeshNode instance, second node of the face
	 * @param n2 : a MeshNode instance, third node of the face
	 * @return double : the quality value.
	 */
	public static double qualite(MeshNode n0, MeshNode n1, MeshNode n2)
	{
		double Q = 2 * Math.PI;
		double a0 = 2 * Math.PI, a1 = 2 * Math.PI, a2 = 2 * Math.PI;
		a1 = Math.abs(n0.angle(n1, n2));
		a2 = Math.abs(n1.angle(n2, n0));
		a0 = Math.abs(n2.angle(n0, n1));
		// compute the triangle quality
		Q = Math.min(a0, a1);
		Q = Math.min(a2, Q);
		return Q;
	}
	
	/** Computes quality of the triangle.
	 * @return double : the quality of the current triangular face.
	 */
	public double qualite()
	{
		HashSet nodelist = getNodes();
		if (nodelist.size()==3)
		{
			Iterator it = nodelist.iterator();
			MeshNode n0 = (MeshNode)it.next();
			MeshNode n1 = (MeshNode)it.next();
			MeshNode n2 = (MeshNode)it.next();
			return qualite(n0, n1, n2);
		}
		else
		{
			logger.warn("Unable to compute the quality of a face with "+nodelist.size() +" nodes");			
			return 0;
		}
	}
	
	
	/** Finds the apex of the triangle, giving an edge.
	 * @param e : a MeshEdge instance, the giving edge of the triangle
	 * @return MeshNode : the apex.
	 */
	public MeshNode apex(MeshEdge e)
	{
		HashSet triangleNodes = getNodes();
		if (triangleNodes.size()==3)
		{
			triangleNodes.removeAll(e.getNodes());
			if (triangleNodes.size() != 1)
			{
				return null;
			}
			else
			{
				MeshNode n = (MeshNode)triangleNodes.iterator().next();
				return n;
			}
		}
		else
		{
			logger.warn("Impossible de trouver l'apex sur ce type de face");
			return null;
		}
	}
	
	public int getType()
	{
		return MeshElement.FACE;
	}	

	/** Computes the area of the triangle.
	 * @return double : the area of the current triangle.
	 */
	public double computeArea()
	{
		double area = 0.;
		HashSet nodes = getNodes();

		if (nodes.size() != 3 )
			throw new RuntimeException("3 nodes element expected");
		
		Iterator it = nodes.iterator();
		MeshNode pt1 = (MeshNode)it.next();
		MeshNode pt2 = (MeshNode)it.next();
		MeshNode pt3 = (MeshNode)it.next();
		area = 0.5*Calculs.norm(Calculs.prodVect3D(pt1, pt2, pt3));
		
		return area;
	}
	
	public Iterator getFacesIterator()
	{
		return new SingletonIterator(this);
	}
	
	public Iterator getNodesIterator()
	{
		return getNodes().iterator();
	}	
}
