/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2006, by EADS CRC

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

import org.jcae.mesh.amibe.ds.AbstractTriangle;
import org.jcae.mesh.amibe.ds.AbstractVertex;
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
	private static final int TRIANGLESET      = 30;
	private static final int NODESET          = 31;

	private VertexTraitsBuilder vertexTraitsBuilder = new VertexTraitsBuilder();
	private HalfEdgeTraitsBuilder halfedgeTraitsBuilder = new HalfEdgeTraitsBuilder();
	private TriangleTraitsBuilder triangleTraitsBuilder = new TriangleTraitsBuilder();

	private int dimension;

	public MeshTraitsBuilder()
	{
		super();
	}

	public MeshTraitsBuilder addTriangleList()
	{
		attributes |= TRIANGLES;
		attributes &= ~TRIANGLESET;
		return this;
	}

	public MeshTraitsBuilder addTriangleSet()
	{
		attributes |= TRIANGLES;
		attributes |= TRIANGLESET;
		return this;
	}

	@SuppressWarnings("unchecked")
	public Collection<AbstractTriangle> getTriangles(Traits t)
	{
		if ((attributes & TRIANGLES) != 0)
			return (Collection<AbstractTriangle>) t.array[index[BITTRIANGLES]];
		return null;
	}

	public MeshTraitsBuilder addNodeList()
	{
		attributes |= NODES;
		attributes &= ~NODESET;
		return this;
	}

	public MeshTraitsBuilder addNodeSet()
	{
		attributes |= NODES;
		attributes |= NODESET;
		return this;
	}

	@SuppressWarnings("unchecked")
	public Collection<AbstractVertex> getNodes(Traits t)
	{
		if ((attributes & NODES) != 0)
			return (Collection<AbstractVertex>) t.array[index[BITNODES]];
		return null;
	}

	public boolean hasNodes()
	{
		return hasCapability(NODES);
	}

	public boolean hasTriangles()
	{
		return hasCapability(TRIANGLES);
	}

	public boolean hasKdTree()
	{
		return hasCapability(KDTREE);
	}

	public MeshTraitsBuilder addGroupList()
	{
		attributes |= GROUPLIST;
		return this;
	}

	public Collection getGroups(Traits t)
	{
		if ((attributes & GROUPLIST) != 0)
			return (Collection) t.array[index[BITGROUPS]];
		return null;
	}

	public MeshTraitsBuilder addKdTree(int d)
	{
		attributes |= KDTREE;
		dimension = d;
		return this;
	}

	public KdTree getKdTree(Traits t)
	{
		if ((attributes & KDTREE) != 0)
			return (KdTree) t.array[index[BITKDTREE]];
		return null;
	}

	@Override
	protected void subInitTraits(Traits t)
	{
		if ((attributes & TRIANGLESET) != 0)
			t.array[index[BITTRIANGLES]] = new LinkedHashSet<AbstractTriangle>();
		else if ((attributes & TRIANGLES) != 0)
			t.array[index[BITTRIANGLES]] = new ArrayList<AbstractTriangle>();
		if ((attributes & NODESET) != 0)
			t.array[index[BITNODES]] = new LinkedHashSet<AbstractVertex>();
		else if ((attributes & NODES) != 0)
			t.array[index[BITNODES]] = new ArrayList<AbstractVertex>();
		if ((attributes & GROUPLIST) != 0)
			t.array[index[BITGROUPS]] = new ArrayList();
		if ((attributes & KDTREE) != 0)
			t.array[index[BITKDTREE]] = new KdTree(dimension);
	}

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

	public VertexTraitsBuilder getVertexTraitsBuilder()
	{
		return vertexTraitsBuilder;
	}

	public HalfEdgeTraitsBuilder getHalfEdgeTraitsBuilder()
	{
		return halfedgeTraitsBuilder;
	}

	public TriangleTraitsBuilder getTriangleTraitsBuilder()
	{
		return triangleTraitsBuilder;
	}

	/**
	 * Returns default 2D <code>MeshTraitsBuilder</code> instance.  This instance
	 * calls following methods: {@link #addTriangleList}, {@link #addKdTree}
	 * and {@link TriangleTraitsBuilder#addShallowHalfEdge}. It is implicitly used when
	 * calling {@link org.jcae.mesh.amibe.patch.Mesh2D} constructor without
	 * {@link MeshTraitsBuilder} argument.
	 */
	public static final MeshTraitsBuilder getDefault2D()
	{
		MeshTraitsBuilder ret = new MeshTraitsBuilder();
		ret.addTriangleList();
		ret.addKdTree(2);
		ret.triangleTraitsBuilder = new TriangleTraitsBuilder();
		ret.triangleTraitsBuilder.addShallowHalfEdge();
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
