/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2007,2008, by EADS France

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
import org.jcae.mesh.oemm.MeshReader;
import org.jcae.mesh.oemm.TraversalProcedure;
import org.jcae.mesh.oemm.OEMM.Node;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.algos3d.AbstractAlgoHalfEdge;
import org.jcae.mesh.amibe.algos3d.QEMDecimateHalfEdge;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.amibe.traits.TriangleTraitsBuilder;

import java.util.HashMap;
import java.util.Map;
import gnu.trove.set.hash.TIntHashSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Decimates all cells of an OEMM.
 */
public class MeshOEMMDecimate
{
	private static final Logger logger=Logger.getLogger(MeshOEMMDecimate.class.getName());
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
		// Add a public constructor to avoid synthetic access
		public CountProcedure()
		{
		}
		@Override
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
		private final TIntHashSet leaves = new TIntHashSet();
		private final Map<String, String> options = new HashMap<String, String>();
		private final MeshTraitsBuilder mtb = new MeshTraitsBuilder();
		
		public DecimateProcedure(MeshReader mr, int s, int minimalNumberOfTriangles)
		{
			reader = mr;
			scale = s;
			minTN = minimalNumberOfTriangles;
			TriangleTraitsBuilder ttb = new TriangleTraitsBuilder();
			ttb.addHalfEdge();
			mtb.add(ttb);
			mtb.addTriangleSet();
			mtb.addNodeSet();
		}

		@Override
		public final int action(OEMM oemm, OEMM.Node current, int octant, int visit)
		{
			if (current.tn <= minTN)
				return SKIPCHILD;
			if (visit == LEAF)
			{
				leaves.clear();
				leaves.add(current.leafIndex);
				process(oemm);
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
			getChildLeaves(current);
			process(oemm);
			return OK;
		}
		private void getChildLeaves(Node current)
		{
			for (OEMM.Node node: current.child)
			{
				if (node == null)
					continue;
				if (node.isLeaf) {
					leaves.add(node.leafIndex);
				} else {
					getChildLeaves(node);
				}
			}
			
		}
		private void process(OEMM oemm)
		{
			Mesh amesh = reader.buildMesh(mtb, leaves);
			int nrT = AbstractAlgoHalfEdge.countInnerTriangles(amesh);
			options.clear();
			options.put("maxtriangles", ""+(nrT / scale));
			new QEMDecimateHalfEdge(amesh, options).compute();
			Storage.saveNodes(oemm, amesh, leaves);
		}
	}
}
