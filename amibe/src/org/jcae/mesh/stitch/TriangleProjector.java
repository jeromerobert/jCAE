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
import org.jcae.mesh.amibe.metrics.Location;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;

/**
 *
 * @author Jerome Robert
 */
class TriangleProjector {
	public static enum ProjectionType { FACE, EDGE, VERTEX,
		/** The projection is out of the triangle. */
		OUT,
		/**
		 * The point to be projected is too far from the triangle but its
		 * projection is on the triangle
		 */
		FAR}
	private final Location projection = new Location();
	/** The vertex of the triangle where to project when type is VERTEX. */
	private Vertex vertexProject;
	/** The edge of the triangle where to project when type is EDGE. */
	private AbstractHalfEdge edgeProject;
	private ProjectionType type;
	/**
	 * The square of the distance between the projection on the triangle
	 * plane and each vertices.
	 */
	private final double[] dist2ToVertices = new double[3];
	/**
	 * vector from the triangle vertices to the projection on the triangle
	 * plane.
	 */
	private final double[][] vectToVertices = new double[3][3];
	/** The cross product between vectToVertices and edges. */
	private final double[][] crossProducts = new double[3][3];
	private double sqrDistance;
	/** Max projection distance. */
	public double sqrMaxDistance = Double.MAX_VALUE;
	/** Smallest allowed distance from vertex and edge for FACE project. */
	public double sqrTolerance = 0.01;
	public boolean boundaryOnly;

	/**
	 * The relative abscissa of the projected point on each edges.
	 * If this is between 0 and 1 the point is on the edge.
	 * If this is lower 0 or greater than 1 the point is on the edge line.
	 * If this is NaN the distance between the point and the edge line is
	 * greater than sqrTolerance.
	 * <ul>
	 * <li>edgeAbscissa[0] is for getAbstractHalfEdge().prev()</li>
	 * <li>edgeAbscissa[1] is for getAbstractHalfEdge()</li>
	 * <li>edgeAbscissa[2] is for getAbstractHalfEdge().next()</li>
	 * </ul>
	 */
	public double[] edgeAbscissa = new double[3];
	public boolean[] onEdgeLine = new boolean[3];
	public void project(Location point, Triangle triangle) {
		project(point, new TriangleHelper(triangle));
	}

	public void projectOnBoundary(Location point, Triangle t)
	{
		double sqrClosestEdgeDistance = Double.MAX_VALUE;
		AbstractHalfEdge currentEdge = t.getAbstractHalfEdge();
		type = ProjectionType.FAR;

		for (int i = 0; i < 3; i++) {
			assert currentEdge != null: t;
			if(currentEdge.hasAttributes(AbstractHalfEdge.BOUNDARY))
			{
				//use vectToVertices to store temporary vectors
				currentEdge.destination().sub(currentEdge.origin(), vectToVertices[0]);
				double triaEdgeSqrNorm = Matrix3D.prodSca(vectToVertices[0], vectToVertices[0]);
				point.sub(currentEdge.origin(), vectToVertices[1]);
				double r = Matrix3D.prodSca(vectToVertices[0], vectToVertices[1]) /
					triaEdgeSqrNorm;
				for(int j = 0; j < 3; j++)
					vectToVertices[2][j] = - vectToVertices[1][j] + vectToVertices[0][j] * r;
				sqrDistance = Matrix3D.prodSca(vectToVertices[2], vectToVertices[2]);
				if(sqrDistance < sqrMaxDistance && sqrDistance < sqrClosestEdgeDistance)
				{
					sqrClosestEdgeDistance = sqrDistance;
					projection.moveTo(vectToVertices[2][0], vectToVertices[2][1], vectToVertices[2][2]);
					projection.add(point);
					if(currentEdge.origin().sqrDistance3D(projection) < sqrTolerance)
					{
						type = ProjectionType.VERTEX;
						vertexProject = currentEdge.origin();
					}
					else if(projection.sqrDistance3D(currentEdge.destination()) < sqrTolerance)
					{
						type = ProjectionType.VERTEX;
						vertexProject = currentEdge.destination();
					}
					else if(r >= 0 && r <= 1)
					{
						type = ProjectionType.EDGE;
					}
					else
					{
						type = ProjectionType.OUT;
					}
					edgeProject = currentEdge;
				}
			}
			currentEdge = currentEdge.next();
		}
	}

