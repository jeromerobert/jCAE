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

package org.jcae.mesh.amibe.patch;

import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.ds.TriangleVH;
import org.jcae.mesh.amibe.ds.ElementFactoryInterface;
import org.jcae.mesh.amibe.traits.VertexTraitsBuilder;
import org.jcae.mesh.amibe.traits.TriangleTraitsBuilder;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;

public class ElementPatchFactory implements ElementFactoryInterface
{
	private final VertexTraitsBuilder vertexTraitsBuilder;
	private final TriangleTraitsBuilder triangleTraitsBuilder;

	public ElementPatchFactory(MeshTraitsBuilder mtb)
	{
		vertexTraitsBuilder   = mtb.getVertexTraitsBuilder();
		triangleTraitsBuilder = mtb.getTriangleTraitsBuilder();
	}

	public Vertex2D createVertex(double u, double v)
	{
		return new Vertex2D(vertexTraitsBuilder, u, v);
	}

	public Vertex2D createVertex(double x, double y, double z)
	{
		throw new RuntimeException();
	}

	public Vertex2D createVertex(double [] x)
	{
		assert x.length == 2;
		return createVertex(x[0], x[1]);
	}

	private Triangle createTriangle()
	{
		if (triangleTraitsBuilder.hasCapability(TriangleTraitsBuilder.VIRTUALHALFEDGE))
			return new TriangleVH(triangleTraitsBuilder);
		return new Triangle(triangleTraitsBuilder);
	}
	
	public Triangle createTriangle(Vertex v0, Vertex v1, Vertex v2)
	{
		Triangle ret = createTriangle();
		Vertex2D [] vArray = new Vertex2D[3];
		vArray[0] = (Vertex2D) v0;
		vArray[1] = (Vertex2D) v1;
		vArray[2] = (Vertex2D) v2;
		for (int i = 0; i < 3; i++)
			if (vArray[i].getLink() == null)
				vArray[i].setLink(ret);
		ret.vertex = vArray;
		return ret;
	}

	public Triangle createTriangle(Vertex [] v)
	{
		Triangle ret = createTriangle();
		Vertex2D [] vArray = new Vertex2D[3];
		for (int i = 0; i < v.length; i++)
		{
			vArray[i] = (Vertex2D) v[i];
			if (vArray[i].getLink() == null)
				vArray[i].setLink(ret);
		}
		ret.vertex = vArray;
		return ret;
	}

	/**
	 * Clone an existing triangle.
	 */
	public Triangle createTriangle(Triangle that)
	{
		Triangle ret = createTriangle();
		ret.vertex = new Vertex2D[3];
		ret.copy(that);
		return ret;
	}

	public boolean hasAdjacency()
	{
		return (triangleTraitsBuilder.hasCapability(TriangleTraitsBuilder.HALFEDGE | TriangleTraitsBuilder.VIRTUALHALFEDGE));
	}

}
