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
 * (C) Copyright 2013, by EADS France
 */

package org.jcae.mesh.amibe.algos3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Matrix3D;

/**
 * Collapse all half edges between 2 vertices so they are linked between only
 * one half edge.
 * This algorithm only work if the vertices between the 2 input vertices are
 * almost aligned.
 * @author Jerome Robert
 */
public class EdgesCollapser {
	private final double[] vector1 = new double[3], vector2 = new double[3];
	private final Mesh mesh;
	private final VertexSwapper vertexSwapper;
	private transient boolean someCollapseDone;
	public EdgesCollapser(Mesh mesh)
	{
		this.mesh = mesh;
		this.vertexSwapper = new VertexSwapper(mesh);
	}

	private AbstractHalfEdge collapseImpl(Vertex v1, Vertex v2, boolean swap)
	{
		v2.sub(v1, vector1);
		AbstractHalfEdge toCollapse = nextEdge(v1, vector1, v1);
		while(toCollapse.destination() != v2)
		{
			if(toCollapse.hasAttributes(AbstractHalfEdge.OUTER))
				toCollapse = toCollapse.sym();
			assert !toCollapse.hasAttributes(AbstractHalfEdge.OUTER): toCollapse;
			assert !toCollapse.hasAttributes(AbstractHalfEdge.IMMUTABLE): toCollapse;
			Vertex target = v1;
			if(swap && !mesh.canCollapseEdge(toCollapse, v1))
			{
				vertexSwapper.swapOrigin(toCollapse.destination() == v1 ?
					toCollapse.origin() : toCollapse.destination());
			}
			if(!mesh.canCollapseEdge(toCollapse, v1))
				return null;
			collapsingEdge(toCollapse);
			someCollapseDone = true;
			mesh.edgeCollapse(toCollapse, target);
			toCollapse = nextEdge(v1, vector1, v1);
		}
		return toCollapse;
	}

	private AbstractHalfEdge collapseImpl(Vertex v1, Vertex v2)
	{
		someCollapseDone = false;
		AbstractHalfEdge r = collapseImpl(v1, v2, false);
		if(r == null)
		{
			// we failed to collapse from v2 to v1, let's try in the other side
			r = collapseImpl(v2, v1, false);
			if(r == null)
			{
				// we are still failing but the configuration may have change
				// so let's try forward again but with swap.
				r = collapseImpl(v1, v2, true);
				if(r == null)
				{
					r = collapseImpl(v2, v1, true);
					if(r != null)
						r = r.sym();
				}
			}
			else
				r = r.sym();
		}
		return r;
	}

	public AbstractHalfEdge collapse(Vertex v1, Vertex v2)
	{
		AbstractHalfEdge r = collapseImpl(v1, v2);
		while(r == null && someCollapseDone)
			r = collapseImpl(v1, v2);
		return r;
	}

	/**
	 * Called before collapsing an edged.
	 * To be overriden by subclassers.
	 */
	protected void collapsingEdge(AbstractHalfEdge edge)
	{
	}

	private AbstractHalfEdge nextEdge(Vertex v, double[] direction, Vertex notDirection)
	{
		Iterator<AbstractHalfEdge> it = v.getNeighbourIteratorAbstractHalfEdge();
		double bestDot = Double.NEGATIVE_INFINITY;
		AbstractHalfEdge bestEdge = null;
		while(it.hasNext())
		{
			AbstractHalfEdge e = it.next();
			if((e.hasAttributes(AbstractHalfEdge.NONMANIFOLD) || e.hasAttributes(
				AbstractHalfEdge.BOUNDARY)) && notDirection != e.destination())
			{
				e.destination().sub(e.origin(), vector2);
				double norm = Matrix3D.norm(vector2);
				for(int i = 0; i < 3; i++)
					vector2[i] /= norm;
				double dot = Matrix3D.prodSca(direction, vector2);
				if(dot > bestDot)
				{
					bestDot = dot;
					bestEdge = e;
				}
			}
		}
		return bestEdge;
	}
}
