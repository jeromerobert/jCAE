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

import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.Iterator;

/**
 * Typesafe enum of CAD types
 */
public abstract class CADShapeEnum
{
	private final String name;
	private final int ordinal;
	private static int nextOrdinal = 0;
	CADShapeEnum(String name)
	{
		this.name = name;
		this.ordinal = nextOrdinal++;
	}
	public abstract Class asClass();
	public abstract int asType();
	public String toString()
	{
		return name;
	}

	public static final CADShapeEnum VERTEX = new CADShapeEnum("vertex") {
		public Class asClass() { return CADVertex.class; }
		public int asType() { return CADExplorer.VERTEX; }
	};
	public static final CADShapeEnum EDGE = new CADShapeEnum("edge") {
		public Class asClass() { return CADEdge.class; }
		public int asType() { return CADExplorer.EDGE; }
	};
	public static final CADShapeEnum WIRE = new CADShapeEnum("wire") {
		public Class asClass() { return CADWire.class; }
		public int asType() { return CADExplorer.WIRE; }
	};
	public static final CADShapeEnum FACE = new CADShapeEnum("face") {
		public Class asClass() { return CADFace.class; }
		public int asType() { return CADExplorer.FACE; }
	};
	public static final CADShapeEnum SHELL = new CADShapeEnum("shell") {
		public Class asClass() { return CADShell.class; }
		public int asType() { return CADExplorer.SHELL; }
	};
	public static final CADShapeEnum SOLID = new CADShapeEnum("solid") {
		public Class asClass() { return CADSolid.class; }
		public int asType() { return CADExplorer.SOLID; }
	};
	public static final CADShapeEnum COMPSOLID = new CADShapeEnum("compsolid") {
		public Class asClass() { return CADCompSolid.class; }
		public int asType() { return CADExplorer.COMPSOLID; }
	};
	public static final CADShapeEnum COMPOUND = new CADShapeEnum("compound") {
		public Class asClass() { return CADCompound.class; }
		public int asType() { return CADExplorer.COMPOUND; }
	};
	// Note: PRIVATE_VALUES must contain items in the same order as they
	// have been declared!
	private static final CADShapeEnum [] PRIVATE_VALUES = { VERTEX, EDGE, WIRE, FACE, SHELL, SOLID, COMPSOLID, COMPOUND };
	public static final List VALUES = Collections.unmodifiableList(Arrays.asList(PRIVATE_VALUES));

	public static final Iterator iterator(final CADShapeEnum start, final CADShapeEnum end)
	{
		final int iStep;
		if (start.ordinal > end.ordinal)
			iStep = -1;
		else
			iStep = 1;
		return new Iterator()
		{
			CADShapeEnum current = null;
			public boolean hasNext()
			{
				return current != end;
			}
			public Object next()
			{
				if (current == null)
					current = start;
				else
					current = PRIVATE_VALUES[current.ordinal+iStep];
				return current;
			}
			public void remove()
			{
			}
		};
	}

}
