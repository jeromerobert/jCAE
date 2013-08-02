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

package org.jcae.mesh.stitch;

import java.util.Set;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.util.HashFactory;

/**
 *
 * @author Jerome Robert
 */
public class NonManifoldSplitter {
	private final Mesh mesh;
	public NonManifoldSplitter(Mesh mesh)
	{
		this.mesh = mesh;
	}

	public void compute()
	{
		Set<Vertex> nonManifold = HashFactory.createSet();
		for(Triangle t:mesh.getTriangles())
		{
			for(Vertex v:t.vertex)
				if(!v.isManifold())
					nonManifold.add(v);
		}
		nonManifold.remove(mesh.outerVertex);
		VertexMerger merger = new VertexMerger();
		for(Vertex v: nonManifold)
			merger.unmerge(mesh, v);
		assert mesh.isValid();
	}
}
