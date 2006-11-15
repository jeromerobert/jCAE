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

import org.jcae.mesh.cad.CADShape;
import org.jcae.mesh.cad.CADShapeBuilder;
import org.jcae.mesh.cad.CADExplorer;
import org.jcae.mesh.cad.CADVertex;
import org.jcae.mesh.cad.CADEdge;
import org.jcae.mesh.cad.CADFace;
import org.jcae.mesh.cad.CADSolid;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 * Mesh graph.
 */
public class BSupport
{
	private static Logger logger=Logger.getLogger(BSupport.class);
	//   Model
	private BModel model;
	//   First free index
	private static int freeIndex = 0;
	//   Unique identitier
	private int id = -1;
	//   List of children
	private Collection setShapes = new LinkedHashSet();
	//   Tessellation
	public Object mesh = null;

	/**
	 * Creates a root mesh.
	 */
	public BSupport(BModel m, int offset)
	{
		model = m;
		id = offset + freeIndex;
		freeIndex++;
	}
	
	public int getId()
	{
		return id;
	}

	/**
	 * Adds a shape to current group.
	 *
	 * @param s  shape
	 */
	public void add(CADShape s)
	{
		CADShapeBuilder factory = CADShapeBuilder.factory;
		CADExplorer exp = factory.newExplorer();
		for (int t = 0; t < BCADGraph.shapeTypeArray.length; t++)
		{
			for (exp.init(s, BCADGraph.shapeTypeArray[t]); exp.more(); exp.next())
			{
				CADShape sub = exp.current();
				// In OccJava, orientation is not taken into account
				if (setShapes.contains(sub))
					continue;
				setShapes.add(sub);
			}
		}
	}
	
	public void add(BSupport g)
	{
		setShapes.addAll(g.setShapes);
	}

	/**
	 * Gets all geometrical elements belonging to current mesh.
	 *
	 * @return  the list of elements
	 */
	public Collection getShapes()
	{
		return setShapes;
	}

	/**
	 * Adds an hypothesis to a submesh.
	 *
	 * @param  h  hypothesis
	 */
	public void setHypothesis(Hypothesis h)
	{
		BCADGraph graph = model.getGraph();
		for (Iterator it = setShapes.iterator(); it.hasNext(); )
		{
			CADShape s = (CADShape) it.next();
			BCADGraphCell c = graph.cadToGraphCell(s);
			assert c != null : s;
			c.setHypothesis(h);
		}
	}

	// Returns an iterator on all geometrical elements of dimension d
	public Iterator shapesExplorer(final int d)
	{
		return new Iterator()
		{
			private Class sample = BCADGraph.classTypeArray[d];
			private Iterator its = setShapes.iterator();
			private final BCADGraph graph = model.getGraph();
			private CADShape cur = null;
			private CADShape next = null;
			private boolean initialized = false;
			public boolean hasNext()
			{
				if (cur != next && next != null)
					return true;
				if (!its.hasNext())
					return false;
				next = (CADShape) its.next();
				while (next != null && !sample.isInstance(next))
				{
					if (!its.hasNext())
						return false;
					next = (CADShape) its.next();
				}
				return next != null;
			}
			public Object next()
			{
				if (!initialized)
				{
					hasNext();
					initialized = true;
				}
				else if (cur == next)
					hasNext();
				cur = next;
				return graph.cadToGraphCell(cur);
			}
			public void remove()
			{
			}
		};
	}

	/**
	 * Prints the list of geometrical elements.
	 */
	public void printShapes()
	{
		System.out.println("List of geometrical entities");
		for (int t = BCADGraph.classTypeArray.length - 1; t >= 0; t--)
			BCADGraph.printShapes(t, shapesExplorer(t));
		System.out.println("End list");
	}

	// Sample test
	public static void main(String args[])
	{
		String file = "brep/2cubes.brep";

		BModel model = new BModel(file, "out");
		CADShape shape = model.getCADShape();
		CADShapeBuilder factory = CADShapeBuilder.factory;
		CADExplorer exp = factory.newExplorer();
		CADShape [] solids = new CADShape[2];
		exp.init(shape, CADExplorer.SOLID);
		solids[0] = exp.current();
		exp.next();
		solids[1] = exp.current();
		BSupport submesh1 = model.newMesh();
		submesh1.add(solids[0]);
		BSupport submesh2 = model.newMesh();
		submesh2.add(solids[1]);

		Hypothesis h1 = new Hypothesis();
		h1.setElement("T3");
		h1.setLength(0.3, true);
		h1.setDeflection(0.05);

		Hypothesis h2 = new Hypothesis();
		h2.setElement("T4");
		h2.setLength(0.1);

		submesh1.setHypothesis(h1);
		submesh2.setHypothesis(h2);
		model.printAllHypothesis();
		model.compute();
		model.printShapes();
		// model.printConstraints();
	}
}
