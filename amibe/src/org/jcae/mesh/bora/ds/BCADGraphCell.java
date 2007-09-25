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
import org.jcae.mesh.cad.CADShapeFactory;
import org.jcae.mesh.cad.CADShapeEnum;
import org.jcae.mesh.cad.CADExplorer;
import org.jcae.mesh.cad.CADIterator;

import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Iterator;
import gnu.trove.THashSet;
import gnu.trove.TObjectHashingStrategy;

/**
 * Graph cell.  This class is a decorator for the CAD graph.
 */
public class BCADGraphCell
{
	/**
	 * Unique identifier.
	 */
	private int id = -1;
	/**
	 * Link to root graph.
	 */
	private BCADGraph graph;
	/**
	 * CAD shape.
	 */
	private CADShape shape;
	/**
	 * CAD shape type.
	 */
	private CADShapeEnum type;
	/**
	 * Link to the reversed shape, if it does exist.
	 */
	private BCADGraphCell reversed;
	/**
	 * List of parents.
	 */
	private Collection<BCADGraphCell> parents = new LinkedHashSet<BCADGraphCell>();
	/**
	 * List of discretizations.
	 */
	private Collection<BDiscretization> discrete = new ArrayList<BDiscretization>();

	// In OccJava, two CADShape instances can be equal with different
	// orientations.  We sometimes need to keep track of shape orientation
	// in our graph, hash sets and maps can then use the keepOrientation
	// instance as hashing strategy.
	private static TObjectHashingStrategy<CADShape> keepOrientation = new TObjectHashingStrategy<CADShape>()
	{
		public int computeHashCode(CADShape o)
		{
			return o.hashCode();
		}
		public boolean equals(CADShape s1, CADShape s2)
		{
			return s1 != null && s1.equals(s2) && s1.orientation() == s2.orientation();
		}
	};

	/**
	 * Constructor.
	 * @param g  graph cell
	 * @param s  CAD shape contained in this cell
	 * @param t  CAD shape type
	 */
	public BCADGraphCell (BCADGraph g, CADShape s, CADShapeEnum t)
	{
		graph = g;
		shape = s;
		type  = t;
	}
	
	/**
	 * Returns cell id.
	 * @return cell id
	 */
	public int getId()
	{
		return id;
	}

	/**
	 * Sets cell id.
	 * @param i  cell id
	 */
	public void setId(int i)
	{
		id = i;
	}

	/**
	 * Returns CAD graph.
	 * @return CAD graph
	 */
	public BCADGraph getGraph()
	{
		return graph;
	}

	/**
	 * Returns CAD shape.
	 * @return CAD shape
	 */
	public CADShape getShape()
	{
		return shape;
	}

	/**
	 * Returns shape orientation.
	 * @return shape orientation
	 */
	public int getOrientation()
	{
		return shape.orientation();
	}

	/**
	 * Returns cell containing reversed shape.
	 * @return cell containing reversed shape
	 */
	public BCADGraphCell getReversed()
	{
		return reversed;
	}

	/**
	 * Returns CAD shape type.
	 * @return CAD shape type
	 */
	public CADShapeEnum getType()
	{
		return type;
	}

	public Collection<BCADGraphCell> getParents()
	{
		return parents;
	}

	public void addParent(BCADGraphCell that)
	{
		assert that != null;
		parents.add(that);
	}

	/**
	 * Binds two cells containing reversed shapes together.  These shapes then
	 * contain the same discretizations.
	 */
	public void bindReversed(BCADGraphCell that)
	{
		assert shape.equals(that.shape);
		assert shape.orientation() != that.shape.orientation();
		reversed = that;
		that.reversed = this;
		if (shape.orientation() != 0)
			discrete = that.discrete;
		else
			that.discrete = discrete;
	}

	/**
	 * Returns an iterator on geometrical elements of a given type.
	 * CAD graph is traversed recursively and geometrical elements of
	 * a given type are returned.  There are no duplicates, but shapes
	 * with different orientations may both be listed.
	 *
	 * @param cse  CAD shape type
	 * @returns iterator on geometrical elements.
	 */
	public Iterator<BCADGraphCell> shapesExplorer(CADShapeEnum cse)
	{
		return shapesExplorer(cse, new THashSet<CADShape>(keepOrientation));
	}

	/**
	 * Returns an iterator on unique geometrical elements.
	 * CAD graph is traversed recursively and geometrical elements of a
	 * given type are returned.  There are no duplicates, shapes with
	 * different orientations are listed only once.
	 *
	 * @param cse  CAD shape type
	 * @returns iterator on unique geometrical elements.
	 */
	public Iterator<BCADGraphCell> uniqueShapesExplorer(CADShapeEnum cse)
	{
		return shapesExplorer(cse, new THashSet<CADShape>());
	}

	/**
	 * Returns an iterator on all geometrical elements of a given type.
	 * CAD graph is traversed recursively and geometrical elements of a
	 * given type are returned, even if they have already been seen.
	 *
	 * @param cse  CAD shape type
	 * @returns iterator on all geometrical elements.
	 */
	public Iterator<BCADGraphCell> allShapesExplorer(CADShapeEnum cse)
	{
		return shapesExplorer(cse, null);
	}

