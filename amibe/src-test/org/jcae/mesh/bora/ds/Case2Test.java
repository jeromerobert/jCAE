/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2009, by EADS France

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

import java.io.File;
import java.util.Iterator;

import org.junit.Test;
import static org.junit.Assert.*;

public class Case2Test
{
	private static final String dir = System.getProperty("test.dir", "test")+File.separator+"input";

	@Test public void print()
	{
		String file = dir+File.separator+"2cubes.brep";

		BModel model = new BModel(file, "out");
		BCADGraphCell root = model.getGraph().getRootCell();
		Iterator its = root.shapesExplorer(CADShapeEnum.SOLID);
		BCADGraphCell [] solids = new BCADGraphCell[2];
		solids[0] = (BCADGraphCell) its.next();
		solids[1] = (BCADGraphCell) its.next();
		BCADGraphCell face = model.getGraph().getById(7);

		Hypothesis h1 = new Hypothesis();
		h1.setElement("T4");
		h1.setLength(0.3);

		Hypothesis h2 = new Hypothesis();
		h2.setElement("T4");
		h2.setLength(0.1);

		Hypothesis h3 = new Hypothesis();
		h3.setElement("T3");
		h3.setLength(0.01);

		Hypothesis h4 = new Hypothesis();
		h4.setElement("T3");
		h4.setLength(0.04);

		Constraint c1 = new Constraint(solids[0], h1);
		Constraint c2 = new Constraint(solids[1], h2);
		Constraint c3 = new Constraint(face, h3);

		BSubMesh submesh1 = model.newMesh();
		submesh1.add(c1);
		submesh1.add(c2);
		submesh1.add(c3);

		//model.printAllHypothesis();
		//model.printConstraints();
		model.compute();
		//model.printConstraints();
	}
}

