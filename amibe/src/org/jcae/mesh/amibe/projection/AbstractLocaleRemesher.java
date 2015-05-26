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

import gnu.trove.map.hash.TCustomHashMap;
import gnu.trove.strategy.HashingStrategy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.util.HashFactory;

/**
 * Rebuild a triangulation in closed set of AbstractHalfEdge.
 * This class only provide the glue to convert a set of AbstractHalfEdge to a
 * polyline, and update the adjacency using the new triangulation.
 * @author Jerome Robert
 */
public abstract class AbstractLocaleRemesher {
	private static class OrientedEdge
	{
		public Vertex origin;
		public Vertex destination;

		public OrientedEdge() {
		}

		public final void set(AbstractHalfEdge edge)
		{
			this.origin = edge.origin();
			this.destination = edge.destination();
		}

		public final void setSym(AbstractHalfEdge edge)
		{
			this.destination = edge.origin();
			this.origin = edge.destination();
		}
	}

	private final Map<Vertex, Vertex> vertexMap = HashFactory.createMap();
	private final OrientedEdge searchKey = new OrientedEdge();
	private final TCustomHashMap<AbstractHalfEdge, AbstractHalfEdge> edgeMap =
		new TCustomHashMap(new HashingStrategy<Object>() {

		public int computeHashCode(Object object) {
			Vertex v1, v2;
			if(object instanceof AbstractHalfEdge)
			{
				AbstractHalfEdge edge = (AbstractHalfEdge) object;
				v1 = edge.origin();
				v2 = edge.destination();
			}
			else
			{
				OrientedEdge edge = (OrientedEdge) object;
				v1 = edge.origin;
				v2 = edge.destination;
			}
			return v1.hashCode()+v2.hashCode();
		}

		public boolean equals(Object o1, Object o2) {
			Vertex origin1, origin2, destination1, destination2;
			if(o1 instanceof AbstractHalfEdge)
			{
				AbstractHalfEdge edge = (AbstractHalfEdge) o1;
				origin1 = edge.origin();
				destination1 = edge.destination();
			}
			else
			{
				OrientedEdge edge = (OrientedEdge) o1;
				origin1 = edge.origin;
				destination1 = edge.destination;
			}

			if(o2 instanceof AbstractHalfEdge)
			{
				AbstractHalfEdge edge = (AbstractHalfEdge) o2;
				origin2 = edge.origin();
				destination2 = edge.destination();
			}
			else
			{
				OrientedEdge edge = (OrientedEdge) o2;
				origin2 = edge.origin;
				destination2 = edge.destination;
			}
			return origin1 == origin2 && destination1 == destination2;
		}
	});

	private Collection<List<Vertex>> createPolylines(Collection<AbstractHalfEdge> edges)
	{
		assert !edges.isEmpty();
		for(AbstractHalfEdge e: edges)
			vertexMap.put(e.origin(), e.destination());

		Collection<List<Vertex>> polylines = new ArrayList<List<Vertex>>();
		while(!vertexMap.isEmpty())
		{
			Vertex start = vertexMap.keySet().iterator().next();
			ArrayList<Vertex> polyline = new ArrayList<Vertex>();
			polylines.add(polyline);
			Vertex current = vertexMap.get(start);
			vertexMap.remove(start);
			polyline.add(start);
			while(current != start)
			{
				Vertex next = vertexMap.get(current);
				assert next != null : "Cannot find next point after " + current
					+ " in\n" + edges + "\n. Map is\n" + vertexMap +
					"\n polylines is " + polylines;
				vertexMap.remove(current);
				polyline.add(current);
				current = next;
			}
			assert !polyline.isEmpty();
		}
		assert !polylines.isEmpty();
		return polylines;
	}

	private void updateLink(Vertex v, Triangle oldTri, Triangle newTri)
	{
		if(v.isManifold())
		{
			if(v.getLink() == oldTri)
				v.setLink(newTri);
		}
		else
		{
			Triangle[] link = (Triangle[])v.getLink();
			for(int i = 0; i < link.length; i++)
			{
				if(link[i] == oldTri)
				{
					link[i] = newTri;
					break;
				}
			}
		}
	}

