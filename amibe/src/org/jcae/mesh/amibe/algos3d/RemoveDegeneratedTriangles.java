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
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.projection.MeshLiaison;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.Math;

/**
 * Remove degenerated triangles.  Triangles whose edge ratio is greater than a
 * given (large) tolerance are removed. Such triangles are sorted out and
 * removed in turn, those of largest edge ratio processed first.  The shortest
 * edge of such a triangle is collapsed into its middle vertex.  Works with
 * non manifold edges as well.
 *
 * Remark:  the edge ratio is the ratio between the longest edge and the
 * shortest edge of a triangle.
 *
 * Example:  if a mesh m has already been performed, the following commands will
 * remove triangles of edge ratio greater than 50.0 (default value is 100.0).
 * <pre>
 *   RemoveDegeneratedTriangles rdt = new RemoveDegeneratedTriangles(m, 50.);
 *   rdt.compute();
 * </pre>
 *
 * WARNING:  this algorithm is intended to clean a mesh, hence all regular
 * edges (i.e. not OUTER) except IMMUTABLE are processed. Neither free edges
 * nor ridges should be tagged as IMMUTABLE (unless you know what you are
 * doing). This makes possible to remove small edges on ridges without changing
 * actually the geometry.
 *
 * TODO:  process flat triangles with bad aspect ratio but good edge ratio by
 * first splitting them into two degenerated triangles and updating the tree.
 */
public class RemoveDegeneratedTriangles extends AbstractAlgoHalfEdge
{
	private static final Logger LOGGER = Logger.getLogger(
		RemoveDegeneratedTriangles.class.getName());
	private Vertex collapseVertex = null;

	/**
	 * Creates a <code>RemoveDegeneratedTriangles</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to refine.
	 * @param options  map containing key-value pairs to modify algorithm
	 *        behaviour.  Valid key is <code>rho</code>.
	 */
	public RemoveDegeneratedTriangles(final Mesh m, final Map<String, String> options)
	{
		this(m, null, options);
	}

	public RemoveDegeneratedTriangles(final MeshLiaison liaison, final Map<String, String> options)
	{
		this(liaison.getMesh(), liaison, options);
	}

