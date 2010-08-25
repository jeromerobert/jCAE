/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2010, by EADS France
 
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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.xmldata;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;
import java.util.logging.Level;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SubMeshWorker
{
	private static final Logger logger=Logger.getLogger(SubMeshWorker.class.getName());
	private static final String submeshDirExt = "ext";
	private static final String submeshDirInt = "int";

	private final String amibeDir;

	public SubMeshWorker(String dir)
	{
		amibeDir = dir;
	}

	/**
	 * Extract groups from a mesh.  Two meshes are created on disk,
	 * one with the selected groups, and one with the complement.
	 *
	 * @param groupIds  array of selected groups
	 * @return the directory containing the extracted mesh
	 * @throws IOException
	 */
	public String extractGroups(int[] groupIds) throws IOException
	{
		logger.config("Extracting specified groups from directory: "+amibeDir);
		MeshTraitsBuilder mtbInt = new MeshTraitsBuilder().addTriangleSet();
		Mesh meshInt = new Mesh(mtbInt);
		MeshReader.readObject3D(meshInt, amibeDir);

		Mesh meshExt = splitSelectedGroups(meshInt, groupIds);
		MeshWriter.writeObject3DWithReferences(meshInt, amibeDir+File.separator+submeshDirInt, null);
		MeshWriter.writeObject3DWithReferences(meshExt, amibeDir+File.separator+submeshDirExt, null);
		return amibeDir+File.separator+submeshDirExt;
	}

	private Mesh splitSelectedGroups(Mesh meshInt, int[] groupIds)
	{
		TIntHashSet groups = new TIntHashSet(groupIds);
		Mesh meshExt = new Mesh();
		int approxNrNodes = meshInt.getTriangles().size() / 2;
		HashMap<Vertex, Vertex> mapVertexIntToExt = new HashMap<Vertex, Vertex>(approxNrNodes);
		Vertex[] vExt = new Vertex[3];
		List<Triangle> removedTriangles = new ArrayList<Triangle>();
		int maxRef = 0;
		for (Triangle t : meshInt.getTriangles())
		{
			for (int i = 0; i < 3; i++)
				maxRef = Math.max(maxRef, Math.abs(t.vertex[i].getRef()));

			if (!groups.contains(t.getGroupId()))
				continue;

			removedTriangles.add(t);
			for (int i = 0; i < 3; i++)
			{
				Vertex vn = mapVertexIntToExt.get(t.vertex[i]);
				if (null == vn)
				{
					Vertex v = t.vertex[i];
					vn = meshExt.createVertex(v.getUV());
					vn.copy(v);
					mapVertexIntToExt.put(v, vn);
				}
				vExt[i] = vn;
			}
			meshExt.add(meshExt.createTriangle(vExt[0], vExt[1], vExt[2]));
		}
		for (Triangle t : removedTriangles)
			meshInt.remove(t);

		logger.config("Extracted "+meshExt.getTriangles().size()+" triangles");

		// Ensure that boundary nodes of the selected groups have
		// a reference set.
		meshExt.buildAdjacency(maxRef);

		// Adjust references on interface
		for (Map.Entry<Vertex, Vertex> entry : mapVertexIntToExt.entrySet())
			entry.getKey().setRef(entry.getValue().getRef());

		return meshExt;
	}

	public void mergeMeshes(String newDir) throws IOException
	{
		MeshTraitsBuilder mtbInt = new MeshTraitsBuilder();
		mtbInt.addTriangleSet();
		mtbInt.addNodeSet();
		Mesh meshInt = new Mesh(mtbInt);
		MeshReader.readObject3D(meshInt, amibeDir+File.separator+submeshDirInt);
		TIntObjectHashMap<Vertex> ref2Vertex = new TIntObjectHashMap<Vertex>();
		for (Vertex v : meshInt.getNodes())
		{
			if (v.getRef() != 0)
				ref2Vertex.put(v.getRef(), v);
		}

		Mesh meshExt = new Mesh(mtbInt);
		MeshReader.readObject3D(meshExt, amibeDir+File.separator+submeshDirExt);

		Vertex[] vInt = new Vertex[3];
		for (Triangle t : meshExt.getTriangles())
		{
			for (int i = 0; i < 3; i++)
			{
				int ref1d = t.vertex[i].getRef();
				if (0 == ref1d)
				{
					// Inner vertex
					vInt[i] = t.vertex[i];
				}
				else if (ref2Vertex.contains(ref1d))
				{
					// Already processed boundary point
					vInt[i] = ref2Vertex.get(ref1d);
				}
				else
				{
					// New boundary point
					vInt[i] = meshInt.createVertex(t.vertex[i].getUV());
					vInt[i].copy(t.vertex[i]);
					ref2Vertex.put(ref1d, vInt[i]);
				}
				meshInt.add(vInt[i]);
			}
			meshInt.add(meshInt.createTriangle(vInt[0], vInt[1], vInt[2]));
		}
		MeshWriter.writeObject3D(meshInt, newDir, null);
	}

	/*
	 * Usage: dirIn dirOut groupNumber targetSize
	 */
	public static void main(String [] args)
	{
		SubMeshWorker smw = new SubMeshWorker(args[0]);
		int[] groups = new int[1];
		groups[0] = Integer.parseInt(args[2]);
		try {
			String extractedDir = smw.extractGroups(groups);
			Mesh workMesh = new Mesh();
			MeshReader.readObject3D(workMesh, extractedDir);
			workMesh.tagFreeEdges(org.jcae.mesh.amibe.ds.AbstractHalfEdge.IMMUTABLE);

			org.jcae.mesh.amibe.projection.MeshLiaison liaison = new org.jcae.mesh.amibe.projection.MeshLiaison(workMesh);
			liaison.getMesh().buildRidges(0.9);

			HashMap<String, String> opts = new HashMap<String, String>();
			opts.put("size", args[3]);
			org.jcae.mesh.amibe.algos3d.Remesh algo = new org.jcae.mesh.amibe.algos3d.Remesh(liaison, opts);
			algo.compute();

			HashMap<String, String> swapOpts = new HashMap<String, String>();
			org.jcae.mesh.amibe.algos3d.SwapEdge swap = new org.jcae.mesh.amibe.algos3d.SwapEdge(liaison, swapOpts);
			swap.compute();
			MeshWriter.writeObject3D(liaison.getMesh(), extractedDir, null);
			smw.mergeMeshes(args[1]);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
	}
}

