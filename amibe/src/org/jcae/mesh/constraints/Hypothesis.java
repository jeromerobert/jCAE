/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

   (C) Copyright 2006, by EADS CRC

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


package org.jcae.mesh.constraints;

import org.apache.log4j.Logger;

public class Hypothesis
{
	private static Logger logger = Logger.getLogger(Hypothesis.class);
	protected String elementType = null;
	protected double lengthMin = -1.0, lengthMax = -1.0, deflection = -1.0;
	protected boolean lengthBool = false, numberBool = false;
	protected int numberMin = -1, numberMax = -1;
	private boolean locked = false;

	public void setElement(String e)
	{
		checkLock();
		logger.debug("("+Integer.toHexString(this.hashCode())+") Setting element type to "+e);
		elementType = e;
	}

	public void setLength(double l)
	{
		setLength(l, false);
	}

	public void setLength(double l, boolean b)
	{
		checkLock();
		logger.debug("("+Integer.toHexString(this.hashCode())+") Setting length to "+l+"; strong constraint: "+b);
		lengthMin = l;
		lengthMax = l;
		lengthBool = b;
	}

	public void setLength(double l1, double l2)
	{
		setLength(l1, l2,false);
	}

	public void setLength(double l1, double l2, boolean b)
	{
		checkLock();
		logger.debug("("+Integer.toHexString(this.hashCode())+") Setting length; min="+l1+" max="+l2+"; strong constraint: "+b);
		lengthMin = l1;
		lengthMax = l2;
		lengthBool = b;
	}

	public void setDeflection(double d)
	{
		checkLock();
		logger.debug("("+Integer.toHexString(this.hashCode())+") Setting deflection to "+d);
		deflection = d;
	}

	public void setNumber(int n)
	{
		setNumber(n, false);
	}

	public void setNumber(int n, boolean b)
	{
		checkLock();
		logger.debug("("+Integer.toHexString(this.hashCode())+") Setting number of discretized points to "+n+"; strong constraint: "+b);
		numberMin = n;
		numberMax = n;
		numberBool = b;
	}

	public void setNumber(int n1, int n2)
	{
		setNumber(n1, n2, false);
	}

	public void setNumber(int n1, int n2, boolean b)
	{
		checkLock();
		logger.debug("("+Integer.toHexString(this.hashCode())+") Setting number of discretized points; min="+n1+" max="+n2+"; strong constraint: "+b);
		numberMin = n1;
		numberMax = n2;
		numberBool = b;
	}

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
		String ret = "elementType: "+elementType+"\n";
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
