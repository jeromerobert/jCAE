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

package org.jcae.netbeans.viewer3d.actions;

import java.awt.event.ActionEvent;
import org.jcae.netbeans.viewer3d.CurrentViewableChangeListener;
import org.jcae.netbeans.viewer3d.ViewManager;
import org.jcae.vtk.View;
import org.jcae.vtk.Viewable;
import org.jcae.vtk.ViewableCAD;
import org.openide.util.actions.BooleanStateAction;
import org.openide.util.actions.SystemAction;

/**
 * This class is an interface of all type of shape selection of CAO but also manages
 * the boolean state of all buttons with the method updateButtons.
 * If you add a different type of shape dot not remember to add it to this method.
 * @author Julian Ibarz
 */
public abstract class CAOShapeSelection extends BooleanStateAction implements CurrentViewableChangeListener
{	
	public CAOShapeSelection()
	{
		ViewManager.getDefault().addViewableListener(this);
		setEnabled(false);
		setBooleanState(false);
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0)
	{
		View v = ViewManager.getDefault().getCurrentView();
		if(v!=null)
		{
			ViewableCAD interactor = (ViewableCAD)v.getCurrentViewable();
			if(interactor == null)
				throw new RuntimeException("A InteractorCAO is expected");
			actionPerformed(interactor);
			
			updateButtons(interactor);
		}
	}

	public void currentViewableChanged(Viewable interactor)
	{	
		if(interactor == null || interactor.getClass() != ViewableCAD.class)
		{
			setEnabled(false);
		}
		else
		{
			setEnabled(true);
			
			updateButton(((ViewableCAD)interactor));
		}
	}
	
	protected abstract void updateButton(ViewableCAD viewer);

	/**
	 * When the selection was maded update all the buttons selection to be to the correct value
	 * TODO : should not be here (maybe use ButtonGroup to direct manage this ?)
	 */
	protected void updateButtons(ViewableCAD viewer)
	{	
		SystemAction.get(CAOVertexSelection.class).updateButton(viewer);
		SystemAction.get(CAOEdgeSelection.class).updateButton(viewer);
		SystemAction.get(CAOFaceSelection.class).updateButton(viewer);
	}
	
	protected boolean asynchronous() {
        // performAction() should run in event thread for actions that need a rendering of canva
        return false;
    }
		
	public abstract void actionPerformed(ViewableCAD interactor);
}