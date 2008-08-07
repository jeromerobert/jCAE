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

package org.jcae.netbeans.viewer3d.actions;

import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jcae.netbeans.viewer3d.ViewCameraList;
import org.jcae.vtk.View;


public class GoToAction extends ViewAction
{
	private static ImageIcon icon = new ImageIcon(GoToAction.class.getResource("im-aim.png"));	
	/**
	 * @param view
	 */
	public GoToAction()
	{			
		putValue(Action.NAME, "Go To");
		putValue(Action.SHORT_DESCRIPTION, "Go To");
		putValue(Action.SMALL_ICON, icon);
		setIcon(icon);
	}
	
	public void actionPerformed(View view)
	{
		new ViewCameraList(view.getCameraManager()).setVisible(true);
	}

	@Override
	protected boolean asynchronous()
	{
		return false;
	}
}