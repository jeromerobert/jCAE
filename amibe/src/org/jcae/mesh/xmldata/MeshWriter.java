/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005,2006, by EADS CRC
 
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
import org.jcae.mesh.amibe.ds.AbstractTriangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.ds.AbstractVertex;
import gnu.trove.TObjectIntHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectProcedure;
import gnu.trove.TIntArrayList;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashSet;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.apache.log4j.Logger;


public class MeshWriter
{
	private static Logger logger=Logger.getLogger(MeshWriter.class);

	/**
	 * Used by {@link writeObject}
	 */
	private static Element writeObjectNodes(Document document, Collection<AbstractVertex> nodelist, AbstractVertex outer, File nodesFile, File refFile, String baseDir, TObjectIntHashMap<AbstractVertex> nodeIndex)
		throws IOException
	{
		//save nodes
		logger.debug("begin writing "+nodesFile);
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(nodesFile)));
		DataOutputStream refout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(refFile)));
		//  Write interior nodes first
		int i = 0;
		for(AbstractVertex av: nodelist)
		{
			if (av == outer)
				continue;
			Vertex n = (Vertex) av;
			int ref1d = n.getRef();
			if (0 == ref1d)
			{
				double [] p = n.getUV();
				for (int d = 0; d < p.length; d++)
					out.writeDouble(p[d]);
				nodeIndex.put(n, i);
				i++;
			}
		}
		
		//  Write boundary nodes and 1D references
		int nref = 0;
		for(AbstractVertex av: nodelist)
		{
			if (av == outer)
				continue;
			Vertex n = (Vertex) av;
			int ref1d = n.getRef();
			if (0 != ref1d)
			{
				double [] p = n.getUV();
				for (int d = 0; d < p.length; d++)
					out.writeDouble(p[d]);
				refout.writeInt(Math.abs(ref1d));
				nodeIndex.put(n, i);
				i++;
				nref++;
			}
		}
		out.close();
		refout.close();
		logger.debug("end writing "+nodesFile);

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
	 * Used by {@link writeObject}
	 */
	private static Element writeObjectTriangles(Document document, Collection<AbstractTriangle> trianglelist, File trianglesFile, String baseDir, TObjectIntHashMap<AbstractVertex> nodeIndex)
		throws IOException
	{
		//save triangles
		logger.debug("begin writing "+trianglesFile);
		DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(trianglesFile)));
		int nrTriangles=0;
		for(AbstractTriangle f: trianglelist)
		{
			if (!f.isWritable())
				continue;
			for (int j = 0; j < 3; j++)
				out.writeInt(nodeIndex.get(f.vertex[j]));
			nrTriangles++;
		}
		out.close();
		logger.debug("end writing "+trianglesFile);
		
		return XMLHelper.parseXMLString(document, "<triangles>"+
			"<number>"+nrTriangles+"</number>"+
			"<file format=\"integerstream\" location=\""+XMLHelper.canonicalize(baseDir, trianglesFile.toString())+"\"/>"+
			"</triangles>");
	}

	private static Element writeObjectGroups(Document document, Collection<AbstractTriangle> trianglelist, File groupsFile, String baseDir)
		throws IOException
	{
		logger.debug("begin writing "+groupsFile);
		int i=0;
		TIntObjectHashMap<TIntArrayList> groupMap = new TIntObjectHashMap<TIntArrayList>();
		for(AbstractTriangle f: trianglelist)
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
			list.add(i);
			i++;
		}
		// FIXME: sort group ids
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(groupsFile)));
		WriteGroupProcedure wgp = new WriteGroupProcedure(out, document, XMLHelper.canonicalize(baseDir, groupsFile.toString()));
		groupMap.forEachEntry(wgp);
		out.close();
		logger.debug("end writing "+groupsFile);
		
		return wgp.getGroupsElement();
	}
	
	private static final class WriteGroupProcedure implements TIntObjectProcedure<TIntArrayList> {
		private DataOutputStream out;
		private Document document;
		private Element groups;
		private String filename;
		private int offset = 0;
		public WriteGroupProcedure(DataOutputStream out, Document document, String filename)
		{
			this.out = out;
			this.document = document;
			this.filename = filename;
			groups = document.createElement("groups");
		}
		public final Element getGroupsElement()
		{
			return groups;
		}
		public final boolean execute(int key, TIntArrayList list) {
			int nrTriangles = 0;
			try
			{
				for(int i = 0; i < list.size(); i++)
				{
					out.writeInt(list.get(i));
					nrTriangles++;
				}
			}
			catch(IOException ex)
			{
				ex.printStackTrace();
			}
			groups.appendChild(
				XMLHelper.parseXMLString(document, "<group id=\""+key+"\">"+
					"<name>"+(key+1)+"</name>"+
					"<number>"+nrTriangles+"</number>"+					
					"<file format=\"integerstream\" location=\""+filename+"\""+
					" offset=\""+offset+"\"/></group>"));
			offset += nrTriangles;
			return true;
		}
	}
	
	/**
	 * Write the current object to an Amibe 2D XML file and binary files.
	 *
	 * @param submesh      mesh to be written on disk
	 * @param xmlDir       name of the XML file
	 * @param xmlFile      basename of the main XML file
	 * @param brepDir      path to brep file, relative to xmlDir
	 * @param brepFile     basename of the brep file
	 * @param index        shape index
	 */
	public static void writeObject(Mesh2D submesh, String xmlDir, String xmlFile, String brepDir, String brepFile, int index)
	{
		try
		{
			File file = new File(xmlDir, xmlFile);
			File dir = new File(xmlDir, xmlFile+".files");
			
			//create the directory if it does not exist
			if(!dir.exists())
				dir.mkdirs();

			File nodesFile=new File(dir, JCAEXMLData.nodes2dFilename);
			File refFile = new File(dir, JCAEXMLData.ref1dFilename);
			File trianglesFile=new File(dir, JCAEXMLData.triangles2dFilename);
			Collection<AbstractTriangle> trianglelist = submesh.getTriangles();
			Collection<AbstractVertex> nodelist = submesh.getNodes();
			if (nodelist == null)
			{
				HashSet<AbstractVertex> nodeset = new HashSet<AbstractVertex>();
				nodelist = new ArrayList<AbstractVertex>();
				for (AbstractTriangle t: trianglelist)
				{
					if (!t.isWritable())
						continue;
					for (int j = 0; j < 3; j++)
					{
						if (!nodeset.contains(t.vertex[j]))
						{
							nodeset.add(t.vertex[j]);
							nodelist.add(t.vertex[j]);
						}
					}
				}
			}
			TObjectIntHashMap<AbstractVertex> nodeIndex=new TObjectIntHashMap<AbstractVertex>(nodelist.size());
			
			// Create and fill the DOM
			Document document=JCAEXMLWriter.createJcaeDocument();
			
			Element jcaeElement=document.getDocumentElement();
			Element meshElement=document.createElement("mesh");
			Element shapeElement=XMLHelper.parseXMLString(document, "<shape>"+
				"<file format=\"brep\" location=\""+brepDir+File.separator+brepFile+"\"/>"+"</shape>");
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
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	/**
	 * Write the current object to an Amibe 3D XML file and binary files.
	 *
	 * @param submesh      mesh to be written on disk
	 * @param xmlDir       name of the XML file
	 * @param xmlFile      basename of the main XML file
	 * @param brepDir      path to brep file, relative to xmlDir
	 * @param brepFile     basename of the brep file
	 */
	public static void writeObject3D(Mesh submesh, String xmlDir, String xmlFile, String brepDir, String brepFile)
	{
		try
		{
			File file = new File(xmlDir, xmlFile);
			File dir = new File(xmlDir, xmlFile+".files");
			
			//create the directory if it does not exist
			if(!dir.exists())
				dir.mkdirs();

			File nodesFile=new File(dir, JCAEXMLData.nodes3dFilename);
			File refFile = new File(dir, JCAEXMLData.ref1dFilename);
			File trianglesFile=new File(dir, JCAEXMLData.triangles3dFilename);
			File groupsFile = new File(dir, JCAEXMLData.groupsFilename);
			Collection<AbstractTriangle> trianglelist = submesh.getTriangles();
			HashSet<AbstractVertex> nodeset = new HashSet<AbstractVertex>();
			Collection<AbstractVertex> nodelist = new ArrayList<AbstractVertex>();
			for (AbstractTriangle t: trianglelist)
			{
				if (!t.isWritable())
					continue;
				for (int j = 0; j < 3; j++)
				{
					if (!nodeset.contains(t.vertex[j]))
					{
						nodeset.add(t.vertex[j]);
						nodelist.add(t.vertex[j]);
					}
				}
			}
			nodeset.clear();
			TObjectIntHashMap<AbstractVertex> nodeIndex=new TObjectIntHashMap<AbstractVertex>(nodelist.size());
			
			// Create and fill the DOM
			Document document=JCAEXMLWriter.createJcaeDocument();
			
			Element jcaeElement=document.getDocumentElement();
			Element meshElement=document.createElement("mesh");
			Element shapeElement=XMLHelper.parseXMLString(document, "<shape>"+
				"<file format=\"brep\" location=\""+brepDir+File.separator+brepFile+"\"/>"+"</shape>");
			meshElement.appendChild(shapeElement);
			Element subMeshElement=document.createElement("submesh");
			subMeshElement.appendChild(writeObjectNodes(document, nodelist, submesh.outerVertex, nodesFile, refFile, xmlDir, nodeIndex));
			subMeshElement.appendChild(writeObjectTriangles(document, trianglelist, trianglesFile, xmlDir, nodeIndex));
			subMeshElement.appendChild(writeObjectGroups(document, trianglelist, groupsFile, xmlDir));
			meshElement.appendChild(subMeshElement);
			jcaeElement.appendChild(meshElement);

			// save the DOM to file
			XMLHelper.writeXML(document, file);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}

