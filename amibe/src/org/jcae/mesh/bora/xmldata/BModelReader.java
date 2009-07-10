/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2006, by EADS CRC
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

package org.jcae.mesh.bora.xmldata;


import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.bora.ds.BModel;
import org.jcae.mesh.bora.ds.BSubMesh;
import org.jcae.mesh.bora.ds.Constraint;
import org.jcae.mesh.bora.ds.Hypothesis;
import org.jcae.mesh.xmldata.XMLHelper;
import org.jcae.mesh.cad.CADShapeEnum;

import java.io.File;
import java.util.Iterator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import gnu.trove.TIntObjectHashMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.logging.Logger;

public class BModelReader
{
	private static final Logger LOGGER = Logger.getLogger(BModelReader.class.getName());

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
		// Reset ids
		BModel.reset();
		try
		{
			Document document = XMLHelper.parseXML(new File(xmlDir, xmlFile));
			XPath xpath = XPathFactory.newInstance().newXPath();
			String cadFile = xpath.evaluate("/jcae/model/shape/file/@location", document);
			if(!new File(cadFile).isAbsolute())
				cadFile = xmlDir+File.separator+cadFile;
			model = new BModel(cadFile, xmlDir);
			Node constraints = (Node) xpath.evaluate("/jcae/model/constraints", document, XPathConstants.NODE);
			if (constraints != null)
			{
				NodeList hypList = (NodeList) xpath.evaluate("hypothesis", constraints, XPathConstants.NODESET);
				TIntObjectHashMap<Hypothesis> hypIdMap = new TIntObjectHashMap<Hypothesis>(hypList.getLength());
				for (int i = 0, n = hypList.getLength(); i < n; i++)
				{
					Node hypNode = hypList.item(i);
					int id = Integer.parseInt(xpath.evaluate("@id", hypNode));
					Hypothesis h = new Hypothesis(id);
					hypIdMap.put(id, h);
					String type = xpath.evaluate("element/text()", hypNode);
					String length = xpath.evaluate("length/text()", hypNode);
					String deflection = xpath.evaluate("deflection/text()", hypNode);
					if (type != null && type.length() > 0)
						h.setElement(type);
					if (length != null && length.length() > 0)
						h.setLength(Double.parseDouble(length));
					if (deflection != null && deflection.length() > 0)
						h.setDeflection(Double.parseDouble(deflection));
				}
				NodeList consList = (NodeList) xpath.evaluate("constraint", constraints, XPathConstants.NODESET);
				TIntObjectHashMap<Constraint> consIdMap = new TIntObjectHashMap<Constraint>(consList.getLength());
				for (int i = 0, n = consList.getLength(); i < n; i++)
				{
					Node consNode = consList.item(i);
					int id = Integer.parseInt(xpath.evaluate("@id", consNode));
					int cId = Integer.parseInt(xpath.evaluate("cadId/text()", consNode));
					int hId = Integer.parseInt(xpath.evaluate("hypId/text()", consNode));
					Constraint c = new Constraint(model.getGraph().getById(cId), hypIdMap.get(hId));
					consIdMap.put(id, c);
				}
				NodeList subList = (NodeList) xpath.evaluate("submesh", constraints, XPathConstants.NODESET);
				for (int i = 0, n = subList.getLength(); i < n; i++)
				{
					Node subNode = subList.item(i);
					BSubMesh s = model.newMesh();
					String [] c = subNode.getAttributes().getNamedItem("list").getNodeValue().split(",");
					for (int j = 0; j < c.length; j++)
						s.add(consIdMap.get(Integer.parseInt(c[j])));
				}
//				model.computeConstraints();
			}
			if (!validate)
				return model;
			// Check consistency
			String errmsg = "Error when parsing XML file "+xmlDir+File.separator+xmlFile;
			for (CADShapeEnum cse : CADShapeEnum.iterable(CADShapeEnum.COMPOUND, CADShapeEnum.VERTEX))
			{
				for (Iterator<BCADGraphCell> it = model.getGraph().getRootCell().shapesExplorer(cse); it.hasNext(); )
				{
					BCADGraphCell s = it.next();
					Node id = (Node) xpath.evaluate("/jcae/model/graph/cad[@id="+s.getId()+"]", document, XPathConstants.NODE);
					if (id == null)
					{
						LOGGER.severe(errmsg+", shape id "+s.getId()+" not found");
						return null;
					}
					if (!id.getAttributes().getNamedItem("orientation").getNodeValue().equals(""+s.getOrientation()))
					{
						LOGGER.severe(errmsg+", wrong orientation for shape id "+s.getId()+". Expected result was: orientation=\""+s.getOrientation()+"\"");
						return null;
					}
					Node children = getChild(id, "children");
					Iterator<BCADGraphCell> itc = s.shapesIterator();
					if (itc.hasNext())
					{
						BCADGraphCell c = itc.next();
						StringBuilder sblist = new StringBuilder(""+c.getId());
						while (itc.hasNext())
						{
							c = itc.next();
							sblist.append(","+c.getId());
						}
						String list = sblist.toString();
						if (!children.getAttributes().getNamedItem("list").getNodeValue().equals(list))
						{
							LOGGER.severe(errmsg+", wrong children for shape id "+s.getId()+". Expected result was: list=\""+list+"\"");
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

