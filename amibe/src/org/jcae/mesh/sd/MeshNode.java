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
import java.util.ArrayList;
import java.util.Collection;
import org.jcae.mesh.util.*;
import org.jcae.opencascade.jni.*;
import org.jcae.mesh.sd.MeshMesh;
import org.apache.log4j.Logger;

/** A class to describe a node in 3D space.
 *
 * \n This class allows to describe a mesh's node in 3D space.
 * \n It also provides the list of all elements linked with it.
 * \n\n The MeshNode entity is the more basic entity which is included in a mesh definition.
 * \n
 * @author Cyril BRANDY & Marie-Helene GARAT.
 */
public class MeshNode extends MeshElement
{
	private static Logger logger=Logger.getLogger(MeshNode.class);
	/** The set of MeshElement entities linked to the node.
	 * This HashSet contains all the elements linked with the current node
	 */
	private MeshElement[] elementlist=new MeshElement[0];
	
	/** The hascode */
	private int hashcode;
	
	/** x coordinate in <code>int</code> */
	protected int intx;
	
	/** y coordinate in <code>int</code> */
	protected int inty;
	
	/** z coordinate in <code>int</code> */
	protected int intz;
	
	/** scale to convert double coordinates into int coordinates */
	public static int scale=10000;
	
	/** x coordinate of origin in double */
	public static double xorig;
	
	/** y coordinate of origin in double */
	public static double yorig;
	
	/** z coordinate of origin in double */
	public static double zorig;
	
	/** The natural coordinates of the node */
	public PST_Position pos;
	
	/** Default constructor */
	public MeshNode()
	{
		pos=PST_Position.PST_UNDEFINED;
	}
	
	/**
	 * Constructor with 3D coordinates and natural coordinates.
	 *
	 * @param x : a double value, the x coordinate of the node
	 * @param y : a double value, the y coordinate of the node
	 * @param z : a double value, the z coordinate of the node
	 * @param pos : a PST_Position instance, the natural coordinate of the node
	 */
	public MeshNode(double x, double y, double z, PST_Position pos)
	{
		intx = (int)(0.5 + (x - MeshNode.xorig) * MeshNode.scale);
		inty = (int)(0.5 + (y - MeshNode.yorig) * MeshNode.scale);
		intz = (int)(0.5 + (z - MeshNode.zorig) * MeshNode.scale);
		this.pos = pos;
		if ( (pos!=PST_Position.PST_UNDEFINED) && (pos.getType()!=PST_Position.UNDEFINED) )
		{
			hashcode=pos.getShape().hashCode();
		}
		else hashcode=intx+inty+intz;
		setID(super.hashCode());
	}
	
	/**
	 * Constructeur with 3D coordinates.
	 *
	 * @param x : a double value, the x coordinate of the node
	 * @param y : a double value, the y coordinate of the node
	 * @param z : a double value, the z coordinate of the node
	 */
	public MeshNode(double x, double y, double z)
	{
		this(x, y, z, PST_Position.PST_UNDEFINED);
	}
	
	/**
	 * Constructeur with natural coordinates.
	 *
	 * @param pos : a PST_Position instance, the natural coordinate of the node
	 */
	public MeshNode( PST_Position pos)
	{
		this(0,0,0,pos);
	}
	
	/** Copy constructor */
	public MeshNode(MeshNode n)
	{
		this.intx = n.intx;
		this.inty = n.inty;
		this.intz = n.intz;
		this.pos = n.pos;
		this.hashcode=n.hashCode();
		setID(n.getID());		
	}
	
	/** Returns the hashcode.
	 *
	 *  @return int - the @c hashCode
	 */
	public int hashCode()
	{
		return hashcode;
	}
	
	/** Get x coordinate.
	 *
	 * @return double - the x coordinate
	 **/
	public double getX()
	{
		//double titi =scale;
		return xorig + (double)intx / scale;
	}
	
