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

import gnu.trove.set.hash.THashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.algos3d.VertexSwapper;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.TriangleHE;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Location;
import org.jcae.mesh.amibe.projection.TriangleKdTree;
import org.jcae.mesh.stitch.TriangleProjector.ProjectionType;
import org.jcae.mesh.xmldata.Amibe2VTK;
import org.jcae.mesh.xmldata.MeshWriter;

/**
 *
 * @author Jerome Robert
 */
class EdgeProjector {
	private interface Pool<E> extends Collection<E>
	{
		E get();
		E pop();
	}

	private static <E> Pool<E> createPool()
	{
		return new DebugPool<E>();
	}

	private static class TPool<E> extends THashSet<E> implements Pool<E>
	{
		public E get()
		{
			int n = _set.length;
			for(int i = 0; i < n; i++)
			{
				if(_set[i] != FREE && _set[i] != REMOVED)
					return (E) _set[i];
			}
			throw new NoSuchElementException();
		}

		public E pop()
		{
			int n = _set.length;
			for(int i = 0; i < n; i++)
			{
				if(_set[i] != FREE && _set[i] != REMOVED)
				{
					E toReturn = (E) _set[i];
					removeAt(i);
					return toReturn;
				}
			}
			throw new NoSuchElementException();
		}
	}

	private static class DebugPool<E> extends LinkedHashSet<E> implements Pool<E>
	{
		public E get()
		{
			return iterator().next();
		}

		public E pop()
		{
			Iterator<E> it = iterator();
			E toReturn = it.next();
			it.remove();
			return toReturn;
		}

		@Override
		public boolean add(E e) {
			assert e != null;
			return super.add(e);
		}
	}
	private final static Logger LOGGER = Logger.getLogger(EdgeProjector.class.getName());
	private final Pool<Triangle> triangles = createPool();
	//projectors for origin vertices
	private final List<TriangleProjector> triangleProjectors1 = new ArrayList<TriangleProjector>();
	//the current projector for origin vertices
	private TriangleProjector triangleProjector1;
	private boolean projector1Valid;

	//projectors for origin vertices
	private final List<TriangleProjector> triangleProjectors2 = new ArrayList<TriangleProjector>();
	//the current projector for origin vertices
	private TriangleProjector triangleProjector2;
	private boolean projector2Valid;

	private Integer[] triangleProjectorOrder;
	/** A projector which will alway be too far to be concidered */
	private TriangleProjector farProjector = new TriangleProjector();
	private final double[] aabb = new double[6];
	private final Mesh mesh;
	private final TriangleKdTree kdTree;
	private final Pool<AbstractHalfEdge> toProject = createPool();
	private final Pool<AbstractHalfEdge> halfInserted = createPool();
	private final int group;
	private boolean ignoreGroup;
	private final Collection<Triangle> splittedTriangle = new ArrayList<Triangle>(3);
	private final List<TriangleHelper> triangleHelpers = new ArrayList<TriangleHelper>();
	private Vertex lastMergeSource;
	private Vertex lastMergeTarget;
	private AbstractHalfEdge lastSplitted1;
	private AbstractHalfEdge lastSplitted2;
	private AbstractHalfEdge edgeToCollapse;
	double weight;
	private final TriangleSplitter triangleSplitter = new TriangleSplitter();
	private final VertexMerger vertexMerger = new VertexMerger();
	private final VertexSwapper vertexSwapper;
	public boolean checkMerge = true, boundaryOnly;
	private final double maxSqrDist, sqrTol;
	public EdgeProjector(Mesh mesh, TriangleKdTree kdTree,
		Collection<AbstractHalfEdge> edges, int group, double maxDist,
		double tol) {
		this.mesh = mesh;
		this.kdTree = kdTree;
		final double tol3 = tol * tol * tol;
		vertexSwapper = new VertexSwapper(mesh)
		{
			@Override
			protected boolean isQualityImproved(AbstractHalfEdge.Quality quality) {
				return quality.getSwappedQuality()> quality.getQuality() &&
					quality.getSwappedAngle() > 0 &&
					quality.swappedVolume() < tol3;
			}
		};
		this.toProject.addAll(edges);
		vertexSwapper.setKdTree(kdTree);
		this.group = group;
		maxSqrDist = maxDist * maxDist;
		sqrTol = tol * tol;
	}

