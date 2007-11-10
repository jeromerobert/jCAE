/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005, by EADS CRC
    Copyright (C) 2007, by EADS France

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
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;
import java.io.IOException;
import java.io.File;
import java.util.Map;
import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 * Laplacian smoothing.
 */

public class SwapEdge extends AbstractAlgoHalfEdge
{
	private static Logger logger=Logger.getLogger(SwapEdge.class);
	private double planarMin = 0.95;
	private int counter = 0;
	
	/**
	 * Creates a <code>SwapEdge</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to modify
	 * @param options  map containing key-value pairs to modify algorithm
	 *        behaviour.  Valid key is <code>angle</code>.
	 */
	public SwapEdge(final Mesh m, final Map<String, String> options)
	{
		super(m);
		for (final Map.Entry<String, String> opt: options.entrySet())
		{
			final String key = opt.getKey();
			final String val = opt.getValue();
			if (key.equals("angle"))
			{
				planarMin = new Double(val).doubleValue();
				logger.debug("Planar angle: "+planarMin);
			}
			else
				throw new RuntimeException("Unknown option: "+key);
		}
		counter = m.getTriangles().size() * 3;
	}
	
	@Override
	public Logger thisLogger()
	{
		return logger;
	}

	@Override
	public void preProcessAllHalfEdges()
	{
	}

	@Override
	public double cost(final HalfEdge e)
	{
		return - e.checkSwap3D(planarMin);
	}
	
	@Override
	public boolean canProcessEdge(HalfEdge current)
	{
		return counter > 0 && !current.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD);
	}

	@Override
	public HalfEdge processEdge(HalfEdge current, double costCurrent)
	{
		if (logger.isDebugEnabled())
			logger.debug("Swap edge: "+current+"  cost="+costCurrent);
		counter --;
		for (int i = 0; i < 3; i++)
		{
			HalfEdge h = uniqueOrientation(current);
			if (!tree.remove(uniqueOrientation(current)))
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
			current = current.prev();
			addToTree(uniqueOrientation(current));
		}
		for (int i = 0; i < 2; i++)
		{
			current = current.prev();
			addToTree(uniqueOrientation(current));
		}
		return current.prev();
	}
	
	@Override
	public void postProcessAllHalfEdges()
	{
		logger.info("Number of swapped edges: "+processed);
		//logger.info("Number of edges which were not in the binary tree before being removed: "+notInTree);
		logger.info("Number of edges still present in the binary tree: "+tree.size());
	}

	private final static String usageString = "<xmlDir> <brepFile> <outputDir>";

	/**
	 * 
	 * @param args xmlDir, -t tolerance | -n triangle, brepFile, output
	 */
	public static void main(final String[] args)
	{
		final HashMap<String, String> options = new HashMap<String, String>();
		if(args.length != 3)
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
		new SwapEdge(mesh, options).compute();
		final File brepFile=new File(args[1]);
		try
		{
			MeshWriter.writeObject3D(mesh, args[2], "jcae3d", brepFile.getParent(), brepFile.getName());
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
}