	private Iterator<BCADGraphCell> shapesExplorer(final CADShapeEnum cse, final Collection<CADShape> cadShapeSet)
	{
		final CADExplorer exp = CADShapeFactory.factory.newExplorer();
		exp.init(shape, cse);
		return new Iterator<BCADGraphCell>()
		{
			public boolean hasNext()
			{
				return exp.more();
			}
			public BCADGraphCell next()
			{
				CADShape curr = exp.current();
				BCADGraphCell ret = graph.getByShape(curr);
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

	/**
	 * Returns an iterator on immediate sub-shapes.
	 * There are no duplicates, but shapes with different orientations may
	 * both be listed.
	 *
	 * @returns an iterator on immediate sub-shapes
	 */
	public Iterator<BCADGraphCell> shapesIterator()
	{
		return shapesIterator(new THashSet<CADShape>(keepOrientation));
	}

	/**
	 * Returns an iterator on immediate sub-shapes.
	 * There are no duplicates, shapes with different orientations are
	 * listed only once.
	 *
	 * @returns an iterator on immediate sub-shapes
	 */
	public Iterator<BCADGraphCell> uniqueShapesIterator()
	{
		return shapesIterator(new THashSet<CADShape>());
	}

	/**
	 * Returns an iterator on all immediate sub-shapes.
	 *
	 * @returns an iterator on all immediate sub-shapes
	 */
	public Iterator<BCADGraphCell> allShapesIterator()
	{
		return shapesIterator(null);
	}

	private Iterator<BCADGraphCell> shapesIterator(final Collection<CADShape> cadShapeSet)
	{
		final CADIterator it = CADShapeFactory.factory.newIterator();
		it.initialize(shape);
		return new Iterator<BCADGraphCell>()
		{
			public boolean hasNext()
			{
				return it.more();
			}
			public BCADGraphCell next()
			{
				CADShape curr = it.value();
				BCADGraphCell ret = graph.getByShape(curr);
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

	/**
	 * Returns the BDiscretization instance corresponding to a given submesh.
	 *
	 * @param sub  submesh
	 * @returns the discretization corresponding to a given submesh.
	 */
	public BDiscretization getDiscretizationSubMesh(BSubMesh sub)
	{
		for (Iterator<BDiscretization> it = discrete.iterator(); it.hasNext(); )
		{
			BDiscretization discr = it.next();
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
		for (Iterator<BDiscretization> it = discrete.iterator(); it.hasNext(); )
		{
			discr = it.next();
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

	/**
	 * Returns an iterator on BDiscretization instances bound to this cell.
	 */
	public Iterator<BDiscretization> discretizationIterator()
	{
		return discrete.iterator();
	}

	public void addImplicitConstraints(CADShapeEnum cse, boolean recursive)
	{
		for (Iterator<BDiscretization> it = discrete.iterator(); it.hasNext(); )
		{
			BDiscretization discr = it.next();
			Iterator<BCADGraphCell> itc;
			if (recursive)
				itc = shapesExplorer(cse);
			else
				itc = shapesIterator();

			while (itc.hasNext())
			{
				BCADGraphCell child = itc.next();
				if (!recursive && child.getType() != cse)
					continue;
				boolean allIntersectionsEmpty = false;
				BDiscretization discrChild = null;
				for (Iterator<BDiscretization> itd = child.discrete.iterator(); itd.hasNext(); )
				{
					discrChild = itd.next();
					if (!discr.emptyIntersection(discrChild))
						break;
					discrChild = null;
				}
				if (discrChild == null)
				{
					discrChild = new BDiscretization(child, null);
					child.discrete.add(discrChild);
					allIntersectionsEmpty = true;
				}
				discrChild.combineConstraint(discr);
				discrChild.addAllSubMeshes(discr);
				/*
				 * We want to ensure that in each BCADGraphCell, the intersection of
				 * the submeshes list for any two different BDiscretization is void. 
				 *
				 * If a new BDiscretization has been created, there is no need to 
				 * perform an additionnal test to enforce this.
				 * But if we had an intersection between the submesh list of discr and 
				 * the one of discrChild, we have to check if the combined BDiscretization 
				 * has an intersection with other child BDiscretization.
				 * The other child BDiscretization that intersect discrChild has to be removed
				 * after combination with discrChild.
				 */
				if (!allIntersectionsEmpty)
				{
					for (Iterator<BDiscretization> itd = child.discrete.iterator(); itd.hasNext(); )
					{
						BDiscretization otherDiscrChild = itd.next();
						if ((otherDiscrChild != discrChild) && 
						   (!discrChild.emptyIntersection(otherDiscrChild)))
						{
							discrChild.combineConstraint(otherDiscrChild);
							discrChild.addAllSubMeshes(otherDiscrChild);
							itd.remove();
						}
					}
				}
			}
		}
	}

	@Override
	public String toString()
	{
		String ret = id+" "+shape+" "+shape.orientation()+" "+Integer.toHexString(hashCode());
		if (reversed != null)
			ret += " rev="+Integer.toHexString(reversed.hashCode());
		return ret;
	}

}
