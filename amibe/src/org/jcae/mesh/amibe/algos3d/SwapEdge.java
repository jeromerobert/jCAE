/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005, by EADS CRC
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
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.projection.MeshLiaison;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Laplacian smoothing.
 */

public class SwapEdge extends AbstractAlgoHalfEdge
{
	private static final Logger LOGGER=Logger.getLogger(SwapEdge.class.getName());
	private int counter = 0;
	/** Swap only if the quality is improved by at least this factory */
	private double minQualityFactor;
	/**
	 * Creates a <code>SwapEdge</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to modify
	 * @param options  map containing key-value pairs to modify algorithm
	 *        behaviour.  Valid key is <code>coplanarity</code>.
	 */
	public SwapEdge(final Mesh m, final Map<String, String> options)
	{
		this(m, null, options);
	}

	public SwapEdge(final MeshLiaison liaison, final Map<String, String> options)
	{
		this(liaison.getMesh(), liaison, options);
	}

	private SwapEdge(final Mesh m, final MeshLiaison meshLiaison, final Map<String, String> options)
	{
		super(m, meshLiaison);
		for (final Map.Entry<String, String> opt: options.entrySet())
		{
			final String key = opt.getKey();
			final String val = opt.getValue();
			if (key.equals("coplanarity"))
			{
				minCos = Double.parseDouble(val);
				LOGGER.fine("Minimum dot product of face normals allowed for swapping an edge: "+minCos);
			}
			else if(key.equals("minQualityFactor"))
			{
				minQualityFactor = Double.parseDouble(val);
			}
			else
				throw new RuntimeException("Unknown option: "+key);
		}
		if (meshLiaison == null)
			mesh.buildRidges(minCos);
		counter = m.getTriangles().size() * 3;
		setNoSwapAfterProcessing(true);
	}

	@Override
	public Logger thisLogger()
	{
		return LOGGER;
	}

	@Override
	public void preProcessAllHalfEdges()
	{
	}

	@Override
	public double cost(final HalfEdge e)
	{
		if (liaison != null)
		{
			double[] tNormal = liaison.getBackgroundNormal(e.origin());
			if (e.checkSwapNormal(mesh, minCos, tNormal) < -1.0)
				return Double.MAX_VALUE;
		}

		return - e.checkSwap3D(mesh, minCos, 0, minQualityFactor);
	}
	
	@Override
	public boolean canProcessEdge(HalfEdge current)
	{
		return counter > 0 && !current.hasAttributes(AbstractHalfEdge.IMMUTABLE | AbstractHalfEdge.OUTER | AbstractHalfEdge.SHARP | AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD);
	}

	@Override
	public HalfEdge processEdge(HalfEdge current, double costCurrent)
	{
		if (LOGGER.isLoggable(Level.FINE))
			LOGGER.fine("Swap edge: "+current+"  cost="+costCurrent);
		counter --;
		for (int i = 0; i < 3; i++)
		{
			HalfEdge h = uniqueOrientation(current);
			if (!tree.remove(h))
				notInTree++;
			assert !tree.contains(h);
			h.clearAttributes(AbstractHalfEdge.MARKED);
			current = current.next();
		}
		HalfEdge sym = current.sym();
		for (int i = 0; i < 2; i++)
		{
			sym = sym.next();
			HalfEdge h = uniqueOrientation(sym);
			if (!tree.remove(h))
				notInTree++;
			h.clearAttributes(AbstractHalfEdge.MARKED);
		}
		current = (HalfEdge) mesh.edgeSwap(current);
		// Update edge costs
		for (int i = 0; i < 2; i++)
		{
			addToTree(uniqueOrientation(current));
			current = current.prev();
		}
		current = current.sym();
		for (int i = 0; i < 2; i++)
		{
			current = current.next();
			addToTree(uniqueOrientation(current));
		}
		return current.prev();
	}
	
	@Override
	public void postProcessAllHalfEdges()
	{
		LOGGER.info("Number of swapped edges: "+processed);
		//LOGGER.info("Number of edges which were not in the binary tree before being removed: "+notInTree);
		LOGGER.info("Number of edges still present in the binary tree: "+tree.size());
	}

	private final static String usageString = "<xmlDir> <coplanarity> <outputDir>";

	/**
	 * 
	 * @param args xmlDir, -t tolerance | -n triangle, brepFile, output
	 */
	public static void main(final String[] args)
	{
		if(args.length != 3)
		{
			System.out.println(usageString);
			return;
		}
		LOGGER.info("Load mesh");
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
		final HashMap<String, String> options = new HashMap<String, String>();
		options.put("coplanarity", args[1]);
		new SwapEdge(mesh, options).compute();
		try
		{
			MeshWriter.writeObject3D(mesh, args[2], null);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
}
