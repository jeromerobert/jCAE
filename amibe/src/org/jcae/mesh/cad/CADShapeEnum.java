/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2006, by EADS CRC
    Copyright (C) 2007, by EADS France

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
	public abstract Class<? extends CADShape> asClass();
	@Override
	public String toString()
	{
		return name;
	}

	public static final CADShapeEnum VERTEX = CADShapeFactory.getFactory().getShapeEnumInstance("vertex");
	public static final CADShapeEnum EDGE = CADShapeFactory.getFactory().getShapeEnumInstance("edge");
	public static final CADShapeEnum WIRE = CADShapeFactory.getFactory().getShapeEnumInstance("wire");
	public static final CADShapeEnum FACE = CADShapeFactory.getFactory().getShapeEnumInstance("face");
	public static final CADShapeEnum SHELL = CADShapeFactory.getFactory().getShapeEnumInstance("shell");
	public static final CADShapeEnum SOLID = CADShapeFactory.getFactory().getShapeEnumInstance("solid");
	public static final CADShapeEnum COMPSOLID = CADShapeFactory.getFactory().getShapeEnumInstance("compsolid");
	public static final CADShapeEnum COMPOUND = CADShapeFactory.getFactory().getShapeEnumInstance("compound");
	
	@Deprecated
	public static Iterator<CADShapeEnum> iterator(CADShapeEnum start, CADShapeEnum end)
	{
		return CADShapeFactory.getFactory().newShapeEnumIterator(start, end);
	}

	public static Iterable<CADShapeEnum> iterable(final CADShapeEnum start, final CADShapeEnum end)
	{
		return new Iterable<CADShapeEnum>() {
			public Iterator<CADShapeEnum> iterator()
			{
				return CADShapeFactory.getFactory().newShapeEnumIterator(start, end);
			}
		};
	}

}
