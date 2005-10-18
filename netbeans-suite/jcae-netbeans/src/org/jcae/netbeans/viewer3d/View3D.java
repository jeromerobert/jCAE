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
 * (C) Copyright 2004, by EADS CRC
 */

package org.jcae.netbeans.viewer3d;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;
import org.jcae.viewer3d.View;
import org.openide.ErrorManager;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * @author Jerome Robert
 *
 */
public class View3D extends TopComponent
{
	static
	{
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
	}
	
	public static View3D[] getAllView3D()
	{
		ArrayList v3ds=new ArrayList();
		Iterator it=WindowManager.getDefault().getModes().iterator();
		while(it.hasNext())
		{		
			Mode mode=(Mode)it.next();
			TopComponent[] t=mode.getTopComponents();		
			
			for(int i=0; i<t.length; i++)
				if(t[i] instanceof View3D) v3ds.add(t[i]);
		}
		
		return (View3D[])v3ds.toArray(new View3D[0]);
	}
	
	/**
	 * If the current selected view is a 3D View return it, else return null
	 */
	public static View3D getSelectedView3D()
	{
		return activeView;
	}
	
	/**
	 * If the current selected view is a 3D View return it, else create a new
	 * view and return it
	 */
	public static View3D getView3D()
	{
		View3D t=getSelectedView3D();			
		if(t!=null) return t;
		else return createView3D();
	}
	
	public static View3D createView3D()
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
	
	private static int counter=0;
	protected View canvas;
	private boolean activated;	
	static private View3D activeView; 
	public View3D()
	{
		counter++;		
		this.canvas=new View();
		setLayout(new BorderLayout());
		add(canvas, BorderLayout.CENTER);
		canvas.setOriginAxisVisible(true);		
	}
	
	public int getPersistenceType()
	{
		return TopComponent.PERSISTENCE_NEVER;
	}
    
	public void setVisible(boolean b)
    {
        if(b)
        {
        	if(canvas.getParent()!=this)
        		this.add(canvas, BorderLayout.CENTER);
        }
        else
        {
        	if(canvas.getParent()==this)
        		this.remove(canvas);
        }
        super.setVisible(b);
    }
	
	public View getView()
	{
		return canvas;
	}
        
	/**
	 *
	 */
	protected void componentClosed()
	{
		canvas.setVisible(false);
		if(activeView==this)
			activeView=null;
	    super.componentClosed();
	}
	
	/**
	 *
	 */
	public String toString()
	{
	    return this.getName();
	}
	
	/* (non-Javadoc)
	 * @see org.openide.windows.TopComponent#componentActivated()
	 */
	protected void componentActivated()
	{	
		activated=true;
		activeView=this;
	}
	
	/* (non-Javadoc)
	 * @see org.openide.windows.TopComponent#componentDeactivated()
	 */
	protected void componentDeactivated()
	{	
		activated=false;
	}	
}
