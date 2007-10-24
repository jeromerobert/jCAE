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

package org.jcae.viewer3d.fe;

import java.awt.Color;
import java.util.Iterator;

/**
 * A consistant FEDomain that contain no elements.
 * This class aims to be used as helper to build real FEDomain.
 * @author Jerome Robert
 * @todo implements all methods
 */
public class FEDomainAdaptor implements FEDomain
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
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfNodes()
	 */
	public int getNumberOfNodes()
	{
		return 0;
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNodesIterator()
	 */
	public Iterator getNodesIterator()
	{
		return EmptyIterator.instance;
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfNodeComp()
	 */
	public int getNumberOfNodeComp()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNodesRes()
	 */
	public Iterator getNodesRes()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfVertices()
	 */
	public int getNumberOfVertices()
	{
		return 0;
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getVerticesIterator()
	 */
	public Iterator getVerticesIterator()
	{
		return EmptyIterator.instance;
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfVerticesComp()
	 */
	public int getNumberOfVerticesComp()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfVerticesRes()
	 */
	public Iterator getNumberOfVerticesRes()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfBeam2()
	 */
	public int getNumberOfBeam2()
	{
		return 0;
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getBeam2Iterator()
	 */
	public Iterator getBeam2Iterator()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfBeam2Comp()
	 */
	public int getNumberOfBeam2Comp()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getBeam2Res()
	 */
	public Iterator getBeam2Res()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfBeam3()
	 */
	public int getNumberOfBeam3()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getBeam3Iterator()
	 */
	public Iterator getBeam3Iterator()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfBeam3Comp()
	 */
	public int getNumberOfBeam3Comp()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getBeam3Res()
	 */
	public Iterator getBeam3Res()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfTria3()
	 */
	public int getNumberOfTria3()
	{
		return 0;
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getTria3Iterator()
	 */
	public Iterator<int[]> getTria3Iterator()
	{
		return EmptyIterator.instance;
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfTria3Comp()
	 */
	public int getNumberOfTria3Comp()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getTria3Res()
	 */
	public Iterator getTria3Res()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfTria6()
	 */
	public int getNumberOfTria6()
	{
		return 0;
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getTria6Iterator()
	 */
	public int[] getTria6()
	{
		return new int[0];
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfTria6Comp()
	 */
	public int getNumberOfTria6Comp()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getTria6Res()
	 */
	public Iterator getTria6Res()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfQuad4()
	 */
	public int getNumberOfQuad4()
	{
		return 0;
	}
	

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfQuad4Comp()
	 */
	public int getNumberOfQuad4Comp()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getQuad4Res()
	 */
	public Iterator getQuad4Res()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfQuad8()
	 */
	public int getNumberOfQuad8()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getQuad8Iterator()
	 */
	public Iterator getQuad8Iterator()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfQuad8Comp()
	 */
	public int getNumberOfQuad8Comp()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getQuad8Res()
	 */
	public Iterator getQuad8Res()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfPoly()
	 */
	public int getNumberOfPoly()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getPolyIterator()
	 */
	public Iterator getPolyIterator()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfPolyComp()
	 */
	public int getNumberOfPolyComp()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getPolyRes()
	 */
	public Iterator getPolyRes()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfTetra4()
	 */
	public int getNumberOfTetra4()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getTetra4Iterator()
	 */
	public Iterator getTetra4Iterator()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfTetra4Comp()
	 */
	public int getNumberOfTetra4Comp()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getTetra4Res()
	 */
	public Iterator getTetra4Res()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfTetra10()
	 */
	public int getNumberOfTetra10()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getTetra10Iterator()
	 */
	public Iterator getTetra10Iterator()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfTetra10Comp()
	 */
	public int getNumberOfTetra10Comp()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getTetra10Res()
	 */
	public Iterator getTetra10Res()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfPyra5()
	 */
	public int getNumberOfPyra5()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getPyra5Iterator()
	 */
	public Iterator getPyra5Iterator()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfPyra5Comp()
	 */
	public int getNumberOfPyra5Comp()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getPyra5Res()
	 */
	public Iterator getPyra5Res()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfPyra13()
	 */
	public int getNumberOfPyra13()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getPyra13Iterator()
	 */
	public Iterator getPyra13Iterator()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfPyra13Comp()
	 */
	public int getNumberOfPyra13Comp()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getPyra13Res()
	 */
	public Iterator getPyra13Res()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfHexa8()
	 */
	public int getNumberOfHexa8()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getHexa8Iterator()
	 */
	public Iterator getHexa8Iterator()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfHexa8Comp()
	 */
	public int getNumberOfHexa8Comp()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getHexa8Res()
	 */
	public Iterator getHexa8Res()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfHexa20()
	 */
	public int getNumberOfHexa20()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getHexa20Iterator()
	 */
	public Iterator getHexa20Iterator()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfHexa20Comp()
	 */
	public int getNumberOfHexa20Comp()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getHexa20Res()
	 */
	public Iterator getHexa20Res()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfPenta6()
	 */
	public int getNumberOfPenta6()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getPenta6Iterator()
	 */
	public Iterator getPenta6Iterator()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfPenta6Comp()
	 */
	public int getNumberOfPenta6Comp()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getPenta6Res()
	 */
	public Iterator getPenta6Res()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfPenta15()
	 */
	public int getNumberOfPenta15()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getPenta15Iterator()
	 */
	public Iterator getPenta15Iterator()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfPenta15Comp()
	 */
	public int getNumberOfPenta15Comp()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getPenta15Res()
	 */
	public Iterator getPenta15Res()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.Domain#getColor()
	 */
	public Color getColor()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.MarkDomain#getMarksTypes()
	 */
	public Object[] getMarksTypes()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.MarkDomain#getMarks(java.lang.Object)
	 */
	public float[] getMarks(Object type)
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fe.FEDomain#getID()
	 */
	public int getID()
	{
		return 0;
	}

	public float[] getNodes()
	{
		return new float[0];
	}

	public int[] getBeam2Indices()
	{
		return new int[0];
	}

	public int[] getQuad4()
	{
		return new int[0];
	}
}
