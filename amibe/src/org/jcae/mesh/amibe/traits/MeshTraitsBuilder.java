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

package org.jcae.mesh.amibe.traits;

import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.util.KdTree;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Collection;

public class MeshTraitsBuilder extends TraitsBuilder
{
	// TraitsBuilder already uses bits 0-7
	private static final int BITTRIANGLES = 8;
	private static final int BITNODES     = 9;
	private static final int BITGROUPS    = 10;
	private static final int BITKDTREE    = 11;

	private static final int TRIANGLES        = 1 << BITTRIANGLES;
	private static final int NODES            = 1 << BITNODES;
	private static final int GROUPLIST        = 1 << BITGROUPS;
	private static final int KDTREE           = 1 << BITKDTREE;
	private static final int TRIANGLESET      = 1 << 30;
	private static final int NODESET          = 1 << 31;

	private VertexTraitsBuilder vertexTraitsBuilder = new VertexTraitsBuilder();
	private HalfEdgeTraitsBuilder halfedgeTraitsBuilder = new HalfEdgeTraitsBuilder();
	private TriangleTraitsBuilder triangleTraitsBuilder = new TriangleTraitsBuilder();

	private int dimension;

	/**
	 * Constructor.
	 */
	public MeshTraitsBuilder()
	{
		super();
		attributes |= TRIANGLES;
	}

	/**
	 * Adds triangle list to mesh traits.
	 *
	 * @return  this instance
	 */
	public MeshTraitsBuilder addTriangleList()
	{
		attributes &= ~TRIANGLESET;
		return this;
	}

	/**
	 * Adds triangle set to mesh traits.
	 *
	 * @return  this instance
	 */
	public MeshTraitsBuilder addTriangleSet()
	{
		attributes |= TRIANGLESET;
		return this;
	}

	/**
	 * Returns collection of triangles.
	 *
	 * @param t  mesh traits
	 * @return collection of triangles
	 */
	@SuppressWarnings("unchecked")
	public Collection<Triangle> getTriangles(Traits t)
	{
		return (Collection<Triangle>) t.array[index[BITTRIANGLES]];
	}

	/**
	 * Tells whether mesh traits stores collection of triangles.  This method
	 * always returns <code>true</code> because our data structure is based on
	 * triangles.
	 *
	 * @return <code>true</code>
	 */
	public boolean hasTriangles()
	{
		return true;
	}

	/**
	 * Adds node list to mesh traits.
	 *
	 * @return  this instance
	 */
	public MeshTraitsBuilder addNodeList()
	{
		attributes |= NODES;
		attributes &= ~NODESET;
		return this;
	}

	/**
	 * Adds node set to mesh traits.
	 *
	 * @return  this instance
	 */
	public MeshTraitsBuilder addNodeSet()
	{
		attributes |= NODES;
		attributes |= NODESET;
		return this;
	}

	/**
	 * Returns collection of nodes.
	 *
	 * @param t  mesh traits
	 * @return collection of nodes
	 */
	@SuppressWarnings("unchecked")
	public Collection<Vertex> getNodes(Traits t)
	{
		if ((attributes & NODES) != 0)
			return (Collection<Vertex>) t.array[index[BITNODES]];
		return null;
	}

	/**
	 * Tells whether mesh traits stores collection of nodes.
	 *
	 * @return <code>true</code> if {@link #addNodeList} or {@link #addNodeSet}
	 * was called, <code>false</code> otherwise.
	 */
	public boolean hasNodes()
	{
		return hasCapability(NODES);
	}

	/**
	 * Adds group list to mesh traits.
	 *
	 * @return  this instance
	 */
	public MeshTraitsBuilder addGroupList()
	{
		attributes |= GROUPLIST;
		return this;
	}

	/**
	 * Returns collection of groups.
	 *
	 * @param t  mesh traits
	 * @return collection of groups
	 */
	public Collection getGroups(Traits t)
	{
		if ((attributes & GROUPLIST) != 0)
			return (Collection) t.array[index[BITGROUPS]];
		return null;
	}

	/**
	 * Adds {@link KdTree} instance to mesh traits.
	 *
	 * @return  this instance
	 */
	public MeshTraitsBuilder addKdTree(int d)
	{
		attributes |= KDTREE;
		dimension = d;
		return this;
	}

	/**
	 * Returns {@link KdTree} instance.
	 *
	 * @param t  mesh traits
	 * @return {@link KdTree} instance
	 */
	public KdTree getKdTree(Traits t)
	{
		if ((attributes & KDTREE) != 0)
			return (KdTree) t.array[index[BITKDTREE]];
		return null;
	}

	@Override
	protected void subInitTraits(Traits t)
	{
		// These methods must be synchronized with ensureCapacity()
		if ((attributes & TRIANGLESET) != 0)
			t.array[index[BITTRIANGLES]] = new LinkedHashSet<Triangle>();
		else if ((attributes & TRIANGLES) != 0)
			t.array[index[BITTRIANGLES]] = new ArrayList<Triangle>();
		if ((attributes & NODESET) != 0)
			t.array[index[BITNODES]] = new LinkedHashSet<Vertex>();
		else if ((attributes & NODES) != 0)
			t.array[index[BITNODES]] = new ArrayList<Vertex>();
		if ((attributes & GROUPLIST) != 0)
			t.array[index[BITGROUPS]] = new ArrayList();
		if ((attributes & KDTREE) != 0)
			t.array[index[BITKDTREE]] = new KdTree(dimension);
	}

