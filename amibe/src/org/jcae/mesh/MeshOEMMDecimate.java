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
import org.jcae.mesh.oemm.Storage;
import org.jcae.mesh.oemm.MeshReader;
import org.jcae.mesh.oemm.TraversalProcedure;
import org.jcae.mesh.oemm.OEMM.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.log4j.Logger;

/**
 * Decimates all cells of an OEMM.
 */
public class MeshOEMMDecimate
{
	private static Logger logger=Logger.getLogger(MeshOEMMDecimate.class);
	private static final int SIZE_OF_BUFFER = 20000024;
	private final byte[] buffer = new byte[SIZE_OF_BUFFER];
	
	public static void main(String[] args)
		throws IOException
	{
		if (args.length < 2)
		{
			System.out.println("Usage: MeshOEMMDecimate oemm scaleTriangles [minimal number for decimation [output directory]]");
			System.exit(0);
		}
		String dir = args[0];
		int scale = Integer.valueOf(args[1]).intValue();
		int minimalNumberOfTriangles = 0;
		if (args.length >= 3) {
			// If an octree node contains less than minimalNumberOfTriangles triangles,
			// it is not decimated.
			minimalNumberOfTriangles = Integer.parseInt(args[2]);
		}
		if (args.length >= 4)
		{
			logger.info("Copy OEMM directory from "+args[0]+" into "+args[3]);
			dir = args[3];
			File decDir = new File(dir);
			if (decDir.exists()) {
				deleteFiles(decDir);
			}
			new MeshOEMMDecimate().copyFiles(new File(args[0]), decDir);
		}
		
		logger.info("Read OEMM structure");
		OEMM oemm = Storage.readOEMMStructure(dir);
		// Count triangles in non-leaf nodes
		CountProcedure c_proc = new CountProcedure();
		oemm.walk(c_proc);
		logger.info("Decimate all octree nodes");
		MeshReader reader = new MeshReader(oemm);
		DecimateProcedure d_proc = new DecimateProcedure(reader, scale, minimalNumberOfTriangles);
		oemm.walk(d_proc);
	}
	
	private static void deleteFiles(File decDir)
	{
		if (decDir.isDirectory()) {
			for (File subFile: decDir.listFiles()) {
				deleteFiles(subFile);
			}
			decDir.delete();
		} else {
			decDir.delete();
		}
	}

	private void copyFiles(File src, File dest)
		throws IOException
	{
		if (src.isDirectory()) {
			dest.mkdirs();
			String[] list = src.list();

			for (String strFile: list)
				copyFiles(new File(src, strFile), new File(dest, strFile));
		} else {
			FileInputStream fin = new FileInputStream(src);
			FileOutputStream fout = new FileOutputStream(dest);
			int c;
			while ((c = fin.read(buffer)) >= 0)
				fout.write(buffer, 0, c);
			fin.close();
			fout.close();
		}
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
		private final MeshReader reader;
		private final int scale;
		private final int minTN;
		private final Set<Integer> leaves = new HashSet<Integer>();
		private final Map<String, String> options = new HashMap<String, String>();
		
		public DecimateProcedure(MeshReader mr, int s, int minimalNumberOfTriangles)
		{
			reader = mr;
			scale = s;
			minTN = minimalNumberOfTriangles;
		}

		public final int action(OEMM oemm, OEMM.Node current, int octant, int visit)
		{
			if (current.tn <= minTN)
				return SKIPCHILD;
			if (visit == LEAF)
			{
				leaves.clear();
				leaves.add(Integer.valueOf(current.leafIndex));
				process(oemm, leaves);
				return OK;
			}
			else if (visit != POSTORDER )
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
			
			leaves.clear();
			getChildLeaves(current, leaves);
			process(oemm, leaves);
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
		private void process(OEMM oemm, Set<Integer> leaves)
		{
			Mesh amesh = reader.buildMesh(leaves);
			options.clear();
			options.put("maxtriangles", ""+(amesh.getTriangles().size() / scale));
			new org.jcae.mesh.amibe.algos3d.DecimateHalfEdge(amesh, options).compute();
			Storage.saveNodes(oemm, amesh, leaves);
		}
	}
}
