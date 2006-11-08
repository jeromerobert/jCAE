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

import org.jcae.mesh.xmldata.*;
import org.jcae.mesh.mesher.ds.*;
import org.jcae.mesh.cad.CADShape;
import org.jcae.mesh.cad.CADShapeBuilder;
import org.jcae.mesh.cad.CADExplorer;
import org.jcae.mesh.cad.CADVertex;
import org.jcae.mesh.cad.CADEdge;
import org.jcae.mesh.cad.CADFace;
import org.jcae.mesh.cad.CADSolid;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import java.io.File;

import org.apache.log4j.Logger;

public class Mesh
{
	private static Logger logger=Logger.getLogger(Mesh.class);
	// The following members are shared between all instances
	// of the same Mesh root
	//   Tree parent
	private Mesh parent = null;
	//   Map between topological elements and Mesh
	private THashMap cadShapeToMeshShape;
	//   Global shape
	private CADShape shape;
	//   Geometry file
	private String brepFile;
	//   Output variables
	private String xmlDir;
	private String xmlBrepDir;

	// Local members
	// List of all hyposthesis
	private ArrayList allHypothesis = null;
	// List of constraints applied to this Mesh
	private ArrayList constraints = new ArrayList();
	private Constraint resultConstraint = null;
	// List of CADShape
	private ArrayList listShapes = new ArrayList();
	private THashSet setShapes = new THashSet();
	// 
	public Object mesh = null;
	public MMesh1D mesh1D;

	private static int shapeTypeArray[] = { CADExplorer.VERTEX, CADExplorer.EDGE, CADExplorer.FACE, CADExplorer.SOLID};
	private static Class classTypeArray[] = { CADVertex.class, CADEdge.class, CADFace.class, CADSolid.class};

	/**
	 * Creates a root mesh.
	 */
	public Mesh (String brep, String out)
	{
		parent = this;
		cadShapeToMeshShape = new THashMap();
		allHypothesis = new ArrayList();
		xmlDir = out;
		File xmlDirF = new File(xmlDir);
		xmlDirF.mkdirs();
		if(!xmlDirF.exists() || !xmlDirF.isDirectory())
		{
			System.out.println("Cannot write to "+xmlDir);
			return;
		}

		brepFile = (new File(brep)).getName();
		xmlBrepDir = relativize(new File(brep).getAbsoluteFile().getParentFile(), new File(xmlDir).getAbsoluteFile()).getPath();

		CADShapeBuilder factory = CADShapeBuilder.factory;
		shape = factory.newShape(brep);
		shape.setIds();
		add(shape);
	}
	
	private Mesh (Mesh p)
	{
		parent = p;
		cadShapeToMeshShape = parent.cadShapeToMeshShape;
		shape = parent.shape;
		allHypothesis = parent.allHypothesis;
		xmlDir = parent.xmlDir;
		brepFile = parent.brepFile;
		xmlBrepDir = parent.xmlBrepDir;
	}
	
	public String getCADFile()
	{
		return brepFile;
	}

	public String getOutputDir()
	{
		return xmlDir;
	}

	/**
	 * Creates a submesh of current mesh.
	 */
	public Mesh createSubMesh()
	{
		return new Mesh(this);
	}

	/*
	public CADShape [] explode(String type)
	{
		CADShapeBuilder factory = CADShapeBuilder.factory;
		CADExplorer exp = factory.newExplorer();
		THashSet seen = new THashSet();
		CADShape [] ret = new CADShape[10];
		int index = 0;
		int cadType = -1;
		if (type.equals("SOLID"))
			cadType = CADExplorer.SOLID;
		else if (type.equals("FACE"))
			cadType = CADExplorer.FACE;
		else if (type.equals("EDGE"))
			cadType = CADExplorer.EDGE;
		for (Iterator it = listShapes.iterator(); it.hasNext(); )
		{
			CADShape s = (CADShape) it.next();
			for (exp.init(s, cadType); exp.more(); exp.next())
			{
				CADShape sub = exp.current();
				if (!sub.isOrientationForward())
					sub = sub.reversed();
				if (seen.contains(sub))
					continue;
				seen.add(sub);
				if (index == ret.length)
				{
					CADShape [] temp = new CADShape[index+10];
					System.arraycopy(ret, 0, temp, 0, index);
					ret = temp;
				}
				ret[index] = sub;
				index++;
			}
		}
		return ret;
	}
	*/

