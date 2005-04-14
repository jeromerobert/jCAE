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

import org.jcae.mesh.cad.CADGeomSurface;
import org.jcae.mesh.cad.CADFace;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import gnu.trove.TIntIntHashMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.apache.xpath.CachedXPathAPI;
import org.apache.log4j.Logger;


public class MeshToMMesh3DConvert extends JCAEXMLData
{
	private static Logger logger=Logger.getLogger(MeshToMMesh3DConvert.class);
	private int nrRefs = 0;
	private int nrIntNodes = 0;
	private int nrTriangles = 0;
	private int offsetBnd = 0;
	private int nodeOffset = 0;
	private TIntIntHashMap xrefs = null;
	private double [] coordRefs = null;
	private DataOutputStream nodesOut, refsOut, normalsOut, trianglesOut, groupsOut;
	private String xmlDir;
	private File xmlFile;
	private File nodesFile, refFile, normalsFile, trianglesFile, groupsFile;
	private Document documentOut;
	private Element groupsElement;
	
	public MeshToMMesh3DConvert (String dir)
	{
		xmlDir = dir;
	}
	
	public void computeRefs(String xmlInFile)
	{
		Document document;
		try
		{
			document = XMLHelper.parseXML(new File(xmlDir, xmlInFile));
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
		CachedXPathAPI xpath = new CachedXPathAPI();
		try
		{
			Node submeshElement = xpath.selectSingleNode(document,
				"/jcae/mesh/submesh");
			Node submeshNodes = xpath.selectSingleNode(submeshElement, "nodes");
			
			int numberOfReferences = Integer.parseInt(
				xpath.selectSingleNode(submeshNodes, "references/number/text()").getNodeValue());
			nrRefs += numberOfReferences;
			int numberOfNodes = Integer.parseInt(
				xpath.selectSingleNode(submeshNodes, "number/text()").getNodeValue());
			nrIntNodes += numberOfNodes - numberOfReferences;
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		logger.debug("Total: "+nrRefs+" references");
	}
	
	public void initialize(String xmlOutFile, boolean writeNormal)
	{
		coordRefs = new double[3*nrRefs];
		xrefs = new TIntIntHashMap(nrRefs);
		
		xmlFile = new File(xmlDir, xmlOutFile);
		File dir = new File(xmlDir, xmlOutFile+".files");
		//create the directory if it does not exist
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
		logger.debug("Append coordinates of "+nrRefs+" nodes");
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
			
			// Write jcae3d
			Element jcaeElement = documentOut.getDocumentElement();
			Element meshElement = documentOut.createElement("mesh");
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

			XMLHelper.writeXML(documentOut, xmlFile);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Convert 2D files to 3D files.
	 * @param xmlInFile The name of the XML file
	 * @param groupId Group number of this 2D mesh
	 * @param F Topological face
	 */
	public void convert(String xmlInFile, int groupId, CADFace F)
	{
		Document documentIn;
		try
		{
			documentIn = XMLHelper.parseXML(new File(xmlDir, xmlInFile));
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
		int i;
		CachedXPathAPI xpath = new CachedXPathAPI();
		CADGeomSurface surface = F.getGeomSurface();
		surface.dinit(1);
		try
		{
			String nodesFile = xpath.selectSingleNode(documentIn,
				"/jcae/mesh/submesh/nodes/file/@location").getNodeValue();
			DataInputStream nodesIn=new DataInputStream(new BufferedInputStream(new FileInputStream(xmlDir+File.separator+nodesFile)));
			String refFile = xpath.selectSingleNode(documentIn,
				"/jcae/mesh/submesh/nodes/references/file/@location").getNodeValue();
			DataInputStream refsIn=new DataInputStream(new BufferedInputStream(new FileInputStream(xmlDir+File.separator+refFile)));
			String trianglesFile = xpath.selectSingleNode(documentIn,
				"/jcae/mesh/submesh/triangles/file/@location").getNodeValue();
			DataInputStream trianglesIn=new DataInputStream(new BufferedInputStream(new FileInputStream(xmlDir+File.separator+trianglesFile)));
			
			Node submeshElement = xpath.selectSingleNode(documentIn,
				"/jcae/mesh/submesh");
			Node submeshNodes = xpath.selectSingleNode(submeshElement, "nodes");
			
			int numberOfReferences = Integer.parseInt(
				xpath.selectSingleNode(submeshNodes, "references/number/text()").getNodeValue());
			int [] refs = new int[numberOfReferences];
			logger.debug("Reading "+numberOfReferences+" references");
			int numberOfNodes = Integer.parseInt(
				xpath.selectSingleNode(submeshNodes, "number/text()").getNodeValue());
			logger.debug("Reading "+numberOfNodes+" nodes");
			double [] normals = new double[3*numberOfNodes];
			//  Interior nodes
			for (i = 0; i < numberOfNodes - numberOfReferences; i++)
			{
				double u = nodesIn.readDouble();
				double v = nodesIn.readDouble();
				double [] p3 = surface.value(u, v);
				for (int j = 0; j < 3; j++)
					nodesOut.writeDouble(p3[j]);
				surface.setParameter(u, v);
				p3 = surface.normal();
				for (int j = 0; j < 3; j++)
					normals[3*i+j] = p3[j];
			}
			//  Boundary nodes
			for (i = 0; i < numberOfReferences; i++)
				refs[i] = refsIn.readInt();
			for (i = 0; i < numberOfReferences; i++)
			{
				double u = nodesIn.readDouble();
				double v = nodesIn.readDouble();
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
			
			Node submeshFaces = xpath.selectSingleNode(submeshElement, "triangles");
			int numberOfFaces = Integer.parseInt(
					xpath.selectSingleNode(submeshFaces, "number/text()").getNodeValue());
			logger.debug("Reading "+numberOfFaces+" faces");
			int ind [] = new int[3];
			for (i=0; i < numberOfFaces; i++)
			{
				for (int j = 0; j < 3; j++)
				{
					// Local node number for this group
					int indLoc = trianglesIn.readInt();
					// Write normals
					if (normalsOut != null)
					{
						if (F.isOrientationForward())
						{
							for (int k = 0; k < 3; k++)
								normalsOut.writeDouble(normals[3*indLoc+k]);
						}
						else
						{
							for (int k = 0; k < 3; k++)
								normalsOut.writeDouble(- normals[3*indLoc+k]);
						}
					}
					// Global node number
					if (indLoc < numberOfNodes - numberOfReferences)
						ind[j] = indLoc + nodeOffset;
					else
						ind[j] = xrefs.get(refs[indLoc - numberOfNodes + numberOfReferences]) + nrIntNodes;
				}
				if (F.isOrientationForward())
				{
					int temp = ind[0];
					ind[0] = ind[2];
					ind[2] = temp;
				}
				for (int j = 0; j < 3; j++)
					trianglesOut.writeInt(ind[j]);
			}
			logger.debug("End reading");
			
			for (i=0; i < numberOfFaces; i++)
				groupsOut.writeInt(i+nrTriangles);
			groupsElement.appendChild(XMLHelper.parseXMLString(documentOut,
				"<group id=\""+(groupId-1)+"\">"+
				"<name>"+groupId+"</name>"+
				"<number>"+numberOfFaces+"</number>"+ 
				"<file format=\"integerstream\" location=\""+
				XMLHelper.canonicalize(xmlDir, groupsFile.toString())+"\""+
				" offset=\""+nrTriangles+"\"/></group>"));
			
			nodeOffset += numberOfNodes - numberOfReferences;
			nrTriangles += numberOfFaces;
			nodesIn.close();
			trianglesIn.close();
			refsIn.close();
			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
}

