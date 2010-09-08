/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005,2006, by EADS CRC
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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.xmldata;

import java.util.List;
import org.jcae.mesh.amibe.patch.Mesh2D;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import gnu.trove.TObjectIntHashMap;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import java.io.IOException;
import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.logging.Logger;


public class MeshWriter
{
	private static final Logger logger=Logger.getLogger(MeshWriter.class.getName());

	/**
	 * Used by {@link #writeObject(org.jcae.mesh.amibe.patch.Mesh2D, String, String, int)}
	 */
	private static void writeObjectNodes(Collection<Vertex> nodelist,
		Vertex outer, AmibeWriter out, TObjectIntHashMap<Vertex> nodeIndex)
		throws IOException
	{
		//  Write interior nodes first
		int nref = 0;
		int i = 0;
		for(Vertex v: nodelist)
		{
			if (v == outer)
				continue;
			int ref1d = v.getRef();
			if (0 == ref1d)
			{
				out.addNode(v.getUV());
				nodeIndex.put(v, i);
				i++;
			}
			else
				nref++;
		}
		//  Write boundary nodes and 1D references
		if (nref > 0)
		{
			//  Duplicate nodes, which are endpoints of 2D degenerated edges,
			//  are written at the end so that indices of regular vertices
			//  do not have to be modified during 2D->3D conversion.
			ArrayList<Vertex> duplicate3DNodes = new ArrayList<Vertex>();
			TIntHashSet refs = new TIntHashSet(nref);
			nref = 0;
			for(Vertex v: nodelist)
			{
				if (v == outer)
					continue;
				int ref1d = v.getRef();
				if (0 == ref1d)
					continue;
				if (!refs.contains(ref1d))
				{
					refs.add(ref1d);
					out.addNode(v.getUV());
					out.addNodeRef(Math.abs(ref1d));
					nodeIndex.put(v, i);
					i++;
					nref++;
				}
				else
					duplicate3DNodes.add(v);
			}
			for (Vertex v: duplicate3DNodes)
			{
				out.addNode(v.getUV());
				out.addNodeRef(Math.abs(v.getRef()));
				nodeIndex.put(v, i);
				i++;
				nref++;
			}
		}
		// Eventually add outer vertex.  It is not written onto disk, but its
		// index may be used by outer triangles.
		nodeIndex.put(outer, i);
	}
	
	/**
	 * Used by {@link #writeObject(org.jcae.mesh.amibe.patch.Mesh2D, String, String, int)}
	 */
	private static void writeObjectTriangles(Collection<Triangle> trianglelist,
		TObjectIntHashMap<Vertex> nodeIndex, AmibeWriter aw) throws IOException
	{
		int nrTriangles=0;
		// First write inner triangles
		for(Triangle f: trianglelist)
		{
			if (!f.isWritable())
				continue;
			aw.addTriangle(
				nodeIndex.get(f.vertex[0]),
				nodeIndex.get(f.vertex[1]),
				nodeIndex.get(f.vertex[2]));
			nrTriangles++;
		}
		// Next write outer triangles
		for(Triangle f: trianglelist)
		{
			if (f.isWritable())
				continue;
			aw.addTriangle(
				-nodeIndex.get(f.vertex[0]),
				-nodeIndex.get(f.vertex[1]),
				-nodeIndex.get(f.vertex[2]));
			nrTriangles++;
		}
	}

