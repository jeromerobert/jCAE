/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2004 Jerome Robert <jeromerobert@users.sourceforge.net>

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
import org.apache.log4j.Logger;

public class OCCShapeBuilder extends CADShapeBuilder
{
	private static Logger logger=Logger.getLogger(OCCShapeBuilder.class);
	
	public OCCShapeBuilder ()
	{
	}
	
	public CADShapeBuilder newInstance()
	{
		return new OCCShapeBuilder();
	}
	
	public CADShape newShape(Object o)
	{
		TopoDS_Shape ts = (TopoDS_Shape) o;
		OCCShape shape;
		switch (ts.shapeType())
		{
			case TopAbs_ShapeEnum.FACE:
				shape = new OCCFace();
				break;
			case TopAbs_ShapeEnum.WIRE:
				shape = new OCCWire();
				break;
			case TopAbs_ShapeEnum.EDGE:
				shape = new OCCEdge();
				break;
			case TopAbs_ShapeEnum.VERTEX:
				shape = new OCCVertex();
				break;
			default:
				shape = new OCCShape();
				break;
		}
		shape.setShape(o);
		return (CADShape) shape;
	}
	
	public CADShape newShape (String fileName)
	{
		TopoDS_Shape brepShape;
		if (fileName.endsWith(".step"))
		{
			STEPControl_Reader aReader = new STEPControl_Reader();
			aReader.readFile(fileName);
			aReader.nbRootsForTransfer();
			aReader.transferRoots();
			brepShape = aReader.oneShape();
		}
		else if (fileName.endsWith(".igs"))
		{
			IGESControl_Reader aReader = new IGESControl_Reader();
			aReader.readFile(fileName);
			aReader.nbRootsForTransfer();
			aReader.transferRoots();
			brepShape = aReader.oneShape();
		}
		else
			brepShape = BRepTools.read(fileName, new BRep_Builder());
		return newShape(brepShape);
	}
	
	public CADExplorer newExplorer()
	{
		return (CADExplorer) new OCCExplorer();
	}
	
	public CADWireExplorer newWireExplorer()
	{
		return (CADWireExplorer) new OCCWireExplorer();
	}
	
	public CADGeomCurve2D newCurve2D(Object oe, Object of)
	{
		CADEdge E = (CADEdge) oe;
		CADFace F = (CADFace) of;
		CADGeomCurve2D curve = null;
		try
		{
			curve = (CADGeomCurve2D) new OCCGeomCurve2D(E, F);
		}
		catch (RuntimeException ex)
		{
		}
		return curve;
	}
	
	public CADGeomCurve3D newCurve3D(Object o)
	{
		CADEdge E = (CADEdge) o;
		CADGeomCurve3D curve = null;
		try
		{
			curve = (CADGeomCurve3D) new OCCGeomCurve3D(E);
		}
		catch (RuntimeException ex)
		{
		}
		return curve;
	}
	
}
