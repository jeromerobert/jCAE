/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005, by EADS CRC
    Copyright (C) 2009, by EADS France

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

import org.jcae.mesh.cad.CADShape;
import org.jcae.opencascade.jni.BRepTools;
import org.jcae.opencascade.jni.TopoDS_Vertex;
import org.jcae.opencascade.jni.TopoDS_Edge;
import org.jcae.opencascade.jni.TopoDS_Face;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.jcae.opencascade.jni.TopoDS_Solid;
import org.jcae.opencascade.jni.TopAbs_Orientation;
import org.jcae.opencascade.jni.Bnd_Box;
import org.jcae.opencascade.jni.BRepBndLib;

public class OCCShape implements CADShape
{
	protected TopoDS_Shape myShape = null;
	
	protected OCCShape()
	{
	}
	
	protected void setShape(TopoDS_Shape o)
	{
		myShape = o;
	}
	
	protected TopoDS_Shape getShape()
	{
		return myShape;
	}
	
	public double [] boundingBox()
	{
		Bnd_Box box = new Bnd_Box();
		BRepBndLib.add(myShape, box);
		return box.get();
	}
	
	public OCCShape reversed()
	{
		OCCShape s;
		if (myShape instanceof TopoDS_Vertex)
			s = new OCCVertex();
		else if (myShape instanceof TopoDS_Edge)
			s = new OCCEdge();
		else if (myShape instanceof TopoDS_Face)
			s = new OCCFace();
		else if (myShape instanceof TopoDS_Solid)
			s = new OCCSolid();
		else
			s = new OCCShape();
		s.setShape(myShape.reversed());
		return s;
	}
	
	public int orientation()
	{
		return myShape.orientation().swigValue();
	}
	
	public boolean isOrientationForward()
	{
		return myShape.orientation() == TopAbs_Orientation.FORWARD;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (o == null)
			return false;
		if (!(o instanceof OCCShape))
			return false;
		OCCShape that = (OCCShape) o;
		return myShape.equals(that.myShape);
	}
	
	public boolean isSame(Object o)
	{
		OCCShape that = (OCCShape) o;
		return myShape.isSame(that.myShape);
	}
	
	public void writeNative(String filename)
	{
  		BRepTools.write(myShape, filename);
	}
	
	@Override
	public int hashCode()
	{
		return myShape.hashCode();
	}
	
}
