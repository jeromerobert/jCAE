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

/**
 *
 */
public class TraceNull implements TraceInterface
{
	public void setLogFile(String logName)
	{
	}

	public void createMesh(String meshName, Mesh mesh)
	{
	}

	public boolean getDisabled()
	{
		return true;
	}

	public void setDisabled(boolean b)
	{
	}

	public void add(Vertex v)
	{
	}

	public void add(Vertex v, int id)
	{
	}

	public void remove(Vertex v)
	{
	}

	public Vertex getVertex(int id)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getVertexId(Vertex v)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void add(Triangle t)
	{
	}

	public void add(Triangle t, int id)
	{
	}

	public void remove(Triangle t)
	{
	}

	public Triangle getTriangle(int id)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getTriangleId(Triangle t)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void println(String x)
	{
	}

	public void finish()
	{
	}

	public void edgeSwap(AbstractHalfEdge h)
	{
	}

	public void edgeCollapse(AbstractHalfEdge h, Vertex v)
	{
	}

	public void vertexSplitAfter(AbstractHalfEdge h, Vertex v)
	{
	}

	public void vertexSplitBefore(AbstractHalfEdge h, Vertex v)
	{
	}

	public void moveVertex(Vertex v, double x, double y, double z)
	{
	}

	public void addAdjacentTriangles(Mesh m)
	{
	}

}
