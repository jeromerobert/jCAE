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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.MetricSupport.AnalyticMetricInterface;
import org.jcae.mesh.amibe.projection.MeshLiaison;
import org.jcae.mesh.amibe.util.HashFactory;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;

/**
 *
 * @author Jerome Robert
 */
public class RemeshSkeleton {
	private final Mesh mesh;
	private final double angle;
	/** tolerance for point insertion */
	private final double tolerance;
	private final AnalyticMetricInterface metric;
	private final MeshLiaison liaison;
	private final VertexSwapper vertexSwapper;
	private double swapVolume;

	public RemeshSkeleton(MeshLiaison liaison, double angle, double tolerance,
		final double size) {
		this(liaison, angle, tolerance, new AnalyticMetricInterface() {

			public double getTargetSize(double x, double y, double z, int groupId) {
				return size;
			}

			public double getTargetSizeTopo(Mesh mesh, Vertex v) {
				return size;
			}
		});
	}

	public RemeshSkeleton(MeshLiaison liaison, double angle, double tolerance,
		AnalyticMetricInterface metric) {
		this.liaison = liaison;
		this.mesh = liaison.getMesh();
		this.angle = angle;
		this.tolerance = tolerance * tolerance;
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
		throw new NoSuchElementException(v1+" "+v2);
	}

	private static double dot(double[] v1, double[] v2)
	{
		return v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2];
	}

	private static double getAbscissa(Vertex v, AbstractHalfEdge edge)
	{
		double[] diff = new double[3];
		double[] seg = new double[3];
		v.sub(edge.origin(), diff);
		edge.destination().sub(edge.origin(), seg);
		return dot(seg, diff) / edge.origin().sqrDistance3D(edge.destination());
	}

	public void compute()
	{
		Skeleton skeleton = new Skeleton(mesh, angle);
		main: for(List<Vertex> polyline: skeleton.getPolylinesVertices())
		{
			RemeshPolyline rp = new RemeshPolyline(mesh, polyline, metric);
			rp.setBuildBackgroundLink(true);
			List<Vertex> toInsert = rp.compute();
			List<Integer> bgLink = rp.getBackgroundLink();
			int k = 0;
			final Set<Vertex> toKeep = HashFactory.createSet(toInsert);
			ArrayList<AbstractHalfEdge> edgeIndex = new ArrayList<AbstractHalfEdge>(polyline.size()-1);
			for(int i = 0; i < polyline.size() - 1; i++)
			{
				AbstractHalfEdge e = getEdge(polyline, i);
				if(e.hasAttributes(AbstractHalfEdge.IMMUTABLE))
					continue main;
				edgeIndex.add(e);
			}

			for(Vertex v:toInsert)
			{
				int segId = bgLink.get(k++);
				AbstractHalfEdge toSplit = edgeIndex.get(segId);
				double od = v.sqrDistance3D(toSplit.origin());
				double dd = v.sqrDistance3D(toSplit.destination());
				if(od <= dd && od <= tolerance)
				{
					toKeep.remove(v);
					toKeep.add(toSplit.origin());
				}
				else if(dd <= tolerance)
				{
					toKeep.remove(v);
					toKeep.add(toSplit.destination());
				}
				else
				{
					Vertex oldDestination = toSplit.destination();
					mesh.vertexSplit(toSplit, v);
					liaison.addVertex(v, toSplit.getTri());
					//TODO this will be slow as as toSplit.getTri() may be far
					//from the wanted triangle so we will loop on all triangles
					liaison.move(v, v, true);
					AbstractHalfEdge e = getEdge(v, oldDestination);
					edgeIndex.set(segId, e);
					double m2 = metric.getTargetSizeTopo(mesh, v);
					swapVolume = m2 * m2 * m2 / 64;
					vertexSwapper.swap(v);
				}
			}
			for(Vertex v:toKeep)
				v.setMutable(false);
			/*HashSet<Vertex> toCollapse = new HashSet<Vertex>(polyline);
			toCollapse.removeAll(toKeep);
			int nbNotCollapsed = collapse(toKeep, toCollapse);
			if(nbNotCollapsed > 0)
				System.err.println(nbNotCollapsed+" were not "+nbNotCollapsed);*/
		}
	}

	private int collapse(final Set<Vertex> toKeep, Set<Vertex> toCollapse)
	{
		boolean changed = true;
		while(changed)
		{
			//check(toCollapse);
			changed = false;
			Iterator<Vertex> itToCollapse = toCollapse.iterator();
			while(itToCollapse.hasNext())
			{
				Vertex v = itToCollapse.next();
				Iterator<AbstractHalfEdge> it = v.getNeighbourIteratorAbstractHalfEdge();
				while(it.hasNext())
				{
					AbstractHalfEdge edge = it.next();
					Vertex dest = edge.destination();
					if(toKeep.contains(dest) && mesh.canCollapseEdge(edge, dest))
					{
						mesh.edgeCollapse(edge, dest);
						liaison.removeVertex(v);
						changed = true;
						itToCollapse.remove();
						break;
					}
				}
			}
		}
		return toCollapse.size();
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
