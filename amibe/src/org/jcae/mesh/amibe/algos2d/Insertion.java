/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2003,2004,2005
                  Jerome Robert <jeromerobert@users.sourceforge.net>

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

import org.jcae.mesh.amibe.ds.*;
import java.util.Iterator;
import java.util.ArrayList;
import gnu.trove.PrimeFinder;
import org.apache.log4j.Logger;

/**
 */

public class Insertion
{
	private static Logger logger=Logger.getLogger(Insertion.class);
	private Mesh mesh;
	
	static public final double minlen = 1.0 / Math.sqrt(2.);
	static public final double maxlen = 1.0 * Math.sqrt(2.);
	
	/**
	 * Creates a <code>Insertion</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to refine.
	 */
	public Insertion(Mesh m)
	{
		mesh = m;
	}
	
	/**
	 * Iteratively collapses and refines edges.
	 *
	 * @see CollapseEdges
	 * @see CutEdges
	 */
	public void compute()
	{
		int maxNodes = 0;
		logger.debug(" Insert inner nodes");
		while (true)
		{
			maxNodes = 0;
			ArrayList nodes = new ArrayList();
			OTriangle sym = new OTriangle();
			OTriangle ot = new OTriangle();
			for(Iterator it = mesh.getTriangles().iterator(); it.hasNext(); )
			{
				Triangle t = (Triangle) it.next();
				ot.bind(t);
				for (int i = 0; i < 3; i++)
				{
					ot.nextOTri();
					if (ot.hasAttributes(OTriangle.BOUNDARY) || ot.hasAttributes(OTriangle.OUTER))
						ot.setAttributes(OTriangle.MARKED);
					else
						ot.clearAttributes(OTriangle.MARKED);
				}
			}
			for(Iterator it = mesh.getTriangles().iterator(); it.hasNext(); )
			{
				Triangle t = (Triangle) it.next();
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
					Vertex start = ot.origin();
					Vertex end = ot.destination();
					double [] xs = start.getUV();
					double [] xe = end.getUV();
					int segments = (int) (2.0*l/lcrit) + 10;
					Vertex [] np = new Vertex[segments-1];
					for (int ns = 1; ns < segments; ns++)
						np[ns-1] = new Vertex(xs[0]+ns*(xe[0]-xs[0])/segments, xs[1]+ns*(xe[1]-xs[1])/segments);
					
					Vertex last = start;
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
					int prime = gnu.trove.PrimeFinder.nextPrime(nrTriNodes);
					int imax = triNodes.size();
					while (imax % prime == 0)
						prime = gnu.trove.PrimeFinder.nextPrime(prime+1);
					if (prime >= imax)
						prime = 1;
					int index = imax / 2;
					for (int i = 0; i < imax; i++)
					{
						Vertex v = (Vertex) triNodes.get(index);
						Vertex n = mesh.getQuadTree().getNearestVertex(v);
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
				Vertex v = t.centroid();
				Vertex n = mesh.getQuadTree().getNearestVertex(v);
				assert n == mesh.getQuadTree().getNearestVertexDebug(v);
				if (mesh.compGeom().distance(v, n) > minlen)
				{
					mesh.getQuadTree().add(v);
					nodes.add(v);
				}
			}
			for (Iterator it = nodes.iterator(); it.hasNext(); )
			{
				Vertex v = (Vertex) it.next();
				//  These vertiuces are not bound to any triangles, so
				//  they must be removed, otherwise getSurroundingOTriangle
				//  may return a null pointer.
				mesh.getQuadTree().remove(v);
			}
			if (nodes.size() == 0)
				break;
			//  Process in pseudo-random order
			int prime = gnu.trove.PrimeFinder.nextPrime(maxNodes);
			int imax = nodes.size();
			while (imax % prime == 0)
				prime = gnu.trove.PrimeFinder.nextPrime(prime+1);
			if (prime >= imax)
				prime = 1;
			int index = imax / 2;
			int skippedNodes = 0;
			for (int i = 0; i < imax; i++)
			{
				Vertex v = (Vertex) nodes.get(index);
				OTriangle vt = v.getSurroundingOTriangle();
				if (vt.split3(v, false))
					mesh.getQuadTree().add(v);
				else
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
	
	private int countSpecialTriangles ()
	{
			int res = 0;
			OTriangle ot = new OTriangle();
			for(Iterator it = mesh.getTriangles().iterator(); it.hasNext(); )
			{
				Triangle t = (Triangle) it.next();
				ot.bind(t);
				for (int i = 0; i < 3; i++)
				{
					ot.nextOTri();
					if (ot.hasAttributes(OTriangle.BOUNDARY) || ot.hasAttributes(OTriangle.OUTER))
						res++;
				}
			}
			return res;
	}
}
