/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005, by EADS CRC
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

package org.jcae.mesh.cad.occ;

import org.jcae.mesh.cad.CADExplorer;
import org.jcae.mesh.cad.CADShape;
import org.jcae.mesh.cad.CADShapeEnum;
import org.jcae.mesh.cad.CADShapeFactory;
import org.jcae.opencascade.jni.TopExp_Explorer;

public class OCCExplorer implements CADExplorer
{
	private final TopExp_Explorer occExp;
	public OCCExplorer ()
	{
		occExp = new TopExp_Explorer();
	}
	
	public void init(CADShape s, CADShapeEnum t)
	{
		OCCShape shape = (OCCShape) s;
		OCCShapeEnum type = (OCCShapeEnum) t;
		occExp.init(shape.getShape(), type.asType());
	}
	
	public boolean more()
	{
		return occExp.more();
	}
	
	public void next()
	{
		occExp.next();
	}
	
	public CADShape current()
	{
		return CADShapeFactory.getFactory().newShape(occExp.current());
	}
}
