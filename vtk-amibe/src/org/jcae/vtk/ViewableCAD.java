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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import org.jcae.opencascade.jni.TopoDS_Edge;
import org.jcae.opencascade.jni.TopoDS_Face;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.jcae.opencascade.jni.TopoDS_Vertex;
import vtk.vtkActor;

/**
 *
 * @author Julian Ibarz
 */
public class ViewableCAD extends Viewable
{
	private final OCCMeshExtractor meshExtractor;
	/** Specify which type of object is selectable */
	private ShapeType shapeTypeSelection = ShapeType.FACE;
	private final Color frontFaceColor = new Color(255 / 2, 255 / 2, 255);
	private final Color backFaceColor = Color.LIGHT_GRAY;//new Color(255 / 2, 255, 255 / 2);
	private final Color vertexColor = Color.BLUE;
	private Color edgeColor = Color.WHITE;
	private Color freeEdgeColor = Color.GREEN;
	private final HashMap<TopoDS_Vertex, LeafNode> topoToNodeVertice = new HashMap<TopoDS_Vertex, LeafNode>();
	private final HashMap<TopoDS_Edge, LeafNode> topoToNodeEdge = new HashMap<TopoDS_Edge, LeafNode>();
	private final HashMap<TopoDS_Face, LeafNode> topoToNodeFaceFront = new HashMap<TopoDS_Face, LeafNode>();
	private final HashMap<TopoDS_Face, LeafNode> topoToNodeFaceBack = new HashMap<TopoDS_Face, LeafNode>();
	private final HashMap<LeafNode, TopoDS_Shape> nodeToTopo = new HashMap<LeafNode, TopoDS_Shape>();
	private Node faces = null;
	private Node edges = null;
	private Node vertice = null;
	private int vertexSize = 4;
	private int edgeSize = 2;
	private boolean onlyFreeEdges = false;
	
	public enum ShapeType
	{
		VERTEX, EDGE, FACE
	}
			
	private ViewableCAD(OCCMeshExtractor meshExtractor, boolean onlyFreeEdges)
	{
		super(new Scene(), new Node(null));
		this.meshExtractor = meshExtractor;
		this.onlyFreeEdges = onlyFreeEdges;
		
		computeNodes();
		setShapeTypeSelection(shapeTypeSelection);
	}

	public void refresh()
	{
		for(AbstractNode n:new ArrayList<AbstractNode>(rootNode.getChildren()))
			rootNode.removeChild(n);
		computeNodes();
		rootNode.refresh();
		render();
	}
	
	public ViewableCAD(TopoDS_Shape shape)
	{
		this(new OCCMeshExtractor(shape), false);
	}
	
	public ViewableCAD(TopoDS_Shape shape, boolean onlyFreeEdges)
	{
		this(new OCCMeshExtractor(shape), onlyFreeEdges);
	}
	
	public ViewableCAD(String filename)
	{
		this(new OCCMeshExtractor(filename), false);
	}

	public Color getEdgeColor()
	{
		return edgeColor;
	}

	public void setEdgeColor(Color edgeColor)
	{
		this.edgeColor = edgeColor;
		for(LeafNode edge : topoToNodeEdge.values())
			edge.setColor(edgeColor);
		
		edges.refresh();
	}

	public int getEdgeSize()
	{
		return edgeSize;
	}

	public void setEdgeSize(int edgeSize)
	{
		this.edgeSize = edgeSize;
		
		edges.applyActorCustomiser();
	}

	public void testDataChange()
	{
		int i = 0;
		for(LeafNode leaf : rootNode.getLeaves())
		{
			LeafNode.DataProvider data = new LeafNode.DataProvider();
			data.setNodes(new float[] {i,0,0,i+1,0,0} );
			i+=2;
			data.setLines(new int[]{2,0,1});
			leaf.setData(data);
		}
		rootNode.refresh();
	}
	
	private void computeNodes()
	{
		vertice = new Node(rootNode);
		vertice.setManager(true);
		vertice.setActorCustomiser(new AbstractNode.ActorCustomiser()
		{

			public void customiseActor(vtkActor actor)
			{
				actor.GetProperty().SetPointSize(vertexSize);			
			}
		});
		
		vertice.setActorHighlightedCustomiser(new Viewable.ActorHighlightedCustomiser()
		{
			@Override
			public void customiseActorHighlighted(vtkActor actor)
			{
				super.customiseActorHighlighted(actor);
				
				actor.GetProperty().SetPointSize(vertexSize*2);
			}
		});
		
		edges = new Node(rootNode);
		edges.setManager(true);
		edges.setActorCustomiser(new AbstractNode.ActorCustomiser()
		{
			public void customiseActor(vtkActor actor)
			{
				actor.GetProperty().SetLineWidth(edgeSize);
			}
		});
		
		faces = new Node(rootNode);
		faces.setActorCustomiser(new AbstractNode.ActorCustomiser()
		{

			public void customiseActor(vtkActor actor)
			{
				actor.GetProperty().BackfaceCullingOn();
			}
		});
		
		Node facesFront = new Node(faces);
		Node facesBack = new Node(faces);
		facesFront.setManager(true);
		facesBack.setManager(true);

		// Add the edges
		Collection freeEdges = meshExtractor.getFreeEdges();
		for (TopoDS_Edge edge : this.meshExtractor.getEdges())
		{
			Color color = edgeColor;
			if (freeEdges.contains(edge))
				color = freeEdgeColor;
			else if(onlyFreeEdges)
				continue;

			LeafNode edgeNode = new LeafNode(edges, new OCCMeshExtractor.EdgeData(edge), color);
			topoToNodeEdge.put(edge, edgeNode);
			nodeToTopo.put(edgeNode, edge);
		}

		if (!onlyFreeEdges)
		{
			// Add vertice
			for (TopoDS_Vertex vertex : this.meshExtractor.getVertice())
			{
				LeafNode vertexNode = new LeafNode(vertice, new OCCMeshExtractor.VertexData(vertex), this.vertexColor);				
				topoToNodeVertice.put(vertex, vertexNode);
				nodeToTopo.put(vertexNode, vertex);
			}

			// Add faces
			for (TopoDS_Face face : this.meshExtractor.getFaces())
			{
				LeafNode faceNode = new LeafNode(facesFront, new OCCMeshExtractor.FaceData(face, false), this.frontFaceColor);
				topoToNodeFaceFront.put(face, faceNode);
				nodeToTopo.put(faceNode, face);
				
				LeafNode backFaceNode = new LeafNode(facesBack, new OCCMeshExtractor.FaceData(face, true), this.backFaceColor);
				topoToNodeFaceBack.put(face, backFaceNode);
				nodeToTopo.put(backFaceNode, face);
			}
		}
		
		rootNode.refresh();
	}

	public Node getEdges()
	{
		return edges;
	}

	public Collection<TopoDS_Shape> getSelection()
	{
		HashSet<TopoDS_Shape> selection = new HashSet<TopoDS_Shape>(selectionNode.size());
		
		for(LeafNode leaf : selectionNode)
		{
			selection.add(nodeToTopo.get(leaf));
		}
		return selection;
	}
	
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
	}

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

		vertice.setPickableRecursive(false);
		edges.setPickableRecursive(false);
		faces.setPickableRecursive(false);
		
		switch(shapeTypeSelection)
		{
			case VERTEX:
				pixelTolerance = 3;
				vertice.setPickableRecursive(true);
				break;
			case EDGE:
				pixelTolerance = 3;
				edges.setPickableRecursive(true);
				break;
			default:
				pixelTolerance = 0;
				faces.setPickableRecursive(true);
		}
	}
	
}
