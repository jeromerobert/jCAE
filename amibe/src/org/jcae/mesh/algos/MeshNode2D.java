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
import org.jcae.opencascade.jni.*;

/** A class to describe a node in 2D space (uv), used to mesh the surfaces of
 * the solid.
 * @author Cyril BRANDY & Marie-Helene GARAT.
 */
public class MeshNode2D extends MeshNode
{
	/** List of nodes defining the original surface. */
	private HashSet nodesMaj=new HashSet();
	
	/**
	 * Constructor with the node's 2D coordinates
	 * @param x : a double value, the u coordinate
	 * @param y : a double value, the v coordinate
	 **/
	public MeshNode2D(double x, double y)
	{
		super(x, y, 0.0);
	}
	
	/**
	 * Constructor from a MeshNode
	 **/
	public MeshNode2D(MeshNode n)
	{
		super(n.getX(),n.getY(), 0.0, n.getPosition());
	}
	
	/** Copy constructor */
	public MeshNode2D(MeshNode2D n)
	{
		super(n.getX(), n.getY(), 0.0, n.getPosition());
		this.intx = n.intx;
		this.inty = n.inty;
		this.intz = 0;
		this.pos = n.pos;
		setID(n.getID());
	}
	
	/**
	 * Equals operator
	 * Nodes in 2D space are compared with their 2D coordinates
	 * @param o : an Object instance, the node to compare
	 * @return boolean : set to \c true if the 2 nodes' coordinates are the same
	 **/
	public boolean equals(Object o)
	{
		if (this==o) return true;
		MeshNode2D n=(MeshNode2D)o;
		return (intx==n.intx && inty==n.inty);
	}
	
	
	/**
	 * Middle operator
	 * Computes the middle point of 2 distinct nodes
	 * @param n : a MeshNode2D instance, one of the 2 distinct nodes
	 * @return MeshNode2D : the middle point
	 **/
	public MeshNode2D middle(MeshNode2D n)
	{
		return new MeshNode2D((getX() + n.getX()) * 0.5, (getY() + n.getY()) * 0.5);
	}
	
	/**
	 * Adds a MeshNode in the nodesMaj list
	 * @param n : a MeshNode instance, the node to add
	 * */
	public void addNodeMaj(MeshNode n)
	{
		nodesMaj.add(n);
	}
	
	/**
	 * Returns the list of nodesMaj
	 * @return HashSet : the list of nodesMaj
	 * */
	public HashSet getNodesMaj()
	{
		return nodesMaj;
	}
	
	/**
	 * This method computes if the current node is on an edge defined by this two extrema points
	 * This edge is contained by a triangle which contains this point, so
	 * we only have to test whether these three points are aligned.
	 * @param p1 : a MeshNode2D instance, one on the edge's node
	 * @param p2 : a MeshNode2D instance, the other node
	 * @return boolean : set to true if the current node is on the edge, false if not
	 **/
	public boolean ptOnEdge(MeshNode2D p1, MeshNode2D p2)
	{
		return (0.0 == orient2D(p1, p2));
	}
	
	/**
	 * Compute the orientation of the current node regarding the vector n1-n2.
	 * @param n1 : a MeshNode2D instance, first endpoint of the vector
	 * @param n2 : a MeshNode2D instance, second endpoint of the vector
	 * @return double : positive if the current node is on the left of vector n1-n2
	 */
	public double orient2D(MeshNode2D n1, MeshNode2D n2)
	{
		double [] pa = new double[2];
		double [] pb = new double[2];
		double [] pc = new double[2];
		pa[0] = getX();
		pa[1] = getY();
		pb[0] = n1.getX();
		pb[1] = n1.getY();
		pc[0] = n2.getX();
		pc[1] = n2.getY();
		return org.jcae.mesh.util.Predicates.orient2d(pa, pb, pc);
	}

	public double incircle (MeshNode2D n1, MeshNode2D n2, MeshNode2D n3)
	{
		double [] pa = new double[2];
		double [] pb = new double[2];
		double [] pc = new double[2];
		double [] pd = new double[2];
		pa[0] = n1.getX();
		pa[1] = n1.getY();
		pb[0] = n2.getX();
		pb[1] = n2.getY();
		pc[0] = n3.getX();
		pc[1] = n3.getY();
		pd[0] = getX();
		pd[1] = getY();
		return org.jcae.mesh.util.Predicates.incircle(pa, pb, pc, pd);
	}

	/**
	 * Method getCoord2D returns a MeshNode2D, eg a point expressed in 2D space
	 * @param _face : a TopoDS_Face instance, the topological surface in 2D space
	 * @param E : a TopoDS_Edge instance, the topological edge
	 * @param pst : a PST_Position instance, the natural coordinate of the node
	 * @return MeshMesh2D : the node expressed in 2D space
	 **/
	public static MeshNode2D getCoord2D(TopoDS_Face _face,TopoDS_Edge E,PST_Position pst)
	{
		double xy[] = new double[2];
		if (pst==null) return null;
		PST_Position npst=null;
		// Vertex
		if (pst.getType() == PST_Position.VERTEX )
		{
			double fl[] = new double[2];
			PST_VertexPosition pstl=(PST_VertexPosition)pst;
			
			TopoDS_Vertex[] vv = TopExp.vertices(E);
			TopoDS_Vertex v= (TopoDS_Vertex)pst.getShape();
			
			Geom2d_Curve curve = BRep_Tool.curveOnSurface(E, _face, fl);
			float param1;
			if (v.equals(vv[0]))
			{
				param1=(float)fl[0];
			}
			else
			{
				param1=(float)fl[1];
			}
			Geom2dAdaptor_Curve C2d = new Geom2dAdaptor_Curve(curve);
			xy = C2d.value(param1);
			
		}
		// node on edge
		else if ( (pst.getType() == PST_Position.EDGE)   )
		{
			double fl[] = new double[2];
			PST_LinePosition pstl=(PST_LinePosition)pst;
			Pair p=pstl.getCurvePosition();
			Double param1=(Double)p.second;
			
			Geom2d_Curve curve = BRep_Tool.curveOnSurface(E, _face, fl);
			Geom2dAdaptor_Curve C2d = new Geom2dAdaptor_Curve(curve);
			xy = C2d.value(param1.floatValue());
		}
		else if((pst.getType() == PST_Position.DEGENERATEDLINE))
		{
			double fl[] = new double[2];
			PST_DegeneratedLinePosition pstl=(PST_DegeneratedLinePosition)pst;
			Pair p=pstl.getCurvePosition();
			Double param1=(Double)p.second;
			Geom2d_Curve curve = BRep_Tool.curveOnSurface(E, _face, fl);
			Geom2dAdaptor_Curve C2d = new Geom2dAdaptor_Curve(curve);
			xy = C2d.value(param1.floatValue());
			
		}
		// node on surface
		else if (pst.getType() == PST_Position.SURFACE)
		{
			PST_SurfacePosition pstf=(PST_SurfacePosition)pst;
			Pair p=pstf.getSurfacePosition();
			xy[0]=((Double)p.first).doubleValue();
			xy[1]=((Double)p.second).doubleValue();			
		}
		
		MeshNode2D n=new MeshNode2D(xy[0],xy[1]);
		return n;
	}	
}
