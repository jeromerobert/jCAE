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
				throw new RuntimeException("Syngular matrice: "+this);
			Matrix2D ret = new Matrix2D(data[1][1], -data[0][1], -data[1][0], data[0][0]);
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
		//  For efficiency reasons, restrict2D returns a static array
		double [][] temp = m3d.restrict2D();
		E = temp[0][0];
		F = temp[0][1];
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
			return new Matrix2D(1.0, 0.0, 0.0, 1.0);
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
			ret = 0.0;
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
	
	public String toString()
	{
		return "Metric2D: E="+E+" F="+F+" G="+G;
	}
}
