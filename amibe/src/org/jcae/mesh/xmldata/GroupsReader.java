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
 * (C) Copyright 2005-2009, by EADS France
 */

package org.jcae.mesh.xmldata;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GroupsReader
{
	private static final Logger LOGGER = Logger.getLogger(GroupsReader.class.getCanonicalName());

	/**
	 * Le texte contenu dans le premier fils. du premier element accessible
	 * depuis l'element racine qui a le tag voulu.
	 * @param racine l'element dont part la recherche
	 * @param tag le tag de l'element dont on veut obtenir le Text contenu
	 */
	private static String getStringByTagName(Element racine, String tag)
	{
		Node element = racine.getElementsByTagName(tag).item(0);
		String result = null;
		Node fils = element.getFirstChild();
		if (fils != null)
		{ // Cas d'un element non vide !
			result = fils.getNodeValue();        
			while (fils.getNextSibling() != null)
			{
				fils = fils.getNextSibling();
				result = result + fils.getNodeValue();
			}
		}
		return result;
	}

	public static Groups getGroups(String xmlPath)
	{
		File xmlFile = new File(xmlPath, "jcae3d");
		Groups groupList = new Groups(xmlPath);
		/* parser le fichier et obtenir un Document */
		try
		{
			Document xmlDoc = XMLHelper.parseXML(xmlFile);
			/* On fait une NodeList des groupes */
			NodeList groups = xmlDoc.getElementsByTagName("group");
			if (groups.getLength() == 0)
			{
				Element triangles = (Element) xmlDoc.getElementsByTagName(
					"triangles").item(1); /* on recupere l'element triangle */
				if(triangles != null)
				{
					Group group = new Group();
					group.setId(0);
					group.setName("default");
					group.setNumberOfElements(Integer.valueOf(
						getStringByTagName(triangles, "number")).intValue());
					group.setOffset(0);
				}
			}
			else
			{
				for (int i = 0; i < groups.getLength(); i++)
				{
					Element elt = (Element) groups.item(i);
					Group group = new Group();
					group.setId(Integer.valueOf(elt.getAttribute("id"))
						.intValue());
                                        if (elt.getElementsByTagName("name").getLength()==0)
                                        {
                                            group.setName("group_" + Integer.toString(group.getId()));
                                        }
                                        else
                                        {
                                            group.setName(getStringByTagName(elt,"name"));
                                        }
					group.setNumberOfElements(Integer.valueOf(
						getStringByTagName(elt, "number")).intValue());
					/*
					 * elt.getElementsByTagName("file") sera une Nodelist
					 * contenant un seul Node
					 */
					Element fichier = (Element) 
						(elt.getElementsByTagName("file").item(0));
					
					String os = fichier.getAttribute("offset");
					if(!os.isEmpty())
						group.setOffset(Integer.parseInt(os));
					/* Puis on ajoute le nouveau groupe a la liste de groupes : */
					groupList.addGroup(group);
                                        //groupList.addGroupReference(group);
				}
			}
		}
		catch (ParserConfigurationException e)
		{
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (SAXException e)
		{
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (IOException e)
		{
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		return groupList;
	}
}
