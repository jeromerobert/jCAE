/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2004,2005
                  Jerome Robert <jeromerobert@users.sourceforge.net>

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
			case TopAbs_ShapeEnum.COMPOUND:
				shape = new OCCCompound();
				break;
			case TopAbs_ShapeEnum.SOLID:
				shape = new OCCSolid();
				break;
			case TopAbs_ShapeEnum.SHELL:
				shape = new OCCShell();
				break;
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
		if (fileName.endsWith(".step") || fileName.endsWith(".stp"))
		{
			logger.debug("Read STEP file: "+fileName);
			STEPControl_Reader aReader = new STEPControl_Reader();
			aReader.readFile(fileName);
			logger.debug("Transfer roots into shape...");
			aReader.nbRootsForTransfer();
			aReader.transferRoots();
			brepShape = aReader.oneShape();
			logger.debug("... done");
		}
		else if (fileName.endsWith(".igs"))
		{
			logger.debug("Read IGES file: "+fileName);
			IGESControl_Reader aReader = new IGESControl_Reader();
			aReader.readFile(fileName);
			logger.debug("Transfer roots into shape...");
			aReader.nbRootsForTransfer();
			aReader.transferRoots();
			brepShape = aReader.oneShape();
			logger.debug("... done");
		}
		else
		{
			logger.debug("Read BREP file: "+fileName);
			brepShape = BRepTools.read(fileName, new BRep_Builder());
			logger.debug("... done");
		}
		return newShape(brepShape);
	}
	
	/**
	 * @param type 'u'=fuse 'n'=common '\\'=cut
	 */
	public CADShape newShape(CADShape o1, CADShape o2, char type)
	{
		CADShape res = null;
		TopoDS_Shape s1 = (TopoDS_Shape) ((OCCShape) o1).getShape();
		TopoDS_Shape s2 = (TopoDS_Shape) ((OCCShape) o2).getShape();
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
			TopoDS_Shape s = op.shape();
			if (s != null)
				res = newShape(s);
		}
		catch (RuntimeException ex)
		{
		}
		return res;
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
