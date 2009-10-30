/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2008, by EADS France

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

package org.jcae.mesh.amibe.projection;

import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Location;

public class TangentPlaneProjection implements LocalSurfaceProjection
{
	final double[] origin = new double[3];
	final double[] normal;

	public TangentPlaneProjection(Vertex o)
	{
		if (!o.isManifold() || !o.isMutable())
		{
			normal = null;
			return;
		}
		double [] avgNormal = new double[3];
		if (!o.discreteAverageNormal(avgNormal))
		{
			normal = null;
			return;
		}
		normal = avgNormal;
		double [] param = o.getUV();
		System.arraycopy(param, 0, origin, 0, 3);
	}

	/**
	 * Project a point on this average plane.
	 *
	 * @param pt   point to project on the approximated surface.
	 * @return <code>true</code> if projection has been performed
	 * successfully, <code>false</code> otherwise.
	 */
	public boolean project(Location pt)
	{
		double [] loc = new double[3];
		double [] param = pt.getUV();
		for (int i = 0; i < 3; i++)
			loc[i] = param[i] - origin[i];

		double dist = loc[0] * normal[0] + loc[1] * normal[1] + loc[2] * normal[2];
		for (int i = 0; i < 3; i++)
			loc[i] -= dist * normal[i];
 
		pt.moveTo(origin[0] + loc[0], origin[1] + loc[1], origin[2] + loc[2]);
		return true;
	}
	
	public boolean canProject()
	{
		return normal != null;
	}

	
}
