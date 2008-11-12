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

import java.awt.Color;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.vecmath.Point3f;
import org.jcae.geometry.Transform3D;
import vtk.vtkActor;
import vtk.vtkExtractSelectedPolyDataIds;
import vtk.vtkIdTypeArray;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkSelection;

/**
 * TODO replace Color by javax.vecmath.Color3f
 * @author Julian Ibarz
 */
public class LeafNode extends AbstractNode
{
	private final static Logger LOGGER = Logger.getLogger(LeafNode.class.getName());

	public static class DataProvider
	{
		private long modifiedTime = System.nanoTime();
		private float[] nodesTransformed = new float[0];
		protected float[] nodes = new float[0];
		protected float[] normals = null;
		protected int[] vertices = new int[0];
		protected int[] lines = new int[0];
		protected int[] polys = new int[0];
		protected int nbrOfPolys;
		protected int nbrOfLines;
		protected int nbrOfVertices;
		private Transform3D transform;
		
		public static final DataProvider EMPTY = new DataProvider();

		public void setVertices(int[] vertices)
		{
			this.vertices = vertices;
			nbrOfVertices = vertices.length / 2;
			modified();
		}
		
		public void setLines(int[] lines)
		{
			this.lines = lines;
			nbrOfLines = lines.length / 3;
			modified();
		}
		
		public void setNodes(float[] nodes)
		{
			this.nodes = nodes;
			
			makeTransform();
			modified();
		}
		
		private void makeTransform()
		{
			if(transform != null)
			{
				this.nodesTransformed = new float[this.nodes.length];
				
				Point3f point = new Point3f();
				int j = 0;
				for(int i = 0 ; i < this.nodes.length ; )
				{
					point.x = this.nodes[i++];
					point.y = this.nodes[i++];
					point.z = this.nodes[i++];
					
					transform.transform(point);
					
					nodesTransformed[j++] = point.x;
					nodesTransformed[j++] = point.y;
					nodesTransformed[j++] = point.z;
				}
			}
			else
				this.nodesTransformed = this.nodes;
		}
		
		public void setTransform(Transform3D transform)
		{
			this.transform = transform;
			
			makeTransform();
		}
		
		public void setPolys(int nbrOfPolys, int[] polys)
		{
			this.nbrOfPolys = nbrOfPolys;
			this.polys = polys;
			modified();
		}
		
		public int getNbrOfPolys()
		{
			return nbrOfPolys;
		}

		public int getNbrOfLines()
		{
			return nbrOfLines;
		}

		public int getNbrOfVertices()
		{
			return nbrOfVertices;
		}

		public void load()
		{
			// Do nothing
		}

		public float[] getNodes()
		{
			return nodesTransformed;
		}

		public int[] getPolys()
		{
			return polys;
		}
		
		public int[] getLines()
		{
			return lines;
		}
		
		public int[] getVertices()
		{
			return vertices;
		}
		
		public float[] getNormals()
		{
			return normals;
		}
		
		public void unLoad()
		{
			nodes = new float[0];
			nodesTransformed = new float[0];
			normals = null;
			vertices = new int[0];
			lines = new int[0];
			polys = new int[0];
			modified();
		}

		protected void modified()
		{
			modifiedTime = System.nanoTime();
		}

		protected long getModifiedTime()
		{
			return modifiedTime;
		}
	}
	
	private int [] selection = new int[0];
	private Color color;
	private DataProvider dataProvider;
	
	public LeafNode(Node parent, DataProvider dataProvider, Color color)
	{
		super(parent);
		parent.addChild(this);
		this.dataProvider = dataProvider;
		this.color = color;
	}
	
	public List<LeafNode> getLeaves()
	{
		return Collections.singletonList(this);
	}

	public Color getColor()
	{
		return color;
	}

	public void setColor(Color color)
	{
		if(this.color.equals(color))
			return;
		
		this.color = color;
		
		if(actor != null)
			Utils.vtkPropertySetColor(actor.GetProperty(), color);
		else
			timestampModified();
	}
	
	public void setData(LeafNode.DataProvider data)
	{
		this.dataProvider = data;
		timestampData();
	}

	public DataProvider getDataProvider()
	{
		return dataProvider;
	}

	public void setTransform(Transform3D transform)
	{
		dataProvider.setTransform(transform);
	}
	
	public void refresh()
	{
		if (!isManager())
		{
			lastUpdate = System.nanoTime();
			return;
		}

		if (LOGGER.isLoggable(Level.FINER))
			LOGGER.log(Level.FINER, "Refreshing leaf: "+this);
		
		// Were data modified?
		if (dataTime < dataProvider.getModifiedTime())
			refreshData();

		// Was actor modified?
		if (lastUpdate <= modificationTime)
			refreshActor();

		// Did selection happen?
		if (lastUpdate <= selectionTime)
			refreshHighlight();

		lastUpdate = System.nanoTime();
	}

