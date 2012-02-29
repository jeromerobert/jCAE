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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.jcae.vtk.AbstractNode.ActorCustomiser;
import org.jcae.vtk.AbstractNode.MapperCustomiser;
import org.jcae.vtk.LeafNode.DataProvider;
import vtk.vtkActor;
import vtk.vtkPainterPolyDataMapper;
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
	private static class BeamNodeWrapper
	{
		public LeafNode beams, vertexBeams;
	}
	
	private ViewMode viewMode = ViewMode.WIRED;
	private final Map<String, LeafNode> groupToTrias = new HashMap<String, LeafNode>();
	private final Map<LeafNode, String> triasToNode = new HashMap<LeafNode, String>();
	private final Map<String, BeamNodeWrapper> groupToBeams = new HashMap<String, BeamNodeWrapper>();
	private final Map<LeafNode, String> beamsToNode = new HashMap<LeafNode, String>();
	private int currentColorID;
	private final Palette palette;
	private boolean beamVertVisible = true;
	private final static MapperCustomiser MAPPER_CUSTOMIZER = new MapperCustomiser() {
		@Override
		public void customiseMapper(vtkPainterPolyDataMapper mapper)
		{
			Utils.setPolygonOffset(mapper, Utils.getOffsetFactor(),
				Utils.getOffsetValue()*2);
		}
	};

	private final static MapperCustomiser MAPPER_CUSTOMIZER_S = new MapperCustomiser() {
		@Override
		public void customiseMapper(vtkPainterPolyDataMapper mapper)
		{
			Utils.setPolygonOffset(mapper, Utils.getOffsetFactor(),
				Utils.getOffsetValue());
		}
	};
	
	/**
	 * It explain how the mesh is displayed :
	 * _ FILLED means the mesh is not in wired mode but the selection remains filled ;
	 * _ WIRED means the mesh is wired.
	 */
	public enum ViewMode { FILLED, WIRED }

	public ViewableMesh()
	{
		this(new Palette(Integer.MAX_VALUE));
	}
	
	public ViewableMesh(Map<String, LeafNode.DataProvider> triangles)
	{
		this();
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
				vtkProperty p = actor.GetProperty();
				p.EdgeVisibilityOn();
				p.LightingOff();
				p.SetEdgeColor(0.4, 0.4, 0.4);
			}
		});
		rootNode.setMapperCustomiser(MAPPER_CUSTOMIZER);
		rootNode.setSelectionMapperCustomiser(MAPPER_CUSTOMIZER_S);
		setViewMode(viewMode);
		currentColorID = 0;
		this.palette = palette;
	}

	/**
	 * @param mesh a map associating group id to DataProvider. If dataprovider
	 * is null the group is removed from the viewable.
	 */
	public void addTriangles(Map<String, LeafNode.DataProvider> mesh)
	{
		for (Entry<String, LeafNode.DataProvider> group : mesh.entrySet())
		{
			LeafNode groupNode = groupToTrias.get(group.getKey());
			if(group.getValue() == null && groupNode != null)
			{				
				rootNode.removeChild(groupNode);
				groupToTrias.remove(group.getKey());
				triasToNode.remove(groupNode);
			}
			else if(group.getValue() != null && groupNode == null)
			{
				// Warning: Do *not* replace colorManager.getColor() by
				// selectionColor here, some ColorManager may have a
				// different behavior!
				groupNode = new LeafNode(rootNode, group.getValue(),
					getColor(group.getKey()));
				groupNode.setManager(true);
				groupNode.setPickable(true);
				groupToTrias.put(group.getKey(), groupNode);
				triasToNode.put(groupNode, group.getKey());
			}
			else if(group.getValue() != null && groupNode != null)
			{
				groupNode.setDataProvider(group.getValue());
			}
		}
		rootNode.refresh();
	}

	public Collection<String> getTriaGroups()
	{
		return Collections.unmodifiableCollection(groupToTrias.keySet());
	}

	public Collection<String> getBeamGroups()
	{
		return Collections.unmodifiableCollection(groupToBeams.keySet());
	}

	/** ensure that beams and trias in the same groups have the same color */
	private Color getColor(String groupid)
	{
		BeamNodeWrapper aa = groupToBeams.get(groupid);
		LeafNode a = aa == null ? null : groupToBeams.get(groupid).beams;
		if(a != null)
			return a.getColor();
		a = groupToTrias.get(groupid);
		if(a != null)
			return a.getColor();
		return palette.getColor(currentColorID++);
	}

	private DataProvider createVertices(final DataProvider value) {
		DataProvider toReturn = new DataProvider()
		{
			@Override
			public void load() {
				if(value.getNodes().length == 0)
					value.load();
				setNodes(value.getNodes());
				int n = nodes.length / 3;
				int[] vert = new int[2*n];
				for(int i = 0; i<n; i++)
				{
					vert[2*i] = 1;
					vert[2*i+1] = i;
				}
				setVertices(vert);
			}
		};
		return toReturn;
	}
	
	public void addBeams(Map<String, LeafNode.DataProvider> beams)
	{
		for (Entry<String, LeafNode.DataProvider> group : beams.entrySet())
		{
			BeamNodeWrapper groupNode = groupToBeams.get(group.getKey());
			if(group.getValue() == null && groupNode != null)
			{
				rootNode.removeChild(groupNode.beams);
				if(groupNode.vertexBeams != null)
					rootNode.removeChild(groupNode.vertexBeams);
				groupToBeams.remove(group.getKey());
				beamsToNode.remove(groupNode.beams);
			}
			else if(group.getValue() != null && groupNode == null)
			{
				BeamNodeWrapper bnw = new BeamNodeWrapper();
				final LeafNode fgn = new LeafNode(rootNode, group.getValue(),
					getColor(group.getKey()));				
				fgn.setActorCustomiser(new ActorCustomiser() {
					public void customiseActor(vtkActor actor) {
						vtkProperty p = actor.GetProperty();
						Utils.vtkPropertySetColor(p, fgn.getColor());
						p.SetLineWidth(2.0);					
					}
				});
				fgn.setManager(true);
				fgn.setPickable(true);
				bnw.beams = fgn;

				if(beamVertVisible)
				{
					final LeafNode vertexNode = new LeafNode(rootNode, createVertices(group.getValue()), Color.WHITE);
					vertexNode.setActorCustomiser(new ActorCustomiser() {
						public void customiseActor(vtkActor actor) {
							actor.GetProperty().SetPointSize(3.0);
						}
					});
					vertexNode.setManager(true);
					vertexNode.setPickable(false);
					bnw.vertexBeams = vertexNode;
				}

				groupToBeams.put(group.getKey(), bnw);
				beamsToNode.put(fgn, group.getKey());
			}
			else if(group.getValue() != null && groupNode != null)
			{				
				groupNode.beams.setDataProvider(group.getValue());
				if(groupNode.vertexBeams != null)
					groupNode.vertexBeams.setDataProvider(
						createVertices(group.getValue()));
			}
		}
		rootNode.refresh();
	}

	public boolean isBeamVertVisible() {
		return beamVertVisible;
	}

	public void setBeamVertVisible(boolean beamVertVisible) {
		this.beamVertVisible = beamVertVisible;
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
				vtkProperty p = actor.GetProperty();
				p.LightingOff();
				p.SetEdgeVisibility(wired);
				p.SetEdgeColor(0.2, 0.2, 0.2);
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
			LeafNode n = groupToTrias.get(id);
			if(n != null)
				selectionNode.add(n);
		}
	}
}

