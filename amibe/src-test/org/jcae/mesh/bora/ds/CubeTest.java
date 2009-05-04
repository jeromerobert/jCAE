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
		 *        |  f4  |
		 *        |      |
		 *        |  Y=1 |
		 * +------+------+------+------+
		 * |  f6  |  f1  |  f5  |  f2  |
		 * |      |      |      |      |
		 * |  Z=1 |  X=1 | Z=-1 | X=-1 |
		 * +------+------+------+------+
		 *        |  f3  |
		 *        |      |
		 *        | Y=-1 |
		 *        +------+
		 */
		double l = 2.0;
		for (Iterator<BCADGraphCell> its = root.shapesExplorer(CADShapeEnum.FACE); its.hasNext(); )
		{
			Hypothesis h = new Hypothesis();
			h.setElement("T3");
			h.setLength(l);
			submesh.add(new Constraint(its.next(), h));

			l *= 0.5;
		}

		//model.printAllHypothesis();
		//model.printConstraints();
		model.compute();
		//model.printConstraints();
	}
}

