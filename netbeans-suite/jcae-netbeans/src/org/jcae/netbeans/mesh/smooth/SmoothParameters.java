/*
 * SmoothParameters.java
 *
 * Created on 20 février 2006, 19:06
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jcae.netbeans.mesh.smooth;

/**
 *
 * @author jerome
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
