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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

package org.jcae.mesh.algos;

import org.jcae.mesh.sd.MeshOfCAD;
import org.jcae.mesh.MeshConstraint;
import org.apache.log4j.Logger;

/**
 * @author Marie-Helene Garat
 */

public class BasicMesh2
{
	private static Logger logger=Logger.getLogger(BasicMesh2.class);

	public 	BasicMesh2()
	{
	}
	
	public void compute(MeshOfCAD mesh, MeshConstraint constraint)
	{		
		try
		{
			/* Edge tessellation (pour algo 1D)*/
			RefineEdge algo1d = new RefineEdge();
			algo1d.compute(mesh, constraint);
			
			/* face tesselation (pour algo 2D)*/
			RefineFace2 algo2d = new RefineFace2();
			algo2d.compute(mesh, constraint);

			double len = constraint.getValue();
			new MiscAlgos(mesh).runTargetSizeConvergence(len/Math.sqrt(2), len*Math.sqrt(2));
		} catch (Exception e)
		{
			logger.fatal(e.toString());
			e.printStackTrace();
		}		
	}
}