	/**
	 * When true project on all groups but the specified one, instead of
	 * projecting to the specified group
	 */
	public void setIgnoreGroup(boolean ignore)
	{
		ignoreGroup = ignore;
	}

	public void setBoundaryOnly(boolean b)
	{
		boundaryOnly = b;
	}

	public void project() {
		while (true) {
			AbstractHalfEdge tp;
			if (!halfInserted.isEmpty()) {
				tp = halfInserted.pop();
			} else if (!toProject.isEmpty()) {
				tp = toProject.pop();
				if(!tp.hasAttributes(AbstractHalfEdge.BOUNDARY))
					//previous merge has render this edge not boundary so
					//we skip it
					continue;
			} else {
				break;
			}
			projectEdge(tp);
		}
	}

	private Vertex splitTriangle(Triangle t, TriangleProjector tp) {
		Vertex toInsert = mesh.createVertex(tp.getProjection());
		((TriangleHE) t).split(mesh, toInsert, splittedTriangle);
		kdTree.replace(t, splittedTriangle);
		splittedTriangle.clear();
		vertexSwapper.swap(toInsert);
		return toInsert;
	}

	private boolean canMerge(Vertex source, Location target) {
		assert source != null;
		assert target != null;
		if(source == target)
			return false;
		Location realPosition = null;
		if(checkMerge)
			realPosition = new Location(weight * source.getX() + (1 - weight) * target.getX(),
				weight * source.getY() + (1 - weight) * target.getY(),
				weight * source.getZ() + (1 - weight) * target.getZ());
		Iterator<AbstractHalfEdge> it = source.getNeighbourIteratorAbstractHalfEdge();
		edgeToCollapse = null;
		while (it.hasNext()) {
			AbstractHalfEdge e = it.next();
			assert e.origin() == source;
			assert e.origin().isMutable(): e;
			assert e.origin().isManifold(): e;
			if (checkMerge && !mesh.canMoveOrigin(e, realPosition)) {
				LOGGER.info("Cannot move " + source + " to " + realPosition +
					". distance=" + source.distance3D(realPosition));
				return false;
			}
			boolean boundary = e.hasAttributes(AbstractHalfEdge.BOUNDARY) ||
				(e.sym() != null && e.sym().hasAttributes(AbstractHalfEdge.BOUNDARY));
			if(boundaryOnly)
			{
				// check that we do not create non-manifold edges in boundary
				// only mode (manifold stitch).
				Iterator<AbstractHalfEdge> itt = e.destination().getNeighbourIteratorAbstractHalfEdge();
				while(itt.hasNext())
				{
					AbstractHalfEdge ee = itt.next();
					if(ee.destination() == target &&
						(!ee.hasAttributes(AbstractHalfEdge.BOUNDARY) &&
						!ee.sym().hasAttributes(AbstractHalfEdge.BOUNDARY) || !boundary))
						return false;
				}
			}

			if(e.destination() == target)
			{
				if(e.hasAttributes(AbstractHalfEdge.OUTER))
					e = e.sym();
				if(boundary && mesh.canCollapseEdge(e, target))
				{
					// target has alread been projected to the target mesh so we
					// collapse the edge of the source mesh instead of merging vertices.
					// Merging vertices would create degenerated triangle.
					edgeToCollapse = e;
				}
				else
				{
					LOGGER.info(
						"Cannot collapse " + source + " to " + target +
						". boundary=" + e.hasAttributes(AbstractHalfEdge.BOUNDARY) +
						" canCollapse=" + mesh.canCollapseEdge(e, target));
					return false;
				}
			}
		}
		return true;
	}

