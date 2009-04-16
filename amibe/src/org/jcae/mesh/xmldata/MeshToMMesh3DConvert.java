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

import org.jcae.mesh.cad.CADGeomSurface;
import org.jcae.mesh.cad.CADFace;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.nio.DoubleBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import gnu.trove.TIntIntHashMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import java.util.logging.Logger;


public class MeshToMMesh3DConvert extends JCAEXMLData
{
	private static Logger logger=Logger.getLogger(MeshToMMesh3DConvert.class.getName());
	private int nrRefs = 0;
	private int nrIntNodes = 0;
	private int nrTriangles = 0;
	private int offsetBnd = 0;
	private int nodeOffset = 0;
	private TIntIntHashMap xrefs = null;
	private double [] coordRefs = null;
	private DataOutputStream nodesOut, refsOut, normalsOut, trianglesOut, groupsOut;
	private String xmlDir;
	private String brepFile;
	private File xmlFile3d;
	private File nodesFile, refFile, normalsFile, trianglesFile, groupsFile;
	private Document documentOut;
	private Element groupsElement;
	private MeshToUNVConvert unv = null;
	
	public MeshToMMesh3DConvert (String dir, String bFile)
	{
		xmlDir = dir;
		brepFile = bFile;
	}
	
	public void exportUNV(boolean b, String unvName)
	{
		if (b)
			unv = new MeshToUNVConvert(unvName);
	}
	
