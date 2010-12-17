/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2010, by EADS France

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

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.EuclidianMetric3D;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class RemeshPolylineTest
{
	private Mesh bgMesh;
	private List<Vertex> v;
	private List<EuclidianMetric3D> m;

	@Before
	public void setup()
	{
		bgMesh = new Mesh();
		v = new ArrayList<Vertex>();
		v.add(bgMesh.createVertex(0.0, 0.0, 0.0));
		v.add(bgMesh.createVertex(100.0, 0.0, 0.0));
		m = new ArrayList<EuclidianMetric3D>();
		m.add(new EuclidianMetric3D(100.0));
		m.add(new EuclidianMetric3D(100.0));
	}

	@Test public void test2exact()
	{
		m.clear();
		m.add(new EuclidianMetric3D(100.0));
		m.add(new EuclidianMetric3D(100.0));

		List<Vertex> result = new RemeshPolyline(bgMesh, v, m).compute();
		assertTrue("Expected 2 vertices, found "+result.size(), 2 == result.size());
	}

	@Test public void test2lower()
	{
		m.clear();
		m.add(new EuclidianMetric3D(100.0));
		m.add(new EuclidianMetric3D(100.0));

		List<Vertex> result = new RemeshPolyline(bgMesh, v, m).compute();
		assertTrue("Expected 2 vertices, found "+result.size(), 2 == result.size());
	}

	@Test public void test2upper()
	{
		m.clear();
		m.add(new EuclidianMetric3D(500.0));
		m.add(new EuclidianMetric3D(500.0));

		List<Vertex> result = new RemeshPolyline(bgMesh, v, m).compute();
		assertTrue("Expected 2 vertices, found "+result.size(), 2 == result.size());
	}

	@Test public void test101exact()
	{
		m.clear();
		m.add(new EuclidianMetric3D(1));
		m.add(new EuclidianMetric3D(1));

		List<Vertex> result = new RemeshPolyline(bgMesh, v, m).compute();
		assertTrue("Expected 101 vertices, found "+result.size(), 101 == result.size());
	}

	@Test public void test1001exact()
	{
		m.clear();
		m.add(new EuclidianMetric3D(0.1));
		m.add(new EuclidianMetric3D(0.1));

		List<Vertex> result = new RemeshPolyline(bgMesh, v, m).compute();
		assertTrue("Expected 1001 vertices, found "+result.size(), 1001 == result.size());
	}

}
