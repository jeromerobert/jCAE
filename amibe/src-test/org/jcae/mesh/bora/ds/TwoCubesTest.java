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
import java.util.HashSet;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class TwoCubesTest
{
	private static final String dir = System.getProperty("test.dir", "test")+File.separator+"input";
	private BModel model;

	@Before public void loadModel()
	{
		String file = dir+File.separator+"2cubes.brep";
		model = new BModel(file, "out");
		
	}

	/*
	 * Shape numbers for 2cubes.brep
	 * Faces 7 and 16 share the same geometrical shape
	 *             +-----------+
	 *             |    43-    |
	 *             |           |
	 *   Solid 2+  |44+  9+ 42-|
	 *             |           |
	 *             |    45+    |
	 * +-----------+-----------+-----------+-----------+
	 * |    51-    |    32-    |    47+    |    36+    |
	 * |           |           |           |           |
	 * |52+ 11+ 50-|31+  6- 33-|46+ 10- 48-|37+  7+ 35-|
	 * |           |           |           |           |
	 * |    53+    |    30+    |    49-    |    34-    |
	 * +-----------+-----------+-----------+-----------+
	 *             |    41-    |
	 *             |           |
	 *             |40-  8- 38+|
	 *             |           |
	 *             |    39+    |
	 *             +-----------+
	 *             +-----------+
	 *             |    57-    |
	 *             |           |
	 *   Solid 3-  |59+ 13+ 35-|
	 *             |           |
	 *             |    58+    |
	 * +-----------+-----------+-----------+-----------+
	 * |    67-    |    54-    |    52+    |    60+    |
	 * |           |           |           |           |
	 * |68+ 17+ 66-|56+ 12+ 34-|39+ 16- 43-|36+ 14- 62-|
	 * |           |           |           |           |
	 * |    69+    |    55+    |    48-    |    61-    |
	 * +-----------+-----------+-----------+-----------+
	 *             |    64-    |
	 *             |           |
	 *             |65- 15- 37+|
	 *             |           |
	 *             |    63+    |
	 *             +-----------+
	 */

	@Test public void testcase1()
	{
		BCADGraphCell root = model.getGraph().getRootCell();
		Iterator<BCADGraphCell> its = root.shapesExplorer(CADShapeEnum.SOLID);
		BCADGraphCell [] solids = new BCADGraphCell[2];
		solids[0] = its.next();
		solids[1] = its.next();
		BCADGraphCell f0 = model.getGraph().getById(7);
		BCADGraphCell f1 = model.getGraph().getById(16);

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
		Constraint c3 = new Constraint(f0, h3);
		Constraint c4 = new Constraint(f1, h4);

		BSubMesh[] submesh = new BSubMesh[2];
		submesh[0] = model.newMesh();
		submesh[0].add(c1);
		submesh[0].add(c3);
		submesh[1] = model.newMesh();
		submesh[1].add(c2);
		submesh[1].add(c4);

		//model.printAllHypothesis();
		//model.printConstraints();
		model.computeConstraints();
		//model.printConstraints();

		double l1 = h1.getLength();
		double l2 = h2.getLength();
		double l3 = h3.getLength();
		double l4 = h4.getLength();
		double[][] expectedLength = {
		{
			l1, l1, l1, l1,
			l3, l3, l3, l3,
			l1, l3, l1, l1,
			l1, l3, l1, l1,
			l1, l1, l3, l1,
			l1, l1, l3, l1
		}, {
			l2, l2, l4, l2,
			l2, l2, l4, l2,
			l2, l2, l4, l2,
			l2, l2, l4, l2,
			l4, l4, l4, l4,
			l2, l2, l2, l2
		}};
		HashSet<BCADGraphCell> multipleEdges = new HashSet<BCADGraphCell>();
		for (Iterator<BCADGraphCell> ite = f0.shapesExplorer(CADShapeEnum.EDGE); ite.hasNext(); )
			multipleEdges.add(ite.next());
		for (Iterator<BCADGraphCell> ite = f1.shapesExplorer(CADShapeEnum.EDGE); ite.hasNext(); )
			multipleEdges.add(ite.next());

		for (int i = 0; i < solids.length; i++)
		{
			int j = 0;
			for (Iterator<BCADGraphCell> itf = solids[i].shapesExplorer(CADShapeEnum.FACE); itf.hasNext(); )
			{
				BCADGraphCell f = itf.next();
				for (Iterator<BCADGraphCell> ite = f.shapesExplorer(CADShapeEnum.EDGE); ite.hasNext(); )
				{
					BCADGraphCell e = ite.next();
					if (multipleEdges.contains(e))
					{
						assertEquals(f.toString(), 2, e.getDiscretizations().size());
						BDiscretization d = e.getDiscretizationSubMesh(submesh[i]);
						assertEquals(expectedLength[i][j], d.getConstraint().getHypothesis().getLength(), 1.e-6);
					}
					else
					{
						assertEquals(e.toString(), 1, e.getDiscretizations().size());
						BDiscretization d = e.getDiscretizationSubMesh(submesh[i]);
						assertEquals(expectedLength[i][j], d.getConstraint().getHypothesis().getLength(), 1.e-6);
					}
					j++;
				}

			}
		}
	}

	@Test public void testcase2()
	{
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

		Constraint c1 = new Constraint(solids[0], h1);
		Constraint c2 = new Constraint(solids[1], h2);
		Constraint c3 = new Constraint(face, h3);

		BSubMesh submesh1 = model.newMesh();
		submesh1.add(c1);
		submesh1.add(c2);
		submesh1.add(c3);

		//model.printAllHypothesis();
		//model.printConstraints();
		model.computeConstraints();
		//model.printConstraints();
		double l1 = h1.getLength();
		double l2 = h2.getLength();
		double l3 = h3.getLength();
		double[][] expectedLength = {
		{
			l1, l1, l1, l1,
			l3, l3, l3, l3,
			l1, l3, l1, l1,
			l1, l3, l1, l1,
			l1, l1, l3, l1,
			l1, l1, l3, l1
		}, {
			l2, l2, l3, l2,
			l2, l2, l3, l2,
			l2, l2, l3, l2,
			l2, l2, l3, l2,
			l3, l3, l3, l3,
			l2, l2, l2, l2
		}};
		for (int i = 0; i < solids.length; i++)
		{
			int j = 0;
			for (Iterator<BCADGraphCell> itf = solids[i].shapesExplorer(CADShapeEnum.FACE); itf.hasNext(); )
			{
				BCADGraphCell f = itf.next();
				for (Iterator<BCADGraphCell> ite = f.shapesExplorer(CADShapeEnum.EDGE); ite.hasNext(); )
				{
					BCADGraphCell e = ite.next();
					assertEquals(1, e.getDiscretizations().size());
					BDiscretization d = e.getDiscretizations().iterator().next();
					assertEquals(expectedLength[i][j], d.getConstraint().getHypothesis().getLength(), 1.e-6);
					j++;
				}

			}
		}
	}

	@Test public void testcase3()
	{
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

		Constraint c1 = new Constraint(solids[0], h1);
		Constraint c2 = new Constraint(solids[1], h2);
		Constraint c3 = new Constraint(face, h3);

		BSubMesh[] submesh = new BSubMesh[2];
		submesh[0] = model.newMesh();
		submesh[0].add(c1);
		submesh[0].add(c3);
		submesh[1] = model.newMesh();
		submesh[1].add(c2);
		submesh[1].add(c3);

		//model.printAllHypothesis();
		//model.printConstraints();
		model.computeConstraints();
		//model.printConstraints();
		double l1 = h1.getLength();
		double l2 = h2.getLength();
		double l3 = h3.getLength();
		double[][] expectedLength = {
		{
			l1, l1, l1, l1,
			l3, l3, l3, l3,
			l1, l3, l1, l1,
			l1, l3, l1, l1,
			l1, l1, l3, l1,
			l1, l1, l3, l1
		}, {
			l2, l2, l3, l2,
			l2, l2, l3, l2,
			l2, l2, l3, l2,
			l2, l2, l3, l2,
			l3, l3, l3, l3,
			l2, l2, l2, l2
		}};
		for (int i = 0; i < solids.length; i++)
		{
			int j = 0;
			for (Iterator<BCADGraphCell> itf = solids[i].shapesExplorer(CADShapeEnum.FACE); itf.hasNext(); )
			{
				BCADGraphCell f = itf.next();
				for (Iterator<BCADGraphCell> ite = f.shapesExplorer(CADShapeEnum.EDGE); ite.hasNext(); )
				{
					BCADGraphCell e = ite.next();
					assertEquals(1, e.getDiscretizations().size());
					BDiscretization d = e.getDiscretizations().iterator().next();
					assertEquals(expectedLength[i][j], d.getConstraint().getHypothesis().getLength(), 1.e-6);
					j++;
				}

			}
		}
	}
}