	/**
	 * Adds a shape to current mesh.
	 *
	 * @param s  shape
	 */
	public Mesh add(CADShape s)
	{
		logger.debug("add: "+s);
		if (!s.isOrientationForward())
			s = s.reversed();
		if (!setShapes.contains(s))
		{
			listShapes.add(s);
			setShapes.add(s);
			if (!cadShapeToMeshShape.contains(s))
			{
				cadShapeToMeshShape.put(s, this);
				logger.debug("  Add submesh: "+s+" "+this);
			}
		}
		CADShapeBuilder factory = CADShapeBuilder.factory;
		CADExplorer exp = factory.newExplorer();
		for (int t = 0; t < shapeTypeArray.length; t++)
		{
			for (exp.init(s, shapeTypeArray[t]); exp.more(); exp.next())
			{
				CADShape sub = exp.current();
				// By convention, store forward shapes in
				// shape map.
				if (!sub.isOrientationForward())
					sub = sub.reversed();
				if (setShapes.contains(sub))
					continue;
				listShapes.add(sub);
				setShapes.add(sub);
				if (!cadShapeToMeshShape.contains(sub))
				{
					Mesh submesh = new Mesh(this);
					cadShapeToMeshShape.put(sub, submesh);
					logger.debug("  Add submesh: "+sub+" "+submesh);
				}
			}
		}
		return this;
	}

	/**
	 * Gets all geometrical elements belonging to current mesh.
	 *
	 * @return  the list of elements
	 */
	public ArrayList getShapes()
	{
		return listShapes;
	}

	/**
	 * Gets the submesh of a shape.
	 *
	 * @return  the submesh
	 */
	public Mesh cadToMesh(CADShape s)
	{
		if (!s.isOrientationForward())
			s = s.reversed();
		Mesh m = (Mesh) cadShapeToMeshShape.get(s);
		assert m != null;
		return m;
	}

	/**
	 * Adds an hypothesis to a submesh.
	 *
	 * @param  h  hypothesis
	 */
	public void setHypothesis(Hypothesis h)
	{
		h.lock();
		allHypothesis.add(h);
		MeshHypothesis c = new MeshHypothesis(this, h);
		THashSet seen = new THashSet();
		for (int t = 0; t < classTypeArray.length; t++)
		{
			for (Iterator it = subshapeIterator(t); it.hasNext(); )
			{
				CADShape s = (CADShape) it.next();
				Mesh m = cadToMesh(s);
				m.constraints.add(c);
			}
		}
	}

