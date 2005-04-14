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
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

package org.jcae.mesh.xmldata;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.io.StringReader;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
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
	{
		try
		{
			DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
			DocumentBuilder builder=factory.newDocumentBuilder();
			Document subDoc=builder.parse(new InputSource(new StringReader(string)));
			Element e=subDoc.getDocumentElement();		
			return (Element)document.importNode(e, true);
		} catch(ParserConfigurationException ex)
		{
			ex.printStackTrace();			
		}
		catch(SAXException ex)
		{
			ex.printStackTrace();
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
		return null;
	}
	
	/** Write a DOM to a file. */
	public static void writeXML(Document document, File file)
		throws FileNotFoundException, TransformerConfigurationException, TransformerException
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
	}
	
	/** Removes useless path components.  */
	public static String canonicalize(String path)
	{
		String pattern=File.separator;
		if(pattern.equals("\\"))
			pattern="\\\\";
		
		String [] components = path.split(pattern);
		while (true)
		{
			String [] newpath = new String[components.length];
			int j = 0;
			boolean redo = false;
			for (int i = 0; i < components.length; i++)
			{
				if (components[i].equals("..") && i+2 < components.length && !components[i+1].equals(".."))
				{
					i++;
					redo = true;
				}
				else if (components[i].equals("."))
					redo = true;
				else
				{
					newpath[j] = components[i];
					j++;
				}
			}
	
			if (j == 0)
				return "";
			components = new String[j];
			System.arraycopy(newpath, 0, components, 0, j);
			if (!redo)
				break;
		}
		StringBuffer ret = new StringBuffer();
		if (components[0].length() > 0)
			ret.append(components[0]);
		for (int i = 1; i < components.length; i++)
	 		if (components[i].length() > 0)
				ret.append(File.separator+components[i]);
		return ret.toString();
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
