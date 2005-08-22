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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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
			FileChannel fcR = new FileInputStream(refFile).getChannel();
			MappedByteBuffer bbR = fcR.map(FileChannel.MapMode.READ_ONLY, 0L, fcR.size());
			IntBuffer refsBuffer = bbR.asIntBuffer();
			String nodesFile = xpath.selectSingleNode(submeshNodes, "file/@location").getNodeValue();
			if (nodesFile.charAt(0) != File.separatorChar)
				nodesFile = xmlDir+File.separator+nodesFile;
			FileChannel fcN = new FileInputStream(nodesFile).getChannel();
			MappedByteBuffer bbN = fcN.map(FileChannel.MapMode.READ_ONLY, 0L, fcN.size());
			DoubleBuffer nodesBuffer = bbN.asDoubleBuffer();
			
			Node submeshTriangles = xpath.selectSingleNode(submeshElement, "triangles");
			String trianglesFile = xpath.selectSingleNode(submeshTriangles,
				"file/@location").getNodeValue();
			if (trianglesFile.charAt(0) != File.separatorChar)
				trianglesFile = xmlDir+File.separator+trianglesFile;
			FileChannel fcT = new FileInputStream(trianglesFile).getChannel();
			MappedByteBuffer bbT = fcT.map(FileChannel.MapMode.READ_ONLY, 0L, fcT.size());
			IntBuffer trianglesBuffer = bbT.asIntBuffer();

			Node submeshNormals = xpath.selectSingleNode(submeshTriangles, "normals");
			FileChannel fcNormals = null;
			MappedByteBuffer bbNormals = null;
			DoubleBuffer normalsBuffer = null;
			if (submeshNormals != null)
			{
				String normalsFile = xpath.selectSingleNode(submeshNormals, "file/@location").getNodeValue();
				if (normalsFile.charAt(0) != File.separatorChar)
					normalsFile = xmlDir+File.separator+normalsFile;
				try
				{
					fcNormals = new FileInputStream(normalsFile).getChannel();
					bbNormals = fcNormals.map(FileChannel.MapMode.READ_ONLY, 0L, fcNormals.size());
					normalsBuffer = bbNormals.asDoubleBuffer();
				}
				catch (FileNotFoundException ex)
				{
				}
			}
			int numberOfReferences = Integer.parseInt(
				xpath.selectSingleNode(submeshNodes, "references/number/text()").getNodeValue());
			logger.debug("Reading "+numberOfReferences+" references");
			int [] refs = new int[numberOfReferences];
			refsBuffer.get(refs);
			
			int numberOfNodes = Integer.parseInt(
				xpath.selectSingleNode(submeshNodes, "number/text()").getNodeValue());
			MNode3D [] nodelist = new MNode3D[numberOfNodes];
			int label;
			double [] coord = new double[3];
			logger.debug("Reading "+numberOfNodes+" nodes");
			for (i=0; i < numberOfNodes; i++)
			{
				if (i < numberOfNodes - numberOfReferences)
					label = 0;
				else
					label = refs[i+numberOfReferences-numberOfNodes];
				nodesBuffer.get(coord);
				nodelist[i] = new MNode3D(coord, label);
				m3d.addNode(nodelist[i]);
			}
			
			int numberOfTriangles = Integer.parseInt(
				xpath.selectSingleNode(submeshTriangles, "number/text()").getNodeValue());
			logger.debug("Reading "+numberOfTriangles+" elements");
			MFace3D [] facelist = new MFace3D[numberOfTriangles];
			double [] n = new double[3];
			for (i=0; i < numberOfTriangles; i++)
			{
				MNode3D pt1 = nodelist[trianglesBuffer.get()];
				if (fcNormals != null)
				{
					normalsBuffer.get(n);
					pt1.addNormal(n);
				}
				MNode3D pt2 = nodelist[trianglesBuffer.get()];
				if (fcNormals != null)
				{
					normalsBuffer.get(n);
					pt2.addNormal(n);
				}
				MNode3D pt3 = nodelist[trianglesBuffer.get()];
				if (fcNormals != null)
				{
					normalsBuffer.get(n);
					pt3.addNormal(n);
				}
				facelist[i] = new MFace3D(pt1, pt2, pt3);
				m3d.addFace(facelist[i]);
			}
			
			Node groupsElement = xpath.selectSingleNode(submeshElement, "groups");
			NodeList groupsList = xpath.selectNodeList(groupsElement, "group");
			int numberOfGroups = groupsList.getLength();
			String groupsFile = xpath.selectSingleNode(groupsList.item(0), "file/@location").getNodeValue();
			if (groupsFile.charAt(0) != File.separatorChar)
				groupsFile = xmlDir+File.separator+groupsFile;
			FileChannel fcG = new FileInputStream(groupsFile).getChannel();
			MappedByteBuffer bbG = fcG.map(FileChannel.MapMode.READ_ONLY, 0L, fcG.size());
			IntBuffer groupsBuffer = bbG.asIntBuffer();
			for (i=0; i < numberOfGroups; i++)
			{
				Node groupNode = groupsList.item(i);
				
				int numberOfElements = Integer.parseInt(
					xpath.selectSingleNode(groupNode, "number/text()").getNodeValue());
				int fileOffset = Integer.parseInt(
					xpath.selectSingleNode(groupNode, "file/@offset").getNodeValue());
				
				//int id=Integer.parseInt(
				//	xpath.selectSingleNode(groupNode, "@id").getNodeValue());
				
				String name=
					xpath.selectSingleNode(groupNode, "name/text()").getNodeValue();
				logger.debug("Group "+name+": reading "+numberOfElements+" elements");
								
				Collection newfacelist = new ArrayList(numberOfElements);
				for (int j=0; j < numberOfElements; j++)
					newfacelist.add(facelist[groupsBuffer.get(fileOffset+j)]);
				MGroup3D g = new MGroup3D(name, newfacelist);
				m3d.addGroup(g);
			}
			fcG.close();
			UNVConverter.clean(bbG);
			fcT.close();
			UNVConverter.clean(bbT);
			fcN.close();
			UNVConverter.clean(bbN);
			fcR.close();
			UNVConverter.clean(bbR);
			if (fcNormals != null)
			{
				fcNormals.close();
				UNVConverter.clean(bbNormals);
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		logger.debug("end reading "+xmlFile);
		return m3d;
	}
	
	public static int [] getInfos(String xmlDir, String xmlFile)
	{
		int [] ret = new int[3];
		CachedXPathAPI xpath = new CachedXPathAPI();
		try
		{
			Document document = XMLHelper.parseXML(new File(xmlDir, xmlFile));
			Node submeshElement = xpath.selectSingleNode(document, "/jcae/mesh/submesh");
			Node submeshNodes = xpath.selectSingleNode(submeshElement, "nodes");
			ret[0] = Integer.parseInt(
				xpath.selectSingleNode(submeshNodes, "number/text()").getNodeValue());
			Node submeshTriangles = xpath.selectSingleNode(submeshElement, "triangles");
			ret[1] = Integer.parseInt(
				xpath.selectSingleNode(submeshTriangles, "number/text()").getNodeValue());
			Node groupsElement = xpath.selectSingleNode(submeshElement, "groups");
			NodeList groupsList = xpath.selectNodeList(groupsElement, "group");
			ret[2] = groupsList.getLength();
		}
		catch(FileNotFoundException ex)
		{
			//  Do nothing if 3d was not processed
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		return ret;
	}
}

