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

import org.jcae.mesh.cad.CADShape;
import org.jcae.mesh.cad.CADShapeBuilder;
import org.jcae.mesh.cad.CADShapeEnum;
import org.jcae.mesh.cad.CADExplorer;
import org.jcae.mesh.cad.CADIterator;
import org.jcae.mesh.mesher.ds.MMesh1D;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Iterator;
import gnu.trove.THashSet;
import gnu.trove.THashMap;
import gnu.trove.TObjectObjectProcedure;
import gnu.trove.TObjectHashingStrategy;

import org.apache.log4j.Logger;

/**
 * Graph cell.  This class is a decorator for the CAD graph.
 */
public class BCADGraphCell
{
	private static Logger logger=Logger.getLogger(BCADGraphCell.class);

	// Unique identitier
	private int id = -1;
	// Link to root graph
	private BCADGraph graph;
	// CAD shape
	private CADShape shape;
	// Shape orientation  (0=forward, 1=reversed)
	private int orientation;
	// Link to the reversed shape, if it does exist
	private BCADGraphCell reversed;
	// List of parents
	private Collection parents = new LinkedHashSet();
	//   Map { submesh => constraint } applied to this instance:
	private THashMap constraints = new THashMap();
	private THashMap implicitConstraints = new THashMap();
	private THashMap resultConstraint = new THashMap();

	private THashMap mesh = new THashMap();
	// public Object mesh = null;
	private THashMap mesh1D = new THashMap();
	// public MMesh1D mesh1D;
	
	// In OccJava, two CADShape instances can be equal with different
	// orientations.  We sometimes need to keep track of shape orientation
	// in our graph, hash sets and maps can then use the keepOrientation
	// instance as hashing strategy.
	private static TObjectHashingStrategy keepOrientation = new TObjectHashingStrategy()
	{
		public int computeHashCode(Object o)
		{
			return o.hashCode();
		}
		public boolean equals(Object o1, Object o2)               
		{
			CADShape s1 = ((CADShape) o1);
			CADShape s2 = ((CADShape) o2);
			return s1 != null && s1.equals(s2) && s1.orientation() == s2.orientation();
		}
	};

	/**
	 * Creates a graph cell.
	 */
	public BCADGraphCell (BCADGraph g, CADShape s)
	{
		graph = g;
		shape = s;
		orientation = s.orientation();
	}
	
	public int getId()
	{
		return id;
	}

	public void setId(int i)
	{
		id = i;
	}

	public BCADGraph getGraph()
	{
		return graph;
	}

	public CADShape getShape()
	{
		return shape;
	}

	public int getOrientation()
	{
		return orientation;
	}

	public BCADGraphCell getReversed()
	{
		return reversed;
	}

	public void setReversed(BCADGraphCell that)
	{
		assert shape.equals(that.shape);
		assert shape.orientation() != that.shape.orientation();
		reversed = that;
	}

	public Collection getParents()
	{
		return parents;
	}

	public void addParent(Object that)
	{
		assert that != null;
		parents.add(that);
	}

	public void printParents()
	{
		System.out.println(parents);
	}

	// Returns an iterator on geometrical elements of dimension d
	public Iterator shapesExplorer(CADShapeEnum d)
	{
		return shapesExplorer(d, new THashSet(keepOrientation));
	}
	// Returns an iterator on unique geometrical elements of dimension d
	public Iterator uniqueShapesExplorer(CADShapeEnum d)
	{
		return shapesExplorer(d, new THashSet());
	}
	// Returns an iterator on all geometrical elements of dimension d
	public Iterator allShapesIterator(CADShapeEnum d)
	{
		return shapesExplorer(d, null);
	}
	private Iterator shapesExplorer(final CADShapeEnum cse, final Collection cadShapeSet)
	{
		final CADExplorer exp = CADShapeBuilder.factory.newExplorer();
		exp.init(shape, cse);
		return new Iterator()
		{
			public boolean hasNext()
			{
				return exp.more();
			}
			public Object next()
			{
				CADShape curr = exp.current();
				Object ret = graph.getByShape(curr);
				if (cadShapeSet == null)
				{
					if (exp.more())
						exp.next();
				}
				else
				{
					cadShapeSet.add(curr);
					while (exp.more())
					{
						if (!cadShapeSet.contains(exp.current()))
							break;
						exp.next(); 
					}
				}
				return ret;
			}
			public void remove()
			{
			}
		};
	}

