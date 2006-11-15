/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2006, by EADS CRC
 
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

package org.jcae.mesh.bora.xmldata;

import org.jcae.mesh.bora.ds.*;
import org.jcae.mesh.cad.*;
import org.jcae.mesh.xmldata.*;
import java.io.File;
import java.util.Iterator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.apache.log4j.Logger;

public class BModelReader
{
	private static Logger logger=Logger.getLogger(BModelReader.class);

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
	/**
	 * Write the current object to a XML file and binary files. The XML file
	 * have links to the binary files.
	 * @param xmlFile The name of the XML file
	 */	
	public static BModel readObject(String xmlDir)
	{
		return readObject(xmlDir, "model", true);
	}
	public static BModel readObject(String xmlDir, String xmlFile)
	{
		return readObject(xmlDir, xmlFile, true);
	}
	public static BModel readObject(String xmlDir, String xmlFile, boolean validate)
	{
		BModel model = null;
		try
		{
			Document document = XMLHelper.parseXML(new File(xmlDir, xmlFile));
			XPath xpath = XPathFactory.newInstance().newXPath();
			String cadFile = xpath.evaluate("/jcae/model/shape/file/@location", document);
			if(!new File(cadFile).isAbsolute())
				cadFile = xmlDir+File.separator+cadFile;
			model = new BModel(cadFile, xmlDir);
			if (!validate)
				return model;
			// Check consistency
			String errmsg = "Error when parsing XML file "+xmlDir+File.separator+xmlFile;
			CADShapeBuilder factory = CADShapeBuilder.factory;
			CADExplorer exp = factory.newExplorer();
			for (int t = BCADGraph.classTypeArray.length - 1; t >= 0; t--)
			{
				for (Iterator it = model.getGraph().getRootCell().shapesExplorer(t); it.hasNext(); )
				{
					BCADGraphCell s = (BCADGraphCell) it.next();
					Node id = (Node) xpath.evaluate("/jcae/model/graph/cad[@id="+s.getId()+"]", document, XPathConstants.NODE);
					if (id == null)
					{
						logger.error(errmsg+", shape id "+s.getId()+" not found");
						return null;
					}
					if (!id.getAttributes().getNamedItem("orientation").getNodeValue().equals(""+s.getOrientation()))
					{
						logger.error(errmsg+", wrong orientation for shape id "+s.getId()+". Expected result was: orientation=\""+s.getOrientation()+"\"");
						return null;
					}
					Node children = getChild(id, "children");
					Iterator itc = s.shapesIterator();
					if (itc.hasNext())
					{
						BCADGraphCell c = (BCADGraphCell) itc.next();
						String list = ""+c.getId();
						while (itc.hasNext())
						{
							c = (BCADGraphCell) itc.next();
							list += ","+c.getId();
						}
						if (!children.getAttributes().getNamedItem("list").getNodeValue().equals(list))
						{
							logger.error(errmsg+", wrong children for shape id "+s.getId()+". Expected result was: list=\""+list+"\"");
							return null;
						}
					}
				}
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		return model;
	}
}

