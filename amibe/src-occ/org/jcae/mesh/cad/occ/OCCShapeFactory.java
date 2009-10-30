/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005, by EADS CRC
    Copyright (C) 2007,2008,2009, by EADS France

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
import org.jcae.opencascade.jni.BRep_Builder;
import org.jcae.opencascade.jni.BRepTools;
import org.jcae.opencascade.jni.IGESControl_Reader;
import org.jcae.opencascade.jni.STEPControl_Reader;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.jcae.opencascade.jni.TopAbs_ShapeEnum;
import org.jcae.opencascade.jni.BRepAlgoAPI_BooleanOperation;
import org.jcae.opencascade.jni.BRepAlgoAPI_Fuse;
import org.jcae.opencascade.jni.BRepAlgoAPI_Common;
import org.jcae.opencascade.jni.BRepAlgoAPI_Cut;
import java.util.Iterator;
import java.util.logging.Logger;

/*
 * Note: this class is used only by reflection, see CADShapeFactory#factory
 * initialization.
 */
public class OCCShapeFactory extends CADShapeFactory
{
	private static final Logger logger=Logger.getLogger(OCCShapeFactory.class.getName());
	
	@Override
	public final CADShape newShape(Object o)
	{
		if (!(o instanceof TopoDS_Shape))
			throw new IllegalArgumentException();
		TopoDS_Shape ts = (TopoDS_Shape) o;
		OCCShape shape;
		switch (ts.shapeType())
		{
			case COMPOUND:
				shape = new OCCCompound();
				break;
			case SOLID:
				shape = new OCCSolid();
				break;
			case SHELL:
				shape = new OCCShell();
				break;
			case FACE:
				shape = new OCCFace();
				break;
			case WIRE:
				shape = new OCCWire();
				break;
			case EDGE:
				shape = new OCCEdge();
				break;
			case VERTEX:
				shape = new OCCVertex();
				break;
			default:
				shape = new OCCShape();
				break;
		}
		shape.setShape(ts);
		return shape;
	}
	
	@Override
	public CADShape newShape (String fileName)
	{
		TopoDS_Shape brepShape;
		if (fileName.endsWith(".step") || fileName.endsWith(".stp"))
		{
			logger.fine("Read STEP file: "+fileName);
			STEPControl_Reader aReader = new STEPControl_Reader();
			aReader.readFile(fileName.getBytes());
			logger.fine("Transfer roots into shape...");
			aReader.nbRootsForTransfer();
			aReader.transferRoots();
			brepShape = aReader.oneShape();
			logger.fine("... done");
		}
		else if (fileName.endsWith(".igs"))
		{
			logger.fine("Read IGES file: "+fileName);
			IGESControl_Reader aReader = new IGESControl_Reader();
			aReader.readFile(fileName.getBytes());
			logger.fine("Transfer roots into shape...");
			aReader.nbRootsForTransfer();
			aReader.transferRoots();
			brepShape = aReader.oneShape();
			logger.fine("... done");
		}
		else
		{
			logger.fine("Read BREP file: "+fileName);
			brepShape = BRepTools.read(fileName, new BRep_Builder());
			logger.fine("... done");
		}
		return newShape(brepShape);
	}
	
	/**
	 * @param type 'u'=fuse 'n'=common '\\'=cut
	 */
	@Override
	public CADShape newShape(CADShape o1, CADShape o2, char type)
	{
		CADShape res = null;
		TopoDS_Shape s1 = ((OCCShape) o1).getShape();
		TopoDS_Shape s2 = ((OCCShape) o2).getShape();
/* With libOccJava
		short t = -1;
		if (type == 'u')
			t = 0;
		else if (type == 'n')
			t = 1;
		else if (type == '\\')
			t = 2;
		BRepAlgoAPI_BooleanOperation op = new BRepAlgoAPI_BooleanOperation(s1, s2, t);
		TopoDS_Shape s = op.shape();
		if (s != null)
			res = newShape(s);
*/
		BRepAlgoAPI_BooleanOperation op = null;
		try
		{
			if (type == 'u')
				op = new BRepAlgoAPI_Fuse(s1, s2);
			else if (type == 'n')
				op = new BRepAlgoAPI_Common(s1, s2);
			else if (type == '\\')
				op = new BRepAlgoAPI_Cut(s1, s2);
			else
				throw new IllegalArgumentException();
			TopoDS_Shape s = op.shape();
			if (s != null)
				res = newShape(s);
		}
		catch (RuntimeException ex)
		{
		}
		return res;
	}
	
	@Override
	public CADExplorer newExplorer()
	{
		return new OCCExplorer();
	}
	
	@Override
	public CADWireExplorer newWireExplorer()
	{
		return new OCCWireExplorer();
	}

	@Override
	public CADIterator newIterator()
	{
		return new OCCIterator();
	}
	
	@Override
	protected Iterator<CADShapeEnum> newShapeEnumIterator(CADShapeEnum start, CADShapeEnum end)
	{
		return OCCShapeEnum.newShapeEnumIterator((OCCShapeEnum) start, (OCCShapeEnum) end);
	}

	@Override
	protected CADShapeEnum getShapeEnumInstance(String name)
	{
		return OCCShapeEnum.getSingleton(name);
	}

	@Override
	public CADGeomCurve2D newCurve2D(CADEdge E, CADFace F)
	{
		CADGeomCurve2D curve = null;
		try
		{
			curve = new OCCGeomCurve2D(E, F);
		}
		catch (RuntimeException ex)
		{
		}
		return curve;
	}
	
	@Override
	public CADGeomCurve3D newCurve3D(CADEdge E)
	{
		CADGeomCurve3D curve = null;
		try
		{
			curve = new OCCGeomCurve3D(E);
		}
		catch (RuntimeException ex)
		{
		}
		return curve;
	}
	
}
