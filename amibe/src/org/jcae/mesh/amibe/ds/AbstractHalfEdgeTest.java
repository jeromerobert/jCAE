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
import java.util.Iterator;
import java.util.LinkedHashMap;

public abstract class AbstractHalfEdgeTest
{
	protected Mesh mesh;
	protected Vertex [] v;
	protected Triangle [] T;
	
	protected abstract AbstractHalfEdge find(Vertex v1, Vertex v2);

	protected void buildMesh()
	{
		/*
		 *  v4        v3        v2
		 *   +---------+---------+
		 *   | \       |       / |
		 *   |   \  T2 | T1  /   |
		 *   |     \   |   /     |
		 *   |  T3   \ | /   T0  |
		 *   +---------+---------+
		 *   v5        v0       v1
		 */
		v = new Vertex[6];
		v[0] = (Vertex) mesh.factory.createVertex(0.0, 0.0, 0.0);
		v[1] = (Vertex) mesh.factory.createVertex(1.0, 0.0, 0.0);
		v[2] = (Vertex) mesh.factory.createVertex(1.0, 1.0, 0.0);
		v[3] = (Vertex) mesh.factory.createVertex(0.0, 1.0, 0.0);
		v[4] = (Vertex) mesh.factory.createVertex(-1.0, 1.0, 0.0);
		v[5] = (Vertex) mesh.factory.createVertex(-1.0, 0.0, 0.0);
		T = new Triangle[4];
		T[0] = (Triangle) mesh.factory.createTriangle(v[0], v[1], v[2]);
		T[1] = (Triangle) mesh.factory.createTriangle(v[2], v[3], v[0]);
		T[2] = (Triangle) mesh.factory.createTriangle(v[4], v[0], v[3]);
		T[3] = (Triangle) mesh.factory.createTriangle(v[5], v[0], v[4]);
		v[0].setLink(T[0]);
		v[1].setLink(T[0]);
		v[2].setLink(T[0]);
		v[3].setLink(T[2]);
		v[4].setLink(T[2]);
		v[5].setLink(T[3]);
		mesh.add(T[0]);
		mesh.add(T[1]);
		mesh.add(T[2]);
		mesh.add(T[3]);
		mesh.buildAdjacency(v, -1.0);
	}
	
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
		for (Triangle t: T)
			mesh.add(t);
	}
	
	protected void buildMesh2()
	{
		create3x4Shell();
		mesh.buildAdjacency(v, -1.0);
	}
	
	protected void buildMeshNM()
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
				vy[i]   = (Vertex) mesh.factory.createVertex(-xyz[2], xyz[1], xyz[0]);
				vy[i].setLabel(label);
				label++;
			}
		}
		Triangle [] Ty = new Triangle[12];
		for (int i = 0; i < 3; i++)
		{
			Ty[4*i]   = (Triangle) mesh.factory.createTriangle(vy[3*i], vy[3*i+1], vy[3*i+3]);
			Ty[4*i+1] = (Triangle) mesh.factory.createTriangle(vy[3*i+1], vy[3*i+4], vy[3*i+3]);
			Ty[4*i+2] = (Triangle) mesh.factory.createTriangle(vy[3*i+5], vy[3*i+4], vy[3*i+1]);
			Ty[4*i+3] = (Triangle) mesh.factory.createTriangle(vy[3*i+1], vy[3*i+2], vy[3*i+5]);
			vy[3*i].setLink(Ty[4*i]);
			vy[3*i+1].setLink(Ty[4*i]);
			vy[3*i+2].setLink(Ty[4*i+3]);
		}
		vy[9].setLink(Ty[9]);
		vy[10].setLink(Ty[9]);
		vy[11].setLink(Ty[11]);
		for (Triangle t: Ty)
			mesh.add(t);
		Vertex [] vTotal = new Vertex[20];
		System.arraycopy(v, 0, vTotal, 0, v.length);
		for (int i = 0; i < 4; i++)
			vTotal[12+i] = vy[3*i];
		for (int i = 0; i < 4; i++)
			vTotal[16+i] = vy[3*i+2];
		mesh.buildAdjacency(vTotal, -1.0);
		assertTrue("Mesh is not valid", mesh.isValid());
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
			e = e.sym().next();
			LinkedHashMap<Triangle, Integer> adj = (LinkedHashMap<Triangle, Integer>) e.getAdj();
			n = adj.size();
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
