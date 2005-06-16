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
 * (C) Copyright 2005, by EADS CRC
 */

package org.jcae.viewer3d;

/**
 * This class describe a Domain which contains 0D elements (vertices).
 * @author Jerome Robert
 */
public interface MarkDomain extends Domain
{
	/**
	 * Return objects describing each type of marks in the domain.
	 * 4 cases are handeled:
	 * <ul>
	 * <li>The object is a PointAttribute. This PointAttribute is used in the
	 * Appearance at the Java3D level</li>
	 * <li>The object is a RenderedImage. Each vertex is displayed as a javax.media.j3d.Raster.</li>
	 * <li>The object is a float. This float is the size of the point (square, circle...) to be rendered.</li>
	 * <li>The object is null. The default Appearance is used.</li>
	 * <li>In all other cases vertices are displayed as a javax.media.j3d.Raster
	 * showing the toString string of the object, with the default font.</li> 
	 * </ul>
	 * @return
	 */
	Object[] getMarksTypes();
	
	/**
	 * Return a {x0, y0, z0, x1, y1, z1...} array for the given mark type.
	 * @param type
	 * @return
	 */
	float[] getMarks(Object type);
}
