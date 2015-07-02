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
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class Group
{
	private static final Logger LOGGER = Logger.getLogger(Group.class.getCanonicalName());
	private String name;
	private int number;
	private int offset;
	private int beamNumber;
	private int beamOffset;
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
	 * @return Returns the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @return Returns the number of triangles in the group
	 */
	public final int getNumberOfElements()
	{
		return number;
	}

	/**
	 * @param newName The new name to set
	 */
	public final void setName(String newName)
	{
		String oldName=name;
		this.name = newName;
		propertyChangeSupport.firePropertyChange("name", oldName, newName);
	}

	/**
	 * @param newNumber The new number to set
	 */
	public final void setNumberOfElements(int newNumber)
	{
		this.number = newNumber;
	}

	/**
	 * @param newOffset The new offset to set
	 */
	final void setOffset(int newOffset)
	{
		this.offset = newOffset;
	}

	public int getBeamNumber() {
		return beamNumber;
	}

	public void setBeamNumber(int beamNumber) {
		this.beamNumber = beamNumber;
	}

	public int getBeamOffset() {
		return beamOffset;
	}

	public void setBeamOffset(int beamOffset) {
		this.beamOffset = beamOffset;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString()
	{
		return name;
	}

	final int getOffset()
	{
		return offset;
	}

	/**
	 * It creates a DOM element from the document of jcae3d.xml corresponding to this group.
	 *
	 *@param xmlDoc the xml DOM document which comes from the parse of jcae3d.xml.
	 *@param groupFile the path of the file groups.bin from baseDir.
	 *@param baseDir the directory which contains jcae3d.xml.
	 *@return the DOM element corresponding to the group.
	 */
	public final Element createXMLGroup(Document xmlDoc)
	{
		Element newElt = null;
		try
		{
			String bg = "";
			if(beamNumber > 0) {
				bg ="<beams><number>" + beamNumber + "</number>"
					+ "<file format='integerstream' location='jcae3d.files/bgroups.bin' offset='"
					+ beamOffset + "'/></beams>";
			}

			String tg = "";
			if(number > 0) {
				tg = "<number>" + number + "</number>"
					+ "<file format='integerstream' location='jcae3d.files/groups.bin' offset='"
					+ offset + "'/>";
			}

			newElt = org.jcae.mesh.xmldata.XMLHelper.parseXMLString(xmlDoc,
				"<group><name>" + name + "</name>" + tg + bg + "</group>");
		} catch (ParserConfigurationException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		} catch (SAXException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		return newElt;
	}
	
    /**
     * Adds a PropertyChangeListener to the listener list.
     * @param l The listener to add.
     */
    public final void addPropertyChangeListener(PropertyChangeListener l)
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
