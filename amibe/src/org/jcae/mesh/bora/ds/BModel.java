/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

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
import org.jcae.mesh.xmldata.*;
import org.jcae.mesh.cad.CADShapeBuilder;
import org.jcae.mesh.cad.CADShape;
import org.jcae.mesh.cad.CADVertex;
import org.jcae.mesh.cad.CADEdge;
import org.jcae.mesh.cad.CADFace;
import org.jcae.mesh.mesher.ds.MNode1D;
import org.jcae.mesh.mesher.ds.MMesh1D;
import org.jcae.mesh.mesher.ds.SubMesh1D;
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
	//   User-defined groups
	private LinkedHashSet groups;
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
	private MMesh1D mesh1D;

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

	public BSupport newMesh()
	{
		BSupport ret = new BSupport(this, cad.getFreeIndex());
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
		computeHypothesis();
		computeAlgorithms();
	}

	private void computeHypothesis()
	{
		BCADGraphCell root = cad.getRootCell();
		for (int t = 0; t < BCADGraph.classTypeArray.length; t++)
		{
			for (Iterator it = root.shapesExplorer(t); it.hasNext(); )
			{
				BCADGraphCell s = (BCADGraphCell) it.next();
				s.combineHypothesis(t);
			}
		}
	}

	private void computeAlgorithms()
	{
		BCADGraphCell root = cad.getRootCell();
		int cnt = 0;
		// Vertices
		logger.info("Find all vertices");
		for (Iterator it = root.shapesExplorer(BCADGraph.DIM_VERTEX); it.hasNext(); )
		{
			BCADGraphCell s = (BCADGraphCell) it.next();
			if (s.mesh == null)
				s.mesh = (CADVertex) s.getShape();
		}
		// Edges
		logger.info("Discretize edges");
		for (Iterator it = root.uniqueShapesExplorer(BCADGraph.DIM_EDGE); it.hasNext(); )
		{
			BCADGraphCell s = (BCADGraphCell) it.next();
			if (!s.hasConstraints())
				continue;
			s.discretize();
			BinaryWriter.writeCADEdge(s, xmlDir+File.separator+dir1d);
		}
		for (Iterator it = root.shapesExplorer(BCADGraph.DIM_EDGE); it.hasNext(); )
		{
			BCADGraphCell s = (BCADGraphCell) it.next();
			if (s.getReversed() != null && s.mesh == null)
				s.mesh = s.getReversed().mesh;
		}
		mesh1D = new MMesh1D(this);
		// Faces
		logger.info("Discretize faces");
		cnt = 0;
		computeVertexReferences();
		updateNodeLabels();
		int nrFaces = 0;
		for (Iterator it = root.uniqueShapesExplorer(BCADGraph.DIM_FACE); it.hasNext(); )
		{
			BCADGraphCell s = (BCADGraphCell) it.next();
			if (s.hasConstraints())
				nrFaces++;
		}
		int indexShape = 0;
		for (Iterator it = root.uniqueShapesExplorer(BCADGraph.DIM_FACE); it.hasNext(); )
		{
			BCADGraphCell s = (BCADGraphCell) it.next();
			indexShape++;
			if (!s.hasConstraints())
				continue;
			cnt++;
			logger.info("Face "+cnt+"/"+nrFaces);
			s.mesh1D = mesh1D;
			s.discretize();
			BinaryWriter.writeCADFace(s, xmlDir+File.separator+dir2d);
		}
		// Solids
		logger.info("Discretize solids");
		int nrSolids = 0;
		for (Iterator it = root.shapesExplorer(BCADGraph.DIM_SOLID); it.hasNext(); )
		{
			BCADGraphCell s = (BCADGraphCell) it.next();
			if (!s.hasConstraints())
				continue;
			nrSolids++;
		}
		cnt = 0;
		for (Iterator it = root.shapesExplorer(BCADGraph.DIM_SOLID); it.hasNext(); )
		{
			BCADGraphCell s = (BCADGraphCell) it.next();
			if (!s.hasConstraints())
				continue;
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
		BCADGraphCell root = cad.getRootCell();
		for (Iterator ite = root.shapesExplorer(BCADGraph.DIM_EDGE); ite.hasNext(); )
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
		for (Iterator ite = root.shapesExplorer(BCADGraph.DIM_EDGE); ite.hasNext(); )
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
		BCADGraphCell root = cad.getRootCell();
		int nVertex = 0;
		for (Iterator itn = root.uniqueShapesExplorer(BCADGraph.DIM_VERTEX); itn.hasNext(); itn.next())
			nVertex++;
		THashMap vertex2Ref = new THashMap(nVertex);
		for (Iterator itn = root.uniqueShapesExplorer(BCADGraph.DIM_VERTEX); itn.hasNext(); )
		{
			BCADGraphCell s = (BCADGraphCell) itn.next();
			vertex2Ref.put(s.getShape(), new ArrayList());
		}
		for (Iterator ite = root.shapesExplorer(BCADGraph.DIM_EDGE); ite.hasNext(); )
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
		
		for (Iterator itn = root.uniqueShapesExplorer(BCADGraph.DIM_VERTEX); itn.hasNext(); )
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
