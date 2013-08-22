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
import org.jcae.mesh.amibe.metrics.Location;
import org.jcae.mesh.amibe.traits.VertexTraitsBuilder;
import org.jcae.mesh.amibe.traits.TriangleTraitsBuilder;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;

class ElementPatchFactory implements ElementFactoryInterface
{
	private final VertexTraitsBuilder vertexTraitsBuilder;
	private final TriangleTraitsBuilder triangleTraitsBuilder;

	ElementPatchFactory(MeshTraitsBuilder mtb)
	{
		vertexTraitsBuilder   = mtb.getVertexTraitsBuilder();
		triangleTraitsBuilder = mtb.getTriangleTraitsBuilder();
	}

	public final Vertex2D createVertex(double u, double v)
	{
		return new Vertex2D(vertexTraitsBuilder, u, v);
	}

	public Vertex2D createVertex(double x, double y, double z)
	{
		throw new RuntimeException();
	}

	public Vertex2D createVertex(Location x)
	{
		return createVertex(x.getX(), x.getY());
	}

	private Triangle createTriangleImpl(Vertex v0, Vertex v1, Vertex v2)
	{
		if (triangleTraitsBuilder.hasCapability(TriangleTraitsBuilder.VIRTUALHALFEDGE))
			return new TriangleVH(v0, v1, v2);
		return new Triangle(v0, v1, v2);
	}

	@Override
	public Triangle createTriangle(Vertex v0, Vertex v1, Vertex v2)
	{
		Triangle ret = createTriangleImpl(v0, v1, v2);
		if (v0.getLink() == null)
			v0.setLink(ret);
		if (v1.getLink() == null)
			v1.setLink(ret);
		if (v2.getLink() == null)
			v2.setLink(ret);
		return ret;
	}

	/**
	 * Clone an existing triangle.
	 */
	public Triangle createTriangle(Triangle that)
	{
		Triangle ret = createTriangleImpl(null, null, null);
		ret.copy(that);
		return ret;
	}

	public boolean hasAdjacency()
	{
		return (triangleTraitsBuilder.hasCapability(TriangleTraitsBuilder.HALFEDGE | TriangleTraitsBuilder.VIRTUALHALFEDGE));
	}

}
