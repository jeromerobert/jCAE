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
import org.jcae.mesh.xmldata.*;
import org.jcae.mesh.cad.CADShapeEnum;
import java.io.File;
import java.util.Iterator;
import java.util.Collection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.apache.log4j.Logger;

public class BModelWriter
{
	private static Logger logger=Logger.getLogger(BModelWriter.class);

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
			logger.debug("Writing file "+file);

			// Create and fill the DOM
			Document document=JCAEXMLWriter.createJcaeBoraDocument();
			
			Element jcaeElement=document.getDocumentElement();
			Element modelElement=document.createElement("model");
			Element shapeElement=document.createElement("shape");
			Element fileElement=document.createElement("file");
			fileElement.setAttribute("location", model.getCADFile());
			shapeElement.appendChild(fileElement);
			modelElement.appendChild(shapeElement);

			Collection allConstraints = model.getConstraints();
			if (allConstraints != null && allConstraints.size() > 0)
			{
				Element constraintsElement=document.createElement("constraints");
				for (Iterator it = allConstraints.iterator(); it.hasNext(); )
				{
					Constraint c = (Constraint) it.next();
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
				for (Iterator it = allConstraints.iterator(); it.hasNext(); )
				{
					Constraint c = (Constraint) it.next();
					constraintsElement.appendChild(
						XMLHelper.parseXMLString(document, "<constraint id=\""+c.getId()+"\">"+
							"<cadId>"+c.getGraphCell().getId()+"</cadId>"+
							"<hypId>"+c.getHypothesis().getId()+"</hypId></constraint>"));
				}
				for (Iterator it = model.getSubMeshIterator(); it.hasNext(); )
				{
					BSubMesh s = (BSubMesh) it.next();
					Iterator itc = s.getConstraints().iterator();
					if (itc.hasNext())
					{
						Constraint c = (Constraint) itc.next();
						StringBuffer sblist = new StringBuffer(""+c.getId());
						while (itc.hasNext())
						{
							c = (Constraint) itc.next();
							sblist.append(","+c.getId());
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
			for (Iterator itcse = CADShapeEnum.iterator(CADShapeEnum.COMPOUND, CADShapeEnum.VERTEX); itcse.hasNext(); )
			{
				CADShapeEnum cse = (CADShapeEnum) itcse.next();
				for (Iterator it = model.getGraph().getCellList(cse).iterator(); it.hasNext(); )
				{
					BCADGraphCell s = (BCADGraphCell) it.next();
					Element elt = document.createElement("cad");
					elt.setAttribute("id", ""+s.getId());
					elt.setAttribute("type", cse.toString());
					elt.setAttribute("orientation", ""+s.getShape().orientation());
					if (s.getReversed() != null)
						elt.setAttribute("reversed", ""+s.getReversed().getId());
					Iterator itc = s.shapesIterator();
					if (itc.hasNext())
					{
						BCADGraphCell c = (BCADGraphCell) itc.next();
						StringBuffer sblist = new StringBuffer(""+c.getId());
						while (itc.hasNext())
						{
							c = (BCADGraphCell) itc.next();
							sblist.append(","+c.getId());
						}
						Element child = document.createElement("children");
						child.setAttribute("list", sblist.toString());
						elt.appendChild(child);
					}
					if (s.getParents().size() > 0)
					{
						StringBuffer sblist = new StringBuffer();
						boolean first = true;
						for (Iterator itp = s.getParents().iterator(); itp.hasNext(); )
						{
							BCADGraphCell p = (BCADGraphCell) itp.next();
							if (!first)
								sblist.append(",");
							first = false;
							sblist.append(p.getId());
						}
						Element parent = document.createElement("parents");
						parent.setAttribute("list", ""+sblist.toString());
						elt.appendChild(parent);
					}
					Iterator itp = s.discretizationIterator();
					if (itp.hasNext())
					{
						StringBuffer sblist = new StringBuffer();
						boolean first = true;
						while (itp.hasNext())
						{
							BDiscretization d = (BDiscretization) itp.next();
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

