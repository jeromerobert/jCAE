/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2005,2006, by EADS CRC
 
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
import org.jcae.mesh.amibe.ds.AbstractTriangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.ds.MGroup3D;
import org.jcae.mesh.amibe.patch.Mesh2D;
import org.jcae.mesh.amibe.patch.Vertex2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import gnu.trove.TIntIntHashMap;
import org.apache.log4j.Logger;


public class MeshReader
{
	private static Logger logger=Logger.getLogger(MeshReader.class);
	
	/**
	 * Loads an Amibe 2D XML file into an existing Mesh2D instance.
	 *
	 * @param mesh     data structure updated when reading files
	 * @param xmlDir   directory containing XML files
	 * @param xmlFile  basename of the main XML file
	 */
	public static void readObject(Mesh2D mesh, String xmlDir, String xmlFile)
		throws IOException
	{
		logger.debug("begin reading "+xmlDir+File.separator+xmlFile);
		XPath xpath = XPathFactory.newInstance().newXPath();
		Document document;
		
		try
		{
			document = XMLHelper.parseXML(new File(xmlDir, xmlFile));
		}
		catch (ParserConfigurationException ex)
		{
			throw new IOException(ex);
		}
		catch (SAXException ex)
		{
			throw new IOException(ex);
		}

		try
		{
			Node submeshElement = (Node) xpath.evaluate("/jcae/mesh/submesh",
				document, XPathConstants.NODE);
			Node submeshNodes = (Node) xpath.evaluate("nodes", submeshElement,
				XPathConstants.NODE);
			String refFile = xpath.evaluate("references/file/@location", submeshNodes);
			
			if (refFile.charAt(0) != File.separatorChar)
				refFile = xmlDir + File.separator + refFile;
			
			FileChannel fcR = new FileInputStream(refFile).getChannel();
			
			MappedByteBuffer bbR = fcR.map(FileChannel.MapMode.READ_ONLY, 0L,
				fcR.size());
			
			IntBuffer refsBuffer = bbR.asIntBuffer();
			String nodesFile = xpath.evaluate("file/@location", submeshNodes);
			
			if (nodesFile.charAt(0) != File.separatorChar) nodesFile = xmlDir
				+ File.separator + nodesFile;
			
			FileChannel fcN = new FileInputStream(nodesFile).getChannel();
			MappedByteBuffer bbN = fcN.map(FileChannel.MapMode.READ_ONLY, 0L,
				fcN.size());
			DoubleBuffer nodesBuffer = bbN.asDoubleBuffer();
			
			Node submeshTriangles = (Node) xpath.evaluate("triangles",
				submeshElement, XPathConstants.NODE);
			String trianglesFile = xpath.evaluate("file/@location",
				submeshTriangles);
			if (trianglesFile.charAt(0) != File.separatorChar)
				trianglesFile = xmlDir+File.separator+trianglesFile;
			FileChannel fcT = new FileInputStream(trianglesFile).getChannel();
			MappedByteBuffer bbT = fcT.map(FileChannel.MapMode.READ_ONLY, 0L, fcT.size());
			IntBuffer trianglesBuffer = bbT.asIntBuffer();

			int numberOfReferences = Integer.parseInt(
				xpath.evaluate("references/number/text()", submeshNodes));
			logger.debug("Reading "+numberOfReferences+" references");
			int [] refs = new int[numberOfReferences];
			refsBuffer.get(refs);
			
			int numberOfNodes = Integer.parseInt(
				xpath.evaluate("number/text()", submeshNodes));
			Vertex2D [] nodelist = new Vertex2D[numberOfNodes];
			int label;
			double [] coord = new double[2];
			logger.debug("Reading "+numberOfNodes+" nodes");
			double [] bbmin = { Double.MAX_VALUE, Double.MAX_VALUE };
			double [] bbmax = { Double.MIN_VALUE, Double.MIN_VALUE };
			for (int i=0; i < numberOfNodes; i++)
			{
				nodesBuffer.get(coord);
				nodelist[i] = (Vertex2D) mesh.createVertex(coord);
				if (i < numberOfNodes - numberOfReferences)
					label = 0;
				else
					label = refs[i+numberOfReferences-numberOfNodes];
				nodelist[i].setRef(label);
				for (int k=0; k < 2; k++)
				{
					if (coord[k] > bbmax[k])
						bbmax[k] = coord[k];
					if (coord[k] < bbmin[k])
						bbmin[k] = coord[k];
				}
			}
			mesh.resetQuadTree(bbmin, bbmax);
			for (int i=0; i < numberOfNodes; i++)
				mesh.getQuadTree().add(nodelist[i]);
			if (mesh.hasNodes())
			{
				for (int i=0; i < numberOfNodes; i++)
					mesh.add(nodelist[i]);
			}
			
			int numberOfTriangles = Integer.parseInt(
				xpath.evaluate("number/text()", submeshTriangles));
			logger.debug("Reading "+numberOfTriangles+" elements");
			AbstractTriangle [] facelist = new AbstractTriangle[numberOfTriangles];
			for (int i=0; i < numberOfTriangles; i++)
			{
				Vertex2D pt1 = nodelist[trianglesBuffer.get()];
				Vertex2D pt2 = nodelist[trianglesBuffer.get()];
				Vertex2D pt3 = nodelist[trianglesBuffer.get()];
				facelist[i] = mesh.createTriangle(pt1, pt2, pt3);
				mesh.add(facelist[i]);
			}
			fcT.close();
			MeshExporter.clean(bbT);
			fcN.close();
			MeshExporter.clean(bbN);
			fcR.close();
			MeshExporter.clean(bbR);
			//  Build adjacency relations
			mesh.buildAdjacency();
		}
		catch(XPathExpressionException ex)
		{
			throw new IOException(ex);
		}
		logger.debug("end reading "+xmlFile);
	}
	
