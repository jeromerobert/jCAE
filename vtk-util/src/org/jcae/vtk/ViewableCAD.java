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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.jcae.opencascade.jni.TopoDS_Edge;
import org.jcae.opencascade.jni.TopoDS_Face;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.jcae.opencascade.jni.TopoDS_Vertex;
import vtk.vtkActor;
import vtk.vtkMapper;

/**
 *
 * @author ibarz
 */
public class ViewableCAD extends Viewable
{
	/*Collection<TopoDS_Shape> actualSelection = Collections.EMPTY_LIST;
	private Collection<TopoDS_Shape> selection =
			new HashSet<TopoDS_Shape>();*/
	private final OCCMeshExtractor meshExtractor;
	/** Specify which type of object is selectable */
	private ShapeType shapeTypeSelection = ShapeType.FACE;
	private final Color frontFaceColor = new Color(0,255,255,180);
	private final Color backFaceColor = new Color(255,255,0,180);
	private final Color vertexColor = new Color(0,0,255);
	private final Color edgeColor = new Color(0,255,0);
	
	private final HashMap<TopoDS_Vertex, LeafNode> topoToNodeVertice = new HashMap<TopoDS_Vertex, LeafNode>();
	private final HashMap<TopoDS_Edge, LeafNode> topoToNodeEdge = new HashMap<TopoDS_Edge, LeafNode>();
	private final HashMap<TopoDS_Face, LeafNode> topoToNodeFaceFront = new HashMap<TopoDS_Face, LeafNode>();
	private final HashMap<TopoDS_Face, LeafNode> topoToNodeFaceBack = new HashMap<TopoDS_Face, LeafNode>();
	private final HashMap<LeafNode, TopoDS_Shape> nodeToTopo = new HashMap<LeafNode, TopoDS_Shape>();
	private Node faces = null;
	
	
	public enum ShapeType
	{
		VERTEX, EDGE, FACE
	}
			
	private ViewableCAD(OCCMeshExtractor meshExtractor)
	{
		super(new Scene(), new Node(null));
		this.meshExtractor = meshExtractor;
		
		computeNodes();
	}


	public ViewableCAD(TopoDS_Shape shape)
	{
		this(new OCCMeshExtractor(shape));
	}
	
	public ViewableCAD(String filename)
	{
		this(new OCCMeshExtractor(filename));
	}
		
	@Override
	public void actorCreated(AbstractNode node, vtkActor actor)
	{
		super.actorCreated(node, actor);
		
		actor.GetProperty().SetPointSize(4);
		actor.GetProperty().SetLineWidth(3);
		
		vtkMapper mapper = actor.GetMapper();
		
		//mapper.SetResolveCoincidentTopologyToPolygonOffset();
		//mapper.SetResolveCoincidentTopologyPolygonOffsetParameters(Utils.getOffSetFactor(), Utils.getOffSetValue());
		
		//actor.GetProperty().BackfaceCullingOn();
		
		/*if(actor == faces.getActor())
		{
			System.out.println("FACES !");
			actor.GetMapper().ScalarVisibilityOff();
			
			vtkProperty frontProperty = actor.GetProperty();
			Utils.vtkPropertySetColor(frontProperty, Color.RED);
			actor.SetProperty(frontProperty);
			
			vtkProperty property = actor.MakeProperty();
			Utils.vtkPropertySetColor(property, Color.BLUE);
			actor.SetBackfaceProperty(property);
			actor.ApplyProperties();
		}*/
	}
	
	public void testDataChange()
	{
		List<LeafNode> leaves = rootNode.getLeaves();
		int i = 0;
		for(LeafNode leaf : leaves)
		{
			LeafNode.DataProvider data = new LeafNode.DataProvider();
			data.setNodes(new float[] {i++,0,0,i++,0,0} );
			data.setLines(new int[]{2,0,1});
			leaf.setData(data);
		}
		rootNode.refresh();
	}
	
