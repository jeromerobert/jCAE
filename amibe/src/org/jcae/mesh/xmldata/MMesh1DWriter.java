/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005, by EADS CRC
    Copyright (C) 2007, by EADS France
 
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

import org.jcae.mesh.amibe.ds.MEdge1D;
import org.jcae.mesh.amibe.ds.MMesh1D;
import org.jcae.mesh.amibe.ds.MNode1D;
import org.jcae.mesh.amibe.ds.SubMesh1D;
import org.jcae.mesh.cad.CADShape;
import org.jcae.mesh.cad.CADShapeFactory;
import org.jcae.mesh.cad.CADShapeEnum;
import org.jcae.mesh.cad.CADEdge;
import org.jcae.mesh.cad.CADExplorer;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.util.Iterator;
import java.util.HashSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.apache.log4j.Logger;


public class MMesh1DWriter
{
	private static Logger logger=Logger.getLogger(MMesh1DWriter.class);
	
	/**
	 * Used by {@link writeObject}
	 */
	private static Element writeObjectNodes(Document document, Iterator<MNode1D> nodesIterator, File nodesFile, File refFile, String baseDir, MMesh1D m1d)
		throws IOException
	{
		//save nodes
		logger.debug("begin writing "+nodesFile);
		DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(nodesFile, true)));
		DataOutputStream refout=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(refFile, true)));
		long offsetNodes = nodesFile.length();
		long offsetRefNodes = refFile.length();
		int i=0, nref=0;
		while(nodesIterator.hasNext())
		{
			MNode1D n=nodesIterator.next();
			out.writeDouble(n.getParameter());
			if (null != n.getCADVertex())
			{
				refout.writeInt(n.getLabel());
				refout.writeInt(m1d.getIndexGeometricalVertex(n.getCADVertex()));
				nref++;
			}
			i++;
		}
		out.close();
		refout.close();
		
		Element nodes=document.createElement("nodes");
		Element number=document.createElement("number");
		Element file=document.createElement("file");
		Element references=document.createElement("references");
		Element refNumber=document.createElement("number");
		Element reffile=document.createElement("file");		


		reffile.setAttribute("format", "integerstream");
		reffile.setAttribute("location", XMLHelper.canonicalize(baseDir, refFile.toString()));
		reffile.setAttribute("offset", ""+offsetRefNodes);
		refNumber.appendChild(document.createTextNode(""+nref));
		references.appendChild(refNumber);
		references.appendChild(reffile);
		file.setAttribute("format", "doublestream");
		file.setAttribute("location", XMLHelper.canonicalize(baseDir, nodesFile.toString()));
		file.setAttribute("offset", ""+offsetNodes);
		number.appendChild(document.createTextNode(""+i));
		nodes.appendChild(number);
		nodes.appendChild(file);
		nodes.appendChild(references);
		
		logger.debug("end writing "+nodesFile);
		return nodes;
/*		// Append elements to <nodes>
		return XMLHelper.parseXMLString(document, "<nodes>"+
			"<number>"+i+"</number>"+
			"<file format=\"doublestream\" location=\""+XMLHelper.canonicalize(baseDir, nodesFile.toString())+"\" offset=\""+offsetNodes+"\"/>"+
			"<references>"+
			"<number>"+nref+"</number>"+
			"<file format=\"integerstream\" location=\""+XMLHelper.canonicalize(baseDir, refFile.toString())+"\" offset=\""+offsetRefNodes+"\"/>"+
			"</references></nodes>");*/
	}
	
	/**
	 * Used by {@link writeObject}
	 */
	private static Element writeObjectBeams(Document document, Iterator<MEdge1D> edgesIterator, File beamsFile, String baseDir)
		throws IOException
	{
		//save beams
		logger.debug("begin writing "+beamsFile);
		DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(beamsFile, true)));
		long offsetBeams = beamsFile.length();
		int i=0;
		while(edgesIterator.hasNext())
		{
			MEdge1D e=edgesIterator.next();
			i++;
			MNode1D pt1 = e.getNodes1();
			MNode1D pt2 = e.getNodes2();
			out.writeInt(pt1.getLabel());
			out.writeInt(pt2.getLabel());
		}
		out.close();
		logger.debug("end writing "+beamsFile);
		
		Element beams=document.createElement("beams");
		Element number=document.createElement("number");
		Element file=document.createElement("file");
		number.appendChild(document.createTextNode(""+i));
		file.setAttribute("format", "integerstream");
		file.setAttribute("location", XMLHelper.canonicalize(baseDir, beamsFile.toString()));
		file.setAttribute("offset", ""+offsetBeams);
		beams.appendChild(number);
		beams.appendChild(file);
		return beams;
		/*return XMLHelper.parseXMLString(document, "<beams>"+
			"<number>"+i+"</number>"+
			"<file format=\"integerstream\" location=\""+XMLHelper.canonicalize(baseDir, beamsFile.toString())+"\" offset=\""+offsetBeams+"\"/>"+
			"</beams>");*/
	}

	/**
	 * Write the current object to a XML file and binary files. The XML file
	 * have links to the binary files.
	 * @param xmlDir       name of the XML file
	 * @param xmlFilename  basename of the main XML file
	 * @param brepFile     brep file
	 */
	public static void writeObject(MMesh1D m1d, String xmlDir, String xmlFilename, String brepFile)
	{
		//  Compute node labels
		m1d.updateNodeLabels();
		try
		{
			File xmlFile = new File(xmlDir, xmlFilename);
			File dir = new File(xmlDir, xmlFilename+".files");

			//create the output directory if it does not exist
			if(!dir.exists())
				dir.mkdirs();
		
			File nodesFile = new File(dir, JCAEXMLData.nodes1dFilename);
			if(nodesFile.exists())
				nodesFile.delete();
			File refFile = new File(dir, JCAEXMLData.ref1dFilename);
			if(refFile.exists())
				refFile.delete();
			File beamsFile=new File(dir, JCAEXMLData.beams1dFilename);
			if(beamsFile.exists())
				beamsFile.delete();
			
			CADShape shape = m1d.getGeometry();
			
			// Create and fill the DOM
			Document document=JCAEXMLWriter.createJcaeDocument();
			
			Element jcaeElement=document.getDocumentElement();
			Element meshElement=document.createElement("mesh");
			Element shapeElement=XMLHelper.parseXMLString(document, "<shape>"+
				"<file format=\"brep\" location=\""+brepFile+"\"/>"+"</shape>");
			meshElement.appendChild(shapeElement);
			
			int iEdge = 0;
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
				// Create <submesh> element
				Element subMeshElement=document.createElement("submesh");
				Element subshapeElement=document.createElement("subshape");
				subshapeElement.appendChild(document.createTextNode(""+iEdge));
				subMeshElement.appendChild(subshapeElement);
				
				// Create <nodes>
				subMeshElement.appendChild(writeObjectNodes(document, submesh.getNodesIterator(), nodesFile, refFile, xmlDir, m1d));
				// Create <beams>
				subMeshElement.appendChild(writeObjectBeams(document, submesh.getEdgesIterator(), beamsFile, xmlDir));
				meshElement.appendChild(subMeshElement);
			}
			jcaeElement.appendChild(meshElement);
				
			// save the DOM to file
			XMLHelper.writeXML(document, xmlFile);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}

