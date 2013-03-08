/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2005,2006 by EADS CRC
    Copyright (C) 2007,2009,2010, by EADS France
 
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

import java.io.Serializable;

/**
 * General 3D matrix.
 */
public class Matrix3D implements Serializable
{
	private static final long serialVersionUID = 4514401276126330902L;
	private final double [] data = new double[9];
	
	/**
	 * Create a <code>Matrix3D</code> instance and set it to the identity
	 * matrix.
	 */
	private Matrix3D()
	{
		data[0] = data[4] = data[8] = 1.0;
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
		for (int i = 0; i < 3; i++)
		{
			data[i]   = e1[i];
			data[i+3] = e2[i];
			data[i+6] = e3[i];
		}
	}
	
	/**
	 * Create a diagonal <code>Matrix3D</code> instance from three values.
	 *
	 * @param d1  first diagonal value.
	 * @param d2  second diagonal value.
	 * @param d3  third diagonal value.
	 */
	public Matrix3D(double d1, double d2, double d3)
	{
		data[0] = d1;
		data[4] = d2;
		data[8] = d3;
	}

	/**
	 * Transpose current matrix.
	 */
	public final void transp()
	{
		swap(0, 1);
		swap(0, 2);
		swap(1, 2);
	}
	
	/**
	 * Create a <code>Matrix3D</code> instance containing the multiplication
	 * of this <code>Matrix3D</code> instance by another one.
	 *
	 * @param A  another Matrix3D
	 * @return a new Matrix3D containing the multiplication this*A
	 */
	public final Matrix3D multR(Matrix3D A)
	{
		Matrix3D ret = new Matrix3D();
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
			{
				ret.data[i+3*j] = 0.0;
				for (int k = 0; k < 3; k++)
					ret.data[i+3*j] += data[i+3*k] * A.data[k+3*j];
			}
		return ret;
	}
	
	/**
	 * Create a <code>Matrix3D</code> instance containing the multiplication
	 * of another <code>Matrix3D</code> instance by this one.
	 *
	 * @param A  another Matrix3D
	 * @return a new Matrix3D containing the multiplication A*this
	 */
	public final Matrix3D multL(Matrix3D A)
	{
		Matrix3D ret = new Matrix3D();
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
			{
				ret.data[i+3*j] = 0.0;
				for (int k = 0; k < 3; k++)
					ret.data[i+3*j] += A.data[i+3*k] * data[k+3*j];
			}
		return ret;
	}
	
	public final void apply(double [] in, double [] out)
	{
		if (3 != in.length)
			throw new IllegalArgumentException(in.length+" is different from 3");
		for (int i = 0; i < 3; i++)
			out[i] = data[i] * in[0] + data[i+3] * in[1] + data[i+6] * in[2];
	}
	
	public final void getValues(double [] temp)
	{
		System.arraycopy(data, 0, temp, 0, 9);
	}

	private void swap(int i, int j)
	{
		double temp = data[i+3*j];
		data[i+3*j] = data[j+3*i];
		data[j+3*i] = temp;
	}
	
	//  Basic 3D linear algebra.
	//  Note: these routines have been moved from Metric3D.
	
	/**
	 * Return the dot product between two 3D vectors.
	 *
	 * @param A first 3D vector
	 * @param B second 3D vector
	 * @return the dot product between the two vectors.
	 */
	public static double prodSca(double [] A, double [] B)
	{
		return ((A[0]*B[0])+(A[1]*B[1])+(A[2]*B[2]));
	}

	public static double prodSca(double [] a, Location b)
	{
		return a[0] * b.getX() + a[1] * b.getY() + a[2] * b.getZ();
	}
	/**
	 * Return the Euclidian norm of a 3D vector.
	 *
	 * @param A 3D vector.
	 * @return the Euclidian norm of A.
	 */
	public static double norm(double [] A)
	{
		return Math.sqrt((A[0]*A[0])+(A[1]*A[1])+(A[2]*A[2]));
	}
	
