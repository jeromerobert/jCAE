/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2003,2006 by EADS CRC

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
import org.jcae.mesh.amibe.metrics.Quadric3DError;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.util.PAVLSortedTree;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;
import java.util.Stack;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 * Decimates a mesh.  This method is based on Michael Garland's work on
 * <a href="http://graphics.cs.uiuc.edu/~garland/research/quadrics.html">quadric error metrics</a>.
 *
 * <p>
 * A plane is fully determined by its normal <code>N</code> and the signed
 * distance <code>d</code> of the frame origin to this plane, or in other 
 * words the equation of this plane is <code>tN V + d = 0</code>.
 * The squared distance of a point to this plane is
 * </p>
 * <pre>
 *   D*D = (tN V + d) * (tN V + d)
 *       = tV (N tN) V + 2d tN V + d*d
 *       = tV A V + 2 tB V + c
 * </pre>
 * <p>
 * The quadric <code>Q=(A,B,c)=(N tN, dN, d*d)</code> is thus naturally
 * defined.  Addition of these quadrics have a simple form:
 * <code>Q1(V)+Q2(V)=(Q1+Q2)(V)</code> with
 * <code>Q1+Q2=(A1+A2, B1+B2, c1+c2)</code>
 * To compute the squared distance of a point to a set of planes, we can
 * then compute this quadric for each plane and sum each element of
 * these quadrics.  
 * </p>
 *
 * <p>
 * When an edge <code>(V1,V2)</code> is contracted into <code>V3</code>,
 * <code>Q1(V3)+Q2(V3)</code> represents the deviation to the set of
 * planes at <code>V1</code> and <code>V2</code>.  The cost of this
 * contraction is thus defined as <code>Q1(V3)+Q2(V3)</code>.
 * We want to minimize this error.  It can be shown that if <code>A</code>
 * is non singular, the optimal placement is for <code>V3=-inv(A) B</code>.
 * </p>
 *
 * <p>
 * The algorithm is straightforward:
 * </p>
 * <ol>
 *   <li>Quadrics are computed for all vertices.</li>
 *   <li>For each edge, compute the optimal placement and its cost.</li>
 *   <li>Loop on edges: starting with the lowest cost, each edge is processed
 *       until its cost is greater than the desired tolerance, and costs
 *       of adjacent edges are updated.</li>
 * </ol>
 *
 * <p>
 * The real implementation is slightly modified:
 * </p>
 * <ol type='a'>
 *   <li>Some checks must be performed to make sure that edge contraction does
 *       not modify the topology of the mesh.</li>
 *   <li>Optimal placement strategy can be chosen at run time among several
 *       choices.</li>
 *   <li>Boundary edges have to be preserved, otherwise they
 *       will shrink.  Virtual planes are added perpendicular to triangles at
 *       boundaries so that vertices can be decimated along those edges, but
 *       edges are stuck on their boundary.  Garland's thesis dissertation
 *       contains all informations about this process.</li>
 *   <li>Weights are added to compute quadrics, as described in Garland's
 *       dissertation.</li>
 *   <li>Edges are swapped after being contracted to improve triangle quality,
 *       as described by Frey in
 *       <a href="http://www.lis.inpg.fr/pages_perso/attali/DEA-IVR/PAPERS/frey00.ps">About Surface Remeshing</a>.</li>
 * </ol>
 */
public class DecimateVertex
{
	private static Logger logger=Logger.getLogger(DecimateVertex.class);
	private Mesh mesh;
	// 0.0 is not a valid value because it is a normalization factor.
	private double tolerance = 1.0;
	private int nrFinal = 0;
	private int placement = Quadric3DError.POS_EDGE;
	
	/**
	 * Creates a <code>DecimateVertex</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to refine.
	 * @param tol  maximal allowed error
	 */
	public DecimateVertex(Mesh m, double tol)
	{
		mesh = m;
		tolerance = tol * tol;
	}
	
