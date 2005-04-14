/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.
 
	Copyright (C) 2003 Jerome Robert <jeromerobert@users.sourceforge.net>
 
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
				out.writeDouble(p[0]);
				out.writeDouble(p[1]);
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
				out.writeDouble(p[0]);
				out.writeDouble(p[1]);
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
	private static Element writeObjectTriangles(Document document, Iterator facesIterator, File trianglesFile, String baseDir, TObjectIntHashMap nodeIndex)
		throws IOException
	{
		//save triangles
		logger.debug("begin writing "+trianglesFile);
		DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(trianglesFile)));
		int i=0;
		while(facesIterator.hasNext())
		{
			Triangle f = (Triangle) facesIterator.next();
			if (f.vertex[0] == Vertex.outer || f.vertex[1] == Vertex.outer || f.vertex[2] == Vertex.outer)
				continue;
			if (f.isOuter())
				continue;
			i++;
			for (int j = 0; j < 3; j++)
				out.writeInt(nodeIndex.get(f.vertex[j]));
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
				nodelist = new ArrayList();
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
			subMeshElement.appendChild(writeObjectTriangles(document, trianglelist.iterator(), trianglesFile, xmlDir, nodeIndex));
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

