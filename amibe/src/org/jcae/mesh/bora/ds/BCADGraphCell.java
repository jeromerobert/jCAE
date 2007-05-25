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
import java.util.ArrayList;
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
	// CAD shape type
	private CADShapeEnum type;
	// Shape orientation  (0=forward, 1=reversed)
	private int orientation;
	// Link to the reversed shape, if it does exist
	private BCADGraphCell reversed;
	// List of parents
	private Collection parents = new LinkedHashSet();
	// List of discretizations
	private Collection discrete = new ArrayList();

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
	public BCADGraphCell (BCADGraph g, CADShape s, CADShapeEnum t)
	{
		graph = g;
		shape = s;
		type  = t;
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
		if (orientation != 0)
			discrete = that.discrete;
	}

	public CADShapeEnum getType()
	{
		return type;
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

public Object getMesh(BSubMesh sub)
{
return null;
}
public void setMesh(BSubMesh sub, Object m)
{
}
public MMesh1D getMesh1D(BSubMesh sub)
{
return null;
}


	public BDiscretization getDiscretizationSubMesh(BSubMesh sub)
	{
		for (Iterator it = discrete.iterator(); it.hasNext(); )
		{
			BDiscretization discr = (BDiscretization) it.next();
			if (discr.contains(sub))
				return discr;
		}
		return null;
	}

	public void addSubMeshConstraint(BSubMesh sub, Constraint cons)
	{
		BDiscretization discr = getDiscretizationSubMesh(sub);
		if (discr != null)
			throw new RuntimeException("Constraint "+cons+" cannot be applied to shape "+shape+", another constraint "+discr.getConstraint()+" is already defined");
		BDiscretization found = null;
		for (Iterator it = discrete.iterator(); it.hasNext(); )
		{
			discr = (BDiscretization) it.next();
			if (discr.getConstraint() == cons)
			{
				found = discr;
				break;
			}
		}
		if (found == null)
		{
			found = new BDiscretization(this, cons);
			discrete.add(found);
		}
		found.addSubMesh(sub);
	}

	public Iterator discretizationIterator()
	{
		return discrete.iterator();
	}

	public void addImplicitConstraints(CADShapeEnum cse, boolean recursive)
	{
		for (Iterator it = discrete.iterator(); it.hasNext(); )
		{
			BDiscretization discr = (BDiscretization) it.next();
			Iterator itc;
			if (recursive)
				itc = shapesExplorer(cse);
			else
				itc = shapesIterator();

			while (itc.hasNext())
			{
				BCADGraphCell child = (BCADGraphCell) itc.next();
				if (!recursive && child.getType() != cse)
					continue;
				BDiscretization discrChild = null;
				for (Iterator itd = child.discrete.iterator(); itd.hasNext(); )
				{
					discrChild = (BDiscretization) itd.next();
					if (!discr.emptyIntersection(discrChild))
							break;
					discrChild = null;
				}
				if (discrChild == null)
				{
						discrChild = new BDiscretization(child, null);
						child.discrete.add(discrChild);
				}
				discrChild.combineConstraint(discr);
				discrChild.addAllSubMeshes(discr);
			}
		}
	}

	public String toString()
	{
		String ret = id+" "+shape+" "+orientation+" "+Integer.toHexString(hashCode());
		if (reversed != null)
			ret += " rev="+Integer.toHexString(reversed.hashCode());
		return ret;
	}

}
