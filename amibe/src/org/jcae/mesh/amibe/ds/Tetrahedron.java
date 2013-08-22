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

package org.jcae.mesh.amibe.ds;

/**
 * Hack to add a Tetrahedron in a mesh.
 * See usage in Netgen and Tetgen classes.
 * @author Jerome Robert
 */
public class Tetrahedron extends Triangle {

	private Vertex v3;
	public Tetrahedron(Vertex[] vs) {
		super(vs[0], vs[1], vs[2]);
	}

	@Override
	public Vertex getV(int i) {
		switch(i)
		{
		case 0: return v0;
		case 1: return v1;
		case 2: return v2;
		case 3: return v3;
		default:
			throw new IllegalArgumentException();
		}
	}

	@Override
	public int vertexNumber() {
		return 4;
	}
}
