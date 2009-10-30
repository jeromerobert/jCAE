/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

   (C) Copyright 2006, by EADS CRC
   (C) Copyright 2009, by EADS France

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
import org.jcae.opencascade.jni.TopAbs_ShapeEnum;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Typesafe enum of CAD types
 */
public abstract class OCCShapeEnum extends CADShapeEnum
{
	// occExplorerEnum must be identical to TopAbs_ShapeEnum.
	private final TopAbs_ShapeEnum occExplorerEnum;
	OCCShapeEnum(String name, int t)
	{
		super(name);
		occExplorerEnum = TopAbs_ShapeEnum.swigToEnum(t);
	}
	TopAbs_ShapeEnum asType()
	{
		return occExplorerEnum;
	}

	private static final OCCShapeEnum OCC_VERTEX = new OCCShapeEnum("vertex", 7) {
		@Override
		public Class<CADVertex> asClass() { return CADVertex.class; }
	};
	private static final OCCShapeEnum OCC_EDGE = new OCCShapeEnum("edge", 6) {
		@Override
		public Class<CADEdge> asClass() { return CADEdge.class; }
	};
	private static final OCCShapeEnum OCC_WIRE = new OCCShapeEnum("wire", 5) {
		@Override
		public Class<CADWire> asClass() { return CADWire.class; }
	};
	private static final OCCShapeEnum OCC_FACE = new OCCShapeEnum("face", 4) {
		@Override
		public Class<CADFace> asClass() { return CADFace.class; }
	};
	private static final OCCShapeEnum OCC_SHELL = new OCCShapeEnum("shell", 3) {
		@Override
		public Class<CADShell> asClass() { return CADShell.class; }
	};
	private static final OCCShapeEnum OCC_SOLID = new OCCShapeEnum("solid", 2) {
		@Override
		public Class<CADSolid> asClass() { return CADSolid.class; }
	};
	private static final OCCShapeEnum OCC_COMPSOLID = new OCCShapeEnum("compsolid", 1) {
		@Override
		public Class<CADCompSolid> asClass() { return CADCompSolid.class; }
	};
	private static final OCCShapeEnum OCC_COMPOUND = new OCCShapeEnum("compound", 0) {
		@Override
		public Class<CADCompound> asClass() { return CADCompound.class; }
	};
	// Note: VALUES must contain items in the same order as they
	// have been declared in this file!
	static final OCCShapeEnum [] VALUES = {
		OCC_VERTEX,
		OCC_EDGE,
		OCC_WIRE,
		OCC_FACE,
		OCC_SHELL,
		OCC_SOLID,
		OCC_COMPSOLID,
		OCC_COMPOUND };

	// Export ordinal to newShapeEnumIterator
	int getOrdinal()
	{
		return ordinal;
	}
	static Iterator<CADShapeEnum> newShapeEnumIterator(final OCCShapeEnum start, final OCCShapeEnum end)
	{
		final int iStep;
		if (start.getOrdinal() > end.getOrdinal())
			iStep = -1;
		else
			iStep = 1;
		return new Iterator<CADShapeEnum>()
		{
			OCCShapeEnum current = null;
			public boolean hasNext()
			{
				return current != end;
			}
			public CADShapeEnum next()
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
	static OCCShapeEnum getSingleton(String name)
	{
		for (int i = 0; i < VALUES.length; i++)
			if (VALUES[i].toString().equals(name))
				return VALUES[i];
		throw new IllegalArgumentException();
	}

}
