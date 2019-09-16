/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2003,2006 by EADS CRC
    Copyright (C) 2007-2011, by EADS France

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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.amibe.algos3d;

import gnu.trove.list.array.TIntArrayList;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.HalfEdge;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.TriangleHE;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.projection.MeshLiaison;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;
import java.util.logging.Logger;


/**
 * Split tagged triangles
 *
 */
public class SplitTagged
{
	/**
	 * Define a HashMap with default return value for missing keys
	 */
	private class DefaultHashMap<Key, Value> extends HashMap<Key, Value>
	{
		private Value defaultValue;

		public DefaultHashMap(Value defaultValue)
		{
			super();
			this.defaultValue = defaultValue;
		}

		@Override
		public Value get(Object key)
		{
			return containsKey(key) ? super.get(key) : defaultValue;
		}
	}

	Mesh mesh;
	MeshLiaison liaison;
	Collection<Triangle> taggedTriangles;
	DefaultHashMap<Triangle, Integer> nsplitTriangle;
	HashMap<HalfEdge, Vertex> middleRegister;
	int vertices = 0;
	int triangles = 0;
	private static final Logger LOGGER = Logger.getLogger(SplitTagged.class.getName());

	/**
	 * Creates a <code>SplitTagged</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to refine.
	 * @param liaison  the <code>MeshLiaison</code> for projections.
	 * @param tagged   the <code>Collection<Triangle></code> of tagged elements.
	 */
	public SplitTagged(final MeshLiaison liaison)
	{
		this(liaison.getMesh(), liaison, null);
	}

	public SplitTagged(final MeshLiaison liaison, final Collection<Triangle> tagged)
	{
		this(liaison.getMesh(), liaison, tagged);
	}

	public SplitTagged(final Mesh m, final MeshLiaison meshLiaison, final Collection<Triangle> tagged)
	{
		mesh = m;
		liaison = meshLiaison;
		taggedTriangles = tagged;
		nsplitTriangle = new DefaultHashMap<Triangle, Integer>(0);
		middleRegister = new HashMap<HalfEdge, Vertex>();
	}

	/**
	 * Tag elements.
	 *
	 * @param tags  triangle indexes
	 */
	public void tagTriangles(final List<Integer> tags)
	{
		taggedTriangles = mesh.getTriangles(tags);
	}

	/**
	 * Split tagged elements into 4 and neighbors accordingly such that mesh
	 * remains conformal.
	 */
	public void compute()
	{
		if (taggedTriangles == null)
			taggedTriangles = mesh.getTriangles();
		LOGGER.info("Number of elements to process: "+taggedTriangles.size());
		markTriangles();
		LOGGER.info("Number of vertices to insert: "+vertices);
		split();
		LOGGER.info("Number of inserted triangles: "+triangles);
		LOGGER.info("Fix adjacency");
		fixAdjacency();
		assert mesh.isValid();
	}

	private void markTriangles()
	{
		// tag triangle edges and create middle vertices
		for (Triangle t: taggedTriangles)
			if (!t.hasAttributes(AbstractHalfEdge.OUTER))
				markEdges((TriangleHE) t);
		// clear MARKED attribute
		for (HalfEdge e: middleRegister.keySet())
			e.clearAttributes(AbstractHalfEdge.MARKED);
	}

	private void markEdges(TriangleHE t)
	{
		HalfEdge e = t.getAbstractHalfEdge();
		for (int i=0; i<3; i++)
		{
			if (!e.hasAttributes(AbstractHalfEdge.IMMUTABLE | AbstractHalfEdge.OUTER | AbstractHalfEdge.MARKED))
			{
				// create middle vertex and register but do not split yet
				Vertex inserted = mesh.createVertex(0, 0, 0);
				inserted.middle(e.origin(), e.destination());
				vertices++;

				// project node on background mesh
				liaison.move(inserted, inserted, t.getGroupId(), false);

				// add to mesh list of nodes
				mesh.add(inserted);

				// mark fan edges that will be splitted by inserted (fan = identity if manifold)
				for (Iterator<AbstractHalfEdge> it = e.fanIterator(); it.hasNext(); )
				{
					HalfEdge h = (HalfEdge) it.next();
					h.setAttributes(AbstractHalfEdge.MARKED);
					middleRegister.put(h, inserted);
					nsplitTriangle.put(h.getTri(), nsplitTriangle.get(h.getTri())+1);
				}

				// mark sym edge that will be splitted by inserted
				if (e.hasSymmetricEdge())
				{
					e.sym().setAttributes(AbstractHalfEdge.MARKED);
					middleRegister.put(e.sym(), inserted);
					nsplitTriangle.put(e.sym().getTri(), nsplitTriangle.get(e.sym().getTri())+1);
				}
			}
			e = e.next();
		}
	}

