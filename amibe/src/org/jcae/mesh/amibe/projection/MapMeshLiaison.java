/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2008-2011, by EADS France

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

package org.jcae.mesh.amibe.projection;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.iterator.TIntObjectIterator;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.metrics.Location;

public class MapMeshLiaison extends MeshLiaison
{
	private final static Logger LOGGER = Logger.getLogger(MeshLiaison.class.getName());

	// Map between vertices of currentMesh and their projection on backgroundMesh
	// Each group has its own map
	private TIntObjectHashMap<Map<Vertex, ProjectedLocation>> mapCurrentVertexProjection;
	private final ProjectedLocation savedProjectedLocation = new ProjectedLocation();
	// Map to keep track of a near point in background mesh, used as a starting point of locators
	private TIntObjectHashMap<Map<Vertex, Vertex>> neighborBgMap;

	public MapMeshLiaison(Mesh backgroundMesh)
	{
		this(backgroundMesh, backgroundMesh.getBuilder());
	}

	public MapMeshLiaison(Mesh backgroundMesh, MeshTraitsBuilder mtb)
	{
		super(backgroundMesh, mtb);
	}

	public void initBgMap(TIntIntHashMap numberOfTriangles, Collection<Vertex> nodeset)
	{
		neighborBgMap = new TIntObjectHashMap<Map<Vertex, Vertex>>(numberOfTriangles.size());
		neighborBgMap.put(-1, new HashMap<Vertex, Vertex>(nodeset.size()));
		for (TIntIntIterator it = numberOfTriangles.iterator(); it.hasNext(); )
		{
			it.advance();
			neighborBgMap.put(it.key(), new HashMap<Vertex, Vertex>(it.value() / 2));
		}
		for (Vertex v : nodeset)
		{
			if (null == v.getLink())
				continue;
			Triangle t = getBackgroundTriangle(v);
			assert !t.hasAttributes(AbstractHalfEdge.OUTER);
			addVertexInNeighborBgMap(v, t);
		}
	}

	public void clearBgMap()
	{
		neighborBgMap = null;
	}

	public void addVertexInNeighborBgMap(Vertex v, Triangle bgT)
	{
		double d0 = v.sqrDistance3D(bgT.vertex[0]);
		double d1 = v.sqrDistance3D(bgT.vertex[1]);
		double d2 = v.sqrDistance3D(bgT.vertex[2]);
		Vertex bgNearestVertex;
		if (d0 <= d1 && d0 <= d2)
		{
			bgNearestVertex = bgT.vertex[0];
		}
		else if (d1 <= d0 && d1 <= d2)
		{
			bgNearestVertex = bgT.vertex[1];
		}
		else
		{
			bgNearestVertex = bgT.vertex[2];
		}
		neighborBgMap.get(-1).put(v, bgNearestVertex);
		if (v.isManifold())
		{
			neighborBgMap.get(bgT.getGroupId()).put(v, bgNearestVertex);
		}
		else
		{
			for (Iterator<Triangle> itT = v.getNeighbourIteratorTriangle(); itT.hasNext(); )
			{
				int groupId = itT.next().getGroupId();
				if (groupId >= 0)
					neighborBgMap.get(groupId).put(v, bgNearestVertex);
			}
		}
	}

