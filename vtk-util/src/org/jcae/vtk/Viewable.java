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


import gnu.trove.TIntHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import vtk.vtkActor;
import vtk.vtkPlaneCollection;
import vtk.vtkProperty;

/**
 * @author Julian Ibarz
 */
public abstract class Viewable extends MultiCanvas
{
	private final static Logger LOGGER = Logger.getLogger(Viewable.class.getName());
	/** The position of the mouse when the press event occurs */
	private String name;
	private final ArrayList<SelectionListener> selectionListeners =
			new ArrayList<SelectionListener>();
	protected final double tolerance = 0.002; // 0.2% of tolerance in function of the (far-near) distance
	protected final Scene scene;
	/** The rootNode node of the viewable */
	protected final Node rootNode;
	/** Set of selected nodes */
	protected Set<LeafNode> selectionNode = new HashSet<LeafNode>();
	/** Map of selected cells */
	protected final Map<LeafNode, TIntHashSet> selectionCell = new HashMap<LeafNode, TIntHashSet>();
	/** Flag to know if selection has changed */
	protected boolean selectionChanged;
	/** Flag to set selection in append or replace mode */
	protected boolean appendSelection;
	private SelectionType selectionType = SelectionType.NODE;
	private int pixelTolerance;

	public enum SelectionType
	{
		NODE,
		CELL,
		POINT
	}

	public Viewable()
	{
		scene = new Scene();
		rootNode = new Node(null);

		addNode(rootNode);
		rootNode.addChildCreationListener(this);
		rootNode.setSelectionActorCustomiser(new SelectionActorCustomiser());
	}

	protected class SelectionActorCustomiser implements AbstractNode.ActorCustomiser
	{
		@Override
		public void customiseActor(vtkActor actor)
		{
			vtkProperty p = actor.GetProperty();
			Utils.vtkPropertySetColor(p, selectionColor);
			p.SetLineWidth(3.0);
			p.Delete();
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
	
	public int getPixelTolerance()
	{
		return pixelTolerance;
	}

	public void setPixelTolerance(int pixelTolerance)
	{
		this.pixelTolerance = pixelTolerance;
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

	/**
	 * Pick and update informations about picked objects.
	 * {@link Scene#select(org.jcae.vtk.PickContext)} is called
	 * to perform picking and find selected nodes and cells,
	 * then {@link #manageSelection(org.jcae.vtk.PickContext)}
	 * updates selection state.
	 * 
	 * @param pickContext object carrying informations about picking context
	 */
	public void performSelection(PickContext pickContext)
	{
		scene.select(pickContext);
		manageSelection(pickContext);
	}

	/**
	 * Update selection state.
	 * <ol>
	 *   <li>When {@link #selectionType} is NODE, {@link #selectionNode}
	 *       set is updated to contain the new set of selected nodes,
	 *       based on append mode, the set of previously selected nodes
	 *       and the set of picked nodes.  The {@link #selectionChanged}
	 *       member is set to <code>true</code> if selection has changed.
	 *   </li>
	 *   <li>When {@link #selectionType} is CELL, {@link #selectionCell}
	 *       map is updated to contain the new map of selected nodes,
	 *       based on append mode, the map of previously selected nodes
	 *       and the set of picked cells.  Selected cells in leaves are
	 *       also updated by calling
	 *       {@link LeafNode#setCellSelection(org.jcae.vtk.PickContext, int[])}.
	 *       The {@link #selectionChanged} member is set to <code>true</code>
	 *       if anything has been picked.
	 *   </li>
	 * </ol>
	 * 
	 * @param pickContext object carrying informations about picking context
	 */
	protected void manageSelection(PickContext pickContext)
	{
		switch (selectionType)
		{
			case NODE:
				manageNodeSelection(pickContext);
				break;
			case CELL:
				manageCellSelection(pickContext);
				break;
			case POINT:
				// No-op for now
				break;
			default:
				throw new IllegalStateException();
		}

		if (selectionChanged)
			fireSelectionChanged();
		selectionChanged = false;
	}

	/**
	 * Update selectionCell to take previous selection into account.
	 * 
	 * @param pickContext object carrying informations about picking context
	 */
	private void manageCellSelection(PickContext pickContext)
	{
		selectionNode.clear();
		if (!appendSelection)
			selectionCell.clear();
		
		Set<LeafNode> oldSelection = new HashSet<LeafNode>(selectionCell.keySet());
		Set<LeafNode> newSelection = pickContext.getSelectedNodes();
		for (LeafNode node : newSelection)
		{
			selectionChanged = true;
			TIntHashSet nodeCellSelection = selectionCell.get(node);
			if (nodeCellSelection == null)
			{
				pickContext.addToSelectedNodes(node);
				selectionCell.put(node, 
					new TIntHashSet(node.getCellSelection()));
			}
			else
			{
				for (int cell : node.getCellSelection())
				{
					// If already selected, we are in append mode, then delete it
					if (!nodeCellSelection.add(cell))
						nodeCellSelection.remove(cell);
				}

				// Send the new selection to the leaf
				node.setCellSelection(pickContext, nodeCellSelection.toArray());
			}
		}
		
		// Send old selection to leaves, it may have been reset by scene.select()
		for (LeafNode leaf : oldSelection)
		{
			if (!newSelection.contains(leaf))
				leaf.setCellSelection(pickContext, selectionCell.get(leaf).toArray());
		}

		if (selectionChanged)
			LOGGER.finest("Selection changed");			
		else
			LOGGER.finest("Selection not changed");
	}

	/**
	 * Update selectionNode to take previous selection into account.
	 * 
	 * @param pickContext object carrying informations about picking context
	 */
	private void manageNodeSelection(PickContext pickContext)
	{		
		selectionCell.clear();
		
		HashSet<LeafNode> newSelection;
		if (appendSelection)
			newSelection = new HashSet<LeafNode>(selectionNode);
		else
			newSelection = new HashSet<LeafNode>();

		for (LeafNode node : pickContext.getSelectedNodes())
		{
			// If already selected, we are in append mode, then delete it
			if (newSelection.add(node))
				node.select();
			else
			{
				newSelection.remove(node);
				node.unselect();
			}
			
			// Clear cell selection
			node.clearCellSelection();
		}

		if (!selectionNode.equals(newSelection))
			selectionChanged = true;

		selectionNode = newSelection;
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
	 * If you want the highlight disappears call highlight() afterwards
	 */
	protected void unselectAll()
	{
		for (LeafNode leaf : selectionCell.keySet())
			leaf.clearCellSelection();
		selectionCell.clear();
		
		for (LeafNode leaf : selectionNode)
			leaf.unselect();
		selectionNode.clear();
	}

	@Override
	void addNode(AbstractNode node)
	{
		super.addNode(node);
		scene.addNode(node);
	}

	@Override
	void removeNode(AbstractNode node)
	{
		scene.removeNode(node);
		if(selectionNode.remove(node))
			selectionChanged = true;
		super.removeNode(node);
	}
	
	public void highlight()
	{
		// Refresh viewable
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
		vtkPlaneCollection empty = new vtkPlaneCollection();
		setClippingPlanes(empty);
		empty.Delete();
		render();
	}

	void setClippingPlanes(vtkPlaneCollection planes)
	{
		scene.setClippingPlanes(planes);
	}

	void setPickable(boolean pickable)
	{
		scene.setPickable(pickable);
	}

	/** VTK garbage collect all nodes associated to this viewable */
	public void delete()
	{
		lockCanvas();
		rootNode.removeAllChildren();
		rootNode.deleteData();
		unlockCanvas();
	}
}
