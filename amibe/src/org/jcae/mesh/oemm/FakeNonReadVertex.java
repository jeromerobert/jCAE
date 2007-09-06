/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2007, by EADS France

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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

package org.jcae.mesh.oemm;

import org.jcae.mesh.amibe.ds.Vertex;

public class FakeNonReadVertex extends Vertex
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2250050153619169866L;
	/**
	 * Containing OEMM node 
	 */
	private OEMM.Node containingNode;

	public FakeNonReadVertex(OEMM oemm, int leaf, int localNumber)
	{
		super(0.0, 0.0, 0.0);
		containingNode = oemm.leaves[leaf];
		setLabel(containingNode.minIndex + localNumber);
		/*
		int[] positions = new int[]{containingNode.i0, containingNode.j0, containingNode.k0};
		oemm.int2double(positions, getUV());
		assert oemm.search(positions) == containingNode;
		*/
		setReadable(false);
		setWritable(false);
	}

	public int getLocalNumber() {
		return getLabel() - containingNode.minIndex;
	}

	@Override
	public boolean isWritable() {
		return false;
	}

	public int getOEMMIndex() {
		return containingNode.leafIndex;
	}

	@Override
	public String toString() {
		return super.toString()+" (fake vertex)";
	}

}
