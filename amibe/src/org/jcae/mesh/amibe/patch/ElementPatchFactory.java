/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2006, by EADS CRC

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

import org.jcae.mesh.amibe.ds.AbstractTriangle;
import org.jcae.mesh.amibe.ds.AbstractVertex;
import org.jcae.mesh.amibe.ds.Triangle;
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

	public AbstractVertex createVertex(double u, double v)
	{
		return new Vertex2D(vertexTraitsBuilder, u, v);
	}

	public AbstractVertex createVertex(double x, double y, double z)
	{
		throw new RuntimeException();
	}

	public AbstractVertex createVertex(double [] x)
	{
		assert x.length == 2;
		return createVertex(x[0], x[1]);
	}

	private AbstractTriangle createTriangle()
	{
		if (triangleTraitsBuilder.hasCapability(TriangleTraitsBuilder.SHALLOWHALFEDGE))
			return new TriangleVH(triangleTraitsBuilder);
		return new AbstractTriangle(triangleTraitsBuilder);
	}
	
	public AbstractTriangle createTriangle(AbstractVertex v0, AbstractVertex v1, AbstractVertex v2)
	{
		AbstractTriangle ret = createTriangle();
		ret.vertex = new Vertex2D[3];
		ret.vertex[0] = (Vertex2D) v0;
		ret.vertex[1] = (Vertex2D) v1;
		ret.vertex[2] = (Vertex2D) v2;
		for (int i = 0; i < 3; i++)
			if (ret.vertex[i].getLink() == null)
				ret.vertex[i].setLink(ret);
		return ret;
	}

	public AbstractTriangle createTriangle(AbstractVertex [] v)
	{
		AbstractTriangle ret = createTriangle();
		ret.vertex = new Vertex2D[v.length];
		for (int i = 0; i < v.length; i++)
		{
			ret.vertex[i] = (Vertex2D) v[i];
			if (ret.vertex[i].getLink() == null)
				ret.vertex[i].setLink(ret);
		}
		return ret;
	}

	/**
	 * Clone an existing triangle.
	 */
	public AbstractTriangle createTriangle(AbstractTriangle that)
	{
		AbstractTriangle ret = createTriangle();
		ret.vertex = new Vertex2D[3];
		if (ret instanceof Triangle)
			((Triangle) ret).copy((Triangle) that);
		else
			ret.copy(that);
		return ret;
	}

	public boolean hasAdjacency()
	{
		return (triangleTraitsBuilder.hasCapability(TriangleTraitsBuilder.HALFEDGE | TriangleTraitsBuilder.SHALLOWHALFEDGE));
	}

}
