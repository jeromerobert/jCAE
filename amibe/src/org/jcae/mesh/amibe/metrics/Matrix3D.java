/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.
 
    Copyright (C) 2005,2006 by EADS CRC
 
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
	protected double [] data = new double[9];
	
	/**
	 * Create a <code>Matrix3D</code> instance and set it to the identity
	 * matrix.
	 */
	public Matrix3D()
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
	 * Reset all coefficients to zero.
	 */
	public void reset()
	{
		for (int i = 0; i < 9; i++)
			data[i] = 0.0;
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
				ret.data[3*i+j] = data[3*j+i];
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
	public Matrix3D multL(Matrix3D A)
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
			out[i] = data[i] * in[0] + data[i+3] * in[1] + data[i+6] * in[2];
		return out;
	}
	
	/**
	 * Multiply all matrix coefficients by a factor.
	 *
	 * @param f  scale factor
	 */
	protected void scale(double f)
	{
		for (int i = 0; i < 9; i++)
			data[i] *= f;
	}
	
	/**
	 * Set diagonal coefficients.
	 *
	 * @param d1  first diagonal coefficient
	 * @param d2  second diagonal coefficient
	 * @param d3  third diagonal coefficient
	 */
	protected void setDiagonal(double d1, double d2, double d3)
	{
		data[0] = d1;
		data[4] = d2;
		data[8] = d3;
	}
	
	public void saxpby0(double l1, Matrix3D a, double l2, Matrix3D b)
	{
		for (int i = 0; i < 9; i++)
			data[i] = l1 * a.data[i] + l2 * b.data[i];
	}
	
	protected final void swap(int i, int j)
	{
		double temp = data[i+3*j];
		data[i+3*j] = data[j+3*i];
		data[j+3*i] = temp;
	}
	
	protected final void copyColumn(int i, double [] dest)
	{
		System.arraycopy(data, 3*i, dest, 0, 3);
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
				ret += " "+data[i+3*j];
			if (i < 2)
				ret += "\n";
		}
		return ret;
	}
	
}
