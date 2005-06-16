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
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.media.j3d.PointAttributes;
import org.jcae.viewer3d.Domain;

/**
 * @author Jerome Robert
 *
 */
public class DefaultFDProvider implements FDProvider
{

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fd.FDProvider#getXGridCount()
	 */
	public int getXGridCount()
	{
		return 100;
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fd.FDProvider#getXGrid(int)
	 */
	public double getXGrid(int index)
	{
		return index*10;
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fd.FDProvider#getYGridCount()
	 */
	public int getYGridCount()
	{
		return 100;
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fd.FDProvider#getYGrid(int)
	 */
	public double getYGrid(int index)
	{
		return index*10;
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fd.FDProvider#getZGridCount()
	 */
	public int getZGridCount()
	{
		return 100;
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fd.FDProvider#getZGrid(int)
	 */
	public double getZGrid(int index)
	{
		return index*10;
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.DomainProvider#getDomainIDs()
	 */
	public int[] getDomainIDs()
	{
		return new int[]{0, 1};
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.DomainProvider#getDomain(int)
	 */
	public Domain getDomain(int id)
	{
		switch(id)
		{
			case 0:
				return new FDDomainAdapter()
				{
					public int getNumberOfXPlate()
					{
						return 1;
					}
					
					/* (non-Javadoc)
					 * @see org.jcae.viewer3d.fd.FDDomainAdapter#getXPlateIterator()
					 */
					public Iterator getXPlateIterator()
					{
						ArrayList a=new ArrayList();
						a.add(new int[]{5,2,3,8,7});
						return a.iterator();
					}
				};				
			case 1:
				return new FDDomainAdapter()
				{
					/* (non-Javadoc)
					 * @see org.jcae.viewer3d.fd.FDDomainAdapter#getNumberOfYWire()
					 */
					public int getNumberOfYWire()
					{
						return 1;
					}
					
					/* (non-Javadoc)
					 * @see org.jcae.viewer3d.fd.FDDomainAdapter#getXPlateIterator()
					 */
					public Iterator getYWireIterator()
					{
						ArrayList a=new ArrayList();
						a.add(new int[]{1,1,1,9});
						return a.iterator();
					}
					
					/* (non-Javadoc)
					 * @see org.jcae.viewer3d.fd.FDDomainAdapter#getMarksTypes()
					 */
					public Object[] getMarksTypes()
					{
						return new Object[]{"prout", new PointAttributes()};
					}
					
					/* (non-Javadoc)
					 * @see org.jcae.viewer3d.fd.FDDomainAdapter#getMarks(java.lang.Object)
					 */
					public float[] getMarks(Object type)
					{
						if(type instanceof String)						
							return new float[]{0,0,0};
						else
							return new float[]{45,28,28};
					}
				};
			default:
				throw new NoSuchElementException();
		}
	}
}
