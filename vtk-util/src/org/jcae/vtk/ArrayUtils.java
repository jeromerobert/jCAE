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
package org.jcae.vtk;

import java.util.Collection;

/**
 *
 * @author ibarz
 */
public class ArrayUtils
{
	/**
	 * Make in one tab the collection of tabs
	 * @param indices
	 * @return
	 */
	public static int[] concatInt(Collection<int[]> collection)
	{
		// Search for the size
		int length = 0;
		for(int[] tab : collection)
		{
			length += tab.length;
		}
		
		int[] toReturn = new int[length];
		int offset = 0;
		for(int[] tab : collection)
		{
			System.arraycopy(tab, 0, toReturn, offset, tab.length);
			offset += tab.length;
		}
		
		return toReturn;
	}
	
	public static void setOffset(int[] tab, int indexBegin, int indexEnd, int offset)
	{
		for(int i = indexBegin ; i < indexEnd ; ++i)
			tab[i] += offset;
	}
	
	public static float[] doubleToFloat(double[] array)
	{
		float[] toReturn = new float[array.length];

		for (int i = 0; i < array.length; ++i)
			toReturn[i] = (float) array[i];

		return toReturn;
	}
}
