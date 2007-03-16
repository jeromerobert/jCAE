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
	private THashMap originConstraints = new THashMap();
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

	// Returns an iterator on geometrical elements of a given type
	public Iterator shapesExplorer(CADShapeEnum cse)
	{
		return shapesExplorer(cse, new THashSet(keepOrientation));
	}
	// Returns an iterator on unique geometrical elements of a given type
	public Iterator uniqueShapesExplorer(CADShapeEnum cse)
	{
		return shapesExplorer(cse, new THashSet());
	}
	// Returns an iterator on all geometrical elements of a given type
	public Iterator allShapesIterator(CADShapeEnum cse)
	{
		return shapesExplorer(cse, null);
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

	void addSubMeshConstraint(BSubMesh sub, Constraint cons)
	{
		// Only one explicit constraint per submesh is allowed
		if (constraints.get(sub) != null)
			throw new RuntimeException("Constraint "+cons+" cannot be applied to shape "+shape+", another constraint "+constraints.get(sub)+" is already defined");
		constraints.put(sub, cons);
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
					Constraint cons = (Constraint) e.getKey();
					Constraint derived = cons.newConstraint(cse);
					THashSet smSet = (THashSet) e.getValue();
					assert smSet != null;
					for (Iterator itsm = smSet.iterator(); itsm.hasNext(); )
					{
						BSubMesh sm = (BSubMesh) itsm.next();
						THashSet subsmSet = (THashSet) sub.implicitConstraints.get(sm);
						THashSet originSet = (THashSet) sub.originConstraints.get(sm);
						if (subsmSet == null)
						{
							subsmSet = new THashSet();
							sub.implicitConstraints.put(sm, subsmSet);
							assert originSet == null;
							originSet = new THashSet();
							sub.originConstraints.put(sm, originSet);

							if (sub.reversed != null)
							{
								sub.reversed.implicitConstraints.put(sm, subsmSet);
								sub.reversed.originConstraints.put(sm, originSet);
							}
						}

						Constraint origin = derived.getOrigin();
						if (origin != null)
						{
							if (!originSet.contains(origin))
							{
								originSet.add(origin);
								subsmSet.add(derived);
							}
						}
						else
							subsmSet.add(derived);
					}
				}
			}
		}
	}

	public Constraint getSubMeshConstraint(BSubMesh sub)
	{
		return (Constraint) constraints.get(sub);
	}

	public Collection setOfSubMesh()
	{
		return constraints.keySet();
	}

	public void combineHypothesis(CADShapeEnum d)
	{
		for (Iterator its = implicitConstraints.keySet().iterator(); its.hasNext(); )
		{
			BSubMesh sub = (BSubMesh) its.next();
			LinkedHashSet mh = new LinkedHashSet();
			Constraint cons = (Constraint) constraints.get(sub);
			if (cons != null)
				mh.add(cons);
			THashSet h = (THashSet) implicitConstraints.get(sub);
			if (h != null)
				for (Iterator it = h.iterator(); it.hasNext(); )
					mh.add(it.next());
			resultConstraint.put(sub, ResultConstraint.combineAll(mh, d));
		}
	}

	public boolean hasConstraints(BSubMesh sub)
	{
		return resultConstraint.get(sub) != null;
	}

	public boolean discretize(BSubMesh sub)
	{
		ResultConstraint cons = (ResultConstraint) resultConstraint.get(sub);
		if (cons != null)
			cons.applyAlgorithm(this, sub);
		return true;
	}

	public Object getMesh(BSubMesh sub)
	{
		return mesh.get(sub);
	}

	public void setMesh(BSubMesh sub, Object m)
	{
		mesh.put(sub, m);
	}

	public MMesh1D getMesh1D(BSubMesh sub)
	{
		return (MMesh1D) mesh1D.get(sub);
	}

	public void setMesh1D(BSubMesh sub, MMesh1D m)
	{
		mesh1D.put(sub, m);
	}

	private class PrintProcedure implements TObjectObjectProcedure
	{
		private final String header;
		private PrintProcedure(String h)
		{
			header = h;
		}
		public boolean execute(Object key, Object val)
		{
			BSubMesh sub = (BSubMesh) key;
			Constraint cons = (Constraint) val;
			System.out.println(header+" submesh "+sub.getId()+" "+cons);
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
			BSubMesh sub = (BSubMesh) key;
			THashSet h = (THashSet) val;
			StringBuffer r = new StringBuffer();
			for (Iterator it = h.iterator(); it.hasNext(); )
			{
				Constraint cons = (Constraint) it.next();
				r.append(" implicit "+cons.getOrigin());
			}
			System.out.println(header+" submesh "+sub.getId()+" ["+r.toString()+"]");
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
		String ret = id+" "+shape+" "+orientation+" "+Integer.toHexString(hashCode());
		if (reversed != null)
			ret += " rev="+Integer.toHexString(reversed.hashCode());
		return ret;
	}

}
