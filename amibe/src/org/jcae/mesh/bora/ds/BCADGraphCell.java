/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2006, by EADS CRC
    Copyright (C) 2007,2009, by EADS France

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

import gnu.trove.set.hash.TCustomHashSet;
import org.jcae.mesh.cad.CADShape;
import org.jcae.mesh.cad.CADShapeFactory;
import org.jcae.mesh.cad.CADShapeEnum;
import org.jcae.mesh.cad.CADExplorer;
import org.jcae.mesh.cad.CADIterator;

import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Iterator;
import gnu.trove.set.hash.THashSet;
import java.util.Collections;

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
	private final BCADGraph graph;
	/**
	 * CAD shape.
	 */
	private final CADShape shape;
	/**
	 * CAD shape type.
	 */
	private final CADShapeEnum type;
	/**
	 * Link to the reversed shape, if it does exist.
	 */
	private BCADGraphCell reversed;
	/**
	 * List of parents.
	 */
	private final Collection<BCADGraphCell> parents = new LinkedHashSet<BCADGraphCell>();
	/**
	 * List of discretizations.
	 */
	private Collection<BDiscretization> discrete = new ArrayList<BDiscretization>();

	/**
	 * Constructor.
	 * @param g  graph cell
	 * @param s  CAD shape contained in this cell
	 * @param t  CAD shape type
	 */
	protected BCADGraphCell (BCADGraph g, CADShape s, CADShapeEnum t)
	{
		graph = g;
		shape = s;
		type  = t;
	}
	
	/**
	 * Returns cell id.
	 * @return cell id
	 */
	public final int getId()
	{
		return id;
	}

	/**
	 * Sets cell id.
	 * @param i  cell id
	 */
	public final void setId(int i)
	{
		id = i;
	}

	/**
	 * Returns CAD graph.
	 * @return CAD graph
	 */
	public final BCADGraph getGraph()
	{
		return graph;
	}

	/**
	 * Returns CAD shape.
	 * @return CAD shape
	 */
	public final CADShape getShape()
	{
		return shape;
	}

	/**
	 * Returns shape orientation.
	 * @return shape orientation
	 */
	public final int getOrientation()
	{
		return shape.orientation();
	}

	/**
	 * Returns cell containing reversed shape.
	 * @return cell containing reversed shape
	 */
	public final BCADGraphCell getReversed()
	{
		return reversed;
	}

	/**
	 * Returns CAD shape type.
	 * @return CAD shape type
	 */
	public final CADShapeEnum getType()
	{
		return type;
	}

	public final Collection<BCADGraphCell> getParents()
	{
		return Collections.unmodifiableCollection(parents);
	}

	final void addParent(BCADGraphCell that)
	{
		assert that != null;
		parents.add(that);
	}

	/**
	 * Binds two cells containing reversed shapes together.  These shapes then
	 * contain the same discretizations.
	 */
	final void bindReversed(BCADGraphCell that)
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
	 * @return iterator on geometrical elements.
	 */
	public final Iterator<BCADGraphCell> shapesExplorer(CADShapeEnum cse)
	{
		return shapesExplorer(cse, new TCustomHashSet<CADShape>(
			KeepOrientationHashingStrategy.getInstance()));
	}

	/**
	 * Returns an iterator on unique geometrical elements.
	 * CAD graph is traversed recursively and geometrical elements of a
	 * given type are returned.  There are no duplicates, shapes with
	 * different orientations are listed only once.
	 *
	 * @param cse  CAD shape type
	 * @return iterator on unique geometrical elements.
	 */
	public final Iterator<BCADGraphCell> uniqueShapesExplorer(CADShapeEnum cse)
	{
		return shapesExplorer(cse, new THashSet<CADShape>());
	}

	/**
	 * Returns an iterator on all geometrical elements of a given type.
	 * CAD graph is traversed recursively and geometrical elements of a
	 * given type are returned, even if they have already been seen.
	 *
	 * @param cse  CAD shape type
	 * @return iterator on all geometrical elements.
	 */
	public Iterator<BCADGraphCell> allShapesExplorer(CADShapeEnum cse)
	{
		return shapesExplorer(cse, null);
	}

	private Iterator<BCADGraphCell> shapesExplorer(final CADShapeEnum cse, final Collection<CADShape> cadShapeSet)
	{
		final CADExplorer exp = CADShapeFactory.getFactory().newExplorer();
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
	 * @return an iterator on immediate sub-shapes
	 */
	public final Iterator<BCADGraphCell> shapesIterator()
	{
		return shapesIterator(new TCustomHashSet<CADShape>(
			KeepOrientationHashingStrategy.getInstance()));
	}

	/**
	 * Returns an iterator on immediate sub-shapes.
	 * There are no duplicates, shapes with different orientations are
	 * listed only once.
	 *
	 * @return an iterator on immediate sub-shapes
	 */
	public Iterator<BCADGraphCell> uniqueShapesIterator()
	{
		return shapesIterator(new THashSet<CADShape>());
	}

	/**
	 * Returns an iterator on all immediate sub-shapes.
	 *
	 * @return an iterator on all immediate sub-shapes
	 */
	public final Iterator<BCADGraphCell> allShapesIterator()
	{
		return shapesIterator(null);
	}

	private Iterator<BCADGraphCell> shapesIterator(final Collection<CADShape> cadShapeSet)
	{
		final CADIterator it = CADShapeFactory.getFactory().newIterator();
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
	 * @return the discretization corresponding to a given submesh.
	 */
	public final BDiscretization getDiscretizationSubMesh(BSubMesh sub)
	{
		for (BDiscretization discr : discrete)
		{
			if (discr.contains(sub))
				return discr;
		}
		return null;
	}

	final void addSubMeshConstraint(BSubMesh sub, Constraint cons)
	{
		BDiscretization d = getDiscretizationSubMesh(sub);
		if (d != null)
			throw new RuntimeException("Constraint "+cons+" cannot be applied to shape "+shape+", another constraint "+d.getConstraint()+" is already defined");
		BDiscretization found = null;
		for (BDiscretization discr : discrete)
		{
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

	final void removeSubMeshConstraint(BSubMesh sub, Constraint cons)
	{
		BDiscretization d = getDiscretizationSubMesh(sub);
		if (d == null)
			throw new RuntimeException("Constraint "+cons+" cannot be removed from shape "+shape+", beacause it doesn't exist");
		discrete.remove(d);
	}

	/**
	 * Returns an immutable view of the list of BDiscretization instances bound to this cell.
	 */
	public final Collection<BDiscretization> getDiscretizations()
	{
		return Collections.unmodifiableCollection(discrete);
	}

	final void addImplicitConstraints(CADShapeEnum cse, boolean recursive)
	{
		for (BDiscretization discr : discrete)
		{
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
				for (BDiscretization dc : child.discrete)
				{
					if (!discr.emptyIntersection(dc))
					{
						discrChild = dc;
						break;
					}
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

	final void removeDiscretizations()
	{
		discrete.clear();
	}

	@Override
	public final String toString()
	{
		String ret = id+" "+shape+" "+shape.orientation()+" "+Integer.toHexString(hashCode());
		if (reversed != null)
			ret += " rev="+Integer.toHexString(reversed.hashCode());
		return ret;
	}

}
