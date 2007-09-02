/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005,2006 by EADS CRC
 
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
 * 3D metrics computed on a CAD surface.  This class provides 3D metrics at a
 * point to have a unit mesh with respect to edge length and deflection
 * criteria.
 *
 * <p>
 * A metric M is a symmetric positive matrix.  It defines a dot product
 * <code>&lt;X, Y> = tX M Y</code>.  If metrics are constant, the length
 * of the <code>[PQ]</code> segment in this metrics is
 * <code>l(M,P,Q)=sqrt(t(PQ) M (PQ))</code>.
 * A good presentation of meshes governed by metrics can be found in
 * <a href="ftp://ftp.inria.fr/INRIA/publication/publi-pdf/RR/RR-2928.pdf">Maillage
 * de surfaces paramétriques</a> (in French), by Houman Borouchaki and Paul
 * Louis George.
 * </p>
 *
 * <p>
 * The metrics associated with an edge length criterion is the 3x3 matrix
 * <code>M=Id/(h*h)</code>, where <code>h</code> is the target size.  Indeed the
 * relation above clearly shows that <code>l(M,P,Q)=1</code> if and only
 * if the Euclidian distance between <code>P</code> and <code>Q</code> is
 * <code>h</code>.  Such a metric is computed by the {@link #iso(double)}
 * method.
 * </p>
 *
 * <p>
 * An isotropic metric governed by a given <code>defl</code> geometric error is
 * <code>M=Id*(Cm*Cm)/(alpha*alpha)</code>, where <code>Cm</code> is the largest
 * curvature and <code>alpha=2*sqrt(defl*(2-defl))</code>.
 * Of course this geometric error can be guaranteed onlyelocally, it becomes
 * can be larger if <code>defl</code> is not small enough.
 * An anisotropic metric can also be computed along principal curvature
 * directions, see the technical report above or these sources to find the
 * exact computations.
 * </p>
 *
 * <p>
 * Some applications require an absolute geometric error.  A first order
 * approximation is obtained by replacing <code>defl</code> by
 * <code>defl*Cm</code> in the previous metrics.
 * </p>
 *
 * <p>
 * When meshing parametrized surfaces, we need the 2D metric induced
 * by these 3D metrics to the tangent plane.  This is performed by
 * {@link #restrict2D}.
 * </p>
 */
public class Metric3D extends Matrix3D implements Cloneable
{
	private static Logger logger=Logger.getLogger(Metric3D.class);
	
	//  Cached variables to improve performance
	private static CADGeomSurface cacheSurf = null;
	
	private static double discr = 1.0;
	private static double defl = 0.0;
	private static boolean relDefl = true;
	private static boolean isotropic = true;
	private static double [] c0 = new double[3];
	private static double [] c1 = new double[3];
	private static double [] c2 = new double[3];
	private static double [] c3 = new double[3];
	private static double [] c4 = new double[3];
	private static double [][] temp32 = new double[3][2];
	private static double [][] temp22 = new double[2][2];
	
	/**
	 * Create a <code>Metric3D</code> instance at a given point.
	 *
	 * @param surf  geometrical surface
	 * @param pt  node where metrics is computed.
	 */
	public Metric3D(CADGeomSurface surf, Vertex pt)
	{
		super();
		if (!surf.equals(cacheSurf))
		{
			surf.dinit(2);
			cacheSurf = surf;
		}
		double uv[] = pt.getUV();
		cacheSurf.setParameter(uv[0], uv[1]);
	}
	
	/**
	 * Create a <code>Metric3D</code> instance and set it to the identity
	 * matrix.
	 */
	public Metric3D()
	{
		super();
	}
	
	/**
	 * Create a <code>Metric3D</code> instance from three column vectors.
	 *
	 * @param e1  first column.
	 * @param e2  second column.
	 * @param e3  third column.
	 */
	public Metric3D(double [] e1, double [] e2, double [] e3)
	{
		super(e1, e2, e3);
	}
	
	/**
	 * Create a <code>Metric3D</code> instance and copy another instance.
	 *
	 * @param that  instance being copied.
	 */
	public Object clone()
	{
		Metric3D ret = null;
		try {
			ret = (Metric3D) super.clone();
		} catch(CloneNotSupportedException ex) {
			ex.printStackTrace();
		}
		System.arraycopy(data, 0, ret.data, 0, 9);
		return ret;
	}
	
	/**
	 * Check whether deflection is requested.
	 *
	 * @return <code>true</code> if deflection is requested,
	 * <code>false</code> otherwise.
	 */
	public static boolean hasDeflection()
	{
		return (defl > 0.0);
	}
	
	/**
	 * Check whether deflection is relative or absolute.
	 *
	 * @return <code>true</code> if deflection is relative,
	 * <code>false</code> otherwise.
	 */
	public static boolean hasRelativeDeflection()
	{
		return relDefl;
	}
	
	/**
	 * Select relative or absolute deflection.
	 *
	 * @param b if <code>true</code>, deflection is relative,
	 * otherwise it is absolute.
	 */
	public static void setRelativeDeflection(boolean b)
	{
		relDefl = b;
	}
	
	/**
	 * Select isotropic or anisotropic discretization.
	 *
	 * @param b if <code>true</code>, discretization is isotropic,
	 * otherwise it is anisotropic.
	 */
	public static void setIsotropic(boolean b)
	{
		isotropic = b;
	}
	
	/**
	 * Check whether a length criterion is requested.
	 *
	 * @return <code>true</code> if a length criterion is requested,
	 * <code>false</code> otherwise.
	 */
	public static boolean hasLength()
	{
		return (discr > 0.0);
	}
	
	/**
	 * Set the desired edge length.
	 *
	 * @param l  the desired edge length.
	 */
	public static void setLength(double l)
	{
		discr = l;
	}
	
	/**
	 * Get the desired edge length.
	 *
	 * @return  the desired edge length.
	 */
	public static double getLength()
	{
		return discr;
	}
	
	/**
	 * Set the desired deflection.
	 *
	 * @param l  the desired deflection.
	 */
	public static void setDeflection(double l)
	{
		defl = l;
	}
	
	/**
	 * Get the desired deflection.
	 *
	 * @return  the desired deflection.
	 */
	public static double getDeflection()
	{
		return defl;
	}
	
	/**
	 * Compute matrix determinant.
	 *
	 * @return the matrix determinant.
	 */
	public final double det()
	{
		// adjoint matrix
		copyColumn(1, c1);
		copyColumn(2, c2);
		prodVect3D(c1, c2, c0);
		copyColumn(0, c1);
		return prodSca(c0, c1);
	}
	
	/**
	 * Replace current metrics by its inverse.
	 *
	 * @return <code>true</code> if it is not singular, <code>false</code>
	 * otherwise.
	 */
	public final boolean inv()
	{
		// adjoint matrix
		copyColumn(0, c0);
		copyColumn(1, c1);
		copyColumn(2, c2);
		prodVect3D(c1, c2, c3);
		double det = prodSca(c0, c3);
		if (det < 1.e-20)
			return false;
		prodVect3D(c2, c0, c4);
		prodVect3D(c0, c1, c2);
		// Metric3D adj = new Metric3D(c3, c4, c2);
		System.arraycopy(c3, 0, data, 0, 3);
		System.arraycopy(c4, 0, data, 3, 3);
		System.arraycopy(c2, 0, data, 6, 3);
		swap(0, 1);
		swap(0, 2);
		swap(1, 2);
		scale(1.0 / det);
		return true;
	}
	
	/**
	 * Set the current metrics to be governed by an edge length.
	 * It is thus Id/l^2.
	 *
	 * @param l  the desired edge length.
	 * @return <code>true</code> if this metrics has been successfully
	 * computed, <code>false</code> otherwise.
	 */
	public boolean iso(double l)
	{
		if (l <= 0)
			return false;
		reset();
		double diag = 1.0/l/l;
		setDiagonal(diag, diag, diag);
		return true;
	}
	
	/**
	 * Set the current metrics to be governed by surface deflection.
	 * Deflection is relative or absolute depending on the
	 * <code>relDefl</code> instance variable.
	 *
	 * @return <code>true</code> if this metrics has been successfully
	 * computed, <code>false</code> otherwise.
	 */
	public boolean deflection()
	{
		if (relDefl)
			return relDeflection();
		return absDeflection();
	}
	
	private boolean relDeflection()
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
		double [] dcurvmax = c0;
		double [] dcurvmin = c1;
		if (cmin < cmax)
		{
			System.arraycopy(dcurv, 0, dcurvmax, 0, 3);
			System.arraycopy(dcurv, 3, dcurvmin, 0, 3);
		}
		else
		{
			double temp = cmin;
			cmin = cmax;
			cmax = temp;
			System.arraycopy(dcurv, 0, dcurvmin, 0, 3);
			System.arraycopy(dcurv, 3, dcurvmax, 0, 3);
		}
		prodVect3D(dcurvmax, dcurvmin, c2);
		Metric3D A = new Metric3D(dcurvmax, dcurvmin, c2);
		double epsilon = defl;
		if (epsilon > 1.0)
			epsilon = 1.0;
		//  In org.jcae.mesh.amibe.algos2d.Insertion, mean lengths are
		//  targeted, and there is a sqrt(2) factor.  Division by 2
		//  provides a maximal deflection, 
		double alpha2 = 4.0 * epsilon * (2.0 - epsilon) / 2.0;
		double diag = cmax*cmax / alpha2;
		if (isotropic)
			setDiagonal(diag, diag, diag);
		else
		{
			epsilon *= cmax / cmin;
			if (epsilon > 1.0)
				epsilon = 1.0;
			alpha2 = 4.0 * epsilon * (2.0 - epsilon) / 2.0;
			setDiagonal(diag, cmin*cmin / alpha2, 1.0/discr/discr);
		}
		A.transp();
		Matrix3D temp = this.multL(A);
		A.transp();
		Matrix3D res = temp.multR(A);
		System.arraycopy(res.data, 0, data, 0, 9);
		return true;
	}
	
	private boolean absDeflection()
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
			logger.debug("Null curvature");
			return false;
		}
		if (defl * cmax >= 1.0 || defl * cmin >= 1.0)
		{
			logger.debug("Curvature too large");
			iso(defl);
			return true;
		}
		double [] dcurv = cacheSurf.curvatureDirections();
		double [] dcurvmax = c0;
		double [] dcurvmin = c1;
		if (cmin < cmax)
		{
			System.arraycopy(dcurv, 0, dcurvmax, 0, 3);
			System.arraycopy(dcurv, 3, dcurvmin, 0, 3);
		}
		else
		{
			double temp = cmin;
			cmin = cmax;
			cmax = temp;
			System.arraycopy(dcurv, 0, dcurvmin, 0, 3);
			System.arraycopy(dcurv, 3, dcurvmax, 0, 3);
		}
		prodVect3D(dcurvmax, dcurvmin, c2);
		Metric3D A = new Metric3D(dcurvmax, dcurvmin, c2);
		double epsilon = defl * cmax;
		//  In org.jcae.mesh.amibe.algos2d.Insertion, mean lengths are
		//  targeted, and there is a sqrt(2) factor.  Division by 2
		//  provides a maximal deflection, 
		double alpha2 = 4.0 * epsilon * (2.0 - epsilon) / 2.0;
		double diag = cmax*cmax / alpha2;
		if (isotropic)
			setDiagonal(diag, diag, diag);
		else
		{
			if (cmin > 0.0)
			{
				epsilon *= cmin / cmax;
				if (epsilon >= 1.0)
					epsilon = 1.0;
				alpha2 = 4.0 * epsilon * (2.0 - epsilon) / 2.0;
			}
			setDiagonal(diag, cmin*cmin / alpha2, diag);
		}
		A.transp();
		Matrix3D temp = this.multL(A);
		A.transp();
		Matrix3D res = temp.multR(A);
		System.arraycopy(res.data, 0, data, 0, 9);
		return true;
	}
	
	/**
	 * Compute the matrics induced to the tangent plane.
	 */
	public double[][] restrict2D()
	{
		double d1U[] = cacheSurf.d1U();
		double d1V[] = cacheSurf.d1V();
		// Check whether there is a tangent plane
		prodVect3D(d1U, d1V, c0);
		if (norm(c0) <= 0.0)
			logger.debug("Unable to compute normal vector");
		
		// B = (d1U,d1V,c0) is the local frame.
		// The metrics induced by M3 on the tangent plane is tB M3 B
		// temp32 = M3 * B
		for (int i = 0; i < 3; i++)
		{
			temp32[i][0] = data[i] * d1U[0] + data[i+3] * d1U[1] + data[i+6] * d1U[2];
			temp32[i][1] = data[i] * d1V[0] + data[i+3] * d1V[1] + data[i+6] * d1V[2];
		}
		//  temp22 = tB * temp32
		temp22[0][0] = d1U[0] * temp32[0][0] + d1U[1] * temp32[1][0] + d1U[2] * temp32[2][0];
		temp22[0][1] = d1U[0] * temp32[0][1] + d1U[1] * temp32[1][1] + d1U[2] * temp32[2][1];
		temp22[1][0] = d1V[0] * temp32[0][0] + d1V[1] * temp32[1][0] + d1V[2] * temp32[2][0];
		temp22[1][1] = d1V[0] * temp32[0][1] + d1V[1] * temp32[1][1] + d1V[2] * temp32[2][1];
		return temp22;
	}
	
}
