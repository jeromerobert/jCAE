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
import org.jcae.mesh.util.Calculs;
import org.apache.log4j.Logger;

/**
 * 2D metrics.
 */
public class Metric2D
{
	private static Logger logger=Logger.getLogger(Metric2D.class);
	
	//  First fundamental form
	private double E, F, G;
	private static CADGeomSurface cacheSurf = null;
	private static double discr = 1.0;
	private Vertex loc;
	// Static array to speed up orth() method
	private static double [] orthRes = new double[2];

	private class Matrix2D extends Matrix
	{
		public Matrix2D()
		{
			rank = 2;
			data = new double[rank][rank];
			data[0][0] = 1.0;
			data[0][1] = 0.0;
			data[1][0] = 0.0;
			data[1][1] = 1.0;
		}
		public Matrix2D(double Axx, double Axy, double Ayx, double Ayy)
		{
			rank = 2;
			data = new double[rank][rank];
			data[0][0] = Axx;
			data[0][1] = Axy;
			data[1][0] = Ayx;
			data[1][1] = Ayy;
		}
		public double det()
		{
			return data[0][0] * data[1][1] - data[0][1] * data[1][0];
		}
		public Matrix2D inv()
		{
			double detA = det();
			if (Math.abs(detA) < 1.e-10)
				throw new RuntimeException("Singular matrice: "+this);
			Matrix2D ret = new Matrix2D(data[1][1], -data[1][0], -data[0][1], data[0][0]);
			ret.scale(1.0 / detA);
			return ret;
		}
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
		public double norm2(double vx, double vy)
		{
			return data[0][0] * vx * vx + (data[1][0] + data[0][1]) * vx * vy + data[1][1] * vy * vy;
		}
		
		/**
		 *  Computes the simultaneous reduction of 2 metrics
		 */
		private Matrix2D simultaneousReduction (Matrix2D B)
		{
			Matrix2D ret = new Matrix2D();
			// det(A - l B) = (a11 - l b11)*(a22 - l b22) - (a21 - l b21)^2
			//  = detB l^2 - l (a11*b22+a22*b11-2*a21*b21) + detA
			//  = a l^2 - b l + c
			// Delta = (a11*b22+a22*b11-2*a21*b21)^2 - 4 detA detB
			double a = B.det();
			double b = data[0][0]*B.data[1][1] + data[1][1]*B.data[0][0] - 2.0 * data[1][0]*B.data[1][0];
			double c = det();
			double delta = b*b - 4.0 * a * c;
			if (a < 1.e-10 * b)
			{
				ret.data[0][0] = ret.data[1][1] = 1.0;
				ret.data[0][1] = ret.data[1][0] = 0.0;
				return ret;
			}
			else if (delta < 1.e-4 * Math.abs(a*b))
			{
				//  B is similar to A.  Search for eigenvectors of A
				delta = (data[0][0] - data[1][1]) * (data[0][0] - data[1][1]) + 4.0 * data[0][1] * data[0][1];
				double l1 = 0.5 * (data[0][0] + data[1][1] + Math.sqrt(delta));
				double l2 = 0.5 * (data[0][0] + data[1][1] - Math.sqrt(delta));
				double invnorm1 = 1.0 / Math.sqrt((data[0][0]-l1) * (data[0][0]-l1) + data[1][0] * data[1][0]);
				ret.data[0][0] = -data[1][0] * invnorm1;
				ret.data[1][0] = (data[0][0]-l1) * invnorm1;
				ret.data[0][1] = -ret.data[1][0];
				ret.data[1][1] = ret.data[0][0];
				return ret;
			}
			
			double l1 = 0.5 * (b + Math.sqrt(delta)) / a;
			double l2 = 0.5 * (b - Math.sqrt(delta)) / a;
			/*  Now solve
			 *   (A -l1 B) V1 = 0
			 *  An eigenvector is colinear to
			 *    ( a21-l1*B.a21, -a11+l1 B.a11) and
			 *    ( a22-l1*B.a22, -a21+l1*B.a21)
			 *  and the other one is orthogonal.
			 */
	
			double t1 = (data[0][0]-l1*B.data[0][0]) * (data[0][0]-l1*B.data[0][0]);
			double t2 = (data[1][0]-l1*B.data[1][0]) * (data[1][0]-l1*B.data[1][0]);
			double t3 = (data[1][1]-l1*B.data[1][1]) * (data[1][1]-l1*B.data[1][1]);
			if (t1 < t3)
			{
				double invnorm = 1.0 / Math.sqrt(t2 + t3);
				ret.data[0][0] = (data[1][1]-l1*B.data[1][1]) * invnorm;
				ret.data[1][0] = (-data[1][0]+l1*B.data[1][0]) * invnorm;
			}
			else
			{
				double invnorm = 1.0 / Math.sqrt(t2 + t1);
				ret.data[0][0] = (data[1][0]-l1*B.data[1][0]) * invnorm;
				ret.data[1][0] = (-data[0][0]+l1*B.data[0][0]) * invnorm;
			}
			if (delta == 0.0)
			{
				ret.data[0][1] = - ret.data[1][0];
				ret.data[1][1] = ret.data[0][0];
				return ret;
			}
			
			t1 = (data[0][0]-l2*B.data[0][0]) * (data[0][0]-l2*B.data[0][0]);
			t2 = (data[1][0]-l2*B.data[1][0]) * (data[1][0]-l2*B.data[1][0]);
			t3 = (data[1][1]-l2*B.data[1][1]) * (data[1][1]-l2*B.data[1][1]);
			if (t1 < t3)
			{
				double invnorm = 1.0 / Math.sqrt(t2 + t3);
				ret.data[0][1] = (data[1][1]-l2*B.data[1][1]) * invnorm;
				ret.data[1][1] = (-data[1][0]+l2*B.data[1][0]) * invnorm;
			}
			else
			{
				double invnorm = 1.0 / Math.sqrt(t2 + t1);
				ret.data[0][1] = (data[1][0]-l2*B.data[1][0]) * invnorm;
				ret.data[1][1] = (-data[0][0]+l2*B.data[0][0]) * invnorm;
			}
			return ret;
		}
		
