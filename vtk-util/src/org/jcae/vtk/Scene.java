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
import gnu.trove.TLongObjectHashMap;
import java.util.ArrayList;
import vtk.vtkActor;
import vtk.vtkCanvas;
import vtk.vtkIdTypeArray;
import vtk.vtkProp;
import vtk.vtkSelection;
import vtk.vtkVisibleCellSelector;

/**
 * This class is used to make picking on a tree node. It permit to keep the association
 * between a node and his actor with a map (idActorToNode).
 * @author ibarz
 */
public class Scene implements AbstractNode.ActorListener {
	private TLongObjectHashMap<AbstractNode> idActorToNode = new TLongObjectHashMap<AbstractNode>();
	
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
	
	
	
	public void pick(vtkCanvas canvas, int[] firstPoint, int []secondPoint)
	{
		vtkVisibleCellSelector selector = new vtkVisibleCellSelector();
		selector.SetRenderer(canvas.GetRenderer());
		selector.SetArea(firstPoint[0], firstPoint[1], secondPoint[0],
				secondPoint[1]);
		selector.SetRenderPasses(0, 1, 0, 0, 1, 0);

		canvas.lock();
		selector.Select();
		canvas.unlock();
		
		//long begin = System.currentTimeMillis();
		vtkIdTypeArray idArray = new vtkIdTypeArray();
		selector.GetSelectedIds(idArray);

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
				
				if(node != null)
				{
					vtkIdTypeArray ids = (vtkIdTypeArray)child.GetSelectionList();
					node.manageSelection(Utils.getValues(ids));
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
}
