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
	private final Map<Vertex, LocalProjection> mapCurrentVertexProjection;
	
	private final static double [] work1 = new double[3];
	private final static double [] work2 = new double[3];
	private final static double [] work3 = new double[3];
		
	
	public MeshLiaison(Mesh backgroundMesh)
	{
		this.backgroundMesh = backgroundMesh;
		
		// Adjacency relations are needed on backgroundMesh
		if (!this.backgroundMesh.hasAdjacency())
			throw new IllegalArgumentException();

		Collection<Vertex> backgroundNodeset;
		if (this.backgroundMesh.hasNodes())
			backgroundNodeset= this.backgroundMesh.getNodes();
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
		
		this.currentMesh = new Mesh();
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
		this.mapCurrentVertexProjection = new HashMap<Vertex, LocalProjection>(backgroundNodeset.size());
		for (Vertex v: backgroundNodeset)
		{
			Vertex currentV = mapBgToCurrent.get(v);
			this.mapCurrentVertexProjection.put(currentV,
				new LocalProjection(currentV.getUV(), v.getNeighbourIteratorTriangle().next()));
		}
		mapBgToCurrent.clear();
	}
	
	public Mesh getMesh()
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
	public boolean move(Vertex v, double [] target)
	{
		if (LOGGER.isLoggable(Level.FINER))
			LOGGER.log(Level.FINER, "Trying to move vertex "+v+" to ("+target[0]+", "+target[1]+", "+target[2]+")");
		Set<Triangle> visited = new HashSet<Triangle>();
		LocalProjection proj = mapCurrentVertexProjection.get(v);
		assert proj != null : "No projection found at vertex " + v;
		if (!proj.quadric.canProject())
		{
			if (LOGGER.isLoggable(Level.FINE))
				LOGGER.log(Level.FINE, "Point can not be moved because of its quadric: "+proj.quadric);
			return false;
		}
		visited.add(proj.t);
		int counter = 0;
		// Coordinates of the projection of v on triangle plane
		double [] vPlane;
		while(true)
		{
			// Move v to desired location
			v.moveTo(target[0], target[1], target[2]);
			// Project v on surface
			proj.quadric.project(v);
			// Check if v crossed triangle boundary
			vPlane = proj.projectOnTriangle(v.getUV());
			boolean inside = proj.computeBarycentricCoordinates(vPlane);
			
			double [] p0 = proj.t.vertex[0].getUV();
			double [] p1 = proj.t.vertex[1].getUV();
			double [] p2 = proj.t.vertex[2].getUV();
			// Constrain move within triangle boundary
			if (!inside)
			{
				for (int i = 0; i < 3; i++)
				{
					if (proj.b[i] < 0.0)
						proj.b[i] = 0.0;
				}
				// Values have been truncated
				double invSum = 1.0 / (proj.b[0] + proj.b[1] + proj.b[2]);
				for (int i = 0; i < 3; i++)
					proj.b[i] *= invSum;
				// Move vertex on boundary
				vPlane[0] = proj.b[0]*p0[0] + proj.b[1]*p1[0] + proj.b[2]*p2[0];
				vPlane[1] = proj.b[0]*p0[1] + proj.b[1]*p1[1] + proj.b[2]*p2[1];
				vPlane[2] = proj.b[0]*p0[2] + proj.b[1]*p1[2] + proj.b[2]*p2[2];

				AbstractHalfEdge edge = proj.t.getAbstractHalfEdge();
				AbstractHalfEdge sym = proj.t.getAbstractHalfEdge();
				if (proj.b[1] == 0.0)
					edge = edge.next();
				else if (proj.b[2] == 0.0)
					edge = edge.prev();
				if (LOGGER.isLoggable(Level.FINER))
					LOGGER.log(Level.FINER, "Point is moved out of triangle by edge "+edge);

				sym = edge.sym(sym);
				if (visited.contains(sym.getTri()))
				{
					LOGGER.fine("Loop detected when moving vertex");
					return false;
				}
				visited.add(sym.getTri());
				proj.updateTriangle(sym.getTri());
				counter = 0;
			}
			counter++;
			if (counter > 2)
			{
				LOGGER.fine("Loop in triangle detected when moving vertex");
				return false;				
			}
			// Compute barycentric coordinates in the new triangle
			proj.computeBarycentricCoordinates(vPlane);
			if (proj.updateQuadric(v.getUV()))
			{
				// Quadric has changed, check if projection can still be performed
				if (!proj.quadric.canProject())
				{
					LOGGER.fine("Quadric does not allow vertex projection");
					return false;
				}
			}
			else
			{
				// Quadric has not changed, we found the projected point
				return true;
			}
		}
	}

	
	private class LocalProjection
	{
		LocalSurfaceProjection quadric;
		// triangle where vertex is projected into
		Triangle t;
		// inverse of triangle area
		double invArea;
		// normal to triangle plane
		double [] normal = new double[3];
		// local index of origin
		int vIndex = -1;
		// barycentric coordinates
		double [] b = new double[3];
		
		public LocalProjection(double [] xyz, Triangle t)
		{
			updateTriangle(t);
			computeBarycentricCoordinates(xyz);
			updateQuadric(xyz);
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
		
		private boolean updateQuadric(double [] xyz)
		{
			int oldIndex = vIndex;
			double d0 = Matrix3D.distance2(t.vertex[0].getUV(), xyz);
			double d1 = Matrix3D.distance2(t.vertex[1].getUV(), xyz);
			double d2 = Matrix3D.distance2(t.vertex[2].getUV(), xyz);
			if (d0 <= d1 && d0 <= d2)
				vIndex = 0;
			else if (d1 <= d0 && d1 <= d2)
				vIndex = 1;
			else
				vIndex = 2;
			
			if (vIndex == oldIndex)
				return false;
			quadric = MeshLiaison.this.localSurface.get(t.vertex[vIndex]);
			return true;
		}
		
		private double [] projectOnTriangle(double [] xyz)
		{
			double [] proj = new double[3];
			double [] o = t.vertex[vIndex].getUV();
			double dist =
				(xyz[0] - o[0]) * normal[0] +
				(xyz[1] - o[1]) * normal[1] +
				(xyz[2] - o[2]) * normal[2];
			for (int i = 0; i < 3; i++)
				proj[i] = xyz[i] - dist * normal[i];
			return proj;
		}
	}
}