	/** Get y coordinate.
	 *
	 * @return double - the y coordinate
	 **/
	public double getY()
	{
		return yorig + (double)inty / scale;
	}
	
	/** Get z coordinate.
	 *
	 * @return double - the z coordinate
	 **/
	public double getZ()
	{
		return zorig + (double)intz / scale;
	}
	
	
	/** Set x coordinate.
	 * @param x : a double value, the x coordinate
	 **/
	public void setX(double x)
	{
		intx = (int)(0.5 + (x - MeshNode.xorig) * MeshNode.scale);
	}
	
	/** Set y coordinate.
	 *
	 * @param y : a double value, the y coordinate
	 **/
	public void setY(double y)
	{
		inty = (int)(0.5 + (y - MeshNode.yorig) * MeshNode.scale);
	}
	
	/** Set z coordinate.
	 *
	 * @param z : a double value, the z coordinate
	 **/
	public void setZ(double z)
	{
		intz = (int)(0.5 + (z - MeshNode.zorig) * MeshNode.scale);
	}
	
	/** Get the <code>PST_Position</code> of the node.
	 *
	 * @return PST_Position - the natural coordinate of the point
	 * @see #setPosition
	 **/
	public PST_Position getPosition()
	{
		return pos;
	}
	
	/** Set the <code>PST_Position</code> of the node.
	 * This method is used to build a (1,1) relation between the under focus
	 * MeshNode entity and a specific PST_Position entity. This relation allows to classify the MeshNode entity from various type.
	 * @param pos : a PST_Position instance, the natural coordinate of the node
	 * @see PST_Position
	 **/
	public void setPosition(PST_Position pos)
	{
		this.pos = pos;
		if (pos.getType()!=PST_Position.UNDEFINED)
			hashcode=pos.getShape().hashCode();
		else hashcode=intx+inty+intz;
	}
	
	/** Test node equality.
	 *
	 * @param elem : a MeshElement instance, an element to compare with the current node
	 * @return boolean : set to @c true if the node is same as the element
	 * @see #equals
	 **/
	public boolean isSameElement(MeshElement elem)
	{
		if (getType() != elem.getType()) return false;
		else return this.equals(elem);
	}
	
	/** Comparaison of nodes upon their natural coordinates.
	 *
	 * @param o : an Object instance, the node to compare with the current node
	 * @return boolean - set to @c true if the 2 nodes are the same
	 * @see PST_Position#equals
	 **/
	public boolean equals(Object o)
	{
		if (this==o) return true;
		MeshNode n=(MeshNode)o;

		if( (pos==PST_Position.PST_UNDEFINED) || (pos.getType()==PST_Position.UNDEFINED) 
		|| (n.pos==PST_Position.PST_UNDEFINED) || (n.pos.getType()==PST_Position.UNDEFINED) )
		{
			// Provisoire   return getID()==n.getID();
			return (getX()==n.getX() && getY()==n.getY() && getZ()==n.getZ() );
		}
		else  return pos.equals(n.pos);
	}
	
	
	/** Middle operator returns the middle node of two nodes.
	 *
	 * middle operator returns the middle node of two nodes. \n The coordinates of this middle node are
	 * computed with the mean coordinates of the 2 nodes
	 * @param n : a MeshNode instance, one of the 2 nodes
	 * @return MeshNode - the middle point
	 **/
	public MeshNode middle(MeshNode n)
	{
		return new MeshNode((getX() + n.getX()) * 0.5, (getY() + n.getY()) * 0.5, (getZ() + n.getZ()) * 0.5);
	}
	
	/**
	 * Distance between two nodes.
	 *
	 * @param pnt : a MeshNode instance, the distant node from the current node
	 * @return double : the distance between both nodes
	 * */
	public double distance(MeshNode pnt)
	{
		return Calculs.norm(this, pnt);
	}
	
