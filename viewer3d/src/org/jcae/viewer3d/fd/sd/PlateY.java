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
 * @author  robert
 */
public class PlateY extends Plate
{
	
	/** Creates a new instance of PlateY */
	public PlateY()
	{
	}

	@Override
	public float[] getCoordinates(float[][] grid)
	{
		float[] coords=new float[12];

		coords[0]=grid[0][min1];
		coords[1]=grid[1][position];
		coords[2]=grid[2][min2];

		coords[3]=grid[0][min1];
		coords[4]=grid[1][position];
		coords[5]=grid[2][max2];

		coords[6]=grid[0][max1];
		coords[7]=grid[1][position];
		coords[8]=grid[2][max2];

		coords[9]=grid[0][max1];
		coords[10]=grid[1][position];
		coords[11]=grid[2][min2];
		return coords;
	}
	
	@Override
	protected String getStringPosition()
	{
		return "Y";
	}
	
	@Override
	public float[] getCoordinates(float[][] grid, int i, int j)
	{
		float[] result=new float[3];
		result[0]=grid[0][i];
		result[1]=grid[1][position];
		result[2]=grid[2][j];
		return result;
	}

	@Override
	public int[] getXYZGridIndices(int i, int j) {
		return new int[] {i, position, j};
	}	

}
