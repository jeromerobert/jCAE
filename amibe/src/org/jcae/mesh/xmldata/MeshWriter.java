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

import org.jcae.mesh.amibe.patch.Mesh2D;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import gnu.trove.TObjectIntHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Arrays;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import java.util.logging.Logger;


public class MeshWriter
{
	private static final Logger logger=Logger.getLogger(MeshWriter.class.getName());

	/**
	 * Used by {@link #writeObject(org.jcae.mesh.amibe.patch.Mesh2D, String, String, int)}
	 */
	private static Element writeObjectNodes(Document document, Collection<Vertex> nodelist, Vertex outer, File nodesFile, File refFile, String baseDir, TObjectIntHashMap<Vertex> nodeIndex)
		throws ParserConfigurationException, SAXException, IOException
	{
		//save nodes
		logger.fine("begin writing "+nodesFile);
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(nodesFile)));
		DataOutputStream refout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(refFile)));
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
				double [] p = v.getUV();
				for (int d = 0; d < p.length; d++)
					out.writeDouble(p[d]);
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
					double [] p = v.getUV();
					for (int d = 0; d < p.length; d++)
						out.writeDouble(p[d]);
					refout.writeInt(Math.abs(ref1d));
					nodeIndex.put(v, i);
					i++;
					nref++;
				}
				else
					duplicate3DNodes.add(v);
			}
			for (Vertex v: duplicate3DNodes)
			{
				double [] p = v.getUV();
				for (int d = 0; d < p.length; d++)
					out.writeDouble(p[d]);
				refout.writeInt(Math.abs(v.getRef()));
				nodeIndex.put(v, i);
				i++;
				nref++;
			}
		}
		// Eventually add outer vertex.  It is not written onto disk, but its
		// index may be used by outer triangles.
		nodeIndex.put(outer, i);
		out.close();
		refout.close();
		logger.fine("end writing "+nodesFile);

		// Create the <nodes> element
		return XMLHelper.parseXMLString(document, "<nodes>"+
			"<number>"+i+"</number>"+
			"<file format=\"doublestream\" location=\""+XMLHelper.canonicalize(baseDir, nodesFile.toString())+"\"/>"+
			"<references>"+
			"<number>"+nref+"</number>"+
			"<file format=\"integerstream\" location=\""+XMLHelper.canonicalize(baseDir, refFile.toString())+"\"/>"+
			"</references>"+
			"</nodes>");
	}
	
	/**
	 * Used by {@link #writeObject(org.jcae.mesh.amibe.patch.Mesh2D, String, String, int)}
	 */
	private static Element writeObjectTriangles(Document document, Collection<Triangle> trianglelist, File trianglesFile, String baseDir, TObjectIntHashMap<Vertex> nodeIndex)
		throws ParserConfigurationException, SAXException, IOException
	{
		//save triangles
		logger.fine("begin writing "+trianglesFile);
		DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(trianglesFile)));
		int nrTriangles=0;
		// First write inner triangles
		for(Triangle f: trianglelist)
		{
			if (!f.isWritable())
				continue;
			for (int j = 0; j < 3; j++)
				out.writeInt(nodeIndex.get(f.vertex[j]));
			nrTriangles++;
		}
		// Next write outer triangles
		for(Triangle f: trianglelist)
		{
			if (f.isWritable())
				continue;
			for (int j = 0; j < 3; j++)
				out.writeInt(-nodeIndex.get(f.vertex[j]));
			nrTriangles++;
		}
		out.close();
		logger.fine("end writing "+trianglesFile);
		
		return XMLHelper.parseXMLString(document, "<triangles>"+
			"<number>"+nrTriangles+"</number>"+
			"<file format=\"integerstream\" location=\""+XMLHelper.canonicalize(baseDir, trianglesFile.toString())+"\"/>"+
			"</triangles>");
	}

	private static Element writeObjectGroups(Document document, Collection<Triangle> trianglelist, File groupsFile, String baseDir)
		throws IOException
	{
		logger.fine("begin writing "+groupsFile);
		int cnt=0;
		TIntObjectHashMap<TIntArrayList> groupMap = new TIntObjectHashMap<TIntArrayList>();
		for(Triangle f: trianglelist)
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
		String filename = XMLHelper.canonicalize(baseDir, groupsFile.toString());
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(groupsFile)));
		Element groups = document.createElement("groups");

		// Sort group ids
		int [] sortedKeys = new int[groupMap.size()];
		System.arraycopy(groupMap.keys(), 0, sortedKeys, 0, sortedKeys.length);
		Arrays.sort(sortedKeys);

		int offset = 0;
		for (int id: sortedKeys)
		{
			int nrTriangles = 0;
			TIntArrayList list = groupMap.get(id);
			try
			{
				for(int i = 0, n = list.size(); i < n; i++)
				{
					out.writeInt(list.get(i));
					nrTriangles++;
				}
				groups.appendChild(
					XMLHelper.parseXMLString(document, "<group id=\""+id+"\">"+
						"<name>"+(id+1)+"</name>"+
						"<number>"+nrTriangles+"</number>"+					
						"<file format=\"integerstream\" location=\""+filename+"\""+
						" offset=\""+offset+"\"/></group>"));
			}
			catch(IOException ex)
			{
				ex.printStackTrace();
			}
			catch(ParserConfigurationException ex)
			{
				ex.printStackTrace();
			}
			catch(SAXException ex)
			{
				ex.printStackTrace();
			}
			offset += nrTriangles;
		}
		out.close();
		logger.fine("end writing "+groupsFile);
		
		return groups;
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
		File file = new File(xmlDir, JCAEXMLData.xml2dFilename+index);
		File dir = new File(xmlDir, JCAEXMLData.xml2dFilename+index+".files");
		
		//create the directory if it does not exist
		if(!dir.exists())
			dir.mkdirs();

		File nodesFile=new File(dir, JCAEXMLData.nodes2dFilename);
		File refFile = new File(dir, JCAEXMLData.ref1dFilename);
		File trianglesFile=new File(dir, JCAEXMLData.triangles2dFilename);
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
			
		try
		{
			// Create and fill the DOM
			Document document=JCAEXMLWriter.createJcaeDocument();
		
			Element jcaeElement=document.getDocumentElement();
			Element meshElement=document.createElement("mesh");
			Element shapeElement=XMLHelper.parseXMLString(document, "<shape>"+
				"<file format=\"brep\" location=\""+brepFile+"\"/>"+"</shape>");
			meshElement.appendChild(shapeElement);
			Element subMeshElement=document.createElement("submesh");
			
			// Create <subshape> element
			Element subshapeElement=document.createElement("subshape");
			subshapeElement.appendChild(document.createTextNode(""+index));
			subMeshElement.appendChild(subshapeElement);
			
			// Create <dimension> element
			Element dimensionElement=document.createElement("dimension");
			dimensionElement.appendChild(document.createTextNode("2"));
			subMeshElement.appendChild(dimensionElement);
			
			subMeshElement.appendChild(writeObjectNodes(document, nodelist, submesh.outerVertex, nodesFile, refFile, xmlDir, nodeIndex));
			subMeshElement.appendChild(writeObjectTriangles(document, trianglelist, trianglesFile, xmlDir, nodeIndex));
			meshElement.appendChild(subMeshElement);
			jcaeElement.appendChild(meshElement);

			// save the DOM to file
			XMLHelper.writeXML(document, file);
		}
		catch (ParserConfigurationException ex)
		{
			throw new RuntimeException(ex);
		}
		catch (SAXException ex)
		{
			throw new RuntimeException(ex);
		}
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
		try
		{
			File file = new File(xmlDir, JCAEXMLData.xml3dFilename);
			File dir = new File(xmlDir, JCAEXMLData.xml3dFilename+".files");
			
			//create the directory if it does not exist
			if(!dir.exists())
				dir.mkdirs();

			File nodesFile=new File(dir, JCAEXMLData.nodes3dFilename);
			File refFile = new File(dir, JCAEXMLData.ref1dFilename);
			File trianglesFile=new File(dir, JCAEXMLData.triangles3dFilename);
			File groupsFile = new File(dir, JCAEXMLData.groupsFilename);
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
			
			// Create and fill the DOM
			Document document=JCAEXMLWriter.createJcaeDocument();
			
			Element jcaeElement=document.getDocumentElement();
			Element meshElement=document.createElement("mesh");
			if (brepFile != null)
			{
				Element shapeElement=XMLHelper.parseXMLString(document, "<shape>"+
					"<file format=\"brep\" location=\""+brepFile+"\"/>"+"</shape>");
				meshElement.appendChild(shapeElement);
			}
			Element subMeshElement=document.createElement("submesh");
			subMeshElement.appendChild(writeObjectNodes(document, nodelist, submesh.outerVertex, nodesFile, refFile, xmlDir, nodeIndex));
			subMeshElement.appendChild(writeObjectTriangles(document, trianglelist, trianglesFile, xmlDir, nodeIndex));
			subMeshElement.appendChild(writeObjectGroups(document, trianglelist, groupsFile, xmlDir));
			meshElement.appendChild(subMeshElement);
			jcaeElement.appendChild(meshElement);

			// save the DOM to file
			XMLHelper.writeXML(document, file);
		}
		catch (ParserConfigurationException ex)
		{
			throw new RuntimeException(ex);
		}
		catch (SAXException ex)
		{
			throw new RuntimeException(ex);
		}
	}
}

