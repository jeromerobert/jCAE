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

import org.jcae.mesh.bora.xmldata.BModelWriter;
import org.jcae.mesh.cad.CADShapeBuilder;
import org.jcae.mesh.cad.CADShape;
import org.jcae.mesh.cad.CADShapeEnum;
import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Stack;
import java.util.Iterator;
import java.io.File;

import org.apache.log4j.Logger;

/**
 * CAD object.
 */
public class BModel
{
	private static Logger logger=Logger.getLogger(BModel.class);
	//   Next available index
	private static int freeIndex = 1;
	//   Model number
	private int id;
	//   CAD graph
	private BCADGraph cad;
	//   List of submeshes
	private Collection submesh = new ArrayList();
	//   Geometry file
	private String cadFile;
	//   Output variables
	private String xmlDir;
	private String xmlFile = "model";
	private String xmlBrepDir;
	//   List of all constraints
	private Collection allConstraints = new LinkedHashSet();
	//   Internal state
	private int state = 0;
	//   Valid state values
	private static int INPUT        = 0;
	private static int CONSTRAINTS  = 1;
	private static int TESSELLATION = 2;

	/**
	 * Bind a CAD representation to a disk directory.
	 */
	public BModel (String brep, String out)
	{
		id = freeIndex;
		if (logger.isDebugEnabled())
			logger.debug("Building model "+id+" from "+brep+" into "+out);
		freeIndex++;
		CADShapeBuilder factory = CADShapeBuilder.factory;
		xmlDir = out;
		File xmlDirF = new File(xmlDir);
		xmlDirF.mkdirs();
		if(!xmlDirF.exists() || !xmlDirF.isDirectory())
		{
			System.out.println("Cannot write to "+xmlDir);
			return;
		}

		cadFile = (new File(brep)).getAbsoluteFile().getPath();
		xmlBrepDir = relativize(new File(brep).getAbsoluteFile().getParentFile(), new File(xmlDir).getAbsoluteFile()).getPath();
		// CAD graph
		cad = new BCADGraph(this, factory.newShape(brep));
		// Store CAD graph on disk
		BModelWriter.writeObject(this);
	}
	
	public int getId()
	{
		return id;
	}

	public String getCADFile()
	{
		return cadFile;
	}

	public String getRelativeCADDir()
	{
		return xmlBrepDir;
	}

	public CADShape getCADShape()
	{
		return cad.getRootCell().getShape();
	}

	public String getOutputDir()
	{
		return xmlDir;
	}

	public String getOutputDir(BSubMesh sub)
	{
		return xmlDir+File.separator+"s"+sub.getId();
	}

	public String getOutputFile()
	{
		return xmlFile;
	}

	public BCADGraph getGraph()
	{
		return cad;
	}

	private static File relativize(File file, File reference)
	{
		File current = file;
		Stack l = new Stack();
		while (current != null && !current.equals(reference))
		{
			l.push(current.getName());
			current = current.getParentFile();
		}
		if (l.isEmpty())
			return new File(".");
		else if (current == null)
			return file;
		else
		{
			current = new File(l.pop().toString());
			while(!l.isEmpty())
				current = new File(current, l.pop().toString());
			return current;
		}
	}

	public BSubMesh newMesh()
	{
		if (state != INPUT)
			throw new RuntimeException("BModel.newMesh() cannot be called after model has been computed");
		BSubMesh ret = new BSubMesh(this, cad.getFreeIndex());
		submesh.add(ret);
		return ret;
	}

	/**
	 * Prints the list of geometrical elements.
	 */
	public void printShapes()
	{
		cad.printShapes();
	}

	public void addConstraint(Constraint cons)
	{
		if (state != INPUT)
			throw new RuntimeException("Constraints cannot be added after model has been computed");
		allConstraints.add(cons);
	}

	/**
  	 * Combines all hypothesis.
	 */
	public void computeConstraints()
	{
		BCADGraphCell root = cad.getRootCell();
		for (Iterator its = root.shapesExplorer(CADShapeEnum.SOLID); its.hasNext(); )
		{
			BCADGraphCell cell = (BCADGraphCell) its.next();
			cell.addImplicitConstraints(CADShapeEnum.FACE, true);
			cell.addImplicitConstraints(CADShapeEnum.EDGE, false);
			cell.addImplicitConstraints(CADShapeEnum.VERTEX, false);
		}
		for (Iterator its = root.shapesExplorer(CADShapeEnum.FACE); its.hasNext(); )
		{
			BCADGraphCell cell = (BCADGraphCell) its.next();
			cell.addImplicitConstraints(CADShapeEnum.EDGE, true);
			cell.addImplicitConstraints(CADShapeEnum.VERTEX, false);
		}
		for (Iterator its = root.shapesExplorer(CADShapeEnum.EDGE); its.hasNext(); )
		{
			BCADGraphCell cell = (BCADGraphCell) its.next();
			cell.addImplicitConstraints(CADShapeEnum.VERTEX, true);
		}
		state = CONSTRAINTS;
	}

