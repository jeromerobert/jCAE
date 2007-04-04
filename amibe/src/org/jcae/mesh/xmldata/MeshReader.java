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
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.patch.Mesh2D;
import org.jcae.mesh.amibe.patch.Vertex2D;
import org.jcae.mesh.cad.CADFace;
import java.io.File;
import java.io.FileInputStream;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.log4j.Logger;


public class MeshReader
{
	private static Logger logger=Logger.getLogger(MeshReader.class);
	
	/**
	 * Create a Mesh instance from an XML file.
	 * @param xmlDir       directory containing XML files
	 * @param xmlFile      basename of the main XML file
	 * @param F            yopological surface
	 */
	public static Mesh2D readObject(String xmlDir, String xmlFile, CADFace F)
	{
		Mesh2D mesh = new Mesh2D(F);
		logger.debug("begin reading "+xmlDir+File.separator+xmlFile);
		XPath xpath = XPathFactory.newInstance().newXPath();
		
		try
		{
			Document document = XMLHelper.parseXML(new File(xmlDir, xmlFile));
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
				nodelist[i] = (Vertex2D) Vertex.valueOf(coord);
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
			mesh.initQuadTree(bbmin, bbmax);
			for (int i=0; i < numberOfNodes; i++)
				mesh.getQuadTree().add(nodelist[i]);
			
			int numberOfTriangles = Integer.parseInt(
				xpath.evaluate("number/text()", submeshTriangles));
			logger.debug("Reading "+numberOfTriangles+" elements");
			Triangle [] facelist = new Triangle[numberOfTriangles];
			for (int i=0; i < numberOfTriangles; i++)
			{
				Vertex2D pt1 = nodelist[trianglesBuffer.get()];
				Vertex2D pt2 = nodelist[trianglesBuffer.get()];
				Vertex2D pt3 = nodelist[trianglesBuffer.get()];
				facelist[i] = new Triangle(pt1, pt2, pt3);
				mesh.add(facelist[i]);
				pt1.setLink(facelist[i]);
				pt2.setLink(facelist[i]);
				pt3.setLink(facelist[i]);
			}
			fcT.close();
			MeshExporter.clean(bbT);
			fcN.close();
			MeshExporter.clean(bbN);
			fcR.close();
			MeshExporter.clean(bbR);
			//  Build adjacency relations
			mesh.buildAdjacency(nodelist, -1.0);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		logger.debug("end reading "+xmlFile);
		return mesh;
	}
	
	public static Mesh readObject3D(String xmlDir, String xmlFile)
	{
		return readObject3D(xmlDir, xmlFile, 0.0, false);
	}
	
	public static Mesh readObject3D(String xmlDir, String xmlFile, double ridgeAngle)
	{
		return readObject3D(xmlDir, xmlFile, ridgeAngle, true);
	}
	
	private static Mesh readObject3D(String xmlDir, String xmlFile, double ridgeAngle, boolean buildAdj)
	{
		Mesh mesh = new Mesh();
		logger.debug("begin reading "+xmlDir+File.separator+xmlFile);
		XPath xpath = XPathFactory.newInstance().newXPath();
		try
		{
			Document document = XMLHelper.parseXML(new File(xmlDir, xmlFile));
			Node submeshElement = (Node) xpath.evaluate("/jcae/mesh/submesh", document, XPathConstants.NODE);
			Node submeshNodes = (Node) xpath.evaluate("nodes", submeshElement, XPathConstants.NODE);
			String refFile = xpath.evaluate("references/file/@location", submeshNodes);

			if (refFile.charAt(0) != File.separatorChar)
				refFile = xmlDir+File.separator+refFile;
			FileChannel fcR = new FileInputStream(refFile).getChannel();
			MappedByteBuffer bbR = fcR.map(FileChannel.MapMode.READ_ONLY, 0L, fcR.size());
			IntBuffer refsBuffer = bbR.asIntBuffer();
			int numberOfReferences = Integer.parseInt(
				xpath.evaluate("references/number/text()", submeshNodes));
			logger.debug("Reading "+numberOfReferences+" references");
			int [] refs = new int[numberOfReferences];
			refsBuffer.get(refs);
			fcR.close();
			MeshExporter.clean(bbR);
			
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
				nodelist[i] = Vertex.valueOf(coord);
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
			Triangle [] facelist = new Triangle[numberOfTriangles];
			for (int i=0; i < numberOfTriangles; i++)
			{
				Vertex pt1 = nodelist[trianglesBuffer.get()];
				Vertex pt2 = nodelist[trianglesBuffer.get()];
				Vertex pt3 = nodelist[trianglesBuffer.get()];
				facelist[i] = new Triangle(pt1, pt2, pt3);
				mesh.add(facelist[i]);
				pt1.setLink(facelist[i]);
				pt2.setLink(facelist[i]);
				pt3.setLink(facelist[i]);
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
			for (int i=0; i < numberOfGroups; i++)
			{
				Node groupNode = groupsList.item(i);
				
				int numberOfElements = Integer.parseInt(
					xpath.evaluate("number/text()", groupNode));
				int fileOffset = Integer.parseInt(
					xpath.evaluate("file/@offset", groupNode));
				
				int id=Integer.parseInt(
					xpath.evaluate("@id", groupNode));
				logger.debug("Group "+id+": reading "+numberOfElements+" elements");
								
				for (int j=0; j < numberOfElements; j++)
					facelist[groupsBuffer.get(fileOffset+j)].setGroupId(id);
			}
			fcG.close();
			MeshExporter.clean(bbG);
			//  Build adjacency relations
			if (buildAdj)
				mesh.buildAdjacency(nodelist, ridgeAngle);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		logger.debug("end reading "+xmlFile);
		return mesh;
	}
}

