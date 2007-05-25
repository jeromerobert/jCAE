/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

   (C) Copyright 2006, by EADS CRC
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
import java.util.Collection;
import java.util.ArrayList;

public class Constraint
{
	private final BCADGraphCell graphCell;
	private final Hypothesis hypothesis;
	// List of BSubMesh instances containing this Constraint.
	private final Collection submesh = new ArrayList();
	private Constraint origin;

	// Unique identitier
	private int id = -1;
	private static int nextId = -1;

	public Constraint(BCADGraphCell g, Hypothesis h)
	{
		// Store forward oriented cell
		if (g.getOrientation() != 0 && g.getReversed() != null)
			graphCell = g.getReversed();
		else
			graphCell = g;
		hypothesis = h;
		if (!hypothesis.checkCompatibility(graphCell.getType()))
			throw new RuntimeException("Element type "+hypothesis.getElement()+" cannot be applied to CAD element of type: "+graphCell.getType()+"   "+h);
		setId();
	}

	public Constraint createInheritedConstraint(BCADGraphCell g, Constraint old)
	{
		Constraint ret = new Constraint(g, hypothesis.createInheritedHypothesis(g.getType()));
		if (old != null)
		{
			if (old.origin != null)
				ret.origin = old.origin;
			else
				ret.origin = old;
		}
		else if (origin != null)
			ret.origin = origin;
		else
			ret.origin = this;
		return ret;
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

	public Hypothesis getHypothesis()
	{
		return hypothesis;
	}

	public void addSubMesh(BSubMesh s)
	{
		submesh.add(s);
	}

	public String toString()
	{
		String ret = "Constraint: "+id;
		ret += " (hyp "+hypothesis+", cell "+Integer.toHexString(graphCell.hashCode())+")";
		if (origin != null)
			ret += " ["+origin.id+"]";
		return ret;
	}

}