	/**
	 * Resizes internal collections of vertices and triangles.
	 *
	 * @param triangles  desired number of triangles
	 * @param t  mesh traits
	 */
	public void ensureCapacity(int triangles, Traits t)
	{
		// These methods must be synchronized with subInitTraits()
		if ((attributes & TRIANGLESET) != 0)
		{
			Collection<Triangle> oldList = (Collection<Triangle>) t.array[index[BITTRIANGLES]];
			if (oldList.size() < triangles)
			{
				LinkedHashSet<Triangle> newList = new LinkedHashSet<Triangle>(triangles);
				newList.addAll(oldList);
				oldList.clear();
				t.array[index[BITTRIANGLES]] = newList;
			}
		}
		else if ((attributes & TRIANGLES) != 0)
		{
			ArrayList<Triangle> oldList = (ArrayList<Triangle>) t.array[index[BITTRIANGLES]];
			oldList.ensureCapacity(triangles);
		}
		int nodes = (int) (0.51 * triangles);
		if ((attributes & NODESET) != 0)
		{
			Collection<Vertex> oldList = (Collection<Vertex>) t.array[index[BITNODES]];
			if (oldList.size() < nodes)
			{
				LinkedHashSet<Vertex> newList = new LinkedHashSet<Vertex>(nodes);
				newList.addAll(oldList);
				oldList.clear();
				t.array[index[BITNODES]] = newList;
			}
		}
		else if ((attributes & NODES) != 0)
		{
			ArrayList<Triangle> oldList = (ArrayList<Triangle>) t.array[index[BITNODES]];
			oldList.ensureCapacity(nodes);
		}
	}

	/**
	 * Adds {@link VertexTraitsBuilder}, {@link HalfEdgeTraitsBuilder} or {@link TriangleTraitsBuilder}
	 * instance to current mesh traits.
	 *
	 * @param t   traits builder
	 * @return {@link KdTree} instance
	 */
	public MeshTraitsBuilder add(TraitsBuilder t)
	{
		if (t instanceof VertexTraitsBuilder)
			vertexTraitsBuilder = (VertexTraitsBuilder) t;
		else if (t instanceof HalfEdgeTraitsBuilder)
			halfedgeTraitsBuilder = (HalfEdgeTraitsBuilder) t;
		else if (t instanceof TriangleTraitsBuilder)
			triangleTraitsBuilder = (TriangleTraitsBuilder) t;
		else
			throw new RuntimeException();
		return this;
	}

	/**
	 * Returns {@link VertexTraitsBuilder} instance.
	 *
	 * @return {@link VertexTraitsBuilder} instance
	 */
	public VertexTraitsBuilder getVertexTraitsBuilder()
	{
		return vertexTraitsBuilder;
	}

	/**
	 * Returns {@link HalfEdgeTraitsBuilder} instance.
	 *
	 * @return {@link HalfEdgeTraitsBuilder} instance
	 */
	public HalfEdgeTraitsBuilder getHalfEdgeTraitsBuilder()
	{
		return halfedgeTraitsBuilder;
	}

	/**
	 * Returns {@link TriangleTraitsBuilder} instance.
	 *
	 * @return {@link TriangleTraitsBuilder} instance
	 */
	public TriangleTraitsBuilder getTriangleTraitsBuilder()
	{
		return triangleTraitsBuilder;
	}

	/**
	 * Returns default 2D <code>MeshTraitsBuilder</code> instance.  This instance
	 * calls following methods: {@link #addTriangleList}, {@link #addKdTree}
	 * and {@link TriangleTraitsBuilder#addVirtualHalfEdge}. It is implicitly used when
	 * calling {@link org.jcae.mesh.amibe.patch.Mesh2D} constructor without
	 * {@link MeshTraitsBuilder} argument.
	 */
	public static final MeshTraitsBuilder getDefault2D()
	{
		MeshTraitsBuilder ret = new MeshTraitsBuilder();
		ret.addTriangleList();
		ret.addKdTree(2);
		ret.triangleTraitsBuilder = new TriangleTraitsBuilder();
		ret.triangleTraitsBuilder.addVirtualHalfEdge();
		return ret;
	}

	/**
	 * Returns default 3D <code>MeshTraitsBuilder</code> instance.  This instance
	 * calls following methods: {@link #addTriangleSet} and
	 * {@link TriangleTraitsBuilder#addHalfEdge}.  It is implicitly used when
	 * calling {@link org.jcae.mesh.amibe.ds.Mesh} constructor without
	 * {@link MeshTraitsBuilder} argument.
	 */
	public static final MeshTraitsBuilder getDefault3D()
	{
		MeshTraitsBuilder ret = new MeshTraitsBuilder();
		ret.addTriangleSet();
		ret.triangleTraitsBuilder = new TriangleTraitsBuilder();
		ret.triangleTraitsBuilder.addHalfEdge();
		return ret;
	}

}
