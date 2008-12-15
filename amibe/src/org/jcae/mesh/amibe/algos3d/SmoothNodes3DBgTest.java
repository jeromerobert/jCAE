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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.amibe.algos3d;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import java.util.Map;
import java.util.HashMap;
import org.jcae.mesh.amibe.projection.SphereBuilder;
import org.jcae.mesh.amibe.validation.MinAngleFace;
import org.jcae.mesh.amibe.validation.QualityFloat;
import static org.junit.Assert.*;
import org.junit.Test;

public class SmoothNodes3DBgTest
{
	private Mesh mesh;
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
				v[m*j+i] = mesh.createVertex(i,j,0);
				/*v[m*j+i] = mesh.createVertex(
					Math.cos(i*dtheta),
					Math.sin(i*dtheta), z);*/
			}
		}
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
				tt[2*(m-1)*j+2*i]   = mesh.createTriangle(vv[m*j+i], vv[m*j+i+1], vv[m*(j+1)+i]);
				tt[2*(m-1)*j+2*i+1] = mesh.createTriangle(vv[m*j+i+1], vv[m*(j+1)+i+1], vv[m*(j+1)+i]);
				vv[m*j+i].setLink(tt[2*(m-1)*j+2*i]);
			}
			vv[m*j+m-1].setLink(tt[2*(m-1)*j+2*m-3]);
		}
		// Last row
		for (int i = 0; i < m-1; i++)
			vv[m*(n-1)+i].setLink(tt[2*(m-1)*(n-2)+2*i]);
		vv[m*n-1].setLink(tt[2*(m-1)*(n-1)-1]);
		for (Triangle t: tt)
			mesh.add(t);
		return tt;
	}
	
	private void testShell(int m, int n)
	{
		final Map<String, String> options = new HashMap<String, String>();
		options.put("iterations", "10");
		mesh = new Mesh();
		createMxNCylinder(m, n);
		mesh.buildAdjacency();
		assertTrue("Mesh is not valid", mesh.isValid());
		new SmoothNodes3D(mesh, options).compute();
		assertTrue("Mesh is not valid", mesh.isValid());
	}

	@Test public void testShellLarge()
	{
		testShell(3, 3);
	}

	@Test public void testSphere()
	{
		mesh = SphereBuilder.createShuffledSphereMesh(3);
		assertTrue("Mesh is not valid", mesh.isValid());
		final Map<String, String> options = new HashMap<String, String>();
		options.put("iterations", "20");
		options.put("check", "false");
		options.put("refresh", "true");
		options.put("relaxation", "0.9");
		Mesh smoothedMesh = new SmoothNodes3DBg(mesh, options).compute().getOutputMesh();
		assertTrue("Mesh is not valid", smoothedMesh.isValid());
		MinAngleFace qproc = new MinAngleFace();
		QualityFloat data = new QualityFloat(1000);
		data.setQualityProcedure(qproc);
                for (Triangle f: smoothedMesh.getTriangles())
                        data.compute(f);
                data.finish();
                data.setTarget((float) Math.PI/3.0f);
		double qmin = data.getValueByPercent(0.0);
		assertTrue("Min. angle too small: "+(qmin*180.0 / Math.PI), qmin > 0.9);
	}

}
