/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005, by EADS CRC
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

import java.io.IOException;
import java.util.logging.Level;
import org.jcae.mesh.cad.CADExplorer;
import org.jcae.mesh.cad.CADFace;
import org.jcae.mesh.cad.CADGeomSurface;
import org.jcae.mesh.cad.CADShape;
import org.jcae.mesh.cad.CADShapeEnum;
import org.jcae.mesh.cad.CADShapeFactory;
import java.io.File;
import java.io.FileNotFoundException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import java.util.logging.Logger;


public class MeshToMMesh3DConvert implements FilterInterface, JCAEXMLData
{
	private static final Logger LOGGER=Logger.getLogger(MeshToMMesh3DConvert.class.getName());
	private int nrRefs = 0;
	private int nrIntNodes = 0;
	private int nrTriangles = 0;
	private int offsetBnd = 0;
	private int nodeOffset = 0;
	private TIntIntHashMap xrefs;
	private final TIntObjectHashMap<CADFace> mapFaces = new TIntObjectHashMap<CADFace>();
	private double [] coordRefs = null;
	private final String xmlDir;
	private final String brepFile;
	private UNVGenericWriter unvWriter;
	private AmibeWriter.Dim3 amibeWriter;
	private boolean writeNormal;
	
	public MeshToMMesh3DConvert(String dir, String bFile, CADShape shape)
	{
		xmlDir = dir;
		brepFile = bFile;
		int iFace = 0;
		CADExplorer expF = CADShapeFactory.getFactory().newExplorer();
		for (expF.init(shape, CADShapeEnum.FACE); expF.more(); expF.next())
		{
			iFace++;
			mapFaces.put(iFace, (CADFace) expF.current());
		}
	}
	
	public final void exportUNV(boolean b, String unvName)
	{
		if (b)
			unvWriter = new UNVGenericWriter(unvName);
	}
	
