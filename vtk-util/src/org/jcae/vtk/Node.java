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
import java.util.logging.Level;
import java.util.logging.Logger;
import vtk.vtkActor;
import vtk.vtkExtractSelectedPolyDataIds;
import vtk.vtkIdTypeArray;
import vtk.vtkIntArray;
import vtk.vtkLookupTable;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkSelection;

/**
 *
 * @author Julian Ibarz
 */
public class Node extends AbstractNode
{
	private static final Logger LOGGER = Logger.getLogger(Node.class.getName());
	
	private ArrayList<AbstractNode> children = new ArrayList<AbstractNode>();
	// Datas if the node manage
	private TIntArrayList offsetsVertices;
	private TIntArrayList offsetsLines;
	private TIntArrayList offsetsPolys;
	private int nbrOfVertice;
	private int nbrOfLines;
	private int nbrOfPolys;
	private vtkLookupTable table;
	private vtkActor highlighter;
	private vtkPolyDataMapper highlighterMapper;
	private ArrayList<ChildCreationListener> childCreationListeners = new ArrayList<ChildCreationListener>();

	private static class NodeData extends LeafNode.DataProvider
	{
		NodeData(float[] nodes, float[] normals, int nbrOfVertice, int[] vertice, int nbrOfLines, int[] lines, int nbrOfPolys, int[] polys)
		{
			this.nodes = nodes;
			this.normals = normals;
			this.nbrOfVertices = nbrOfVertice;
			this.vertices = vertice;
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
		public int[] getVertices()
		{
			return vertices;
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

	public void setPickableRecursive(boolean pickable)
	{
		super.setPickable(pickable);
		
		for(AbstractNode child : children)
		{
			if(child instanceof Node)
				((Node)child).setPickableRecursive(pickable);
			else
				child.setPickable(pickable);
		}
	}
	
	public List<AbstractNode> getChildren()
	{
		return children;
	}
	
	public List<LeafNode> getLeaves()
	{
		// Do not keep the leaves, just compute
		ArrayList<LeafNode> toReturn = new ArrayList<LeafNode>();

		for (AbstractNode child : children)
			toReturn.addAll(child.getLeaves());

		return toReturn;
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

	public void removeChild(AbstractNode child)
	{
		if (children.remove(child))
		{
			child.deleteDatas();
			fireChildDeletionListener(child);
			modified();
		}
	}
	
	public int numChildren()
	{
		return children.size();
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
	public void applyActorCustomiser()
	{
		if(isManager())
			super.applyActorCustomiser();
		
		for(AbstractNode child : children)
		{
			child.setActorCustomiser(actorCustomiser);
			child.applyActorCustomiser();
		}
	}

	@Override
	public void applyActorSelectionCustomiser()
	{
		if(isManager())
			super.applyActorSelectionCustomiser();
		for(AbstractNode child : children)
		{
			child.setActorSelectionCustomiser(actorSelectionCustomiser);
			child.applyActorSelectionCustomiser();
		}
	}

	@Override
	public void applyMapperCustomiser()
	{
		if(isManager())
			super.applyMapperCustomiser();
		for(AbstractNode child : children)
		{
			child.setMapperCustomiser(mapperCustomiser);
			child.applyMapperCustomiser();
		}
	}

	@Override
	public void applyMapperSelectionCustomiser()
	{
		if(isManager())
			super.applyMapperSelectionCustomiser();
		for(AbstractNode child : children)
		{
			child.setMapperSelectionCustomiser(mapperSelectionCustomiser);
			child.applyMapperSelectionCustomiser();
		}
	}
	
	@Override
	public void applyActorHighlightedCustomiser()
	{
		if(highlighter != null)
			getActorHighlightedCustomiser().customiseActorHighlighted(highlighter);
		for(AbstractNode child : children)
		{
			child.setActorHighlightedCustomiser(actorHighlightedCustomiser);
			child.applyActorHighlightedCustomiser();
		}
	}

	@Override
	public void applyMapperHighlightedCustomiser()
	{
		if(highlighterMapper != null)
			getMapperHighlightedCustomiser().customiseMapperHighlighted(highlighterMapper);
		for(AbstractNode child : children)
		{
			child.setMapperHighlightedCustomiser(mapperHighlightedCustomiser);
			child.applyMapperHighlightedCustomiser();
		}
	}

	@Override
	public void setVisible(boolean visible)
	{
		for (AbstractNode child : children)
			child.setVisible(visible);

		super.setVisible(visible);
	}

	@Override
	public void refresh()
	{
		for (AbstractNode child : children)
			child.refresh();
		if (!isManager())
		{
			lastUpdate = System.nanoTime();
			return;
		}

		if (lastUpdate <= getModificationTime())
		{
			refreshData();
			refreshActor();
		}
		else
		{
			for (LeafNode leaf : getLeaves())
			{
				if (lastUpdate <= leaf.getModificationTime())
				{
					refreshData();
					refreshActor();
					break;
				}
			}
		}
		manageHighlight();

		lastUpdate = System.nanoTime();
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

	@Override
	protected void refreshData()
	{
		// Compute the sizes
		int nodesSize = 0;
		int verticeSize = 0;
		int linesSize = 0;
		int polysSize = 0;
		nbrOfVertice = 0;
		nbrOfLines = 0;
		nbrOfPolys = 0;
		boolean buildNormals = true;

		List<LeafNode> leaves = getLeaves();
		int numberOfLeaves = leaves.size();
		offsetsVertices = new TIntArrayList(numberOfLeaves + 1);
		offsetsLines    = new TIntArrayList(numberOfLeaves + 1);
		offsetsPolys    = new TIntArrayList(numberOfLeaves + 1);

		for (LeafNode leaf : leaves)
		{
			offsetsVertices.add(nbrOfVertice);
			offsetsLines.add(nbrOfLines);
			offsetsPolys.add(nbrOfPolys);

			if (!leaf.isVisible())
				continue;

			LeafNode.DataProvider dataProvider = leaf.getDataProvider();

			dataProvider.load();

			nodesSize += dataProvider.getNodes().length;
			verticeSize += dataProvider.getVertices().length;
			linesSize += dataProvider.getLines().length;
			polysSize += dataProvider.getPolys().length;


			nbrOfVertice += dataProvider.getNbrOfVertices();

			nbrOfLines += dataProvider.getNbrOfLines();

			nbrOfPolys += dataProvider.getNbrOfPolys();

			if (dataProvider.getNormals() == null)
				buildNormals = false;
		}
		offsetsVertices.add(nbrOfVertice);
		offsetsLines.add(nbrOfLines);
		offsetsPolys.add(nbrOfPolys);

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

		for (int i = 0; i < numberOfLeaves; ++i)
		{
			LeafNode leaf = leaves.get(i);

			if (!leaf.isVisible())
				continue;

			LeafNode.DataProvider dataProvider = leaf.getDataProvider();

			final int numberOfNode = offSetNode / 3;
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



			int[] verticeNode = dataProvider.getVertices();
			System.arraycopy(verticeNode, 0, vertice, offSetVertice, verticeNode.length);

			// Make an offset
			for (int j = offSetVertice; j < offSetVertice + verticeNode.length;)
			{
				vertice[++j] += numberOfNode;
				++j;
			}
			offSetVertice += verticeNode.length;

			int[] linesNode = dataProvider.getLines();
			System.arraycopy(linesNode, 0, lines, offSetLine, linesNode.length);

			// Make an offset
			for (int j = offSetLine; j < offSetLine + linesNode.length;)
			{
				lines[++j] += numberOfNode;
				lines[++j] += numberOfNode;
				++j;
			}
			offSetLine += linesNode.length;

			int[] polysNode = dataProvider.getPolys();
			System.arraycopy(polysNode, 0, polys, offSetPoly, polysNode.length);

			// Make an offset
			for (int j = offSetPoly; j < offSetPoly + polysNode.length;)
			{
				int size = polys[j++];
				for (int c = 0; c < size; ++c)
					polys[j++] += numberOfNode;
			}
			offSetPoly += polysNode.length;

			Color color = leaf.getColor();
			if (LOGGER.isLoggable(Level.FINEST))
				LOGGER.finest("Compound: set color to "+color+" (opacity="+color.getAlpha()+")");
			table.SetTableValue(i, (double) color.getRed() / 255., (double) color.getGreen() / 255., (double) color.getBlue() / 255., (double) color.getAlpha() / 255.);

			dataProvider.unLoad();
		}

		// Compute the id association array
		int[] ids = new int[nbrOfVertice + nbrOfLines + nbrOfPolys];
		for (int leafIndex = 0; leafIndex < numberOfLeaves; ++leafIndex)
		{
			// Vertice part
			int begin = offsetsVertices.get(leafIndex);
			int end = offsetsVertices.get(leafIndex + 1);
			Arrays.fill(ids, begin, end, leafIndex);

			// Line part
			begin = nbrOfVertice + offsetsLines.get(leafIndex);
			end = nbrOfVertice + offsetsLines.get(leafIndex + 1);
			Arrays.fill(ids, begin, end, leafIndex);

			// Poly part
			begin = nbrOfVertice + nbrOfLines + offsetsPolys.get(leafIndex);
			end = nbrOfVertice + nbrOfLines + offsetsPolys.get(leafIndex + 1);
			Arrays.fill(ids, begin, end, leafIndex);
		}

		NodeData nodeData = new NodeData(nodes, normals, nbrOfVertice, vertice, nbrOfLines, lines, nbrOfPolys, polys);

		createData(nodeData);

		vtkIntArray idsNative = new vtkIntArray();
		idsNative.SetJavaArray(ids);
		data.GetCellData().SetScalars(idsNative);
	}

	@Override
	protected void deleteDatas()
	{
		super.deleteDatas();
		offsetsVertices = null;
		offsetsLines = null;
		offsetsPolys = null;
		table = null;
		if (highlighter != null)
		{
			fireActorDeleted(highlighter);
			highlighter = null;
			highlighterMapper = null;
		}
		for(AbstractNode n : getChildren())
			n.deleteDatas();
	}

	private int getNbrOfCells()
	{
		return nbrOfVertice + nbrOfLines + nbrOfPolys;
	}

	protected void manageHighlight()
	{
		if (!isManager())
			return;

		// If the nodes are selected select all cells of the node
		TIntArrayList selection = new TIntArrayList(getNbrOfCells());
		int i = -1;
		for (LeafNode leaf : getLeaves())
		{
			i++;
			if (!leaf.isSelected())
				continue;

			// Add vertices
			int begin = offsetsVertices.get(i);
			int end = offsetsVertices.get(i + 1);
			for (int j = begin; j < end; ++j)
				selection.add(j);

			// Add lines
			begin = nbrOfVertice;
			end = nbrOfVertice;
			begin += offsetsLines.get(i);
			end += offsetsLines.get(i + 1);
			for (int j = begin; j < end; ++j)
				selection.add(j);

			// Add polys
			begin = nbrOfVertice + nbrOfLines;
			end = nbrOfVertice + nbrOfLines;
			begin += offsetsPolys.get(i);
			end += offsetsPolys.get(i + 1);
			for (int j = begin; j < end; ++j)
				selection.add(j);
		}

		if (selection.isEmpty())
		{
			if (highlighter != null)
			{
				fireActorDeleted(highlighter);
				highlighter = null;
			}
		}
		else
		{
			boolean actorCreated = false;
			
			if (highlighter == null)
			{
				actorCreated = true;
				highlighter = new vtkActor();
				highlighter.PickableOff();
				
				getActorHighlightedCustomiser().customiseActorHighlighted(highlighter);
			}

			highlighterMapper = new vtkPolyDataMapper();
			highlighterMapper.ScalarVisibilityOff();
			highlighterMapper.SetInput(selectInto(data, selection.toNativeArray()));
			highlighter.SetMapper(highlighterMapper);

			getMapperHighlightedCustomiser().customiseMapperHighlighted(highlighterMapper);
			
			if (actorCreated)
			{
				fireActorCreated(highlighter);
			}
		}

		if (lastUpdate <= selectionTime())
		{
			if (this.isSelected())
				highlight();
			else
				unHighlight();
		}
	}

	@Override
	protected void highlight()
	{
		mapper.ScalarVisibilityOff();
		if(highlighter != null)
			highlighter.VisibilityOff();

		super.highlight();
	}

	@Override
	protected void unHighlight()
	{
		mapper.ScalarVisibilityOn();
		if(highlighter != null)
			highlighter.VisibilityOn();
		
		super.unHighlight();
	}

	public void highlightSelection()
	{
		if (!isManager())
		{
			for (AbstractNode child : children)
				child.highlightSelection();
			return;
		}

		boolean actorCreated = false;
		
		if (selectionHighlighter == null)
		{
			actorCreated = true;
			selectionHighlighter = new vtkActor();
			selectionHighlighter.PickableOff();
			getActorSelectionCustomiser().customiseActorSelection(selectionHighlighter);
		}

		selectionHighlighterMapper = new vtkPolyDataMapper();
		selectionHighlighter.SetMapper(selectionHighlighterMapper);
		selectionHighlighterMapper.ScalarVisibilityOff();
		
		// Compute the list of cells to be selected
		TIntArrayList selection = new TIntArrayList();

		int leafIndex = -1;
		for (LeafNode leaf : getLeaves())
		{
			leafIndex++;
			int[] cellSelection = leaf.getSelection().toNativeArray();
			if (cellSelection.length == 0)
				continue;
			
			int offset = selection.size();
			selection.ensureCapacity(offset+cellSelection.length);
			for (int j = 0; j < cellSelection.length; ++j)
				selection.add(leafIndexToNodeIndex(leaf, leafIndex, cellSelection[j]));
		}

		if (selection.isEmpty())
		{
			unHighlightSelection();
			return;
		}

		selectionHighlighterMapper.SetInput(selectInto(data, selection.toNativeArray()));
		
		getMapperSelectionCustomiser().customiseMapperSelection(selectionHighlighterMapper);
		
		if(actorCreated)
		{
			fireActorCreated(selectionHighlighter);
		}
	}

	private final int nodeIndexToLeafIndex(int leaf, int index)
	{
		if (0 <= index && index < nbrOfVertice)
			return index - offsetsVertices.getQuick(leaf);

		index -= nbrOfVertice;
		if (0 <= index && index < nbrOfLines)
			return index - offsetsLines.getQuick(leaf);

		index -= nbrOfLines;
		if (0 <= index && index < nbrOfPolys)
			return index - offsetsPolys.getQuick(leaf);

		throw new IllegalArgumentException("Wrong index: "+index);
	}

	private final int leafIndexToNodeIndex(LeafNode leaf, int leafIndex, int index)
	{
		LeafNode.DataProvider leafDataProvider = leaf.getDataProvider();
		int numberOfVerticeLeaf = leafDataProvider.getNbrOfVertices();
		int numberOfLinesLeaf = leafDataProvider.getNbrOfLines();
		int numberOfPolysLeaf = leafDataProvider.getNbrOfPolys();

		if (0 <= index && index < numberOfVerticeLeaf)
			return index + offsetsVertices.getQuick(leafIndex);

		index -= numberOfVerticeLeaf;

		if (0 <= index && index < numberOfLinesLeaf)
			return index + nbrOfVertice + offsetsLines.getQuick(leafIndex);

		index -= numberOfLinesLeaf;
		if (0 <= index && index < numberOfPolysLeaf)
			return index + nbrOfVertice + nbrOfLines + offsetsPolys.getQuick(leafIndex);

		throw new IllegalArgumentException("Wrong index: "+index);
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

	void setCellSelection(TIntArrayList cellSelection)
	{
		if (actor == null)
			throw new RuntimeException("The Node has to be a manager to manage the selection");

		int[] ids = ((vtkIntArray) data.GetCellData().GetScalars()).GetJavaArray();

		List<LeafNode> leaves = getLeaves();
		ArrayList<TIntArrayList> selectionChildren = new ArrayList<TIntArrayList>(leaves.size());
		for (int i = 0; i < leaves.size(); ++i)
			selectionChildren.add(new TIntArrayList());
		// Compute the selections
		
		for (int i = 0, n = cellSelection.size(); i < n; i++)
		{
			int cellID = cellSelection.get(i);
			int nodeID = ids[cellID];

			selectionChildren.get(nodeID).add(nodeIndexToLeafIndex(nodeID, cellID));
		}

		// Send the selections to the children
		for (int i = 0; i < leaves.size(); ++i)
			leaves.get(i).setCellSelection(selectionChildren.get(i));
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder(super.toString());
		if (highlighter != null)
		{
			sb.append(" highlighter@"+Integer.toHexString(highlighter.hashCode()));
			if (highlighter.GetVisibility() != 0)
				sb.append(" visible");
			if (highlighter.GetPickable() != 0)
				sb.append(" pickable");
		}
		return sb.toString();
	}
}
