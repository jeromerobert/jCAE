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
 * (C) Copyright 2014, by Airbus Group SAS
 */


package org.jcae.mesh.amibe.projection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.algos3d.TriMultPoly;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.xmldata.Amibe2VTK;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;

/**
 * Insert an edge into a triangulation.
 * Triangles under the edge are removed, then the created hole is remeshed
 * taking the inserted edge into account.
 * @author Jerome Robert
 */
public class EdgeProjectorNG {
	private final Mesh mesh;
	private final TriangleKdTree kdTree;
	private final double tolerance;
	private final TriangleFinder triangleFinder = new TriangleFinder();
    private final TriMultPoly triMultPoly = new TriMultPoly();
	private final HoleFiller holeFiller = new HoleFiller();

	private static void compteEdgeAABB(Vertex v1, Vertex v2, double[] aabb)
	{
		aabb[0] = Math.min(v1.getX(), v2.getX());
		aabb[1] = Math.min(v1.getY(), v2.getY());
		aabb[2] = Math.min(v1.getZ(), v2.getZ());
		aabb[3] = Math.max(v1.getX(), v2.getX());
		aabb[4] = Math.max(v1.getY(), v2.getY());
		aabb[5] = Math.max(v1.getZ(), v2.getZ());
	}

	private static double normalize(double[] m, int c)
	{
		c = c * 4;
		double norm = Math.sqrt(m[c] * m[c] + m[c+1] * m[c+1] + m[c+2] * m[c+2]);
		m[c] /= norm;
		m[c+1] /= norm;
		m[c+2] /= norm;
		return norm;
	}

	/**
	 * Compute a transformation where the image of v1,v2 is along x.
	 * @param m a 3x4 matrix, rotation and translation
	 * see From Efficient Construction of Perpendicular Vectors without Branching
	 * by Michael M. Stark
	 */
	private static double nullSpace(Vertex v1, Vertex v2, double[] m)
	{
		// image of X
		m[0] = v2.getX() - v1.getX();
		m[1] = v2.getY() - v1.getY();
		m[2] = v2.getZ() - v1.getZ();
		double norm = normalize(m, 0);
		// compute a vector very not collinear to X
		double x = Math.abs(m[0]);
		double y = Math.abs(m[1]);
		double z = Math.abs(m[2]);
		boolean uyx = (x - y) < 0;
		boolean uzx = (x - z) < 0;
		boolean uzy = (y - z) < 0;
		boolean xm = uyx && uzx;
		boolean ym = !xm && uzy;
		boolean zm = !(xm || ym);
		x = xm ? 1 : 0;
		y = ym ? 1 : 0;
		z = zm ? 1 : 0;
		// image of Y
		m[4] = m[2] * y - m[1] * z;
		m[5] = m[0] * z - m[2] * x;
		m[6] = m[1] * x - m[0] * y;
		normalize(m, 1);
		// image of Z
		m[8] = m[1] * m[6] - m[2] * m[5];
		m[9] = m[4] * m[2] - m[6] * m[0];
		m[10] = m[0] * m[5] - m[1] * m[4];

		// translation
		x = -v1.getX();
		y = -v1.getY();
		z = -v1.getZ();
		m[3] = m[0] * x + m[1] * y + m[2] * z;
		m[7] = m[4] * x + m[5] * y + m[6] * z;
		m[11] = m[8] * x + m[9] * y + m[10] * z;
		return norm;
	}

	private class TriangleFinder
	{
		private final Collection<Triangle> trianglesInAABB = HashFactory.createSet();
		public final Collection<Triangle> trianglesInOBB = HashFactory.createSet();
		private final double[] aabb = new double[6];
		private final double[] matrix = new double[12];
		private final TriangleInterAABB taabb = new TriangleInterAABB();
		/**
		 * Find the triangles which must be recreated.
		 * Fill trianglesInOBB
		 */
		public void findTriangles(Vertex v1, Vertex v2, int group)
		{
			compteEdgeAABB(v1, v2, aabb);
			for(int i = 0; i < 3; i++)
			{
				aabb[i] -= tolerance;
				aabb[3+i] += tolerance;
			}
			trianglesInAABB.clear();
			kdTree.getNearTriangles(aabb, trianglesInAABB, group);
			double norm = nullSpace(v1, v2, matrix);
			for(int i = 0; i < 3; i++)
			{
				aabb[i] = -tolerance;
				aabb[3+i] = tolerance;
			}
			aabb[3] += norm;
			trianglesInOBB.clear();
			for(Triangle t: trianglesInAABB)
			{
				if(isProjectionAllowed(t))
				{
					taabb.setTriangle(t, matrix);
					if(taabb.triBoxOverlap(aabb, true))
						trianglesInOBB.add(t);
				}
			}
		}
	}

