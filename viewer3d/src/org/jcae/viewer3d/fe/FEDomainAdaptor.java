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

/**
 * A consistant FEDomain that contain no elements.
 * This class aims to be used as helper to build real FEDomain.
 * @author Jerome Robert
 * @todo implements all methods
 */
public class FEDomainAdaptor implements FEDomain
{
	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfNodes()
	 */
	public int getNumberOfNodes()
	{
		return 0;
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfVertices()
	 */
	public int getNumberOfVertices()
	{
		return 0;
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.fe.FEDomain#Vertices()
	 */
	public int[] getVertices()
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
	public int[] getBeam3()
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
	 * @see jcae.viewer3d.fe.FEDomain#getTria3()
	 */
	public int[] getTria3()
	{
		return new int[0];
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
	 * @see jcae.viewer3d.fe.FEDomain#getNumberOfQuad4()
	 */
	public int getNumberOfQuad4()
	{
		return 0;
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
	 * @see jcae.viewer3d.fe.FEDomain#getQuad8()
	 */
	public int[] getQuad8()
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
	 * @see jcae.viewer3d.fe.FEDomain#getTetra4()
	 */
	public int[] getTetra4()
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
	 * @see jcae.viewer3d.fe.FEDomain#getHexa8()
	 */
	public int[] getHexa8()
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

	public int[] getBeam2()
	{
		return new int[0];
	}

	public int[] getQuad4()
	{
		return new int[0];
	}
}