	/** @return true if the merge was possible */
	private void mergeVertices(Vertex source, Vertex target) {
		saveAsVTK(mesh);
		assert source != target;
		assert !Double.isNaN(source.getX());
		assert !Double.isNaN(target.getX());
		assert !(source.getLink() instanceof Triangle[]);
		assert canMerge(source, target);
		target.moveTo(weight * source.getX() + (1 - weight) * target.getX(),
			weight * source.getY() + (1 - weight) * target.getY(),
			weight * source.getZ() + (1 - weight) * target.getZ());
		if(edgeToCollapse == null)
			vertexMerger.merge(mesh, target, source, target);
		else
		{
			assert (edgeToCollapse.origin() == source && edgeToCollapse.destination() == target) ||
				(edgeToCollapse.origin() == target && edgeToCollapse.destination() == source);
			Triangle t = edgeToCollapse.getTri();
			if(lastSplitted1.getTri() == t)
				lastSplitted1 = null;
			if(lastSplitted2 .getTri() == t)
				lastSplitted2 = null;
			toProject.remove(edgeToCollapse);
			toProject.remove(edgeToCollapse.next());
			toProject.remove(edgeToCollapse.prev());
			mesh.edgeCollapse(edgeToCollapse, target);
			kdTree.remove(t);
			edgeToCollapse = null;
		}
		target.setMutable(false);
		saveAsVTK(mesh);
	}

	private void splitEdge(AbstractHalfEdge toSplit, Vertex v) {
		if (toSplit.hasAttributes(AbstractHalfEdge.BOUNDARY))
		{
			Triangle t = toSplit.getTri();
			kdTree.remove(t);
			mesh.vertexSplit(toSplit, v);
			AbstractHalfEdge newEdge = toSplit.next().sym().next();
			addTriangleToKdTree(toSplit.getTri());
			addTriangleToKdTree(newEdge.getTri());
		}
		else
		{
			Triangle t1 = toSplit.getTri();
			Triangle t2 = toSplit.sym().getTri();
			kdTree.remove(t1);
			kdTree.remove(t2);
			mesh.vertexSplit(toSplit, v);
			AbstractHalfEdge newEdge = toSplit.next().sym().next();
			assert toSplit.destination() == v;
			assert newEdge.origin() == v;
			addTriangleToKdTree(toSplit.getTri());
			addTriangleToKdTree(toSplit.sym().getTri());
			addTriangleToKdTree(newEdge.getTri());
			addTriangleToKdTree(newEdge.sym().getTri());
		}
		vertexSwapper.swap(v);
	}

	private void addTriangleToKdTree(Triangle triangle) {
		if (!triangle.hasAttributes(AbstractHalfEdge.OUTER)) {
			kdTree.addTriangle(triangle);
		}
	}

	private Vertex splitEdge(TriangleProjector tp) {
		Vertex v = mesh.createVertex(tp.getProjection());
		AbstractHalfEdge toSplit = tp.getEdge();
		if (toSplit.hasAttributes(AbstractHalfEdge.NONMANIFOLD)) {
			LOGGER.info(
				"I will not split a non-manifold edge as there should not be "+
				"any non-manifold edges here: "+toSplit);
			return null;
		} else {
			splitEdge(toSplit, v);
		}
		return v;
	}

	/**
	 * Split 2 edges
	 * @param edge1 an edge to be splitted with a copy of vertexToInsert
	 * @param edge2 an edge to be splitted with vertexToInsert
	 * @param vertexToInsert the point to insert on edge2
	 * @return The vertex splitting edge1
	 */
	private void split2Edges(Vertex v1, AbstractHalfEdge edge1, Vertex v2,
		AbstractHalfEdge edge2) {
		assert v1 != null;
		assert v1 != edge1.origin();
		assert v1 != edge1.destination();
		assert TriangleHelper.isOnEdge(v1, edge1.origin(), edge1.destination(),
			triangleProjector1.sqrMaxDistance);
		// I duplicate the vertex because I'm almost sure that
		// vertexSplit doesn't support inserted vertex which are
		// already in the mesh
		check(mesh);
		splitEdge(edge1, v1);
		check(mesh);
		if (edge2 != null) {
			assert v2 != edge2.origin();
			assert v2 != edge2.destination();
			assert TriangleHelper.isOnEdge(v2, edge2.origin(),
				edge2.destination(), triangleProjector1.sqrTolerance);
			splitEdge(edge2, v2);
			check(mesh);
		}
	}

