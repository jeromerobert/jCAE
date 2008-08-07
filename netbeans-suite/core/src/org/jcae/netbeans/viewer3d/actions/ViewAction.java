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

import javax.swing.Action;
import org.jcae.netbeans.viewer3d.ViewManager;
import org.jcae.vtk.View;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;

public abstract class ViewAction extends CallableSystemAction
{	
	public void performAction()
	{
		View v = ViewManager.getDefault().getCurrentView();
		if(v!=null)
		{
			actionPerformed(v);
		}
	}

	public String getName()
	{
		return getValue(Action.NAME).toString();
	}

	public HelpCtx getHelpCtx()
	{
		return HelpCtx.DEFAULT_HELP;
	}
	
	
	public abstract void actionPerformed(View view);
}