	public void project(Location point, TriangleHelper th) {
		if(boundaryOnly)
		{
			projectOnBoundary(point, th.getTriangle());
			return;
		}
		// use vectToVertices[0] as a temporary vector
		point.sub(th.getTriangle().getV0(), vectToVertices[0]);
		double amDotAN = Matrix3D.prodSca(th.normal, vectToVertices[0]);
		double alpha = amDotAN / th.normAN2;
		sqrDistance = amDotAN * alpha;
		projection.moveTo(point.getX() - alpha * th.normal[0],
			point.getY() - alpha * th.normal[1],
			point.getZ() - alpha * th.normal[2]);

		for (int i = 0; i < 3; i++) {
			projection.sub(th.getTriangle().getV(i), vectToVertices[i]);
			dist2ToVertices[i] = Matrix3D.prodSca(vectToVertices[i],
				vectToVertices[i]);
			if (dist2ToVertices[i] < sqrTolerance) {
				if(sqrDistance < sqrMaxDistance)
				{
					vertexProject = th.getTriangle().getV(i);
					type = ProjectionType.VERTEX;
				}
				else
					type = ProjectionType.FAR;
				return;
			}
		}
		edgeProject = th.getTriangle().getAbstractHalfEdge().prev();
		for (int i = 0; i < 3; i++) {
			assert th.getTriangle().getV(i) == edgeProject.origin();			
			Matrix3D.prodVect3D(th.edges[i], vectToVertices[i], crossProducts[i]);
			double edgeNorm = th.getEdgeNorm(i);
			double normMP2 = Matrix3D.prodSca(crossProducts[i], crossProducts[i]) / edgeNorm;
			
			// Compute edgeAbsissa because even if don't need them right here
			// we may need them in the outVertex method.
			double dot = Matrix3D.prodSca(vectToVertices[i], th.edges[i]);
			dot /= edgeNorm;
			edgeAbscissa[i] = dot;
			onEdgeLine[i] = normMP2 < sqrTolerance;
			if (onEdgeLine[i]) {
				if(sqrDistance >= sqrMaxDistance)
				{
					type = ProjectionType.FAR;
					return;
				}
				type = ProjectionType.EDGE;
				if (dot > 0 && dot < 1) {
					projection.moveTo(th.getTriangle().getV(i).getX() + dot * th.edges[i][0],
						th.getTriangle().getV(i).getY() + dot * th.edges[i][1],
						th.getTriangle().getV(i).getZ() + dot * th.edges[i][2]);
					return;
				}
			}
			edgeProject = edgeProject.next();
		}
		for (int i = 0; i < 3; i++) {
			if (Matrix3D.prodSca(crossProducts[i], th.normal) < 0) {
				type = ProjectionType.OUT;
				return;
			}
		}
		if(sqrDistance < sqrMaxDistance)
			type = ProjectionType.FACE;
		else
			type = ProjectionType.FAR;
	}

	/**
	 * Return the distance between the point and it's projection on the
	 * triangle plane.
	 * As the projection may be fused to close edges or vertices this
	 * distance may not be exactly the one between the projected point
	 * and getProjection().
	 */
	public double getSqrDistance() {
		return sqrDistance;
	}

	public Location getProjection() {
		return projection;
	}

	public Vertex getVertex() {
		assert type == ProjectionType.VERTEX;
		return vertexProject;
	}

	public AbstractHalfEdge getEdge() {
		assert type == ProjectionType.EDGE;
		return edgeProject;
	}

	public ProjectionType getType() {
		return type;
	}

	@Override
	public String toString() {
		return "TriangleProjector{" + "projection=" + projection + ", type=" +
			type + ", sqrDistance=" + sqrDistance + '}';
	}

	public void reset()
	{
		type = ProjectionType.FAR;
		sqrDistance = Double.POSITIVE_INFINITY;
	}

	public static void test()
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
		assert tp.getType() == ProjectionType.EDGE;

		vp.moveTo(-0.5, 0.5, 1);
		tp.projectOnBoundary(vp, triangle);
		System.err.println(tp);
		assert tp.getType() == ProjectionType.VERTEX;

		vp.moveTo(0.5, 0.01, 0);
		tp.project(vp, triangle);
		System.err.println(tp);
		assert tp.getType() == ProjectionType.EDGE;

		vp.moveTo(0.1, 0.2, 0.5);
		tp.project(vp, triangle);
		System.err.println(tp);
		assert tp.getType() == ProjectionType.FACE;

		vp.moveTo(-1, -1, 0);
		tp.project(vp, triangle);
		System.err.println(tp);
		assert tp.getType() == ProjectionType.OUT;

		vp.moveTo(0, 0.1, 0);
		tp.project(vp, triangle);
		System.err.println(tp);
		assert tp.getType() == ProjectionType.EDGE;

		vp.moveTo(0, 0.01, 0);
		tp.project(vp, triangle);
		System.err.println(tp);
		assert tp.getType() == ProjectionType.VERTEX;

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
		assert tp.getType() == ProjectionType.OUT;

		v0.moveTo(1000.0, 1875, -1000);
		v1.moveTo(1000, 2250, -1000);
		v2.moveTo(4000, 1500, -1000);
		vp.moveTo(1000, 293.35, -950);
		tp.project(vp, triangle);
		System.err.println(tp);
		assert tp.getType() == ProjectionType.OUT;

		TriangleHelper th = new TriangleHelper(triangle);
		double[] uv = new double[2];
		th.getBarycentricCoords(vp, uv);
		assert th.getLocation(uv[0], uv[1]).sqrDistance3D(tp.getProjection()) < 1E-8;
	}
}
