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

import java.util.ArrayList;
import java.util.List;
import vtk.vtkActor;
import vtk.vtkCanvas;
import vtk.vtkFloatArray;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkPolyDataNormals;

/**
 *
 * @author ibarz
 */
public abstract class AbstractNode {
// Datas
	protected final AbstractNode parent;
	private final ArrayList<ActorListener> actorListeners = new ArrayList<ActorListener>();
	//private final ArrayList<DataListener> dataListeners = new ArrayList<DataListener>();
	private boolean manager = false;
	protected vtkActor actor = null; // If actor != null the node is not a manager
	protected vtkPolyDataMapper mapper = null;
	protected vtkPolyData data = null;

	protected long lastUpdate = 0;
	
	protected long modificationTime = System.nanoTime();
	protected boolean visible = true;
	protected int[] selectionPoint = new int[0];
	protected long selectionTime = 0   ;
	protected boolean selected;
	
	protected vtkActor selectionHighLighter = null;
	
	public static interface ActorListener
	{
		void actorCreated(AbstractNode node, vtkActor actor);
		void actorDeleted(AbstractNode node, vtkActor actor);
		void actorHighLighted(AbstractNode node, vtkActor actor);
	}
		
	/*public static interface DataListener
	{
		void dataModified(AbstractNode node, vtkPolyData data);
	}*/

	public AbstractNode(AbstractNode parent)
	{
		this.parent = parent;
		if(parent != null) parent.addChild(this);
	}

	public AbstractNode getRoot()
	{
		if(this.parent != null)
			return this.parent.getRoot();
		else return this;
	}
	
	public AbstractNode getParent()
	{
		return this.parent;
	}
	
	protected abstract void addChild(AbstractNode parent);

	public void addActorListener(ActorListener listener)
	{
		actorListeners.add(listener);
	}
	
	public void removeActorListener(ActorListener listener)
	{
		actorListeners.remove(listener);
	}
	
	/*public void addDataListener(DataListener listener)
	{
		dataListeners.add(listener);
	}
	
	public void removeDataListener(DataListener listener)
	{
		dataListeners.remove(listener);
	}*/
	
	public vtkActor getActor()
	{
		return this.actor;
	}

	protected void fireActorCreated(vtkActor actor)
	{
		/*System.out.println("NUMBER OF ACTOR LISTENERS : " + actorListeners.size());
		for(ActorListener listener : actorListeners)
		{
			System.out.println("TYPE : " + listener.getClass().getSimpleName());
		}*/
		
		for (ActorListener listener : actorListeners)
			listener.actorCreated(this, actor);
	}

	protected void fireActorDeleted(vtkActor actor)
	{
		for (ActorListener listener : actorListeners)
			listener.actorDeleted(this, actor);
	}
	
	/*protected void fireDataModified(vtkPolyData data)
	{
		for(DataListener listener : dataListeners)
			listener.dataModified(this, data);
	}*/
	
	protected void fireActorHighLighted(vtkActor actor)
	{
		for (ActorListener listener : actorListeners)
			listener.actorHighLighted(this, actor);
	}
	
	public void setManager(boolean manager)
	{
		if(this.manager = manager)
			return;
		
		this.manager = manager;
		
		if(!this.manager)
			deleteDatas();
		
		modified();
	}

	public boolean isVisible()
	{
		return visible;
	}

	public void setVisible(boolean visible)
	{
		//System.out.println("this.visible = " + this.visible);
		if(this.visible == visible)
			return;
		
		//System.out.println("CHANGE VISIIBLITY " + visible);
		
		this.visible = visible;
		
		if(actor != null)
			actor.SetVisibility((visible) ? 1 : 0);
		
		modified();
	}
	
	protected abstract void refresh();
	
	protected long getModificationTime()
	{
		return this.modificationTime;
	}
	
	public void modified()
	{
		this.modificationTime = System.nanoTime();
	}
	