		/**
		 *  Computes the intersection of 2 metrics.
		 */
		public Matrix2D intersection (Matrix2D B)
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
			Matrix2D D = new Matrix2D(ev1, 0.0, 0.0, ev2);
			double a11 = ev1 * resInv.data[0][0] * resInv.data[0][0] + ev2 * resInv.data[1][0] * resInv.data[1][0];
			double a21 = ev1 * resInv.data[0][0] * resInv.data[0][1] + ev2 * resInv.data[1][0] * resInv.data[1][1];
			double a22 = ev1 * resInv.data[0][1] * resInv.data[0][1] + ev2 * resInv.data[1][1] * resInv.data[1][1];
			return new Matrix2D(a11, a21, a21, a22);
		}
		public String toString()
		{
			return "Matric2D: ("+data[0][0]+", "+data[0][1]+", "+data[1][0]+", "+data[1][1]+")";
		}
	}
	
	/**
	 * Creates a <code>Metric2D</code> instance at a given point.
	 *
	 * @param surf  geometrical surface
	 * @param pt  node where metrics is computed.
	 */
	public Metric2D(CADGeomSurface surf, Vertex pt)
	{
		loc = pt;
		if (!surf.equals(cacheSurf))
		{
			surf.dinit(2);
			cacheSurf = surf;
		}
		Metric3D m3d = new Metric3D(surf, pt);
		m3d.iso();
		//  For efficiency reasons, restrict2D returns a static array
		double [][] temp = m3d.restrict2D();
		double sym = 0.5 * (temp[0][1] + temp[1][0]);
		if (Metric3D.hasDeflection())
		{
			Metric3D m3dbis = new Metric3D(surf, pt);
			if (m3dbis.curvIso())
			{
				//  The curvature metrics is defined, so we can compute
				//  its intersection with m3d.
				Matrix2D m2d0 = new Matrix2D(temp[0][0], sym, sym, temp[1][1]);
				temp = m3dbis.restrict2D();
				sym = 0.5 * (temp[0][1] + temp[1][0]);
				Matrix2D m2d1 = new Matrix2D(temp[0][0], sym, sym, temp[1][1]);
				Matrix2D res = m2d0.intersection(m2d1);
				temp[0][0] = res.data[0][0];
				temp[1][1] = res.data[1][1];
				//  FIXME:  Why is -1 needed?
				sym = -0.5 * (res.data[0][1] + res.data[1][0]);
			}
		}
		E = temp[0][0];
		F = sym;
		G = temp[1][1];
	}
	
	public Matrix2D param2tangent()
	{
		double uv[] = loc.getUV();
		cacheSurf.setParameter(uv[0], uv[1]);
		double d1U[] = cacheSurf.d1U();
		double d1V[] = cacheSurf.d1V();
		double nd1U = Calculs.norm(d1U);
		double nd1V = Calculs.norm(d1V);
		if (nd1U * nd1V == 0.0)
		{
			logger.debug("unable to compute tangent plane");
			return new Matrix2D(1.0, 0.0, 0.0, 1.0);
		}
		double unitNorm[] = Calculs.prodVect3D(d1U, d1V);
		double nnorm = Calculs.norm(unitNorm);
		double ct = Calculs.prodSca(d1U, d1V) / nd1U / nd1V;
		double st = nnorm / nd1U / nd1V;
		return (new Matrix2D(nd1U, nd1V * ct, 0, nd1V * st));
	}
	
	public Metric2D()
	{
		E = 1.0;
		F = 0.0;
		G = 1.0;
	}
	
	public Metric2D(CADGeomSurface surf, Vertex pt, double theta, double p1, double p2)
	{
		loc = pt;
		double ct = Math.cos(theta);
		double st = Math.sin(theta);
		double l1 = 1.0/p1/p1;
		double l2 = 1.0/p2/p2;
		E = l1 * ct * ct + l2 * st * st;
		F = (l1 - l2) * ct * st;
		G = l1 * st * st + l2 * ct * ct;
	}
	
	public Matrix2D toMatrix2D()
	{
		return new Matrix2D(E, F, F, G);
	}
	
	public double [] apply(double x1, double y1)
	{
		Matrix2D m = param2tangent();
		double [] in = new double[2];
		in[0] = x1;
		in[1] = y1;
		return m.apply(in);
	}
	
	public double [] applyInv(double x1, double y1)
	{
		Matrix2D m = param2tangent();
		double [] in = new double[2];
		in[0] = x1;
		in[1] = y1;
		return m.inv().apply(in);
	}
	
	public Metric2D add(Metric2D B)
	{
		Metric2D ret = new Metric2D();
		ret.E = E + B.E;
		ret.F = F + B.F;
		ret.G = G + B.G;
		return ret;
	}
	
	public void scale(double f)
	{
		E *= f;
		F *= f;
		G *= f;
	}
	
	public double det()
	{
		//  As this matric is a metric, its determinant
		//  is positive, but there may be rounding errors.
		double ret = E * G - F * F;
		if (ret < 0.0)
		{
			logger.debug("Singular matrix");
			ret = 0.0;
		}
		return ret;
	}
	
	public double minEV()
	{
		return 0.5 * (E+G - Math.sqrt((E-G)*(E-G)+4.0*F*F));
	}
	
	public double maxEV()
	{
		return 0.5 * (E+G + Math.sqrt((E-G)*(E-G)+4.0*F*F));
	}
	
	public double dot(double x0, double y0, double x1, double y1)
	{
		return E * x0 * x1 + F * (x0 * y1 + x1 * y0) + G * y0 * y1;
	}
	
	public double [] orth(double x0, double y0)
	{
		orthRes[0] = - F * x0 - G * y0;
		orthRes[1] = E * x0 + F * y0;
		return orthRes;
	}
	
	public boolean isPseudoIsotropic()
	{
		double epsilon = 0.001;
		return (E > 0.0 && Math.abs(F) < epsilon * E && Math.abs(E-G) < epsilon * E);
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
	 * Returns the distance between two points with this metrics.
	 *
	 * @param p1  coordinates of the first node
	 * @param p2  coordinates of the second node
	 * @return the distance between two points with this metrics.
	 */
	public double distance(double []p1, double []p2)
	{
		double u = p2[0] - p1[0];
		double v = p2[1] - p1[1];
		double temp = E * u * u + 2.0 * F * u * v + G * v * v;
		if (temp > 0.0)
			return Math.sqrt(temp);
		return 0.0;
	}
	
	public double [] getCoefs()
	{
		double [] toReturn = new double[3];
		toReturn[0] = E;
		toReturn[1] = F;
		toReturn[2] = G;
		return toReturn;
	}
	
	/**
	 *  Computes the intersection of 2 metrics, but preserves the
	 *  principal directions of active metric
	 */
/*
	public Metric2D intersectionPreserve (Metric2D B)
	{
		double V1x = simultaneousReduction(B);
		double V1y = Math.sqrt(1. - sqr(V1x));

		//  Eigenvalues of A
		double evA1 = data[0][0]*sqr(V1x) + 2.*data[1][0]*V1x*V1y + data[1][1]*sqr(V1y);
		double evA2 = data[0][0]*sqr(V1y) - 2.*data[1][0]*V1x*V1y + data[1][1]*sqr(V1x);
		//  Eigenvalues of B
		double evB1 = B.data[0][0]*sqr(V1x) + 2.*B.data[1][0]*V1x*V1y + B.data[1][1]*sqr(V1y);
		double evB2 = B.data[0][0]*sqr(V1y) - 2.*B.data[1][0]*V1x*V1y + B.data[1][1]*sqr(V1x);

		double omega = Math.max(evB1/evA1, evB2/evA2);
		omega = Math.max(omega, 1.);
		return new Metric2D(omega*data[0][0], omega*data[1][0], omega*data[1][1]);
	}
*/
	
	public String toString()
	{
		return "Metric2D: E="+E+" F="+F+" G="+G;
	}
}
