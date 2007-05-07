/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2003,2004,2005,2006, by EADS CRC

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

package org.jcae.mesh.amibe.algos2d;

import org.jcae.mesh.mesher.ds.MMesh1D;
import org.jcae.mesh.amibe.patch.Mesh2D;

/**
 * Performs an initial Delaunay triangulation.
 * This algorithm is invoked to perform the initial triangulation of
 * each CAD patch, after the discretization of edges has been performed.
 * Wires of this patch are processed in turn.  All nodes belonging to
 * discretization of these edges are projected onto this 2D patch
 * and collected into a list.  These nodes are boundary nodes, and
 * all other nodes will be inserted in the interior domain.  A bounding
 * box enclosing all these nodes in the 2D space is computed, and
 * a {@link org.jcae.mesh.amibe.util.KdTree} instance can then be
 * initialized by
 * {@link Mesh2D#initQuadTree(double [], double [])}.
 *
 * <p>
 * Some checks have been added to remove tiny edges and make sure that
 * boundary is closed.  But this is a hack, the right solution is
 * to analyze the overall CAD structure and make sure that edges are well
 * connected and not too small.  In particular, the tolerance on vertex
 * location should be used to remove vertices which may be duplicates.
 * </p>
 *
 * <p>
 * A first triangle is created by iterating over the list of boundary nodes to
 * find three vertices which are not aligned.  The outer domain is also
 * triangulated; {@link org.jcae.mesh.amibe.ds.Mesh#outerVertex} is a vertex at
 * infinite, and three outer triangles are created by joining this vertex to
 * vertices of the first triangle.  With this trick, there is no need to have
 * special cases when vertices are inserted outside the convex hull of already
 * inserted vertices, and triangle location always succeed.  If these outer
 * triangles did not exist, we would have to triangulate the convex hull of
 * nodes.
 * </p>
 *
 * <p>
 * Boundary nodes are then inserted iteratively.  For the moment, an Euclidian
 * 2D metric is used because a 3D metric will not help on a very rough
 * triangulation.  The nearest vertex already inserted in the mesh is retrieved
 * with {@link org.jcae.mesh.amibe.util.KdTree#getNearestVertex(Mesh,Vertex)}.
 * It has a reference to a triangle containing this vertex.  From this starting
 * point, we search for the {@link org.jcae.mesh.amibe.ds.AbstractTriangle} containing
 * this boundary node by looking for adjacent triangles into the right
 * direction.  This <code>AbstractTriangle</code> is splitted into three triangles
 * (even if the vertex is inserted on an edge), and edges are swapped if they
 * are not Delaunay.
 * (This criterion also applied with our Euclidian 2D metric)
 * </p>
 *
 * <p>
 * When all boundary nodes are inserted, an unconstrained Delaunay mesh has
 * been built.  The list of boundary nodes computed previously gives a list of
 * boundary edges, which needs to be enforced.  This is performed by
 * {@link Mesh2D#forceBoundaryEdge(Vertex2D, Vertex2D, int)}; the segments
 * which intersect the enforced edge are swapped.
 * The {@link org.jcae.mesh.amibe.ds.AbstractHalfEdge#BOUNDARY} attribute is set on
 * these edges (and on matte edges).
 * </p>
 *
 * <p>
 * We know that the {@link org.jcae.mesh.amibe.ds.AbstractTriangle} bound to
 * {@link org.jcae.mesh.amibe.ds.Mesh#outerVertex} is an outer triangle.
 * Triangles adjacent through a boundary edge are interior triangles, and
 * triangles adjacent through non-boundary edges are also outer triangles.  All
 * triangles of the mesh are visited, and outer triangles are tagged with the
 * {@link org.jcae.mesh.amibe.ds.AbstractHalfEdge#OUTER} attribute.  If an
 * inconsistency is found (for instance a boundary edge seperate two outer
 * triangles), {@link org.jcae.mesh.amibe.InitialTriangulationException} is
 * raised.  This means that boundary was invalid, eg. it is not closed
 * or intersects itself.
 * This detection of broken boundaries could be improved by taking
 * advantage of some OpenCascade features, like the detection of
 * self-intersection and object tolerance.
 * </p>
 *
 * <p>
 * It is important to note that triangles in holes have their
 * <code>OUTER</code> attribute set, but are not linked to
 * {@link org.jcae.mesh.amibe.ds.Mesh#outerVertex}.  So this attribute is the
 * only safe way to detect outer triangles.  As outer triangles are not
 * removed, vertex location can still be performed as if the domain was convex.
 * All subsequent 2D algorithms should consider these points.
 * </p>
 *
 * <p>
 * This is very different when remeshing 3D meshes; in such a case,
 * boundary edges are detected because they have only one incident
 * face.  An outer triangle is then added by connecting end points to
 * {@link org.jcae.mesh.amibe.ds.Mesh#outerVertex}, but outer triangles are not
 * connected together.  Mesh domain is not convex, but that does not matter
 * because 3D algorithms do not require vertex location.
 * </p>
 *
 * <p>
 * After this initial triangulation has been performed, it is time to
 * add interior vertices to fulfill user's requirements.  The Euclidian
 * 2D metric is replaced by the 2D Riemannian metric
 * {@link org.jcae.mesh.amibe.metrics.Metric2D} induced by surface local
 * properties and user constraints.  But current triangles can cross
 * the whole surface, so metrics of its vertices may be very different.
 * There are two problems:
 * </p>
 * <ul>
 *   <li>Length computations are not accurate.</li>
 *   <li>If the parametrization of the surface has large variations,
 *       triangles in 3D space may be inverted.</li>
 * </ul>
 *
 * <p>
 * In order to improve accuracy, Frédéric Hecht advised to recursively
 * split segments when metrics at end points are very different.  This
 * has been implemented in
 * {@link org.jcae.mesh.amibe.patch.Calculus3D#distance(Vertex2D,Vertex2D)}
 * but did not give good results.  Now that the whole process works much
 * better, this issue could be investigated again.
 * </p>
 *
 * <p>
 * About inverted triangles, he also explained that we do not have to
 * care if large triangles are inverted in 3D, because they will be
 * fixed naturally when being splitted up.
 * </p>
 *
 * <p>
 * The current implementation begins with swapping edges (by calling
 * {@link org.jcae.mesh.amibe.patch.OTriangle2D#checkSmallerAndSwap})
 * if the opposite diagonal is smaller.
 * This method did improve some test cases, but is
 * certainly useless with the current meshing process because it has
 * been dramatically improved since these tests have been performed.
 * The following steps are then performed:
 * </p>
 * <ol>
 *   <li>Insert interior nodes (see {@link Insertion}) to
 *       have a mesh with target size of 16.</li>
 *   <li>Check compatibility between triangle normals and normals to
 *       the surface (see {@link ConstraintNormal3D}).  If triangle
 *       inversion gives better result, edges are swapped.</li>
 *   <li>Insert interior nodes to have a mesh with target size of 4.</li>
 *   <li>Check compatibility between triangle normals and normals to
 *       the surface.</li>
 *   <li>Insert interior nodes to have a mesh with target size of 1.</li>
 *   <li>Check compatibility between triangle normals and normals to
 *       the surface.</li>
 * </ol>
 */
public class BasicMesh
{
	private Mesh2D mesh = null;
	private MMesh1D mesh1d = null;
	
	/**
	 * Creates a <code>BasicMesh</code> instance.
	 *
	 * @param m  the data structure in which the mesh will be stored.
	 * @param m1d  discretization of edges.
	 */
	public BasicMesh(Mesh2D m, MMesh1D m1d)
	{
		mesh = m;
		mesh1d = m1d;
	}
	
	/**
	 * Launch method to mesh a surface.
	 */
	public void compute()
	{
		new Initial(mesh, mesh1d).compute();
		mesh.pushCompGeom(3);
		new Insertion(mesh, 16.0).compute();
		new ConstraintNormal3D(mesh).compute();
		new Insertion(mesh, 4.0).compute();
		new ConstraintNormal3D(mesh).compute();
		new Insertion(mesh).compute();
		new ConstraintNormal3D(mesh).compute();
		mesh.popCompGeom(3);
		
		assert (mesh.isValid());
	}
	
}
