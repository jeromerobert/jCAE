/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2009,2010,2011, by EADS France

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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.amibe.algos3d;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.ds.Skeleton;
import org.jcae.mesh.amibe.metrics.KdTree;
import org.jcae.mesh.amibe.projection.MeshLiaison;
import org.jcae.mesh.amibe.ds.HalfEdge;
import org.jcae.mesh.amibe.metrics.EuclidianMetric3D;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.metrics.Metric;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;
import org.jcae.mesh.xmldata.DoubleFileReader;
import org.jcae.mesh.xmldata.PrimitiveFileReaderFactory;

import gnu.trove.PrimeFinder;
import gnu.trove.TIntObjectHashMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Remesh an existing mesh.
 *
 * See org.jcae.mesh.amibe.algos2d.Insertion
 * @author Denis Barbier
 */
public class Remesh
{
	private final static Logger LOGGER = Logger.getLogger(Remesh.class.getName());
	private static final double ONE_PLUS_SQRT2 = 1.0 + Math.sqrt(2.0);
	private int progressBarStatus = 10000;
	private final Mesh mesh;
	private final MeshLiaison liaison;
	private final Skeleton skeleton;
	// Octree to find nearest Vertex in current mesh
	private final KdTree<Vertex> kdTree;
	private Map<Vertex, Vertex> neighborBgMap = new HashMap<Vertex, Vertex>();
	private DoubleFileReader dfrMetrics;
	private final double minlen;
	private final double maxlen;
	// useful to see if addCandidatePoints() does its job
	private int nrInterpolations;
	private int nrFailedInterpolations;

	Map<Triangle, Collection<Vertex>> mapTriangleVertices = new HashMap<Triangle, Collection<Vertex>>();
	Map<Vertex, Triangle> surroundingTriangle = new HashMap<Vertex, Triangle>();

	private final boolean project;
	private final boolean hasRidges;
	// true if mesh has free edges, ridges or nonmanifold edges, false otherwise
	private final boolean hasFeatureEdges;
	private final double coplanarity;
	private final boolean allowNearNodes;
	private final boolean remeshOnlyFeatureEdges;
	private AnalyticMetricInterface analyticMetric = LATER_BINDING;
	private final Map<Vertex, EuclidianMetric3D> metrics;
	private static final AnalyticMetricInterface LATER_BINDING = new AnalyticMetricInterface() {
		public double getTargetSize(double x, double y, double z)
		{
			throw new RuntimeException();
		}
	};
	private TIntObjectHashMap<AnalyticMetricInterface> metricsPartitionMap = new TIntObjectHashMap<AnalyticMetricInterface>();

	public interface AnalyticMetricInterface
	{
		double getTargetSize(double x, double y, double z);
	}

	/**
	 * Creates a <code>Remesh</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to refine.
	 */
	@Deprecated
	public Remesh(Mesh m)
	{
		this(m, MeshTraitsBuilder.getDefault3D(), new HashMap<String, String>());
	}

	@Deprecated
	public Remesh(Mesh m, Map<String, String> opts)
	{
		this(m, MeshTraitsBuilder.getDefault3D(), opts);
	}

	/**
	 * Creates a <code>Remesh</code> instance.
	 *
	 * @param bgMesh  the <code>Mesh</code> instance to refine.
	 * @param options  map containing key-value pairs to modify algorithm
	 *        behaviour.  No options are available for now.
	 */
	private Remesh(final Mesh bgMesh, final MeshTraitsBuilder mtb, final Map<String, String> options)
	{
		this(new MeshLiaison(bgMesh, mtb), options);
	}

	public Remesh(final MeshLiaison liaison, final Map<String, String> options)
	{
		this(liaison.getMesh(), liaison, options);
	}

