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
 * (C) Copyright 2007, by EADS France
 */

package org.jcae.viewer3d;

import java.util.Map;
import javax.media.j3d.Bounds;
import javax.media.j3d.Node;
import javax.media.j3d.PickInfo;

/**
 * This class represents viewable object of a View
 * @author Jerome Robert
 */
public interface Viewable
{
	/** 
	 * @param domainId The ids (Integer) of domains that changed or null to
	 * specify that even ids may have change
	 */
	void domainsChanged(int[] domainId);
	
	/**
	 * Return the domain provider of this viewable
	 * @return
	 */
	DomainProvider getDomainProvider();
	
	/** @param map map domain id (Integer) to visible state (Boolean) */
	void setDomainVisible(Map<Integer, Boolean> map);
	
	/** Return the Java3D Node associated to this Viewable */
	Node getJ3DNode();
	
	void pick(PickViewable result);
	
	/**
	 * Called when an area is picked
	 * @param result the result of the picking
	 * @param bound the volume used to do the picking
	 */
	void pickArea(PickInfo[] result, Bounds bound);
	
	void unselectAll();
	
	void addSelectionListener(SelectionListener listener);
	void removeSelectionListener(SelectionListener listener);
}
