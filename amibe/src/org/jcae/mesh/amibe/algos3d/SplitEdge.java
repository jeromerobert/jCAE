/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2003,2006 by EADS CRC
    Copyright (C) 2007,2008, by EADS France

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
 * the surface by {@link Vertex#discreteProject(Vertex)}; if this
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
	private static Logger logger=Logger.getLogger(SplitEdge.class.getName());
	private double [] newXYZ = new double[3];
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
		super(m);
		for (final Map.Entry<String, String> opt: options.entrySet())
		{
			final String key = opt.getKey();
			final String val = opt.getValue();
			if (key.equals("size"))
			{
				double sizeTarget = Double.valueOf(val).doubleValue();
				tolerance = 1.0 / (sizeTarget * sizeTarget);
			}
			else if (key.equals("maxtriangles"))
				nrFinal = Integer.valueOf(val).intValue();
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
	public void preProcessAllHalfEdges()
	{
	}

	@Override
	public double cost(final HalfEdge e)
	{
		double [] p0 = e.origin().getUV();
		double [] p1 = e.destination().getUV();
		double l2 = (p1[0] - p0[0]) * (p1[0] - p0[0]) +
		            (p1[1] - p0[1]) * (p1[1] - p0[1]) +
		            (p1[2] - p0[2]) * (p1[2] - p0[2]);
		if (l2 == 0.0)
			return Double.MAX_VALUE;
		return 1.0 / l2;
	}

	@Override
	public boolean canProcessEdge(HalfEdge current)
	{
		current = uniqueOrientation(current);
		// New point
		double [] p0 = current.origin().getUV();
		double [] p1 = current.destination().getUV();
		for (int i = 0; i < 3; i++)
			newXYZ[i] = 0.5*(p0[i]+p1[i]);
		insertedVertex = mesh.createVertex(newXYZ);
		if (current.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
		{
			// FIXME: Check deflection
			mesh.setRefVertexOnboundary(insertedVertex);
			return true;
		}
		// Discrete differential operators, and thus
		// discreteProject, does not work on boundary
		// nodes.
		Vertex vm = current.origin();
		if (vm.getRef() != 0)
			vm = current.destination();
		if (vm.getRef() == 0 && !vm.discreteProject(insertedVertex))
		{
			if (logger.isLoggable(Level.FINE))
				logger.fine("Point "+vm+" cannot be projected onto discrete surface!");
			return false;
		}

		if (tolerance <= 0.0)
			return true;
		double dapex = insertedVertex.distance3D(current.apex());
		if (!current.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
		{
			current = current.sym();
			dapex = Math.min(dapex, insertedVertex.distance3D(current.apex()));
		}
		if (dapex * dapex * tolerance * 16.0 > 1.0)
			return true;
		if (logger.isLoggable(Level.FINE))
			logger.fine("Point "+vm+" too near from apical vertex");
		return false;
	}

	private void updateTree(HalfEdge current)
	{
		if (!current.origin().isReadable() || !current.destination().isReadable())
			return;
		double newCost = cost(current);
		if (nrFinal == 0 && newCost > tolerance)
			return;
		HalfEdge h = uniqueOrientation(current);
		if (tree.contains(h))
			tree.update(h, newCost);
		else
		{
			tree.insert(h, newCost);
			h.setAttributes(AbstractHalfEdge.MARKED);
		}
	}

	@Override
	public HalfEdge processEdge(HalfEdge current, double costCurrent)
	{
		current = uniqueOrientation(current);
		if (logger.isLoggable(Level.FINE))
		{
			logger.fine("Split edge: "+current+" by "+insertedVertex+"  cost="+costCurrent);
			if (current.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
			{
				logger.fine("Non-manifold edge:");
				for (Iterator<AbstractHalfEdge> it = current.fanIterator(); it.hasNext(); )
					logger.fine(" --> "+it.next());
			}
		}
		if (current.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
		{
			for (Iterator<AbstractHalfEdge> it = current.fanIterator(); it.hasNext(); )
			{
				HalfEdge f = (HalfEdge) it.next();
				if (!tree.remove(uniqueOrientation(f)))
					notInTree++;
			}
		}
		else
			if (!tree.remove(current))
				notInTree++;
		current.clearAttributes(AbstractHalfEdge.MARKED);
		assert !tree.contains(current);
		mesh.vertexSplit(current, insertedVertex);
		assert current.destination() == insertedVertex : insertedVertex+" "+current;
		assert mesh.isValid();
		HalfEdge ret = current.next();
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
		logger.info("Number of splitted edges: "+processed);
		logger.info("Total number of edges not splitted during processing: "+notProcessed);
		logger.info("Total number of edges swapped to increase quality: "+swapped);
		//logger.info("Number of edges which were not in the binary tree before being removed: "+notInTree);
		logger.info("Number of edges still present in the binary tree: "+tree.size());
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
		logger.info("Load geometry file");
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
