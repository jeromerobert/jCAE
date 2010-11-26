/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005,2006, by EADS CRC
    Copyright (C) 2007,2008,2009,2010, by EADS France
 
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

package org.jcae.mesh.amibe.patch;

import org.jcae.mesh.cad.CADGeomSurface;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.metrics.Matrix2D;
import org.jcae.mesh.amibe.metrics.PoolWorkVectors;
import org.jcae.mesh.amibe.ds.MeshParameters;
import java.util.logging.Logger;

/**
 * 3D metrics computed on a CAD surface.  This class provides 3D metrics at a
 * point to have a unit mesh with respect to edge length and getDeflectionMetric
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
 * The metric associated with an edge length criterion is the 3x3 matrix
 * <code>M=Id/(h*h)</code>, where <code>h</code> is the target size.  Indeed the
 * relation above clearly shows that <code>l(M,P,Q)=1</code> if and only
 * if the Euclidian distance between <code>P</code> and <code>Q</code> is
 * <code>h</code>.  Such a metric is an instance of {@link Metric2D}
 * method.
 * </p>
 *
 * <p>
 * An isotropic metric governed by a given <code>defl</code> geometric error is
 * <code>M=Id*(Cm*Cm)/(alpha*alpha)</code>, where <code>Cm</code> is the largest
 * curvature and <code>alpha=2*sqrt(defl*(2-defl))</code>.
 * Of course this geometric error can be guaranteed only locally, it
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
class MetricBuilder
{
	private static final Logger LOGGER=Logger.getLogger(MetricBuilder.class.getName());

	private final PoolWorkVectors temp;
	private final CADGeomSurface surf;
	private final MeshParameters mp;

	MetricBuilder(CADGeomSurface surf, MeshParameters mp, PoolWorkVectors temp)
	{
		this.surf = surf;
		this.mp = mp;
		this.temp = temp;
	}

	/**
	 * Creates a <code>MetricOnSurface</code> instance at a given point.
	 *
	 * @param surf  geometrical surface
	 * @param mp    mesh parameters
	 */
	MetricOnSurface computeMetricOnSurface()
	{
		Matrix2D m2d0 = computeIsotropic();
		Matrix2D m2d1 = computeGeometric();
		if (m2d1 != null)
		{
			//  The curvature metric is defined, so we can compute
			//  its intersection with isotropic m2d0.
			m2d0.makeSymmetric();
			m2d1.makeSymmetric();
			m2d0.intersection(m2d1).getValues(temp.tt22);
		}
		else
			m2d0.getValues(temp.tt22);

		double E = temp.tt22[0][0];
		double F = 0.5 * (temp.tt22[0][1] + temp.tt22[1][0]);
		double G = temp.tt22[1][1];
		return new MetricOnSurface(E, F, G);
	}

	private Matrix2D computeIsotropic()
	{
		double length = mp.getLength();
		double diag = 1.0/length/length;
		Matrix3D iso = new Matrix3D(diag, diag, diag);
		return restrict2D(iso, surf, temp);
	}

	private Matrix2D computeGeometric()
	{
		if (!mp.hasDeflection())
			return null;
		Matrix3D m;
		if (mp.hasRelativeDeflection())
			m = getRelativeDeflectionMetric();
		else
			m = getAbsoluteDeflectionMetric();
		if (m == null)
			return null;
		return restrict2D(m, surf, temp);
	}

	private Matrix3D getRelativeDeflectionMetric()
	{
		double cmin = Math.abs(surf.minCurvature());
		double cmax = Math.abs(surf.maxCurvature());
		if (Double.isNaN(cmin) || Double.isNaN(cmax))
		{
			LOGGER.fine("Undefined curvature");
			return null;
		}
		if (cmin == 0.0 && cmax == 0.0)
		{
			LOGGER.fine("Infinite curvature");
			return null;
		}
		double [] dcurv = surf.curvatureDirections();
		double [] dcurvmax = temp.t3_0;
		double [] dcurvmin = temp.t3_1;
		if (cmin < cmax)
		{
			System.arraycopy(dcurv, 0, dcurvmax, 0, 3);
			System.arraycopy(dcurv, 3, dcurvmin, 0, 3);
		}
		else
		{
			double tmp = cmin;
			cmin = cmax;
			cmax = tmp;
			System.arraycopy(dcurv, 0, dcurvmin, 0, 3);
			System.arraycopy(dcurv, 3, dcurvmax, 0, 3);
		}
		Matrix3D.prodVect3D(dcurvmax, dcurvmin, temp.t3_2);
		Matrix3D A = new Matrix3D(dcurvmax, dcurvmin, temp.t3_2);
		double epsilon = mp.getDeflection();
		if (epsilon > 1.0)
			epsilon = 1.0;
		//  In org.jcae.mesh.amibe.algos2d.Insertion, mean lengths are
		//  targeted, and there is a sqrt(2) factor.  Division by 2
		//  provides a maximal getDeflectionMetric,
		double alpha2 = 4.0 * epsilon * (2.0 - epsilon) / 2.0;
		double diag = cmax*cmax / alpha2;
		Matrix3D param;
		if (mp.isIsotropic())
			param = new Matrix3D(diag, diag, diag);
		else
		{
			epsilon *= cmax / cmin;
			if (epsilon > 1.0)
				epsilon = 1.0;
			alpha2 = 4.0 * epsilon * (2.0 - epsilon) / 2.0;
			param = new Matrix3D(diag, cmin*cmin / alpha2, 1.0/mp.getLength()/mp.getLength());
		}
		A.transp();
		Matrix3D tempM = param.multL(A);
		A.transp();
		return tempM.multR(A);
	}
	
	private Matrix3D getAbsoluteDeflectionMetric()
	{
		double cmin = Math.abs(surf.minCurvature());
		double cmax = Math.abs(surf.maxCurvature());
		if (Double.isNaN(cmin) || Double.isNaN(cmax))
		{
			LOGGER.fine("Undefined curvature");
			return null;
		}
		if (cmin == 0.0 && cmax == 0.0)
		{
			LOGGER.fine("Null curvature");
			return null;
		}
		double deflection = mp.getDeflection();
		if (deflection * cmax >= 1.0 || deflection * cmin >= 1.0)
		{
			LOGGER.fine("Curvature too large");
			return null;
		}
		double [] dcurv = surf.curvatureDirections();
		double [] dcurvmax = temp.t3_0;
		double [] dcurvmin = temp.t3_1;
		if (cmin < cmax)
		{
			System.arraycopy(dcurv, 0, dcurvmax, 0, 3);
			System.arraycopy(dcurv, 3, dcurvmin, 0, 3);
		}
		else
		{
			double tmp = cmin;
			cmin = cmax;
			cmax = tmp;
			System.arraycopy(dcurv, 0, dcurvmin, 0, 3);
			System.arraycopy(dcurv, 3, dcurvmax, 0, 3);
		}
		Matrix3D.prodVect3D(dcurvmax, dcurvmin, temp.t3_2);
		Matrix3D A = new Matrix3D(dcurvmax, dcurvmin, temp.t3_2);
		double epsilon = deflection * cmax;
		//  In org.jcae.mesh.amibe.algos2d.Insertion, mean lengths are
		//  targeted, and there is a sqrt(2) factor.  Division by 2
		//  provides a maximal getDeflectionMetric,
		double alpha2 = 4.0 * epsilon * (2.0 - epsilon) / 2.0;
		double diag = cmax*cmax / alpha2;
		Matrix3D param;
		if (mp.isIsotropic())
			param = new Matrix3D(diag, diag, diag);
		else
		{
			if (cmin > 0.0)
			{
				epsilon *= cmin / cmax;
				if (epsilon >= 1.0)
					epsilon = 1.0;
				alpha2 = 4.0 * epsilon * (2.0 - epsilon) / 2.0;
			}
			param = new Matrix3D(diag, cmin*cmin / alpha2, diag);
		}
		A.transp();
		Matrix3D tempM = param.multL(A);
		A.transp();
		return tempM.multR(A);
	}
	
	/**
	 * Compute the metric induced to the tangent plane.
	 */
	private static Matrix2D restrict2D(Matrix3D m, CADGeomSurface surf, PoolWorkVectors temp)
	{
		double d1U[] = surf.d1U();
		double d1V[] = surf.d1V();
		// Check whether there is a tangent plane
		Matrix3D.prodVect3D(d1U, d1V, temp.t3_0);
		if (Matrix3D.norm(temp.t3_0) <= 0.0)
			LOGGER.fine("Unable to compute normal vector");

		m.getValues(temp.t9);
		// B = (d1U,d1V,c0) is the local frame.
		// The metrics induced by M3 on the tangent plane is tB M3 B
		// temp32 = M3 * B
		for (int i = 0; i < 3; i++)
		{
			temp.tt32[i][0] = temp.t9[i] * d1U[0] + temp.t9[i+3] * d1U[1] + temp.t9[i+6] * d1U[2];
			temp.tt32[i][1] = temp.t9[i] * d1V[0] + temp.t9[i+3] * d1V[1] + temp.t9[i+6] * d1V[2];
		}
		//  ret = tB * temp32
		return new Matrix2D(
			d1U[0] * temp.tt32[0][0] + d1U[1] * temp.tt32[1][0] + d1U[2] * temp.tt32[2][0],
			d1U[0] * temp.tt32[0][1] + d1U[1] * temp.tt32[1][1] + d1U[2] * temp.tt32[2][1],
			d1V[0] * temp.tt32[0][0] + d1V[1] * temp.tt32[1][0] + d1V[2] * temp.tt32[2][0],
			d1V[0] * temp.tt32[0][1] + d1V[1] * temp.tt32[1][1] + d1V[2] * temp.tt32[2][1]
		);
	}
	
}
