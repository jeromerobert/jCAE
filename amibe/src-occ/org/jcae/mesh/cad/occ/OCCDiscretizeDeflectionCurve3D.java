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

import org.jcae.opencascade.jni.Adaptor3d_Curve;
import org.jcae.opencascade.jni.GCPnts_QuasiUniformDeflection;
import org.apache.log4j.Logger;
import java.util.ArrayList;

public class OCCDiscretizeDeflectionCurve3D extends OCCDiscretizeCurve3D
{
	private static Logger logger=Logger.getLogger(OCCDiscretizeDeflectionCurve3D.class);
	
	public void initialize(Adaptor3d_Curve myCurve, double len, double defl, double start, double end)
	{
		logger.debug("Initialize curve: "+len+" "+defl+" "+start+" "+end);
		curve = myCurve;
		GCPnts_QuasiUniformDeflection discret = new GCPnts_QuasiUniformDeflection();
		discret.initialize(myCurve, defl*len, start, end);
		nr = discret.nbPoints();
		logger.debug("  deflection only: "+nr+" points");
		a = new double[nr];
		for (int i = 0; i < nr; i++)
			a[i] = discret.parameter(i+1);
		
		//  Now make sure that edge length is no more than len.
		double [] oldXYZ = curve.value(a[0]);
		double [] newXYZ;
		double dist;
		int ns = 1;
		while (ns < nr)
		{
			newXYZ = curve.value(a[ns]);
			dist = Math.sqrt(
			  (oldXYZ[0] - newXYZ[0]) * (oldXYZ[0] - newXYZ[0]) +
			  (oldXYZ[1] - newXYZ[1]) * (oldXYZ[1] - newXYZ[1]) +
			  (oldXYZ[2] - newXYZ[2]) * (oldXYZ[2] - newXYZ[2]));
			if (dist > len)
			{
				OCCDiscretizeCurve3D refine = new OCCDiscretizeCurve3D();
				refine.initialize(myCurve, len, a[ns-1], a[ns]);
				if (refine.nr > 2)
				{
					double [] newA = new double[nr+refine.nr-2];
					if (ns > 1)
						System.arraycopy(a, 0, newA, 0, ns-1);
					System.arraycopy(refine.a, 0, newA, ns-1, refine.nr);
					if (ns < nr - 1)
						System.arraycopy(a, ns+1, newA, ns-1+refine.nr, nr - ns - 1);
					a = newA;
					nr += refine.nr - 2;
					ns += refine.nr - 2;
				}
			}
			ns ++;
		}
	}
	
}