	private void updateAdjacency(Mesh mesh, Collection<AbstractHalfEdge> edges,
		Collection<Triangle> newTriangles)
	{
		Collection<Triangle> newTrianglesCopy = HashFactory.createSet(newTriangles);
		edgeMap.clear();
		for(Triangle t: newTriangles)
		{
			AbstractHalfEdge e = t.getAbstractHalfEdge();
			for(int i = 0; i < 3; i++)
			{
				edgeMap.put(e, e);
				e = e.next();
			}
		}
		for(AbstractHalfEdge e: edges)
			edgeMap.put(e.sym(), e.sym());

		for(Triangle t: newTriangles)
		{
			AbstractHalfEdge e = t.getAbstractHalfEdge();
			for(int i = 0; i < 3; i++)
			{
				searchKey.setSym(e);
				AbstractHalfEdge other = edgeMap.get(searchKey);
				other.glue(e);
				if(other.hasAttributes(AbstractHalfEdge.BOUNDARY))
					e.setAttributes(AbstractHalfEdge.BOUNDARY);
				if(other.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
					e.setAttributes(AbstractHalfEdge.NONMANIFOLD);
				e = e.next();
			}
		}

		int group = -1;
		for(AbstractHalfEdge e: edges)
		{
			assert e.sym().sym() != e : e + "\n**\n" + e.sym() + "\n**\n" + e.sym().sym();
			group = e.getTri().getGroupId();
			assert edgeMap.contains(e) : e + "\n:sym" + edgeMap.get(e.sym());
			Triangle newTri = edgeMap.remove(e).getTri();
			newTrianglesCopy.remove(newTri);
			//assert newTri.getGroupId() == -1: newTri;
			newTri.setGroupId(group);
			assert newTriangles.contains(newTri);
			updateLink(e.origin(), e.getTri(), newTri);
			updateLink(e.destination(), e.getTri(), newTri);
		}

		// only triangles created from inside vertices remain in newTriangleCopy.
		// We set their group to the group of an input edge
		for(Triangle t:newTrianglesCopy) {
			t.setGroupId(group);
			for(int i = 0; i < 3; i++) {
				Vertex v = t.getV(i);
				if(v.getLink() == null)
					v.setLink(t);
			}
		}
	}

	protected Collection<Triangle> newTriangles = new ArrayList<Triangle>();
	protected List<Vertex> vertIndex = new ArrayList<Vertex>();

	public void triangulate(Mesh mesh,
		Collection<AbstractHalfEdge> edges, Collection<List<Vertex>> vertices)
		throws IOException
	{
		Collection<List<Vertex>> polylines = createPolylines(edges);
		polylines.addAll(vertices);
		vertIndex.clear();
		for(List<Vertex> vs: polylines)
			vertIndex.addAll(vs);
		newTriangles.clear();
		triangulate(mesh, polylines);
		assert !newTriangles.isEmpty();
		updateAdjacency(mesh, edges, newTriangles);
	}

	public Collection<Triangle> getNewTriangles()
	{
		return newTriangles;
	}
	/**
	 * return and edge create in triangulate(Mesh, Collection, Collection) using
	 * the vertices parameter
	 */
	public AbstractHalfEdge getEdge(Vertex origin, Vertex destination)
	{
		searchKey.origin = origin;
		searchKey.destination = destination;
		return edgeMap.get(searchKey);
	}

	protected void addTriangle(Mesh m, int i1, int i2, int i3)
	{
		Vertex v1 = vertIndex.get(i1);
		Vertex v2 = vertIndex.get(i2);
		Vertex v3 = vertIndex.get(i3);
		newTriangles.add(m.createTriangle(v1, v2, v3));
	}

	/** Actually triangulate and call addTriangle for each triangle */
	protected abstract void triangulate(Mesh mesh, Collection<List<Vertex>> vertices) throws IOException;
}
