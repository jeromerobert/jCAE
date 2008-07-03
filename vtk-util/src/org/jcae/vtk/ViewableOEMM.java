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

import gnu.trove.TFloatArrayList;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIterator;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectIntHashMap;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import org.jcae.mesh.oemm.OEMM;
import vtk.vtkActor;
import vtk.vtkCanvas;
import vtk.vtkCellCenterDepthSort;
import vtk.vtkDataSet;
import vtk.vtkExtractSelectedFrustum;
import vtk.vtkIdTypeArray;
import vtk.vtkPlaneCollection;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;

/**
 *
 * @author ibarz
 */
public class ViewableOEMM extends Viewable implements MouseMotionListener
{

	private OEMM oemm;
	private MeshVisuReader reader;
	private vtkActor octree;
	private vtkActor octreeForPicking;
	private vtkActor edgesActor;
	private vtkActor freeEdgesActor;
	private boolean automaticSelection = false;
	private boolean octreeVisible = true;
	private final int leafVisibleMax = 10;
	private TObjectIntHashMap<LeafNode> nodeToID = new TObjectIntHashMap<LeafNode>();
	private TIntObjectHashMap<LeafNode> IDToEdgeNode = new TIntObjectHashMap<LeafNode>();
	private TIntObjectHashMap<LeafNode> IDToFreeEdgeNode = new TIntObjectHashMap<LeafNode>();
	private Node edgesNode;
	private Node freeEdgesNode;
	private Node octreeNode;
	volatile boolean rendering = false;

	/*private static class DataLeaf extends  LeafNode.DataProvider
	{
	float[] nodes;
	int[] lines;
	int[] quads;
	
	DataLeaf(float[] nodes, int[] lines, int[] quads)
	{
	this.nodes = nodes;
	this.lines = lines;
	nbrOfLines = lines.length / 3;
	this.quads = quads;
	nbrOfPolys = quads.length / 5;
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
	// Do nothong
	}
	
	@Override
	public int[] getLines()
	{
	return lines;
	}
	
	@Override
	public int[] getPolys()
	{
	return quads;
	}
	
	}*/
	public ViewableOEMM(OEMM oemm)
	{
		super(new Scene(), new Node(null));
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
		int offSet = 0;
		for (int i = 0; i < nodes.length - 1;)
		{
			int first = i / 3;
			// First
			quadsFilled[offSet++] = 2;
			quadsFilled[offSet++] = i / 3;
			++i;
			quadsFilled[offSet++] = i / 3;
			// Second
			quadsFilled[offSet++] = 2;
			quadsFilled[offSet++] = i / 3;
			++i;
			quadsFilled[offSet++] = i / 3;
			// Three
			quadsFilled[offSet++] = 2;
			quadsFilled[offSet++] = i / 3;
			++i;
			quadsFilled[offSet++] = i / 3;
			// Four
			quadsFilled[offSet++] = 2;
			quadsFilled[offSet++] = i / 3;
			++i;
			quadsFilled[offSet++] = first;
		}
		data.SetPoints(points);
		data.SetLines(Utils.createCells(quadsFilled.length / 3, quadsFilled));
		vtkPolyDataMapper mapper = new vtkPolyDataMapper();
		mapper.SetInput(data);
		octree.SetMapper(mapper);
		octree.GetProperty().SetColor(0., 0., 1.);
		octree.PickableOff();
		octree.SetVisibility(Utils.booleanToInt(octreeVisible));

		int[] quadsLeaf = new int[5 * 6];

		int offSetLeaf = 0;
		for (int i = 0; i < quadsLeaf.length;)
		{
			quadsLeaf[i++] = 4;
			quadsLeaf[i++] = offSetLeaf++;
			quadsLeaf[i++] = offSetLeaf++;
			quadsLeaf[i++] = offSetLeaf++;
			quadsLeaf[i++] = offSetLeaf++;
		}

		int ID = 0;
		octreeNode = new Node(rootNode);
		for (int i = 0; i < nodes.length;)
		{
			float[] leafNodes = new float[6 * 4 * 3];
			System.arraycopy(nodes, i, leafNodes, 0, leafNodes.length);
			i += leafNodes.length;

			LeafNode.DataProvider dataLeaf = new LeafNode.DataProvider();
			dataLeaf.setNodes(leafNodes);
			dataLeaf.setPolys(quadsLeaf.length / 5, quadsLeaf);
			LeafNode leaf = new LeafNode(octreeNode, dataLeaf, Color.BLUE);
			nodeToID.put(leaf, ID++);
		}
		octreeNode.setManager(true);
		octreeNode.refresh();
		octreeNode.setPickableRecursive(true);
		octreeForPicking = octreeNode.getActor();
		octreeForPicking.SetVisibility(0);

		/*octreeForPicking = new vtkActor();
		data = new vtkPolyData();
		int[] quads = new int[(nodes.length / (4 * 3)) * 5];
		offSet = 0;
		for (int i = 0; i < nodes.length / 3;)
		{
		quads[offSet++] = 4;
		quads[offSet++] = i++;
		quads[offSet++] = i++;
		quads[offSet++] = i++;
		quads[offSet++] = i++;
		}
		data.SetPoints(points);
		data.SetPolys(Utils.createCells(quads.length / 5, quads));
		mapper = new vtkPolyDataMapper();
		mapper.SetInput(data);
		octreeForPicking.SetMapper(mapper);
		octreeForPicking.VisibilityOff();
		octreeForPicking.PickableOn();*/

		// Construct Mesh
		edgesActor = new vtkActor();
		edgesActor.GetProperty().SetColor(1., 1., 1.);
		edgesActor.PickableOff();

		// Construct the freeEdgesActor
		freeEdgesActor = new vtkActor();
		freeEdgesActor.GetProperty().SetColor(1., 0., 0.);
		freeEdgesActor.PickableOff();
	}

