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

import org.jcae.mesh.cad.CADVertex;
import org.jcae.mesh.cad.CADFace;
import org.jcae.opencascade.jni.TopoDS_Vertex;
import org.jcae.opencascade.jni.TopoDS_Face;
import org.jcae.opencascade.jni.BRep_Tool;

public class OCCVertex extends OCCShape implements CADVertex
{
	@Override
	public final TopoDS_Vertex getShape()
	{
		return (TopoDS_Vertex) myShape;
	}

	public double [] parameters(CADFace o)
	{
		OCCFace face = (OCCFace) o;
		TopoDS_Face F = face.getShape();
		TopoDS_Vertex V = getShape();
		return BRep_Tool.parameters(V, F);
	}
	
	public double [] pnt()
	{
		return BRep_Tool.pnt(getShape());
	}
	
}
