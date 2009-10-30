/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2008, by EADS France

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
import org.junit.Test;
import static org.junit.Assert.*;

public class MeshTest
{
	private Mesh mesh;
	private Vertex [] v;
	private Triangle [] T;
	
	// m Vertex on rows, n Vertex on columns
	private void createMxNShell(int m, int n)
	{
		v = new Vertex[m*n];
		for (int j = 0; j < n; j++)
			for (int i = 0; i < m; i++)
				v[m*j+i] = mesh.createVertex(i, j, 0.0);
		T = createMxNTriangles(m, n, v);
	}

	private Triangle [] createMxNTriangles(int m, int n, Vertex [] vv)
	{
		/*   v3       v4        v5 
		 *   +---------+---------+
		 *   | \       | \       |
		 *   |   \  T1 |   \  T3 |
		 *   |     \   |     \   |
		 *   |  T0   \ |  T2   \ |
		 *   +---------+---------+
		 *   v0        v1       v2
		 * or
		 *   v3       v4        v5 
		 *   +---------+---------+
		 *   |       / |       / |
		 *   | T0  /   | T2  /   |
		 *   |   /     |   /     |
		 *   | /   T1  | /   T3  |
		 *   +---------+---------+
		 *   v0        v1       v2
		 */
		Triangle [] tt = new Triangle[2*(m-1)*(n-1)];
		for (int j = 0; j < n-1; j++)
		{
			if (j%2 == 0)
			{
				for (int i = 0; i < m-1; i++)
				{
					tt[2*(m-1)*j+2*i]   = mesh.createTriangle(vv[m*j+i], vv[m*j+i+1], vv[m*(j+1)+i]);
					tt[2*(m-1)*j+2*i+1] = mesh.createTriangle(vv[m*j+i+1], vv[m*(j+1)+i+1], vv[m*(j+1)+i]);
					vv[m*j+i].setLink(tt[2*(m-1)*j+2*i]);
				}
				vv[m*j+m-1].setLink(tt[2*(m-1)*j+2*m-3]);
			}
			else
			{
				for (int i = 0; i < m-1; i++)
				{
					tt[2*(m-1)*j+2*i]   = mesh.createTriangle(vv[m*j+i], vv[m*(j+1)+i+1], vv[m*(j+1)+i]);
					tt[2*(m-1)*j+2*i+1] = mesh.createTriangle(vv[m*j+i+1], vv[m*(j+1)+i+1], vv[m*j+i]);
					vv[m*j+i].setLink(tt[2*(m-1)*j+2*i]);
				}
				vv[m*j+m-1].setLink(tt[2*(m-1)*j+2*m-3]);
			}
		}
		// Last row
		for (int i = 0; i < m-1; i++)
			vv[m*(n-1)+i].setLink(tt[2*(m-1)*(n-2)+2*i]);
		vv[m*n-1].setLink(tt[2*(m-1)*(n-1)-1]);
		for (Triangle t: tt)
			mesh.add(t);
		return tt;
	}

	@Test public void groups()
	{
		TriangleTraitsBuilder ttb = new TriangleTraitsBuilder();
		ttb.addHalfEdge();
		MeshTraitsBuilder mtb = new MeshTraitsBuilder();
		mtb.addTriangleList();
		mtb.add(ttb);
		mesh = new Mesh(mtb);
		createMxNShell(3, 3);
		for (int i = 0; i < 8; i++)
			T[i].setGroupId(i/4);
		assertTrue("Wrong number of triangles: "+mesh.getTriangles().size(), 8 == mesh.getTriangles().size());
		mesh.buildAdjacency();
		assertTrue("Wrong number of triangles after buildAdjacency: "+mesh.getTriangles().size(), 16 == mesh.getTriangles().size());
		int nr = mesh.buildGroupBoundaries();
		assertTrue("Wrong return value of buildGroupBoundaries (2 was expected): "+nr, 2 == nr);
		nr = mesh.scratchVirtualBoundaries();
		assertTrue("Wrong return value of scratchVirtualBoundaries (2 was expected): "+nr, 2 == nr);
	}
	
	@Test public void groupsWithTriangleSet()
	{
		TriangleTraitsBuilder ttb = new TriangleTraitsBuilder();
		ttb.addHalfEdge();
		MeshTraitsBuilder mtb = new MeshTraitsBuilder();
		mtb.addTriangleSet();
		mtb.add(ttb);
		mesh = new Mesh(mtb);
		createMxNShell(3, 3);
		for (int i = 0; i < 8; i++)
			T[i].setGroupId(i/4);
		assertTrue("Wrong number of triangles: "+mesh.getTriangles().size(), 8 == mesh.getTriangles().size());
		mesh.buildAdjacency();
		assertTrue("Wrong number of triangles after buildAdjacency: "+mesh.getTriangles().size(), 16 == mesh.getTriangles().size());
		int nr = mesh.buildGroupBoundaries();
		assertTrue("Wrong return value of buildGroupBoundaries (2 was expected): "+nr, 2 == nr);
		nr = mesh.scratchVirtualBoundaries();
		assertTrue("Wrong return value of scratchVirtualBoundaries (2 was expected): "+nr, 2 == nr);
	}
	
}
