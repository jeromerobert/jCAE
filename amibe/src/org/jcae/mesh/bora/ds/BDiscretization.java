/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

   (C) Copyright 2007, by EADS France

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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */


package org.jcae.mesh.bora.ds;

import org.jcae.mesh.cad.CADShapeEnum;
import org.jcae.mesh.bora.algo.AlgoInterface;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.log4j.Logger;

public class BDiscretization
{
	private static Logger logger=Logger.getLogger(BDiscretization.class);
	private final BCADGraphCell graphCell;
	// List of set of BSubMesh instances containing this BDiscretization.
	private final Collection submesh = new HashSet();
	private Constraint constraint;
	private AlgoInterface algo;
	private boolean computed = false;
	private Object mesh;

	// Unique identitier
	private int id = -1;
	private static int nextId = -1;

	public BDiscretization(BCADGraphCell g, Constraint c)
	{
		// Store forward oriented cell
		if (g.getOrientation() != 0 && g.getReversed() != null)
			graphCell = g.getReversed();
		else
			graphCell = g;
		constraint = c;
		setId();
	}

	private void setId()
	{
		nextId++;
		id = nextId;
	}

	public int getId()
	{
		return id;
	}

	public BCADGraphCell getGraphCell()
	{
		return graphCell;
	}

	public Constraint getConstraint()
	{
		return constraint;
	}

	public void addSubMesh(BSubMesh s)
	{
		submesh.add(s);
	}

	public BSubMesh getFirstSubMesh()
	{
		return (BSubMesh) submesh.iterator().next();
	}

	public void combineConstraint(BDiscretization parent)
	{
		Constraint newCons = parent.constraint.createInheritedConstraint(graphCell, constraint);
		if (constraint != null)
			newCons.getHypothesis().combine(constraint.getHypothesis());
		constraint = newCons;
	}

	public void addAllSubMeshes(BDiscretization parent)
	{
		submesh.addAll(parent.submesh);
	}

	/**
	 * Check whether a <code>BSubMesh</code> is already present.
	 * @param s <code>BSubMesh</code> being checked.
	 * @return <code>true</code> if <code>BSubMesh</code> is already
	 * found, <code>false</code> otherwise.
	 */
	public boolean contains(BSubMesh s)
	{
		return submesh.contains(s);
	}

	/**
	 * Check whether a <code>BDiscretization</code> instance contains
	 * at least a common <code>BSubMesh</code>.
	 * @param that  object being checked.
	 * @return <code>false</code> if a <code>BSubMesh</code> is common to
	 * both sets, <code>true</code> otherwise.
	 */
	public boolean emptyIntersection(BDiscretization that)
	{
		for (Iterator it = that.submesh.iterator(); it.hasNext(); )
		{
			BSubMesh s = (BSubMesh) it.next();
			if (submesh.contains(s))
				return false;
		}
		return true;
	}

	public Object getMesh()
	{
		return mesh;
	}

	public void setMesh(Object m)
	{
		mesh = m;
	}

	public void applyAlgorithm()
	{
		if (algo == null)
			algo = constraint.getHypothesis().findAlgorithm(graphCell.getType());
		if (!algo.isAvailable())
			return;
		if (!algo.compute(this))
			logger.warn("Failed! "+algo);
		computed = true;
	}

	public void discretize()
	{
	}

	public String toString()
	{
		String ret = "Discretization: "+id;
		ret += " (cons. "+constraint+") "+submesh;
		return ret;
	}
}
