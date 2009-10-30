/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005, by EADS CRC
 
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

import org.jcae.mesh.cad.CADVertex;
import org.jcae.mesh.cad.CADShapeEnum;
import org.jcae.mesh.bora.ds.BDiscretization;
import org.jcae.mesh.bora.ds.BCADGraphCell;
import gnu.trove.TObjectIntHashMap;

/**
 * 1D node.
 * Nodes are located via their curvilinear abscissa on the working edge.
 * The edge on which <code>MNode1D</code> are lying is not stored here
 * because there is currently no need for such a link, all methods
 * using <code>MNode1D</code> instances are looping on all topological
 * edges, and thus current edge is known within those loops.
 */
public class MNode1D
{
	//  Curvilinear coordinate on current edge
	private final double param;
	
	//  The geometrical vertex if any
	private CADVertex vertex;
	
	//  Discretization definition if any
	private BDiscretization discr;

	//  Label used when exchanging data
	private int label = 0;
	
	//  Several MNode1D instances may share the same location.
	//  One of them is called the master, and all others have
	//  a link to it.
	private MNode1D master;
	
	//  Flag set to true if this node belongs to a degenerated edge.
	private boolean isDegenerated;
	
	//  ID used for debugging purpose
	private static int id = 0;
	private static TObjectIntHashMap<MNode1D> mapHashcodeToID;
	//  Initialize mapHashcodeToID
	static { assert(null != (mapHashcodeToID = new TObjectIntHashMap<MNode1D>())); }
	
	/**
	 * Creates a <code>MNode1D</code> instance.
	 *
	 * @param t  curvilinear abscissa on current edge
	 * @param v  if not null, the topological vertex which shares the same 
	 * location
	 */
	public MNode1D(double t, CADVertex v)
	{
		param = t;
		vertex = v;
		isDegenerated = false;
		assert(setID());
	}
	
	/**
	 * Creates a <code>MNode1D</code> instance.
	 *
	 * @param t  curvilinear abscissa on current edge
	 * @param d  if not null, the vertex discretization used
	 */
	protected MNode1D(double t, BDiscretization d)
	{
		param = t;
		discr = d;
		if (d != null)
		{
			BCADGraphCell cell = discr.getGraphCell();
			if (cell.getType() != CADShapeEnum.VERTEX)
				throw new RuntimeException("Attempt to use invalid discretization "+d+" in MNode1D ");
			vertex = (CADVertex) cell.getShape();
		}
		else
		{
		    vertex = null;
		}
		isDegenerated = false;
		assert(setID());
	}

	private boolean setID()
	{
		id++;
		mapHashcodeToID.put(this, id);
		return true;
	}
	
	/**
	 * Returns the public identifer.
	 *
	 * @return the public identifer.
	 */
	public final int getID()
	{
		if (id > 0)
			return mapHashcodeToID.get(this);
		return hashCode();
	}
	
	/**
	 * Sets node label.
	 *
	 * @param l  node label
	 */
	public final void setLabel(int l)
	{
		label = l;
	}
	
	/**
	 * Returns the node label.
	 *
	 * @return the node label.
	 */
	public final int getLabel()
	{
		return label;
	}
	
	/**
	 * Returns the curvilinear abscissa.
	 *
	 * @return the curvilinear abscissa.
	 */
	public final double getParameter()
	{
		return param;
	}

	/**
	 * Returns the topological vertex sharing the same location.
	 *
	 * @return the topological vertex sharing the same location.
	 */
	public final CADVertex getCADVertex()
	{
		if (null != master)
			return master.vertex;
		return vertex;
	}
	
	/**
	 * Returns the <i>master</i> <code>MNode1D</code> instance.
	 * Return value is a valid object, it cannot be null.
	 *
	 * @return the <i>master</i> <code>MNode1D</code> instance.
	 */
	public final MNode1D getMaster()
	{
		if (null != master)
			return master;
		return this;
	}
	
	/**
	 * Binds to a <i>master</i> <code>MNode1D</code> instance.
	 *
	 * @param ref   the <i>master</i> <code>MNode1D</code> instance.
	 */
	public final MNode1D setMaster(MNode1D ref)
	{
		if (null != ref && (ref.isDegenerated() || isDegenerated()))
		{
			ref.isDegenerated(true);
			isDegenerated = true;
		}
		master = ref;
		return master;
	}
	
	/**
	 * Tells if this node can be altered.
	 *
	 * @return <code>true</code> if this node can be altered, and
	 * <code>false</code> otherwise.
	 */
	public boolean isMutable()
	{
		return (null == vertex);
	}
	
	/**
	 * Tells if this node lies on a degenerated edge.
	 *
	 * @return <code>true</code> if this node lies on a degenerated edge,
	 * <code>false</code> otherwise.
	 */
	public final boolean isDegenerated()
	{
		return isDegenerated;
	}
	
	/**
	 * Changes the node status about degenerated edges.
	 *
	 * @param d  set status about degenerated edges to the boolean
	 * value <code>d</code>.
	 * @return its <code>d</code> argument.
	 */
	public final boolean isDegenerated(boolean d)
	{
		isDegenerated = d;
		return d;
	}
	
	@Override
	public final String toString()
	{
		String r = "MNode1D: id="+getID()+" "+param;
		if (null != vertex)
			r += " "+vertex;
		if (null != master)
			r += "  master -> " + master;
		return r;
	}
}