	public EdgeProjectorNG(Mesh mesh, TriangleKdTree kdTree, double tolerance) {
		this.mesh = mesh;
		this.kdTree = kdTree;
		this.tolerance = tolerance;
	}

	public void project(Vertex v1, Vertex v2, int group)
	{
		triangleFinder.findTriangles(v1, v2, group);
		ArrayList<AbstractHalfEdge> border = new ArrayList<AbstractHalfEdge>();
		for(Triangle t:triangleFinder.trianglesInOBB)
		{
			AbstractHalfEdge e = t.getAbstractHalfEdge();
			for(int i = 0; i < 3; i++)
			{
				if(!triangleFinder.trianglesInOBB.contains(e.sym().getTri()))
					border.add(e);
				e = e.next();
			}
		}
		holeFiller.triangulate(mesh, border,
			Collections.singleton(Arrays.asList(v1, v2)));
		for(Triangle t:triangleFinder.trianglesInOBB)
		{
			mesh.remove(t);
			kdTree.remove(t);
		}
		for(Triangle t:holeFiller.getNewTriangles())
		{
			mesh.add(t);
			kdTree.addTriangle(t);
		}
		assert mesh.isValid();
	}

	public void projectTriMultPoly(Vertex v1, Vertex v2, int group) throws IOException
	{
		//TODO make it work with delauney
		triMultPoly.setDelauneyTetra(false);
		triangleFinder.findTriangles(v1, v2, group);
		ArrayList<AbstractHalfEdge> border = new ArrayList<AbstractHalfEdge>();
		for(Triangle t:triangleFinder.trianglesInOBB)
		{
			AbstractHalfEdge e = t.getAbstractHalfEdge();
			for(int i = 0; i < 3; i++)
			{
				if(!triangleFinder.trianglesInOBB.contains(e.sym().getTri()))
					border.add(e);
				e = e.next();
			}
		}
		Vertex[] vToAdd = new Vertex[3];
		vToAdd[0] = v2;
		vToAdd[1] = v1;
		// Add a 3rd point so that the polyline is not degenerated
		vToAdd[2] = mesh.createVertex(
			v1.getX() - 1E-9 * (v1.getX() - v2.getX()),
			v1.getY(), v1.getZ());
		vToAdd[2] = mesh.createVertex(v1.getX(), v2.getY(), v1.getZ());
		triMultPoly.triangulate(mesh, border,
			Collections.singleton(Arrays.asList(vToAdd)));
		AbstractHalfEdge e = triMultPoly.getEdge(v1, vToAdd[2]);
		for(Triangle t:triMultPoly.getNewTriangles())
			mesh.add(t);
		for(Triangle t:triangleFinder.trianglesInOBB)
			mesh.remove(t);
		assert mesh.isValid();
		mesh.edgeCollapse(e, v1);
	}

	/**
	 * Return true is the projection can be done on this triangle.
	 * This methods aims at being redefine in subclasses.
	 * The default implementation return true.
	 */
	protected boolean isProjectionAllowed(Triangle triangle)
	{
		return true;
	}

	public static void test()
	{
		Mesh m = new Mesh(MeshTraitsBuilder.getDefault3D());
		Vertex v1 = m.createVertex(0,0,0);
		Vertex v2 = m.createVertex(1,0,0);
		Vertex v3 = m.createVertex(0,1,0);
		Vertex v4 = m.createVertex(0.1,0.1,0);
		Vertex v5 = m.createVertex(0.2,0.2,0);
		m.add(m.createTriangle(v1, v2, v3));
		m.buildAdjacency();
		TriangleKdTree mkdTree = new TriangleKdTree(m);
		EdgeProjectorNG projector = new EdgeProjectorNG(m, mkdTree, 0.1);
		projector.project(v4, v5, -1);
		assert m.isValid();
	}

	public static void main(final String[] args) {
		try {
			test();
			String file = "/tmp/A350-1000_FULL-Config_Masked_Shape3422.amibe";
			Mesh m = new Mesh(MeshTraitsBuilder.getDefault3D());
			MeshReader.readObject3D(m, file);
			Vertex v1 = new Vertex(null, 5969.1334634938175, 602.1781046704872, -2639.907227074926);
			Vertex v2 = new Vertex(null, 5974.089704834468, 576.5380328193008, -2675.8479767020112);
			TriangleKdTree mkdTree = new TriangleKdTree(m);
			EdgeProjectorNG projector = new EdgeProjectorNG(m, mkdTree, 0.1);
			projector.project(v1, v2, -1);
			String tmpFile = "/tmp/debug.amibe";
			MeshWriter.writeObject3D(m, tmpFile, null);
			new Amibe2VTK(tmpFile).write("/tmp/debug.vtp");
		} catch (Exception ex) {
			Logger.getLogger(EdgeProjectorNG.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
