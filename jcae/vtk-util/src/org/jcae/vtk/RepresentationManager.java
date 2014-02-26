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
 * (C) Copyright 2014, by EADS France
 */

package org.jcae.vtk;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;
import vtk.vtkActor;
import vtk.vtkPanel;
import vtk.vtkProperty;
import vtk.vtkRenderWindow;

/**
 * Manage edges and back face visibility of a set of VTK actors
 * @author Jerome Robert
 */
public class RepresentationManager {
	public interface Provider
	{
		boolean isEdgeVisibility();
		boolean isBackFaceVisibility();
		/** The list of actors to update */
		Iterable<vtkActor> getActors();
		/** The list of windows to update */
		Iterable<vtkRenderWindow> getRenderWindows();
	}

	public abstract static class AbstractProvider implements Provider
	{
		protected Collection<vtkActor> actors = new ArrayList<vtkActor>();
		public Iterable<vtkActor> getActors()
		{
			actors.clear();
			addActors();
			return actors;
		}

		public Iterable<vtkRenderWindow> getRenderWindows()
		{
			return Collections.singleton(getPanel().GetRenderWindow());
		}

		public boolean isBackFaceVisibility() {
			return true;
		}

		abstract protected vtkPanel getPanel();
		abstract protected void addActors();
	}

	public final static String PREF_EDGE_VISIBILITY = "viewer.edges.visible";
	public final static String PREF_BACKFACE_VISIBILITY = "viewer.backface.visible";
	private final static RepresentationManager INSTANCE = new RepresentationManager();
	private final Collection<Provider> actorProviders = new ArrayList<Provider>();
	private final Runnable updater = new Runnable()
	{
		public void run() {
			update();
			for(Provider ap: actorProviders)
				for(vtkRenderWindow rw:ap.getRenderWindows())
					rw.Render();
		}
	};

	private RepresentationManager()
	{
		Preferences pref = NbPreferences.forModule(getClass());
		System.err.println("pref: "+pref.absolutePath());
		pref.addPreferenceChangeListener(new PreferenceChangeListener() {
			public void preferenceChange(PreferenceChangeEvent evt) {
				if(PREF_EDGE_VISIBILITY.equals(evt.getKey()) ||
					PREF_BACKFACE_VISIBILITY.equals(evt.getKey()))
				{
					updateRender();
				}
			}
		});
	}

	public static RepresentationManager getInstance()
	{
		return INSTANCE;
	}

	public void addProvider(Provider actorProvider)
	{
		actorProviders.add(actorProvider);
	}

	public void removeProvider(Provider actorProvider)
	{
		actorProviders.remove(actorProvider);
	}

	public void update()
	{
		Preferences pref = NbPreferences.forModule(getClass());
		boolean edgeVisible = pref.getBoolean(PREF_EDGE_VISIBILITY, false);
		boolean backFaceVisible = pref.getBoolean(PREF_BACKFACE_VISIBILITY, false);
		for(Provider ap: actorProviders)
		{
			for(vtkActor a: ap.getActors())
			{
				if(a == null)
					continue;
				vtkProperty aprop = a.GetProperty();
				if(ap.isEdgeVisibility())
					aprop.SetEdgeVisibility(edgeVisible ? 1 : 0);
				if(ap.isBackFaceVisibility())
				{
					vtkProperty bfp;
					if(backFaceVisible)
					{
						bfp = a.MakeProperty();
						bfp.SetAmbient(1.0);
						bfp.SetAmbientColor(1,1,1);
					}
					else
					{
						bfp = null;
					}
					a.SetBackfaceProperty(bfp);
				}
				aprop.SetLighting(!edgeVisible || backFaceVisible);
			}
		}
	}

	public void updateRender()
	{
		if(EventQueue.isDispatchThread())
			updater.run();
		else
			EventQueue.invokeLater(updater);
	}
}
