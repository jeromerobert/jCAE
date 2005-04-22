/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2005
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

package org.jcae.mesh.amibe.validation;

import org.jcae.mesh.amibe.ds.MFace3D;
import org.jcae.mesh.amibe.ds.MNode3D;
import org.jcae.mesh.amibe.metrics.Metric3D;
import java.util.Iterator;

public class Area extends QualityProcedure
{
	public static double [] v1 = new double[3];
	public static double [] v2 = new double[3];
	
	public Area()
	{
		setType(QualityProcedure.FACE);
	}
	
	public float quality(Object o)
	{
		if (!(o instanceof MFace3D))
			throw new IllegalArgumentException();
		MFace3D f = (MFace3D) o;
		Iterator itn = f.getNodesIterator();
		MNode3D n1 = (MNode3D) itn.next();
		MNode3D n2 = (MNode3D) itn.next();
		MNode3D n3 = (MNode3D) itn.next();
		double [] p1 = n1.getXYZ();
		double [] p2 = n2.getXYZ();
		double [] p3 = n3.getXYZ();
		for (int i = 0; i < 3; i++)
		{
			v1[i] = p2[i] - p1[i];
			v2[i] = p3[i] - p1[i];
		}
		double [] v3 = Metric3D.prodVect3D(v1, v2);
		return (float) (0.5 * Metric3D.norm(v3));
	}
}
