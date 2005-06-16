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
 */

package org.jcae.viewer3d.fe.amibe;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.jcae.viewer3d.Domain;
import org.jcae.viewer3d.Palette;
import org.jcae.viewer3d.fe.FEDomainAdapter;
import org.jcae.viewer3d.fe.FEProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A FEProvider which get data from the XML/binaries file of the amibe mesher
 * @author Jerome Robert
 * @todo implements, add constructors...
 */
public class AmibeProvider implements FEProvider
{	
	private static Palette palette=new Palette(Integer.MAX_VALUE);
	private File directory;
	private Document document;
	private int[] groupsID=new int[0];
	private long lastUpdateTime;
	private File jcae3d;
	public static Document parseXML(File file)
		throws ParserConfigurationException, SAXException, IOException
	{
		DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
		//factory.setValidating(true);
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
		jcae3d = new File(directory, "jcae3d");
		load();
	}
	
	private void load() throws ParserConfigurationException, SAXException, IOException
	{				
		lastUpdateTime=jcae3d.lastModified();
		document = parseXML(jcae3d);
		Element xmlGroups = (Element) document.getElementsByTagName(
			"groups").item(0);
		NodeList nodeList=xmlGroups.getElementsByTagName("group");
		groupsID=new int[nodeList.getLength()];
		for(int i=0; i<groupsID.length; i++)
		{
			Element e=(Element) nodeList.item(i);
			groupsID[i]=Integer.parseInt(e.getAttribute("id"));
		}
	}
	/* (non-Javadoc)
	 * @see jcae.viewer3d.DomainProvider#getDomainIDs()
	 */
	public int[] getDomainIDs()
	{
		try
		{
			if(jcae3d.lastModified()!=lastUpdateTime)
				load();
		}catch(ParserConfigurationException ex)
		{
			ex.printStackTrace();
		} catch (SAXException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{ 
			e.printStackTrace();
		}
		return groupsID;
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.DomainProvider#getDomain(int)
	 */
	public Domain getDomain(int id)
	{
		try
		{
			return new AmibeDomain(directory, document, id, palette.getColor(id));
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return new FEDomainAdapter();
		}
	}
}