	private void split()
	{
		ArrayList<Triangle> added = new ArrayList<Triangle>();
		ArrayList<Triangle> removed = new ArrayList<Triangle>();

		for (Triangle t: mesh.getTriangles())
		{
			switch (nsplitTriangle.get(t))
			{
				case 1:
					split1(t, added);
					removed.add(t);
					break;
				case 2:
					split2(t, added);
					removed.add(t);
					break;
				case 3:
					split3(t, added);
					removed.add(t);
					break;
				default:
					break;
			}
		}
		for (Triangle t: removed)
			mesh.remove(t);
		for (Triangle t: added)
			mesh.add(t);
		triangles = added.size();
	}

	private void split1(Triangle t, Collection<Triangle> added)
	{
		HalfEdge e = (HalfEdge) t.getAbstractHalfEdge();

		// reference edge is the one that will be splitted
		while (!middleRegister.containsKey(e))
			e = e.next();

		Vertex n = middleRegister.get(e);

		Triangle t1 = mesh.createTriangle(e.apex(), e.origin(), n);
		t1.setGroupId(t.getGroupId());
		HalfEdge e1 = (HalfEdge) t1.getAbstractHalfEdge();
		e1.setAttributes(e.getAttributes());
		e1.prev().setAttributes(e.prev().getAttributes());

		Triangle t2 = mesh.createTriangle(e.apex(), n, e.destination());
		t2.setGroupId(t.getGroupId());
		HalfEdge e2 = (HalfEdge) t2.getAbstractHalfEdge();
		e2.setAttributes(e.getAttributes());
		e2.next().setAttributes(e.next().getAttributes());

		added.add(t1);
		added.add(t2);
	}

	private void split2(Triangle t, Collection<Triangle> added)
	{
		HalfEdge e = (HalfEdge) t.getAbstractHalfEdge();

		// reference edge is the one that will not be splitted
		while (middleRegister.containsKey(e))
			e = e.next();

		Vertex dp = middleRegister.get(e.next());
		Vertex ap = middleRegister.get(e.prev());

		Triangle t1 = mesh.createTriangle(dp, e.origin(), e.destination());
		t1.setGroupId(t.getGroupId());
		HalfEdge e1 = (HalfEdge) t1.getAbstractHalfEdge();
		e1.setAttributes(e.getAttributes());
		e1.next().setAttributes(e.next().getAttributes());

		Triangle t2 = mesh.createTriangle(ap, e.origin(), dp);
		t2.setGroupId(t.getGroupId());
		HalfEdge e2 = (HalfEdge) t2.getAbstractHalfEdge();
		e2.prev().setAttributes(e.prev().getAttributes());

		Triangle t3 = mesh.createTriangle(e.apex(), ap, dp);
		t3.setGroupId(t.getGroupId());
		HalfEdge e3 = (HalfEdge) t3.getAbstractHalfEdge();
		e3.next().setAttributes(e.next().getAttributes());
		e3.prev().setAttributes(e.prev().getAttributes());

		added.add(t1);
		added.add(t2);
		added.add(t3);
	}

	private void split3(Triangle t, Collection<Triangle> added)
	{
		// reference edge is any
		HalfEdge e = (HalfEdge) t.getAbstractHalfEdge();

		Vertex ap = middleRegister.get(e);
		Vertex op = middleRegister.get(e.next());
		Vertex dp = middleRegister.get(e.prev());

		Triangle t1 = mesh.createTriangle(dp, e.origin(), ap);
		t1.setGroupId(t.getGroupId());
		HalfEdge e1 = (HalfEdge) t1.getAbstractHalfEdge();
		e1.setAttributes(e.getAttributes());
		e1.prev().setAttributes(e.prev().getAttributes());

		Triangle t2 = mesh.createTriangle(ap, e.destination(), op);
		t2.setGroupId(t.getGroupId());
		HalfEdge e2 = (HalfEdge) t2.getAbstractHalfEdge();
		e2.setAttributes(e.next().getAttributes());
		e2.prev().setAttributes(e.getAttributes());

		Triangle t3 = mesh.createTriangle(op, e.apex(), dp);
		t3.setGroupId(t.getGroupId());
		HalfEdge e3 = (HalfEdge) t3.getAbstractHalfEdge();
		e3.setAttributes(e.prev().getAttributes());
		e3.prev().setAttributes(e.next().getAttributes());

		Triangle t4 = mesh.createTriangle(ap, op, dp);
		t4.setGroupId(t.getGroupId());

		added.add(t1);
		added.add(t2);
		added.add(t3);
		added.add(t4);
	}

	/**
	 * Rebuild adjacency
	 * TODO: fix adjacency locally since clear+build might be time consuming
	 */
	private void fixAdjacency()
	{
		mesh.clearAdjacency();
		mesh.buildAdjacency();
	}
}
