/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005, by EADS CRC

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

import org.jcae.mesh.cad.CADWireExplorer;
import org.jcae.mesh.cad.CADEdge;
import org.jcae.mesh.cad.CADFace;
import org.jcae.mesh.cad.CADWire;
import org.jcae.opencascade.jni.BRepTools_WireExplorer;

public class OCCWireExplorer implements CADWireExplorer
{
	private BRepTools_WireExplorer occWExp;
	public OCCWireExplorer ()
	{
		occWExp = new BRepTools_WireExplorer();
	}
	
	public void init(CADWire w, CADFace f)
	{
		OCCWire occWire = (OCCWire) w;
		OCCFace occFace = (OCCFace) f;
		occWExp.init(occWire.asTopoDS_Wire(), occFace.asTopoDS_Face());
	}
	
	public boolean more()
	{
		return occWExp.more();
	}
	
	public void next()
	{
		occWExp.next();
	}
	
	public CADEdge current()
	{
		return (CADEdge) OCCShapeFactory.factory.newShape(occWExp.current());
	}
}
