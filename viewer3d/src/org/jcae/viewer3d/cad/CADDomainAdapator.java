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

package org.jcae.viewer3d.cad;

import java.util.Iterator;

/**
 * @author Jerome Robert
 *
 */
public class CADDomainAdapator implements CADDomain
{
	static private class EmptyIterator implements Iterator
	{
		public static EmptyIterator instance;
		/* (non-Javadoc)
		 * @see java.util.Iterator#remove()
		 */
		public void remove()
		{
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException();
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext()
		{
			return false;
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		public Object next()
		{
			return null;
		}		
	}
	
	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.cad.CADDomain#getEdgeIterator()
	 */
	public Iterator<float[]> getEdgeIterator()
	{
		return EmptyIterator.instance;
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.cad.CADDomain#getVertexIterator()
	 */
	public Iterator<float[]> getVertexIterator()
	{
		return EmptyIterator.instance;
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.cad.CADDomain#getFaceIterator()
	 */
	public Iterator<FaceMesh> getFaceIterator()
	{
		return EmptyIterator.instance;
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
}
