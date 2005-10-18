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
 * (C) Copyright 2004, by EADS CRC
 */

package org.jcae.netbeans.mesh;

/* import */
import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.jcae.mesh.xmldata.XMLHelper;
import org.openide.ErrorManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

//import org.jcae.mesh.xmldata;
public class GroupsReader
{
	/**
	 * Le texte contenu dans le premier fils. du premier élément accessible
	 * depuis l'élément racine qui a le tag voulu.
	 * @param racine l'élément dont part la recherche
	 * @param tag le tag de l'élément dont on veut obtenir le Text contenu
	 */
	public static String getStringByTagName(Element racine, String tag)
	{
		Node element = racine.getElementsByTagName(tag).item(0);
		String result = null;
		Node fils = element.getFirstChild();
		if (fils != null)
		{ // Cas d'un élément non vide !
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
		File xmlFile = new File(xmlPath);		
		Groups groupList = new Groups();
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
				Group group = new Group();
				group.setId(0);
				group.setName("default");
				group.setNumberOfElements(Integer.valueOf(
					getStringByTagName(triangles, "number")).intValue());
				group.setOffset(0);
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
					group.setOffset(Integer.valueOf(
						fichier.getAttribute("offset")).intValue());
					/* Puis on ajoute le nouveau groupe a la liste de groupes : */
					groupList.addGroup(group);
                                        //groupList.addGroupReference(group);
				}
			}
		}
		catch (ParserConfigurationException e)
		{
			ErrorManager.getDefault().notify(e);			
		}
		catch (SAXException e)
		{
			ErrorManager.getDefault().notify(e);
		}
		catch (IOException e)
		{
			ErrorManager.getDefault().notify(e);
		}
		return groupList;
	}
}