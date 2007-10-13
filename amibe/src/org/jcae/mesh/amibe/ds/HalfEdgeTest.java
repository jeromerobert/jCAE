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
	@Test public void nextOriginLoop()
	{
		buildMesh2();
		super.nextOriginLoop();
	}
	
	// Unit tests for {@link AbstractHalfEdge#canCollapse} on
	// manifold meshes.
	@Test public void canCollapse474()
	{
		buildMesh2();
		super.canCollapse(v[4], v[7], v[4], true);
	}
	// Check whether vertices 4 and 7 can be collapsed into vertex 5.
	// Expected result is <code>false</code> because this would give
	// degenerated triangles.
	@Test public void canCollapse475()
	{
		buildMesh2();
		super.canCollapse(v[4], v[7], v[5], false);
	}
	// Check whether vertices 3 and 6 can be collapsed into vertex 6.
	// Expected result is <code>false</code> because edge is outer.
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
	// Check whether vertices 1 and 3 can be collapsed into vertex 3.
	// Expected result is <code>false</code> because topology is
	// modified.
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
		super.canCollapse(v[1], v[4], v[1], false);
	}
	@Test public void canCollapseTopo010()
	{
		buildMeshTopo();
		super.canCollapse(v[0], v[1], v[0], false);
	}

	// Unit tests for {@link AbstractHalfEdge#collapse} on
	// manifold meshes.
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
	
	// Unit tests for {@link AbstractHalfEdge#swap} on
	// manifold meshes.
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

	// Unit tests for {@link AbstractHalfEdge#split} on
	// manifold meshes.
	@Test public void split47()
	{
		buildMesh2();
		Vertex n = (Vertex) mesh.createVertex(0.0, 1.5, 0.0);
		super.split(v[4], v[7], n);
	}
	@Test public void split63()
	{
		buildMesh2();
		Vertex n = (Vertex) mesh.createVertex(-1.0, 1.5, 0.0);
		super.split(v[6], v[3], n);
	}
	@Test(expected= IllegalArgumentException.class) public void split36()
	{
		buildMesh2();
		Vertex n = (Vertex) mesh.createVertex(-1.0, 1.5, 0.0);
		super.split(v[3], v[6], n);
	}
	@Test public void split45()
	{
		buildMesh2();
		Vertex n = (Vertex) mesh.createVertex(0.5, 1.0, 0.0);
		super.split(v[4], v[5], n);
	}

	// Verify that non-manifold vertices are bound to expected
	// number of fans.
	@Test public void countVertexLinks0()
	{
		buildMeshNM();
		super.countVertexLinks(v[0], 4);
	}
	@Test public void countVertexLinks1()
	{
		buildMeshNM();
		super.countVertexLinks(v[1], 1);
	}
	@Test public void countVertexLinks2()
	{
		buildMeshNM();
		super.countVertexLinks(v[2], 4);
	}

	// Verify that non-manifold edges are bound to expected
	// number of fans.
	@Test public void countEdgeLinks31()
	{
		buildMeshNM();
		super.countEdgeLinks(v[3], v[1], 1);
	}
	@Test public void countEdgeLinks12()
	{
		buildMeshNM();
		super.countEdgeLinks(v[1], v[2], 2);
	}
	@Test public void countEdgeLinks42()
	{
		buildMeshNM();
		super.countEdgeLinks(v[4], v[2], 4);
	}
	@Test public void countEdgeLinks20()
	{
		buildMeshNM();
		super.countEdgeLinks(v[2], v[0], 4);
	}

	// Unit tests for {@link AbstractHalfEdge#fanIterator} on
	// non-manifold meshes.
	@Test public void countFanIterator31()
	{
		buildMeshNM();
		super.countFanIterator(v[3], v[1], 1);
	}
	@Test public void countFanIterator42()
	{
		buildMeshNM();
		super.countFanIterator(v[4], v[2], 4);
	}
	@Test public void countFanIterator20()
	{
		buildMeshNM();
		super.countFanIterator(v[2], v[0], 4);
	}

	// Unit tests for {@link AbstractHalfEdge#canCollapse} on
	// non-manifold meshes.
	@Test public void canCollapseNM424()
	{
		buildMeshNM();
		super.canCollapse(v[4], v[2], v[4], true);
	}
	@Test public void canCollapseNM423()
	{
		buildMeshNM();
		super.canCollapse(v[4], v[2], v[3], false);
	}
	@Test public void canCollapseNM355()
	{
		buildMeshNM();
		super.canCollapse(v[3], v[5], v[5], true);
	}
	@Test public void canCollapseNM535()
	{
		buildMeshNM();
		super.canCollapse(v[5], v[3], v[5], false);
	}
	@Test public void canCollapseNM565()
	{
		buildMeshNM();
		super.canCollapse(v[5], v[6], v[5], false);
	}

	// Unit tests for {@link AbstractHalfEdge#collapse} on
	// non-manifold meshes.
	// Check collapsing non-manifold edge
	@Test public void collapseNM848()
	{
		buildMeshNM44();
		super.collapse(v[8], v[4], v[8]);
	}
	// Check when non-manifold edge is prevOrigin()
	@Test public void collapseNM898()
	{
		buildMeshNM44();
		super.collapse(v[8], v[9], v[8]);
	}
	// Check when non-manifold edge is nextDest()
	@Test public void collapseNM944()
	{
		buildMeshNM44();
		super.collapse(v[9], v[4], v[4]);
	}
	// Check when non-manifold edge is next()
	@Test public void collapseNM494()
	{
		buildMeshNM44();
		super.collapse(v[4], v[9], v[4]);
	}
	// Check when non-manifold edge is prev()
	@Test public void collapseNM988()
	{
		buildMeshNM44();
		super.collapse(v[9], v[8], v[8]);
	}
	// Check when apical vertex is non-manifold
	@Test public void collapseNM595()
	{
		buildMeshNM44();
		super.collapse(v[5], v[9], v[5]);
	}
	// Check when symmetric apical vertex is non-manifold
	@Test public void collapseNM959()
	{
		buildMeshNM44();
		super.collapse(v[9], v[5], v[9]);
	}
	
	@Test(expected= IllegalArgumentException.class) public void collapseNM533()
	{
		buildMeshNM();
		AbstractHalfEdge e = find(v[5], v[3]);
		e.collapse(mesh, v[3]);
	}
	@Test public void collapseNM131()
	{
		buildMeshNM();
		super.collapse(v[1], v[3], v[1]);
	}
	
	// Unit tests for {@link AbstractHalfEdge#swap} on
	// non-manifold meshes.
	@Test(expected= IllegalArgumentException.class) public void swapNM42()
	{
		buildMeshNM();
		super.swap(v[4], v[2]);
	}
	@Test(expected= IllegalArgumentException.class) public void swapNM24()
	{
		buildMeshNM();
		super.swap(v[2], v[4]);
	}
	@Test(expected= IllegalArgumentException.class) public void swapNM53()
	{
		buildMeshNM();
		super.swap(v[5], v[3]);
	}
	@Test(expected= IllegalArgumentException.class) public void swapNM3Outer()
	{
		buildMeshNM();
		super.swap(v[3], mesh.outerVertex);
	}
	// Check when non-manifold edge is prevOrigin()
	@Test public void swapNM65()
	{
		buildMeshNM();
		super.swap(v[6], v[5]);
	}
	// Check when non-manifold edge is nextDest()
	@Test public void swapNM54()
	{
		buildMeshNM();
		super.swap(v[5], v[4]);
	}
	// Check when non-manifold edge is next()
	@Test public void swapNM56()
	{
		buildMeshNM();
		super.swap(v[5], v[6]);
	}
	// Check when non-manifold edge is prev()
	@Test public void swapNM45()
	{
		buildMeshNM();
		super.swap(v[4], v[5]);
	}
	// Check when apical vertex is non-manifold
	@Test public void swapNM47()
	{
		createMxNShell(3, 3);
		rotateMxNShellAroundY(3, 3, 90);
		rotateMxNShellAroundY(3, 3, 180);
		rotateMxNShellAroundY(3, 3, 270);
		mesh.buildAdjacency();
		super.swap(v[4], v[7]);
	}
	// Check when symmetric apical vertex is non-manifold
	@Test public void swapNM74()
	{
		createMxNShell(3, 3);
		rotateMxNShellAroundY(3, 3, 90);
		rotateMxNShellAroundY(3, 3, 180);
		rotateMxNShellAroundY(3, 3, 270);
		mesh.buildAdjacency();
		super.swap(v[7], v[4]);
	}

	// Unit tests for {@link AbstractHalfEdge#split} on
	// non-manifold meshes.
	@Test public void splitNM42()
	{
		buildMeshNM();
		Vertex n = (Vertex) mesh.createVertex(0.0, 1.5, 0.0);
		super.split(v[4], v[2], n);
	}
	@Test public void splitNM35()
	{
		buildMeshNM();
		Vertex n = (Vertex) mesh.createVertex(-1.0, 1.5, 0.0);
		super.split(v[3], v[5], n);
	}
	@Test(expected= IllegalArgumentException.class) public void splitNM53()
	{
		buildMeshNM();
		Vertex n = (Vertex) mesh.createVertex(-1.0, 1.5, 0.0);
		super.split(v[5], v[3], n);
	}
	@Test public void splitNM45()
	{
		buildMeshNM();
		Vertex n = (Vertex) mesh.createVertex(0.5, 2.0, 0.0);
		super.split(v[4], v[5], n);
	}


}
