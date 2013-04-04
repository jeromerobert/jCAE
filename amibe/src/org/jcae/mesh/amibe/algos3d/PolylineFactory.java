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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.util.HashFactory;

/**
 * Compute polylines from the beams of a mesh.
 * The key of the map is the group id.
 * @author Jerome Robert
 */
public class PolylineFactory extends HashMap<Integer, Collection<List<Vertex>>>{

	/** Remove polylines ends which would lead to small polylines */
	private static void filterSmall(Set<BeamVertex> polylineEnds, double small2) {
		Set<BeamVertex> toRemove = HashFactory.createSet();
		for(BeamVertex bv: polylineEnds)
		{
			for(Beam b:bv)
			{
				BeamVertex other = b.getOther(bv);
				if(polylineEnds.contains(other) && !toRemove.contains(bv) && b.lengthSqr() < small2)
				{
					if(bv.isManifold() && !other.isManifold())
						toRemove.add(bv);
					else if(other.isManifold() && !bv.isManifold())
						toRemove.add(other);
					else if(bv.isManifold() && other.isManifold())
						toRemove.add(other);
				}
			}
		}
		polylineEnds.removeAll(toRemove);
	}
	private final boolean ignoreGroups;
	private static int beamCounter = 0;

	private class BeamVertex extends ArrayList<Beam>
		implements Comparable<BeamVertex>
	{
		/** Replace hashCode to ensure the algorithm is reproducible */
		private final int id = beamCounter ++;
		public final Vertex vertex;

		public boolean isPolylineEnd(double dotProdLimit)
		{
			return !isManifold() || !isSmooth(dotProdLimit);
		}

		private boolean isManifold()
		{
			return size() == 2 && (ignoreGroups || get(0).group == get(1).group);
		}

		private double[] getVector(int i)
		{
			Vertex vv1 = get(i).getOther(this).vertex;
			double[] r = new double[3];
			vv1.sub(vertex, r);
			return r;
		}
		private boolean isSmooth(double dotProdLimit)
		{
			if(dotProdLimit < -1)
				return true;
			double[] v1 = getVector(0);
			double[] v2 = getVector(1);
			double nv1 = Matrix3D.norm(v1);
			double nv2 = Matrix3D.norm(v2);
			double dot = Matrix3D.prodSca(v1, v2) / nv1 / nv2;
			return dot < dotProdLimit;
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
		public final int group;
		public Beam(BeamVertex v1, BeamVertex v2, int group) {
			this.v1 = v1;
			this.v2 = v2;
			this.group = group;
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

		public double lengthSqr()
		{
			return v1.vertex.sqrDistance3D(v2.vertex);
		}
	}

	private BeamVertex createBeamVertex(Vertex v, Map<Vertex, BeamVertex> map)
	{
		BeamVertex bv = map.get(v);
		if(bv == null)
		{
			bv = new BeamVertex(v);
			map.put(v, bv);	
		}
		return bv;
	}

	private Map<Integer, Collection<Beam>> indexify(Mesh mesh)
	{
		List<Vertex> beams = mesh.getBeams();
		int nbBeams = beams.size() / 2;
		Map<Vertex, BeamVertex> verticeMap = HashFactory.createMap();
		Map<Integer, Collection<Beam>> beamMap = HashFactory.createMap();
		for(int i = 0; i<nbBeams; i++)
		{
			BeamVertex v1 = createBeamVertex(beams.get(i*2), verticeMap);
			BeamVertex v2 = createBeamVertex(beams.get(i*2+1), verticeMap);
			int group = ignoreGroups ? -1 : mesh.getBeamGroup(i);
			Beam beam = new Beam(v1, v2, group);
			v1.add(beam);
			v2.add(beam);
			Collection<Beam> beamSet = beamMap.get(group);
			if(beamSet == null)
			{
				beamSet = HashFactory.createSet();
				beamMap.put(group, beamSet);
			}
			beamSet.add(beam);
		}
		return beamMap;
	}

	/**
	 * Create a polyline
	 * @param startV start vertex of the polyline
	 * @param start first beam of the polyline
	 * @param polyline the created polyline as a list of vertex
	 * @param beams the created polyline as a list of beams
	 */
	private static void createPolyline(BeamVertex startV, Beam start,
		List<Vertex> polyline, List<Beam> beams, Set<BeamVertex> polylineEnds)
	{
		polyline.add(startV.vertex);
		BeamVertex cv = start.getOther(startV);
		Beam cb = start;
		beams.add(start);
		while(cv != startV && !polylineEnds.contains(cv) )
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

	/** Create polylines without angle constraints */
	public PolylineFactory(Mesh mesh) {
		this(mesh, -1.0, 0);
	}

	public PolylineFactory(Mesh mesh, double angle, double smallBeams) {
		this(mesh, angle, smallBeams, false);
	}
	/**
	 * @param mesh
	 * @param angle Ridge limit angle in degrees. Polylines won't contains angle
	 *   smaller than this value.
	 * @param smallBeams beams smaller than this value will be ignore when
	 *   calculating the smooth criteria
	 */
	public PolylineFactory(Mesh mesh, double angle, double smallBeams, boolean ignoreGroups) {
		this.ignoreGroups = ignoreGroups;
		angle = Math.cos(Math.toRadians(angle));
		Map<Integer, Collection<Beam>> beamMap = indexify(mesh);
		Set<BeamVertex> polylineEnds = HashFactory.createSet();
		ArrayList<Beam> polylineB = new ArrayList<Beam>();
		for(Entry<Integer, Collection<Beam>> e:beamMap.entrySet())
		{
			int group = e.getKey();
			Collection<List<Vertex>> polylines = new ArrayList<List<Vertex>>();
			put(group, polylines);
			Collection<Beam> beamSet = e.getValue();
			polylineEnds.clear();
			for(Beam b:beamSet)
			{
				if(b.v1.isPolylineEnd(angle))
					polylineEnds.add(b.v1);
				if(b.v2.isPolylineEnd(angle))
					polylineEnds.add(b.v2);
			}
			filterSmall(polylineEnds, smallBeams * smallBeams);

			//The first iteration is for polyline ends detected by isPolylineEnd.
			//Following iterations are for smooth polylines
			do
			{
				for(BeamVertex bv:polylineEnds)
				{
					for(Beam startBeam:bv)
					{
						if(beamSet.contains(startBeam))
						{
							ArrayList<Vertex> polyline = new ArrayList<Vertex>();
							polylineB.clear();
							createPolyline(bv, startBeam, polyline, polylineB, polylineEnds);
							polylines.add(polyline);
							beamSet.removeAll(polylineB);
						}
					}
				}
				polylineEnds.clear();
				// Beams which are in smooth loops are not detected by
				// isPolylineEnd. At this step beamSet should only contains such
				// beams, so any vertex could be concidered as a polyline end.
				if(!beamSet.isEmpty())
					polylineEnds.add(beamSet.iterator().next().v1);
			}
			while(!polylineEnds.isEmpty());
		}
	}
}
