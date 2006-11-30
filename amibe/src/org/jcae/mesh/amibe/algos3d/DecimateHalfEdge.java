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
import org.jcae.mesh.amibe.ds.HalfEdge;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.OTriangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Quadric3DError;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;
import java.io.File;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
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
public class DecimateHalfEdge extends AbstractAlgoHalfEdge
{
	private static Logger logger=Logger.getLogger(DecimateHalfEdge.class);
	private int placement = Quadric3DError.POS_EDGE;
	private HashMap quadricMap = null;
	private OTriangle ot = new OTriangle();
	private Vertex v1 = null, v2 = null;
	private Quadric3DError q1 = null, q2 = null;
	private Vertex v3 = Vertex.valueOf(0.0, 0.0, 0.0);
	private Quadric3DError q3 = new Quadric3DError();
	private static final boolean testDump = false;
	
	/**
	 * Creates a <code>DecimateHalfEdge</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to refine.
	 * @param tol  maximal allowed error
	 * @param p  placement of the new point. Legitimate values are <code>POS_VERTEX</code> (at a vertex location), <code>POS_MIDDLE</code> (at the middle of the contracted edge), <code>POS_EDGE</code> (optimal placement on the contracted edge, this is the default) and <code>POS_OPTIMAL</code> (optimal placement).
	 */
	public DecimateHalfEdge(Mesh m, Map options)
	{
		super(m, options);
		for (Iterator it = options.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry opt = (Map.Entry) it.next();
			String key = (String) opt.getKey();
			String val = (String) opt.getValue();
			if (key.equals("size"))
			{
				double sizeTarget = new Double(val).doubleValue();
				tolerance = sizeTarget * sizeTarget;
			}
			else if (key.equals("placement"))
				placement = Integer.valueOf(val).intValue();
			else if (key.equals("maxtriangles"))
				nrFinal = Integer.valueOf(val).intValue();
			else
				throw new RuntimeException("Unknown option: "+key);
		}
	}
	
	public Logger thisLogger()
	{
		return logger;
	}

	public void preProcessAllHalfEdges()
	{
		// Store triangles in a LinkedHashSet to speed up removal.
		LinkedHashSet newList = new LinkedHashSet(mesh.getTriangles());
		mesh.setTrianglesList(newList);
		int roughNrNodes = mesh.getTriangles().size()/2;
		quadricMap = new HashMap(roughNrNodes);
		for (Iterator itf = mesh.getTriangles().iterator(); itf.hasNext(); )
		{
			Triangle f = (Triangle) itf.next();
			if (f.isOuter())
				continue;
			for (int i = 0; i < 3; i++)
			{
				Vertex n = f.vertex[i];
				if (!quadricMap.containsKey(n))
					quadricMap.put(n, new Quadric3DError());
			}
		}
		// Compute quadrics
		double [] vect1 = new double[3];
		double [] vect2 = new double[3];
		double [] normal = new double[3];
		for (Iterator itf = mesh.getTriangles().iterator(); itf.hasNext(); )
		{
			Triangle f = (Triangle) itf.next();
			if (f.isOuter())
				continue;
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
			Matrix3D.prodVect3D(vect1, vect2, normal);
			double norm = Matrix3D.norm(normal);
			double area = norm;
			if (tolerance > 0.0)
				area /= tolerance;
			if (norm > 1.e-20)
			{
				for (int i = 0; i < 3; i++)
					normal[i] /=  norm;
			}
			double d = - Matrix3D.prodSca(normal, f.vertex[0].getUV());
			for (int i = 0; i < 3; i++)
			{
				Quadric3DError q = (Quadric3DError) quadricMap.get(f.vertex[i]);
				q.addError(normal, d, area);
			}
			// Penalty for boundary triangles
			HalfEdge e = f.getHalfEdge();
			for (int i = 0; i < 3; i++)
			{
				e = e.next();
				if (e.hasAttributes(OTriangle.BOUNDARY) || e.hasAttributes(OTriangle.NONMANIFOLD))
				{
					//  Add a virtual plane
					//  In his dissertation, Garland suggests to
					//  add a weight proportional to squared edge
					//  length.
					//  length(vect2) == length(e)
					p0 = e.origin().getUV();
					p1 = e.destination().getUV();
					vect1[0] = p1[0] - p0[0];
					vect1[1] = p1[1] - p0[1];
					vect1[2] = p1[2] - p0[2];
					Matrix3D.prodVect3D(vect1, normal, vect2);
					for (int k = 0; k < 3; k++)
						vect2[k] *= 100.0;
					d = - Matrix3D.prodSca(vect2, e.origin().getUV());
					Quadric3DError q1 = (Quadric3DError) quadricMap.get(e.origin());
					Quadric3DError q2 = (Quadric3DError) quadricMap.get(e.destination());
					//area = Matrix3D.norm(vect2) / tolerance;
					area = 0.0;
					q1.addError(vect2, d, area);
					q2.addError(vect2, d, area);
				}
			}
		}
	}

	protected void postComputeTree()
	{
		if (testDump)
			restoreState();
	}

	protected void appendDumpState(ObjectOutputStream out)
		throws IOException
	{
		out.writeObject(quadricMap);
	}

