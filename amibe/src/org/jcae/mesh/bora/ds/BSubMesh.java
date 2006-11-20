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

import org.jcae.mesh.bora.xmldata.*;
import org.jcae.mesh.cad.CADShape;
import org.jcae.mesh.cad.CADShapeBuilder;
import org.jcae.mesh.cad.CADShapeEnum;
import org.jcae.mesh.cad.CADExplorer;
import org.jcae.mesh.cad.CADVertex;
import org.jcae.mesh.mesher.ds.MNode1D;
import org.jcae.mesh.mesher.ds.MMesh1D;
import org.jcae.mesh.mesher.ds.SubMesh1D;

import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import gnu.trove.THashMap;
import gnu.trove.TIntObjectHashMap;
import java.util.Iterator;
import java.io.File;

import org.apache.log4j.Logger;

public class BSubMesh
{
	private static Logger logger=Logger.getLogger(BSubMesh.class);
	//   Model
	private BModel model;
	//   First free index
	private static int freeIndex = 0;
	//   Unique identitier
	private int id = -1;
	//   List of shapes added to this BSubMesh
	private Collection setTopShapes = new LinkedHashSet();
	//   List of children
	private Collection setCells = new LinkedHashSet();
	private THashMap mapShapeToSubElement = new THashMap();
	//   Tessellation
	public Object mesh = null;
	private boolean output1d = false;
	private boolean output2d = false;
	private boolean output3d = false;

	private static class SubElement
	{
		private BCADGraphCell cell;
		private boolean isVisible = true;
		private Object mesh;
		private SubElement(BCADGraphCell that)
		{
			cell = that;
		}
	}

	/**
	 * Creates a root mesh.
	 */
	public BSubMesh(BModel m, int offset)
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
	 * Adds a shape to current submesh
	 *
	 * @param s  shape
	 */
	public void add(CADShape s)
	{
		BCADGraphCell c = model.getGraph().getByShape(s);
		setTopShapes.add(c);
		for (Iterator itcse = CADShapeEnum.iterator(CADShapeEnum.VERTEX, CADShapeEnum.COMPOUND); itcse.hasNext(); )
		{
			CADShapeEnum cse = (CADShapeEnum) itcse.next();
			for (Iterator it = c.shapesExplorer(cse); it.hasNext(); )
			{
				BCADGraphCell sub = (BCADGraphCell) it.next();
				CADShape shape = sub.getShape();
				SubElement old = (SubElement) mapShapeToSubElement.get(shape);
				if (old != null && old.cell.getShape().orientation() != shape.orientation())
				{
					old.isVisible = false;
					continue;
				}
				SubElement se = new SubElement(sub);
				setCells.add(sub);
				mapShapeToSubElement.put(shape, se);
			}
		}
	}
	
	/**
	 * Gets all CAD graph cells belonging to current mesh.
	 *
	 * @return  the list of CAD graph cells
	 */
	public Collection getCells()
	{
		return setTopShapes;
	}

	/**
	 * Prints the list of geometrical elements.
	 */
	public void printShapes()
	{
		// We cannot use
		//   BCADGraph.printShapes(t, shapesExplorer(t));
		// here because we want to flag interior elements.
		System.out.println("List of geometrical entities");
		for (Iterator itcse = CADShapeEnum.iterator(CADShapeEnum.VERTEX, CADShapeEnum.COMPOUND); itcse.hasNext(); )
		{
			CADShapeEnum cse = (CADShapeEnum) itcse.next();
			for (Iterator it = shapesExplorer(cse); it.hasNext(); )
			{
				BCADGraphCell sub = (BCADGraphCell) it.next();
				SubElement old = (SubElement) mapShapeToSubElement.get(sub.getShape());
				System.out.println(""+sub+(old.isVisible ? "" : " (*)"));
			}
		}
		System.out.println("End list");
	}

