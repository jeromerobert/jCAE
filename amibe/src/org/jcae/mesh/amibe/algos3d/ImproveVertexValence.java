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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.metrics.Location;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.amibe.util.QSortedTree.Node;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;

/**
 * Remove vertices with low valence, and duplicate vertices with high valence.
 */

public class ImproveVertexValence extends AbstractAlgoVertex
{
	private static final Logger LOGGER=Logger.getLogger(ImproveVertexValence.class.getName());
	private final LinkedHashSet<Vertex> immutableNodes = new LinkedHashSet<Vertex>();
	private int valence3;
	private int valence4;
	private int inserted;
	private int minValence = 1, maxValence = Integer.MAX_VALUE;
	private boolean checkNormals = true;
	/** valences for alternate pattern  */
	private int[] valences = new int[6];
	private double[] tNormal = new double[3];
	/** neighbourgs edges for alternate pattern */
	private List<AbstractHalfEdge> aPEdges = new ArrayList<AbstractHalfEdge>(6);
	private List<Vertex> aPVertices = new ArrayList<Vertex>(6);
	/**
	 * Level neighbours from a node.
	 * This is needed to update the tree. Level 1 is not enough because of
	 * alternate and 55 patterns.
	 */
	private Collection<Vertex> level2Neighbours = new HashSet<Vertex>(18 * 4 / 3);

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
			else if("checkNormals".equals(key))
			{
				checkNormals = Boolean.parseBoolean(val);
			}
			else if("minValence".equals(key))
			{
				minValence = Integer.parseInt(val);
			}
			else if("maxValence".equals(key))
			{
				maxValence = Integer.parseInt(val);
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
 		AbstractHalfEdge ot = null;
		for (Triangle t: mesh.getTriangles())
		{
			if (t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			ot = t.getAbstractHalfEdge(ot);
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
	private int getValence(Vertex v)
	{
		if(v.isManifold())
		{
			AbstractHalfEdge start = v.getIncidentAbstractHalfEdge((Triangle)v.getLink(), null);
			AbstractHalfEdge edge = start;
			int r = 0;
			do
			{
				r++;
				edge = edge.nextOrigin();
				if(edge == null)
					//border or non-manifold vertex
					return -1;
			}
			while(edge != start);
			return r;
		}
		else
		{
			Iterator<AbstractHalfEdge> it = v.getNeighbourIteratorAbstractHalfEdge();
			int r = 0;
			while(it.hasNext())
			{
				AbstractHalfEdge e = it.next();
				if(!e.hasAttributes(AbstractHalfEdge.OUTER))
					r++;
			}
			return r;
		}
	}

	private int getAPValences(Vertex v, boolean withNeighbours)
	{
		assert getValence(v) == 6: getValence(v);
		AbstractHalfEdge start = v.getIncidentAbstractHalfEdge((Triangle)v.getLink(), null);
		AbstractHalfEdge edge = start;
		int k = 0;
		int min = Integer.MAX_VALUE;
		int imin = 0;
		if(withNeighbours)
		{
			aPEdges.clear();
			aPVertices.clear();
		}
		do
		{
			if(withNeighbours)
			{
				aPEdges.add(edge);
				aPVertices.add(edge.destination());
			}
			int valence = getValence(edge.destination());
			if(valence < 0)
				return -1;
			valences[k] = valence;
			if(valence < min)
			{
				min = valence;
				imin = k;
			}
			k++;
			edge = edge.nextOrigin();
		}
		while(edge != start);
		return imin;
	}

	/**
	 * Assuming that v have a valence of 6, check that v neighbours have
	 * &gt;6, &lt6, &gt;6, &lt6, &gt;6, &lt6 as connectivity
	 */
	private boolean isAlternatePattern(Vertex v)
	{
		if(!v.isManifold())
			return false;
		int imin = getAPValences(v, false);
		if(imin < 0)
			return false;
		int currentCost = 0;
		int swappedCost = 0;
		for(int i = 0; i < 6; i+=2)
		{
			int valence = valences[(imin + i) % 6];
			if(valence > 6)
				return false;
			currentCost += Math.abs(6 - valence);
			swappedCost += Math.abs(6 - (valence + 1));
			valence = valences[(imin + i + 1) % 6];
			currentCost += Math.abs(valence - 6);
			swappedCost += Math.abs(valence - 1 - 6);
		}
		return swappedCost < currentCost;
	}

	private boolean processAlternatePattern(Vertex v)
	{
		getLevel2Neighbors(v);
		int imin = getAPValences(v, true);
		AbstractHalfEdge he5 = aPEdges.get(imin);
		if(!mesh.canCollapseEdge(he5, he5.destination()))
			return false;
		for(int i = 0; i < 3; i++)
		{
			AbstractHalfEdge toSwap = aPEdges.get((imin + 2 * i + 1) % 6);
			mesh.edgeSwap(toSwap);
		}
		mesh.edgeCollapse(he5, he5.destination());
		tree.remove(v);
		for(Vertex mv:level2Neighbours)
		{
			if(canProcessVertex(mv))
			{
				if(tree.contains(mv))
					tree.update(mv, cost(mv));
				else
					tree.insert(mv, cost(mv));
			}
			else
				tree.remove(mv);
		}
		return true;
	}

	private AbstractHalfEdge get55Pattern(Vertex v)
	{
		AbstractHalfEdge start = v.getIncidentAbstractHalfEdge((Triangle)v.getLink(), null);
		AbstractHalfEdge edge = start;
		for(int i = 0; i < 5; i++)
		{
			if(getValence(edge.destination()) == 5)
			{
				int v1 = getValence(edge.apex());
				int v2 = getValence(edge.sym().apex());
				if((v1 > 6 && v2 >= 6) || (v1 >= 6 && v2 > 6))
					return edge;
			}
			edge = edge.nextOrigin();
		}
		return null;
	}
	/**
	 * Assuming that v have a valence of 5 check that is has a neightbour with a
	 * valence of 5, and that the 2 apex of this edges have valences greater
	 * than 6
	 */
	private boolean is55Pattern(Vertex v)
	{
		return get55Pattern(v) != null;
	}

	private boolean process55Pattern(Vertex v)
	{
		AbstractHalfEdge edge = get55Pattern(v);
		assert edge.origin() == v;
		getLevel2Neighbors(edge.origin());
		if(mesh.canCollapseEdge(edge, edge.destination()))
			mesh.edgeCollapse(edge, edge.destination());
		else
			return false;
		tree.remove(v);
		for(Vertex mv:level2Neighbours)
		{
			if(canProcessVertex(mv))
			{
				if(tree.contains(mv))
					tree.update(mv, cost(mv));
				else
					tree.insert(mv, cost(mv));
			}
			else
				tree.remove(mv);
		}
		return true;
	}

	private void getLevel2Neighbors(Vertex v)
	{
		level2Neighbours.clear();
		AbstractHalfEdge start = v.getIncidentAbstractHalfEdge((Triangle)v.getLink(), null);
		AbstractHalfEdge edge = start;
		do
		{
			Vertex d = edge.destination();
			level2Neighbours.add(d);
			if(d.isManifold())
			{
				AbstractHalfEdge start2 = d.getIncidentAbstractHalfEdge((Triangle)d.getLink(), null);
				AbstractHalfEdge edge2 = start2;
				do
				{
					level2Neighbours.add(edge2.destination());
					edge2 = edge2.nextOrigin();
				}
				while(edge2 != start2 && edge2 != null);
			}
			edge = edge.nextOrigin();
		}
		while(edge != start);
		level2Neighbours.remove(v);
	}

	@Override
	protected final double cost(final Vertex v)
	{
		if (!v.isManifold() || !v.isMutable())
			return Double.MAX_VALUE;
		int q = getValence(v);
		if(q < minValence || q > maxValence)
			return Double.MAX_VALUE;
		switch(q)
		{
			case 1:
			case 2:
				return tolerance + 1;
			case 3:
				return 4;
			case 4:
				return 3;
			case 5:
				return is55Pattern(v) ? 6 : tolerance + 1;
			case 6:
				return isAlternatePattern(v) ? 5 : tolerance + 1;
			case 7:
				return tolerance + 1;
			default:
				return 7 + (tolerance - 7) / (q - 7);
		}
	}

	@Override
	public boolean canProcessVertex(Vertex v)
	{
		if (!v.isManifold() || !v.isMutable() || immutableNodes.contains(v))
			return false;
		if(checkNormals)
		{
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
		else
			return true;
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
		if(cost == 5)
			return processAlternatePattern(v);
		else if(cost == 6)
			return process55Pattern(v);
		else if (cost == 4 || cost == 3)
		{
			// Very low valence, try to remove vertex
			int iVal = (cost == 4 ? 3 : 4);
			// For valence 3, the edge to collapse does not matter
			ot = iVal == 3 ? ot : checkLowValence(ot, iVal);
			if (ot == null)
				return false;
			//no need to check collapse if the valence is 3 as collapse is
			//equivalent to remove 3 triangles and create a new one.
			if (iVal != 3 && !mesh.canCollapseEdge(ot, ot.destination()))
				return false;
			Vertex destination = ot.destination();
			Vertex origin = ot.origin();
			getLevel2Neighbors(origin);
			tree.remove(origin);
			ot = mesh.edgeCollapse(ot, destination);
			if (iVal == 3)
				valence3++;
			else
				valence4++;
			for(Vertex o: level2Neighbours)
			{
				if (canProcessVertex(o))
				{
					double val = cost(o);
					if (tree.contains(o))
						tree.update(o, val);
					else
						tree.insert(o, val);
				}
				else
					tree.remove(o);
			}
		}
		else if (cost >= 7.0)
		{
			getLevel2Neighbors(v);
			// Valence is 8 or more, try to insert a vertex
			ot = checkLargeValence(ot);
			if (ot == null)
				return false;
			Location newPt = new Location();
			newPt.middle(ot.destination(), v);
			Vertex newV = mesh.createVertex(newPt);
			liaison.addVertex(newV, liaison.getBackgroundTriangle(v, tNormal));
			liaison.move(newV, newV, false);
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
			mesh.edgeSwap(h);
			for(Vertex o: level2Neighbours)
			{
				if (canProcessVertex(o))
				{
					double val = cost(o);
					if (tree.contains(o))
						tree.update(o, val);
					else
						tree.insert(o, val);
				}
				else
					tree.remove(o);
			}
			inserted++;
		}
		return true;
	}

	private AbstractHalfEdge checkLowValence(AbstractHalfEdge ot, int valence)
	{
		Vertex o = ot.origin();
		Metric mo = mesh.getMetric(o);
		Vertex d = ot.destination();
		double dMin = Double.MAX_VALUE;
		int iMin = -1;
		for (int i = valence; i > 0; --i)
		{
			if (ot == null || ot.hasAttributes(AbstractHalfEdge.OUTER))
				return null;
			double dist = mo.distance2(o, ot.destination());
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
		Metric mo = mesh.getMetric(o);
		Vertex d = ot.destination();
		double dMax = Double.MIN_VALUE;
		int iMax = -1;
		int cnt = 0;
		do
		{
			if (ot == null || ot.hasAttributes(AbstractHalfEdge.OUTER))
				return null;
			double dist = mo.distance2(o, ot.destination());
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

	/** Debugging method to check that the tree has been properly updated */
	private void checkTree()
	{
		Iterator<Node<Vertex>> it = tree.iterator();
		while(it.hasNext())
		{
			Node<Vertex> n = it.next();
			if(n.getValue() != cost(n.getData()))
			{
				System.err.println(n.getData());
				System.err.println(n.getValue());
				System.err.println(getValence(n.getData()));
				System.err.println(cost(n.getData()));
				throw new IllegalStateException();
			}
		}
	}

	public static void main(final String[] args) {
		try {
			MeshTraitsBuilder mtb = MeshTraitsBuilder.getDefault3D();
			mtb.addNodeSet();
			Mesh mesh = new Mesh(mtb);
			MeshReader.readObject3D(mesh, "/home/robert/ast-a319-neo/demo-anabelle/demo/AST_mesh.amibe");
			MeshLiaison ml = MeshLiaison.create(mesh);
			ml.getMesh().buildGroupBoundaries();			
			HashMap opts = new HashMap();
			opts.put("checkNormals", "false");
			opts.put("maxValence", "7");
			new ImproveVertexValence(ml, opts).compute();
			MeshWriter.writeObject3D(ml.getMesh(), "/home/robert/ast-a319-neo/demo-anabelle/demo/AST_mesh3.amibe", null);
		} catch (Exception ex) {
			Logger.getLogger(VertexInsertion.class.getName()).log(Level.SEVERE,
				null, ex);
		}
	}
}
