/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2003 Jerome Robert <jeromerobert@users.sourceforge.net>

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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.algos;

import org.jcae.opencascade.jni.*;
import org.jcae.mesh.sd.*;
import org.jcae.mesh.util.*;
import java.util.Iterator;
import java.util.Set;
import org.apache.log4j.*;
import java.util.ArrayList;
import org.jcae.mesh.*;
import org.apache.log4j.Logger;

//provisoire (pour test)
import java.io.*;
import org.jcae.mesh.drivers.UNVWriter;

/**
 * This class allows face discretization.
 * The value of discretisation is provided by the constraint hypothesis.
 * \n
 */

public class RefineFace2 extends RefineFace
{
	private static Logger logger=Logger.getLogger(RefineFace2.class);

	public MeshOfCAD compute(MeshOfCAD mesh, MeshConstraint constraint)
	{		
		Iterator itf = mesh.getGeometryIterator();
		int nbfaces = 0;
		
		/* Explore the shape for each face */
		while (itf.hasNext())
		{
			TopoDS_Shape s = (TopoDS_Shape) itf.next();
			if (s.shapeType() == TopAbs_ShapeEnum.FACE)
			{
				MeshOfCAD m = mesh.getMeshFromMapOfSubMesh(s);
				Mesh2D m2d = computeFace(m, constraint);
				if (m2d!=null)
				{
					innerRefine(m2d, constraint.getValue());
					nbfaces++;
					m2d.addMesh(mesh);
					logger.info(" Fin add , face: "+nbfaces);
					m2d=null;
				}
			}
		}
		return mesh;
	}
}

