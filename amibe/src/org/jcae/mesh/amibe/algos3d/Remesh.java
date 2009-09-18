/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2009, by EADS France

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

import gnu.trove.PrimeFinder;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.KdTree;
import org.jcae.mesh.amibe.projection.MeshLiaison;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.ds.HalfEdge;
import org.jcae.mesh.amibe.metrics.EuclidianMetric3D;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.metrics.Metric;


/**
 * Remesh an existing mesh.
 *
 * See org.jcae.mesh.amibe.algos2d.Insertion
 * @author Denis Barbier
 */
public class Remesh
{
	private final static Logger LOGGER = Logger.getLogger(Remesh.class.getName());
	private final Mesh mesh;
	private final MeshLiaison liaison;
	// Octree to find nearest Vertex in current mesh
	private final KdTree<Vertex> kdTree;
	// Octree to find nearest Vertex in background mesh
	private final KdTree<Vertex> bgKdTree;
	private final double sizeTarget;
	private final double minlen;
	private final double maxlen;
	private final EuclidianMetric3D euclidian_metric3d = new EuclidianMetric3D();
	private final Map<Vertex, EuclidianMetric3D> metrics;
	private final Set<Vertex> immutableNodes = new LinkedHashSet<Vertex>();
	
	/**
	 * Creates a <code>Remesh</code> instance.
	 *
	 * @param bgMesh  the <code>Mesh</code> instance to refine.
	 */
	public Remesh(Mesh m)
	{
		this(m, new HashMap<String, String>());
	}
	
