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

import org.jcae.mesh.cad.CADGeomSurface;
import org.jcae.mesh.amibe.ds.Vertex;
import org.apache.log4j.Logger;

/**
 * 2D metrics.  This class provides metrics on the tangent plane.
 * A {@link Metric3D} is computed and projected onto the tangent plane.
 * This metric is then attached to the {@link Vertex} at which it is
 * computed, and is used to compute distance to other vertices in
 * {@link org.jcae.mesh.amibe.ds.tools.Calculus3D}.
 * It can be shown that vertices at a distance <code>D</code> of a point
 * lies on an ellipsis centered at <code>P</code>.
 *
 * <p>
 * If multiple constraints are combined, ellipsis are intersected so that the
 * resulting metrics fulfill all requirements.  There are several ways to
 * perform this intersection, here is how it is done in
 * {@link Matrix2D#intersection(Matrix2D)}.
 * If <code>A</code> and <code>B</code> are 2D metrics, there exists
 * a matrix <code>P</code> such that <code>A=tP d(a1,a2) P</code>
 * and <code>B=tP d(b1,b2) P</code>, where <code>d(x,y)</code> is the
 * diagonal matrix of coefficients <code>x</code> and <code>y</code>.
 * Then the metric <code>C=tP d(max(a1,b1),max(a2,b2)) P</code>
 * defines an ellipsis which is interior to both ellipsis.
 * </p>
 */
public class Metric2D
{
	private static Logger logger=Logger.getLogger(Metric2D.class);
	
	//  First fundamental form
	private double E, F, G;
	private static CADGeomSurface cacheSurf = null;
	private static double discr = 1.0;
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
		if (!surf.equals(cacheSurf))
		{
			surf.dinit(2);
			cacheSurf = surf;
		}
		double [][] temp = { { 0.0, 0.0 }, { 0.0, 0.0 } };
		double sym = 0.0;
		Metric3D m3dbis = null;
		Metric3D m3d = new Metric3D(surf, pt);
		m3d.iso(discr);
		if (Metric3D.hasDeflection())
		{
			m3dbis = new Metric3D(surf, pt);
			if (!m3dbis.deflection())
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
	
	public Metric2D()
	{
		E = 1.0;
		F = 0.0;
		G = 1.0;
	}
	
	/**
	 * Return the determinant of this matrix.
	 *
	 * @return the determinant of this matrix.
	 */
	public double det()
	{
		//  As this matrix is a metric, its determinant
		//  is positive, but there may be rounding errors.
		double ret = E * G - F * F;
		if (ret < 0.0)
		{
			logger.debug("Singular matrix");
			ret = 0.0;
		}
		return ret;
	}
	
	/**
	 * Return the smallest eigenvalue.
	 *
	 * @return the smallest eigenvalue.
	 */
	public double minEV()
	{
		return 0.5 * (E+G - Math.sqrt((E-G)*(E-G)+4.0*F*F));
	}
	
	/**
	 * Return the largest eigenvalue.
	 *
	 * @return the largest eigenvalue.
	 */
	public double maxEV()
	{
		return 0.5 * (E+G + Math.sqrt((E-G)*(E-G)+4.0*F*F));
	}
	
	/**
	 * Return the dot product of two vectors in this Riemannian metrics.
	 *
	 * @param x0 first coordinate of the first vector.
	 * @param y0 second coordinate of the first vector.
	 * @param x1 first coordinate of the second vector.
	 * @param y1 second coordinate of the second vector.
	 * @return the dot product of two vectors in this Riemannian metrics.
	 */
	public double dot(double x0, double y0, double x1, double y1)
	{
		return E * x0 * x1 + F * (x0 * y1 + x1 * y0) + G * y0 * y1;
	}
	
	/**
	 * Return an orthogonal vector in this metrics.
	 * This vector O is such that
	 *   dot(transp(O), O) = 0.
	 *   dot(transp(O), transp(O)) = det(M) dot(V, V).
	 * Warning: for efficiency reasons, the returned array is a static
	 * class variable.
	 *
	 * @param x0 first coordinate
	 * @param y0 second coordinate
	 * @return a static array containing the orthogal vector.
	 */
	public double [] orth(double x0, double y0)
	{
		orthRes[0] = - F * x0 - G * y0;
		orthRes[1] = E * x0 + F * y0;
		return orthRes;
	}
	
	/**
	 * Test whether this matrics is Euclidian.
	 *
	 * @return <code>true</code> if this metrics is quasi-Euclidian,
	 * <code>false</code> otherwise.
	 */
	public boolean isPseudoIsotropic()
	{
		double epsilon = 0.001;
		return (E > 0.0 && Math.abs(F) < epsilon * E && Math.abs(E-G) < epsilon * E);
	}
	
	/**
	 * Set the desired edge length.
	 *
	 * @param l  edge length
	 */
	public static void setLength(double l)
	{
		discr = l;
	}
	
	/**
	 * Get the desired edge length.
	 *
	 * @return  edge length
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
		if (temp < 0.0)
			temp = 0.0;
		return Math.sqrt(temp);
	}
	
	private double [] getCoefs()
	{
		double [] toReturn = new double[3];
		toReturn[0] = E;
		toReturn[1] = F;
		toReturn[2] = G;
		return toReturn;
	}
	
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
