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
import org.jcae.mesh.amibe.ds.Mesh;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;

import org.junit.Test;
import static org.junit.Assert.*;

public class JunctionTest
{
	private static final String dir = System.getProperty("test.dir", "test")+File.separator+"input";

	@Test public void junction()
	{
		String file = dir+File.separator+"junction.brep";

		BModel model = new BModel(file, "out");
		BCADGraphCell root = model.getGraph().getRootCell();

		Iterator<BCADGraphCell> itf = root.shapesExplorer(CADShapeEnum.FACE);
		BCADGraphCell face = itf.next();

		HashSet<BCADGraphCell> edges = new HashSet<BCADGraphCell>();
		Iterator<BCADGraphCell> ite = face.shapesExplorer(CADShapeEnum.EDGE);
		while(ite.hasNext())
			edges.add(ite.next());
		BCADGraphCell beam = null;
		ite = root.shapesExplorer(CADShapeEnum.EDGE);
		while(ite.hasNext())
		{
			BCADGraphCell edge = ite.next();
			if (!edges.contains(edge))
			{
				beam = edge;
				break;
			}
		}
		assertNotNull(beam);

		Hypothesis h1 = new Hypothesis();
		h1.setElement("E2");
		h1.setLength(0.01);

		Hypothesis h2 = new Hypothesis();
		h2.setElement("T3");
		h2.setLength(0.1);

		BSubMesh submesh = model.newMesh();
		submesh.add(new Constraint(beam, h1));
		submesh.add(new Constraint(face, h2));

		//model.printAllHypothesis();
		//model.printConstraints();
		model.compute();
		//model.printConstraints();

		assertEquals(1, beam.getDiscretizations().size());
		BDiscretization d = beam.getDiscretizations().iterator().next();
		SubMesh1D mesh1d = (SubMesh1D) d.getMesh();
		long expected = (long) (1.0 / h1.getLength());
		assertEquals(Long.valueOf(expected), Long.valueOf(mesh1d.getEdges().size()));

		ite = face.shapesExplorer(CADShapeEnum.EDGE);
		expected = (long) (1.0 / h2.getLength());
		while(ite.hasNext())
		{
			d = ite.next().getDiscretizations().iterator().next();
			mesh1d = (SubMesh1D) d.getMesh();
			assertEquals(Long.valueOf(expected), Long.valueOf(mesh1d.getEdges().size()));
		}

		assertEquals(1, face.getDiscretizations().size());
		d = face.getDiscretizations().iterator().next();
		Mesh mesh2d = (Mesh) d.getMesh();
		int nrTriangles = mesh2d.getTriangles().size();
		assertTrue(nrTriangles > 200 && nrTriangles < 300);
	}
}

