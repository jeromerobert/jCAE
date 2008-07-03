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

import gnu.trove.TByteObjectHashMap;
import gnu.trove.TLongObjectHashMap;
import gnu.trove.TObjectByteHashMap;
import java.awt.Point;
import javax.swing.SwingUtilities;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import org.jcae.geometry.BoundingBox;
import org.jcae.geometry.Bounds;
import vtk.vtkActor;
import vtk.vtkActorCollection;
import vtk.vtkCanvas;
import vtk.vtkFloatArray;
import vtk.vtkIdTypeArray;
import vtk.vtkPlaneCollection;
import vtk.vtkProp;
import vtk.vtkSelection;
import vtk.vtkVisibleCellSelector;

/**
 * This class is used to make picking on a tree node. It permit to keep the association
 * between a node and his actor with a map (idActorToNode).
 * @author ibarz
 */
public class Scene implements AbstractNode.ActorListener
{

	private TLongObjectHashMap<AbstractNode> idActorToNode = new TLongObjectHashMap<AbstractNode>();
	private boolean actorFiltering = true;
	private vtkPlaneCollection planes = null;
	/**
	 * Store the previous pickability of the scene.
	 * 1 means the node is pickable
	 * -1 means the node is not pickable
	 * other value means we don't know
	 */
	private TObjectByteHashMap<AbstractNode> pickBackup = null;

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
		idActorToNode.put(actor.GetVTKId(), node);
		if (planes != null)
			actor.GetMapper().SetClippingPlanes(planes);
	}

	public void actorDeleted(AbstractNode node, vtkActor actor)
	{
		idActorToNode.remove(actor.GetVTKId());
	}

	public void actorHighLighted(AbstractNode node, vtkActor actor)
	{
		// Do nothing
	}

	public void actorUnHighLighted(AbstractNode node, vtkActor actor)
	{
		// Do nothing
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
			for (AbstractNode node : nodes)
			{
				boolean isPickable = pickable;
				if (pickBackup != null)
				{
					byte pickability = pickBackup.get(node);
					if (pickability == 1)
						isPickable = true;
					else if (pickability == -1)
						isPickable = false;
				}
				node.setPickable(isPickable);
			}
		else
		{
			pickBackup = new TObjectByteHashMap<AbstractNode>(nodes.length);

			for (AbstractNode node : nodes)
			{
				byte isPickable = (node.isPickable()) ? (byte) 1 : (byte) -1;
				pickBackup.put(node, isPickable);
				node.setPickable(false);
			}
		}

	}

	public void setActorFiltering(boolean actorFiltering)
	{
		this.actorFiltering = actorFiltering;
	}

	/**
	 * Warning : If you are making highlight with offSet (by default this is down) then the selection will not take in
	 * case the highlighted objects because the z-buffer is not cleaned and so the normal geometry
	 * will not be drawned for selection because the highlighted geometry is nearest of the camera
	 * due to the offset. If you want bypass this you have to take care of the highlighted objects :
	 * _ draw them in rendering selection i.e. make them pickable.
	 * _ find the initial geometry corresponding to the selected highlighted object.
	 * @param canvas
	 * @param firstPoint
	 * @param secondPoint
	 */
	public void pick(vtkCanvas canvas, int[] firstPoint, int[] secondPoint)
	{
		/*if(canvas instanceof Canvas)
		/{
		((Canvas)canvas).RenderSecured();
		}
		else throw new RuntimeException("PWET");   */
		/*if (!SwingUtilities.isEventDispatchThread())
		throw new RuntimeException("NOT IN THE GOOD THREAD");
		vtkFloatArray beforeNative = new vtkFloatArray();
		canvas.GetRenderWindow().GetZbufferData(firstPoint[0], firstPoint[1], secondPoint[0], secondPoint[1], beforeNative);
		float[] before = beforeNative.GetJavaArray();*/

		/*if(canvas instanceof Canvas)
		{
		((Canvas)canvas).RenderSecured();
		}*/
		vtkVisibleCellSelector selector = new vtkVisibleCellSelector();
		selector.SetRenderer(canvas.GetRenderer());
		selector.SetArea(firstPoint[0], firstPoint[1], secondPoint[0],
				secondPoint[1]);
		selector.SetRenderPasses(0, 1, 0, 0, 1, 0);

		int[] pickBackup = null;
		if (actorFiltering)
		{
			boolean pointPicking = false;
			if (firstPoint[0] == secondPoint[0] && firstPoint[1] == secondPoint[1])
				pointPicking = true;

			long begin = System.currentTimeMillis();
			vtkActorCollection actors = canvas.GetRenderer().GetActors();
			pickBackup = new int[actors.GetNumberOfItems()];
			actors.InitTraversal();

			Point3d pickOrigin = new Point3d();
			Vector3d pickDirection = new Vector3d();
			Bounds frustum = null;

			if (pointPicking)
				Utils.computeRay(canvas.GetRenderer(), new Point(firstPoint[0], firstPoint[1]), pickOrigin, pickDirection);
			else
				frustum = Utils.computePolytope(Utils.computeVerticesFrustum(firstPoint[0], firstPoint[1], secondPoint[0], secondPoint[1], canvas.GetRenderer()));

			int j = 0;
			for (vtkActor actor; (actor = actors.GetNextActor()) != null; ++j)
			{
				if ((pickBackup[j] = actor.GetPickable()) == 0)
					continue;

				double[] bounds = actor.GetBounds();
				BoundingBox box = new BoundingBox();
				box.setLower(bounds[0], bounds[2], bounds[4]);
				box.setUpper(bounds[1], bounds[3], bounds[5]);

				if (pointPicking)
					if (!box.intersect(pickOrigin, pickDirection))
						actor.PickableOff();
					else
						System.out.println("ONE NODE PICKED !");
				else
					if (!frustum.intersect(box))
						actor.PickableOff();
			}
		/*actors.InitTraversal();
		int nbrPick = 0;
		int nbrPolys = 0;
		for(vtkActor actor ; (actor = actors.GetNextActor()) != null ; ++j)
		{
		if(actor.GetPickable() == 1)
		nbrPick++;
		nbrPolys += actor.GetMapper().GetInputAsDataSet().GetNumberOfCells();
		}
		System.out.println("NBR OF PICKED ACTORS : " + nbrPick);
		System.out.println("NBR OF ACTORS : " + actors.GetNumberOfItems());
		System.out.println("NUMBER OF POLYS : " + nbrPolys);
		System.out.println("TIME TO FILTER : " + (System.currentTimeMillis() - begin));*/
		}

		canvas.lock();
		canvas.GetRenderer().ClearDepthForSelectionOff();
		selector.Select();
		canvas.unlock();

		/*vtkFloatArray afterNative = new vtkFloatArray();
		canvas.GetRenderWindow().GetZbufferData(firstPoint[0], firstPoint[1], secondPoint[0], secondPoint[1], afterNative);
		
		float[] after = afterNative.GetJavaArray();
		
		System.out.println("Z-BUFFER : ");
		for(int i = 0 ; i < before.length ; ++i)
		{
		System.out.println(before[i] + " -> " + after[i]);
		}
		
		System.out.println("after components " + afterNative.GetNumberOfComponents());
		System.out.println("after tuples " + afterNative.GetNumberOfTuples());
		 */
		//long begin = System.currentTimeMillis();
		vtkIdTypeArray idArray = new vtkIdTypeArray();
		selector.GetSelectedIds(idArray);

		if (actorFiltering)
		{
			vtkActorCollection actors = canvas.GetRenderer().GetActors();
			actors.InitTraversal();
			int j = 0;
			for (vtkActor actor; (actor = actors.GetNextActor()) != null; ++j)
				actor.SetPickable(pickBackup[j]);
		}

		System.out.println("NUMBER OF ACTORS SELECTED : " + (idArray.GetDataSize() / 4));

		// If no selection was made leave
		if (idArray.GetDataSize() == 0)
			return;

		vtkSelection selection = new vtkSelection();
		selection.ReleaseDataFlagOn();
		selector.GetSelectedIds(selection);

		// Find the ID Selection of the actor
		int IDActor = -1;
		for (int i = 0; i < selection.GetNumberOfChildren(); ++i)
		{
			vtkSelection child = selection.GetChild(i);
			IDActor = child.GetProperties().Get(selection.PROP_ID());
			vtkProp prop = selector.GetActorFromId(IDActor);

			if (prop != null)
			{
				AbstractNode node = idActorToNode.get(prop.GetVTKId());

				if (node != null)
				{
					vtkIdTypeArray ids = (vtkIdTypeArray) child.GetSelectionList();
					node.manageSelection(Utils.getValues(ids));
					System.out.println("ACTOR ID PICKED : " + prop.GetVTKId());
					System.out.println("NODE PICKED : " + node);
				}
			}
		}
	//long end = System.currentTimeMillis();
	//System.out.println("TIME SELECTING DISPATCH : " + (begin - end));



	/*int[] globalIDs = Utils.getValues(idArray);
	TIntArrayList ids = new TIntArrayList(globalIDs.length / 4);
	int nbOfSelection = 0;
	for (int i = 0; i < globalIDs.length / 4; ++i)
	if (globalIDs[i * 4 + 1] == IDActor)
	{
	ids.add(globalIDs[i * 4 + 3]);
	++nbOfSelection;
	}*/

	// If no selection was maded leave
		/*if (nbOfSelection == 0)
	return new int[0];
	
	return ids.toNativeArray();*/
	}

	public void setClippingPlanes(vtkPlaneCollection planes)
	{
		AbstractNode[] nodes = new AbstractNode[idActorToNode.size()];
		idActorToNode.getValues(nodes);
		for (AbstractNode node : nodes)
			node.getActor().GetMapper().SetClippingPlanes(planes);
	}
}
