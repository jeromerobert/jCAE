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
 * (C) Copyright 2018, by Airbus S.A.S
 */


package org.jcae.mesh.amibe.algos3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Location;
import org.jcae.mesh.amibe.util.HashFactory;

/**
 * Crop or tag group a mesh with an implicit function
 * @author Jerome Robert
 */
public class ImplicitSelector {
	public interface Function {
		boolean inside(Location v);
	}
	public static class CenteredBox implements Function {
		private Location center;
		private double dx, dy, dz;

		public CenteredBox(Location center, double dx, double dy, double dz) {
			this.center = center;
			this.dx = dx;
			this.dy = dy;
			this.dz = dz;
		}

		public CenteredBox(Location center, double d) {
			this(center, d, d, d);
		}

		@Override
		public boolean inside(Location v) {
			return
				Math.abs(v.getX()-center.getX()) < dx &&
				Math.abs(v.getY()-center.getY()) < dy &&
				Math.abs(v.getZ()-center.getZ()) < dz;
		}
	}
	public static void crop(Mesh mesh, Function f) {
		Iterator<Triangle> it = mesh.getTriangles().iterator();
		while(it.hasNext()) {
			Triangle t = it.next();
			if(!f.inside(t.getV0()) || !f.inside(t.getV1()) || !f.inside(t.getV2())) {
				it.remove();
			}
		}
		//TODO also remove vertices
		//TODO implement crop on wire
	}

	public static void setNodeGroup(Mesh mesh, String name, Function f, int edgeAttribute) {
		Set<Vertex> toTag = HashFactory.createSet();
		for(Triangle t: mesh.getTriangles()) {
			AbstractHalfEdge e = t.getAbstractHalfEdge();
			for(int i = 0; i < 3; i++) {
				if(e.hasAttributes(edgeAttribute)) {
					toTag.add(e.origin());
					toTag.add(e.destination());
				}
			}
		}
		for(Vertex v: toTag) {
			mesh.setVertexGroup(v, name);
		}
	}
	private ImplicitSelector() {
	}
}
