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

package org.jcae.mesh.util;

/**
 * @author cb
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public abstract class Calculs {

	public static double prodSca(double [] A, double [] B) {
		return ((A[0]*B[0])+(A[1]*B[1])+(A[2]*B[2]));
	}

	public static double norm(double [] A) {
		return Math.sqrt((A[0]*A[0])+(A[1]*A[1])+(A[2]*A[2]));
	}
	
	public static double norm2(double [] A) {
		return (A[0]*A[0])+(A[1]*A[1])+(A[2]*A[2]);
	}
	
	public static double [] prodVect3D(double [] v1, double [] v2)
	{
		double [] ret = new double[3];
		ret[0] = v1[1] * v2[2] - v1[2] * v2[1];
		ret[1] = v1[2] * v2[0] - v1[0] * v2[2];
		ret[2] = v1[0] * v2[1] - v1[1] * v2[0];
		return ret;			
	}
 	
}
