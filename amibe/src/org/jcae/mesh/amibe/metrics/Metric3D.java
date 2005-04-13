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

package org.jcae.mesh.amibe.metrics;

import org.jcae.mesh.cad.CADGeomSurface;
import org.jcae.mesh.amibe.ds.Vertex;
import org.apache.log4j.Logger;

/**
 * 3D metrics.
 */
public class Metric3D extends Matrix
{
	private static Logger logger=Logger.getLogger(Metric3D.class);
	
	//  Cached variables to improve performance
	private static CADGeomSurface cacheSurf = null;
	
	private static double discr = 1.0;
	private static double defl = 0.0;
	private static boolean relDefl = true;
	private static double [][] temp32 = new double[3][2];
	private static double [][] temp22 = new double[2][2];
	
	/**
	 * Creates a <code>Metric3D</code> instance at a given point.
	 *
	 * @param surf  geometrical surface
	 * @param pt  node where metrics is computed.
	 */
	public Metric3D(CADGeomSurface surf, Vertex pt)
	{
		rank = 3;
		data = new double[rank][rank];
		for (int i = 0; i < rank; i++)
		{
			for (int j = 0; j < rank; j++)
				data[i][j] = 0.0;
			data[i][i] = 1.0;
		}
		if (!surf.equals(cacheSurf))
		{
			surf.dinit(2);
			cacheSurf = surf;
		}
		double uv[] = pt.getUV();
		cacheSurf.setParameter(uv[0], uv[1]);
	}
	
	public Metric3D(double [] e1, double [] e2, double [] e3)
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
	
	public static double prodSca(double [] A, double [] B) {
		return ((A[0]*B[0])+(A[1]*B[1])+(A[2]*B[2]));
	}
	
	public static double norm(double [] A) {
		return Math.sqrt((A[0]*A[0])+(A[1]*A[1])+(A[2]*A[2]));
	}
	
	public static double [] prodVect3D(double [] v1, double [] v2)
	{
		double [] ret = new double[3];
		ret[0] = v1[1] * v2[2] - v1[2] * v2[1];
		ret[1] = v1[2] * v2[0] - v1[0] * v2[2];
		ret[2] = v1[0] * v2[1] - v1[1] * v2[0];
		return ret;			
	}
	
	public boolean iso()
	{
		for (int i = 0; i < 3; i++)
		{
			for (int j = 0; j < 3; j++)
				data[i][j] = 0.0;
			data[i][i] = 1.0/discr/discr;
		}
		return true;
	}
	
	public boolean absDeflection(boolean isotropic)
	{
		double cmin = Math.abs(cacheSurf.minCurvature());
		double cmax = Math.abs(cacheSurf.maxCurvature());
		if (Double.isNaN(cmin) || Double.isNaN(cmax))
		{
			logger.debug("Undefined curvature");
			return false;
		}
		if (cmin == 0.0 && cmax == 0.0)
		{
			logger.debug("Infinite curvature");
			return false;
		}
		double [] dcurv = cacheSurf.curvatureDirections();
		double [] dcurvmax = new double[3];
		double [] dcurvmin = new double[3];
		System.arraycopy(dcurv, 0, dcurvmax, 0, 3);
		System.arraycopy(dcurv, 3, dcurvmin, 0, 3);
		if (cmin > cmax)
		{
			double temp = cmin;
			cmin = cmax;
			cmax = temp;
			System.arraycopy(dcurv, 0, dcurvmin, 0, 3);
			System.arraycopy(dcurv, 3, dcurvmax, 0, 3);
		}
		Metric3D A = new Metric3D(dcurvmax, dcurvmin, prodVect3D(dcurvmax, dcurvmin));
		double epsilon = defl * cmax;
		if (epsilon >= 1.0)
			epsilon = 1.0;
		//  In org.jcae.mesh.amibe.algos2d.Insertion, mean lengths are
		//  targeted, and there is a sqrt(2) factor.  Division bt 2
		//  provides a maximal deflection, 
		double alpha2 = 4.0 * epsilon * (2.0 - epsilon) / 2.0;
		data[0][0] = cmax*cmax / alpha2;
		if (isotropic)
			data[1][1] = data[0][0];
		else
		{
			if (cmin > 0.0)
			{
				epsilon *= cmin / cmax;
				if (epsilon >= 1.0)
					epsilon = 1.0;
				alpha2 = 4.0 * epsilon * (2.0 - epsilon) / 2.0;
			}
			data[1][1] = cmin*cmin / alpha2;
		}
		data[2][2] = data[0][0];
		Matrix res = (this.multL(A.transp())).multR(A);
		data = res.data;
		return true;
	}
	