	/**
	 * Creates a <code>DecimateVertex</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to refine.
	 * @param tol  maximal allowed error
	 * @param p  placement of the new point. Legitimate values are <code>POS_VERTEX</code> (at a vertex location), <code>POS_MIDDLE</code> (at the middle of the contracted edge), <code>POS_EDGE</code> (optimal placement on the contracted edge, this is the default) and <code>POS_OPTIMAL</code> (optimal placement).
	 */
	public DecimateVertex(Mesh m, double tol, int p)
	{
		mesh = m;
		tolerance = tol * tol;
		placement = p;
	}
	
	/**
	 * Creates a <code>DecimateVertex</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to refine.
	 * @param n  the desired number of triangles
	 */
	public DecimateVertex(Mesh m, int n)
	{
		mesh = m;
		nrFinal = n;
	}
	
	/**
	 * Creates a <code>DecimateVertex</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to refine.
	 * @param n  the desired number of triangles
	 * @param p  placement of the new point. Legitimate values are <code>POS_VERTEX</code> (at a vertex location), <code>POS_MIDDLE</code> (at the middle of the contracted edge), <code>POS_EDGE</code> (optimal placement on the contracted edge, this is the default) and <code>POS_OPTIMAL</code> (optimal placement).
	 */
	public DecimateVertex(Mesh m, int n, int p)
	{
		mesh = m;
		nrFinal = n;
		placement = p;
	}
	
