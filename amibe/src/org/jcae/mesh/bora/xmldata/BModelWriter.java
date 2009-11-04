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
import org.jcae.mesh.bora.ds.BDiscretization;
import org.jcae.mesh.bora.ds.BModel;
import org.jcae.mesh.bora.ds.BSubMesh;
import org.jcae.mesh.bora.ds.Constraint;
import org.jcae.mesh.bora.ds.Hypothesis;
import org.jcae.mesh.xmldata.JCAEXMLWriter;
import org.jcae.mesh.xmldata.XMLHelper;
import org.jcae.mesh.cad.CADShapeEnum;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.io.File;
import java.util.Iterator;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BModelWriter
{
	private static final Logger LOGGER = Logger.getLogger(BModelWriter.class.getName());

	/**
	 * Write the current object to a XML file and binary files. The XML file
	 * have links to the binary files.
	 * @param model  The model to store on disk
	 */	
	public static void writeObject(BModel model)
	{
		try
		{
			File file = new File(model.getOutputDir(), model.getOutputFile());
			if (LOGGER.isLoggable(Level.FINE))
				LOGGER.log(Level.FINE, "Writing file "+file);

			// Create and fill the DOM
			Document document=JCAEXMLWriter.createJcaeBoraDocument();
			
			Element jcaeElement=document.getDocumentElement();
			Element modelElement=document.createElement("model");
			Element shapeElement=document.createElement("shape");
			Element fileElement=document.createElement("file");
			fileElement.setAttribute("location", model.getCADFile());
			shapeElement.appendChild(fileElement);
			modelElement.appendChild(shapeElement);

			Collection<Constraint> allConstraints = model.getConstraints();
			if (allConstraints != null && !allConstraints.isEmpty())
			{
				Element constraintsElement=document.createElement("constraints");
				for (Constraint c : allConstraints)
				{
					Hypothesis h = c.getHypothesis();
					Element hypElement=document.createElement("hypothesis");
					hypElement.setAttribute("id", ""+h.getId());
					if (h.getElement() != null)
					{
						Element elt = document.createElement("element");
						elt.appendChild(document.createTextNode(""+h.getElement()));
						hypElement.appendChild(elt);
					}
					if (h.getLength() >= 0)
					{
						Element elt = document.createElement("length");
						elt.appendChild(document.createTextNode(""+h.getLength()));
						hypElement.appendChild(elt);
					}
					if (h.getDeflection() >= 0)
					{
						Element elt = document.createElement("deflection");
						elt.appendChild(document.createTextNode(""+h.getDeflection()));
						hypElement.appendChild(elt);
					}
					constraintsElement.appendChild(hypElement);
				}
				for (Constraint c : allConstraints)
				{
					String xmlString = "<constraint id=\""+c.getId()+"\">"+
							"<cadId>"+c.getGraphCell().getId()+"</cadId>"+
							"<hypId>"+c.getHypothesis().getId()+"</hypId>";
					if (c.getGroup() != null)
						xmlString +="<group>" + c.getGroup() + "</group>";
					xmlString +="</constraint>";
					
					constraintsElement.appendChild(	
							XMLHelper.parseXMLString(document, xmlString));
				}
				for (BSubMesh s : model.getSubMeshes())
				{
					if (!s.getConstraints().isEmpty())
					{
						StringBuilder sblist = new StringBuilder();
						boolean first = true;
						for (Constraint c : s.getConstraints())
						{
							if (!first)
								sblist.append(",");
							first = false;
							sblist.append(c.getId());
						}
						Element subElement=document.createElement("submesh");
						subElement.setAttribute("id", ""+s.getId());
						subElement.setAttribute("list", sblist.toString());
						constraintsElement.appendChild(subElement);
					}
				}
				modelElement.appendChild(constraintsElement);
			}
			Element graphElement=document.createElement("graph");
			for (CADShapeEnum cse : CADShapeEnum.iterable(CADShapeEnum.COMPOUND, CADShapeEnum.VERTEX))
			{
				for (BCADGraphCell s : model.getGraph().getCellList(cse))
				{
					Element elt = document.createElement("cad");
					elt.setAttribute("id", ""+s.getId());
					elt.setAttribute("type", cse.toString());
					elt.setAttribute("orientation", ""+s.getShape().orientation());
					if (s.getReversed() != null)
						elt.setAttribute("reversed", ""+s.getReversed().getId());
					Iterator<BCADGraphCell> itc = s.shapesIterator();
					if (itc.hasNext())
					{
						BCADGraphCell c = itc.next();
						StringBuilder sblist = new StringBuilder();
						sblist.append(c.getId());
						while (itc.hasNext())
						{
							c = itc.next();
							sblist.append(",").append(c.getId());
						}
						Element child = document.createElement("children");
						child.setAttribute("list", sblist.toString());
						elt.appendChild(child);
					}
					if (!s.getParents().isEmpty())
					{
						StringBuilder sblist = new StringBuilder();
						boolean first = true;
						for (BCADGraphCell p : s.getParents())
						{
							if (!first)
								sblist.append(",");
							first = false;
							sblist.append(p.getId());
						}
						Element parent = document.createElement("parents");
						parent.setAttribute("list", ""+sblist.toString());
						elt.appendChild(parent);
					}
					if (!s.getDiscretizations().isEmpty())
					{
						StringBuilder sblist = new StringBuilder();
						boolean first = true;
						for (BDiscretization d : s.getDiscretizations())
						{
							if (!first)
								sblist.append(",");
							first = false;
							sblist.append(d.getId());
						}
						Element discr = document.createElement("discretization");
						discr.setAttribute("list", ""+sblist.toString());
						elt.appendChild(discr);
					}
					graphElement.appendChild(elt);
				}
			}
			modelElement.appendChild(graphElement);
			jcaeElement.appendChild(modelElement);			
			XMLHelper.writeXML(document, file);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}

