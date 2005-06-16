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

package org.jcae.viewer3d.fd.sd;

public class WireX extends Wire
{
	
	/** Creates a new instance of WireX */
	public WireX()
	{
	}

	public float[] getCoordinates(float[][] grid)
	{
		float[] coords=new float[6];
		coords[0]=grid[0][min];
		coords[1]=grid[1][position1];
		coords[2]=grid[2][position2];
		coords[3]=grid[0][max];
		coords[4]=grid[1][position1];
		coords[5]=grid[2][position2];
		return coords;
	}
	
	public String toString()
	{
		return "Wire X: min="+min+", max="+max+", Y="+position1+", Z="+position2;
	}
	
	public float[] getCoordinates(float[][] grid, int i)
	{
		float[] result=new float[3];
		result[0]=grid[0][i];
		result[1]=grid[1][position1];
		result[2]=grid[2][position2];
		return result;
	}

	public int[] getXYZGridIndices(int i) {
		return new int[] {i, position1, position2};
	}

}
