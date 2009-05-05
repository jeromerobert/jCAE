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
import org.jcae.mesh.amibe.ds.SubMesh1D;

import java.io.File;
import java.util.Iterator;

import org.junit.Test;
import static org.junit.Assert.*;

public class CubeTest
{
	private static final String dir = System.getProperty("test.dir", "test")+File.separator+"input";

	@Test public void cube()
	{
		String file = dir+File.separator+"cube.brep";

		BModel model = new BModel(file, "out");
		BCADGraphCell root = model.getGraph().getRootCell();
		BSubMesh submesh = model.newMesh();

		/*
		 *        +------+
		 *        |  f3  |
		 *        |      |
		 *        |  Y=1 |
		 * +------+------+------+------+
		 * |  f5  |  f0  |  f4  |  f1  |
		 * |      |      |      |      |
		 * |  Z=1 |  X=1 | Z=-1 | X=-1 |
		 * +------+------+------+------+
		 *        |  f2  |
		 *        |      |
		 *        | Y=-1 |
		 *        +------+
		 * On each face f(i), edge length is set to k(i)=2^(1-i).
		 * Constraints on edges are thus:
		 *        +------+
		 *        |  k3  |
		 *        |k5  k4|
		 *        |  k3  |
		 * +------+------+------+------+
		 * |  k5  |  k3  |  k4  |  k3  |
		 * |k5  k5|k5  k4|k4  k4|k4  k5|
		 * |  k5  |  k2  |  k4  |  k2  |
		 * +------+------+------+------+
		 *        |  k2  |
		 *        |k5  k4|
		 *        |  k2  |
		 *        +------+
		 * Shape numbers are printed on the figure below, and +/- represents
		 * shape orientation (+ is forward, - means reversed):
		 *            +----------+
		 *            |    30-   |
		 *            |          |
		 *            |29+ 6- 27-|
		 *            |          |
		 *            |    28+   |
		 * +----------+----------+----------+----------+
		 * |   38-    |    17-   |    34+   |    21+   |
		 * |          |          |          |          |
		 * |37+ 8- 35-|18+ 3+ 16-|31+ 7+ 33-|20+ 4- 22-|
		 * |          |          |          |          |
		 * |   36+    |    15+   |    32-   |    19-   |
		 * +----------+----------+----------+----------+
		 *            |    24-   |
		 *            |          |
		 *            |25- 5+ 23+|
		 *            |          |
		 *            |    26+   |
		 *            +----------+
		 */
		double l = 2.0;
		BCADGraphCell[] faces = new BCADGraphCell[6];
		int i = 0;
		int [] e = new int[6];
		for (Iterator<BCADGraphCell> its = root.shapesExplorer(CADShapeEnum.FACE); its.hasNext(); )
		{
			faces[i] = its.next();
			e[i] = (int) (2.0 / l);
			Hypothesis h = new Hypothesis();
			h.setElement("T3");
			h.setLength(l);
			submesh.add(new Constraint(faces[i], h));

			i++;
			l *= 0.5;
		}
		int [] expected = {
			e[2], e[4], e[3], e[5],
			e[2], e[4], e[3], e[5],
			e[4], e[2], e[5], e[2],
			e[4], e[3], e[5], e[3],
			e[4], e[4], e[4], e[4],
			e[5], e[5], e[5], e[5]
		};

		//model.printAllHypothesis();
		//model.printConstraints();
		model.compute();
		//model.printConstraints();

		for (Iterator<BCADGraphCell> its = root.shapesExplorer(CADShapeEnum.EDGE); its.hasNext(); )
		{
			BCADGraphCell c = its.next();
			SubMesh1D mesh1d = (SubMesh1D) c.getDiscretizations().iterator().next().getMesh();
			assertEquals(expected[c.getId() - 15], mesh1d.getEdges().size());
		}
	}
}

