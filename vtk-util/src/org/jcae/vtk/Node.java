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
import java.util.Arrays;
import java.util.List;
import vtk.vtkActor;
import vtk.vtkExtractSelectedPolyDataIds;
import vtk.vtkFloatArray;
import vtk.vtkIdTypeArray;
import vtk.vtkIntArray;
import vtk.vtkLookupTable;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkPolyDataNormals;
import vtk.vtkSelection;

/**
 *
 * @author Julian Ibarz
 */
public class Node extends AbstractNode
{

	private ArrayList<AbstractNode> children = new ArrayList<AbstractNode>();
	// Datas if the node manage
	private TIntArrayList offSetsVertice = null;
	private TIntArrayList offSetsLines = null;
	private TIntArrayList offSetsPolys = null;
	private int nbrOfVertice;
	private int nbrOfLines;
	private int nbrOfPolys;
	private vtkLookupTable table = null;
	private vtkActor highLighter = null;
	private ArrayList<ChildCreationListener> childCreationListeners = new ArrayList<ChildCreationListener>();

	public Node(AbstractNode parent)
	{
		// Make it to not make a course...
		super(null);
		if(parent != null)
		{
			this.parent = parent;
			parent.addChild(this);
		}
	}

	public interface ChildCreationListener
	{
		void childCreated(AbstractNode node);		
		void childDeleted(AbstractNode node);
	}
	
	public void addChild(AbstractNode child)
	{
		if(children.add(child))
		{
			fireChildCreationListener(child);
			modified();			
		}
	}

	public int numChildren()
	{
		return children.size();
	}

	public void removeChild(AbstractNode child)
	{
		if (children.remove(child))
		{
			fireChildDeletionListener(child);
			modified();
		}
	}
	
	public void addChildCreationListener(ChildCreationListener listener)
	{
		childCreationListeners.add(listener);
	}
	
	public void removeChildCreationListener(ChildCreationListener listener)
	{
		childCreationListeners.remove(listener);
	}
	
	private void fireChildCreationListener(AbstractNode node)
	{
		for(ChildCreationListener listener : childCreationListeners)
			listener.childCreated(node);
	}
	
	private void fireChildDeletionListener(AbstractNode node)
	{
		for(ChildCreationListener listener : childCreationListeners)
			listener.childDeleted(node);
	}

	@Override
	protected void refreshMapper()
	{
		super.refreshMapper();
		mapper.SetLookupTable(table);
		mapper.UseLookupTableScalarRangeOn();
		mapper.SetScalarModeToUseCellData();
		getMapperCustomiser().customiseMapper(mapper);
	}

	private static class NodeData extends LeafNode.DataProvider
	{

		private final float[] nodes;
		private final float[] normals;
		private final int[] vertice;
		private final int[] lines;
		private final int[] polys;

		NodeData(float[] nodes, float[] normals, int nbrOfVertice, int[] vertice, int nbrOfLines, int[] lines, int nbrOfPolys, int[] polys)
		{
			this.nodes = nodes;
			this.normals = normals;
			this.nbrOfVertice = nbrOfVertice;
			this.vertice = vertice;
			this.nbrOfLines = nbrOfLines;
			this.lines = lines;
			this.nbrOfPolys = nbrOfPolys;
			this.polys = polys;
		}

		@Override
		public float[] getNormals()
		{
			return normals;
		}

		@Override
		public int[] getLines()
		{
			return lines;
		}

		@Override
		public int[] getPolys()
		{
			return polys;
		}

		@Override
		public int[] getVertice()
		{
			return vertice;
		}

		@Override
		public float[] getNodes()
		{
			return nodes;
		}

		@Override
		public void load()
		{
			// Do nothing
		}

		@Override
		public void unLoad()
		{
			// Do nothing
		}
	}

	protected LeafNode getNode(int cellID)
	{
		if (!isManager())
			return parent.getNode(cellID);
		else if (0 <= cellID && cellID < data.GetNumberOfCells())
		{
			List<LeafNode> leaves = getLeaves();
			int ID = ((vtkIntArray) data.GetCellData().GetScalars()).GetValue(cellID);

			return leaves.get(ID);
		} else
			throw new RuntimeException("cellID out of bounds");
	}

