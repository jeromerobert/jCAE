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

import java.io.FileNotFoundException;
import java.util.List;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MeshLiaison
{
	private final static Logger LOGGER = Logger.getLogger(MeshLiaison.class.getName());
	
	private final Mesh backgroundMesh;
	private final Mesh currentMesh;
	// Map between vertices of currentMesh and their projection on backgroundMesh
	private final Map<Vertex, ProjectedLocation> mapCurrentVertexProjection;
	
	private final double [] work1 = new double[3];
	private final double [] work2 = new double[3];
	private final double [] work3 = new double[3];
	
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
		this.currentMesh.getTrace().setDisabled(this.backgroundMesh.getTrace().getDisabled());

		// Create vertices of currentMesh
		Map<Vertex, Vertex> mapBgToCurrent = new HashMap<Vertex, Vertex>(backgroundNodeset.size()+1);
		for (Vertex v : backgroundNodeset)
		{
			Vertex currentV = cloneVertex(v, currentMesh, mapBgToCurrent);
			if (this.currentMesh.hasNodes())
				this.currentMesh.add(currentV);
		}

		mapBgToCurrent.put(this.backgroundMesh.outerVertex, this.currentMesh.outerVertex);

		// Create triangles of currentMesh
		for (Triangle t : this.backgroundMesh.getTriangles())
		{
			if (t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			Triangle newT = this.currentMesh.createTriangle(
				mapBgToCurrent.get(t.vertex[0]),
				mapBgToCurrent.get(t.vertex[1]),
				mapBgToCurrent.get(t.vertex[2]));
			newT.setGroupId(t.getGroupId());
			this.currentMesh.add(newT);
		}

		cloneBeams(backgroundMesh, currentMesh, mapBgToCurrent);
		// Create groups of currentMesh
		for (int i = 1; i <= this.backgroundMesh.getNumberOfGroups(); i++)
			this.currentMesh.setGroupName(i, this.backgroundMesh.getGroupName(i));
		this.currentMesh.buildAdjacency();
		
		// Compute projections of vertices from currentMesh
		this.mapCurrentVertexProjection = new HashMap<Vertex, ProjectedLocation>(backgroundNodeset.size());
		for (Vertex v: backgroundNodeset)
		{
			Iterator<Triangle> it = v.getNeighbourIteratorTriangle();
			if(it.hasNext())
				this.addVertex(mapBgToCurrent.get(v), it.next());
		}
		mapBgToCurrent.clear();

		this.currentMesh.setPersistentReferences(this.backgroundMesh.hasPersistentReferences());
	}

	private void cloneBeams(Mesh backgroundMesh, Mesh currentMesh, Map<Vertex, Vertex> map) {
		List<Vertex> beams = backgroundMesh.getBeams();

		int nb = beams.size();
		for (int i = 0; i < nb; i += 2)
		{
			Vertex v1 = map.get(beams.get(i));
			if (v1 == null)
				v1 = cloneVertex(beams.get(i), currentMesh, map);
			Vertex v2 = map.get(beams.get(i + 1));
			if (v2 == null)
				v2 = cloneVertex(beams.get(i + 1), currentMesh, map);
			currentMesh.addBeam(v1, v2, backgroundMesh.getBeamGroup(i / 2));
		}
	}

	private Vertex cloneVertex(Vertex v, Mesh dst, Map<Vertex, Vertex> map)
	{
			Vertex currentV = this.currentMesh.createVertex(v.getUV());
			currentV.setRef(v.getRef());
			currentV.setLabel(v.getLabel());
			map.put(v, currentV);
			return currentV;
	}

	public final Mesh getMesh()
	{
		return currentMesh;
	}

	/**
	 * Move Vertex on the desired location, project onto background mesh
	 * and update projection map.
	 * @param v Vertex being moved
	 * @param target  new location
	 * @return <code>true</code> if a projection has been found, <code>false</code> otherwise.
	 * In this case, vertex is not moved to the target position.
	 */
	public final boolean move(Vertex v, double [] target)
	{
		if (LOGGER.isLoggable(Level.FINER))
			LOGGER.log(Level.FINER, "Trying to move vertex "+v+" to ("+target[0]+", "+target[1]+", "+target[2]+")");
		// Old projection
		ProjectedLocation location = mapCurrentVertexProjection.get(v);
		if (LOGGER.isLoggable(Level.FINEST))
			LOGGER.log(Level.FINEST, "Old projection: "+location);
		LocationFinder lf = new LocationFinder(target);
		AbstractHalfEdge ot = location.t.getAbstractHalfEdge();
		if (ot.apex() == location.t.vertex[location.vIndex])
			ot = ot.prev();
		else if (ot.destination() == location.t.vertex[location.vIndex])
			ot = ot.next();
		lf.walkAroundOrigin(ot);
		lf.walkByAdjacency();
		// Now lf contains the new location.
		// Update location
		location.updateTriangle(lf.current);
		location.updateVertexIndex(target);

		if (LOGGER.isLoggable(Level.FINEST))
			LOGGER.log(Level.FINEST, "New projection: "+location);
		double [] newPosition = new double[3];
		location.projectOnTriangle(target, newPosition);
		if (!location.computeBarycentricCoordinates(newPosition))
		{
			int[] index = new int[2];
			double maxError = sqrDistanceVertexTriangle(target, lf.current, index);
			AbstractHalfEdge newEdge = ot;
			do
			{
				ot = newEdge;
				newEdge = findBetterTriangleInNeighborhood(target, ot, maxError);
				maxError *= 0.5;
			} while (newEdge != null);
			if (ot != null)
			{
				location.updateTriangle(ot.getTri());
				location.updateVertexIndex(target);
			}
		}
		if (!location.computeBarycentricCoordinates(newPosition))
		{
			/* FIXME: this should not happen. Try all triangles to find the best projection */
			LOGGER.log(Level.CONFIG, "Position found outside triangle: "+newPosition[0]+" "+newPosition[1]+" "+newPosition[2]+"; checking all triangles, this may be slow");
			lf.walkDebug(backgroundMesh);
			location.updateTriangle(lf.current);
			location.updateVertexIndex(target);
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
		currentMesh.getTrace().moveVertex(v, newPosition[0], newPosition[1], newPosition[2]);
		v.moveTo(newPosition[0], newPosition[1], newPosition[2]);

		if (LOGGER.isLoggable(Level.FINER))
			LOGGER.log(Level.FINER, "Final position: "+v);
		return true;
	}

	public final boolean project(Vertex v, double[] target, Vertex start)
	{
		throw new RuntimeException("Not implemented yet");

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

	/**
	 * Remove a Vertex.
	 *
	 * @param v vertex in current mesh
	 * @return  triangle in the background mesh
	 */
	public final Triangle removeVertex(Vertex v)
	{
		return mapCurrentVertexProjection.remove(v).t;
	}

	public final void updateAll()
	{
		LOGGER.config("Update projections");
		for (Vertex v : mapCurrentVertexProjection.keySet())
			move(v, v.getUV());
	}

	public static AbstractHalfEdge findSurroundingTriangleDebug(Vertex v, Mesh mesh)
	{
		LocationFinder lf = new LocationFinder(v.getUV());
		lf.walkDebug(mesh);
		int i = lf.localEdgeIndex;
		AbstractHalfEdge ret = lf.current.getAbstractHalfEdge();
		if (ret.origin() == lf.current.vertex[i])
			ret = ret.next();
		else if (ret.destination() == lf.current.vertex[i])
			ret = ret.prev();
		return ret;
	}

	public AbstractHalfEdge findSurroundingTriangle(Vertex v, Vertex start, double maxError, boolean background)
	{
		Triangle t = null;
		for (Iterator<Triangle> itf = start.getNeighbourIteratorTriangle(); itf.hasNext(); )
		{
			Triangle f = itf.next();
			if (!f.hasAttributes(AbstractHalfEdge.OUTER))
			{
				t = f;
				break;
			}
		}
		if (t != null)
		{
			AbstractHalfEdge ot = t.getAbstractHalfEdge();
			if (start == ot.destination())
				ot = ot.next(ot);
			else if (start == ot.apex())
				ot = ot.prev(ot);
			assert start == ot.origin();

			AbstractHalfEdge ret = findSurroundingTriangle(v, t, ot.getLocalNumber(), maxError);
			if (ret != null)
				return ret;
		}
		// We were not able to find a valid triangle.
		// Iterate over all triangles to find the best one.
		// FIXME: This is obviously very slow!
		if (LOGGER.isLoggable(Level.FINE))
			LOGGER.log(Level.FINE, "Maximum error reached, search into the whole mesh for vertex "+v);
		return findSurroundingTriangleDebug(v, (background ? backgroundMesh : currentMesh));
	}

	public static AbstractHalfEdge findSurroundingTriangle(Vertex v, Triangle start, int localEdge, double maxError)
	{
		double[] pos = v.getUV();
		LocationFinder lf = new LocationFinder(pos);
		AbstractHalfEdge ot = start.getAbstractHalfEdge();
		if (localEdge == 1)
			ot = ot.next();
		else if (localEdge == 2)
			ot = ot.prev();
		lf.walkAroundOrigin(ot);
		lf.walkByAdjacency();

		if (lf.dmin < maxError)
		{
			AbstractHalfEdge ret = lf.current.getAbstractHalfEdge();
			if (ret.origin() == lf.current.vertex[lf.localEdgeIndex])
				ret = ret.next();
			else if (ret.destination() == lf.current.vertex[lf.localEdgeIndex])
				ret = ret.prev();
			return ret;
		}

		// Check a better start edge in neighborhood
		if (LOGGER.isLoggable(Level.FINER))
			LOGGER.log(Level.FINER, "Error too large: "+lf.dmin+" > "+maxError);
		return findBetterTriangleInNeighborhood(pos, ot, maxError);
	}

	private static AbstractHalfEdge findBetterTriangleInNeighborhood(double[] pos, AbstractHalfEdge ot, double maxError)
	{
		int[] index = new int[2];
		Triangle.List seen = new Triangle.List();
		LinkedList<Triangle> queue = new LinkedList<Triangle>();
		queue.add(ot.origin().getNeighbourIteratorTriangle().next());
		while (!queue.isEmpty())
		{
			Triangle t = queue.poll();
			if (seen.contains(t) || t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			double dist = sqrDistanceVertexTriangle(pos, t, index);
			if (dist < maxError)
			{
				seen.clear();
				int i = index[0];
				ot = t.getAbstractHalfEdge(ot);
				if (ot.origin() == t.vertex[i])
					ot = ot.next();
				else if (ot.destination() == t.vertex[i])
					ot = ot.prev();
				if (LOGGER.isLoggable(Level.FINER))
					LOGGER.log(Level.FINER, "Found better edge: error="+dist+" "+ot);
				return ot;
			}
			seen.add(t);
			// Add symmetric triangles
			ot = t.getAbstractHalfEdge(ot);
			for (int i = 0; i < 3; i++)
			{
				ot = ot.next();
				if (ot.hasAttributes(AbstractHalfEdge.BOUNDARY))
					continue;
				if (ot.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
				{
					for (Iterator<AbstractHalfEdge> it = ot.fanIterator(); it.hasNext(); )
						queue.add(it.next().getTri());
				}
				else
					queue.add(ot.sym().getTri());
			}
			// Add links to non-manifold vertices
			for (Vertex n : t.vertex)
			{
				if (!n.isManifold())
				{
					Triangle[] links = (Triangle[]) n.getLink();
					for (Triangle f : links)
						queue.add(f);
				}
			}
		}
		seen.clear();
		return null;
	}

	public static double getDistanceVertexTriangle(Vertex v, Triangle tri)
	{
		int[] index = new int[2];
		return Math.sqrt(sqrDistanceVertexTriangle(v.getUV(), tri, index));
	}

	public static Triangle getNearestTriangleDebug(Vertex v, Mesh m)
	{
		LocationFinder lf = new LocationFinder(v.getUV());
		lf.walkDebug(m);
		return lf.current;
	}

	/**
	 * Compute squared distance between a point and a triangle.  See
	 *   http://www.geometrictools.com/Documentation/DistancePoint3Triangle3.pdf
	 */
	private static double sqrDistanceVertexTriangle(double[] pos, Triangle tri, int[] index)
	{
		double[] t0 = tri.vertex[0].getUV();
		double[] t1 = tri.vertex[1].getUV();
		double[] t2 = tri.vertex[2].getUV();
		double a = tri.vertex[0].sqrDistance3D(tri.vertex[1]);
		double b =
			(t1[0] - t0[0]) * (t2[0] - t0[0]) +
			(t1[1] - t0[1]) * (t2[1] - t0[1]) +
			(t1[2] - t0[2]) * (t2[2] - t0[2]);
		double c = tri.vertex[0].sqrDistance3D(tri.vertex[2]);
		double d =
			(t1[0] - t0[0]) * (t0[0] - pos[0]) +
			(t1[1] - t0[1]) * (t0[1] - pos[1]) +
			(t1[2] - t0[2]) * (t0[2] - pos[2]);
		double e =
			(t2[0] - t0[0]) * (t0[0] - pos[0]) +
			(t2[1] - t0[1]) * (t0[1] - pos[1]) +
			(t2[2] - t0[2]) * (t0[2] - pos[2]);
		double f =
			(pos[0] - t0[0]) * (pos[0] - t0[0]) +
			(pos[1] - t0[1]) * (pos[1] - t0[1]) +
			(pos[2] - t0[2]) * (pos[2] - t0[2]);
		// Minimize Q(s,t) = a*s*s + 2.0*b*s*t + c*t*t + 2.0*d*s + 2.0*e*t + f
		double det = a*c - b*b;
		double s = b*e - c*d;
		double t = b*d - a*e;
		index[0] = index[1] = -1;
		if ( s+t <= det )
		{
			if ( s < 0.0 )
			{
				if ( t < 0.0 )
				{
					// region 4
					if (d < 0.0)
					{
						t = 0.0;
						index[0] = 2;
						if (-d >= a)
						{
							index[1] = 6;
							s = 1.0;
						}
						else
						{
							index[1] = 5;
							s = -d/a;
						}
					}
					else
					{
						s = 0.0;
						index[0] = 1;
						if (e >= 0.0)
						{
							index[1] = 4;
							t = 0.0;
						}
						else if (-e >= c)
						{
							index[1] = 2;
							t = 1.0;
						}
						else
						{
							index[1] = 3;
							t = -e/c;
						}
					}
				}
				else
				{
					// region 3
					s = 0.0;
					index[0] = 1;
					if (e >= 0.0)
					{
						index[1] = 4;
						t = 0.0;
					}
					else if (-e >= c)
					{
						index[1] = 2;
						t = 1.0;
					}
					else
					{
						index[1] = 3;
						t = -e/c;
					}
				}
			}
			else if ( t < 0.0 )
			{
				// region 5
				t = 0.0;
				index[0] = 2;
				if (d >= 0.0)
				{
					index[1] = 4;
					s = 0.0;
				}
				else if (-d >= a)
				{
					index[1] = 6;
					s = 1.0;
				}
				else
				{
					index[1] = 5;
					s = -d/a;
				}
			}
			else
			{
				// region 0
				double invDet = 1.0 / det;
				s *= invDet;
				t *= invDet;
				if (t <= s && t <= 1.0 - s - t)
					index[1] = 5;
				else if (s <= t && s <= 1.0 - s - t)
					index[1] = 3;
				else if (s >= 1.0 - s - t && t >= 1.0 - s -t)
					index[1] = 1;
				else
					throw new RuntimeException("Illegal arguments: s="+s+" t="+t+" "+det+"\n"+tri);
				index[0] = index[1] / 2;
			}
		}
		else
		{
			if ( s < 0.0 )
			{
				// region 2
				if (c+e > b+d)
				{
					// minimum on edge s+t = 1
					double numer = (c+e) - (b+d);
					double denom = (a-b) + (c-b);
					index[0] = 0;
					if (numer >= denom)
					{
						index[1] = 6;
						s = 1.0;
					}
					else
					{
						index[1] = 1;
						s = numer / denom;
					}
					t = 1.0 - s;
				}
				else
				{
					// minimum on edge s = 0
					s = 0.0;
					index[0] = 1;
					if (e >= 0.0)
					{
						index[1] = 4;
						t = 0.0;
					}
					else if (-e >= c)
					{
						index[1] = 2;
						t = 1.0;
					}
					else
					{
						index[1] = 3;
						t = -e/c;
					}
				}
			}
			else if ( t < 0.0 )
			{
				// region 6
				if (a+d > b+e)
				{
					// minimum on edge s+t = 1
					double numer = (a+d) - (b+e);
					double denom = (a-b) + (c-b);
					index[0] = 0;
					if (numer >= denom)
					{
						index[1] = 2;
						t = 1.0;
					}
					else
					{
						index[1] = 1;
						t = numer / denom;
					}
					s = 1.0 - t;
				}
				else
				{
					// minimum on edge t=0
					t = 0.0;
					index[0] = 2;
					if (d >= 0.0)
					{
						index[1] = 4;
						s = 0.0;
					}
					else if (-d >= a)
					{
						index[1] = 6;
						s = 1.0;
					}
					else
					{
						index[1] = 5;
						s = -d/a;
					}
				}
			}
			else
			{
				// region 1
				double numer = (c+e) - (b+d);
				index[0] = 0;
				if (numer <= 0.0)
				{
					index[1] = 2;
					s = 0.0;
				}
				else
				{
					double denom = (a-b)+(c-b);
					if (numer >= denom)
					{
						index[1] = 6;
						s = 1.0;
					}
					else
					{
						index[1] = 1;
						s = numer/denom;
					}
				}
				t = 1.0 - s;
			}
		}
		double ret = a*s*s + 2.0*b*s*t + c*t*t + 2.0*d*s + 2.0*e*t + f;
		// Fix possible numerical errors
		if (ret < 0.0)
			ret = 0.0;
		return ret;
	}

	public static void checkFindSurroundingTriangle(String[] args) throws FileNotFoundException
	{
		org.jcae.mesh.amibe.traits.MeshTraitsBuilder mtb = org.jcae.mesh.amibe.traits.MeshTraitsBuilder.getDefault3D();
		mtb.addNodeList();
		Mesh mesh = new Mesh(mtb);
		Vertex v0 = mesh.createVertex(10.0, 20.0, 30.0);
		Vertex v1 = mesh.createVertex(16.0, 20.0, 30.0);
		Vertex v2 = mesh.createVertex(12.0, 26.0, 30.0);
		Triangle t = mesh.createTriangle(v0, v1, v2);
		int [] index = new int[2];
		int nGrid = 128;
		double[] pos = new double[3];
		java.io.PrintStream outMesh = new java.io.PrintStream("test.mesh");
		java.io.PrintStream outBB = new java.io.PrintStream("region.bb");
		java.io.PrintStream distBB = new java.io.PrintStream("test.bb");
		outMesh.println("MeshVersionFormatted 1\n\nDimension\n3\n\nGeometry\n\"test.mesh\"\n\nVertices");
		outMesh.println(nGrid*nGrid+3);
		outBB.println("3 1 "+(nGrid*nGrid+3)+" 2");
		distBB.println("3 1 "+(nGrid*nGrid+3)+" 2");
		for (int j = 0; j < nGrid; j++)
		{
			pos[1] = 15.0 + (j * 16) / (double)nGrid;
			pos[2] = 30.05;
			for (int i = 0; i < nGrid; i++)
			{
				pos[0] =  5.0 + (i * 16) / (double)nGrid;
				double d = sqrDistanceVertexTriangle(pos, t, index);
				outMesh.println(pos[0]+" "+pos[1]+" "+pos[2]+" 0");
				outBB.println((double)index[1]);
				distBB.println(d);
			}
		}
		index[1] = 0;
		pos = v0.getUV();
		outMesh.println(pos[0]+" "+pos[1]+" "+pos[2]+" "+index[1]);
		outBB.println("0.0");
		distBB.println(sqrDistanceVertexTriangle(pos, t, index));
		pos = v1.getUV();
		outMesh.println(pos[0]+" "+pos[1]+" "+pos[2]+" "+index[1]);
		outBB.println("0.0");
		distBB.println(sqrDistanceVertexTriangle(pos, t, index));
		pos = v2.getUV();
		outMesh.println(pos[0]+" "+pos[1]+" "+pos[2]+" "+index[1]);
		outBB.println("0.0");
		distBB.println(sqrDistanceVertexTriangle(pos, t, index));

		outMesh.println("\n\nQuadrilaterals\n"+((nGrid-1)*(nGrid-1)));
		for (int j = 0; j < nGrid - 1; j++)
		{
			for (int i = 0; i < nGrid - 1; i++)
			{
				outMesh.println(""+(j*nGrid+i+1)+" "+(j*nGrid+i+2)+" "+((j+1)*nGrid+i+2)+" "+((j+1)*nGrid+i+1)+" 0");
			}
		}
		int o = nGrid*nGrid;
		outMesh.println("\n\nTriangles\n1\n"+(o+1)+" "+(o+2)+" "+(o+3)+" 0");
		outMesh.println("\n\nEnd");
		outMesh.close();
		outBB.close();
		distBB.close();
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

		/**
		 *
		 * @param xyz coordinates
		 * @param t triangle in background mesh
		 */
		public ProjectedLocation(double [] xyz, Triangle t)
		{
			updateTriangle(t);
			computeBarycentricCoordinates(xyz);
			updateVertexIndex(xyz);
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
		
		private boolean updateVertexIndex(double [] xyz)
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

			return vIndex != oldIndex;
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

	private static class LocationFinder
	{
		private double[] target = new double[3];
		double dmin = Double.MAX_VALUE;
		Triangle current;
		int localEdgeIndex = -1;
		int region = -1;
		int[] index = new int[2];

		LocationFinder(double[] pos)
		{
			System.arraycopy(pos, 0, target, 0, 3);
		}

		void walkOnTriangle(Triangle t)
		{
			double dist = sqrDistanceVertexTriangle(target, t, index);
			if (dist < dmin)
			{
				dmin = dist;
				current = t;
				localEdgeIndex = index[0];
				region = index[1];
			}
		}
	
		void walkAroundOrigin(AbstractHalfEdge ot)
		{
			AbstractHalfEdge loop = ot.getTri().getAbstractHalfEdge();
			if (loop.origin() == ot.destination())
				loop = loop.prev();
			else if (loop.origin() == ot.apex())
				loop = loop.next();
			Vertex d = loop.destination();
			do
			{
				if (loop.hasAttributes(AbstractHalfEdge.OUTER))
				{
					loop = loop.nextOriginLoop();
					continue;
				}
				walkOnTriangle(loop.getTri());
				loop = loop.nextOriginLoop();
			}
			while (loop.destination() != d);
		}

		// Cross edges to see if adjacent triangle is nearer
		void walkByAdjacency()
		{
			AbstractHalfEdge ot = current.getAbstractHalfEdge();
			if (ot.origin() == current.vertex[localEdgeIndex])
				ot = ot.next();
			else if (ot.destination() == current.vertex[localEdgeIndex])
				ot = ot.prev();
			do
			{
				if (ot.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
					break;
				AbstractHalfEdge sym = ot.sym();
				Triangle t = sym.getTri();
				double dist = sqrDistanceVertexTriangle(target, t, index);
				if (dist >= dmin)
					break;
				dmin = dist;
				ot = sym;
				current = t;
				localEdgeIndex = index[0];
				if (index[1] % 2 == 0)
				{
					int i = ((index[1] / 2) + 1) % 3;
					if (ot.apex() == current.vertex[i])
						ot = ot.prev();
					else if (ot.destination() == current.vertex[i])
						ot = ot.next();
					walkAroundOrigin(ot);
				}
				else
				{
					if (ot.origin() == current.vertex[localEdgeIndex])
						ot = ot.next();
					else if (ot.destination() == current.vertex[localEdgeIndex])
						ot = ot.prev();
				}
			} while (true);
		}

		void walkDebug(Mesh mesh)
		{
			for (Triangle f : mesh.getTriangles())
			{
				if (f.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
				double dist = sqrDistanceVertexTriangle(target, f, index);
				if (dist < dmin)
				{
					dmin = dist;
					current = f;
					localEdgeIndex = index[0];
				}
			}
		}

		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder("Distance: ");
			sb.append(dmin);
			sb.append("\nEdge index: ");
			sb.append(localEdgeIndex);
			sb.append("\n");
			sb.append(current);
			return sb.toString();
		}

	}

}
