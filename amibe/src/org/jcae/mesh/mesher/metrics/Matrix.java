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

package org.jcae.mesh.mesher.metrics;

import org.apache.log4j.Logger;

/**
 * 2D metrics.
 */
public class Matrix
{
	private static Logger logger=Logger.getLogger(Matrix.class);
	
	protected int rank = 0;
	public double data[][];
	
	public void clone(Matrix A)
	{
		if (rank != A.rank || rank <= 0)
			throw new IllegalArgumentException(rank+" is different from "+A.rank);
		for (int i = 0; i < rank; i++)
			for (int j = 0; j < rank; j++)
				data[i][j] = A.data[i][j];
	}
	
	public Matrix dup()
	{
		if (rank <= 0)
			throw new IllegalArgumentException("Uninitialized matrix");
		Matrix ret = new Matrix();
		ret.rank = rank;
		ret.data = new double[rank][rank];
		ret.clone(this);
		return ret;
	}
	
	public Matrix transp()
	{
		if (rank <= 0)
			throw new IllegalArgumentException("Uninitialized matrix");
		Matrix ret = dup();
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
	
	public double tr()
	{
		if (rank <= 0)
			throw new IllegalArgumentException("Uninitialized matrix");
		double ret = 0.0;
		for (int i = 0; i < rank; i++)
			ret += data[i][i];
		return ret;
	}
	
	public Matrix neg()
	{
		if (rank <= 0)
			throw new IllegalArgumentException("Uninitialized matrix");
		Matrix ret = dup();
		for (int i = 0; i < rank; i++)
			for (int j = 0; j < rank; j++)
				data[i][j] = - data[i][j];
		return ret;
	}
	
	public Matrix add(Matrix A)
	{
		if (rank != A.rank || rank <= 0)
			throw new IllegalArgumentException(rank+" is different from "+A.rank);
		Matrix ret = dup();
		for (int i = 0; i < rank; i++)
			for (int j = 0; j < rank; j++)
				data[i][j] += A.data[i][j];
		return ret;
	}
	
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
	
	public void scale(double f)
	{
		if (rank <= 0)
			throw new IllegalArgumentException("Uninitialized matrix");
		for (int i = 0; i < rank; i++)
			for (int j = 0; j < rank; j++)
				data[i][j] *= f;
	}
	
	public Matrix factor(double f)
	{
		if (rank <= 0)
			throw new IllegalArgumentException("Uninitialized matrix");
		Matrix ret = dup();
		ret.scale(f);
		return ret;
	}
	
}