	public final void collectBoundaryNodes(int[] faces)
	{
		for (int iFace : faces)
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
				continue;
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
			LOGGER.fine("Total: "+nrRefs+" references");
		}
	}
	
	public final void beforeProcessingAllShapes(boolean writeNormal)
	{
		try {
			coordRefs = new double[3 * nrRefs];
			xrefs = new TIntIntHashMap(nrRefs);
			amibeWriter = new AmibeWriter.Dim3(xmlDir, writeNormal);
			amibeWriter.setShape(brepFile);
			this.writeNormal = writeNormal;
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
		}
	}
	
	public final void afterProcessingAllShapes()
	{
		//  Stores coordinates of boundary nodes
		//  Set nrRefs to its final value after elimination
		//  of duplicates
		nrRefs = offsetBnd;
		int nrNodes = nrIntNodes + nrRefs;
		LOGGER.fine("Append coordinates of "+nrRefs+" nodes");
		try
		{
			for (int i = 0; i < 3*nrRefs; i+=3)
				amibeWriter.addNode(coordRefs[i], coordRefs[i+1], coordRefs[i+2]);
			if (unvWriter != null)
				unvWriter.finish(nrRefs, nrIntNodes, nrTriangles, coordRefs);

			amibeWriter.finish();
			LOGGER.info("Total number of nodes: "+nrNodes);
			LOGGER.info("Total number of triangles: "+nrTriangles);			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Convert 2D files to 3D files.
	 * @param groupId group number of this 2D mesh
	 * @param groupName group name
	 * @param iFace face number
	 */
	public final void processOneShape(int groupId, String groupName, int iFace)
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
		CADFace F = mapFaces.get(iFace);
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
			PrimitiveFileReaderFactory pfrf = new PrimitiveFileReaderFactory();
			DoubleFileReader dfrN = pfrf.getDoubleReader(new File(xmlDir, nodesFileInput));

			String refFileInput = xpath.evaluate(
				"/jcae/mesh/submesh/nodes/references/file/@location",
				documentIn);
			IntFileReader ifrR = pfrf.getIntReader(new File(xmlDir, refFileInput));

			String trianglesFileInput = xpath.evaluate(
				"/jcae/mesh/submesh/triangles/file/@location", documentIn);
			IntFileReader ifrT = pfrf.getIntReader(new File(xmlDir, trianglesFileInput));
			
			Node submeshElement = (Node) xpath.evaluate("/jcae/mesh/submesh",
				documentIn, XPathConstants.NODE);
			Node submeshNodes = (Node) xpath.evaluate("nodes", submeshElement,
				XPathConstants.NODE);
			
			int numberOfReferences = Integer.parseInt(
				xpath.evaluate("references/number/text()", submeshNodes));
			int [] refs = new int[numberOfReferences];
			LOGGER.fine("Reading "+numberOfReferences+" references");
			int numberOfNodes = Integer.parseInt(
				xpath.evaluate("number/text()", submeshNodes));
			LOGGER.fine("Reading "+numberOfNodes+" nodes");
			double [] normals = new double[3*numberOfNodes];
			//  Interior nodes
			for (int i = 0; i < numberOfNodes - numberOfReferences; i++)
			{
				double u = dfrN.get();
				double v = dfrN.get();
				double [] p3 = surface.value(u, v);
				amibeWriter.addNode(p3);
				if (unvWriter != null)
					unvWriter.writeNode(i+nodeOffset+1, p3);
				surface.setParameter(u, v);
				p3 = surface.normal();
				System.arraycopy(p3, 0, normals, 3 * i, 3);
			}
			//  Boundary nodes
			ifrR.get(refs);
			for (int i = 0; i < numberOfReferences; i++)
			{
				double u = dfrN.get();
				double v = dfrN.get();
				surface.setParameter(u, v);
				double [] p3 = surface.normal();
				System.arraycopy(p3, 0, normals, 3 * (i + numberOfNodes - numberOfReferences), 3);
				if (!xrefs.contains(refs[i]))
				{
					p3 = surface.value(u, v);
					xrefs.put(refs[i], offsetBnd);
					System.arraycopy(p3, 0, coordRefs, 3 * offsetBnd, 3);
					offsetBnd++;
					amibeWriter.addNodeRef(refs[i]);
				}
			}
			
			Node submeshFaces = (Node) xpath.evaluate("triangles",
				submeshElement, XPathConstants.NODE);
			int numberOfFaces = Integer.parseInt(xpath.evaluate(
				"number/text()", submeshFaces));
			LOGGER.fine("Reading "+numberOfFaces+" faces");
			int ind [] = new int[4];
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
						ind[j+1] = indLoc[j] + nodeOffset;
					else
						ind[j+1] = xrefs.get(refs[indLoc[j] - numberOfNodes + numberOfReferences]) + nrIntNodes;
				}
				if (ind[1] == ind[2] || ind[2] == ind[3] || ind[3] == ind[1])
				{
					LOGGER.fine("Triangle bound from a degenerated edge skipped");
					continue;
				}
				if (writeNormal)
				{
					for (int j = 0; j < 3; j++)
					{
						int u = 3*indLoc[j];
						// Write normals
						if (F.isOrientationForward())
							amibeWriter.addNormal(normals[u], normals[u+1], normals[u+2]);
						else
							amibeWriter.addNormal(-normals[u], -normals[u+1], -normals[u+2]);
					}
				}
				if (F.isOrientationForward())
				{
					int temp = ind[1];
					ind[1] = ind[2];
					ind[2] = temp;
				}

				amibeWriter.addTriangle(ind[1], ind[2], ind[3]);
				if (unvWriter != null)
				{
					ind[0] = 3;
					for (int j = 1; j < 4; j++)
						ind[j]++;
					unvWriter.writeElement(cntTriangles+nrTriangles+1, ind);
				}
				cntTriangles++;
			}
			LOGGER.fine("End reading");
			
			amibeWriter.nextGroup(groupName);
			for (int i=0; i < cntTriangles; i++)
				amibeWriter.addTriaToGroup(i+nrTriangles);

			if (unvWriter != null)
			{
				int [] ids = new int[cntTriangles];
				for (int i = 0; i < cntTriangles; i++)
					ids[i] = i + nrTriangles + 1;
				unvWriter.writeGroup(groupId, groupName, ids);
			}
			
			nodeOffset += numberOfNodes - numberOfReferences;
			nrTriangles += cntTriangles;
			ifrT.close();
			dfrN.close();
			ifrR.close();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
}

