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

package org.jcae.mesh.mesher.ds;

import org.jcae.mesh.mesher.ds.MNode1D;
import org.jcae.mesh.mesher.metrics.Metric2D;
import org.jcae.mesh.cad.*;
import org.jcae.mesh.util.Predicates;

import gnu.trove.THashSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;

/**
 * 2D node.
 */
public class MNode2D
{
	private static Logger logger=Logger.getLogger(MNode2D.class);

	//  The natural coordinates of the node
	private double[] param = new double[2];
	
	//  Metrics at this location
	private Metric2D m2;
	
	//  Link to the geometrical node, if any
	private MNode1D ref1d;
	
	//  Node label
	private int label = -1;
	
	//  Predicates
	public static final Predicates pred = new Predicates();
	
	//  Link to finite elements
	private MFace2D[] element2Dlist=new MFace2D[0];
	private MEdge2D[] element1Dlist=new MEdge2D[0];
	
	//  ID used for debugging purpose
	private static int id = 0;
	private static HashMap mapHashcodeToID;
	//  Initialize mapHashcodeToID
	static { assert(null != (mapHashcodeToID = new HashMap())); }
	
	/**
	 * Creates a <code>MNode2D</code> instance by specifying its 2D coordinates.
	 *
	 * @param u  coordinate along the X-axis.
	 * @param v  coordinate along the Y-axis.
	 */
	public MNode2D(double u, double v)
	{
		param[0] = u;
		param[1] = v;
		assert(setID());
	}
	
	/**
	 * Creates a <code>MNode2D</code> instance by projecting a
	 * <code>MNode1D</code> node to the current surface.
	 * The <code>MNode1D</code> instance can either be a vertex (in which
	 * case <code>C2d</code> is null and the vertex is retrieved with
	 * <code>pt.getRef()</code>) or a discretization point added during edge
	 * tessellation.
	 *
	 * @param pt  the <code>MNode1D</code> being projected.
	 * @param C2d  a 2D curve representing the edge on which <code>pt</code>
	 * lies.
	 * @param F  current topological face.
	 */
	public MNode2D(MNode1D pt, CADGeomCurve2D C2d, CADFace F)
	{
		ref1d = pt;
		if (null != C2d)
			param = C2d.value(pt.getParameter());
		else
		{
			CADVertex V = pt.getRef();
			if (null == V)
				throw new java.lang.RuntimeException("Error in MNode2D()");
			param = V.parameters(F);
		}
		assert(setID());
	}
	
	private boolean setID()
	{
		id++;
		mapHashcodeToID.put(this, new Integer(id));
		return true;
	}
	
	/**
	 * Returns the public identifer.
	 *
	 * @return the public identifer.
	 */
	public int getID()
	{
		if (id > 0)
			return ((Integer)mapHashcodeToID.get(this)).intValue();
		else
			return hashCode();
	}
	
	/**
	 * Returns the coordinate along X-axis.
	 *
	 * @return the coordinate along X-axis.
	 */
	public double getU()
	{
		return param[0];
	}
	
	/**
	 * Returns the coordinate along Y-axis.
	 *
	 * @return the coordinate along Y-axis.
	 */
	public double getV()
	{
		return param[1];
	}
	
	/**
	 * Returns the coordinate array.
	 *
	 * @return the coordinate array.
	 */
	public double[] getUV()
	{
		return param;
	}
	
	/**
	 * Sets coordinates.
	 *
	 * @param u   coordinate along X-axis.
	 * @param v   coordinate along Y-axis.
	 */
	public void setUV(double u, double v)
	{
		param[0] = u;
		param[1] = v;
		//  Deletes previous metrics so that it gets computed again
		//  if needed.
		m2 = null;
	}
	
	public Metric2D getMetrics(CADGeomSurface surf)
	{
		if (null == m2)
			m2 = new Metric2D(surf, this);
		return m2;
	}
	
	/**
	 * Sets node label.
	 *
	 * @param l  node label
	 */
	public void setLabel(int l)
	{
		label = l;
	}
	
	/**
	 * Returns the node label.
	 *
	 * @return the node label.
	 */
	public int getLabel()
	{
		return label;
	}
	
