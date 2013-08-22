/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005,2006, by EADS CRC
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

import java.util.Collection;

public class TriangleHE extends Triangle
{
	private static final long serialVersionUID = 3364148266246993225L;
	// In this class, adjacency relations are supported by an HalfEdge instance,
	// which is created by ElementFactory.createTriangle.
	private HalfEdge e0;

	/**
	 * Constructor.
	 */
	TriangleHE(Vertex v0, Vertex v1, Vertex v2)
	{
		super(v0, v1, v2);
	}

	@Override
	public final void copy(Triangle src)
	{
		super.copy(src);
		e0.copy(((TriangleHE) src).e0);
	}
	
	/**
	 * Gets an <code>AbstractHalfEdge</code> instance bound to this triangle.
	 * This method returns <code>HalfEdge</code> 0.
	 * @return  an <code>HalfEdge</code> instance bound to this triangle
	 */
	@Override
	public final HalfEdge getAbstractHalfEdge()
	{
		return e0;
	}

	/**
	 * Gets an <code>AbstractHalfEdge</code> instance bound to this triangle.
	 * This method returns <code>HalfEdge</code> 0, argument is unused
	 * @param  that   dummy argument
	 * @return  an <code>HalfEdge</code> instance bound to this triangle
	 */
	@Override
	public HalfEdge getAbstractHalfEdge(AbstractHalfEdge that)
	{
		return e0;
	}

	/**
	 * Sets <code>HalfEdge</code> 0 of this triangle.  This method must not
	 * be called, it is meant to be used by {@link ElementFactory#createTriangle}.
	 *
	 * @param e  edge to set as edge 0 of this triangle
	 */
	final void setHalfEdge(HalfEdge e)
	{
		e0 = e;
	}

	/**
	 * Sets attributes for all edges of this triangle.
	 *
	 * @param attr  attributes to set on edges
	 */
	@Override
	public void setAttributes(int attr)
	{
		e0.setAttributes(attr);
		e0.next().setAttributes(attr);
		e0.prev().setAttributes(attr);
	}
	
	/**
	 * Resets attributes for all edges of this triangle.
	 *
	 * @param attr  attributes to reset on edges
	 */
	@Override
	public void clearAttributes(int attr)
	{
		e0.clearAttributes(attr);
		e0.next().clearAttributes(attr);
		e0.prev().clearAttributes(attr);
	}
	
	/**
	 * Checks if some attributes of this triangle are set.
	 *
	 * @param attr  attributes to check
	 * @return <code>true</code> if any edge of this triangle has
	 * one of these attributes set, <code>false</code> otherwise
	 */
	@Override
	public boolean hasAttributes(int attr)
	{
		return e0.hasAttributes(attr) || e0.next().hasAttributes(attr) || e0.prev().hasAttributes(attr);
	}
	
	@Override
	public final String toString()
	{
		StringBuilder r = new StringBuilder(super.toString());
		r.append("\nEdge attributes: ").append(e0.getAttributes());
		if (null != e0.next())
		{
			r.append(" n=").append(e0.next().getAttributes());
			if (null != e0.prev())
				r.append(" p=").append(e0.prev().getAttributes());
		}
		return r.toString();
	}

	/** Replace this triangle by t in v link */
	private void replaceTriangle(Vertex v, Triangle t)
	{
		if(v.getLink() == this)
			v.setLink(t);
		else if(!v.isManifold())
		{
			Triangle[] ts = (Triangle[])v.getLink();
			for(int i = 0; i < ts.length; i++)
				if(ts[i] == this)
					ts[i] = t;
		}
	}

	/** Split at barycenter */
	public void split(Mesh mesh)
	{
		Vertex uv = mesh.createVertex(0,0,0);
		uv.add(v0);
		uv.add(v1);
		uv.add(v2);
		uv.scale(1/3.0);
		split(mesh, uv, null);
	}

	private void glue(HalfEdge oldHE, Triangle t)
	{
		AbstractHalfEdge newHE = t.getAbstractHalfEdge();
		oldHE.sym().glue(newHE);
		newHE.setAttributes(oldHE.getAttributes());
	}
	public void split(Mesh mesh, Vertex v, Collection<Triangle> output)
	{
		assert v != null;
		//TODO reuse the current instance and create only 2 new triangles
		Triangle t1 = mesh.createTriangle(v, v0, v1);
		replaceTriangle(v0, t1);
		t1.setGroupId(getGroupId());
		Triangle t2 = mesh.createTriangle(v, v1, v2);
		replaceTriangle(v1, t2);
		t2.setGroupId(getGroupId());
		Triangle t3 = mesh.createTriangle(v, v2, v0);
		replaceTriangle(v2, t3);
		t3.setGroupId(getGroupId());
		v.setLink(t1);
		AbstractHalfEdge t1he = t1.getAbstractHalfEdge().next();
		AbstractHalfEdge t2he = t2.getAbstractHalfEdge().next();
		AbstractHalfEdge t3he = t3.getAbstractHalfEdge().next();
		t1he.glue(t2he.next());
		t2he.glue(t3he.next());
		t3he.glue(t1he.next());

		HalfEdge oldHE = e0;
		glue(oldHE, t2);
		oldHE = oldHE.next();
		glue(oldHE, t3);
		oldHE = oldHE.next();
		glue(oldHE, t1);

		if(output != null)
		{
			output.add(t1);
			output.add(t2);
			output.add(t3);
		}
		if(mesh.hasNodes())
			mesh.add(v);
		mesh.add(t1);
		mesh.add(t2);
		mesh.add(t3);
		mesh.remove(this);
	}
}