	private void computeNodes()
	{
		Node vertice = new Node(rootNode);
		
		for(TopoDS_Vertex vertex : this.meshExtractor.getVertice())
		{
			LeafNode vertexNode = new LeafNode(vertice, new OCCMeshExtractor.VertexData(vertex), this.vertexColor);
			//vertexNode.setManager(true);
			topoToNodeVertice.put(vertex, vertexNode);
			this.nodeToTopo.put(vertexNode, vertex);
		}
		vertice.setManager(true);
		
		Node edges = new Node(rootNode);
		
		for(TopoDS_Edge edge : this.meshExtractor.getEdges())
		{
			LeafNode edgeNode = new LeafNode(edges, new OCCMeshExtractor.EdgeData(edge), this.edgeColor);
			topoToNodeEdge.put(edge, edgeNode);
			nodeToTopo.put(edgeNode, edge);
			//edgeNode.setManager(true);
		}
		edges.setManager(true);

		faces = new Node(rootNode);
		
		for(TopoDS_Face face : this.meshExtractor.getFaces())
		{
			LeafNode faceNode = new LeafNode(faces, new OCCMeshExtractor.FaceData(face, false), this.frontFaceColor);
			topoToNodeFaceFront.put(face, faceNode);
			nodeToTopo.put(faceNode, face);
			LeafNode backFaceNode = new LeafNode(faces, new OCCMeshExtractor.FaceData(face, true), this.backFaceColor);
			topoToNodeFaceBack.put(face, backFaceNode);
			nodeToTopo.put(backFaceNode, face);
			//faceNode.setManager(true);
			//backFaceNode.setManager(true);
		}
		faces.setManager(true);
		//rootNode.setManager(true);
		rootNode.refresh();
	}
	
	@Override
	protected void selectNodeOnSurface(Canvas canvas, int[] firstPoint, int[] secondPoint)
	{
		HashSet<LeafNode> selectionNodeBefore = new HashSet<LeafNode>(this.selectionNode);
		
		super.selectNodeOnSurface(canvas, firstPoint, secondPoint);
		
		HashSet<LeafNode> selectionNodeAfter = selectionNode;
		selectionNode = new HashSet<LeafNode>(selectionNodeAfter.size());
		
		// Add only the nodes with the good type
		for(LeafNode leaf : selectionNodeAfter)
		{
			// If in the previous selection and in the actual add always (appended mode...)
			if(selectionNodeBefore.contains(leaf))
			{
				selectionNode.add(leaf);
				continue;
			}
			
			TopoDS_Shape shape = nodeToTopo.get(leaf);
			if(shape == null)
				continue;
			
			switch(this.shapeTypeSelection)
			{
				case VERTEX:
					if(shape instanceof TopoDS_Vertex)
						selectionNode.add(leaf);
					break;
				case EDGE:
					if(shape instanceof TopoDS_Edge)
						selectionNode.add(leaf);
					break;
				case FACE:
					if(shape instanceof TopoDS_Face)
						selectionNode.add(leaf);
					break;
				default:
					throw new RuntimeException("Type of shape unknown !");
			}
		}
	}
	
	/*public void manageActualSelection()
	{
		if (appendSelection)
		{
			// If it's already on the selection unselect
			ArrayList<TopoDS_Shape> cleanedSelection = new ArrayList<TopoDS_Shape>(selection);
			
			for(TopoDS_Shape shape : actualSelection)
			{
				if(cleanedSelection.contains(shape))
					cleanedSelection.remove(shape);
				else
					cleanedSelection.add(shape);	
			}
			
			selection = cleanedSelection;
		}
		else
			selection = actualSelection;
		
		actualSelection = Collections.EMPTY_LIST;
	}*/

	public Collection<TopoDS_Shape> getSelection()
	{
		HashSet<TopoDS_Shape> selection = new HashSet<TopoDS_Shape>(selectionNode.size());
		
		for(LeafNode leaf : selectionNode)
		{
			selection.add(nodeToTopo.get(leaf));
		}
		return selection;
	}

	/*public void highLight()
	{	
		highLight(selection);
		
		super.highLight();
	}

	@Override
	public void unSelectAll()
	{
		selection = Collections.EMPTY_LIST;
	}*/
	
	/**
	 * Set the new selection.
	 * Be careful, the listeners of change selection will not  be informed.
	 * 
	 * @param selection 
	 */
	public void setSelection(Collection<TopoDS_Shape> selection)
	{
		selectionNode = new HashSet<LeafNode>(selection.size());
		
		for(TopoDS_Shape shape : selection)
		{
			if(shape instanceof TopoDS_Vertex)
			{
				selectionNode.add(topoToNodeVertice.get((TopoDS_Vertex)shape));
			}
			else if(shape instanceof TopoDS_Edge)
			{
				selectionNode.add(topoToNodeEdge.get((TopoDS_Edge)shape));
			}
			else if(shape instanceof TopoDS_Face)
			{
				selectionNode.add(topoToNodeFaceFront.get((TopoDS_Face)shape));
				selectionNode.add(topoToNodeFaceBack.get((TopoDS_Face)shape));
			}
		}
		/*if(appendSelection)
			{
			// Clean the selection to desactive the unselect behaviour (see manageActualSelection)
			HashSet<TopoDS_Shape> hashSelection = new HashSet<TopoDS_Shape>(selection.size());

			for(TopoDS_Shape shape : selection)
				if(!this.selection.contains(shape))
					hashSelection.add(shape);

			actualSelection = hashSelection;
		}
		else*/
//		this.selection = selection;
	}

