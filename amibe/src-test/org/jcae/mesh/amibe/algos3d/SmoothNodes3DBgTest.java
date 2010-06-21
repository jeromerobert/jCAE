/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2008,2010, by EADS France

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
import org.jcae.mesh.amibe.projection.MeshLiaison;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.amibe.validation.MinAngleFace;
import org.jcae.mesh.amibe.validation.QualityFloat;
import org.jcae.mesh.xmldata.MeshReader;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;

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
				v[m*j+i] = mesh.createVertex(
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
				v[m*j+i] = mesh.createVertex(i,j,0);
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
		createMxNShell(m, n);
		mesh.buildAdjacency();
		assertTrue("Mesh is not valid", mesh.isValid());
		Mesh smoothedMesh = new SmoothNodes3DBg(new MeshLiaison(mesh), options).compute().getOutputMesh();
		assertTrue("Mesh is not valid", smoothedMesh.isValid());
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
		options.put("relaxation", "1.0");
		Mesh smoothedMesh = new SmoothNodes3DBg(new MeshLiaison(mesh), options).compute().getOutputMesh();
		assertTrue("Mesh is not valid", smoothedMesh.isValid());
		MinAngleFace qproc = new MinAngleFace();
		QualityFloat data = new QualityFloat(1000);
		data.setQualityProcedure(qproc);
		data.setTarget((float) Math.PI/3.0f);
		for (Triangle f: smoothedMesh.getTriangles())
			data.compute(f);
		data.finish();
		double qmin = data.getValueByPercent(0.0);
		assertTrue("Min. angle too small: "+(qmin*60.0), qmin > 0.85);
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
		mesh = new Mesh();
		createMxNShell(2, 2);
		mesh.buildAdjacency();
		// Add a middle point
		Vertex mid = mesh.createVertex(0.3, 0.1, 0.0);
		mesh.vertexSplit(T[0].getAbstractHalfEdge(), mid);
		assertTrue("Mesh is not valid", mesh.isValid());

		final Map<String, String> options = new HashMap<String, String>();
		options.put("iterations", "1");
		options.put("relaxation", "1.0");
		Mesh smoothedMesh = new SmoothNodes3DBg(new MeshLiaison(mesh), options).compute().getOutputMesh();
		assertTrue("Mesh is not valid", smoothedMesh.isValid());

		MinAngleFace qproc = new MinAngleFace();
		QualityFloat data = new QualityFloat(10);
		data.setQualityProcedure(qproc);
		data.setTarget((float) Math.PI/4.0f);
		for (Triangle f: smoothedMesh.getTriangles())
		{
			if (f.isWritable())
				data.compute(f);
		}
		data.finish();
		double qmin = data.getValueByPercent(0.0);
		assertTrue("Min. angle too small: "+(qmin*45.0), qmin > 0.98);
	}

	@Ignore("Inner class")
	private static class CheckSmoothNodes3DBg extends SmoothNodes3DBg
	{

		public CheckSmoothNodes3DBg(MeshLiaison liaison, Map<String, String> options)
		{
			super(liaison, options);
		}
		public final boolean hasMoved()
		{
			return processed > 0;
		}
	}
	@Test public void testSingularQuadric()
	{
		/*
		 * This case was found in practice
		 */
		mesh = new Mesh();
		v = new Vertex[5];
		v[0] = mesh.createVertex(-0.62187508381251378, -0.34751501841852017,  0.085315828205300027);
		v[1] = mesh.createVertex(-0.63117823882666904, -0.31365340768558072,  0.053534273359757991);
		v[2] = mesh.createVertex(-0.61407841179235211, -0.34672837373181228,  0.055636259430818010);
		v[3] = mesh.createVertex(-0.59344399181142545, -0.38181921866005472,  0.058020011330282950);
		v[4] = mesh.createVertex(-0.60926256265657037, -0.34866382633430167,  0.034357448682585102);
		T = new Triangle[4];
		T[0] = mesh.createTriangle(v[0], v[1], v[2]);
		T[1] = mesh.createTriangle(v[2], v[3], v[0]);
		T[2] = mesh.createTriangle(v[3], v[2], v[4]);
		T[3] = mesh.createTriangle(v[4], v[2], v[1]);
		v[0].setLink(T[0]);
		v[1].setLink(T[0]);
		v[2].setLink(T[0]);
		v[3].setLink(T[1]);
		v[4].setLink(T[2]);
		for (Triangle t : T)
			mesh.add(t);
		mesh.buildAdjacency();
		assertTrue("Mesh is not valid", mesh.isValid());

		final Map<String, String> options = new HashMap<String, String>();
		options.put("iterations", "1");
		options.put("relaxation", "1.0");
		CheckSmoothNodes3DBg algo = new CheckSmoothNodes3DBg(new MeshLiaison(mesh), options);
		Mesh smoothedMesh = algo.compute().getOutputMesh();
		assertTrue("Mesh is not valid", smoothedMesh.isValid());
		assertTrue("Singular quadric detected", algo.hasMoved());
	}

	@Test public void testTorus()
	{
		MeshTraitsBuilder mtb = MeshTraitsBuilder.getDefault3D();
		mtb.addNodeList();
		mesh = new Mesh(mtb);
		try {
			MeshReader.readObject3D(mesh, "test"+File.separator+"input"+File.separator+"torus1426");
		} catch (IOException ex) {
			Logger.getLogger(SmoothNodes3DTest.class.getName()).log(Level.SEVERE, null, ex);
			throw new RuntimeException();
		}
		assertTrue("Mesh is not valid", mesh.isValid());
		SmoothNodes3DTest.shuffleTorus(mesh, 0.3, 1.0);

		final Map<String, String> options = new HashMap<String, String>();
		options.put("iterations", "50");
		options.put("check", "false");
		options.put("refresh", "true");
		options.put("relaxation", "0.9");
		Mesh smoothedMesh = new SmoothNodes3DBg(new MeshLiaison(mesh, mtb), options).compute().getOutputMesh();
		assertTrue("Mesh is not valid", smoothedMesh.isValid());
		MinAngleFace qproc = new MinAngleFace();
		QualityFloat data = new QualityFloat(1000);
		data.setQualityProcedure(qproc);
		data.setTarget((float) Math.PI/3.0f);
		for (Triangle f: smoothedMesh.getTriangles())
			data.compute(f);
		data.finish();
		double qmin = data.getValueByPercent(0.0);
		assertTrue("Min. angle too small: "+(qmin*60.0), qmin > 0.25);
	}

}