	@Override
	protected void refreshData()
	{
		List<LeafNode> leaves = getLeaves();

		// Compute the sizes
		int nodesSize = 0;
		int verticeSize = 0;
		int linesSize = 0;
		int polysSize = 0;
		nbrOfVertice = 0;
		nbrOfLines = 0;
		nbrOfPolys = 0;
		boolean buildNormals = true;

		offSetsVertice = new TIntArrayList(leaves.size());
		offSetsLines = new TIntArrayList(leaves.size());
		offSetsPolys = new TIntArrayList(leaves.size());
		int nbrOfLeaves = leaves.size();

		for (LeafNode leaf : leaves)
		{
			offSetsVertice.add(nbrOfVertice);
			offSetsLines.add(nbrOfLines);
			offSetsPolys.add(nbrOfPolys);

			if (!leaf.isVisible())
				continue;

			LeafNode.DataProvider dataProvider = leaf.getDataProvider();

			dataProvider.load();

			nodesSize += dataProvider.getNodes().length;
			verticeSize += dataProvider.getVertice().length;
			linesSize += dataProvider.getLines().length;
			polysSize += dataProvider.getPolys().length;


			nbrOfVertice += dataProvider.getNbrOfVertice();

			nbrOfLines += dataProvider.getNbrOfLines();

			nbrOfPolys += dataProvider.getNbrOfPolys();

			if (dataProvider.getNormals() == null)
				buildNormals = false;
		}

		// If there is no nodes then there is no normals
		if (nodesSize == 0)
			buildNormals = false;

		// Compute the arrays
		float[] nodes = new float[nodesSize];
		float[] normals = null;
		if (buildNormals)
			normals = new float[nodesSize];
		int[] vertice = new int[verticeSize];
		int[] lines = new int[linesSize];
		int[] polys = new int[polysSize];
		int offSetNode = 0;
		int offSetVertice = 0;
		int offSetLine = 0;
		int offSetPoly = 0;

		table = new vtkLookupTable();
		table.SetNumberOfTableValues(leaves.size());
		table.SetTableRange(0, leaves.size());

		for (int i = 0; i < nbrOfLeaves; ++i)
		{
			LeafNode leaf = leaves.get(i);

			if (!leaf.isVisible())
				continue;

			LeafNode.DataProvider dataProvider = leaf.getDataProvider();

			final int nbrOfNode = offSetNode / 3;
			float[] nodesNode = dataProvider.getNodes();
			System.arraycopy(nodesNode, 0, nodes, offSetNode, nodesNode.length);
			if (buildNormals)
			{
				float[] normalsNode = dataProvider.getNormals();
				if (normalsNode == null)
					Arrays.fill(normals, offSetNode, offSetNode + nodesNode.length, 0.f);
				else
					System.arraycopy(normalsNode, 0, normals, offSetNode, normalsNode.length);
			}
			offSetNode += nodesNode.length;



			int[] verticeNode = dataProvider.getVertice();
			System.arraycopy(verticeNode, 0, vertice, offSetVertice, verticeNode.length);

			// Make an offSet
			for (int j = offSetVertice; j < offSetVertice + verticeNode.length;)
			{
				vertice[++j] += nbrOfNode;
				++j;
			}
			offSetVertice += verticeNode.length;

			int[] linesNode = dataProvider.getLines();
			System.arraycopy(linesNode, 0, lines, offSetLine, linesNode.length);

			// Make an offSet
			for (int j = offSetLine; j < offSetLine + linesNode.length;)
			{
				lines[++j] += nbrOfNode;
				lines[++j] += nbrOfNode;
				++j;
			}
			offSetLine += linesNode.length;

			int[] polysNode = dataProvider.getPolys();
			System.arraycopy(polysNode, 0, polys, offSetPoly, polysNode.length);

			// Make an offSet
			for (int j = offSetPoly; j < offSetPoly + polysNode.length;)
			{
				int size = polys[j++];
				for (int c = 0; c < size; ++c)
					polys[j++] += nbrOfNode;
			}
			offSetPoly += polysNode.length;

			Color color = leaf.getColor();
			table.SetTableValue(i, (double) color.getRed() / 255., (double) color.getGreen() / 255., (double) color.getBlue() / 255., (double) color.getAlpha() / 255.);

			dataProvider.unLoad();
		}

		// Compute the id association array
		int[] ids = new int[nbrOfVertice + nbrOfLines + nbrOfPolys];
		for (int i = 0; i < nbrOfLeaves; ++i)
		{
			// Vertice part
			int begin = offSetsVertice.get(i);
			int end = 0;
			if (i == nbrOfLeaves - 1)
				end = nbrOfVertice;
			else
				end = offSetsVertice.get(i + 1);
			Arrays.fill(ids, begin, end, i);

			// Line part
			begin = nbrOfVertice;
			end = nbrOfVertice;
			begin += offSetsLines.get(i);
			if (i == nbrOfLeaves - 1)
				end += nbrOfLines;
			else
				end += offSetsLines.get(i + 1);
			Arrays.fill(ids, begin, end, i);

			// Poly part
			begin = nbrOfVertice + nbrOfLines;
			end = nbrOfVertice + nbrOfLines;
			begin += offSetsPolys.get(i);
			if (i == nbrOfLeaves - 1)
				end += nbrOfPolys;
			else
				end += offSetsPolys.get(i + 1);
			Arrays.fill(ids, begin, end, i);
		}

		NodeData nodeData = new NodeData(nodes, normals, nbrOfVertice, vertice, nbrOfLines, lines, nbrOfPolys, polys);

		createData(nodeData);

		vtkIntArray idsNative = new vtkIntArray();
		idsNative.SetJavaArray(ids);
		data.GetCellData().SetScalars(idsNative);
		//System.out.println("number of cells : " + data.GetNumberOfCells());
	}

