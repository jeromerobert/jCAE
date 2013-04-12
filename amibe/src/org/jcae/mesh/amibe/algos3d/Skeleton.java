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
 * (C) Copyright 2012, by EADS France
 */

package org.jcae.mesh.amibe.algos3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.amibe.util.HashFactory;
import org.jcae.mesh.xmldata.Amibe2VTK;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;

/**
 * Compute polylines from non-manifold, boundary, and group boundary edges
 * @author Jerome Robert
 */
public class Skeleton {
	private final Collection<List<AbstractHalfEdge>> polylines;
	/**
	 * @param mesh
	 * @param angle min polyline angle
	 */
	public Skeleton(Mesh mesh, double angle)
	{
		polylines = computePolylines(getNonManifoldHE(mesh), angle);
	}

	/**
	 * Return half edges witch are border of the give groups
	 * @param groupIds
	 * @return
	 */
	public Collection<AbstractHalfEdge> getByGroups(int ... groupIds)
	{
		ArrayList<AbstractHalfEdge> a = new ArrayList<AbstractHalfEdge>();
		for(List<AbstractHalfEdge> l:getPolylines(groupIds))
			a.addAll(l);
		return a;
	}

	public Collection<AbstractHalfEdge> getByGroups(int groupIds)
	{
		ArrayList<AbstractHalfEdge> a = new ArrayList<AbstractHalfEdge>();
		for(List<AbstractHalfEdge> l:getPolylines(groupIds))
			a.addAll(l);
		return a;
	}

	public Collection<List<AbstractHalfEdge>> getPolylines(int groupIds)
	{
		ArrayList<List<AbstractHalfEdge>> toReturn = new ArrayList<List<AbstractHalfEdge>>();
		main: for(List<AbstractHalfEdge> l: polylines)
		{
			AbstractHalfEdge e = l.get(0);
			if(e.getTri().getGroupId() == groupIds)
				toReturn.add(l);
		}
		return toReturn;
	}