	/**
	 * Return the outer product of two 3D vectors.
	 *
	 * @param v1 first vector
	 * @param v2 second vector
	 * @param ret allocated array to store the output product.
	 */
	public static void prodVect3D(double [] v1, double [] v2, double [] ret)
	{
		ret[0] = v1[1] * v2[2] - v1[2] * v2[1];
		ret[1] = v1[2] * v2[0] - v1[0] * v2[2];
		ret[2] = v1[0] * v2[1] - v1[1] * v2[0];
	}

	private static void prodVect3D(double [] v1, int offset1, double [] v2, int offset2,
		double [] ret, int offset)
	{
		ret[offset]   = v1[offset1+1] * v2[offset2+2] - v1[offset1+2] * v2[offset2+1];
		ret[offset+1] = v1[offset1+2] * v2[offset2]   - v1[offset1]   * v2[offset2+2];
		ret[offset+2] = v1[offset1]   * v2[offset2+1] - v1[offset1+1] * v2[offset2];
	}

	public static double computeNormal3D(Location p0, Location p1, Location p2, double [] tempD1, double [] tempD2, double [] ret)
	{
		p1.sub(p0, tempD1);
		p2.sub(p0, tempD2);
		prodVect3D(tempD1, tempD2, ret);
		double norm = norm(ret);
		if (norm*norm > 1.e-12 * (
				tempD1[0]*tempD1[0] + tempD1[1]*tempD1[1] + tempD1[2]*tempD1[2] +
				tempD2[0]*tempD2[0] + tempD2[1]*tempD2[1] + tempD2[2]*tempD2[2]
				))
		{
			ret[0] /= norm;
			ret[1] /= norm;
			ret[2] /= norm;
		}
		else
		{
			ret[0] = ret[1] = ret[2] = 0.0;
			norm = 0.0;
		}
		return 0.5 * norm;
	}

	public static double computeNormal3DT(Location p0, Location p1, Location p2, double [] tempD1, double [] tempD2, double [] ret)
	{
		p1.sub(p0, tempD1);
		p2.sub(p0, ret);
		prodVect3D(tempD1, ret, tempD2);
		double norm = norm(tempD2);
		if (norm*norm > 1.e-12 * (
				tempD1[0]*tempD1[0] + tempD1[1]*tempD1[1] + tempD1[2]*tempD1[2] +
				ret[0]*ret[0] + ret[1]*ret[1] + ret[2]*ret[2]
				))
		{
			tempD2[0] /= norm;
			tempD2[1] /= norm;
			tempD2[2] /= norm;
		}
		else
		{
			tempD2[0] = tempD2[1] = tempD2[2] = 0.0;
			norm = 0.0;

		}
		prodVect3D(tempD1, tempD2, ret);
		return 0.5*norm;
	}
	/**
	 * Replace current metrics by its inverse.
	 *
	 * @return <code>true</code> if it is not singular, <code>false</code>
	 * otherwise.
	 */
	public final boolean inv()
	{
		double [] temp = new double[9];

		// r0 <- c1 cross c2
		prodVect3D(data, 3, data, 6, temp, 0);
		// r0 . c0
		double det = data[0]*temp[0] + data[1]*temp[1] + data[2]*temp[2];
		if (det < 1.e-20)
			return false;
		// r1 <- c2 cross c0
		prodVect3D(data, 6, data, 0, temp, 3);
		// r2 <- c0 cross c1
		prodVect3D(data, 0, data, 3, temp, 6);
		// Replace data by temp
		System.arraycopy(temp, 0, data, 0, 9);
		transp();
		det = 1.0 / det;
		for (int i = 0; i < 9; i++)
			data[i] *= det;
		return true;
	}
	
	@Override
	public final String toString()
	{
		StringBuilder ret = new StringBuilder();
		for (int i = 0; i < 3; i++)
		{
			ret.append("data|").append(i).append("][] ");
			for (int j = 0; j < 3; j++)
				ret.append(" ").append(data[i + 3 * j]);
			if (i < 2)
				ret.append("\n");
		}
		return ret.toString();
	}
	
}