	@Override
	protected void deleteDatas()
	{
		super.deleteDatas();
		offSetsVertice = null;
		offSetsLines = null;
		offSetsPolys = null;
		table = null;
		if (highLighter != null)
		{
			fireActorDeleted(highLighter);
			highLighter = null;
		}
	}

	private int getNbrOfCells()
	{
		return nbrOfVertice + nbrOfLines + nbrOfPolys;
	}

	@Override
	protected void highLight()
	{
		mapper.ScalarVisibilityOff();
		if(highLighter != null)
			highLighter.VisibilityOff();

		super.highLight();
	}

	@Override
	protected void unHighLight()
	{
		mapper.ScalarVisibilityOn();
		if(highLighter != null)
			highLighter.VisibilityOn();
		
		super.unHighLight();
	}

	
	
	protected void manageHighLight()
	{
		// Make sure the leaves are refreshed...
		List<LeafNode> leaves = getLeaves();

		if (isManager())
		{
			// If the nodes are selected select all cells of the node
			TIntArrayList selection = new TIntArrayList(getNbrOfCells());
			for (int i = 0; i < leaves.size(); ++i)
			{
				LeafNode leaf = leaves.get(i);

				if (!leaf.isSelected())
					continue;

				// Add vertice
				int begin = offSetsVertice.get(i);
				int end = (i + 1 < leaves.size()) ? offSetsVertice.get(i + 1) : nbrOfVertice;
				for (int j = begin; j < end; ++j)
					selection.add(j);

				// Add lines
				begin = nbrOfVertice;
				end = nbrOfVertice;
				begin += offSetsLines.get(i);
				end += (i + 1 < leaves.size()) ? offSetsLines.get(i + 1) : nbrOfLines;
				for (int j = begin; j < end; ++j)
					selection.add(j);

				// Add polys
				begin = nbrOfVertice + nbrOfLines;
				end = nbrOfVertice + nbrOfLines;
				begin += offSetsPolys.get(i);
				end += (i + 1 < leaves.size()) ? offSetsPolys.get(i + 1) : nbrOfPolys;
				for (int j = begin; j < end; ++j)
					selection.add(j);
			}

			if (selection.size() == 0)
			{
				if (highLighter != null)
				{
					fireActorDeleted(highLighter);
					highLighter = null;
				}
			}
			else
			{
				boolean actorCreated = false;
				
				if (highLighter == null)
				{
					actorCreated = true;
					highLighter = new vtkActor();
					highLighter.PickableOff();
					
					getActorHighLightedCustomiser().customiseActorHighLighted(highLighter);
				}

				vtkPolyDataMapper mapperHighLighter = new vtkPolyDataMapper();
				mapperHighLighter.ScalarVisibilityOff();
				mapperHighLighter.SetResolveCoincidentTopologyToPolygonOffset();
				mapperHighLighter.SetResolveCoincidentTopologyPolygonOffsetParameters(-Utils.getOffSetFactor(), -Utils.getOffSetValue());
				mapperHighLighter.SetInput(selectInto(data, selection.toNativeArray()));
				highLighter.SetMapper(mapperHighLighter);

				getMapperHighLightedCustomiser().customiseMapperHighLighted(mapperHighLighter);
				
				if (actorCreated)
				{
					fireActorCreated(highLighter);
					fireActorHighLighted(highLighter);
				}
			}

			if (lastUpdate <= selectionTime())
				if (this.isSelected())
				{
					highLight();
				}
				else
					unHighLight();
		}
		
	}

