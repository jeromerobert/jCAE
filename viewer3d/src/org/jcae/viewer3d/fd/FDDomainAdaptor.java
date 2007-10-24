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

package org.jcae.viewer3d.fd;

import java.awt.Color;
import java.util.Iterator;

/**
 * An empty FDDomain that help creating FDDomains
 * @author Jerome Robert
 *
 */
public class FDDomainAdaptor implements FDDomain
{
	static private class EmtpyIterator implements Iterator<int[]>
	{
		private final static EmtpyIterator instance=new EmtpyIterator();
		public void remove()
		{
		}

		public boolean hasNext()
		{
			return false;
		}

		public int[] next()
		{
			throw new UnsupportedOperationException();
		}	
		
		static public Iterator<int[]> getDefault()
		{
			return instance;
		}
	}
	
	
	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fd.FDDomain#getNumberOfXPlate()
	 */
	public int getNumberOfXPlate()
	{
		return 0; 
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fd.FDDomain#getNumberOfYPlate()
	 */
	public int getNumberOfYPlate()
	{
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fd.FDDomain#getNumberOfZPlate()
	 */
	public int getNumberOfZPlate()
	{
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fd.FDDomain#getNumberOfXWire()
	 */
	public int getNumberOfXWire()
	{
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fd.FDDomain#getNumberOfYWire()
	 */
	public int getNumberOfYWire()
	{
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fd.FDDomain#getNumberOfZWire()
	 */
	public int getNumberOfZWire()
	{
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fd.FDDomain#getNumberOfSolid()
	 */
	public int getNumberOfSolid()
	{
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fd.FDDomain#getXPlateIterator()
	 */
	public Iterator<int[]> getXPlateIterator()
	{
		return EmtpyIterator.getDefault();
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fd.FDDomain#getYPlateIterator()
	 */
	public Iterator<int[]> getYPlateIterator()
	{
		return EmtpyIterator.getDefault();
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fd.FDDomain#getZPlateIterator()
	 */
	public Iterator<int[]> getZPlateIterator()
	{
		return EmtpyIterator.getDefault();
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fd.FDDomain#getXWireIterator()
	 */
	public Iterator<int[]> getXWireIterator()
	{
		return EmtpyIterator.getDefault();
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fd.FDDomain#getYWireIterator()
	 */
	public Iterator<int[]> getYWireIterator()
	{
		return EmtpyIterator.getDefault();
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fd.FDDomain#getZWireIterator()
	 */
	public Iterator<int[]> getZWireIterator()
	{
		return EmtpyIterator.getDefault();
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fd.FDDomain#getSolidIterator()
	 */
	public Iterator<int[]> getSolidIterator()
	{
		return EmtpyIterator.getDefault();
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.ColoredDomain#getColor()
	 */
	public Color getColor()
	{
		return Color.YELLOW;
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.MarkDomain#getMarksTypes()
	 */
	public Object[] getMarksTypes()
	{
		return new Object[0];
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.MarkDomain#getMarks(java.lang.Object)
	 */
	public float[] getMarks(Object type)
	{
		return new float[0];
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fd.FDDomain#getNumberOfSlot(byte)
	 */
	public int getNumberOfSlot(byte orientation)
	{
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fd.FDDomain#getSlotIterator()
	 */
	public Iterator<?> getSlotIterator(byte type)
	{
		return EmtpyIterator.getDefault();
	}
}
