/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005, by EADS CRC
 
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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh;

public class MeshConstraint extends MeshHypothesis
{
	private double value;
	
	/** Constructor with an Id number and a constraint value.
	 * @param id : a long value, Id
	 * @param value : a double value, the constraint value
	 */
	public MeshConstraint(long id, double value)
	{
		super("toto", id,MeshHypothesis.CONSTRAINT);
		this.value = value;
	}
	
	/**
	 * Constructor with an Id, a constraint type and its value.
	 * @param id : a long value, the Id
	 * @param type : an integer value, the constraint type
	 * @param value : a double value, the constraint value
	 */
	public MeshConstraint(long id, int type,double value)
	{
		super("toto", id, type);
		this.value = value;
	}
	
	/**
	 * Get the constraint value.
	 * @return double : the value
	 */
	public double getValue()
	{
		return value;
	}
}