	/**
	 * Returns a reference to a <code>MNode1D</code> object.
	 * When an edge is common to several faces, boundary nodes
	 * must not be inserted twice in the final mesh.  Uniqueness
	 * is tested by checking that <code>ref1d</code> is null or unique.
	 *
	 * @return a reference to a <code>MNode1D</code> object.
	 **/
	public MNode1D getRef()
	{
		if (null == ref1d)
			return null;
		return ref1d.getMaster();
	}
	
	/**
	 * Returns <code>true</code> if this node can be moved.
	 *
	 * @return <code>true</code> if this node can be moved.
	 **/
	public boolean isMutable()
	{
		return (null == ref1d && label == -1);
	}
	
	/**
	 * Link node to a 2D finite element.
	 *
	 * @param elt  the 2D finite element to be linked against.
	 */
	public void link(MFace2D elt)
	{
		for (int i=0; i<element2Dlist.length; i++)
			if (element2Dlist[i] == elt)
				return;

		MFace2D[] ne = new MFace2D[element2Dlist.length+1];
		if (element2Dlist.length > 0)
			System.arraycopy(element2Dlist, 0, ne, 0, element2Dlist.length);
		ne[element2Dlist.length] = elt;
		element2Dlist = ne;
	}
	
	/**
	 * Link node to a 1D finite element.
	 *
	 * @param elt  the 1D finite element to be linked against.
	 */
	public void link(MEdge2D elt)
	{
		for (int i=0; i<element1Dlist.length; i++)
			if (element1Dlist[i] == elt)
				return;

		MEdge2D[] ne = new MEdge2D[element1Dlist.length+1];
		System.arraycopy(element1Dlist, 0, ne, 0, element1Dlist.length);
		ne[element1Dlist.length] = elt;
		element1Dlist = ne;
	}
	
	/**
	 * Unlink node to a 2D finite element.
	 *
	 * @param elt  the 2D finite element to be removed from the list of 2D
	 * elements.
	 */
	public void unlink(MFace2D elt)
	{
		for (int i=0; i<element2Dlist.length; i++)
		{
			if (element2Dlist[i] == elt)
			{
				MFace2D[] ne = new MFace2D[element2Dlist.length-1];
				if (i>0)
					System.arraycopy(element2Dlist, 0, ne, 0, i);
				if (i<element2Dlist.length-1)
					System.arraycopy(element2Dlist, i+1, ne, i, element2Dlist.length-i-1);
				element2Dlist = ne;
				return;
			}
		}
		throw new NoSuchElementException(""+this+" not linked to "+elt);
	}
	
	/**
	 * Unlink node to a 1D finite element.
	 *
	 * @param elt  the 1D finite element to be removed from the list of 1D
	 * elements.
	 */
	public void unlink(MEdge2D elt)
	{
		for (int i=0; i<element1Dlist.length; i++)
		{
			if (element1Dlist[i] == elt)
			{
				MEdge2D[] ne = new MEdge2D[element1Dlist.length-1];
				if (i>0)
					System.arraycopy(element1Dlist, 0, ne, 0, i);
				if (i<element1Dlist.length-1)
					System.arraycopy(element1Dlist, i+1, ne, i, element1Dlist.length-i-1);
				element1Dlist = ne;
				return;
			}
		}
		throw new NoSuchElementException(""+elt);
	}
	
	/**
	 * Returns the set of edges this node is connected to.
	 *
	 * @return the set of edges this node is connected to.
	 **/
	private Collection getEdges()
	{
		THashSet s = new THashSet();
		for(int i=0; i<element1Dlist.length; i++)
		{
			MEdge2D e = element1Dlist[i];
			if (this==e.getNodes1() || this==e.getNodes2())
				s.add(e);
		}
		for(int i=0; i<element2Dlist.length; i++)
		{
			Iterator it = element2Dlist[i].getEdgesIterator();
			while(it.hasNext())
			{
				MEdge2D e = (MEdge2D) it.next();
				if (this==e.getNodes1() || this==e.getNodes2())
					s.add(e);
			}
		}
		return s;
	}
	
	/**
	 * Returns an iterator over the set of edges this node is connected to.
	 *
	 * @return an iterator over the set of edges this node is connected to.
	 **/
	public Iterator getEdgesIterator()
	{
		return getEdges().iterator();
	}
	
