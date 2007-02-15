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

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.oemm.OEMM;
import org.jcae.mesh.oemm.OEMMNode;
import org.jcae.mesh.oemm.Storage;
import org.jcae.mesh.oemm.TraversalProcedure;
import java.util.HashMap;
import gnu.trove.TIntHashSet;

/**
 * Decimates all cells of an OEMM.
 */
public class MeshOEMMDecimate
{

	public static void main(String args[])
	{
		if (args.length < 2)
		{
			System.out.println("Usage: MeshOEMMDecimate oemm scaleTriangles");
			System.exit(0);
		}
		String dir = args[0];
		int scale = Integer.valueOf(args[1]).intValue();
		
		OEMM oemm = Storage.readOEMMStructure(dir);
		DecimateProcedure d_proc = new DecimateProcedure(scale);
		oemm.walk(d_proc);
		// TODO: write mesh back into oemm
	}

	private static class DecimateProcedure extends TraversalProcedure
	{
		private int scale;
		public DecimateProcedure(int s)
		{
			scale = s;
		}
		public final int action(OEMM oemm, OEMMNode current, int octant, int visit)
		{
			if (visit != LEAF)
				return SKIPWALK;
			TIntHashSet leaves = new TIntHashSet();
			leaves.add(current.leafIndex);
			Mesh amesh = Storage.loadNodes(oemm, leaves);
			HashMap options = new HashMap();
			options.put("maxtriangles", ""+(current.tn / scale));
			System.out.println("Processing octant nr. "+current.leafIndex);
			new org.jcae.mesh.amibe.algos3d.DecimateHalfEdge(amesh, options).compute();
			return OK;
		}
	}

}
