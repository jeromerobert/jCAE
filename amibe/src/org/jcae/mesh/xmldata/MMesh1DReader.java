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

import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.xpath.CachedXPathAPI;
import org.jcae.mesh.cad.CADEdge;
import org.jcae.mesh.cad.CADVertex;
import org.jcae.mesh.cad.CADShape;
import org.jcae.mesh.cad.CADShapeBuilder;
import org.jcae.mesh.cad.CADExplorer;
import org.jcae.mesh.mesher.ds.MEdge1D;
import org.jcae.mesh.mesher.ds.MMesh1D;
import org.jcae.mesh.mesher.ds.MNode1D;
import org.jcae.mesh.mesher.ds.SubMesh1D;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.log4j.Logger;


public class MMesh1DReader
{
	private static Logger logger=Logger.getLogger(MMesh1DReader.class);
	
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
	 * @param xmlFile      basename of the main XML file
	 */
	public static MMesh1D readObject(String xmlDir, String xmlFile)
	{
		MMesh1D m1d = null;
		int i;
		logger.debug("begin reading "+xmlDir+File.separator+xmlFile);
		CachedXPathAPI xpath = new CachedXPathAPI();
		HashMap map1DToMaster = new HashMap();
		try
		{
			Document document = XMLHelper.parseXML(new File(xmlDir, xmlFile));
			String brepFile = xpath.selectSingleNode(document,
				"/jcae/mesh/shape/file/@location").getNodeValue();
			
			if(!new File(brepFile).isAbsolute())
				brepFile = xmlDir+File.separator+brepFile;
			
			CADShape shape = CADShapeBuilder.factory.newShape(brepFile);
			m1d = new MMesh1D(shape);
			
			String nodesFile = xpath.selectSingleNode(document,
				"/jcae/mesh/submesh/nodes/file/@location").getNodeValue();
			if (nodesFile.charAt(0) != File.separatorChar)
				nodesFile = xmlDir+File.separator+nodesFile;
			DataInputStream nodesIn=new DataInputStream(new BufferedInputStream(new FileInputStream(nodesFile)));
			String refFile = xpath.selectSingleNode(document,
				"/jcae/mesh/submesh/nodes/references/file/@location").getNodeValue();
			if (refFile.charAt(0) != File.separatorChar)
				refFile = xmlDir+File.separator+refFile;
			DataInputStream refsIn=new DataInputStream(new BufferedInputStream(new FileInputStream(refFile)));
			String edgesFile = xpath.selectSingleNode(document,
				"/jcae/mesh/submesh/beams/file/@location").getNodeValue();
			if (edgesFile.charAt(0) != File.separatorChar)
				edgesFile = xmlDir+File.separator+edgesFile;
			DataInputStream edgesIn=new DataInputStream(new BufferedInputStream(new FileInputStream(edgesFile)));

			NodeList submeshList = xpath.selectNodeList(
				document.getDocumentElement(),
				"/jcae/mesh/submesh");

			int iEdge = 0;
			int offset = 0;
			HashSet setSeenEdges = new HashSet();
			CADExplorer expE = CADShapeBuilder.factory.newExplorer();
			for (expE.init(shape, CADExplorer.EDGE); expE.more(); expE.next())
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
				for (i=0; i < 2*numberOfReferences; i+=2)
				{
					refs[i]= refsIn.readInt() - offset;
					refs[i+1] = refsIn.readInt();
				}
				
				int numberOfNodes = Integer.parseInt(
					getChild(submeshNodes, "number").getFirstChild().getNodeValue());
				MNode1D [] nodelist = new MNode1D[numberOfNodes];
				int iref = 0;
				for (i=0; i < numberOfNodes; i++)
				{
					if (iref < 2*numberOfReferences && refs[iref] == i)
					{
						CADVertex V = m1d.getGeometricalVertex(refs[iref+1]);
						nodelist[i] = new MNode1D(nodesIn.readDouble(), V);
						MNode1D master = (MNode1D) map1DToMaster.get(V);
						if (null == master)
							map1DToMaster.put(V, nodelist[i]);
						else
							nodelist[i].setMaster(master);
						iref += 2;
					}
					else
						nodelist[i] = new MNode1D(nodesIn.readDouble(), null);
					submesh.getNodes().add(nodelist[i]);
				}
				
				Node submeshEdges = getChild(submeshElement, "beams"); 
				int numberOfEdges = Integer.parseInt(
					getChild(submeshEdges, "number").getFirstChild().getNodeValue());
					
				MEdge1D [] edgelist = new MEdge1D[numberOfEdges];
				for (i=0; i < numberOfEdges; i++)
				{
					MNode1D pt1 = nodelist[edgesIn.readInt() - offset];
					MNode1D pt2 = nodelist[edgesIn.readInt() - offset];
					MEdge1D e = new MEdge1D(pt1, pt2, false);
					submesh.getEdges().add(e);
				}
				offset += numberOfNodes;
			}
			nodesIn.close();
			edgesIn.close();
			refsIn.close();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		logger.debug("end reading "+xmlFile);
		return m1d;
	}
}

