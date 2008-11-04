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

import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectIntHashMap;
import java.util.Map.Entry;
import java.util.Set;
import vtk.vtkMapper;
import vtk.vtkActor;

/**
 * TODO : the beams are stored in the polydata with the scalar UNVProvider.OTHERS_GROUP wich can make a bug of color. It can make a bug for the selection in jcae
 * because this group doesn't really exist. Find an example of .unv to check this feature.
 * @author Julian Ibarz
 */
public class ViewableMesh extends Viewable
{
	private ViewMode viewMode = ViewMode.WIRED;
	private ColorManager colorManager;
	private TIntObjectHashMap<LeafNode> groupIDToNode = new TIntObjectHashMap<LeafNode>();
	private TObjectIntHashMap<LeafNode> groupNodeToID = new TObjectIntHashMap<LeafNode>();
	private TIntHashSet groupsLoaded;
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

	public ViewableMesh(Mesh mesh)
	{
		this(mesh, new GroupColorManager());
	}
	
	public ViewableMesh(Mesh mesh, ColorManager colorManager)
	{
		this.colorManager = colorManager;
		this.colorManager.setColor(selectionColor);
		
		rootNode.setActorHighlightedCustomiser(new ActorHighlightedCustomiser()
		{
			@Override
			public void customiseActorHighlighted(vtkActor actor)
			{
				super.customiseActorHighlighted(actor);
				actor.GetProperty().EdgeVisibilityOn();
				actor.SetEnableLighting(0);
				actor.GetProperty().SetEdgeColor(0.4, 0.4, 0.4);
			}
		});
		rootNode.setActorSelectionCustomiser(new ActorSelectionCustomiser()
		{

			@Override
			public void customiseActorSelection(vtkActor actor)
			{
				super.customiseActorSelection(actor);
				actor.GetProperty().EdgeVisibilityOn();
				actor.SetEnableLighting(0);
				actor.GetProperty().SetEdgeColor(0.4, 0.4, 0.4);
			}
		});
		rootNode.setMapperCustomiser(new AbstractNode.MapperCustomiser() {

			@Override
			public void customiseMapper(vtkMapper mapper)
			{
				mapper.SetResolveCoincidentTopologyToPolygonOffset();
				mapper.SetResolveCoincidentTopologyPolygonOffsetParameters(Utils.getOffSetFactor(), Utils.getOffSetValue()*2.);
			}
		});
		rootNode.setMapperSelectionCustomiser(new AbstractNode.MapperSelectionCustomiser() {

			@Override
			public void customiseMapperSelection(vtkMapper mapper)
			{
				mapper.SetResolveCoincidentTopologyToPolygonOffset();
				mapper.SetResolveCoincidentTopologyPolygonOffsetParameters(Utils.getOffSetFactor(), Utils.getOffSetValue());
			}
		});
		setViewMode(viewMode);

		Set<Entry<Integer, LeafNode.DataProvider>> groupSet = mesh.getGroupSet();
		groupsLoaded = new TIntHashSet(groupSet.size());
		for (Entry<Integer, LeafNode.DataProvider> entry : groupSet)
			groupsLoaded.add(entry.getKey());

		for (Entry<Integer, LeafNode.DataProvider> group : groupSet)
		{
			// Warning: Do *not* replace colorManager.getColor() by
			// selectionColor here, some ColorManager may have a
			// different behavior!
			LeafNode groupNode = new LeafNode(rootNode, group.getValue(), colorManager.getColor());
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
				actor.SetEnableLighting(0);
				actor.GetProperty().SetEdgeVisibility(wired);
				actor.GetProperty().SetEdgeColor(0.2, 0.2, 0.2);
			}
		});
		rootNode.applyActorCustomiser();
		render();
	}
	/**
	 * Return the selection of the different ids groups
	 * @return
	 */
	public int[] getSelection()
	{
		int[] selectionGroups = new int[selectionNode.size()];

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
	public void setSelection(int[] selection)
	{
		selectionNode.clear();
		for (int id : selection)
		{
			if (groupsLoaded.contains(id))
				selectionNode.add(groupIDToNode.get(id));
		}
	}
}
