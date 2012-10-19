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

import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIntIterator;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import gnu.trove.TIntObjectHashMap;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class MeshLiaison
{
	private final static Logger LOGGER = Logger.getLogger(MeshLiaison.class.getName());

	protected final Mesh backgroundMesh;
	protected final Mesh currentMesh;
	private Skeleton skeleton;

	protected final double [] work1 = new double[3];
	protected final double [] work2 = new double[3];
	protected final double [] work3 = new double[3];

	public static MeshLiaison create(Mesh backgroundMesh)
	{
		return new MapMeshLiaison(backgroundMesh);
	}

	public static MeshLiaison create(Mesh backgroundMesh, MeshTraitsBuilder mtb)
	{
		return new MapMeshLiaison(backgroundMesh, mtb);
	}

	protected MeshLiaison(Mesh backgroundMesh)
	{
		this(backgroundMesh, backgroundMesh.getBuilder());
	}

	protected MeshLiaison(Mesh backgroundMesh, MeshTraitsBuilder mtb)
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
		
		// count the number of vertices for each group
		this.currentMesh = new Mesh(mtb);
		this.currentMesh.getTrace().setDisabled(this.backgroundMesh.getTrace().getDisabled());
		Map<Vertex, String> vgGroups = createVGMap();

		// Create vertices of currentMesh
		Map<Vertex, Vertex> mapBgToCurrent = new HashMap<Vertex, Vertex>(backgroundNodeset.size()+1);
		for (Vertex v : backgroundNodeset)
		{
			Vertex currentV = cloneVertex(v, mapBgToCurrent, vgGroups);
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

		cloneBeams(backgroundMesh, currentMesh, mapBgToCurrent, vgGroups);
		// Create groups of currentMesh
		for (int i = 1; i <= this.backgroundMesh.getNumberOfGroups(); i++)
			this.currentMesh.setGroupName(i, this.backgroundMesh.getGroupName(i));
		this.currentMesh.buildAdjacency();

		init(backgroundNodeset);

		for (Vertex v: backgroundNodeset)
		{
			Iterator<Triangle> it = v.getNeighbourIteratorTriangle();
			if(it.hasNext())
				this.addVertex(mapBgToCurrent.get(v), it.next());
		}
		mapBgToCurrent.clear();

		this.currentMesh.setPersistentReferences(this.backgroundMesh.hasPersistentReferences());
	}

	protected abstract void init(Collection<Vertex> backgroundNodeset);

	private void cloneBeams(Mesh backgroundMesh, Mesh currentMesh,
		Map<Vertex, Vertex> map, Map<Vertex, String> vGroups)
	{
		List<Vertex> beams = backgroundMesh.getBeams();

		int nb = beams.size();
		for (int i = 0; i < nb; i += 2)
		{
			Vertex v1 = map.get(beams.get(i));
			if (v1 == null)
				v1 = cloneVertex(beams.get(i), map, vGroups);
			Vertex v2 = map.get(beams.get(i + 1));
			if (v2 == null)
				v2 = cloneVertex(beams.get(i + 1), map, vGroups);
			currentMesh.addBeam(v1, v2, backgroundMesh.getBeamGroup(i / 2));
		}
	}

	private Vertex cloneVertex(Vertex v, Map<Vertex, Vertex> map, Map<Vertex, String> vGroups)
	{
			Vertex currentV = this.currentMesh.createVertex(v.getUV());
			currentV.setRef(v.getRef());
			currentV.setLabel(v.getLabel());
			currentV.setMutable(v.isMutable());
			String g = vGroups.get(v);
			if(g != null)
				currentMesh.setVertexGroup(currentV, g);
			map.put(v, currentV);
			return currentV;
	}

	private Map<Vertex, String> createVGMap()
	{
		Map<Vertex, String> toReturn = new HashMap<Vertex, String>();
		for(Entry<String, Collection<Vertex>> e:backgroundMesh.getVertexGroup().entrySet())
		{
			for(Vertex v: e.getValue())
			{
				toReturn.put(v, e.getKey());
			}
		}
		return toReturn;
	}

	public final Mesh getMesh()
	{
		return currentMesh;
	}

	public abstract void backupRestore(Vertex v, boolean restore, int group);

	/**
	 *
	 * @param v The vertex to project
	 * @param target Where the vertex should be projected
	 * @param checkGroup Whether or not to ensure that the point is projected on
	 * a triangle of the same group as the group of the vertex. The test is only
	 * made if all triangles adjacent to the point are from the same group.
	 * @return Whether or not the projection succeeded.
	 */
	public final boolean backupAndMove(Vertex v, double [] target, int group)
	{
		return move(v, target, true, group, true);
	}

	/**
	 * Move Vertex on the desired location, project onto background mesh
	 * and update projection map.
	 * @param v Vertex being moved
	 * @param target  new location
	 * @return <code>true</code> if a projection has been found, <code>false</code> otherwise.
	 * In this case, vertex is not moved to the target position.
	 */
	public final boolean move(Vertex v, double [] target, boolean doCheck)
	{
		return move(v, target, false, -1, doCheck);
	}

	public final boolean move(Vertex v, double [] target, int group, boolean doCheck)
	{
		return move(v, target, false, group, doCheck);
	}

	protected abstract boolean move(Vertex v, double [] target, boolean backup,  int group, boolean doCheck);

	public final boolean project(Vertex v, double[] target, Vertex start)
	{
		throw new RuntimeException("Not implemented yet");

	}

	public abstract Triangle getBackgroundTriangle(Vertex v);

	public abstract double[] getBackgroundNormal(Vertex v);

	/**
	 * If all adjacent triangles of the vertex are in the same group, return
	 * this group, else return null.
	 */
	private int getGroup(Vertex v)
	{
		Iterator<Triangle> it = v.getNeighbourIteratorTriangle();
		int group = -1;
		if(it.hasNext())
			group = it.next().getGroupId();
		while(it.hasNext())
		{
			if(group != it.next().getGroupId())
				return -1;
		}
		return group;
	}

	/**
	 * Add a Vertex.
	 *
	 * @param v vertex in current mesh
	 * @param bgT triangle in the background mesh
	 */
	public abstract void addVertex(Vertex v, Triangle bgT);

	/**
	 * Remove a Vertex.
	 *
	 * @param v vertex in current mesh
	 * @return  triangle in the background mesh
	 */
	public abstract Triangle removeVertex(Vertex v);

	public abstract void updateAll();

	public static AbstractHalfEdge findSurroundingTriangleDebug(
		Vertex v, Mesh mesh, int group)
	{
		LocationFinder lf = new LocationFinder(v.getUV(), group);
		lf.walkDebug(mesh, group);
		AbstractHalfEdge ret = lf.current.getAbstractHalfEdge();
		if (ret.origin() == lf.current.vertex[lf.localEdgeIndex])
			ret = ret.next();
		else if (ret.destination() == lf.current.vertex[lf.localEdgeIndex])
			ret = ret.prev();
		return ret;
	}

	/** Square of the distance between v and it's orthogonal projection on e */
	private static double sqrOrthoDistance(Vertex v, AbstractHalfEdge e)
	{
		double[] o = e.origin().getUV();
		double[] d = e.destination().getUV();
		double[] vv = v.getUV();
		double[] od = new double[3];
		double[] ov = new double[3];
		//norm of od
		double n2 = 0;
		//dot product od.ov
		double dot = 0;
		for(int i = 0; i < 3; i++)
		{
			od[i] = d[i] - o[i];
			ov[i] = vv[i] - o[i];
			n2 += od[i] * od[i];
			dot += od[i] * ov[i];
		}
		dot /= n2;
		n2 = 0;
		for(int i = 0; i < 3; i++)
		{
			double tmp = od[i] * dot - ov[i];
			n2 += tmp * tmp;
		}
		return n2;
	}

	/**
	 *
	 * @param v The vertex whose to find the nearest edge
	 * @param t The triangle where to take the edges
	 * @param excludeAttr Edges which have this attribut will be excluded from
	 * the search
	 * @param dist a 2 sized array containing the squared distance between v and
	 * the returnd edge and the squared distance between v and closest edge
	 * included excluded edges. Can be null.
	 * @return the found vertex or null if all edges were excluded by excludeAttr
	 */
	public static AbstractHalfEdge findNearestEdge(Vertex v, Triangle t,
		int excludeAttr, double[] dist)
	{
		double minDistance = Double.MAX_VALUE;
		double minExDistance = Double.MAX_VALUE;
		AbstractHalfEdge minEdge = null;
		AbstractHalfEdge e1 = t.getAbstractHalfEdge();
		for(int i = 0; i < 3; i++)
		{
			boolean hasAttr = e1.hasAttributes(excludeAttr);
			if(!hasAttr || dist != null)
			{
				double d = sqrOrthoDistance(v, e1);
				if(hasAttr)
				{
					if(dist != null && d < minExDistance)
						minExDistance = d;
				}
				else if(d < minDistance)
				{
					minDistance = d;
					minEdge = e1;
				}
			}
			e1 = e1.next();
		}
		if(dist != null)
		{
			dist[0] = minDistance;
			dist[1] = minExDistance;
		}
		return minEdge;
	}

	public static AbstractHalfEdge findNearestEdge(Vertex v, Triangle t)
	{
		return findNearestEdge(v, t, 0, null);
	}

	public AbstractHalfEdge findSurroundingTriangle(Vertex v, Vertex start, double maxError, boolean background)
	{
		return findSurroundingTriangle(v, start, maxError, background, -1);
	}

	/**
	 *
	 * @param v The vertex around which to search.
	 * @param start A vertex from witch to start the search
	 * @param maxError Maximum acceptable distance between v and it's projection
	 * @param background must be true if start is on the background mesh, false
	 * if it's on the forground mesh
	 * @param group Search only in this group (-1 to search in all groups)
	 * @return The closest HalfEdge of vertex, on the background mesh whose
	 * triangle contains the projection of v.
	 */
	public AbstractHalfEdge findSurroundingTriangle(Vertex v, Vertex start,
		double maxError, boolean background, int group)
	{
		Triangle t = null;
		for (Iterator<Triangle> itf = start.getNeighbourIteratorTriangle(); itf.hasNext(); )
		{
			Triangle f = itf.next();
			if (!f.hasAttributes(AbstractHalfEdge.OUTER))
			{
				if((group >= 0 && f.getGroupId() == group) || (group < 0) || !itf.hasNext())
				{
					t = f;
					break;
				}
			}
		}
		if (t != null)
		{
			AbstractHalfEdge ret = findSurroundingTriangle(v, t, maxError, group);
			if (ret != null)
				return ret;
		}
		// We were not able to find a valid triangle.
		// Iterate over all triangles to find the best one.
		// FIXME: This is obviously very slow!
		if (LOGGER.isLoggable(Level.FINE))
			LOGGER.log(Level.FINE, "Maximum error reached, search into the whole "+(background ? "background " : "")+"mesh for vertex "+v+" into group "+group);
		return findSurroundingTriangleDebug(v, (background ? backgroundMesh : currentMesh), group);
	}

	public static Triangle findSurroundingInAdjacentTriangles(Vertex v, Triangle start)
	{
		double[] pos = v.getUV();
		AbstractHalfEdge ot = start.getAbstractHalfEdge();
		AbstractHalfEdge sym = start.getAbstractHalfEdge();
		int[] index = new int[2];
		double dmin = sqrDistanceVertexTriangle(pos, start, index);
		Triangle ret = start;
		for (int i = 0; i < 3; i++)
		{
			ot = ot.next();
			if (ot.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
				continue;
			sym = ot.sym(sym);
			Triangle t = sym.getTri();
			double dist = sqrDistanceVertexTriangle(pos, t, index);
			if (dist < dmin)
			{
				dmin = dist;
				ret = t;
			}
		}

		return ret;
	}

	/**
	 * @return an AbstractHalfEdge whose distance to vertex v is lower than maxError.
	 *     If no edge is found, null is returned.
	 */
	public static AbstractHalfEdge findSurroundingTriangle(Vertex v,
		Triangle start, double maxError, int group)
	{
		double[] pos = v.getUV();
		boolean redo = true;
		LocationFinder lf = new LocationFinder(pos, group);

		Triangle t = start;
		AbstractHalfEdge ot = t.getAbstractHalfEdge();
		while(true)
		{
			lf.walkOnTriangle(t);
			if (lf.localEdgeIndex == 1)
				ot = ot.next();
			else if (lf.localEdgeIndex == 2)
				ot = ot.prev();
			lf.walkFlipFlop(ot);

			if((group >= 0 && lf.current.getGroupId() == group) || group < 0)
			{
				ot = lf.current.getAbstractHalfEdge(ot);
				if (ot.origin() == lf.current.vertex[lf.localEdgeIndex])
					ot = ot.next();
				else if (ot.destination() == lf.current.vertex[lf.localEdgeIndex])
					ot = ot.prev();
				if (lf.dmin < maxError)
					return ot;
			}

			if (!redo)
				break;
			// Check a better start edge in neighborhood
			if (LOGGER.isLoggable(Level.FINER))
				LOGGER.log(Level.FINER, "Error too large: "+lf.dmin+" > "+maxError);
			ot = findBetterTriangleInNeighborhood(pos, ot, maxError, group);
			if (ot == null)
				return null;
			redo = false;
			t = ot.getTri();
			if (ot.origin() == t.vertex[0])
				ot = ot.next();
			else if (ot.destination() == t.vertex[0])
				ot = ot.prev();
		}

		return null;
	}

	protected static AbstractHalfEdge findBetterTriangleInNeighborhood(double[] pos, AbstractHalfEdge ot, double maxError, int group)
	{
		int[] index = new int[2];
		Triangle.List seen = new Triangle.List();
		LinkedList<Triangle> queue = new LinkedList<Triangle>();
		queue.add(ot.origin().getNeighbourIteratorTriangle().next());
		while (!queue.isEmpty())
		{
			Triangle t = queue.poll();
			if(group >= 0 && t.getGroupId() != group)
				continue;
			if (seen.contains(t) || t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			double dist = sqrDistanceVertexTriangle(pos, t, index);
			if (group >= 0 && dist < maxError)
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

	public final void buildSkeleton()
	{
		skeleton = new Skeleton(backgroundMesh);
	}

	public final boolean isNearSkeleton(Vertex v, int groupId, double distance2)
	{
		return skeleton.isNearer(v, groupId, distance2);
	}

	public static double getDistanceVertexTriangle(Vertex v, Triangle tri)
	{
		int[] index = new int[2];
		return Math.sqrt(sqrDistanceVertexTriangle(v.getUV(), tri, index));
	}

	/**
	 * Compute squared distance between a point and a triangle.  See
	 *   http://www.geometrictools.com/Documentation/DistancePoint3Triangle3.pdf
	 * @param index index[0] is the local id of the closest edge and index[1]
	 * the region.
	 */
	protected static double sqrDistanceVertexTriangle(double[] pos, Triangle tri, int[] index)
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

	protected static class LocationFinder
	{
		private final static Logger LOGGER2 = Logger.getLogger(LocationFinder.class.getName());
		private double[] target = new double[3];
		double dmin = Double.MAX_VALUE;
		Triangle current;
		int localEdgeIndex = -1;
		int region = -1;
		int[] index = new int[2];
		private final int groupID;

		LocationFinder(double[] pos, int groupID)
		{
			System.arraycopy(pos, 0, target, 0, 3);
			this.groupID = groupID;
		}

		/*
		 * Finds the best approximated point on this triangle.
		 * This method initializes the {#current} member and must be
		 * called before other methods.
		 */
		boolean walkOnTriangle(Triangle t)
		{
			double dist = sqrDistanceVertexTriangle(target, t, index);
			if (dist < dmin)
			{
				dmin = dist;
				current = t;
				localEdgeIndex = index[0];
				region = index[1];
				return true;
			}
			return false;
		}
	
		protected boolean walkAroundOrigin(AbstractHalfEdge ot)
		{
			AbstractHalfEdge loop = ot.getTri().getAbstractHalfEdge();
			if (loop.origin() == ot.destination())
				loop = loop.prev();
			else if (loop.origin() == ot.apex())
				loop = loop.next();
			boolean modified = false;
			Vertex d = loop.destination();
			do
			{
				if (loop.hasAttributes(AbstractHalfEdge.OUTER))
				{
					loop = loop.nextOriginLoop();
					continue;
				}
				if(groupID == -1 || loop.getTri().getGroupId() == groupID)
					modified |= walkOnTriangle(loop.getTri());
				loop = loop.nextOriginLoop();
			}
			while (loop.destination() != d);
			return modified;
		}

		/**
		 * @param initEdge From where to start the flip flop
		 */
		boolean walkFlipFlop(AbstractHalfEdge initEdge)
		{
			walkAroundOrigin(initEdge);

			AbstractHalfEdge ot = current.getAbstractHalfEdge();
			if (ot.origin() == current.vertex[localEdgeIndex])
				ot = ot.next();
			else if (ot.destination() == current.vertex[localEdgeIndex])
				ot = ot.prev();

			boolean modified = false;
			int countdown = 2;
			while (true)
			{
				if (walkAroundOrigin(ot))
				{
					modified = true;
					countdown = 2;
					ot = current.getAbstractHalfEdge(ot);
					if (ot.origin() == current.vertex[localEdgeIndex])
						ot = ot.next();
					else if (ot.destination() == current.vertex[localEdgeIndex])
						ot = ot.prev();
				}
				ot = ot.sym();
				countdown--;
				if (countdown <= 0)
					break;
			}
			return modified;
		}

		// Cross edges to see if adjacent triangle is nearer
		boolean walkByAdjacency()
		{
			AbstractHalfEdge ot = current.getAbstractHalfEdge();
			if (ot.origin() == current.vertex[localEdgeIndex])
				ot = ot.next();
			else if (ot.destination() == current.vertex[localEdgeIndex])
				ot = ot.prev();
			boolean modified = false;
			do
			{
				if (ot.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
					break;
				AbstractHalfEdge sym = ot.sym();
				Triangle t = sym.getTri();
				double dist = sqrDistanceVertexTriangle(target, t, index);
				if (dist >= dmin)
					break;
				modified = true;
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
			return modified;
		}

		void walkDebug(Mesh mesh, int group)
		{
			LOGGER2.fine("Before walkDebug(): "+toString());
			for (Triangle f : mesh.getTriangles())
			{
				if (f.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
				if (group >= 0 && f.getGroupId() != group)
					continue;
				double dist = sqrDistanceVertexTriangle(target, f, index);
				if (dist < dmin)
				{
					dmin = dist;
					current = f;
					localEdgeIndex = index[0];
				}
			}
			LOGGER2.fine("After walkDebug(): "+toString());
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

	private static class Skeleton
	{
		private final TIntObjectHashMap<Collection<Line>> mapGroupBorder = new TIntObjectHashMap<Collection<Line>>();
	
		Skeleton(Mesh mesh)
		{
			if (!mesh.hasAdjacency())
				throw new IllegalArgumentException("Mesh does not contain adjacency relations");
			// Always add group -1
			mapGroupBorder.put(-1, new ArrayList<Line>());
			AbstractHalfEdge ot = null;
			for (Triangle t : mesh.getTriangles())
			{
				if (t.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
				int groupId = t.getGroupId();
				Collection<Line> borders = mapGroupBorder.get(groupId);
				if (borders == null)
				{
					borders = new ArrayList<Line>();
					mapGroupBorder.put(groupId, borders);
				}
				// This test is performed here so that mapGroupBorder.get(N)
				// is not null if a group has no boundary edge.
				if (!t.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD | AbstractHalfEdge.SHARP))
					continue;
				ot = t.getAbstractHalfEdge(ot);
				for (int i = 0; i < 3; i++)
				{
					ot = ot.next();
					if (ot.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD | AbstractHalfEdge.SHARP))
						borders.add(new Line(ot.origin(), ot.destination()));
				}
			}
		}
	
		double getSqrDistance(Vertex v, int groupId)
		{
			Collection<Line> borders = mapGroupBorder.get(groupId);
			if (borders == null)
				throw new IllegalArgumentException("group identifier not found");
			double[] pos = v.getUV();
			double dMin = Double.MAX_VALUE;
			for (Line l : borders)
			{
				double d = l.sqrDistance(pos);
				if (d < dMin)
					dMin = d;
			}
			return dMin;
		}
	
		boolean isNearer(Vertex v, int groupId, double distance2)
		{
			Collection<Line> borders = mapGroupBorder.get(groupId);
			if (borders == null)
				throw new IllegalArgumentException("group identifier "+groupId+" not found");
			double[] pos = v.getUV();
			for (Line l : borders)
			{
				if (l.sqrDistance(pos) <= distance2)
				{
					return true;
				}
			}
			return false;
		}
	
		private static class Line
		{
			private final double[] origin = new double[3];
			private final double[] direction = new double[3];
			private final double sqrNormDirection;
			Line(Vertex v1, Vertex v2)
			{
				System.arraycopy(v1.getUV(), 0, origin, 0, 3);
				double[] end = v2.getUV();
				direction[0] = end[0] - origin[0];
				direction[1] = end[1] - origin[1];
				direction[2] = end[2] - origin[2];
				sqrNormDirection = direction[0] * direction[0] + direction[1] * direction[1] + direction[2] * direction[2];
			}
	
			double sqrDistance(double[] v)
			{
				double t =
					direction[0] * (v[0] - origin[0]) +
					direction[1] * (v[1] - origin[1]) +
					direction[2] * (v[2] - origin[2]);
				if (t <= 0)
					t = 0.0;
				else if (t >= sqrNormDirection)
					t = 1.0;
				else
					t /= sqrNormDirection;
				double dx = v[0] - (origin[0] + t * direction[0]);
				double dy = v[1] - (origin[1] + t * direction[1]);
				double dz = v[2] - (origin[2] + t * direction[2]);
				return dx * dx + dy * dy + dz * dz;
			}
		}
	}
}
