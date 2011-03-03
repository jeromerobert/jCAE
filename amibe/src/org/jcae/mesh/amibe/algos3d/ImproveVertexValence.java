/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2011, by EADS France

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
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.HalfEdge;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.projection.MeshLiaison;
import org.jcae.mesh.amibe.metrics.Metric;
import gnu.trove.TObjectIntHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.logging.Logger;

/**
 * Remove vertices with low valence, and duplicate vertices with high valence.
 */

public class ImproveVertexValence extends AbstractAlgoVertex
{
	private static final Logger LOGGER=Logger.getLogger(ImproveVertexValence.class.getName());
	private TObjectIntHashMap<Vertex> map;
	private final LinkedHashSet<Vertex> immutableNodes = new LinkedHashSet<Vertex>();
	private int valence3;
	private int valence4;
	private int inserted;
	
	/**
	 * Creates a <code>ImproveConnectivity</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to modify
	 * @param options  map containing key-value pairs to modify algorithm
	 *        behaviour.  Valid key is <code>coplanar</code>.
	 */
	public ImproveVertexValence(final Mesh m, final Map<String, String> options)
	{
		this(m, null, options);
	}

	public ImproveVertexValence(final MeshLiaison liaison, final Map<String, String> options)
	{
		this(liaison.getMesh(), liaison, options);
	}

	private ImproveVertexValence(final Mesh m, final MeshLiaison meshLiaison, final Map<String, String> options)
	{
		super(m, meshLiaison);
		for (final Map.Entry<String, String> opt: options.entrySet())
		{
			final String key = opt.getKey();
			final String val = opt.getValue();
			if (key.equals("coplanarity"))
			{
				minCos = Double.parseDouble(val);
				LOGGER.fine("Coplanar value: "+minCos);
			}
			else
				throw new RuntimeException("Unknown option: "+key);
		}
		// Do not change vertices with a valence of 5,6,7
		tolerance = 40.0;
		if (meshLiaison == null)
			mesh.buildRidges(minCos);
	}
	
	@Override
	public Logger thisLogger()
	{
		return LOGGER;
	}