	public void computeRefs(int iFace)
	{
		Document document;
		File xmlFile2d = null;
		try
		{
			xmlFile2d = new File(xmlDir, JCAEXMLData.xml2dFilename+iFace);
			document = XMLHelper.parseXML(xmlFile2d);
		}
		catch(FileNotFoundException ex)
		{
			return;
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		XPath xpath = XPathFactory.newInstance().newXPath();
		try
		{
			String formatVersion = xpath.evaluate("/jcae/@version", document);
			if (formatVersion != null && formatVersion.length() > 0)
				throw new RuntimeException("File "+xmlFile2d+" has been written by a newer version of jCAE and cannot be re-read");
			Node submeshElement = (Node) xpath.evaluate("/jcae/mesh/submesh",
				document, XPathConstants.NODE);
			Node submeshNodes = (Node) xpath.evaluate("nodes", submeshElement,
				XPathConstants.NODE);
			
			int numberOfReferences = Integer.parseInt(
				xpath.evaluate("references/number/text()", submeshNodes));
			nrRefs += numberOfReferences;
			int numberOfNodes = Integer.parseInt(
				xpath.evaluate("number/text()", submeshNodes));
			nrIntNodes += numberOfNodes - numberOfReferences;
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		logger.fine("Total: "+nrRefs+" references");
	}
	
	public void initialize(boolean writeNormal)
	{
		coordRefs = new double[3*nrRefs];
		xrefs = new TIntIntHashMap(nrRefs);
		
		xmlFile3d = new File(xmlDir, JCAEXMLData.xml3dFilename);
		File dir = new File(xmlDir, JCAEXMLData.xml3dFilename+".files");
		//create the directory if it does not exiswriteInit(PrintStream arg0)t
		if(!dir.exists())
			dir.mkdirs();
		
		nodesFile = new File(dir, JCAEXMLData.nodes3dFilename);
		refFile = new File(dir, JCAEXMLData.ref1dFilename);
		normalsFile = new File(dir, JCAEXMLData.normals3dFilename);
		trianglesFile = new File(dir, JCAEXMLData.triangles3dFilename);
		groupsFile = new File(dir, JCAEXMLData.groupsFilename);
		
		try
		{
			documentOut = JCAEXMLWriter.createJcaeDocument();
			groupsElement = documentOut.createElement("groups");
		
			nodesOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(nodesFile)));
			refsOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(refFile, true)));
			if (writeNormal)
				normalsOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(normalsFile)));
			trianglesOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(trianglesFile)));
			groupsOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(groupsFile)));
		}
		catch(FileNotFoundException ex)
		{
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	
	public void finish()
	{
		//  Stores coordinates of boundary nodes
		//  Set nrRefs to its final value after elimination
		//  of duplicates
		nrRefs = offsetBnd;
		int nrNodes = nrIntNodes + nrRefs;
		logger.fine("Append coordinates of "+nrRefs+" nodes");
		try
		{
			for (int i = 0; i < 3*nrRefs; i++)
				nodesOut.writeDouble(coordRefs[i]);
			nodesOut.close();
			refsOut.close();
			if (normalsOut != null)
				normalsOut.close();
			trianglesOut.close();
			groupsOut.close();
			if (unv != null)
				unv.finish(nrRefs, nrIntNodes, nrTriangles, coordRefs);
			
			// Write 3d files
			Element jcaeElement = documentOut.getDocumentElement();
			Element meshElement = documentOut.createElement("mesh");
			Element shapeElement=XMLHelper.parseXMLString(documentOut, "<shape>"+
				"<file format=\"brep\" location=\""+brepFile+"\"/>"+"</shape>");
			meshElement.appendChild(shapeElement);
			Element subMeshElement = documentOut.createElement("submesh");
			Element nodesElement = XMLHelper.parseXMLString(documentOut,
				"<nodes>"+
				"<number>"+nrNodes+"</number>"+
				"<file format=\"doublestream\" location=\""+XMLHelper.canonicalize(xmlDir, nodesFile.toString())+"\"/>"+
				"<references>"+
				"<number>"+nrRefs+"</number>"+
				"<file format=\"integerstream\" location=\""+XMLHelper.canonicalize(xmlDir, refFile.toString())+"\"/>"+
				"</references>"+
				"</nodes>");
			subMeshElement.appendChild(nodesElement);
			Element trianglesElement = XMLHelper.parseXMLString(documentOut,
				"<triangles>"+
				"<number>"+nrTriangles+"</number>"+
				"<file format=\"integerstream\" location=\""+XMLHelper.canonicalize(xmlDir, trianglesFile.toString())+"\"/>"+
				"<normals>"+
				"<file format=\"doublestream\" location=\""+XMLHelper.canonicalize(xmlDir, normalsFile.toString())+"\"/>"+
				"</normals>"+
				"</triangles>");
			subMeshElement.appendChild(trianglesElement);
			subMeshElement.appendChild(groupsElement);
			
			meshElement.appendChild(subMeshElement);
			jcaeElement.appendChild(meshElement);

			XMLHelper.writeXML(documentOut, xmlFile3d);
			logger.info("Total number of nodes: "+nrNodes);
			logger.info("Total number of triangles: "+nrTriangles);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Convert 2D files to 3D files.
	 * @param groupId Group number of this 2D mesh
	 * @param F Topological face
	 */
	public void convert(int groupId, CADFace F)
	{
		Document documentIn;
		File xmlFile2d = null;
		try
		{
			xmlFile2d = new File(xmlDir, JCAEXMLData.xml2dFilename+groupId);
			documentIn = XMLHelper.parseXML(xmlFile2d);
		}
		catch(FileNotFoundException ex)
		{
			return;
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		XPath xpath = XPathFactory.newInstance().newXPath();
		CADGeomSurface surface = F.getGeomSurface();
		surface.dinit(1);
		try
		{
			String formatVersion = xpath.evaluate("/jcae/@version", documentIn);
			if (formatVersion != null && formatVersion.length() > 0)
				throw new RuntimeException("File "+xmlFile2d+" has been written by a newer version of jCAE and cannot be re-read");
			String nodesFileInput = xpath.evaluate(
				"/jcae/mesh/submesh/nodes/file/@location", documentIn);
			FileChannel fcN = new FileInputStream(xmlDir+File.separator+nodesFileInput).getChannel();
			MappedByteBuffer bbN = fcN.map(FileChannel.MapMode.READ_ONLY, 0L, fcN.size());
			DoubleBuffer nodesBuffer = bbN.asDoubleBuffer();

			String refFileInput = xpath.evaluate(
				"/jcae/mesh/submesh/nodes/references/file/@location",
				documentIn);
			IntFileReader ifrR = new IntFileReaderByDirectBuffer(new File(xmlDir, refFileInput));

			String trianglesFileInput = xpath.evaluate(
				"/jcae/mesh/submesh/triangles/file/@location", documentIn);
			IntFileReader ifrT = new IntFileReaderByDirectBuffer(new File(xmlDir, trianglesFileInput));
			
			Node submeshElement = (Node) xpath.evaluate("/jcae/mesh/submesh",
				documentIn, XPathConstants.NODE);
			Node submeshNodes = (Node) xpath.evaluate("nodes", submeshElement,
				XPathConstants.NODE);
			
			int numberOfReferences = Integer.parseInt(
				xpath.evaluate("references/number/text()", submeshNodes));
			int [] refs = new int[numberOfReferences];
			logger.fine("Reading "+numberOfReferences+" references");
			int numberOfNodes = Integer.parseInt(
				xpath.evaluate("number/text()", submeshNodes));
			logger.fine("Reading "+numberOfNodes+" nodes");
			double [] normals = new double[3*numberOfNodes];
			//  Interior nodes
			for (int i = 0; i < numberOfNodes - numberOfReferences; i++)
			{
				double u = nodesBuffer.get();
				double v = nodesBuffer.get();
				double [] p3 = surface.value(u, v);
				for (int j = 0; j < 3; j++)
					nodesOut.writeDouble(p3[j]);
				if (unv != null)
					unv.writeNode(i+nodeOffset, p3);
				surface.setParameter(u, v);
				p3 = surface.normal();
				for (int j = 0; j < 3; j++)
					normals[3*i+j] = p3[j];
			}
			//  Boundary nodes
			ifrR.get(refs);
			for (int i = 0; i < numberOfReferences; i++)
			{
				double u = nodesBuffer.get();
				double v = nodesBuffer.get();
				surface.setParameter(u, v);
				double [] p3 = surface.normal();
				for (int j = 0; j < 3; j++)
					normals[3*(i+numberOfNodes-numberOfReferences)+j] = p3[j];
				if (!xrefs.contains(refs[i]))
				{
					p3 = surface.value(u, v);
					xrefs.put(refs[i], offsetBnd);
					for (int j = 0; j < 3; j++)
						coordRefs[3*offsetBnd+j] = p3[j];
					offsetBnd++;
					refsOut.writeInt(refs[i]);
				}
			}
			
			Node submeshFaces = (Node) xpath.evaluate("triangles",
				submeshElement, XPathConstants.NODE);
			int numberOfFaces = Integer.parseInt(xpath.evaluate(
				"number/text()", submeshFaces));
			logger.fine("Reading "+numberOfFaces+" faces");
			int ind [] = new int[3];
			int indLoc [] = new int[3];
			int cntTriangles = 0;
			for (int i = 0; i < numberOfFaces; i++)
			{
				for (int j = 0; j < 3; j++)
				{
					// Local node number for this group
					indLoc[j] = ifrT.get();
				}
				if (indLoc[0] < 0 || indLoc[1] < 0 || indLoc[2] < 0)
				{
					// Skip outer triangles
					continue;
				}
				for (int j = 0; j < 3; j++)
				{
					// Global node number
					if (indLoc[j] < numberOfNodes - numberOfReferences)
						ind[j] = indLoc[j] + nodeOffset;
					else
						ind[j] = xrefs.get(refs[indLoc[j] - numberOfNodes + numberOfReferences]) + nrIntNodes;
				}
				if (ind[0] == ind[1] || ind[1] == ind[2] || ind[2] == ind[0])
				{
					logger.fine("Triangle bound from a degenerated edge skipped");
					continue;
				}
				if (normalsOut != null)
				{
					for (int j = 0; j < 3; j++)
					{
						// Write normals
						if (F.isOrientationForward())
						{
							for (int k = 0; k < 3; k++)
								normalsOut.writeDouble(normals[3*indLoc[j]+k]);
						}
						else
						{
							for (int k = 0; k < 3; k++)
								normalsOut.writeDouble(- normals[3*indLoc[j]+k]);
						}
					}
				}
				if (F.isOrientationForward())
				{
					int temp = ind[0];
					ind[0] = ind[2];
					ind[2] = temp;
				}
				for (int j = 0; j < 3; j++)
					trianglesOut.writeInt(ind[j]);
				if (unv != null)
					unv.writeTriangle(cntTriangles+nrTriangles, ind);
				cntTriangles++;
			}
			logger.fine("End reading");
			
			for (int i=0; i < cntTriangles; i++)
				groupsOut.writeInt(i+nrTriangles);
			if (unv != null)
				unv.writeGroup(""+groupId, nrTriangles, cntTriangles);
			groupsElement.appendChild(XMLHelper.parseXMLString(documentOut,
				"<group id=\""+(groupId-1)+"\">"+
				"<name>"+groupId+"</name>"+
				"<number>"+cntTriangles+"</number>"+ 
				"<file format=\"integerstream\" location=\""+
				XMLHelper.canonicalize(xmlDir, groupsFile.toString())+"\""+
				" offset=\""+nrTriangles+"\"/></group>"));
			
			nodeOffset += numberOfNodes - numberOfReferences;
			nrTriangles += cntTriangles;
			ifrT.close();
			fcN.close();
			MeshExporter.clean(bbN);
			ifrR.close();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
}

