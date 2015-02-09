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
 * (C) Copyright 2015, by Airbus Group SAS
 */

package org.jcae.mesh.amibe.algos3d;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;

/**
 *
 * @author Jerome Robert
 */
public abstract class Extruder {
	public void compute(Mesh m, int group, double dx, double dy, double dz)
	{
		Vertex[] endVertices = new Vertex[getVertexNumber()];
		for(int i = 0; i < getVertexNumber(); i++)
		{
			Vertex v = getVertex(i);
			endVertices[i] =
				m.createVertex(v.getX() + dx, v.getY() + dy, v.getZ() + dz);
			if(m.hasNodes())
				m.add(endVertices[i]);
		}

		for(int i = 0; i < getVertexNumber(); i++)
		{
			int j = (i + 1) % getVertexNumber();
			Triangle t1 = m.createTriangle(getVertex(i), getVertex(j), endVertices[j]);
			Triangle t2 = m.createTriangle(getVertex(i), endVertices[j], endVertices[i]);
			m.add(t1);
			m.add(t2);
			t1.setGroupId(group);
			t2.setGroupId(group);
		}
	}

	protected abstract Vertex getVertex(int i);
	protected abstract int getVertexNumber();
}