	// Returns an iterator on all geometrical elements of dimension d
	public Iterator subshapeIterator(final int d)
	{
		return new Iterator()
		{
			private Class sample = classTypeArray[d];
			private Iterator its = listShapes.iterator();
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
				return cur;
			}
			public void remove()
			{
			}
		};
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
		for (int t = 0; t < classTypeArray.length; t++)
		{
			for (Iterator it = subshapeIterator(t); it.hasNext(); )
			{
				CADShape s = (CADShape) it.next();
				Mesh m = cadToMesh(s);
				m.resultConstraint = Constraint.combineAll(m.constraints, t);
			}
		}
	}

	private void computeAlgorithms()
	{
		int cnt = 0;
		// Vertices
		logger.info("Find all vertices");
		for (Iterator it = subshapeIterator(0); it.hasNext(); )
		{
			CADShape s = (CADShape) it.next();
			Mesh m = cadToMesh(s);
			if (m.mesh == null)
				m.mesh = (CADVertex) s;
		}
		// Edges
		logger.info("Discretize edges");
		for (Iterator it = subshapeIterator(1); it.hasNext(); )
		{
			CADShape s = (CADShape) it.next();
			Mesh m = cadToMesh(s);
			if (m.resultConstraint == null)
				continue;
			cnt++;
			m.resultConstraint.applyAlgorithm(m, s, cnt);
		}
		updateNodeLabels();
		String xmlFile = "jcae1d";
		Bora1DWriter.writeObject(this, xmlDir, xmlFile, xmlBrepDir, brepFile);
		mesh1D = Bora1DReader.readObject(this, xmlFile);
		// Faces
		logger.info("Discretize faces");
		cnt = 0;
		duplicateEdges();
		updateNodeLabels();
		int nrFaces = 0;
		for (Iterator it = subshapeIterator(2); it.hasNext(); )
		{
			CADShape s = (CADShape) it.next();
			Mesh m = cadToMesh(s);
			if (m.resultConstraint == null)
				continue;
			nrFaces++;
		}
		for (Iterator it = subshapeIterator(2); it.hasNext(); )
		{
			CADShape s = (CADShape) it.next();
			Mesh m = cadToMesh(s);
			if (m.resultConstraint == null)
				continue;
			cnt++;
			logger.info("Face "+cnt+"/"+nrFaces);
			m.mesh1D = mesh1D;
			m.resultConstraint.applyAlgorithm(m, s, cnt);
		}
		// logger.info("Discretize volumes");
		MeshToMMesh3DConvert m2dTo3D = new MeshToMMesh3DConvert(xmlDir);
		cnt = 0;
		for (Iterator it = subshapeIterator(2); it.hasNext(); it.next())
		{
			cnt++;
			xmlFile = "jcae2d."+cnt;
			m2dTo3D.computeRefs(xmlFile);
		}
		m2dTo3D.initialize("jcae3d", false);
		cnt = 0;
		for (Iterator it = subshapeIterator(2); it.hasNext(); )
		{
			CADFace F = (CADFace) it.next();
			cnt++;
			xmlFile = "jcae2d."+cnt;
			m2dTo3D.convert(xmlFile, cnt, F);
		}
		m2dTo3D.finish();
	}

	/**
	 * Update node labels.
	 */
	private void updateNodeLabels()
	{
		logger.debug("Update node labels");
		//  Resets all labels
		for (Iterator ite = subshapeIterator(1); ite.hasNext(); )
		{
			CADEdge E = (CADEdge) ite.next();
			Mesh m = cadToMesh(E);
			SubMesh1D submesh1d = (SubMesh1D) m.mesh;
			for (Iterator itn = submesh1d.getNodesIterator(); itn.hasNext(); )
			{
				MNode1D n = (MNode1D) itn.next();
				n.setLabel(0);
			}
		}
		int i = 0;
		for (Iterator ite = subshapeIterator(1); ite.hasNext(); )
		{
			CADEdge E = (CADEdge) ite.next();
			Mesh m = cadToMesh(E);
			SubMesh1D submesh1d = (SubMesh1D) m.mesh;
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
	private void duplicateEdges()
	{
		logger.debug("Compute vertex references");
		//  For each topological vertex, compute the list of
		//  MNode1D objects which are bound to this vertex.
		int nVertex = 0;
		for (Iterator itn = subshapeIterator(0); itn.hasNext(); itn.next())
			nVertex++;
		THashMap vertex2Ref = new THashMap(nVertex);
		for (Iterator itn = subshapeIterator(0); itn.hasNext(); )
			vertex2Ref.put(itn.next(), new ArrayList());
		for (Iterator ite = subshapeIterator(1); ite.hasNext(); )
		{
			CADEdge E = (CADEdge) ite.next();
			Mesh m = cadToMesh(E);
			SubMesh1D submesh1d = (SubMesh1D) m.mesh;
			Iterator itn = submesh1d.getNodesIterator();
			while (itn.hasNext())
			{
				MNode1D pt = (MNode1D) itn.next();
				if (null != pt.getCADVertex())
					((ArrayList) vertex2Ref.get(pt.getCADVertex())).add(pt);
			}
		}
		
		THashSet seen = new THashSet();
		for (Iterator itn = subshapeIterator(0); itn.hasNext(); )
		{
			CADVertex V = (CADVertex) itn.next();
			if (seen.contains(V))
				continue;
			seen.add(V);
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
	 * Prints the list of geometrical elements.
	 */
	public void printShapes()
	{
		CADShapeBuilder factory = CADShapeBuilder.factory;
		CADExplorer exp = factory.newExplorer();
		System.out.println("List of geometrical entities");

		for (int t = classTypeArray.length - 1; t >= 0; t--)
		{
			for (Iterator it = subshapeIterator(t); it.hasNext(); )
			{
				CADShape s = (CADShape) it.next();
				if (t == 0)
				{
					CADVertex v = (CADVertex) s;
					double [] coord = v.pnt();
					System.out.println("Shape "+v.getId()+" "+v+ " ("+coord[0]+", "+coord[1]+", "+coord[2]+")");
				}
				else
				{
					System.out.println("Shape "+s.getId()+" "+s+":");
					for (exp.init(s, shapeTypeArray[t-1]); exp.more(); exp.next())
					{
						CADShape sub = exp.current();
						System.out.println(" +> shape "+sub.getId()+" "+sub);
					}
				}
			}
		}
		System.out.println("End list");
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
		String indent = "";
		for (int t = classTypeArray.length - 1; t >= 0; t--)
		{
			for (Iterator it = subshapeIterator(t); it.hasNext(); )
			{
				CADShape s = (CADShape) it.next();
				System.out.println(indent+"Shape "+s.getId()+" "+s);
				Mesh m = cadToMesh(s);
				if (m.resultConstraint == null)
					continue;
				for (Iterator ita = m.constraints.iterator(); ita.hasNext(); )

					System.out.println(indent+"    + "+ita.next());
				System.out.println(indent+"  Total constraint "+m.resultConstraint);
				if (m.resultConstraint.algo != null)
					System.out.println(indent+"  Algo "+m.resultConstraint.algo);
			}
			indent += "  ";
		}
		System.out.println("End list");
	}

	/*
	public String toString()
	{
		if (rootShape == null)
		{
			return ""+explicitHypothesis;
		}
		String ret = "Root shape : "+rootShape+"\n";
		if (listShapes.size() > 0)
		{
			ret += "List of shapes: ";
			for (Iterator it = listShapes.iterator(); it.hasNext(); )
			{
				CADShape s = (CADShape) it.next();
				ret += "   "+s+"\n";
			}
			ret += "\n";
		}
		return ret+explicitHypothesis;
	}
	*/

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

	// Sample test
	public static void main(String args[])
	{
		String file = "brep/2cubes.brep";

		Mesh mesh = new Mesh(file, "out");
		Iterator it = mesh.subshapeIterator(3);
		CADShape [] solids = new CADShape[2];
		solids[0] = (CADShape) it.next();
		solids[1] = (CADShape) it.next();

		Mesh submesh1 = mesh.createSubMesh();
		submesh1.add(solids[0]);

		Mesh submesh2 = mesh.createSubMesh();
		submesh2.add(solids[1]);
		mesh.printShapes();

		Hypothesis h1 = new Hypothesis();
		h1.setElement("T3");
		h1.setLength(0.2);
		//h1.setDeflection(0.05);

		Hypothesis h2 = new Hypothesis();
		h2.setElement("T3");
		h2.setLength(0.02);
		//h2.setDeflection(0.01);

		submesh1.setHypothesis(h1);
		submesh2.setHypothesis(h2);
		mesh.printAllHypothesis();

		//mesh.printConstraints();
		mesh.compute();
		//submesh2.compute();
		//submesh2.printConstraints();
		new UNVConverter(mesh.xmlDir).writeMESH("main.mesh");
	}
}
