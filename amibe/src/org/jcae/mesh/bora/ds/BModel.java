/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2006, by EADS CRC
    Copyright (C) 2007,2008,2009, by EADS France

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
import org.jcae.mesh.bora.xmldata.Storage;
import org.jcae.mesh.cad.CADShapeFactory;
import org.jcae.mesh.cad.CADShape;
import org.jcae.mesh.cad.CADShapeEnum;
import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Stack;
import java.util.Iterator;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CAD object.
 */
public class BModel
{
	private static final Logger LOGGER = Logger.getLogger(BModel.class.getName());

	//   Next available index
	private static int freeIndex = 1;
	//   Model number
	private final int id;
	//   CAD graph
	private final BCADGraph cad;
	//   List of submeshes
	private final Collection<BSubMesh> submesh = new ArrayList<BSubMesh>();
	//   Geometry file
	private final String cadFile;
	//   Output variables
	private final String xmlDir;
	private String xmlFile = "model";
	private final String xmlBrepDir;
	//   List of all constraints
	private final Collection<Constraint> allConstraints = new LinkedHashSet<Constraint>();
	//   Internal state
	private static enum State {
		INPUT,
		CONSTRAINTS,
		TESSELLATION_0,
		TESSELLATION_1,
		TESSELLATION_2,
		TESSELLATION_3
	}
	private State state = State.INPUT;

	/**
	 * Bind a CAD representation to a disk directory.
	 */
	public BModel (String brep, String out)
	{
		id = freeIndex;
		if (LOGGER.isLoggable(Level.FINE))
			LOGGER.log(Level.FINE, "Building model "+id+" from "+brep+" into "+out);
		freeIndex++;
		CADShapeFactory factory = CADShapeFactory.getFactory();
		xmlDir = out;
		File xmlDirF = new File(xmlDir);
		xmlDirF.mkdirs();
		if(!xmlDirF.exists() || !xmlDirF.isDirectory())
			throw new RuntimeException("Cannot write to "+xmlDir);

		cadFile = (new File(brep)).getAbsoluteFile().getPath();
		xmlFile = brep.substring(brep.lastIndexOf("/"), brep.lastIndexOf(".")) + ".bora.xml";
		xmlBrepDir = relativize(new File(brep).getAbsoluteFile().getParentFile(), new File(xmlDir).getAbsoluteFile()).getPath();
		// CAD graph
		cad = new BCADGraph(this, factory.newShape(brep));
	}
	
