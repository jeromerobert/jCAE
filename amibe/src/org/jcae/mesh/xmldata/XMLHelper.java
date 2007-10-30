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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

package org.jcae.mesh.xmldata;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.io.StringReader;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.util.ArrayList;
import java.util.ListIterator;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/** Some methods to help using DOM */
public class XMLHelper
{	
	/** Read an XML file with DTD validation. Use ClassPathEntityResolver to solve URI.
	 * Return a normalized Document
	 */	
	public static Document parseXML(File file)
		throws ParserConfigurationException, SAXException, IOException
	{
		DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
		factory.setValidating(true);
		DocumentBuilder builder=factory.newDocumentBuilder();
		builder.setEntityResolver(new ClassPathEntityResolver());
		Document document=builder.parse(file);
		document.normalize();
		return document;
	}
	
	/** Parse a valid xml string and return the Element representing this string. */	
	public static Element parseXMLString(Document document, String string)
		throws ParserConfigurationException, SAXException, IOException
	{
		DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
		DocumentBuilder builder=factory.newDocumentBuilder();
		Document subDoc=builder.parse(new InputSource(new StringReader(string)));
		Element e=subDoc.getDocumentElement();		
		return (Element)document.importNode(e, true);
	}
	
	/** Write a DOM to a file. */
	public static void writeXML(Document document, File file)
		throws IOException, FileNotFoundException, TransformerConfigurationException, TransformerException
	{
		// save the DOM to file
		StreamResult result = new StreamResult(new BufferedOutputStream(new FileOutputStream(file)));
		TransformerFactory transFactory = TransformerFactory.newInstance();
		Transformer transformer = transFactory.newTransformer();
		transformer.setOutputProperty("indent", "yes");
		// hack from http://java.sun.com/xml/jaxp/dist/1.1/docs/tutorial/xslt/2_write.html
		// to keep DOCTYPE field			
		transformer.setOutputProperty(javax.xml.transform.OutputKeys.DOCTYPE_SYSTEM, document.getDoctype().getSystemId());
		transformer.transform(new DOMSource(document), result);
		result.getOutputStream().close();
	}

	private static final String listToString(ArrayList<String> pathSpec)
	{
		if (pathSpec.isEmpty())
			return "";
		StringBuilder ret = new StringBuilder();
		ret.append(pathSpec.get(0));
		for (int i = 1, n = pathSpec.size(); i < n; i++)
			ret.append(File.separator+pathSpec.get(i));
		return ret.toString();
	}

	/** Removes useless path components.  */
	public static String canonicalize(String path)
	{
		String pattern=File.separator;
		if(pattern.equals("\\"))
			pattern="\\\\";
		
		String [] splitted = path.split(pattern);
		ArrayList<String> pathSpec = new ArrayList<String>(splitted.length);
		for (int i = 0; i < splitted.length; i++)
			pathSpec.add(splitted[i]);
		// Warning: these steps must be performed in this exact order!
		// Step 1: Remove empty paths
		for (ListIterator<String> it = pathSpec.listIterator(); it.hasNext(); )
		{
			String c = it.next();
			if (c.length() == 0 && it.previousIndex() > 0)
				it.remove();
		}
		// Step 2: Remove all occurrences of "."
		for (ListIterator<String> it = pathSpec.listIterator(); it.hasNext(); )
		{
			String c = it.next();
			if (c.equals("."))
				it.remove();
		}
		// Step 3: Remove all occurrences of "foo/.."
		for (ListIterator<String> it = pathSpec.listIterator(); it.hasNext(); )
		{
			String c = it.next();
			if (c.equals("..") && it.previousIndex() > 0)
			{
				if (it.previousIndex() == 1 && pathSpec.get(0).length() == 0)
				{
					// "/.." is replaced by "/"
					it.remove();
				}
				else if (!pathSpec.get(it.previousIndex() - 1).equals(".."))
				{
					it.remove();
					it.previous();
					it.remove();
				}
			}
		}
		return listToString(pathSpec);
	}
	
	/** Removes useless path components.  */
	public static String canonicalize(String dir, String file)
	{
		if (dir != null && dir.length() > 0)
		{
			if (file.startsWith(dir+File.separator))
				file = file.substring(dir.length()+1);
		}
		return canonicalize(file);
	}
}
