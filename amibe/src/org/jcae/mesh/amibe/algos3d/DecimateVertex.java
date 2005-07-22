/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2003 Jerome Robert <jeromerobert@users.sourceforge.net>

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
import org.jcae.mesh.amibe.ds.OTriangle;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Metric3D;
import org.jcae.mesh.amibe.util.PAVLSortedTree;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 * Laplacian smoothing.
 */

public class DecimateVertex
{
	private static Logger logger=Logger.getLogger(DecimateVertex.class);
	private Mesh mesh;
	private double tolerance;
	
	private class Quadric
	{
		public Metric3D A = new Metric3D();
		public double [] b = new double[3];
		public double c;
		public void add(Quadric that)
		{
			A.add(that.A);
			for (int i = 0; i < b.length; i++)
				b[i] += that.b[i];
			c += that.c;
		}
		public double value(double [] vect)
		{
			double ret = c;
			ret += 2.0 * Metric3D.prodSca(b, vect);
			for (int i = 0; i < b.length; i++)
				for (int j = 0; j < b.length; j++)
					ret += A.data[i][j] * vect[i] * vect[j];
			return ret;
		}
	}
	
	/**
	 * Creates a <code>DecimateVertex</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to refine.
	 * @param tol  maximal allowed error
	 */
	public DecimateVertex(Mesh m, double tol)
	{
		mesh = m;
		tolerance = tol;
	}
	
	/**
	 * Moves all nodes until all iterations are done.
	 *
	 * @see #computeFace
	 */
	public void compute()
	{
		logger.debug("Running DecimateVertex");
		int roughNrNodes = mesh.getTriangles().size()/2;
		HashSet nodeset = new HashSet(roughNrNodes);
		HashMap quadricMap = new HashMap(roughNrNodes);
		OTriangle ot = new OTriangle();
		OTriangle sym = new OTriangle();
		for (Iterator itf = mesh.getTriangles().iterator(); itf.hasNext(); )
		{
			Triangle f = (Triangle) itf.next();
			if (f.isOuter())
				continue;
			ot.bind(f);
			for (int i = 0; i < 3; i++)
			{
				Vertex n = f.vertex[i];
				nodeset.add(n);
				quadricMap.put(n, new Quadric());
				ot.nextOTri();
				ot.clearAttributes(OTriangle.MARKED);
			}
		}
		// Compute quadrics
		PAVLSortedTree tree = new PAVLSortedTree();
		double [] b = new double[3];
		for (Iterator itf = mesh.getTriangles().iterator(); itf.hasNext(); )
		{
			Triangle f = (Triangle) itf.next();
			if (f.isOuter())
				continue;
			double [] normal = f.normal3D();
			double d = - Metric3D.prodSca(normal, f.vertex[0].getUV());
			for (int i = 0; i < 3; i++)
			{
				Quadric q = (Quadric) quadricMap.get(f.vertex[i]);
				for (int k = 0; k < 3; k++)
				{
					q.b[k] += d * normal[k];
					for (int l = 0; l < 3; l++)
						q.A.data[k][l] += normal[k]*normal[l];
				}
				q.c += d*d;
			}
		}
		//  Compute edge cost
		for (Iterator itf = mesh.getTriangles().iterator(); itf.hasNext(); )
		{
			Triangle f = (Triangle) itf.next();
			if (f.isOuter())
				continue;
			ot.bind(f);
			for (int i = 0; i < 3; i++)
			{
				ot.nextOTri();
				if (ot.hasAttributes(OTriangle.MARKED))
					continue;
				ot.setAttributes(OTriangle.MARKED);
				OTriangle.symOTri(ot, sym);
				sym.setAttributes(OTriangle.MARKED);
				if (ot.hasAttributes(OTriangle.BOUNDARY))
					continue;
				Vertex v1 = ot.origin();
				Vertex v2 = ot.destination();
				Quadric q1 = (Quadric) quadricMap.get(v1);
				Quadric q2 = (Quadric) quadricMap.get(v2);
				double cost = 
				  q1.value(v1.getUV()) + q1.value(v2.getUV()) +
				  q2.value(v1.getUV()) + q2.value(v2.getUV());
				tree.insert(ot.clone(), cost);
			}
		}
		
		int contracted = 0;
		while (tree.size() > 0)
		{
			OTriangle edge = (OTriangle) tree.first();
			double cost = tree.remove(edge);
			if (cost > tolerance)
				break;
			edge.pullAttributes();
			if (!edge.hasAttributes(OTriangle.MARKED))
				continue;
			if (!edge.canContract())
				continue;
			Vertex v1 = edge.origin();
			Vertex v2 = edge.destination();
			Quadric q1 = (Quadric) quadricMap.get(v1);
			Quadric q2 = (Quadric) quadricMap.get(v2);
			boolean contractV2 = (
				q1.value(v1.getUV()) + q2.value(v1.getUV()) >
				q1.value(v2.getUV()) + q2.value(v2.getUV())
			);
			if ((!v2.isMutable() && !contractV2) ||
			    (!v1.isMutable() && contractV2))
				continue;
			//  Update quadrics first
			Triangle t1 = edge.getTri();
			OTriangle.symOTri(edge, sym);
			Triangle t2 = sym.getTri();
			Vertex apex1 = edge.apex();
			Vertex apex2 = sym.apex();
			//  TODO: Move v1 and v2 to an optimal point
			Vertex v3;
			if (contractV2)
				v3 = (Vertex) v2.clone();
			else
				v3 = (Vertex) v1.clone();
			Quadric q3 = new Quadric();
			q3.add(q1);
			q3.add(q2);
			if (!edge.contract(v3))
				continue;
			contracted++;
			t1.unmark();
			t2.unmark();
			mesh.remove(t1);
			mesh.remove(t2);
			// Update edge costs
			quadricMap.remove(v1);
			quadricMap.remove(v2);
			quadricMap.put(v3, q3);
			if (apex1 != Vertex.outer)
			{
				v2 = apex1;
				ot = v3.findOTriangle(v2);
				assert ot != null : ""+edge+"\n"+apex1+"\n"+v3+"\n"+v2;
				q1 = (Quadric) quadricMap.get(v3);
				assert q1 != null : v3;
				q2 = (Quadric) quadricMap.get(v2);
				assert q1 != null : v2;
				cost = 
				  q1.value(v3.getUV()) + q1.value(v2.getUV()) +
				  q2.value(v3.getUV()) + q2.value(v2.getUV());
				tree.insert(ot.clone(), cost);
			}
			if (apex2 != Vertex.outer)
			{
				v2 = apex2;
				ot = v3.findOTriangle(v2);
				assert ot != null : ""+edge+"\n"+apex1+"\n"+v3+"\n"+v2;
				q1 = (Quadric) quadricMap.get(v3);
				assert q1 != null : v3;
				q2 = (Quadric) quadricMap.get(v2);
				assert q1 != null : v2;
				cost = 
				  q1.value(v3.getUV()) + q1.value(v2.getUV()) +
				  q2.value(v3.getUV()) + q2.value(v2.getUV());
				tree.insert(ot.clone(), cost);
			}
		}
		logger.info("Number of contracted edges: "+contracted);
	}
}
