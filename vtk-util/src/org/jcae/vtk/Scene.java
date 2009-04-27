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


import gnu.trove.TLongObjectHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.geometry.BoundingBox;
import vtk.vtkActor;
import vtk.vtkActorCollection;
import vtk.vtkCanvas;
import vtk.vtkGlobalJavaHash;
import vtk.vtkIdTypeArray;
import vtk.vtkIntArray;
import vtk.vtkPlaneCollection;
import vtk.vtkProp;
import vtk.vtkSelection;
import vtk.vtkSelectionNode;
import vtk.vtkVisibleCellSelector;

/**
 * This class is used to make picking on a tree node. It permit to keep the association
 * between a node and his actor with a map (idActorToNode).
 * @author ibarz
 */
public class Scene implements AbstractNode.ActorListener
{
	private final static Logger LOGGER = Logger.getLogger(Scene.class.getName());

	private TLongObjectHashMap<AbstractNode> idActorToNode =
		new TLongObjectHashMap<AbstractNode>();
	private boolean actorFiltering = true;
	private vtkPlaneCollection planes = null;
	private boolean checkColorDepth = Boolean.parseBoolean(
		System.getProperty("org.jcae.vtk.checkColorDepth", "true"));
	
	/**
	 * Store the previous pickability of the scene.
	 * Boolean.TRUE means the node is pickable
	 * Boolean.FALSE means the node is not pickable
	 */
	private Map<AbstractNode, Boolean> pickBackup;

	public Scene()
	{
	}

	public void addNode(AbstractNode node)
	{
		node.addActorListener(this);
	}

	public void removeNode(AbstractNode node)
	{
		node.removeActorListener(this);
	}

	public void actorCreated(AbstractNode node, vtkActor actor)
	{
		if (LOGGER.isLoggable(Level.FINE))
			LOGGER.log(Level.FINE, "Create actor id="+actor.GetVTKId()+" hashcode="+Integer.toHexString(actor.hashCode()));
		idActorToNode.put(actor.GetVTKId(), node);
		if (planes != null)
			actor.GetMapper().SetClippingPlanes(planes);
	}

	public void actorDeleted(AbstractNode node, vtkActor actor)
	{
		if (LOGGER.isLoggable(Level.FINE))
			LOGGER.log(Level.FINE, "Delete actor id="+actor.GetVTKId()+" hashcode="+Integer.toHexString(actor.hashCode()));
		idActorToNode.remove(actor.GetVTKId());
	}

	private AbstractNode[] getNodes()
	{
		AbstractNode[] nodes = new AbstractNode[idActorToNode.size()];
		idActorToNode.getValues(nodes);
		
		return nodes;
	}
	
	public void setPickable(boolean pickable)
	{
		AbstractNode[] nodes = getNodes();
		
		if (pickable)
		{
			for (AbstractNode node : nodes)
			{
				boolean isPickable = pickable;
				if (pickBackup != null && pickBackup.containsKey(node))
					isPickable = pickBackup.get(node).booleanValue();
				node.setPickable(isPickable);
			}
		}
		else
		{
			pickBackup = new HashMap<AbstractNode, Boolean>(nodes.length);

			for (AbstractNode node : nodes)
			{
				pickBackup.put(node, Boolean.valueOf(node.isPickable()));
				node.setPickable(false);
			}
		}

	}

	public void setActorFiltering(boolean actorFiltering)
	{
		this.actorFiltering = actorFiltering;
	}

	/**
	 * Warning : If you are making highlight with offset (by default this is down) then the selection will not take in
	 * case the highlighted objects because the z-buffer is not cleaned and so the normal geometry
	 * will not be drawned for selection because the highlighted geometry is nearest of the camera
	 * due to the offset. If you want bypass this you have to take care of the highlighted objects :
	 * _ draw them in rendering selection i.e. make them pickable.
	 * _ find the initial geometry corresponding to the selected highlighted object.
	 * @param canvas
	 * @param firstPoint
	 * @param secondPoint
	 */
	public void select(PickContext pickContext)
	{
		if (pickContext.onlyVisible())
			selectVisibleNodes(pickContext);
		else
			selectAllNodes(pickContext);
	}
	
