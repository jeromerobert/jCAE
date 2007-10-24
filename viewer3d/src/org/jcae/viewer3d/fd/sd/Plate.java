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
 * (C) Copyright 2007, by EADS France
 */

package org.jcae.viewer3d.fd.sd;

/**
 *
 * @author Jerome Robert
 */
public abstract class Plate
{	
	public int position,min1,min2,max1,max2;
	public float[] values;

	abstract public float[] getCoordinates(float[][] grid);
	abstract public float[] getCoordinates(float[][] grid, int i, int j);
	abstract public int[] getXYZGridIndices(int i, int j);
	protected abstract String getStringPosition();

	public int numberOfCells()
	{
		return (max1-min1)*(max2-min2);
	}

	int getWidth()
	{
		return max1-min1;
	}
	
	int getHeight()
	{
		return max2-min2;
	}
	
	
	@Override
	public String toString()
	{
		return getStringPosition()+" "+min1+" "+max1+" "+min2+" "+max2+" "+position;
	}
}
