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
import org.jcae.mesh.amibe.ds.AbstractVertex;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.AbstractTriangle;
import java.util.Map;
import java.util.HashMap;
import static org.junit.Assert.*;
import org.junit.Test;

public class SplitEdgeTest
{
	private Mesh mesh;
	private Vertex [] v;
	private Triangle [] T;
	private int vertexLabel = 0;

	// m Vertex on rows, n Vertex on columns
	private void createMxNShell(int m, int n)
	{
		/*   v3       v4        v5 
		 *   +---------+---------+
		 *   | \       | \       |
		 *   |   \  T1 |   \  T3 |
		 *   |     \   |     \   |
		 *   |  T0   \ |  T2   \ |
		 *   +---------+---------+
		 *   v0        v1       v2
		 */
		v = new Vertex[m*n];
		for (int j = 0; j < n; j++)
			for (int i = 0; i < m; i++)
				v[m*j+i] = (Vertex) mesh.createVertex(i, j, 0.0);
		for (int i = 0; i < v.length; i++)
			v[i].setLabel(i);
		vertexLabel = v.length;
		T = createMxNTriangles(m, n, v);
	}

	private Triangle [] createMxNTriangles(int m, int n, Vertex [] vv)
	{
		Triangle [] tt = new Triangle[2*(m-1)*(n-1)];
		for (int j = 0; j < n-1; j++)
		{
			for (int i = 0; i < m-1; i++)
			{
				tt[2*(m-1)*j+2*i]   = (Triangle) mesh.createTriangle(vv[m*j+i], vv[m*j+i+1], vv[m*(j+1)+i]);
				tt[2*(m-1)*j+2*i+1] = (Triangle) mesh.createTriangle(vv[m*j+i+1], vv[m*(j+1)+i+1], vv[m*(j+1)+i]);
				vv[m*j+i].setLink(tt[2*(m-1)*j+2*i]);
			}
			vv[m*j+m-1].setLink(tt[2*(m-1)*j+2*m-3]);
		}
		// Last row
		for (int i = 0; i < m-1; i++)
			vv[m*(n-1)+i].setLink(tt[2*(m-1)*(n-2)+2*i]);
		vv[m*n-1].setLink(tt[2*(m-1)*(n-1)-1]);
		int cnt = mesh.getTriangles().size();
		for (Triangle t: tt)
		{
			mesh.add(t);
			t.setGroupId(cnt);
			cnt++;
		}
		return tt;
	}
	
	private void rotateMxNShellAroundY(int m, int n, double angle)
	{
		// Create new vertices and append them to current mesh
		assert v.length == m*n;
		Vertex [] vy = new Vertex[v.length];
		vertexLabel = v.length;
		double ct = Math.cos(angle*Math.PI / 180.0);
		double st = Math.sin(angle*Math.PI / 180.0);
		for (int i = 0; i < v.length; i++)
		{
			if (i%m == 0)
				vy[i]   = v[i];
			else
			{
				double [] xyz = v[i].getUV();
				vy[i]   = (Vertex) mesh.createVertex(ct*xyz[0]+st*xyz[2], xyz[1], -st*xyz[0]+ct*xyz[2]);
				vy[i].setLabel(vertexLabel);
				vertexLabel++;
			}
		}
		createMxNTriangles(m, n, vy);
	}
	
	private void addVertexRefs()
	{
		int ref = 0;
		for (AbstractTriangle at: mesh.getTriangles())
		{
			Triangle t = (Triangle) at;
			if (t.hasAttributes(AbstractHalfEdge.OUTER))
			{
				for (AbstractVertex v: t.vertex)
				{
					if (0 == v.getRef())
					{
						ref++;
						v.setRef(ref);
					}
				}
			}
		}
	}

	private void testShell(int m, int n)
	{
		final Map<String, String> options = new HashMap<String, String>();
		double length = 0.4;
		options.put("size", ""+length);
		mesh = new Mesh();
		createMxNShell(m, n);
		mesh.buildAdjacency();
		addVertexRefs();
		assertTrue("Mesh is not valid", mesh.isValid());
		int p = 2-((int) (Math.log(length*length)/Math.log(2.0)));
		int expected = (m-1)*(n-1)*(2 << p);
		new SplitEdge(mesh, options).compute();
		assertTrue("Mesh is not valid", mesh.isValid());
		int res = AbstractAlgoHalfEdge.countInnerTriangles(mesh);
		assertTrue("Final number of triangles: "+res+", expected="+expected, res == expected);
		assertTrue("Mesh is not valid", mesh.isValid());
	}

