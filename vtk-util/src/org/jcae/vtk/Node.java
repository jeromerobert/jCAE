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


import gnu.trove.list.array.TIntArrayList;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import vtk.vtkActor;
import vtk.vtkAlgorithmOutput;
import vtk.vtkCellData;
import vtk.vtkExtractSelectedPolyDataIds;
import vtk.vtkIdTypeArray;
import vtk.vtkIntArray;
import vtk.vtkLookupTable;
import vtk.vtkPainterPolyDataMapper;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkSelection;
import vtk.vtkSelectionNode;

/**
 *
 * @author Julian Ibarz
 */
public class Node extends AbstractNode
{
	private static final Logger LOGGER = Logger.getLogger(Node.class.getName());
	
	private final ArrayList<AbstractNode> children = new ArrayList<AbstractNode>();
	// Datas if the node manage
	private TIntArrayList offsetsVertices;
	private TIntArrayList offsetsLines;
	private TIntArrayList offsetsPolys;
	private int nbrOfVertices;
	private int nbrOfLines;
	private int nbrOfPolys;
	
	// Lookup table for color of leaves
	private vtkLookupTable table;
	
	private final ArrayList<ChildCreationListener> childCreationListeners = new ArrayList<ChildCreationListener>();

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
	
	@Override
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
			timeStampData();
		}
	}

	public void removeChild(AbstractNode child)
	{
		if (children.remove(child))
		{
			child.deleteData();
			child.deleteSelectionActor();
			for(ChildCreationListener listener : childCreationListeners)
				listener.childDeleted(child);
			timeStampData();
		}
	}

	public void removeAllChildren()
	{
		for (AbstractNode child : new ArrayList<AbstractNode>(children))
			removeChild(child);
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
		
		List<LeafNode> leaves = null;
		if(lastUpdate <= dataTime || lastUpdate <= selectionTime)
		{
			//Loaded data provider are needed in refreshData and
			//refreshHighlight
			leaves = getLeaves();
			for (LeafNode leaf : leaves)
				leaf.getDataProvider().load();
		}

		// Were data modified?
		if (lastUpdate <= dataTime)
			refreshData(leaves);

		// Was actor modified?
		if (lastUpdate <= modificationTime)
			refreshActor();

		// Did selection happen?
		if (lastUpdate <= selectionTime)
			refreshHighlight();

		if(leaves != null)
			for (LeafNode leaf : leaves)
				leaf.getDataProvider().unLoad();

		lastUpdate = System.nanoTime();
	}

	private void refreshData(List<LeafNode> leaves)
	{
		if (LOGGER.isLoggable(Level.FINEST))
			LOGGER.finest("Refresh data for "+this);
		// Compute the sizes
		int nodesSize = 0;
		int verticesSize = 0;
		int linesSize = 0;
		int polysSize = 0;
		nbrOfVertices = 0;
		nbrOfLines = 0;
		nbrOfPolys = 0;
		boolean buildNormals = true;
		
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
		vtkCellData cellData = data.GetCellData();
		cellData.SetScalars(idsNative);
		cellData = null;
		idsNative = null;
		timeStampData();

		if(mapper == null)
		{
			mapper = new vtkPainterPolyDataMapper();
			//This should help reducing the memory footprint of display list
			//but it don't. Must be kept for further investigations.
			/*vtkInformation i = mapper.GetInformation();
			i.Set(Utils.CONSERVE_MEMORY, 1);
			i.Set(Utils.HIGH_QUALITY, 0);
			vtkDefaultPainter vdp = (vtkDefaultPainter) mapper.GetPainter();
			System.out.println(vdp.GetDisplayListPainter().GetInformation());*/
		}
		getMapperCustomiser().customiseMapper(mapper);
		mapper.SetInputData(data);
		mapper.Update();		
	}

	// Must always be called after refreshData
	private void refreshActor()
	{
		if (LOGGER.isLoggable(Level.FINEST))
			LOGGER.finest("Refresh actor for "+this);

		boolean actorCreated = (actor == null);
		if(actorCreated)
			actor = createActor();
		getActorCustomiser().customiseActor(actor);	
		actor.SetMapper(mapper);
		actor.SetVisibility(Utils.booleanToInt(visible));
		actor.SetPickable(Utils.booleanToInt(pickable));
		
		// Update mapper, colors may have changed
		List<LeafNode> leaves = getLeaves();
		int numberOfLeaves = leaves.size();
		table = new vtkLookupTable();
		table.SetNumberOfTableValues(numberOfLeaves);
		table.SetTableRange(0, numberOfLeaves);
		for (int i = 0; i < numberOfLeaves; ++i)
		{
			LeafNode leaf = leaves.get(i);
			Color color = leaf.getColor();
			if (LOGGER.isLoggable(Level.FINEST))
				LOGGER.finest("Compound: set color to "+color+" (opacity="+color.getAlpha()+")");
			table.SetTableValue(i, (double) color.getRed() / 255., (double) color.getGreen() / 255., (double) color.getBlue() / 255., (double) color.getAlpha() / 255.);
		}
		mapper.SetLookupTable(table);
		mapper.UseLookupTableScalarRangeOn();
		mapper.SetScalarModeToUseCellData();

		if (actorCreated)
		{
			fireActorCreated(actor);
			if (LOGGER.isLoggable(Level.FINEST))
				LOGGER.log(Level.FINEST, "New actor created: vtkActor@"+Integer.toHexString(actor.hashCode()));
		}
	}

	@Override
	protected void deleteData()
	{
		super.deleteData();
		offsetsVertices = null;
		offsetsLines = null;
		offsetsPolys = null;
		table = null;
		for(AbstractNode n : children)
			n.deleteData();
	}

	private void refreshHighlight()
	{
		if (LOGGER.isLoggable(Level.FINEST))
			LOGGER.log(Level.FINEST, "Refresh highlight for "+this);


		if (selected)
		{
			// The whole actor is selected, so display it
			// as highlighted.
			mapper.ScalarVisibilityOff();
			getSelectionActorCustomiser().customiseActor(actor);
			getSelectionMapperCustomiser().customiseMapper(mapper);

			deleteSelectionActor();
		}
		else
		{
			// Reset original actor colors
			mapper.ScalarVisibilityOn();
			getActorCustomiser().customiseActor(actor);
			getMapperCustomiser().customiseMapper(mapper);

			refreshSelectionActor();
		}
	}

	private void refreshSelectionActor()
	{
		TIntArrayList selection = new TIntArrayList(nbrOfVertices + nbrOfLines + nbrOfPolys);
		int leafIndex = -1;
		for (LeafNode leaf : getLeaves())
		{
			leafIndex++;
			if (leaf.selected)
			{
				// If a node is selected, select all cells

				// Vertices
				int vBegin = offsetsVertices.get(leafIndex);
				int vEnd = offsetsVertices.get(leafIndex + 1);
	
				// Lines
				int lBegin = offsetsLines.get(leafIndex) + nbrOfVertices;
				int lEnd = offsetsLines.get(leafIndex + 1) + nbrOfVertices;
	
				// Polys
				int pBegin = offsetsPolys.get(leafIndex) + nbrOfVertices + nbrOfLines;
				int pEnd = offsetsPolys.get(leafIndex + 1) + nbrOfVertices + nbrOfLines;
				selection.ensureCapacity(selection.size() +
					(vEnd + 1 - vBegin) +
					(lEnd + 1 - lBegin) +
					(pEnd + 1 - pBegin));
	
				// Add vertices
				for (int j = vBegin; j < vEnd; ++j)
					selection.add(j);
				// Add lines
				for (int j = lBegin; j < lEnd; ++j)
					selection.add(j);
				// Add polys
				for (int j = pBegin; j < pEnd; ++j)
					selection.add(j);
			}
			else if (leaf.hasCellSelection())
			{
				int[] cellSelection = leaf.getCellSelection();
				selection.ensureCapacity(selection.size()+cellSelection.length);
				for (int j = 0; j < cellSelection.length; ++j)
					selection.add(leafIndexToNodeIndex(leaf, leafIndex, cellSelection[j]));
			}
		}

		if (selection.isEmpty())
		{
			deleteSelectionActor();
			return;
		}

		boolean actorCreated = (selectionActor == null);
		if (actorCreated)
		{
			selectionActor = new vtkActor();
			selectionActor.PickableOff();
		}
		getSelectionActorCustomiser().customiseActor(selectionActor);

		if(selectionMapper == null)
			selectionMapper = new vtkPainterPolyDataMapper();
		selectionMapper.ScalarVisibilityOff();
		selectionMapper.SetInputConnection(selectInto(data, selection.toArray()));
		selectionActor.SetMapper(selectionMapper);
		getSelectionMapperCustomiser().customiseMapper(selectionMapper);
		
		if (actorCreated)
			fireActorCreated(selectionActor);
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

	private vtkAlgorithmOutput selectInto(vtkPolyData input, int[] cellID)
	{
		vtkSelection selection = new vtkSelection();
		vtkSelectionNode selectionNode = new vtkSelectionNode();
		//sel.ReleaseDataFlagOn();
		// 4 MEANS INDICES (see the enumeration)
		selectionNode.GetProperties().Set(selectionNode.CONTENT_TYPE(), 4);

		// 0 MEANS CELLS
		selectionNode.GetProperties().Set(selectionNode.FIELD_TYPE(), 0);

		// list of cells to be selected
		vtkIdTypeArray arr = Utils.setValues(cellID);
		selectionNode.SetSelectionList(arr);
		selection.AddNode(selectionNode);
		vtkExtractSelectedPolyDataIds selFilter = new vtkExtractSelectedPolyDataIds();
		selFilter.ReleaseDataFlagOn();
		selFilter.SetInputData(1, selection);
		selFilter.SetInputData(0, input);
		return selFilter.GetOutputPort();
	}

	@Override
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
				leaves.get(i).setCellSelection(pickContext, selectionChildren[i].toArray());
		}
		
		timeStampSelected();
	}
	
	@Override
	public void clearCellSelection()
	{
		for (LeafNode leaf : getLeaves())
			leaf.clearCellSelection();
		
		timeStampSelected();
	}

	public List<AbstractNode> getChildren()
	{
		return Collections.unmodifiableList(children);
	}

	@Override
	public void setEdgeVisible(boolean b)
	{
		super.setEdgeVisible(b);
		for(AbstractNode f:getChildren())
			f.setEdgeVisible(b);
	}

	@Override
	public void setCulling(boolean front, boolean back) {
		super.setCulling(front, back);
		for(AbstractNode f:getChildren())
			f.setCulling(front, back);
	}
}
