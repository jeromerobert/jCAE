/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2003,2006 by EADS CRC
    Copyright (C) 2007,2008,2009,2011, by EADS France

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
import org.jcae.mesh.amibe.projection.MeshLiaison;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;
import java.io.IOException;
import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.metrics.MetricSupport;

public class LengthDecimateHalfEdge extends AbstractAlgoHalfEdge
{
	private static final Logger LOGGER=Logger.getLogger(LengthDecimateHalfEdge.class.getName());
	private Vertex v3;
	private boolean freeEdgesOnly = false;
	private final double freeEdgeFactor;
	private double maxEdgeLength = -1;
	private final MetricSupport metrics;
	/**
	 * Creates a <code>LengthDecimateHalfEdge</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to refine.
	 * @param options  map containing key-value pairs to modify algorithm
	 *        behaviour.  Valid keys are <code>size</code>,
	 *        <code>placement</code> and <code>maxtriangles</code>.
	 */
	public LengthDecimateHalfEdge(final Mesh m, final Map<String, String> options)
	{
		this(m, null, options);
	}

	public LengthDecimateHalfEdge(final MeshLiaison meshLiaison, final Map<String, String> options)
	{
		this(meshLiaison.getMesh(), meshLiaison, options);
	}

	private LengthDecimateHalfEdge(final Mesh m, final MeshLiaison meshLiaison, final Map<String, String> options)
	{
		super(m, meshLiaison);
		v3 = null;
		m.createVertex(0.0, 0.0, 0.0);
		double freeEdgeTol = Double.NaN;
		tolerance = 1;
		metrics = new MetricSupport(mesh, options, "size");
		for (final Map.Entry<String, String> opt: options.entrySet())
		{
			String key = opt.getKey();
			String val = opt.getValue();
			if ("maxtriangles".equals(key))
			{
				nrFinal = Integer.parseInt(val);
				LOGGER.fine("Nr max triangles: "+nrFinal);
			}
			else if ("maxlength".equals(key))
			{
				maxEdgeLength = Double.parseDouble(val);
				LOGGER.fine("Max edge length: "+maxEdgeLength);
				maxEdgeLength = maxEdgeLength*maxEdgeLength;
			}
			else if ("freeEdgesOnly".equals(key))
			{
				freeEdgesOnly = Boolean.parseBoolean(val);
				LOGGER.fine("freeEdgesOnly: "+freeEdgesOnly);
			}
			else if ("freeEdgeTol".equals(key))
			{
				freeEdgeTol = Double.parseDouble(val);
				freeEdgeTol = freeEdgeTol*freeEdgeTol;
			}
			else if (key.equals("coplanarity"))
			{
				minCos = Double.parseDouble(val);
				LOGGER.fine("Minimum dot product of face normals allowed for swapping an edge: "+minCos);
			}
			else if(!metrics.isKnownOption(key))
				throw new RuntimeException("Unknown option: "+key);
		}
		if(Double.isNaN(freeEdgeTol))
			freeEdgeFactor = 1.0;
		else
			freeEdgeFactor = tolerance / freeEdgeTol;
		if (meshLiaison == null)
			mesh.buildRidges(minCos);
		if (freeEdgesOnly)
			setNoSwapAfterProcessing(true);
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
	protected void preProcessAllHalfEdges()
	{
		metrics.compute();
	}

	@Override
	protected final double cost(final HalfEdge e)
	{
		//Ensure that boundary and non manifold edges are never processed
		if (freeEdgesOnly && !e.hasAttributes(AbstractHalfEdge.IMMUTABLE | AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
			return 4.0 * tolerance;
		
		double toReturn = metrics.interpolatedDistance(e.origin(), e.destination());
		
		//Handle the case of specific tolerance for free edges
		if(freeEdgeFactor != 1.0 &&
			e.origin().getRef()>0 && e.destination().getRef()>0)
			toReturn = toReturn * freeEdgeFactor;

		return toReturn;
	}

	@Override
	public boolean canProcessEdge(HalfEdge current)
	{
		if (freeEdgesOnly && !current.hasAttributes(AbstractHalfEdge.IMMUTABLE | AbstractHalfEdge.SHARP | AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
			return false;
		current = uniqueOrientation(current);
		Vertex v1 = current.origin();
		Vertex v2 = current.destination();
		assert v1 != v2 : current;
		// If an endpoint is not writable, its neighborhood is
		// not fully determined and contraction must not be
		// performed.
		if (!v1.isWritable() || !v2.isWritable())
			return false;
		if (!v1.isMutable() && !v2.isMutable())
			return false;
		v3 = optimalPlacementGroups(v1, v2);
		if(v3 == null)
			return false;
		if (!mesh.canCollapseEdge(current, v3))
			return false;
		if (maxEdgeLength > 0.0)
		{
			for (Iterator<Vertex> itnv = v1.getNeighbourIteratorVertex(); itnv.hasNext(); )
			{
				Vertex n = itnv.next();
				if (n != mesh.outerVertex && v3.sqrDistance3D(n) > maxEdgeLength)
					return false;
			}
			for (Iterator<Vertex> itnv = v2.getNeighbourIteratorVertex(); itnv.hasNext(); )
			{
				Vertex n = itnv.next();
				if (n != mesh.outerVertex && v3.sqrDistance3D(n) > maxEdgeLength)
					return false;
			}
		}
		return true;
	}

	private Collection<Integer> getGroups(Vertex v)
	{
		Iterator<Triangle> it = v.getNeighbourIteratorTriangle();
		TreeSet<Integer> r = new TreeSet<Integer>();
		while(it.hasNext())
			r.add(it.next().getGroupId());
		return r;
	}

	/**
	 * Compute the optimal point if we are on differents borders of groups.
	 * Delegate to optimalPlacement in other cases.
	 */
	private Vertex optimalPlacementGroups(Vertex v1, Vertex v2)
	{
		Vertex toReturn;
		if(v1.isManifold() && v2.isManifold())
			toReturn = optimalPlacement(v1, v2);
		else
		{
			Collection<Integer> grps1 = getGroups(v1);
			Collection<Integer> grps2 = getGroups(v2);
			if(grps1.containsAll(grps2))
			{
				if(grps1.size() == grps2.size())
				{
					//both points are on the same group border so we delegate to
					//optimalPlacement as if it was manifold
					toReturn = optimalPlacement(v1, v2);
				}
				else
					toReturn = v1;
			}
			else if(grps2.containsAll(grps1))
				toReturn = v2;
			else
				//group set are disjoin so collapse is forbidden
				toReturn = null;
		}
		return toReturn;
	}

	private Vertex optimalPlacement(Vertex v1, Vertex v2)
	{
		if (v1.getRef() > 0 || !v1.isMutable())
			return v1;
		else if (v2.getRef() != 0 || !v2.isMutable())
			return v2;
		double[] uv1 = v1.getUV();
		double[] uv2 = v2.getUV();
		return mesh.createVertex(
			(uv1[0] + uv2[0]) / 2.0,
			(uv1[1] + uv2[1]) / 2.0,
			(uv1[2] + uv2[2]) / 2.0);
	}

	@Override
	public HalfEdge processEdge(HalfEdge current, double costCurrent)
	{
		current = uniqueOrientation(current);
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
		Vertex apex = current.apex();
		Vertex v1 = current.origin();
		Vertex v2 = current.destination();
		current = (HalfEdge) mesh.edgeCollapse(current, v3);
		// Now current == (v3*a)
		if (liaison != null)
		{
			Triangle bgT = liaison.removeVertex(v1);
			liaison.removeVertex(v2);
			liaison.addVertex(v3, bgT);
		}
		// Update edge costs
		assert current != null : v3+" not connected to "+apex;
		assert current.origin() == v3 : ""+current+"\n"+v3+"\n"+apex;
		assert current.apex() == apex : ""+current+"\n"+v3+"\n"+apex;
		if (current.origin().isManifold())
		{
			do
			{
				current = current.nextOriginLoop();
				assert !current.hasAttributes(AbstractHalfEdge.NONMANIFOLD);
				if (current.destination().isReadable() && current.origin().isReadable())
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
			return current.next();
		}
		Vertex o = current.origin();
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
				if (f.destination().isReadable() && f.origin().isReadable())
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
			current = f;
		}
		return current.next();
	}
	
	@Override
	public void postProcessAllHalfEdges()
	{
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
		HashMap<String, String> options = new HashMap<String, String>();
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
		Mesh mesh = new Mesh();
		try
		{
			MeshReader.readObject3D(mesh, args[0]);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		new LengthDecimateHalfEdge(mesh, options).compute();
		File brepFile=new File(args[3]);
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
