/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005, by EADS CRC
 
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

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;
import java.io.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 *
 * @author Jerome Robert
 * @todo handle multiple submesh and &lt;label&gt; element.
 */

public class ComputeEdgesConnectivity
{
	private static Logger logger=Logger.getLogger(ComputeEdgesConnectivity.class.getName());
	
	private static class Edge
	{
		private int n1, n2;
		
		Edge(int n1, int n2)
		{
			this.n1=n1;
			this.n2=n2;		
		}
		
		int getN1()
		{
			return n1;
		}
		
		int getN2()
		{
			return n2;
		}
		
		@Override
		public boolean equals(Object object)
		{
			if(object instanceof Edge)
			{
				Edge e=(Edge)object;
				return ((e.n1==n1)&&(e.n2==n2))||((e.n1==n2)&&(e.n2==n1));
			}
			return false;
		}
		
		@Override
		public int hashCode()
		{
			return n1+n2;
		}
		
		@Override
		public String toString()
		{
			return "Edge["+n1+","+n2+"]";
		}
	}
	
	File xmlDir;
	File xmlFile;
	int numberOfTriangles, numberOfFreeEdges, numberOfMultiEdges;
	Document document;
	
	public ComputeEdgesConnectivity(String dir, String file)
	{
		xmlFile = new File(dir, file);
		xmlDir = new File(dir);
	}
	
	public void compute() throws XPathExpressionException, ParserConfigurationException,
		SAXException, IOException
	{
		XPath xpath=XPathFactory.newInstance().newXPath();
		document=XMLHelper.parseXML(xmlFile);
		String formatVersion = xpath.evaluate("/jcae/@version", document);
		if (formatVersion != null && formatVersion.length() > 0)
			throw new RuntimeException("File "+xmlFile+" has been written by a newer version of jCAE and cannot be re-read: ");
		
		String trianglesFileName=(String) xpath.evaluate(
			"/jcae/mesh/submesh/triangles/file/@location", document, XPathConstants.STRING);
		numberOfTriangles=((Double)
			xpath.evaluate("/jcae/mesh/submesh/triangles/number/text()",
				document, XPathConstants.NUMBER)).intValue();				
		File trianglesFile=new File(xmlDir, trianglesFileName);
		File subDir=trianglesFile.getParentFile();
		File freeEdgesFile=new File(subDir, "freeEdges.bin");
		File multiEdgesFile=new File(subDir, "multiEdges.bin");
		
		TObjectIntHashMap<Edge> edges=new TObjectIntHashMap<Edge>();
		DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(trianglesFile)));
		Edge e1, e2, e3;
		int n1, n2, n3;
		for(int i=0; i<numberOfTriangles; i++)
		{
			n1=in.readInt();
			n2=in.readInt();
			n3=in.readInt();
			e1=new Edge(n1,n2);
			e2=new Edge(n2,n3);
			e3=new Edge(n3,n1);
			edges.put(e1, edges.get(e1)+1);
			edges.put(e2, edges.get(e2)+1);
			edges.put(e3, edges.get(e3)+1);
		}
		logger.fine("Number of imported edges: "+edges.size());		
		
		in.close();
		
		TObjectIntIterator<Edge> it=edges.iterator();		
		DataOutputStream outFree=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(freeEdgesFile)));
		DataOutputStream outMulti=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(multiEdgesFile)));
		DataOutputStream current;
		
		while(it.hasNext())
		{					
			it.advance();			
			if( it.value() < 2 )
			{
				current=outFree;
				numberOfFreeEdges++;
			}
			else if( it.value() > 2 )
			{	
				current=outMulti;
				numberOfMultiEdges++;
			}
			else current=null;
			
			if(current!=null)
			{
				Edge e=it.key();
				current.writeInt(e.getN1());
				current.writeInt(e.getN2());
			}					
		}
		
		logger.info("Number of free edges: "+numberOfFreeEdges);
		logger.info("Number of multiple edges: "+numberOfMultiEdges);
		
		outFree.close();
		outMulti.close();
		
		Element eOldNode=(Element)xpath.evaluate(
			"/jcae/mesh/submesh/nodes", document, XPathConstants.NODE);
		
		Element meshElement=(Element)xpath.evaluate(
			"/jcae/mesh", document, XPathConstants.NODE);
		
		Element freeMeshElement=XMLHelper.parseXMLString(document,
			"<submesh><flag value=\"FreeEdges\"/></submesh>");
		
		freeMeshElement.appendChild(eOldNode.cloneNode(true));				
		meshElement.appendChild(freeMeshElement);
				
		String s="<beams>";
		s+="<number>"+numberOfFreeEdges+"</number>";
		s+="<file format=\"integerstream\" location=\""+XMLHelper.canonicalize(xmlDir.toString(), freeEdgesFile.toString())+"\"/>";
		s+="</beams>";
		Element beamElement=XMLHelper.parseXMLString(document, s);
		freeMeshElement.appendChild(beamElement);
		
		Element multiMeshElement=XMLHelper.parseXMLString(document,
			"<submesh><flag value=\"MultiEdges\"/></submesh>");
		
		multiMeshElement.appendChild(eOldNode.cloneNode(true));				
		meshElement.appendChild(multiMeshElement);
				
		s="<beams>";
		s+="<number>"+numberOfMultiEdges+"</number>";
		s+="<file format=\"integerstream\" location=\""+XMLHelper.canonicalize(xmlDir.toString(), multiEdgesFile.toString())+"\"/>";
		s+="</beams>";
		beamElement=XMLHelper.parseXMLString(document, s);
		multiMeshElement.appendChild(beamElement);
		
		XMLHelper.writeXML(document, xmlFile);
	}
	
}
