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

package org.jcae.viewer3d.fd;

import org.jcae.viewer3d.DomainProvider;

/**
 * A DomainProvider which provide FDDomain objects
 * @author Jerome Robert
 *
 */
public interface FDProvider extends DomainProvider
{
	/** Returns number of cells along X axis */
	public int getXGridCount();

	/**
	 * Returns the value along X axis at the specified position.
	 * @param index index of value to return. Value must be between 0 and
	 * <code>getXGridCount-1</code>.
	 */
	public double getXGrid(int index);

	/** Returns number of values along Y axis */
	public int getYGridCount();

	/**
	 * Returns the value along Y axis at the specified position.
	 * @param index index of value to return. Value must be between 0 and
	 * <code>getYGridCount-1</code>.
	 */
	public double getYGrid(int index);

	/** Returns number of values along Z axis */
	public int getZGridCount();

	/**
	 * Returns the value along Z axis at the specified position.
	 * @param index index of value to return. Value must be between 0 and
	 * <code>getZGridCount-1</code>.
	 */
	public double getZGrid(int index);
}