	/**
	 * Distance between a node and a edge.
	 *
	 * @param e : a MeshEdge instance
	 * @return double : the distance from edge. 
	 * */	
	public double distance(MeshEdge e)
	{
		MeshNode n1 = e.getNodes1();
		double sinalpha = Math.sin(n1.angle(this, e.getNodes2()));
		if (sinalpha == 0.) return 0.;
		return Math.abs(Calculs.norm(this,n1)*sinalpha);		
	}
	
	/** Link the node to the element.
	 * Method used to link an element with the under focus node.
	 * @param elem : a MeshElement instance, the element the node is linked with.
	 */
	public void link(MeshElement elem)
	{		
		for(int i=0;i<elementlist.length;i++)
			if(elementlist[i].equals(elem)) return;

		MeshElement[] na=new MeshElement[elementlist.length+1];
		System.arraycopy(elementlist, 0, na, 0, elementlist.length);
		na[elementlist.length]=elem;
		elementlist=na;
	}
	
	/** Unlink the node
	 *
	 * @param elem : a MeshElement instance, the element linked to the node
	 */
	public void unlink(MeshElement elem)
	{
		int i;
		for(i=0;i<elementlist.length;i++)
			if(elementlist[i].equals(elem))
				break;
		if(i>=elementlist.length)
			return;

		MeshElement[] na=new MeshElement[elementlist.length-1];
		System.arraycopy(elementlist, 0, na, 0, i);
		System.arraycopy(elementlist, i+1, na, i, elementlist.length-i-1);
		elementlist=na;
	}
	
	/** Test if node is unused.
	 * @return boolean : set to true if the node is unused so we can delete it.
	 */
	public boolean canDestroy()
	{
		return (elementlist.length==0);
	}
	
	/** Get the element list
	 * @return HashSet : the list of elements linked with the current node.
	 */
	public HashSet getElements()
	{
		return new HashSet(java.util.Arrays.asList(elementlist));
	}
	
	/** Set an element list to the list of elements linked to the current node.
	 *
	 * @param elem : a HashSet of element to add in the element list
	 */
	public void setElements(HashSet elem)
	{
		elementlist=new MeshElement[elem.size()];
		System.arraycopy(elem.toArray(), 0, elementlist, 0, elementlist.length);
	}
	
	
	/** Unused method for this class.
	 */
	public HashSet getNodes()
	{
		return null;
	}
	
	/** Get the faces linked with current node
	 * @return HashSet : the list of faces linked to the current node
	 */
	public HashSet getFaces()
	{
		HashSet resultlist = new HashSet();		
		
		for(int i=0;i<elementlist.length;i++)
		{
			MeshElement elem = elementlist[i];
			int type = elem.getType();
			switch (type)
			{
				case FACE:
					resultlist.add((MeshFace)elem);
					break;
				case VOLUME:
					resultlist.addAll(((MeshVolume)elem).getFaces());
					break;
				default:;
			}
		}
		
		return resultlist;
	}
	
	/** Clear the list of elements */
	public void clearList()
	{
		elementlist=new MeshElement[0];
	}
	
	/**
	 * Returns the list of free edges linked with current node
	 * @return HashSet : the list of free edges (can be empty)
	 */
	public HashSet getFreeEdges()
	{
		HashSet freeEdges = new HashSet ();
		Iterator ite = getEdgesIterator();
		while (ite.hasNext())
		{
			MeshEdge e = (MeshEdge)ite.next();
			if (e.numberOfFaces()==1) 
				freeEdges.addFast(e);
		}
		return freeEdges;
	}
	
	/** Compute the 2D angle between vector n-n1 and vector n-n2
	 * @param n1 : a MeshNode instance, the end of vector n-n1
	 * @param n2 : a MeshNode instance, the end of vector n-n2
	 * @return double : the angle between both vector (in radius)
	 */
	public double angle(MeshNode n1, MeshNode n2)
	{
		return Calculs.angleVect(this, n1, n2);
	}
	
