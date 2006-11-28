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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.DOMImplementation;


public class JCAEXMLWriter
{
	/** 
	 * Creates a jcae XML document.
	 */	
	public static Document createJcaeDocument()
		throws ParserConfigurationException
	{
		DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
		factory.setValidating(true);
		DocumentBuilder builder=factory.newDocumentBuilder();
		DOMImplementation domImpl=builder.getDOMImplementation();
		DocumentType docType=domImpl.createDocumentType("jcae", null,
			"classpath:///org/jcae/mesh/xmldata/jcae.dtd");
		return domImpl.createDocument(null, "jcae", docType);
	}

	/** 
	 * Creates a jcae XML document.
	 */	
	public static Document createJcaeBoraDocument()
		throws ParserConfigurationException
	{
		DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
		factory.setValidating(true);
		DocumentBuilder builder=factory.newDocumentBuilder();
		DOMImplementation domImpl=builder.getDOMImplementation();
		DocumentType docType=domImpl.createDocumentType("jcae", null,
			"classpath:///org/jcae/mesh/xmldata/jcaebora.dtd");
		return domImpl.createDocument(null, "jcae", docType);
	}
}
