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

import java.nio.DoubleBuffer;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import org.jcae.mesh.amibe.ds.TriangleHE;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.projection.MeshLiaison;
import org.jcae.mesh.amibe.projection.TriangleKdTree;

/**
 *
 * @author Jerome Robert
 */
public class VertexInsertion {
	private final MeshLiaison liaison;

	public VertexInsertion(MeshLiaison liaison) {
		this.liaison = liaison;
	}

	public Collection<Vertex> insertNodes(final DoubleBuffer vertices, int group,
		double insertionTol)
	{
		final int size = vertices.limit() / 3;
		AbstractList<Vertex> l = new AbstractList<Vertex>() {

			@Override
			public Vertex get(int index) {
				int n = index * 3;
				return new Vertex(null, vertices.get(n), vertices.get(n + 1),
					vertices.get(n + 2));
			}

			@Override
			public int size() {
				return size;
			}
		};
		return insertNodes(l, group, insertionTol);
	}

	/**
	 * Insert vertices from another mesh
	 * @param vertices the vertices to insert
	 * @param group insert vertices only in triangles of this group. Use -1
	 * to try to insert in all groups
	 * @param liaisonError the maximal acceptable distance between the
	 * inserted point and it's projection.
	 * @param insertionTol Do not the insert point if a point  already exist at
	 * a distance lower than this value
	 */
	public Collection<Vertex> insertNodes(Collection<Vertex> vertices, int group,
		double insertionTol)
	{
		double tol2 = insertionTol * insertionTol;
		TriangleKdTree kdTree = new TriangleKdTree(liaison.getMesh());
		double[] projection = new double[3];
		ArrayList<Vertex> toReturn = new ArrayList<Vertex>(vertices.size());
		main: for(Vertex v: vertices)
		{
			TriangleHE t = (TriangleHE) kdTree.getClosestTriangle(
				v.getUV(), projection, group);
			for(Vertex tv:t.vertex)
			{
				if(tv.sqrDistance3D(v) < tol2)
				{
					toReturn.add(tv);
					continue main;
				}
			}
			liaison.move(v, projection, true);
			t.split(liaison.getMesh(), v);
			toReturn.add(v);
		}
		return toReturn;
	}
}
