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

import org.jcae.mesh.mesher.ds.*;
import java.io.*;
import gnu.trove.TIntIntHashMap;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import org.w3c.dom.*;
import org.apache.xpath.CachedXPathAPI;
import org.apache.log4j.Logger;


public class MMesh3DReader
{
	private static Logger logger=Logger.getLogger(MMesh3DReader.class);
	
	/**
	 * Write the current object to a XML file and binary files. The XML file
	 * have links to the binary files.
	 * @param xmlDir       name of the XML file
	 * @param xmlFile      basename of the main XML file
	 */
	public static MMesh3D readObject(String xmlDir, String xmlFile)
	{
		MMesh3D m3d = new MMesh3D();
		int i;
		logger.debug("begin reading "+xmlDir+File.separator+xmlFile);
		CachedXPathAPI xpath = new CachedXPathAPI();
		HashMap map1DToMaster = new HashMap();
		try
		{
			Document document = XMLHelper.parseXML(new File(xmlDir, xmlFile));
			Node submeshElement = xpath.selectSingleNode(document, "/jcae/mesh/submesh");
			Node submeshNodes = xpath.selectSingleNode(submeshElement, "nodes");
			String refFile = xpath.selectSingleNode(submeshNodes,
				"references/file/@location").getNodeValue();
			if (refFile.charAt(0) != File.separatorChar)
				refFile = xmlDir+File.separator+refFile;
			DataInputStream refsIn=new DataInputStream(new FileInputStream(refFile));
			String nodesFile = xpath.selectSingleNode(submeshNodes, "file/@location").getNodeValue();
			if (nodesFile.charAt(0) != File.separatorChar)
				nodesFile = xmlDir+File.separator+nodesFile;
			DataInputStream nodesIn=new DataInputStream(new FileInputStream(nodesFile));
			String trianglesFile = xpath.selectSingleNode(document,
				"/jcae/mesh/submesh/triangles/file/@location").getNodeValue();
			if (trianglesFile.charAt(0) != File.separatorChar)
				trianglesFile = xmlDir+File.separator+trianglesFile;
			DataInputStream trianglesIn=new DataInputStream(new FileInputStream(trianglesFile));

			int numberOfReferences = Integer.parseInt(
				xpath.selectSingleNode(submeshNodes, "references/number/text()").getNodeValue());
			int [] refs = new int[2*numberOfReferences];
			TIntIntHashMap xrefs = new TIntIntHashMap(numberOfReferences);
			for (i=0; i < numberOfReferences; i++)
			{
				int from = refsIn.readInt();
				int to   = refsIn.readInt();
				xrefs.put(from, to+1);
			}
			
			int numberOfNodes = Integer.parseInt(
				xpath.selectSingleNode(submeshNodes, "number/text()").getNodeValue());
			MNode3D [] nodelist = new MNode3D[numberOfNodes];
			int label;
			double [] coord = new double[3];
			for (i=0; i < numberOfNodes; i++)
			{
				label = xrefs.get(i) - 1;
				for (int j = 0; j < 3; j++)
					coord[j] = nodesIn.readDouble();
				nodelist[i] = new MNode3D(coord, label);
				m3d.addNode(nodelist[i]);
			}
			
			int numberOfTriangles = Integer.parseInt(
				xpath.selectSingleNode(submeshElement, "triangles/number/text()").getNodeValue());
			MFace3D [] facelist = new MFace3D[numberOfTriangles];
			for (i=0; i < numberOfTriangles; i++)
			{
				MNode3D pt1 = nodelist[trianglesIn.readInt()];
				MNode3D pt2 = nodelist[trianglesIn.readInt()];
				MNode3D pt3 = nodelist[trianglesIn.readInt()];
				facelist[i] = new MFace3D(pt1, pt2, pt3);
				m3d.addFace(facelist[i]);
			}
			
			Node groupsElement = xpath.selectSingleNode(submeshElement, "groups");
			NodeList groupsList = xpath.selectNodeList(groupsElement, "group");
			int numberOfGroups = groupsList.getLength();
			String groupsFile = xpath.selectSingleNode(groupsList.item(0), "file/@location").getNodeValue();
			if (groupsFile.charAt(0) != File.separatorChar)
				groupsFile = xmlDir+File.separator+groupsFile;
			DataInputStream groupsIn=new DataInputStream(new FileInputStream(groupsFile));
			for (i=0; i < numberOfGroups; i++)
			{
				Node groupNode = groupsList.item(i);
				
				int numberOfElements = Integer.parseInt(
					xpath.selectSingleNode(groupNode, "number/text()").getNodeValue());
				
				//int id=Integer.parseInt(
				//	xpath.selectSingleNode(groupNode, "@id").getNodeValue());
				
				String name=
					xpath.selectSingleNode(groupNode, "name/text()").getNodeValue();
								
				Collection newfacelist = new ArrayList(numberOfElements);
				for (int j=0; j < numberOfElements; j++)
				{
					newfacelist.add(facelist[groupsIn.readInt()]);
				}
				MGroup3D g = new MGroup3D(name, newfacelist);
				m3d.addGroup(g);
			}
			groupsIn.close();
			trianglesIn.close();
			nodesIn.close();
			refsIn.close();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		logger.debug("end reading "+xmlFile);
		 return m3d;
	}
}