	public static void reset()
	{
		freeIndex = 1;
		BDiscretization.nextId = -1;
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

	public String getOutputDir(BDiscretization d)
	{
		String ret = xmlDir+File.separator;
		CADShapeEnum cse = d.getGraphCell().getType();
		if (cse == CADShapeEnum.EDGE)
			ret += "1";
		else if (cse == CADShapeEnum.FACE)
			ret += "2";
		else if (cse == CADShapeEnum.SOLID)
			ret += "3";
		else
			throw new RuntimeException("Invalid CADShapeEnum: "+cse);
		return ret+"d"+d.getId();
	}

	public String getOutputFile()
	{
		return xmlFile;
	}

	public BCADGraph getGraph()
	{
		return cad;
	}

	public Collection<Constraint> getConstraints()
	{
		return allConstraints;
	}

	private static File relativize(File file, File reference)
	{
		File current = file;
		Stack<String> l = new Stack<String>();
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
		if (state != State.INPUT)
			throw new RuntimeException("BModel.newMesh() cannot be called after model has been computed");
		BSubMesh ret = new BSubMesh(this, cad.getFreeIndex());
		submesh.add(ret);
		return ret;
	}

	/**
	 * Get an immutable view of the list of submeshes.
	 * @return the list of submeshes.
	 */
	public Collection<BSubMesh> getSubMeshes()
	{
		return Collections.unmodifiableCollection(submesh);
	}

	void addConstraint(Constraint cons)
	{
		if (state != State.INPUT)
			throw new RuntimeException("Constraints cannot be added after model has been computed");
		allConstraints.add(cons);
	}

	void removeConstraint(Constraint cons) {
		if (state != State.INPUT)
			throw new RuntimeException("Constraints cannot be removed after model has been computed");
		allConstraints.remove(cons);
	}

	/**
  	 * Combines all hypothesis.
	 */
	public void computeConstraints()
	{
		LOGGER.info("Compute constraints");
		BCADGraphCell root = cad.getRootCell();
		for (Iterator<BCADGraphCell> its = root.shapesExplorer(CADShapeEnum.SOLID); its.hasNext(); )
		{
			BCADGraphCell cell = its.next();
			cell.addImplicitConstraints(CADShapeEnum.FACE, true);
			cell.addImplicitConstraints(CADShapeEnum.EDGE, false);
			cell.addImplicitConstraints(CADShapeEnum.VERTEX, false);
		}
		for (Iterator<BCADGraphCell> its = root.shapesExplorer(CADShapeEnum.FACE); its.hasNext(); )
		{
			BCADGraphCell cell = its.next();
			cell.addImplicitConstraints(CADShapeEnum.EDGE, true);
			cell.addImplicitConstraints(CADShapeEnum.VERTEX, false);
		}
		for (Iterator<BCADGraphCell> its = root.shapesExplorer(CADShapeEnum.EDGE); its.hasNext(); )
		{
			BCADGraphCell cell = its.next();
			cell.addImplicitConstraints(CADShapeEnum.VERTEX, true);
		}
		state = State.CONSTRAINTS;
		// Update constraints
		BModelWriter.writeObject(this);
	}

	public void resetConstraints()
	{
		if (state == State.INPUT)
			return;
		LOGGER.info("Reset constraints");
		Map<BSubMesh, Collection<Constraint>> saveConstraints = new HashMap<BSubMesh, Collection<Constraint>>();
		for (BSubMesh s : submesh)
		{
			saveConstraints.put(s, s.getConstraints());
			s.resetConstraints();
		}
		BCADGraphCell root = cad.getRootCell();
		for (CADShapeEnum cse : CADShapeEnum.iterable(CADShapeEnum.VERTEX, CADShapeEnum.COMPOUND))
		{
			for (Iterator<BCADGraphCell> it = root.shapesExplorer(cse); it.hasNext(); )
				it.next().removeDiscretizations();
		}
		state = State.INPUT;
		allConstraints.clear();
		for (BSubMesh s : submesh)
		{
			for (Constraint c : saveConstraints.get(s))
				s.add(c);
		}
	}
	
	public void save() {
		BModelWriter.writeObject(this);
	}

	/**
	 * Combines all hypothesis and computes meshes.
	 */
	public void compute()
	{
		discretizeSolids();
	}

	public void discretizeVertices()
	{
		if (state.compareTo(State.TESSELLATION_0) >= 0)
			return;
		if (state == State.INPUT)
			computeConstraints();

		BCADGraphCell root = cad.getRootCell();
		for (Iterator<BCADGraphCell> its = root.shapesExplorer(CADShapeEnum.VERTEX); its.hasNext(); )
		{
			BCADGraphCell cell = its.next();
			for (BDiscretization d : cell.getDiscretizations())
				d.discretize();
		}
		state = State.TESSELLATION_0;
	}

	public void discretizeEdges()
	{
		if (state.compareTo(State.TESSELLATION_1) >= 0)
			return;
		discretizeVertices();
		LOGGER.config("Discretize edges");
		BCADGraphCell root = cad.getRootCell();
		for (Iterator<BCADGraphCell> its = root.shapesExplorer(CADShapeEnum.EDGE); its.hasNext(); )
		{
			BCADGraphCell cell = its.next();
			for (BDiscretization d : cell.getDiscretizations())
			{
				d.discretize();
				Storage.writeEdge(d, getOutputDir(d));
			}
		}
		state = State.TESSELLATION_1;
	}

	public void discretizeFaces()
	{
		if (state.compareTo(State.TESSELLATION_2) >= 0)
			return;
		discretizeEdges();
		LOGGER.config("Discretize faces");
		BCADGraphCell root = cad.getRootCell();
		for (Iterator<BCADGraphCell> its = root.shapesExplorer(CADShapeEnum.FACE); its.hasNext(); )
		{
			BCADGraphCell cell = its.next();
			for (BDiscretization d : cell.getDiscretizations())
			{
				d.discretize();
				Storage.writeFace(d, getOutputDir(d));
			}
		}
		state = State.TESSELLATION_2;
	}

	public void discretizeSolids()
	{
		if (state.compareTo(State.TESSELLATION_3) >= 0)
			return;
		discretizeFaces();
		LOGGER.config("Discretize solids");
		BCADGraphCell root = cad.getRootCell();
		for (Iterator<BCADGraphCell> its = root.shapesExplorer(CADShapeEnum.SOLID); its.hasNext(); )
		{
			BCADGraphCell cell = its.next();
			for (BDiscretization d : cell.getDiscretizations())
			{
				d.discretize();
				Storage.writeSolid(d, getOutputDir(d));
			}
		}
		state = State.TESSELLATION_3;
	}

	/**
	 * Prints all hypothesis applied to any submesh.
	 */
	public void printAllHypothesis()
	{
		System.out.println("List of hypothesis");
		for (Constraint cons : allConstraints)
		{
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
		StringBuilder indent = new StringBuilder();
		for (CADShapeEnum cse : CADShapeEnum.iterable(CADShapeEnum.VERTEX, CADShapeEnum.COMPOUND))
		{
			String tab = indent.toString();
			for (Iterator<BCADGraphCell> it = root.shapesExplorer(cse); it.hasNext(); )
			{
				BCADGraphCell cell = it.next();
				boolean first = true;
				for (BDiscretization d : cell.getDiscretizations())
				{
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
	 * Prints the constraints applied to a given submesh.
	 */
	public void printConstraints(BSubMesh sm)
	{
		System.out.println("List of constraints applied on submesh "+sm.getId());
		BCADGraphCell root = cad.getRootCell();
		StringBuilder indent = new StringBuilder();
		for (CADShapeEnum cse : CADShapeEnum.iterable(CADShapeEnum.VERTEX, CADShapeEnum.COMPOUND))
		{
			String tab = indent.toString();
			for (Iterator<BCADGraphCell> it = root.shapesExplorer(cse); it.hasNext(); )
			{
				BCADGraphCell cell = it.next();
				for (BDiscretization d : cell.getDiscretizations())
				{
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

	/**
	 * Print all discretizations.
	 */
	@SuppressWarnings("unused")
	private void printDiscretizations()
	{
		System.out.println("List of discretizations");
		BCADGraphCell root = cad.getRootCell();
		StringBuilder indent = new StringBuilder();
		for (CADShapeEnum cse : CADShapeEnum.iterable(CADShapeEnum.VERTEX, CADShapeEnum.COMPOUND))
		{
			CADShapeEnum normalChildType;
			if (cse == CADShapeEnum.EDGE)
				normalChildType = CADShapeEnum.VERTEX;
			else if (cse == CADShapeEnum.FACE)
				normalChildType = CADShapeEnum.EDGE;
			else if (cse == CADShapeEnum.SOLID)
				normalChildType = CADShapeEnum.FACE;
			else
				normalChildType = CADShapeEnum.VERTEX;

			if ((cse == CADShapeEnum.WIRE) || (cse == CADShapeEnum.SHELL) ||
			    (cse == CADShapeEnum.COMPSOLID) || (cse == CADShapeEnum.COMPOUND))
			    continue;

			String tab = indent.toString();
			for (Iterator<BCADGraphCell> itp = root.shapesExplorer(cse); itp.hasNext(); )
			{
				BCADGraphCell pcell = itp.next();
				System.out.println(tab+"Discretizations of shape "+pcell);
				for (BDiscretization pd : pcell.getDiscretizations())
				{
					if (pcell == pd.getGraphCell())
						System.out.println(tab+"    + "+"Used discretization: "+pd.getId()+" with orientation: forward;  element type : "+pd.getConstraint().getHypothesis().getElement()+" constraint : "+pd.getConstraint().getId()+" submesh list : "+pd.getSubmesh());
					else if (pcell == pd.getGraphCell().getReversed() )
						System.out.println(tab+"    + "+"Used discretization: "+pd.getId()+" with orientation: reversed; element type : "+pd.getConstraint().getHypothesis().getElement()+" constraint : "+pd.getConstraint().getId()+" submesh list : "+pd.getSubmesh());
					else
						throw new RuntimeException("Invalid discretization "+pd+" on shape "+pcell);

 					if (cse != CADShapeEnum.VERTEX)
 					{
 						System.out.println(tab+"    + "+"    + "+"discretizations of boundary shapes: ");
						for (Iterator<BCADGraphCell> itc = pcell.shapesExplorer(normalChildType); itc.hasNext(); )
						{
							BCADGraphCell ccell = itc.next();
							for (BDiscretization cd : ccell.getDiscretizations())
							{
								if (pd.contained(cd))
								{
									if (ccell == cd.getGraphCell())
										System.out.println(tab+"    + "+"    + "+"    + "+"Used discretization: "+cd.getId()+" with orientation: forward;  element type : "+cd.getConstraint().getHypothesis().getElement()+" constraint : "+cd.getConstraint().getId()+" submesh list : "+cd.getSubmesh());
									else if (ccell == cd.getGraphCell().getReversed() )
										System.out.println(tab+"    + "+"    + "+"    + "+"Used discretization: "+cd.getId()+" with orientation: reversed; element type : "+cd.getConstraint().getHypothesis().getElement()+" constraint : "+cd.getConstraint().getId()+" submesh list : "+cd.getSubmesh());
									else
										throw new RuntimeException("Invalid discretization "+cd+" on shape "+ccell);
								}

							}
						}
						boolean first = true;
 						if (cse != CADShapeEnum.EDGE)
						{
							for (CADShapeEnum ccse : CADShapeEnum.iterable(CADShapeEnum.VERTEX, normalChildType))
							{
								for (Iterator<BCADGraphCell> itc = pcell.shapesIterator(); itc.hasNext(); )
								{
									BCADGraphCell ccell = itc.next();
									if (ccell.getType() == ccse)
									{
										for (BDiscretization cd : ccell.getDiscretizations())
										{
											if (pd.contained(cd))
											{
												if (first)
												{
													System.out.println(tab+"    + "+"    + "+"discretizations of additionnal boundary shapes: ");
													first = false;
												}
												if (ccell == cd.getGraphCell())
													System.out.println(tab+"    + "+"    + "+"    + "+"    + "+"Used discretization: "+cd.getId()+" with orientation: forward;  element type : "+cd.getConstraint().getHypothesis().getElement()+" constraint : "+cd.getConstraint().getId()+" submesh list : "+cd.getSubmesh());
												else if (ccell == cd.getGraphCell().getReversed() )
													System.out.println(tab+"    + "+"    + "+"    + "+"    + "+"Used discretization: "+cd.getId()+" with orientation: reversed; element type : "+cd.getConstraint().getHypothesis().getElement()+" constraint : "+cd.getConstraint().getId()+" submesh list : "+cd.getSubmesh());
												else
													throw new RuntimeException("Invalid discretization "+cd+" on shape "+ccell);
											}
										}
									}
								}
							}
						}
					}
				}
			}
			indent.append("  ");
		}
		System.out.println("End list");
	}

	/**
	 * Prints the list of geometrical elements.
	 */
	@SuppressWarnings("unused")
	private void printShapes()
	{
		cad.printShapes();
	}

}
