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
import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectIntHashMap;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.HashSet;
import org.jcae.mesh.oemm.OEMM;
import vtk.vtkActor;
import vtk.vtkCellCenterDepthSort;
import vtk.vtkDataSet;
import vtk.vtkExtractSelectedFrustum;
import vtk.vtkIdTypeArray;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;

/**
 *
 * @author ibarz
 */
public class ViewableOEMM extends Viewable implements MouseMotionListener
{
	private final OEMM oemm;
	private final MeshVisuReader reader;
	private final vtkActor octree;
	private final vtkActor octreeForPicking;
	private boolean automaticSelection = false;
	private final int leafVisibleMax = 10;
	private final TObjectIntHashMap<LeafNode> nodeToID = new TObjectIntHashMap<LeafNode>();
	private final TIntObjectHashMap<LeafNode> IDToEdgeNode = new TIntObjectHashMap<LeafNode>();
	private final TIntObjectHashMap<LeafNode> IDToFreeEdgeNode = new TIntObjectHashMap<LeafNode>();
	private final Node edgesNode;
	private final Node freeEdgesNode;
	private final Node octreeNode;
	volatile boolean rendering = false;

	public ViewableOEMM(OEMM oemm)
	{
		this.oemm = oemm;
		reader = new MeshVisuReader(oemm);

		edgesNode = new Node(rootNode);
		freeEdgesNode = new Node(rootNode);

		// Construct octree
		float[] nodes = reader.getNodesQuad();
		vtkPoints points = Utils.createPoints(nodes);
		octree = new vtkActor();
		vtkPolyData data = new vtkPolyData();

		int[] quadsFilled = new int[nodes.length * 3];
		int offset = 0;
		for (int i = 0; i < nodes.length - 1;)
		{
			int first = i / 3;
			// First
			quadsFilled[offset++] = 2;
			quadsFilled[offset++] = i / 3;
			++i;
			quadsFilled[offset++] = i / 3;
			// Second
			quadsFilled[offset++] = 2;
			quadsFilled[offset++] = i / 3;
			++i;
			quadsFilled[offset++] = i / 3;
			// Three
			quadsFilled[offset++] = 2;
			quadsFilled[offset++] = i / 3;
			++i;
			quadsFilled[offset++] = i / 3;
			// Four
			quadsFilled[offset++] = 2;
			quadsFilled[offset++] = i / 3;
			++i;
			quadsFilled[offset++] = first;
		}
		data.SetPoints(points);
		data.SetLines(Utils.createCells(quadsFilled.length / 3, quadsFilled));
		vtkPolyDataMapper mapper = new vtkPolyDataMapper();
		mapper.SetInput(data);
		octree.SetMapper(mapper);
		octree.GetProperty().SetColor(0., 0., 1.);
		octree.PickableOff();
		octree.VisibilityOn();

		int[] quadsLeaf = new int[5 * 6];

		int offsetLeaf = 0;
		for (int i = 0; i < quadsLeaf.length;)
		{
			quadsLeaf[i++] = 4;
			quadsLeaf[i++] = offsetLeaf++;
			quadsLeaf[i++] = offsetLeaf++;
			quadsLeaf[i++] = offsetLeaf++;
			quadsLeaf[i++] = offsetLeaf++;
		}

		int ID = 0;
		octreeNode = new Node(rootNode);
		octreeNode.setManager(true);
		for (int i = 0; i < nodes.length;)
		{
			float[] leafNodes = new float[6 * 4 * 3];
			System.arraycopy(nodes, i, leafNodes, 0, leafNodes.length);
			i += leafNodes.length;

			LeafNode.DataProvider dataLeaf = new LeafNode.DataProvider();
			dataLeaf.setNodes(leafNodes);
			dataLeaf.setPolys(quadsLeaf.length / 5, quadsLeaf);
			LeafNode leaf = new LeafNode(octreeNode, dataLeaf, Color.BLUE);
			nodeToID.put(leaf, ID);
			ID++;
		}
		octreeNode.refresh();
		octreeNode.setPickableRecursive(true);
		octreeForPicking = octreeNode.getActor();
		octreeForPicking.VisibilityOff();
	}