	private static void writeObjectGroups(Mesh mesh, AmibeWriter aw)
		throws IOException
	{
		int cnt=0;
		HashMap<Integer, TIntArrayList> groupMap = new HashMap<Integer, TIntArrayList>();
		HashMap<Integer, TIntArrayList> bgroupMap = new HashMap<Integer, TIntArrayList>();
		for(Triangle f: mesh.getTriangles())
		{
			if (!f.isWritable())
				continue;
			int id = f.getGroupId();
			TIntArrayList list = groupMap.get(id);
			if (list == null)
			{
				list = new TIntArrayList(100);
				groupMap.put(id, list);
			}
			list.add(cnt);
			cnt++;
		}

		List<Vertex> beams = mesh.getBeams();
		for(int i = 0 ; i < beams.size(); i+=2)
		{
			int id = mesh.getBeamGroup(i/2);
			TIntArrayList list = bgroupMap.get(id);
			if (list == null)
			{
				list = new TIntArrayList(100);
				bgroupMap.put(id, list);
			}
			list.add(i/2);
		}

		// Sort group ids
		TreeSet<Integer> sortedKeys = new TreeSet<Integer>();
		sortedKeys.addAll(groupMap.keySet());
		sortedKeys.addAll(bgroupMap.keySet());
		
		for (int id: sortedKeys)
		{
			String name = mesh.getGroupName(id);
			aw.nextGroup(name == null ? Integer.toString(id) : name);
			TIntArrayList list = groupMap.get(id);
			if(list != null)
				for(int i = 0, n = list.size(); i < n; i++)
					aw.addTriaToGroup(list.get(i));

			list = bgroupMap.get(id);
			if(list != null)
				for(int i = 0, n = list.size(); i < n; i++)
					aw.addBeamToGroup(list.get(i));
		}
	}
	
	/**
	 * Write the current object to an Amibe 2D XML file and binary files.
	 *
	 * @param submesh      mesh to be written on disk
	 * @param xmlDir       name of the XML file
	 * @param brepFile     basename of the brep file
	 * @param index        shape index
	 */
	public static void writeObject(Mesh2D submesh, String xmlDir, String brepFile, int index)
		throws IOException
	{
		AmibeWriter aw = new AmibeWriter.Dim2(xmlDir, index);
		Collection<Triangle> trianglelist = submesh.getTriangles();
		Collection<Vertex> nodelist = submesh.getNodes();
		if (nodelist == null)
		{
			nodelist = new LinkedHashSet<Vertex>(trianglelist.size() / 2);
			for (Triangle t: trianglelist)
			{
				if (!t.isWritable())
					continue;
				for (int j = 0; j < 3; j++)
				{
					if (!nodelist.contains(t.vertex[j]))
						nodelist.add(t.vertex[j]);
				}
			}
		}
		TObjectIntHashMap<Vertex> nodeIndex=new TObjectIntHashMap<Vertex>(nodelist.size());
		aw.setShape(brepFile);
		aw.setSubShape(index);
		writeObjectNodes(nodelist, submesh.outerVertex, aw, nodeIndex);
		writeObjectTriangles(trianglelist, nodeIndex, aw);
		aw.finish();
	}
	
	/**
	 * Write the current object to an Amibe 3D XML file and binary files.
	 *
	 * @param submesh      mesh to be written on disk
	 * @param xmlDir       name of the XML file
	 * @param brepFile     basename of the brep file
	 */
	public static void writeObject3D(Mesh submesh, String xmlDir, String brepFile)
		throws IOException
	{
		logger.info("Write mesh into "+xmlDir+java.io.File.separator+JCAEXMLData.xml3dFilename);
		Collection<Triangle> trianglelist = submesh.getTriangles();
		Collection<Vertex> nodelist = submesh.getNodes();
		if (nodelist == null)
		{
			nodelist = new LinkedHashSet<Vertex>(trianglelist.size() / 2);
			for (Triangle t: trianglelist)
			{
				if (!t.isWritable())
					continue;
				for (int j = 0; j < 3; j++)
				{
					if (!nodelist.contains(t.vertex[j]))
						nodelist.add(t.vertex[j]);
				}
			}
			nodelist.addAll(submesh.getBeams());
		}
		TObjectIntHashMap<Vertex> nodeIndex=new TObjectIntHashMap<Vertex>(nodelist.size());
		AmibeWriter.Dim3 aw = new AmibeWriter.Dim3(xmlDir, false,
			submesh.hasPersistentReferences());
		if (brepFile != null)
			aw.setShape(brepFile);

		writeObjectNodes(nodelist, submesh.outerVertex, aw, nodeIndex);
		writeObjectTriangles(trianglelist, nodeIndex, aw);
		writeObjectGroups(submesh, aw);
		List<Vertex> beams = submesh.getBeams();
		for(int i = 0; i<beams.size(); i+=2)
			aw.addBeam(
				nodeIndex.get(beams.get(i)),
				nodeIndex.get(beams.get(i+1)));
		aw.finish();
	}
}

