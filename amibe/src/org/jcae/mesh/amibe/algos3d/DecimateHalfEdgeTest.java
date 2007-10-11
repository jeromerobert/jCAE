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

	private void create3xNShell(int n)
	{
		/*
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
		v = new Vertex[3*n];
		for (int i = 0; i < n; i++)
		{
			v[3*i]   = (Vertex) mesh.createVertex(-1.0, i, 0.0);
			v[3*i+1] = (Vertex) mesh.createVertex(0.0, i, 0.0);
			v[3*i+2] = (Vertex) mesh.createVertex(1.0, i, 0.0);
		}
		for (int i = 0; i < v.length; i++)
			v[i].setLabel(i);

		T = new Triangle[4*(n-1)];
		for (int i = 0; i < n-1; i++)
		{
			T[4*i]   = (Triangle) mesh.createTriangle(v[3*i], v[3*i+1], v[3*i+3]);
			T[4*i+1] = (Triangle) mesh.createTriangle(v[3*i+1], v[3*i+4], v[3*i+3]);
			T[4*i+2] = (Triangle) mesh.createTriangle(v[3*i+5], v[3*i+4], v[3*i+1]);
			T[4*i+3] = (Triangle) mesh.createTriangle(v[3*i+1], v[3*i+2], v[3*i+5]);
			v[3*i].setLink(T[4*i]);
			v[3*i+1].setLink(T[4*i]);
			v[3*i+2].setLink(T[4*i+3]);
		}
		v[3*n-3].setLink(T[4*n-7]);
		v[3*n-2].setLink(T[4*n-7]);
		v[3*n-1].setLink(T[4*n-5]);
		int cnt = 0;
		for (Triangle t: T)
		{
			mesh.add(t);
			t.setGroupId(cnt);
			cnt++;
		}
	}
	
	private void rotateNM()
	{
		// pi/2 clockwise rotation of vertices v around y
		Vertex [] vy = new Vertex[v.length];
		int label = v.length;
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
		Triangle [] Ty = new Triangle[T.length];
		for (int i = 0; i < T.length/4; i++)
		{
			Ty[4*i]   = (Triangle) mesh.createTriangle(vy[3*i],   vy[3*i+1], vy[3*i+3]);
			Ty[4*i+1] = (Triangle) mesh.createTriangle(vy[3*i+1], vy[3*i+4], vy[3*i+3]);
			Ty[4*i+2] = (Triangle) mesh.createTriangle(vy[3*i+5], vy[3*i+4], vy[3*i+1]);
			Ty[4*i+3] = (Triangle) mesh.createTriangle(vy[3*i+1], vy[3*i+2], vy[3*i+5]);
			vy[3*i].setLink(Ty[4*i]);
			vy[3*i+1].setLink(Ty[4*i]);
			vy[3*i+2].setLink(Ty[4*i+3]);
		}
		vy[v.length-3].setLink(Ty[T.length-3]);
		vy[v.length-2].setLink(Ty[T.length-3]);
		vy[v.length-1].setLink(Ty[T.length-1]);
		for (Triangle t: Ty)
			mesh.add(t);
		int cnt = T.length;
		for (Triangle t: Ty)
		{
			t.setGroupId(cnt);
			cnt++;
		}
		Vertex [] vTotal = new Vertex[5*v.length/3];
		System.arraycopy(v, 0, vTotal, 0, v.length);
		for (int i = 0; i < v.length/3; i++)
			vTotal[v.length+i] = vy[3*i];
		for (int i = 0; i < v.length/3; i++)
			vTotal[4*v.length/3+i] = vy[3*i+2];
		v = vTotal;
		Triangle [] TTotal = new Triangle[2*T.length];
		System.arraycopy(T, 0, TTotal, 0, T.length);
		System.arraycopy(Ty, 0, TTotal, T.length, T.length);
		T = TTotal;
	}

	@Test public void testNonManifold()
	{
		final Map<String, String> options = new HashMap<String, String>();
		options.put("size", "0.1");
		mesh = new Mesh();
		//  v3         v4        v5
		//   +---------+---------+
		//   | \       |       / |
		//   |   \  T2 | T5  /   |
		//   |     \   |   /     |
		//   |       \ | /       |
		//   |  T1     + v6  T4  |
		//   |       / | \       |
		//   |     /   |   \     |
		//   |   /     |     \   |
		//   | /   T0  |  T3   \ |
		//   +---------+---------+
		//  v0         v1        v2
		v = new Vertex[11];
		v[0] = (Vertex) mesh.createVertex(-1.0, 0.0, 0.0);
		v[1] = (Vertex) mesh.createVertex(0.0, 0.0, 0.0);
		v[2] = (Vertex) mesh.createVertex(1.0, 0.0, 0.0);
		v[3] = (Vertex) mesh.createVertex(-1.0, 2.0, 0.0);
		v[4] = (Vertex) mesh.createVertex(0.0, 2.0, 0.0);
		v[5] = (Vertex) mesh.createVertex(1.0, 2.0, 0.0);
		v[6] = (Vertex) mesh.createVertex(0.0, 1.0, 0.0);
		v[7] = (Vertex) mesh.createVertex(0.0, 0.0, 1.0);
		v[8] = (Vertex) mesh.createVertex(0.0, 2.0, 1.0);
		v[9] = (Vertex) mesh.createVertex(0.0, 0.0, -1.0);
		v[10] = (Vertex) mesh.createVertex(0.0, 2.0, -1.0);
		for (int i = 0; i < v.length; i++)
			v[i].setLabel(i);

		T = new Triangle[12];
		T[0] = (Triangle) mesh.createTriangle(v[0], v[1], v[6]);
		T[1] = (Triangle) mesh.createTriangle(v[6], v[3], v[0]);
		T[2] = (Triangle) mesh.createTriangle(v[3], v[6], v[4]);
		T[3] = (Triangle) mesh.createTriangle(v[2], v[1], v[6]);
		T[4] = (Triangle) mesh.createTriangle(v[5], v[2], v[6]);
		T[5] = (Triangle) mesh.createTriangle(v[5], v[6], v[4]);
		T[6] = (Triangle) mesh.createTriangle(v[7], v[1], v[6]);
		T[7] = (Triangle) mesh.createTriangle(v[6], v[4], v[8]);
		T[8] = (Triangle) mesh.createTriangle(v[7], v[6], v[8]);
		T[9] = (Triangle) mesh.createTriangle(v[9], v[1], v[6]);
		T[10] = (Triangle) mesh.createTriangle(v[10], v[9], v[6]);
		T[11] = (Triangle) mesh.createTriangle(v[10], v[6], v[4]);
		v[0].setLink(T[0]);
		v[1].setLink(T[0]);
		v[2].setLink(T[3]);
		v[3].setLink(T[1]);
		v[4].setLink(T[5]);
		v[5].setLink(T[4]);
		v[6].setLink(T[4]);
		v[7].setLink(T[8]);
		v[8].setLink(T[8]);
		v[9].setLink(T[10]);
		v[10].setLink(T[10]);
		int cnt = 0;
		for (Triangle t: T)
		{
			if (t == null)
				continue;
			mesh.add(t);
			t.setGroupId(cnt);
			cnt++;
		}
		mesh.buildAdjacency(v, -1.0);
		assertTrue("Mesh is not valid", mesh.isValid());
		new DecimateHalfEdge(mesh, options).compute();
		int res = DecimateHalfEdge.countInnerTriangles(mesh);
		int expected = 8;
		assertTrue("Final number of triangles: "+res, res == expected);
		assertTrue("Mesh is not valid", mesh.isValid());
	}

	@Test public void testSquare()
	{
		final Map<String, String> options = new HashMap<String, String>();
		options.put("size", "0.1");
		mesh = new Mesh();
		//  v2         v3
		//   +---------+
		//   | \       |
		//   |   \  T1 |
		//   |     \   |
		//   |  T0   \ |
		//   +---------+
		//  v0         v1
		v = new Vertex[4];
		v[0] = (Vertex) mesh.createVertex(0.0, 0.0, 0.0);
		v[1] = (Vertex) mesh.createVertex(1.0, 0.0, 0.0);
		v[2] = (Vertex) mesh.createVertex(0.0, 1.0, 0.0);
		v[3] = (Vertex) mesh.createVertex(1.0, 1.0, 0.0);
		for (int i = 0; i < v.length; i++)
			v[i].setLabel(i);

		T = new Triangle[2];
		T[0] = (Triangle) mesh.createTriangle(v[0], v[1], v[2]);
		T[1] = (Triangle) mesh.createTriangle(v[3], v[2], v[1]);
		v[0].setLink(T[0]);
		v[1].setLink(T[0]);
		v[2].setLink(T[0]);
		v[3].setLink(T[1]);
		int cnt = 0;
		for (Triangle t: T)
		{
			mesh.add(t);
			t.setGroupId(cnt);
			cnt++;
		}
		mesh.buildAdjacency(v, -1.0);
		assertTrue("Mesh is not valid", mesh.isValid());
		new DecimateHalfEdge(mesh, options).compute();
		int res = DecimateHalfEdge.countInnerTriangles(mesh);
		int expected = 2;
		assertTrue("Final number of triangles: "+res, res == expected);
		assertTrue("Mesh is not valid", mesh.isValid());
	}

	@Test public void testShell3()
	{
		final Map<String, String> options = new HashMap<String, String>();
		options.put("size", "0.1");
		mesh = new Mesh();
		create3xNShell(4);
		mesh.buildAdjacency(v, -1.0);
		assertTrue("Mesh is not valid", mesh.isValid());
		new DecimateHalfEdge(mesh, options).compute();
		int res = DecimateHalfEdge.countInnerTriangles(mesh);
		int expected = 2;
		if (res != expected)
		{
			new DecimateHalfEdge(mesh, options).compute();
			res = DecimateHalfEdge.countInnerTriangles(mesh);
		}
		assertTrue("Final number of triangles: "+res, res == expected);
		assertTrue("Mesh is not valid", mesh.isValid());
	}

	@Test public void testShellNM1()
	{
		final Map<String, String> options = new HashMap<String, String>();
		options.put("size", "0.1");
		mesh = new Mesh();
		create3xNShell(2);
		rotateNM();
		mesh.buildAdjacency(v, -1.0);
		assertTrue("Mesh is not valid", mesh.isValid());
		new DecimateHalfEdge(mesh, options).compute();
		int res = DecimateHalfEdge.countInnerTriangles(mesh);
		int expected = 8;
		assertTrue("Final number of triangles: "+res, res == expected);
		assertTrue("Mesh is not valid", mesh.isValid());
	}

	@Test public void testShellNM2()
	{
		final Map<String, String> options = new HashMap<String, String>();
		options.put("size", "0.1");
		mesh = new Mesh();
		create3xNShell(3);
		rotateNM();
		mesh.buildAdjacency(v, -1.0);
		assertTrue("Mesh is not valid", mesh.isValid());
		new DecimateHalfEdge(mesh, options).compute();
		int res = DecimateHalfEdge.countInnerTriangles(mesh);
		int expected = 8;
		assertTrue("Final number of triangles: "+res, res == expected);
		assertTrue("Mesh is not valid", mesh.isValid());
	}

	@Test public void testShellNM3()
	{
		final Map<String, String> options = new HashMap<String, String>();
		options.put("size", "0.5");
		mesh = new Mesh();
		create3xNShell(4);
		rotateNM();
		mesh.buildAdjacency(v, -1.0);
		assertTrue("Mesh is not valid", mesh.isValid());
		new DecimateHalfEdge(mesh, options).compute();
		int res = DecimateHalfEdge.countInnerTriangles(mesh);
		int expected = 8;
		assertTrue("Final number of triangles: "+res, res == expected);
		assertTrue("Mesh is not valid", mesh.isValid());
	}

	@Test public void testShellNM3Inverted()
	{
		final Map<String, String> options = new HashMap<String, String>();
		options.put("size", "0.1");
		mesh = new Mesh();
		create3xNShell(4);
		rotateNM();
		// Invert triangles at the right
		for (int i = 0; i < 3; i++)
		{
			for (int j = 0; j < 2; j++)
			{
				Vertex temp = T[4*i+j].vertex[1];
				T[4*i+j].vertex[1] = T[4*i+j].vertex[2];
				T[4*i+j].vertex[2] = temp;
				temp = T[4*i+j+12].vertex[1];
				T[4*i+j+12].vertex[1] = T[4*i+j+12].vertex[2];
				T[4*i+j+12].vertex[2] = temp;
			}
		}
		mesh.buildAdjacency(v, -1.0);
		assertTrue("Mesh is not valid", mesh.isValid());
		new DecimateHalfEdge(mesh, options).compute();
		int res = DecimateHalfEdge.countInnerTriangles(mesh);
		int expected = 8;
		assertTrue("Mesh is not valid", mesh.isValid());
		assertTrue("Final number of triangles: "+res, res == expected);
	}

}
