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
import java.awt.Component;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import vtk.vtkActor;
import vtk.vtkProp;
import vtk.vtkRenderer;

/**
 * Manage multi canvas drawing.
 * @author Julian Ibarz
 */
public abstract class MultiCanvas implements Node.ActorListener, Node.ChildCreationListener {
	private final static Logger LOGGER = Logger.getLogger(MultiCanvas.class.getName());
	protected final ArrayList<Canvas> listCanvas = new ArrayList<Canvas>();
	private final ArrayList<vtkProp> props = new ArrayList<vtkProp>();
	protected Color selectionColor = Color.RED;
	
	public MultiCanvas()
	{
		
	}

	@Override
	public void childCreated(AbstractNode child)
	{
		addNode(child);
	}

	@Override
	public void childDeleted(AbstractNode child)
	{
		removeNode(child);
	}
	
	void addNode(AbstractNode node)
	{
		node.addActorListener(this);
	}
	
	void removeNode(AbstractNode node)
	{
		node.removeActorListener(this);
	}

	public void addProp(vtkProp prop)
	{
		props.add(prop);
		for(Canvas canvas : listCanvas)
		{
			canvas.lock();
			canvas.GetRenderer().AddViewProp(prop);
			canvas.unlock();
		}
	}
	
	public void deleteProp(vtkProp prop)
	{
		props.remove(prop);
		
		for(Canvas canvas : listCanvas)
		{
			canvas.lock();
			canvas.GetRenderer().RemoveViewProp(prop);
			canvas.unlock();
		}
	}
	
	@Override
	public void actorCreated(AbstractNode node, vtkActor actor)
	{
		if (LOGGER.isLoggable(Level.FINE))
			LOGGER.log(Level.FINE, "Create actor id="+actor.GetVTKId()+" hashcode="+Integer.toHexString(actor.hashCode()));
		addProp(actor);
	}

	@Override
	public void actorDeleted(AbstractNode node, vtkActor actor)
	{
		if (LOGGER.isLoggable(Level.FINE))
			LOGGER.log(Level.FINE, "Delete actor id="+actor.GetVTKId()+" hashcode="+Integer.toHexString(actor.hashCode()));
		deleteProp(actor);
	}

	/**
	 * 
	 * @param canvas
	 */
	public void addCanvas(Canvas canvas)
	{
		if (LOGGER.isLoggable(Level.FINE))
		{
			LOGGER.log(Level.FINE, "Adding one canvas : " + canvas);
			LOGGER.log(Level.FINE, "Number of actors : " + canvas.GetRenderer().GetViewProps().GetNumberOfItems());
		}
		
		listCanvas.add(canvas);
		canvas.lock();
		vtkRenderer renderer = canvas.GetRenderer();
		for(vtkProp actor : props)
		{
			renderer.AddViewProp(actor);
		}
		canvas.unlock();
		
		if (LOGGER.isLoggable(Level.FINE))
			LOGGER.log(Level.FINE, "Number of actors after : " + canvas.GetRenderer().GetViewProps().GetNumberOfItems());
	}
	
	public void render()
	{
		for(Canvas canvas : listCanvas)
			canvas.RenderSecured();
	}
	
	
	public void removeCanvas(Canvas canvas)
	{
		if (LOGGER.isLoggable(Level.FINE))
		{
			LOGGER.log(Level.FINE, "Removing one canvas : " + canvas);
			LOGGER.log(Level.FINE, "Number of actors : " + canvas.GetRenderer().GetViewProps().GetNumberOfItems());
		}

		listCanvas.remove(canvas);
		
		// Dot not lock the canvas when doing this because the 3d context can be already removed
		vtkRenderer renderer = canvas.GetRenderer();
		for(vtkProp actor : props)
		{
			renderer.RemoveViewProp(actor);
		}
		canvas.RenderSecured();
	}
		
	public void lockCanvas()
	{
		for(Canvas canvas : listCanvas)
			canvas.lock();
	}
	
	public void unlockCanvas()
	{
		for(Canvas canvas : listCanvas)
			canvas.unlock();
	}
	
	protected Canvas retrieveCanvas(ComponentEvent e)
	{
		Component c = e.getComponent();
		if(c instanceof Canvas)
			return (Canvas)c;
		else
			throw new NoSuchElementException("Found "+c.getClass()+
				" when "+Canvas.class+" expected.");
	}
}
