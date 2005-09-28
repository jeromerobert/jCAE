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
import org.jcae.mesh.amibe.ds.NotOrientedEdge;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Metric3D;
import org.jcae.mesh.amibe.util.PAVLSortedTree;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
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
		public void reset()
		{
			A.reset();
			for (int i = 0; i < b.length; i++)
				b[i] = 0.0;
			c = 0.0;
		}
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
		public String toString()
		{
			return "A: "+A+"\n"+
			       " b: "+b[0]+" "+b[1]+" "+b[2]+"\n"+
			       " c: "+c;
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
		NotOrientedEdge noe = new NotOrientedEdge();
		for (Iterator itf = mesh.getTriangles().iterator(); itf.hasNext(); )
		{
			Triangle f = (Triangle) itf.next();
			for (int i = 0; i < 3; i++)
			{
				Vertex n = f.vertex[i];
				nodeset.add(n);
				quadricMap.put(n, new Quadric());
			}
		}
		// Compute quadrics
		PAVLSortedTree tree = new PAVLSortedTree();
		double [] b = new double[3];
		double [] vect1 = new double[3];
		double [] vect2 = new double[3];
		for (Iterator itf = mesh.getTriangles().iterator(); itf.hasNext(); )
		{
			Triangle f = (Triangle) itf.next();
			if (f.isOuter())
				continue;
			noe.bind(f);
			double [] p0 = f.vertex[0].getUV();
			double [] p1 = f.vertex[1].getUV();
			double [] p2 = f.vertex[2].getUV();
			vect1[0] = p1[0] - p0[0];
			vect1[1] = p1[1] - p0[1];
			vect1[2] = p1[2] - p0[2];
			vect2[0] = p2[0] - p0[0];
			vect2[1] = p2[1] - p0[1];
			vect2[2] = p2[2] - p0[2];
			// This is in fact 2*area, but that does not matter
			Metric3D.prodVect3D(vect1, vect2, noe.getTempVector());
			double area = Metric3D.norm(noe.getTempVector());
			noe.computeNormal3D();
			double [] normal = noe.getTempVector();
			double d = - Metric3D.prodSca(normal, f.vertex[0].getUV());
			for (int i = 0; i < 3; i++)
			{
				Quadric q = (Quadric) quadricMap.get(f.vertex[i]);
				for (int k = 0; k < 3; k++)
				{
					q.b[k] += area * d * normal[k];
					for (int l = 0; l < 3; l++)
						q.A.data[k][l] += area * normal[k]*normal[l];
				}
				q.c += area * d*d;
			}
			for (int i = 0; i < 3; i++)
			{
				noe.nextOTri();
				if (noe.hasAttributes(OTriangle.BOUNDARY))
				{
					//  Add a virtual plane
					//  This vector has the same length as noe
					noe.computeNormal3DT();
					double [] nu = noe.getTempVector();
					area = 1000.0;
					d = - Metric3D.prodSca(nu, noe.origin().getUV());
					Quadric q1 = (Quadric) quadricMap.get(noe.origin());
					Quadric q2 = (Quadric) quadricMap.get(noe.destination());
					for (int k = 0; k < 3; k++)
					{
						q1.b[k] += area * d * nu[k];
						q2.b[k] += area * d * nu[k];
						for (int l = 0; l < 3; l++)
						{
							double delta = area * nu[k]*nu[l];
							q1.A.data[k][l] += delta;
							q2.A.data[k][l] += delta;
						}
					}
					q1.c += area * d*d;
					q2.c += area * d*d;
				}
			}
		}
		unmarkEdges();
		computeTree(tree, quadricMap);
		contractAllVertices(tree, quadricMap);
	}
	
	private void unmarkEdges()
	{
		NotOrientedEdge noe = new NotOrientedEdge();
		for (Iterator itf = mesh.getTriangles().iterator(); itf.hasNext(); )
		{
			Triangle f = (Triangle) itf.next();
			if (f.isOuter())
			{
				f.unmark();
				continue;
			}
			noe.bind(f);
			for (int i = 0; i < 3; i++)
			{
				noe.nextOTri();
				noe.clearAttributes(OTriangle.MARKED);
			}
		}
	}
	
	private void computeTree(PAVLSortedTree tree, HashMap quadricMap)
	{
		NotOrientedEdge noe = new NotOrientedEdge();
		NotOrientedEdge sym = new NotOrientedEdge();
		//  Compute edge cost
		for (Iterator itf = mesh.getTriangles().iterator(); itf.hasNext(); )
		{
			Triangle f = (Triangle) itf.next();
			if (f.isOuter())
				continue;
			noe.bind(f);
			for (int i = 0; i < 3; i++)
			{
				noe.nextOTri();
				if (noe.hasAttributes(OTriangle.MARKED))
					continue;
				noe.setAttributes(OTriangle.MARKED);
				OTriangle.symOTri(noe, sym);
				sym.setAttributes(OTriangle.MARKED);
				Vertex v1 = noe.origin();
				Vertex v2 = noe.destination();
				tree.insert(new NotOrientedEdge(noe), cost(v1, v2, quadricMap));
			}
		}
	}
	
	private boolean contractAllVertices(PAVLSortedTree tree, HashMap quadricMap)
	{
		int contracted = 0;
		HashSet trash = new HashSet();
		OTriangle ot = new OTriangle();
		NotOrientedEdge sym = new NotOrientedEdge();
		while (tree.size() > 0)
		{
			NotOrientedEdge edge = (NotOrientedEdge) tree.first();
			double cost = tree.getKey(edge);
			Vertex v1 = null, v2 = null, v3 = null;
			Quadric q1 = null, q2 = null;
			Quadric q3 = new Quadric();
			do {
				if (cost > tolerance)
					break;
				edge.pullAttributes();
				v1 = edge.origin();
				v2 = edge.destination();
				q1 = (Quadric) quadricMap.get(v1);
				q2 = (Quadric) quadricMap.get(v2);
				q3.reset();
				q3.add(q1);
				q3.add(q2);
				v3 = optimalPlacement(v1, v2, q1, q2, q3);
				if (edge.canContract(v3))
					break;
				if (logger.isDebugEnabled())
					logger.info("Edge not contracted: "+edge);
				edge = (NotOrientedEdge) tree.next();
				cost = tree.getKey(edge);
			} while (edge != null && cost <= tolerance);
			if (cost > tolerance)
				break;
			tree.remove(edge);
			if (logger.isDebugEnabled())
				logger.debug("Contract edge: "+edge);
			//  Keep track of triangles deleted when contracting
			Triangle t1 = edge.getTri();
			OTriangle.symOTri(edge, sym);
			Triangle t2 = sym.getTri();
			// Remove all edges of t1 and t2 from tree
			for (int i = 0; i < 3; i++)
			{
				edge.nextOTri();
				tree.remove(edge);
				assert !tree.containsValue(edge);
			}
			for (int i = 0; i < 3; i++)
			{
				sym.nextOTri();
				tree.remove(sym);
				assert !tree.containsValue(sym);
			}
			Vertex apex1 = edge.apex();
			Vertex apex2 = sym.apex();
			//  Compute quadrics
			edge.contract(v3);
			contracted++;
			trash.add(t1);
			trash.add(t2);
			// Update edge costs
			quadricMap.remove(v1);
			quadricMap.remove(v2);
			quadricMap.put(v3, q3);
			if (apex1 != Vertex.outer)
				ot = v3.findOTriangle(apex1);
			else
				ot = v3.findOTriangle(apex2);
			assert ot != null : ""+edge+"\n"+apex1+"\n"+v3+"\n"+apex1;
			Vertex first = ot.destination();
			if (first == Vertex.outer)
			{
				ot.nextOTriOrigin();
				first = ot.destination();
			}
			assert first != Vertex.outer;
			while (true)
			{
				if (ot.destination() != Vertex.outer)
					tree.update(new NotOrientedEdge(ot), cost(ot.destination(), v3, quadricMap));
				ot.setAttributes(OTriangle.MARKED);
				ot.nextOTriOrigin();
				if (ot.destination() == first)
					break;
			}
		}
		ArrayList newlist = new ArrayList();
		for (Iterator itf = mesh.getTriangles().iterator(); itf.hasNext(); )
		{
			Triangle f = (Triangle) itf.next();
			if (!trash.contains(f))
				newlist.add(f);
		}
		mesh.setTrianglesList(newlist);
		logger.info("Number of contracted edges: "+contracted);
		int cnt = 0;
		NotOrientedEdge edge = (NotOrientedEdge) tree.first();
		while (edge != null)
		{
			if (tree.getKey(edge) > tolerance)
				break;
			cnt++;
			edge = (NotOrientedEdge) tree.next();
		}
		logger.info("Number of edges which could have been contracted: "+cnt);
		logger.info("Number of other edges not contracted: "+(tree.size() - cnt));
		return contracted > 0;
	}
	
	private static double cost(Vertex v1, Vertex v2, HashMap quadricMap)
	{
		Quadric q1 = (Quadric) quadricMap.get(v1);
		assert q1 != null : v1;
		Quadric q2 = (Quadric) quadricMap.get(v2);
		assert q2 != null : v2;
		double ret = Math.min(
		  q1.value(v1.getUV()) + q2.value(v1.getUV()),
		  q1.value(v2.getUV()) + q2.value(v2.getUV()));
		assert ret >= -1.e-2 : q1+"\n"+q2+"\n"+ret;
		return ret;
	}
	
	private static Vertex optimalPlacement(Vertex v1, Vertex v2, Quadric q1, Quadric q2, Quadric q3)
	{
		Vertex ret;
		Metric3D Qinv = q3.A.inv();
		if (Qinv != null)
		{
			ret = (Vertex) v1.clone();
			double [] dx = Qinv.apply(q3.b);
			ret.moveTo(-dx[0], -dx[1], -dx[2]);
		}
		else
		{
			if (q1.value(v2.getUV()) + q2.value(v2.getUV()) < q1.value(v1.getUV()) + q2.value(v1.getUV()))
				ret = (Vertex) v2.clone();
			else
				ret = (Vertex) v1.clone();
		}
		return ret;
	}
}
