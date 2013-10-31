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

import java.util.Arrays;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.junit.Test;

/**
 *
 * @author Jerome Robert
 */
public class TriangleProjectorTest {
	@Test public static void test()
	{
		Mesh mesh = new Mesh(MeshTraitsBuilder.getDefault3D());
		TriangleProjector tp = new TriangleProjector();
		Vertex v0 = mesh.createVertex(0, 0, 0);
		Vertex v1 = mesh.createVertex(1, 0, 0);
		Vertex v2 = mesh.createVertex(0, 1, 0);
		Triangle triangle = mesh.createTriangle(v0, v1, v2);
		//tag v1-v2 as boundary
		triangle.getAbstractHalfEdge().setAttributes(AbstractHalfEdge.BOUNDARY);
		Vertex vp = mesh.createVertex(0, 0, 0);

		vp.moveTo(1, 1, 0);
		tp.projectOnBoundary(vp, triangle);
		System.err.println(tp);
		assert tp.getType() == TriangleProjector.ProjectionType.EDGE;

		vp.moveTo(-0.5, 0.5, 1);
		tp.projectOnBoundary(vp, triangle);
		System.err.println(tp);
		assert tp.getType() == TriangleProjector.ProjectionType.VERTEX;

		vp.moveTo(0.5, 0.01, 0);
		tp.project(vp, triangle);
		System.err.println(tp);
		assert tp.getType() == TriangleProjector.ProjectionType.EDGE;

		vp.moveTo(0.1, 0.2, 0.5);
		tp.project(vp, triangle);
		System.err.println(tp);
		assert tp.getType() == TriangleProjector.ProjectionType.FACE;

		vp.moveTo(-1, -1, 0);
		tp.project(vp, triangle);
		System.err.println(tp);
		assert tp.getType() == TriangleProjector.ProjectionType.OUT;

		vp.moveTo(0, 0.1, 0);
		tp.project(vp, triangle);
		System.err.println(tp);
		assert tp.getType() == TriangleProjector.ProjectionType.EDGE;

		vp.moveTo(0, 0.01, 0);
		tp.project(vp, triangle);
		System.err.println(tp);
		assert tp.getType() == TriangleProjector.ProjectionType.VERTEX;

		vp.moveTo(0, 100.0, 0);
		tp.project(vp, triangle);
		System.err.println(Arrays.toString(tp.onEdgeLine)+" "+Arrays.toString(tp.edgeAbscissa));
		assert tp.onEdgeLine[2];

		v0.moveTo(-2000, 2625, -1000);
		v1.moveTo(4000, -3000, -1000);
		v2.moveTo(-2000, 2250, -1000);
		vp.moveTo(1000, 0, -1000);
		tp.project(vp, triangle);
		System.err.println(tp);
		assert tp.getType() == TriangleProjector.ProjectionType.OUT;

		v0.moveTo(1000.0, 1875, -1000);
		v1.moveTo(1000, 2250, -1000);
		v2.moveTo(4000, 1500, -1000);
		vp.moveTo(1000, 293.35, -950);
		tp.project(vp, triangle);
		System.err.println(tp);
		assert tp.getType() == TriangleProjector.ProjectionType.OUT;

		TriangleHelper th = new TriangleHelper(triangle);
		double[] uv = new double[2];
		th.getBarycentricCoords(vp, uv);
		assert th.getLocation(uv[0], uv[1]).sqrDistance3D(tp.getProjection()) < 1E-8;
	}
}
