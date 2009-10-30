/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2006, by EADS CRC
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

import org.jcae.mesh.amibe.traits.VertexTraitsBuilder;
import org.jcae.mesh.amibe.traits.HalfEdgeTraitsBuilder;
import org.jcae.mesh.amibe.traits.TriangleTraitsBuilder;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;

class ElementFactory implements ElementFactoryInterface
{
	private final VertexTraitsBuilder vertexTraitsBuilder;
	private final HalfEdgeTraitsBuilder halfedgeTraitsBuilder;
	private final TriangleTraitsBuilder triangleTraitsBuilder;

	ElementFactory(MeshTraitsBuilder mtb)
	{
		vertexTraitsBuilder   = mtb.getVertexTraitsBuilder();
		halfedgeTraitsBuilder = mtb.getHalfEdgeTraitsBuilder();
		triangleTraitsBuilder = mtb.getTriangleTraitsBuilder();
	}

	public Vertex createVertex(double u, double v)
	{
		throw new RuntimeException();
	}

	public Vertex createVertex(double x, double y, double z)
	{
		return new Vertex(vertexTraitsBuilder, x, y, z);
	}

	public Vertex createVertex(double [] x)
	{
		assert x.length == 3;
		return new Vertex(vertexTraitsBuilder, x[0], x[1], x[2]);
	}

	private AbstractHalfEdge createHalfEdge(TriangleHE t, byte orientation, byte attributes)
	{
		return new HalfEdge(halfedgeTraitsBuilder, t, orientation, attributes);
	}

	private Triangle createTriangle()
	{
		if (triangleTraitsBuilder.hasCapability(TriangleTraitsBuilder.HALFEDGE))
		{
			TriangleHE ret = new TriangleHE(triangleTraitsBuilder);
			HalfEdge e0 = (HalfEdge) createHalfEdge(ret, (byte) 0, (byte) 0);
			HalfEdge e1 = (HalfEdge) createHalfEdge(ret, (byte) 1, (byte) 0);
			HalfEdge e2 = (HalfEdge) createHalfEdge(ret, (byte) 2, (byte) 0);
			e0.setNext(e1);
			e1.setNext(e2);
			e2.setNext(e0);
			ret.setHalfEdge(e0);
			return ret;
		}
		else if (triangleTraitsBuilder.hasCapability(TriangleTraitsBuilder.VIRTUALHALFEDGE))
			return new TriangleVH(triangleTraitsBuilder);
		else
			return new Triangle(triangleTraitsBuilder);
	}

	public Triangle createTriangle(Vertex v0, Vertex v1, Vertex v2)
	{
		Triangle ret = createTriangle();
		ret.vertex[0] = v0;
		ret.vertex[1] = v1;
		ret.vertex[2] = v2;
		return ret;
	}

	public Triangle createTriangle(Vertex [] v)
	{
		Triangle ret = createTriangle();
		ret.vertex = new Vertex[v.length];
		System.arraycopy(v, 0, ret.vertex, 0, v.length);
		return ret;
	}

	/**
	 * Clone an existing triangle.
	 */
	public Triangle createTriangle(Triangle that)
	{
		Triangle ret = createTriangle();
		ret.copy(that);
		return ret;
	}

	public boolean hasAdjacency()
	{
		return (triangleTraitsBuilder.hasCapability(TriangleTraitsBuilder.HALFEDGE | TriangleTraitsBuilder.VIRTUALHALFEDGE));
	}

}