	/**
	 * Return polylines which are border of the given groups
	 * @param groupIds
	 * @return
	 */
	public Collection<List<AbstractHalfEdge>> getPolylines(int ... groupIds)
	{
		ArrayList<List<AbstractHalfEdge>> toReturn = new ArrayList<List<AbstractHalfEdge>>();
		main: for(List<AbstractHalfEdge> l: polylines)
		{
			AbstractHalfEdge e = l.get(0);
			if(e.getTri().getGroupId() == groupIds[0])
			{
				if(e.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
				{
					int k = 0;
					Iterator<AbstractHalfEdge> it = e.fanIterator();
					while(it.hasNext())
					{
						AbstractHalfEdge ne = it.next();
						if(k >= groupIds.length || ne.getTri().getGroupId() != groupIds[k++])
							continue main;
					}
				}
				else if(groupIds.length != 2 || e.sym().getTri().getGroupId() != groupIds[1])
					continue main;

				toReturn.add(l);
			}
		}
		return toReturn;
	}

	/** Wrap List&ltVertex&gt to ensure polyline unicity */
	private class VertexPolyline
	{
		public final List<Vertex> vertices;

		public VertexPolyline(List<AbstractHalfEdge> edges) {
			vertices = new ArrayList<Vertex>(edges.size() + 1);
			for(AbstractHalfEdge e:edges)
				vertices.add(e.origin());
			vertices.add(edges.get(edges.size()-1).destination());
		}

		@Override
		public boolean equals(Object obj) {
			List<Vertex> o = ((VertexPolyline) obj).vertices;
			int s = vertices.size() - 1;
			int os = o.size() - 1;
			return s == os &&
				((vertices.get(0) == o.get(0) &&
				vertices.get(1) == o.get(1) &&
				vertices.get(s) == o.get(s) &&
				vertices.get(s - 1) == o.get(s - 1)) ||
				(vertices.get(0) == o.get(s) &&
				vertices.get(1) == o.get(s-1) &&
				vertices.get(s) == o.get(0) &&
				vertices.get(1) == o.get(s - 1)));
		}

		@Override
		public int hashCode() {
			return vertices.get(0).hashCode() +
				vertices.get(vertices.size()-1).hashCode();
		}
	}

	/**
	 * Return all polylines as vertices.
	 *
	 */
	public Collection<List<Vertex>> getPolylinesVertices()
	{
		Set<VertexPolyline> hs = HashFactory.createSet(polylines.size());
		for(List<AbstractHalfEdge> l:polylines)
			hs.add(new VertexPolyline(l));
		ArrayList<List<Vertex>> toReturn = new ArrayList<List<Vertex>>(hs.size());
		for(VertexPolyline l:hs)
			toReturn.add(l.vertices);
		return toReturn;
	}

	/**
	 * Return all polylines as half edges.
	 * One polyline is returned by fan (ex 3 polyline for a T junction).
	 */
	public Collection<List<AbstractHalfEdge>> getPolylines()
	{
		return Collections.unmodifiableCollection(polylines);
	}

	private Collection<AbstractHalfEdge> getNonManifoldHE(Mesh mesh)
	{
		Set<AbstractHalfEdge> toReturn = HashFactory.createSet();
		for(Triangle t:mesh.getTriangles())
		{
			AbstractHalfEdge he = t.getAbstractHalfEdge();
			assert he != null;
			for(int i = 0; i < 3; i++)
			{
				if(isNonManifold(he))
					toReturn.add(he);
				he = he.next();
			}
		}
		return toReturn;
	}

	private boolean isNonManifold(AbstractHalfEdge he)
	{
		if(he.hasAttributes(AbstractHalfEdge.OUTER))
			return false;
		return he.hasAttributes(AbstractHalfEdge.NONMANIFOLD) ||
			he.hasAttributes(AbstractHalfEdge.BOUNDARY) ||
			he.getTri().getGroupId() != he.sym().getTri().getGroupId();
	}

	private void getGroups(AbstractHalfEdge v, Collection<Integer> groups)
	{
		if(v.hasAttributes(AbstractHalfEdge.BOUNDARY))
		{
			groups.add(v.getTri().getGroupId());
		}
		else if(v.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
		{
			Iterator<AbstractHalfEdge> it = v.fanIterator();
			while(it.hasNext())
				groups.add(it.next().getTri().getGroupId());
		}
		else
		{
			groups.add(v.getTri().getGroupId());
			groups.add(v.sym().getTri().getGroupId());
		}
	}

	private boolean isPolylineEnd(AbstractHalfEdge edge, double angle)
	{
		AbstractHalfEdge next = null;
		Iterator<AbstractHalfEdge> it = edge.destination().getNeighbourIteratorAbstractHalfEdge();
		while(it.hasNext())
		{
			AbstractHalfEdge e = it.next();
			assert e != edge;
			assert e.origin() == edge.destination();
			if((next == null || e.destination() != next.destination()) &&
				e.destination() != edge.origin() && isNonManifold(e))
			{
				if(next == null)
					next = e;
				else
					return true;
			}
		}
		if(next == null)
			//next should have been an outer half edge so we ignore this case
			//The vertex edge.destination() will be check using an other edge
			return false;
		if(edge.hasAttributes(AbstractHalfEdge.IMMUTABLE) != next.hasAttributes(
			AbstractHalfEdge.IMMUTABLE))
			return true;
		Set<Integer> g1 = HashFactory.createSet();
		Set<Integer> g2 = HashFactory.createSet();
		getGroups(edge, g1);
		getGroups(next, g2);
		if(!g1.equals(g2))
			return true;
		return edge.destination().angle3D(edge.origin(), next.destination()) < angle;
	}

	private List<AbstractHalfEdge> createPolyline(AbstractHalfEdge startEdge,
		Collection<Vertex> possibleEnds)
	{
		ArrayList<AbstractHalfEdge> beams = new ArrayList<AbstractHalfEdge>();
		Vertex cv = startEdge.destination();
		AbstractHalfEdge cb = startEdge;
		beams.add(startEdge);
		while(cv != startEdge.origin() && !possibleEnds.contains(cv))
		{
			Iterator<AbstractHalfEdge> it = cv.getNeighbourIteratorAbstractHalfEdge();
			while(it.hasNext())
			{
				AbstractHalfEdge e = it.next();
				if(e.destination() != cb.origin() &&
					e.getTri().getGroupId() == startEdge.getTri().getGroupId() &&
					isNonManifold(e))
				{
					cb = e;
					break;
				}
			}
			assert !beams.contains(cb): "Cannot find the edge next to "+cv+
				" in group "+startEdge.getTri().getGroupId();
			beams.add(cb);
			cv = cb.destination();
		}
		return beams;
	}

	private Collection<List<AbstractHalfEdge>> computePolylines(
		Collection<AbstractHalfEdge> input, double angle)
	{
		ArrayList<List<AbstractHalfEdge>> toReturn = new ArrayList<List<AbstractHalfEdge>>();
		Collection<AbstractHalfEdge> beamSet = HashFactory.createSet(input);
		Set<Vertex> polylineEnds = HashFactory.createSet();
		for(AbstractHalfEdge b:beamSet)
		{
			if(isPolylineEnd(b, angle))
				polylineEnds.add(b.destination());
		}

		//The first iteration is for polyline ends detected by isPolylineEnd.
		//Following iterations are for smooth polylines
		do
		{
			for(Vertex bv:polylineEnds)
			{
				Iterator<AbstractHalfEdge> it = bv.getNeighbourIteratorAbstractHalfEdge();
				while(it.hasNext())
				{
					AbstractHalfEdge startBeam = it.next();
					if(beamSet.contains(startBeam))
					{
						List<AbstractHalfEdge> polylineB = createPolyline(
							startBeam, polylineEnds);
						beamSet.removeAll(polylineB);
						toReturn.add(Collections.unmodifiableList(polylineB));
					}
				}
			}
			polylineEnds.clear();
			// Beams which are in smooth loops are not detected by
			// isPolylineEnd. At this step beamSet should only contains such
			// beams, so any vertex could be concidered as a polyline end.
			if(!beamSet.isEmpty())
				polylineEnds.add(beamSet.iterator().next().origin());
		}
		while(!polylineEnds.isEmpty());
		return toReturn;
	}

	public static void main(final String[] args) {
		try {
			Mesh m = new Mesh(MeshTraitsBuilder.getDefault3D());
			assert m.hasAdjacency();
			MeshReader.readObject3D(m, "/home/robert/ast-a319-neo/demo-anabelle/demo/amibe.dir");
			Skeleton sk = new Skeleton(m, 0);
			System.out.println(sk.getPolylines().size());
			int k = 1000;
			for(List<AbstractHalfEdge> p:sk.getPolylines())
			{
				for(AbstractHalfEdge e:p)
					m.addBeam(e.origin(), e.destination(), k);
				k++;
			}
			m.getTriangles().clear();
			MeshWriter.writeObject3D(m, "/tmp/zob.amibe", null);
			new Amibe2VTK("/tmp/zob.amibe").write("/tmp/zob.vtp");
		} catch (Exception ex) {
			Logger.getLogger(Skeleton.class.getName()).log(Level.SEVERE, null,
				ex);
		}
	}
}
