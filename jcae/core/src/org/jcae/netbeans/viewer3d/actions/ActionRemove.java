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

package org.jcae.netbeans.viewer3d.actions;

import org.jcae.netbeans.viewer3d.SelectionManager;
import org.jcae.vtk.View;
import org.jcae.vtk.Viewable;
import org.openide.util.HelpCtx;
import org.openide.util.actions.SystemAction;

/**
 * @author Jerome Robert
 *
 */
public class ActionRemove  extends ViewAction
{
	@Override
	public void actionPerformed(View view)
	{
		if(view!=null)
		{
			Viewable viewable = view.getCurrentViewable();
			if(viewable != null)
			{
				view.remove(viewable);
				viewable.delete();
				SelectionManager.getDefault().removeInteractor(viewable);
				SystemAction.get(SelectViewable.class).refresh();
			}
		}	
	}

	/* (non-Javadoc)
	 * @see org.openide.util.actions.SystemAction#getName()
	 */
	@Override
	public String getName()
	{
		return "remove";
	}

	/* (non-Javadoc)
	 * @see org.openide.util.actions.SystemAction#getHelpCtx()
	 */
	@Override
	public HelpCtx getHelpCtx()
	{
		return HelpCtx.DEFAULT_HELP;
	}
	
	@Override
	protected String iconResource()
    {
        return "org/jcae/netbeans/viewer3d/actions/removeViewable.gif";
    }
}
