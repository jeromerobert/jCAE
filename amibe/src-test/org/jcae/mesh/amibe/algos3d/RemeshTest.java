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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.amibe.algos3d;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.xmldata.MeshReader;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import org.junit.Test;

public class RemeshTest
{
	private Mesh bgMesh;
	private Vertex [] v;
	private Triangle [] T;

	// m Vertex on rows, n Vertex on columns
	private void createMxNCylinder(int m, int n)
	{
		/*   v3       v4        v5 
		 *   +---------+---------+
		 *   | \       | \       |
		 *   |   \  T1 |   \  T3 |
		 *   |     \   |     \   |
		 *   |  T0   \ |  T2   \ |
		 *   +---------+---------+
		 *   v0        v1       v2
		 */
		v = new Vertex[m*n];
		for (int j = 0; j < n; j++)
		{
			double dtheta = 2.0 * Math.PI / m;
			double z = 2.0 * Math.PI * j / (n - 1.0);
			for (int i = 0; i < m; i++)
			{
				v[m*j+i] = bgMesh.createVertex(
					Math.cos(i*dtheta),
					Math.sin(i*dtheta), z);
			}
		}
		for (int i = 0; i < v.length; i++)
			v[i].setLabel(i);
		T = createMxNTriangles(m, n, v);
	}
	
	// m Vertex on rows, n Vertex on columns
	private void createMxNShell(int m, int n)
	{
		/*   v3       v4        v5 
		 *   +---------+---------+
		 *   | \       | \       |
		 *   |   \  T1 |   \  T3 |
		 *   |     \   |     \   |
		 *   |  T0   \ |  T2   \ |
		 *   +---------+---------+
		 *   v0        v1       v2
		 */
		v = new Vertex[m*n];
		for (int j = 0; j < n; j++)
			for (int i = 0; i < m; i++)
				v[m*j+i] = bgMesh.createVertex(i,j,0);
		for (int i = 0; i < v.length; i++)
			v[i].setLabel(i);
		T = createMxNTriangles(m, n, v);
	}

	private Triangle [] createMxNTriangles(int m, int n, Vertex [] vv)
	{
		Triangle [] tt = new Triangle[2*(m-1)*(n-1)];
		for (int j = 0; j < n-1; j++)
		{
			for (int i = 0; i < m-1; i++)
			{
				tt[2*(m-1)*j+2*i]   = bgMesh.createTriangle(vv[m*j+i], vv[m*j+i+1], vv[m*(j+1)+i]);
				tt[2*(m-1)*j+2*i+1] = bgMesh.createTriangle(vv[m*j+i+1], vv[m*(j+1)+i+1], vv[m*(j+1)+i]);
				vv[m*j+i].setLink(tt[2*(m-1)*j+2*i]);
			}
			vv[m*j+m-1].setLink(tt[2*(m-1)*j+2*m-3]);
		}
		// Last row
		for (int i = 0; i < m-1; i++)
			vv[m*(n-1)+i].setLink(tt[2*(m-1)*(n-2)+2*i]);
		vv[m*n-1].setLink(tt[2*(m-1)*(n-1)-1]);
		for (Triangle t: tt)
			bgMesh.add(t);
		return tt;
	}

	private void testShell(int m, int n, String size)
	{
		final Map<String, String> options = new HashMap<String, String>();
		options.put("size", size);
		bgMesh = new Mesh();
		createMxNShell(m, n);
		bgMesh.buildAdjacency();
		assertTrue("Mesh is not valid", bgMesh.isValid());
		Mesh newMesh = new Remesh(bgMesh, options).compute().getOutputMesh();
		assertTrue("Mesh is not valid", newMesh.isValid());
	}

	@Test public void testShellLarge()
	{
		testShell(3, 3, "0.5");
	}

	@Test public void testSphere()
	{
		bgMesh = SphereBuilder.createShuffledSphereMesh(3);
		assertTrue("Mesh is not valid", bgMesh.isValid());
		final Map<String, String> options = new HashMap<String, String>();
		options.put("size", "0.02");
		Mesh newMesh = new Remesh(bgMesh, options).compute().getOutputMesh();
		assertTrue("Mesh is not valid", newMesh.isValid());
		assertTrue("Mesh contains inverted triangles", newMesh.checkNoInvertedTriangles());
	}
	
	@Test public void test4Neighbors()
	{
		/*   v2       v3
		 *   +---------+
		 *   | \       |
		 *   |   \  T1 |
		 *   |     \   |
		 *   |  T0   \ |
		 *   +---------+
		 *   v0        v1
		 */
		bgMesh = new Mesh();
		createMxNShell(2, 2);
		bgMesh.buildAdjacency();
		// Add a middle point
		Vertex mid = bgMesh.createVertex(0.3, 0.1, 0.0);
		bgMesh.vertexSplit(T[0].getAbstractHalfEdge(), mid);
		assertTrue("Mesh is not valid", bgMesh.isValid());

		final Map<String, String> options = new HashMap<String, String>();
		options.put("size", "0.01");
		Mesh newMesh = new Remesh(bgMesh, options).compute().getOutputMesh();
		assertTrue("Mesh is not valid", newMesh.isValid());
	}

	@Test public void testTorus()
	{
		MeshTraitsBuilder mtb = MeshTraitsBuilder.getDefault3D();
		mtb.addNodeList();
		bgMesh = new Mesh(mtb);
		try {
			MeshReader.readObject3D(bgMesh, "test"+File.separator+"input"+File.separator+"torus1426");
		} catch (IOException ex) {
			Logger.getLogger(RemeshTest.class.getName()).log(Level.SEVERE, null, ex);
			throw new RuntimeException();
		}
		assertTrue("Mesh is not valid", bgMesh.isValid());

		final Map<String, String> options = new HashMap<String, String>();
		options.put("size", "0.05");
		options.put("ridgeAngle", "0.9");
		Mesh newMesh = new Remesh(bgMesh, options).compute().getOutputMesh();
// try { org.jcae.mesh.xmldata.MeshWriter.writeObject3D(newMesh, "XXX", null); } catch (IOException ex) { ex.printStackTrace(); throw new RuntimeException(ex); }

		assertTrue("Mesh is not valid", newMesh.isValid());
		assertTrue("Mesh contains inverted triangles", newMesh.checkNoInvertedTriangles());
	}

}
