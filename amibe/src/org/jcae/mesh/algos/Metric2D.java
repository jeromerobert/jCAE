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
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

package org.jcae.mesh.algos;

import  org.jcae.opencascade.jni.*;

/**
 *  Mathematics
 *  In the 2D plane, a metric M can be defined by either
 *    a.  Its coefficient values a11, a21 and a22
 *    b.  Its eigenvalues and a normal vector (x,y)
 *    c.  Its eigenvalues and an angle t
 *   Indeed a metric is a symmetric positive definite quadratic form
 *   it has thus 2 positive eigenvalues and orthogonal eigenvectors.
 *
 *   Definitions:
 *     Tr(A) = a11 + a22 = l1 + l2
 *     det(A) = a11*a22 - a21*a21 = l1 * l2
 *
 *   Eigenvalues are solution of det(M - x Id) = 0
 *     x^2 - Tr(M) x + det(M) = 0
 *     Delta = Tr(M)^2 - 4 * det(M)
 *     l1 = (Tr(M) + sqrt(Delta))/2
 *     l2 = (Tr(M) - sqrt(Delta))/2
 *
 *               /  a22  -a21 \
 *      inv(A) = |            |  / det(A)
 *               \ -a21   a11 /
 *
 *   Let V1(V1x,V1y) and V2=(V2x,V2y) 2 normalized eigenvectors of A
 *         / V1x  V1y \
 *     P = |          |
 *         \ V2x  V2y /
 *   Note: V2 can be chosen as (-V1y,V1x) and thus
 *         / V1x  V1y \
 *     P = |          |
 *         \ -V1y V1x /
 *   Then inv(P) = tP and
 *     A = tP D(l1,l2) P
 *       a11 = l1*V1x^2 + l2*V1y^2
 *       a21 = (l1-l2)*V1x*V1y
 *       a22 = l2*V1x^2 + l1*V1y^2
 *
 *   Moreover
 *    l1 = tV1 A V1 = a11 V1x^2 + 2 a21 V1x V1y + a22 V1y^2
 *    l2 = tV2 A V2 = a11 V1y^2 - 2 a21 V1x V1y + a22 V1x^2
 */

/**
 */
public class Metric2D
{
	protected double a11;
	protected double a21;
	protected double a22;
	
	public Metric2D(double a11, double a21, double a22)
	{
		this.a11 = a11;
		this.a21 = a21;
		this.a22 = a22;
	}

	private static double sqr(double x)
	{
		return x*x;
	}

	public Metric2D(GeomLProp_SLProps sprop, double u, double v)
	{
		double k1 = sprop.minCurvature();
		double k2 = sprop.maxCurvature();
		//  Huh?  Why does ICC not take sign into account?
		//  We can have |k1| > |k2|
		if (Math.abs(k1) > Math.abs(k2)) {
			double temp = k1;
			k1 = k2;
			k2 = temp;
		}
	}

	public double det()
	{
		return (a11*a22 - a21*a21);
	}

	/**
	 *  Computes the simultaneous reduction of 2 metrics
	 *  Returns a double cosT such that V1(cosT,sinT) satisfies
	 *     A V1 = lambda1 V1,  B V1 = mu1 V1
	 *     A V2 = lambda2 V2,  B V2 = mu2 V2
	 *  where V2(-sinT,cosT)
	 *  Note: sinT = sart(1-cosT^2)
	 */
	private double simultaneousReduction (Metric2D B)
	{
		// det(A - l B) = (a11 - l b11)*(a22 - l b22) - (a21 - l b21)^2
		//  = detB l^2 - l (a11*b22+a22*b11-2*a21*b21) + detA
		//  = a l^2 - b l + c
		// Delta = (a11*b22+a22*b11-2*a21*b21)^2 - 4 detA detB
		double a = B.det();
		double b = a11*B.a22 + a22*B.a11 - 2.0 * a21*B.a21;
		double c = det();
		double delta = b*b - 4.0 * a * c;
		double l1 = 0.5 * (b + Math.sqrt(delta)) / a;
		double l2 = 0.5 * (b - Math.sqrt(delta)) / a;
		/*  Now solve
		 *   (A -l1 B) V1 = 0
		 *  An eigenvector is colinear to
		 *    (-a21+l1*B.a21, a11-l1 B.a11) and
		 *    ( a22-l1*B.a22,-a21+l1*B.a21)
		 *  and the other one is orthogonal.
		 */

		double norm = sqr(a11-l1*B.a11) + sqr(-a21+l1*B.a21);
		if (norm < 1.e-20)
			return 1.;
		return (-a21+l1*B.a21) / Math.sqrt(norm);
	}

	/**
	 *  Computes the intersection of 2 metrics.
	 */
	public Metric2D intersection (Metric2D B)
	{
		double V1x = simultaneousReduction(B);
		double V1y = Math.sqrt(1. - sqr(V1x));

		//  Eigenvalues of A
		double evA1 = a11*sqr(V1x) + 2.*a21*V1x*V1y + a22*sqr(V1y);
		double evA2 = a11*sqr(V1y) - 2.*a21*V1x*V1y + a22*sqr(V1x);
		//  Eigenvalues of B
		double evB1 = B.a11*sqr(V1x) + 2.*B.a21*V1x*V1y + B.a22*sqr(V1y);
		double evB2 = B.a11*sqr(V1y) - 2.*B.a21*V1x*V1y + B.a22*sqr(V1x);

		double ev1 = Math.max(evA1, evB1);
		double ev2 = Math.max(evA2, evB2);
		return new Metric2D(
			ev1*sqr(V1x) + ev2*sqr(V1y),
			(ev1-ev2) * V1x*V1y,
			ev2*sqr(V1x) + ev2*sqr(V1y));
	}

	/**
	 *  Computes the intersection of 2 metrics, but preserves the
	 *  principal directions of active metric
	 */
	public Metric2D intersectionPreserve (Metric2D B)
	{
		double V1x = simultaneousReduction(B);
		double V1y = Math.sqrt(1. - sqr(V1x));

		//  Eigenvalues of A
		double evA1 = a11*sqr(V1x) + 2.*a21*V1x*V1y + a22*sqr(V1y);
		double evA2 = a11*sqr(V1y) - 2.*a21*V1x*V1y + a22*sqr(V1x);
		//  Eigenvalues of B
		double evB1 = B.a11*sqr(V1x) + 2.*B.a21*V1x*V1y + B.a22*sqr(V1y);
		double evB2 = B.a11*sqr(V1y) - 2.*B.a21*V1x*V1y + B.a22*sqr(V1x);

		double omega = Math.max(evB1/evA1, evB2/evA2);
		omega = Math.max(omega, 1.);
		return new Metric2D(omega*a11, omega*a21, omega*a22);
	}
}