	private void selectVisibleNodes(PickContext pickContext)
	{
		vtkCanvas canvas = pickContext.getCanvas();
		int [] firstPoint = pickContext.getPressPosition();
		int [] secondPoint = pickContext.getReleasePosition();
		if (checkColorDepth)
		{
			canvas.lock();
			vtkIntArray tmp = new vtkIntArray();
			if (canvas.GetRenderWindow().GetColorBufferSizes(tmp) < 24)
			{
				canvas.unlock();
				throw new RuntimeException("Color depth is lower than 24 bits, picking does not work");
			}
			tmp.Delete();
			canvas.unlock();
			checkColorDepth = false;
		}

		int[] pickableActorBackup = null;
		if (actorFiltering)
		{
			vtkActorCollection actors = canvas.GetRenderer().GetActors();
			pickableActorBackup = new int[actors.GetNumberOfItems()];

			actors.InitTraversal();
			int j = 0;
			for (vtkActor actor; (actor = actors.GetNextActor()) != null; ++j)
			{
				pickableActorBackup[j] = actor.GetPickable();
				if (pickableActorBackup[j] == 0)
					continue;
				double[] bounds = actor.GetBounds();
				BoundingBox box = new BoundingBox();
				box.setLower(bounds[0], bounds[2], bounds[4]);
				box.setUpper(bounds[1], bounds[3], bounds[5]);

				if (!pickContext.intersect(box))
					actor.PickableOff();
			}
		}

		vtkVisibleCellSelector selector = new vtkVisibleCellSelector();
		selector.SetRenderer(canvas.GetRenderer());
		selector.SetArea(firstPoint[0], firstPoint[1], secondPoint[0],
				secondPoint[1]);
		selector.SetRenderPasses(0, 1, 0, 0, 1, 0);

		canvas.lock();
		int savePreserve = canvas.GetRenderer().GetPreserveDepthBuffer();
		canvas.GetRenderer().PreserveDepthBufferOn();
		selector.Select();
		canvas.GetRenderer().SetPreserveDepthBuffer(savePreserve);
		canvas.unlock();

		vtkSelection selection = new vtkSelection();
		selection.ReleaseDataFlagOn();
		selector.GetSelectedIds(selection);
		
		if (actorFiltering)
		{
			vtkActorCollection actors = canvas.GetRenderer().GetActors();
			actors.InitTraversal();
			int j = 0;
			for (vtkActor actor; (actor = actors.GetNextActor()) != null; ++j)
				actor.SetPickable(pickableActorBackup[j]);
		}
		
		// Find the ID Selection of the actor
		for (int i = 0; i < selection.GetNumberOfNodes(); ++i)
		{
			vtkSelectionNode child = selection.GetNode(i);
			int IDActor = child.GetProperties().Get(child.PROP_ID());
			vtkProp prop = selector.GetActorFromId(IDActor);

			if (prop != null)
			{
				AbstractNode node = idActorToNode.get(prop.GetVTKId());

				if (node != null)
				{
					vtkGlobalJavaHash.PointerToReference.clear();
					vtkIdTypeArray ids = (vtkIdTypeArray) child.GetSelectionList();
					node.setCellSelection(pickContext, Utils.getValues(ids));
					LOGGER.finest("Actor picked id: "+prop.GetVTKId());
					LOGGER.finest("Picked node: "+node);
				}
			}
			else
			{
				// FIXME: For unknown reason, garbage is sometimes
				// appended to real data. Exit loop when an unknown
				// actor is found, further processing is useless.
				LOGGER.warning("No selection found for actor "+IDActor);
				break;
			}
		}
		selection.Delete();
		selector.Delete();
	}

	private void selectAllNodes(PickContext pickContext)
	{
		vtkCanvas canvas = pickContext.getCanvas();
		vtkActorCollection actors = canvas.GetRenderer().GetActors();
		actors.InitTraversal();
		for (vtkActor actor; (actor = actors.GetNextActor()) != null; )
		{
			if (actor.GetPickable() == 0)
				continue;
			double[] bounds = actor.GetBounds();
			BoundingBox box = new BoundingBox();
			box.setLower(bounds[0], bounds[2], bounds[4]);
			box.setUpper(bounds[1], bounds[3], bounds[5]);
			if (pickContext.intersect(box))
			{
				AbstractNode node = idActorToNode.get(actor.GetVTKId());
				if (node instanceof LeafNode)
					pickContext.addToSelectedNodes((LeafNode) node);
				else
				{
					for (LeafNode leaf : node.getLeaves())
					{
						LeafNode.DataProvider leafProvider = leaf.getDataProvider();
						leafProvider.load();
						leaf.createData(leafProvider);
						bounds = leaf.data.GetBounds();
						box.setLower(bounds[0], bounds[2], bounds[4]);
						box.setUpper(bounds[1], bounds[3], bounds[5]);
						leaf.deleteData();
						leafProvider.unLoad();
						if (pickContext.intersect(box))
							pickContext.addToSelectedNodes(leaf);
					}
				}
			}
		}
	}

	public void setClippingPlanes(vtkPlaneCollection planes)
	{
		AbstractNode[] nodes = new AbstractNode[idActorToNode.size()];
		idActorToNode.getValues(nodes);
		for (AbstractNode node : nodes)
			node.getActor().GetMapper().SetClippingPlanes(planes);
	}
}