	public static boolean checkMesh = false;
	private static void check(Mesh mesh) {
		if(checkMesh)
		{
			assert mesh.isValid();
			assert mesh.checkNoDegeneratedTriangles();
			assert mesh.checkNoInvertedTriangles();
		}
	}

	/**
	 * Find the border edge adjacent to the given edge and sharing its
	 * destination vertex
	 */
	private AbstractHalfEdge getNextBorderEdge(AbstractHalfEdge edge) {
		assert edge.hasAttributes(AbstractHalfEdge.BOUNDARY): "boundary edge expected: "+edge;
		AbstractHalfEdge toReturn = edge.next();
		int gid = edge.getTri().getGroupId();
		while (toReturn.destination() != mesh.outerVertex &&
			!(toReturn.hasAttributes(AbstractHalfEdge.BOUNDARY) &&
			toReturn.getTri().getGroupId() == gid)) {
			toReturn = toReturn.sym().next();
		}
		assert toReturn.origin() == edge.destination();
		return toReturn.destination() == mesh.outerVertex ? null : toReturn;
	}

	private AbstractHalfEdge getPreviousBorderEdge(AbstractHalfEdge edge) {
		assert edge.hasAttributes(AbstractHalfEdge.BOUNDARY): "boundary edge expected: "+edge;
		AbstractHalfEdge toReturn = edge.next().next();
		int gid = edge.getTri().getGroupId();
		while (toReturn.origin() != mesh.outerVertex &&
			!(toReturn.hasAttributes(AbstractHalfEdge.BOUNDARY) &&
			toReturn.getTri().getGroupId() == gid)) {
			toReturn = toReturn.sym().next().next();
		}

		assert toReturn.destination() == edge.origin();
		return toReturn.origin() == mesh.outerVertex ? null : toReturn;
	}

	private void handleOutOutCase(AbstractHalfEdge edge) {
		lastMergeSource = null;
		lastMergeTarget = null;
		if (triangleProjector1.getType() == ProjectionType.OUT && triangleProjector2.getType() == ProjectionType.OUT) {
			//split and edge of the triangle and split the projected
			//edge. The edge may cut 2 edges of the triangle but we will
			//insert only one
			Location p2 = triangleProjector2.getProjection();
			Location p1 = triangleProjector1.getProjection();
			triangleSplitter.split(p1, p2, triangleProjector1.sqrTolerance);
			lastMergeTarget = triangleSplitter.getSplitVertex();
			if (triangleSplitter.getSplittedEdge() != null && lastMergeTarget == null) {
				lastMergeTarget = mesh.createVertex(triangleSplitter.getSplitPoint());
				assert TriangleHelper.isOnEdge(lastMergeTarget, p1, p2,
					triangleProjector1.sqrTolerance);
			}
			if (lastMergeTarget != null) {
				lastMergeSource = mesh.createVertex(0, 0, 0);
				double l = TriangleHelper.reverseProject(p2, edge.destination(),
					edge.origin(), lastMergeTarget, lastMergeSource,
					triangleProjector1.sqrTolerance);
				if (l > triangleProjector1.sqrMaxDistance) {
					lastMergeTarget = null;
				}
			}
			if (lastMergeTarget != null) {
				assert lastMergeSource != null : lastMergeTarget;
				split2Edges(lastMergeSource, edge, lastMergeTarget,
					triangleSplitter.getSplittedEdge());
				lastSplitted1 = edge;
				lastSplitted2 = edge.next().sym().next();
				assert TriangleHelper.isOnEdge(lastMergeSource, edge.origin(),
					edge.destination(), triangleProjector1.sqrTolerance);
			}
			if (lastMergeSource != null && lastMergeTarget != null && !canMerge(lastMergeSource,
				lastMergeTarget)) {
				lastMergeTarget = null;
			}
		}
	}

