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

import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectIntHashMap;
import java.awt.Color;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import org.jcae.vtk.LeafNode;
import vtk.vtkActor;
import vtk.vtkMapper;

/**
 * TODO : the beams are stored in the polydata with the scalar UNVProvider.OTHERS_GROUP wich can make a bug of color. It can make a bug for the selection in jcae
 * because this group doesn't really exist. Find an example of .unv to check this feature.
 * @author Julian Ibarz
 */
public class ViewableMesh extends Viewable
{

	//private boolean[] groupIsLoaded;
	//private int nbrOfGroupLoaded;
	private ViewMode viewMode = ViewMode.FILLED;
	private ColorManager colorManager;
	private TIntObjectHashMap<LeafNode> groupIDToNode = new TIntObjectHashMap<LeafNode>();
	private TObjectIntHashMap<LeafNode> groupNodeToID = new TObjectIntHashMap<LeafNode>();
	private TIntHashSet groupsLoaded;
	/**
	 * It explain how the mesh is displayed :
	 * _ FILLED means the mesh is not in wired mode but the selection remains filled ;
	 * _ wired means the mesh is wired.
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
		super(new Scene(), new Node(null));
		this.colorManager = colorManager;
		rootNode.setActorHighLightedCustomiser(new ActorHighLightedCustomiser()
		{

			@Override
			public void customiseActorHighLighted(vtkActor actor)
			{
				super.customiseActorHighLighted(actor);
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

		colorManager.setColor(this.selectionColor);
		
		computeNodes(mesh);
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
	 * @param mesh
	 * @param fieldName
	 * @return
	 */
	private void computeNodes(Mesh mesh)
	{
		for (Entry<Integer, LeafNode.DataProvider> group : mesh.getGroupSet())
		{
			LeafNode groupNode = new LeafNode(rootNode, group.getValue(), colorManager.getColor());

			groupNode.setManager(true);

			groupIDToNode.put(group.getKey(), groupNode);
			groupNodeToID.put(groupNode, group.getKey());
		}
		//rootNode.setManager(true);
		rootNode.refresh();
	}

	/**
	 * Return the selection of the different ids groups
	 * @return
	 */
	public int[] getSelection()
	{
		int[] selectionGroups = new int[selectionNode.size()];

		Iterator<LeafNode> iter = selectionNode.iterator();
		for (int i = 0; i < selectionGroups.length; ++i)
		{
			LeafNode leaf = iter.next();
			selectionGroups[i] = groupNodeToID.get(leaf);
		}

		return selectionGroups;
	}

	/**
	 * Set the new selection and highlight it.
	 * Be careful, the listeners of change selection will not  be informed.
	 * The function verify if the group is loaded or not. If the group is not loaded
	 * it is removed from the selection.
	 * 
	 * @param selection 
	 */
	public void setSelection(int[] selection)
	{
		TIntArrayList cleanedSelection = new TIntArrayList(selection.length);
		for (int id : selection)
			if (groupsLoaded.contains(id))
				cleanedSelection.add(id);

		//if (selectionNode.size() != 0)
			//selectionChanged = true;
		selectionNode.clear();
		for (int i = 0; i < cleanedSelection.size(); ++i)
			if (selectionNode.add(groupIDToNode.get(cleanedSelection.get(i))));
				//this.selectionChanged = true;
	}
}