	/**
	 * Tells if this node can be removed.  A node can be removed only if
	 * it is not on a boundary edge, and if it is not connected to any
	 * finite element.
	 *
	 * @return <code>true</code> if this node can be removed,
	 * <code>false</code> otherwise.
	 **/
	public boolean canDestroy()
	{
		if (!isMutable())
			return false;
		return (0 == element1Dlist.length + element2Dlist.length);
	}
	
	/**
	 * Returns the set of finite elements this node is connected to.
	 *
	 * @return the set of finite elements this node is connected to.
	 **/
	public Collection getElements()
	{
		Collection s = new THashSet(element1Dlist.length + element2Dlist.length);	
		for(int i=0; i<element1Dlist.length; i++)
			s.add(element1Dlist[i]);
		for(int i=0; i<element2Dlist.length; i++)
			s.add(element2Dlist[i]);
		return s;
	}
	
	/**
	 * Returns an iterator over the set of finite elements this node is
	 * connected to.
	 *
	 * @return an iterator over the set of finite elements this node is
	 * connected to.
	 **/
	public Iterator getElementsIterator()
	{
		return getElements().iterator();
	}
	
	/**
	 * Returns the set of 2D finite elements this node is connected to.
	 *
	 * @return the set of 2D finite elements this node is connected to.
	 **/
	public HashSet getElements2D()
	{
		return new HashSet(java.util.Arrays.asList(element2Dlist));
	}
	
	/**
	 * Returns an iterator over the set of 2D finite elements this node is
	 * connected to.
	 *
	 * @return an iterator over the set of 2D finite elements this node is
	 * connected to.
	 **/
	public Iterator getElements2DIterator()
	{
		return getElements2D().iterator();
	}
	
	/**
	 * Returns the set of neighbour nodes.
	 *
	 * @return the set of neighbour nodes.
	 **/
	public HashSet getNeighboursNodes()
	{
		HashSet toReturn=new HashSet();
		for(Iterator it=getEdgesIterator(); it.hasNext(); )
		{
			MEdge2D e = (MEdge2D) it.next();
			if (e.getNodes1() == this)
				toReturn.add(e.getNodes2());
			else
				toReturn.add(e.getNodes1());
		}
		return toReturn;
	}
	
	/**
	 * Checks whether points are counterclockwise oriented.
	 *
	 * @param n1  first node,
	 * @param n2  secnd node.
	 * @return a positive value if current instance, <code>n1</code> and
	 * <code>n2</code> occur in counterclockwise order, zero if they are
	 * collinear, and a negative value otherwise.
	 * @see Predicates#orient2d
	 */
	public double orient2d(MNode2D n1, MNode2D n2, CADGeomSurface surf)
	{
		double [] pa = new double[2];
		double [] pb = new double[2];
		double [] pc = new double[2];
		pa[0] = getU(); 
		pa[1] = getV(); 
		pb[0] = n1.getU(); 
		pb[1] = n1.getV(); 
		pc[0] = n2.getU(); 
		pc[1] = n2.getV(); 
		double xpc = (pa[0]+pb[0]+pc[0])/3.0;
		double ypc = (pa[1]+pb[1]+pc[1])/3.0;
		MNode2D centroid = new MNode2D(xpc, ypc);
		Metric2D m2d = centroid.getMetrics(surf);
		double [] tp1 = m2d.apply(pa[0]-xpc, pa[1]-ypc);
		double [] tp2 = m2d.apply(pb[0]-xpc, pb[1]-ypc);
		double [] tp3 = m2d.apply(pc[0]-xpc, pc[1]-ypc);
		return pred.orient2d(tp1, tp2, tp3);
	}
	
	/**
	 * Checks whether a point is inside or outside a triangle.
	 * It calls Predicates.orient2d() and not orient2d() for accuracy.
	 *
	 * @param face  the triangle being checked.
	 * @return 0 if the node is interior, i if it is on edge i (1&lt;=i&lt;=3), -1 otherwise.
	 */
	public int inTriangle(MFace2D face, CADGeomSurface surf)
	{
		Iterator node_it = face.getNodesIterator();
		MNode2D P1 = (MNode2D) node_it.next();
		MNode2D P2 = (MNode2D) node_it.next();
		MNode2D P3 = (MNode2D) node_it.next();
		return inTriangle(P1, P2, P3, surf);
	}
	
