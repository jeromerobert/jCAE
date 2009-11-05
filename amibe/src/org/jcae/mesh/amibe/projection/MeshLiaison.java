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

package org.jcae.mesh.amibe.projection;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MeshLiaison
{
	private final static Logger LOGGER = Logger.getLogger(MeshLiaison.class.getName());
	
	private final Mesh backgroundMesh;
	private final Mesh currentMesh;
	// Local surface definition on background mesh
	private final Map<Vertex, LocalSurfaceProjection> localSurface;
	// Map between vertices of currentMesh and their projection on backgroundMesh
	private final Map<Vertex, ProjectedLocation> mapCurrentVertexProjection;
	
	private final static double [] work1 = new double[3];
	private final static double [] work2 = new double[3];
	private final static double [] work3 = new double[3];
	
	public MeshLiaison(Mesh backgroundMesh)
	{
		this(backgroundMesh, MeshTraitsBuilder.getDefault3D());
	}

	public MeshLiaison(Mesh backgroundMesh, MeshTraitsBuilder mtb)
	{
		this.backgroundMesh = backgroundMesh;
		
		// Adjacency relations are needed on backgroundMesh
		if (!this.backgroundMesh.hasAdjacency())
			throw new IllegalArgumentException();

		Collection<Vertex> backgroundNodeset;
		if (this.backgroundMesh.hasNodes())
		{
			backgroundNodeset= this.backgroundMesh.getNodes();
			int label = 0;
			for (Vertex v : backgroundNodeset)
			{
				label++;
				v.setLabel(label);
			}
		}
		else
		{
			backgroundNodeset = new LinkedHashSet<Vertex>(this.backgroundMesh.getTriangles().size() / 2);
			for (Triangle f: this.backgroundMesh.getTriangles())
			{
				if (f.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
				for (Vertex v: f.vertex)
					backgroundNodeset.add(v);
			}
		}
		
		this.currentMesh = new Mesh(mtb);
		// Create vertices of currentMesh
		Map<Vertex, Vertex> mapBgToCurrent = new HashMap<Vertex, Vertex>(backgroundNodeset.size()+1);
		for (Vertex v : backgroundNodeset)
		{
			Vertex currentV = this.currentMesh.createVertex(v.getUV());
			currentV.setRef(v.getRef());
			currentV.setLabel(v.getLabel());
			mapBgToCurrent.put(v, currentV);
			if (this.currentMesh.hasNodes())
				this.currentMesh.add(currentV);
		}
		mapBgToCurrent.put(this.backgroundMesh.outerVertex, this.currentMesh.outerVertex);

		// Create triangles of currentMesh
		for (Triangle t : this.backgroundMesh.getTriangles())
		{
			if (t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			this.currentMesh.add(this.currentMesh.createTriangle(
				mapBgToCurrent.get(t.vertex[0]),
				mapBgToCurrent.get(t.vertex[1]),
				mapBgToCurrent.get(t.vertex[2])));
		}
		this.currentMesh.buildAdjacency();

		// Compute discrete surface on backgroundMesh.
		// localSurface is computed on every point, canProject()
		// is used during processing to check that surface
		// approximation can really be performed.
		this.localSurface = new HashMap<Vertex, LocalSurfaceProjection>(backgroundNodeset.size());
		for (Vertex v: backgroundNodeset)
			this.localSurface.put(v, new QuadricProjection(v));
		
		// Compute projections of vertices from currentMesh
		this.mapCurrentVertexProjection = new HashMap<Vertex, ProjectedLocation>(backgroundNodeset.size());
		for (Vertex v: backgroundNodeset)
		{
			Vertex currentV = mapBgToCurrent.get(v);
			this.mapCurrentVertexProjection.put(currentV,
				new ProjectedLocation(currentV.getUV(), v.getNeighbourIteratorTriangle().next()));
		}
		mapBgToCurrent.clear();
	}
	
	public final Mesh getMesh()
	{
		return currentMesh;
	}
	
	/**
	 * Move Vertex on the desired location and update projection map.
	 * @param v Vertex being moved
	 * @param target  new location
	 * @return <code>true</code> if a projection has been found, <code>false</code> otherwise.  In the
	 *  latter case, vertex could have been moved, but iterative scheme did not converge to a solution.
	 */
	public final boolean move(Vertex v, double [] target)
	{
		if (LOGGER.isLoggable(Level.FINER))
			LOGGER.log(Level.FINER, "Trying to move vertex "+v+" to ("+target[0]+", "+target[1]+", "+target[2]+")");
		ProjectedLocation location = mapCurrentVertexProjection.get(v);
		return updateLocation(v, target, location);
	}

	private boolean updateLocation(Vertex v, double [] target, ProjectedLocation location)
	{
		Set<Triangle> visited = new HashSet<Triangle>();
		assert location != null : "No projection found at vertex " + v;
		if (location.projection == null || !location.projection.canProject())
		{
			if (LOGGER.isLoggable(Level.FINE))
				LOGGER.log(Level.FINE, "Point can not be moved because of its quadric: "+location.projection);
			return false;
		}
		visited.add(location.t);
		int counter = 0;
		// Coordinates of the projection of v on triangle plane
		double [] vPlane = new double[3];
		while(true)
		{
			// Move v to desired location
			v.moveTo(target[0], target[1], target[2]);
			// Project v on surface
			location.projection.project(v);
			// Check if v crossed triangle boundary
			location.projectOnTriangle(v.getUV(), vPlane);
			boolean inside = location.computeBarycentricCoordinates(vPlane);
			
			double [] p0 = location.t.vertex[0].getUV();
			double [] p1 = location.t.vertex[1].getUV();
			double [] p2 = location.t.vertex[2].getUV();
			// Constrain move within triangle boundary
			if (!inside)
			{
				for (int i = 0; i < 3; i++)
				{
					if (location.b[i] < 0.0)
						location.b[i] = 0.0;
				}
				// Values have been truncated
				double invSum = 1.0 / (location.b[0] + location.b[1] + location.b[2]);
				for (int i = 0; i < 3; i++)
					location.b[i] *= invSum;
				// Move vertex on boundary
				vPlane[0] = location.b[0]*p0[0] + location.b[1]*p1[0] + location.b[2]*p2[0];
				vPlane[1] = location.b[0]*p0[1] + location.b[1]*p1[1] + location.b[2]*p2[1];
				vPlane[2] = location.b[0]*p0[2] + location.b[1]*p1[2] + location.b[2]*p2[2];

				AbstractHalfEdge edge = location.t.getAbstractHalfEdge();
				AbstractHalfEdge sym = location.t.getAbstractHalfEdge();
				if (location.b[1] == 0.0)
					edge = edge.next();
				else if (location.b[2] == 0.0)
					edge = edge.prev();
				if (LOGGER.isLoggable(Level.FINER))
					LOGGER.log(Level.FINER, "Point is moved out of triangle by edge "+edge);

				if (edge.hasAttributes(AbstractHalfEdge.BOUNDARY))
				{
					LOGGER.log(Level.FINER, "Boundary edge "+edge);
					return true;
				}
				sym = edge.sym(sym);
				if (visited.contains(sym.getTri()))
				{
					LOGGER.fine("Loop detected when moving vertex");
					return true;
				}
				visited.add(sym.getTri());
				location.updateTriangle(sym.getTri());
				counter = 0;
			}
			counter++;
			if (counter > 2)
			{
				LOGGER.fine("Loop in triangle detected when moving vertex");
				return true;
			}
			// Compute barycentric coordinates in the new triangle
			location.computeBarycentricCoordinates(vPlane);
			if (location.updateProjection(v.getUV()))
			{
				// Projection has changed, check if projection can still be performed
				if (!location.projection.canProject())
				{
					LOGGER.fine("Quadric does not allow vertex projection");
					return false;
				}
			}
			else
			{
				// Projection has not changed, we found the projected point
				return true;
			}
		}
	}

	public final boolean project(Vertex v, double[] target, Vertex start)
	{
		ProjectedLocation location = mapCurrentVertexProjection.get(start);
		assert location != null : "Vertex "+start+" not found";
		ProjectedLocation proj = new ProjectedLocation(target, location.t);
		return updateLocation(v, target, proj);
	}

	public final Triangle getBackgroundTriangle(Vertex v)
	{
		ProjectedLocation location = mapCurrentVertexProjection.get(v);
		assert location != null : "Vertex "+v+" not found";
		return location.t;
	}

	/**
	 * Add a Vertex.
	 *
	 * @param v vertex in current mesh
	 * @param bgT triangle in the background mesh
	 */
	public final void addVertex(Vertex v, Triangle bgT)
	{
		mapCurrentVertexProjection.put(v, new ProjectedLocation(v.getUV(), bgT));
	}

	private class ProjectedLocation
	{
		private LocalSurfaceProjection projection;
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

		/**
		 *
		 * @param xyz coordinates
		 * @param t triangle in background mesh
		 */
		public ProjectedLocation(double [] xyz, Triangle t)
		{
			updateTriangle(t);
			computeBarycentricCoordinates(xyz);
			updateProjection(xyz);
		}
		
		private boolean updateTriangle(Triangle newT)
		{
			if (newT == t)
				return false;

			t = newT;
			invArea = 1.0 / Matrix3D.computeNormal3D(t.vertex[0].getUV(),
				t.vertex[1].getUV(), t.vertex[2].getUV(),
				work1, work2, normal);
			return true;
		}
		
		private boolean computeBarycentricCoordinates(double [] coord)
		{
			b[0] = Matrix3D.computeNormal3D(coord,
				t.vertex[1].getUV(), t.vertex[2].getUV(),
				work1, work2, work3) * invArea;
			b[0] *= (work3[0]*normal[0] + work3[1]*normal[1] + work3[2]*normal[2]);
			b[1] = Matrix3D.computeNormal3D(t.vertex[0].getUV(),
				coord, t.vertex[2].getUV(),
				work1, work2, work3) * invArea;
			b[1] *= (work3[0]*normal[0] + work3[1]*normal[1] + work3[2]*normal[2]);
			b[2] = Matrix3D.computeNormal3D(t.vertex[0].getUV(),
				t.vertex[1].getUV(), coord,
				work1, work2, work3) * invArea;
			b[2] *= (work3[0]*normal[0] + work3[1]*normal[1] + work3[2]*normal[2]);
			return b[0] >= 0.0 && b[1] >= 0.0 && b[2] >= 0.0;
		}
		
		private boolean updateProjection(double [] xyz)
		{
			int oldIndex = vIndex;
			double d0 = backgroundMesh.distance2(t.vertex[0].getUV(), xyz);
			double d1 = backgroundMesh.distance2(t.vertex[1].getUV(), xyz);
			double d2 = backgroundMesh.distance2(t.vertex[2].getUV(), xyz);
			if (d0 <= d1 && d0 <= d2)
				vIndex = 0;
			else if (d1 <= d0 && d1 <= d2)
				vIndex = 1;
			else
				vIndex = 2;
			
			if (vIndex == oldIndex)
				return false;
			LocalSurfaceProjection newProjection = MeshLiaison.this.localSurface.get(t.vertex[vIndex]);
			if (!newProjection.canProject())
				return false;
			projection = newProjection;
			return true;
		}
		
		private void projectOnTriangle(double [] xyz, double [] proj)
		{
			double [] o = t.vertex[vIndex].getUV();
			double dist =
				(xyz[0] - o[0]) * normal[0] +
				(xyz[1] - o[1]) * normal[1] +
				(xyz[2] - o[2]) * normal[2];
			for (int i = 0; i < 3; i++)
				proj[i] = xyz[i] - dist * normal[i];
		}
	}
}
