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

import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.HalfEdge;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.projection.MeshLiaison;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Split long edges.  Edges are sorted and splitted in turn, the longest edge
 * being processed first.
 * Example:  if a mesh has already been performed with a target size
 * of discr, the following commands will split edges longer then discr/2
 * <pre>
 *   SplitEdge split = new SplitEdge(m, 0.5*discr);
 *   split.compute();
 * </pre>
 * TODO: currently edges longer than sqrt(2)*discr/2 will be splitted
 *
 * When an interior edge is splitted, its midpoint is projected onto
 * the surface by {@link org.jcae.mesh.amibe.projection.QuadricProjection}; if this
 * projection fails, this edge is not splitted.  It is removed from
 * the tree because there are few chances that this projection works
 * later.  This means in particular that an interior edge whose
 * endpoints are both on boundaries cannot be splitted, because
 * discrete projection cannot be performed on boundary nodes.
 * As for now, boundary edges are always splitted, and the new point
 * is in the middle of this edge.
 *
 * In all cases, the distance between the newly inserted point and
 * apical vertices is computed; if it is too low, the edge is not
 * splitted to avoid bad triangles.
 * TODO: edges should be swapped too to improve triangle quality.
 */
public class SplitEdge extends AbstractAlgoHalfEdge
{
	private static final Logger LOGGER=Logger.getLogger(SplitEdge.class.getName());
	private Vertex insertedVertex = null;
	
	/**
	 * Creates a <code>SplitEdge</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to refine.
	 * @param options  map containing key-value pairs to modify algorithm
	 *        behaviour.  Valid keys are <code>size</code> and
	 *        <code>maxtriangles</code>.
	 */
	public SplitEdge(final Mesh m, final Map<String, String> options)
	{
		this(m, null, options);
	}

	public SplitEdge(final MeshLiaison liaison, final Map<String, String> options)
	{
		this(liaison.getMesh(), liaison, options);
	}

	public SplitEdge(final Mesh m, final MeshLiaison meshLiaison, final Map<String, String> options)
	{
		super(m, meshLiaison);
		moreTriangles = true;
		for (final Map.Entry<String, String> opt: options.entrySet())
		{
			final String key = opt.getKey();
			final String val = opt.getValue();
			if (key.equals("size"))
			{
				double sizeTarget = Double.valueOf(val).doubleValue();
				tolerance = - (sizeTarget * sizeTarget);
			}
			else if (key.equals("maxtriangles"))
				nrFinal = Integer.valueOf(val).intValue();
			else if (key.equals("coplanarity"))
			{
				minCos = Double.parseDouble(val);
				LOGGER.fine("Minimum dot product of face normals allowed for swapping an edge: "+minCos);
			}
			else
				throw new RuntimeException("Unknown option: "+key);
		}
		if (tolerance == 0.0 && nrFinal == 0)
			throw new RuntimeException("Either 'size' or 'maxtriangles' must be specified");
		if (meshLiaison == null)
			mesh.buildRidges(minCos);
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
	protected final double cost(final HalfEdge e)
	{
		return - e.origin().sqrDistance3D(e.destination());
	}

	@Override
	public boolean canProcessEdge(HalfEdge current)
	{
		current = uniqueOrientation(current);
		if (current.hasAttributes(AbstractHalfEdge.IMMUTABLE))
			return false;
		insertedVertex = mesh.createVertex(0, 0, 0);
		insertedVertex.middle(current.origin(), current.destination());
		return true;
	}

	private void updateTree(HalfEdge current)
	{
		if (!current.origin().isReadable() || !current.destination().isReadable())
			return;
		double newCost = cost(current);
		if (nrFinal == 0 && newCost > tolerance)
			return;
		updateCost(current, newCost);
	}

	@Override
	public HalfEdge processEdge(HalfEdge current, double costCurrent)
	{
		current = uniqueOrientation(current);
		if (LOGGER.isLoggable(Level.FINE))
		{
			LOGGER.fine("Split edge: "+current+" by "+insertedVertex+"  cost="+costCurrent);
			if (current.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
			{
				LOGGER.fine("Non-manifold edge:");
				for (Iterator<AbstractHalfEdge> it = current.fanIterator(); it.hasNext(); )
					LOGGER.fine(" --> "+it.next());
			}
		}
		if (current.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
		{
			for (Iterator<AbstractHalfEdge> it = current.fanIterator(); it.hasNext(); )
				removeOneFromTree((HalfEdge) it.next());
		}
		else
			removeOneFromTree(current);
		current.clearAttributes(AbstractHalfEdge.MARKED);
		mesh.vertexSplit(current, insertedVertex);
		nrTriangles += 2;
		assert current.destination() == insertedVertex : insertedVertex+" "+current;
		assert mesh.isValid();
		if (liaison != null)
		{
			liaison.addVertex(insertedVertex, current.origin());
		}

		HalfEdge ret = current.prev();
		// Update edge lengths
		if (current.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
		{
			for (Iterator<AbstractHalfEdge> it = current.fanIterator(); it.hasNext(); )
			{
				HalfEdge f = (HalfEdge) it.next();
				f = f.next();
				updateTree(f);
			}
			updateTree(current);
			current = current.next();
			current = current.sym();
			current = current.next();
			updateTree(current);
		}
		else
		{
			current = current.sym();
			Vertex d = current.destination();
			do
			{
				if (current.destination() != mesh.outerVertex)
					updateTree(current);
				current = current.nextOriginLoop();
			}
			while (current.destination() != d);
		}
		return ret;
	}
	
	@Override
	public void postProcessAllHalfEdges()
	{
		LOGGER.info("Number of splitted edges: "+processed);
		LOGGER.info("Total number of edges not splitted during processing: "+notProcessed);
		LOGGER.info("Total number of edges swapped to increase quality: "+swapped);
		super.postProcessAllHalfEdges();
	}

	private final static String usageString = "<xmlDir> <-t maxLength | -n nrTriangles> <brepFile> <outputDir>";

	/**
	 * 
	 * @param args xmlDir, -t tolerance | -n triangle, brepFile, output
	 */
	public static void main(String[] args)
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
		org.jcae.mesh.amibe.traits.MeshTraitsBuilder mtb = org.jcae.mesh.amibe.traits.MeshTraitsBuilder.getDefault3D();
		mtb.addTriangleSet();
		Mesh mesh = new Mesh(mtb);
		try
		{
			MeshReader.readObject3D(mesh, args[0]);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		new SplitEdge(mesh, options).compute();
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
