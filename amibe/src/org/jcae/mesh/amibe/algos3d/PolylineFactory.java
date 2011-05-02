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
 * (C) Copyright 2011, by EADS France
 */
package org.jcae.mesh.amibe.algos3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Vertex;

/**
 * Compute polylines from the beams of a mesh
 * @author Jerome Robert
 */
public class PolylineFactory extends HashMap<Integer, Collection<List<Vertex>>>{
	private static class BeamVertex extends ArrayList<Beam>
		implements Comparable<BeamVertex>
	{
		private static int beamCounter = 0;
		/** Replace hashCode to ensure the algorithm is reproducible */
		private final int id = beamCounter ++;
		public final Vertex vertex;
		public boolean isManifold()
		{
			return size() == 2;
		}

		/** Non manifold vertices are concidered higher than others */
		public int compareTo(BeamVertex o) {
			if(!isManifold() && o.isManifold())
				return 1;
			else if(isManifold() && !o.isManifold())
				return -1;
			else
				return id - o.id;
		}

		public BeamVertex(Vertex vertex) {
			super(2);
			this.vertex = vertex;
		}

		public Beam getOther(Beam b)
		{
			assert isManifold();
			if(b == get(0))
				return get(1);
			else
				return get(0);
		}

		@Override
		public boolean equals(Object o) {
			return id == ((BeamVertex)o).id;
		}

		@Override
		public int hashCode() {
			return id;
		}
	}

	private static class Beam
	{
		public final BeamVertex v1, v2;
		public Beam(BeamVertex v1, BeamVertex v2) {
			this.v1 = v1;
			this.v2 = v2;
		}

		public BeamVertex getOther(BeamVertex v)
		{
			return (v == v1) ? v2 : v1;
		}

		@Override
		public String toString() {
			return "(" + v1.id + ", " + v2.id + ")";
		}

		public void remove()
		{
			v1.remove(this);
			v2.remove(this);
		}

		public void check()
		{
			assert v1.contains(this);
			assert v2.contains(this);
		}
	}

	private static BeamVertex createBeamVertex(Vertex v, Map<Vertex, BeamVertex> map)
	{
		BeamVertex bv = map.get(v);
		if(bv == null)
		{
			bv = new BeamVertex(v);
			map.put(v, bv);	
		}
		return bv;
	}

	private static Map<Integer, Collection<Beam>> indexify(Mesh mesh)
	{
		List<Vertex> beams = mesh.getBeams();
		int nbBeams = beams.size() / 2;
		Map<Vertex, BeamVertex> verticeMap = new HashMap<Vertex, BeamVertex>();
		Map<Integer, Collection<Beam>> beamMap = new HashMap<Integer, Collection<Beam>>();
		for(int i = 0; i<nbBeams; i++)
		{
			BeamVertex v1 = createBeamVertex(beams.get(i*2), verticeMap);
			BeamVertex v2 = createBeamVertex(beams.get(i*2+1), verticeMap);
			Beam beam = new Beam(v1, v2);
			v1.add(beam);
			v2.add(beam);
			int group = mesh.getBeamGroup(i);
			Collection<Beam> beamSet = beamMap.get(group);
			if(beamSet == null)
			{
				beamSet = new HashSet<Beam>();
				beamMap.put(group, beamSet);
			}
			beamSet.add(beam);
		}
		return beamMap;
	}

	/** @return the oposit beam of the polyline */
	private static void createPolyline(BeamVertex startV, Beam start,
		List<Vertex> polyline, List<Beam> beams)
	{
		polyline.add(startV.vertex);
		BeamVertex cv = start.getOther(startV);
		Beam cb = start;
		beams.add(start);
		while(cv.isManifold() && cv != startV)
		{			
			assert startV.vertex != cv.vertex;
			assert !polyline.contains(cv.vertex):polyline.indexOf(cv.vertex)+" / "+polyline.size();
			polyline.add(cv.vertex);
			cb = cv.getOther(cb);
			beams.add(cb);
			cv = cb.getOther(cv);
			assert start != cb;
		}
		polyline.add(cv.vertex);
	}

	public PolylineFactory(Mesh mesh) {
		Map<Integer, Collection<Beam>> beamMap = indexify(mesh);
		HashSet<BeamVertex> nonManVerts = new HashSet<BeamVertex>();
		ArrayList<Beam> polylineB = new ArrayList<Beam>();
		for(Entry<Integer, Collection<Beam>> e:beamMap.entrySet())
		{
			int group = e.getKey();
			Collection<List<Vertex>> polylines = new ArrayList<List<Vertex>>();
			put(group, polylines);
			Collection<Beam> beamSet = e.getValue();
			nonManVerts.clear();
			for(Beam b:beamSet)
			{
				if(!b.v1.isManifold())
					nonManVerts.add(b.v1);
				if(!b.v2.isManifold())
					nonManVerts.add(b.v2);
			}
			for(BeamVertex bv:nonManVerts)
			{
				for(Beam startBeam:bv)
				{
					if(beamSet.contains(startBeam))
					{
						ArrayList<Vertex> polyline = new ArrayList<Vertex>();
						polylineB.clear();
						createPolyline(bv, startBeam, polyline, polylineB);
						polylines.add(polyline);
						beamSet.removeAll(polylineB);
					}
				}
			}
		}
	}
}