	/*@Override
	public void pointSelection(vtkCanvas canvas)
	{

		/*TopoDS_Shape shape = selectionPoint(canvas,pressPosition, 5);
		System.out.println("SHAPE SELECTED : " + shape);
		actualSelection = new ArrayList<TopoDS_Shape>();
		if (shape != null)			
			actualSelection.add(shape);*/
	/*}*/

	/*@Override
	protected boolean isSelectionEmpty()
	{
		return actualSelection == null || actualSelection.size() == 0;
	}
	

	@Override
	public void surfaceSelection(vtkCanvas canvas)
	{
		actualSelection = selectOnSurface(canvas,
				pressPosition, releasePosition);
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		switch (e.getKeyCode())
		{
			case KeyEvent.VK_E:
				setShapeTypeSelection(ShapeType.EDGE);
				break;
			case KeyEvent.VK_F:
				setShapeTypeSelection(ShapeType.FACE);
				break;
			case KeyEvent.VK_V:
				setShapeTypeSelection(ShapeType.VERTEX);
				break;
			}

		super.keyPressed(e);
	}*/
	
	/*public vtkActor getBackFaces()
	{
		return backFaces;
	}

	public vtkActor getEdgesActor()
	{
		return edgesActor;
	}

	public vtkActor getFacesActor()
	{
		return facesActor;
	}*/

	public OCCMeshExtractor getMeshExtractor()
	{
		return meshExtractor;
	}

	/*public vtkActor getVerticesActor()
	{
		return verticesActor;
	}
*/

	public ShapeType getShapeTypeSelection()
	{
		return shapeTypeSelection;
	}

	public void setShapeTypeSelection(ShapeType shapeTypeSelection)
	{
		this.shapeTypeSelection = shapeTypeSelection;
		switch(shapeTypeSelection)
		{
			case VERTEX:
			case EDGE:
				pixelTolerance = 3;
				break;
			default:
				pixelTolerance = 0;
		}
	}
	
	/*private vtkActor getSelectingActor()
	{
		switch(shapeTypeSelection)
		{
			case VERTEX: return verticesActor;
			case EDGE: return edgesActor;
			case FACE: return facesActor;
			default:
				throw new IllegalStateException();
		}
	}*/
	
	/**
	 * TODO : Be carefull if the number of cell is very important, it can make problems.
	 * @param canvas 
	 * @param firstPoint
	 * @param secondPoint
	 * @return 
	 */
	/*public Collection<TopoDS_Shape> selectOnSurface(vtkCanvas canvas, int firstPoint[],
			int secondPoint[])
	{*/
	/*	vtkVisibleCellSelector selector = new vtkVisibleCellSelector();
		selector.SetRenderer(canvas.GetRenderer());
		selector.SetArea(firstPoint[0], firstPoint[1], secondPoint[0],
				secondPoint[1]);
		selector.SetRenderPasses(0, 1, 0, 0, 1, 0);

		canvas.lock();
		selector.Select();
		canvas.unlock();

		vtkIdTypeArray idArray = new vtkIdTypeArray();
		selector.GetSelectedIds(idArray);

		// If no selection was made leave
		if (idArray.GetDataSize() == 0)
			return Collections.EMPTY_LIST;

		vtkSelection selection = new vtkSelection();
		selector.GetSelectedIds(selection);

		// Find the ID Selection of the actor
		int IDActor = -1;
		int IDActorBackFaces = -1;
		for (int i = 0; i < selection.GetNumberOfChildren(); ++i)
		{
			int ID = selection.GetChild(i).GetProperties().Get(selection.PROP_ID());
			vtkProp prop = selector.GetActorFromId(ID);
			if(prop == null)
				continue;
			if (prop.GetVTKId() == getSelectingActor().GetVTKId())
				IDActor = ID;
			else if (prop.GetVTKId() == backFaces.GetVTKId() && shapeTypeSelection == ShapeType.FACE)
				IDActorBackFaces = ID;
		}

		int[] globalIDs = Utils.getValues(idArray);
		TIntArrayList ids = new TIntArrayList(globalIDs.length / 4);
		int nbOfSelection = 0;
		for (int i = 0; i < globalIDs.length / 4; ++i)
			if ((globalIDs[i * 4 + 1] == IDActor) || (globalIDs[i * 4 + 1] == IDActorBackFaces))
			{
				ids.add(globalIDs[i * 4 + 3]);
				++nbOfSelection;
			}

		// If no selection was maded leave
		if (nbOfSelection == 0)
			return Collections.EMPTY_LIST;
		
		// Affichage des ids
		int[] idsNavite = ids.toNativeArray();
		/*System.out.print("IDS : ");
		for(int i : idsNavite)
			System.out.print(" " + i);
		System.out.println("");*/ 

