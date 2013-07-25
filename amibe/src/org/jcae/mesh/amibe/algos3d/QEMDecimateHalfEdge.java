/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2003,2006 by EADS CRC
    Copyright (C) 2007-2011, by EADS France

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
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.projection.MeshLiaison;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.metrics.EuclidianMetric3D;
import org.jcae.mesh.amibe.metrics.MetricSupport;

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
public class QEMDecimateHalfEdge extends AbstractAlgoHalfEdge
{
	private static final Logger LOGGER=Logger.getLogger(QEMDecimateHalfEdge.class.getName());
	private Quadric3DError.Placement placement = Quadric3DError.Placement.OPTIMAL;
	private HashMap<Vertex, Quadric3DError> quadricMap = null;
	private boolean freeEdgesOnly = false;
	private Vertex v3;
	private Quadric3DError q3 = new Quadric3DError();
	// vCostOpt and qCostOpt must be used only by cost() method.
	// Their aim is to avoid creating new objects for each cost() call.
	private final Vertex vCostOpt;
	private final Quadric3DError qCostOpt = new Quadric3DError();
	private static final boolean testDump = false;
	private final MetricSupport metrics;
	/**
	 * Creates a <code>QEMDecimateHalfEdge</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to refine.
	 * @param options  map containing key-value pairs to modify algorithm
	 *        behaviour.  Valid keys are <code>size</code>,
	 *        <code>placement</code> and <code>maxtriangles</code>.
	 */
	public QEMDecimateHalfEdge(final Mesh m, final Map<String, String> options)
	{
		this(m, null, options);
	}

	public QEMDecimateHalfEdge(final MeshLiaison liaison, final Map<String, String> options)
	{
		this(liaison.getMesh(), liaison, options);
	}

	private QEMDecimateHalfEdge(final Mesh m, final MeshLiaison meshLiaison, final Map<String, String> options)
	{
		super(m, meshLiaison);
		v3 = m.createVertex(0.0, 0.0, 0.0);
		vCostOpt = m.createVertex(0.0, 0.0, 0.0);
		metrics = new MetricSupport(mesh, options, "maxlength");
		for (final Map.Entry<String, String> opt: options.entrySet())
		{
			final String key = opt.getKey();
			final String val = opt.getValue();
			if (key.equals("size"))
			{
				final double sizeTarget = Double.parseDouble(val);
				LOGGER.info("Tolerance: "+sizeTarget);
				tolerance = sizeTarget * sizeTarget;
			}
			else if (key.equals("placement"))
			{
				placement = Quadric3DError.Placement.getByName(val);
				LOGGER.info("Placement: "+placement);
			}
			else if (key.equals("maxtriangles"))
			{
				nrFinal = Integer.valueOf(val).intValue();
				LOGGER.info("Nr max triangles: "+nrFinal);
			}
			else if (key.equals("coplanarity"))
			{
				minCos = Double.parseDouble(val);
				LOGGER.info("Minimum dot product of face normals allowed for swapping an edge: "+minCos);
			}
			else if ("freeEdgesOnly".equals(key))
			{
				freeEdgesOnly = Boolean.parseBoolean(val);
				LOGGER.info("freeEdgesOnly: "+freeEdgesOnly);
			}
			// This is a workaround for a bug which currently cannot be found.
			// When the metric is small close to a non-manifold or boundary
			// edge, adjacent triangles may be collapsed. So it break the border.
			else if("freezeNonManifold".equals(key))
			{
				for(Vertex v:mesh.getNodes())
				{
					if(v.getRef() != 0)
						v.setMutable(false);
				}
			}
			else if(!metrics.isKnownOption(key))
				throw new RuntimeException("Unknown option: "+key);
		}
		if (meshLiaison == null)
			mesh.buildRidges(minCos);
		if (freeEdgesOnly)
			setNoSwapAfterProcessing(true);
	}

	@Override
	protected void preCheck()
	{
		//disable checkNoInvertedTriangles as most of the time such triangle
		//are thin and removed by this algo
		assert mesh.checkNoDegeneratedTriangles();
	}

	public void setAnalyticMetric(MetricSupport.AnalyticMetricInterface m)
	{
		metrics.setAnalyticMetric(m);
	}

	public void setAnalyticMetric(int groupId, MetricSupport.AnalyticMetricInterface m)
	{
		metrics.setAnalyticMetric(groupId, m);
	}

	@Override
	public Logger thisLogger()
	{
		return LOGGER;
	}

