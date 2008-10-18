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
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import vtk.vtkActor;
import vtk.vtkPlaneCollection;

/**
 * @author Julian Ibarz
 */
public abstract class Viewable extends MultiCanvas
{
	private final static Logger LOGGER = Logger.getLogger(Viewable.class.getName());
	/** The position of the mouse when the press event occurs */
	private String name;
	private ArrayList<SelectionListener> selectionListeners =
			new ArrayList<SelectionListener>();
	protected final double tolerance = 0.002; // 0.2% of tolerance in function of the (far-near) distance
	protected int pixelTolerance = 0; // pixel tolerance for point picking (0 by default)
	protected static final int DEFAULT_PIXEL_TOLERANCE = 3;
	protected final Scene scene;
	protected final Node rootNode; // The rootNode node of the viewable
	protected HashSet<LeafNode> selectionNode = new HashSet<LeafNode>();
	protected HashMap<LeafNode, TIntHashSet> selectionCell = new HashMap<LeafNode, TIntHashSet>();
	protected boolean selectionChanged;
	/* Flag to set selection in append or replace mode */
	protected boolean appendSelection;
	/*
	 * Type of rectangle selection.
	 * If true, the rectangle selection is on surface.
	 * If false, the rectangle selection is in frustum.
	 */
	protected boolean surfaceSelection = true;
	private SelectionType selectionType = SelectionType.NODE;

	public enum SelectionType
	{
		NODE,
		CELL,
		POINT
	}

	public Viewable()
	{
		this(new Scene(), new Node(null));
	}

	public Viewable(Scene scene, Node root)
	{
		this.scene = scene;
		this.rootNode = root;

		addNode(root);
		root.addChildCreationListener(this);
		root.setActorHighLightedCustomiser(new ActorHighLightedCustomiser());
		root.setActorSelectionCustomiser(new ActorSelectionCustomiser());
	}

	protected class ActorHighLightedCustomiser implements AbstractNode.ActorHighLightedCustomiser
	{

		public void customiseActorHighLighted(vtkActor actor)
		{
			Utils.vtkPropertySetColor(actor.GetProperty(), selectionColor);
		}
	}

	protected class ActorSelectionCustomiser implements AbstractNode.ActorSelectionCustomiser
	{

		public void customiseActorSelection(vtkActor actor)
		{
			Utils.vtkPropertySetColor(actor.GetProperty(), selectionColor);
		}
	}

	public void setSelectionType(SelectionType selectionType)
	{
		this.selectionType = selectionType;
	}

	public SelectionType getSelectionType()
	{
		return this.selectionType;
	}
	
	/**
	 * Return current mode of selection.
	 * If true, selection is added to current selection.
	 * If false, it replaces current selection.
	 * @return current mode of selection
	 */
	public boolean getAppendSelection()
	{
		return this.appendSelection;
	}

	/**
	 * Set mode of selection.
	 * If true, selection is added to current selection.
	 * If false, it replaces current selection.
	 * @param appendSelection
	 */
	public void setAppendSelection(boolean appendSelection)
	{
		this.appendSelection = appendSelection;
	}

	public void surfaceSelection(Canvas canvas, Point pressPosition_, Point releasePosition_)
	{
		int[] pressPosition = new int[2];
		pressPosition[0] = pressPosition_.x;
		pressPosition[1] = pressPosition_.y;
		int[] releasePosition = new int[2];
		releasePosition[0] = releasePosition_.x;
		releasePosition[1] = releasePosition_.y;

		switch (selectionType)
		{
			case POINT:
				selectPointOnSurface(canvas, pressPosition, releasePosition);
				break;
			case CELL:
				selectCellOnSurface(canvas, pressPosition, releasePosition);
				break;
			case NODE:
				selectNodeOnSurface(canvas, pressPosition, releasePosition);
				break;
		}

		manageSelection();
	}

	protected int[] selectPointOnSurface(Canvas canvas, int[] firstPoint, int[] secondPoint)
	{
		return new int[0];
	}

