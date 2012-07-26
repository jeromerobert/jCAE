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
import org.jcae.mesh.amibe.metrics.KdTree;
import org.jcae.mesh.amibe.projection.MeshLiaison;
import org.jcae.mesh.amibe.ds.HalfEdge;
import org.jcae.mesh.amibe.metrics.EuclidianMetric3D;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.metrics.Metric;
import org.jcae.mesh.amibe.metrics.MetricSupport;
import org.jcae.mesh.amibe.metrics.MetricSupport.AnalyticMetricInterface;
import static org.jcae.mesh.amibe.metrics.MetricSupport.interpolatedDistance;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;
import org.jcae.mesh.xmldata.DoubleFileReader;
import org.jcae.mesh.xmldata.PrimitiveFileReaderFactory;

import gnu.trove.PrimeFinder;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIntIterator;
import gnu.trove.TIntObjectHashMap;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
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
	// Octree to find nearest Vertex in current mesh
	private TIntObjectHashMap<KdTree<Vertex>> kdTrees;
	// Map to keep track of a near point in background mesh, used as a starting point of locators
	private TIntObjectHashMap<Map<Vertex, Vertex>> neighborBgMap;
	private final double minlen;
	private final double maxlen;
	// useful to see if addCandidatePoints() does its job
	private int nrInterpolations;
	private int nrFailedInterpolations;

	Map<Triangle, Collection<Vertex>> mapTriangleVertices = new HashMap<Triangle, Collection<Vertex>>();
	// Keeps track of surrounding triangle
	Map<Vertex, Triangle> surroundingTriangle = new HashMap<Vertex, Triangle>();

	private final boolean project;
	private final boolean hasRidges;
	// true if mesh has free edges, ridges or nonmanifold edges, false otherwise
	private final boolean hasFeatureEdges;
	private final double coplanarity;
	private final boolean allowNearNodes;
	private final boolean remeshOnlyFeatureEdges;
	private final MetricSupport metrics;
	private double minCosAfterSwap = -2;
	// Number of nodes which are too near from existing vertices
	int tooNearNodes = 0;
	private final ArrayList<Vertex> triNodes = new ArrayList<Vertex>();
	private final ArrayList<EuclidianMetric3D> triMetrics = new ArrayList<EuclidianMetric3D>();
	private final ArrayList<Vertex> triNeighbor = new ArrayList<Vertex>();
	//  Map to keep track of all groups near a vertex
	private final Map<Vertex, int[]> groups = new HashMap<Vertex, int[]>();
	private Set<Vertex> boundaryNodes = new LinkedHashSet<Vertex>();
	// Number of checked edges
	private int edgesCheckedDuringIteration;
	//  The nodes variable contains the list of valid candidate points.
	private ArrayList<Vertex> nodes = new ArrayList<Vertex>();
	//  We keep track of the background triangle so that it is not
	//  searched again.
	private ArrayList<Triangle> bgTriangles = new ArrayList<Triangle>();
	private double currentScale = Double.MAX_VALUE;

	private double[][] temp = new double[4][3];
	private int processed = 0;
	// Number of nodes which were skipped
	private int skippedNodes = 0;

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
		metrics = new MetricSupport(mesh, options);
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
			if (key.equals("nearLengthRatio"))
			{
				nearLengthRatio = Double.valueOf(val).doubleValue();
			}
			else if (key.equals("coplanarity"))
			{
				copl = Double.valueOf(val).doubleValue();
			}
			else if(key.equals("minCosAfterSwap"))
			{
				minCosAfterSwap = Double.parseDouble(val);
			}
			else if (key.equals("decimateSize"))
			{
				decimateOptions.put("size", val);
			}
			else if (key.equals("decimateTarget"))
			{
				decimateOptions.put("maxtriangles", val);
			}
			else if (key.equals("project"))
				proj = Boolean.valueOf(val).booleanValue();
			else if (key.equals("allowNearNodes"))
				nearNodes = Boolean.valueOf(val).booleanValue();
			else if (key.equals("features"))
				onlyFeatureEdges = Boolean.valueOf(val).booleanValue();
			else if(!metrics.isKnownOption(key))
				LOGGER.warning("Unknown option: "+key);
		}
		if (meshLiaison == null)
			mesh.buildRidges(copl);

		minlen = nearLengthRatio;
		maxlen = Math.sqrt(2.0);
		project = proj;
		coplanarity = copl;
		allowNearNodes = nearNodes;
		remeshOnlyFeatureEdges = onlyFeatureEdges;

		liaison.buildSkeleton();

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

		TIntIntHashMap numberOfTriangles = computeNumberOfTriangles(mesh.getTriangles());
		neighborBgMap = new TIntObjectHashMap<Map<Vertex, Vertex>>(numberOfTriangles.size());
		neighborBgMap.put(-1, new HashMap<Vertex, Vertex>(nodeset.size()));
		for (TIntIntIterator it = numberOfTriangles.iterator(); it.hasNext(); )
		{
			it.advance();
			neighborBgMap.put(it.key(), new HashMap<Vertex, Vertex>(it.value() / 2));
		}
		kdTrees = createKdTree(nodeset, mesh.getTriangles(), numberOfTriangles);
		for (Vertex v : nodeset)
		{
			if (null == v.getLink())
				continue;
			Triangle t = liaison.getBackgroundTriangle(v);
			assert !t.hasAttributes(AbstractHalfEdge.OUTER);
			addVertexInNeighborBgMap(v, t);
		}
		metrics.compute();
	}

	/** Return the number of triangles in each groups */
	private TIntIntHashMap computeNumberOfTriangles(Iterable<Triangle> triangles)
	{
		TIntIntHashMap numberOfTriangles = new TIntIntHashMap();
		for (Triangle t : triangles)
		{
			if (t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			numberOfTriangles.putIfAbsent(t.getGroupId(), 0);
			numberOfTriangles.increment(t.getGroupId());
		}
		return numberOfTriangles;
	}

	/**
	 * @param nodeset the nodes to add to the kdTree
	 * @param numberOfTriangles a map containing the number of triangles for
	 * each groups, to speed hash table allocations
	 * @return a map whose keys are group ids and values a KdTree
	 */
	private static TIntObjectHashMap<KdTree<Vertex>> createKdTree(
		Collection<Vertex> nodeset, Iterable<Triangle> triangles,
		TIntIntHashMap numberOfTriangles)
	{
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
		TIntObjectHashMap<KdTree<Vertex>> kdTrees = new TIntObjectHashMap<KdTree<Vertex>>();
		KdTree<Vertex> globalKdTree = new KdTree<Vertex>(bbox);
		kdTrees.put(-1, globalKdTree);
		TIntObjectHashMap<HashSet<Vertex>> seenByGroup = new TIntObjectHashMap<HashSet<Vertex>>(numberOfTriangles.size());
		HashSet<Vertex> globalSeen = new HashSet(nodeset.size());
		seenByGroup.put(-1, globalSeen);
		for (TIntIntIterator it = numberOfTriangles.iterator(); it.hasNext(); )
		{
			it.advance();
			kdTrees.put(it.key(), new KdTree<Vertex>(bbox));
			seenByGroup.put(it.key(), new HashSet(it.value() / 2));
		}
		
		for (Triangle f : triangles)
		{
			if (f.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
			int group = f.getGroupId();
			KdTree<Vertex> kdTree = kdTrees.get(group);
			for (int i = 0; i < 3; ++i)
			{
				Vertex v = f.vertex[i];
				HashSet<Vertex> seen = seenByGroup.get(group);
				if (seen.contains(v))
					continue;
				seen.add(v);
				kdTree.add(v);
				if (globalSeen.contains(v))
					continue;
				globalSeen.add(v);
				globalKdTree.add(v);
			}
		}
		for (TIntIntIterator it = numberOfTriangles.iterator(); it.hasNext(); )
		{
			it.advance();
			seenByGroup.get(it.key()).clear();
		}
		seenByGroup.clear();
		return kdTrees;
	}

	private void addVertexInNeighborBgMap(Vertex v, Triangle bgT)
	{
		double d0 = v.sqrDistance3D(bgT.vertex[0]);
		double d1 = v.sqrDistance3D(bgT.vertex[1]);
		double d2 = v.sqrDistance3D(bgT.vertex[2]);
		Vertex bgNearestVertex;
		if (d0 <= d1 && d0 <= d2)
		{
			bgNearestVertex = bgT.vertex[0];
		}
		else if (d1 <= d0 && d1 <= d2)
		{
			bgNearestVertex = bgT.vertex[1];
		}
		else
		{
			bgNearestVertex = bgT.vertex[2];
		}
		neighborBgMap.get(-1).put(v, bgNearestVertex);
		if (v.isManifold())
		{
			neighborBgMap.get(bgT.getGroupId()).put(v, bgNearestVertex);
		}
		else
		{
			for (Iterator<Triangle> itT = v.getNeighbourIteratorTriangle(); itT.hasNext(); )
			{
				int groupId = itT.next().getGroupId();
				if (groupId >= 0)
					neighborBgMap.get(groupId).put(v, bgNearestVertex);
			}
		}
	}

	public void setAnalyticMetric(AnalyticMetricInterface m)
	{
		metrics.setAnalyticMetric(m);
	}

	public void setAnalyticMetric(int groupId, AnalyticMetricInterface m)
	{
		metrics.setAnalyticMetric(groupId, m);
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
		
		//  All edges of the current mesh are checked; and large edges
		//  are splitted.  In order to avoid bad geometrical patterns,
		//  vertices are not inserted, but put into a list of candidate
		//  points.  This list is then iterated over randomly.
		//  Algorithm looks like this:
		//   A. Iterate over all triangles
		//     B. Iterate over its edges which had not been scanned yet
		//       C. Compute nodes which would be at the right distance
                //          and store them into a bag.
		//     D. Iterate randomly over this bag and keep only vertices
		//        which are not too near of an existing vertex; these valid
                //        candidate points are inserted into the 'nodes' list.
		//   E. Iterate over the 'nodes' list and insert all vertices.
		//      We know that those vertices are not near an existing vertex,
		//      but we take care to not introduce inverted triangles here.
		//   F. Go to A if at least one node had been inserted

		int nrIter = 0;
		AbstractHalfEdge h = null;
		AbstractHalfEdge sym = null;

		updateCurrentScale();
		resetMarkedTags();

		// We try to insert new nodes by splitting large edges.  As edge collapse
		// is costful, nodes are inserted only if it does not create small edges,
		// which means that nodes are not deleted.
		// We iterate over all edges, and put candidate nodes into triNodes.
		// If an edge has no candidates, either because it is small or because no
		// nodes can be inserted, it is tagged and will not have to be checked
		// during next iterations.

		boolean reversed = true;
		while (true)
		{
			nrIter++;
			reversed = !reversed;
			// Maximal number of nodes which are inserted on an edge
			int maxNodes = 0;
			nodes.clear();
			bgTriangles.clear();
			groups.clear();
			surroundingTriangle.clear();
			mapTriangleVertices.clear();
			boundaryNodes.clear();
			skippedNodes = 0;
			LOGGER.fine("Check all edges");
			//   Step A. Iterate over all triangles
			for(Triangle t : mesh.getTriangles())
			{
				if (t.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
				h = t.getAbstractHalfEdge(h);
				sym = t.getAbstractHalfEdge(sym);
				triNodes.clear();
				triMetrics.clear();
				triNeighbor.clear();
				// Step B. Iterate over its edges which had not been scanned yet
				int nrTriNodes = 0;
				for (int i = 0; i < 3; i++)
				{
					h = h.next();
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

					// Step C. Compute nodes which would be at the right distance
					//         and store them into a bag (triNodes).
					int nrNodes = collectCandidatesOnEdge(h, reversed);
					if (nrNodes > nrTriNodes)
					{
						nrTriNodes = nrNodes;
					}
					edgesCheckedDuringIteration++;
				}
				// Number of nodes which are inserted on edges of this triangle
				if (nrTriNodes > maxNodes)
					maxNodes = nrTriNodes;
				// Step D. Iterate randomly over this bag and keep only vertices
				//         which are not too near of an existing vertex; these valid
			        //         candidate points are inserted into the 'nodes' list.
				if (!triNodes.isEmpty())
				{
					//  Process in pseudo-random order
					int prime = PrimeFinder.nextPrime(nrTriNodes);
					int imax = triNodes.size();
					while (imax % prime == 0)
						prime = PrimeFinder.nextPrime(prime+1);
					if (prime >= imax)
						prime = 1;
					Collection<Vertex> newVertices = checkDistanceCandidates(t, prime);
					if (!newVertices.isEmpty())
						mapTriangleVertices.put(t, newVertices);
				}
			}
			if (nodes.isEmpty())
			{
				if (meshingDone())
					break;
				else
					continue;
			}

			// Step E. Iterate over the 'nodes' list and insert all vertices.
			//         We know that those vertices are not near an existing vertex,
			//         but we take care to not introduce inverted triangles here.
			for (Vertex v : nodes)
			{
				//  These vertices are not bound to any triangles, so
				//  they must be removed, otherwise getSurroundingOTriangle
				//  may return a null pointer.
				for (int group : groups.get(v))
				{
					kdTrees.get(group).remove(v);
				}
			}
			insertNodes(maxNodes);
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
				if (edgesCheckedDuringIteration > 0)
					LOGGER.fine(edgesCheckedDuringIteration+" edges checked");
				if (tooNearNodes > 0)
					LOGGER.fine(tooNearNodes+" nodes are too near from existing vertices and cannot be inserted");
				if (skippedNodes > 0)
					LOGGER.fine(skippedNodes+" nodes are skipped");
			}
			if (nodes.size() == skippedNodes)
			{
				if (meshingDone())
					break;
			}
		}
		LOGGER.info("Number of inserted vertices: "+processed);
		LOGGER.fine("Number of iterations to insert all nodes: "+nrIter);
		if (nrFailedInterpolations > 0)
			LOGGER.info("Number of failed interpolations: "+nrFailedInterpolations);
		LOGGER.config("Leave compute()");

		mesh.getTrace().println("# End Remesh");
		return this;
	}


	private void insertNodes(int maxNodes)
	{
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
			Triangle bgT = bgTriangles.get(index);
			Triangle curStart = surroundingTriangle.remove(v);
			AbstractHalfEdge ot = MeshLiaison.findNearestEdge(v, curStart);
			AbstractHalfEdge sym = ot.sym();
			if (ot.hasAttributes(AbstractHalfEdge.IMMUTABLE))
			{
				// Vertex is not inserted
				skippedNodes++;
				mapTriangleVertices.get(curStart).remove(v);
				liaison.removeVertex(v);
				continue;
			}
			if (!ot.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD | AbstractHalfEdge.SHARP))
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
					mapTriangleVertices.get(curStart).remove(v);
					liaison.removeVertex(v);
					continue;
				}
			}
			else if (!boundaryNodes.contains(v))
			{
				// Vertex is not inserted
				skippedNodes++;
				mapTriangleVertices.get(curStart).remove(v);
				liaison.removeVertex(v);
				continue;
			}

			Map<Triangle, Collection<Vertex>> verticesToDispatch = collectVertices(ot);

			ot = mesh.vertexSplit(ot, v);
			assert ot.destination() == v : v+" "+ot;
			// Triangles around v have been modified, they
			// must reset their MARKED flag.  This will be
			// done below when swapping edges.

			dispatchVertices(v, verticesToDispatch);

			for (int group : groups.get(v))
				kdTrees.get(group).add(v);
			processed++;
			afterSplitHook();
			// Swap edges
			HalfEdge edge = (HalfEdge) ot;
			edge = edge.prev();
			Vertex s = edge.origin();
			boolean advance = true;
			double [] tNormal = liaison.getBackgroundNormal(v);
			do
			{
				advance = true;
				edge.getTri().clearAttributes(AbstractHalfEdge.MARKED);
				double checkNormal = edge.checkSwapNormal(mesh, coplanarity, tNormal);
				if (checkNormal < -1.0 || !edge.canSwapTopology())
				{
					edge = edge.nextApexLoop();
					continue;
				}
				if (edge.checkSwap3D(mesh, -2.0, 0, 0, true,
					minCosAfterSwap, minCosAfterSwap) > 0.0)
				{
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
			addVertexInNeighborBgMap(v, bgT);
			if (processed > 0 && (processed % progressBarStatus) == 0)
				LOGGER.info("Vertices inserted: "+processed);

			if (LOGGER.isLoggable(Level.FINE))
			{
				if (imax > 0)
					LOGGER.fine(imax+" nodes added");
				if (totNrSwap > 0)
					LOGGER.fine(totNrSwap+" edges have been swapped during processing");
			}
		}
	}
	private boolean updateCurrentScale()
	{
		double maxLength = 0.0;
		AbstractHalfEdge h = null;
		for (Triangle f : mesh.getTriangles())
		{
			if (f.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
			f.clearAttributes(AbstractHalfEdge.MARKED);
			h = f.getAbstractHalfEdge(h);
			for (int i = 0; i < 3; i++)
			{
				h = h.next();
				double edgeLength = metrics.interpolatedDistance(h.origin(), h.destination());
				if (edgeLength > maxLength)
					maxLength = edgeLength;
			}
		}
		LOGGER.config("Maximal edge length: "+maxLength);
		LOGGER.config("currentScale="+currentScale);

		double nextScale = currentScale;
		if (maxLength > 80.0 && currentScale > 3.0)
		{
			nextScale =  maxLength / 10.0;
			// Scaling should decrease significantly
			// If not, this means that long edges could not be splitted,
			// try with a smaller scale
			if (nextScale > 0.8 * currentScale)
				nextScale = currentScale / 2.0;
		}
		else
			nextScale = 1.0;
		if (currentScale - nextScale > 0.4)
		{
			currentScale = nextScale;
			LOGGER.config("Set scaling to "+currentScale);
			return true;
		}
		return false;
	}

	private void resetMarkedTags()
	{
		// Clear MARKED attribute
		for (Triangle f : mesh.getTriangles())
			f.clearAttributes(AbstractHalfEdge.MARKED);
		// Tag IMMUTABLE edges
		mesh.tagIf(AbstractHalfEdge.IMMUTABLE, AbstractHalfEdge.MARKED);
	}

	private boolean meshingDone()
	{
		if (currentScale < 1.01)
			return true;
		if (!updateCurrentScale())
			return true;
		resetMarkedTags();
		return false;
	}

	private int collectCandidatesOnEdge(AbstractHalfEdge ot, boolean reversed)
	{
		int nrNodes = 0;
		int group = ot.getTri().getGroupId();
		Vertex start = ot.origin();
		Vertex end = ot.destination();
		// Step C. Compute nodes which would be at the right distance
		//         and store them into a bag.
		double edgeLength = metrics.interpolatedDistance(start, end);
		if (edgeLength < currentScale * maxlen)
		{
			// This edge is smaller than target size and is not split
			ot.setAttributes(AbstractHalfEdge.MARKED);
			return nrNodes;
		}
		EuclidianMetric3D mS = metrics.get(start);
		EuclidianMetric3D mE = metrics.get(end);
		//  Ensure that start point has the lowest edge size
		double [] xs = start.getUV();
		double [] xe = end.getUV();
		if (reversed || mS.distance2(xs, xe) < mE.distance2(xs, xe))
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
		boolean border = ot.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD | AbstractHalfEdge.SHARP);
		int nr;
		double maxError, target;
		double scaledEdgeLength = edgeLength / currentScale;
		if (scaledEdgeLength < ONE_PLUS_SQRT2)
		{
			//  Add middle point; otherwise point would be too near from end point
			nr = 1;
			target = 0.5*edgeLength;
			maxError = Math.min(0.02, 0.9*Math.abs(target - 0.5*Math.sqrt(2)));
		}
		else if (scaledEdgeLength > 4.0)
		{
			//  Long edges are discretized, but do not create more than 4 subsegments
			nr = 3;
			target = edgeLength / (nr + 1);
			maxError = 0.1;
		}
		else
		{
			nr = (int) scaledEdgeLength;
			target = currentScale;
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
				EuclidianMetric3D m = metrics.get(np, ot.getTri());
				if(m == null)
					m = new EuclidianMetric3D(hS*Math.exp(alpha*logRatio));
				double l = interpolatedDistance(last, lastMetric, np, m);
				if (Math.abs(l - target) < maxError)
				{
					last = np;
					lastMetric = m;
					if (!border)
					{
						// Check that point is not near of a border
						double localSize = 0.9 * currentScale * minlen * m.getUnitBallBBox()[0];
						double localSize2 = localSize * localSize;
						if (liaison.isNearSkeleton(np, group, localSize2))
						{
							r--;
							break;
						}
					}
					else
						boundaryNodes.add(last);
					triNodes.add(last);
					triMetrics.add(m);
					if (start.getRef() == 0 && end.getRef() != 0)
						triNeighbor.add(start);
					else if (start.getRef() != 0 && end.getRef() == 0)
							triNeighbor.add(end);
					else if (m.distance2(pos, start.getUV()) < m.distance2(pos, end.getUV()))
						triNeighbor.add(start);
					else
						triNeighbor.add(end);
					addGroups(ot, last);
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

	private Collection<Vertex> checkDistanceCandidates(Triangle t, int step)
	{
		int imax = triNodes.size();
		int index = imax / 2;
		Collection<Vertex> newVertices = new ArrayList<Vertex>();
		int group = t.getGroupId();
		Map<Vertex, Vertex> mapBgGroupVertices = neighborBgMap.get(group);
		KdTree<Vertex> kdTreeGroup = kdTrees.get(group);
		for (int i = 0; i < imax; i++)
		{
			Vertex v = triNodes.get(index);
			EuclidianMetric3D metric = triMetrics.get(index);
			assert metric != null;
			double localSize = 0.5 * metric.getUnitBallBBox()[0];
			double localSize2 = localSize * localSize;
			Vertex bgNear = mapBgGroupVertices.get(triNeighbor.get(index));
			Triangle bgT = liaison.findSurroundingTriangle(v, bgNear, localSize2, true, group).getTri();
			assert bgT.getGroupId() == group || group < 0:
				mesh.getGroupName(group)+" "+mesh.getGroupName(bgT.getGroupId())+" "+v;
			liaison.addVertex(v, bgT);
			liaison.move(v, v.getUV(), group);

			boolean validCandidate = allowNearNodes;
			if (!validCandidate)
			{
				if (boundaryNodes.contains(v))
					validCandidate = true;
			}
			if (!validCandidate)
			{
				Vertex n = kdTreeGroup.getNearestVertex(metric, v.getUV());
				validCandidate = interpolatedDistance(v, metric, n, metrics.get(n)) > currentScale * minlen;
			}
			if (validCandidate)
			{
				for (int g : groups.get(v))
					kdTrees.get(g).add(v);
				metrics.put(v, metric);
				nodes.add(v);
				bgTriangles.add(bgT);
				newVertices.add(v);
				surroundingTriangle.put(v, t);
			}
			else
			{
				tooNearNodes++;
				liaison.removeVertex(v);
			}
			index += step;
			if (index >= imax)
				index -= imax;
		}
		return newVertices;
	}

	private void addGroups(AbstractHalfEdge ot, Vertex v)
	{
		assert !groups.containsKey(v);
		if (!ot.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
		{
			int g1 = ot.getTri().getGroupId();
			int g2 = g1;
			if (ot.hasSymmetricEdge())
			{
				g2 = ot.sym().getTri().getGroupId();
			}
			if (g1 == g2 && g1 == -1)
			{
				groups.put(v, new int[] {-1});
			}
			if (g1 == g2 && g1 != -1)
			{
				groups.put(v, new int[] {-1, g1});
			}
			else if (g1 == -1 || g2 == -1)
			{
				groups.put(v, new int[] {g1, g2});
			}
			else
			{
				groups.put(v, new int[] {-1, g1, g2});
			}
		}
		else
		{
			TIntHashSet groupSet = new TIntHashSet();
			for (Iterator<AbstractHalfEdge> it = ot.fanIterator(); it.hasNext(); )
			{
				groupSet.add(it.next().getTri().getGroupId());
			}
			groupSet.add(-1);
			groups.put(v, groupSet.toArray());
		}
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


