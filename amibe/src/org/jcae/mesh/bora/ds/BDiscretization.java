/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2007, by EADS France

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

import org.jcae.mesh.bora.algo.AlgoInterface;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Iterator;

import org.apache.log4j.Logger;

public class BDiscretization
{
	private static Logger logger=Logger.getLogger(BDiscretization.class);
	private final BCADGraphCell graphCell;
	// List of set of BSubMesh instances containing this BDiscretization.
	private final Collection<BSubMesh> submesh = new LinkedHashSet<BSubMesh>();
	private Constraint constraint;
	private AlgoInterface algo;
	private boolean computed = false;
	private Object mesh;

	// Unique identitier
	private int id = -1;
	protected static int nextId = -1;

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

	public Collection<BSubMesh> getSubmesh()
	{
		return submesh;
	}

	public void addSubMesh(BSubMesh s)
	{
		submesh.add(s);
	}

	public BSubMesh getFirstSubMesh()
	{
		return submesh.iterator().next();
	}

	/**
	 * When combining two constraints on the same BCADGraphCell (this happens during
	 * the phase when the set of needed BDiscretization are created), we want to
	 * ensure that those two constraints are not both "original" constraints
	 * set by the user on the same object of the CAD.
	 */
	public void combineConstraint(BDiscretization baseDiscr)
	{
		Constraint newCons = baseDiscr.constraint.createInheritedConstraint(graphCell, constraint);
		if (constraint != null)
		{
			Constraint baseOrigCons = baseDiscr.constraint.originConstraint(graphCell);
			Constraint localOrigCons = constraint.originConstraint(graphCell);
			// situation where there are two conflicting user
			// constraints for the same discretization
			if (((localOrigCons != null) && (baseOrigCons != null)) && (localOrigCons != baseOrigCons))
				throw new RuntimeException("Definition of model imposes the same discretization for shape "+graphCell.getShape()+" but there are two different user-defined constraints for this discretization:" + baseOrigCons + " and " + localOrigCons );

			if (!newCons.getHypothesis().combine(constraint.getHypothesis()))
				throw new RuntimeException("Cannot combine "+newCons+" with "+constraint+" on "+graphCell);
			// TODO: in combine(), it will be necessary to detect 
			// which is the original constraint
		} 
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
	 * Test of inclusion of the submesh list in the submesh list of that
	 * Check whether a <code>BDiscretization</code> has all of its
	 * submesh list contained in the parameter's submesh list
	 * @param that  object being checked.
	 */
	public boolean contained(BDiscretization that)
	{
		for (Iterator<BSubMesh> it = submesh.iterator(); it.hasNext(); )
		{
			BSubMesh s = it.next();
			if (!(that.submesh.contains(s)))
				return false;
		}
		return true;
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
		for (Iterator<BSubMesh> it = that.submesh.iterator(); it.hasNext(); )
		{
			BSubMesh s = it.next();
			if (submesh.contains(s))
				return false;
		}
		return true;
	}

	/**
	 * Check whether a <code>BDiscretization</code> instance is needed for 
	 * the definition of a <code>BSubMesh</code>.
	 */
	public boolean isSubmeshChild(BSubMesh that)
	{
		// if the submesh is not contained in the submesh list, there is no need
		// to continue further
		if (!submesh.contains(that))
			return false;
		// loop on the constraints of the submesh
		for (Iterator<Constraint> itc = that.getConstraints().iterator(); itc.hasNext(); )
		{
			Constraint cons = itc.next();
			BCADGraphCell cell = cons.getGraphCell();
			// loop on the childs of the graphcell of the constraint of the same type
			for (Iterator<BCADGraphCell> it = cell.shapesExplorer(graphCell.getType()); it.hasNext(); )
			{
				BCADGraphCell child = it.next();
				// loop on the discretizations of the child
				for (Iterator<BDiscretization> itd = child.discretizationIterator(); itd.hasNext(); )
				{
					BDiscretization discr = itd.next();

					if (discr == this)
					    return true;
				}
			}

		}
		return false;
	}

	public Object getMesh()
	{
		return mesh;
	}

	public void setMesh(Object m)
	{
		mesh = m;
	}

	public void discretize()
	{
		if (computed)
			return;
		if (algo == null)
			algo = constraint.getHypothesis().findAlgorithm(graphCell.getType());
		if (algo == null || !algo.isAvailable())
			return;
		if (!algo.compute(this))
			logger.warn("Failed! "+algo);
		computed = true;
	}

	@Override
	public String toString()
	{
		String ret = "Discretization: "+id;
		ret += " (cons. "+constraint+") "+submesh;
		return ret;
	}
}