	/**
	 * Combines all hypothesis and computes meshes.
	 */
	public void compute()
	{
		if (state == INPUT)
			computeConstraints();
		else if (state != CONSTRAINTS)
			throw new RuntimeException("Invalid state: "+state);
		BCADGraphCell root = cad.getRootCell();
		for (Iterator its = root.shapesExplorer(CADShapeEnum.VERTEX); its.hasNext(); )
		{
			BCADGraphCell cell = (BCADGraphCell) its.next();
			for (Iterator itd = cell.discretizationIterator(); itd.hasNext(); )
			{
				BDiscretization d = (BDiscretization) itd.next();
				if (d.getMesh() == null)
					d.setMesh(cell.getShape());
			}
		}
		logger.debug("Discretize edges");
		for (Iterator its = root.shapesExplorer(CADShapeEnum.EDGE); its.hasNext(); )
		{
			BCADGraphCell cell = (BCADGraphCell) its.next();
			for (Iterator itd = cell.discretizationIterator(); itd.hasNext(); )
			{
				BDiscretization d = (BDiscretization) itd.next();
				d.discretize();
			}
		}
		/*
		for (Iterator it = submesh.iterator(); it.hasNext(); )
		{
			BSubMesh sm = (BSubMesh) it.next();
			sm.computeAlgorithms1d();
		}
		logger.info("Discretize faces");
		for (Iterator it = submesh.iterator(); it.hasNext(); )
		{
			BSubMesh sm = (BSubMesh) it.next();
			sm.computeAlgorithms2d();
		}
		logger.info("Discretize solids");
		for (Iterator it = submesh.iterator(); it.hasNext(); )
		{
			BSubMesh sm = (BSubMesh) it.next();
			sm.computeAlgorithms3d();
		}
		state = TESSELLATION;
		*/
	}

	/**
	 * Prints all hypothesis applied to any submesh.
	 */
	public void printAllHypothesis()
	{
		System.out.println("List of hypothesis");
		for (Iterator it = allConstraints.iterator(); it.hasNext(); )
		{
			Constraint cons = (Constraint) it.next();
			Hypothesis h = cons.getHypothesis();
			System.out.println(" + ("+Integer.toHexString(h.hashCode())+") "+h);
		}
		System.out.println("End list");
	}

	/**
	 * Prints the constraints applied to geometrical elements of the current mesh.
	 */
	public void printConstraints()
	{
		System.out.println("List of constraints");
		BCADGraphCell root = cad.getRootCell();
		StringBuffer indent = new StringBuffer();
		for (Iterator itcse = CADShapeEnum.iterator(CADShapeEnum.VERTEX, CADShapeEnum.COMPOUND); itcse.hasNext(); )
		{
			CADShapeEnum cse = (CADShapeEnum) itcse.next();
			String tab = indent.toString();
			for (Iterator it = root.shapesExplorer(cse); it.hasNext(); )
			{
				BCADGraphCell cell = (BCADGraphCell) it.next();
				boolean first = true;
				for (Iterator itd = cell.discretizationIterator(); itd.hasNext(); )
				{
					BDiscretization d = (BDiscretization) itd.next();
					if (first)
						System.out.println(tab+"Shape "+cell);
					first = false;
					System.out.println(tab+"    + "+d);
				}
			}
			indent.append("  ");
		}
		System.out.println("End list");
	}

	/**
	 * Prints the constraints applied to a given submesh
	 */
	public void printConstraints(BSubMesh sm)
	{
		System.out.println("List of constraints applied on submesh "+sm.getId());
		BCADGraphCell root = cad.getRootCell();
		StringBuffer indent = new StringBuffer();
		for (Iterator itcse = CADShapeEnum.iterator(CADShapeEnum.VERTEX, CADShapeEnum.COMPOUND); itcse.hasNext(); )
		{
			CADShapeEnum cse = (CADShapeEnum) itcse.next();
			String tab = indent.toString();
			for (Iterator it = root.shapesExplorer(cse); it.hasNext(); )
			{
				BCADGraphCell cell = (BCADGraphCell) it.next();
				for (Iterator itd = cell.discretizationIterator(); itd.hasNext(); )
				{
					BDiscretization d = (BDiscretization) itd.next();
					if (d.contains(sm))
					{
						System.out.println(tab+"Shape "+cell);
						System.out.println(tab+"    + "+d);
					}
				}
			}
			indent.append("  ");
		}
		System.out.println("End list");
	}

	// Sample test
	public static void main(String args[])
	{
		String file = "brep/2cubes.brep";

		BModel model = new BModel(file, "out");
		model.cad.printShapes();
		BModelWriter.writeObject(model);
	}
}