	/**
	 * Set the 2D coordinates
	 * @param _face : a TopoDS_Face instance, the topological surface of 2D space
	 */
	public void setCoord2D(TopoDS_Face _face)
	{
		double xy[] = new double[2];
		if (pos==null) return;
		
		// Vertex
		if (pos.getType() == PST_Position.VERTEX)
		{
			xy = ((PST_VertexPosition)(this.pos)).getUV();
			if (xy==null)
			{
				TopoDS_Vertex v= (TopoDS_Vertex)pos.getShape();
				xy=BRep_Tool.parameters(v, _face);
			}
			
		}
		// node on edge
		else if ( (pos.getType() == PST_Position.EDGE)   )
		{
			double fl[] = new double[2];
			PST_LinePosition pst=(PST_LinePosition)pos;
			Pair p=pst.getCurvePosition();
			Double param1=(Double)p.second;
			TopoDS_Edge E=(TopoDS_Edge)p.first;
			Geom2d_Curve curve = BRep_Tool.curveOnSurface(E, _face, fl);
			Geom2dAdaptor_Curve C2d = new Geom2dAdaptor_Curve(curve);
			xy = C2d.value(param1.floatValue());
		}
		else if((pos.getType() == PST_Position.DEGENERATEDLINE))
		{
			double fl[] = new double[2];
			PST_DegeneratedLinePosition pst=(PST_DegeneratedLinePosition)pos;
			Pair p=pst.getCurvePosition();
			Double param1=(Double)p.second;
			TopoDS_Edge E=(TopoDS_Edge)p.first;
			Geom2d_Curve curve = BRep_Tool.curveOnSurface(E, _face, fl);
			if (curve!=null)
			{
				Geom2dAdaptor_Curve C2d = new Geom2dAdaptor_Curve(curve);
				xy = C2d.value(param1.floatValue());
			}
			else
			{
				TopoDS_Vertex v= (TopoDS_Vertex)pst.getVertex();
				xy=BRep_Tool.parameters(v, _face);
			}
			
			
		}
		// node on surface
		else if (pos.getType() == PST_Position.SURFACE)
		{
			PST_SurfacePosition pst=(PST_SurfacePosition)pos;
			Pair p=pst.getSurfacePosition();
			xy[0]=((Double)p.first).doubleValue();
			xy[1]=((Double)p.second).doubleValue();
			
		}
		setX(xy[0]);
		setY(xy[1]);
		setZ(0.0);
		
	}
	
	/**
	 * Set the 3D coordinates.
	 * Set values to intx, inty and intz
	 * @see #setX
	 * @see #setY
	 * @see #setZ
	 */
	public void setCoord3D()
	{
		double P[] = new double[3];
		// Test if the node has a type
		if (pos==null) return;
		if (pos.getType()==PST_Position.UNDEFINED) return;
		
		if ( pos.getType() == PST_Position.VERTEX )
		{
			//PST_VertexPosition pos=(PST_VertexPosition)pos;
			P = BRep_Tool.pnt(((PST_VertexPosition)pos).getVertex());
		}
		else if ( (pos.getType() == PST_Position.EDGE) || (pos.getType() == PST_Position.DEGENERATEDLINE) )
		{
			Pair Et = new Pair();
			
			Et=((PST_LinePosition)pos).getCurvePosition();
			double fl[] = new double[2];
			Geom_Curve curve = BRep_Tool.curve((TopoDS_Edge)Et.first, fl);
			if (curve != null)
			{
				GeomAdaptor_Curve c3d = new GeomAdaptor_Curve(curve);
				P= c3d.value(((Double)Et.second).doubleValue());
			}
			else
			{
				if (BRep_Tool.degenerated((TopoDS_Edge)Et.first))
				{
					// retrieve the TopoDS_Face that contains the node
					
					TopoDS_Face topoface = ((PST_DegeneratedLinePosition)pos).getGeometry();
					if (topoface==null)
					{
						logger.error("Cannot retrieve the face underlying the degenerated edge");
					}
					else
					{
						setCoord2D(topoface);
						Geom_Surface S=BRep_Tool.surface(topoface);
						P=S.value(this.getX(),this.getY());
					}
				}
			}
		}
		else if (pos.getType() == PST_Position.SURFACE)
		{
			Geom_Surface S=BRep_Tool.surface(((PST_SurfacePosition)pos).getSurface());
			PST_SurfacePosition poss=(PST_SurfacePosition)pos;
			Pair uv = new Pair();
			uv.first = new Double(0);
			uv.second = new Double(0);
			uv=poss.getSurfacePosition();
			P = S.value(((Double)(uv.first)).doubleValue(),((Double)(uv.second)).doubleValue());
		}
		
		setX(P[0]);
		setY(P[1]);
		setZ(P[2]);
		
	}
		