	/**
	 * Loads an Amibe 3D XML file into an existing Mesh instance.
	 *
	 * @param mesh     data structure updated when reading files
	 * @param xmlDir   directory containing XML files
	 * @param xmlFile  basename of the main XML file
	 */
	public static void readObject3D(Mesh mesh, String xmlDir, String xmlFile)
		throws IOException
	{
		readObject3D(mesh, xmlDir, xmlFile, 0.0);

	}

	/**
	 * Loads an Amibe 3D XML file into an existing Mesh instance.
	 *
	 * @param mesh     data structure updated when reading files
	 * @param xmlDir   directory containing XML files
	 * @param xmlFile  basename of the main XML file
	 * @param ridgeAngle  an edge with a dihedral angle lower than this value is considered as a sharp edge which has to be preserved.
	 */
	public static void readObject3D(Mesh mesh, String xmlDir, String xmlFile, double ridgeAngle)
		throws IOException
	{
		logger.debug("begin reading "+xmlDir+File.separator+xmlFile);
		XPath xpath = XPathFactory.newInstance().newXPath();
		Document document;
		try
		{
			document = XMLHelper.parseXML(new File(xmlDir, xmlFile));
		}
		catch (ParserConfigurationException ex)
		{
			throw new IOException(ex);
		}
		catch (SAXException ex)
		{
			throw new IOException(ex);
		}

		try
		{
			Node submeshElement = (Node) xpath.evaluate("/jcae/mesh/submesh", document, XPathConstants.NODE);
			Node submeshNodes = (Node) xpath.evaluate("nodes", submeshElement, XPathConstants.NODE);
			String refFile = xpath.evaluate("references/file/@location", submeshNodes);
			int [] refs = null;
			int numberOfReferences = 0;

			if (refFile != null && refFile.length() > 0)
			{
				if (refFile.charAt(0) != File.separatorChar)
					refFile = xmlDir+File.separator+refFile;
				FileChannel fcR = new FileInputStream(refFile).getChannel();
				MappedByteBuffer bbR = fcR.map(FileChannel.MapMode.READ_ONLY, 0L, fcR.size());
				IntBuffer refsBuffer = bbR.asIntBuffer();
				numberOfReferences = Integer.parseInt(
					xpath.evaluate("references/number/text()", submeshNodes));
				logger.debug("Reading "+numberOfReferences+" references");
				refs = new int[numberOfReferences];
				refsBuffer.get(refs);
				fcR.close();
				MeshExporter.clean(bbR);
			}
			
			String nodesFile = xpath.evaluate("file/@location", submeshNodes);
			if (nodesFile.charAt(0) != File.separatorChar)
				nodesFile = xmlDir+File.separator+nodesFile;
			FileChannel fcN = new FileInputStream(nodesFile).getChannel();
			MappedByteBuffer bbN = fcN.map(FileChannel.MapMode.READ_ONLY, 0L, fcN.size());
			DoubleBuffer nodesBuffer = bbN.asDoubleBuffer();
			
			int numberOfNodes = Integer.parseInt(
				xpath.evaluate("number/text()", submeshNodes));
			Vertex [] nodelist = new Vertex[numberOfNodes];
			int label;
			double [] coord = new double[3];
			logger.debug("Reading "+numberOfNodes+" nodes");
			double [] bbmin = new double[3];
			double [] bbmax = new double[3];
			for (int j = 0; j < 3; j++)
			{
				bbmin[j] = Double.MAX_VALUE;
				bbmax[j] = Double.MIN_VALUE;
			}
			for (int i=0; i < numberOfNodes; i++)
			{
				nodesBuffer.get(coord);
				nodelist[i] = mesh.createVertex(coord);
				if (i < numberOfNodes - numberOfReferences)
					label = 0;
				else
					label = refs[i+numberOfReferences-numberOfNodes];
				nodelist[i].setRef(label);
				for (int j = 0; j < 3; j++)
				{
					if (coord[j] > bbmax[j])
						bbmax[j] = coord[j];
					if (coord[j] < bbmin[j])
						bbmin[j] = coord[j];
				}
			} 
			fcN.close();
			MeshExporter.clean(bbN);
			
			if (mesh.hasNodes())
			{
				for (int i=0; i < numberOfNodes; i++)
					mesh.add(nodelist[i]);
			}

			Node submeshTriangles = (Node) xpath.evaluate("triangles",
				submeshElement, XPathConstants.NODE);
			String trianglesFile = xpath.evaluate("file/@location",
				submeshTriangles);
			if (trianglesFile.charAt(0) != File.separatorChar)
				trianglesFile = xmlDir+File.separator+trianglesFile;
			FileChannel fcT = new FileInputStream(trianglesFile).getChannel();
			MappedByteBuffer bbT = fcT.map(FileChannel.MapMode.READ_ONLY, 0L, fcT.size());
			IntBuffer trianglesBuffer = bbT.asIntBuffer();
			int numberOfTriangles = Integer.parseInt(
				xpath.evaluate("number/text()", submeshTriangles));
			logger.debug("Reading "+numberOfTriangles+" elements");
			AbstractTriangle [] facelist = new AbstractTriangle[numberOfTriangles];
			for (int i=0; i < numberOfTriangles; i++)
			{
				Vertex pt1 = nodelist[trianglesBuffer.get()];
				Vertex pt2 = nodelist[trianglesBuffer.get()];
				Vertex pt3 = nodelist[trianglesBuffer.get()];
				facelist[i] = mesh.createTriangle(pt1, pt2, pt3);
				mesh.add(facelist[i]);
			}
			fcT.close();
			MeshExporter.clean(bbT);
			
			Node groupsElement = (Node) xpath.evaluate("groups", submeshElement,
				XPathConstants.NODE);
			NodeList groupsList = (NodeList) xpath.evaluate("group",
				groupsElement, XPathConstants.NODESET);
			int numberOfGroups = groupsList.getLength();
			String groupsFile = xpath.evaluate("file/@location", groupsList.item(0));
			if (groupsFile.charAt(0) != File.separatorChar)
				groupsFile = xmlDir+File.separator+groupsFile;
			FileChannel fcG = new FileInputStream(groupsFile).getChannel();
			MappedByteBuffer bbG = fcG.map(FileChannel.MapMode.READ_ONLY, 0L, fcG.size());
			IntBuffer groupsBuffer = bbG.asIntBuffer();
			// FIXME: Why is it much faster to build node lists than to iterate over
			//        group nodes and extract XPath expressions?
			NodeList groupNumberList = (NodeList) xpath.evaluate("group/number/text()",
				groupsElement, XPathConstants.NODESET);
			NodeList groupOffsetList = (NodeList) xpath.evaluate("group/file/@offset",
				groupsElement, XPathConstants.NODESET);
			NodeList groupIdList = (NodeList) xpath.evaluate("group/@id",
				groupsElement, XPathConstants.NODESET);
			for (int i=0; i < numberOfGroups; i++)
			{
				int numberOfElements = Integer.parseInt(groupNumberList.item(i).getTextContent());
				int fileOffset = Integer.parseInt(groupOffsetList.item(i).getTextContent());
				int id = Integer.parseInt(groupIdList.item(i).getTextContent());
				logger.debug("Group "+id+": reading "+numberOfElements+" elements");
				for (int j=0; j < numberOfElements; j++)
					facelist[groupsBuffer.get(fileOffset+j)].setGroupId(id);
			}
			fcG.close();
			MeshExporter.clean(bbG);
			//  Build adjacency relations
			if (mesh.hasAdjacency())
			{
				logger.debug("Build mesh adjacency");
				mesh.buildAdjacency(ridgeAngle);
			}
		}
		catch(XPathExpressionException ex)
		{
			throw new IOException(ex);
		}
		logger.debug("end reading "+xmlFile);
	}