	protected void createData(LeafNode.DataProvider dataProvider)
	{
		data = new vtkPolyData();
		data.SetPoints(Utils.createPoints(dataProvider.getNodes()));
		data.SetVerts(Utils.createCells(dataProvider.getNbrOfVertice(), dataProvider.getVertice()));
		data.SetLines(Utils.createCells(dataProvider.getNbrOfLines(), dataProvider.getLines()));
		data.SetPolys(Utils.createCells(dataProvider.getNbrOfPolys(), dataProvider.getPolys()));
		
		//System.out.println("DATA RECREATED : " + data.GetNumberOfPoints());
		
		if(dataProvider.getNormals() == null)
			return;
		
		// Compute normals that are not given
		vtkPolyDataNormals algoNormals = new vtkPolyDataNormals();
		algoNormals.SetInput(data);
		algoNormals.SplittingOff();
		algoNormals.FlipNormalsOff();
		algoNormals.AutoOrientNormalsOff();
		algoNormals.ComputePointNormalsOn();
		algoNormals.Update();

		data = algoNormals.GetOutput();
		
		vtkFloatArray computedNormals = (vtkFloatArray) data.GetPointData().GetNormals();
		float[] javaComputedNormals = computedNormals.GetJavaArray();
		float[] javaNormals = dataProvider.getNormals();
		
		// If the normals are not computed change them by the normals computed by the meshes
		for(int i = 0 ; i < javaComputedNormals.length / 3 ; i += 3)
		{
			if(javaNormals[i] == 0. && javaNormals[i + 1] == 0. && javaNormals[i + 2] == 0.)
			{
				javaNormals[i] = javaComputedNormals[i];
				javaNormals[i + 1] = javaComputedNormals[i  + 1];
				javaNormals[i + 2] = javaComputedNormals[i + 2];
			}
		}
		
		vtkFloatArray normals = new vtkFloatArray();
		normals.SetNumberOfComponents(3);
		normals.SetJavaArray(javaNormals);
		
		data.GetPointData().SetNormals(normals);
		//fireDataModified(data);
	}
	
	protected void unHighLightSelection()
	{
		if(selectionHighLighter == null)
			return;
		
		//System.out.println("DELETE HIGHLIGHT SELECTION");
		
		fireActorDeleted(selectionHighLighter);
		selectionHighLighter = null;
	}
	protected void refreshMapper()
	{
		mapper.SetInput(data);		
		mapper.Update();
	}
	protected abstract void refreshData();
	public abstract void highLightSelection();
	
	protected void refreshActor()
	{
		refreshData();
		boolean actorCreated = false;
		
		//System.out.println("REFRESH ACTOR !");
		if(actor == null)
		{
			actorCreated = true;
			//System.out.println("CREATING AN ACTOR !");
			actor = new vtkActor();
			mapper = new vtkPolyDataMapper();
			actor.SetMapper(mapper);
			actor.SetVisibility((visible) ? 1 : 0);
		}
		refreshMapper();

		// Call fire after the map creation
		if(actorCreated)
			fireActorCreated(actor);

			
		/*System.out.println("number of lines : " + data.GetNumberOfLines());
		System.out.println("number of polys : " + data.GetNumberOfPolys());
		System.out.println("number of nodes : " + data.GetNumberOfPoints());*/
		/*mapper.SetColorModeToDefault();
		mapper.ScalarVisibilityOff();
		actor.GetProperty().SetColor(1, 0, 0);*/
	}
	
	public boolean isSelected()
	{
		return selected;
	}
	
	protected void deleteDatas()
	{
		data = null;
		if(actor != null)
		{
			fireActorDeleted(actor);
			actor = null;
		}
		mapper = null;
	}

	public abstract List<LeafNode> getLeaves();
	
	/**
	 * Find the node that contains the cellID
	 * @param cellID
	 * @return
	 */
	protected abstract LeafNode getNode(int cellID);
	
	protected void pickPoint(vtkCanvas canvas, int[] firstPoint, int[] secondPoint, double tolerance)
	{
		/*vtkSelectVisiblePoints selector = new vtkSelectVisiblePoints();
		selector.ReleaseDataFlagOn();

		data.Update();
		selector.SetInput(data);
		selector.SelectionWindowOn();
		selector.SetSelection(firstPoint[0], secondPoint[0], secondPoint[1], firstPoint[1]);
		selector.SetRenderer(canvas.GetRenderer());
		selector.
		
		selector.SetTolerance(Utils.computeTolerance(canvas, tolerance));
		
		// We have to render without the highlight and then update to have the points
		canvas.lock();
		canvas.GetRenderer().Render();
		selector.Update();
		canvas.unlock();
		
		// We have putted the ids in the field data of points on the creation of the vtkPolyData with vtkIdFilter
		vtkPolyData data = selector.GetOutput();
		data.ReleaseDataFlagOn();
		vtkIdTypeArray ids = (vtkIdTypeArray) data.GetPointData().GetAbstractArray(fieldDataName);

		Utils.getValues(ids);*/
	}
	
	protected abstract void manageHighLight();
			
	protected void highLight()
	{
		assert actor != null;
		
		fireActorHighLighted(actor);
	}
	
	protected void unHighLight()
	{
		assert actor != null;
	}
	
	protected long selectionTime()
	{
		return selectionTime;
	}
	
	public void select()
	{
		if(selected)
			return;
		
		selected = true;
		selectionTime = System.nanoTime();
	}

	public void unSelect()
	{
		if(!selected)
			return;
		
		selected = false;
		selectionTime = System.nanoTime();
	}
	protected abstract void manageSelection(int[] cellSelection);
	
	public boolean isManager()
	{
		return manager;
	}
}