	/**
	 * Select the good algorithm, apply it and update lastInserted,
	 * lastSplitted1 and lastSplitted2.
	 */
	private void dispatchCases(boolean origin, boolean destination,
		AbstractHalfEdge edge, Triangle t) {
		lastMergeSource = null;
		lastMergeTarget = null;
		lastSplitted1 = null;
		lastSplitted2 = null;
		if (triangleProjector1.getType() == ProjectionType.OUT && !destination) {
			//Split and edge of the triangle and split the projected edge
			triangleSplitter.splitApex(edge.destination(),
				triangleProjector1.getProjection(),
				triangleProjector1.sqrTolerance);
			lastMergeTarget = triangleSplitter.getSplitVertex();
			if (lastMergeTarget == null && triangleSplitter.getSplittedEdge() != null) {
				lastMergeTarget = mesh.createVertex(triangleSplitter.getSplitPoint());
			}
			if (lastMergeTarget != null) {
				lastMergeSource = mesh.createVertex(0, 0, 0);
				double l = TriangleHelper.reverseProject(triangleProjector1.getProjection(),
					edge.origin(), edge.destination(), lastMergeTarget,
					lastMergeSource, triangleProjector1.sqrTolerance);
				if (l > triangleProjector1.sqrMaxDistance) {
					lastMergeTarget = null;
				}
			}
			if (lastMergeTarget != null && canMerge(lastMergeSource,
				lastMergeTarget)) {
				split2Edges(lastMergeSource, edge, lastMergeTarget,
					triangleSplitter.getSplittedEdge());
				lastSplitted1 = edge;
				assert TriangleHelper.isOnEdge(lastMergeSource, edge.origin(),
					edge.destination(), triangleProjector1.sqrTolerance);
			} else {
				lastMergeTarget = null;
			}
		} else if (triangleProjector2.getType() == ProjectionType.OUT && !origin) {
			//Split and edge of the triangle and split the projected edge
			triangleSplitter.splitApex(edge.origin(),
				triangleProjector2.getProjection(),
				triangleProjector2.sqrTolerance);
			lastMergeTarget = triangleSplitter.getSplitVertex();
			if (lastMergeTarget == null && triangleSplitter.getSplittedEdge() != null) {
				lastMergeTarget = mesh.createVertex(triangleSplitter.getSplitPoint());
			}
			if (lastMergeTarget != null) {
				lastMergeSource = mesh.createVertex(0, 0, 0);
				double l = TriangleHelper.reverseProject(triangleProjector2.getProjection(),
					edge.destination(), edge.origin(), lastMergeTarget,
					lastMergeSource, triangleProjector2.sqrTolerance);
				if (l > triangleProjector2.sqrMaxDistance) {
					lastMergeTarget = null;
				}
			}
			if (lastMergeTarget != null && canMerge(lastMergeSource,
				lastMergeTarget)) {
				split2Edges(lastMergeSource, edge, lastMergeTarget,
					triangleSplitter.getSplittedEdge());
				lastSplitted2 = getNextBorderEdge(edge);
				assert TriangleHelper.isOnEdge(lastMergeSource, edge.origin(),
					edge.destination(), triangleProjector2.sqrTolerance);
			} else {
				lastMergeTarget = null;
			}
		} else if (origin && triangleProjector1.getType() == ProjectionType.VERTEX) {
			lastMergeSource = edge.origin();
			lastMergeTarget = triangleProjector1.getVertex();
			lastSplitted1 = edge;
			lastSplitted2 = getPreviousBorderEdge(edge);
			assert TriangleHelper.isOnEdge(lastMergeSource, edge.origin(),
				edge.destination(), triangleProjector1.sqrTolerance);
		} else if (destination && triangleProjector2.getType() == ProjectionType.VERTEX) {
			lastMergeSource = edge.destination();
			lastMergeTarget = triangleProjector2.getVertex();
			lastSplitted1 = edge;
			lastSplitted2 = getNextBorderEdge(edge);
			assert TriangleHelper.isOnEdge(lastMergeSource, edge.origin(),
				edge.destination(), triangleProjector2.sqrTolerance);
		} else if (origin && triangleProjector1.getType() == ProjectionType.FACE) {
			lastMergeSource = edge.origin();
			if (canMerge(lastMergeSource, triangleProjector1.getProjection())) {
				lastMergeTarget = splitTriangle(t, triangleProjector1);
				lastSplitted1 = edge;
				lastSplitted2 = getPreviousBorderEdge(edge);
				assert TriangleHelper.isOnEdge(lastMergeSource, edge.origin(),
					edge.destination(), triangleProjector1.sqrTolerance);
			}
		} else if (destination && triangleProjector2.getType() == ProjectionType.FACE) {
			lastMergeSource = edge.destination();
			if (canMerge(lastMergeSource, triangleProjector2.getProjection())) {
				lastMergeTarget = splitTriangle(t, triangleProjector2);
				lastSplitted1 = edge;
				lastSplitted2 = getNextBorderEdge(edge);
				assert TriangleHelper.isOnEdge(lastMergeSource, edge.origin(),
					edge.destination(), triangleProjector2.sqrTolerance);
			}
		} else if (origin && triangleProjector1.getType() == ProjectionType.EDGE) {
			lastMergeSource = edge.origin();
			if (canMerge(lastMergeSource, triangleProjector1.getProjection())) {
				lastMergeTarget = splitEdge(triangleProjector1);
				lastSplitted1 = edge;
				lastSplitted2 = getPreviousBorderEdge(edge);
				assert TriangleHelper.isOnEdge(lastMergeSource, edge.origin(),
					edge.destination(), triangleProjector1.sqrTolerance);
			}
		} else if (destination && triangleProjector2.getType() == ProjectionType.EDGE) {
			lastMergeSource = edge.destination();
			if (canMerge(lastMergeSource, triangleProjector2.getProjection())) {
				lastMergeTarget = splitEdge(triangleProjector2);
				lastSplitted1 = edge;
				lastSplitted2 = getNextBorderEdge(edge);
				assert TriangleHelper.isOnEdge(lastMergeSource, edge.origin(),
					edge.destination(), triangleProjector2.sqrTolerance);
			}
		}
		if (lastMergeSource != null && lastMergeTarget != null && !canMerge(lastMergeSource,
			lastMergeTarget)) {
			lastMergeTarget = null;
		}
	}

