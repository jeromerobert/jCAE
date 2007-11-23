/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2003,2006 by EADS CRC
    Copyright (C) 2007 by EADS France

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
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;
import java.io.IOException;
import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.log4j.Logger;

public class LengthDecimateHalfEdge extends AbstractAlgoHalfEdge
{
	private static Logger logger=Logger.getLogger(LengthDecimateHalfEdge.class);
	private Vertex v3;
	private boolean freeEdgeOnly = false;

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
		super(m);
		v3 = null;
		m.createVertex(0.0, 0.0, 0.0);
		for (final Map.Entry<String, String> opt: options.entrySet())
		{
			final String key = opt.getKey();
			final String val = opt.getValue();
			if (key.equals("size"))
			{
				tolerance = new Double(val).doubleValue();
				logger.debug("Tolerance: "+tolerance);
			}
			else if (key.equals("maxtriangles"))
			{
				nrFinal = Integer.valueOf(val).intValue();
				logger.debug("Nr max triangles: "+nrFinal);
			}
			else if (key.equals("freeEdgeOnly"))
			{
				freeEdgeOnly = new Boolean(val).booleanValue();
				logger.debug("freeEdgeOnly: "+freeEdgeOnly);
			}
			else
				throw new RuntimeException("Unknown option: "+key);
		}
	}
	
	@Override
	public Logger thisLogger()
	{
		return logger;
	}

	@Override
	protected void preProcessAllHalfEdges()
	{
	}

	@Override
	public double cost(final HalfEdge e)
	{
		if (freeEdgeOnly && !e.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
			return 2.0 * tolerance;
		return e.origin().distance3D(e.destination());
	}

	@Override
	public boolean canProcessEdge(HalfEdge current)
	{
		if (freeEdgeOnly && !current.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
				return false;
		current = uniqueOrientation(current);
		final Vertex v1 = current.origin();
		final Vertex v2 = current.destination();
		assert v1 != v2 : current;
		// If an endpoint is not writable, its neighborhood is
		// not fully determined and contraction must not be
		// performed.
		if (!v1.isWritable() || !v2.isWritable())
			return false;
		v3 = optimalPlacement(v1, v2);
		return (mesh.canCollapseEdge(current, v3));
	}

	private Vertex optimalPlacement(Vertex v1, Vertex v2)
	{
		if (v1.getRef() > 0)
			return v1;
		else if (v2.getRef() != 0)
			return v2;
		return v1;
	}

	@Override
	public HalfEdge processEdge(HalfEdge current, double costCurrent)
	{
		current = uniqueOrientation(current);
		if (logger.isDebugEnabled())
		{
			logger.debug("Contract edge: "+current+" into "+v3+"  cost="+costCurrent);
			if (current.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
			{
				logger.debug("Non-manifold edge:");
				for (Iterator<AbstractHalfEdge> it = current.fanIterator(); it.hasNext(); )
					logger.debug(" --> "+it.next());
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
		current = (HalfEdge) mesh.edgeCollapse(current, v3);
		// Now current == (v3*a)
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
		logger.info("Number of contracted edges: "+processed);
		logger.info("Total number of edges not contracted during processing: "+notProcessed);
		logger.info("Total number of edges swapped to increase quality: "+swapped);
		//logger.info("Number of edges which were not in the binary tree before being removed: "+notInTree);
		logger.info("Number of edges still present in the binary tree: "+tree.size());
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
		logger.info("Load geometry file");
		final Mesh mesh = new Mesh();
		try
		{
			MeshReader.readObject3D(mesh, args[0], "jcae3d", -1);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		new LengthDecimateHalfEdge(mesh, options).compute();
		final File brepFile=new File(args[3]);
		try
		{
			MeshWriter.writeObject3D(mesh, args[4], "jcae3d", brepFile.getName());
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}

}
