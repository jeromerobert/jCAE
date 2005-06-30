/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005
                  Jerome Robert <jeromerobert@users.sourceforge.net>
 
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

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import gnu.trove.TObjectIntHashMap;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.util.Iterator;
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
	private static Element writeObjectNodes(Document document, ArrayList nodelist, File nodesFile, File refFile, String baseDir, TObjectIntHashMap nodeIndex)
		throws IOException
	{
		//save nodes
		logger.debug("begin writing "+nodesFile);
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(nodesFile)));
		DataOutputStream refout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(refFile)));
		Iterator nodesIterator = nodelist.iterator();
		//  Write interior nodes first
		int i = 0;
		while (nodesIterator.hasNext())
		{
			Vertex n = (Vertex) nodesIterator.next();
			if (n == Vertex.outer)
				continue;
			int ref1d = n.getRef();
			if (-1 == ref1d)
			{
				double [] p = n.getUV();
				for (int d = 0; d < p.length; d++)
					out.writeDouble(p[d]);
				nodeIndex.put(n, i);
				i++;
			}
		}
		
		//  Write boundary nodes and 1D references
		nodesIterator = nodelist.iterator();
		int nref = 0;
		while (nodesIterator.hasNext())
		{
			Vertex n = (Vertex) nodesIterator.next();
			if (n == Vertex.outer)
				continue;
			int ref1d = n.getRef();
			if (-1 != ref1d)
			{
				double [] p = n.getUV();
				for (int d = 0; d < p.length; d++)
					out.writeDouble(p[d]);
				refout.writeInt(ref1d);
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
	private static Element writeObjectTriangles(Document document, ArrayList trianglelist, File trianglesFile, String baseDir, TObjectIntHashMap nodeIndex)
		throws IOException
	{
		//save triangles
		Iterator facesIterator = trianglelist.iterator();
		logger.debug("begin writing "+trianglesFile);
		DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(trianglesFile)));
		TObjectIntHashMap faceIndex=new TObjectIntHashMap(trianglelist.size());
		int i=0;
		while(facesIterator.hasNext())
		{
			Triangle f = (Triangle) facesIterator.next();
			if (f.isOuter())
				continue;
			for (int j = 0; j < 3; j++)
				out.writeInt(nodeIndex.get(f.vertex[j]));
			faceIndex.put(f, i);
			i++;
		}
		out.close();
		logger.debug("end writing "+trianglesFile);
		
		return XMLHelper.parseXMLString(document, "<triangles>"+
			"<number>"+i+"</number>"+
			"<file format=\"integerstream\" location=\""+XMLHelper.canonicalize(baseDir, trianglesFile.toString())+"\"/>"+
			"</triangles>");
	}

	/**
	 * Write the current object to a XML file and binary files. The XML file
	 * have links to the binary files.
	 * @param xmlDir       name of the XML file
	 * @param xmlFile      basename of the main XML file
	 * @param brepDir      path to brep file, relative to xmlDir
	 * @param brepFile     basename of the brep file
	 */
	public static void writeObject(Mesh submesh, String xmlDir, String xmlFile, String brepDir, String brepFile, int index)
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
			ArrayList trianglelist = submesh.getTriangles();
			ArrayList nodelist;
			if (submesh.quadtree != null)
				nodelist = submesh.quadtree.getAllVertices(trianglelist.size() / 2);
			else
			{
				HashSet nodeset = new HashSet();
				nodelist = new ArrayList();
				for (Iterator itf = trianglelist.iterator(); itf.hasNext(); )
				{
					Triangle t = (Triangle) itf.next();
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
			TObjectIntHashMap nodeIndex=new TObjectIntHashMap(nodelist.size());
			
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
			
			subMeshElement.appendChild(writeObjectNodes(document, nodelist, nodesFile, refFile, xmlDir, nodeIndex));
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
	
	private static Element writeObjectGroups(Document document, ArrayList trianglelist, File groupsFile, String baseDir)
		throws IOException
	{
		logger.debug("begin writing "+groupsFile);
		DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(groupsFile)));
		int i=0;
		Iterator facesIterator = trianglelist.iterator();
		while(facesIterator.hasNext())
		{
			Triangle f = (Triangle) facesIterator.next();
			if (f.isOuter())
				continue;
			out.writeInt(i);
			i++;
		}
		out.close();
		logger.debug("end writing "+groupsFile);
		
		int groupId = 1;
		return XMLHelper.parseXMLString(document,
				"<groups>"+
				"<group id=\""+(groupId-1)+"\">"+
				"<name>"+groupId+"</name>"+
				"<number>"+i+"</number>"+ 
				"<file format=\"integerstream\" location=\""+
				XMLHelper.canonicalize(baseDir, groupsFile.toString())+"\""+
				" offset=\"0\"/></group></groups>");
	}
	
	public static void writeObject3D(Mesh submesh, String xmlDir, String xmlFile, String brepDir, String brepFile, int index)
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
			File groupsFile = new File(dir, JCAEXMLData.groupsFilename);
			ArrayList trianglelist = submesh.getTriangles();
			ArrayList nodelist;
			if (submesh.quadtree != null)
				nodelist = submesh.quadtree.getAllVertices(trianglelist.size() / 2);
			else
			{
				HashSet nodeset = new HashSet();
				nodelist = new ArrayList();
				for (Iterator itf = trianglelist.iterator(); itf.hasNext(); )
				{
					Triangle t = (Triangle) itf.next();
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
			TObjectIntHashMap nodeIndex=new TObjectIntHashMap(nodelist.size());
			
			// Create and fill the DOM
			Document document=JCAEXMLWriter.createJcaeDocument();
			
			Element groupsElement = document.createElement("groups");
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
			
			subMeshElement.appendChild(writeObjectNodes(document, nodelist, nodesFile, refFile, xmlDir, nodeIndex));
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