	private Remesh(final Mesh m, final MeshLiaison meshLiaison, final Map<String, String> options)
	{
		liaison = meshLiaison;
		mesh = m;
		double size = 0.0;
		double nearLengthRatio = 1.0 / Math.sqrt(2.0);
		boolean proj = false;
		boolean nearNodes = false;
		boolean onlyFeatureEdges = false;
		double copl = 0.8;
		Map<String, String> decimateOptions = new HashMap<String, String>();
		for (final Map.Entry<String, String> opt: options.entrySet())
		{
			final String key = opt.getKey();
			final String val = opt.getValue();
			if (key.equals("size"))
			{
				size = Double.valueOf(val).doubleValue();
				analyticMetric = null;
				dfrMetrics = null;
			}
			else if (key.equals("nearLengthRatio"))
			{
				nearLengthRatio = Double.valueOf(val).doubleValue();
			}
			else if (key.equals("coplanarity"))
			{
				copl = Double.valueOf(val).doubleValue();
			}
			else if (key.equals("decimateSize"))
			{
				decimateOptions.put("size", val);
			}
			else if (key.equals("decimateTarget"))
			{
				decimateOptions.put("maxtriangles", val);
			}
			else if (key.equals("metricsFile"))
			{
				PrimitiveFileReaderFactory pfrf = new PrimitiveFileReaderFactory();
				try {
					dfrMetrics = pfrf.getDoubleReader(new File(val));
				} catch (FileNotFoundException ex) {
					LOGGER.log(Level.SEVERE, null, ex);
				} catch (IOException ex) {
					LOGGER.log(Level.SEVERE, null, ex);
				}
				analyticMetric = null;
			}
			else if (key.equals("project"))
				proj = Boolean.valueOf(val).booleanValue();
			else if (key.equals("allowNearNodes"))
				nearNodes = Boolean.valueOf(val).booleanValue();
			else if (key.equals("features"))
				onlyFeatureEdges = Boolean.valueOf(val).booleanValue();
			else
				LOGGER.warning("Unknown option: "+key);
		}
		if (meshLiaison == null)
			mesh.buildRidges(copl);

		double targetSize = size;
		minlen = nearLengthRatio;
		maxlen = Math.sqrt(2.0);
		project = proj;
		coplanarity = copl;
		allowNearNodes = nearNodes;
		remeshOnlyFeatureEdges = onlyFeatureEdges;

		skeleton = new Skeleton(mesh);

		if (!decimateOptions.isEmpty())
		{
			new QEMDecimateHalfEdge(liaison, decimateOptions).compute();
		}

		boolean ridges = false;
		boolean features = false;
		for (Triangle f: mesh.getTriangles())
		{
			if (f.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			if (!ridges && f.hasAttributes(AbstractHalfEdge.SHARP))
				ridges = true;
			if (!features && f.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
				features = true;
		}
		hasRidges = ridges;
		hasFeatureEdges = ridges | features;

		Collection<Vertex> nodeset = mesh.getNodes();
		if (nodeset == null)
		{
			nodeset = new LinkedHashSet<Vertex>(mesh.getTriangles().size() / 2);
			for (Triangle f : mesh.getTriangles())
			{
				if (f.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
				nodeset.addAll(Arrays.asList(f.vertex));
			}
		}

		// Compute bounding box
		double [] bbox = new double[6];
		bbox[0] = bbox[1] = bbox[2] = Double.MAX_VALUE;
		bbox[3] = bbox[4] = bbox[5] = - (Double.MAX_VALUE / 2.0);
		for (Vertex v : nodeset)
		{
			double[] xyz = v.getUV();
			for (int k = 2; k >= 0; k--)
			{
				if (xyz[k] < bbox[k])
					bbox[k] = xyz[k];
				if (xyz[k] > bbox[3+k])
					bbox[3+k] = xyz[k];
			}
		}
		LOGGER.fine("Bounding box: lower("+bbox[0]+", "+bbox[1]+", "+bbox[2]+"), upper("+bbox[3]+", "+bbox[4]+", "+bbox[5]+")");
		kdTree = new KdTree<Vertex>(bbox);
		for (Vertex v : nodeset)
			kdTree.add(v);

		for (Vertex v : nodeset)
		{
			if (null == v.getLink())
				continue;
			Triangle t = liaison.getBackgroundTriangle(v);
			double d0 = v.sqrDistance3D(t.vertex[0]);
			double d1 = v.sqrDistance3D(t.vertex[1]);
			double d2 = v.sqrDistance3D(t.vertex[2]);
			if (d0 <= d1 && d0 <= d2)
				neighborBgMap.put(v, t.vertex[0]);
			else if (d1 <= d0 && d1 <= d2)
				neighborBgMap.put(v, t.vertex[1]);
			else
				neighborBgMap.put(v, t.vertex[2]);
		}

		// Arbitrary size: 2*initial number of nodes
		metrics = new HashMap<Vertex, EuclidianMetric3D>(2*nodeset.size());
		if (dfrMetrics != null)
		{
			try {
				for (Vertex v : nodeset)
					metrics.put(v, new EuclidianMetric3D(dfrMetrics.get(v.getLabel() - 1)));
			} catch (IOException ex) {
					LOGGER.log(Level.SEVERE, null, ex);
					throw new RuntimeException("Error when loading metrics map file");
			}
		}
		else if (targetSize > 0.0)
		{
			// If targetSize is 0.0, metrics will be set by calling setAnalyticMetric()
			// below.
			for (Vertex v : nodeset)
				metrics.put(v, new EuclidianMetric3D(targetSize));
		}
	}

	public void setAnalyticMetric(AnalyticMetricInterface m)
	{
		analyticMetric = m;
	}

	public void setAnalyticMetric(int groupId, AnalyticMetricInterface m)
	{
		metricsPartitionMap.put(groupId, m);
	}

	public final Mesh getOutputMesh()
	{
		return mesh;
	}

        // Can be extended by subclasses
	protected void afterSplitHook()
	{
	}

	// Can be extended by subclasses
	protected void afterSwapHook()
	{
	}

	// Can be extended by subclasses
	protected void afterIterationHook()
	{
	}

	private static boolean isInside(double[] pos, Triangle t)
	{
		double [][] temp = new double[4][3];
		double[] p0 = t.vertex[0].getUV();
		double[] p1 = t.vertex[1].getUV();
		double[] p2 = t.vertex[2].getUV();
		Matrix3D.computeNormal3D(p0, p1, p2, temp[0], temp[1], temp[2]);
		Matrix3D.computeNormal3D(p0, p1, pos, temp[0], temp[1], temp[3]);
		if (Matrix3D.prodSca(temp[2], temp[3]) < 0.0)
			return false;
		Matrix3D.computeNormal3D(p1, p2, pos, temp[0], temp[1], temp[3]);
		if (Matrix3D.prodSca(temp[2], temp[3]) < 0.0)
			return false;
		Matrix3D.computeNormal3D(p2, p0, pos, temp[0], temp[1], temp[3]);
		return Matrix3D.prodSca(temp[2], temp[3]) >= 0.0;
	}

	private static double interpolatedDistance(Vertex pt1, Metric m1, Vertex pt2, Metric m2)
	{
		assert m1 != null : "Metric null at point "+pt1;
		assert m2 != null : "Metric null at point "+pt2;
		double[] p1 = pt1.getUV();
		double[] p2 = pt2.getUV();
		double a = Math.sqrt(m1.distance2(p1, p2));
		double b = Math.sqrt(m2.distance2(p1, p2));
		// Linear interpolation:
		//double l = (2.0/3.0) * (a*a + a*b + b*b) / (a + b);
		// Geometric interpolation
		double l = Math.abs(a-b) < 1.e-6*(a+b) ? 0.5*(a+b) : (a - b)/Math.log(a/b);
		
		return l;
	}

	private Map<Triangle, Collection<Vertex>> collectVertices(AbstractHalfEdge ot)
	{
		Map <Triangle, Collection<Vertex>> verticesToDispatch = new HashMap<Triangle, Collection<Vertex>>();
		if (ot.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
		{
			for (Iterator<AbstractHalfEdge> itf = ot.fanIterator(); itf.hasNext(); )
			{
				Triangle t = itf.next().getTri();
				assert !t.hasAttributes(AbstractHalfEdge.OUTER);
				Collection<Vertex> prev = mapTriangleVertices.remove(t);
				if (prev != null)
				{
					verticesToDispatch.put(t, new ArrayList<Vertex>(prev));
					prev.clear();
				}
			}
		}
		else
		{
			Triangle t = ot.getTri();
			Collection<Vertex> prev = mapTriangleVertices.remove(t);
			if (prev != null)
			{
				verticesToDispatch.put(t, new ArrayList<Vertex>(prev));
				prev.clear();
			}
			if (!ot.hasAttributes(AbstractHalfEdge.BOUNDARY))
			{
				ot = ot.sym();
				t = ot.getTri();
				prev = mapTriangleVertices.remove(t);
				ot = ot.sym();
				if (prev != null)
				{
					verticesToDispatch.put(t, new ArrayList<Vertex>(prev));
					prev.clear();
				}
			}
		}
		return verticesToDispatch;
	}

	private void dispatchVertices(Vertex newVertex, Map<Triangle, Collection<Vertex>> verticesToDispatch)
	{
		for (Map.Entry<Triangle, Collection<Vertex>> entry : verticesToDispatch.entrySet())
		{
			Triangle t = entry.getKey();
			for (Vertex v : entry.getValue())
			{
				surroundingTriangle.remove(v);
				if (v == newVertex)
				{
					// There is no need to insert this vertex
					continue;
				}
				Triangle vT = MeshLiaison.findSurroundingInAdjacentTriangles(v, t);
				surroundingTriangle.put(v, vT);
				Collection<Vertex> c = mapTriangleVertices.get(vT);
				if (c == null)
				{
					c = new ArrayList<Vertex>();
					mapTriangleVertices.put(vT, c);
				}
				c.add(v);
			}
		}
	}

	public final Remesh compute()
	{
		LOGGER.info("Run "+getClass().getName());
		mesh.getTrace().println("# Begin Remesh");

		if (analyticMetric != null || !metricsPartitionMap.isEmpty())
		{
			for (Triangle t : mesh.getTriangles())
			{
				if (!t.isReadable())
					continue;
				AnalyticMetricInterface metric = metricsPartitionMap.get(t.getGroupId());
				if (metric == null)
					metric = analyticMetric;
				if (metric.equals(LATER_BINDING))
					throw new RuntimeException("Cannot determine metrics, either set 'size' or 'metricsMap' arguments, or call Remesh.setAnalyticMetric()");
				for (Vertex v : t.vertex)
				{
					double[] pos = v.getUV();
					EuclidianMetric3D curMetric = metrics.get(v);
					EuclidianMetric3D newMetric = new EuclidianMetric3D(metric.getTargetSize(pos[0], pos[1], pos[2]));
					if (curMetric == null || curMetric.getUnitBallBBox()[0] > newMetric.getUnitBallBBox()[0])
						metrics.put(v, newMetric);
				}
			}
		}

		ArrayList<Vertex> nodes = new ArrayList<Vertex>();
		ArrayList<Vertex> triNodes = new ArrayList<Vertex>();
		ArrayList<EuclidianMetric3D> triMetrics = new ArrayList<EuclidianMetric3D>();

		Map<Vertex, Vertex> neighborMap = new HashMap<Vertex, Vertex>();
		int nrIter = 0;
		int processed = 0;
		// Number of nodes which were skipped
		int skippedNodes = 0;
		AbstractHalfEdge h = null;
		AbstractHalfEdge sym = null;

		double[][] temp = new double[4][3];
		// We try to insert new nodes by splitting large edges.  As edge collapse
		// is costful, nodes are inserted only if it does not create small edges,
		// which means that nodes are not deleted.
		// We iterate over all edges, and put candidate nodes into triNodes.
		// If an edge has no candidates, either because it is small or because no
		// nodes can be inserted, it is tagged and will not have to be checked
		// during next iterations.

		// Clear MARKED attribute
		for (Triangle f : mesh.getTriangles())
			f.clearAttributes(AbstractHalfEdge.MARKED);

		while (true)
		{
			nrIter++;
			// Maximal number of nodes which are inserted on an edge
			int maxNodes = 0;
			// Number of checked edges
			int checked = 0;
			// Number of nodes which are too near from existing vertices
			int tooNearNodes = 0;
			nodes.clear();
			surroundingTriangle.clear();
			mapTriangleVertices.clear();
			neighborMap.clear();
			skippedNodes = 0;
			LOGGER.fine("Check all edges");
			for(Triangle t : mesh.getTriangles())
			{
				if (t.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
				h = t.getAbstractHalfEdge(h);
				sym = t.getAbstractHalfEdge(sym);
				triNodes.clear();
				triMetrics.clear();
				Collection<Vertex> newVertices = mapTriangleVertices.get(t);
				if (newVertices == null)
					newVertices = new ArrayList<Vertex>();
				// Maximal number of nodes which are inserted on edges of this triangle
				int nrTriNodes = 0;
				for (int i = 0; i < 3; i++)
				{
					h = h.next();
					if (h.hasAttributes(AbstractHalfEdge.IMMUTABLE))
						continue;
					if (h.hasAttributes(AbstractHalfEdge.MARKED))
					{
						// This edge has already been checked and cannot be split
						continue;
					}

					// Tag symmetric edge to process edges only once
					if (!h.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
					{
						sym = h.sym(sym);
						sym.setAttributes(AbstractHalfEdge.MARKED);
					}
					else
					{
						for (Iterator<AbstractHalfEdge> it = h.fanIterator(); it.hasNext(); )
						{
							AbstractHalfEdge f = it.next();
							f.setAttributes(AbstractHalfEdge.MARKED);
							f.sym().setAttributes(AbstractHalfEdge.MARKED);
						}
					}

					Vertex start = h.origin();
					Vertex end = h.destination();
					EuclidianMetric3D mS = metrics.get(start);
					EuclidianMetric3D mE = metrics.get(end);
					double l = interpolatedDistance(start, mS, end, mE);
					if (l < maxlen)
					{
						// This edge is smaller than target size and is not split
						h.setAttributes(AbstractHalfEdge.MARKED);
						continue;
					}
					int nrNodes = addCandidatePoints(h, l, triNodes, triMetrics, neighborMap);
					if (nrNodes > nrTriNodes)
					{
						nrTriNodes = nrNodes;
					}
					checked++;
				}
				if (nrTriNodes > maxNodes)
					maxNodes = nrTriNodes;
				if (!triNodes.isEmpty())
				{
					//  Process in pseudo-random order
					int prime = PrimeFinder.nextPrime(nrTriNodes);
					int imax = triNodes.size();
					while (imax % prime == 0)
						prime = PrimeFinder.nextPrime(prime+1);
					if (prime >= imax)
						prime = 1;
					int index = imax / 2;
					for (int i = 0; i < imax; i++)
					{
						Vertex v = triNodes.get(index);
						EuclidianMetric3D metric = triMetrics.get(index);
						assert metric != null;
						double localSize = 0.5 * metric.getUnitBallBBox()[0];
						double localSize2 = localSize * localSize;
						Vertex bgNear = neighborBgMap.get(neighborMap.get(v));
						Triangle bgT = liaison.findSurroundingTriangle(v, bgNear, localSize2, true).getTri();
						liaison.addVertex(v, bgT);
						liaison.move(v, v.getUV());

						double[] uv = v.getUV();
						Vertex n = kdTree.getNearestVertex(metric, uv);
						if (allowNearNodes || interpolatedDistance(v, metric, n, metrics.get(n)) > minlen)
						{
							kdTree.add(v);
							metrics.put(v, metric);
							nodes.add(v);
							newVertices.add(v);
							surroundingTriangle.put(v, t);
							double d0 = v.sqrDistance3D(bgT.vertex[0]);
							double d1 = v.sqrDistance3D(bgT.vertex[1]);
							double d2 = v.sqrDistance3D(bgT.vertex[2]);
							if (d0 <= d1 && d0 <= d2)
								neighborBgMap.put(v, bgT.vertex[0]);
							else if (d1 <= d0 && d1 <= d2)
								neighborBgMap.put(v, bgT.vertex[1]);
							else
								neighborBgMap.put(v, bgT.vertex[2]);
						}
						else
						{
							tooNearNodes++;
							liaison.removeVertex(v);
						}
						index += prime;
						if (index >= imax)
							index -= imax;
					}
					if (!newVertices.isEmpty())
						mapTriangleVertices.put(t, newVertices);
				}
			}
			if (nodes.isEmpty())
				break;

			for (Vertex v : nodes)
			{
				//  These vertices are not bound to any triangles, so
				//  they must be removed, otherwise getSurroundingOTriangle
				//  may return a null pointer.
				kdTree.remove(v);
			}
			LOGGER.fine("Try to insert "+nodes.size()+" nodes");
			//  Process in pseudo-random order.  There are at most maxNodes nodes
			//  on an edge, we choose an increment step greater than this value
			//  to try to split all edges.
			int prime = PrimeFinder.nextPrime(maxNodes);
			int imax = nodes.size();
			while (imax % prime == 0)
				prime = PrimeFinder.nextPrime(prime+1);
			if (prime >= imax)
				prime = 1;
			int index = imax / 2 - prime;
			int totNrSwap = 0;
			for (int i = 0; i < imax; i++)
			{
				index += prime;
				if (index >= imax)
					index -= imax;
				Vertex v = nodes.get(index);
				Triangle start = surroundingTriangle.remove(v);
				AbstractHalfEdge ot = MeshLiaison.findNearestEdge(v, start);
				sym = ot.sym(sym);
				if (ot.hasAttributes(AbstractHalfEdge.IMMUTABLE))
				{
					// Vertex is not inserted
					skippedNodes++;
					mapTriangleVertices.get(start).remove(v);
					liaison.removeVertex(v);
					neighborBgMap.remove(v);
					continue;
				}
				if (!ot.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
				{
					// Check whether edge can be split
					Vertex o = ot.origin();
					Vertex d = ot.destination();
					Vertex n = sym.apex();
					double[] pos = v.getUV();
					Matrix3D.computeNormal3D(o.getUV(), n.getUV(), pos, temp[0], temp[1], temp[2]);
					Matrix3D.computeNormal3D(n.getUV(), d.getUV(), pos, temp[0], temp[1], temp[3]);
					if (Matrix3D.prodSca(temp[2], temp[3]) <= 0.0)
					{
						// Vertex is not inserted
						skippedNodes++;
						mapTriangleVertices.get(start).remove(v);
						liaison.removeVertex(v);
						neighborBgMap.remove(v);
						continue;
					}
				}
				else
				{
					// Split boundary edge.  Project the vertex onto this edge
					double [] xo = ot.origin().getUV();
					double [] xd = ot.destination().getUV();
					double xNorm2 =
						(xd[0] - xo[0]) * (xd[0] - xo[0]) +
						(xd[1] - xo[1]) * (xd[1] - xo[1]) +
						(xd[2] - xo[2]) * (xd[2] - xo[2]);
					if (xNorm2 > 0)
					{
						double[] pos = v.getUV();
						double xScal = (1.0 / xNorm2) * (
							(xd[0] - xo[0]) * (pos[0] - xo[0]) +
							(xd[1] - xo[1]) * (pos[1] - xo[1]) +
							(xd[2] - xo[2]) * (pos[2] - xo[2])
							);
						if (xScal < 0.01 || xScal > 0.99)
						{
							// Vertex is not inserted
							skippedNodes++;
							mapTriangleVertices.get(start).remove(v);
							liaison.removeVertex(v);
							neighborBgMap.remove(v);
							continue;
						}
						v.moveTo(
							xo[0] + xScal * (xd[0] - xo[0]),
							xo[1] + xScal * (xd[1] - xo[1]),
							xo[2] + xScal * (xd[2] - xo[2]));
						mesh.getTrace().moveVertex(v);
					}
				}
				ot.clearAttributes(AbstractHalfEdge.MARKED);
				sym.clearAttributes(AbstractHalfEdge.MARKED);

				Map<Triangle, Collection<Vertex>> verticesToDispatch = collectVertices(ot);

				ot = mesh.vertexSplit(ot, v);
				assert ot.destination() == v : v+" "+ot;

				dispatchVertices(v, verticesToDispatch);

				kdTree.add(v);
				processed++;
				afterSplitHook();
				// Swap edges
				HalfEdge edge = (HalfEdge) ot;
				edge = edge.prev();
				Vertex s = edge.origin();
				boolean advance = true;
				do
				{
					advance = true;
					if (edge.checkSwap3D(mesh, coplanarity) >= 0.0)
					{
						edge.getTri().clearAttributes(AbstractHalfEdge.MARKED);
						edge.sym().getTri().clearAttributes(AbstractHalfEdge.MARKED);
						Map<Triangle, Collection<Vertex>> vTri = collectVertices(edge);
						edge = (HalfEdge) mesh.edgeSwap(edge);
						dispatchVertices(null, vTri);
						totNrSwap++;
						advance = false;
					}
					else
						edge = edge.nextApexLoop();
				}
				while (!advance || edge.origin() != s);
				afterSwapHook();
				if (processed > 0 && (processed % progressBarStatus) == 0)
					LOGGER.info("Vertices inserted: "+processed);
			}
			afterIterationHook();
			assert mesh.isValid();
			if (hasRidges)
			{
				assert mesh.checkNoInvertedTriangles();
			}
			assert mesh.checkNoDegeneratedTriangles();
			assert surroundingTriangle.isEmpty() : "surroundingTriangle still contains "+surroundingTriangle.size()+" vertices";

			if (LOGGER.isLoggable(Level.FINE))
			{
				LOGGER.fine("Mesh now contains "+mesh.getTriangles().size()+" triangles");
				if (checked > 0)
					LOGGER.fine(checked+" edges checked");
				if (imax > 0)
					LOGGER.fine(imax+" nodes added");
				if (tooNearNodes > 0)
					LOGGER.fine(tooNearNodes+" nodes are too near from existing vertices and cannot be inserted");
				if (skippedNodes > 0)
					LOGGER.fine(skippedNodes+" nodes are skipped");
				if (totNrSwap > 0)
					LOGGER.fine(totNrSwap+" edges have been swapped during processing");
			}
			if (nodes.size() == skippedNodes)
				break;
		}
		LOGGER.info("Number of inserted vertices: "+processed);
		LOGGER.fine("Number of iterations to insert all nodes: "+nrIter);
		if (nrFailedInterpolations > 0)
			LOGGER.info("Number of failed interpolations: "+nrFailedInterpolations);
		LOGGER.config("Leave compute()");

		mesh.getTrace().println("# End Remesh");
		return this;
	}

	private int addCandidatePoints(AbstractHalfEdge ot, double edgeLength,
		ArrayList<Vertex> triNodes, ArrayList<EuclidianMetric3D> triMetrics,
		Map<Vertex, Vertex> neighborMap)
	{
		int nrNodes = 0;
		Vertex start = ot.origin();
		Vertex end = ot.destination();
		EuclidianMetric3D mS = metrics.get(start);
		EuclidianMetric3D mE = metrics.get(end);
		//  Ensure that start point has the lowest edge size
		double [] xs = start.getUV();
		double [] xe = end.getUV();
		if (mS.distance2(xs, xe) < mE.distance2(xs, xe))
		{
			Vertex tempV = start;
			start = end;
			end = tempV;
			xs = xe;
			xe = end.getUV();
			EuclidianMetric3D tempM = mS;
			mS = mE;
			mE = tempM;
		}
		double hS = mS.getUnitBallBBox()[0];
		double hE = mE.getUnitBallBBox()[0];
		double logRatio = Math.log(hE/hS);
		double [] lower = new double[3];
		double [] upper = new double[3];
		boolean border = ot.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD);
		int borderGroup = ot.getTri().getGroupId();
		int nr;
		double maxError, target;
		if (edgeLength < ONE_PLUS_SQRT2)
		{
			//  Add middle point; otherwise point would be too near from end point
			nr = 1;
			target = 0.5*edgeLength;
			maxError = Math.min(0.02, 0.9*Math.abs(target - 0.5*Math.sqrt(2)));
		}
		else if (edgeLength > 3.0)
		{
			//  Long edges are discretized, but do not create more than 4 subsegments
			nr = 3;
			target = edgeLength / (nr + 1);
			maxError = 0.1;
		}
		else
		{
			nr = (int) edgeLength;
			target = 1.0;
			maxError = 0.05;
		}
		// One could take nrDichotomy = 1-log(maxError)/log(2), but this
		// value may not work when surface parameters have a large
		// gradient, so take a larger value to be safe.
		int nrDichotomy = 20;
		int r = nr;
		Vertex last = start;
		Metric lastMetric = metrics.get(last);
		while (r > 0)
		{
			System.arraycopy(last.getUV(), 0, lower, 0, 3);
			System.arraycopy(end.getUV(), 0, upper, 0, 3);
			// 1-d coordinate between lower and upper points
			double alpha = 0.5;
			double delta = 0.5;
			Vertex np = mesh.createVertex(
				0.5*(lower[0]+upper[0]),
				0.5*(lower[1]+upper[1]),
				0.5*(lower[2]+upper[2]));
			int cnt = nrDichotomy;
			while(cnt >= 0)
			{
				cnt--;
				nrInterpolations++;
				// Update vertex position if 'project' flag was set
				double [] pos = np.getUV();
				if (project && !ot.hasAttributes(AbstractHalfEdge.SHARP | AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
				{
					liaison.project(np, pos, start);
				}
				// Compute metrics at this position
				EuclidianMetric3D m;
				AnalyticMetricInterface metric = metricsPartitionMap.get(ot.getTri().getGroupId());
				if (metric == null)
					metric = analyticMetric;
				if (metric != null)
					m = new EuclidianMetric3D(metric.getTargetSize(pos[0], pos[1], pos[2]));
				else
					m = new EuclidianMetric3D(hS*Math.exp(alpha*logRatio));
				double l = interpolatedDistance(last, lastMetric, np, m);
				if (Math.abs(l - target) < maxError)
				{
					last = np;
					lastMetric = m;
					if (!border)
					{
						// Check that point is not near of a border
						double localSize = 0.9 * minlen * m.getUnitBallBBox()[0];
						double localSize2 = localSize * localSize;
						if (skeleton.isNearer(np, borderGroup, localSize2))
						{
							r--;
							break;
						}
					}
					triNodes.add(last);
					triMetrics.add(m);
					if (start.getRef() == 0 && end.getRef() != 0)
						neighborMap.put(last, start);
					else if (start.getRef() != 0 && end.getRef() == 0)
							neighborMap.put(last, end);
					else if (m.distance2(pos, start.getUV()) < m.distance2(pos, end.getUV()))
						neighborMap.put(last, start);
					else
						neighborMap.put(last, end);
					nrNodes++;
					r--;
					break;
				}
				else if (l > target)
				{
					delta *= 0.5;
					alpha -= delta;
					System.arraycopy(pos, 0, upper, 0, 3);
					np.moveTo(
						0.5*(lower[0] + pos[0]),
						0.5*(lower[1] + pos[1]),
						0.5*(lower[2] + pos[2]));
				}
				else
				{
					delta *= 0.5;
					alpha += delta;
					System.arraycopy(pos, 0, lower, 0, 3);
					np.moveTo(
						0.5*(upper[0] + pos[0]),
						0.5*(upper[1] + pos[1]),
						0.5*(upper[2] + pos[2]));
				}
			}
			if (cnt < 0)
			{
				nrFailedInterpolations++;
				return nrNodes;
			}
		}
		return nrNodes;
	}

	protected void postProcessIteration(Mesh mesh, int i)
	{
		// Can be overridden
	}
	
	public void setProgressBarStatus(int n)
	{
		progressBarStatus = n;
	}

	private static void usage(int rc)
	{
		System.out.println("Usage: Remesh [options] xmlDir outDir");
		System.out.println("Options:");
		System.out.println(" -h, --help         Display this message and exit");
		System.exit(rc);
	}

	/**
	 * 
	 * @param args [options] xmlDir outDir
	 */
	public static void main(String[] args) throws IOException
	{
		org.jcae.mesh.amibe.traits.MeshTraitsBuilder mtb = org.jcae.mesh.amibe.traits.MeshTraitsBuilder.getDefault3D();
		mtb.addNodeList();
		Mesh mesh = new Mesh(mtb);
		Map<String, String> opts = new HashMap<String, String>();
		int argc = 0;
		for (String arg: args)
			if (arg.equals("--help") || arg.equals("-h"))
				usage(0);
		if (argc + 3 != args.length)
			usage(1);
		opts.put("size", args[1]);
		opts.put("coplanarity", "0.9");

if(false) {
		String metricsFile = args[0]+File.separator+"metricsMap";
		opts.put("metricsFile", metricsFile);

		PrimitiveFileReaderFactory pfrf = new PrimitiveFileReaderFactory();
		DoubleFileReader dfr = pfrf.getDoubleReader(new File(args[0]+File.separator+"jcae3d.files"+File.separator+"nodes3d.bin"));
		long n = dfr.size();
		java.io.DataOutputStream out = new java.io.DataOutputStream(new java.io.BufferedOutputStream(new java.io.FileOutputStream(metricsFile)));
		for (long i = 0; i < n; i += 3)
		{
			double x = dfr.get();
			double y = dfr.get();
			double z = dfr.get();
			double val = (x - 9000.0)*(x - 9000.0) / 2250.0;
			if (val > 200.0)
				val = 200.0;
			out.writeDouble(val);
		}
		out.close();
}
		System.out.println("Running "+args[0]+" "+args[1]+" "+args[2]);
		try
		{
			MeshReader.readObject3D(mesh, args[0]);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}

		Remesh smoother = new Remesh(mesh, opts);
		smoother.compute();			
		try
		{
			MeshWriter.writeObject3D(smoother.getOutputMesh(), args[2], null);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
}


