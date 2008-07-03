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
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectIntHashMap;
import java.awt.Color;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import org.jcae.vtk.LeafNode;
import vtk.vtkActor;
import vtk.vtkMapper;
import vtk.vtkPlaneCollection;

/**
 * TODO : the beams are stored in the polydata with the scalar UNVProvider.OTHERS_GROUP wich can make a bug of color. It can make a bug for the selection in jcae
 * because this group doesn't really exist. Find an example of .unv to check this feature.
 * @author Julian Ibarz
 */
public class ViewableMesh extends Viewable
{

	private boolean[] groupIsLoaded;
	private int nbrOfGroupLoaded;
	private ViewMode viewMode = ViewMode.FILLED;
	private ColorManager colorManager = new ColorManager();
	private TIntObjectHashMap<LeafNode> groupIDToNode = new TIntObjectHashMap<LeafNode>();
	private TObjectIntHashMap<LeafNode> groupNodeToID = new TObjectIntHashMap<LeafNode>();

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

	/**
	 * 
	 * @param filename
	 * @param groupExtraction @see UNVToMesh constructor
	 */
	public ViewableMesh(String filename, Collection<Integer> groupExtraction)
	{
		this(new UNVToMesh(filename, groupExtraction).getMesh());
	}

	public ViewableMesh(Mesh mesh)
	{
		super(new Scene(), new Node(null));

		rootNode.setActorHighLightedCustomiser(new ActorHighLightedCustomiser()
		{

			@Override
			public void customiseActorHighLighted(vtkActor actor)
			{
				super.customiseActorHighLighted(actor);
				actor.GetProperty().EdgeVisibilityOn();
			}
		});
		rootNode.setActorSelectionCustomiser(new ActorSelectionCustomiser()
		{

			@Override
			public void customiseActorSelection(vtkActor actor)
			{
				super.customiseActorSelection(actor);
				actor.GetProperty().EdgeVisibilityOn();
			}
		});
		rootNode.setMapperCustomiser(new AbstractNode.MapperCustomiser() {

			public void customiseMapper(vtkMapper mapper)
			{
				mapper.SetResolveCoincidentTopologyToPolygonOffset();
				mapper.SetResolveCoincidentTopologyPolygonOffsetParameters(Utils.getOffSetFactor(), Utils.getOffSetValue()*2.);
			}
		});
		rootNode.setMapperSelectionCustomiser(new AbstractNode.MapperSelectionCustomiser() {

			public void customiseMapperSelection(vtkMapper mapper)
			{
				mapper.SetResolveCoincidentTopologyToPolygonOffset();
				mapper.SetResolveCoincidentTopologyPolygonOffsetParameters(Utils.getOffSetFactor(), Utils.getOffSetValue());
			}
		});
		setViewMode(viewMode);
		int groupIDMax = 0;
		for (Entry<Integer, LeafNode.DataProvider> entry : mesh.getGroupSet())
			groupIDMax = Math.max(groupIDMax, entry.getKey());

		groupIsLoaded = new boolean[groupIDMax + 1];
		int index = 0;
		for (Entry<Integer, LeafNode.DataProvider> entry : mesh.getGroupSet())
		{
			groupIsLoaded[entry.getKey()] = true;
			++index;
		}
		nbrOfGroupLoaded = index;

		colorManager.setColor(new Color(255, 0, 0));

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

			public void customiseActor(vtkActor actor)
			{
				actor.GetProperty().SetEdgeVisibility(wired);
			}
		});
		rootNode.applyActorCustomiser();
		render();
	}

	/**
	 * Return the vtkPolyData representation of a mesh. The groups are stored in the scalars of the cell datas.
	 * Also the polygons are stored like this :
	 * by order of the groups and in the groups :
	 * first the cells
	 * second the quads
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
			if (id < nbrOfGroupLoaded && groupIsLoaded[id])
				cleanedSelection.add(id);

		if (selectionNode.size() != 0)
			selectionChanged = true;
		selectionNode.clear();
		for (int i = 0; i < cleanedSelection.size(); ++i)
			if (selectionNode.add(groupIDToNode.get(cleanedSelection.get(i))))
				this.selectionChanged = true;
	}
}