	private final int nodeIndiceToLeafIndice(int leaf, int indice)
	{
		if (0 <= indice && indice < nbrOfVertice)
			return indice - offSetsVertice.getQuick(leaf);

		indice -= nbrOfVertice;
		if (0 <= indice && indice < nbrOfLines)
			return indice - offSetsLines.getQuick(leaf);

		indice -= nbrOfLines;
		if (0 <= indice && indice < nbrOfPolys)
			return indice - offSetsPolys.getQuick(leaf);

		throw new IllegalArgumentException("The indice is not good !");
	}

	private final int leafIndiceToNodeIndice(LeafNode leaf, int leafIndice, int indice)
	{
		LeafNode.DataProvider dataProvider = leaf.getDataProvider();
		int nbrOfVerticeLeaf = dataProvider.getNbrOfVertice();
		int nbrOfLinesLeaf = dataProvider.getNbrOfLines();
		int nbrOfPolysLeaf = dataProvider.getNbrOfPolys();

		if (0 <= indice && indice < nbrOfVerticeLeaf)
			return indice + offSetsVertice.getQuick(leafIndice);

		indice -= nbrOfVerticeLeaf;

		if (0 <= indice && indice < nbrOfLinesLeaf)
			return indice + nbrOfVertice + offSetsLines.getQuick(leafIndice);

		indice -= nbrOfLinesLeaf;
		if (0 <= indice && indice < nbrOfPolysLeaf)
			return indice + nbrOfVertice + nbrOfLines + offSetsPolys.getQuick(leafIndice);

		throw new IllegalArgumentException("The indice is not good !");
	}

	private vtkPolyData selectInto(vtkPolyData input, int[] cellID)
	{
		vtkSelection sel = new vtkSelection();
		//sel.ReleaseDataFlagOn();
		sel.GetProperties().Set(sel.CONTENT_TYPE(), 4); // 4 MEANS INDICES (see the enumeration)

		sel.GetProperties().Set(sel.FIELD_TYPE(), 0); // 0 MEANS CELLS

		// list of cells to be selected
		vtkIdTypeArray arr = Utils.setValues(cellID);
		sel.SetSelectionList(arr);

		vtkExtractSelectedPolyDataIds selFilter = new vtkExtractSelectedPolyDataIds();
		selFilter.ReleaseDataFlagOn();
		selFilter.SetInput(1, sel);
		selFilter.SetInput(0, input);

		vtkPolyData dataFiltered = selFilter.GetOutput();
		selFilter.Update();

		return dataFiltered;
	}

