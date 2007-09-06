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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.oemm.OEMM;
import org.jcae.mesh.oemm.Storage;
import org.jcae.mesh.oemm.MeshReader;
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
		String decimatedRepository = args[0];
		int scale = Integer.valueOf(args[1]).intValue();
		int minimalNumberOfTriangles = 0;
		if (args.length >= 3) {
			// If an octree node contains less than minimalNumberOfTriangles triangles,
			// it is not decimated.
			minimalNumberOfTriangles = Integer.parseInt(args[2]);
		}
		
		OEMM oemm = Storage.readOEMMStructure(decimatedRepository);
		// Count triangles in non-leaf nodes
		CountProcedure c_proc = new CountProcedure();
		oemm.walk(c_proc);
		MeshReader reader = new MeshReader(oemm);
		DecimateProcedure d_proc = new DecimateProcedure(reader, scale, minimalNumberOfTriangles);
		oemm.walk(d_proc);
	}
	
	private static class CountProcedure extends TraversalProcedure
	{
		public final int action(OEMM oemm, OEMM.Node current, int octant, int visit)
		{
			if (visit != POSTORDER)
				return OK;
			current.tn = 0;
			for (OEMM.Node node: current.child)
			{
				if (node != null)
					current.tn += node.tn;
			}
			return OK;
		}
	}
	private static class DecimateProcedure extends TraversalProcedure
	{
		private MeshReader reader;
		private int scale;
		private int minTN;
		
		public DecimateProcedure(MeshReader mr, int s, int minimalNumberOfTriangles)
		{
			reader = mr;
			scale = s;
			minTN = minimalNumberOfTriangles;
		}

		public final int action(OEMM oemm, OEMM.Node current, int octant, int visit) {
			if (current.tn <= minTN)
				return SKIPCHILD;
			if (visit != POSTORDER )
				return OK;
			// Children may have been decimated, update current.tn
			current.tn = 0;
			for (OEMM.Node node: current.child)
			{
				if (node != null)
					current.tn += node.tn;
			}
			if (current.tn <= minTN)
				return OK;
			
			Set<Integer> leaves = new HashSet<Integer>();
			getChildLeaves(current, leaves);
			Mesh amesh = reader.buildMesh(leaves);
			HashMap options = new HashMap();
			options.put("maxtriangles", "" + (amesh.getTriangles().size() / scale));
			new org.jcae.mesh.amibe.algos3d.DecimateHalfEdge(amesh, options).compute();
			Storage.saveNodes(oemm, amesh, leaves);
			return OK;
		}

		private void getChildLeaves(Node current, Set<Integer> leaves)
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