	public boolean isOctreeVisible()
	{
		return octree.GetVisibility() != 0;
	}

	public void setOctreeVisible(boolean octreeVisible)
	{
		octree.SetVisibility(Utils.booleanToInt(octreeVisible));

	}

	public boolean isAutomaticSelection()
	{
		return automaticSelection;
	}

	public void setAutomaticSelection(boolean automaticSelection)
	{
		this.automaticSelection = automaticSelection;
	}

	@Override
	public void highlight()
	{
		Thread run = new Thread()
		{
			@Override
			public void run()
			{
				ViewableOEMM.this.highlightThreaded();
			}
		};
		run.start();
	}

	public void highlightThreaded()
	{
		TIntHashSet selection;
		rendering = true;

		synchronized (selectionNode)
		{
			selection = new TIntHashSet(selectionNode.size());

			for (LeafNode leaf : selectionNode)
				selection.add(nodeToID.get(leaf));

			// Delete the nodes not selected
			for (int id : IDToEdgeNode.keys())
				if (!selection.contains(id))
				{
					LeafNode leaf = IDToEdgeNode.get(id);
					leaf.deleteDatas();
					removeNode(leaf);
					edgesNode.removeChild(leaf);
					IDToEdgeNode.remove(id);
				}

			// Delete the nodes not selected
			for (int id : IDToFreeEdgeNode.keys())
				if (!selection.contains(id))
				{
					LeafNode leaf = IDToFreeEdgeNode.get(id);
					leaf.deleteDatas();
					freeEdgesNode.removeChild(leaf);
					removeNode(leaf);
					IDToFreeEdgeNode.remove(id);
				}


			if (selection.isEmpty())
				return;

			reader.buildMeshVisu(selection.toArray());

			int[] leaves = reader.getLeavesLoaded();
			MeshVisuReader.MeshVisu[] meshes = reader.getMeshes();

			// Add all the nodes and compute the hash set node
			for (int i = 0; i < leaves.length; ++i)
			{
				// If the node is already added continue
				if (IDToEdgeNode.containsKey(leaves[i]))
					continue;

				float[] meshNodes = meshes[i].nodes;

				TIntArrayList[] allEdges = new TIntArrayList[]
				{
					new TIntArrayList(meshes[i].edges.length),
					new TIntArrayList(meshes[i].freeEdges.length)
				};
				
				ArrayList<int[]> edgesMesh = new ArrayList<int[]>(2);
				edgesMesh.add(meshes[i].edges);
				edgesMesh.add(meshes[i].freeEdges);

				for (int type = 0; type < 2; ++type)
				{
					int[] ones = edgesMesh.get(type);
					
					for (int j = 0; j < ones.length; j+=2)
					{
						int begin = ones[j];
						int end = ones[j+1];
						
						allEdges[type].add(2);
						allEdges[type].add(begin);
						allEdges[type].add(end);
					}
				}

				LeafNode.DataProvider dataEdge = new LeafNode.DataProvider();
				dataEdge.setNodes(meshNodes);
				dataEdge.setLines(allEdges[0].toNativeArray());
				LeafNode edgeNode = new LeafNode(edgesNode, dataEdge, Color.WHITE);
				edgeNode.setManager(true);
				IDToEdgeNode.put(leaves[i], edgeNode);
				lockCanvas();
				edgeNode.refresh();
				unlockCanvas();

				LeafNode.DataProvider dataFreeEdge = new LeafNode.DataProvider();
				dataFreeEdge.setNodes(meshNodes);
				dataFreeEdge.setLines(allEdges[1].toNativeArray());
				LeafNode freeEdgeNode = new LeafNode(freeEdgesNode, dataFreeEdge, Color.RED);
				freeEdgeNode.setManager(true);
				IDToFreeEdgeNode.put(leaves[i], freeEdgeNode);
				lockCanvas();
				freeEdgeNode.refresh();
				unlockCanvas();
				if(!automaticSelection)
					render();
			}
		}
		rendering = false;
	}