	public Iterator shapesIterator()
	{
		return shapesIterator(new THashSet(keepOrientation));
	}
	public Iterator uniqueShapesIterator()
	{
		return shapesIterator(new THashSet());
	}
	public Iterator allShapesIterator()
	{
		return shapesIterator(new THashSet());
	}
	private Iterator shapesIterator(final Collection cadShapeSet)
	{
		final CADIterator it = CADShapeBuilder.factory.newIterator();
		it.initialize(shape);
		return new Iterator()
		{
			public boolean hasNext()
			{
				return it.more();
			}
			public Object next()
			{
				CADShape curr = it.value();
				Object ret = graph.getByShape(curr);
				if (cadShapeSet == null)
				{
					if (it.more())
						it.next();
				}
				else
				{
					cadShapeSet.add(curr);
					while (it.more())
					{
						if (!cadShapeSet.contains(it.value()))
							break;
						it.next(); 
					}
				}
				return ret;
			}
			public void remove()
			{
			}
		};
	}

	public void addSubMeshConstraint(BSubMesh s, Constraint c)
	{
		constraints.put(s, c);
		// For convenience, constraints contain a link to all
		// BSubMesh instances in which they appear.
		c.addSubMesh(s);
	}
	
	private static class BuildConstraintToSubMeshMapProcedure implements TObjectObjectProcedure
	{
		private final THashMap map;
		private BuildConstraintToSubMeshMapProcedure(THashMap m)
		{
			map = m;
		}
		public boolean execute(Object key, Object val)
		{
			THashSet meshes = (THashSet) map.get(val);
			if (meshes == null)
			{
				meshes = new THashSet();
				map.put(val, meshes);
			}
			// Add submesh
			meshes.add(key);
			return true;
		}
	}

	// Add implicit constraints on subshapes
	public void addImplicitConstraints()
	{
		// If a constraint appears in several submeshes, it
		// must be derived only once.  First create a map
		//   Constraint ==> { list of BSubMesh }
		THashMap map = new THashMap();
		BuildConstraintToSubMeshMapProcedure proc = new BuildConstraintToSubMeshMapProcedure(map);
		constraints.forEachEntry(proc);

		for (Iterator itcse = CADShapeEnum.iterator(CADShapeEnum.VERTEX, CADShapeEnum.COMPOUND); itcse.hasNext(); )
		{
			CADShapeEnum cse = (CADShapeEnum) itcse.next();
			for (Iterator it = shapesExplorer(cse); it.hasNext(); )
			{
				BCADGraphCell sub = (BCADGraphCell) it.next();
				for (Iterator itc = map.entrySet().iterator(); itc.hasNext(); )
				{
					Map.Entry e = (Map.Entry) itc.next();
					Constraint c = (Constraint) e.getKey();
					Constraint derived = c.newConstraint(cse);
					THashSet smSet = (THashSet) e.getValue();
					assert smSet != null;
					for (Iterator itsm = smSet.iterator(); itsm.hasNext(); )
					{
						BSubMesh sm = (BSubMesh) itsm.next();
						THashSet subsmSet = (THashSet) sub.implicitConstraints.get(sm);
						if (subsmSet == null)
						{
							subsmSet = new THashSet();
							sub.implicitConstraints.put(sm, subsmSet);
						}
						subsmSet.add(derived);
					}
				}
			}
		}
	}

	public Constraint getSubMeshConstraint(BSubMesh s)
	{
		return (Constraint) constraints.get(s);
	}

	public Collection setOfSubMesh()
	{
		return constraints.keySet();
	}

	public void combineHypothesis(CADShapeEnum d)
	{
		for (Iterator its = implicitConstraints.keySet().iterator(); its.hasNext(); )
		{
			BSubMesh s = (BSubMesh) its.next();
			LinkedHashSet mh = new LinkedHashSet();
			Constraint c = (Constraint) constraints.get(s);
			if (c != null)
				mh.add(c);
			THashSet h = (THashSet) implicitConstraints.get(s);
			if (h != null)
				for (Iterator it = h.iterator(); it.hasNext(); )
					mh.add(it.next());
			resultConstraint.put(s, ResultConstraint.combineAll(mh, d));
		}
	}

	public boolean hasConstraints(BSubMesh s)
	{
		return resultConstraint.get(s) != null;
	}