	@Override
	public void preProcessAllHalfEdges()
	{
		metrics.compute();
		final int roughNrNodes = mesh.getTriangles().size()/2;
		quadricMap = new HashMap<Vertex, Quadric3DError>(roughNrNodes);
		for (Triangle af: mesh.getTriangles())
		{
			if (!af.isWritable())
				continue;
			for (int i = 0; i < 3; i++)
			{
				final Vertex n = af.vertex[i];
				if (!quadricMap.containsKey(n))
					quadricMap.put(n, new Quadric3DError());
			}
		}
		// Compute quadrics
		final double [] vect1 = new double[3];
		final double [] vect2 = new double[3];
		final double [] normal = new double[3];
		for (Triangle f: mesh.getTriangles())
		{
			if (!f.isWritable())
				continue;
			f.vertex[1].sub(f.vertex[0], vect1);
			f.vertex[2].sub(f.vertex[0], vect2);
			Matrix3D.prodVect3D(vect1, vect2, normal);
			double norm = Matrix3D.norm(normal);
			// This is in fact 2*area, but that does not matter
			double area = norm;
			if (tolerance > 0.0)
				area /= tolerance;
			if (norm > 1.e-20)
			{
				norm = 1.0 / norm;
				for (int k = 0; k < 3; k++)
					normal[k] *=  norm;
			}
			double d = - Matrix3D.prodSca(normal, f.vertex[0]);
			for (int i = 0; i < 3; i++)
			{
				final Quadric3DError q = quadricMap.get(f.vertex[i]);
				q.addError(normal, d, area);
			}
			// Penalty for boundary triangles
			HalfEdge e = (HalfEdge) f.getAbstractHalfEdge();
			for (int i = 0; i < 3; i++)
			{
				e = e.next();
				if (e.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
				{
					for (Iterator<AbstractHalfEdge> it = e.fanIterator(); it.hasNext(); )
					{
						HalfEdge b = (HalfEdge) it.next();
						//  Add a virtual plane
						//  In his dissertation, Garland suggests to
						//  add a weight proportional to squared edge
						//  length.
						//  Here norm(vect2) == norm(vect1)
						b.destination().sub(b.origin(), vect1);
						Matrix3D.prodVect3D(vect1, normal, vect2);
						norm = Matrix3D.norm(vect2);
						if (norm > 1.e-20)
						{
							double invNorm = 1.0 / norm;
							for (int k = 0; k < 3; k++)
								vect2[k] *=  invNorm;
						}
						d = - Matrix3D.prodSca(vect2, b.origin());
						final Quadric3DError q1 = quadricMap.get(b.origin());
						final Quadric3DError q2 = quadricMap.get(b.destination());
						q1.addWeightedError(vect2, d, norm);
						q2.addWeightedError(vect2, d, norm);
					}
				}
			}
		}
	}

	@Override
	protected void postComputeTree()
	{
		if (testDump)
			restoreState();
	}

	@Override
	protected void appendDumpState(final ObjectOutputStream out)
		throws IOException
	{
		out.writeObject(quadricMap);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void appendRestoreState(final ObjectInputStream q)
		throws IOException
	{
		try
		{
			quadricMap = (HashMap<Vertex, Quadric3DError>) q.readObject();
		}
		catch (final ClassNotFoundException ex)
		{
			ex.printStackTrace();
			System.exit(-1);
		}
	}

	@Override
	protected final double cost(final HalfEdge e)
	{
		final Vertex o = e.origin();
		final Vertex d = e.destination();
		if (!o.isMutable() && !d.isMutable())
			return Double.MAX_VALUE;
		final Quadric3DError q1 = quadricMap.get(o);
		assert q1 != null : o;
		final Quadric3DError q2 = quadricMap.get(d);
		assert q2 != null : d;
		qCostOpt.computeQuadric3DError(q1, q2);
		qCostOpt.optimalPlacement(o, d, q1, q2, placement, vCostOpt);
		final double ret = q1.value(vCostOpt) + q2.value(vCostOpt);
		// TODO: check why this assertion sometimes fail
		// assert ret >= -1.e-2 : q1+"\n"+q2+"\n"+ret;
		return ret;
	}

	@Override
	public boolean canProcessEdge(HalfEdge current)
	{
		current = uniqueOrientation(current);
		if (current.hasAttributes(AbstractHalfEdge.IMMUTABLE))
			return false;
		if (freeEdgesOnly && !current.hasAttributes(AbstractHalfEdge.BOUNDARY))
			return false;
		final Vertex v1 = current.origin();
		final Vertex v2 = current.destination();
		assert v1 != v2 : current;
		// If an endpoint is not writable, its neighborhood is
		// not fully determined and contraction must not be
		// performed.
		if (!v1.isWritable() || !v2.isWritable())
			return false;
		if (!v1.isMutable() && !v2.isMutable())
			return false;
		/* FIXME: add an option so that boundary nodes may be frozen. */
		final Quadric3DError q1 = quadricMap.get(v1);
		final Quadric3DError q2 = quadricMap.get(v2);
		assert q1 != null : v1;
		assert q2 != null : v2;
		q3.computeQuadric3DError(q1, q2);
		q3.optimalPlacement(v1, v2, q1, q2, placement, v3);
		v3.setLink(null);
		liaison.move(v3, v3, false);
		if (!mesh.canCollapseEdge(current, v3))
			return false;
		if (!metrics.isEmpty())
		{
			EuclidianMetric3D m3 = metrics.get(v3, current.getTri());
			if(!checkSize(v1, m3))
				return false;
			if(!checkSize(v2, m3))
				return false;
		}
		return true;
	}

	private boolean checkSize(Vertex v1, EuclidianMetric3D m3)
	{
		Iterator<Vertex> itnv = v1.getNeighbourIteratorVertex();
		while(itnv.hasNext())
		{
			Vertex n = itnv.next();
			if (n != mesh.outerVertex) {
				EuclidianMetric3D m = metrics.get(n);
				double d = MetricSupport.interpolatedDistance(v3, m3, n, m);
				if (d > 1.0)
					return false;
			}
		}
		return true;
	}

	@Override
	public void preProcessEdge()
	{
		if (testDump)
			dumpState();
	}

	@Override
	public HalfEdge processEdge(HalfEdge current, double costCurrent)
	{
		current = uniqueOrientation(current);
		Vertex v1 = current.origin();
		Vertex v2 = current.destination();
		// If v1 or v2 are on a beam, they must not be replaced by v3,
		// otherwise beams are no more connected to triangles.
		if (!v1.isMutable())
			v3 = v1;
		else if (!v2.isMutable())
			v3 = v2;
		if (LOGGER.isLoggable(Level.FINE))
		{
			LOGGER.fine("Contract edge: "+current+" into "+v3+"  cost="+costCurrent);
			if (current.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
			{
				LOGGER.fine("Non-manifold edge:");
				for (Iterator<AbstractHalfEdge> it = current.fanIterator(); it.hasNext(); )
					LOGGER.fine(" --> "+it.next());
			}
		}
		// HalfEdge instances on t1 and t2 will be deleted
		// when edge is contracted, and we do not know whether
		// they appear within tree or their symmetric ones,
		// so remove them now.
		for (Iterator<AbstractHalfEdge> it = current.fanIterator(); it.hasNext(); )
		{
			HalfEdge f = (HalfEdge) it.next();
			HalfEdge h = uniqueOrientation(f);
			if (!tree.remove(h))
				notInTree++;
			assert !tree.contains(h);
			h.clearAttributes(AbstractHalfEdge.MARKED);
			if (f.getTri().isWritable())
			{
				nrTriangles--;
				for (int i = 0; i < 2; i++)
				{
					f = f.next();
					removeFromTree(f);
				}
				f = f.next();
			}
		}
		HalfEdge sym = current.sym();
		if (sym.getTri().isWritable())
		{
			nrTriangles--;
			for (int i = 0; i < 2; i++)
			{
				sym = sym.next();
				removeFromTree(sym);
			}
			sym = sym.next();
		}
		//  Contract (v1,v2) into v3
		//  By convention, collapse() returns edge (v3, apex)
		assert (!current.hasAttributes(AbstractHalfEdge.OUTER));
		final Vertex apex = current.apex();
		// If v1 or v2 are manifold, they are removed from the
		// mesh and can be reused.  There is a problem vith vertex
		// on beams, they may be considered as manifold whereas they
		// are not.  Add an isMutable() test, but ideally isManifold()
		// should get fixed.
		Vertex vFree = null;
		Quadric3DError qFree = null;
		if (v1.isManifold() && v1.isMutable())
		{
			vFree = v1;
			qFree = quadricMap.remove(vFree);
		}
		if (v2.isManifold() && v2.isMutable())
		{
			vFree = v2;
			qFree = quadricMap.remove(vFree);
		}
		current = (HalfEdge) mesh.edgeCollapse(current, v3);
		if (liaison != null)
		{
			if (v3.sqrDistance3D(v1) < v3.sqrDistance3D(v2))
			{
				liaison.replaceVertex(v1, v3);
				liaison.removeVertex(v2);
			}
			else
			{
				liaison.replaceVertex(v2, v3);
				liaison.removeVertex(v1);
			}
		}
		// Now current == (v3*a)
		// Update edge costs
		quadricMap.put(v3, q3);
		assert current != null : v3+" not connected to "+apex;
		if(!metrics.isEmpty())
			metrics.put(v3, metrics.get(v3, current.getTri()));
		assert current.origin() == v3 : ""+current+"\n"+v3+"\n"+apex;
		assert current.apex() == apex : ""+current+"\n"+v3+"\n"+apex;
		v3 = vFree;
		if (v3 == null)
			v3 = mesh.createVertex(0.0, 0.0, 0.0);
		q3 = qFree;
		if (q3 == null)
			q3 = new Quadric3DError();
		updateIncidentEdges(current);
		if (!freeEdgesOnly && minCos >= -1.0)
			checkAndSwapAroundOrigin(current);
		return current.next();
	}

	private void updateIncidentEdges(HalfEdge current)
	{
		Vertex o = current.origin();
		if (!o.isReadable())
			return;
		if (o.isManifold())
		{
			Vertex apex = current.apex();
			do
			{
				current = current.nextOriginLoop();
				assert !current.hasAttributes(AbstractHalfEdge.NONMANIFOLD);
				if (current.destination().isReadable())
				{
					double newCost = cost(current);
					HalfEdge h = uniqueOrientation(current);
					if (tree.contains(h))
						tree.update(h, newCost);
					else
					{
						tree.insert(h, newCost);
						h.setAttributes(AbstractHalfEdge.MARKED);
					}
				}
			}
			while (current.apex() != apex);
			return;
		}
		Triangle [] list = (Triangle []) o.getLink();
		for (Triangle t: list)
		{
			HalfEdge f = (HalfEdge) t.getAbstractHalfEdge();
			if (f.destination() == o)
				f = f.next();
			else if (f.apex() == o)
				f = f.prev();
			assert f.origin() == o;
			Vertex d = f.destination();
			do
			{
				f = f.nextOriginLoop();
				if (f.destination().isReadable())
				{
					double newCost = cost(f);
					HalfEdge h = uniqueOrientation(f);
					if (tree.contains(h))
						tree.update(h, newCost);
					else
					{
						tree.insert(h, newCost);
						h.setAttributes(AbstractHalfEdge.MARKED);
					}
				}
			}
			while (f.destination() != d);
		}
	}

	private void checkAndSwapAroundOrigin(HalfEdge current)
	{
		Vertex d = current.destination();
		boolean redo = true;
		while(redo)
		{
			redo = false;
			while(true)
			{
				if (current.checkSwap3D(mesh, minCos) >= 0.0 && current.canSwapTopology())
				{
					// Swap edge
					for (int i = 0; i < 3; i++)
					{
						current = current.next();
						removeFromTree(current);
					}
					HalfEdge sym = current.sym();
					for (int i = 0; i < 2; i++)
					{
						sym = sym.next();
						removeFromTree(sym);
					}
					Vertex a = current.apex();
					boolean updateDestination = current.destination() == d;
					current = (HalfEdge) mesh.edgeSwap(current);
					swapped++;
					redo = true;
					if (updateDestination)
						d = current.destination();
					assert a == current.apex();
					for (int i = 0; i < 3; i++)
					{
						current = current.next();
						for (Iterator<AbstractHalfEdge> it = current.fanIterator(); it.hasNext(); )
						{
							HalfEdge e = uniqueOrientation((HalfEdge) it.next());
							addToTree(e);
						}
					}
					sym = current.next().sym();
					for (int i = 0; i < 2; i++)
					{
						sym = sym.next();
						for (Iterator<AbstractHalfEdge> it = sym.fanIterator(); it.hasNext(); )
						{
							HalfEdge e = uniqueOrientation((HalfEdge) it.next());
							addToTree(e);
						}
					}
				}
				else
				{
					current = current.nextOriginLoop();
					if (current.destination() == d)
						break;
				}
			}
		}
	}

	@Override
	public void postProcessAllHalfEdges()
	{
		if (liaison != null)
			liaison.updateAll();
		LOGGER.info("Number of contracted edges: "+processed);
		LOGGER.info("Total number of edges not contracted during processing: "+notProcessed);
		LOGGER.info("Total number of edges swapped to increase quality: "+swapped);
		//LOGGER.info("Number of edges which were not in the binary tree before being removed: "+notInTree);
		LOGGER.info("Number of edges still present in the binary tree: "+tree.size());
	}

	private final static String usageString = "<xmlDir> <-t tolerance | -n nrTriangles> <brepFile> <outputDir>";

	/**
	 * 
	 * @param args xmlDir, -t tolerance | -n triangle, brepFile, output
	 */
	public static void main(final String[] args)
	{
		final HashMap<String, String> options = new HashMap<String, String>();
		if(args.length != 5)
		{
			System.out.println(usageString);
			return;
		}
		if(args[1].equals("-n"))
			options.put("maxtriangles", args[2]);
		else if(args[1].equals("-t"))
			options.put("size", args[2]);
		else
		{
			System.out.println(usageString);
			return;
		}
		LOGGER.info("Load geometry file");
		final Mesh mesh = new Mesh();
		try
		{
			MeshReader.readObject3D(mesh, args[0]);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		new QEMDecimateHalfEdge(mesh, options).compute();
		final File brepFile=new File(args[3]);
		try
		{
			MeshWriter.writeObject3D(mesh, args[4], brepFile.getName());
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
}
