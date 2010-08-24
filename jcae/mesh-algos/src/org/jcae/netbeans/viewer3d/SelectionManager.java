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

package org.jcae.netbeans.viewer3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import org.jcae.vtk.View;
import org.jcae.vtk.Viewable;

/**
 * This singleton manage the connection between the netbeans tree and the
 * viewables and control the appending mode.
 * Because an entity (mesh or CAD for example) can be represented by many
 * viewables in many views we have to link the different viewable with the
 * entity. It's the role of the map&lt;Viewable,Object&gt;. And the real link
 * between the netbeans tree and the viewables is managed by an EntitySelection.
 * See CADSelection for an example. So if the viewable does not have to be
 * linked with the tree you can not create a EntitySelection for your type of
 * entity. For example the OEMM viewable is not connected with the tree and no
 * OEMMSelection is needed, only adding the viewable in the SelectionManager is
 * needed.
 * @author Julian Ibarz
 */
public class SelectionManager {
	private HashMap<Object,EntitySelection> selections = new HashMap<Object,EntitySelection>();
	private HashMap<Viewable,Object> interactors = new HashMap<Viewable,Object>();
	private static SelectionManager instance;
	private boolean appendSelection;
	private boolean disableListeningProperty;
	private Collection<SelectionFactory> selectionFactories =
		new ArrayList<SelectionFactory>();
	
	private SelectionManager()
	{
		
	}
	
	/**
	 * 	 Permit to the Selection instances to know if a change selection on explorers
	 * is due to Selection instance or to the user.
	 */
	public boolean isAppendSelection()
	{
		return appendSelection;
	}

	public boolean isDisableListeningProperty()
	{
		return disableListeningProperty;
	}

	public void setDisableListeningProperty(boolean disableListeningProperty)
	{
		this.disableListeningProperty = disableListeningProperty;
	}
	
	
	
	/**
	 * Call this method if you want the management of the append selection.
	 * This method unselect all the items if it's necessary.
	 */
	public void prepareSelection()
	{
		if(!appendSelection)
		{
			unselectAll();
		}
	}

	public void setAppendSelection(boolean appendSelection)
	{
		this.appendSelection = appendSelection;
		
		for(View view : ViewManager.getDefault().getAllView())
		{
			view.setAppendSelection(appendSelection);
		}
	}
	
	static public SelectionManager getDefault()
	{
		if(instance == null)
			instance = new SelectionManager();
		
		return instance;
	}

	public interface SelectionFactory
	{
		EntitySelection create(Object entity);
		/**
		 * As create may return null (like for ViewableOEMM) we need to tell if
		 * this factory can create objects for the required configuration
		 */
		boolean canCreate(Viewable viewable, Object entity);
	}

	public void addSelectionFactory(SelectionFactory sf)
	{
		selectionFactories.add(sf);
	}
	
	public void addInteractor(Viewable interactor, Object entity)
	{
		interactors.put(interactor, entity);
		interactor.setAppendSelection(appendSelection);
		
		// If we don't have the EntitySelection create it
		if(selections.get(entity) == null)
		{
			boolean found = false;
			for(SelectionFactory s:selectionFactories)
			{
				if(s.canCreate(interactor, entity))
				{
					selections.put(entity, s.create(entity));
					found = true;
					break;
				}
			}
			if(!found)
				throw new IllegalArgumentException("Cannot add "+entity+" to "+interactor);
		}
	}
	
	public void removeInteractor(Viewable interactor)
	{
		Object entity = interactors.get(interactor);
		interactors.remove(interactor);
		selections.remove(entity);
	}
	
	public Object getEntity(Viewable interactor)
	{
		return interactors.get(interactor);
	}
	
	public EntitySelection getEntitySelection(Object entity)
	{
		return selections.get(entity);
	}
	
	public void unselectAll()
	{
		for(EntitySelection entitySelection : selections.values())
		{
			if (entitySelection != null)
				entitySelection.unselectAll();
		}
	}
}
