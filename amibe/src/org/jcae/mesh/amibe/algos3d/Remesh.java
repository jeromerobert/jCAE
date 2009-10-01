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
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;
import org.jcae.mesh.xmldata.DoubleFileReader;
import org.jcae.mesh.xmldata.PrimitiveFileReaderFactory;

import gnu.trove.PrimeFinder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.ArrayList;
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
	private final Mesh mesh;
	private final MeshLiaison liaison;
	// Octree to find nearest Vertex in current mesh
	private final KdTree<Vertex> kdTree;
	// Octree to find nearest Vertex in background mesh
	private final KdTree<Vertex> bgKdTree;
	private final double sizeTarget;
	private DoubleFileReader dfrMetrics;
	private final double minlen;
	private final double maxlen;
	private final Map<Vertex, EuclidianMetric3D> metrics;
	
	/**
	 * Creates a <code>Remesh</code> instance.
	 *
	 * @param bgMesh  the <code>Mesh</code> instance to refine.
	 */
	public Remesh(Mesh m)
	{
		this(m, MeshTraitsBuilder.getDefault3D());
	}

	public Remesh(Mesh m, Map<String, String> opts)
	{
		this(m, MeshTraitsBuilder.getDefault3D(), opts);
	}

	public Remesh(Mesh m, MeshTraitsBuilder mtb)
	{
		this(m, mtb, new HashMap<String, String>());
	}

	/**
	 * Creates a <code>Remesh</code> instance.
	 *
	 * @param bgMesh  the <code>Mesh</code> instance to refine.
	 * @param options  map containing key-value pairs to modify algorithm
	 *        behaviour.  No options are available for now.
	 */
	public Remesh(final Mesh bgMesh, final MeshTraitsBuilder mtb, final Map<String, String> options)
	{
		liaison = new MeshLiaison(bgMesh, mtb);
		mesh = liaison.getMesh();
		double size = 1.0;
		for (final Map.Entry<String, String> opt: options.entrySet())
		{
			final String key = opt.getKey();
			final String val = opt.getValue();
			if (key.equals("size"))
				size = Double.valueOf(val).doubleValue();
			else if (key.equals("ridgeAngle"))
				mesh.buildRidges(Double.valueOf(val).doubleValue());
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
			}
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
			if (dfrMetrics != null)
			{
				try {
					metrics.put(v, new EuclidianMetric3D(dfrMetrics.get(v.getLabel() - 1)));
				} catch (IOException ex) {
					LOGGER.log(Level.SEVERE, null, ex);
					metrics.put(v, new EuclidianMetric3D(sizeTarget));
				}
			}
			else
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
		int[] index = new int[2];
		int i = -1;
		Vertex d = ot.destination();
		t = null;
		// First, find the best triangle in the neighborhood of 'start' vertex
		do
		{
			ot = ot.nextOriginLoop();
			if (ot.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			double dist = sqrDistanceVertexTriangle(pos, ot.getTri(), index);
			if (dist < dmin)
			{
				dmin = dist;
				t = ot.getTri();
				i = (index[1] - 1)/ 2;
			}
		}
		while (ot.destination() != d);
		assert i >= 0 && t != null;
		ot = t.getAbstractHalfEdge(ot);
		if (ot.origin() == t.vertex[i])
			ot = ot.next();
		else if (ot.destination() == t.vertex[i])
			ot = ot.prev();

		// Now cross edges to see if adjacent triangle is nearer
		do
		{
			if (ot.hasAttributes(AbstractHalfEdge.BOUNDARY))
				break;
			AbstractHalfEdge sym = ot.sym();
			t = sym.getTri();
			double dist = sqrDistanceVertexTriangle(pos, t, index);
			if (dist >= dmin)
				break;
			dmin = dist;
			ot = sym;
			i = (index[1] - 1)/ 2;
			if (ot.origin() == t.vertex[i])
				ot = ot.next();
			else if (ot.destination() == t.vertex[i])
				ot = ot.prev();
		} while (true);
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
		index[0] = index[1] = -1;
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
						{
							index[1] = 6;
							s = 1.0;
						}
						else
						{
							index[1] = 5;
							s = -d/a;
						}
					}
					else
					{
						s = 0.0;
						if (e >= 0.0)
						{
							index[1] = 4;
							t = 0.0;
						}
						else if (-e >= c)
						{
							index[1] = 2;
							t = 1.0;
						}
						else
						{
							index[1] = 3;
							t = -e/c;
						}
					}
				}
				else
				{
					// region 3
					s = 0.0;
					if (e >= 0.0)
					{
						index[1] = 4;
						t = 0.0;
					}
					else if (-e >= c)
					{
						index[1] = 2;
						t = 1.0;
					}
					else
					{
						index[1] = 3;
						t = -e/c;
					}
				}
			}
			else if ( t < 0.0 )
			{
				// region 5
				t = 0.0;
				if (d >= 0.0)
				{
					index[1] = 4;
					s = 0.0;
				}
				else if (-d >= a)
				{
					index[1] = 6;
					s = 1.0;
				}
				else
				{
					index[1] = 5;
					s = -d/a;
				}
			}
			else
			{
				// region 0
				double invDet = 1.0 / det;
				s *= invDet;
				t *= invDet;
				if (t <= s && t <= 1.0 - s - t)
					index[1] = 5;
				else if (s <= t && s <= 1.0 - s - t)
					index[1] = 3;
				else if (s >= 1.0 - s - t && t >= 1.0 - s -t)
					index[1] = 1;
				else
					throw new RuntimeException("Illegal arguments: s="+s+" t="+t+" "+det+"\n"+tri);
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
					{
						index[1] = 6;
						s = 1.0;
					}
					else
					{
						index[1] = 1;
						s = numer / denom;
					}
					t = 1.0 - s;
				}
				else
				{
					// minimum on edge s = 0
					s = 0.0;
					if (e >= 0.0)
					{
						index[1] = 4;
						t = 0.0;
					}
					else if (-e >= c)
					{
						index[1] = 2;
						t = 1.0;
					}
					else
					{
						index[1] = 3;
						t = -e/c;
					}
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
					{
						index[1] = 6;
						t = 1.0;
					}
					else
					{
						index[1] = 1;
						t = numer / denom;
					}
					s = 1.0 - t;
				}
				else
				{
					// minimum on edge t=0
					t = 0.0;
					if (d >= 0.0)
					{
						index[1] = 4;
						s = 0.0;
					}
					else if (-d >= a)
					{
						index[1] = 6;
						s = 1.0;
					}
					else
					{
						index[1] = 5;
						s = -d/a;
					}
				}
			}
			else
			{
				// region 1
				double numer = (c+e) - (b+d);
				if (numer <= 0.0)
				{
					index[1] = 2;
					s = 0.0;
				}
				else
				{
					double denom = (a-b)+(c-b);
					if (numer >= denom)
					{
						index[1] = 6;
						s = 1.0;
					}
					else
					{
						index[1] = 1;
						s = numer/denom;
					}
				}
				t = 1.0 - s;
			}
		}
		double ret = a*s*s + 2.0*b*s*t + c*t*t + 2.0*d*s + 2.0*e*t + f;
		// Fix possible numerical errors
		if (ret < 0.0)
			ret = 0.0;
		return ret;
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

	public Remesh compute()
	{
		LOGGER.info("Run "+getClass().getName());
		ArrayList<Vertex> nodes = new ArrayList<Vertex>();
		ArrayList<Vertex> triNodes = new ArrayList<Vertex>();
		ArrayList<EuclidianMetric3D> triMetrics = new ArrayList<EuclidianMetric3D>();

		int nrIter = 0;
		int processed = 0;
		// Number of nodes which were skipped
		int skippedNodes = 0;
		AbstractHalfEdge h = null;
		AbstractHalfEdge sym = null;

		EuclidianMetric3D euclid = new EuclidianMetric3D();
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
			for (int pass=0; pass < 2; pass++)
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
					// Maximal number of nodes which are inserted on edges of this triangle
					int nrTriNodes = 0;
					for (int i = 0; i < 3; i++)
					{
						h = h.next(h);
						if ((pass == 0) != (h.hasAttributes(AbstractHalfEdge.SHARP | AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD)))
							continue;
						if (h.hasAttributes(AbstractHalfEdge.MARKED))
						{
							// This edge has already been checked and cannot be split
							continue;
						}

						// Tag symmetric edge to process edges only once
						sym = h.sym(sym);
						sym.setAttributes(AbstractHalfEdge.MARKED);

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
						//  Long edges are discretized, but do not create more than 2 subsegments
						double lcrit = 1.0;
						if (l > (3.0 - pass))
							lcrit = l / (3.0 - pass);
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
							mS = mE;
							mE = metrics.get(end);
						}
						int segments = (int) (2.0*l/lcrit) + 10;
						Vertex [] np = new Vertex[segments-1];
						double[] pos = new double[3];
						double delta = 1.0 / (double) segments;
						for (int ns = 1; ns < segments; ns++)
						{
							pos[0] = xs[0]+ns*(xe[0]-xs[0])*delta;
							pos[1] = xs[1]+ns*(xe[1]-xs[1])*delta;
							pos[2] = xs[2]+ns*(xe[2]-xs[2])*delta;
							np[ns-1] = mesh.createVertex(pos);
	//						if (2*ns <= segments)
	//							liaison.project(np[ns-1], pos, start);
	//						else
	//							liaison.project(np[ns-1], pos, end);*/
						}

						Vertex last = start;
						Metric lastMetric = mS;
						int nrNodes = 0;

						l = 0.0;
						double hS = mS.getUnitBallBBox()[0];
						double hE = mE.getUnitBallBBox()[0];
						double logRatio = Math.log(hE/hS);
						for (int ns = 0; ns < segments-1; ns++)
						{
							EuclidianMetric3D m = new EuclidianMetric3D(hS*Math.exp((ns+1.0)*delta*logRatio));
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
							assert metric != null;
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
				if (nodes.isEmpty())
					continue;
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
					double[] pos = v.getUV();
					Vertex near = kdTree.getNearestVertex(euclid, pos);
					AbstractHalfEdge ot = findSurroundingTriangle(v, near);
					// Check whether edge can be split
					sym = ot.sym(sym);
					Vertex o = ot.origin();
					Vertex d = ot.destination();
					Vertex n = sym.apex();
					Matrix3D.computeNormal3D(o.getUV(), n.getUV(), pos, temp[0], temp[1], temp[2]);
					Matrix3D.computeNormal3D(n.getUV(), d.getUV(), pos, temp[0], temp[1], temp[3]);
					if (Matrix3D.prodSca(temp[2], temp[3]) < -0.6)
					{
						// Vertex is not inserted
						skippedNodes++;
						continue;
					}
					if (pass == 1 && ot.hasAttributes(AbstractHalfEdge.SHARP | AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
					{
						// Vertex is not inserted
						skippedNodes++;
						continue;
					}
					ot.clearAttributes(AbstractHalfEdge.MARKED);
					ot.sym().clearAttributes(AbstractHalfEdge.MARKED);
					mesh.vertexSplit(ot, v);
					assert ot.destination() == v : v+" "+ot;
					kdTree.add(v);
					Vertex bgNear = bgKdTree.getNearestVertex(euclid, pos);
					liaison.addVertex(v, findSurroundingTriangle(v, bgNear).getTri());
					processed++;
					// Swap edges
					HalfEdge edge = (HalfEdge) ot;
					edge = edge.prev();
					Vertex s = edge.origin();
					int counter = 0;
					do
					{
						edge = edge.nextApexLoop();
						counter++;
						if (edge.hasAttributes(AbstractHalfEdge.SHARP | AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
							continue;
						if (edge.checkSwap3D(0.8) >= 0.0)
						{
							edge.getTri().clearAttributes(AbstractHalfEdge.MARKED);
							edge.sym().getTri().clearAttributes(AbstractHalfEdge.MARKED);
							edge = (HalfEdge) mesh.edgeSwap(edge);
							counter--;
							totNrSwap++;
						}
					}
					while ((edge.origin() != s || counter == 0) && counter < 20);
				}
				assert mesh.isValid();
				assert mesh.checkNoInvertedTriangles();
				assert mesh.checkNoDegeneratedTriangles();

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
					if (kdtreeSplit > 0)
						LOGGER.fine(kdtreeSplit+" quadtree cells split");
				}
			}
			if (nodes.size() == skippedNodes)
				break;
			LOGGER.info("Number of inserted vertices: "+processed);
			LOGGER.fine("Number of iterations to insert all nodes: "+nrIter);
		}
		LOGGER.config("Leave compute()");

		return this;
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

	public static void checkFindSurroundingTriangle(String[] args) throws FileNotFoundException
	{
		org.jcae.mesh.amibe.traits.MeshTraitsBuilder mtb = org.jcae.mesh.amibe.traits.MeshTraitsBuilder.getDefault3D();
		mtb.addNodeList();
		Mesh mesh = new Mesh(mtb);
		Vertex v0 = mesh.createVertex(10.0, 20.0, 30.0);
		Vertex v1 = mesh.createVertex(16.0, 20.0, 30.0);
		Vertex v2 = mesh.createVertex(12.0, 26.0, 30.0);
		Triangle t = mesh.createTriangle(v0, v1, v2);
		int [] index = new int[2];
		int nGrid = 128;
		double[] pos = new double[3];
		java.io.PrintStream outMesh = new java.io.PrintStream("test.mesh");
		java.io.PrintStream outBB = new java.io.PrintStream("region.bb");
		java.io.PrintStream distBB = new java.io.PrintStream("test.bb");
		outMesh.println("MeshVersionFormatted 1\n\nDimension\n3\n\nGeometry\n\"test.mesh\"\n\nVertices");
		outMesh.println(nGrid*nGrid+3);
		outBB.println("3 1 "+(nGrid*nGrid+3)+" 2");
		distBB.println("3 1 "+(nGrid*nGrid+3)+" 2");
		for (int j = 0; j < nGrid; j++)
		{
			pos[1] = 15.0 + (j * 16) / (double)nGrid;
			pos[2] = 30.05;
			for (int i = 0; i < nGrid; i++)
			{
				pos[0] =  5.0 + (i * 16) / (double)nGrid;
				double d = sqrDistanceVertexTriangle(pos, t, index);
				outMesh.println(pos[0]+" "+pos[1]+" "+pos[2]+" 0");
				outBB.println((double)index[1]);
				distBB.println(d);
			}
		}
		index[1] = 0;
		pos = v0.getUV();
		outMesh.println(pos[0]+" "+pos[1]+" "+pos[2]+" "+index[1]);
		outBB.println("0.0");
		distBB.println(sqrDistanceVertexTriangle(pos, t, index));
		pos = v1.getUV();
		outMesh.println(pos[0]+" "+pos[1]+" "+pos[2]+" "+index[1]);
		outBB.println("0.0");
		distBB.println(sqrDistanceVertexTriangle(pos, t, index));
		pos = v2.getUV();
		outMesh.println(pos[0]+" "+pos[1]+" "+pos[2]+" "+index[1]);
		outBB.println("0.0");
		distBB.println(sqrDistanceVertexTriangle(pos, t, index));

		outMesh.println("\n\nQuadrilaterals\n"+((nGrid-1)*(nGrid-1)));
		for (int j = 0; j < nGrid - 1; j++)
		{
			for (int i = 0; i < nGrid - 1; i++)
			{
				outMesh.println(""+(j*nGrid+i+1)+" "+(j*nGrid+i+2)+" "+((j+1)*nGrid+i+2)+" "+((j+1)*nGrid+i+1)+" 0");
			}
		}
		int o = nGrid*nGrid;
		outMesh.println("\n\nTriangles\n1\n"+(o+1)+" "+(o+2)+" "+(o+3)+" 0");
		outMesh.println("\n\nEnd");
		outMesh.close();
		outBB.close();
		distBB.close();
	}

	/**
	 * 
	 * @param args [options] xmlDir outDir
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException
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
		opts.put("ridgeAngle", "20");
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


