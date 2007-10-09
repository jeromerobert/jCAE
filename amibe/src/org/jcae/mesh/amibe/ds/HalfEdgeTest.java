/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2007, by EADS France

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

package org.jcae.mesh.amibe.ds;

import org.jcae.mesh.amibe.traits.TriangleTraitsBuilder;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class HalfEdgeTest extends AbstractHalfEdgeTest
{
	@Before public void createMesh()
	{
		TriangleTraitsBuilder ttb = new TriangleTraitsBuilder();
		ttb.addHalfEdge();
		MeshTraitsBuilder mtb = new MeshTraitsBuilder();
		mtb.addTriangleList();
		mtb.add(ttb);
		mesh = new Mesh(mtb);
	}
	
	@Override
	protected AbstractHalfEdge find(Vertex v1, Vertex v2)
	{
		if (v1.getLink() instanceof Triangle)
		{
			AbstractHalfEdge ret = findSameFan(v1, v2, (Triangle) v1.getLink());
			if (ret == null)
				throw new RuntimeException();
			return ret;
		}
		Triangle [] tArray = (Triangle []) v1.getLink();
		for (Triangle start: tArray)
		{
			AbstractHalfEdge f = findSameFan(v1, v2, start);
			if (f != null)
				return f;
		}
		throw new RuntimeException();
	}

	private AbstractHalfEdge findSameFan(Vertex v1, Vertex v2, Triangle start)
	{
		AbstractHalfEdge ret = ((TriangleHE) start).getHalfEdge();
		if (ret == null)
			throw new RuntimeException();
		if (ret.destination() == v1)
			ret = ret.next();
		else if (ret.apex() == v1)
			ret = ret.prev();
		assertTrue(ret.origin() == v1);
		Vertex d = ret.destination();
		if (d == v2)
			return ret;
		do
		{
			ret = ret.nextOriginLoop();
			if (ret.destination() == v2)
				return ret;
		}
		while (ret.destination() != d);
		return null;
	}
	
	@Override
	@Test public void nextOriginLoop()
	{
		buildMesh2();
		super.nextOriginLoop();
	}
	
	/**
	 * Unit tests for {@link AbstractHalfEdge#canCollapse} on
	 * manifold meshes.
	 */
	@Test public void canCollapse474()
	{
		buildMesh2();
		super.canCollapse(v[4], v[7], v[4], true);
	}
	/**
	 * Check whether vertices 4 and 7 can be collapsed into vertex 5.
	 * Expected result is <code>false</code> because this would give
	 * degenerated triangles.
	 */
	@Test public void canCollapse475()
	{
		buildMesh2();
		super.canCollapse(v[4], v[7], v[5], false);
	}
	/**
	 * Check whether vertices 3 and 6 can be collapsed into vertex 6.
	 * Expected result is <code>false</code> because edge is outer.
	 */
	@Test public void canCollapse366()
	{
		buildMesh2();
		super.canCollapse(v[3], v[6], v[6], false);
	}
	@Test public void canCollapse636()
	{
		buildMesh2();
		super.canCollapse(v[6], v[3], v[6], true);
	}
	/**
	 * Check whether vertices 1 and 3 can be collapsed into vertex 3.
	 * Expected result is <code>false</code> because topology is
	 * modified.
	 */
	@Test public void canCollapse133()
	{
		buildMesh2();
		super.canCollapse(v[1], v[3], v[3], false);
	}

	@Test public void canCollapseTopo030()
	{
		// With simple mesh from buildMesh(), vertices 0 and
		// 3 must not be merged, otherwise a non-manifold vertex
		// is created.
		buildMesh();
		super.canCollapse(v[0], v[3], v[0], false);
	}
	@Test public void canCollapseTopo010()
	{
		buildMeshTopo();
		super.canCollapse(v[0], v[1], v[0], false);
	}

	/**
	 * Unit tests for {@link AbstractHalfEdge#collapse} on
	 * manifold meshes.
	 */
	@Test public void collapse474()
	{
		buildMesh2();
		super.collapse(v[4], v[7], v[4]);
	}
	@Test public void collapse455()
	{
		buildMesh2();
		super.collapse(v[4], v[5], v[5]);
	}
	@Test public void collapse636()
	{
		buildMesh2();
		super.collapse(v[6], v[3], v[6]);
	}
	@Test(expected= IllegalArgumentException.class) public void collapse366()
	{
		buildMesh2();
		AbstractHalfEdge e = find(v[3], v[6]);
		e.collapse(mesh, v[6]);
	}
	@Test public void collapse300()
	{
		buildMesh2();
		super.collapse(v[3], v[0], v[0]);
	}
	
	/**
	 * Unit tests for {@link AbstractHalfEdge#swap} on
	 * manifold meshes.
	 */
	@Test public void swap47()
	{
		buildMesh2();
		super.swap(v[4], v[7]);
	}
	@Test(expected= IllegalArgumentException.class) public void swap36()
	{
		buildMesh2();
		super.swap(v[3], v[6]);
	}
	@Test(expected= IllegalArgumentException.class) public void swap3Outer()
	{
		buildMesh2();
		super.swap(v[3], mesh.outerVertex);
	}

	/**
	 * Unit tests for {@link AbstractHalfEdge#split} on
	 * manifold meshes.
	 */
	@Test public void split47()
	{
		buildMesh2();
		Vertex n = (Vertex) mesh.factory.createVertex(0.0, 1.5, 0.0);
		super.split(v[4], v[7], n);
	}
	@Test public void split63()
	{
		buildMesh2();
		Vertex n = (Vertex) mesh.factory.createVertex(-1.0, 1.5, 0.0);
		super.split(v[6], v[3], n);
	}
	@Test(expected= IllegalArgumentException.class) public void split36()
	{
		buildMesh2();
		Vertex n = (Vertex) mesh.factory.createVertex(-1.0, 1.5, 0.0);
		super.split(v[3], v[6], n);
	}
	@Test public void split45()
	{
		buildMesh2();
		Vertex n = (Vertex) mesh.factory.createVertex(0.5, 1.0, 0.0);
		super.split(v[4], v[5], n);
	}

	/**
	 * Verify that non-manifold vertices are bound to expected
	 * number of fans.
	 */
	@Test public void countVertexLinks0()
	{
		buildMeshNM();
		super.countVertexLinks(v[0], 1);
	}
	@Test public void countVertexLinks1()
	{
		buildMeshNM();
		super.countVertexLinks(v[1], 4);
	}
	@Test public void countVertexLinks4()
	{
		buildMeshNM();
		super.countVertexLinks(v[4], 4);
	}

	/**
	 * Verify that non-manifold edges are bound to expected
	 * number of fans.
	 */
	@Test public void countEdgeLinks36()
	{
		buildMeshNM();
		super.countEdgeLinks(v[3], v[6], 1);
	}
	@Test public void countEdgeLinks46()
	{
		buildMeshNM();
		super.countEdgeLinks(v[4], v[6], 2);
	}
	@Test public void countEdgeLinks47()
	{
		buildMeshNM();
		super.countEdgeLinks(v[4], v[7], 4);
	}
	@Test public void countEdgeLinks14()
	{
		buildMeshNM();
		super.countEdgeLinks(v[1], v[4], 4);
	}

	/**
	 * Unit tests for {@link AbstractHalfEdge#fanIterator} on
	 * non-manifold meshes.
	 */
	@Test public void countFanIterator36()
	{
		buildMeshNM();
		super.countFanIterator(v[3], v[6], 1);
	}
	@Test public void countFanIterator47()
	{
		buildMeshNM();
		super.countFanIterator(v[4], v[7], 4);
	}
	@Test public void countFanIterator14()
	{
		buildMeshNM();
		super.countFanIterator(v[1], v[4], 4);
	}

	/**
	 * Unit tests for {@link AbstractHalfEdge#canCollapse} on
	 * non-manifold meshes.
	 */
	@Test public void canCollapseNM474()
	{
		buildMeshNM();
		super.canCollapse(v[4], v[7], v[4], true);
	}
	@Test public void canCollapseNM475()
	{
		buildMeshNM();
		super.canCollapse(v[4], v[7], v[5], false);
	}
	@Test public void canCollapseNM366()
	{
		buildMeshNM();
		super.canCollapse(v[3], v[6], v[6], false);
	}
	@Test public void canCollapseNM636()
	{
		buildMeshNM();
		super.canCollapse(v[6], v[3], v[6], true);
	}
	@Test public void canCollapseNM133()
	{
		buildMeshNM();
		super.canCollapse(v[1], v[3], v[3], false);
	}

	/**
	 * Unit tests for {@link AbstractHalfEdge#collapse} on
	 * non-manifold meshes.
	 */
	@Test public void collapseNM474()
	{
		buildMeshNM();
		super.collapse(v[4], v[7], v[4]);
	}
	@Test public void collapseNM455()
	{
		buildMeshNM();
		super.collapse(v[4], v[5], v[5]);
	}
	@Test public void collapseNM545()
	{
		buildMeshNM();
		super.collapse(v[5], v[4], v[5]);
	}
	@Test public void collapseNM433()
	{
		buildMeshNM();
		super.collapse(v[4], v[3], v[3]);
	}
	@Test public void collapseNM343()
	{
		buildMeshNM();
		super.collapse(v[3], v[4], v[3]);
	}
	@Test public void collapseNM636()
	{
		buildMeshNM();
		super.collapse(v[6], v[3], v[6]);
	}
	@Test(expected= IllegalArgumentException.class) public void collapseNM366()
	{
		buildMeshNM();
		AbstractHalfEdge e = find(v[3], v[6]);
		e.collapse(mesh, v[6]);
	}
	@Test public void collapseNM300()
	{
		buildMeshNM();
		super.collapse(v[3], v[0], v[0]);
	}
	
	/**
	 * Unit tests for {@link AbstractHalfEdge#split} on
	 * non-manifold meshes.
	 */
	@Test public void splitNM47()
	{
		buildMeshNM();
		Vertex n = (Vertex) mesh.factory.createVertex(0.0, 1.5, 0.0);
		super.split(v[4], v[7], n);
	}
	@Test public void splitNM63()
	{
		buildMeshNM();
		Vertex n = (Vertex) mesh.factory.createVertex(-1.0, 1.5, 0.0);
		super.split(v[6], v[3], n);
	}
	@Test(expected= IllegalArgumentException.class) public void splitNM36()
	{
		buildMeshNM();
		Vertex n = (Vertex) mesh.factory.createVertex(-1.0, 1.5, 0.0);
		super.split(v[3], v[6], n);
	}
	@Test public void splitNM45()
	{
		buildMeshNM();
		Vertex n = (Vertex) mesh.factory.createVertex(0.5, 1.0, 0.0);
		super.split(v[4], v[5], n);
	}


}