	protected void init(Collection<Vertex> backgroundNodeset)
	{
		TIntIntHashMap numberOfTriangles = new TIntIntHashMap();
		for (Triangle t : this.backgroundMesh.getTriangles())
		{
			if (t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			numberOfTriangles.putIfAbsent(t.getGroupId(), 0);
			numberOfTriangles.increment(t.getGroupId());
		}
		// Compute projections of vertices from currentMesh
		this.mapCurrentVertexProjection = new TIntObjectHashMap<Map<Vertex, ProjectedLocation>>(numberOfTriangles.size());
		this.mapCurrentVertexProjection.put(-1, new HashMap<Vertex, ProjectedLocation>(backgroundNodeset.size()));
		for (TIntIntIterator it = numberOfTriangles.iterator(); it.hasNext(); )
		{
			it.advance();
			this.mapCurrentVertexProjection.put(it.key(), new HashMap<Vertex, ProjectedLocation>(it.value() / 2));
		}
	}

	public final void backupRestore(Vertex v, boolean restore, int group)
	{
		ProjectedLocation location = mapCurrentVertexProjection.get(group).get(v);
		if (!location.isCached)
			throw new IllegalStateException();
		if (restore)
			location.copy(savedProjectedLocation);
		else
			currentMesh.getTrace().moveVertex(v);
		location.isCached = false;
	}

	protected boolean move(Vertex v, Location target, boolean backup,  int group, boolean doCheck)
	{
		if (LOGGER.isLoggable(Level.FINER))
			LOGGER.log(Level.FINER, "Trying to move vertex "+v+" to ("+target+") in group "+group);
		// Old projection
		ProjectedLocation location = mapCurrentVertexProjection.get(group).get(v);
		if (backup)
		{
			if (location.isCached)
				throw new IllegalStateException();
			savedProjectedLocation.copy(location);
			location.isCached = true;
		}
		if (LOGGER.isLoggable(Level.FINEST))
			LOGGER.log(Level.FINEST, "Old projection: "+location);
		LocationFinder lf = new LocationFinder(target, group);
		AbstractHalfEdge ot = location.t.getAbstractHalfEdge();
		if (ot.apex() == location.t.vertex[location.vIndex])
			ot = ot.prev();
		else if (ot.destination() == location.t.vertex[location.vIndex])
			ot = ot.next();
		lf.walkAroundOrigin(ot);
		if (null == lf.current)
			return false;
		lf.walkFlipFlop(ot);
		// Now lf contains the new location.
		// Update location
		location.updateTriangle(lf.current);
		location.updateVertexIndex(target);

		if (LOGGER.isLoggable(Level.FINEST))
			LOGGER.log(Level.FINEST, "New projection: "+location);
		Location newPosition = new Location();
		location.projectOnTriangle(target, newPosition);
		// There are 2 distinct cases:
		//   1. The projected vertex should be on the background mesh, and
		//      we try hard to project vertex onto the background mesh
		//   2. We want to compute the best projected vertex, but it may
		//      be at a small distance from the background mesh
		// The first case is important when smoothing, in which case
		// backup parameter is true.  We use it to distinguish between
		// both use cases, but adding a new parameter wpuld be better.
		if (doCheck)
		{
			if (!location.computeBarycentricCoordinates(newPosition))
			{
				int[] index = new int[2];
				double maxError = sqrDistanceVertexTriangle(target, lf.current, index);
				AbstractHalfEdge newEdge = ot;
				do
				{
					ot = newEdge;
					newEdge = findBetterTriangleInNeighborhood(target, ot, maxError, group);
					maxError *= 0.5;
				} while (newEdge != null);
				if (ot != null && backup)
				{
					location.updateTriangle(ot.getTri());
					location.updateVertexIndex(target);
				}
			}
			if (!location.computeBarycentricCoordinates(newPosition))
			{
				// FIXME: this should not happen. Try all triangles to find the best projection
				LOGGER.log(Level.CONFIG, "Position found outside triangle: " + newPosition + "; checking all triangles, this may be slow");
				lf.walkDebug(backgroundMesh, group);
				if(backup)
				{
					location.updateTriangle(lf.current);
					location.updateVertexIndex(target);
				}
			}
			if (!location.computeBarycentricCoordinates(newPosition))
			{
/* FIXME: this should not happen. For now, do not move in such a case
				double [] p0 = location.t.vertex[0].getUV();
				double [] p1 = location.t.vertex[1].getUV();
				double [] p2 = location.t.vertex[2].getUV();
				for (int i = 0; i < 3; i++)
				{
					if (location.b[i] < 0.0)
						location.b[i] = 0.0;
				}
				// Values have been truncated
				double invSum = 1.0 / (location.b[0] + location.b[1] + location.b[2]);
				for (int i = 0; i < 3; i++)
					location.b[i] *= invSum;
				LOGGER.log(Level.WARNING, "Position found outside triangle: "+newPosition[0]+" "+newPosition[1]+" "+newPosition[2]);
				// Move vertex on triangle boundary
				newPosition[0] = location.b[0]*p0[0] + location.b[1]*p1[0] + location.b[2]*p2[0];
				newPosition[1] = location.b[0]*p0[1] + location.b[1]*p1[1] + location.b[2]*p2[1];
				newPosition[2] = location.b[0]*p0[2] + location.b[1]*p1[2] + location.b[2]*p2[2];
				 */
				return false;
			}
		}
		v.moveTo(newPosition);
		if (!backup)
			currentMesh.getTrace().moveVertex(v);

		if (LOGGER.isLoggable(Level.FINER))
			LOGGER.log(Level.FINER, "Final position: "+v);
		return true;
	}

	public final Triangle getBackgroundTriangle(Vertex v)
	{
		ProjectedLocation location = mapCurrentVertexProjection.get(-1).get(v);
		assert location != null : "Vertex "+v+" not found";
		return location.t;
	}

	public final double[] getBackgroundNormal(Vertex v)
	{
		ProjectedLocation location = mapCurrentVertexProjection.get(-1).get(v);
		assert location != null : "Vertex "+v+" not found";
		return location.normal;
	}
	@Override
	public Triangle getBackgroundTriangle(Vertex v, double[] normal) {
		System.arraycopy(getBackgroundNormal(v), 0, normal, 0, 3);
		return getBackgroundTriangle(v);
	}

	@Override
	public Triangle getBackgroundTriangle(Vertex v, Vertex start,
		double maxError, int group) {
		Map<Vertex, Vertex> mapBgGroupVertices = neighborBgMap.get(group);
		Vertex bgNear = mapBgGroupVertices.get(start);
		return findSurroundingTriangle(v, bgNear, maxError, true, group).getTri();
	}
	/**
	 * Add a Vertex.
	 *
	 * @param v vertex in current mesh
	 * @param bgT triangle in the background mesh
	 */
	public final void addVertex(Vertex v, Triangle bgT)
	{
		mapCurrentVertexProjection.get(-1).put(v, new ProjectedLocation(v, bgT));
		if (bgT.getGroupId() != -1)
			mapCurrentVertexProjection.get(bgT.getGroupId()).put(v, new ProjectedLocation(v, bgT));
	}

	@Override
	public void replaceVertex(Vertex oldV, Vertex newV) {
		addVertex(newV, popVertex(oldV));
	}

	@Override
	public void removeVertex(Vertex v) {
		popVertex(v);
	}

	/**
	 * Remove a Vertex.
	 *
	 * @param v vertex in current mesh
	 * @return  triangle in the background mesh
	 */
	public final Triangle popVertex(Vertex v)
	{
		for (TIntObjectIterator<Map<Vertex, ProjectedLocation>> it = mapCurrentVertexProjection.iterator(); it.hasNext(); )
		{
			it.advance();
			if (it.key() != -1)
				it.value().remove(v);
		}
		return mapCurrentVertexProjection.get(-1).remove(v).t;
	}

	public final void updateAll()
	{
		LOGGER.config("Update projections");
		for (TIntObjectIterator<Map<Vertex, ProjectedLocation>> it = mapCurrentVertexProjection.iterator(); it.hasNext(); )
		{
			it.advance();
			if (it.key() != -1)
			{
				for (Vertex v : it.value().keySet())
					move(v, v, it.key(), false);
			}
		}
		LOGGER.config("Finish updating projections");
	}

	private class ProjectedLocation
	{
		// triangle where vertex is projected into
		private Triangle t;
		// inverse of triangle area
		private double invArea;
		// normal to triangle plane
		private final double [] normal = new double[3];
		// local index of origin
		private int vIndex = -1;
		// barycentric coordinates
		private final double [] b = new double[3];
		private boolean isCached;
		ProjectedLocation()
		{
		}

		/**
		 *
		 * @param xyz coordinates
		 * @param t triangle in background mesh
		 */
		public ProjectedLocation(Location xyz, Triangle t)
		{
			updateTriangle(t);
			computeBarycentricCoordinates(xyz);
			updateVertexIndex(xyz);
		}

		void copy(ProjectedLocation that)
		{
			t = that.t;
			invArea = that.invArea;
			vIndex = that.vIndex;
			System.arraycopy(that.normal, 0, normal, 0, 3);
			System.arraycopy(that.b, 0, b, 0, 3);
		}

		private boolean updateTriangle(Triangle newT)
		{
			if (newT == t)
				return false;

			t = newT;
			invArea = 1.0 / Matrix3D.computeNormal3D(
				t.vertex[0], t.vertex[1], t.vertex[2], work1, work2, normal);
			return true;
		}

		private boolean updateVertexIndex(Location xyz)
		{
			int oldIndex = vIndex;
			double d0 = t.vertex[0].sqrDistance3D(xyz);
			double d1 = t.vertex[1].sqrDistance3D(xyz);
			double d2 = t.vertex[2].sqrDistance3D(xyz);
			if (d0 <= d1 && d0 <= d2)
				vIndex = 0;
			else if (d1 <= d0 && d1 <= d2)
				vIndex = 1;
			else
				vIndex = 2;

			return vIndex != oldIndex;
		}

		private boolean computeBarycentricCoordinates(Location coord)
		{
			b[0] = Matrix3D.computeNormal3D(coord, t.vertex[1], t.vertex[2],
				work1, work2, work3) * invArea;
			b[0] *= (work3[0]*normal[0] + work3[1]*normal[1] + work3[2]*normal[2]);
			b[1] = Matrix3D.computeNormal3D(t.vertex[0], coord, t.vertex[2],
				work1, work2, work3) * invArea;
			b[1] *= (work3[0]*normal[0] + work3[1]*normal[1] + work3[2]*normal[2]);
			b[2] = Matrix3D.computeNormal3D(t.vertex[0], t.vertex[1], coord,
				work1, work2, work3) * invArea;
			b[2] *= (work3[0]*normal[0] + work3[1]*normal[1] + work3[2]*normal[2]);
			return b[0] >= 0.0 && b[1] >= 0.0 && b[2] >= 0.0;
		}

		private void projectOnTriangle(Location xyz, Location proj)
		{
			Vertex o = t.vertex[vIndex];
			double dist =
				(xyz.getX() - o.getX()) * normal[0] +
				(xyz.getY() - o.getY()) * normal[1] +
				(xyz.getZ() - o.getZ()) * normal[2];
			proj.moveTo(
				xyz.getX() - dist * normal[0],
				xyz.getY() - dist * normal[1],
				xyz.getZ() - dist * normal[2]);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("Vertex index: ");
			sb.append(vIndex);
			sb.append("\n");
			sb.append(t);
			sb.append("\nLocal coordinates: ")
				.append(b[0]).append(" ")
				.append(b[1]).append(" ")
				.append(b[2]);
			return sb.toString();
		}

	}

}