	public RemoveDegeneratedTriangles(final Mesh m, final MeshLiaison meshLiaison, final Map<String, String> options)
	{
		super(m, meshLiaison);
		/* Allow edges on ridges to be collapsed */
		/* TODO: how to ensure it has not been done already? */
		minCos = -2.0;
		/* Do not swap after removing triangles */
		setNoSwapAfterProcessing(true);
		/* Only 'a few' triangles should be removed */
		setProgressBarStatus(10);
		/* Tolerance is 1 / rho * rho; default value for rho is 100 */
		tolerance = 1. / 10000;
		for (final Map.Entry<String, String> opt: options.entrySet())
		{
			final String key = opt.getKey();
			final String val = opt.getValue();
			if (key.equals("rho"))
			{
				double sizeTarget = Double.valueOf(val).doubleValue();
				tolerance = 1. / (sizeTarget * sizeTarget);
			}
			else
				throw new RuntimeException("Unknown option: "+key);
		}
		/*
		 * This algorithm is intended to repair ill-shaped triangles without
		 * modifying the geometry. Since all regular edges are considered,
		 * only very small edges have to be considered. Hence set a minimal
		 * valid value for rho. Let this value be 2 for tests but a reasonable
		 * one is larger than 10.
		 */
		if (tolerance >= 0.25)
			throw new RuntimeException("Edge ratio 'rho' must be striclty greater than 2.0");
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

	/**
	 * Compute (square of inverse of) edge ratio.  Since 'rho' is greater than
	 * two, 'tolerance' = 1/rho**2 is smaller than 1/4 (and positive).  Hence
	 * the edge 'e' can be processed only if the ratio between its length and
	 * one of its neighbor is the inverse of the edge ratio of the triangle.
	 */
	@Override
	protected final double cost(final HalfEdge e)
	{
		double inverseSqrEdgeRatio = tolerance + 1.;
		int nfan = 0;

		/* square length of 'e' */
		double ae = e.origin().sqrDistance3D(e.destination());

		/* neighbors of 'e' (including non-manifold) */
		for (Iterator<AbstractHalfEdge> it = e.fanIterator(); it.hasNext(); )
		{
			HalfEdge fan = (HalfEdge) it.next();
			HalfEdge fn = fan.next();
			HalfEdge fp = fan.prev();
			double an = fn.origin().sqrDistance3D(fn.destination());
			double ap = fp.origin().sqrDistance3D(fp.destination());
			inverseSqrEdgeRatio = Math.min(inverseSqrEdgeRatio, ae/an);
			inverseSqrEdgeRatio = Math.min(inverseSqrEdgeRatio, ae/ap);
			nfan++;
		}
		/* free edges are considered but not their symmetric (OUTER) */
		if (e.hasSymmetricEdge() && !e.hasAttributes(AbstractHalfEdge.BOUNDARY))
		{
			HalfEdge sym = e.sym();
			assert !sym.hasAttributes(AbstractHalfEdge.OUTER) : "sym() edge is OUTER!";

			HalfEdge sn = sym.next();
			HalfEdge sp = sym.prev();
			double an = sn.origin().sqrDistance3D(sn.destination());
			double ap = sp.origin().sqrDistance3D(sp.destination());
			inverseSqrEdgeRatio = Math.min(inverseSqrEdgeRatio, ae/an);
			inverseSqrEdgeRatio = Math.min(inverseSqrEdgeRatio, ae/ap);
		}

		/* debug info if candidate */
		if (LOGGER.isLoggable(Level.FINE)) {
			if (inverseSqrEdgeRatio <= tolerance) {
				LOGGER.fine("Candidate edge: "+e);
				LOGGER.fine("Ratio: "+1./Math.sqrt(inverseSqrEdgeRatio));
				LOGGER.fine("Nr fan: "+nfan);
			}
		}
		return inverseSqrEdgeRatio;
	}

	@Override
	void postComputeTree()
	{
	}

	/**
	 * Can process if can collapse.
	 */
	@Override
	public boolean canProcessEdge(HalfEdge current)
	{
		/* ensure that current is not OUTER */
		current = uniqueOrientation(current);

		if (current.hasAttributes(AbstractHalfEdge.IMMUTABLE))
			return false;

		/* check that endpoints are writable */
		Vertex v1 = current.origin();
		Vertex v2 = current.destination();
		if (!v1.isWritable() || !v2.isWritable())
			return false;

		/* create middle vertex of current edge */
		collapseVertex = mesh.createVertex(0., 0., 0.);
		collapseVertex.middle(v1, v2);

		/* check if can collapse edge with middle vertex */
		return mesh.canCollapseEdge(current, collapseVertex);
	}

	/**
	 * Update cost of edges that are still existing.  The one that are removed
	 * during collapse are removed from tree in processEdge.
	 */
	private void updateTree(HalfEdge current)
	{
		if (!current.origin().isReadable() || !current.destination().isReadable())
			return;
		double newCost = cost(current);
		if (newCost > tolerance)
			return;
		updateCost(current, newCost);
	}

	/**
	 * Collapse edge with its middle vertex (created in canProcessEdge).
	 * Remove from tree all edges belonging to the triangles about to be
	 * removed by the collapse.
	 */
	@Override
	public HalfEdge processEdge(HalfEdge current, double costCurrent)
	{
		current = uniqueOrientation(current);
		if (LOGGER.isLoggable(Level.FINE))
		{
			LOGGER.fine("Collapse edge: "+current+" into "+collapseVertex+"  cost="+costCurrent);
			if (current.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
			{
				LOGGER.fine("Non-manifold edge:");
				for (Iterator<AbstractHalfEdge> it = current.fanIterator(); it.hasNext(); )
					LOGGER.fine(" --> "+it.next());
			}
		}
		/*
		 * HalfEdge instances on t1 and t2 will be deleted when edge is
		 * contracted, and we do not know whether they appear within tree or
		 * their symmetric ones, so remove them now.
		 * (see LengthDecimateHalfEdge)
		 */
		for (Iterator<AbstractHalfEdge> it = current.fanIterator(); it.hasNext(); )
		{
			HalfEdge f = (HalfEdge) it.next();
			HalfEdge h = removeOneFromTree(f);
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
		/* Contract (v1,v2) into collapseVertex. By convention, collapse()
		 * returns edge (collapseVertex, apex)
		 */
		assert (!current.hasAttributes(AbstractHalfEdge.OUTER));
		Vertex apex = current.apex();
		Vertex v1 = current.origin();
		Vertex v2 = current.destination();
		current = (HalfEdge) mesh.edgeCollapse(current, collapseVertex);
		/* Now current == (collapseVertex*a) */
		if (liaison != null)
		{
			liaison.removeVertex(v2);
			liaison.replaceVertex(v1, collapseVertex);
		}
		/* Update cost of modified edges */
		assert current != null : collapseVertex+" not connected to "+apex;
		assert current.origin() == collapseVertex : ""+current+"\n"+collapseVertex+"\n"+apex;
		assert current.apex() == apex : ""+current+"\n"+collapseVertex+"\n"+apex;
		assert mesh.isValid();
		if (current.origin().isManifold())
		{
			do
			{
				current = current.nextOriginLoop();
				assert !current.hasAttributes(AbstractHalfEdge.NONMANIFOLD);
				updateTree(current);
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
				updateTree(f);
			}
			while (f.destination() != d);
			current = f;
		}
		/* Since we do not swap after collapsing this valued is ignored */
		return current.next();
	}

	@Override
	public void postProcessAllHalfEdges()
	{
		LOGGER.info("Number of collapsed edges: "+processed);
		LOGGER.info("Total number of edges not collapsed during processing: "+notProcessed);
		LOGGER.info("Total number of edges swapped to increase quality: "+swapped);
		super.postProcessAllHalfEdges();
	}

	private final static String usageString = "<xmlDir> <rho> <brepFile> <outputDir>";

	/**
	 *
	 * @param args xmlDir, rho, brepFile, output
	 */
	public static void main(String[] args)
	{
		HashMap<String, String> options = new HashMap<String, String>();
		if(args.length != 4)
		{
			System.out.println(usageString);
			return;
		}
		options.put("rho", args[1]);
		LOGGER.info("Load geometry file");
		MeshTraitsBuilder mtb = MeshTraitsBuilder.getDefault3D();
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
		new RemoveDegeneratedTriangles(mesh, options).compute();
		File brepFile = new File(args[2]);
		try
		{
			MeshWriter.writeObject3D(mesh, args[3], brepFile.getName());
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
}