	private void refreshData()
	{
		if (LOGGER.isLoggable(Level.FINEST))
			LOGGER.log(Level.FINEST, "Refresh data for "+this);

		dataProvider.load();
		createData(dataProvider);
		dataProvider.unLoad();
		
		timestampData();

		if(mapper == null)
			mapper = new vtkPolyDataMapper();
		getMapperCustomiser().customiseMapper(mapper);
		mapper.SetInput(data);
		mapper.Update();
	}

	// Must always be called after refreshData
	private void refreshActor()
	{
		if (LOGGER.isLoggable(Level.FINEST))
			LOGGER.log(Level.FINEST, "Refresh actor for "+this);

		boolean actorCreated = (actor == null);
		if(actorCreated)
			actor = new vtkActor();
		getActorCustomiser().customiseActor(actor);
		actor.SetMapper(mapper);
		actor.SetVisibility(Utils.booleanToInt(visible));
		actor.SetPickable(Utils.booleanToInt(pickable));

		if (actorCreated)
		{
			fireActorCreated(actor);
			if (LOGGER.isLoggable(Level.FINEST))
				LOGGER.log(Level.FINEST, "New actor created: vtkActor@"+Integer.toHexString(actor.hashCode()));
		}

		if (LOGGER.isLoggable(Level.FINEST))
			LOGGER.log(Level.FINEST, "Attach color "+color+
				" (opacity="+color.getAlpha()+") to actor @"+Integer.toHexString(actor.hashCode()));
		Utils.vtkPropertySetColor(actor.GetProperty(), color);
	}

	private void refreshHighlight()
	{
		if (LOGGER.isLoggable(Level.FINEST))
			LOGGER.log(Level.FINEST, "Refresh highlight for "+this);

		if (selected)
		{
			// Highlight actor
			getActorSelectionCustomiser().customiseActorSelection(actor);
			getMapperSelectionCustomiser().customiseMapperSelection(mapper);
			
			deleteSelectionHighlighter();
		}
		else
		{
			// Reset original color
			Utils.vtkPropertySetColor(actor.GetProperty(), color);
			getActorCustomiser().customiseActor(actor);
			getMapperCustomiser().customiseMapper(mapper);
			
			refreshSelectionHighlighter();
		}
	}

	private void refreshSelectionHighlighter()
	{
		if (selection.length == 0)
		{
			deleteSelectionHighlighter();
			return;
		}

		if (selectionHighlighter == null)
		{
			selectionHighlighter = new vtkActor();
			selectionHighlighter.PickableOff();
			// fireActorCreated is called before creating its mapper
			// to not take clipping planes into account
			fireActorCreated(selectionHighlighter);
		}
		getActorSelectionCustomiser().customiseActorSelection(selectionHighlighter);

		selectionHighlighterMapper = new vtkPolyDataMapper();
		selectionHighlighter.SetMapper(selectionHighlighterMapper);
		getMapperSelectionCustomiser().customiseMapperSelection(selectionHighlighterMapper);

		vtkSelection sel = new vtkSelection();
		//sel.ReleaseDataFlagOn();
		sel.GetProperties().Set(sel.CONTENT_TYPE(), 4); // 4 MEANS INDICES (see the enumeration)

		sel.GetProperties().Set(sel.FIELD_TYPE(), 0); // 0 MEANS CELLS

		// list of cells to be selected
		vtkIdTypeArray arr = Utils.setValues(selection);
		sel.SetSelectionList(arr);

		vtkExtractSelectedPolyDataIds selFilter = new vtkExtractSelectedPolyDataIds();
		selFilter.ReleaseDataFlagOn();
		selFilter.SetInput(1, sel);
		selFilter.SetInput(0, data);

		vtkPolyData dataFiltered = selFilter.GetOutput();
		selFilter.Update();

		selectionHighlighterMapper.SetInput(dataFiltered);
	}

	void setCellSelection(PickContext pickContext, int [] cellSelection)
	{
		selection = new int[cellSelection.length];
		System.arraycopy(cellSelection, 0, selection, 0, cellSelection.length);
		pickContext.addToSelectedNodes(this);
		timestampSelected();
	}

	int [] getCellSelection()
	{
		return selection;
	}

	public void clearCellSelection()
	{
		selection = new int[0];
		timestampSelected();
	}
	
	public boolean hasCellSelection()
	{
		return selection.length != 0;
	}
	
	@Override
	public void select()
	{
		selection = new int[0];
		
		super.select();
	}

}