	protected void selectCellOnSurface(Canvas canvas, int[] firstPoint, int[] secondPoint)
	{
		scene.pick(canvas, firstPoint, secondPoint);

		List<LeafNode> nodes = rootNode.getLeaves();
		for (LeafNode node : nodes)
		{
			TIntHashSet nodeCellSelection = selectionCell.get(node);
			if (nodeCellSelection == null)
			{
				TIntHashSet newCellSelection =
					new TIntHashSet(node.getSelection().toNativeArray());
				if (!newCellSelection.isEmpty())
				{
					selectionCell.put(node, newCellSelection);
					selectionChanged = true;
				}
			}
			else
			{
				TIntArrayList selection = node.getSelection();
				for (int i = 0; i < selection.size(); ++i)
				{
					selectionChanged = true;
					int cell = selection.get(i);
					if (!nodeCellSelection.add(cell))
						nodeCellSelection.remove(cell);
				}

				// Send the new selection to the leaf
				node.setSelection(new TIntArrayList(nodeCellSelection.toArray()));
			}
		}

		if (LOGGER.isLoggable(Level.FINEST))
		{
			for (LeafNode node : nodes)
				if (node.getSelection().size() != 0)
					LOGGER.finest("One not empty selection found");

			if (selectionChanged)
				LOGGER.finest("Selection changed");			
			else
				LOGGER.finest("Selection not changed");
		}
	}

	protected void selectNodeOnSurface(Canvas canvas, int[] firstPoint, int[] secondPoint)
	{
		LOGGER.finest("Start pick color");
		scene.pick(canvas, firstPoint, secondPoint);
		LOGGER.finest("End pick color");
		
		LOGGER.finest("Start dispatch");
		List<LeafNode> nodes = rootNode.getLeaves();

		HashSet<LeafNode> newSelection;

		if (appendSelection)
			newSelection = new HashSet<LeafNode>(selectionNode);
		else
			newSelection = new HashSet<LeafNode>();

		for (LeafNode node : nodes)
		{
			TIntArrayList nodeSelection = node.getSelection();

			if (!nodeSelection.isEmpty())
			{
				// If already selected, we are in append mode, then delete it
				if (!newSelection.add(node))
					newSelection.remove(node);

				// Clear the selectionNode because it's not a cell selectionNode
				nodeSelection.clear();

				// Remove the node from the selection cell
				selectionCell.remove(node);
			}
		}

		if (!selectionNode.equals(newSelection))
			selectionChanged = true;

		selectionNode = newSelection;
		LOGGER.finest("End dispatch");
	}

	public void pointSelection(Canvas canvas, Point pickPosition)
	{
		LOGGER.fine("Making point selection");

		Point pressPosition   = new Point(pickPosition.x - pixelTolerance, pickPosition.y - pixelTolerance);
		Point releasePosition = new Point(pickPosition.x + pixelTolerance, pickPosition.y + pixelTolerance);
		surfaceSelection(canvas, pressPosition, releasePosition);
	}

	protected void manageSelection()
	{
		if (selectionChanged)
			fireSelectionChanged();
		selectionChanged = false;
	}

	public void addSelectionListener(SelectionListener listener)
	{
		selectionListeners.add(listener);
	}

	public void removeSelectionListener(SelectionListener listener)
	{
		selectionListeners.remove(listener);
	}

	protected void fireSelectionChanged()
	{
		for (SelectionListener listener : selectionListeners)
			listener.selectionChanged(this);
	}

	/**
	 * If you want the highlight disappears call highlight...
	 */
	public void unselectAll()
	{
		for (LeafNode leaf : selectionCell.keySet())
			leaf.unSelectCells();

		selectionCell.clear();
		selectionNode.clear();
	}

	@Override
	public void addNode(AbstractNode node)
	{
		super.addNode(node);
		scene.addNode(node);
	}

	@Override
	public void removeNode(AbstractNode node)
	{
		scene.removeNode(node);
		super.removeNode(node);
	}

	protected void highLightNodes()
	{
		List<LeafNode> leaves = rootNode.getLeaves();

		for (LeafNode leaf : leaves)
			if (selectionNode.contains(leaf))
				leaf.select();
			else
				leaf.unSelect();
	}

	private void highLightCells()
	{
		rootNode.highLightSelection();
	}

	public void highLight()
	{
		highLightCells();
		highLightNodes();
		rootNode.refresh();
		render();
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	@Override
	public String toString()
	{
		if (name == null)
			return super.toString();
		else
			return name;
	}

	public void removeClippingPlanes()
	{
		// Send empty plane collection to removeCanvas the older planes
		setClippingPlanes(new vtkPlaneCollection());
		render();
	}

	void setClippingPlanes(vtkPlaneCollection planes)
	{
		scene.setClippingPlanes(planes);
	}

	protected boolean isSelectionEmpty()
	{
		return selectionCell.isEmpty() && selectionNode.isEmpty();
	}

	public void setPickable(boolean pickable)
	{
		scene.setPickable(pickable);
	}

}
