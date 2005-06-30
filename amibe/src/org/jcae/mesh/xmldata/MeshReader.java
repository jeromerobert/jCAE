/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.
 
    Copyright (C) 2005
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
import org.jcae.mesh.cad.CADFace;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.xpath.CachedXPathAPI;
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
	public static Mesh readObject(String xmlDir, String xmlFile, CADFace F)
	{
		Mesh mesh = new Mesh(F);
		logger.debug("begin reading "+xmlDir+File.separator+xmlFile);
		CachedXPathAPI xpath = new CachedXPathAPI();
		try
		{
			Document document = XMLHelper.parseXML(new File(xmlDir, xmlFile));
			Node submeshElement = xpath.selectSingleNode(document, "/jcae/mesh/submesh");
			Node submeshNodes = xpath.selectSingleNode(submeshElement, "nodes");
			String refFile = xpath.selectSingleNode(submeshNodes,
				"references/file/@location").getNodeValue();
			if (refFile.charAt(0) != File.separatorChar)
				refFile = xmlDir+File.separator+refFile;
			DataInputStream refsIn=new DataInputStream(new BufferedInputStream(new FileInputStream(refFile)));
			String nodesFile = xpath.selectSingleNode(submeshNodes, "file/@location").getNodeValue();
			if (nodesFile.charAt(0) != File.separatorChar)
				nodesFile = xmlDir+File.separator+nodesFile;
			DataInputStream nodesIn=new DataInputStream(new BufferedInputStream(new FileInputStream(nodesFile)));
			
			Node submeshTriangles = xpath.selectSingleNode(submeshElement, "triangles");
			String trianglesFile = xpath.selectSingleNode(submeshTriangles,
				"file/@location").getNodeValue();
			if (trianglesFile.charAt(0) != File.separatorChar)
				trianglesFile = xmlDir+File.separator+trianglesFile;
			DataInputStream trianglesIn=new DataInputStream(new BufferedInputStream(new FileInputStream(trianglesFile)));

			int numberOfReferences = Integer.parseInt(
				xpath.selectSingleNode(submeshNodes, "references/number/text()").getNodeValue());
			logger.debug("Reading "+numberOfReferences+" references");
			int [] refs = new int[numberOfReferences];
			for (int i=0; i < numberOfReferences; i++)
				refs[i] = refsIn.readInt();
			
			int numberOfNodes = Integer.parseInt(
				xpath.selectSingleNode(submeshNodes, "number/text()").getNodeValue());
			Vertex [] nodelist = new Vertex[numberOfNodes];
			int label;
			double [] coord = new double[2];
			logger.debug("Reading "+numberOfNodes+" nodes");
			double umin = Double.MAX_VALUE;
			double umax = Double.MIN_VALUE;
			double vmin = Double.MAX_VALUE;
			double vmax = Double.MIN_VALUE;
			for (int i=0; i < numberOfNodes; i++)
			{
				coord[0] = nodesIn.readDouble();
				coord[1] = nodesIn.readDouble();
				nodelist[i] = new Vertex(coord[0], coord[1]);
				if (i < numberOfNodes - numberOfReferences)
					label = -1;
				else
					label = refs[i+numberOfReferences-numberOfNodes];
				nodelist[i].setRef(label);
				if (coord[0] > umax)
					umax = coord[0];
				if (coord[0] < umin)
					umin = coord[0];
				if (coord[1] > vmax)
					vmax = coord[1];
				if (coord[1] < vmin)
					vmin = coord[1];
			}
			mesh.initQuadTree(umin, umax, vmin, vmax);
			for (int i=0; i < numberOfNodes; i++)
				nodelist[i].addToQuadTree();
			
			int numberOfTriangles = Integer.parseInt(
				xpath.selectSingleNode(submeshTriangles, "number/text()").getNodeValue());
			logger.debug("Reading "+numberOfTriangles+" elements");
			Triangle [] facelist = new Triangle[numberOfTriangles];
			for (int i=0; i < numberOfTriangles; i++)
			{
				Vertex pt1 = nodelist[trianglesIn.readInt()];
				Vertex pt2 = nodelist[trianglesIn.readInt()];
				Vertex pt3 = nodelist[trianglesIn.readInt()];
				facelist[i] = new Triangle(pt1, pt2, pt3);
				mesh.add(facelist[i]);
				pt1.tri = facelist[i];
				pt2.tri = facelist[i];
				pt3.tri = facelist[i];
			}
			trianglesIn.close();
			nodesIn.close();
			refsIn.close();
			//  Build adjacency relations
			mesh.buildAdjacency(nodelist);
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
		Mesh mesh = new Mesh();
		logger.debug("begin reading "+xmlDir+File.separator+xmlFile);
		CachedXPathAPI xpath = new CachedXPathAPI();
		try
		{
			Document document = XMLHelper.parseXML(new File(xmlDir, xmlFile));
			Node submeshElement = xpath.selectSingleNode(document, "/jcae/mesh/submesh");
			Node submeshNodes = xpath.selectSingleNode(submeshElement, "nodes");
			String refFile = xpath.selectSingleNode(submeshNodes,
				"references/file/@location").getNodeValue();
			if (refFile.charAt(0) != File.separatorChar)
				refFile = xmlDir+File.separator+refFile;
			DataInputStream refsIn=new DataInputStream(new BufferedInputStream(new FileInputStream(refFile)));
			String nodesFile = xpath.selectSingleNode(submeshNodes, "file/@location").getNodeValue();
			if (nodesFile.charAt(0) != File.separatorChar)
				nodesFile = xmlDir+File.separator+nodesFile;
			DataInputStream nodesIn=new DataInputStream(new BufferedInputStream(new FileInputStream(nodesFile)));
			
			Node submeshTriangles = xpath.selectSingleNode(submeshElement, "triangles");
			String trianglesFile = xpath.selectSingleNode(submeshTriangles,
				"file/@location").getNodeValue();
			if (trianglesFile.charAt(0) != File.separatorChar)
				trianglesFile = xmlDir+File.separator+trianglesFile;
			DataInputStream trianglesIn=new DataInputStream(new BufferedInputStream(new FileInputStream(trianglesFile)));

			int numberOfReferences = Integer.parseInt(
				xpath.selectSingleNode(submeshNodes, "references/number/text()").getNodeValue());
			logger.debug("Reading "+numberOfReferences+" references");
			int [] refs = new int[numberOfReferences];
			for (int i=0; i < numberOfReferences; i++)
				refs[i] = refsIn.readInt();
			
			int numberOfNodes = Integer.parseInt(
				xpath.selectSingleNode(submeshNodes, "number/text()").getNodeValue());
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
				coord[0] = nodesIn.readDouble();
				coord[1] = nodesIn.readDouble();
				coord[2] = nodesIn.readDouble();
				nodelist[i] = new Vertex(coord[0], coord[1], coord[2]);
				if (i < numberOfNodes - numberOfReferences)
					label = -1;
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
			
			int numberOfTriangles = Integer.parseInt(
				xpath.selectSingleNode(submeshTriangles, "number/text()").getNodeValue());
			logger.debug("Reading "+numberOfTriangles+" elements");
			Triangle [] facelist = new Triangle[numberOfTriangles];
			for (int i=0; i < numberOfTriangles; i++)
			{
				Vertex pt1 = nodelist[trianglesIn.readInt()];
				Vertex pt2 = nodelist[trianglesIn.readInt()];
				Vertex pt3 = nodelist[trianglesIn.readInt()];
				facelist[i] = new Triangle(pt1, pt2, pt3);
				mesh.add(facelist[i]);
				pt1.tri = facelist[i];
				pt2.tri = facelist[i];
				pt3.tri = facelist[i];
			}
			trianglesIn.close();
			nodesIn.close();
			refsIn.close();
			//  Build adjacency relations
			mesh.buildAdjacency(nodelist);
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

