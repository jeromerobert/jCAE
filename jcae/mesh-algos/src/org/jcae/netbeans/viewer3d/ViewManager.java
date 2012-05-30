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
 * (C) Copyright 2006-2010, by EADS France
 */
package org.jcae.netbeans.viewer3d;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;
import org.jcae.netbeans.viewer3d.actions.SelectViewable;
import org.jcae.vtk.View;
import org.jcae.vtk.Viewable;
import org.openide.ErrorManager;
import org.openide.util.Lookup;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * Patterns : Singleton, Factory
 * This class manage the views and the viewables. It creates the views and inform if the current view
 * or current viewable has changed.
 * @author Jerome Robert and Julian Ibarz
 */
@ServiceProvider(service=ViewManager.class)
public class ViewManager
{
	private class View3D extends TopComponent
	{
		private final View view;

		protected View3D(View view)
		{
			setLayout(new BorderLayout());
			this.view = view;
			add(view, BorderLayout.CENTER);
		}

		@Override
		public int getPersistenceType()
		{
			return TopComponent.PERSISTENCE_NEVER;
		}

		public View getView()
		{
			return view;
		}

		@Override
		protected void componentClosed()
		{
			view.setVisible(false);
			view.detachAllViewables();
			currentView = null;
			super.componentClosed();
		}

		/* (non-Javadoc)
		 * @see org.openide.windows.TopComponent#componentActivated()
		 */
		@Override
		protected void componentActivated()
		{
			if(!hasView() || getCurrentView() != getView())
				setCurrentView(getView());
		}

		@Override
		public String toString()
		{
			return this.getName();
		}

		@Override
		public boolean canClose() {
			return false;
		}
	}
	/**
	 * Make the stuff to connect the CurrentViewable notifications to add and remove viewables of View.
	 */
	private class MyView extends View
	{
		@Override
		public void add(Viewable viewable)
		{
			super.add(viewable);		
			SystemAction.get(SelectViewable.class).refresh();
			fireCurrentViewableChanged();
		}
		

		@Override
		public void remove(Viewable interactor)
		{
			super.remove(interactor);
			fireCurrentViewableChanged();
		}

		@Override
		public void setCurrentViewable(Viewable viewable)
		{
			super.setCurrentViewable(viewable);
			fireCurrentViewableChanged();
		}
		
	}
	
	private int counter = 0;
	protected View currentView = null;
	private ArrayList<CurrentViewChangeListener> viewChangeListeners = new ArrayList<CurrentViewChangeListener>();
	private ArrayList<CurrentViewableChangeListener> viewableChangeListeners = new ArrayList<CurrentViewableChangeListener>();

	/**
	 * Shortcut for Lookup.getDefault().lookup(ViewManager.class)
	 */
	public static ViewManager getDefault()
	{
		return Lookup.getDefault().lookup(ViewManager.class);
	}

	public ViewManager()
	{
		try
		{
			//Starting with Java 7, jawt is not automatically loaded and it's
			//required by VTK.
			System.loadLibrary("jawt");
		}
		catch(UnsatisfiedLinkError e)
		{
			if(e.getMessage() == null ||
				!e.getMessage().contains("already loaded in another classloader"))
				Logger.getLogger(ViewManager.class.getName()).log(Level.SEVERE, null, e);
		}
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
	}

	public View[] getAllView()
	{
		ArrayList<View> views = new ArrayList<View>();
		Iterator it = WindowManager.getDefault().getModes().iterator();
		while (it.hasNext())
		{
			Mode mode = (Mode) it.next();
			TopComponent[] t = mode.getTopComponents();

			for (int i = 0; i < t.length; i++)
				if (t[i] instanceof View3D)
					views.add(((View3D) t[i]).getView());
		}

		return views.toArray(new View[views.size()]);
	}
	
	/**
	 * If there is not yet a view it return false
	 * @return
	 */
	public boolean hasView()
	{
		return currentView != null;
	}

	private void fireCurrentViewableChanged()
	{
		for (CurrentViewableChangeListener listener : viewableChangeListeners)
			listener.currentViewableChanged(currentView.getCurrentViewable());
	}

	public void addViewableListener(CurrentViewableChangeListener listener)
	{
		viewableChangeListeners.add(listener);
	}

	public void removeViewableListener(CurrentViewableChangeListener listener)
	{
		viewableChangeListeners.remove(listener);
	}

	public void setCurrentView(View currentView)
	{
		this.currentView = currentView;

		fireCurrentViewChanged();
	}

	private void fireCurrentViewChanged()
	{
		for (CurrentViewChangeListener listener : viewChangeListeners)
			listener.currentViewChanged(currentView);
		}

	public void addViewListener(CurrentViewChangeListener listener)
	{
		viewChangeListeners.add(listener);
	}

	public void removeViewListener(CurrentViewChangeListener listener)
	{
		viewChangeListeners.remove(listener);
	}

	/**
	 * If the current selected view is a 3D View return it, else create a new
	 * view and return it
	 */
	public View getCurrentView()
	{
		if (currentView != null)
			return currentView;
		else
			return createView();
	}
	
	/**
	 * Give the current viewable of all views
	 * @return
	 */
	public Collection<Viewable> getCurrentViewables()
	{
		View[] views = getAllView();
		HashSet<Viewable> viewables = new HashSet<Viewable>(views.length);
		for(View view : views)
			viewables.add(view.getCurrentViewable());
		
		return viewables;
	}

	/**
	 * Create a view and set the view the current View
	 * @return
	 */
	public View createView()
	{
		try
		{
			View3D topComponent = new View3D(new MyView());
			++counter;
			topComponent.setName("3D View " + counter);
			WindowManager.getDefault().findMode("editor").dockInto(topComponent);
			topComponent.open();

			// Make current
			topComponent.requestActive();
			setCurrentView(currentView);

			return topComponent.getView();
		} catch (Exception ex)
		{
			ErrorManager.getDefault().notify(ex);
			return null;
		}
	}
}
