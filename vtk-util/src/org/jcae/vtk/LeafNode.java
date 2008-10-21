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
import java.awt.Color;
import java.util.ArrayList;
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
		protected int[] vertice = new int[0];
		protected int[] lines = new int[0];
		protected int[] polys = new int[0];
		protected int nbrOfPolys = 0;
		protected int nbrOfLines = 0;
		protected int nbrOfVertice = 0;
		private Transform3D transform = null;
		
		public static final DataProvider EMPTY = new DataProvider();

		public void setVertice(int[] vertice)
		{
			this.vertice = vertice;
			nbrOfVertice = vertice.length / 2;
		}
		
		public void setLines(int[] lines)
		{
			this.lines = lines;
			nbrOfLines = lines.length / 3;
		}
		
		public void setNodes(float[] nodes)
		{
			this.nodes = nodes;
			
			makeTransform();
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
			else this.nodesTransformed = this.nodes;
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
		}
		
		public int getNbrOfPolys()
		{
			return nbrOfPolys;
		}

		public int getNbrOfLines()
		{
			return nbrOfLines;
		}

		public int getNbrOfVertice()
		{
			return nbrOfVertice;
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
		
		public int[] getVertice()
		{
			return vertice;
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
			vertice = new int[0];
			lines = new int[0];
			polys = new int[0];
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
	
	private TIntArrayList selection = new TIntArrayList();
	private Color color;
	private DataProvider dataProvider;
	private long timeDataCreated = 0;
	
	public LeafNode(AbstractNode parent, DataProvider dataProvider, Color color)
	{
		super(parent);
		this.dataProvider = dataProvider;
		this.color = color;
	}
	
	protected void addChild(AbstractNode parent)
	{
		throw new RuntimeException("Leaves cannot have a child!");
	}

	public List<LeafNode> getLeaves()
	{
		ArrayList<LeafNode> toReturn = new ArrayList<LeafNode>();
		toReturn.add(this);
		return toReturn;
	}

	protected LeafNode getNode(int cellID)
	{
		if(!isManager())
			return parent.getNode(cellID);
		
		else if(0 <= cellID && cellID < data.GetNumberOfCells())
			return this;
		else
			throw new RuntimeException("cellID out of bounds");
	}
	
	public Color getColor()
	{
		return color;
	}

	public void setColor(Color color)
	{
		if(this.color.equals(color))
			return;
		
		if (LOGGER.isLoggable(Level.FINEST))
			LOGGER.log(Level.FINEST, "Change color of actor "+actor+" from "+this.color+" (opacity="+this.color.getAlpha()+") to "+color+" (opacity="+color.getAlpha()+")");
		this.color = color;
		
		if(actor != null)
			Utils.vtkPropertySetColor(actor.GetProperty(), color);
		else
			modified();
	}
	
	public void setData(LeafNode.DataProvider data)
	{
		this.dataProvider = data;
		modified();
	}

	// TEMPORARY : remove it
	protected vtkPolyData getData()
	{
		return data;
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

		checkData();
		if (lastUpdate <= modificationTime)
		{
			//System.out.println("REFRESH ACTOR !");
			LOGGER.finest("Refresh actor: "+lastUpdate+" <= "+modificationTime);
			refreshActor();
		}
		manageHighLight();

		lastUpdate = System.nanoTime();
	}

	@Override
	protected void refreshData()
	{
		LOGGER.finest("Refresh data, old creation date="+timeDataCreated);
		dataProvider.load();
		createData(dataProvider);
		dataProvider.unLoad();
		
		modified();
		timeDataCreated = System.nanoTime();
	}

	@Override
	protected void refreshActor()
	{
		super.refreshActor();
		if (LOGGER.isLoggable(Level.FINEST))
			LOGGER.log(Level.FINEST, "Attach color "+color+" (opacity="+color.getAlpha()+") to actor "+actor);
		Utils.vtkPropertySetColor(actor.GetProperty(), color);
	}

	protected void checkData()
	{
		// The data was modified ?
		if (timeDataCreated < dataProvider.getModifiedTime())
			refreshData();
	}

	@Override
	public void applyActorHighLightedCustomiser()
	{
		if(isSelected() && actor != null)
			getActorHighLightedCustomiser().customiseActorHighLighted(actor);
	}

	@Override
	public void applyMapperHighLightedCustomiser()
	{
		if(isSelected() && mapper != null)
			getMapperHighLightedCustomiser().customiseMapperHighLighted(mapper);
	}
	
	protected void manageHighLight()
	{
		if (selectionTime() <= lastUpdate)
			return;

		if (isSelected())
			highLight();
		else
			unHighLight();
	}

	@Override
	protected void unHighLight()
	{
		if (actor != null)
		{
			if (LOGGER.isLoggable(Level.FINEST))
				LOGGER.log(Level.FINEST, "Unhighlight actor "+actor);
			Utils.vtkPropertySetColor(actor.GetProperty(), color);
			getActorCustomiser().customiseActor(actor);
		}
	}

	void setCellSelection(TIntArrayList cellSelection)
	{
		selection = cellSelection;
	}

	TIntArrayList getSelection()
	{
		return selection;
	}

	@Override
	public void select()
	{
		selection.clear();
		
		super.select();
	}

	public void highLightSelection()
	{
		if (!isManager())
		{
			if (selectionHighLighter != null)
				unHighLightSelection();
 
			return;
		}

		if (selection.isEmpty())
		{
			if (selectionHighLighter != null)
				unHighLightSelection();
			return;
		}

		if (selectionHighLighter == null)
		{
			selectionHighLighter = new vtkActor();
			selectionHighLighter.PickableOff();
			getActorSelectionCustomiser().customiseActorSelection(selectionHighLighter);
			
			fireActorCreated(selectionHighLighter);
		}

		selectionHighLighterMapper = new vtkPolyDataMapper();
		selectionHighLighter.SetMapper(selectionHighLighterMapper);
		getMapperSelectionCustomiser().customiseMapperSelection(selectionHighLighterMapper);

		vtkSelection sel = new vtkSelection();
		//sel.ReleaseDataFlagOn();
		sel.GetProperties().Set(sel.CONTENT_TYPE(), 4); // 4 MEANS INDICES (see the enumeration)

		sel.GetProperties().Set(sel.FIELD_TYPE(), 0); // 0 MEANS CELLS

		// list of cells to be selected
		vtkIdTypeArray arr = Utils.setValues(selection.toNativeArray());
		sel.SetSelectionList(arr);

		vtkExtractSelectedPolyDataIds selFilter = new vtkExtractSelectedPolyDataIds();
		selFilter.ReleaseDataFlagOn();
		selFilter.SetInput(1, sel);
		selFilter.SetInput(0, data);

		vtkPolyData dataFiltered = selFilter.GetOutput();
		selFilter.Update();

		//System.out.println("Number of triangles selected : " + dataFiltered.GetNumberOfCells());

		selectionHighLighterMapper.SetInput(dataFiltered);
		
		getMapperSelectionCustomiser().customiseMapperSelection(selectionHighLighterMapper);
	}

	public void unSelectCells()
	{
		selection.reset();
	}
}
