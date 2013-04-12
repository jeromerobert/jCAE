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
 * (C) Copyright 2013, by EADS France
 */

package org.jcae.mesh.amibe.algos3d;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.metrics.MetricSupport;
import org.jcae.mesh.amibe.projection.MeshLiaison;
import org.jcae.mesh.amibe.projection.TriangleKdTree;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;

/**
 * Insert beams on the border of groups.
 * The inserted half edges are then tagged as immutable
 * @author Jerome Robert
 */
public class BeamInsertion {
	private static class BorderTriangleIterator implements Iterator<Triangle>
	{
		private final Iterator<Triangle> delegate;
		private Triangle current;
		public BorderTriangleIterator(Mesh mesh) {
			this.delegate = mesh.getTriangles().iterator();
		}

		private void nextImpl()
		{
			if(current == null)
			{
				while((current == null ||
					!(current.hasAttributes(AbstractHalfEdge.BOUNDARY) ||
					current.hasAttributes(AbstractHalfEdge.NONMANIFOLD))) &&
					delegate.hasNext())
					current = delegate.next();
			}
		}
		public boolean hasNext() {
			nextImpl();
			return delegate.hasNext();
		}

		public Triangle next() {
			nextImpl();
			Triangle toReturn = current;
			current = null;
			return toReturn;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private final Mesh mesh;
	private final TriangleKdTree kdTree;
	private final double[] vector1 = new double[3], vector2 = new double[3];
	private final VertexSwapper swapper;
	//only for tolerance
	private final MetricSupport.AnalyticMetricInterface metric;
	public BeamInsertion(final Mesh mesh, final double edgeSize)
	{
		this(mesh, new MetricSupport.AnalyticMetricInterface() {
			public double getTargetSize(double x, double y, double z,
				int groupId) {
				return edgeSize;
			}

			public double getTargetSizeTopo(Mesh mesh, Vertex v) {
				return edgeSize;
			}
		});
	}

	public BeamInsertion(final Mesh mesh, MetricSupport.AnalyticMetricInterface metric)
	{
		this.mesh = mesh;
		kdTree = new TriangleKdTree(new Iterable<Triangle>() {
			public Iterator<Triangle> iterator() {
				return new BorderTriangleIterator(mesh);
			}
		});
		this.metric = metric;
		swapper = new VertexSwapper(mesh);
		swapper.setKdTree(kdTree);
	}

	/** Insert a set of beams from binary files */
	public void insert(String vertexFile, String beamFile) throws IOException
	{
		FileChannel vertexChannel = new FileInputStream(vertexFile).getChannel();
		ByteBuffer bb = ByteBuffer.allocate((int)vertexChannel.size());
		bb.order(ByteOrder.nativeOrder());
		vertexChannel.read(bb);
		bb.rewind();
		DoubleBuffer vertexBuffer = bb.asDoubleBuffer();

		FileChannel beamChannel = new FileInputStream(beamFile).getChannel();
		bb = ByteBuffer.allocate((int)beamChannel.size());
		bb.order(ByteOrder.nativeOrder());
		beamChannel.read(bb);
		bb.rewind();
		IntBuffer beamBuffer = bb.asIntBuffer();
		int nbBeams = beamBuffer.capacity() / 2;
		for(int i = 0; i < nbBeams; i++)
		{
			int i1 = beamBuffer.get();
			int i2 = beamBuffer.get();
			assert i1 != i2: "Beam number "+i+" on "+nbBeams+" is degenerated: "+i1+"-"+i2;
			Vertex v1 = createVertex(i1, vertexBuffer);
			Vertex v2 = createVertex(i2, vertexBuffer);
			setImmutable(insert(v1, v2));
		}
	}

	private static void setImmutable(AbstractHalfEdge edge)
	{
		if(edge.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
		{
			Iterator<AbstractHalfEdge> it = edge.fanIterator();
			while(it.hasNext())
			{
				AbstractHalfEdge e = it.next();
				e.setAttributes(AbstractHalfEdge.IMMUTABLE);
				e.sym().setAttributes(AbstractHalfEdge.IMMUTABLE);
			}
		}
		else
		{
			edge.setAttributes(AbstractHalfEdge.IMMUTABLE);
			edge.sym().setAttributes(AbstractHalfEdge.IMMUTABLE);
		}
	}
	private Vertex createVertex(int i, DoubleBuffer vertices)
	{
		int k = 3 * i;
		double x = vertices.get(k++);
		double y = vertices.get(k++);
		double z = vertices.get(k);
		return mesh.createVertex(x, y, z);
	}

	/** Insert v1-v2 as an AbstractHalfEdge and return it */
	public AbstractHalfEdge insert(Vertex v1, Vertex v2)
	{
		v1 = insert(v1);
		v2 = insert(v2);
		assert v1 != v2;
		v2.sub(v1, vector1);
		AbstractHalfEdge toCollapse = nextEdge(v1, vector1, v1);
		while(toCollapse.destination() != v2)
		{
			if(toCollapse.hasAttributes(AbstractHalfEdge.OUTER))
				toCollapse = toCollapse.sym();
			assert !toCollapse.hasAttributes(AbstractHalfEdge.OUTER): toCollapse;
			assert !toCollapse.hasAttributes(AbstractHalfEdge.IMMUTABLE): toCollapse;
			Vertex target = v1;
			if(!mesh.canCollapseEdge(toCollapse, v1))
			{
				toCollapse = nextEdge(toCollapse.origin(), vector1, toCollapse.origin());
				target = toCollapse.destination();
				if(toCollapse.hasAttributes(AbstractHalfEdge.OUTER))
					toCollapse = toCollapse.sym();
				assert mesh.canCollapseEdge(toCollapse, target):
					"Cannot collapse "+toCollapse+" to "+target;
			}
			removeFromKdTree(toCollapse);
			mesh.edgeCollapse(toCollapse, target);
			toCollapse = nextEdge(v1, vector1, v1);
		}
		swapper.swap(v1);
		swapper.swap(v2);
		return toCollapse;
	}

	private AbstractHalfEdge nextEdge(Vertex v, double[] direction, Vertex notDirection)
	{
		Iterator<AbstractHalfEdge> it = v.getNeighbourIteratorAbstractHalfEdge();
		double bestDot = Double.NEGATIVE_INFINITY;
		AbstractHalfEdge bestEdge = null;
		while(it.hasNext())
		{
			AbstractHalfEdge e = it.next();
			if((e.hasAttributes(AbstractHalfEdge.NONMANIFOLD) || e.hasAttributes(
				AbstractHalfEdge.BOUNDARY)) && notDirection != e.destination())
			{
				e.destination().sub(e.origin(), vector2);
				double dot = Matrix3D.prodSca(vector1, vector2);
				if(dot > bestDot)
				{
					bestDot = dot;
					bestEdge = e;
				}
			}
		}
		return bestEdge;
	}

	private void addTriangleToKdTree(AbstractHalfEdge edge)
	{
		if(!edge.hasAttributes(AbstractHalfEdge.OUTER))
			kdTree.addTriangle(edge.getTri());
	}

	private void removeFromKdTree(AbstractHalfEdge edge)
	{
		if(edge.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
		{
			Iterator<AbstractHalfEdge> it = edge.fanIterator();
			while(it.hasNext())
			{
				AbstractHalfEdge e = it.next();
				if(!e.hasAttributes(AbstractHalfEdge.OUTER))
					kdTree.remove(e.getTri());
			}
		}
		else
		{
			if(!edge.hasAttributes(AbstractHalfEdge.OUTER))
				kdTree.remove(edge.getTri());
			if(!edge.sym().hasAttributes(AbstractHalfEdge.OUTER))
			kdTree.remove(edge.sym().getTri());
		}
	}

	private Vertex insert(Vertex v)
	{
		Triangle t = kdTree.getClosestTriangle(v, null, -1);
		AbstractHalfEdge e = MeshLiaison.findNearestEdge(v, t);
		double localMetric = metric.getTargetSize(
			v.getX(), v.getY(), v.getZ(), -1);
		double localMetric2 = localMetric * localMetric;
		double tol2 = localMetric2 / (40 * 40);
		if(v.sqrDistance3D(e.origin()) < tol2)
			return e.origin();

		if(v.sqrDistance3D(e.destination()) < tol2)
			return e.destination();
		removeFromKdTree(e);
		mesh.vertexSplit(e, v);
		AbstractHalfEdge newEdge = e.next().sym().next();
		if(e.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
		{
			Iterator<AbstractHalfEdge> it = e.fanIterator();
			while(it.hasNext())
				addTriangleToKdTree(it.next());
			it = newEdge.fanIterator();
			while(it.hasNext())
				addTriangleToKdTree(it.next());
		}
		else
		{
			addTriangleToKdTree(e);
			addTriangleToKdTree(newEdge);
		}
		return v;
	}

	public static void main(final String[] args) {
		try {
			Mesh mesh = new Mesh(MeshTraitsBuilder.getDefault3D());
			MeshReader.readObject3D(mesh, "/tmp/AMIBE/Container.amibe");
			mesh.buildGroupBoundaries();
			MeshLiaison liaison = MeshLiaison.create(mesh);
			BeamInsertion bi = new BeamInsertion(liaison.getMesh(), 200);
			AbstractHalfEdge e = bi.insert(
				mesh.createVertex(1000, -500, 0),
				mesh.createVertex(1000, 100, 0));
			setImmutable(e);
			bi = new BeamInsertion(liaison.getMesh(), 200);
			e = bi.insert(
				mesh.createVertex(500, 3000, 0),
				mesh.createVertex(-500, 3000, 0));
			setImmutable(e);
			RemeshSkeleton rs = new RemeshSkeleton(liaison, -2, 1.0, 300);
			rs.compute();
			MeshWriter.writeObject3D(liaison.getMesh(), "/tmp/AMIBE/c2.amibe", null);
		} catch (IOException ex) {
			Logger.getLogger(BeamInsertion.class.getName()).log(Level.SEVERE,
				null, ex);
		}
	}
}
