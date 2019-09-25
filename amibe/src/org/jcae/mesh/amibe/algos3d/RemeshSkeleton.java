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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.MetricSupport.AnalyticMetricInterface;
import org.jcae.mesh.amibe.projection.MeshLiaison;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;

/**
 *
 * @author Jerome Robert
 */
public class RemeshSkeleton {
	private final Mesh mesh;
	private final double angle;
	/**
	 * Factor to apply to the square of distance metric to get the square of the
	 * distance tolerance for point insertion
	 */
	private final double toleranceFactor;
	private final AnalyticMetricInterface metric;
	private final MeshLiaison liaison;
	private final VertexSwapper vertexSwapper;
	private double swapVolume;

	public RemeshSkeleton(MeshLiaison liaison, double angle, double tolerance,
		final double size) {
		this(liaison, angle, new AnalyticMetricInterface() {

			public double getTargetSize(double x, double y, double z, int groupId) {
				return size;
			}

			public double getTargetSizeTopo(Mesh mesh, Vertex v) {
				return size;
			}
		}, tolerance / size);
	}

	public RemeshSkeleton(MeshLiaison liaison, double angle,
		AnalyticMetricInterface metric, double toleranceFactor) {
		this.liaison = liaison;
		this.mesh = liaison.getMesh();
		this.angle = angle;
		this.toleranceFactor = toleranceFactor * toleranceFactor;
		this.metric = metric;
		vertexSwapper = new VertexSwapper(liaison)
		{
			@Override
			protected boolean isQualityImproved(AbstractHalfEdge.Quality quality) {
				return super.isQualityImproved(quality) && quality.swappedVolume() < swapVolume;
			}
		};
	}

	private AbstractHalfEdge getEdge(List<Vertex> polyline, int segId)
	{
		return getEdge(polyline.get(segId), polyline.get(segId+1));
	}

	private AbstractHalfEdge getEdge(Vertex v1, Vertex v2)
	{
		Iterator<AbstractHalfEdge> it = v1.getNeighbourIteratorAbstractHalfEdge();
		while(it.hasNext())
		{
			AbstractHalfEdge e = it.next();
			if(e.destination() == v2 && !e.hasAttributes(AbstractHalfEdge.OUTER))
				return e;
		}
		//TODO in some cases v1.getNeighbourIteratorAbstractHalfEdge does not
		//return all HE. Check why. Star with vertex with more than one outer
		//triangle (more than one group/group frontier).
		it = v2.getNeighbourIteratorAbstractHalfEdge();
		while(it.hasNext())
		{
			AbstractHalfEdge e = it.next();
			if(e.destination() == v1 && !e.sym().hasAttributes(AbstractHalfEdge.OUTER))
				return e.sym();
		}
		throw new NoSuchElementException(v1+" "+v2+" "+v1.getLink()+" "+v2.getLink());
	}

	public void compute()
	{
		Skeleton skeleton = new Skeleton(mesh, angle);
		EdgesCollapserNG edgeCollapser = new EdgesCollapserNG(mesh);
		main: for(List<Vertex> polyline: skeleton.getPolylinesVertices())
		{
			int[] groups = null;
			RemeshPolyline rp = new RemeshPolyline(mesh, polyline, metric);
			rp.setBuildBackgroundLink(true);
			List<Vertex> toInsert = rp.compute();
			if(polyline.size() == 2 && toInsert.size() == 2)
				continue;
			List<Integer> bgLink = rp.getBackgroundLink();

			ArrayList<AbstractHalfEdge> edgeIndex = new ArrayList<AbstractHalfEdge>(polyline.size()-1);
			for(int i = 0; i < polyline.size() - 1; i++)
			{
				AbstractHalfEdge e = getEdge(polyline, i);
				if(e.hasAttributes(AbstractHalfEdge.IMMUTABLE))
					continue main;
				edgeIndex.add(e);
			}

			if(toInsert.size() == 2)
			{
				int n = Math.max(1, polyline.size() / 2);
				if(toInsert.get(0) != toInsert.get(1))
					edgeCollapser.collapse(polyline.get(0), polyline.get(polyline.size() - 1), polyline.get(n));
				continue main;
			}

			for(int k = 0; k < toInsert.size(); k++)
			{
				Vertex v = toInsert.get(k);
				double m2 = metric.getTargetSize(v.getX(), v.getY(), v.getZ(), -1);
				double tolerance = m2 * m2 * toleranceFactor;
				int segId = bgLink.get(k);
				AbstractHalfEdge toSplit = edgeIndex.get(segId);
				double od = v.sqrDistance3D(toSplit.origin());
				double dd = v.sqrDistance3D(toSplit.destination());
				if(od <= dd && od <= tolerance)
				{
					toInsert.set(k, toSplit.origin());
				}
				else if(dd <= tolerance)
				{
					toInsert.set(k, toSplit.destination());
				}
				else
				{
					Vertex oldDestination = toSplit.destination();
					mesh.vertexSplit(toSplit, v);
					liaison.addVertex(v, toSplit.getTri());
					if(groups == null) {
						// Lazily get the list of groups which is the same for all edges of this polyline
						groups = getGroups(toSplit);
					} else {
						assert(Arrays.equals(groups, getGroups(toSplit)));
					}
					boolean r = liaison.moveToClosestEdge(v, groups);
					assert(r);
					AbstractHalfEdge e = getEdge(v, oldDestination);
					edgeIndex.set(segId, e);
					swapVolume = m2 * m2 * m2 / 64;
					vertexSwapper.swap(v);
				}
			}

			for(int k = 0; k < toInsert.size() - 1; k++)
				edgeCollapser.collapse(toInsert.get(k), toInsert.get(k+1), null);

			for(Vertex v:toInsert)
				v.setMutable(false);
		}
	}

	private static int[] getGroups(AbstractHalfEdge edge) {
		Iterator<AbstractHalfEdge> it = edge.fanIterator();
		List<Integer> r = new ArrayList<Integer>();
		while(it.hasNext()) {
			r.add(it.next().getTri().getGroupId());
		}
		// convert the ArrayList to int[]
		int[] rr = new int[r.size()];
		for(int i = 0; i < rr.length; i++)
			rr[i] = r.get(i);
		return rr;
	}
	public static void main(final String[] args) {
		try {
			Mesh mesh = new Mesh();
			MeshReader.readObject3D(mesh, "/home/robert/ast-a319-neo/demo-anabelle/demo/amibe.dir");
			MeshLiaison liaison = MeshLiaison.create(mesh);
			liaison.getMesh().buildGroupBoundaries();
			new RemeshSkeleton(liaison, 0, 1.0, 300).compute();
			MeshWriter.writeObject3D(liaison.getMesh(), "/home/robert/ast-a319-neo/demo-anabelle/demo/amibe2.amibe", null);
		} catch (IOException ex) {
			Logger.getLogger(RemeshSkeleton.class.getName()).log(Level.SEVERE,
				null, ex);
		}
	}
}