	/**
	 * Find the edge bounding patch
	 * Method used with meshSmoothing method to retrieve the edge that bounds the patch
	 * @param node - the point on the bounds of the patch
	 * @return the edge bounding the patch
	 */
	public MeshEdge getPatchEdge(MeshNode node)
	{
		MeshEdge edge = null;
		HashSet edges = this.getTopologicContour(1, EDGE);
		Iterator ite = edges.iterator();
		while (ite.hasNext())
		{
			MeshEdge e = (MeshEdge)ite.next();
			MeshNode n1 = e.getNodes1();
			MeshNode n2 = e.getNodes2();
			double a = node.angle(n1,n2);
			if ( (a==Math.PI) /*|| (a==0.) */)
				/*return*/ edge = e;
		}
		return edge;
	}
	
	/**
	 * Retreive the closest edge linked to current node from a giving 3D point
	 * @param picked - a point defined by its 3d coordinates
	 * @return The edge linked to current node and closest from the picked point
	 */
	public MeshEdge getClosestEdge(MeshNode picked)
	{
		MeshEdge edge = null;
		double angle = 0.;
		// retrieve all edges linked to this		
		Iterator ite = getEdgesIterator();
		while (ite.hasNext())
		{
			MeshEdge e = (MeshEdge)ite.next();
			MeshNode n = e.getNodes1();
			if (n.equals(this)) n = e.getNodes2();
			// the closest edge is such that the angle  is the closest to PI
			double a = picked.angle(this, n);
			if (a > angle) 
			{
				angle = a;
				edge = e;
			}
		}
		
		return edge;
	}
	
	/**
	 * Print on screen the three node's coordinates
	 */
	public String toString()
	{
		return ""+hashCode()+" "+intx+" "+inty+" "+intz+" "+pos;
	}
	
	public int getType()
	{
		return MeshElement.NODE;
	}
	
	public Collection getNeighboursNodes()
	{
		HashSet toReturn=new HashSet();
		for(int i=0;i<elementlist.length;i++)
		{
			Iterator it=elementlist[i].getNodesIterator();
			while(it.hasNext()) toReturn.add(it.next());
		}
		
		toReturn.remove(this);
		return toReturn;
	}

	public Collection getEdges()
	{
		Collection s=new HashSet();
		for(int i=0;i<elementlist.length; i++)
		{			
			Iterator it=elementlist[i].getEdgesIterator();			
			while(it.hasNext())
			{	
				MeshEdge e=(MeshEdge)it.next();
				Iterator nit=e.getNodesIterator();
				while(nit.hasNext())
				{
					if(this==nit.next())
					{
						s.add(e);
						continue;
					}
				}
			}
		}
		return s;
	}
	
	public Iterator getEdgesIterator()
	{		
		return getEdges().iterator();
	}
	
	public Iterator getFacesIterator()
	{
		/** @TODO: Make it work for volumique mesh */
		IteratorOfIterator it=new IteratorOfIterator();
		for(int i=0;i<elementlist.length; i++)
			it.addIterator(elementlist[i].getFacesIterator());
		return it;
	}
	
	public Iterator getNodesIterator()
	{
		return new SingletonIterator(this);
	}	
}
