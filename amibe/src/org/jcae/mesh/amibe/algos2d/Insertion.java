/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2003,2004,2005,2006, by EADS CRC
    Copyright (C) 2007,2008,2009, by EADS France

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

package org.jcae.mesh.amibe.algos2d;

import org.jcae.mesh.amibe.ds.TriangleVH;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.patch.Mesh2D;
import org.jcae.mesh.amibe.patch.VirtualHalfEdge2D;
import org.jcae.mesh.amibe.patch.Vertex2D;
import org.jcae.mesh.amibe.metrics.KdTree;
import org.jcae.mesh.amibe.metrics.Metric;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashSet;
import gnu.trove.PrimeFinder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Insert nodes to produce a unit mesh.  Process all edges; if an edge
 * is longer than sqrt(2), candidate vertices are added to a bucket
 * to virtually provide unit length subsegments.
 * The next step is to take vertices from the bucket in random order.
 * For each vertex <code>v</code>, the closest vertex <code>w</code>
 * already present in the mesh is returned by
 * {@link org.jcae.mesh.amibe.metrics.KdTree#getNearestVertex}
 * If the distance between <code>v</code> and <code>w</code> is lower
 * than 1/sqrt(2), <code>v</code> is dropped, otherwise it is inserted
 * into the mesh.  Just after a vertex is inserted, incident edges are
 * swapped if they are not Delaunay.
 * The whole process is repeated until no new vertex is added.
 *
 * <p>
 * If all vertices of an edge were inserted at the same time, adjacent
 * edges may get in trouble because their candidate vertices could be
 * too near from these points.  In order to avoid this problem, vertices
 * are processed in a random order so that all edges have a chance to be
 * splitted.  As we want reproducible meshes, a pseudo-random order is
 * preferred.
 * </p>
 *
 * <p>
 * Triangle centroids are also inserted if they are not too near of
 * existing vertices.  This was added to try to improve triangle
 * quality, but is a bad idea.  Bad triangles should instead be sorted
 * (with {@link org.jcae.mesh.amibe.util.PAVLSortedTree}) and their
 * circumcenter added to the mesh if the overall quality is improved, 
 * </p>
 *
 * <p>
 * The {@link AbstractHalfEdge#MARKED} flag has two purposes: half-edges
 * must be processed once, and when a small edge has been found, it will
 * never be processed, so there is no need to compute its length again
 * and again.  By convention, if an half-edge and its symmetric half-edge
 * has both been tagged with <code>AbstractHalfEdge.MARKED</code>, this
 * means that this edge is small.  If either an half-edge ot its symmetric
 * half-edge is tagged, this edge has already been processed.
 * </p>
 */
public class Insertion
{
	private static final Logger LOGGER=Logger.getLogger(Insertion.class.getName());
	private static final double ONE_PLUS_SQRT2 = 1.0 + Math.sqrt(2.0);
	private final Mesh2D mesh;
	private final KdTree<Vertex> kdTree;
	
	private final double minlen;
	private final double maxlen;

	// useful to see if addCandidatePoints() does its job
	private int nrInterpolations;
	private int nrFailedInterpolations;
	
	/**
	 * Creates a <code>Insertion</code> instance.
	 *
	 * @param m  the <code>Mesh2D</code> instance to refine.
	 */	
	public Insertion(Mesh2D m, double minlen, double maxlen)
	{
		mesh = m;
		kdTree = mesh.getKdTree();
		this.minlen = 0.95*minlen;
		this.maxlen = maxlen;
	}
	
	/**
	 * Iteratively insert inner nodes.
	 */
	public final void compute()
	{
		int nrIter = 0;
		LOGGER.config("Enter compute()");
		LOGGER.fine(" Insert inner nodes");
		ArrayList<Vertex2D> nodes = new ArrayList<Vertex2D>();
		ArrayList<Vertex2D> triNodes = new ArrayList<Vertex2D>();
		VirtualHalfEdge2D sym = new VirtualHalfEdge2D();
		VirtualHalfEdge2D ot = new VirtualHalfEdge2D();
		HashSet<Triangle> trianglesToCheck = new HashSet<Triangle>(mesh.getTriangles().size());
		// We use a LinkedHashSet instance below to keep triangle order
		LinkedHashSet<Triangle> oldTrianglesToCheck = new LinkedHashSet<Triangle>(mesh.getTriangles().size());
		// We do not want to split boundary edges.
		for(Triangle gt : mesh.getTriangles())
		{
			if (gt.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			TriangleVH t = (TriangleVH) gt;
			ot.bind(t);
			oldTrianglesToCheck.add(t);
			for (int i = 0; i < 3; i++)
			{
				ot.next();
				if (ot.hasAttributes(AbstractHalfEdge.BOUNDARY))
				{
					ot.setAttributes(AbstractHalfEdge.MARKED);
					sym.bind((TriangleVH) ot.getTri(), ot.getLocalNumber());
					sym.sym();
					sym.setAttributes(AbstractHalfEdge.MARKED);
				}
				else
					ot.clearAttributes(AbstractHalfEdge.MARKED);
			}
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
			for(Triangle gt : mesh.getTriangles())
			{
				if (gt.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
				TriangleVH t = (TriangleVH) gt;
				ot.bind(t);
				triNodes.clear();
				// Maximal number of nodes which are inserted on edges of this triangle
				int nrTriNodes = 0;
				for (int i = 0; i < 3; i++)
				{
					ot.next();
					if (ot.hasAttributes(AbstractHalfEdge.MARKED))
					{
						// This edge has already been checked and cannot be split
						continue;
					}
					sym.bind((TriangleVH) ot.getTri(), ot.getLocalNumber());
					sym.sym();
					if (sym.hasAttributes(AbstractHalfEdge.MARKED))
					{
						// This edge has already been checked and cannot be split
						continue;
					}
					double l = mesh.interpolatedDistance((Vertex2D) ot.origin(), (Vertex2D) ot.destination());
					if (l < maxlen)
					{
						// This edge is smaller than target size and is not split
						ot.setAttributes(AbstractHalfEdge.MARKED);
						sym.setAttributes(AbstractHalfEdge.MARKED);
						continue;
					}
					// Tag symmetric edge so that edges are checked only once
					sym.setAttributes(AbstractHalfEdge.MARKED);
					int nrNodes = addCandidatePoints(ot, sym, l, triNodes);
					if (nrNodes > nrTriNodes)
					{
						nrTriNodes = nrNodes;
					}
					else if (nrNodes == 0)
					{
						ot.setAttributes(AbstractHalfEdge.MARKED);
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
						Vertex2D v = triNodes.get(index);
						Metric metric = mesh.getMetric(v);
						double[] uv = v.getUV();
						Vertex2D n = (Vertex2D) kdTree.getNearestVertex(metric, uv);
						assert checkNearestVertex(metric, uv, n);
						if (mesh.interpolatedDistance(v, n) > minlen)
						{
							kdTree.add(v);
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
			Vertex2D c = null;
			trianglesToCheck.clear();
			LOGGER.fine("Check triangle centroids for "+oldTrianglesToCheck.size()+" triangles");
			for (Triangle gt : oldTrianglesToCheck)
			{
				if (gt.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
				TriangleVH t = (TriangleVH) gt;
				// Check triangle centroid only if at least one edge is large
				boolean tooSmall = true;
				ot.bind(t);
				for (int j = 0; tooSmall && j < 3; j++)
				{
					ot.next();
					if (ot.hasAttributes(AbstractHalfEdge.MARKED))
					{
						sym.bind((TriangleVH) ot.getTri(), ot.getLocalNumber());
						sym.sym();
						if (!sym.hasAttributes(AbstractHalfEdge.MARKED))
							tooSmall = false;
					}
					else
						tooSmall = false;
				}
				if (tooSmall)
					continue;
				if (c == null)
					c = (Vertex2D) mesh.createVertex(0.0, 0.0);
				mesh.moveVertexToCentroid(c, t);
				// Link to surrounding triangle to speed up
				// kdTree.getNearestVertex() and thus
				// v.getSurroundingOTriangle() below.
				c.setLink(t);
				Metric metric = mesh.getMetric(c);
				Vertex2D n = (Vertex2D) kdTree.getNearestVertex(metric, c.getUV());
				assert checkNearestVertex(metric, c.getUV(), n);
				if (mesh.interpolatedDistance(c, n) > minlen)
				{
					kdTree.add(c);
					nodes.add(c);
					trianglesToCheck.add(t);
					c = null;
				}
				else
					tooNearNodes++;
			}
			if (nodes.isEmpty())
				break;
			for (Vertex2D v : nodes)
			{
				//  These vertices are not bound to any triangles, so
				//  they must be removed, otherwise getSurroundingOTriangle
				//  may return a null pointer.
				kdTree.remove(v);
			}
			LOGGER.fine("Try to insert "+nodes.size()+" nodes");
			//  Process in pseudo-random order.  There is at most maxNodes nodes
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
				Vertex2D v = nodes.get(index);
				VirtualHalfEdge2D vt = v.getSurroundingOTriangle(mesh);
				int nrSwap = vt.split3(mesh, v, trianglesToCheck, false);
				if (0 == nrSwap)
					skippedNodes++;
				else
					totNrSwap += nrSwap;
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
		LOGGER.fine("Number of iterations to insert all nodes: "+nrIter);
		LOGGER.fine("Number of lengths computed: "+nrInterpolations);
		if (nrFailedInterpolations > 0)
			LOGGER.info("Number of failed interpolations: "+nrFailedInterpolations);
		LOGGER.config("Leave compute()");
	}
	
	private int addCandidatePoints(VirtualHalfEdge2D ot, VirtualHalfEdge2D sym,
		double edgeLength, ArrayList<Vertex2D> triNodes)
	{
		int nrNodes = 0;
		Vertex2D start = (Vertex2D) ot.origin();
		Vertex2D end = (Vertex2D) ot.destination();
		double [] lower = new double[2];
		double [] upper = new double[2];
		int nr;
		double delta, target;
		if (edgeLength < ONE_PLUS_SQRT2)
		{
			//  Add middle point; otherwise point would be too near from end point
			nr = 1;
			target = 0.5*edgeLength;
			delta = Math.min(0.02, 0.9*Math.abs(target - 0.5*Math.sqrt(2)));
		}
		else if (edgeLength > 4.0)
		{
			//  Long edges are discretized, but do not create more than 4 subsegments
			nr = 3;
			target = edgeLength  / 4.0;
			delta = 0.1;
		}
		else
		{
			nr = (int) edgeLength;
			target = 1.0;
			delta = 0.05;
		}
		// One could take nrDichotomy = 1-log(delta)/log(2), but this
		// value may not work when surface parameters have a large
		// gradient, so take a larger value to be safe.
		int nrDichotomy = 20;
		int r = nr;
		Vertex2D last = start;
		while (r > 0)
		{
			System.arraycopy(last.getUV(), 0, lower, 0, 2);
			System.arraycopy(end.getUV(), 0, upper, 0, 2);
			Vertex2D np = (Vertex2D) mesh.createVertex(0.5*(lower[0]+upper[0]), 0.5*(lower[1]+upper[1]));
			int cnt = nrDichotomy;
			while(cnt >= 0)
			{
				cnt--;
				nrInterpolations++;
				double l = mesh.interpolatedDistance(last, np);
				if (Math.abs(l - target) < delta)
				{
					last = np;
					Metric metric = mesh.getMetric(last);
					// Link to surrounding triangle to speed up
					// kdTree.getNearestVertex()
					if (!sym.hasAttributes(AbstractHalfEdge.OUTER) && metric.distance2(last.getUV(), sym.apex().getUV()) < metric.distance2(last.getUV(), ot.apex().getUV()))
						last.setLink(sym.getTri());
					else
						last.setLink(ot.getTri());
					triNodes.add(last);
					nrNodes++;
					r--;
					break;
				}
				else if (l > target)
				{
					double [] current = np.getUV();
					System.arraycopy(current, 0, upper, 0, 2);
					np.moveTo(0.5*(lower[0] + current[0]), 0.5*(lower[1] + current[1]));
				}
				else
				{
					double [] current = np.getUV();
					System.arraycopy(current, 0, lower, 0, 2);
					np.moveTo(0.5*(upper[0] + current[0]), 0.5*(upper[1] + current[1]));
				}
			}
			if (cnt < 0)
				nrFailedInterpolations++;
			return nrNodes;
		}
		return nrNodes;
	}
	
	private boolean checkNearestVertex(Metric metric, double[] uv, Vertex n)
	{
		double d1 = metric.distance2(uv, n.getUV());
		Vertex debug = kdTree.getNearestVertexDebug(metric, uv);
		double d2 = metric.distance2(uv, debug.getUV());
		assert d1 == d2 : ""+n+" is at a distance "+d1+" but nearest point is "+debug+" at distance "+d2;
		return true;
	}
}