	private boolean mergeAndFinish() {
		if (lastMergeTarget != null) {
			if(lastMergeSource == lastMergeTarget)
			{
				LOGGER.info("Something strange append around "+lastMergeSource);
				return false;
			}
			assert lastMergeSource.sqrDistance3D(lastMergeTarget) < triangleProjector1.sqrMaxDistance * 2 : lastMergeSource.sqrDistance3D(lastMergeTarget);
			check(mesh);
			mergeVertices(lastMergeSource, lastMergeTarget);
			check(mesh);
			if (lastSplitted1 != null &&
				lastSplitted1.hasAttributes(AbstractHalfEdge.BOUNDARY) &&
				isToProject(lastSplitted1))
			{
				halfInserted.add(lastSplitted1);
			}

			if (lastSplitted2 != null &&
				lastSplitted2.hasAttributes(AbstractHalfEdge.BOUNDARY) &&
				isToProject(lastSplitted2))
			{
				halfInserted.add(lastSplitted2);
			}
			return true;
		}
		return false;
	}

	protected boolean isToProject(AbstractHalfEdge edge)
	{
		return true;
	}
	/**
	 * Init the triangles field with triangles where the edge may be
	 * projected
	 */
	private void findCandidateTriangles(AbstractHalfEdge edge) {
		boolean origin = edge.origin().isMutable();
		boolean destination = edge.destination().isMutable();
		//if neither origin nor destination both extremities of the edge
		//are already projected but the full edge may not be fully projected
		//so we will try only the out/out case.
		triangles.clear();
		boolean notProjected = !origin && !destination &&
			edge.hasAttributes(AbstractHalfEdge.BOUNDARY);
		if ((origin && destination) || notProjected) {
			compteEdgeAABB(edge, aabb);
			kdTree.getNearTriangles(aabb, triangles, group, ignoreGroup);
		} else {
			Vertex vv = origin ? edge.destination() : edge.origin();
			Iterator<Triangle> it = vv.getNeighbourIteratorTriangle();
			while (it.hasNext()) {
				Triangle t = it.next();
				if (!t.hasAttributes(AbstractHalfEdge.OUTER) &&
					((!ignoreGroup && group >= 0 && t.getGroupId() == group) ||
					(ignoreGroup && group >= 0 && t.getGroupId() != group)))
				{
					triangles.add(t);
				}
			}
		}
	}

