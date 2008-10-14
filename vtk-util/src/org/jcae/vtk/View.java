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


import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import vtk.vtkInteractorStyleRubberBand3D;
import vtk.vtkInteractorStyleTrackballCamera;
import vtk.vtkPlaneCollection;

/**
 *
 * @author Julian Ibarz
 */
public class View extends Canvas {
	private final static Logger LOGGER = Logger.getLogger(View.class.getName());
	protected final ArrayList<Viewable> viewables = new ArrayList<Viewable>();
	protected Viewable currentViewable;
	private final CameraManager cameraManager = new CameraManager(this);
	private MouseMode mouseMode = MouseMode.POINT_SELECTION;
	private Point pressPosition;
	private Point releasePosition;
	private boolean interactive = true;
	private boolean appendSelection;
	
	public enum MouseMode
	{
		POINT_SELECTION,
		RECTANGLE_SELECTION,
		CLIPPING_PLANE,
		CHANGE_ROTATION_CENTER
	}
	
	public View()
	{
		super();
		
		// By default the translucent objects can be picked
		GetRenderer().PickTranslucentOn();
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		super.keyPressed(e);
		
		if(!interactive)
			return;
		
		switch (e.getKeyCode())
		{
			case KeyEvent.VK_SPACE:
				setMouseMode(MouseMode.RECTANGLE_SELECTION);
				break;
			case KeyEvent.VK_C:
				setMouseMode(MouseMode.CLIPPING_PLANE);
				break;
			case KeyEvent.VK_R:
				setMouseMode(MouseMode.CHANGE_ROTATION_CENTER);
				break;
		}
	}

	public MouseMode getMouseMode()
	{
		return mouseMode;
	}
	
	@Override
	public void mousePressed(MouseEvent e)
	{
		super.mousePressed(e);
		
		if(!interactive)
			return;
		
		pressPosition = new Point(e.getPoint());
		pressPosition.y = e.getComponent().getHeight() - pressPosition.y;
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		super.mouseReleased(e);
		
		if(!interactive)
			return;
		
		if(e.getButton() != MouseEvent.BUTTON1)
			return;
		
		releasePosition = new Point(e.getPoint());
		releasePosition.y = e.getComponent().getHeight() - releasePosition.y;
	
		switch(mouseMode)
		{
			case RECTANGLE_SELECTION:
				if(currentViewable != null)
				{
					if(pressPosition.equals(releasePosition))
						currentViewable.pointSelection(this, pressPosition);
					else
						currentViewable.surfaceSelection(this, pressPosition, releasePosition);
				}
				setMouseMode(MouseMode.POINT_SELECTION);
				break;
			case CLIPPING_PLANE:
				vtkPlaneCollection planes = Utils.computeClippingPlane(this,
					pressPosition, releasePosition);
				for(Viewable viewable : getViewables())
					viewable.setClippingPlanes(planes);
				setMouseMode(MouseMode.POINT_SELECTION);
				RenderSecured();
		}
		
		// If the press and release positions are not close, this is
		// not a selection
		if(pressPosition.distance(releasePosition) > 5.)
			return;
		
		switch(mouseMode)
		{
			case POINT_SELECTION:
				if(currentViewable != null)
				{
					if (!e.isControlDown())
						currentViewable.unselectAll();
					currentViewable.pointSelection(this, releasePosition);
				}
				break;
			case CHANGE_ROTATION_CENTER:
				cameraManager.centerRotationSelection(releasePosition);
		}
	}

	public boolean isAppendSelection()
	{
		return appendSelection;
	}

	public void setAppendSelection(boolean appendSelection)
	{
		this.appendSelection = appendSelection;
		
		for(Viewable viewable : viewables)
			viewable.setAppendSelection(appendSelection);
	}
	
	public CameraManager getCameraManager()
	{
		return cameraManager;
	}

	public boolean isInteractive()
	{
		return interactive;
	}

	public void setInteractive(boolean interactive)
	{
		this.interactive = interactive;
	}

	public void remove(Viewable interactor)
	{
		LOGGER.fine("Remove an interactor, left " + (viewables.size() - 1) + "viewables");
		viewables.remove(interactor);
		interactor.removeCanvas(this);
		
		if(viewables.size() != 0)
			setCurrentViewable(viewables.get(0));
		else
			setCurrentViewable(null);
	}
      

	public void setMouseMode(MouseMode mode)
	{
		boolean actualIsPoint = this.mouseMode == MouseMode.POINT_SELECTION ||
				this.mouseMode == MouseMode.CHANGE_ROTATION_CENTER;

		boolean futureIsPoint = mode == MouseMode.POINT_SELECTION ||
				mode == MouseMode.CHANGE_ROTATION_CENTER;

		if (actualIsPoint && !futureIsPoint)
		{
			//getIren().GetInteractorStyle().EnabledOff();
			getIren().SetInteractorStyle(new vtkInteractorStyleRubberBand3D());
		}
		else if (!actualIsPoint && futureIsPoint)
		{
			//getIren().GetInteractorStyle().EnabledOff();
			getIren().SetInteractorStyle(new vtkInteractorStyleTrackballCamera());
		}

		this.mouseMode = mode;
	}

	
	/**
	 * Add a viewable to this view and make his canvas the view canvas and make it current. If viewable implements
	 * PropertyChangeListener it will be notified when the global node selection
	 * changes.
	 * 
	 * @param interactor 
	 * @param viewable
	 */
	public void add(Viewable interactor)
	{
		// If already added return
		if(viewables.contains(interactor))
			return;
		
		viewables.add(interactor);
		
		interactor.setAppendSelection(appendSelection);
		interactor.addCanvas(this);
		cameraManager.fitAll();
		// Set it the current viewable
		setCurrentViewable(interactor);
		
		// TODO : know why the relative axis disappear, this is a hack to make visible the axes
		boolean isVisible = cameraManager.isRelativeAxisVisible();
		
		if(isVisible)
		{
			cameraManager.setRelativeAxisVisible(false);
			cameraManager.setRelativeAxisVisible(true);
		}
	}

	/**
	 * @param viewable
	 */
	public void setCurrentViewable(Viewable viewable)
	{
		if(this.currentViewable != null)
			this.currentViewable.setPickable(false);
		
		this.currentViewable = viewable;
		if(this.currentViewable != null)
			this.currentViewable.setPickable(true);
	}
	
	public Viewable getCurrentViewable()
	{
		return currentViewable;
	}
	
	public List<Viewable> getViewables()
	{
		return viewables;
	}
	
	/**
	 * Notify Viewables that they should no longer concider as being displayed
	 * in this view. This must called before destroying a view.
	 */
	public void detachAllViewables()
	{
		for(Viewable v:viewables)
			v.removeCanvas(this);		
	}
}
