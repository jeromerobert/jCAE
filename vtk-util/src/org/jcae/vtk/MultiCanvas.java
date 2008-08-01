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
import vtk.vtkActor;
import vtk.vtkProp;
import vtk.vtkRenderer;

/**
 * Manage multi canvas drawing.
 * @author Julian Ibarz
 */
public abstract class MultiCanvas implements Node.ActorListener, Node.ChildCreationListener {
	protected final ArrayList<Canvas> listCanvas = new ArrayList<Canvas>();
	private final ArrayList<vtkProp> props = new ArrayList<vtkProp>();
	protected Color selectionColor = Color.RED;
	
	public MultiCanvas()
	{
		
	}

	public void childCreated(AbstractNode abstractNode)
	{
		addNode(abstractNode);
		
		if(abstractNode instanceof Node)
		{
			Node node = (Node)abstractNode;
			node.addChildCreationListener(this);
		}
	}

	public void childDeleted(AbstractNode abstractNode)
	{
		addNode(abstractNode);
		
		if(abstractNode instanceof Node)
		{
			Node node = (Node)abstractNode;
			node.removeChildCreationListener(this);
		}
	}
	
	public void addNode(AbstractNode node)
	{
		node.addActorListener(this);
	}
	
	public void removeNode(AbstractNode node)
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
	
	public void actorCreated(AbstractNode node, vtkActor actor)
	{
		//System.out.println("ACTOR ADDED " + prop.GetVTKId());
		
		addProp(actor);
	}

	public void actorDeleted(AbstractNode node, vtkActor actor)
	{
		//System.out.println("ACTOR DELETED " + prop.GetVTKId());
		
		deleteProp(actor);
	}

	public void actorHighLighted(AbstractNode node, vtkActor actor)
	{
		//System.out.println("ACTOR HIGHLIGHTED !");
		//Utils.setColorActor(actor, selectionColor);
	}
	
	/**
	 * 
	 * @param canvas
	 */
	public void addCanvas(Canvas canvas)
	{
		listCanvas.add(canvas);
		canvas.lock();
		vtkRenderer renderer = canvas.GetRenderer();
		for(vtkProp actor : props)
		{
			renderer.AddViewProp(actor);
		}
		canvas.unlock();
	}
	
	public void render()
	{
		for(Canvas canvas : listCanvas)
			canvas.RenderSecured();
	}
	
	
	public void removeCanvas(Canvas canvas)
	{
		int index = listCanvas.indexOf(canvas);		
		listCanvas.remove(index);
		
		// Dot not lock the canvas when doing this because the 3d context can be already removed
		vtkRenderer renderer = canvas.GetRenderer();
		for(vtkProp actor : props)
		{
			renderer.RemoveViewProp(actor);
		}
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
