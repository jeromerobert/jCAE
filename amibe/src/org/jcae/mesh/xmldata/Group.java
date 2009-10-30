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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Group
{
	private static final Logger LOGGER = Logger.getLogger(Group.class.getCanonicalName());
	private int id;
	private String name;
	private int number;
	private int offset;
    private final PropertyChangeSupport propertyChangeSupport =  new PropertyChangeSupport(this);
    private boolean visible;   
	private boolean selected;
    
	public void setVisible(boolean visible)
	{
		propertyChangeSupport.firePropertyChange("visible",
			Boolean.valueOf(this.visible),
			Boolean.valueOf(visible));
		this.visible = visible;
	}
	
	/**
	 * @return Returns the id of the group
	 */
	public int getId()
	{
		return id;
	}

	/**
	 * @return Returns the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @return Returns the number of triangles in the group
	 */
	public int getNumberOfElements()
	{
		return number;
	}

	/**
	 * @param newId The new id to set
	 */
	public void setId(int newId)
	{
		this.id = newId;
	}

	/**
	 * @param newName The new name to set
	 */
	public void setName(String newName)
	{
		String oldName=name;
		this.name = newName;
		propertyChangeSupport.firePropertyChange("name", oldName, newName);
	}

	/**
	 * @param newNumber The new number to set
	 */
	public void setNumberOfElements(int newNumber)
	{
		this.number = newNumber;
	}

	/**
	 * @param newOffset The new offset to set
	 */
	void setOffset(int newOffset)
	{
		this.offset = newOffset;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return name;
	}

	int getOffset()
	{
		return offset;
	}

	/**
	 * test
	 */
	public void writeXMLName(String xmlPath)
	{
		java.io.File f = new java.io.File(xmlPath);
		org.w3c.dom.Document xmlDoc = null;
		if (f.exists())
		{
			try
			{
				xmlDoc = org.jcae.mesh.xmldata.XMLHelper.parseXML(f);
				org.w3c.dom.NodeList groups = xmlDoc
					.getElementsByTagName("group");
				org.w3c.dom.Element group = null;
				if (groups.getLength() > 0)
				{
					boolean found = false;
					int i = 0;
					while (!found && i < groups.getLength())
					{
						group = (org.w3c.dom.Element) groups.item(i);
						int idGroup = Integer.valueOf(group.getAttribute("id"))
							.intValue();
						if (idGroup == id)
						{
							found = true;
						}
						i++;
					}
				}
				org.w3c.dom.Element eltName = org.jcae.mesh.xmldata.XMLHelper
					.parseXMLString(xmlDoc, "<name>" + name + "</name>");
				group.appendChild(eltName);
				org.jcae.mesh.xmldata.XMLHelper.writeXML(xmlDoc, f);				
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		} else
		{
			LOGGER.warning("Mesh (" + f.getPath() + ") not found.");
		}
	}

	/**
	 * It creates a DOM element from the document of jcae3d.xml corresponding to this group.
	 *
	 *@param xmlDoc the xml DOM document which comes from the parse of jcae3d.xml.
	 *@param groupFile the path of the file groups.bin from baseDir.
	 *@param baseDir the directory which contains jcae3d.xml.
	 *@return the DOM element corresponding to the group.
	 */
	public Element createXMLGroup(Document xmlDoc, java.io.File groupFile,
		String baseDir)
	{
		Element newElt = null;
		try
		{
			newElt = org.jcae.mesh.xmldata.XMLHelper.parseXMLString(xmlDoc,
				"<group id=\"" + id + "\">"
				+ "<name>" + name + "</name>" 
				+ "<number>" + number + "</number>"
				+ "<file format=\"integerstream\" location=\""
				+ XMLHelper.canonicalize(baseDir, groupFile.toString())
				+ "\" offset=\"" + offset + "\"/>" + "</group>");
		}
		catch (Exception ex)
		{
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
		}
		return newElt;
	}
	
    /**
     * Adds a PropertyChangeListener to the listener list.
     * @param l The listener to add.
     */
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        propertyChangeSupport.addPropertyChangeListener(l);
    }
    
    /**
     * Removes a PropertyChangeListener from the listener list.
     * @param l The listener to remove.
     */
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        propertyChangeSupport.removePropertyChangeListener(l);
    }
    
	public void setSelected(boolean selected)
	{
		propertyChangeSupport.firePropertyChange("selected",
			Boolean.valueOf(this.selected),
			Boolean.valueOf(selected));
		this.selected = selected;
	}
}
