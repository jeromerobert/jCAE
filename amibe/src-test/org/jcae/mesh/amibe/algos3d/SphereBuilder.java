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

package org.jcae.mesh.amibe.algos3d;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.MeshOEMMIndex;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.oemm.MeshReader;
import org.jcae.mesh.oemm.OEMM;
import org.jcae.mesh.oemm.Storage;

import org.junit.Ignore;

/**
 * Create a regular mesh of a sphere.  We first generate an icosahedron,
 * this is the Platonic solid which is the best approximation of a sphere
 * by equilateral triangles.  Then each triangle is refined and new vertices
 * are projected onto a sphere.  Triangles are no more equilateral, but
 * deviation is very low.  Valence of the 12 initial vertices is 5, and all
 * other vertices have a valence of 6.
 *
 * @author Denis Barbier
 */
@Ignore("Utility class")
public class SphereBuilder
{
	private final static double TAU = 0.5 * (1.0 + Math.sqrt(5.0));
	private final static double [] ICO = new double[] {
		   0.0,  1.0,  TAU,
		   0.0, -1.0,  TAU,
		   0.0,  1.0, -TAU,
		   0.0, -1.0, -TAU,
		   1.0,  TAU,  0.0,
		  -1.0,  TAU,  0.0,
		   1.0, -TAU,  0.0,
		  -1.0, -TAU,  0.0,
		   TAU,  0.0,  1.0,
		  -TAU,  0.0,  1.0,
		   TAU,  0.0, -1.0,
		  -TAU,  0.0, -1.0
	};
	private final static double SCALE = 1.0 / Math.sqrt(1.0 + TAU*TAU);
	private final static int [] FACES = new int[] {
		 0,  1,  8,
		 8,  1,  6,
		 6,  1,  7,
		 7,  1,  9,
		 9,  1,  0,
		
		 2,  5,  4,
		 4,  5,  0,
		 0,  5,  9,
		 9,  5, 11,
		11,  5,  2,
		 
		 2,  3, 11,
		11,  3,  7,
		 7,  3,  6,
		 6,  3, 10,
		10,  3,  2,
		 
		 9, 11,  7,
		 8,  4,  0,
		 6, 10,  8,
		 4,  8, 10,
		 4, 10,  2
	};
	private final Set<UnindexedTriangle> triangles = new HashSet<UnindexedTriangle>();

	@Ignore("Inner class")
	private static class UnindexedTriangle
	{
		final double [] coord = new double[9];
		int group;
		UnindexedTriangle()
		{
		}

		public UnindexedTriangle(double [] v0, double [] v1, double [] v2, int group)
		{
			this.group = group;
			System.arraycopy(v0, 0, coord, 0, 3);
			System.arraycopy(v1, 0, coord, 3, 3);
			System.arraycopy(v2, 0, coord, 6, 3);
		}
		
	}
	
	private void createIcosahedron()
	{
		for (int i = 0; i < 20; i++)
		{
			UnindexedTriangle t = new UnindexedTriangle();
			t.group = i + 1;
			for (int j = 0; j < 3; j++)
			{
				int index = FACES[3*i+j];
				t.coord[3*j]   = SCALE * ICO[3*index];
				t.coord[3*j+1] = SCALE * ICO[3*index+1];
				t.coord[3*j+2] = SCALE * ICO[3*index+2];
			}
			triangles.add(t);
		}
	}
	
	private void refine()
	{
		double [] x = new double[3];
		double [][] vertex = new double[3][3];
		double [][] middle = new double[3][3];
		for (UnindexedTriangle t : new ArrayList<UnindexedTriangle>(triangles))
		{
			int last = 2;
			for (int i = 0; i < 3; i++)
			{
				vertex[i][0] = t.coord[3*i];
				vertex[i][1] = t.coord[3*i+1];
				vertex[i][2] = t.coord[3*i+2];
				for (int j = 0; j < 3; j++)
					x[j] = 0.5 * (t.coord[3*i+j] + t.coord[3*last+j]);
				double invNorm = 1.0 / Matrix3D.norm(x);
				middle[i][0] = x[0] * invNorm;
				middle[i][1] = x[1] * invNorm;
				middle[i][2] = x[2] * invNorm;
				last = i;
			}
			triangles.add(new UnindexedTriangle(vertex[0], middle[1], middle[0], t.group));
			triangles.add(new UnindexedTriangle(vertex[1], middle[2], middle[1], t.group));
			triangles.add(new UnindexedTriangle(vertex[2], middle[0], middle[2], t.group));
			triangles.add(new UnindexedTriangle(middle[0], middle[1], middle[2], t.group));
			triangles.remove(t);
		}
	}
	
	private boolean writeSoup(String dirname) throws IOException
	{
		File dir = new File(dirname);
		if (!dir.isDirectory())
			dir.mkdirs();
		DataOutputStream out = new DataOutputStream(
			new BufferedOutputStream(new FileOutputStream(dirname+File.separator+"soup")));
		for (UnindexedTriangle t : triangles)
		{
			for (int i = 0; i < 9; i++)
				out.writeDouble(t.coord[i]);
			out.writeInt(t.group);
			out.writeInt(0);
		}
		out.close();
		return true;
	}
	
	private static Mesh createSphereMesh(int level)
	{
		SphereBuilder sphere = new SphereBuilder();
		// Create icosahedron
		sphere.createIcosahedron();

		// Refine icosahedron
		for (int i = level; i > 0; i--)
			sphere.refine();
		
		// Write triangle soup and oemm into the same temporary dir
		String tmpdir;
		try {
			File tempFile = File.createTempFile("oemm", ".dir");
			tmpdir = tempFile.getAbsolutePath();
			tempFile.delete();
			File tempDir = new File(tmpdir);
			tempDir.mkdir();
			sphere.writeSoup(tmpdir);
		} catch (IOException ex) {
			Logger.getLogger(SphereBuilder.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		}
		
		// Build oemm
		String [] mainArgs = new String[4];
		mainArgs[0] = tmpdir;
		mainArgs[1] = tmpdir;
		mainArgs[2] = "4";
		mainArgs[3] = "50000";
		MeshOEMMIndex.main(mainArgs);
		// Read oemm into a Mesh
		OEMM oemm = Storage.readOEMMStructure(mainArgs[1]);
		MeshReader mr = new MeshReader(oemm);
		Mesh toReturn = mr.buildWholeMesh();
		// Clean up
		return toReturn;
	}
	
	// Move vertices
	public static Mesh createShuffledSphereMesh(int level)
	{
		Mesh mesh = createSphereMesh(level);
		for (Vertex v : mesh.getNodes())
		{
			if (v.getRef() > 0)
				continue;
			double c = Math.cos(Math.PI * v.getZ());
			if (v.getZ() >= 0.0)
				v.moveTo(v.getX(), v.getY(), (1.0 - c*c*c*c*c) / 2.0);
			else
				v.moveTo(v.getX(), v.getY(), - (1.0 - c*c*c*c*c) / 2.0);

			double r = Math.sqrt(v.getX()*v.getX() + v.getY()*v.getY() + v.getZ()*v.getZ());
			v.scale(1/r);
		}
		return mesh;
	}
	
}
