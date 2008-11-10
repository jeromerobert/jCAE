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
	private int nbrOfVertices;
	private int nbrOfLines;
	private int nbrOfPolys;
	
	// When a Node is managing, geometry for all leaves is contained in
	// AbstractNode.actor.  Thus it can not be used to highlight only some
	// leaves, another vtkActor is needed for this purpose.
	// FIXME: See how to use selectionHighlighter instead of a new vtkActor
	private vtkActor highlighter;
	private vtkPolyDataMapper highlighterMapper;
	// Lookup table for color of leaves
	private vtkLookupTable table;
	
	private ArrayList<ChildCreationListener> childCreationListeners = new ArrayList<ChildCreationListener>();

	private static class NodeData extends LeafNode.DataProvider
	{
		NodeData(float[] nodes, float[] normals, int nbrOfVertices, int[] vertices, int nbrOfLines, int[] lines, int nbrOfPolys, int[] polys)
		{
			this.nodes = nodes;
			this.normals = normals;
			this.nbrOfVertices = nbrOfVertices;
			this.vertices = vertices;
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

	public Node(Node parent)
	{
		super(parent);
		
		if(parent != null)
		{
			parent.addChild(this);
			// Inherits child creation listeners
			childCreationListeners.addAll(parent.childCreationListeners);
		}
	}

	LeafNode getLeafNodeFromCell(int cellID)
	{
		if (!isManager())
			throw new RuntimeException("Node is not a manager");
		if (cellID < 0 || cellID >= data.GetNumberOfCells())
			throw new RuntimeException("cellID out of bounds");
		
		List<LeafNode> leaves = getLeaves();
		int ID = ((vtkIntArray) data.GetCellData().GetScalars()).GetValue(cellID);

		return leaves.get(ID);
	}

	/**
	 * Set pickable the actor of the node. If the node is not a manager,
	 * it processes its children recursively.
	 * @param pickable
	 */
	@Override
	public void setPickable(boolean pickable)
	{
		if (isManager())
		{
			super.setPickable(pickable);
			return;
		}

		for(AbstractNode child : children)
			child.setPickable(pickable);
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
			for(ChildCreationListener listener : childCreationListeners)
				listener.childCreated(child);
			modified();
		}
	}

	public void removeChild(AbstractNode child)
	{
		if (children.remove(child))
		{
			child.deleteDatas();
			for(ChildCreationListener listener : childCreationListeners)
				listener.childDeleted(child);
			modified();
		}
	}

	public void removeAllChildren()
	{
		ArrayList<AbstractNode> savedList = new ArrayList<AbstractNode>(children);
		for (AbstractNode child : new ArrayList<AbstractNode>(children))
			removeChild(child);
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

	@Override
	public void applyActorCustomiser()
	{
		if(isManager())
		{
			super.applyActorCustomiser();
			return;
		}
		
		for(AbstractNode child : children)
		{
			child.setActorCustomiser(actorCustomiser);
			child.applyActorCustomiser();
		}
	}

	@Override
	public void applyMapperCustomiser()
	{
		if(isManager())
		{
			super.applyMapperCustomiser();
			return;
		}
		
		for(AbstractNode child : children)
		{
			child.setMapperCustomiser(mapperCustomiser);
			child.applyMapperCustomiser();
		}
	}

	@Override
	public void applyActorHighlightedCustomiser()
	{
		if (isManager())
		{
			if(highlighter != null)
				getActorHighlightedCustomiser().customiseActorHighlighted(highlighter);
			return;
		}
		
		for(AbstractNode child : children)
		{
			child.setActorHighlightedCustomiser(actorHighlightedCustomiser);
			child.applyActorHighlightedCustomiser();
		}
	}

	@Override
	public void applyMapperHighlightedCustomiser()
	{
		if (isManager())
		{
			if(highlighterMapper != null)
				getMapperHighlightedCustomiser().customiseMapperHighlighted(highlighterMapper);
			return;
		}
		
		for(AbstractNode child : children)
		{
			child.setMapperHighlightedCustomiser(mapperHighlightedCustomiser);
			child.applyMapperHighlightedCustomiser();
		}
	}

	@Override
	public void applyActorSelectionCustomiser()
	{
		if(isManager())
		{
			super.applyActorSelectionCustomiser();
			return;
		}
		
		for(AbstractNode child : children)
		{
			child.setActorSelectionCustomiser(actorSelectionCustomiser);
			child.applyActorSelectionCustomiser();
		}
	}

	@Override
	public void applyMapperSelectionCustomiser()
	{
		if(isManager())
		{
			super.applyMapperSelectionCustomiser();
			return;
		}
		
		for(AbstractNode child : children)
		{
			child.setMapperSelectionCustomiser(mapperSelectionCustomiser);
			child.applyMapperSelectionCustomiser();
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

		if (lastUpdate <= modificationTime)
		{
			refreshData();
			refreshActor();
		}
		else
		{
			for (LeafNode leaf : getLeaves())
			{
				if (lastUpdate <= leaf.modificationTime)
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

	private void refreshData()
	{
		// Compute the sizes
		int nodesSize = 0;
		int verticesSize = 0;
		int linesSize = 0;
		int polysSize = 0;
		nbrOfVertices = 0;
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
			offsetsVertices.add(nbrOfVertices);
			offsetsLines.add(nbrOfLines);
			offsetsPolys.add(nbrOfPolys);

			if (!leaf.isVisible())
				continue;

			LeafNode.DataProvider dataProvider = leaf.getDataProvider();

			dataProvider.load();

			nodesSize += dataProvider.getNodes().length;
			verticesSize += dataProvider.getVertices().length;
			linesSize += dataProvider.getLines().length;
			polysSize += dataProvider.getPolys().length;


			nbrOfVertices += dataProvider.getNbrOfVertices();

			nbrOfLines += dataProvider.getNbrOfLines();

			nbrOfPolys += dataProvider.getNbrOfPolys();

			if (dataProvider.getNormals() == null)
				buildNormals = false;
		}
		offsetsVertices.add(nbrOfVertices);
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
		int[] vertices = new int[verticesSize];
		int[] lines = new int[linesSize];
		int[] polys = new int[polysSize];
		int offsetNode = 0;
		int offsetVertex = 0;
		int offsetLine = 0;
		int offsetPoly = 0;

		table = new vtkLookupTable();
		table.SetNumberOfTableValues(leaves.size());
		table.SetTableRange(0, leaves.size());

		for (int i = 0; i < numberOfLeaves; ++i)
		{
			LeafNode leaf = leaves.get(i);

			if (!leaf.isVisible())
				continue;

			LeafNode.DataProvider dataProvider = leaf.getDataProvider();

			final int numberOfNode = offsetNode / 3;
			float[] nodesNode = dataProvider.getNodes();
			System.arraycopy(nodesNode, 0, nodes, offsetNode, nodesNode.length);
			if (buildNormals)
			{
				float[] normalsNode = dataProvider.getNormals();
				if (normalsNode == null)
					Arrays.fill(normals, offsetNode, offsetNode + nodesNode.length, 0.f);
				else
					System.arraycopy(normalsNode, 0, normals, offsetNode, normalsNode.length);
			}
			offsetNode += nodesNode.length;



			int[] verticesNode = dataProvider.getVertices();
			System.arraycopy(verticesNode, 0, vertices, offsetVertex, verticesNode.length);

			// Make an offset
			for (int j = offsetVertex; j < offsetVertex + verticesNode.length;)
			{
				vertices[++j] += numberOfNode;
				++j;
			}
			offsetVertex += verticesNode.length;

			int[] linesNode = dataProvider.getLines();
			System.arraycopy(linesNode, 0, lines, offsetLine, linesNode.length);

			// Make an offset
			for (int j = offsetLine; j < offsetLine + linesNode.length;)
			{
				lines[++j] += numberOfNode;
				lines[++j] += numberOfNode;
				++j;
			}
			offsetLine += linesNode.length;

			int[] polysNode = dataProvider.getPolys();
			System.arraycopy(polysNode, 0, polys, offsetPoly, polysNode.length);

			// Make an offset
			for (int j = offsetPoly; j < offsetPoly + polysNode.length;)
			{
				int size = polys[j++];
				for (int c = 0; c < size; ++c)
					polys[j++] += numberOfNode;
			}
			offsetPoly += polysNode.length;

			Color color = leaf.getColor();
			if (LOGGER.isLoggable(Level.FINEST))
				LOGGER.finest("Compound: set color to "+color+" (opacity="+color.getAlpha()+")");
			table.SetTableValue(i, (double) color.getRed() / 255., (double) color.getGreen() / 255., (double) color.getBlue() / 255., (double) color.getAlpha() / 255.);

			dataProvider.unLoad();
		}

		// Compute the id association array
		int[] ids = new int[nbrOfVertices + nbrOfLines + nbrOfPolys];
		for (int leafIndex = 0; leafIndex < numberOfLeaves; ++leafIndex)
		{
			// Vertex part
			int begin = offsetsVertices.get(leafIndex);
			int end = offsetsVertices.get(leafIndex + 1);
			Arrays.fill(ids, begin, end, leafIndex);

			// Line part
			begin = nbrOfVertices + offsetsLines.get(leafIndex);
			end = nbrOfVertices + offsetsLines.get(leafIndex + 1);
			Arrays.fill(ids, begin, end, leafIndex);

			// Poly part
			begin = nbrOfVertices + nbrOfLines + offsetsPolys.get(leafIndex);
			end = nbrOfVertices + nbrOfLines + offsetsPolys.get(leafIndex + 1);
			Arrays.fill(ids, begin, end, leafIndex);
		}

		NodeData nodeData = new NodeData(nodes, normals, nbrOfVertices, vertices, nbrOfLines, lines, nbrOfPolys, polys);

		createData(nodeData);

		vtkIntArray idsNative = new vtkIntArray();
		idsNative.SetJavaArray(ids);
		data.GetCellData().SetScalars(idsNative);

		if(mapper == null)
			mapper = new vtkPolyDataMapper();
		getMapperCustomiser().customiseMapper(mapper);
		mapper.SetInput(data);
		mapper.Update();		
		mapper.SetLookupTable(table);
		mapper.UseLookupTableScalarRangeOn();
		mapper.SetScalarModeToUseCellData();
	}

	// Must always be called after refreshData
	private void refreshActor()
	{
		if(actor != null)
			return;
		actor = new vtkActor();
		getActorCustomiser().customiseActor(actor);
		actor.SetMapper(mapper);
		actor.SetVisibility(Utils.booleanToInt(visible));
		actor.SetPickable(Utils.booleanToInt(pickable));
		fireActorCreated(actor);
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
		for(AbstractNode n : children)
			n.deleteDatas();
	}

	private int getNbrOfCells()
	{
		return nbrOfVertices + nbrOfLines + nbrOfPolys;
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
			begin = nbrOfVertices;
			end = nbrOfVertices;
			begin += offsetsLines.get(i);
			end += offsetsLines.get(i + 1);
			for (int j = begin; j < end; ++j)
				selection.add(j);

			// Add polys
			begin = nbrOfVertices + nbrOfLines;
			end = nbrOfVertices + nbrOfLines;
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
			int[] cellSelection = leaf.getCellSelection();
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
		if (0 <= index && index < nbrOfVertices)
			return index - offsetsVertices.getQuick(leaf);

		index -= nbrOfVertices;
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
		int numberOfVerticesLeaf = leafDataProvider.getNbrOfVertices();
		int numberOfLinesLeaf = leafDataProvider.getNbrOfLines();
		int numberOfPolysLeaf = leafDataProvider.getNbrOfPolys();

		if (0 <= index && index < numberOfVerticesLeaf)
			return index + offsetsVertices.getQuick(leafIndex);

		index -= numberOfVerticesLeaf;

		if (0 <= index && index < numberOfLinesLeaf)
			return index + nbrOfVertices + offsetsLines.getQuick(leafIndex);

		index -= numberOfLinesLeaf;
		if (0 <= index && index < numberOfPolysLeaf)
			return index + nbrOfVertices + nbrOfLines + offsetsPolys.getQuick(leafIndex);

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

	void setCellSelection(PickContext pickContext, int [] cellSelection)
	{
		if (!isManager())
			throw new RuntimeException("The Node has to be a manager to manage the selection");

		int[] ids = ((vtkIntArray) data.GetCellData().GetScalars()).GetJavaArray();

		List<LeafNode> leaves = getLeaves();
		for (LeafNode leaf : leaves)
			leaf.clearCellSelection();

		TIntArrayList [] selectionChildren = new TIntArrayList[leaves.size()];
		for (int i = 0; i < leaves.size(); ++i)
			selectionChildren[i] = new TIntArrayList();
		// Compute the selections		
		for (int cellID : cellSelection)
		{
			int nodeID = ids[cellID];
			selectionChildren[nodeID].add(nodeIndexToLeafIndex(nodeID, cellID));
		}

		// Send the selections to the children
		for (int i = 0; i < leaves.size(); ++i)
		{
			if (!selectionChildren[i].isEmpty())
				leaves.get(i).setCellSelection(pickContext, selectionChildren[i].toNativeArray());
		}
	}
	
	public void clearCellSelection()
	{
		for (LeafNode leaf : getLeaves())
			leaf.clearCellSelection();
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
