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
import javax.swing.ImageIcon;
import org.jcae.vtk.CameraManager;
import org.jcae.vtk.View;


public abstract class ChangeCameraOrientation extends ViewAction
{
	private CameraManager.Orientation orientation;

	public ChangeCameraOrientation(String label, String icon, CameraManager.Orientation orientation)
	{			
		label=label+" view";
		putValue(Action.NAME, label);
		putValue(Action.SHORT_DESCRIPTION, label);
		putValue(SMALL_ICON, new ImageIcon(ChangeCameraOrientation.class.getResource(icon)));
		setIcon(new ImageIcon(ChangeCameraOrientation.class.getResource(icon)));
		this.orientation=orientation;
	}
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(View view)
	{
		view.getCameraManager().setCameraOrientation(orientation);			
	}
}