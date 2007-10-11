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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.amibe.algos3d;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import java.util.Map;
import java.util.HashMap;
import static org.junit.Assert.*;
import org.junit.Test;

public class DecimateHalfEdgeTest
{
	private Mesh mesh;
	private Vertex [] v;
	private Triangle [] T;

	private void create3x4Shell()
	{
		/*
		 *  v9        v10        v11
		 *   +---------+---------+
		 *   | \       |       / |
		 *   |   \  T9 | T10 /   |
		 *   |     \   |   /     |
		 *   |  T8   \ | /   T11 |
		 *v6 +---------+---------+ v8
		 *   | \       |v7     / |
		 *   |   \  T5 | T6  /   |
		 *   |     \   |   /     |
		 *   |  T4   \ | /   T7  |
		 *v3 +---------+---------+ v5
		 *   | \       |v4     / |
		 *   |   \  T1 | T2  /   |
		 *   |     \   |   /     |
		 *   |  T0   \ | /   T3  |
		 *   +---------+---------+
		 *   v0        v1       v2
		 *
		 */
		v = new Vertex[12];
		for (int i = 0; i < 4; i++)
		{
			v[3*i]   = (Vertex) mesh.createVertex(-1.0, i, 0.0);
			v[3*i+1] = (Vertex) mesh.createVertex(0.0, i, 0.0);
			v[3*i+2] = (Vertex) mesh.createVertex(1.0, i, 0.0);
		}
		for (int i = 0; i < v.length; i++)
			v[i].setLabel(i);

		T = new Triangle[12];
		for (int i = 0; i < 3; i++)
		{
			T[4*i]   = (Triangle) mesh.createTriangle(v[3*i], v[3*i+1], v[3*i+3]);
			T[4*i+1] = (Triangle) mesh.createTriangle(v[3*i+1], v[3*i+4], v[3*i+3]);
			T[4*i+2] = (Triangle) mesh.createTriangle(v[3*i+5], v[3*i+4], v[3*i+1]);
			T[4*i+3] = (Triangle) mesh.createTriangle(v[3*i+1], v[3*i+2], v[3*i+5]);
			v[3*i].setLink(T[4*i]);
			v[3*i+1].setLink(T[4*i]);
			v[3*i+2].setLink(T[4*i+3]);
		}
		v[9].setLink(T[9]);
		v[10].setLink(T[9]);
		v[11].setLink(T[11]);
		int cnt = 0;
		for (Triangle t: T)
		{
			mesh.add(t);
			t.setGroupId(cnt);
			cnt++;
		}
	}
	
	private void buildMeshNM()
	{
		create3x4Shell();
		// pi/2 clockwise rotation of vertices v around y
		Vertex [] vy = new Vertex[12];
		int label = 12;
		for (int i = 0; i < v.length; i++)
		{
			if (i%3 == 1)
				vy[i]   = v[i];
			else
			{
				double [] xyz = v[i].getUV();
				vy[i]   = (Vertex) mesh.createVertex(-xyz[2], xyz[1], xyz[0]);
				vy[i].setLabel(label);
				label++;
			}
		}
		Triangle [] Ty = new Triangle[12];
		for (int i = 0; i < 3; i++)
		{
			Ty[4*i]   = (Triangle) mesh.createTriangle(vy[3*i], vy[3*i+1], vy[3*i+3]);
			Ty[4*i+1] = (Triangle) mesh.createTriangle(vy[3*i+1], vy[3*i+4], vy[3*i+3]);
			Ty[4*i+2] = (Triangle) mesh.createTriangle(vy[3*i+5], vy[3*i+4], vy[3*i+1]);
			Ty[4*i+3] = (Triangle) mesh.createTriangle(vy[3*i+1], vy[3*i+2], vy[3*i+5]);
			vy[3*i].setLink(Ty[4*i]);
			vy[3*i+1].setLink(Ty[4*i]);
			vy[3*i+2].setLink(Ty[4*i+3]);
		}
		vy[9].setLink(Ty[9]);
		vy[10].setLink(Ty[9]);
		vy[11].setLink(Ty[11]);
		for (Triangle t: Ty)
			mesh.add(t);
		int cnt = T.length;
		for (Triangle t: Ty)
		{
			t.setGroupId(cnt);
			cnt++;
		}
		Vertex [] vTotal = new Vertex[20];
		System.arraycopy(v, 0, vTotal, 0, v.length);
		for (int i = 0; i < 4; i++)
			vTotal[12+i] = vy[3*i];
		for (int i = 0; i < 4; i++)
			vTotal[16+i] = vy[3*i+2];
		mesh.buildAdjacency(vTotal, -1.0);
		assertTrue("Mesh is not valid", mesh.isValid());
	}
	
	@Test public void test1()
	{
		final Map<String, String> options = new HashMap<String, String>();
		options.put("size", "0.1");
		mesh = new Mesh();
		create3x4Shell();
		mesh.buildAdjacency(v, -1.0);
		new DecimateHalfEdge(mesh, options).compute();
		int res = DecimateHalfEdge.countInnerTriangles(mesh);
		int expected = 2;
		if (res != expected)
		{
			new DecimateHalfEdge(mesh, options).compute();
			res = DecimateHalfEdge.countInnerTriangles(mesh);
		}
		assertTrue("Final number of triangles: "+res, res == expected);
	}

	@Test public void testNM1()
	{
		final Map<String, String> options = new HashMap<String, String>();
		options.put("size", "0.1");
		mesh = new Mesh();
		buildMeshNM();
		new DecimateHalfEdge(mesh, options).compute();
		int res = DecimateHalfEdge.countInnerTriangles(mesh);
		int expected = 8;
		if (res != 2)
		{
			new DecimateHalfEdge(mesh, options).compute();
			res = DecimateHalfEdge.countInnerTriangles(mesh);
		}
		assertTrue("Final number of triangles: "+res, res == expected);
	}
}