	public boolean relDeflection(boolean isotropic)
	{
		double cmin = Math.abs(cacheSurf.minCurvature());
		double cmax = Math.abs(cacheSurf.maxCurvature());
		if (Double.isNaN(cmin) || Double.isNaN(cmax))
		{
			logger.debug("Undefined curvature");
			return false;
		}
		if (cmin == 0.0 && cmax == 0.0)
		{
			logger.debug("Infinite curvature");
			return false;
		}
		double [] dcurv = cacheSurf.curvatureDirections();
		double [] dcurvmax = new double[3];
		double [] dcurvmin = new double[3];
		System.arraycopy(dcurv, 0, dcurvmax, 0, 3);
		System.arraycopy(dcurv, 3, dcurvmin, 0, 3);
		if (cmin > cmax)
		{
			double temp = cmin;
			cmin = cmax;
			cmax = temp;
			System.arraycopy(dcurv, 0, dcurvmin, 0, 3);
			System.arraycopy(dcurv, 3, dcurvmax, 0, 3);
		}
		Metric3D A = new Metric3D(dcurvmax, dcurvmin, prodVect3D(dcurvmax, dcurvmin));
		double epsilon = defl;
		if (epsilon > 1.0)
			epsilon = 1.0;
		//  In org.jcae.mesh.amibe.algos2d.Insertion, mean lengths are
		//  targeted, and there is a sqrt(2) factor.  Division bt 2
		//  provides a maximal deflection, 
		double alpha2 = 4.0 * epsilon * (2.0 - epsilon) / 2.0;
		data[0][0] = cmax*cmax / alpha2;
		if (isotropic)
		{
			data[1][1] = data[0][0];
			data[2][2] = data[0][0];
		}
		else
		{
			epsilon *= cmax / cmin;
			if (epsilon > 1.0)
				epsilon = 1.0;
			alpha2 = 4.0 * epsilon * (2.0 - epsilon) / 2.0;
			data[1][1] = cmin*cmin / alpha2;
			data[2][2] = 1.0/discr/discr;
		}
		Matrix res = (this.multL(A.transp())).multR(A);
		data = res.data;
		return true;
	}
	
	public static boolean hasDeflection()
	{
		return (defl > 0.0);
	}
	
	public static boolean hasRelativeDeflection()
	{
		return relDefl;
	}
	
	public static void setRelativeDeflection(boolean b)
	{
		relDefl = b;
	}
	
	public static boolean hasLength()
	{
		return (discr > 0.0);
	}
	
	/**
	 * Set the desired edge length.
	 */
	public static void setLength(double l)
	{
		discr = l;
	}
	
	/**
	 * Get the desired edge length.
	 */
	public static double getLength()
	{
		return discr;
	}
	
	/**
	 * Set the desired edge length.
	 */
	public static void setDeflection(double l)
	{
		defl = l;
	}
	
	/**
	 * Get the desired edge length.
	 */
	public static double getDeflection()
	{
		return defl;
	}
	
	/**
	 * Computes the matrics restriction to its tangent plan.
	 */
	public double[][] restrict2D()
	{
		double d1U[] = cacheSurf.d1U();
		double d1V[] = cacheSurf.d1V();
		double unitNorm[] = prodVect3D(d1U, d1V);
		double nnorm = norm(unitNorm);
		if (nnorm > 0.0)
		{
			nnorm = 1.0 / nnorm;
			unitNorm[0] *= nnorm;
			unitNorm[1] *= nnorm;
			unitNorm[2] *= nnorm;
		}
		else
		{
			logger.debug("Unable to compute normal vector");
			unitNorm[0] = 0.0;
			unitNorm[1] = 0.0;
			unitNorm[2] = 0.0;
		}
		
		//  temp32 = M3 * (d1U,d1V,unitNorm)
		for (int i = 0; i < 3; i++)
		{
			temp32[i][0] = data[i][0] * d1U[0] + data[i][1] * d1U[1] + data[i][2] * d1U[2];
			temp32[i][1] = data[i][0] * d1V[0] + data[i][1] * d1V[1] + data[i][2] * d1V[2];
		}
		//  temp22 = t(d1U,d1V,unitNorm) * temp32
		temp22[0][0] = d1U[0] * temp32[0][0] + d1U[1] * temp32[1][0] + d1U[2] * temp32[2][0];
		temp22[0][1] = d1U[0] * temp32[0][1] + d1U[1] * temp32[1][1] + d1U[2] * temp32[2][1];
		temp22[1][0] = d1V[0] * temp32[0][0] + d1V[1] * temp32[1][0] + d1V[2] * temp32[2][0];
		temp22[1][1] = d1V[0] * temp32[0][1] + d1V[1] * temp32[1][1] + d1V[2] * temp32[2][1];
		return temp22;
	}
	
}