	public boolean discretize(BSubMesh s)
	{
		ResultConstraint c = (ResultConstraint) resultConstraint.get(s);
		if (c != null)
			c.applyAlgorithm(this, s);
		return true;
	}

	public Object getMesh(BSubMesh s)
	{
		return mesh.get(s);
	}

	public void setMesh(BSubMesh s, Object m)
	{
		mesh.put(s, m);
	}

	public MMesh1D getMesh1D(BSubMesh s)
	{
		return (MMesh1D) mesh1D.get(s);
	}

	public void setMesh1D(BSubMesh s, MMesh1D m)
	{
		mesh1D.put(s, m);
	}

	private static class PrintProcedure implements TObjectObjectProcedure
	{
		private final String header;
		private PrintProcedure(String h)
		{
			header = h;
		}
		public boolean execute(Object key, Object val)
		{
			BSubMesh s = (BSubMesh) key;
			Constraint c = (Constraint) val;
			System.out.println(header+" submesh "+s.getId()+" "+c);
			return true;
		}
	}

	private static class PrintImplicitProcedure implements TObjectObjectProcedure
	{
		private final String header;
		private PrintImplicitProcedure(String h)
		{
			header = h;
		}
		public boolean execute(Object key, Object val)
		{
			BSubMesh s = (BSubMesh) key;
			THashSet h = (THashSet) val;
			StringBuffer r = new StringBuffer();
			for (Iterator it = h.iterator(); it.hasNext(); )
			{
				Constraint c = (Constraint) it.next();
				r.append(" constraint "+c.getOrigin());
			}
			System.out.println(header+" submesh "+s.getId()+" ["+r.toString()+"]");
			return true;
		}
	}

	public void printConstraints(String headline)
	{
		PrintProcedure proc = new PrintProcedure(headline);
		constraints.forEachEntry(proc);
		PrintImplicitProcedure p2 = new PrintImplicitProcedure(headline);
		implicitConstraints.forEachEntry(p2);
	}

	/*
	private void updateNodeLabels()
	{
		logger.debug("Update node labels");
		//  Resets all labels
		for (Iterator ite = shapesExplorer(1); ite.hasNext(); )
		{
			CADEdge E = (CADEdge) ite.next();
			Mesh m = cadToMesh(E);
			SubMesh1D submesh1d = (SubMesh1D) m.mesh;
			if (submesh1d == null)
				continue;
			for (Iterator itn = submesh1d.getNodesIterator(); itn.hasNext(); )
			{
				MNode1D n = (MNode1D) itn.next();
				n.setLabel(0);
			}
		}
		int i = 0;
		for (Iterator ite = shapesExplorer(1); ite.hasNext(); )
		{
			CADEdge E = (CADEdge) ite.next();
			Mesh m = cadToMesh(E);
			SubMesh1D submesh1d = (SubMesh1D) m.mesh;
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
	private void duplicateEdges()
	{
		logger.debug("Compute vertex references");
		//  For each topological vertex, compute the list of
		//  MNode1D objects which are bound to this vertex.
		int nVertex = 0;
		for (Iterator itn = shapesExplorer(0); itn.hasNext(); itn.next())
			nVertex++;
		THashMap vertex2Ref = new THashMap(nVertex);
		for (Iterator itn = shapesExplorer(0); itn.hasNext(); )
			vertex2Ref.put(itn.next(), new ArrayList());
		for (Iterator ite = shapesExplorer(1); ite.hasNext(); )
		{
			CADEdge E = (CADEdge) ite.next();
			Mesh m = cadToMesh(E);
			SubMesh1D submesh1d = (SubMesh1D) m.mesh;
			if (submesh1d == null)
				continue;
			Iterator itn = submesh1d.getNodesIterator();
			while (itn.hasNext())
			{
				MNode1D pt = (MNode1D) itn.next();
				if (null != pt.getCADVertex())
					((ArrayList) vertex2Ref.get(pt.getCADVertex())).add(pt);
			}
		}
		
		THashSet seen = new THashSet();
		for (Iterator itn = shapesExplorer(0); itn.hasNext(); )
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
	*/

	public String toString()
	{
		String ret = id+" "+shape+" "+orientation;
		if (reversed != null)
			ret += " rev="+Integer.toHexString(reversed.hashCode());
		return ret;
	}

}
