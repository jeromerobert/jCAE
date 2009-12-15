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
package org.jcae.vtk;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import vtk.vtkMapper;
import vtk.vtkActor;

/**
 * TODO : the beams are stored in the polydata with the scalar UNVProvider.OTHERS_GROUP wich can make a bug of color. It can make a bug for the selection in jcae
 * because this group doesn't really exist. Find an example of .unv to check this feature.
 * @author Julian Ibarz
 */
public class AmibeViewable extends Viewable
{
	private ViewMode viewMode = ViewMode.WIRED;
	private Map<String, LeafNode> groupIDToNode = new HashMap<String, LeafNode>();
	private Map<LeafNode, String> groupNodeToID = new HashMap<LeafNode, String>();
	private Collection<String> groupsLoaded;
	/**
	 * It explain how the mesh is displayed :
	 * _ FILLED means the mesh is not in wired mode but the selection remains filled ;
	 * _ WIRED means the mesh is wired.
	 */
	public enum ViewMode
	{
		FILLED,
		WIRED
	}

	public AmibeViewable(Mesh mesh)
	{
		this(mesh, new Palette(Integer.MAX_VALUE));
	}
	
	public AmibeViewable(Mesh mesh, Palette palette)
	{		
		rootNode.setSelectionActorCustomiser(new SelectionActorCustomiser()
		{

			@Override
			public void customiseSelectionActor(vtkActor actor)
			{
				super.customiseSelectionActor(actor);
				actor.GetProperty().EdgeVisibilityOn();
				actor.GetProperty().LightingOff();
				actor.GetProperty().SetEdgeColor(0.4, 0.4, 0.4);
			}
		});
		rootNode.setMapperCustomiser(new AbstractNode.MapperCustomiser() {

			@Override
			public void customiseMapper(vtkMapper mapper)
			{
				mapper.SetResolveCoincidentTopologyToPolygonOffset();
				mapper.SetResolveCoincidentTopologyPolygonOffsetParameters(Utils.getOffsetFactor(), Utils.getOffsetValue()*2.);
			}
		});
		rootNode.setSelectionMapperCustomiser(new AbstractNode.SelectionMapperCustomiser() {

			@Override
			public void customiseSelectionMapper(vtkMapper mapper)
			{
				mapper.SetResolveCoincidentTopologyToPolygonOffset();
				mapper.SetResolveCoincidentTopologyPolygonOffsetParameters(Utils.getOffsetFactor(), Utils.getOffsetValue());
			}
		});
		setViewMode(viewMode);

		Set<Entry<String, LeafNode.DataProvider>> groupSet = mesh.getGroupSet();
		groupsLoaded = new HashSet<String>(groupSet.size());
		for (Entry<String, LeafNode.DataProvider> entry : groupSet)
			groupsLoaded.add(entry.getKey());

		int cID = new Random().nextInt();

		for (Entry<String, LeafNode.DataProvider> group : groupSet)
		{
			// Warning: Do *not* replace colorManager.getColor() by
			// selectionColor here, some ColorManager may have a
			// different behavior!
			LeafNode groupNode = new LeafNode(rootNode, group.getValue(), palette.getColor(cID++));
			groupNode.setManager(true);

			groupIDToNode.put(group.getKey(), groupNode);
			groupNodeToID.put(groupNode, group.getKey());
		}
		rootNode.refresh();
	}

	public ViewMode getViewMode()
	{
		return viewMode;
	}

	public void setViewMode(ViewMode viewMode)
	{
		this.viewMode = viewMode;
		final int wired = (viewMode == ViewMode.WIRED) ? 1 : 0;

		rootNode.setActorCustomiser(new AbstractNode.ActorCustomiser()
		{

			@Override
			public void customiseActor(vtkActor actor)
			{
				actor.GetProperty().LightingOff();
				actor.GetProperty().SetEdgeVisibility(wired);
				actor.GetProperty().SetEdgeColor(0.2, 0.2, 0.2);
			}
		});
		rootNode.refresh();
		render();
	}
	/**
	 * Return the selection of the different ids groups
	 * @return
	 */
	public String[] getSelection()
	{
		String[] selectionGroups = new String[selectionNode.size()];

		int i = 0;
		for (LeafNode leaf : selectionNode)
		{
			selectionGroups[i] = groupNodeToID.get(leaf);
			i++;
		}

		return selectionGroups;
	}

	/**
	 * Set the new selection.
	 * Be careful, the listeners of change selection will not  be informed.
	 * The function verify if the group is loaded or not. If the group is not loaded
	 * it is removed from the selection.
	 * 
	 * @param selection 
	 */
	public void setSelection(String[] selection)
	{
		selectionNode.clear();
		for (String id : selection)
		{
			if (groupsLoaded.contains(id))
				selectionNode.add(groupIDToNode.get(id));
		}
	}
}
