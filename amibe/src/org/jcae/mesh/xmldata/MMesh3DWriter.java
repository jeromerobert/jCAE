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

import org.jcae.mesh.amibe.ds.MGroup3D;
import org.jcae.mesh.amibe.ds.MMesh3D;
import org.jcae.mesh.amibe.ds.MFace3D;
import org.jcae.mesh.amibe.ds.MNode3D;
import gnu.trove.TObjectIntHashMap;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.util.Iterator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.apache.log4j.Logger;

public class MMesh3DWriter
{
	private static Logger logger=Logger.getLogger(MMesh3DWriter.class);

	/**
	 * Used by {@link writeObject}
	 */
	private static Element writeObjectNodes(Document document, MMesh3D mesh3d, File nodesFile, File refFile, String baseDir, TObjectIntHashMap nodeIndex)
		throws IOException
	{
		//save nodes
		logger.debug("begin writing "+nodesFile);
		DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(nodesFile)));
		DataOutputStream refout=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(refFile, true)));
		int i = 0;
		//  Write interior nodes first
		Iterator nodesIterator = mesh3d.getNodesIterator();
		while(nodesIterator.hasNext())
		{
			MNode3D n=(MNode3D)nodesIterator.next();
			int ref1d = n.getRef();
			if (0 == ref1d)
			{
				out.writeDouble(n.getX());
				out.writeDouble(n.getY());
				out.writeDouble(n.getZ());
				nodeIndex.put(n, i);
				i++;
			}
		}
		int nref = 0;
		nodesIterator = mesh3d.getNodesIterator();
		while(nodesIterator.hasNext())
		{
			MNode3D n=(MNode3D)nodesIterator.next();
			int ref1d = n.getRef();
			if (0 != ref1d)
			{
				out.writeDouble(n.getX());
				out.writeDouble(n.getY());
				out.writeDouble(n.getZ());
				refout.writeInt(ref1d);
				nodeIndex.put(n, i);
				nref++;
				i++;
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
	private static Element writeObjectTriangles(Document document,
		Iterator facesIterator, File trianglesFile, String baseDir,
		TObjectIntHashMap nodeIndex, TObjectIntHashMap faceIndex)
		throws IOException
	{
		//save triangles
		logger.debug("begin writing "+trianglesFile);
		DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(trianglesFile)));
		int i=0;
		while(facesIterator.hasNext())
		{
			MFace3D f=(MFace3D)facesIterator.next();			
			faceIndex.put(f, i);
			i++;
			Iterator it2=f.getNodesIterator();
			while(it2.hasNext())
				out.writeInt(nodeIndex.get(it2.next()));
		}
		out.close();
		logger.debug("end writing "+trianglesFile);
		
		return XMLHelper.parseXMLString(document, "<triangles>"+
			"<number>"+i+"</number>"+
			"<file format=\"integerstream\" location=\""+XMLHelper.canonicalize(baseDir, trianglesFile.toString())+"\"/>"+
			"</triangles>");
	}

	/**
	 * Used by {@link writeObject}
	 * @param document
	 * @param groupsIterator
	 * @param groupsFile
	 * @param xmlDir
	 * @param faceIndex
	 * @return
	 * @throws IOException
	 */
	private static Node writeObjectGroups(Document document,
		Iterator groupsIterator, File groupsFile, String xmlDir, TObjectIntHashMap faceIndex)
		throws IOException
	{
		// write binary datas
		Element groups=document.createElement("groups");
		DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(groupsFile)));
		int i=0;
		while(groupsIterator.hasNext())
		{
			MGroup3D g=(MGroup3D)groupsIterator.next();									
			groups.appendChild(
				XMLHelper.parseXMLString(document, "<group id=\""+i+"\">"+
					"<name>"+g.getName()+"</name>"+
					"<number>"+g.numberOfFaces()+"</number>"+					
					"<file format=\"integerstream\" location=\""+
					XMLHelper.canonicalize(xmlDir, groupsFile.toString())+"\""+
					" offset=\""+out.size()/4+"\"/></group>"));
			
			Iterator it2=g.getFacesIterator();
			while(it2.hasNext())
				out.writeInt(faceIndex.get(it2.next()));
			i++;
		}
		out.close();
		logger.debug("end writing "+groupsFile);				
		return groups;
	}
	
	/**
	 * Write the current object to a XML file and binary files. The XML file
	 * have links to the binary files.
	 * @param xmlFile The name of the XML file
	 */	
	public static void writeObject(MMesh3D mesh3d, String xmlDir, String xmlFile, String brepDir)
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
			File groupsFile=new File(dir, JCAEXMLData.groupsFilename);
			TObjectIntHashMap nodeIndex=new TObjectIntHashMap(mesh3d.getNumberOfNodes());
			TObjectIntHashMap faceIndex=new TObjectIntHashMap(mesh3d.getNumberOfFaces());
			// Create and fill the DOM
			Document document=JCAEXMLWriter.createJcaeDocument();
			
			Element jcaeElement=document.getDocumentElement();
			Element meshElement=document.createElement("mesh");
			Element subMeshElement=document.createElement("submesh");
			subMeshElement.appendChild(writeObjectNodes(document, mesh3d, nodesFile, refFile, xmlDir, nodeIndex));
			subMeshElement.appendChild(writeObjectTriangles(document, mesh3d.getFacesIterator(), trianglesFile, xmlDir, nodeIndex, faceIndex));
			nodeIndex=null;
			subMeshElement.appendChild(writeObjectGroups(document, mesh3d.getGroupsIterator(), groupsFile, xmlDir, faceIndex));
			faceIndex=null;
			meshElement.appendChild(subMeshElement);
			jcaeElement.appendChild(meshElement);			
			XMLHelper.writeXML(document, file);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}