	// Returns an iterator on all geometrical elements of dimension d
	public Iterator shapesExplorer(final CADShapeEnum d)
	{
		return new Iterator()
		{
			private Class sample = d.asClass();
			private Iterator its = setCells.iterator();
			private BCADGraphCell cur = null;
			private BCADGraphCell next = null;
			private boolean initialized = false;
			public boolean hasNext()
			{
				if (cur != next && next != null)
					return true;
				if (!its.hasNext())
					return false;
				next = (BCADGraphCell) its.next();
				while (next != null && !sample.isInstance(next.getShape()))
				{
					if (!its.hasNext())
						return false;
					next = (BCADGraphCell) its.next();
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
				return cur;
			}
			public void remove()
			{
			}
		};
	}

	/**
	 * Adds an hypothesis to a submesh.
	 *
	 * @param  h  hypothesis
	 */
	public void setHypothesis(Hypothesis h)
	{
		CADShapeEnum dim = Constraint.getAlgo(h.getElement()).dim();
		if (dim == null)
			return;
		if (dim == CADShapeEnum.EDGE)
			output1d = true;
		else if (dim == CADShapeEnum.FACE)
			output2d = true;
		else if (dim == CADShapeEnum.SOLID)
			output3d = true;
		for (Iterator it = setCells.iterator(); it.hasNext(); )
		{
			BCADGraphCell c = (BCADGraphCell) it.next();
			c.setHypothesis(h);
		}
	}

	public void computeAlgorithms1d()
	{
		logger.debug("Submesh nr. "+id);
		// Edges
		int nrEdges = 0;
		for (Iterator it = shapesExplorer(CADShapeEnum.EDGE); it.hasNext(); )
		{
			BCADGraphCell s = (BCADGraphCell) it.next();
			if (s.hasConstraints())
				nrEdges++;
		}
		int cnt = 0;
		for (Iterator it = shapesExplorer(CADShapeEnum.EDGE); it.hasNext(); )
		{
			BCADGraphCell s = (BCADGraphCell) it.next();
			cnt++;
			if (!s.hasConstraints())
				continue;
			if (s.mesh != null)
				continue;
			if (s.getReversed() != null && s.getReversed().mesh != null)
			{
				s.mesh = s.getReversed().mesh;
				continue;
			}
			logger.debug("Edge "+cnt+"/"+nrEdges);
			s.discretize();
			Storage.writeEdge(s, model.getOutputDir()+File.separator+model.get1dDir());
		}
	}

	public void computeAlgorithms2d()
	{
		logger.info("Submesh nr. "+id);
		MMesh1D mesh1D = new MMesh1D(model);
		// Faces
		computeVertexReferences();
		updateNodeLabels();
		int nrFaces = 0;
		for (Iterator it = shapesExplorer(CADShapeEnum.FACE); it.hasNext(); )
		{
			BCADGraphCell s = (BCADGraphCell) it.next();
			if (s.hasConstraints())
				nrFaces++;
		}
		int cnt = 0;
		for (Iterator it = shapesExplorer(CADShapeEnum.FACE); it.hasNext(); )
		{
			BCADGraphCell s = (BCADGraphCell) it.next();
			cnt++;
			if (!s.hasConstraints())
				continue;
			if (s.mesh != null)
				continue;
			if (s.getReversed() != null && s.getReversed().mesh != null)
			{
				s.mesh = s.getReversed().mesh;
				continue;
			}
			logger.info("Face "+cnt+"/"+nrFaces);
			s.mesh1D = mesh1D;
			s.discretize();
			Storage.writeFace(s, model.getOutputDir()+File.separator+model.get2dDir());
		}
	}

	public void computeAlgorithms3d()
	{
		// Solids
		logger.info("Submesh nr. "+id);
		int nrSolids = 0;
		for (Iterator it = shapesExplorer(CADShapeEnum.SOLID); it.hasNext(); )
		{
			BCADGraphCell s = (BCADGraphCell) it.next();
			if (!s.hasConstraints())
				continue;
			nrSolids++;
		}
		if (nrSolids == 0)
			return;
		int cnt = 0;
		for (Iterator it = shapesExplorer(CADShapeEnum.SOLID); it.hasNext(); )
		{
			BCADGraphCell s = (BCADGraphCell) it.next();
			if (!s.hasConstraints())
				continue;
			if (s.mesh != null)
				continue;
			if (s.getReversed() != null && s.getReversed().mesh != null)
			{
				s.mesh = s.getReversed().mesh;
				continue;
			}
			cnt++;
			logger.info("Solid "+cnt+"/"+nrSolids);
			s.discretize();
		}
	}

	/**
	 * Update node labels.
	 */
	private void updateNodeLabels()
	{
		logger.debug("Update node labels");
		//  Resets all labels
		BCADGraphCell root = model.getGraph().getRootCell();
		for (Iterator ite = root.shapesExplorer(CADShapeEnum.EDGE); ite.hasNext(); )
		{
			BCADGraphCell s = (BCADGraphCell) ite.next();
			SubMesh1D submesh1d = (SubMesh1D) s.mesh;
			if (submesh1d == null)
				continue;
			for (Iterator itn = submesh1d.getNodesIterator(); itn.hasNext(); )
			{
				MNode1D n = (MNode1D) itn.next();
				n.setLabel(0);
			}
		}
		int i = 0;
		for (Iterator ite = root.shapesExplorer(CADShapeEnum.EDGE); ite.hasNext(); )
		{
			BCADGraphCell s = (BCADGraphCell) ite.next();
			SubMesh1D submesh1d = (SubMesh1D) s.mesh;
			if (submesh1d == null)
				continue;
			for (Iterator itn = submesh1d.getNodesIterator(); itn.hasNext(); )
			{
				MNode1D n = (MNode1D) itn.next();
				if (0 == n.getMaster().getLabel())
				{
					i++;
					n.getMaster().setLabel(i);
				}
			}
		}
	}

	/**
	 * Duplicates edges so that boundary faces are closed.
	 * This method must be used after all 1D algorithms have been applied,
	 * and before any 2D meshing is performed.
	 *
	 */
	private void computeVertexReferences()
	{
		logger.debug("Compute vertex references");
		//  For each topological vertex, compute the list of
		//  MNode1D objects which are bound to this vertex.
		BCADGraphCell root = model.getGraph().getRootCell();
		int nVertex = 0;
		for (Iterator itn = root.uniqueShapesExplorer(CADShapeEnum.VERTEX); itn.hasNext(); itn.next())
			nVertex++;
		THashMap vertex2Ref = new THashMap(nVertex);
		for (Iterator itn = root.uniqueShapesExplorer(CADShapeEnum.VERTEX); itn.hasNext(); )
		{
			BCADGraphCell s = (BCADGraphCell) itn.next();
			vertex2Ref.put(s.getShape(), new ArrayList());
		}
		for (Iterator ite = root.shapesExplorer(CADShapeEnum.EDGE); ite.hasNext(); )
		{
			BCADGraphCell s = (BCADGraphCell) ite.next();
			SubMesh1D submesh1d = (SubMesh1D) s.mesh;
			if (submesh1d == null)
				continue;
			Iterator itn = submesh1d.getNodesIterator();
			while (itn.hasNext())
			{
				MNode1D pt = (MNode1D) itn.next();
				CADVertex V = pt.getCADVertex();
				if (null != V)
					((ArrayList) vertex2Ref.get(V)).add(pt);
			}
		}
		
		for (Iterator itn = root.uniqueShapesExplorer(CADShapeEnum.VERTEX); itn.hasNext(); )
		{
			BCADGraphCell s = (BCADGraphCell) itn.next();
			CADVertex V = (CADVertex) s.getShape();
			ArrayList vnodelist = (ArrayList) vertex2Ref.get(V);
			if (vnodelist.size() <= 1)
				continue;
			// Make sure that all MNode1D objects share the same master.
			MNode1D master = (MNode1D) vnodelist.get(0);
			master.setMaster(null);
			for (int i = 1; i<vnodelist.size(); i++)
				((MNode1D) vnodelist.get(i)).setMaster(master);
		}
	}

	public void writeOutput0d(int offset, TIntObjectHashMap nodeMap)
	{
		if (!output1d && !output2d && !output3d)
			return;
	}
	public void writeOutput1d(int offset, TIntObjectHashMap nodeMap, TIntObjectHashMap edgeMap)
	{
		if (!output1d)
			return;
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
		BSubMesh submesh1 = model.newMesh();
		submesh1.add(solids[0]);
		BSubMesh submesh2 = model.newMesh();
		submesh2.add(solids[1]);

		Hypothesis h1 = new Hypothesis();
		h1.setElement("T3");
		h1.setLength(0.3);
		h1.setDeflection(0.05);

		Hypothesis h2 = new Hypothesis();
		h2.setElement("T4");
		h2.setLength(0.1);

		submesh1.setHypothesis(h1);
		submesh2.setHypothesis(h2);
		model.printAllHypothesis();
		model.compute();
		// model.printConstraints();
	}
}
