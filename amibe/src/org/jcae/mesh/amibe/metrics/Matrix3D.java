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
 * General 3D matrix.
 */
public class Matrix3D
{
	public double data[][] = new double[3][3];
	
	/**
	 * Create a <code>Matrix3D</code> instance and set it to the identity
	 * matrix.
	 */
	public Matrix3D()
	{
		for (int i = 0; i < 3; i++)
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
		for (int i = 0; i < 3; i++)
		{
			data[i][0] = e1[i];
			data[i][1] = e2[i];
			data[i][2] = e3[i];
		}
	}
	
	/**
	 * Create a <code>Matrix3D</code> instance containing the transposition
	 * of this one.
	 *
	 * @return a new Matrix3D containing the transposition of this one.
	 */
	public Matrix3D transp()
	{
		Matrix3D ret = new Matrix3D();
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				ret.data[i][j] = data[j][i];
		return ret;
	}
	
	/**
	 * Create a <code>Matrix3D</code> instance containing the sum of this and
	 * another <code>Matrix3D</code>.
	 *
	 * @param A  matrix to add to the current one
	 * @return a new Matrix3D containing the sum of the two matrices.
	 */
	public Matrix3D add(Matrix3D A)
	{
		Matrix3D ret = new Matrix3D();
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				ret.data[i][j] = data[i][j] + A.data[i][j];
		return ret;
	}
	
	/**
	 * Create a <code>Matrix3D</code> instance containing the multiplication
	 * of this <code>Matrix3D</code> instance by another one.
	 *
	 * @param A  another Matrix3D
	 * @return a new Matrix3D containing the multiplication this*A
	 */
	public Matrix3D multR(Matrix3D A)
	{
		Matrix3D ret = new Matrix3D();
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
			{
				ret.data[i][j] = 0.0;
				for (int k = 0; k < 3; k++)
					ret.data[i][j] += data[i][k] * A.data[k][j];
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
	public Matrix3D multL(Matrix3D A)
	{
		Matrix3D ret = new Matrix3D();
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
			{
				ret.data[i][j] = 0.0;
				for (int k = 0; k < 3; k++)
					ret.data[i][j] += A.data[i][k] * data[k][j];
			}
		return ret;
	}
	
	/**
	 * Return the multiplication of this <code>Matrix3D</code> by a vector.
	 *
	 * @param in  input vector.
	 * @return a new vector containing the multiplication this*in.
	 */
	public double [] apply(double [] in)
	{
		if (3 != in.length)
			throw new IllegalArgumentException(in.length+" is different from 3");
		double [] out = new double[3];
		for (int i = 0; i < 3; i++)
			out[i] = data[i][0] * in[0] + data[i][1] * in[1] + data[i][2] * in[2];
		return out;
	}
	
	/**
	 * Multiply all matrix coefficients by a factor.
	 *
	 * @param f  scale factor
	 */
	protected void scale(double f)
	{
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				data[i][j] *= f;
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
	 * Output is stored in a newly allocated array.
	 *
	 * @param v1 first vector
	 * @param v2 second vector
	 * @return the outer product of two 3D vectors.
	 */
	public static double [] prodVect3D(double [] v1, double [] v2)
	{
		double [] ret = new double[3];
		prodVect3D(v1, v2, ret);
		return ret;			
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
	
	public String toString()
	{
		String ret = "";
		for (int i = 0; i < 3; i++)
		{
			ret += "data|"+i+"][] ";
			for (int j = 0; j < 3; j++)
				ret += " "+data[i][j];
			if (i < 2)
				ret += "\n";
		}
		return ret;
	}
	
}
