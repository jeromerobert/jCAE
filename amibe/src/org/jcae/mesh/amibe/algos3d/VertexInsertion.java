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

import gnu.trove.impl.PrimeFinder;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.channels.FileChannel;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge.Quality;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.TriangleHE;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Location;
import org.jcae.mesh.amibe.metrics.MetricSupport.AnalyticMetricInterface;
import org.jcae.mesh.amibe.projection.MeshLiaison;
import org.jcae.mesh.amibe.projection.TriangleKdTree;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MultiDoubleFileReader;

/**
 *
 * @author Jerome Robert
 */
public class VertexInsertion {
	private final MeshLiaison liaison;
	private final Mesh mesh;
	private final static Logger LOGGER = Logger.getLogger(VertexInsertion.class.getName());
	private final TriangleKdTree kdTree;
	/** triangles added when inserting a point in the middle of a triangle */
	private final Collection<Triangle> tmp = new ArrayList<Triangle>(3);
	private Collection<Vertex> notMutableInserted, mutableInserted;
	private final AnalyticMetricInterface metric;
	private final VertexSwapper swapper;
	private int swapperGroup;
	private double swapperDeflection, swapperVolume;
	public VertexInsertion(MeshLiaison liaison, final double size) {
		this(liaison.getMesh(), liaison, size);
	}

	public VertexInsertion(Mesh mesh, final double size) {
		this(mesh, null, size);
	}

	private VertexInsertion(Mesh mesh, MeshLiaison liaison, final double size) {
		this(mesh, liaison, new AnalyticMetricInterface() {

			public double getTargetSize(double x, double y, double z,
				int groupId) {
				return size;
			}

			public double getTargetSizeTopo(Mesh mesh, Vertex v) {
				return size;
			}
		});
	}

	public VertexInsertion(MeshLiaison liaison, AnalyticMetricInterface metric) {
		this(liaison.getMesh(), liaison, metric);
	}

	private VertexInsertion(Mesh mesh, MeshLiaison liaison, AnalyticMetricInterface metric) {
		this.liaison = liaison;
		this.mesh = mesh;
		LOGGER.info("Start creating kd-tree");
		kdTree = new TriangleKdTree(mesh);
		LOGGER.info(kdTree.stats());
		this.metric = metric;
		if(liaison == null)
			swapper = new VertexSwapper(mesh)
			{
				@Override
				protected boolean isQualityImproved(Quality quality) {
					return super.isQualityImproved(quality) &&
						quality.swappedVolume() < swapperVolume;
				}
			};
		else
			swapper = new VertexSwapper(liaison)
			{
				@Override
				protected boolean isQualityImproved(Quality quality) {
					return super.isQualityImproved(quality) &&
						quality.sqrSwappedDeflection(swapperGroup) < swapperDeflection;
				}
			};
		swapper.setKdTree(kdTree);
	}

	public void insertNodes(String fileName, int group) throws IOException
	{
		FileChannel vertexChannel = new FileInputStream(fileName).getChannel();
		ByteBuffer bb = ByteBuffer.allocate((int)vertexChannel.size());
		bb.order(ByteOrder.nativeOrder());
		vertexChannel.read(bb);
		bb.rewind();
		insertNodes(bb.asDoubleBuffer(), group);
	}

	public void insertNodes(final DoubleBuffer vertices, int group)
	{
		final int size = vertices.limit() / 3;
		AbstractList<Vertex> l = new AbstractList<Vertex>() {

			@Override
			public Vertex get(int index) {
				int n = index * 3;
				return new Vertex(null, vertices.get(n), vertices.get(n + 1),
					vertices.get(n + 2));
			}

			@Override
			public int size() {
				return size;
			}
		};
		insertNodes(l, group);
	}

