/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2006  EADS CRC

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

public class VirtualHalfEdgeTest extends AbstractHalfEdgeTest
{
	private VirtualHalfEdge ot = new VirtualHalfEdge();
	@Before public void createMesh()
	{
		TriangleTraitsBuilder ttb = new TriangleTraitsBuilder();
		ttb.addShallowHalfEdge();
		MeshTraitsBuilder mtb = new MeshTraitsBuilder();
		mtb.addTriangleList();
		mtb.add(ttb);
		mesh = new Mesh(mtb);
	}
	
	protected AbstractHalfEdge find(Vertex v1, Vertex v2)
	{
		if (!ot.find(v1, v2))
			throw new RuntimeException();
		return ot;
	}
	
	@Test public void nextOriginLoop()
	{
		buildMesh2();
		super.nextOriginLoop();
	}
	
	@Test public void canCollapse474()
	{
		buildMesh2();
		super.canCollapse(v[4], v[7], v[4], true);
	}
	@Test public void canCollapse475()
	{
		buildMesh2();
		super.canCollapse(v[4], v[7], v[5], false);
	}
	@Test public void canCollapse366()
	{
		buildMesh2();
		super.canCollapse(v[3], v[6], v[6], true);
	}
	@Test public void canCollapse133()
	{
		buildMesh2();
		super.canCollapse(v[1], v[3], v[3], false);
	}

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
	@Test public void collapse366()
	{
		buildMesh2();
		super.collapse(v[3], v[6], v[6]);
	}
	@Test public void collapse030()
	{
		buildMesh2();
		super.collapse(v[0], v[3], v[0]);
	}
	
}
