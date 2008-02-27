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
 * (C) Copyright 2006, by EADS CRC
 */

package org.jcae.netbeans.viewer3d;

import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;
import org.openide.ErrorManager;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 *
 * @author jerome
 */
public class View3DManager
{
	int counter=0;
	View3D activeView; 	
	private static View3DManager singleton=new View3DManager();
	
	public static View3DManager getDefault()
	{
		return singleton;
	}
	
	public View3DManager()
	{
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
	}
	
	public View3D[] getAllView3D()
	{
		ArrayList<View3D> v3ds=new ArrayList<View3D>();
		Iterator it=WindowManager.getDefault().getModes().iterator();
		while(it.hasNext())
		{		
			Mode mode=(Mode)it.next();
			TopComponent[] t=mode.getTopComponents();		
			
			for(int i=0; i<t.length; i++)
				if(t[i] instanceof View3D)
					v3ds.add((View3D)t[i]);
		}
		
		return v3ds.toArray(new View3D[v3ds.size()]);
	}
	
	/**
	 * If the current selected view is a 3D View return it, else return null
	 */
	public View3D getSelectedView3D()
	{
		return activeView;
	}
	
	/**
	 * If the current selected view is a 3D View return it, else create a new
	 * view and return it
	 */
	public View3D getView3D()
	{
		View3D t=getSelectedView3D();			
		if(t!=null) return t;
		else return createView3D();
	}
	
	public View3D createView3D()
	{
		try
		{
			View3D topComponent = new View3D();
			topComponent.setName("3D View "+counter);			
			WindowManager.getDefault().findMode("editor").dockInto(topComponent);
			topComponent.open();
			topComponent.requestActive();
			return topComponent;
		} catch(Exception ex)
		{
			ErrorManager.getDefault().notify(ex);
			return null;
		}		
	}	
}
