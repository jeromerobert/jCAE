/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2003,2004,2005,2006, by EADS CRC

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

import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.OTriangle;
import org.jcae.mesh.amibe.patch.Mesh2D;
import org.jcae.mesh.amibe.patch.OTriangle2D;
import org.jcae.mesh.amibe.patch.Vertex2D;
import java.util.Iterator;
import java.util.ArrayList;
import gnu.trove.PrimeFinder;
import org.apache.log4j.Logger;

/**
 * Insert nodes to produce a unit mesh.  Process all edges; if an edge
 * is longer than sqrt(2), candidate vertices are added to a bucket
 * to virtually provide unit length subsegments.
 * The next step is to take vertices from the bucket in random order.
 * For each vertex <code>v</code>, the closest vertex <code>w</code>
 * already present in the mesh is returned by
 * {@link org.jcae.mesh.amibe.patch.QuadTree#getNearestVertex(Vertex2D)}
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
 */
public class Insertion
{
	private static Logger logger=Logger.getLogger(Insertion.class);
	private Mesh2D mesh;
	
	private double minlen = 1.0 / Math.sqrt(2.);
	private double maxlen = 1.0 * Math.sqrt(2.);
	
	/**
	 * Creates a <code>Insertion</code> instance.
	 *
	 * @param m  the <code>Mesh2D</code> instance to refine.
	 */
	public Insertion(Mesh2D m)
	{
		mesh = m;
	}
	
	public Insertion(Mesh2D m, double scale)
	{
		mesh = m;
		minlen = scale / Math.sqrt(2.);
		maxlen = scale * Math.sqrt(2.);
	}
	
	/**
	 * Iteratively insert inner nodes.
	 */
	public void compute()
	{
		int maxNodes = 0;
		logger.debug(" Insert inner nodes");
		while (true)
		{
			maxNodes = 0;
			ArrayList nodes = new ArrayList();
			OTriangle2D sym = new OTriangle2D();
			OTriangle2D ot = new OTriangle2D();
			for(Iterator it = mesh.getTriangles().iterator(); it.hasNext(); )
			{
				Triangle t = (Triangle) it.next();
				if (t.isOuter())
					continue;
				ot.bind(t);
				for (int i = 0; i < 3; i++)
				{
					ot.nextOTri();
					if (ot.hasAttributes(OTriangle.BOUNDARY))
						ot.setAttributes(OTriangle.MARKED);
					else
						ot.clearAttributes(OTriangle.MARKED);
				}
			}
			for(Iterator it = mesh.getTriangles().iterator(); it.hasNext(); )
			{
				Triangle t = (Triangle) it.next();
				if (t.isOuter())
					continue;
				ot.bind(t);
				ArrayList triNodes = new ArrayList();
				int nrTriNodes = 0;
				for (int i = 0; i < 3; i++)
				{
					ot.nextOTri();
					if (ot.hasAttributes(OTriangle.MARKED))
						continue;
					ot.setAttributes(OTriangle.MARKED);
					OTriangle.symOTri(ot, sym);
					if (sym.hasAttributes(OTriangle.MARKED))
						continue;
					sym.setAttributes(OTriangle.MARKED);
					double l = mesh.compGeom().length(ot);
					if (l < maxlen)
						continue;
					//  Long edges are discretized, but do not create more than 4 subsegments
					double lcrit = 1.0;
					if (l > 4.0)
						lcrit = l / 4.0;
					Vertex2D start = (Vertex2D) ot.origin();
					Vertex2D end = (Vertex2D) ot.destination();
					double [] xs = start.getUV();
					double [] xe = end.getUV();
					int segments = (int) (2.0*l/lcrit) + 10;
					Vertex2D [] np = new Vertex2D[segments-1];
					for (int ns = 1; ns < segments; ns++)
						np[ns-1] = Vertex2D.valueOf(xs[0]+ns*(xe[0]-xs[0])/segments, xs[1]+ns*(xe[1]-xs[1])/segments);
					
					Vertex2D last = start;
					int nrNodes = 0;
					
					l = 0.0;
					for (int ns = 0; ns < segments-1; ns++)
					{
						l = mesh.compGeom().distance(last, np[ns]);
						if (l > lcrit)
						{
							last = np[ns];
							triNodes.add(last);
							l = 0.0;
							nrNodes++;
						}
					}
					if (nrNodes > nrTriNodes)
						nrTriNodes = nrNodes;
				}
				if (nrTriNodes > maxNodes)
					maxNodes = nrTriNodes;
				if (triNodes.size() > 0)
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
						Vertex2D v = (Vertex2D) triNodes.get(index);
						Vertex2D n = mesh.getQuadTree().getNearestVertex(v);
						assert n == mesh.getQuadTree().getNearestVertexDebug(v);
						if (mesh.compGeom().distance(v, n) > minlen)
						{
							mesh.getQuadTree().add(v);
							nodes.add(v);
						}
						index += prime;
						if (index >= imax)
							index -= imax;
					}
				}
			}
			//  Try to insert triangle centroid after all other points.
			for (Iterator it = mesh.getTriangles().iterator(); it.hasNext(); )
			{
				Triangle t = (Triangle) it.next();
				if (t.isOuter())
					continue;
				Vertex2D v = Vertex2D.centroid((Vertex2D[]) t.vertex);
				Vertex2D n = mesh.getQuadTree().getNearestVertex(v);
				assert n == mesh.getQuadTree().getNearestVertexDebug(v);
				if (mesh.compGeom().distance(v, n) > minlen)
				{
					mesh.getQuadTree().add(v);
					nodes.add(v);
				}
			}
			for (Iterator it = nodes.iterator(); it.hasNext(); )
			{
				Vertex2D v = (Vertex2D) it.next();
				//  These vertiuces are not bound to any triangles, so
				//  they must be removed, otherwise getSurroundingOTriangle
				//  may return a null pointer.
				mesh.getQuadTree().remove(v);
			}
			if (nodes.size() == 0)
				break;
			//  Process in pseudo-random order
			int prime = PrimeFinder.nextPrime(maxNodes);
			int imax = nodes.size();
			while (imax % prime == 0)
				prime = PrimeFinder.nextPrime(prime+1);
			if (prime >= imax)
				prime = 1;
			int index = imax / 2;
			int skippedNodes = 0;
			for (int i = 0; i < imax; i++)
			{
				Vertex2D v = (Vertex2D) nodes.get(index);
				OTriangle2D vt = v.getSurroundingOTriangle(mesh);
				if (!vt.split3(mesh, v, false))
					skippedNodes++;
				index += prime;
				if (index >= imax)
					index -= imax;
			}
			if (skippedNodes == nodes.size())
				break;
			if (logger.isDebugEnabled())
				logger.debug((nodes.size()-skippedNodes)+" nodes added");
		}
	}
	
}
