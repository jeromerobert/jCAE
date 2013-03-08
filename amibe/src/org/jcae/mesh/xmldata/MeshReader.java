/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2005,2006, by EADS CRC
    Copyright (C) 2007,2008,2009, by EADS France
 
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

import java.util.logging.Level;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.ds.MGroup3D;
import org.jcae.mesh.amibe.patch.Mesh2D;
import org.jcae.mesh.amibe.patch.Vertex2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.parsers.ParserConfigurationException;
import org.jcae.mesh.xmldata.AmibeReader.SubMesh;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import gnu.trove.TIntIntHashMap;
import java.util.logging.Logger;


public class MeshReader
{
	private static final Logger logger=Logger.getLogger(MeshReader.class.getName());
	
	/**
	 * Loads an Amibe 2D XML file into an existing Mesh2D instance.
	 *
	 * @param mesh     data structure updated when reading files
	 * @param xmlDir   directory containing XML files
	 * @param iFace  face number
	 */
	public static void readObject(Mesh2D mesh, String xmlDir, int iFace)
		throws IOException
	{
		logger.fine("begin reading "+xmlDir+File.separator+JCAEXMLData.xml2dFilename+iFace);
		XPath xpath = XPathFactory.newInstance().newXPath();
		Document document;
		File xmlFile2d = null;
		
		try
		{
			xmlFile2d = new File(xmlDir, JCAEXMLData.xml2dFilename+iFace);
			document = XMLHelper.parseXML(xmlFile2d);
		}
		catch (ParserConfigurationException ex)
		{
			throw new IOException(ex.getMessage());
		}
		catch (SAXException ex)
		{
			throw new IOException(ex.getMessage());
		}

		try
		{
			String formatVersion = xpath.evaluate("/jcae/@version", document);
			if (formatVersion != null && formatVersion.length() > 0)
				throw new RuntimeException("File "+xmlFile2d+" has been written by a newer version of jCAE and cannot be re-read");
			Node submeshElement = (Node) xpath.evaluate("/jcae/mesh/submesh",
				document, XPathConstants.NODE);
			Node submeshNodes = (Node) xpath.evaluate("nodes", submeshElement,
				XPathConstants.NODE);
			String refFile = xpath.evaluate("references/file/@location", submeshNodes);
			
			if (refFile.charAt(0) != File.separatorChar)
				refFile = xmlDir + File.separator + refFile;

			PrimitiveFileReaderFactory pfrf = new PrimitiveFileReaderFactory();
			IntFileReader ifrR = pfrf.getIntReader(new File(refFile));

			String nodesFile = xpath.evaluate("file/@location", submeshNodes);
			
			if (nodesFile.charAt(0) != File.separatorChar) nodesFile = xmlDir
				+ File.separator + nodesFile;
			DoubleFileReader dfrN = pfrf.getDoubleReader(new File(nodesFile));
			
			Node submeshTriangles = (Node) xpath.evaluate("triangles",
				submeshElement, XPathConstants.NODE);
			String trianglesFile = xpath.evaluate("file/@location",
				submeshTriangles);
			if (trianglesFile.charAt(0) != File.separatorChar)
				trianglesFile = xmlDir+File.separator+trianglesFile;
			IntFileReader ifrT = pfrf.getIntReader(new File(trianglesFile));

			int numberOfReferences = Integer.parseInt(
				xpath.evaluate("references/number/text()", submeshNodes));
			logger.fine("Reading "+numberOfReferences+" references");
			int [] refs = new int[numberOfReferences];
			ifrR.get(refs);
			
			int numberOfNodes = Integer.parseInt(
				xpath.evaluate("number/text()", submeshNodes));
			Vertex2D [] nodelist = new Vertex2D[numberOfNodes+1];
			nodelist[numberOfNodes] = (Vertex2D) mesh.outerVertex;
			int label;
			double [] coord = new double[2];
			logger.fine("Reading "+numberOfNodes+" nodes");
			double [] bbmin = { Double.MAX_VALUE, Double.MAX_VALUE };
			double [] bbmax = { Double.MIN_VALUE, Double.MIN_VALUE };
			for (int i=0; i < numberOfNodes; i++)
			{
				dfrN.get(coord);
				nodelist[i] = (Vertex2D) mesh.createVertex(coord[0], coord[1]);
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
			mesh.ensureCapacity(2*numberOfNodes);
			mesh.resetKdTree(bbmin, bbmax);
			for (int i=0; i < numberOfNodes; i++)
				mesh.getKdTree().add(nodelist[i]);
			if (mesh.hasNodes())
			{
				for (int i=0; i < numberOfNodes; i++)
					mesh.add(nodelist[i]);
			}
			
			int numberOfTriangles = Integer.parseInt(
				xpath.evaluate("number/text()", submeshTriangles));
			logger.fine("Reading "+numberOfTriangles+" elements");
			Triangle [] facelist = new Triangle[numberOfTriangles];
			int [] ind = new int[3];
			Vertex2D [] pts = new Vertex2D[3];
			for (int i=0; i < numberOfTriangles; i++)
			{
				boolean outer = false;
				for (int j = 0; j < 3; j++)
				{
					ind[j] = ifrT.get();
					if (ind[j] < 0)
					{
						ind[j] = - ind[j];
						outer = true;
					}
					pts[j] = nodelist[ind[j]];
				}
				facelist[i] = mesh.createTriangle(pts[0], pts[1], pts[2]);
				if (outer)
				{
					facelist[i].setAttributes(AbstractHalfEdge.OUTER);
					facelist[i].setReadable(false);
					facelist[i].setWritable(false);
				}
				mesh.add(facelist[i]);
			}
			ifrT.close();
			dfrN.close();
			ifrR.close();
			//  Build adjacency relations
			mesh.buildAdjacency();
		}
		catch(XPathExpressionException ex)
		{
			throw new IOException(ex.getMessage());
		}
		logger.fine("end reading "+JCAEXMLData.xml2dFilename+iFace);
	}

	/**
	 * Loads an Amibe 3D XML file into an existing Mesh instance.
	 *
	 * @param mesh     data structure updated when reading files
	 * @param xmlDir   directory containing XML files
	 */
	public static void readObject3D(Mesh mesh, String xmlDir)
		throws IOException
	{
		try {
			AmibeReader.Dim3 reader = new AmibeReader.Dim3(xmlDir);
			SubMesh subMesh = reader.getSubmeshes().get(0);
			int numberOfReferences = subMesh.getNumberOfReferences();
			int[] refs = null;
			if (numberOfReferences > 0) {
				refs = subMesh.getReferences();
				mesh.setPersistentReferences(true);
			}
			DoubleFileReader dfrN = subMesh.getNodes();
			int numberOfNodes = subMesh.getNumberOfNodes();
			Vertex[] nodelist = new Vertex[numberOfNodes + 1];
			nodelist[numberOfNodes] = mesh.outerVertex;
			int label;
			double[] coord = new double[3];
			double[] bbmin = new double[3];
			double[] bbmax = new double[3];
			for (int j = 0; j < 3; j++) {
				bbmin[j] = Double.MAX_VALUE;
				bbmax[j] = Double.MIN_VALUE;
			}
			mesh.ensureCapacity(2 * numberOfNodes);
			for (int i = 0; i < numberOfNodes; i++) {
				dfrN.get(coord);
				nodelist[i] = mesh.createVertex(coord[0], coord[1], coord[2]);
				if (i < numberOfNodes - numberOfReferences) {
					label = 0;
				} else {
					assert refs != null;
					label = refs[i + numberOfReferences - numberOfNodes];
				}
				nodelist[i].setRef(label);
				for (int j = 0; j < 3; j++) {
					if (coord[j] > bbmax[j]) {
						bbmax[j] = coord[j];
					}
					if (coord[j] < bbmin[j]) {
						bbmin[j] = coord[j];
					}
				}
			}
			dfrN.close();
			if (mesh.hasNodes()) {
				for (int i = 0; i < numberOfNodes; i++) {
					mesh.add(nodelist[i]);
				}
			}

			int numberOfTriangles = subMesh.getNumberOfTrias();
			Triangle[] facelist = new Triangle[numberOfTriangles];
			if(numberOfTriangles > 0)
			{
				IntFileReader ifrT = subMesh.getTriangles();
				int[] ind = new int[3];
				Vertex[] pts = new Vertex[3];
				for (int i = 0; i < numberOfTriangles; i++) {
					boolean outer = false;
					for (int j = 0; j < 3; j++) {
						ind[j] = ifrT.get();
						if (ind[j] < 0) {
							ind[j] = -ind[j];
							outer = true;
						}
						pts[j] = nodelist[ind[j]];
					}
					if (!outer) {
						facelist[i] = mesh.createTriangle(pts[0], pts[1], pts[2]);
						mesh.add(facelist[i]);
					}
				}
				ifrT.close();
			}

			int numberOfBeams = subMesh.getNumberOfBeams();
			if (numberOfBeams > 0)
			{
				IntFileReader ifrB = subMesh.getBeams();
				for (int i = 0; i < numberOfBeams; i++)
					mesh.addBeam(nodelist[ifrB.get()], nodelist[ifrB.get()], 0);
				ifrB.close();
			}

			int i = 1;
			for (AmibeReader.Group g : subMesh.getGroups()) {
				int id = i++;
				mesh.setGroupName(id, g.getName());
				for (int j : g.readTria3Ids())
				{
					Triangle f = facelist[j];
					int prevId = f.getGroupId();
					if(prevId > 0)
						logger.warning("Trying to tag triangle "+j+" as "+
							g.getName()+"/"+id+" while it's alread tagged as "+
							mesh.getGroupName(prevId)+"/"+prevId+".");
					f.setGroupId(id);
				}
				for(int j : g.readBeamsIds())
					mesh.setBeamGroup(j, id);
				for(int j : g.readNodesIds())
					mesh.setVertexGroup(nodelist[j], g.getName());
			}
			//  Build adjacency relations
			if (mesh.hasAdjacency()) {
				logger.fine("Build mesh adjacency");
				mesh.buildAdjacency();
			}
		} catch (SAXException ex) {
			throw new IOException(ex);
		}
	}

	// Method previously in MMesh3DReader, remove it?
	public static int [] getInfos(String xmlDir)
	{
		int [] ret = new int[3];
		XPath xpath = XPathFactory.newInstance().newXPath();
		try
		{
			Document document = XMLHelper.parseXML(new File(xmlDir, JCAEXMLData.xml3dFilename));
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

	public static void mergeGroups(String xmlDir, String xmlGroupsFile)
	{
		XPath xpath = XPathFactory.newInstance().newXPath();
		try
		{
			File oldXmlFile = new File(xmlDir, JCAEXMLData.xml3dFilename);
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
			IntFileReader ifrG = new PrimitiveFileReaderFactory().getIntReader(oldGroupsFile);
			int maxId = -1;
			MGroup3D [] groups = new MGroup3D[numberOfGroups];
			TIntIntHashMap numGroups = new TIntIntHashMap(numberOfGroups);
			for (int i=0; i < numberOfGroups; i++)
			{
				Element groupNode = (Element) groupsList.item(i);
				int numberOfElements = Integer.parseInt(groupNode.getElementsByTagName("number").item(0).getTextContent());
				int fileOffset = Integer.parseInt(((Element) groupNode.getElementsByTagName("file").item(0)).getAttribute("offset"));
				int id = Integer.parseInt(groupNode.getAttribute("id"));
				numGroups.put(id, i);
				String name = groupNode.getElementsByTagName("name").item(0).getTextContent();
				logger.fine("Group "+name+": reading "+numberOfElements+" elements");
				maxId = Math.max(maxId, id);
				Collection newfacelist = new ArrayList(numberOfElements);
				for (int j=0; j < numberOfElements; j++)
					newfacelist.add(Integer.valueOf(ifrG.get(fileOffset+j)));
				groups[i] = new MGroup3D(id, name, newfacelist);
			}
			ifrG.close();
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
				logger.fine("Group "+name+": merging "+numberOfOldGroups+" groups");
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
			File newXmlFile = new File(xmlDir, JCAEXMLData.xml3dFilename+"-tmp");
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
	
	public static void main(String [] args)
	{
		Mesh mesh = new Mesh();
		try
		{
			readObject3D(mesh, args[0]);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}

