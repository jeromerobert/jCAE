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

package org.jcae.netbeans.mesh;

/**
 * @author Jerome Robert
 */
public class SmoothParameters
{
	
	/** Creates a new instance of SmoothParameters */
	public SmoothParameters()
	{
	}

	/**
	 * Holds value of property elementSize.
	 */
	private double elementSize=-1;

	/**
	 * Getter for property elementSize.
	 * @return Value of property elementSize.
	 */
	public double getElementSize()
	{
		return this.elementSize;
	}

	/**
	 * Setter for property elementSize.
	 * @param elementSize New value of property elementSize.
	 */
	public void setElementSize(double elementSize)
	{
		this.elementSize = elementSize;
	}

	/**
	 * Holds value of property iterationNumber.
	 */
	private int iterationNumber=10;

	/**
	 * Getter for property iterationNumber.
	 * @return Value of property iterationNumber.
	 */
	public int getIterationNumber()
	{
		return this.iterationNumber;
	}

	/**
	 * Setter for property iterationNumber.
	 * @param iterationNumber New value of property iterationNumber.
	 */
	public void setIterationNumber(int iterationNumber)
	{
		this.iterationNumber = iterationNumber;
	}
}
