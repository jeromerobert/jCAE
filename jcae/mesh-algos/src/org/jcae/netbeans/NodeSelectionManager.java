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
 * (C) Copyright 2008, by EADS France
 */
package org.jcae.netbeans;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.openide.explorer.ExplorerManager;

/**
 *
 * @author Jerome Robert
 */
public class NodeSelectionManager implements PropertyChangeListener
{
	private final Collection<PropertyChangeListener> propertyListeners =
		Collections.synchronizedCollection(new ArrayList<PropertyChangeListener>());
	private static NodeSelectionManager instance = new NodeSelectionManager();
	
	public final void propertyChange(PropertyChangeEvent evt)
	{
		if(ExplorerManager.PROP_SELECTED_NODES.equals(evt.getPropertyName()))
		{
			firePropertyChange(evt);
		}
	}

    public void addPropertyChangeListener(
		PropertyChangeListener listener)
	{
		propertyListeners.add(listener);
	}

	public void removePropertyChangeListener(
		PropertyChangeListener listener)
	{
		propertyListeners.remove(listener);
	}
	
    private void firePropertyChange(PropertyChangeEvent evt)
	{
		for(PropertyChangeListener p: propertyListeners)
			p.propertyChange(evt);
    }
	
	public static NodeSelectionManager getDefault()
	{
		return instance;
	}
}
