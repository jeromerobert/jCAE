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

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIterator;

import java.io.IOException;
import java.util.HashMap;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.oemm.OEMM;
import org.jcae.mesh.oemm.Storage;
import org.jcae.mesh.oemm.TraversalProcedure;
import org.jcae.mesh.oemm.OEMM.Node;

/**
 * Decimates all cells of an OEMM.
 */
public class MeshOEMMDecimateMoreLeaves
{
	public static void main(String args[])
		throws IOException
	{
		if (args.length < 2)
		{
			System.out.println("Usage: MeshOEMMDecimate oemm scaleTriangles [minimal number for decimation]");
			System.exit(0);
		}
		int scale = Integer.valueOf(args[1]).intValue();
		int minimalNumberOfTriangles = 0;
		String decimatedRepository = args[0];
		if (args.length >= 3) {
			minimalNumberOfTriangles = Integer.parseInt(args[2]);
		}
		
		OEMM oemm = Storage.readOEMMStructure(decimatedRepository);
		DecimateProcedure d_proc = new DecimateProcedure(scale, minimalNumberOfTriangles);
		oemm.walk(d_proc);
		// TODO: write mesh back into oemm
	}
	
	private static class DecimateProcedure extends TraversalProcedure
	{
		private int scale;
		private int minTN;
		
		public DecimateProcedure(int s, int minimalNumberOfTriangles)
		{
			scale = s;
			this.minTN = minimalNumberOfTriangles;
		}

		public final int action(OEMM oemm, OEMM.Node current, int octant,
				int visit) {
			if (visit != POSTORDER )
				return OK;
			TIntHashSet leaves = new TIntHashSet();
			getChildLeaves(current, leaves);
			
//			System.out.println("Processing octant nr. " + current.leafIndex);
//			new org.jcae.mesh.amibe.algos3d.DecimateHalfEdge(amesh, options)
//					.compute();

//			if (visit != LEAF) {
//				return OK;
//			}
//			if (decimatedNodes.contains(current.leafIndex))
//				return OK;
			
			TIntHashSet decSet = leaves;//getAppropriateNeigbr(oemm, current, 50000);
			if (minTN >=0 && getTriangles(oemm, leaves) < minTN) {
				return OK;
			}
			
//			try {
				
				
//			} catch (AssertionError e) {
//				//ingore this - there is some bug
//				return OK;
//			}
			Mesh amesh = Storage.loadNodes(oemm, decSet, true);
			HashMap options = new HashMap();
			try {
				
				options.put("maxtriangles", "" + (amesh.getTriangles().size() / scale));
				new org.jcae.mesh.amibe.algos3d.DecimateHalfEdge(amesh, options).compute();
				Storage.saveNodes(oemm, amesh, decSet);
			} catch (AssertionError e) {
				
			}
			return OK;
		}

		private int getTriangles(OEMM oemm, TIntHashSet leaves)
		{
			int result = 0;
			for (TIntIterator iter = leaves.iterator(); iter.hasNext(); )
			{
				result += oemm.leaves[iter.next()].tn;
			}
			return result;
		}

		private void getChildLeaves(Node current, TIntHashSet leaves)
		{
			for (OEMM.Node node: current.child)
			{
				if (node != null && node.isLeaf) {
					leaves.add(node.leafIndex);
				}	else if (node != null){
					getChildLeaves(node, leaves);
				}
			}
			
		}
	}
}
