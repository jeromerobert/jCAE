/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2011, by EADS France

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

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.logging.Logger;

public class TraceReplay implements TraceInterface
{
        private static final Logger LOGGER=Logger.getLogger(TraceReplay.class.getName());

	private final TObjectIntHashMap<Vertex> mapVertexId = new TObjectIntHashMap<Vertex>();
	private final TIntObjectHashMap<Vertex> mapIdVertex = new TIntObjectHashMap<Vertex>();

	private final TObjectIntHashMap<Triangle> mapTriangleId = new TObjectIntHashMap<Triangle>();
	private final TIntObjectHashMap<Triangle> mapIdTriangle = new TIntObjectHashMap<Triangle>();

	public void setLogFile(String logName)
	{
		// Do nothing
	}

	public void setHooks(String[] commands)
	{
		// Do nothing
	}

	public void createMesh(String meshName, Mesh mesh)
	{
		// Do nothing
	}

	public void setDisabled(boolean b)
	{
		// Do nothing
	}

	public boolean getDisabled()
	{
		return true;
	}

	public void add(Vertex v)
	{
		// Do nothing
	}

	public void add(Vertex v, int id)
	{
		mapIdVertex.put(id, v);
		mapVertexId.put(v, id);
	}

	public void remove(Vertex v)
	{
		int id = mapVertexId.get(v);
		mapIdVertex.remove(id);
		mapVertexId.remove(v);
	}

	public Vertex getVertex(int id)
	{
		return mapIdVertex.get(id);
	}


	public int getVertexId(Vertex v)
	{
		return mapVertexId.get(v);
	}

	public void add(Triangle t)
	{
		// Do nothing
	}

	public void add(Triangle t, int id)
	{
		mapIdTriangle.put(id, t);
		mapTriangleId.put(t, id);
	}

	public void remove(Triangle t)
	{
		int id = mapTriangleId.get(t);
		mapIdTriangle.remove(id);
		mapTriangleId.remove(t);
	}

	public Triangle getTriangle(int id)
	{
		return mapIdTriangle.get(id);
	}

	public int getTriangleId(Triangle t)
	{
		return mapTriangleId.get(t);
	}

	public void edgeSwap(AbstractHalfEdge h)
	{
		// Do nothing
	}

	public void edgeCollapse(AbstractHalfEdge h, Vertex v)
	{
		// Do nothing
	}

	public void vertexSplitAfter(AbstractHalfEdge h, Vertex v)
	{
		// Do nothing
	}

	public void vertexSplitBefore(AbstractHalfEdge h, Vertex v)
	{
		// Do nothing
	}

	public void moveVertex(Vertex v)
	{
	}

	public void println(String x)
	{
	}

	public void finish()
	{
	}

	public void addAdjacentTriangles(Mesh m)
	{
		// Do nothing
	}

}
