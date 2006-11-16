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

import org.jcae.mesh.bora.xmldata.BModelWriter;
import org.jcae.mesh.xmldata.*;
import org.jcae.mesh.cad.CADShapeBuilder;
import org.jcae.mesh.cad.CADShape;
import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Stack;
import java.util.Iterator;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
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
	private ArrayList submesh = new ArrayList();
	//   Geometry file
	private String cadFile;
	//   Output variables
	private String xmlDir;
	private String xmlFile = "model";
	private String xmlBrepDir;
	//   Directory name for meshes
	private static String dir1d = "1d";
	private static String dir2d = "2d";
	private static String dir3d = "3d";
	//   List of all hyposthesis
	public Collection allHypothesis = new LinkedHashSet();

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

	public CADShape getCADShape()
	{
		return cad.getRootCell().getShape();
	}

	public String getOutputDir()
	{
		return xmlDir;
	}

	public String getOutputFile()
	{
		return xmlFile;
	}

	public String get1dDir()
	{
		return dir1d;
	}

	public String get2dDir()
	{
		return dir2d;
	}

	public String get3dDir()
	{
		return dir3d;
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

	/**
	 * Combines all hypothesis and computes meshes.
	 */
	public void compute()
	{
		// Compute all constraints
		BCADGraphCell root = cad.getRootCell();
		for (int t = 0; t < BCADGraph.classTypeArray.length; t++)
		{
			for (Iterator it = root.shapesExplorer(t); it.hasNext(); )
			{
				BCADGraphCell s = (BCADGraphCell) it.next();
				s.combineHypothesis(t);
			}
		}
		for (Iterator it = root.shapesExplorer(BCADGraph.DIM_VERTEX); it.hasNext(); )
		{
			BCADGraphCell s = (BCADGraphCell) it.next();
			if (s.mesh == null)
				s.mesh = s.getShape();
		}
		logger.debug("Discretize edges");
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
	}

	/**
	 * Prints all hypothesis applied to any submesh.
	 */
	public void printAllHypothesis()
	{
		System.out.println("List of hypothesis");
		for (Iterator it = allHypothesis.iterator(); it.hasNext(); )
		{
			Hypothesis h = (Hypothesis) it.next();
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
		String indent = "";
		for (int t = BCADGraph.classTypeArray.length - 1; t >= 0; t--)
		{
			for (Iterator it = root.shapesExplorer(t); it.hasNext(); )
			{
				BCADGraphCell s = (BCADGraphCell) it.next();
				System.out.println(indent+"Shape "+s);
				s.printConstraints(indent+"    + ");
			}
			indent += "  ";
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
