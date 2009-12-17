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
 * (C) Copyright 2008-2009, by EADS France
 */
package org.jcae.vtk;

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import org.jcae.vtk.AbstractNode.ActorCustomiser;
import org.jcae.vtk.AbstractNode.MapperCustomiser;
import vtk.vtkMapper;
import vtk.vtkActor;
import vtk.vtkProperty;

/**
 * TODO : the beams are stored in the polydata with the scalar
 * UNVProvider.OTHERS_GROUP wich can make a bug of color. It can make a bug for
 * the selection in jcae because this group doesn't really exist. Find an
 * example of .unv to check this feature.
 * @author Julian Ibarz
 */
public class ViewableMesh extends Viewable
{
	private ViewMode viewMode = ViewMode.WIRED;
	private final Map<String, LeafNode> groupToTrias = new HashMap<String, LeafNode>();
	private final Map<LeafNode, String> triasToNode = new HashMap<LeafNode, String>();
	private final Map<String, LeafNode> groupToBeams = new HashMap<String, LeafNode>();
	private final Map<LeafNode, String> beamsToNode = new HashMap<LeafNode, String>();
	private final HashSet<String> groupsLoaded = new HashSet<String>();
	private int currentColorID;
	private final Palette palette;
	private final static MapperCustomiser MAPPER_CUSTOMIZER = new MapperCustomiser() {
		@Override
		public void customiseMapper(vtkMapper mapper)
		{
			mapper.SetResolveCoincidentTopologyToPolygonOffset();
			mapper.SetResolveCoincidentTopologyPolygonOffsetParameters(
				Utils.getOffsetFactor(), Utils.getOffsetValue()*2.);
		}
	};

	private final static MapperCustomiser MAPPER_CUSTOMIZER_S = new MapperCustomiser() {
		@Override
		public void customiseMapper(vtkMapper mapper)
		{
			mapper.SetResolveCoincidentTopologyToPolygonOffset();
			mapper.SetResolveCoincidentTopologyPolygonOffsetParameters(
				Utils.getOffsetFactor(), Utils.getOffsetValue());
		}
	};
	
	/**
	 * It explain how the mesh is displayed :
	 * _ FILLED means the mesh is not in wired mode but the selection remains filled ;
	 * _ WIRED means the mesh is wired.
	 */
	public enum ViewMode { FILLED, WIRED }

	public ViewableMesh(Map<String, LeafNode.DataProvider> triangles)
	{
		this(new Palette(Integer.MAX_VALUE));
		addTriangles(triangles);
	}
	
	public ViewableMesh(Palette palette)
	{		
		rootNode.setSelectionActorCustomiser(new SelectionActorCustomiser()
		{
			@Override
			public void customiseActor(vtkActor actor)
			{
				super.customiseActor(actor);
				actor.GetProperty().EdgeVisibilityOn();
				actor.GetProperty().LightingOff();
				actor.GetProperty().SetEdgeColor(0.4, 0.4, 0.4);
			}
		});
		rootNode.setMapperCustomiser(MAPPER_CUSTOMIZER);
		rootNode.setSelectionMapperCustomiser(MAPPER_CUSTOMIZER_S);
		setViewMode(viewMode);
		currentColorID = new Random().nextInt();
		this.palette = palette;
	}

	public void addTriangles(Map<String, LeafNode.DataProvider> mesh)
	{
		for (Entry<String, LeafNode.DataProvider> group : mesh.entrySet())
		{
			// Warning: Do *not* replace colorManager.getColor() by
			// selectionColor here, some ColorManager may have a
			// different behavior!
			LeafNode groupNode = new LeafNode(rootNode, group.getValue(),
				getColor(group.getKey()));
			groupNode.setManager(true);

			groupToTrias.put(group.getKey(), groupNode);
			triasToNode.put(groupNode, group.getKey());
		}
		rootNode.refresh();
	}

	/** ensure that beams and trias in the same groups have the same color */
	private Color getColor(String groupid)
	{
		LeafNode a = groupToBeams.get(groupid);
		if(a != null)
			return a.getColor();
		a = groupToTrias.get(groupid);
		if(a != null)
			return a.getColor();
		return palette.getColor(currentColorID++);
	}

	public void addBeams(Map<String, LeafNode.DataProvider> beams)
	{
		for (Entry<String, LeafNode.DataProvider> group : beams.entrySet())
		{
			final LeafNode groupNode = new LeafNode(rootNode, group.getValue(),
				getColor(group.getKey()));
			groupNode.setActorCustomiser(new ActorCustomiser() {
				public void customiseActor(vtkActor actor) {
					vtkProperty p = actor.GetProperty();
					Utils.vtkPropertySetColor(p, groupNode.getColor());
					p.SetLineWidth(3.0);
					p.Delete();
				}
			});
			groupNode.setManager(true);

			groupToBeams.put(group.getKey(), groupNode);
			beamsToNode.put(groupNode, group.getKey());
		}
		rootNode.refresh();
	}
	
	public void selectSelectionNodes() {
		for (LeafNode n :rootNode.getLeaves())
			n.unselect();
		for (LeafNode n : selectionNode)
			if (n!=null)
				n.select();
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
			selectionGroups[i] = triasToNode.get(leaf);
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
				selectionNode.add(groupToTrias.get(id));
		}
	}
}
