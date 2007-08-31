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

package org.jcae.mesh;

import org.jcae.mesh.oemm.OEMM;
import org.jcae.mesh.oemm.Storage;
import org.jcae.mesh.oemm.OEMM.Node;

/**
 * Prints total number of triangles and vertices.
 */
public class MeshOEMMCountElements
{
	public static void main(String args[]) throws IOException
	{
		if (args.length < 1)
		{
			System.out.println("Usage: MeshOEMMCountElements oemm");
			System.exit(0);
		}
		String dir = args[0];
		
		OEMM oemm = Storage.readOEMMStructure(dir);
		int triangles = 0;
		int vertices = 0;
		for(Node leave: oemm.leaves) {
			triangles += leave.tn;
			vertices += leave.vn;
		}
		System.out.println("triangles: " + triangles + ", vertices:" + vertices);
	}
}
