/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005, by EADS CRC
    Copyright (C) 2007,2008, by EADS France
 
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

import java.io.File;
import java.io.FileInputStream;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.HashSet;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.jcae.mesh.amibe.ds.MEdge1D;
import org.jcae.mesh.amibe.ds.MMesh1D;
import org.jcae.mesh.amibe.ds.MNode1D;
import org.jcae.mesh.amibe.ds.SubMesh1D;
import org.jcae.mesh.cad.CADEdge;
import org.jcae.mesh.cad.CADVertex;
import org.jcae.mesh.cad.CADShape;
import org.jcae.mesh.cad.CADShapeFactory;
import org.jcae.mesh.cad.CADShapeEnum;
import org.jcae.mesh.cad.CADExplorer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.logging.Logger;


public class MMesh1DReader
{
	private static Logger logger=Logger.getLogger(MMesh1DReader.class.getName());
	
	/** Return the first child element of with the given tag name */
	private static Node getChild(Node e, String tagName)
	{
		Node n=e.getFirstChild();
		while(n!=null)
		{
			if(n instanceof Element)
			{
				if(tagName.equals(n.getNodeName()))
				{
					return n;
				}
			}			
			n=n.getNextSibling();
		}		
		return null;
	}
	
	private static int getReferenceNumber(Node nodeElement)
	{
		Node ref=getChild(nodeElement, "references");
		Node num=getChild(ref, "number");
		return Integer.parseInt(num.getFirstChild().getNodeValue());
	}
	/**
	 * Write the current object to a XML file and binary files. The XML file
	 * have links to the binary files.
	 * @param xmlDir       name of the XML file
	 */
	public static MMesh1D readObject(String xmlDir)
	{
		MMesh1D m1d = null;
		int i;
		File xmlFile1d = new File(xmlDir, JCAEXMLData.xml1dFilename);
		logger.fine("begin reading "+xmlFile1d);
		XPath xpath = XPathFactory.newInstance().newXPath();
		HashMap<CADVertex, MNode1D> map1DToMaster = new HashMap<CADVertex, MNode1D>();
		try
		{
			Document document = XMLHelper.parseXML(xmlFile1d);
			String formatVersion = xpath.evaluate("/jcae/@version", document);
			if (formatVersion != null && formatVersion.length() > 0)
				throw new RuntimeException("File "+xmlFile1d+" has been written by a newer version of jCAE and cannot be re-read");
			String brepFile = xpath.evaluate("/jcae/mesh/shape/file/@location", document);
			
			if(!new File(brepFile).isAbsolute())
				brepFile = xmlDir+File.separator+brepFile;
			
			m1d = new MMesh1D(brepFile);
			CADShape shape = m1d.getGeometry();
			
			String nodesFile = xpath.evaluate(
				"/jcae/mesh/submesh/nodes/file/@location", document);
			if (nodesFile.charAt(0) != File.separatorChar)
				nodesFile = xmlDir+File.separator+nodesFile;
			FileChannel fcN = new FileInputStream(nodesFile).getChannel();
			MappedByteBuffer bbN = fcN.map(FileChannel.MapMode.READ_ONLY, 0L, fcN.size());
			DoubleBuffer nodesBuffer = bbN.asDoubleBuffer();
			String refFile = xpath.evaluate(
				"/jcae/mesh/submesh/nodes/references/file/@location", document);
			if (refFile.charAt(0) != File.separatorChar)
				refFile = xmlDir+File.separator+refFile;
			IntFileReader ifrR = new IntFileReaderByDirectBuffer(new File(refFile));

			String edgesFile = xpath.evaluate(
				"/jcae/mesh/submesh/beams/file/@location", document);
			if (edgesFile.charAt(0) != File.separatorChar)
				edgesFile = xmlDir+File.separator+edgesFile;
			IntFileReader ifrE = new IntFileReaderByDirectBuffer(new File(edgesFile));

			NodeList submeshList = (NodeList) xpath.evaluate(
				"/jcae/mesh/submesh", document.getDocumentElement(),
				XPathConstants.NODESET);

			int iEdge = 0;
			//  References are counted from 1; 0 means an inner
			//  vertex.  Offset is thus set to 1.
			int offset = 1;
			HashSet<CADEdge> setSeenEdges = new HashSet<CADEdge>();
			CADExplorer expE = CADShapeFactory.getFactory().newExplorer();
			for (expE.init(shape, CADShapeEnum.EDGE); expE.more(); expE.next())
			{
				CADEdge E = (CADEdge) expE.current();
				SubMesh1D submesh = m1d.getSubMesh1DFromMap(E);
				if (null == submesh || setSeenEdges.contains(E))
					continue;
				setSeenEdges.add(E);

				iEdge++;
				submesh.getNodes().clear();
				submesh.getEdges().clear();
				Node submeshElement = submeshList.item(iEdge-1);
				Node submeshNodes = getChild(submeshElement, "nodes");				
				int numberOfReferences =  getReferenceNumber(submeshNodes);
				int [] refs = new int[2*numberOfReferences];
				ifrR.get(refs);
				for (i=0; i < numberOfReferences; i++)
					refs[2*i] -= offset;
				
				int numberOfNodes = Integer.parseInt(
					getChild(submeshNodes, "number").getFirstChild().getNodeValue());
				MNode1D [] nodelist = new MNode1D[numberOfNodes];
				int iref = 0;
				for (i=0; i < numberOfNodes; i++)
				{
					if (iref < 2*numberOfReferences && refs[iref] == i)
					{
						CADVertex V = m1d.getGeometricalVertex(refs[iref+1]);
						nodelist[i] = new MNode1D(nodesBuffer.get(), V);
						MNode1D master = map1DToMaster.get(V);
						if (null == master)
							map1DToMaster.put(V, nodelist[i]);
						else
							nodelist[i].setMaster(master);
						iref += 2;
					}
					else
						nodelist[i] = new MNode1D(nodesBuffer.get(), (CADVertex) null);
					submesh.getNodes().add(nodelist[i]);
				}
				
				Node submeshEdges = getChild(submeshElement, "beams"); 
				int numberOfEdges = Integer.parseInt(
					getChild(submeshEdges, "number").getFirstChild().getNodeValue());
					
				for (i=0; i < numberOfEdges; i++)
				{
					MNode1D pt1 = nodelist[ifrE.get() - offset];
					MNode1D pt2 = nodelist[ifrE.get() - offset];
					MEdge1D e = new MEdge1D(pt1, pt2);
					submesh.getEdges().add(e);
				}
				offset += numberOfNodes;
			}
			ifrE.close();
			fcN.close();
			MeshExporter.clean(bbN);
			ifrR.close();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		logger.fine("end reading "+JCAEXMLData.xml1dFilename);
		return m1d;
	}
}

