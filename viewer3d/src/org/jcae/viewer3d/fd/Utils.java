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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author Jerome Robert
 *
 */
public class Utils
{
	static public Collection intArrayToCollection(int[] array)
	{
		Collection toReturn=new ArrayList(array.length);
		for(int i=0; i<array.length; i++)
		{
			toReturn.add(new Integer(array[i]));
		}
		return toReturn;
	}
	
	static public int[] collectionToIntArray(Collection c)
	{
		int[] toReturn = new int[c.size()];
		Iterator it = c.iterator();
		for (int i = 0; i < toReturn.length; i++)
		{
			toReturn[i] = ((Integer) it.next()).intValue();
		}
		return toReturn;
	}

}
