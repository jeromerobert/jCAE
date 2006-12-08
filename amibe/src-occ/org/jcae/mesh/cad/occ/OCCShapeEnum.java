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


package org.jcae.mesh.cad.occ;

import org.jcae.mesh.cad.*;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Typesafe enum of CAD types
 */
public abstract class OCCShapeEnum extends CADShapeEnum
{
	// occExplorerEnum must be identical to TopAbs_ShapeEnum.
	private final int occExplorerEnum;
	OCCShapeEnum(String name, int t)
	{
		super(name);
		occExplorerEnum = t;
	}
	int asType()
	{
		return occExplorerEnum;
	}

	static final OCCShapeEnum VERTEX = new OCCShapeEnum("vertex", 7) {
		public Class asClass() { return CADVertex.class; }
	};
	static final OCCShapeEnum EDGE = new OCCShapeEnum("edge", 6) {
		public Class asClass() { return CADEdge.class; }
	};
	static final OCCShapeEnum WIRE = new OCCShapeEnum("wire", 5) {
		public Class asClass() { return CADWire.class; }
	};
	static final OCCShapeEnum FACE = new OCCShapeEnum("face", 4) {
		public Class asClass() { return CADFace.class; }
	};
	static final OCCShapeEnum SHELL = new OCCShapeEnum("shell", 3) {
		public Class asClass() { return CADShell.class; }
	};
	static final OCCShapeEnum SOLID = new OCCShapeEnum("solid", 2) {
		public Class asClass() { return CADSolid.class; }
	};
	static final OCCShapeEnum COMPSOLID = new OCCShapeEnum("compsolid", 1) {
		public Class asClass() { return CADCompSolid.class; }
	};
	static final OCCShapeEnum COMPOUND = new OCCShapeEnum("compound", 0) {
		public Class asClass() { return CADCompound.class; }
	};
	// Note: VALUES must contain items in the same order as they
	// have been declared in this file!
	private static final OCCShapeEnum [] VALUES = { VERTEX, EDGE, WIRE, FACE, SHELL, SOLID, COMPSOLID, COMPOUND };

	// Export ordinal to newShapeEnumIterator
	int getOrdinal()
	{
		return ordinal;
	}
	static final Iterator newShapeEnumIterator(final OCCShapeEnum start, final OCCShapeEnum end)
	{
		final int iStep;
		if (start.getOrdinal() > end.getOrdinal())
			iStep = -1;
		else
			iStep = 1;
		return new Iterator()
		{
			OCCShapeEnum current = null;
			public boolean hasNext()
			{
				return current != end;
			}
			public Object next()
			{
				if (!hasNext())
					throw new NoSuchElementException();
				if (current == null)
					current = start;
				else
					current = VALUES[current.getOrdinal()+iStep];
				return current;
			}
			public void remove()
			{
			}
		};
	}
	static final OCCShapeEnum getSingleton(String name)
	{
		for (int i = 0; i < VALUES.length; i++)
			if (VALUES[i].toString().equals(name))
				return VALUES[i];
		throw new IllegalArgumentException();
	}

}
