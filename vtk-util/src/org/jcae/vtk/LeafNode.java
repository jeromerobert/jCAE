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
import vtk.vtkActor;
import vtk.vtkExtractSelectedPolyDataIds;
import vtk.vtkIdTypeArray;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkSelection;

/**
 *
 * @author ibarz
 */
public class LeafNode extends AbstractNode
{

	public static class DataProvider
	{
		private long modifiedTime = System.nanoTime();
		protected float[] nodes = new float[0];
		protected float[] normals = null;
		protected int[] vertice = new int[0];
		protected int[] lines = new int[0];
		protected int[] polys = new int[0];
		protected int nbrOfPolys = 0;
		protected int nbrOfLines = 0;
		protected int nbrOfVertice = 0;

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
			return nodes;
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
			throw new RuntimeException("A leaf cannot have a child !");
		}
	public void refresh()
	{
		if (isManager())
		{
			checkData();

			if (lastUpdate <= modificationTime)
			{
				//System.out.println("REFRESH ACTOR !");
				refreshActor();
			}
			manageHighLight();
		}
		lastUpdate = System.nanoTime();
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
	
	protected void manageHighLight()
	{
		if (selectionTime() <= lastUpdate)
			return;

		if (isSelected())
			highLight();
		else
			unHighLight();
	}

	protected void checkData()
	{
		// The data was modified ?
		if (timeDataCreated < dataProvider.getModifiedTime())
			refreshData();
	}

	@Override
	protected void manageSelection(int[] cellSelection)
	{
		selection = new TIntArrayList(cellSelection);
	}

	void setSelection(TIntArrayList selection)
	{
		this.selection = selection;
	}

	TIntArrayList getSelection()
	{
		return this.selection;
	}

	public Color getColor()
	{
		return color;
	}

	@Override
	protected void unHighLight()
	{
		if (actor != null)
		{
			Utils.setColorActor(actor, color);
		}
	}

	public void setColor(Color color)
	{
		if(this.color.equals(color))
			return;
		
		this.color = color;
		
		if(actor != null)
			Utils.setColorActor(actor, color);
		else
			modified();
	}

	public List<LeafNode> getLeaves()
	{
		ArrayList<LeafNode> toReturn = new ArrayList<LeafNode>();
		toReturn.add(this);
		return toReturn;
	}
	
	public void setData(LeafNode.DataProvider data)
	{
		this.dataProvider = data;
		modified();
	}

	@Override
	protected void refreshData()
	{
		//System.out.println("REFRESHING DATA !");
		//System.out.println("DATA NODES : " + dataProvider.getNodes().length);
		dataProvider.load();
		createData(dataProvider);
		dataProvider.unLoad();
		
//		System.out.println("DATA NODES : " + dataProvider.getNodes().length);

		timeDataCreated = System.nanoTime();
		modified();
	}

	// TEMPORARY : remove it
	protected vtkPolyData getData()
	{
		return data;
	}
	
	@Override
	protected void refreshActor()
	{
		super.refreshActor();

		Utils.setColorActor(actor, color);
	}

	public DataProvider getDataProvider()
	{
		return dataProvider;
	}
	
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

		if (selection.size() == 0)
		{
			if (selectionHighLighter != null)
				unHighLightSelection();
			return;
		}

		if (selectionHighLighter == null)
		{
			selectionHighLighter = new vtkActor();
			selectionHighLighter.PickableOff();

			fireActorCreated(selectionHighLighter);
			fireActorHighLighted(selectionHighLighter);
		}

		vtkPolyDataMapper mapper = new vtkPolyDataMapper();
		selectionHighLighter.SetMapper(mapper);

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

		mapper.SetInput(dataFiltered);
	}

	public void unSelectAll()
	{
		selection.reset();
	}
}
