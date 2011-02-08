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
public interface TraceInterface
{
	public void setLogFile(String logName);
	public abstract void createMesh(String meshName, Mesh mesh);
	boolean getDisabled();
	void setDisabled(boolean b);

	void add(Vertex v);
	void add(Vertex v, int id);
	void remove(Vertex v);
	Vertex getVertex(int id);
	int getVertexId(Vertex v);

	void add(Triangle t);
	void add(Triangle t, int id);
	void remove(Triangle t);
	Triangle getTriangle(int id);
	int getTriangleId(Triangle t);

	void addAdjacentTriangles(Mesh m);

	void edgeSwap(AbstractHalfEdge h);
	void edgeCollapse(AbstractHalfEdge h, Vertex v);
	void vertexSplitAfter(AbstractHalfEdge h, Vertex v);
	void vertexSplitBefore(AbstractHalfEdge h, Vertex v);

	void println(String x);
	void finish();
}
