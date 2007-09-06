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
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.amibe.traits.TriangleTraitsBuilder;
import org.jcae.mesh.oemm.OEMM;
import org.jcae.mesh.oemm.Storage;
import org.jcae.mesh.oemm.MeshReader;
import org.jcae.mesh.oemm.TraversalProcedure;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Decimates all cells of an OEMM.
 */
public class MeshOEMMDecimate
{
	
	private static final int SIZE_OF_BUFFER = 20000024;
	private byte[] buffer = new byte[SIZE_OF_BUFFER];
	
	public static void main(String args[])
		throws IOException
	{
		if (args.length < 2)
		{
			System.out.println("Usage: MeshOEMMDecimate oemm scaleTriangles [decimated_oemm]");
			System.exit(0);
		}
		String dir = args[0];
		int scale = Integer.valueOf(args[1]).intValue();
		if (args.length >= 3)
		{
			dir = args[2];
			File decDir = new File(dir);
			if (decDir.exists()) {
				deleteFiles(decDir);
			}
			new MeshOEMMDecimate().copyFiles(args[0], dir);
		}
		
		OEMM oemm = Storage.readOEMMStructure(dir);
		MeshReader mr = new MeshReader(oemm);
		DecimateProcedure d_proc = new DecimateProcedure(scale, mr);
		oemm.walk(d_proc);
		// TODO: write mesh back into oemm
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

	public void copyFiles(String strPath, String dstPath)
		throws IOException
	{
		File src = new File(strPath);
		File dest = new File(dstPath);

		if (src.isDirectory()) {
			dest.mkdirs();
			String list[] = src.list();

			for (int i = 0; i < list.length; i++) {
				String dest1 = dest.getAbsolutePath() + "\\" + list[i];
				String src1 = src.getAbsolutePath() + "\\" + list[i];
				copyFiles(src1, dest1);
			}
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

	private static class DecimateProcedure extends TraversalProcedure
	{
		private final int scale;
		private final MeshReader reader;
		private final Set<Integer> leaves = new HashSet<Integer>();
		private final MeshTraitsBuilder mtb;
		public DecimateProcedure(int s, MeshReader mr)
		{
			scale = s;
			reader = mr;
			mtb = new MeshTraitsBuilder();
			mtb.addTriangleSet();
			TriangleTraitsBuilder ttb = new TriangleTraitsBuilder();
			ttb.addHalfEdge();
			mtb.add(ttb);
		}
		public final int action(OEMM oemm, OEMM.Node current, int octant, int visit)
		{
			if (visit != LEAF)
				return OK;
			leaves.clear();
			leaves.add(Integer.valueOf(current.leafIndex));
			Mesh amesh = reader.buildMesh(mtb, leaves);
			HashMap<String, String> options = new HashMap<String, String>();
			options.put("maxtriangles", ""+(current.tn / scale));
			System.out.println("Processing octant nr. "+current.leafIndex);
			new org.jcae.mesh.amibe.algos3d.DecimateHalfEdge(amesh, options).compute();
			Storage.saveNodes(oemm, amesh, leaves);
			return OK;
		}
	}

}
