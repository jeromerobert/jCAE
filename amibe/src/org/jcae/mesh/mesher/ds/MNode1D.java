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

import org.jcae.mesh.cad.CADVertex;
import java.util.HashMap;
import org.apache.log4j.Logger;

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
	private static Logger logger=Logger.getLogger(MNode1D.class);

	//  Curvilinear coordinate on current edge
	private double param;
	
	//  The geometrical vertexm if any
	private CADVertex vertex;
	
	//  Label used when exchanging data
	private int label = -1;
	
	//  Several MNode1D instances may share the same location.
	//  One of them is called the master, and all others have
	//  a link to it.
	private MNode1D master;
	
	//  Flag set to true if this node belongs to a degenerated edge.
	private boolean isDegenerated;
	
	//  ID used for debugging purpose
	private static int id = 0;
	private static HashMap mapHashcodeToID;
	//  Initialize mapHashcodeToID
	static { assert(null != (mapHashcodeToID = new HashMap())); };
	
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
	 * Returns the curvilinear abscissa.
	 *
	 * @return the curvilinear abscissa.
	 */
	public double getParameter()
	{
		return param;
	}

	/**
	 * Returns the topological vertex sharing the same location.
	 *
	 * @return the topological vertex sharing the same location.
	 */
	public CADVertex getCADVertex()
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
	public MNode1D getMaster()
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
	public MNode1D setMaster(MNode1D ref)
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
	public boolean isDegenerated()
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
	public boolean isDegenerated(boolean d)
	{
		isDegenerated = d;
		return d;
	}
	
	public String toString()
	{
		String r = "MNode1D: id="+getID()+" "+param;
		if (null != vertex)
			r += " "+vertex;
		if (null != master)
			r += "  master -> " + master;
		return r;
	}
}