	public void highLightSelection()
	{
		if (!isManager())
		{
			for (AbstractNode child : children)
				child.highLightSelection();
			return;
		}

		boolean actorCreated = false;
		
		if (selectionHighLighter == null)
		{
			actorCreated = true;
			selectionHighLighter = new vtkActor();
			selectionHighLighter.PickableOff();
			getActorSelectionCustomiser().customiseActorSelection(selectionHighLighter);
		}

		vtkPolyDataMapper selectionMapper = new vtkPolyDataMapper();
		selectionHighLighter.SetMapper(selectionMapper);
		selectionMapper.ScalarVisibilityOff();
		selectionMapper.SetResolveCoincidentTopologyToPolygonOffset();
		selectionMapper.SetResolveCoincidentTopologyPolygonOffsetParameters(-Utils.getOffSetFactor(), -Utils.getOffSetValue());
		
		// Compute the list of cells to be selected
		List<LeafNode> leaves = getLeaves();
		TIntArrayList selection = new TIntArrayList();

		for (int i = 0; i < leaves.size(); ++i)
		{
			LeafNode leaf = leaves.get(i);
			int offSet = selection.size();

			int[] cellSelection = leaf.getSelection().toNativeArray();
			// Add the leaf selection to the selection
			selection.add(cellSelection);
			// Add the offSet of the leaf to the selection
			for (int j = offSet; j < selection.size(); ++j)
				selection.setQuick(j, leafIndiceToNodeIndice(leaf, i, selection.getQuick(j)));
		}

		if (selection.size() == 0)
		{
			unHighLightSelection();
			return;
		}

		selectionMapper.SetInput(selectInto(data, selection.toNativeArray()));
		
		getMapperSelectionCustomiser().customiseMapperSelection(selectionMapper);
		
		if(actorCreated)
		{
			fireActorCreated(selectionHighLighter);
			fireActorHighLighted(selectionHighLighter);			
		}		
	}

	public vtkPolyData temp()
	{

		return data;
	}

	@Override
	public void refresh()
	{
		//int size = 0 ;
		for (AbstractNode child : children)
			child.refresh();

		List<LeafNode> leaves = getLeaves();
		
		if (isManager())
		{
			if (lastUpdate <= getModificationTime())
			{
				//System.out.println("REFRESH ACTOR");
				refreshActor();
			}
			else
				for (LeafNode leaf : leaves)
					if (lastUpdate <= leaf.getModificationTime())
					{
						//System.out.println("REFRESH ACTOR BECAUSE OF LEAF !");
						refreshActor();
						break;
					}
			manageHighLight();
		}

		lastUpdate = System.nanoTime();
	}

	public List<AbstractNode> getChildren()
	{
		return this.children;
	}
	
	public List<LeafNode> getLeaves()
	{
		// Do not keep the leaves, just compute
		ArrayList<LeafNode> toReturn = new ArrayList<LeafNode>();

		for (AbstractNode child : children)
			toReturn.addAll(child.getLeaves());

		return toReturn;
	}

	@Override
	public void setVisible(boolean visible)
	{
		for (AbstractNode child : children)
			child.setVisible(visible);

		super.setVisible(visible);
	}

	protected void manageSelection(int[] cellSelection)
	{
		if (actor == null)
			throw new RuntimeException("The Node has to be a manager to manage the selection");

		//System.out.println("SCALARS : " + data.GetCellData().GetNumberOfArrays());
		int[] ids = ((vtkIntArray) data.GetCellData().GetScalars()).GetJavaArray();

		List<LeafNode> leaves = getLeaves();
		ArrayList<TIntArrayList> selectionChildren = new ArrayList<TIntArrayList>(leaves.size());
		for (int i = 0; i < leaves.size(); ++i)
			selectionChildren.add(new TIntArrayList());

		// Compute the selections
		for (int cellID : cellSelection)
		{
			int nodeID = ids[cellID];

			selectionChildren.get(nodeID).add(nodeIndiceToLeafIndice(nodeID, cellID));
		}

		// Send the selections to the children
		for (int i = 0; i < leaves.size(); ++i)
			leaves.get(i).setSelection(selectionChildren.get(i));
	}
}
