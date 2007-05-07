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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Collection;

public class MeshTraitsBuilder extends TraitsBuilder
{
	private static final int BITTRIANGLELIST = 8;
	private static final int BITTRIANGLESET  = 9;
	private static final int BITNODELIST     = 10;
	private static final int BITNODESET      = 11;
	private static final int BITGROUPLIST    = 12;

	public static final int TRIANGLELIST     = 1 << BITTRIANGLELIST;
	public static final int TRIANGLESET      = 1 << BITTRIANGLESET;
	public static final int NODELIST         = 1 << BITNODELIST;
	public static final int NODESET          = 1 << BITNODESET;
	public static final int GROUPLIST        = 1 << BITGROUPLIST;

	private VertexTraitsBuilder vertexTraitsBuilder = null;
	private HalfEdgeTraitsBuilder halfedgeTraitsBuilder = null;
	private TriangleTraitsBuilder triangleTraitsBuilder = null;

	public MeshTraitsBuilder()
	{
		super();
	}

	public void addTriangleList()
	{
		attributes |= TRIANGLELIST;
		attributes &= ~TRIANGLESET;
	}

	public void addTriangleSet()
	{
		attributes |= TRIANGLESET;
		attributes &= ~TRIANGLELIST;
	}

	public Collection getTriangles(Traits t)
	{
		if ((attributes & TRIANGLELIST) != 0)
			return (Collection) t.array[index[BITTRIANGLELIST]];
		else if ((attributes & TRIANGLESET) != 0)
			return (Collection) t.array[index[BITTRIANGLESET]];
		else
			return null;
	}

	public void addNodeList()
	{
		attributes |= NODELIST;
		attributes &= ~NODESET;
	}

	public void addNodeSet()
	{
		attributes |= NODESET;
		attributes &= ~NODELIST;
	}

	public Collection getNodes(Traits t)
	{
		if ((attributes & NODELIST) != 0)
			return (Collection) t.array[index[BITNODELIST]];
		else if ((attributes & NODESET) != 0)
			return (Collection) t.array[index[BITNODESET]];
		else
			return null;
	}

	public void addGroupList()
	{
		attributes |= GROUPLIST;
	}

	public Collection getGroups(Traits t)
	{
		if ((attributes & GROUPLIST) != 0)
			return (Collection) t.array[index[BITGROUPLIST]];
		else
			return null;
	}

	protected void subInitTraits(Traits t)
	{
		if ((attributes & TRIANGLELIST) != 0)
			t.array[index[BITTRIANGLELIST]] = new ArrayList();
		else if ((attributes & TRIANGLESET) != 0)
			t.array[index[BITTRIANGLESET]] = new LinkedHashSet();
		if ((attributes & NODELIST) != 0)
			t.array[index[BITNODELIST]] = new ArrayList();
		else if ((attributes & NODESET) != 0)
			t.array[index[BITNODESET]] = new LinkedHashSet();
		if ((attributes & GROUPLIST) != 0)
			t.array[index[BITGROUPLIST]] = new ArrayList();
	}

	public void add(TraitsBuilder t)
	{
		if (t instanceof VertexTraitsBuilder)
			vertexTraitsBuilder = (VertexTraitsBuilder) t;
		else if (t instanceof HalfEdgeTraitsBuilder)
			halfedgeTraitsBuilder = (HalfEdgeTraitsBuilder) t;
		else if (t instanceof TriangleTraitsBuilder)
			triangleTraitsBuilder = (TriangleTraitsBuilder) t;
		else
			throw new RuntimeException();
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
}
