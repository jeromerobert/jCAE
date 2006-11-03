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


package org.jcae.mesh.constraints;

import java.util.ArrayList;
import java.util.Iterator;
import gnu.trove.THashMap;
import gnu.trove.THashSet;

import org.apache.log4j.Logger;
import org.jcae.mesh.cad.CADShape;
import org.jcae.mesh.cad.CADShapeBuilder;
import org.jcae.mesh.cad.CADExplorer;
import org.jcae.mesh.cad.CADVertex;

public class Mesh
{
	private static Logger logger=Logger.getLogger(Mesh.class);
	// Tree parent
	private Mesh parent = null;
	// All meshes from the same root share the same map
	private THashMap cadShapeToMeshShape;
	// List of all hyposthesis
	private ArrayList allHypothesis = null;
	// List of constraints applied to this Mesh
	private ArrayList constraints = new ArrayList();
	private Constraint resultConstraint = null;
	// List of CADShape
	private ArrayList listShapes = new ArrayList();
	private THashSet setShapes = new THashSet();

	private static int shapeTypeArray[] = { CADExplorer.VERTEX, CADExplorer.EDGE, CADExplorer.FACE, CADExplorer.SOLID};
	private static Class classTypeArray[] = new Class[shapeTypeArray.length];
	static { try {
		int i = 0;
		classTypeArray[i++] = Class.forName("org.jcae.mesh.cad.CADVertex");
		classTypeArray[i++] = Class.forName("org.jcae.mesh.cad.CADEdge");
		classTypeArray[i++] = Class.forName("org.jcae.mesh.cad.CADFace");
		classTypeArray[i++] = Class.forName("org.jcae.mesh.cad.CADSolid");
	} catch (Exception ex) {}};

	/**
	 * Creates a root mesh.
	 */
	public Mesh ()
	{
		parent = this;
		cadShapeToMeshShape = new THashMap();
		allHypothesis = new ArrayList();
	}
	
	private Mesh (Mesh p)
	{
		parent = p;
		cadShapeToMeshShape = parent.cadShapeToMeshShape;
		allHypothesis = parent.allHypothesis;
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
		CADShapeBuilder factory = CADShapeBuilder.factory;
		CADExplorer exp = factory.newExplorer();
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
	private Iterator subshapeIterator(final int d)
	{
		return new Iterator()
		{
			private THashSet seen = new THashSet();
			private CADShapeBuilder factory = CADShapeBuilder.factory;
			private CADExplorer exp = factory.newExplorer();
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
		for (int t = 0; t < classTypeArray.length; t++)
		{
			for (Iterator it = subshapeIterator(t); it.hasNext(); )
			{
				CADShape s = (CADShape) it.next();
				Mesh m = cadToMesh(s);
				if (m.resultConstraint == null)
					continue;
				m.resultConstraint.findAlgorithm();
			}
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
	// Sample test
	public static void main(String args[])
	{
		String file = "brep/2cubes.brep";

		CADShapeBuilder factory = CADShapeBuilder.factory;
		CADShape s = factory.newShape(file);
		s.setIds();

		CADExplorer expf = factory.newExplorer();
		expf.init(s, CADExplorer.SOLID);
		CADShape [] solids = new CADShape[2];
		solids[0] = (CADShape) expf.current();
		expf.next();
		solids[1] = (CADShape) expf.current();

		Mesh mesh = new Mesh();
		mesh.add(solids[0]);

		Mesh submesh2 = mesh.createSubMesh();
		submesh2.add(solids[1]);
		submesh2.printShapes();

		Hypothesis h1 = new Hypothesis();
		h1.setElement("T3");
		h1.setLength(300.0);
		h1.setDeflection(0.05);

		Hypothesis h2 = new Hypothesis();
		h2.setElement("T4");
		h2.setLength(100.0);
		h2.setDeflection(0.01);

		mesh.setHypothesis(h1);
		submesh2.setHypothesis(h2);
		mesh.printAllHypothesis();

		mesh.compute();
		submesh2.compute();
		submesh2.printConstraints();
	}
}
