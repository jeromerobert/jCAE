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

import static org.junit.Assert.*;
import java.util.Iterator;

public class AbstractHalfEdgeTest
{
	protected Mesh mesh;
	protected Vertex [] v;
	protected Triangle [] T;
	private int vertexLabel = 0;
	
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
			v[3*i]   = (Vertex) mesh.factory.createVertex(-1.0, i, 0.0);
			v[3*i+1] = (Vertex) mesh.factory.createVertex(0.0, i, 0.0);
			v[3*i+2] = (Vertex) mesh.factory.createVertex(1.0, i, 0.0);
		}
		for (int i = 0; i < v.length; i++)
			v[i].setLabel(i);
		vertexLabel = v.length;

		T = new Triangle[12];
		for (int i = 0; i < 3; i++)
		{
			T[4*i]   = (Triangle) mesh.factory.createTriangle(v[3*i], v[3*i+1], v[3*i+3]);
			T[4*i+1] = (Triangle) mesh.factory.createTriangle(v[3*i+1], v[3*i+4], v[3*i+3]);
			T[4*i+2] = (Triangle) mesh.factory.createTriangle(v[3*i+5], v[3*i+4], v[3*i+1]);
			T[4*i+3] = (Triangle) mesh.factory.createTriangle(v[3*i+1], v[3*i+2], v[3*i+5]);
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
	
	// m Vertex on rows, n Vertex on columns
	protected void createMxNShell(int m, int n)
	{
		v = new Vertex[m*n];
		for (int j = 0; j < n; j++)
			for (int i = 0; i < m; i++)
				v[m*j+i] = (Vertex) mesh.factory.createVertex(i, j, 0.0);
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
				tt[2*(m-1)*j+2*i]   = (Triangle) mesh.factory.createTriangle(vv[m*j+i], vv[m*j+i+1], vv[m*(j+1)+i]);
				tt[2*(m-1)*j+2*i+1] = (Triangle) mesh.factory.createTriangle(vv[m*j+i+1], vv[m*(j+1)+i+1], vv[m*(j+1)+i]);
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
	
	protected void rotateMxNShellAroundY(int m, int n, double angle)
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
				vy[i]   = (Vertex) mesh.factory.createVertex(ct*xyz[0]+st*xyz[2], xyz[1], -st*xyz[0]+ct*xyz[2]);
				vy[i].setLabel(vertexLabel);
				vertexLabel++;
			}
		}
		createMxNTriangles(m, n, vy);
	}
	
	protected void buildMesh()
	{
		createMxNShell(3, 2);
		mesh.buildAdjacency();
	}
	
	protected void buildMesh2()
	{
		create3x4Shell();
		mesh.buildAdjacency();
	}
	
	protected void buildMeshTopo()
	{
		/*
		 *    This mesh is a prism, it can be unrolled as:
		 *  v4        v3        v2        v4 
		 *   +---------+---------+---------+
		 *   | \       |       / |       / |
		 *   |   \  T2 | T1  /   | T4  /   |
		 *   |     \   |   /     |   /     |
		 *   |  T3   \ | /   T0  | /  T5   |
		 *   +---------+---------+---------+
		 *   v5        v0       v1        v5
		 *   Edge (v0,v1) cannot be collapsed because T3
		 *   and T5 would then be glued together and
		 *   edge (v4,v0) would become non-manifold.
		 */
		v = new Vertex[6];
		v[0] = (Vertex) mesh.factory.createVertex(0.0, 0.0, 0.0);
		v[1] = (Vertex) mesh.factory.createVertex(1.0, 0.0, 0.0);
		v[2] = (Vertex) mesh.factory.createVertex(1.0, 1.0, 0.0);
		v[3] = (Vertex) mesh.factory.createVertex(0.0, 1.0, 0.0);
		v[4] = (Vertex) mesh.factory.createVertex(0.0, 1.0, 1.0);
		v[5] = (Vertex) mesh.factory.createVertex(0.0, 0.0, 1.0);
		for (int i = 0; i < v.length; i++)
			v[i].setLabel(i);

		T = new Triangle[6];
		T[0] = (Triangle) mesh.factory.createTriangle(v[0], v[1], v[2]);
		T[1] = (Triangle) mesh.factory.createTriangle(v[2], v[3], v[0]);
		T[2] = (Triangle) mesh.factory.createTriangle(v[4], v[0], v[3]);
		T[3] = (Triangle) mesh.factory.createTriangle(v[5], v[0], v[4]);
		T[4] = (Triangle) mesh.factory.createTriangle(v[4], v[2], v[1]);
		T[5] = (Triangle) mesh.factory.createTriangle(v[5], v[4], v[1]);
		v[0].setLink(T[0]);
		v[1].setLink(T[0]);
		v[2].setLink(T[0]);
		v[3].setLink(T[2]);
		v[4].setLink(T[2]);
		v[5].setLink(T[3]);
		int cnt = 0;
		for (Triangle t: T)
		{
			mesh.add(t);
			t.setGroupId(cnt);
			cnt++;
		}
		mesh.buildAdjacency();
	}
	
	protected void buildMeshNM()
	{
		createMxNShell(2, 4);
		rotateMxNShellAroundY(2, 4, 90);
		rotateMxNShellAroundY(2, 4, 180);
		rotateMxNShellAroundY(2, 4, 270);
		mesh.buildAdjacency();
		assertTrue("Mesh is not valid", mesh.isValid());
	}
	
	protected void buildMeshNM43()
	{
		createMxNShell(4, 3);
		rotateMxNShellAroundY(4, 3, 90);
		rotateMxNShellAroundY(4, 3, 180);
		rotateMxNShellAroundY(4, 3, 270);
		mesh.buildAdjacency();
		assertTrue("Mesh is not valid", mesh.isValid());
	}
	
	protected void buildMeshNM44()
	{
		createMxNShell(4, 4);
		rotateMxNShellAroundY(4, 4, 90);
		rotateMxNShellAroundY(4, 4, 180);
		rotateMxNShellAroundY(4, 4, 270);
		mesh.buildAdjacency();
		assertTrue("Mesh is not valid", mesh.isValid());
	}
	
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
		AbstractHalfEdge ret = start.getAbstractHalfEdge();
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
	
	protected void nextOriginLoop()
	{
		// Loop around v0
		AbstractHalfEdge e = find(v[0], v[1]);
		assertTrue(e.origin() == v[0]);
		assertTrue(e.getTri() == T[0]);
		e = e.nextOriginLoop();
		assertTrue(e.origin() == v[0]);
		assertTrue(e.destination() == v[3]);
		assertTrue(e.apex() == mesh.outerVertex);
		e = e.nextOriginLoop();
		assertTrue(e.origin() == v[0]);
		assertTrue(e.destination() == mesh.outerVertex);
		assertTrue(e.apex() == v[1]);
		e = e.nextOriginLoop();
		assertTrue(e.origin() == v[0]);
		assertTrue(e.getTri() == T[0]);
		// Loop around v1
		e = find(v[1], v[2]);
		for (int i = 3; i >= 0; i--)
		{
			assertTrue(e.origin() == v[1]);
			assertTrue(e.getTri() == T[i]);
			e = e.nextOriginLoop();
		}
		assertTrue(e.origin() == v[1]);
		assertTrue(e.destination() == v[0]);
		assertTrue(e.apex() == mesh.outerVertex);
		e = e.nextOriginLoop();
		assertTrue(e.origin() == v[1]);
		assertTrue(e.destination() == mesh.outerVertex);
		assertTrue(e.apex() == v[2]);
		e = e.nextOriginLoop();
		assertTrue(e.origin() == v[1]);
		assertTrue(e.getTri() == T[3]);
		// Loop around v4
		e = e.nextOrigin().prev();
		assertTrue(e.origin() == v[4]);
		assertTrue(e.getTri() == T[2]);
		for (int i = 7; i >= 4; i--)
		{
			e = e.nextOriginLoop();
			assertTrue(e.origin() == v[4]);
			assertTrue(e.getTri() == T[i]);
		}
		e = e.nextOriginLoop();
		assertTrue(e.origin() == v[4]);
		assertTrue(e.getTri() == T[1]);
		e = e.nextOriginLoop();
		assertTrue(e.origin() == v[4]);
		assertTrue(e.getTri() == T[2]);
	}
	
	protected void canCollapse(Vertex o, Vertex d, Vertex n, boolean expected)
	{
		AbstractHalfEdge e = find(o, d);
		assertTrue(expected == e.canCollapse(n));
	}

	protected AbstractHalfEdge collapse(Vertex o, Vertex d, Vertex n)
	{
		AbstractHalfEdge e = find(o, d);
		Vertex a = e.apex();
		e = e.collapse(mesh, n);
		assertTrue("Mesh is not valid", mesh.isValid());
		assertTrue("Error in origin", n == e.origin());
		assertTrue("Error in destination", a == e.destination());
		return e;
	}

	protected void swap(Vertex o, Vertex d)
	{
		AbstractHalfEdge e = find(o, d);
		Vertex a = e.apex();
		e = e.swap();
		assertTrue("Mesh is not valid", mesh.isValid());
		assertTrue("Error in origin", o == e.origin());
		assertTrue("Error in apex", a == e.apex());
	}

	protected AbstractHalfEdge split(Vertex o, Vertex d, Vertex n)
	{
		AbstractHalfEdge e = find(o, d);
		e = e.split(mesh, n);
		assertTrue("Mesh is not valid", mesh.isValid());
		assertTrue("Error in origin", o == e.origin());
		assertTrue("Error in destination", n == e.destination());
		return e;
	}

	protected void countVertexLinks(Vertex o, int count)
	{
		int n = 0;
		if (o.getLink() instanceof Triangle)
			n = 1;
		else
			n = ((Triangle []) o.getLink()).length;
		assertTrue("Found "+n+" links instead of "+count, n == count);
	}

	protected void countEdgeLinks(Vertex o, Vertex d, int count)
	{
		AbstractHalfEdge e = find(o, d);
		int n = 0;
		if (!e.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
		{
			n++;
			if (!e.hasAttributes(AbstractHalfEdge.BOUNDARY))
				n++;
		}
		else
		{
			if (e.hasAttributes(AbstractHalfEdge.OUTER))
				e = e.sym();
			n = e.getAdjNonManifold().size();
		}
		assertTrue("Found "+n+" incident triangles instead of "+count, n == count);
	}

	protected void countFanIterator(Vertex o, Vertex d, int count)
	{
		AbstractHalfEdge e = find(o, d);
		int n = 0;
		for (Iterator<AbstractHalfEdge> it = e.fanIterator(); it.hasNext(); it.next())
			n++;
		assertTrue("Found "+n+" fans instead of "+count, n == count);
	}
}