	@Override
	public void preProcessAllVertices()
	{
		map = new TObjectIntHashMap<Vertex>(mesh.getTriangles().size() / 2);
 		AbstractHalfEdge ot = null;
		for (Triangle t: mesh.getTriangles())
		{
			if (t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			ot = t.getAbstractHalfEdge(ot);
			for (Vertex v: t.vertex)
				map.put(v, map.get(v) + 1);
			for (int i = 0; i < 3; i++)
			{
				ot = ot.next();
				if (ot.hasAttributes(AbstractHalfEdge.IMMUTABLE | AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD | AbstractHalfEdge.SHARP))
				{
					immutableNodes.add(ot.origin());
					immutableNodes.add(ot.destination());
				}
			}
		}
	}

	@Override
	protected final double cost(final Vertex v)
	{
		if (!v.isManifold() || !v.isMutable())
			return 100.0;
		int q = map.get(v);
		if (q > 10)
			return -q;
		switch(q)
		{
			case 1:
			case 2:
				return 100.0;
			case 3:
				return 3.1;
			case 4:
				return 4.1;
			case 5:
			case 6:
			case 7:
				return 50.0;
			case 8: return 2.9;
			case 9: return 1.9;
			case 10: return 0.5;
			default:
				throw new IllegalStateException();
		}
	}

	@Override
	public boolean canProcessVertex(Vertex v)
	{
		if (!v.isManifold() || !v.isMutable() || immutableNodes.contains(v))
			return false;
		double [] tNormal = liaison.getBackgroundNormal(v);
		Triangle t = (Triangle) v.getLink();
		HalfEdge ot = (HalfEdge) t.getAbstractHalfEdge();
		if (ot.destination() == v)
			ot = ot.next();
		else if (ot.apex() == v)
			ot = ot.prev();
		assert ot.origin() == v;
		double checkNormal = ot.checkSwapNormal(mesh, minCos, tNormal);
		return (checkNormal > -1.0);
	}

	@Override
	public boolean processVertex(Vertex v, double cost)
	{
		// FIXME: If a penalty has been added, we do not know what the
		// real valence is.  Skip this vertex for now
		if (cost != cost(v))
			return false;
		Triangle t = (Triangle) v.getLink();
		AbstractHalfEdge ot = t.getAbstractHalfEdge();
		if (ot.destination() == v)
			ot = ot.next();
		else if (ot.apex() == v)
			ot = ot.prev();
		assert ot.origin() == v;
		if (cost > 3.0 && cost < 5.0)
		{
			// Very low valence, try to remove vertex
			int iVal = (cost > 3.0 && cost < 4.0 ? 3 : 4);
			ot = checkLowValence(ot, iVal);
			if (ot == null)
				return false;
			if (!mesh.canCollapseEdge(ot, ot.destination()))
				return false;
			// Fix valence of incident vertices
			fixIncidentVertices(ot, iVal);
			ot = mesh.edgeCollapse(ot, ot.destination());
			if (iVal == 3)
				valence3++;
			else
				valence4++;
			for (Iterator<Vertex> it = ot.origin().getNeighbourIteratorVertex(); it.hasNext(); )
			{
				Vertex o = it.next();
				if (!canProcessVertex(o))
					continue;
				double val = cost(o);
				if (!tree.contains(o))
					tree.insert(o, val);
				else
					tree.update(o, val);
			}
		}
		else if (cost < 3.0)
		{
			// Valence is 8 or more, try to insert a vertex
			ot = checkLargeValence(ot);
			if (ot == null)
				return false;
			double[] newPt = new double[3];
			double [] p1 = v.getUV();
			double [] p2 = ot.destination().getUV();
			for (int i = 0; i < 3; i++)
				newPt[i] = 0.5*(p1[i] + p2[i]);
			Vertex newV = mesh.createVertex(newPt);
			liaison.addVertex(newV, liaison.getBackgroundTriangle(v));
			liaison.move(newV, newV.getUV());
			Vertex a = ot.apex();
			Vertex n = ot.sym().apex();
			double [] tNormal = liaison.getBackgroundNormal(v);
			ot = mesh.vertexSplit(ot, newV);
			// The valence of v has not been changed by
			// inserting a Vertex, we now try to swap an edge.
			// If we can't, revert this split.
			HalfEdge h = (HalfEdge) ot.nextOrigin();
			if (h.checkSwapNormal(mesh, minCos, tNormal) < -1.0)
			{
				h = (HalfEdge) ot.sym().next();
				if (h.checkSwapNormal(mesh, minCos, tNormal) < -1.0)
				{
					mesh.edgeCollapse(ot, ot.origin());
					liaison.removeVertex(newV);
					thisLogger().warning("ERR "+v);
					return false;
				}

			}
			assert h.origin() == v;
			map.put(a, map.get(a) + 1);
			map.put(n, map.get(n) + 1);
			map.put(newV, 4);
			map.put(v, map.get(v) - 1);
			map.put(h.destination(), map.get(h.destination()) - 1);
			map.put(h.apex(), map.get(h.apex()) + 1);
			map.put(h.sym().apex(), map.get(h.sym().apex()) + 1);
			Vertex [] modified = new Vertex[] {
				a, n, newV, v, h.destination(), h.apex(), h.sym().apex()
			};
			mesh.edgeSwap(h);
			for (Vertex o : modified)
			{
				if (!canProcessVertex(o))
					continue;
				double val = cost(o);
				if (!tree.contains(o))
					tree.insert(o, val);
				else
					tree.update(o, val);
			}
			inserted++;
		}

		return true;
	}

	private void fixIncidentVertices(AbstractHalfEdge ot, int valence)
	{
		for (int i = valence; i > 0; --i)
		{
			Vertex d = ot.destination();
			if (valence == 3)
				map.put(d, map.get(d) - 1);
			else if (valence == 4 && i%2 != 0)
				map.put(d, map.get(d) - 1);
		}
	}

	private AbstractHalfEdge checkLowValence(AbstractHalfEdge ot, int valence)
	{
		Vertex o = ot.origin();
		double[] xyz = o.getUV();
		Metric mo = mesh.getMetric(o);
		Vertex d = ot.destination();
		double dMin = Double.MAX_VALUE;
		int iMin = -1;
		for (int i = valence; i > 0; --i)
		{
			if (ot == null || ot.hasAttributes(AbstractHalfEdge.OUTER))
				return null;
			double dist = mo.distance2(xyz, ot.destination().getUV());
			if (dist < dMin)
			{
				dMin = dist;
				iMin = i;
			}
			ot = ot.nextOrigin();
		}
		if (d != ot.destination())
			return null;
		for (int i = valence; i > iMin; --i)
			ot = ot.nextOrigin();
		return ot;
	}

	private AbstractHalfEdge checkLargeValence(AbstractHalfEdge ot)
	{
		Vertex o = ot.origin();
		double[] xyz = o.getUV();
		Metric mo = mesh.getMetric(o);
		Vertex d = ot.destination();
		double dMax = Double.MIN_VALUE;
		int iMax = -1;
		int cnt = 0;
		do
		{
			if (ot == null || ot.hasAttributes(AbstractHalfEdge.OUTER))
				return null;
			double dist = mo.distance2(xyz, ot.destination().getUV());
			if (dist > dMax)
			{
				dMax = dist;
				iMax = cnt;
			}
			cnt++;
			ot = ot.nextOrigin();
		}
		while (d != ot.destination());
		for ( ; iMax > 0; --iMax)
			ot = ot.nextOrigin();
		return ot;
	}

	@Override
	public void postProcessAllVertices()
	{
		if (valence3 > 0)
			LOGGER.info("Number of removed vertices with a valence 3: "+valence3);
		if (valence4 > 0)
			LOGGER.info("Number of removed vertices with a valence 4: "+valence4);
		if (inserted > 0)
			LOGGER.info("Number of inserted vertices: "+inserted);
		LOGGER.info("Number of vertices still present in the binary tree: "+tree.size());
	}

}
