/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005, by EADS CRC
    Copyright (C) 2007, by EADS France
 
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

/**
 * General 2D matrix.
 */
public class Matrix2D
{
	public double[][] data = new double[2][2];
	
	/**
	 * Create a <code>Matrix2D</code> instance and set it to the identity
	 * matrix.
	 */
	public Matrix2D()
	{
		data[0][0] = 1.0;
		data[0][1] = 0.0;
		data[1][0] = 0.0;
		data[1][1] = 1.0;
	}
	
	/**
	 * Create a <code>Matrix2D</code> instance with the given coefficients.
	 *
	 * @param Axx  Axx coefficient
	 * @param Axy  Axy coefficient
	 * @param Ayx  Ayx coefficient
	 * @param Ayy  Ayy coefficient
	 */
	public Matrix2D(double Axx, double Axy, double Ayx, double Ayy)
	{
		data[0][0] = Axx;
		data[0][1] = Axy;
		data[1][0] = Ayx;
		data[1][1] = Ayy;
	}
	
	/**
	 * Return the determinant of this matrix.
	 *
	 * @return the determinant of this matrix.
	 */
	public double det()
	{
		return data[0][0] * data[1][1] - data[0][1] * data[1][0];
	}
	
	/**
	 * Create a <code>Matrix2D</code> instance containing the inverse
	 * matrix.
	 * If the matrix is singular, a null object is returned.
	 *
	 * @return a new Matrix2D containing the inverse matrix.
	 */
	private Matrix2D inv()
	{
		double detA = det();
		if (Math.abs(detA) < 1.e-40)
			throw new RuntimeException("Singular matrix: "+this);
		Matrix2D ret = new Matrix2D(data[1][1], -data[0][1], -data[1][0], data[0][0]);
		ret.scale(1.0 / detA);
		return ret;
	}
	
	/**
	 *  Computes the intersection of 2 metrics.
	 */
	protected Matrix2D intersection(Matrix2D B)
	{
		Matrix2D res = simultaneousReduction(B);
		Matrix2D resInv = res.inv();
		double ev1 = Math.max(
			norm2(res.data[0][0], res.data[1][0]),
			B.norm2(res.data[0][0], res.data[1][0])
		);
		double ev2 = Math.max(
			norm2(res.data[0][1], res.data[1][1]),
			B.norm2(res.data[0][1], res.data[1][1])
		);
		double a11 = ev1 * resInv.data[0][0] * resInv.data[0][0] + ev2 * resInv.data[1][0] * resInv.data[1][0];
		double a21 = ev1 * resInv.data[0][0] * resInv.data[0][1] + ev2 * resInv.data[1][0] * resInv.data[1][1];
		double a22 = ev1 * resInv.data[0][1] * resInv.data[0][1] + ev2 * resInv.data[1][1] * resInv.data[1][1];
		return new Matrix2D(a11, a21, a21, a22);
	}
	/* Currently unused
	public Matrix2D rotation(double theta)
	{
		double ct = Math.cos(theta);
		double st = Math.sin(theta);
		return new Matrix2D(ct, -st, st, ct);
	}
	public double [] apply(double [] in)
	{
		double [] out = new double[2];
		out[0] = data[0][0] * in[0] + data[0][1] * in[1];
		out[1] = data[1][0] * in[0] + data[1][1] * in[1];
		return out;
	}
	*/
	
	private Matrix2D multR(Matrix2D A)
	{
		Matrix2D ret = new Matrix2D();
		for (int i = 0; i < 2; i++)
			for (int j = 0; j < 2; j++)
			{
				ret.data[i][j] = 0.0;
				for (int k = 0; k < 2; k++)
					ret.data[i][j] += data[i][k] * A.data[k][j];
			}
		return ret;
	}
	private double norm2(double vx, double vy)
	{
		return data[0][0] * vx * vx + (data[1][0] + data[0][1]) * vx * vy + data[1][1] * vy * vy;
	}
	private void scale(double f)
	{
		for (int i = 0; i < 2; i++)
			for (int j = 0; j < 2; j++)
				data[i][j] *= f;
	}
	
	/**
	 *  Compute eigenvectors of a positive matrix
	 */
	private Matrix2D eigenvectors ()
	{
		Matrix2D ret = new Matrix2D();
		double delta = (data[0][0] - data[1][1]) * (data[0][0] - data[1][1]) + 4.0 * data[0][1] * data[1][0];
		double l1 = 0.5 * (data[0][0] + data[1][1] + Math.sqrt(delta));
		double l2 = 0.5 * (data[0][0] + data[1][1] - Math.sqrt(delta));
		double t1 = (data[0][0]-l1) * (data[0][0]-l1);
		double t2 = data[0][1] * data[0][1];
		double t3 = data[1][0] * data[1][0];
		double t4 = (data[1][1]-l1) * (data[1][1]-l1);
		if (t1 + t2 < t3 + t4)
		{
			double invnorm = 1.0 / Math.sqrt(t3 + t4);
			ret.data[0][0] = (l1-data[1][1]) * invnorm;
			ret.data[1][0] = data[1][0] * invnorm;
		}
		else if (t1 + t2 == 0.0)
		{
			ret.data[0][0] = 1.0;
			ret.data[1][0] = 0.0;
		}
		else
		{
			double invnorm = 1.0 / Math.sqrt(t1 + t2);
			ret.data[0][0] = data[0][1] * invnorm;
			ret.data[1][0] = (l1-data[0][0]) * invnorm;
		}
		if (Math.abs(l1 - l2) < 1.e-10 * (Math.abs(l1) + Math.abs(l2)))
		{
			ret.data[0][1] = - ret.data[1][0];
			ret.data[1][1] = ret.data[0][0];
			return ret;
		}

		t1 = (data[0][0]-l2) * (data[0][0]-l2);
		t2 = data[0][1] * data[0][1];
		t3 = data[1][0] * data[1][0];
		t4 = (data[1][1]-l2) * (data[1][1]-l2);
		if (t1 + t2 < t3 + t4)
		{
			double invnorm = 1.0 / Math.sqrt(t3 + t4);
			ret.data[0][1] = (l2-data[1][1]) * invnorm;
			ret.data[1][1] = data[1][0] * invnorm;
		}
		else
		{
			double invnorm = 1.0 / Math.sqrt(t1 + t2);
			ret.data[0][1] = data[0][1] * invnorm;
			ret.data[1][1] = (l2-data[0][0]) * invnorm;
		}
		return ret;
	}
	
	/*
	 *  Computes the simultaneous reduction of 2 metrics
	 */
	private Matrix2D simultaneousReduction (Matrix2D B)
	{
		Matrix2D AinvB = inv().multR(B);
		double delta = (AinvB.data[0][0] - AinvB.data[1][1]) * (AinvB.data[0][0] - AinvB.data[1][1]) + 4.0 * AinvB.data[0][1] * AinvB.data[1][0];
		if (delta < 1.e-20 * (AinvB.data[0][0] * AinvB.data[0][0] + AinvB.data[0][1] * AinvB.data[0][1] + AinvB.data[1][0] * AinvB.data[1][0] + AinvB.data[1][1] * AinvB.data[1][1]))
			//  Matrices are similar.  Return eigenvectors of A
			return eigenvectors();
		
		return AinvB.eigenvectors();
	}
	
	@Override
	public String toString()
	{
		return "Matrix2D: ("+data[0][0]+", "+data[0][1]+", "+data[1][0]+", "+data[1][1]+")";
	}
	
}
