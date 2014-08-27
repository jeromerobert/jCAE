/*
 * Project Info:  http://jcae.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2014, by EADS France
 */

package org.jcae.mesh.amibe.algos3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.util.AStar;

/**
 * Collapse all half edges between 2 vertices so they are linked between only
 * one half edge.
 * Unlike EdgesCollapser this algorithm does not need the vertices to be aligned
 * because it internally use a A-star algorithm.
 * Moreover if the Ming Zou TriMultPoly tool is available it will never fail
 * to collapse.
 * @see TriMultPoly
 * @author Jerome Robert
 */
public class EdgesCollapserNG {
	private final Mesh mesh;
	private final TriMultPoly triMultPoly = new TriMultPoly();
	private final AStar<Vertex> astar = new AStar<Vertex>()
	{
		private final Collection<Vertex> neightbors = new ArrayList<Vertex>();
		@Override
		protected double heuristicDistance(Vertex n1, Vertex n2) {
			return n1.distance3D(n2);
		}
		@Override
		protected double distance(Vertex n1, Vertex n2) {
			return n1.distance3D(n2);
		}

		private void neighborNodes(Vertex n, Triangle t)
		{
			AbstractHalfEdge e = n.getIncidentAbstractHalfEdge(t, null);
			Vertex start = e.destination();
			do
			{
				if(canCollapse(e))
					neightbors.add(e.destination());
				e = e.nextOriginLoop();
			}
			while(e.destination() != start);
		}

		@Override
		protected Iterable<Vertex> neighborNodes(final Vertex n) {
			neightbors.clear();
			if(n.isManifold())
			{
				neighborNodes(n, (Triangle) n.getLink());
			}
			else
			{
				for(Triangle t: (Triangle[])n.getLink())
					neighborNodes(n, t);
			}
			return neightbors;
		}
	};

	protected boolean canCollapse(AbstractHalfEdge edge)
	{
		return (edge.hasAttributes(AbstractHalfEdge.BOUNDARY) ||
			edge.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
			&& !edge.hasAttributes(AbstractHalfEdge.OUTER);
	}

	public EdgesCollapserNG(Mesh mesh)
	{
		this.mesh = mesh;
	}

	private AbstractHalfEdge getEdge(Vertex v1, Triangle t, Vertex v2)
	{
		assert v1 != v2;
		AbstractHalfEdge e = v1.getIncidentAbstractHalfEdge(t, null);
		Vertex start = e.destination();
		do
		{
			if(e.destination() == v2)
				return e;
			e = e.nextOriginLoop();
		}
		while(e.destination() != start);
		return null;
	}

	private AbstractHalfEdge getEdge(Vertex v1, Vertex v2)
	{
		assert v1 != v2;
		if(v1.isManifold())
		{
			AbstractHalfEdge r = getEdge(v1, (Triangle) v1.getLink(), v2);
			assert r != null: v1+" "+v2;
			return r;
		}
		else
		{
			for(Triangle t: (Triangle[])v1.getLink())
			{
				AbstractHalfEdge e = getEdge(v1, t, v2);
				if(e != null)
					return e;
			}
		}
		assert false: v1+" "+v2;
		return null;
	}
	/**
	 * Called before collapsing an edged.
	 * To be overriden by subclassers.
	 */
	protected void collapsingEdge(AbstractHalfEdge edge)
	{
	}

	/**
	 * Try to create the edge v1 v2, by collapsing all edges between v1 and v2
	 * @param v1
	 * @param v2
	 * @param intermediate a vertex between v1 and v2. This vertex help to find
	 * the path between v1 and v2. It may be null.
	 * @return
	 */
	public AbstractHalfEdge collapse(Vertex v1, Vertex v2, Vertex intermediate)
	{
		assert v1 != intermediate: v1;
		assert v2 != intermediate: v2;
		assert v1 != v2: v1;
		List<Vertex> path;
		if(intermediate == null)
			path = astar.find(v1, v2);
		else
		{
			path = astar.find(v1, intermediate);
			List<Vertex> path2 = astar.find(intermediate, v2);
			assert !path2.contains(v1): v1+"\n"+intermediate+"\n"+v2;
			for(int i = 1; i < path2.size(); i++)
				path.add(path2.get(i));
		}

		boolean oneCollapseDone = true;
		main: while(path.size() > 2 && oneCollapseDone)
		{
			assert path.get(0) == v1;
			assert path.get(path.size() - 1) == v2: path;
			oneCollapseDone = false;
			AbstractHalfEdge e = getEdge(v1, path.get(1));
			if(mesh.canCollapseEdge(e, v1))
			{
				collapsingEdge(e);
				mesh.edgeCollapse(e, v1);
				path.remove(1);
				oneCollapseDone = true;
			}
			else
			{
				e = getEdge(v2, path.get(path.size() - 2));
				if(mesh.canCollapseEdge(e, v2))
				{
					collapsingEdge(e);
					mesh.edgeCollapse(e, v2);
					path.remove(path.size() - 2);
					oneCollapseDone = true;
				}
				else
				{
					for(int i = 0; i < path.size() - 3; i++)
					{
						Vertex vv1 = path.get(i + 1);
						Vertex vv2 = path.get(i + 2);
						e = getEdge(vv1, vv2);
						if(mesh.canCollapseEdge(e, vv1))
						{
							collapsingEdge(e);
							mesh.edgeCollapse(e, vv1);
							path.remove(i + 2);
							oneCollapseDone = true;
							continue main;
						}
						else if(mesh.canCollapseEdge(e, vv2))
						{
							collapsingEdge(e);
							mesh.edgeCollapse(e, vv2);
							path.remove(i + 1);
							oneCollapseDone = true;
							continue main;
						}
					}
				}
			}
		}
		if(path.size() == 2)
			return getEdge(v1, v2);
		else
		{
			if(triMultPoly.isAvailable())
			{
				runTriMultPoly(path);
				return getEdge(v1, v2);
			}
			else
				return null;
		}
	}

	private void runTriMultPoly(List<Vertex> path) {

	}
}