	/*	switch (shapeTypeSelection)
		{
			case VERTEX:
				return new ArrayList<TopoDS_Shape>(meshExtractor.getVertexShapes(ids.toNativeArray()));
			case EDGE:
				return new ArrayList<TopoDS_Shape>(meshExtractor.getEdgeShapes(ids.toNativeArray()));
			case FACE:
				return new ArrayList<TopoDS_Shape>(meshExtractor.getFaceShapes(ids.toNativeArray()));
			default:
				throw new RuntimeException("Type inconnu !"); // This may not happen
		}*/
	/*	return null;
	}*/

	/*public void highLight(Collection<TopoDS_Shape> shapes)
	{*/
		/*ArrayList<TopoDS_Vertex> vertices =
				new ArrayList<TopoDS_Vertex>(shapes.size());
		ArrayList<TopoDS_Edge> edges = new ArrayList<TopoDS_Edge>(shapes.size());
		ArrayList<TopoDS_Face> faces = new ArrayList<TopoDS_Face>(shapes.size());

		for (TopoDS_Shape shape : shapes)
			if (shape instanceof TopoDS_Vertex)
				vertices.add((TopoDS_Vertex) shape);
			else if (shape instanceof TopoDS_Edge)
				edges.add((TopoDS_Edge) shape);
			else if (shape instanceof TopoDS_Face)
				faces.add((TopoDS_Face) shape);
			else
			{
				throw new RuntimeException("Shape type not supported");
			}
		vtkAppendPolyData dataAppend = new vtkAppendPolyData();
		vtkPolyData dataVertices = null;
		vtkPolyData dataEdges = null;
		vtkPolyData dataFaces = null;

		if (!vertices.isEmpty())
			dataVertices = computeVerticeMeshes(
				meshExtractor.computeVerticeMeshes(vertices));

		if (!edges.isEmpty())
			dataEdges = computeEdgeMeshes(meshExtractor.computeEdgeMeshes(edges));

		if (!faces.isEmpty())
			dataFaces = computeFaceMeshes(meshExtractor.computeFaceMeshes(faces));

		if (dataVertices != null)
			dataAppend.AddInput(dataVertices);
		if (dataEdges != null)
			dataAppend.AddInput(dataEdges);
		if (dataFaces != null)
			dataAppend.AddInput(dataFaces);

		vtkPolyData data;
		// If there is a shape then one of the dataShapes is not null so the dataAppend can be executed
		if (shapes.isEmpty())
			data = new vtkPolyData();
		else
			data = dataAppend.GetOutput();

		vtk.vtkPolyDataMapper mapper = new vtk.vtkPolyDataMapper();
		mapper.SetInputConnection(0, data.GetProducerPort());
		highLight.SetMapper(mapper);
		System.out.println("HIGHLIGHT DOWN : "+ shapes.size() + "NB OF CANVAS : " + listCanvas.size());
*/	//}

	/*private vtkActor createActor(vtkPolyData data, ShapeType shapeType)
	{
		vtkPolyDataMapper mapper = new vtkPolyDataMapper();
		mapper.SetInputConnection(0, data.GetProducerPort());
		vtkActor actor = new vtkActor();

		switch (shapeType)
		{
			case VERTEX:
				actor.GetProperty().SetColor(0, 0, 1);
				actor.GetProperty().SetPointSize(4);
				break;
			case EDGE:
				actor.GetProperty().SetColor(0, 1, 0);
				actor.GetProperty().SetLineWidth(3);
				break;
			case FACE:
				actor.GetProperty().SetColor(0, 1, 1);
				//actor.GetProperty().FrontfaceCullingOff();
				//actor.GetProperty().BackfaceCullingOff();
				break;
			default:
				throw new IllegalArgumentException();
		}

		actor.SetMapper(mapper);
		actor.PickableOn();

		return actor;
	}
	
	private vtkPolyData computeVerticeMeshes(Tesselation t)
	{
		vtkPolyData data = new vtkPolyData();
		
		if(t == null)
			return data;
		
		data.SetPoints(Utils.createPoints(t.getCoordinates()));
		data.SetVerts(Utils.createCells(t.getNumberOfCells(),
			Utils.createVerticesCells(t.getNumberOfCells())));

		return data;		
	}*/

			
	/*private vtkPolyData computeEdgeMeshes(Tesselation t)
	{	
		
		vtkPolyData toReturn = new vtkPolyData();
		
		
		/*if(t == null)
			return toReturn;
		
		toReturn.SetPoints(Utils.createPoints(t.getCoordinates()));
		toReturn.SetLines(Utils.createCells(t.getNumberOfCells(), t.getIndices()));
		*//*return toReturn;
	}		
}*/
}