	private void testCross(int m, int n)
	{
		final Map<String, String> options = new HashMap<String, String>();
		double length = 0.8;
		options.put("size", ""+length);
		mesh = new Mesh();
		createMxNShell(m, n);
		rotateMxNShellAroundY(m, n, 90);
		rotateMxNShellAroundY(m, n, 180);
		rotateMxNShellAroundY(m, n, 270);
		mesh.buildAdjacency();
		addVertexRefs();
		assertTrue("Mesh is not valid", mesh.isValid());
		int p = 2-((int) (Math.log(length*length)/Math.log(2.0)));
		int expected = 4*(m-1)*(n-1)*(2 << p);
		new SplitEdge(mesh, options).compute();
		assertTrue("Mesh is not valid", mesh.isValid());
		int res = AbstractAlgoHalfEdge.countInnerTriangles(mesh);
		assertTrue("Final number of triangles: "+res+", expected="+expected, res == expected);
	}

	@Test public void testSquare()
	{
		final Map<String, String> options = new HashMap<String, String>();
		double length = 0.1;
		options.put("size", ""+length);
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
		mesh.buildAdjacency();
		addVertexRefs();
		assertTrue("Mesh is not valid", mesh.isValid());
		new SplitEdge(mesh, options).compute();
		int res = AbstractAlgoHalfEdge.countInnerTriangles(mesh);
		int p = 2-((int) (Math.log(length*length)/Math.log(2.0)));
		int expected = 2 << p;
		assertTrue("Final number of triangles: "+res+", expected="+expected, res == expected);
		assertTrue("Mesh is not valid", mesh.isValid());
	}

	@Test public void testShell3()
	{
		testShell(3, 3);
	}

	@Test public void testShellLarge()
	{
		testShell(10, 10);
	}

	@Test public void testShellNM1()
	{
		testCross(2, 2);
	}

	@Test public void testShellNM2()
	{
		testCross(3, 3);
	}

	@Test public void testShellNM3()
	{
		testCross(3, 4);
	}

	@Test public void testShellNMLarge()
	{
		testCross(10, 10);
	}

	@Test public void testShellNM3Inverted()
	{
		final Map<String, String> options = new HashMap<String, String>();
		double length = 0.8;
		options.put("size", ""+length);
		mesh = new Mesh();
		int m = 2;
		int n = 2;
		createMxNShell(m, n);
		rotateMxNShellAroundY(m, n, 90);
		rotateMxNShellAroundY(m, n, 180);
		rotateMxNShellAroundY(m, n, 270);
		// Invert triangles at the right
		for (int j = 0; j < n-1; j++)
		{
			for (int i = 0; i < m-1; i++)
			{
				Vertex temp = (Vertex) T[2*(m-1)*j+2*i].vertex[1];
				T[2*(m-1)*j+2*i].vertex[1] = T[2*(m-1)*j+2*i].vertex[2];
				T[2*(m-1)*j+2*i].vertex[2] = temp;
				temp = (Vertex) T[2*(m-1)*j+2*i+1].vertex[1];
				T[2*(m-1)*j+2*i+1].vertex[1] = T[2*(m-1)*j+2*i+1].vertex[2];
				T[2*(m-1)*j+2*i+1].vertex[2] = temp;
			}
		}
		mesh.buildAdjacency();
		addVertexRefs();
		assertTrue("Mesh is not valid", mesh.isValid());
		int p = 2-((int) (Math.log(length*length)/Math.log(2.0)));
		int expected = 4*(m-1)*(n-1)*(2 << p);
		new SplitEdge(mesh, options).compute();
		assertTrue("Mesh is not valid", mesh.isValid());
		int res = AbstractAlgoHalfEdge.countInnerTriangles(mesh);
		assertTrue("Mesh is not valid", mesh.isValid());
		assertTrue("Final number of triangles: "+res, res == expected);
	}

}
