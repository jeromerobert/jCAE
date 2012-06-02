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
import gnu.trove.TLongObjectIterator;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.geometry.BoundingBox;
import vtk.vtkActor;
import vtk.vtkActorCollection;
import vtk.vtkCanvas;
import vtk.vtkHardwareSelector;
import vtk.vtkIdTypeArray;
import vtk.vtkPlaneCollection;
import vtk.vtkProp;
import vtk.vtkSelection;
import vtk.vtkSelectionNode;

/**
 * This class is used to make picking on a tree node. It permit to keep the association
 * between a node and his actor with a map (idActorToNode).
 * @author ibarz
 */
public class Scene implements AbstractNode.ActorListener
{
	private final static Logger LOGGER = Logger.getLogger(Scene.class.getName());

	private final TLongObjectHashMap<AbstractNode> idActorToNode =
		new TLongObjectHashMap<AbstractNode>();
	private boolean actorFiltering = true;
	private boolean checkColorDepth = Boolean.parseBoolean(
		System.getProperty("org.jcae.vtk.checkColorDepth", "true"));
	
	/**
	 * Store the previous pickability of the scene.
	 * Boolean.TRUE means the node is pickable
	 * Boolean.FALSE means the node is not pickable
	 */
	private Map<AbstractNode, Boolean> pickBackup;

	public void addNode(AbstractNode node)
	{
		node.addActorListener(this);
	}

	public void removeNode(AbstractNode node)
	{
		node.removeActorListener(this);
	}

	@Override
	public void actorCreated(AbstractNode node, vtkActor actor)
	{
		if (LOGGER.isLoggable(Level.FINE))
			LOGGER.log(Level.FINE, "Create actor id="+actor.GetVTKId()+" hashcode="+Integer.toHexString(actor.hashCode()));
		idActorToNode.put(actor.GetVTKId(), node);
	}

	@Override
	public void actorDeleted(AbstractNode node, vtkActor actor)
	{
		if (LOGGER.isLoggable(Level.FINE))
			LOGGER.log(Level.FINE, "Delete actor id="+actor.GetVTKId()+" hashcode="+Integer.toHexString(actor.hashCode()));
		idActorToNode.remove(actor.GetVTKId());
	}

	private Collection<AbstractNode> getNodes()
	{
		//there may be more than one actor by node (selection actor) so we need
		//a set to remove duplicate entries.
		HashSet<AbstractNode> nodes = new HashSet<AbstractNode>(idActorToNode.size());
		TLongObjectIterator<AbstractNode> it = idActorToNode.iterator();
		while(it.hasNext())
		{
			it.advance();
			nodes.add(it.value());
		}
		return nodes;
	}
	
	public void setPickable(boolean pickable)
	{
		Collection<AbstractNode> nodes = getNodes();
		if (pickable)
		{
			for (AbstractNode node : getNodes())
			{
				Boolean p = pickBackup == null ? null : pickBackup.get(node);
				node.setPickable( p == null ? pickable : p);
			}
		}
		else
		{
			pickBackup = new HashMap<AbstractNode, Boolean>(nodes.size());

			for (AbstractNode node : nodes)
			{
				pickBackup.put(node, node.isPickable());
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
	 * @param pickContext
	 */
	public void select(PickContext pickContext)
	{
		if (pickContext.onlyVisible())
			selectVisibleNodes(pickContext);
		else
			selectAllNodes(pickContext);
	}

	/**
	 * Fast selection based on bounding box intersection.
	 *
	 * @param <T> LeafNode derived class which implements BoundedNode
	 * @param pickContext
	 * @param candidates  list of T instances to check
	 */
	public static <T extends LeafNode & BoundedNode> void selectIntersectedNodes(PickContext pickContext, Iterable<T> candidates)
	{
		for (T leaf : candidates)
		{
			if (pickContext.intersect(leaf.getBoundingBox()))
				pickContext.addToSelectedNodes(leaf);
		}
	}

	private void selectVisibleNodes(PickContext pickContext)
	{
		vtkCanvas canvas = pickContext.getCanvas();
		int [] firstPoint = pickContext.getPressPosition();
		int [] secondPoint = pickContext.getReleasePosition();
		if (checkColorDepth)
		{
			int cbs = canvas.getColorModel().getPixelSize();
			checkColorDepth = false;
			
			if (cbs < 24)
				throw new RuntimeException(){
					//set localized message for better report in netbeans
					@Override
					public String getLocalizedMessage() {
						return "Color depth is lower than 24 bits, picking does not work";
					}
				};			
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
				{
					continue;
				}
				double[] bounds = actor.GetBounds();
				BoundingBox box = new BoundingBox();
				box.setLower(bounds[0], bounds[2], bounds[4]);
				box.setUpper(bounds[1], bounds[3], bounds[5]);
				if (!pickContext.intersect(box))
					actor.PickableOff();
			}
		}

		vtkHardwareSelector selector = new vtkHardwareSelector();
		selector.SetRenderer(canvas.GetRenderer());
		int xMin = Math.min(firstPoint[0], secondPoint[0]);
		int xMax = Math.max(firstPoint[0], secondPoint[0]);
		int yMin = Math.min(firstPoint[1], secondPoint[1]);
		int yMax = Math.max(firstPoint[1], secondPoint[1]);
		selector.SetArea(xMin, yMin, xMax, yMax);
		selector.SetFieldAssociation(1);
		canvas.lock();
		vtkSelection selection = selector.Select();
		canvas.unlock();

		if(Boolean.getBoolean("sun.java2d.opengl"))
		{
			//Bug of VTK or Java ? If java3d opengl is enabled the
			//vtkHardwareSelector blank the canvas so we need to force a refresh.
			//UpdateLight for the refresh
			canvas.UpdateLight();
			canvas.Render();
		}

		if (actorFiltering)
		{
			vtkActorCollection actors = canvas.GetRenderer().GetActors();
			actors.InitTraversal();
			int j = 0;
			for (vtkActor actor; (actor = actors.GetNextActor()) != null; ++j)
			{
				actor.SetPickable(pickableActorBackup[j]);
			}
		}

		// Find the ID Selection of the actor
		for (int i = 0; i < selection.GetNumberOfNodes(); ++i)
		{
			vtkSelectionNode child = selection.GetNode(i);
			vtkProp prop = (vtkProp) child.GetProperties().Get(child.PROP());
			if (prop != null)
			{
				AbstractNode node = idActorToNode.get(prop.GetVTKId());

				if (node != null)
				{
					vtkIdTypeArray ids = (vtkIdTypeArray) child.GetSelectionList();
					child = null;
					int[] values = Utils.getValues(ids);
					if(pickContext.isOneCell() && values.length > 1)
						values = new int[]{values[0]};

					node.setCellSelection(pickContext, values);
					LOGGER.finest("Actor picked id: "+prop.GetVTKId());
					LOGGER.finest("Picked node: "+node);
					if(pickContext.isOneCell() && values.length > 0)
						break;
				}
			}
		}
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