	// Method previously in MMesh3DReader, remove it?
	public static int [] getInfos(String xmlDir, String xmlFile)
	{
		int [] ret = new int[3];
		XPath xpath = XPathFactory.newInstance().newXPath();
		try
		{
			Document document = XMLHelper.parseXML(new File(xmlDir, xmlFile));
			Node submeshElement = (Node) xpath.evaluate("/jcae/mesh/submesh",
				document, XPathConstants.NODE);
			Node submeshNodes = (Node) xpath.evaluate("nodes", submeshElement,
				XPathConstants.NODE);
			ret[0] = Integer.parseInt(xpath.evaluate("number/text()",
				submeshNodes));
			Node submeshTriangles = (Node) xpath.evaluate("triangles",
				submeshElement, XPathConstants.NODE);
			ret[1] = Integer.parseInt(xpath.evaluate("number/text()",
				submeshTriangles));
			Node groupsElement = (Node) xpath.evaluate("groups",
				submeshElement, XPathConstants.NODE);
			NodeList groupsList = (NodeList) xpath.evaluate("group",
				groupsElement, XPathConstants.NODESET);
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

	public static void mergeGroups(String xmlDir, String xmlInFile, String xmlGroupsFile)
	{
		XPath xpath = XPathFactory.newInstance().newXPath();
		try
		{
			File oldXmlFile = new File(xmlDir, xmlInFile);
			Document document = XMLHelper.parseXML(oldXmlFile);
			Node submeshElement = (Node) xpath.evaluate("/jcae/mesh/submesh",
				document, XPathConstants.NODE);
			Node groupsElement = (Node) xpath.evaluate("groups",
				submeshElement, XPathConstants.NODE);
			NodeList groupsList = (NodeList) xpath.evaluate("group",
				groupsElement, XPathConstants.NODESET);
			int numberOfGroups = groupsList.getLength();
			String groupsFileName = xpath.evaluate("file/@location", groupsList.item(0));
			String groupsFileDir = null;
			if (groupsFileName.charAt(0) != File.separatorChar)
				groupsFileDir = xmlDir;
			File oldGroupsFile = new File(groupsFileDir, groupsFileName);
			FileChannel fcG = new FileInputStream(oldGroupsFile).getChannel();
			MappedByteBuffer bbG = fcG.map(FileChannel.MapMode.READ_ONLY, 0L, fcG.size());
			IntBuffer groupsBuffer = bbG.asIntBuffer();
			int maxId = -1;
			MGroup3D [] groups = new MGroup3D[numberOfGroups];
			TIntIntHashMap numGroups = new TIntIntHashMap(numberOfGroups);
			for (int i=0; i < numberOfGroups; i++)
			{
				Node groupNode = groupsList.item(i);
				
				int numberOfElements = Integer.parseInt(
					xpath.evaluate("number/text()", groupNode));
				int fileOffset = Integer.parseInt(
					xpath.evaluate("file/@offset", groupNode));
				int id = Integer.parseInt(xpath.evaluate("@id", groupNode));
				numGroups.put(id, i);
				String name = xpath.evaluate("name/text()", groupNode);
				logger.debug("Group "+name+": reading "+numberOfElements+" elements");
				maxId = Math.max(maxId, id);
				Collection newfacelist = new ArrayList(numberOfElements);
				for (int j=0; j < numberOfElements; j++)
					newfacelist.add(Integer.valueOf(groupsBuffer.get(fileOffset+j)));
				groups[i] = new MGroup3D(id, name, newfacelist);
			}
			fcG.close();
			MeshExporter.clean(bbG);
			// Now merge groups
			Document documentGroup = XMLHelper.parseXML(new File(xmlDir, xmlGroupsFile));
			Node newGroupsElement = (Node) xpath.evaluate("/mergegroups",
				documentGroup, XPathConstants.NODE);
			NodeList newGroupsList = (NodeList) xpath.evaluate("newgroup",
				newGroupsElement, XPathConstants.NODESET);
			int numberOfNewGroups = newGroupsList.getLength();
			MGroup3D [] tmpgroups = new MGroup3D[numberOfGroups+numberOfNewGroups];
			System.arraycopy(groups, 0, tmpgroups, 0, groups.length);
			groups = tmpgroups;

			for (int i=0; i < numberOfNewGroups; i++)
			{
				Node newGroupNode = newGroupsList.item(i);
				maxId++;
				String name = xpath.evaluate("name/text()", newGroupNode);
				groups[numberOfGroups+i] = new MGroup3D(maxId, name, new ArrayList());
				numGroups.put(maxId, numberOfGroups+i);
				NodeList oldGroupsList = (NodeList) xpath.evaluate("oldgroup",
					newGroupNode, XPathConstants.NODESET);
				int numberOfOldGroups = oldGroupsList.getLength();
				logger.debug("Group "+name+": merging "+numberOfOldGroups+" groups");
				for (int j=0; j < numberOfOldGroups; j++)
				{
					Node oldGroupNode = oldGroupsList.item(j);
					int id = Integer.parseInt(xpath.evaluate("@id", oldGroupNode));
					int k = numGroups.get(id);
					if (k < 0 || k >= groups.length || groups[k] == null)
						throw new RuntimeException("Group id "+id+" does not exist. Aborting.");
					groups[numberOfGroups+i].merge(groups[k]);
					groups[k] = null;
				}
			}
			// Now write merged groups onto file
			File newGroupsFile = new File(groupsFileDir, groupsFileName+"-tmp");
			DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(newGroupsFile)));
			Element newGroups=document.createElement("groups");
			for (int i=0; i < groups.length; i++)
			{
				if (groups[i] == null)
					continue;
				newGroups.appendChild(
					XMLHelper.parseXMLString(document, "<group id=\""+groups[i].getId()+"\">"+
					"<name>"+groups[i].getName()+"</name>"+
					"<number>"+groups[i].numberOfFaces()+"</number>"+					
					"<file format=\"integerstream\" location=\""+
					XMLHelper.canonicalize(xmlDir, oldGroupsFile.toString())+"\""+
					" offset=\""+out.size()/4+"\"/></group>"));
				Iterator it = groups[i].getFacesIterator();
				while(it.hasNext())
					out.writeInt(((Integer) it.next()).intValue());
			}
			out.close();
			// Replace <groups> element
			submeshElement.replaceChild(newGroups, groupsElement);
			File newXmlFile = new File(xmlDir, xmlInFile+"-tmp");
			XMLHelper.writeXML(document, newXmlFile);
			if (!newXmlFile.renameTo(oldXmlFile))
				throw new RuntimeException("Cannot rename "+newXmlFile+" into "+oldXmlFile);
			if (!newGroupsFile.renameTo(oldGroupsFile))
				throw new RuntimeException("Cannot rename "+newGroupsFile+" into "+oldGroupsFile);

		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	
}