	/**
	 * Fill triangleProjectors1 and triangleProjectors2, then order them in the
	 * triangleProjectorOrder array.
	 * triangleProjectorOrder can be used to iterator over triangleProjectors
	 * starting with the closest triangle.
	 * Projecting to the closest triangle instead of a random close triangle is
	 * more robust.
	 */
	private void projectToClosest(AbstractHalfEdge edge)
	{
		boolean origin = edge.origin().isMutable();
		boolean destination = edge.destination().isMutable();
		findCandidateTriangles(edge);
		while(triangleHelpers.size() < triangles.size())
			triangleHelpers.add(new TriangleHelper());
		int k = 0;
		for(Triangle t: triangles)
		{
			assert mesh.getTriangles().contains(t);
			triangleHelpers.get(k++).setTriangle(t);
		}
		projector1Valid = origin || (!origin && !destination);
		projector2Valid = destination || (!origin && !destination);
		if (projector1Valid)
			projectToClosest(edge.origin(), triangleProjectors1);
		if (projector2Valid)
			projectToClosest(edge.destination(), triangleProjectors2);
		if(triangleProjectorOrder == null || triangleProjectorOrder.length < triangles.size())
			triangleProjectorOrder = new Integer[triangles.size()];
		for(int i = 0; i < triangleProjectorOrder.length; i++)
			triangleProjectorOrder[i] = i;
		Arrays.sort(triangleProjectorOrder, 0, triangles.size(), projectorComparator);
	}

	private final Comparator<Integer> projectorComparator
		= new Comparator<Integer>(){

		public int compare(Integer o1, Integer o2) {
			double v1, v2;
			if(projector1Valid && projector2Valid)
			{
				v1 = Math.min(triangleProjectors1.get(o1).getSqrDistance(),
					triangleProjectors2.get(o1).getSqrDistance());
				v2 = Math.min(triangleProjectors1.get(o2).getSqrDistance(),
					triangleProjectors2.get(o2).getSqrDistance());
			}
			else if(projector1Valid)
			{
				v1 = triangleProjectors1.get(o1).getSqrDistance();
				v2 = triangleProjectors1.get(o2).getSqrDistance();
			}
			else //if(projector2Valid)
			{
				assert projector2Valid;
				v1 = triangleProjectors2.get(o1).getSqrDistance();
				v2 = triangleProjectors2.get(o2).getSqrDistance();
			}
			return Double.compare(v1, v2);
		}
	};

