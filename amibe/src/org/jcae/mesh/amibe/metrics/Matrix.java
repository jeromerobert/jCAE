/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005
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
 * Common operations on squared matrices.
 */
public class Matrix implements Cloneable
{
	private static Logger logger=Logger.getLogger(Matrix.class);
	
	protected int rank = 0;
	public double data[][];
	
	/**
	 * Create a <code>Matrix</code> instance with the same coefficients.
	 */
	protected final Object clone()
	{
		Object ret = null;
		try
		{
			ret = super.clone();
			Matrix that = (Matrix) ret;
			that.data = new double[rank][rank];
			for (int i = 0; i < rank; i++)
				for (int j = 0; j < rank; j++)
					that.data[i][j] = data[i][j];
		}
		catch (java.lang.CloneNotSupportedException ex)
		{
		}
		return ret;
	}
	
	/**
	 * Create a <code>Matrix</code> instance containing the transposition
	 * of this one.
	 *
	 * @return a new Matrix containing the transposition of this one.
	 */
	public Matrix transp()
	{
		if (rank <= 0)
			throw new IllegalArgumentException("Uninitialized matrix");
		Matrix ret = (Matrix) clone();
		double temp;
		for (int i = 0; i < rank; i++)
			for (int j = i+1; j < rank; j++)
			{
				temp = data[i][j];
				data[i][j] = data[j][i];
				data[j][i] = temp;
			}
		return ret;
	}
	
	/**
	 * Create a <code>Matrix</code> instance containing the sum of this and
	 * another <code>Matrix</code>.
	 *
	 * @param A  matrix to add to the current one
	 * @return a new Matrix containing the sum of the two matrices.
	 */
	public Matrix add(Matrix A)
	{
		if (rank != A.rank || rank <= 0)
			throw new IllegalArgumentException(rank+" is different from "+A.rank);
		Matrix ret = (Matrix) clone();
		for (int i = 0; i < rank; i++)
			for (int j = 0; j < rank; j++)
				data[i][j] += A.data[i][j];
		return ret;
	}
	
	/**
	 * Create a <code>Matrix</code> instance containing the multiplication
	 * of this <code>Matrix</code> instance by another one.
	 *
	 * @param A  another Matrix
	 * @return a new Matrix containing the multiplication this*A
	 */
	public Matrix multR(Matrix A)
	{
		if (rank != A.rank || rank <= 0)
			throw new IllegalArgumentException(rank+" is different from "+A.rank);
		Matrix ret = new Matrix();
		ret.rank = rank;
		ret.data = new double[rank][rank];
		for (int i = 0; i < rank; i++)
			for (int j = 0; j < rank; j++)
			{
				ret.data[i][j] = 0.0;
				for (int k = 0; k < rank; k++)
					ret.data[i][j] += data[i][k] * A.data[k][j];
			}
		return ret;
	}
	
	/**
	 * Create a <code>Matrix</code> instance containing the multiplication
	 * of another <code>Matrix</code> instance by this one.
	 *
	 * @param A  another Matrix
	 * @return a new Matrix containing the multiplication A*this
	 */
	public Matrix multL(Matrix A)
	{
		if (rank != A.rank || rank <= 0)
			throw new IllegalArgumentException(rank+" is different from "+A.rank);
		Matrix ret = new Matrix();
		ret.rank = rank;
		ret.data = new double[rank][rank];
		for (int i = 0; i < rank; i++)
			for (int j = 0; j < rank; j++)
			{
				ret.data[i][j] = 0.0;
				for (int k = 0; k < rank; k++)
					ret.data[i][j] += A.data[i][k] * data[k][j];
			}
		return ret;
	}
	
	/**
	 * Return the multiplication of this <code>Matrix</code> by a vector.
	 *
	 * @param in  input vector.
	 * @return a new vector containing the multiplication this*in.
	 */
	public double [] apply(double [] in)
	{
		if (rank != in.length || rank <= 0)
			throw new IllegalArgumentException(rank+" is different from "+in.length);
		double [] out = new double[rank];
		for (int i = 0; i < rank; i++)
		{
			out[i] = 0.0;
			for (int j = 0; j < rank; j++)
				out[i] += data[i][j] * in[j];
		}
		return out;
	}
	
	/**
	 * Multiply all matrix coefficients by a factor.
	 *
	 * @param f  scale factor
	 */
	protected void scale(double f)
	{
		if (rank <= 0)
			throw new IllegalArgumentException("Uninitialized matrix");
		for (int i = 0; i < rank; i++)
			for (int j = 0; j < rank; j++)
				data[i][j] *= f;
	}
	
	public String toString()
	{
		if (rank <= 0)
			throw new IllegalArgumentException("Uninitialized matrix");
		String ret = "";
		for (int i = 0; i < rank; i++)
		{
			ret += "data|"+i+"][] ";
			for (int j = 0; j < rank; j++)
				ret += " "+data[i][j];
			if (i < rank - 1)
				ret += "\n";
		}
		return ret;
	}
	
}
