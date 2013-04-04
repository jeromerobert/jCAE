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

import java.util.List;
import java.util.Map;
import org.jcae.mesh.amibe.algos2d.Initial;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.MeshParameters;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.patch.Mesh2D;
import org.jcae.mesh.amibe.patch.Vertex2D;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.amibe.traits.TriangleTraitsBuilder;
import org.jcae.mesh.amibe.util.HashFactory;

/**
 * Wrap org.jcae.mesh.amibe.algos2d to make it work on 3D meshes
 * @author Jerome Robert
 */
public class Delaunay2D {
	private final Dir direction;
	private final int group;
	public static enum Dir {X, Y, Z};
	private final Mesh mesh;
	/**
	 *
	 * @param mesh a mesh containging only one closed beam contour
	 * @param direction The direction of the plane
	 */
	public Delaunay2D(Mesh mesh, Dir direction, int group)
	{
		this.mesh = mesh;
		this.direction = direction;
		this.group = group;
	}

	public void compute()
	{
		PolylineFactory polylineFactory = new PolylineFactory(mesh, -1, 0, true);
		List<Vertex> orderedVertices = polylineFactory.get(-1).iterator().next();
		Vertex2D[] border = new Vertex2D[orderedVertices.size()+1];
		TriangleTraitsBuilder ttb = new TriangleTraitsBuilder();
		ttb.addVirtualHalfEdge();
		MeshTraitsBuilder mtb = new MeshTraitsBuilder();
		mtb.addKdTree(2);
		mtb.add(ttb);
		Mesh2D m = new Mesh2D(mtb, new MeshParameters(), null);
		int k = 0;
		Map<Vertex, Vertex> v2dTov3d = HashFactory.createMap();
		for(Vertex v: orderedVertices)
		{
			border[k] = (Vertex2D) m.createVertex(
				direction == Dir.X ? v.getY() : v.getX(),
				direction == Dir.Z ? v.getX() : v.getZ());
			v2dTov3d.put(border[k], v);
			k++;
		}
		border[k] = border[0];
		new Initial(m, mtb, border, null).compute();
		Vertex[] tmp = new Vertex[3];
		for(Triangle t:m.getTriangles())
		{
			k = 0;
			if(t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			for(Vertex vertex:t.vertex)
				tmp[k++] = v2dTov3d.get(vertex);
			Triangle t3d = mesh.createTriangle(tmp);
			t3d.setGroupId(group);
			mesh.add(t3d);
		}
	}
}
