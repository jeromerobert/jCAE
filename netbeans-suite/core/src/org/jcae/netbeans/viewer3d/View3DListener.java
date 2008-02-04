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

/*
 * View3DListener.java
 *
 * Created on August 3, 2004, 9:28 AM
 */

package org.jcae.netbeans.viewer3d;

import java.awt.PopupMenu;

/**
 *
 * @author  philavong
 */
public interface View3DListener
{
    public void onPickSelection();
    
    public void setPopup(PopupMenu menu);
    
    public PopupMenu getPopup();
    
    public void displayPopup(int x, int y);
}
