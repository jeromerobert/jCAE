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
		double [][] temp = { { 0.0, 0.0 }, { 0.0, 0.0 } };
		double sym = 0.0;
		Metric3D m3d = null;
		Metric3D m3dbis = null;
		if (Metric3D.hasLength())
		{
			m3d = new Metric3D(surf, pt);
			m3d.iso();
		}
		if (Metric3D.hasDeflection())
		{
			m3dbis = new Metric3D(surf, pt);
			if (!m3dbis.curvIso())
				m3dbis = null;
		}
		if (m3d != null)
		{
			//  For efficiency reasons, restrict2D returns a static array
			temp = m3d.restrict2D();
			sym = 0.5 * (temp[0][1] + temp[1][0]);
 			if (m3dbis != null)
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
				sym = 0.5 * (res.data[0][1] + res.data[1][0]);
			}
		}
		else
		{
 			if (m3dbis != null)
			{
				temp = m3dbis.restrict2D();
				sym = 0.5 * (temp[0][1] + temp[1][0]);
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
		double nd1U = Metric3D.norm(d1U);
		double nd1V = Metric3D.norm(d1V);
		if (nd1U * nd1V == 0.0)
		{
			logger.debug("unable to compute tangent plane");
			return new Matrix2D(1.0, 0.0, 0.0, 1.0);
		}
		double unitNorm[] = Metric3D.prodVect3D(d1U, d1V);
		double nnorm = Metric3D.norm(unitNorm);
		double ct = Metric3D.prodSca(d1U, d1V) / nd1U / nd1V;
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
	
	public String stringCoefs()
	{
		return ""+E+" "+F+" "+G;
	}
	
	public String stringCoefs(double scaleX, double scaleY)
	{
		return ""+(E/scaleX/scaleX)+" "+(F/scaleX/scaleY)+" "+(G/scaleY/scaleY);
	}
	
	public String toString()
	{
		return "Metric2D: E="+E+" F="+F+" G="+G;
	}
	
}
