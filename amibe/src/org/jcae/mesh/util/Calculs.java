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

import org.jcae.mesh.sd.*;

/**
 * @author cb
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public abstract class Calculs {

	public static double prodSca(MeshNode n, MeshNode n1, MeshNode n2) {
		double x, y, z;
		double v1x, v1y, v1z;
		double v2x, v2y, v2z;
		x = n.getX();
		y = n.getY();
		z = n.getZ();
		v1x = n1.getX() - x;
		v1y = n1.getY() - y;
		v1z = n1.getZ() - z;
		v2x = n2.getX() - x;
		v2y = n2.getY() - y;
		v2z = n2.getZ() - z;
		return (v1x * v2x + v1y * v2y + v1z * v2z);
	}
	
	public static double prodSca(double [] A, double [] B) {
		return ((A[0]*B[0])+(A[1]*B[1])+(A[2]*B[2]));
	}

	public static double norm(double [] A) {
		return Math.sqrt((A[0]*A[0])+(A[1]*A[1])+(A[2]*A[2]));
	}
	
	public static double norm2(double [] A) {
		return (A[0]*A[0])+(A[1]*A[1])+(A[2]*A[2]);
	}
	
	public static MeshNode prodVect3D(MeshNode n, MeshNode n1, MeshNode n2){
		MeshNode V = new MeshNode();
		
		double x, y, z;
		double v1x, v1y, v1z;
		double v2x, v2y, v2z;
		x = n.getX();
		y = n.getY();
		z = n.getZ();
		v1x = n1.getX() - x;
		v1y = n1.getY() - y;
		v1z = n1.getZ() - z;
		v2x = n2.getX() - x;
		v2y = n2.getY() - y;
		v2z = n2.getZ() - z;
		V.setX(v1y * v2z - v1z * v2y);
		V.setY(v1z * v2x - v1x * v2z);
		V.setZ(v1x * v2y - v1y * v2x);

		return V;			
	}
	
	public static double [] prodVect3D(double [] v1, double [] v2)
	{
		double [] ret = new double[3];
		ret[0] = v1[1] * v2[2] - v1[2] * v2[1];
		ret[1] = v1[2] * v2[0] - v1[0] * v2[2];
		ret[2] = v1[0] * v2[1] - v1[1] * v2[0];
		return ret;			
	}
	
	public static double norm(MeshNode n, MeshNode n1) {
		double x, y, z;
		double v1x, v1y, v1z;
		x = n.getX();
		y = n.getY();
		z = n.getZ();
		v1x = n1.getX() - x;
		v1y = n1.getY() - y;
		v1z = n1.getZ() - z;
		return Math.sqrt(v1x * v1x + v1y * v1y + v1z * v1z);
	}
		
	public static double norm(MeshNode n) {
		double v1x, v1y, v1z;
		v1x = n.getX();
		v1y = n.getY();
		v1z = n.getZ();
		return Math.sqrt(v1x * v1x + v1y * v1y + v1z * v1z);
	}
		
	//  See Paul W. Kahan's paper to learn why this function is
	//  really accurate.  DO NOT REMOVE PARENS IN CALCULUS!!!
	public static double angleVect(MeshNode n, MeshNode n1, MeshNode n2) {
		double normPn1 = norm(n,n1);
		double normPn2 = norm(n,n2);
		if ((normPn1 == 0.0) || (normPn2 == 0.0))
			return 0.0;
		double normPn3 = norm(n1,n2);
		double mu, alpha;

		if (normPn1 < normPn2)
		{
			double temp = normPn1;
			normPn1 = normPn2;
			normPn2 = temp;
		}
		if (normPn2 < normPn3)
			mu = normPn2 - (normPn1 - normPn3);
		else
			mu = normPn3 - (normPn1 - normPn2);
		double denom = (normPn1+(normPn2+normPn3))*((normPn1-normPn3)+normPn2);
		if (denom == 0.0)
			//  Sign does not matter because triangles are not oriented.
			//  The 0/0 case can only happen when all points are equal,
			//  which is caught at the beginning of this prtocedure.
			return Math.PI / 2.0;
		alpha = ((normPn1-normPn2)+normPn3)*mu / denom;
		if (alpha <= 0.0)
			return 0.0;
		return 2.0 * Math.atan(Math.sqrt(alpha));
 	}
 	
}
