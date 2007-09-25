/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

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


package org.jcae.mesh.cad;

import java.util.Iterator;

/**
 * Typesafe enum of CAD types
 */
public abstract class CADShapeEnum
{
	private final String name;
	protected final int ordinal;
	private static int nextOrdinal = 0;
	protected CADShapeEnum(String name)
	{
		this.name = name;
		this.ordinal = nextOrdinal++;
	}
	public abstract Class asClass();
	@Override
	public String toString()
	{
		return name;
	}

	public static final CADShapeEnum VERTEX = CADShapeFactory.factory.getShapeEnumInstance("vertex");
	public static final CADShapeEnum EDGE = CADShapeFactory.factory.getShapeEnumInstance("edge");
	public static final CADShapeEnum WIRE = CADShapeFactory.factory.getShapeEnumInstance("wire");
	public static final CADShapeEnum FACE = CADShapeFactory.factory.getShapeEnumInstance("face");
	public static final CADShapeEnum SHELL = CADShapeFactory.factory.getShapeEnumInstance("shell");
	public static final CADShapeEnum SOLID = CADShapeFactory.factory.getShapeEnumInstance("solid");
	public static final CADShapeEnum COMPSOLID = CADShapeFactory.factory.getShapeEnumInstance("compsolid");
	public static final CADShapeEnum COMPOUND = CADShapeFactory.factory.getShapeEnumInstance("compound");
	public static Iterator<CADShapeEnum> iterator(CADShapeEnum start, CADShapeEnum end)
	{
		return CADShapeFactory.factory.newShapeEnumIterator(start, end);
	}

}