	/**
	 * Project location to the closest triangle available in the triangleHelpers list.
	 * triangleHelpers must contains TriangleHelper initialised with the triangle
	 * of the triangles list.
	 */
	private void projectToClosest(Location location, List<TriangleProjector> projectors)
	{
		while(projectors.size() < triangles.size())
		{
			TriangleProjector tp = new TriangleProjector();
			tp.sqrMaxDistance = maxSqrDist;
			tp.sqrTolerance = sqrTol;
			tp.boundaryOnly = boundaryOnly;
			projectors.add(tp);
		}
		int n = triangles.size();
		for(int k = 0; k < n; k++)
		{
			TriangleProjector tp = projectors.get(k);
			tp.reset();
			tp.project(location, triangleHelpers.get(k));
		}
	}
	/**
	 * Project the edge on the given group of the mesh
	 * @param edge the edge to project
	 * @param halfProjected filled with the list of half projected edges. A
	 * half projected edges is an edge whose only one vertex is projected or
	 * both vertex a projected but on different triangles.
	 */
	private void projectEdge(AbstractHalfEdge edge) {
		boolean origin = edge.origin().isMutable();
		boolean destination = edge.destination().isMutable();
		projectToClosest(edge);
		//if neither origin nor destination both extremities of the edge
		//are already projected but the full edge may not be fully projected
		//so we will try only the out/out case.
		int n = triangles.size();
		for (int i = 0; i < n; i++) {
			int index = triangleProjectorOrder[i];
			triangleProjector1 = farProjector;
			triangleProjector2 = farProjector;
			if(projector1Valid)
				triangleProjector1 = triangleProjectors1.get(index);
			if(projector2Valid)
				triangleProjector2 = triangleProjectors2.get(index);
			TriangleHelper th = triangleHelpers.get(index);
			triangleSplitter.setTriangle(th);
			check(mesh);
			dispatchCases(origin, destination, edge, th.getTriangle());
			check(mesh);
			if (lastMergeTarget != null) {
				assert TriangleHelper.isOnEdge(lastMergeSource, edge.origin(),
					edge.destination(), triangleProjector1.sqrTolerance) : origin + " " + triangleProjector1 + "\n" + destination + " " + triangleProjector2;
			}
			if (mergeAndFinish()) {
				return;
			}
		}
		// OUT/OUT case have a lower priority over other cases so we process
		// it in a separate loop after others.
		if (origin && destination) {
			for (int i = 0; i < n; i++) {
				int index = triangleProjectorOrder[i];
				triangleProjector1 = triangleProjectors1.get(index);
				triangleProjector2 = triangleProjectors2.get(index);
				triangleSplitter.setTriangle(triangleHelpers.get(index));
				handleOutOutCase(edge);
				if (mergeAndFinish()) {
					return;
				}
			}
		}
	}

	private static void compteEdgeAABB(AbstractHalfEdge edge, double[] aabb)
	{
		aabb[0] = Math.min(edge.origin().getX(), edge.destination().getX());
		aabb[1] = Math.min(edge.origin().getY(), edge.destination().getY());
		aabb[2] = Math.min(edge.origin().getZ(), edge.destination().getZ());
		aabb[3] = Math.max(edge.origin().getX(), edge.destination().getX());
		aabb[4] = Math.max(edge.origin().getY(), edge.destination().getY());
		aabb[5] = Math.max(edge.origin().getZ(), edge.destination().getZ());
	}

	private static int saveVTKCounter = 0;
	static boolean saveVTK = false;
	public static void saveAsVTK(Mesh mesh)
	{
		if(saveVTK)
		{
			try {
				String dir = "/tmp/tmp.amibe";
				MeshWriter.writeObject3D(mesh, dir, null);
				String name = "/tmp/non-manifold-stitch"+(saveVTKCounter++)+".vtp";
				System.err.println("saving to "+name);
				new Amibe2VTK(dir).write(name);
			} catch (Exception ex) {
				Logger.getLogger(NonManifoldStitch.class.getName()).log(Level.SEVERE,
					null, ex);
			}
		}
	}
}