	/**
	 * Contract all edges with the given error.
	 */
	public void compute()
	{
		logger.info("Running DecimateVertex");
		// Store triangles in an HashSet to speed up removal.
		HashSet newList = new HashSet(mesh.getTriangles());
		mesh.setTrianglesList(newList);
		int roughNrNodes = mesh.getTriangles().size()/2;
		HashMap quadricMap = new HashMap(roughNrNodes);
		NotOrientedEdge noe = new NotOrientedEdge();
		int nrTriangles = 0;
		for (Iterator itf = mesh.getTriangles().iterator(); itf.hasNext(); )
		{
			Triangle f = (Triangle) itf.next();
			if (f.isOuter())
				continue;
			nrTriangles++;
			for (int i = 0; i < 3; i++)
			{
				Vertex n = f.vertex[i];
				if (!quadricMap.containsKey(n))
					quadricMap.put(n, new Quadric3DError());
			}
		}
		// Compute quadrics
		PAVLSortedTree tree = new PAVLSortedTree();
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
			Matrix3D.prodVect3D(vect1, vect2, noe.getTempVector());
			double area = Matrix3D.norm(noe.getTempVector()) / tolerance;
			noe.computeNormal3D();
			double [] normal = noe.getTempVector();
			double d = - Matrix3D.prodSca(normal, f.vertex[0].getUV());
			for (int i = 0; i < 3; i++)
			{
				Quadric3DError q = (Quadric3DError) quadricMap.get(f.vertex[i]);
				q.addError(normal, d, area);
			}
			// Penalty for boundary triangles
			for (int i = 0; i < 3; i++)
			{
				noe.nextOTri();
				if (noe.hasAttributes(OTriangle.BOUNDARY))
				{
					//  Add a virtual plane
					//  In his dissertation, Garland suggests to
					//  add a weight proportional to squared edge
					//  length.
					noe.computeNormal3DT();
					//  length(nu) == length(noe)
					double [] nu = noe.getTempVector();
					for (int k = 0; k < 3; k++)
						nu[k] *= 100.0;
					d = - Matrix3D.prodSca(nu, noe.origin().getUV());
					Quadric3DError q1 = (Quadric3DError) quadricMap.get(noe.origin());
					Quadric3DError q2 = (Quadric3DError) quadricMap.get(noe.destination());
					//area = Matrix3D.norm(nu) / tolerance;
					area = 0.0;
					q1.addError(nu, d, area);
					q2.addError(nu, d, area);
				}
			}
		}
		unmarkEdges();
		computeTree(tree, quadricMap);
		contractAllVertices(tree, nrTriangles, quadricMap);
	}
	
	private void unmarkEdges()
	{
		NotOrientedEdge noe = new NotOrientedEdge();
		for (Iterator itf = mesh.getTriangles().iterator(); itf.hasNext(); )
		{
			Triangle f = (Triangle) itf.next();
			if (f.isOuter())
				continue;
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
				if (!noe.hasAttributes(OTriangle.BOUNDARY))
				{
					OTriangle.symOTri(noe, sym);
					sym.setAttributes(OTriangle.MARKED);
				}
				Vertex v1 = noe.origin();
				Vertex v2 = noe.destination();
				tree.insert(new NotOrientedEdge(noe), cost(v1, v2, quadricMap));
			}
		}
	}
	
	private boolean contractAllVertices(PAVLSortedTree tree, int nrTriangles, HashMap quadricMap)
	{
		int contracted = 0;
		HashSet trash = new HashSet();
		OTriangle ot = new OTriangle();
		NotOrientedEdge sym = new NotOrientedEdge();
		boolean noSwap = false;
		int cntNotContracted = 0;
		Stack notContracted = new Stack();
		Vertex v1 = null, v2 = null;
		Quadric3DError q1 = null, q2 = null;
		Vertex v3 = Vertex.valueOf(0.0, 0.0, 0.0);
		Quadric3DError q3 = new Quadric3DError();
		while (tree.size() > 0 && nrTriangles > nrFinal)
		{
			if (v1 != null)
			{
				// v1 and v2 have been removed from the
				// mesh,, they can be reused.
				v3 = v1;
				q3 = q1;
			}
			NotOrientedEdge edge = null;
			double cost = -1.0;
			for (Iterator itt = tree.iterator(); itt.hasNext(); )
			{
				edge = (NotOrientedEdge) itt.next();
				if (nrFinal == 0)
					cost = tree.getKey(edge);
				else
					cost = -1.0;
				if (cost > tolerance)
					break;
				edge.pullAttributes();
				v1 = edge.origin();
				v2 = edge.destination();
				assert v1 != v2 : edge;
				/* FIXME: add an option so that boundary nodes may be frozen. */
				q1 = (Quadric3DError) quadricMap.get(v1);
				q2 = (Quadric3DError) quadricMap.get(v2);
				assert q1 != null : v1;
				assert q2 != null : v2;
				q3.computeQuadric3DError(q1, q2);
				q3.optimalPlacement(v1, v2, q1, q2, placement, v3);
				if (edge.canContract(v3))
					break;
				if (logger.isDebugEnabled())
					logger.debug("Edge not contracted: "+edge);
				cntNotContracted++;
				// Add a penalty to edges which could not have been
				// contracted.  This has to be done outside this loop,
				// because PAVLSortedTree instances must not be modified
				// when walked through.
				// FIXME: Handle nrFinal != 0 case also
				if (nrFinal == 0)
				{
					notContracted.push(edge);
					notContracted.push(new Double(cost+0.1*(tolerance - cost)));
				}
			}
			if (cost > tolerance || edge == null)
				break;
			tree.remove(edge);
			// Update costs for edges which were not contracted
			while (notContracted.size() > 0)
			{
				double newCost = ((Double) notContracted.pop()).doubleValue();
				NotOrientedEdge f = (NotOrientedEdge) notContracted.pop();
				tree.update(f, newCost);
			}
			if (logger.isDebugEnabled())
				logger.debug("Contract edge: "+edge+" into "+v3);
			//  Keep track of triangles deleted when contracting
			Triangle t1 = edge.getTri();
			OTriangle.symOTri(edge, sym);
			Triangle t2 = sym.getTri();
			// Remove all edges of t1 and t2 from tree
			if (!t1.isOuter())
			{
				nrTriangles--;
				for (int i = 0; i < 3; i++)
				{
					edge.nextOTri();
					tree.remove(edge);
					assert !tree.contains(edge);
				}
			}
			if (!t2.isOuter())
			{
				nrTriangles--;
				for (int i = 0; i < 3; i++)
				{
					sym.nextOTri();
					tree.remove(sym);
					assert !tree.contains(sym);
				}
			}
			Vertex apex = edge.apex();
			// FIXME: is this test really necessary?
			if (apex == mesh.outerVertex)
				apex = sym.apex();
			//  Compute quadrics
			edge.contract(v3);
			contracted++;
			trash.add(t1);
			trash.add(t2);
			trashBin(trash);
			// Update edge costs
			quadricMap.remove(v1);
			quadricMap.remove(v2);
			quadricMap.put(v3, q3);
			if (!ot.find(v3, apex))
				throw new RuntimeException();
			assert ot.destination() == apex : ""+edge+"\n"+v3+"\n"+apex;
			do
			{
				ot.nextOTriOriginLoop();
				if (!ot.hasAttributes(OTriangle.OUTER))
					tree.update(new NotOrientedEdge(ot), cost(ot.destination(), v3, quadricMap));
				ot.setAttributes(OTriangle.MARKED);
			}
			while (ot.destination() != apex);
			if (noSwap)
				continue;
			
			ot.nextOTri();
			assert ot.apex() == v3;
			// Check if edges can be swapped
			while(true)
			{
				if (ot.checkSwap3D(0.95) >= 0.0)
				{
					// Swap edge
					edge = new NotOrientedEdge(ot);
					for (int i = 0; i < 3; i++)
					{
						edge.nextOTri();
						tree.remove(edge);
						assert !tree.contains(edge);
					}
					edge.symOTri();
					for (int i = 0; i < 3; i++)
					{
						edge.nextOTri();
						tree.remove(edge);
						assert !tree.contains(edge);
					}
					ot.swap();
					// Now ot = (ona) and edge = (dan)
					assert edge.destination() == ot.apex() : ot+" "+edge;
					assert edge.apex() == ot.destination() : ot+" "+edge;
					for (int i = 0; i < 3; i++)
					{
						edge.nextOTri();
						tree.insert(new NotOrientedEdge(edge), cost(edge.origin(), edge.destination(), quadricMap));
					}
					edge.nextOTri();
					edge.symOTri();
					for (int i = 0; i < 2; i++)
					{
						edge.nextOTri();
						tree.insert(new NotOrientedEdge(edge), cost(edge.origin(), edge.destination(), quadricMap));
					}
				}
				else
				{
					ot.nextOTriApexLoop();
					if (ot.origin() == apex)
						break;
				}
			}
		}
		// Remove deleted triangles from the list
		trashBin(trash);
		logger.info("Number of contracted edges: "+contracted);
		logger.info("Total number of edges not contracted during processing: "+cntNotContracted);
		logger.info("Number of edges still present in the binary tree: "+tree.size());
		return contracted > 0;
	}
	
	private static double cost(Vertex v1, Vertex v2, HashMap quadricMap)
	{
		Quadric3DError q1 = (Quadric3DError) quadricMap.get(v1);
		assert q1 != null : v1;
		Quadric3DError q2 = (Quadric3DError) quadricMap.get(v2);
		assert q2 != null : v2;
		double ret = Math.min(
		  q1.value(v1.getUV()) + q2.value(v1.getUV()),
		  q1.value(v2.getUV()) + q2.value(v2.getUV()));
		assert ret >= -1.e-2 : q1+"\n"+q2+"\n"+ret;
		return ret;
	}
	
	// TODO: this routine is obsolete since triangles are now
	// stored in an HahsSet.
	private void trashBin(HashSet trash)
	{
		// Remove deleted triangles from the list
		for (Iterator itf = trash.iterator(); itf.hasNext(); )
		{
			Triangle f = (Triangle) itf.next();
			mesh.remove(f);
		}
		trash.clear();
		assert mesh.isValid();
	}
	
	/**
	 * 
	 * @param args xmlDir, xmlFile, <-t telerance | -n triangle>, brepDir, brepFile
	 */
	//TODO have the same command line as in DecimateHalfEdge (it will break jcae-netbeans)
	public static void main(String[] args)
	{
		Mesh mesh=MeshReader.readObject3D(args[0], args[1], -1);
		if(args[2].equals("-n"))
			new DecimateVertex(mesh, Integer.parseInt(args[3])).compute();
		else if(args[2].equals("-t"))
			new DecimateVertex(mesh, Double.parseDouble(args[3])).compute();
		else
		{
			System.out.println("<xmlDir> <xmlFile> <-t telerance | -n triangle> <brepDir> <brepFile>");
			return;
		}
		MeshWriter.writeObject3D(mesh, args[0], args[1], args[4], args[5],1);
	}
}