	/**
	 * Checks whether a point is inside or outside a triangle.
	 * It calls Predicates.orient2d() and not orient2d() for efficiency.
	 *
	 * @param face  the triangle being checked.
	 * @return 0 if the node is interior, i if it is on edge i (1&lt;=i&lt;=3), -1 otherwise.
	 */
	public int inTriangle(MNode2D P1, MNode2D P2, MNode2D P3, CADGeomSurface surf)
	{
		double [] pa = new double[2];
		double [] pb = new double[2];
		double [] pc = new double[2];
		double [] pd = new double[2];
		pa[0] = getU(); 
		pa[1] = getV(); 
		pb[0] = P1.getU(); 
		pb[1] = P1.getV(); 
		pc[0] = P2.getU(); 
		pc[1] = P2.getV(); 
		pd[0] = P3.getU(); 
		pd[1] = P3.getV(); 

		double pr1 = pred.orient2d(pa, pb, pc);
		double pr2 = pred.orient2d(pa, pc, pd);
		double pr3 = pred.orient2d(pa, pd, pb);
		double eps = 1.e-8;
		int ret = -1;
		if (pr1 > eps && pr2 > eps && Math.abs(pr3) <= eps*(pr1+pr2))
			ret = 3;
		else if (pr2 > eps && pr3 > eps && Math.abs(pr1) <= eps*(pr2+pr3))
			ret = 1;
		else if (pr3 > eps && pr1 > eps && Math.abs(pr2) <= eps*(pr3+pr1))
			ret = 2;
		else if (pr1 < -eps && pr2 < -eps && Math.abs(pr3) <= -eps*(pr1+pr2))
			ret = 3;
		else if (pr2 < -eps && pr3 < -eps && Math.abs(pr1) <= -eps*(pr2+pr3))
			ret = 1;
		else if (pr3 < -eps && pr1 < -eps && Math.abs(pr2) <= -eps*(pr3+pr1))
			ret = 2;
		else if (pr1 > 0.0 && pr2 > 0.0 && pr3 > 0.0)
			ret = 0;
		else if (pr1 < 0.0 && pr2 < 0.0 && pr3 < 0.0)
			ret = 0;
		return ret;
	}
	
	/**
	 * Checks whether a point is inside or outside a triangle.
	 * It calls Predicates.orient2d() and not orient2d() for efficiency.
	 *
	 * @param face  the triangle being checked.
	 * @return <code>true</code> if the node is interior, 
	 */
	public boolean inCircle(MNode2D P1, MNode2D P2, MNode2D P3, CADGeomSurface surf)
	{
		double [] pa = new double[2];
		double [] pb = new double[2];
		double [] pc = new double[2];
		double [] pd = new double[2];
		pa[0] = getU(); 
		pa[1] = getV(); 
		pb[0] = P1.getU(); 
		pb[1] = P1.getV(); 
		pc[0] = P2.getU(); 
		pc[1] = P2.getV(); 
		pd[0] = P3.getU(); 
		pd[1] = P3.getV(); 
		double xpc = (pa[0]+pb[0]+pc[0]+pd[0])/4.0;
		double ypc = (pa[1]+pb[1]+pc[1]+pd[1])/4.0;
		MNode2D centroid = new MNode2D(xpc, ypc);
		Metric2D m2d = centroid.getMetrics(surf);
		double [] tp1 = m2d.apply(pa[0]-xpc, pa[1]-ypc);
		double [] tp2 = m2d.apply(pb[0]-xpc, pb[1]-ypc);
		double [] tp3 = m2d.apply(pc[0]-xpc, pc[1]-ypc);
		double [] tp4 = m2d.apply(pd[0]-xpc, pd[1]-ypc);
		return pred.incircle(tp1, tp2, tp3, tp4) * pred.orient2d(tp1, tp2, tp3) > 0.0;
	}
	
	public String toString()
	{
		String r="MNode2D: id="+getID()+
			" "+param[0]+" "+param[1];
		if (null != ref1d)
			r+=" ref1d="+ref1d.getMaster().getID();
		if (-1 != label)
			r+=" label="+label;
		return r;
	}
}
