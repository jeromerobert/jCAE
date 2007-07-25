/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2003,2006 by EADS CRC

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

import org.jcae.mesh.amibe.ds.Vertex;
import java.io.Serializable;

/**
 * Garland's Quadric Error Metric.  See
 * <a href="http://graphics.cs.uiuc.edu/~garland/research/quadrics.html">quadric error metrics</a>.
 *
 * <p>
 * A plane is fully determined by its normal <code>N</code> and the signed
 * distance <code>d</code> of the frame origin to this plane, or in other 
 * words the equation of this plane is <code>tN V + d = 0</code>.
 * The squared distance of a point to this plane is
 * </p>
 * <pre>
 *   D*D = (tN V + d) * (tN V + d)
 *       = tV (N tN) V + 2d tN V + d*d
 *       = tV A V + 2 tB V + c
 * </pre>
 * <p>
 * The quadric <code>Q=(A,B,c)=(N tN, dN, d*d)</code> is thus naturally
 * defined.  Addition of these quadrics have a simple form:
 * <code>Q1(V)+Q2(V)=(Q1+Q2)(V)</code> with
 * <code>Q1+Q2=(A1+A2, B1+B2, c1+c2)</code>
 * To compute the squared distance of a point to a set of planes, we can
 * then compute this quadric for each plane and sum each element of
 * these quadrics.  
 * </p>
 *
 * <p>
 * When an edge <code>(V1,V2)</code> is contracted into <code>V3</code>,
 * <code>Q1(V3)+Q2(V3)</code> represents the deviation to the set of
 * planes at <code>V1</code> and <code>V2</code>.  The cost of this
 * contraction is thus defined as <code>Q1(V3)+Q2(V3)</code>.
 * We want to minimize this error.  It can be shown that if <code>A</code>
 * is non singular, the optimal placement is for <code>V3=-inv(A) B</code>.
 * </p>
 */
public class Quadric3DError implements Serializable
{
	public Metric3D A = new Metric3D();
	public double [] b = new double[3];
	public double [] temp = new double[3];
	public double c;
	public double area;

	/**
	 * Optimal placement strategy, select the best vertex.
	 */
	public static final int POS_VERTEX = 0;
	/**
	 * Optimal placement strategy, contract an edge into its middle point.
	 */
	public static final int POS_MIDDLE = 1;
	/**
	 * Optimal placement strategy, the contracted point is on the edge.
	 */
	public static final int POS_EDGE = 2;
	/**
	 * Optimal placement strategy, the contracted point is the point which
	 * minimizes error metric.
	 */
	public static final int POS_OPTIMAL = 3;
	
	public Quadric3DError()
	{
		// By default, A is null
		A.reset();
	}

	// Add 2 quadrics
	public void computeQuadric3DError(Quadric3DError q1, Quadric3DError q2)
	{
		assert q1.area > 0.0 : q1;
		assert q2.area > 0.0 : q2;
		double l1 = q1.area / (q1.area + q2.area);
		double l2 = q2.area / (q1.area + q2.area);
		A.saxpby0(l1, q1.A, l2, q2.A);
		for (int i = 0; i < 3; i++)
			b[i] = l1 * q1.b[i] + l2 * q2.b[i];
		c = l1 * q1.c + l2 * q2.c;
		area = q1.area + q2.area;
	}

	public double value(double [] vect)
	{
		double ret = c;
		ret += 2.0 * Matrix3D.prodSca(b, vect);
		A.apply(vect, temp);
		ret += Matrix3D.prodSca(temp, vect);
		return ret;
	}

	public void addError(double [] normal, double d, double a)
	{
		for (int k = 0; k < 3; k++)
		{
			b[k] += d * normal[k];
			for (int l = 0; l < 3; l++)
				A.data[k+3*l] += normal[k]*normal[l];
		}
		c += d*d;
		area += a;
	}

	public void optimalPlacement(Vertex v1, Vertex v2, Quadric3DError q1, Quadric3DError q2, int placement, Vertex ret)
	{
		/* FIXME: add an option so that boundary nodes may be frozen.  */
		if (placement == POS_VERTEX)
			ret.copy(bestCandidateV1V2(v1, v2, q1, q2));
		else if (placement == POS_MIDDLE)
		{
			// Keep a reference if there is one
			double [] p1 = v1.getUV();
			double [] p2 = v2.getUV();
			ret.copy(bestCandidateV1V2Ref(v1, v2, q1, q2));
			ret.moveTo(0.5*(p1[0]+p2[0]), 0.5*(p1[1]+p2[1]), 0.5*(p1[2]+p2[2]));
		}
		else
		{
			// POS_EDGE and POS_OPTIMAL
			// Keep a reference if there is one
			if (placement == POS_OPTIMAL)
			{
				Metric3D Qinv = A.inv();
				if (Qinv != null)
				{
					Qinv.apply(b, temp);
					ret.copy(bestCandidateV1V2Ref(v1, v2, q1, q2));
					ret.moveTo(-temp[0], -temp[1], -temp[2]);
				}
				else
					ret.copy(bestCandidateV1V2(v1, v2, q1, q2));
				return;
			}
			if (A.det() > 1.e-20)
			{
				// Find M = v1 + s(v2-v1) which minimizes
				//   q(M) = s^2 (v2-v1)A(v2-v1) + s(v1A(v2-v1)+(v2-v1)Av1+2b(v2-v1))+cte
				//   q'(M) = 2 s (v2-v1)A(v2-v1) + 2(v1A(v2-v1)+b(v2-v1))
				double [] p1 = v1.getUV();
				double [] p2 = v2.getUV();
				for (int i = 0; i < 3; i++)
					temp[i] = p2[i] - p1[i];
				double den = 0.0;
				double num = Matrix3D.prodSca(b, temp);
				for (int i = 0; i < 3; i++)
				{
					for (int j = 0; j < 3; j++)
					{
						den += A.data[i+3*j] * temp[i] * temp[j];
						num += A.data[i+3*j] * temp[i] * p1[j];
					}
				}
				if (den > 1.0e-4 * Math.abs(num))
				{
					double s = - num / den;
					if (s < 1.0e-4)
						s = 0.0;
					else if (s > 1.0 - 1.0e-4)
						s = 1.0;
					ret.copy(bestCandidateV1V2Ref(v1, v2, q1, q2));
					ret.moveTo(p1[0]+s*temp[0], p1[1]+s*temp[1], p1[2]+s*temp[2]);
					return;
				}
			}
			ret.copy(bestCandidateV1V2(v1, v2, q1, q2));
		}
	}

	private static Vertex bestCandidateV1V2(Vertex v1, Vertex v2, Quadric3DError q1, Quadric3DError q2)
	{
		if (q1.value(v1.getUV()) + q2.value(v1.getUV()) < q1.value(v2.getUV()) + q2.value(v2.getUV()))
			return v1;
		return v2;
	}

	private static Vertex bestCandidateV1V2Ref(Vertex v1, Vertex v2, Quadric3DError q1, Quadric3DError q2)
	{
		if (v1.getRef() == 0 && v2.getRef() == 0)
			return bestCandidateV1V2(v1, v2, q1, q2);
		else if (v1.getRef() == 0)
			return v2;
		else if (v2.getRef() == 0)
			return v1;
		else
			return bestCandidateV1V2(v1, v2, q1, q2);
	}

	public String toString()
	{
		return "A: "+A+"\n"+
		       " b: "+b[0]+" "+b[1]+" "+b[2]+"\n"+
		       " c: "+c;
	}
	
}
