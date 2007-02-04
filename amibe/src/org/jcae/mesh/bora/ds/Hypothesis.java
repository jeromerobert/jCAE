/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

   (C) Copyright 2006, by EADS CRC
   (C) Copyright 2007, by EADS France

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */


package org.jcae.mesh.bora.ds;

import org.apache.log4j.Logger;

public class Hypothesis
{
	private static Logger logger = Logger.getLogger(Hypothesis.class);
	protected String elementType = null;
	protected double lengthMin = -1.0, lengthMax = -1.0, deflection = -1.0;
	protected boolean lengthBool = false, numberBool = false;
	protected int numberMin = -1, numberMax = -1;
	private boolean locked = false;

	// Unique identitier
	private int id = -1;
	private static int nextId = -1;

	public Hypothesis()
	{
		nextId++;
		id = nextId;
	}

	public int getId()
	{
		return id;
	}

	/**
	 * Sets element type.
	 *
	 * @param e  element type
	 */
	public void setElement(String e)
	{
		checkLock();
		logger.debug("("+Integer.toHexString(this.hashCode())+") Setting element type to "+e);
		elementType = e;
	}

	/**
	 * Gets element type.
	 *
	 * @return element type
	 */
	public String getElement()
	{
		return elementType;
	}

	/**
	 * Sets target length.
	 *
	 * @param l  length
	 */
	public void setLength(double l)
	{
		setLength(l, false);
	}

	/**
	 * Sets target length.
	 *
	 * @param l  length
	 * @param b  hard constraint
	 */
	public void setLength(double l, boolean b)
	{
		checkLock();
		logger.debug("("+Integer.toHexString(this.hashCode())+") Setting length to "+l+"; strong constraint: "+b);
		lengthMin = l;
		lengthMax = l;
		lengthBool = b;
	}

	/**
	 * Sets length interval.
	 *
	 * @param l1  length min
	 * @param l2  length max
	 */
	public void setLength(double l1, double l2)
	{
		setLength(l1, l2,false);
	}

	/**
	 * Sets length interval.
	 *
	 * @param l1  length min
	 * @param l2  length max
	 * @param b  hard constraint
	 */
	public void setLength(double l1, double l2, boolean b)
	{
		checkLock();
		logger.debug("("+Integer.toHexString(this.hashCode())+") Setting length; min="+l1+" max="+l2+"; strong constraint: "+b);
		lengthMin = l1;
		lengthMax = l2;
		lengthBool = b;
	}

	/**
	 * Sets deflection.
	 *
	 * @param d  deflection
	 */
	public void setDeflection(double d)
	{
		checkLock();
		logger.debug("("+Integer.toHexString(this.hashCode())+") Setting deflection to "+d);
		deflection = d;
	}

	/**
	 * Sets number of segments.
	 *
	 * @param n  number of segments.
	 */
	public void setNumber(int n)
	{
		setNumber(n, false);
	}

	/**
	 * Sets number of segments.
	 *
	 * @param n  number of segments.
	 * @param b  hard constraint
	 */
	public void setNumber(int n, boolean b)
	{
		checkLock();
		logger.debug("("+Integer.toHexString(this.hashCode())+") Setting number of discretized points to "+n+"; strong constraint: "+b);
		numberMin = n;
		numberMax = n;
		numberBool = b;
	}

	/**
	 * Sets number of segments.
	 *
	 * @param n1  minimal number of segments
	 * @param n2  maximal number of segments
	 */
	public void setNumber(int n1, int n2)
	{
		setNumber(n1, n2, false);
	}

	/**
	 * Sets number of segments.
	 *
	 * @param n1  minimal number of segments
	 * @param n2  maximal number of segments
	 * @param b  hard constraint
	 */
	public void setNumber(int n1, int n2, boolean b)
	{
		checkLock();
		logger.debug("("+Integer.toHexString(this.hashCode())+") Setting number of discretized points; min="+n1+" max="+n2+"; strong constraint: "+b);
		numberMin = n1;
		numberMax = n2;
		numberBool = b;
	}

	/**
	 * Locks the hypothesis so that it cannot be modified.
	 */
	public void lock()
	{
		locked = true;
	}

	private void checkLock()
	{
		if (locked)
			throw new RuntimeException("Cannot modify an Hypothesis after it has been aplied!");
	}

	public String toString()
	{
		String ret = "Hyp. "+id+"\n";
		ret += "elementType: "+elementType+"\n";
		ret += "lengthMin: "+lengthMin+"\n";
		ret += "lengthMax: "+lengthMax+"\n";
		ret += "lengthBool: "+lengthBool+"\n";
		ret += "deflection: "+deflection+"\n";
		ret += "numberMin: "+numberMin+"\n";
		ret += "numberMax: "+numberMax+"\n";
		ret += "numberBool: "+numberBool;
		return ret;
	}

}