	@Override
	public void addCanvas(Canvas canvas)
	{
		super.addCanvas(canvas);
		canvas.addMouseMotionListener(this);
		canvas.GetRenderer().AddViewProp(octree);
	}

	@Override
	public void removeCanvas(Canvas canvas)
	{
		super.removeCanvas(canvas);
		canvas.removeMouseMotionListener(this);
		canvas.GetRenderer().RemoveViewProp(octree);
	}

	public void mouseDragged(MouseEvent e)
	{
		if (!automaticSelection || rendering)
			return;

		Canvas canvas = Utils.retrieveCanvas(e);
		PickContext pickContext = new FrustumPicker(canvas, true,
			new Point(0,0),
			new Point(canvas.getWidth(), canvas.getHeight()));
		performSelection(pickContext);
	}

	public void mouseMoved(MouseEvent e)
	{
		// Do nothing
	}

	@Override
	void performSelection(PickContext pickContext)
	{
		int [] pressPosition = pickContext.getPressPosition();
		int [] releasePosition = pickContext.getReleasePosition();
		if (pressPosition[0] == releasePosition[0] && pressPosition[1] == releasePosition[1])
		{
			octreeForPicking.VisibilityOn();
			octreeForPicking.PickableOn();
			super.performSelection(pickContext);
			octreeForPicking.VisibilityOff();
			return;
		}

		Canvas canvas = pickContext.getCanvas();
		if (automaticSelection)
		{
			// Set the surfaceSelection on all the canvas
			pressPosition[0] = 0;
			pressPosition[1] = 0;
			releasePosition[0] = canvas.getWidth();
			releasePosition[1] = canvas.getHeight();
		}

		vtkExtractSelectedFrustum selector = new vtkExtractSelectedFrustum();

		vtkDataSet dataSet = octreeForPicking.GetMapper().GetInputAsDataSet();
		selector.SetInput(dataSet);
		selector.CreateFrustum(Utils.computeVerticesFrustum(
			pressPosition[0], pressPosition[1],
			releasePosition[0], releasePosition[1],
			canvas.GetRenderer()));

		selector.PreserveTopologyOff();
		lockCanvas();
		selector.Update();
		unlockCanvas();
		vtkDataSet data = (vtkDataSet) selector.GetOutput();

		vtkCellCenterDepthSort sorter = new vtkCellCenterDepthSort();
		sorter.SetInput(data);
		sorter.SetCamera(canvas.GetRenderer().GetActiveCamera());
		sorter.SetMaxCellsReturned(this.leafVisibleMax);
		sorter.SetDirectionToFrontToBack();
		lockCanvas();
		sorter.InitTraversal();
		unlockCanvas();

		synchronized (selectionNode)
		{
			selectionNode = new HashSet<LeafNode>(leafVisibleMax);
			vtkIdTypeArray ids = null;
			boolean full = false;

			while ((ids = sorter.GetNextCells()) != null && !full)
			{
				int[] idsSorted = Utils.getValues(ids);
				vtkIdTypeArray originalCellIDs = (vtkIdTypeArray) data.GetCellData().GetArray("vtkOriginalCellIds");
				for (int id : idsSorted)
				{
					int originalCell = originalCellIDs.GetValue(id);
					LeafNode leaf = octreeNode.getNode(originalCell);
					selectionNode.add(leaf);
					pickContext.addToSelectedNodes(leaf);
					// Stop if we reach the maximal number of leaves
					if (selectionNode.size() >= leafVisibleMax)
					{
						full = true;
						break;
					}
				}
			}
		}
		selectionChanged = true;
		
		manageSelection(pickContext);
	}

}