	protected void appendRestoreState(ObjectInputStream q)
		throws IOException
	{
		try
		{
			quadricMap = (HashMap) q.readObject();
		}
		catch (ClassNotFoundException ex)
		{
			ex.printStackTrace();
			System.exit(-1);
		}
	}

	public double cost(HalfEdge e)
	{
		Vertex o = e.origin();
		Vertex d = e.destination();
		Quadric3DError q1 = (Quadric3DError) quadricMap.get(o);
		assert q1 != null : o;
		Quadric3DError q2 = (Quadric3DError) quadricMap.get(d);
		assert q2 != null : d;
		double ret = Math.min(
		  q1.value(o.getUV()) + q2.value(o.getUV()),
		  q1.value(d.getUV()) + q2.value(d.getUV()));
		// TODO: check why this assertion sometimes fail
		// assert ret >= -1.e-2 : q1+"\n"+q2+"\n"+ret;
		return ret;
	}

	public boolean canProcessEdge(HalfEdge current)
	{
		v1 = current.origin();
		v2 = current.destination();
		assert v1 != v2 : current;
		/* FIXME: add an option so that boundary nodes may be frozen. */
		q1 = (Quadric3DError) quadricMap.get(v1);
		q2 = (Quadric3DError) quadricMap.get(v2);
		assert q1 != null : current;
		assert q2 != null : current;
		q3.computeQuadric3DError(q1, q2);
		q3.optimalPlacement(v1, v2, q1, q2, placement, v3);
		current.copyOTriangle(ot);
		// For now, do not contract non manifold edges
		return (!current.hasAttributes(OTriangle.NONMANIFOLD) && ot.canContract(v3));
	}

	public void preProcessEdge()
	{
		if (testDump)
			dumpState();
		if (v1 != null)
		{
			// v1 and v2 have been removed from the mesh,
			// they can be reused.
			v3 = v1;
			q3 = q1;
		}
	}

	public HalfEdge processEdge(HalfEdge current)
	{
		if (logger.isDebugEnabled())
			logger.debug("Contract edge: "+current+" into "+v3);
		Triangle t1 = current.getTri();
		// HalfEdge instances on t1 and t2 will be deleted
		// when edge is contracted, and we do not know whether
		// they appear within tree or their symmetric ones,
		// so remove them now.
		tree.remove(current.notOriented());
		if (!t1.isOuter())
		{
			nrTriangles--;
			for (int i = 0; i < 2; i++)
			{
				current = current.next();
				tree.remove(current.notOriented());
				assert !tree.containsValue(current.notOriented());
			}
			current = current.next();
		}
		HalfEdge sym = current.sym();
		Triangle t2 = sym.getTri();
		if (!t2.isOuter())
		{
			nrTriangles--;
			for (int i = 0; i < 2; i++)
			{
				sym = sym.next();
				tree.remove(sym.notOriented());
				assert !tree.containsValue(sym.notOriented());
			}
			sym = sym.next();
		}
		Vertex apex = current.apex();
		// FIXME: is this test really necessary?
		if (apex == Vertex.outer)
			apex = sym.apex();
		//  Contract (v1,v2) into v3
		current.contract(mesh, v3);
		quadricMap.remove(v1);
		quadricMap.remove(v2);
		// Update edge costs
		quadricMap.put(v3, q3);
		current = HalfEdge.find(v3, apex);
		assert current != null : v3+" not connected to "+apex;
		assert current.destination() == apex : ""+current+"\n"+v3+"\n"+apex;
		do
		{
			current = current.nextOriginLoop();
			if (current.destination() != Vertex.outer)
				tree.update(current.notOriented(), cost(current));
		}
		while (current.destination() != apex);
		return current.next();
	}
	
	public void postProcessAllHalfEdges()
	{
		int cnt = 0;
		HalfEdge edge = (HalfEdge) tree.first();
		while (edge != null)
		{
			if (tree.getKey(edge) > tolerance)
				break;
			cnt++;
			edge = (HalfEdge) tree.next();
		}
		logger.info("Number of contracted edges: "+processed);
		logger.info("Total number of edges not contracted during processing: "+cntNotProcessed);
		logger.info("Number of edges which could have been contracted: "+cnt);
		logger.info("Number of other edges not contracted: "+(tree.size() - cnt));
	}

	/**
	 * 
	 * @param args xmlDir, -t telerance | -n triangle, brepFile, output
	 */
	public static void main(String[] args)
	{
		HashMap options = new HashMap();
		if(args[1].equals("-n"))
			options.put("maxtriangles", args[2]);
		else if(args[1].equals("-t"))
			options.put("size", args[2]);
		else
		{
			System.out.println("<xmlDir> <-t telerance | -n triangle> <brepFile> <output>");
			return;
		}
		logger.info("Load geometry file");
		Mesh mesh=MeshReader.readObject3D(args[0], "jcae3d", -1);
		new DecimateHalfEdge(mesh, options).compute();
		File brepFile=new File(args[4]);
		MeshWriter.writeObject3D(mesh, args[4], "jcae3d", brepFile.getParent(), brepFile.getName(),1);
	}
}
