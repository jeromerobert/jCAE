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

import org.jcae.netbeans.viewer3d.actions.SelectViewable;
import org.jcae.vtk.View;

/**
 * * All implemented class are informed when the user change the current view3d
 * @see SelectViewable for an exemple of implementation
 * @author Julian Ibarz
 */
public interface CurrentViewChangeListener
{
	/**
	 * Send to all listener the new current View3D.
	 * @param view
	 */
	public void currentViewChanged(View view);
}
