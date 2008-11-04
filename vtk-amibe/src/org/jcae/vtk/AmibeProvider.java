/*
 * Project Info:  http://jcae.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2005, by EADS CRC
 * (C) Copyright 2007-2008, by EADS France
 */

package org.jcae.vtk;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * @author Jerome Robert, Julian Ibarz
 */
public class AmibeProvider
{	
	private final File directory;
	private final Document document;
	
	public static Document parseXML(File file)
		throws ParserConfigurationException, SAXException, IOException
	{
		DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
		DocumentBuilder builder=factory.newDocumentBuilder();
		
		builder.setEntityResolver(new ClassPathEntityResolver());
		Document document=builder.parse(file);
		document.normalize();
		
		return document;
	}
	
	/**
	 * @param directory The directory containing the jcae3d file
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public AmibeProvider(File directory) throws ParserConfigurationException, SAXException, IOException
	{
		this.directory=directory;
		File jcae3d = new File(directory, "jcae3d");
		document = parseXML(jcae3d);
	}
	
	public Document getDocument()
	{
		return document;
	}
	
	public File getDirectory()
	{
		return directory;
	}
}
