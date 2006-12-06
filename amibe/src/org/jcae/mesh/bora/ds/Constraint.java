/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

   (C) Copyright 2006, by EADS CRC

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

import java.util.Collection;
import java.util.ArrayList;

public class Constraint
{
	private final BCADGraphCell graphCell;
	private final Hypothesis hypothesis;
	// List of BSubMesh instances containing this Constraint.
	private final Collection parent = new ArrayList();

	public Constraint(BCADGraphCell g, Hypothesis h)
	{
		// Store forward oriented cell
		if (g.getOrientation() != 0 && g.getReversed() != null)
			graphCell = g.getReversed();
		else
			graphCell = g;
		hypothesis = h;
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
		parent.add(s);
	}

	public String toString()
	{
		String ret = ""+hypothesis.getClass().getName()+"@"+Integer.toHexString(hypothesis.hashCode());
		ret += " "+graphCell.getClass().getName()+"@"+Integer.toHexString(graphCell.hashCode());
		return ret;
	}
}
