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

package org.jcae.mesh.algos;

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.log4j.Logger;
import org.apache.xpath.XPathAPI;
import org.xml.sax.SAXException;
import org.jcae.mesh.xmldata.XMLHelper;
import org.w3c.dom.*;

/**
 *
 * @author Jerome Robert
 * @todo handle multiple submesh and <label> element.
 */

public class ComputeEdgesConnectivity
{
	private static Logger logger=Logger.getLogger(ComputeEdgesConnectivity.class);
	
	private class Edge
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
		
		public boolean equals(Object object)
		{
			if(object instanceof Edge)
			{
				Edge e=(Edge)object;
				return ((e.n1==n1)&&(e.n2==n2))||((e.n1==n2)&&(e.n2==n1));
			}
			else return false;
		}
		
		public int hashCode()
		{
			return n1+n2;
		}
		
		public String toString()
		{
			return "Edge["+n1+","+n2+"]";
		}
	}
	
	File xmlDir;
	File xmlFile;
	String brepDir;
	int numberOfTriangles, numberOfFreeEdges, numberOfMultiEdges;
	Document document;
	
	public ComputeEdgesConnectivity(String dir, String file, String b)
	{
		xmlFile = new File(dir, file);
		xmlDir = new File(dir);
		brepDir = b;
	}
	
	public void compute()
		throws ParserConfigurationException, IOException, TransformerException,
		SAXException, URISyntaxException
	{
		document=XMLHelper.parseXML(xmlFile);
		
		String trianglesFileName=XPathAPI.selectSingleNode(document,
			"/jcae/mesh/submesh/triangles/file/@location").getNodeValue();
		Text trianglesNumberText=(Text)XPathAPI.selectSingleNode(document,
			"/jcae/mesh/submesh/triangles/number/text()");
		numberOfTriangles=Integer.parseInt(trianglesNumberText.getData());				
		File trianglesFile=new File(xmlDir, trianglesFileName);
		File subDir=trianglesFile.getParentFile();
		File freeEdgesFile=new File(subDir, "freeEdges.bin");
		File multiEdgesFile=new File(subDir, "multiEdges.bin");
		
		TObjectIntHashMap edges=new TObjectIntHashMap();
		DataInputStream in = new DataInputStream(new FileInputStream(trianglesFile));	
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
		logger.debug("Number of imported edges: "+edges.size());		
		
		in.close();
		
		TObjectIntIterator it=edges.iterator();		
		DataOutputStream outFree=new DataOutputStream(new FileOutputStream(freeEdgesFile));
		DataOutputStream outMulti=new DataOutputStream(new FileOutputStream(multiEdgesFile));
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
				Edge e=(Edge)it.key();
				current.writeInt(e.getN1());
				current.writeInt(e.getN2());
			}					
		}
		
		logger.info("Number of free edges: "+numberOfFreeEdges);
		logger.info("Number of multiple edges: "+numberOfMultiEdges);
		
		outFree.close();
		outMulti.close();
		
		Element eOldNode=(Element)XPathAPI.selectSingleNode(document,
			"/jcae/mesh/submesh/nodes");
		
		Element meshElement=(Element)XPathAPI.selectSingleNode(document,"/jcae/mesh");
		
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
