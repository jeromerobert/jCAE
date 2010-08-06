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

package org.jcae.netbeans.mesh;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class Mesh 
{
	private double edgeLength=1.0;
	private double deflection=0;
	private String meshFile;
	private String geometryFile;
	private String boraFile;
	
	public Mesh(String name) {
		meshFile=name;
	}
	
	public Mesh()
	{		
	}

	public String getBoraFile() {
		return boraFile;
	}

	public void setBoraFile(String f) {
		boraFile = f;
		propertyChangeSupport.firePropertyChange ("boraFile", null, null);
	}

	public double getDeflection() {
		return deflection;
	}
	public void setDeflection(double deflection) {
		this.deflection = deflection;
		propertyChangeSupport.firePropertyChange ("deflection", null, null);
	}
	public double getEdgeLength() {
		return edgeLength;
	}
	public void setEdgeLength(double edgeLength) {
		this.edgeLength = edgeLength;
		propertyChangeSupport.firePropertyChange ("edgeLength", null, null);
	}
	public String getGeometryFile() {
		return geometryFile;
	}
	public void setGeometryFile(String geometryFile) {
		this.geometryFile = geometryFile;
		propertyChangeSupport.firePropertyChange ("geometryFile", null, null);
	}
	public String getMeshFile() {
		return meshFile;
	}
	public void setMeshFile(String meshFile) {
		this.meshFile = meshFile;
		propertyChangeSupport.firePropertyChange ("meshFile", null, null);
	}	

	private PropertyChangeSupport propertyChangeSupport =  new PropertyChangeSupport(this);


	/**
	 * Notifies the propertyChangesListener that the Mesh object has been changed.
	 */
	public void refresh() {
		propertyChangeSupport.firePropertyChange("", null, null);
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
}