	/**
	 * Creates a <code>Remesh</code> instance.
	 *
	 * @param bgMesh  the <code>Mesh</code> instance to refine.
	 * @param options  map containing key-value pairs to modify algorithm
	 *        behaviour.  No options are available for now.
	 */
	public Remesh(final Mesh bgMesh, final Map<String, String> options)
	{
		liaison = new MeshLiaison(bgMesh);
		mesh = liaison.getMesh();
		double size = 1.0;
		for (final Map.Entry<String, String> opt: options.entrySet())
		{
			final String key = opt.getKey();
			final String val = opt.getValue();
			if (key.equals("size"))
				size = Double.valueOf(val).doubleValue();
		}
		sizeTarget = size;
		minlen = 1.0 / Math.sqrt(2.0);
		maxlen = Math.sqrt(2.0);

		// Compute bounding box
		double [] bbox = new double[6];
		bbox[0] = bbox[1] = bbox[2] = Double.MAX_VALUE;
		bbox[3] = bbox[4] = bbox[5] = - (Double.MAX_VALUE / 2.0);
		for (Triangle f: bgMesh.getTriangles())
		{
			if (f.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			for (Vertex v : f.vertex)
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
		}
		LOGGER.fine("Bounding box: lower("+bbox[0]+", "+bbox[1]+", "+bbox[2]+"), upper("+bbox[3]+", "+bbox[4]+", "+bbox[5]+")");

		kdTree = new KdTree<Vertex>(bbox);
		Collection<Vertex> nodeset = mesh.getNodes();
		if (nodeset == null)
		{
			nodeset = new LinkedHashSet<Vertex>(mesh.getTriangles().size() / 2);
			for (Triangle f : mesh.getTriangles())
			{
				if (f.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
				for (Vertex v: f.vertex)
					nodeset.add(v);
			}
		}

		bgKdTree = new KdTree<Vertex>(bbox);
		Collection<Vertex> bgNodeset = bgMesh.getNodes();
		if (bgNodeset == null)
		{
			bgNodeset = new LinkedHashSet<Vertex>(bgMesh.getTriangles().size() / 2);
			for (Triangle f : bgMesh.getTriangles())
			{
				if (f.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
				for (Vertex v: f.vertex)
					bgNodeset.add(v);
			}
		}


		// Arbitrary size: 2*initial number of nodes
		metrics = new HashMap<Vertex, EuclidianMetric3D>(2*nodeset.size());
		for (Vertex v : nodeset)
		{
			kdTree.add(v);
			metrics.put(v, new EuclidianMetric3D(sizeTarget));
		}

		for (Vertex v : bgNodeset)
			bgKdTree.add(v);
	}

	public Mesh getOutputMesh()
	{
		return mesh;
	}

	private static AbstractHalfEdge findSurroundingTriangle(Vertex v, Vertex start)
	{
		Triangle t = start.getNeighbourIteratorTriangle().next();
		AbstractHalfEdge ot = t.getAbstractHalfEdge();
		if (start == ot.destination())
			ot = ot.next(ot);
		else if (start == ot.apex())
			ot = ot.prev(ot);
		assert start == ot.origin();

		double[] pos = v.getUV();
		double dmin = Double.MAX_VALUE;
		int[] index = new int[1];
		int i = 0;
		Vertex d = ot.destination();
		do
		{
			ot = ot.nextOriginLoop();
			double dist = sqrDistanceVertexTriangle(pos, ot.getTri(), index);
			if (dist < dmin)
			{
				dmin = dist;
				t = ot.getTri();
				i = index[0];
			}
		}
		while (ot.destination() != d);
		ot = t.getAbstractHalfEdge(ot);
		if (ot.destination() == t.vertex[i])
			ot = ot.next();
		else if (ot.apex() == t.vertex[i])
			ot = ot.prev();
		return ot;
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
		if (Matrix3D.prodSca(temp[2], temp[3]) < 0.0)
			return false;
		return true;
	}

	/**
	 * Compute squared distance between a point and a triangle.  See
	 *   http://www.geometrictools.com/Documentation/DistancePoint3Triangle3.pdf
	 */
	private static double sqrDistanceVertexTriangle(double[] pos, Triangle tri, int[] index)
	{
		double[] t0 = tri.vertex[0].getUV();
		double[] t1 = tri.vertex[1].getUV();
		double[] t2 = tri.vertex[2].getUV();
		double a = tri.vertex[0].sqrDistance3D(tri.vertex[1]);
		double b =
			(t1[0] - t0[0]) * (t2[0] - t0[0]) +
			(t1[1] - t0[1]) * (t2[1] - t0[1]) +
			(t1[2] - t0[2]) * (t2[2] - t0[2]);
		double c = tri.vertex[0].sqrDistance3D(tri.vertex[2]);
		double d =
			(t1[0] - t0[0]) * (t0[0] - pos[0]) +
			(t1[1] - t0[1]) * (t0[1] - pos[1]) +
			(t1[2] - t0[2]) * (t0[2] - pos[2]);
		double e =
			(t2[0] - t0[0]) * (t0[0] - pos[0]) +
			(t2[1] - t0[1]) * (t0[1] - pos[1]) +
			(t2[2] - t0[2]) * (t0[2] - pos[2]);
		double f =
			(pos[0] - t0[0]) * (pos[0] - t0[0]) +
			(pos[1] - t0[1]) * (pos[1] - t0[1]) +
			(pos[2] - t0[2]) * (pos[2] - t0[2]);
		// Minimize Q(s,t) = a*s*s + 2.0*b*s*t + c*t*t + 2.0*d*s + 2.0*e*t + f
		double det = a*c - b*b;
		double s = b*e - c*d;
		double t = b*d - a*e;
		if ( s+t <= det )
		{
			if ( s < 0.0 )
			{
				if ( t < 0.0 )
				{
					// region 4
					if (d < 0.0)
					{
						t = 0.0;
						if (-d >= a)
							s = 1.0;
						else
							s = -d/a;
					}
					else
					{
						s = 0.0;
						if (e >= 0.0)
							t = 0.0;
						else if (-e >= c)
							t = 1.0;
						else
							t = -e/c;
					}
				}
				else
				{
					// region 3
					s = 0.0;
					if (e >= 0.0)
						t = 0.0;
					else if (-e >= c)
						t = 1.0;
					else
						t = -e/c;
				}
			}
			else if ( t < 0.0 )
			{
				// region 5
				t = 0.0;
				if (d >= 0.0)
					s = 0.0;
				else if (-d >= a)
					s = 1.0;
				else
					s = -d/a;
			}
			else
			{
				// region 0
				double invDet = 1.0 / det;
				s *= invDet;
				t *= invDet;
			}
		}
		else
		{
			if ( s < 0.0 )
			{
				// region 2
				if (c+e > b+d)
				{
					// minimum on edge s+t = 1
					double numer = (c+e) - (b+d);
					double denom = (a-b) + (c-b);
					if (numer >= denom)
						s = 1.0;
					else
						s = numer / denom;
					t = 1.0 - s;
				}
				else
				{
					// minimum on edge s = 0
					s = 0.0;
					if (e >= 0.0)
						t = 0.0;
					else if (-e >= c)
						t = 1.0;
					else
						t = -e/c;
				}
			}
			else if ( t < 0.0 )
			{
				// region 6
				if (a+d > b+e)
				{
					// minimum on edge s+t = 1
					double numer = (a+d) - (b+e);
					double denom = (a-b) + (c-b);
					if (numer >= denom)
						s = 1.0;
					else
						s = numer / denom;
					t = 1.0 - s;
				}
				else
				{
					// minimum on edge t=0
					t = 0.0;
					if (d >= 0.0)
						s = 0.0;
					else if (-d >= a)
						s = 1.0;
					else
						s = -d/a;
				}
			}
			else
			{
				// region 1
				double numer = (c+e) - (b+d);
				if (numer <= 0.0)
				{
					s = 0.0;
				}
				else
				{
					double denom = (a-b)+(c-b);
					if (numer >= denom)
						s = 1.0;
					else
						s = numer/denom;
					t = 1.0 - s;
				}
			}
		}
		double ret = a*s*s + 2.0*b*s*t + c*t*t + 2.0*d*s + 2.0*e*t + f;
		// Fix possible numerical errors
		if (ret < 0.0)
			ret = 0.0;
		if (t <= s && s <= 1.0 - 2.0*t)
			index[0] = 0;
		else if (1.0 <= s + 2.0*t && 1.0 <= t + 2.0*s)
			index[0] = 1;
		else
			index[0] = 2;
		return ret;
	}

	private static double interpolatedDistance(Vertex pt1, Metric m1, Vertex pt2, Metric m2)
	{
		assert m1 != null : "Metric null at point "+pt1;
		assert m2 != null : "Metric null at point "+pt2;
		double[] p1 = pt1.getUV();
		double[] p2 = pt2.getUV();
		// Linear interpolation:
		double a = Math.sqrt(m1.distance2(p1, p2));
		double b = Math.sqrt(m2.distance2(p1, p2));
		double l = (2.0/3.0) * (a*a + a*b + b*b) / (a + b);
		return l;
	}

	public Remesh compute()
	{
		LOGGER.info("Run "+getClass().getName());
		ArrayList<Vertex> nodes = new ArrayList<Vertex>();
		ArrayList<Vertex> triNodes = new ArrayList<Vertex>();
		ArrayList<EuclidianMetric3D> triMetrics = new ArrayList<EuclidianMetric3D>();

		int nrIter = 0;
		int processed = 0;
		HashSet<Triangle> trianglesToCheck = new HashSet<Triangle>(mesh.getTriangles().size());
		LinkedHashSet<Triangle> oldTrianglesToCheck = new LinkedHashSet<Triangle>(mesh.getTriangles().size());
		AbstractHalfEdge h = null;
		AbstractHalfEdge sym = null;
		for (Triangle f : mesh.getTriangles())
		{
			if (f.hasAttributes(AbstractHalfEdge.OUTER))
				continue;

			oldTrianglesToCheck.add(f);
			// Clear MARKED attribute
			h = f.getAbstractHalfEdge(h);
			if (h.hasAttributes(AbstractHalfEdge.BOUNDARY))
				h.setAttributes(AbstractHalfEdge.MARKED);
			else
				h.clearAttributes(AbstractHalfEdge.MARKED);
		}

		// We try to insert new nodes by splitting large edges.  As edge collapse
		// is costful, nodes are inserted only if it does not create small edges,
		// which means that nodes are not deleted.
		// We iterate over all edges, and put candidate nodes into triNodes.
		// If an edge has no candidates, either because it is small or because no
		// nodes can be inserted, it is tagged and will not have to be checked
		// during next iterations.
		// For triangle centroids, this is a little bit more difficult, we need to
		// keep track of triangles which have been modified at previous iteration.
		while (true)
		{
			nrIter++;
			// Maximal number of nodes which are inserted on an edge
			int maxNodes = 0;
			// Number of checked edges
			int checked = 0;
			// Number of nodes which are too near from existing vertices
			int tooNearNodes = 0;
			// Number of quadtree cells split
			int kdtreeSplit = 0;
			nodes.clear();
			LOGGER.fine("Check all edges");
			for(Triangle t : mesh.getTriangles())
			{
				if (t.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
				h = t.getAbstractHalfEdge(h);
				sym = t.getAbstractHalfEdge(sym);
				triNodes.clear();
				triMetrics.clear();
				// Maximal number of nodes which are inserted on edges of this triangle
				int nrTriNodes = 0;
				for (int i = 0; i < 3; i++)
				{
					h = h.next(h);
					if (h.hasAttributes(AbstractHalfEdge.MARKED))
					{
						// This edge has already been checked and cannot be split
						continue;
					}
					sym = h.sym(sym);
					if (sym.hasAttributes(AbstractHalfEdge.MARKED))
					{
						// This edge has already been checked and cannot be split
						continue;
					}
					// Tag edges
					h.setAttributes(AbstractHalfEdge.MARKED);
					sym.setAttributes(AbstractHalfEdge.MARKED);

					Vertex start = h.origin();
					Vertex end = h.destination();
					double l = interpolatedDistance(start, metrics.get(start), end, metrics.get(end));
					if (l < maxlen)
					{
						// This edge is smaller than target size and is not split
						continue;
					}
					//  Long edges are discretized, but do not create more than 2 subsegments
					double lcrit = 1.0;
					if (l > 2.0)
						lcrit = l / 2.0;
					double [] xs = start.getUV();
					double [] xe = end.getUV();
					int segments = (int) (2.0*l/lcrit) + 10;
					Vertex [] np = new Vertex[segments-1];
					double[] pos = new double[3];
					for (int ns = 1; ns < segments; ns++)
					{
						pos[0] = xs[0]+ns*(xe[0]-xs[0])/segments;
						pos[1] = xs[1]+ns*(xe[1]-xs[1])/segments;
						pos[2] = xs[2]+ns*(xe[2]-xs[2])/segments;
						np[ns-1] = mesh.createVertex(pos);
						liaison.project(np[ns-1], pos, start);
					}

					Vertex last = start;
					Metric lastMetric = metrics.get(start);
					int nrNodes = 0;

					l = 0.0;
					for (int ns = 0; ns < segments-1; ns++)
					{
						EuclidianMetric3D m = new EuclidianMetric3D(sizeTarget);
						l = interpolatedDistance(last, lastMetric, np[ns], m);
						if (l > lcrit)
						{
							last = np[ns];
							triNodes.add(last);
							triMetrics.add(m);
							l = 0.0;
							nrNodes++;
						}
					}
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
						double[] uv = v.getUV();
						Vertex n = kdTree.getNearestVertex(metric, uv);
						if (interpolatedDistance(v, metric, n, metrics.get(n)) > minlen)
						{
							kdTree.add(v);
							metrics.put(v, metric);
							nodes.add(v);
						}
						else
							tooNearNodes++;
						index += prime;
						if (index >= imax)
							index -= imax;
					}
				}
			}
			//  Try to insert triangle centroids after other points.
			//  We scan triangles for which centroid have already
			//  proven to be valid, and all triangles which have been
			//  modified by vertex insertion.
			Vertex c = null;
			trianglesToCheck.clear();
			LOGGER.fine("Check triangle centroids for "+oldTrianglesToCheck.size()+" triangles");
			for (Triangle t : oldTrianglesToCheck)
			{
				if (t.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
				// Check triangle centroid only if at least one edge is large
				boolean tooSmall = true;
				h = t.getAbstractHalfEdge(h);
				for (int j = 0; tooSmall && j < 3; j++)
				{
					h = h.next(h);
					if (h.hasAttributes(AbstractHalfEdge.MARKED))
					{
						sym = h.sym(sym);
						if (!sym.hasAttributes(AbstractHalfEdge.MARKED))
							tooSmall = false;
					}
					else
						tooSmall = false;
				}
				if (tooSmall)
					continue;
				c = mesh.getTriangleCentroid(t, c);
				// Link to surrounding triangle to speed up
				// kdTree.getNearestVertex() and thus
				// v.getSurroundingOTriangle() below.
				c.setLink(t);
				liaison.project(c, c.getUV(), t.vertex[0]);

				EuclidianMetric3D metric = new EuclidianMetric3D(sizeTarget);
				Vertex n = kdTree.getNearestVertex(metric, c.getUV());
				assert checkNearestVertex(metric, c.getUV(), n);
				if (interpolatedDistance(c, metric, n, metrics.get(n)) > minlen)
				{
					kdTree.add(c);
					metrics.put(c, metric);
					nodes.add(c);
					trianglesToCheck.add(t);
					c = null;
				}
				else
					tooNearNodes++;
			}
			if (nodes.isEmpty())
				break;
			for (Vertex v : nodes)
			{
				//  These vertices are not bound to any triangles, so
				//  they must be removed, otherwise getSurroundingOTriangle
				//  may return a null pointer.
				kdTree.remove(v);
				metrics.remove(v);
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
			int index = imax / 2;
			int skippedNodes = 0;
			int totNrSwap = 0;
			for (int i = 0; i < imax; i++)
			{
				Vertex v = nodes.get(index);
				EuclidianMetric3D metric = new EuclidianMetric3D(sizeTarget);
				metrics.put(v, metric);
				double[] pos = v.getUV();
				Vertex near = kdTree.getNearestVertex(metric, pos);
				AbstractHalfEdge ot = findSurroundingTriangle(v, near);
				mesh.vertexSplit(ot, v);
				assert ot.destination() == v : v+" "+ot;
				assert mesh.isValid();
				kdTree.add(v);
				Vertex bgNear = bgKdTree.getNearestVertex(metric, pos);
				liaison.addVertex(v, findSurroundingTriangle(v, bgNear).getTri());
				processed++;
				// Swap edges
				HalfEdge edge = (HalfEdge) ot;
				edge = edge.next();
				Vertex s = edge.destination();
				int	counter = 0;
				do
				{
					edge = edge.nextOriginLoop();
					counter++;
					if (edge.checkSwap3D(0.8) >= 0.0)
					{
						edge = (HalfEdge) mesh.edgeSwap(edge);
						counter--;
					}
				}
				while ((edge.destination() != s || counter == 0) && counter < 20);
				assert mesh.isValid();
				index += prime;
				if (index >= imax)
					index -= imax;
			}

			if (LOGGER.isLoggable(Level.FINE))
			{
				LOGGER.fine("Mesh now contains "+mesh.getTriangles().size()+" triangles");
				if (checked > 0)
					LOGGER.fine(checked+" edges checked");
				if (imax - skippedNodes > 0)
					LOGGER.fine((imax-skippedNodes)+" nodes added");
				if (tooNearNodes > 0)
					LOGGER.fine(tooNearNodes+" nodes are too near from existing vertices and cannot be inserted");
				if (skippedNodes > 0)
					LOGGER.fine(skippedNodes+" nodes cannot be inserted");
				if (totNrSwap > 0)
					LOGGER.fine(totNrSwap+" edges have been swapped during processing");
				if (kdtreeSplit > 0)
					LOGGER.fine(kdtreeSplit+" quadtree cells split");
			}
			if (skippedNodes == nodes.size())
				break;
			// Copy trianglesToCheck into oldTrianglesToCheck and keep original
			// order from mesh.getTriangles().  This is to make sure that this
			// use of trianglesToCheck does not modify result.
			oldTrianglesToCheck.clear();
			for(Triangle t : mesh.getTriangles())
			{
				if (trianglesToCheck.contains(t))
					oldTrianglesToCheck.add(t);
			}
		}
		LOGGER.info("Number of inserted vertices: "+processed);
		LOGGER.fine("Number of iterations to insert all nodes: "+nrIter);
		LOGGER.config("Leave compute()");

		return this;
	}

	private final boolean checkNearestVertex(Metric metric, double[] uv, Vertex n)
	{
		double d1 = metric.distance2(uv, n.getUV());
		Vertex debug = kdTree.getNearestVertexDebug(metric, uv);
		double d2 = metric.distance2(uv, debug.getUV());
		assert d1 == d2 : ""+n+" is at a distance "+d1+" but nearest point is "+debug+" at distance "+d2;
		return true;
	}

	protected void postProcessIteration(Mesh mesh, int i)
	{
		// Can be overridden
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
	public static void main(String[] args)
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