	/**
	 * Insert vertices from another mesh
	 * @param vertices the vertices to insert
	 * @param group insert vertices only in triangles of this group. Use -1
	 * to try to insert in all groups
	 * @param liaisonError the maximal acceptable distance between the
	 * inserted point and it's projection.
	 * @param insertionTol Do not the insert point if a point  already exist at
	 * a distance lower than this value
	 */
	public void insertNodes(List<Vertex> vertices, int group)
	{
		if(vertices.isEmpty())
		{
			mutableInserted = Collections.emptyList();
			notMutableInserted = Collections.emptyList();
			return;
		}
		Location projection = new Location();
		mutableInserted = new ArrayList<Vertex>(vertices.size());
		notMutableInserted = new ArrayList<Vertex>();
		int n = vertices.size();
		int step = PrimeFinder.nextPrime(n+1);
		int k = 0;
		int nbInserted = 0;
		swapper.setGroup(group);
		main: for(int i = 0; i < n; i++)
		{
			k = (k + step) % n;
			Vertex v = vertices.get(k);
			double localMetric = metric.getTargetSize(
				v.getX(), v.getY(), v.getZ(), group);
			double localMetric2 = localMetric * localMetric;
			double tol2 = localMetric2 / (40 * 40);
			TriangleHE t = (TriangleHE) kdTree.getClosestTriangle(
				v, projection, group);
			if(t == null)
				throw new NullPointerException("Cannot find projection for "+v);
			if(liaison != null)
				liaison.move(v, projection, group, true);
			for(int iv = 0; iv < 3; iv++)
			{
				Vertex tv = t.getV(iv);
				if(tv.sqrDistance3D(v) < tol2)
				{
					if(tv.isMutable())
						mutableInserted.add(tv);
					else
						notMutableInserted.add(tv);
					continue main;
				}
			}

			// We could check that we are close from an edge but we don't
			// because swaping will properly handle this case
			t.split(mesh, v, tmp);
			kdTree.replace(t, tmp);
			tmp.clear();
			mutableInserted.add(v);
			swapperGroup = group;
			swapperDeflection = localMetric2 / 2;
			swapperVolume = localMetric2 * localMetric;
			swapper.swap(v);
			nbInserted ++;
		}
		LOGGER.info(nbInserted+" / "+vertices.size()+" inserted nodes on group "+group);
	}

	public Collection<Vertex> getNotMutableInserted() {
		return notMutableInserted;
	}

	public Collection<Vertex> getMutableInserted() {
		return mutableInserted;
	}

	/**
	 * Try to insert a vertex into an edge of the given triangle
	 * @param t
	 * @param tol2
	 * @param v
	 * @return The number of created triangles
	 */
	private boolean edgeSplit(Triangle t, double tol2, Vertex v)
	{
		AbstractHalfEdge e = t.getAbstractHalfEdge();
		for(int i = 0; i < 3; i++)
		{
			if(MeshLiaison.sqrOrthoDistance(v, e) < tol2)
			{
				if(e.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
				{
					Iterator<AbstractHalfEdge> rit = e.fanIterator();
					while(rit.hasNext())
						kdTree.remove(rit.next().getTri());
				}
				else
				{
					kdTree.remove(t);
					kdTree.remove(e.sym().getTri());
				}
				mesh.vertexSplit(e, v);
				Iterator<Triangle> it = v.getNeighbourIteratorTriangle();
				while(it.hasNext())
					kdTree.addTriangle(it.next());
				return true;
			}
			e = e.next();
			assert e != null;
		}
		return false;
	}

	public static void main(final String[] args) {
		try {
			MultiDoubleFileReader mdf = new MultiDoubleFileReader("/tmp/tmps2zXh3/nodes.bin");
			Mesh mesh = new Mesh();
			MeshReader.readObject3D(mesh, "/tmp/debug2.zebra/amibe.dir");
			MeshLiaison ml = MeshLiaison.create(mesh);
			ml.getMesh().buildGroupBoundaries();
			VertexInsertion vi = new VertexInsertion(ml, 300);
			for(int gId = 1; gId <= ml.getMesh().getNumberOfGroups(); gId++)
			{
				DoubleBuffer vs = mdf.next();
				if(gId == 65)
					vi.insertNodes(vs, gId);
			}
		} catch (Exception ex) {
			Logger.getLogger(VertexInsertion.class.getName()).log(Level.SEVERE,
				null, ex);
		}
	}
}
