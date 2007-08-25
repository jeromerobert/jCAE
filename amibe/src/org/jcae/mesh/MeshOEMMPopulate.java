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
import org.jcae.mesh.oemm.RawStorage;
import org.jcae.mesh.oemm.Storage;

/**
 * This class fills an OEMM structure with a new triangle soup.
 */
public class MeshOEMMPopulate
{
	public static void main(String args[])
	{
		if (args.length < 3)
		{
			System.out.println("Usage: MeshOEMMPopulate inOEMM outOEMM soup");
			System.exit(0);
		}
		String inOEMM   = args[0];
		String outOEMM  = args[1];
		String soupFile = args[2];
		// Read initial OEMM structure
		OEMM oemm = Storage.readOEMMStructure(inOEMM);
		// Count triangles for this new triangle soup
		int save_nrLeaves = oemm.getNumberOfLeaves();
		RawStorage.countTriangles(oemm, soupFile, false);
		if (oemm.getNumberOfLeaves() != save_nrLeaves)
			throw new RuntimeException("Octree structure does not fit triangle soup");
		// Convert triangle soup into an intermediate OEMM file
		RawStorage.dispatch(oemm, soupFile, "dispatched", "dispatched.data");
		// Convert intermediate OEMM file into final OEMM
		RawStorage.indexOEMM("dispatched", outOEMM);
	}

}
