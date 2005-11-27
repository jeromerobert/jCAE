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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.amibe.metrics;

import org.apache.log4j.Logger;

/**
 * 3D matrix.
 */
public class Matrix3D extends Matrix
{
	/**
	 * Create a <code>Matrix3D</code> instance and set it to the identity
	 * matrix.
	 */
	public Matrix3D()
	{
		rank = 3;
		data = new double[rank][rank];
		for (int i = 0; i < rank; i++)
			data[i][i] = 1.0;
	}
	
	/**
	 * Create a <code>Matrix3D</code> instance from three column vectors.
	 *
	 * @param e1  first column.
	 * @param e2  second column.
	 * @param e3  third column.
	 */
	public Matrix3D(double [] e1, double [] e2, double [] e3)
	{
		rank = 3;
		data = new double[rank][rank];
		for (int i = 0; i < rank; i++)
		{
			data[i][0] = e1[i];
			data[i][1] = e2[i];
			data[i][2] = e3[i];
		}
	}
	
}
