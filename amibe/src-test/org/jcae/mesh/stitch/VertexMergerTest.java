/*
 * Project Info:  http://jcae.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2013, by EADS France
 */

package org.jcae.mesh.stitch;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Location;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.junit.Test;

/**
 *
 * @author Jerome Robert
 */
public class VertexMergerTest extends VertexMerger {

	private int counter;
	private void isValid(Mesh mesh)
	{
		assert mesh.isValid();
		assert mesh.checkNoDegeneratedTriangles();
		assert mesh.checkNoInvertedTriangles();
		/*mesh.checkAdjacency();
		try {
			MeshWriter.writeObject3D(mesh, "/tmp/tmp.amibe", null);
			new Amibe2VTK("/tmp/tmp.amibe").write("/tmp/merger"+(counter++)+".vtp");
		} catch (Exception ex) {
			Logger.getLogger(VertexMerger.class.getName()).log(Level.SEVERE,
				null, ex);
		}*/
	}

	@Test public void test1()
	{
		Mesh mesh = new Mesh(MeshTraitsBuilder.getDefault3D());
		Vertex v0 = mesh.createVertex(0, 0, 0);
		Vertex v1 = mesh.createVertex(0, 1, 0);
		Vertex v11 = mesh.createVertex(0, 1, 0);
		Vertex v2 = mesh.createVertex(1, 0, 0);
		Vertex v3 = mesh.createVertex(-1, 0, 0);
		Vertex v4 = mesh.createVertex(0, 0, 1);
		Triangle tA = mesh.createTriangle(v0, v1, v2);
		Triangle tB = mesh.createTriangle(v0, v3, v1);
		Triangle tC = mesh.createTriangle(v0, v11, v4);
		mesh.add(tA);
		mesh.add(tB);
		mesh.add(tC);
		mesh.buildAdjacency();
		assert mesh.checkAdjacency();
		merge(mesh, v11, v1, v11);
		isValid(mesh);
		assert !v11.isManifold();
		unmerge(mesh, v11);
		isValid(mesh);
		assert v11.isManifold();
	}

	private Vertex[][] createGrid(Mesh mesh, int n, int m, Location start, double[] v1, double[] v2)
	{
		Vertex[][] toReturn = new Vertex[n][m];
		for(int i = 0; i < n; i++)
			for(int j = 0; j < m; j++)
			{
				toReturn[i][j] = mesh.createVertex(
					start.getX()+i*v1[0]+j*v2[0],
					start.getY()+i*v1[1]+j*v2[1],
					start.getZ()+i*v1[2]+j*v2[2]);
			}
		for(int i = 0; i < n - 1; i++)
			for(int j = 0; j < m - 1; j++)
			{
				mesh.add(mesh.createTriangle(toReturn[i][j], toReturn[i+1][j], toReturn[i][j+1]));
				mesh.add(mesh.createTriangle(toReturn[i+1][j+1], toReturn[i][j+1], toReturn[i+1][j]));
			}
		return toReturn;
	}

	@Test public void test2()
	{
		Mesh mesh = new Mesh(MeshTraitsBuilder.getDefault3D());
		Vertex[][] vs1 = createGrid(mesh, 3, 5, new Location(),
			new double[]{1, 0, 0}, new double[]{0, 1, 0});
		Vertex[][] vs2 = createGrid(mesh, 2, 5, new Location(1,0,0),
			new double[]{0, 0, 1}, new double[]{0, 1, 0});
		mesh.buildAdjacency();
		isValid(mesh);
		merge(mesh, vs2[0][0], vs1[1][0], vs2[0][0]);
		isValid(mesh);
		merge(mesh, vs2[0][1], vs1[1][1], vs2[0][1]);
		isValid(mesh);
		merge(mesh, vs2[0][3], vs1[1][3], vs2[0][3]);
		isValid(mesh);
		merge(mesh, vs2[0][4], vs1[1][4], vs2[0][4]);
		isValid(mesh);
		merge(mesh, vs2[0][2], vs1[1][2], vs2[0][2]);
		isValid(mesh);
		unmerge(mesh, vs2[0][2]);
		isValid(mesh);
		unmerge(mesh, vs2[0][4]);
		isValid(mesh);
		unmerge(mesh, vs2[0][3]);
		isValid(mesh);
		unmerge(mesh, vs2[0][1]);
		isValid(mesh);
		unmerge(mesh, vs2[0][0]);
		isValid(mesh);
	}
}
