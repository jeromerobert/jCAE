/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2009, by EADS France

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

package org.jcae.mesh.amibe.projection;

import org.jcae.mesh.amibe.algos3d.SphereBuilder;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.junit.Test;
import static org.junit.Assert.*;

public class MeshLiaisonTest
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

	@Test public void finderOnPlane()
	{
		mesh = new Mesh();
		createMxNShell(3, 3);
		mesh.buildAdjacency();
		assertTrue("Wrong number of triangles after buildAdjacency: "+mesh.getTriangles().size(), 16 == mesh.getTriangles().size());
		MeshLiaison liaison = new MeshLiaison(mesh);
		Mesh newMesh = liaison.getMesh();
		Vertex vTest = newMesh.createVertex(2.1, 1.8, 0.1);
		AbstractHalfEdge ref = liaison.findSurroundingTriangle(vTest, v[0], -1.0, true);
		assertTrue("findSurroundingTriangle failed", ref != null);
		AbstractHalfEdge ot = liaison.findSurroundingTriangle(vTest, v[0], 0.2, true);
		assertTrue("findSurroundingTriangle failed", ot != null);
		assertTrue("findSurroundingTriangle failed", ref == ot);
	}

	@Test public void finderOnSphere()
	{
		mesh = SphereBuilder.createShuffledSphereMesh(3);
		assertTrue("Mesh is not valid", mesh.isValid());
		MeshLiaison liaison = new MeshLiaison(mesh, MeshTraitsBuilder.getDefault3D().addNodeList());
		Mesh newMesh = liaison.getMesh();
		// Find poles
		Vertex northPole = null;
		Vertex vPos = newMesh.createVertex(0, 0, 1);
		double dmin = Double.MAX_VALUE;
		for (Vertex vP : newMesh.getNodes())
		{
			double d = vPos.sqrDistance3D(vP);
			if (d < dmin)
			{
				northPole = vP;
				dmin = d;
			}
		}
		// Start from South Pole
		Vertex vTest = newMesh.createVertex(0, 0, -1);

		AbstractHalfEdge ref = liaison.findSurroundingTriangle(northPole, vTest, -1.0, false);
		assertTrue("findSurroundingTriangle failed", ref != null);
		AbstractHalfEdge ot = liaison.findSurroundingTriangle(northPole, vTest, 0.2, false);
		assertTrue("findSurroundingTriangle failed", ot != null);
		assertTrue("findSurroundingTriangle failed", ref == ot);
		Triangle tRes = ot.getTri();
		assertTrue("North Pole not found", tRes.vertex[0] == northPole || tRes.vertex[1] == northPole || tRes.vertex[2] == northPole);
	}
	
}