	public boolean isOctreeVisible()
	{
		return octreeVisible;
	}

	public void setOctreeVisible(boolean octreeVisible)
	{
		this.octreeVisible = octreeVisible;
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
	public void highLight()
	{
		final ViewableOEMM me = this;
		Thread run = new Thread()
		{

			public final ViewableOEMM viewable = me;

			@Override
			public void run()
			{
				viewable.highLightThreaded();
			}
		};
		run.start();
	}

	public void highLightThreaded()
	{
		TIntHashSet selection;
		rendering = true;

		synchronized (selectionNode)
		{
			selection = new TIntHashSet(selectionNode.size());

			for (LeafNode leaf : selectionNode)
				selection.add(nodeToID.get(leaf));


			System.out.print("HIGH LIGHTING : ");
			TIntIterator iter = selection.iterator();
			while (iter.hasNext())
			{
				int val = iter.next();
				System.out.print(val + " ");
			}
			System.out.println("");

			// Delete the nodes not selected
			for (int id : IDToEdgeNode.keys())
				if (!selection.contains(id))
				{
					System.out.println("deleting : " + id);
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

			vtkPolyData data = new vtkPolyData();
			int[] leaves = reader.getLeavesLoaded();
			System.out.println("selection : " + selection.toString());
			System.out.println("leaves : " + Arrays.toString(leaves));
			MeshVisuReader.MeshVisu[] meshes = reader.getMeshes();

			// The choice of the preallocated size is an heuristic that works in the major of the cases (permits to not compute exactly the size of the tables
			TFloatArrayList nodes = new TFloatArrayList(meshes.length * meshes[0].nodes.length);
			// The offSet of the nodes of the edgesActor in the wall array node
			int[] offSets = new int[leaves.length];
			// Add all the nodes and compute the hash set node
			for (int i = 0; i < leaves.length; ++i)
			{
				// If the node is already added continue
				if (IDToEdgeNode.containsKey(leaves[i]))
					continue;

				float[] meshNodes = meshes[i].nodes;
				//TIntHashSet nodesLoaded = new TIntHashSet(meshNodes.length / 3);
				System.out.println("mesh " + i + " contains " + meshNodes.length / 3 + " nodes");

				//for (int j = 0; j < meshNodes.length / 3; ++j)
				//	nodesLoaded.add(oemm.leaves[leaves[i]].minIndex + j);

				TIntArrayList[] allEdges = new TIntArrayList[]
				{
					new TIntArrayList(meshes[i].edges.length), new TIntArrayList(meshes[i].freeEdges.length)
				};
				
				ArrayList<int[]> edgesMesh = new ArrayList<int[]>(2);
				edgesMesh.add(meshes[i].edges);
				edgesMesh.add(meshes[i].freeEdges);

				for (int type = 0; type < 2; ++type)
				{
					int[] ones = edgesMesh.get(type);

					System.out.println("TYPE " + type + " size : " + ones.length);
					
					for (int j = 0; j < ones.length;)
					{

						int begin = ones[j++];
						int end = ones[j++];

						/*if(j % 100 == 0)
						{
						System.out.println("beginTime : " + beginTime);
						System.out.println("endTime : " + endTime);
						System.out.println("minde : " + minIndex);
						System.out.println("offSetMesh : " + offSets[i]);
						}*/

						/*if (nodesLoaded.contains(begin) && nodesLoaded.contains(end))
						{
							allEdges[type].add(2);
							allEdges[type].add(begin - oemm.leaves[leaves[i]].minIndex);
							allEdges[type].add(end - oemm.leaves[leaves[i]].minIndex);
						}
						// If they aren't loaded it's a fake vertice so keep the index (it's already good)
						else
						{*/
							allEdges[type].add(2);
							allEdges[type].add(begin);
							allEdges[type].add(end);
						//}
					}
				}

				LeafNode.DataProvider dataEdge = new LeafNode.DataProvider();
				dataEdge.setNodes(meshNodes);
				dataEdge.setLines(allEdges[0].toNativeArray());
				LeafNode edgeNode = new LeafNode(edgesNode, dataEdge, Color.WHITE);
				edgeNode.setManager(true);
				IDToEdgeNode.put(leaves[i], edgeNode);
				lockCanvas();
				System.out.println("REFRESHING EDGE NODE");
				edgeNode.refresh();
				System.out.println("END REFRESHING EDGE NODE");
				unlockCanvas();

				LeafNode.DataProvider dataFreeEdge = new LeafNode.DataProvider();
				dataFreeEdge.setNodes(meshNodes);
				dataFreeEdge.setLines(allEdges[1].toNativeArray());
				LeafNode freeEdgeNode = new LeafNode(freeEdgesNode, dataFreeEdge, Color.RED);
				freeEdgeNode.setManager(true);
				IDToFreeEdgeNode.put(leaves[i], freeEdgeNode);
				lockCanvas();
				System.out.println("REFRESHING FREE EDGE NODE");
				freeEdgeNode.refresh();
				System.out.println("END REFRESHING FREE EDGE NODE");
				unlockCanvas();
			}
		}
		rendering = false;
	}

	public void highLightThreadedOld()
	{
		/*System.out.println("HighLighting : " + selection.toString());
		
		if (selection.isEmpty())
		return;
		
		reader.buildMeshVisu(selection.toNativeArray());
		
		vtkPolyData data = new vtkPolyData();
		int[] leaves = reader.getLeavesLoaded();
		System.out.println("selection : " + selection.toString());
		System.out.println("leaves : " + Arrays.toString(leaves));
		MeshVisuReader.MeshVisu[] meshes = reader.getMeshes();
		
		// The choice of the preallocated size is an heuristic that works in the major of the cases (permits to not compute exactly the size of the tables
		TFloatArrayList nodes = new TFloatArrayList(meshes.length * meshes[0].nodes.length);
		TIntHashSet nodesLoaded = new TIntHashSet(meshes.length * meshes[0].nodes.length);
		TIntArrayList edges = new TIntArrayList(meshes.length * meshes[0].edges.length);
		TIntArrayList freeEdges = new TIntArrayList(meshes.length * meshes[0].freeEdges.length);
		// The offSet of the nodes of the edgesActor in the wall array node
		int[] offSets = new int[leaves.length];
		// Add all the nodes and compute the hash set node
		for (int i = 0; i < leaves.length; ++i)
		{
		float[] meshNodes = meshes[i].nodes;
		System.out.println("mesh " + i + " contains " + meshNodes.length / 3 + " nodes");
		
		offSets[i] = nodes.size() / 3;
		nodes.add(meshNodes);
		
		for (int j = 0; j < meshNodes.length / 3; ++j)
		nodesLoaded.add(oemm.leaves[leaves[i]].minIndex + j);
		}
		
		// Add the edges that have their nodes loaded
		for (int i = 0; i < leaves.length; ++i)
		{
		TIntArrayList[] allEdges = new TIntArrayList[]
		{
		edges, freeEdges
		};
		ArrayList<int[]> edgesMesh = new ArrayList<int[]>(2);
		edgesMesh.add(meshes[i].edges);
		edgesMesh.add(meshes[i].freeEdges);
		
		for (int type = 0; type < 2; ++type)
		{
		int[] ones = edgesMesh.get(type);
		
		for (int j = 0; j < ones.length;)
		{
		
		int begin = ones[j++];
		int end = ones[j++];
		
		/*if(j % 100 == 0)
		{
		System.out.println("beginTime : " + beginTime);
		System.out.println("endTime : " + endTime);
		System.out.println("minde : " + minIndex);
		System.out.println("offSetMesh : " + offSets[i]);
		}*/

		/*if (nodesLoaded.contains(begin) && nodesLoaded.contains(end))
		{
		allEdges[type].add(findVerticeIndex(leaves, i, begin, offSets));
		allEdges[type].add(findVerticeIndex(leaves, i, end, offSets));
		}
		}
		}
		}
		
		vtkPoints points = Utils.createPoints(nodes.toNativeArray());
		
		lockCanvas();
		data.SetPoints(points);
		data.SetLines(Utils.createCells(edges.size() / 2, Utils.createBeamCells(edges.toNativeArray())));
		
		vtkPolyDataMapper mapper = new vtkPolyDataMapper();
		mapper.SetInput(data);
		edgesActor.SetMapper(mapper);
		
		data = new vtkPolyData();
		data.SetPoints(points);
		data.SetLines(Utils.createCells(freeEdges.size() / 2, Utils.createBeamCells(freeEdges.toNativeArray())));
		
		mapper = new vtkPolyDataMapper();
		mapper.SetInput(data);
		freeEdgesActor.SetMapper(mapper);
		unlockCanvas();
		
		//super.highLight();*/
	}

	private int findVerticeIndex(int[] leaves, int currentLeaf, int index, int[] offSets)
	{
		int minIndex = oemm.leaves[leaves[currentLeaf]].minIndex;
		int maxIndex = oemm.leaves[leaves[currentLeaf]].maxIndex;
		int leaf = -1;

		// If the node is in the current leave
		if (index >= minIndex && index < maxIndex)
			leaf = currentLeaf;
		else
			for (int search = 0; search < leaves.length; ++search)
				if (index >= oemm.leaves[leaves[search]].minIndex && index < oemm.leaves[leaves[search]].maxIndex)
				{
					leaf = search;
					break;
				}

		if (leaf == -1)
			throw new RuntimeException("The leaf of the vertice was not found !");

		return index - oemm.leaves[leaves[leaf]].minIndex + offSets[leaf];
	}

	@Override
	public void addCanvas(Canvas canvas)
	{
		super.addCanvas(canvas);
		canvas.addMouseMotionListener(this);

		canvas.GetRenderer().AddViewProp(octree);
		//canvas.GetRenderer().AddViewProp(octreeForPicking);
		canvas.GetRenderer().AddViewProp(edgesActor);
		canvas.GetRenderer().AddViewProp(freeEdgesActor);
	}

	@Override
	public void removeCanvas(Canvas canvas)
	{
		super.removeCanvas(canvas);
		canvas.removeMouseMotionListener(this);


		canvas.GetRenderer().RemoveViewProp(octree);
		//canvas.GetRenderer().RemoveViewProp(octreeForPicking);
		canvas.GetRenderer().RemoveViewProp(edgesActor);
		canvas.GetRenderer().RemoveViewProp(freeEdgesActor);
	}

	public void mouseDragged(MouseEvent e)
	{
		if (!automaticSelection || rendering)
			return;

		System.out.println("PWET !");
		surfaceSelection(Utils.retrieveCanvas(e), new Point(), new Point());
	}

	public void mouseMoved(MouseEvent e)
	{
		// Do nothing
	}

	@Override
	public void pointSelection(Canvas canvas, Point pickPosition)
	{
		octreeForPicking.VisibilityOn();
		octreeForPicking.SetPickable(1);
		super.pointSelection(canvas, pickPosition);
		octreeForPicking.VisibilityOff();
		return;

	/*vtkCellPicker picker = new vtkCellPicker();
	canvas.getIren().SetPicker(picker);
	
	vtkProp prop = null;
	
	// TODO : calculate the diagonal in pixel of the renderer
	//octreeForPicking.VisibilityOn();
	picker.SetTolerance(Utils.getTolerance(canvas.GetRenderer(), pixelTolerance));
	canvas.lock();
	picker.Pick(pickPosition.getX(), pickPosition.getY(), 0., canvas.GetRenderer());
	canvas.unlock();
	//octreeForPicking.VisibilityOff();
	
	// Verify if the picking is on the good prop
	if (picker.GetViewProp() != null &&
	prop.GetVTKId() == picker.GetViewProp().GetVTKId())
	{
	// This is the quad ID so the leave ID is the quadID / 6 (a cube contains 6 quads)
	int leaveID = picker.GetCellId() / 6;
	
	actualSelection.add(leaveID);
	/*vtkPolyData data= (vtkPolyData)octreeForPicking.GetMapper().GetInputAsDataSet();
	System.out.println("NUMBER OF POLYS : " + data.GetNumberOfCells());
	System.out.println("Type of cell : " + data.GetCell(leaveID).GetCellType());*/
	/*} else if (picker.GetViewProp() != null)
	System.out.println("picking on another viewprop... : " + picker.GetCellId());
	
	// TODO
	this.selectionChanged = true;
	//return -1;
	
	// Get the ID
	//return picker.GetCellId();*/
	}

	@Override
	public synchronized void surfaceSelection(Canvas canvas, Point releasePosition, Point pressPosition)
	{
		/*vtkVisibleCellSelector selector = new vtkVisibleCellSelector();
		selector.SetRenderer(canvas.GetRenderer());
		selector.SetArea(pressPosition[0], pressPosition[1], releasePosition[0],
		releasePosition[1]);
		selector.SetRenderPasses(0, 1, 0, 0, 1, 0);*/
		if (automaticSelection)
		{
			// Set the surfaceSelection on all the canvas
			pressPosition.x = 0;
			pressPosition.y = 0;
			releasePosition.x = canvas.getWidth();
			releasePosition.y = canvas.getHeight();
		}

		vtkExtractSelectedFrustum selector = new vtkExtractSelectedFrustum();

		//vtkPlanes planes = new vtkPlanes();
		//selector.SetFrustum(planes);

		//octreeForPicking.GetMapper().Update();
		vtkDataSet dataSet = octreeForPicking.GetMapper().GetInputAsDataSet();
		System.out.println("Number of points : " + dataSet.GetNumberOfPoints());
		System.out.println("bounds : " + Arrays.toString(dataSet.GetBounds()));
		selector.SetInput(dataSet);
		//selector.SetInputConnection(octreeForPicking.GetMapper().GetInputConnection(0, 0));
		//selector.PreserveTopologyOn();
		selector.CreateFrustum(Utils.computeVerticesFrustum(pressPosition.x, pressPosition.y, releasePosition.x,
				releasePosition.y, canvas.GetRenderer()));

		//selector.ShowBoundsOn();
		//selector.Modified();
		//selector.Update();
		//selector.ShowBoundsOn();
		selector.PreserveTopologyOff();
		lockCanvas();
		selector.Update();
		unlockCanvas();
		vtkDataSet data = (vtkDataSet) selector.GetOutput();
		//octree.GetMapper().SetInputConnection(selector.GetOutputPort());
		System.out.println("Number of points picked : " + data.GetNumberOfPoints());

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
			selectionNode = new HashSet<LeafNode>(this.leafVisibleMax);
			vtkIdTypeArray ids = null;
			boolean full = false;

			while ((ids = sorter.GetNextCells()) != null && !full)
			{
				int[] idsSorted = Utils.getValues(ids);
				vtkIdTypeArray originalCellIDs = (vtkIdTypeArray) data.GetCellData().GetArray("vtkOriginalCellIds");
				for (int id : idsSorted)
				{
					int originalCell = originalCellIDs.GetValue(id);
					// If we have the leaf max number leave
					selectionNode.add(octreeNode.getNode(originalCell));
					if (selectionNode.size() >= this.leafVisibleMax)
					{
						full = true;
						break;
					}

				}
			}

			System.out.println("Number of nodes selected : " + selectionNode.size());
		}
		this.selectionChanged = true;

		manageSelection();
	//frustum extractor works on geometry and doesn't care about pickability

	/*vtkExtractSelectedFrustum extractor = new vtkExtractSelectedFrustum();
	extractor.CreateFrustum(computeVerticesFrustum(pressPosition[0], pressPosition[1], releasePosition[0],
	releasePosition[1], canvas.GetRenderer()));
	extractor.SetInput((vtkDataObject)octreeForPicking.GetMapper().GetInputAsDataSet());
	extractor.PreserveTopologyOff();
	extractor.InsideOutOn();
	
	vtkDataSetMapper eMap = new vtkDataSetMapper();
	eMap.SetInput((vtkDataSet)extractor.GetOutput());
	vtkActor bouding = new vtkActor();
	/*vtkDataSetMapper mapper = new vtkDataSetMapper();
	mapper.SetInput(data);*/
	/*bouding.SetMapper(eMap);
	canvas.GetRenderer().AddViewProp(bouding);*/

	/*canvas.lock();
	area.GetFrustum();
	canvas.unlock();
	
	octreeForPicking.VisibilityOn();
	canvas.lock();
	selector.Select();
	canvas.unlock();
	octreeForPicking.VisibilityOff();
	
	vtkIdTypeArray idArray = new vtkIdTypeArray();
	selector.GetSelectedIds(idArray);
	
	// If no selection was made leave
	if (idArray.GetDataSize() == 0)
	return;
	
	vtkSelection selection = new vtkSelection();
	selector.GetSelectedIds(selection);
	
	// Find the ID Selection of the actor
	int IDActor = -1;
	for (int i = 0; i < selection.GetNumberOfChildren(); ++i)
	{
	IDActor = selection.GetChild(i).GetProperties().Get(selection.PROP_ID());
	vtkProp prop = selector.GetActorFromId(IDActor);
	if (prop != null && prop.GetVTKId() == this.octreeForPicking.GetVTKId())
	break;
	}
	
	int[] globalIDs = Utils.getValues(idArray);
	int nbOfSelection = 0;
	for (int i = 0; i < globalIDs.length / 4; ++i)
	if (globalIDs[i * 4 + 1] == IDActor)
	{
	// This is the quad ID so the leave ID is the quadID / 6 (a cube contains 6 quads)
	actualSelection.add(globalIDs[i * 4 + 3] / 6);
	++nbOfSelection;
	}		*/
	}
	/*public void surfaceSelectionOld(vtkCanvas canvas)
	{
	vtkVisibleCellSelector selector = new vtkVisibleCellSelector();
	selector.SetRenderer(canvas.GetRenderer());
	selector.SetArea(pressPosition[0], pressPosition[1], releasePosition[0],
	releasePosition[1]);
	selector.SetRenderPasses(0, 1, 0, 0, 1, 0);
	
	octreeForPicking.VisibilityOn();
	canvas.lock();
	selector.Select();
	canvas.unlock();
	octreeForPicking.VisibilityOff();
	
	vtkIdTypeArray idArray = new vtkIdTypeArray();
	selector.GetSelectedIds(idArray);
	
	// If no selection was made leave
	if (idArray.GetDataSize() == 0)
	return;
	
	vtkSelection selection = new vtkSelection();
	selector.GetSelectedIds(selection);
	
	// Find the ID Selection of the actor
	int IDActor = -1;
	for (int i = 0; i < selection.GetNumberOfChildren(); ++i)
	{
	IDActor = selection.GetChild(i).GetProperties().Get(selection.PROP_ID());
	vtkProp prop = selector.GetActorFromId(IDActor);
	if (prop != null && prop.GetVTKId() == this.octreeForPicking.GetVTKId())
	break;
	}
	
	int[] globalIDs = Utils.getValues(idArray);
	int nbOfSelection = 0;
	for (int i = 0; i < globalIDs.length / 4; ++i)
	if (globalIDs[i * 4 + 1] == IDActor)
	{
	// This is the quad ID so the leave ID is the quadID / 6 (a cube contains 6 quads)
	actualSelection.add(globalIDs[i * 4 + 3] / 6);
	++nbOfSelection;
	}
	}*/
}
