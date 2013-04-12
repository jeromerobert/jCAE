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
 * (C) Copyright 2012, by EADS France
 */

package org.jcae.mesh.amibe.algos3d;

import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge.Quality;
import org.jcae.mesh.amibe.ds.HalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.projection.MeshLiaison;
import org.jcae.mesh.amibe.projection.TriangleKdTree;

/**
 * Swap edges around a give vertex to improve triangles quality
 * @author Jerome Robert
 */
public class VertexSwapper {
	private final Mesh mesh;
	private final MeshLiaison liaison;
	private final Quality quality = new Quality();
	private TriangleKdTree kdTree;
	private final Vertex projectedMiddle, middle;
	private double sqrDeflection;
	private int group;
	public VertexSwapper(MeshLiaison liaison) {
		this(liaison, null);
	}

	public VertexSwapper(Mesh mesh) {
		this(null, mesh);
	}

	private VertexSwapper(MeshLiaison liaison, Mesh mesh) {
		this.liaison = liaison;
		if(liaison == null)
			this.mesh = mesh;
		else
			this.mesh = liaison.getMesh();
		assert this.mesh != null;
		projectedMiddle = this.mesh.createVertex(0, 0, 0);
		middle = this.mesh.createVertex(0, 0, 0);
	}

	public void setKdTree(TriangleKdTree kdTree) {
		this.kdTree = kdTree;
	}

	public void setSqrDeflection(double sqrDeflection) {
		this.sqrDeflection = sqrDeflection;
	}

	public void setGroup(int group) {
		this.group = group;
	}

	public void swap(Vertex v)
	{
		HalfEdge current = (HalfEdge) v.getIncidentAbstractHalfEdge((Triangle)v.getLink(), null);
		current = current.next();
		Vertex o = current.origin();
		assert current.apex() == v;
		boolean redo = true;
		while(redo)
		{
			redo = false;
			while(true)
			{
				boolean isSwapped = false;
				if (!current.hasAttributes(AbstractHalfEdge.NONMANIFOLD |
					AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.OUTER)
					&& current.canSwapTopology())
				{
					quality.setEdge(current);
					if(quality.getSwappedAngle() > 0 &&
						quality.getSwappedQuality() > quality.getQuality())
					{
						if(liaison != null)
						{
							middle.middle(current.apex(), current.sym().apex());
							liaison.move(projectedMiddle, middle, group, true);
						}
						if(liaison == null || projectedMiddle.sqrDistance3D(middle) < sqrDeflection)
						{
							if(kdTree != null)
							{
								kdTree.remove(current.getTri());
								kdTree.remove(current.sym().getTri());
							}
							current = (HalfEdge) mesh.edgeSwap(current);
							HalfEdge swapped = current.next();
							if(kdTree != null)
							{
								kdTree.addTriangle(swapped.getTri());
								kdTree.addTriangle(swapped.sym().getTri());
							}
							redo = true;
							isSwapped = true;
						}
					}
				}

				if(!isSwapped)
				{
					current = current.nextApexLoop();
					if (current.origin() == o)
						break;
				}
			}
		}
	}
}
